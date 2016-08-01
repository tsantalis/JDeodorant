package gr.uom.java.ast.util;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodDeclarationUtility {

	public static MethodInvocation isDelegate(MethodDeclaration methodDeclaration) {
		AbstractTypeDeclaration parentClass = (AbstractTypeDeclaration)methodDeclaration.getParent();
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
	    				List<MethodDeclaration> parentClassMethods = new ArrayList<MethodDeclaration>();
	    				if(parentClass instanceof TypeDeclaration) {
	    					MethodDeclaration[] parentClassMethodArray = ((TypeDeclaration)parentClass).getMethods();
	    					for(MethodDeclaration method : parentClassMethodArray) {
	    						parentClassMethods.add(method);
	    					}
	    				}
	    				else if(parentClass instanceof EnumDeclaration) {
	    					EnumDeclaration enumDeclaration = (EnumDeclaration)parentClass;
	    					List<BodyDeclaration> bodyDeclarations = enumDeclaration.bodyDeclarations();
	    					for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
	    						if(bodyDeclaration instanceof MethodDeclaration) {
	    							parentClassMethods.add((MethodDeclaration)bodyDeclaration);
	    						}
	    					}
	    				}
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
	    				if(binding != null && binding.getKind() == IBinding.VARIABLE) {
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
	    					if(rightHandSideSimpleName.resolveBinding().isEqualTo(parameters.get(0).resolveBinding())) {
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

	public static AbstractVariable createVariable(SimpleName simpleName, AbstractVariable rightPart) {
		IBinding binding = simpleName.resolveBinding();
		if(binding != null && binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding variableBinding = (IVariableBinding)binding;
			AbstractVariable currentVariable = null;
			if(rightPart == null)
				currentVariable = new PlainVariable(variableBinding);
			else
				currentVariable = new CompositeVariable(variableBinding, rightPart);
			
			if(simpleName.getParent() instanceof QualifiedName) {
				QualifiedName qualifiedName = (QualifiedName)simpleName.getParent();
				Name qualifier = qualifiedName.getQualifier();
				if(qualifier instanceof SimpleName) {
					SimpleName qualifierSimpleName = (SimpleName)qualifier;
					if(!qualifierSimpleName.equals(simpleName))
						return createVariable(qualifierSimpleName, currentVariable);
					else
						return currentVariable;
				}
				else if(qualifier instanceof QualifiedName) {
					QualifiedName qualifiedName2 = (QualifiedName)qualifier;
					return createVariable(qualifiedName2.getName(), currentVariable);
				}
			}
			else if(simpleName.getParent() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)simpleName.getParent();
				Expression fieldAccessExpression = fieldAccess.getExpression();
				if(fieldAccessExpression instanceof FieldAccess) {
					FieldAccess fieldAccess2 = (FieldAccess)fieldAccessExpression;
					return createVariable(fieldAccess2.getName(), currentVariable);
				}
				else if(fieldAccessExpression instanceof SimpleName) {
					SimpleName fieldAccessSimpleName = (SimpleName)fieldAccessExpression;
					return createVariable(fieldAccessSimpleName, currentVariable);
				}
				else if(fieldAccessExpression instanceof ThisExpression) {
					return currentVariable;
				}
			}
			else {
				return currentVariable;
			}
		}
		return null;
	}

	public static AbstractVariable processMethodInvocationExpression(Expression expression) {
		if(expression != null) {
			if(expression instanceof QualifiedName) {
				QualifiedName qualifiedName = (QualifiedName)expression;
				return createVariable(qualifiedName.getName(), null);
			}
			else if(expression instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)expression;
				return createVariable(fieldAccess.getName(), null);
			}
			else if(expression instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)expression;
				return createVariable(simpleName, null);
			}
		}
		return null;
	}


	public static SimpleName getRightMostSimpleName(Expression expression) {
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
}
