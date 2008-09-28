package gr.uom.java.ast.decomposition;

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
import org.eclipse.jdt.core.dom.Assignment;
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
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class AbstractExpression {
	
	private Expression expression;
	private CompositeStatementObject owner;
	private List<MethodInvocationObject> methodInvocationList;
	private List<SuperMethodInvocationObject> superMethodInvocationList;
    private List<FieldInstructionObject> fieldInstructionList;
    private List<LocalVariableDeclarationObject> localVariableDeclarationList;
    private List<LocalVariableInstructionObject> localVariableInstructionList;
    
    public AbstractExpression(Expression expression) {
    	this.expression = expression;
    	this.owner = null;
    	this.methodInvocationList = new ArrayList<MethodInvocationObject>();
    	this.superMethodInvocationList = new ArrayList<SuperMethodInvocationObject>();
        this.fieldInstructionList = new ArrayList<FieldInstructionObject>();
        this.localVariableDeclarationList = new ArrayList<LocalVariableDeclarationObject>();
        this.localVariableInstructionList = new ArrayList<LocalVariableInstructionObject>();
        
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
    }

	public List<Assignment> getLocalVariableAssignments(LocalVariableInstructionObject lvio) {
		List<Assignment> localVariableAssignments = new ArrayList<Assignment>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> assignments = expressionExtractor.getAssignments(expression);
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
		List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(expression);
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
		List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(expression);
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
		List<Expression> assignments = expressionExtractor.getAssignments(expression);
		for(Expression expression : assignments) {
			Assignment assignment = (Assignment)expression;
			Expression leftHandSide = assignment.getLeftHandSide();
			SimpleName leftHandSideName = processExpression(leftHandSide);
			if(leftHandSideName != null) {
				IBinding leftHandSideBinding = leftHandSideName.resolveBinding();
				if(leftHandSideBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding leftHandSideVariableBinding = (IVariableBinding)leftHandSideBinding;
					if(leftHandSideVariableBinding.isEqualTo(fio.getSimpleName().resolveBinding()))
						fieldAssignments.add(assignment);
				}
			}
		}
		return fieldAssignments;
	}

	public List<PostfixExpression> getFieldPostfixAssignments(FieldInstructionObject fio) {
		List<PostfixExpression> fieldPostfixAssignments = new ArrayList<PostfixExpression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(expression);
		for(Expression expression : postfixExpressions) {
			PostfixExpression postfixExpression = (PostfixExpression)expression;
			Expression operand = postfixExpression.getOperand();
			SimpleName operandName = processExpression(operand);
			if(operandName != null) {
				IBinding operandBinding = operandName.resolveBinding();
				if(operandBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
					if(operandVariableBinding.isEqualTo(fio.getSimpleName().resolveBinding()))
						fieldPostfixAssignments.add(postfixExpression);
				}
			}
		}
		return fieldPostfixAssignments;
	}

	public List<PrefixExpression> getFieldPrefixAssignments(FieldInstructionObject fio) {
		List<PrefixExpression> fieldPrefixAssignments = new ArrayList<PrefixExpression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(expression);
		for(Expression expression : prefixExpressions) {
			PrefixExpression prefixExpression = (PrefixExpression)expression;
			Expression operand = prefixExpression.getOperand();
			PrefixExpression.Operator operator = prefixExpression.getOperator();
			SimpleName operandName = processExpression(operand);
			if(operandName != null && (operator.equals(PrefixExpression.Operator.INCREMENT) ||
					operator.equals(PrefixExpression.Operator.DECREMENT))) {
				IBinding operandBinding = operandName.resolveBinding();
				if(operandBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
					if(operandVariableBinding.isEqualTo(fio.getSimpleName().resolveBinding()))
						fieldPrefixAssignments.add(prefixExpression);
				}
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
    	return expression;
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

	public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
		return methodInvocationList.contains(methodInvocation);
	}

	public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
		return superMethodInvocationList.contains(superMethodInvocation);
	}

	public boolean containsLocalVariableDeclaration(LocalVariableDeclarationObject lvdo) {
		return localVariableDeclarationList.contains(lvdo);
	}

	public String toString() {
		if(expression != null)
			return expression.toString();
		else
			return null;
	}
}
