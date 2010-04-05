package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ArrayCreationObject;
import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public abstract class AbstractStatement {

	//private Statement statement;
	private ASTInformation statement;
	private CompositeStatementObject parent;
	private List<MethodInvocationObject> methodInvocationList;
	private List<SuperMethodInvocationObject> superMethodInvocationList;
    private List<FieldInstructionObject> fieldInstructionList;
    private List<LocalVariableDeclarationObject> localVariableDeclarationList;
    private List<LocalVariableInstructionObject> localVariableInstructionList;
    private List<CreationObject> creationList;
    
    public AbstractStatement(Statement statement) {
    	//this.statement = statement;
    	this.statement = ASTInformationGenerator.generateASTInformation(statement);
    	this.parent = null;
    	this.methodInvocationList = new ArrayList<MethodInvocationObject>();
    	this.superMethodInvocationList = new ArrayList<SuperMethodInvocationObject>();
        this.fieldInstructionList = new ArrayList<FieldInstructionObject>();
        this.localVariableDeclarationList = new ArrayList<LocalVariableDeclarationObject>();
        this.localVariableInstructionList = new ArrayList<LocalVariableInstructionObject>();
        this.creationList = new ArrayList<CreationObject>();
        
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
						TypeObject fieldType = TypeObject.extractTypeObject(qualifiedName);
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
						TypeObject localVariableType = TypeObject.extractTypeObject(qualifiedName);
						String localVariableName = variableBinding.getName();
						if(simpleName.isDeclaration()) {
							LocalVariableDeclarationObject localVariable = new LocalVariableDeclarationObject(localVariableType, localVariableName);
							VariableDeclaration variableDeclaration = (VariableDeclaration)simpleName.getParent();
							localVariable.setVariableDeclaration(variableDeclaration);
							localVariableDeclarationList.add(localVariable);
						}
						else {
							LocalVariableInstructionObject localVariable = new LocalVariableInstructionObject(localVariableType, localVariableName);
							localVariable.setSimpleName(simpleName);
							localVariableInstructionList.add(localVariable);
						}
					}
				}
			}
		}
		
		List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
		for(Expression methodInvocation : methodInvocations) {
			if(methodInvocation instanceof MethodInvocation) {
				IMethodBinding methodBinding = ((MethodInvocation)methodInvocation).resolveMethodBinding();
				String originClassName = methodBinding.getDeclaringClass().getQualifiedName();
				String methodInvocationName = methodBinding.getName();
				String qualifiedName = methodBinding.getReturnType().getQualifiedName();
				TypeObject returnType = TypeObject.extractTypeObject(qualifiedName);
				MethodInvocationObject methodInvocationObject = new MethodInvocationObject(originClassName, methodInvocationName, returnType);
				methodInvocationObject.setMethodInvocation((MethodInvocation)methodInvocation);
				ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
				for(ITypeBinding parameterType : parameterTypes) {
					String qualifiedParameterName = parameterType.getQualifiedName();
					TypeObject typeObject = TypeObject.extractTypeObject(qualifiedParameterName);
					methodInvocationObject.addParameter(typeObject);
				}
				if((methodBinding.getModifiers() & Modifier.STATIC) != 0)
					methodInvocationObject.setStatic(true);
				methodInvocationList.add(methodInvocationObject);
			}
			else if(methodInvocation instanceof SuperMethodInvocation) {
				IMethodBinding methodBinding = ((SuperMethodInvocation)methodInvocation).resolveMethodBinding();
				String originClassName = methodBinding.getDeclaringClass().getQualifiedName();
				String methodInvocationName = methodBinding.getName();
				String qualifiedName = methodBinding.getReturnType().getQualifiedName();
				TypeObject returnType = TypeObject.extractTypeObject(qualifiedName);
				SuperMethodInvocationObject superMethodInvocationObject = new SuperMethodInvocationObject(originClassName, methodInvocationName, returnType);
				superMethodInvocationObject.setSuperMethodInvocation((SuperMethodInvocation)methodInvocation);
				ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
				for(ITypeBinding parameterType : parameterTypes) {
					String qualifiedParameterName = parameterType.getQualifiedName();
					TypeObject typeObject = TypeObject.extractTypeObject(qualifiedParameterName);
					superMethodInvocationObject.addParameter(typeObject);
				}
				superMethodInvocationList.add(superMethodInvocationObject);
			}
		}
		
		List<Expression> classInctanceCreations = expressionExtractor.getClassInstanceCreations(statement);
		for(Expression classInstanceCreationExpression : classInctanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)classInstanceCreationExpression;
			IMethodBinding constructorBinding = classInstanceCreation.resolveConstructorBinding();
			Type type = classInstanceCreation.getType();
			ITypeBinding typeBinding = type.resolveBinding();
			String qualifiedTypeName = typeBinding.getQualifiedName();
			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedTypeName);
			ClassInstanceCreationObject creationObject = new ClassInstanceCreationObject(typeObject);
			creationObject.setClassInstanceCreation(classInstanceCreation);
			ITypeBinding[] parameterTypes = constructorBinding.getParameterTypes();
			for(ITypeBinding parameterType : parameterTypes) {
				String qualifiedParameterName = parameterType.getQualifiedName();
				TypeObject parameterTypeObject = TypeObject.extractTypeObject(qualifiedParameterName);
				creationObject.addParameter(parameterTypeObject);
			}
			creationList.add(creationObject);
		}
		
		List<Expression> arrayCreations = expressionExtractor.getArrayCreations(statement);
		for(Expression arrayCreationExpression : arrayCreations) {
			ArrayCreation arrayCreation = (ArrayCreation)arrayCreationExpression;
			Type type = arrayCreation.getType();
			ITypeBinding typeBinding = type.resolveBinding();
			String qualifiedTypeName = typeBinding.getQualifiedName();
			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedTypeName);
			ArrayCreationObject creationObject = new ArrayCreationObject(typeObject);
			creationObject.setArrayCreation(arrayCreation);
			creationList.add(creationObject);
		}
    }

    public void setParent(CompositeStatementObject parent) {
    	this.parent = parent;
    }

    public CompositeStatementObject getParent() {
    	return this.parent;
    }

    public Statement getStatement() {
    	//return statement;
    	return (Statement)this.statement.recoverASTNode();
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

	public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
		return superMethodInvocationList;
	}

	public List<CreationObject> getCreations() {
		return creationList;
	}

	public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
		return methodInvocationList.contains(methodInvocation);
	}

	public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
		return superMethodInvocationList.contains(superMethodInvocation);
	}

	public boolean containsLocalVariableDeclaration(LocalVariableDeclarationObject lvdo) {
		return localVariableDeclarationList.contains(lvdo);
	}

	/*public boolean equals(Object o) {
		if(this == o)
    		return true;
    	
    	if(o instanceof AbstractStatement) {
    		AbstractStatement abstractStatement = (AbstractStatement)o;
    		return this.statement.equals(abstractStatement.statement);
    	}
    	return false;
	}

	public int hashCode() {
		return statement.hashCode();
	}*/

	public String toString() {
		return statement.toString();
	}
}
