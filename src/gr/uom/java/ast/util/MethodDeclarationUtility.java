package gr.uom.java.ast.util;

import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodDeclarationUtility {

	public static MethodInvocation isDelegate(MethodDeclaration methodDeclaration) {
		TypeDeclaration parentClass = (TypeDeclaration)methodDeclaration.getParent();
		Block methodBody = methodDeclaration.getBody();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(statements.size() == 1) {
				Statement statement = statements.get(0);
				MethodInvocation methodInvocation = null;
	    		if(statement instanceof ReturnStatement) {
	    			ReturnStatement returnStatement = (ReturnStatement)statement;
	    			if(returnStatement.getExpression() instanceof MethodInvocation) {
	    				methodInvocation = (MethodInvocation)returnStatement.getExpression();
	    			}
	    		}
	    		else if(statement instanceof ExpressionStatement) {
	    			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
	    			if(expressionStatement.getExpression() instanceof MethodInvocation) {
	    				methodInvocation = (MethodInvocation)expressionStatement.getExpression();
	    			}
	    		}
	    		if(methodInvocation != null) {
	    			Expression methodInvocationExpression = methodInvocation.getExpression();
	    			if(methodInvocationExpression instanceof MethodInvocation) {
	    				MethodInvocation previousChainedMethodInvocation = (MethodInvocation)methodInvocationExpression;
	    				MethodDeclaration[] parentClassMethods = parentClass.getMethods();
	    				boolean isDelegationChain = false;
		    			boolean foundInParentClass = false;
	    				for(MethodDeclaration parentClassMethod : parentClassMethods) {
	    					if(parentClassMethod.resolveBinding().isEqualTo(previousChainedMethodInvocation.resolveMethodBinding())) {
	    						foundInParentClass = true;
	    						SimpleName getterField = isGetter(parentClassMethod);
	    						if(getterField == null)
	    							isDelegationChain = true;
	    						break;
	    					}
	    				}
	    				if(!isDelegationChain && foundInParentClass) {
	    					return methodInvocation;
	    				}
	    			}
	    			else if(methodInvocationExpression instanceof FieldAccess) {
	    				FieldAccess fieldAccess = (FieldAccess)methodInvocationExpression;
	    				IVariableBinding variableBinding = fieldAccess.resolveFieldBinding();
	    				if(variableBinding.getDeclaringClass().isEqualTo(parentClass.resolveBinding()) ||
	    						parentClass.resolveBinding().isSubTypeCompatible(variableBinding.getDeclaringClass())) {
	    					return methodInvocation;
	    				}
	    			}
	    			else if(methodInvocationExpression instanceof SimpleName) {
	    				SimpleName simpleName = (SimpleName)methodInvocationExpression;
	    				IBinding binding = simpleName.resolveBinding();
	    				if(binding.getKind() == IBinding.VARIABLE) {
	    					IVariableBinding variableBinding = (IVariableBinding)binding;
	    					if(variableBinding.isField() || variableBinding.isParameter()) {
	    						return methodInvocation;
	    					}
	    				}
	    			}
	    			else if(methodInvocationExpression instanceof ThisExpression) {
	    				return methodInvocation;
	    			}
	    			else if(methodInvocationExpression == null) {
	    				return methodInvocation;
	    			}
	    		}
			}
		}
		return null;
	}

	public static SimpleName isGetter(MethodDeclaration methodDeclaration) {
		Block methodBody = methodDeclaration.getBody();
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(statements.size() == 1 && parameters.size() == 0) {
				Statement statement = statements.get(0);
	    		if(statement instanceof ReturnStatement) {
	    			ReturnStatement returnStatement = (ReturnStatement)statement;
	    			Expression returnStatementExpression = returnStatement.getExpression();
	    			if(returnStatementExpression instanceof SimpleName) {
	    				return (SimpleName)returnStatementExpression;
	    			}
	    			else if(returnStatementExpression instanceof FieldAccess) {
	    				FieldAccess fieldAccess = (FieldAccess)returnStatementExpression;
	    				return fieldAccess.getName();
	    			}
	    		}
			}
		}
		return null;
	}

	public static SimpleName isSetter(MethodDeclaration methodDeclaration) {
		Block methodBody = methodDeclaration.getBody();
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(statements.size() == 1 && parameters.size() == 1) {
				Statement statement = statements.get(0);
	    		if(statement instanceof ExpressionStatement) {
	    			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
	    			Expression expressionStatementExpression = expressionStatement.getExpression();
	    			if(expressionStatementExpression instanceof Assignment) {
	    				Assignment assignment = (Assignment)expressionStatementExpression;
	    				Expression rightHandSide = assignment.getRightHandSide();
	    				if(rightHandSide instanceof SimpleName) {
	    					SimpleName rightHandSideSimpleName = (SimpleName)rightHandSide;
	    					if(rightHandSideSimpleName.getIdentifier().equals(parameters.get(0).getName().getIdentifier())) {
	    						Expression leftHandSide = assignment.getLeftHandSide();
	    						if(leftHandSide instanceof SimpleName) {
	    		    				return (SimpleName)leftHandSide;
	    		    			}
	    		    			else if(leftHandSide instanceof FieldAccess) {
	    		    				FieldAccess fieldAccess = (FieldAccess)leftHandSide;
	    		    				return fieldAccess.getName();
	    		    			}
	    					}
	    				}
	    			}
	    		}
			}
		}
		return null;
	}
}
