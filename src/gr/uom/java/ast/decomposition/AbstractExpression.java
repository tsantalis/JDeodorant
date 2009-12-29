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

import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class AbstractExpression {
	
	//private Expression expression;
	private ASTInformation expression;
	private CompositeStatementObject owner;
	private List<MethodInvocationObject> methodInvocationList;
	private List<SuperMethodInvocationObject> superMethodInvocationList;
    private List<FieldInstructionObject> fieldInstructionList;
    private List<LocalVariableDeclarationObject> localVariableDeclarationList;
    private List<LocalVariableInstructionObject> localVariableInstructionList;
    private List<CreationObject> creationList;
    
    public AbstractExpression(Expression expression) {
    	//this.expression = expression;
    	this.expression = ASTInformationGenerator.generateASTInformation(expression);
    	this.owner = null;
    	this.methodInvocationList = new ArrayList<MethodInvocationObject>();
    	this.superMethodInvocationList = new ArrayList<SuperMethodInvocationObject>();
        this.fieldInstructionList = new ArrayList<FieldInstructionObject>();
        this.localVariableDeclarationList = new ArrayList<LocalVariableDeclarationObject>();
        this.localVariableInstructionList = new ArrayList<LocalVariableInstructionObject>();
        this.creationList = new ArrayList<CreationObject>();
        
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(expression);
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
		
		List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(expression);
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
		
		List<Expression> classInctanceCreations = expressionExtractor.getClassInstanceCreations(expression);
		for(Expression classInstanceCreationExpression : classInctanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)classInstanceCreationExpression;
			Type type = classInstanceCreation.getType();
			ITypeBinding typeBinding = type.resolveBinding();
			String qualifiedTypeName = typeBinding.getQualifiedName();
			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedTypeName);
			ClassInstanceCreationObject creationObject = new ClassInstanceCreationObject(typeObject);
			creationObject.setClassInstanceCreation(classInstanceCreation);
			creationList.add(creationObject);
		}

		List<Expression> arrayCreations = expressionExtractor.getArrayCreations(expression);
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

	public List<Assignment> getLocalVariableAssignments(LocalVariableInstructionObject lvio) {
		List<Assignment> localVariableAssignments = new ArrayList<Assignment>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> assignments = expressionExtractor.getAssignments(getExpression());
		for(Expression expression : assignments) {
			Assignment assignment = (Assignment)expression;
			Expression leftHandSide = assignment.getLeftHandSide();
			SimpleName leftHandSideName = processExpression(leftHandSide);
			if(leftHandSideName != null && leftHandSideName.equals(lvio.getSimpleName())) {
				localVariableAssignments.add(assignment);
			}
		}
		return localVariableAssignments;
	}
	
	public List<PostfixExpression> getLocalVariablePostfixAssignments(LocalVariableInstructionObject lvio) {
		List<PostfixExpression> localVariablePostfixAssignments = new ArrayList<PostfixExpression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(getExpression());
		for(Expression expression : postfixExpressions) {
			PostfixExpression postfixExpression = (PostfixExpression)expression;
			Expression operand = postfixExpression.getOperand();
			SimpleName operandName = processExpression(operand);
			if(operandName != null && operandName.equals(lvio.getSimpleName())) {
				localVariablePostfixAssignments.add(postfixExpression);
			}
		}
		return localVariablePostfixAssignments;
	}
	
	public List<PrefixExpression> getLocalVariablePrefixAssignments(LocalVariableInstructionObject lvio) {
		List<PrefixExpression> localVariablePrefixAssignments = new ArrayList<PrefixExpression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(getExpression());
		for(Expression expression : prefixExpressions) {
			PrefixExpression prefixExpression = (PrefixExpression)expression;
			Expression operand = prefixExpression.getOperand();
			PrefixExpression.Operator operator = prefixExpression.getOperator();
			SimpleName operandName = processExpression(operand);
			if(operandName != null && operandName.equals(lvio.getSimpleName()) && (operator.equals(PrefixExpression.Operator.INCREMENT) ||
					operator.equals(PrefixExpression.Operator.DECREMENT))) {
				localVariablePrefixAssignments.add(prefixExpression);
			}
		}
		return localVariablePrefixAssignments;
	}

	public List<Assignment> getFieldAssignments(FieldInstructionObject fio) {
		List<Assignment> fieldAssignments = new ArrayList<Assignment>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> assignments = expressionExtractor.getAssignments(getExpression());
		for(Expression expression : assignments) {
			Assignment assignment = (Assignment)expression;
			Expression leftHandSide = assignment.getLeftHandSide();
			SimpleName leftHandSideName = processExpression(leftHandSide);
			if(leftHandSideName != null && leftHandSideName.equals(fio.getSimpleName())) {
				fieldAssignments.add(assignment);
			}
		}
		return fieldAssignments;
	}

	public List<PostfixExpression> getFieldPostfixAssignments(FieldInstructionObject fio) {
		List<PostfixExpression> fieldPostfixAssignments = new ArrayList<PostfixExpression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(getExpression());
		for(Expression expression : postfixExpressions) {
			PostfixExpression postfixExpression = (PostfixExpression)expression;
			Expression operand = postfixExpression.getOperand();
			SimpleName operandName = processExpression(operand);
			if(operandName != null && operandName.equals(fio.getSimpleName())) {
				fieldPostfixAssignments.add(postfixExpression);
			}
		}
		return fieldPostfixAssignments;
	}

	public List<PrefixExpression> getFieldPrefixAssignments(FieldInstructionObject fio) {
		List<PrefixExpression> fieldPrefixAssignments = new ArrayList<PrefixExpression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(getExpression());
		for(Expression expression : prefixExpressions) {
			PrefixExpression prefixExpression = (PrefixExpression)expression;
			Expression operand = prefixExpression.getOperand();
			PrefixExpression.Operator operator = prefixExpression.getOperator();
			SimpleName operandName = processExpression(operand);
			if(operandName != null && operandName.equals(fio.getSimpleName()) && (operator.equals(PrefixExpression.Operator.INCREMENT) ||
					operator.equals(PrefixExpression.Operator.DECREMENT))) {
				fieldPrefixAssignments.add(prefixExpression);
			}
		}
		return fieldPrefixAssignments;
	}

	private SimpleName processExpression(Expression expression) {
		SimpleName simpleName = null;
		if(expression instanceof SimpleName) {
			simpleName = (SimpleName)expression;
		}
		else if(expression instanceof QualifiedName) {
			QualifiedName leftHandSideQualifiedName = (QualifiedName)expression;
			simpleName = leftHandSideQualifiedName.getName();
		}
		else if(expression instanceof FieldAccess) {
			FieldAccess leftHandSideFieldAccess = (FieldAccess)expression;
			simpleName = leftHandSideFieldAccess.getName();
		}
		else if(expression instanceof ArrayAccess) {
			ArrayAccess leftHandSideArrayAccess = (ArrayAccess)expression;
			Expression array = leftHandSideArrayAccess.getArray();
			if(array instanceof SimpleName) {
				simpleName = (SimpleName)array;
			}
			else if(array instanceof QualifiedName) {
				QualifiedName arrayQualifiedName = (QualifiedName)array;
				simpleName = arrayQualifiedName.getName();
			}
			else if(array instanceof FieldAccess) {
				FieldAccess arrayFieldAccess = (FieldAccess)array;
				simpleName = arrayFieldAccess.getName();
			}
		}
		return simpleName;
	}
	
    public void setOwner(CompositeStatementObject owner) {
    	this.owner = owner;
    }

    public CompositeStatementObject getOwner() {
    	return this.owner;
    }

    public Expression getExpression() {
    	//return expression;
    	return (Expression)this.expression.recoverASTNode();
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

		if(o instanceof AbstractExpression) {
			AbstractExpression abstractExpression = (AbstractExpression)o;
			return this.expression.equals(abstractExpression.expression);
		}
		return false;
	}

	public int hashCode() {
		return expression.hashCode();
	}*/

	public String toString() {
		return expression.toString();
	}
}
