package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public abstract class AbstractStatement {

	private Statement statement;
	private CompositeStatementObject parent;
	private List<MethodInvocationObject> methodInvocationList;
    private List<FieldInstructionObject> fieldInstructionList;
    private List<LocalVariableDeclarationObject> localVariableDeclarationList;
    private List<LocalVariableInstructionObject> localVariableInstructionList;
    
    public AbstractStatement(Statement statement) {
    	this.statement = statement;
    	this.parent = null;
    	this.methodInvocationList = new ArrayList<MethodInvocationObject>();
        this.fieldInstructionList = new ArrayList<FieldInstructionObject>();
        this.localVariableDeclarationList = new ArrayList<LocalVariableDeclarationObject>();
        this.localVariableInstructionList = new ArrayList<LocalVariableInstructionObject>();
        
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(statement);
		for(Expression variableInstruction : variableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField()) {
					if(variableBinding.getDeclaringClass() != null) {
						String originClassName = variableBinding.getDeclaringClass().getQualifiedName();
						String qualifiedName = variableBinding.getType().getQualifiedName();
						TypeObject fieldType = extractTypeObject(qualifiedName);
						String fieldName = variableBinding.getName();
						FieldInstructionObject fieldInstruction = new FieldInstructionObject(originClassName, fieldType, fieldName);
						fieldInstruction.setSimpleName(simpleName);
						if((variableBinding.getModifiers() & Modifier.STATIC) != 0)
							fieldInstruction.setStatic(true);
						fieldInstructionList.add(fieldInstruction);
					}
				}
				else {
					if(variableBinding.getDeclaringClass() == null) {
						String qualifiedName = variableBinding.getType().getQualifiedName();
						TypeObject localVariableType = extractTypeObject(qualifiedName);
						String localVariableName = variableBinding.getName();
						if(simpleName.isDeclaration()) {
							LocalVariableDeclarationObject localVariable = new LocalVariableDeclarationObject(localVariableType, localVariableName);
							localVariableDeclarationList.add(localVariable);
						}
						else {
							LocalVariableInstructionObject localVariable = new LocalVariableInstructionObject(localVariableType, localVariableName);
							localVariableInstructionList.add(localVariable);
						}
					}
				}
			}
		}
		
		List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
		for(Expression methodInvocation : methodInvocations) {
			IMethodBinding methodBinding = null;
			if(methodInvocation instanceof MethodInvocation)
				methodBinding = ((MethodInvocation)methodInvocation).resolveMethodBinding();
			else if(methodInvocation instanceof SuperMethodInvocation)
				methodBinding = ((SuperMethodInvocation)methodInvocation).resolveMethodBinding();
			String originClassName = methodBinding.getDeclaringClass().getQualifiedName();
			String methodInvocationName = methodBinding.getName();
			String qualifiedName = methodBinding.getReturnType().getQualifiedName();
			TypeObject returnType = extractTypeObject(qualifiedName);
			MethodInvocationObject methodInvocationObject = new MethodInvocationObject(originClassName, methodInvocationName, returnType);
			if(methodInvocation instanceof MethodInvocation)
				methodInvocationObject.setMethodInvocation((MethodInvocation)methodInvocation);
			ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
			for(ITypeBinding parameterType : parameterTypes) {
				String qualifiedParameterName = parameterType.getQualifiedName();
				TypeObject typeObject = extractTypeObject(qualifiedParameterName);
				methodInvocationObject.addParameter(typeObject);
			}
			methodInvocationList.add(methodInvocationObject);
		}
    }

    public void setParent(CompositeStatementObject parent) {
    	this.parent = parent;
    }

    public CompositeStatementObject getParent() {
    	return this.parent;
    }

    public Statement getStatement() {
    	return statement;
    }

	public List<FieldInstructionObject> getFieldInstructions() {
		return fieldInstructionList;
	}

	public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
		return localVariableDeclarationList;
	}

	public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
		return localVariableInstructionList;
	}

	public List<MethodInvocationObject> getMethodInvocations() {
		return methodInvocationList;
	}

	public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
		return methodInvocationList.contains(methodInvocation);
	}

	public boolean containsLocalVariableDeclaration(LocalVariableDeclarationObject lvdo) {
		return localVariableDeclarationList.contains(lvdo);
	}

	private TypeObject extractTypeObject(String qualifiedName) {
		int arrayDimension = 0;
		String generic = null;
		if(qualifiedName.contains("[") && qualifiedName.contains("]")) {
			String arrayDimensionStr = qualifiedName.substring(qualifiedName.indexOf("["), qualifiedName.lastIndexOf("]")+1);
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("["));
			while(arrayDimensionStr.indexOf("[]") != -1) {
				arrayDimensionStr = arrayDimensionStr.substring(arrayDimensionStr.indexOf("]")+1,arrayDimensionStr.length());
				arrayDimension++;
			}
		}
		if(qualifiedName.contains("<") && qualifiedName.contains(">")) {
			generic = qualifiedName.substring(qualifiedName.indexOf("<"), qualifiedName.lastIndexOf(">")+1);
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
		}
		TypeObject typeObject = new TypeObject(qualifiedName);
		typeObject.setGeneric(generic);
		typeObject.setArrayDimension(arrayDimension);
		return typeObject;
	}
	
	public String toString() {
		/*
		StringBuilder sb = new StringBuilder();
		if(fieldInstructionList.size() > 0) {
        	sb.append("FIELD_INSTRUCTIONS:");
	        for(FieldInstructionObject fio : fieldInstructionList) {
	        	sb.append("\n\t").append(fio.toString());
	        }
	        sb.append("\n");
		}
		if(localVariableDeclarationList.size() > 0) {
        	sb.append("LOCAL_VARIABLE_DECLARATIONS:");
	        for(LocalVariableDeclarationObject lvdo : localVariableDeclarationList) {
	        	sb.append("\n\t").append(lvdo.toString());
	        }
	        sb.append("\n");
        }
        if(localVariableInstructionList.size() > 0) {
        	sb.append("LOCAL_VARIABLE_INSTRUCTIONS:");
	        for(LocalVariableInstructionObject lvio : localVariableInstructionList) {
	        	sb.append("\n\t").append(lvio.toString());
	        }
	        sb.append("\n");
        }
        if(methodInvocationList.size() > 0) {
        	sb.append("METHOD_INVOCATIONS:");
	        for(MethodInvocationObject mio : methodInvocationList) {
	            sb.append("\n\t").append(mio.toString());
	        }
	        sb.append("\n");
        }
        return sb.toString();
        */
		if(statement != null)
			return statement.toString();
		else
			return null;
	}
}
