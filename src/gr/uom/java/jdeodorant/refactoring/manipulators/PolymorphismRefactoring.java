package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public abstract class PolymorphismRefactoring extends Refactoring {
	protected IFile sourceFile;
	protected CompilationUnit sourceCompilationUnit;
	protected TypeDeclaration sourceTypeDeclaration;
	protected TypeCheckElimination typeCheckElimination;
	protected Map<ICompilationUnit, CompilationUnitChange> compilationUnitChanges;
	protected Set<IJavaElement> javaElementsToOpenInEditor;
	private Set<FieldDeclaration> fieldDeclarationsChangedWithPublicModifier;

	public PolymorphismRefactoring(IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeCheckElimination typeCheckElimination) {
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.typeCheckElimination = typeCheckElimination;
		this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
		CompilationUnitChange sourceCompilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		sourceCompilationUnitChange.setEdit(sourceMultiTextEdit);
		compilationUnitChanges.put(sourceICompilationUnit, sourceCompilationUnitChange);
		this.javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
		this.fieldDeclarationsChangedWithPublicModifier = new LinkedHashSet<FieldDeclaration>();
	}

	public Set<IJavaElement> getJavaElementsToOpenInEditor() {
		return javaElementsToOpenInEditor;
	}

	protected void modifySourceMethodInvocationsInSubclass(List<Expression> oldMethodInvocations, List<Expression> newMethodInvocations, AST subclassAST,
			ASTRewrite subclassRewriter, Set<MethodDeclaration> accessedMethods, Set<IMethodBinding> superAccessedMethods) {
		int j = 0;
		for(Expression expression : newMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation newMethodInvocation = (MethodInvocation)expression;
				MethodInvocation oldMethodInvocation = (MethodInvocation)oldMethodInvocations.get(j);
				for(MethodDeclaration methodDeclaration : accessedMethods) {
					if(oldMethodInvocation.resolveMethodBinding().isEqualTo(methodDeclaration.resolveBinding())) {
						String invokerName = sourceTypeDeclaration.getName().getIdentifier();
						if((methodDeclaration.resolveBinding().getModifiers() & Modifier.STATIC) == 0)
							invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
						subclassRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
						break;
					}
				}
				for(IMethodBinding superMethodBinding : superAccessedMethods) {
					if(superMethodBinding.isEqualTo(oldMethodInvocation.resolveMethodBinding())) {
						if(oldMethodInvocation.getExpression() == null ||
								(oldMethodInvocation.getExpression() != null && oldMethodInvocation.getExpression() instanceof ThisExpression)) {
							String invokerName = sourceTypeDeclaration.getName().getIdentifier();
							if((superMethodBinding.getModifiers() & Modifier.STATIC) == 0)
								invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
							subclassRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							break;
						}
					}
				}
			}
			j++;
		}
	}

	protected void replaceThisExpressionWithContextParameterInMethodInvocationArguments(List<Expression> newMethodInvocations, AST subclassAST, ASTRewrite subclassRewriter) {
		for(Expression expression : newMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation newMethodInvocation = (MethodInvocation)expression;
				List<Expression> arguments = newMethodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument instanceof ThisExpression) {
						String parameterName = sourceTypeDeclaration.getName().getIdentifier();
						parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
						ListRewrite argumentsRewrite = subclassRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
						argumentsRewrite.replace(argument, subclassAST.newSimpleName(parameterName), null);
					}
				}
			}
		}
	}

	protected void replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(Statement newStatement, AST subclassAST, ASTRewrite subclassRewriter) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(newStatement);
		for(Expression creation : classInstanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)creation;
			List<Expression> arguments = classInstanceCreation.arguments();
			for(Expression argument : arguments) {
				if(argument instanceof ThisExpression) {
					String parameterName = sourceTypeDeclaration.getName().getIdentifier();
					parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
					ListRewrite argumentsRewrite = subclassRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
					argumentsRewrite.replace(argument, subclassAST.newSimpleName(parameterName), null);
				}
			}
		}
	}

	protected void modifySourceVariableInstructionsInSubclass(List<Expression> oldVariableInstructions, List<Expression> newVariableInstructions, AST subclassAST, ASTRewrite subclassRewriter,
			Set<VariableDeclarationFragment> accessedFields, Set<VariableDeclarationFragment> assignedFields, Set<IVariableBinding> superAccessedFields, Set<IVariableBinding> superAssignedFields) {
		int j = 0;
		Set<IVariableBinding> accessedFieldBindings = new LinkedHashSet<IVariableBinding>();
		for(VariableDeclarationFragment fragment : accessedFields) {
			accessedFieldBindings.add(fragment.resolveBinding());
		}
		accessedFieldBindings.addAll(superAccessedFields);
		Set<IVariableBinding> assignedFieldBindings = new LinkedHashSet<IVariableBinding>();
		for(VariableDeclarationFragment fragment : assignedFields) {
			assignedFieldBindings.add(fragment.resolveBinding());
		}
		assignedFieldBindings.addAll(superAssignedFields);
		
		for(Expression expression : newVariableInstructions) {
			SimpleName newSimpleName = (SimpleName)expression;
			SimpleName oldSimpleName = (SimpleName)oldVariableInstructions.get(j);
			Expression newParentExpression = null;
			Expression oldParentExpression = null;
			if(newSimpleName.getParent() instanceof QualifiedName) {
				newParentExpression = (QualifiedName)newSimpleName.getParent();
				oldParentExpression = (QualifiedName)oldSimpleName.getParent();
			}
			else if(newSimpleName.getParent() instanceof FieldAccess) {
				newParentExpression = (FieldAccess)newSimpleName.getParent();
				oldParentExpression = (FieldAccess)oldSimpleName.getParent();
			}
			else {
				newParentExpression = newSimpleName;
				oldParentExpression = oldSimpleName;
			}
			if(newParentExpression.getParent() instanceof Assignment) {
				Assignment newAssignment = (Assignment)newParentExpression.getParent();
				Assignment oldAssignment = (Assignment)oldParentExpression.getParent();
				Expression newLeftHandSide = newAssignment.getLeftHandSide();
				Expression oldLeftHandSide = oldAssignment.getLeftHandSide();
				SimpleName newLeftHandSideName = null;
				SimpleName oldLeftHandSideName = null;
				if(newLeftHandSide instanceof SimpleName) {
					newLeftHandSideName = (SimpleName)newLeftHandSide;
					oldLeftHandSideName = (SimpleName)oldLeftHandSide;
				}
				else if(newLeftHandSide instanceof QualifiedName) {
					QualifiedName newLeftHandSideQualifiedName = (QualifiedName)newLeftHandSide;
					newLeftHandSideName = newLeftHandSideQualifiedName.getName();
					QualifiedName oldLeftHandSideQualifiedName = (QualifiedName)oldLeftHandSide;
					oldLeftHandSideName = oldLeftHandSideQualifiedName.getName();
				}
				else if(newLeftHandSide instanceof FieldAccess) {
					FieldAccess newLeftHandSideFieldAccess = (FieldAccess)newLeftHandSide;
					newLeftHandSideName = newLeftHandSideFieldAccess.getName();
					FieldAccess oldLeftHandSideFieldAccess = (FieldAccess)oldLeftHandSide;
					oldLeftHandSideName = oldLeftHandSideFieldAccess.getName();
				}
				Expression newRightHandSide = newAssignment.getRightHandSide();
				Expression oldRightHandSide = oldAssignment.getRightHandSide();
				SimpleName newRightHandSideName = null;
				SimpleName oldRightHandSideName = null;
				if(newRightHandSide instanceof SimpleName) {
					newRightHandSideName = (SimpleName)newRightHandSide;
					oldRightHandSideName = (SimpleName)oldRightHandSide;
				}
				else if(newRightHandSide instanceof QualifiedName) {
					QualifiedName newRightHandSideQualifiedName = (QualifiedName)newRightHandSide;
					newRightHandSideName = newRightHandSideQualifiedName.getName();
					QualifiedName oldRightHandSideQualifiedName = (QualifiedName)oldRightHandSide;
					oldRightHandSideName = oldRightHandSideQualifiedName.getName();
				}
				else if(newRightHandSide instanceof FieldAccess) {
					FieldAccess newRightHandSideFieldAccess = (FieldAccess)newRightHandSide;
					newRightHandSideName = newRightHandSideFieldAccess.getName();
					FieldAccess oldRightHandSideFieldAccess = (FieldAccess)oldRightHandSide;
					oldRightHandSideName = oldRightHandSideFieldAccess.getName();
				}
				String invokerName = sourceTypeDeclaration.getName().getIdentifier();
				invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
				if(newLeftHandSideName != null && newLeftHandSideName.equals(newSimpleName)) {
					for(IVariableBinding assignedFieldBinding : assignedFieldBindings) {
						if(assignedFieldBinding.isEqualTo(oldLeftHandSideName.resolveBinding())) {
							IMethodBinding setterMethodBinding = null;
							if(superAssignedFields.contains(assignedFieldBinding)) {
								setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(assignedFieldBinding);
							}
							else {
								setterMethodBinding = findSetterMethodInContext(assignedFieldBinding);
							}
							String leftHandMethodName;
							if(setterMethodBinding != null) {
								leftHandMethodName = setterMethodBinding.getName();
							}
							else {
								leftHandMethodName = assignedFieldBinding.getName();
								leftHandMethodName = "set" + leftHandMethodName.substring(0,1).toUpperCase() + leftHandMethodName.substring(1,leftHandMethodName.length());
							}
							MethodInvocation leftHandMethodInvocation = subclassAST.newMethodInvocation();
							subclassRewriter.set(leftHandMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(leftHandMethodName), null);
							subclassRewriter.set(leftHandMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							InfixExpression infixArgument = null;
							if(!newAssignment.getOperator().equals(Assignment.Operator.ASSIGN)) {
								IMethodBinding getterMethodBinding = null;
								if(superAccessedFields.contains(assignedFieldBinding)) {
									getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(assignedFieldBinding);
								}
								else {
									getterMethodBinding = findGetterMethodInContext(assignedFieldBinding);
								}
								String getterMethodName;
								if(getterMethodBinding != null) {
									getterMethodName = getterMethodBinding.getName();
								}
								else {
									getterMethodName = assignedFieldBinding.getName();
									getterMethodName = "get" + getterMethodName.substring(0,1).toUpperCase() + getterMethodName.substring(1,getterMethodName.length());
								}
								MethodInvocation getterMethodInvocation = subclassAST.newMethodInvocation();
								subclassRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(getterMethodName), null);
								subclassRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
								
								infixArgument = subclassAST.newInfixExpression();
								subclassRewriter.set(infixArgument, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
								if(newAssignment.getOperator().equals(Assignment.Operator.PLUS_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.MINUS_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.TIMES_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.TIMES, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.DIVIDE_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.DIVIDE, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.REMAINDER_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.REMAINDER, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_AND_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.AND, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_OR_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.OR, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.XOR, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.LEFT_SHIFT, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_SIGNED, null);
								}
								else if(newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)) {
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, null);
								}
							}
							ListRewrite methodInvocationArgumentsRewrite = subclassRewriter.getListRewrite(leftHandMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							if(newRightHandSideName != null) {
								boolean accessedFieldFound = false;
								for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
									if(accessedFieldBinding.isEqualTo(oldRightHandSideName.resolveBinding())) {
										if((accessedFieldBinding.getModifiers() & Modifier.STATIC) != 0) {
											SimpleName qualifier = subclassAST.newSimpleName(accessedFieldBinding.getDeclaringClass().getName());
											if(newRightHandSideName.getParent() instanceof FieldAccess) {
												FieldAccess fieldAccess = (FieldAccess)newRightHandSideName.getParent();
												subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
												if(infixArgument != null) {
													subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, fieldAccess, null);
													methodInvocationArgumentsRewrite.insertLast(infixArgument, null);
												}
												else
													methodInvocationArgumentsRewrite.insertLast(fieldAccess, null);
											}
											else if(newRightHandSideName.getParent() instanceof QualifiedName) {
												QualifiedName qualifiedName = (QualifiedName)newRightHandSideName.getParent();
												if(infixArgument != null) {
													subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, qualifiedName, null);
													methodInvocationArgumentsRewrite.insertLast(infixArgument, null);
												}
												else
													methodInvocationArgumentsRewrite.insertLast(qualifiedName, null);
											}
											else {
												SimpleName simpleName = subclassAST.newSimpleName(newRightHandSideName.getIdentifier());
												QualifiedName newQualifiedName = subclassAST.newQualifiedName(qualifier, simpleName);
												subclassRewriter.replace(newRightHandSideName, newQualifiedName, null);
												if(infixArgument != null) {
													subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, newQualifiedName, null);
													methodInvocationArgumentsRewrite.insertLast(infixArgument, null);
												}
												else
													methodInvocationArgumentsRewrite.insertLast(newQualifiedName, null);
											}
											if(accessedFieldBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
												setPublicModifierToSourceField(accessedFieldBinding);
											}
										}
										else {
											IMethodBinding getterMethodBinding = null;
											if(superAccessedFields.contains(accessedFieldBinding)) {
												getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(accessedFieldBinding);
											}
											else {
												getterMethodBinding = findGetterMethodInContext(accessedFieldBinding);
											}
											String rightHandMethodName;
											if(getterMethodBinding != null) {
												rightHandMethodName = getterMethodBinding.getName();
											}
											else {
												rightHandMethodName = accessedFieldBinding.getName();
												rightHandMethodName = "get" + rightHandMethodName.substring(0,1).toUpperCase() + rightHandMethodName.substring(1,rightHandMethodName.length());
											}
											MethodInvocation rightHandMethodInvocation = subclassAST.newMethodInvocation();
											subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(rightHandMethodName), null);
											subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
											if(infixArgument != null) {
												subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, rightHandMethodInvocation, null);
												methodInvocationArgumentsRewrite.insertLast(infixArgument, null);
											}
											else
												methodInvocationArgumentsRewrite.insertLast(rightHandMethodInvocation, null);
										}
										accessedFieldFound = true;
										break;
									}
								}
								if(!accessedFieldFound) {
									if(infixArgument != null) {
										subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, newAssignment.getRightHandSide(), null);
										methodInvocationArgumentsRewrite.insertLast(infixArgument, null);
									}
									else
										methodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
								}
							}
							else {
								if(infixArgument != null) {
									subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, newAssignment.getRightHandSide(), null);
									methodInvocationArgumentsRewrite.insertLast(infixArgument, null);
								}
								else
									methodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
							}
							subclassRewriter.replace(newAssignment, leftHandMethodInvocation, null);
							break;
						}
					}
				}
				if(newRightHandSideName != null && newRightHandSideName.equals(newSimpleName)) {
					for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
						if(accessedFieldBinding.isEqualTo(oldRightHandSideName.resolveBinding())) {
							if((accessedFieldBinding.getModifiers() & Modifier.STATIC) != 0) {
								SimpleName qualifier = subclassAST.newSimpleName(accessedFieldBinding.getDeclaringClass().getName());
								if(newSimpleName.getParent() instanceof FieldAccess) {
									FieldAccess fieldAccess = (FieldAccess)newSimpleName.getParent();
									subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
								}
								else if(!(newSimpleName.getParent() instanceof QualifiedName)) {
									SimpleName simpleName = subclassAST.newSimpleName(newSimpleName.getIdentifier());
									QualifiedName newQualifiedName = subclassAST.newQualifiedName(qualifier, simpleName);
									subclassRewriter.replace(newSimpleName, newQualifiedName, null);
								}
								if(accessedFieldBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
									setPublicModifierToSourceField(accessedFieldBinding);
								}
							}
							else {
								IMethodBinding getterMethodBinding = null;
								if(superAccessedFields.contains(accessedFieldBinding)) {
									getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(accessedFieldBinding);
								}
								else {
									getterMethodBinding = findGetterMethodInContext(accessedFieldBinding);
								}
								String rightHandMethodName;
								if(getterMethodBinding != null) {
									rightHandMethodName = getterMethodBinding.getName();
								}
								else {
									rightHandMethodName = accessedFieldBinding.getName();
									rightHandMethodName = "get" + rightHandMethodName.substring(0,1).toUpperCase() + rightHandMethodName.substring(1,rightHandMethodName.length());
								}
								MethodInvocation rightHandMethodInvocation = subclassAST.newMethodInvocation();
								subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(rightHandMethodName), null);
								subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
								subclassRewriter.set(newAssignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, rightHandMethodInvocation, null);
							}
							break;
						}
					}
				}
			}
			else if(newParentExpression.getParent() instanceof PostfixExpression) {
				PostfixExpression newPostfixExpression = (PostfixExpression)newParentExpression.getParent();
				PostfixExpression oldPostfixExpression = (PostfixExpression)oldParentExpression.getParent();
				Expression newOperand = newPostfixExpression.getOperand();
				Expression oldOperand = oldPostfixExpression.getOperand();
				SimpleName newOperandSimpleName = null;
				SimpleName oldOperandSimpleName = null;
				if(newOperand instanceof SimpleName) {
					newOperandSimpleName = (SimpleName)newOperand;
					oldOperandSimpleName = (SimpleName)oldOperand;
				}
				else if(newOperand instanceof QualifiedName) {
					QualifiedName newOperandQualifiedName = (QualifiedName)newOperand;
					newOperandSimpleName = newOperandQualifiedName.getName();
					QualifiedName oldOperandQualifiedName = (QualifiedName)oldOperand;
					oldOperandSimpleName = oldOperandQualifiedName.getName();
				}
				else if(newOperand instanceof FieldAccess) {
					FieldAccess newOperandFieldAccess = (FieldAccess)newOperand;
					newOperandSimpleName = newOperandFieldAccess.getName();
					FieldAccess oldOperandFieldAccess = (FieldAccess)oldOperand;
					oldOperandSimpleName = oldOperandFieldAccess.getName();
				}
				String invokerName = sourceTypeDeclaration.getName().getIdentifier();
				invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
				if(newOperandSimpleName != null && newOperandSimpleName.equals(newSimpleName)) {
					for(IVariableBinding assignedFieldBinding : assignedFieldBindings) {
						if(assignedFieldBinding.isEqualTo(oldOperandSimpleName.resolveBinding())) {
							IMethodBinding setterMethodBinding = null;
							if(superAssignedFields.contains(assignedFieldBinding)) {
								setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(assignedFieldBinding);
							}
							else {
								setterMethodBinding = findSetterMethodInContext(assignedFieldBinding);
							}
							String setterMethodName;
							if(setterMethodBinding != null) {
								setterMethodName = setterMethodBinding.getName();
							}
							else {
								setterMethodName = assignedFieldBinding.getName();
								setterMethodName = "set" + setterMethodName.substring(0,1).toUpperCase() + setterMethodName.substring(1,setterMethodName.length());
							}
							MethodInvocation setterMethodInvocation = subclassAST.newMethodInvocation();
							subclassRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(setterMethodName), null);
							subclassRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							
							IMethodBinding getterMethodBinding = null;
							if(superAccessedFields.contains(assignedFieldBinding)) {
								getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(assignedFieldBinding);
							}
							else {
								getterMethodBinding = findGetterMethodInContext(assignedFieldBinding);
							}
							String getterMethodName;
							if(getterMethodBinding != null) {
								getterMethodName = getterMethodBinding.getName();
							}
							else {
								getterMethodName = assignedFieldBinding.getName();
								getterMethodName = "get" + getterMethodName.substring(0,1).toUpperCase() + getterMethodName.substring(1,getterMethodName.length());
							}
							MethodInvocation getterMethodInvocation = subclassAST.newMethodInvocation();
							subclassRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(getterMethodName), null);
							subclassRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							
							InfixExpression infixArgument = subclassAST.newInfixExpression();
							subclassRewriter.set(infixArgument, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
							if(newPostfixExpression.getOperator().equals(PostfixExpression.Operator.INCREMENT))
								subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
							else if(newPostfixExpression.getOperator().equals(PostfixExpression.Operator.DECREMENT))
								subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
							subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, subclassAST.newNumberLiteral("1"), null);
							ListRewrite setterMethodInvocationArgumentsRewrite = subclassRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							setterMethodInvocationArgumentsRewrite.insertLast(infixArgument, null);
							subclassRewriter.replace(newPostfixExpression, setterMethodInvocation, null);
						}
					}
				}
			}
			else if(newParentExpression.getParent() instanceof PrefixExpression) {
				PrefixExpression newPrefixExpression = (PrefixExpression)newParentExpression.getParent();
				PrefixExpression oldPrefixExpression = (PrefixExpression)oldParentExpression.getParent();
				Expression newOperand = newPrefixExpression.getOperand();
				Expression oldOperand = oldPrefixExpression.getOperand();
				SimpleName newOperandSimpleName = null;
				SimpleName oldOperandSimpleName = null;
				if(newOperand instanceof SimpleName) {
					newOperandSimpleName = (SimpleName)newOperand;
					oldOperandSimpleName = (SimpleName)oldOperand;
				}
				else if(newOperand instanceof QualifiedName) {
					QualifiedName newOperandQualifiedName = (QualifiedName)newOperand;
					newOperandSimpleName = newOperandQualifiedName.getName();
					QualifiedName oldOperandQualifiedName = (QualifiedName)oldOperand;
					oldOperandSimpleName = oldOperandQualifiedName.getName();
				}
				else if(newOperand instanceof FieldAccess) {
					FieldAccess newOperandFieldAccess = (FieldAccess)newOperand;
					newOperandSimpleName = newOperandFieldAccess.getName();
					FieldAccess oldOperandFieldAccess = (FieldAccess)oldOperand;
					oldOperandSimpleName = oldOperandFieldAccess.getName();
				}
				String invokerName = sourceTypeDeclaration.getName().getIdentifier();
				invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
				if(newPrefixExpression.getOperator().equals(PrefixExpression.Operator.INCREMENT) ||
						newPrefixExpression.getOperator().equals(PrefixExpression.Operator.DECREMENT)) {
					if(newOperandSimpleName != null && newOperandSimpleName.equals(newSimpleName)) {
						for(IVariableBinding assignedFieldBinding : assignedFieldBindings) {
							if(assignedFieldBinding.isEqualTo(oldOperandSimpleName.resolveBinding())) {
								IMethodBinding setterMethodBinding = null;
								if(superAssignedFields.contains(assignedFieldBinding)) {
									setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(assignedFieldBinding);
								}
								else {
									setterMethodBinding = findSetterMethodInContext(assignedFieldBinding);
								}
								String setterMethodName;
								if(setterMethodBinding != null) {
									setterMethodName = setterMethodBinding.getName();
								}
								else {
									setterMethodName = assignedFieldBinding.getName();
									setterMethodName = "set" + setterMethodName.substring(0,1).toUpperCase() + setterMethodName.substring(1,setterMethodName.length());
								}
								MethodInvocation setterMethodInvocation = subclassAST.newMethodInvocation();
								subclassRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(setterMethodName), null);
								subclassRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
								
								IMethodBinding getterMethodBinding = null;
								if(superAccessedFields.contains(assignedFieldBinding)) {
									getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(assignedFieldBinding);
								}
								else {
									getterMethodBinding = findGetterMethodInContext(assignedFieldBinding);
								}
								String getterMethodName;
								if(getterMethodBinding != null) {
									getterMethodName = getterMethodBinding.getName();
								}
								else {
									getterMethodName = assignedFieldBinding.getName();
									getterMethodName = "get" + getterMethodName.substring(0,1).toUpperCase() + getterMethodName.substring(1,getterMethodName.length());
								}
								MethodInvocation getterMethodInvocation = subclassAST.newMethodInvocation();
								subclassRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(getterMethodName), null);
								subclassRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
								
								InfixExpression infixArgument = subclassAST.newInfixExpression();
								subclassRewriter.set(infixArgument, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
								if(newPrefixExpression.getOperator().equals(PrefixExpression.Operator.INCREMENT))
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
								else if(newPrefixExpression.getOperator().equals(PrefixExpression.Operator.DECREMENT))
									subclassRewriter.set(infixArgument, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
								subclassRewriter.set(infixArgument, InfixExpression.RIGHT_OPERAND_PROPERTY, subclassAST.newNumberLiteral("1"), null);
								ListRewrite setterMethodInvocationArgumentsRewrite = subclassRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
								setterMethodInvocationArgumentsRewrite.insertLast(infixArgument, null);
								subclassRewriter.replace(newPrefixExpression, setterMethodInvocation, null);
							}
						}
					}
				}
				else {
					if(newOperandSimpleName != null && newOperandSimpleName.equals(newSimpleName)) {
						for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
							if(accessedFieldBinding.isEqualTo(oldOperandSimpleName.resolveBinding())) {
								if((accessedFieldBinding.getModifiers() & Modifier.STATIC) != 0) {
									SimpleName qualifier = subclassAST.newSimpleName(accessedFieldBinding.getDeclaringClass().getName());
									if(newOperandSimpleName.getParent() instanceof FieldAccess) {
										FieldAccess fieldAccess = (FieldAccess)newOperandSimpleName.getParent();
										subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
									}
									else if(!(newOperandSimpleName.getParent() instanceof QualifiedName)) {
										SimpleName simpleName = subclassAST.newSimpleName(newOperandSimpleName.getIdentifier());
										QualifiedName newQualifiedName = subclassAST.newQualifiedName(qualifier, simpleName);
										subclassRewriter.replace(newOperandSimpleName, newQualifiedName, null);
									}
									if(accessedFieldBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
										setPublicModifierToSourceField(accessedFieldBinding);
									}
								}
								else {
									IMethodBinding getterMethodBinding = null;
									if(superAccessedFields.contains(accessedFieldBinding)) {
										getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(accessedFieldBinding);
									}
									else {
										getterMethodBinding = findGetterMethodInContext(accessedFieldBinding);
									}
									String methodName;
									if(getterMethodBinding != null) {
										methodName = getterMethodBinding.getName();
									}
									else {
										methodName = accessedFieldBinding.getName();
										methodName = "get" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
									}
									MethodInvocation methodInvocation = subclassAST.newMethodInvocation();
									subclassRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(methodName), null);
									subclassRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
									if(newOperandSimpleName.getParent() instanceof FieldAccess) {
										FieldAccess fieldAccess = (FieldAccess)newOperandSimpleName.getParent();
										subclassRewriter.replace(fieldAccess, methodInvocation, null);
									}
									else if(newOperandSimpleName.getParent() instanceof QualifiedName) {
										QualifiedName qualifiedName = (QualifiedName)newOperandSimpleName.getParent();
										subclassRewriter.replace(qualifiedName, methodInvocation, null);
									}
									else {
										subclassRewriter.replace(newOperandSimpleName, methodInvocation, null);
									}
								}
							}
						}
					}
				}
			}
			else {
				for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
					if(accessedFieldBinding.isEqualTo(oldSimpleName.resolveBinding())) {
						if((accessedFieldBinding.getModifiers() & Modifier.STATIC) != 0) {
							SimpleName qualifier = subclassAST.newSimpleName(accessedFieldBinding.getDeclaringClass().getName());
							if(newSimpleName.getParent() instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)newSimpleName.getParent();
								subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
							}
							else if(!(newSimpleName.getParent() instanceof QualifiedName)) {
								SimpleName simpleName = subclassAST.newSimpleName(newSimpleName.getIdentifier());
								QualifiedName newQualifiedName = subclassAST.newQualifiedName(qualifier, simpleName);
								subclassRewriter.replace(newSimpleName, newQualifiedName, null);
							}
							if(accessedFieldBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								setPublicModifierToSourceField(accessedFieldBinding);
							}
						}
						else {
							IMethodBinding getterMethodBinding = null;
							if(superAccessedFields.contains(accessedFieldBinding)) {
								getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(accessedFieldBinding);
							}
							else {
								getterMethodBinding = findGetterMethodInContext(accessedFieldBinding);
							}
							String methodName;
							if(getterMethodBinding != null) {
								methodName = getterMethodBinding.getName();
							}
							else {
								methodName = accessedFieldBinding.getName();
								methodName = "get" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
							}
							MethodInvocation methodInvocation = subclassAST.newMethodInvocation();
							subclassRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(methodName), null);
							String invokerName = sourceTypeDeclaration.getName().getIdentifier();
							invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
							subclassRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							if(newSimpleName.getParent() instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)newSimpleName.getParent();
								subclassRewriter.replace(fieldAccess, methodInvocation, null);
							}
							else if(newSimpleName.getParent() instanceof QualifiedName) {
								QualifiedName qualifiedName = (QualifiedName)newSimpleName.getParent();
								subclassRewriter.replace(qualifiedName, methodInvocation, null);
							}
							else {
								subclassRewriter.replace(newSimpleName, methodInvocation, null);
							}
						}
						break;
					}
				}
			}
			j++;
		}
	}

	private void setPublicModifierToSourceField(IVariableBinding variableBinding) {
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				boolean modifierIsReplaced = false;
				if(variableBinding.isEqualTo(fragment.resolveBinding())) {
					ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
					ListRewrite modifierRewrite = sourceRewriter.getListRewrite(fieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
					Modifier publicModifier = fieldDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
					boolean modifierFound = false;
					List<IExtendedModifier> modifiers = fieldDeclaration.modifiers();
					for(IExtendedModifier extendedModifier : modifiers) {
						if(extendedModifier.isModifier()) {
							Modifier modifier = (Modifier)extendedModifier;
							if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
								modifierFound = true;
							}
							else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD)) {
								if(!fieldDeclarationsChangedWithPublicModifier.contains(fieldDeclaration)) {
									fieldDeclarationsChangedWithPublicModifier.add(fieldDeclaration);
									modifierFound = true;
									modifierRewrite.replace(modifier, publicModifier, null);
									modifierIsReplaced = true;
									try {
										TextEdit sourceEdit = sourceRewriter.rewriteAST();
										ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
										CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
										change.getEdit().addChild(sourceEdit);
										change.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
									} catch (JavaModelException e) {
										e.printStackTrace();
									}
								}
							}
							else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
								modifierFound = true;
							}
						}
					}
					if(!modifierFound) {
						if(!fieldDeclarationsChangedWithPublicModifier.contains(fieldDeclaration)) {
							fieldDeclarationsChangedWithPublicModifier.add(fieldDeclaration);
							modifierRewrite.insertFirst(publicModifier, null);
							modifierIsReplaced = true;
							try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
					}
				}
				if(modifierIsReplaced)
					break;
			}
		}
	}

	protected IMethodBinding findSetterMethodInContext(IVariableBinding fieldBinding) {
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			SimpleName simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
			if(simpleName != null && simpleName.resolveBinding().isEqualTo(fieldBinding)) {
				return methodDeclaration.resolveBinding();
			}
		}
		return null;
	}

	protected IMethodBinding findGetterMethodInContext(IVariableBinding fieldBinding) {
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			SimpleName simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
			if(simpleName != null && simpleName.resolveBinding().isEqualTo(fieldBinding)) {
				return methodDeclaration.resolveBinding();
			}
		}
		return null;
	}

	protected IFile getFile(IContainer rootContainer, String fullyQualifiedClassName) {
		String[] subPackages = fullyQualifiedClassName.split("\\.");
		IContainer classContainer = rootContainer;
		IFile classFile = null;
		for(int i = 0; i<subPackages.length; i++) {
			try {
				if(i == subPackages.length-1) {
					IResource[] resources = classContainer.members();
					for(IResource resource : resources) {
						if(resource instanceof IFile) {
							IFile file = (IFile)resource;
							if(file.getName().equals(subPackages[i] + ".java")) {
								classFile = file;
								break;
							}
						}
					}
				}
				else {
					IResource[] resources = classContainer.members();
					for(IResource resource : resources) {
						if(resource instanceof IFolder) {
							IContainer container = (IContainer)resource;
							if(container.getName().equals(subPackages[i])) {
								classContainer = container;
								break;
							}
						}
						else if(resource instanceof IFile) {
							IFile file = (IFile)resource;
							if(file.getName().equals(subPackages[i] + ".java")) {
								classFile = file;
								break;
							}
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return classFile;
	}

	protected void generateGettersForAccessedFields() {
		AST contextAST = sourceTypeDeclaration.getAST();
		Set<VariableDeclarationFragment> accessedFields = new LinkedHashSet<VariableDeclarationFragment>();
		accessedFields.addAll(typeCheckElimination.getAccessedFields());
		accessedFields.addAll(typeCheckElimination.getSuperAccessedFields());
		for(VariableDeclarationFragment fragment : accessedFields) {
			if((fragment.resolveBinding().getModifiers() & Modifier.STATIC) == 0) {
				IMethodBinding getterMethodBinding = null;
				if(typeCheckElimination.getSuperAccessedFields().contains(fragment)) {
					for(IVariableBinding fieldBinding : typeCheckElimination.getSuperAccessedFieldBindings()) {
						if(fieldBinding.isEqualTo(fragment.resolveBinding())) {
							getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(fieldBinding);
							break;
						}
					}
				}
				else {
					getterMethodBinding = findGetterMethodInContext(fragment.resolveBinding());
				}
				if(getterMethodBinding == null) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
					int modifiers = fieldDeclaration.getModifiers();
					if(!fragment.equals(typeCheckElimination.getTypeField()) &&
							!((modifiers & Modifier.PUBLIC) != 0 && (modifiers & Modifier.STATIC) != 0)) {
						ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
						MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
						sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, fieldDeclaration.getType(), null);
						ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
						methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
						String methodName = fragment.getName().getIdentifier();
						methodName = "get" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
						sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(methodName), null);
						Block methodDeclarationBody = contextAST.newBlock();
						ListRewrite methodDeclarationBodyStatementsRewrite = sourceRewriter.getListRewrite(methodDeclarationBody, Block.STATEMENTS_PROPERTY);
						ReturnStatement returnStatement = contextAST.newReturnStatement();
						sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fragment.getName(), null);
						methodDeclarationBodyStatementsRewrite.insertLast(returnStatement, null);
						sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodDeclarationBody, null);
						ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
						contextBodyRewrite.insertLast(newMethodDeclaration, null);
						try {
							TextEdit sourceEdit = sourceRewriter.rewriteAST();
							ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
							CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
							change.getEdit().addChild(sourceEdit);
							change.addTextEditGroup(new TextEditGroup("Create getter method for accessed field", new TextEdit[] {sourceEdit}));
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	protected void generateSettersForAssignedFields() {
		AST contextAST = sourceTypeDeclaration.getAST();
		Set<VariableDeclarationFragment> assignedFields = new LinkedHashSet<VariableDeclarationFragment>();
		assignedFields.addAll(typeCheckElimination.getAssignedFields());
		assignedFields.addAll(typeCheckElimination.getSuperAssignedFields());
		for(VariableDeclarationFragment fragment : assignedFields) {
			IMethodBinding setterMethodBinding = null;
			if(typeCheckElimination.getSuperAssignedFields().contains(fragment)) {
				for(IVariableBinding fieldBinding : typeCheckElimination.getSuperAssignedFieldBindings()) {
					if(fieldBinding.isEqualTo(fragment.resolveBinding())) {
						setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(fieldBinding);
						break;
					}
				}
			}
			else {
				setterMethodBinding = findSetterMethodInContext(fragment.resolveBinding());
			}
			if(setterMethodBinding == null) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				if(!fragment.equals(typeCheckElimination.getTypeField())) {
					ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
					MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newPrimitiveType(PrimitiveType.VOID), null);
					ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					String methodName = fragment.getName().getIdentifier();
					methodName = "set" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(methodName), null);
					ListRewrite methodDeclarationParametersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
					SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
					sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, fieldDeclaration.getType(), null);
					sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, fragment.getName(), null);
					methodDeclarationParametersRewrite.insertLast(parameter, null);
					Block methodDeclarationBody = contextAST.newBlock();
					ListRewrite methodDeclarationBodyStatementsRewrite = sourceRewriter.getListRewrite(methodDeclarationBody, Block.STATEMENTS_PROPERTY);
					Assignment assignment = contextAST.newAssignment();
					sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, fragment.getName(), null);
					sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
					FieldAccess fieldAccess = contextAST.newFieldAccess();
					sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
					sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, fragment.getName(), null);
					sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAccess, null);
					ExpressionStatement expressionStatement = contextAST.newExpressionStatement(assignment);
					methodDeclarationBodyStatementsRewrite.insertLast(expressionStatement, null);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodDeclarationBody, null);
					ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					contextBodyRewrite.insertLast(newMethodDeclaration, null);
					try {
						TextEdit sourceEdit = sourceRewriter.rewriteAST();
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Create setter method for assigned field", new TextEdit[] {sourceEdit}));
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected void setPublicModifierToAccessedMethods() {
		for(MethodDeclaration methodDeclaration : typeCheckElimination.getAccessedMethods()) {
			ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
			List<IExtendedModifier> modifiers = methodDeclaration.modifiers();
			ListRewrite modifierRewrite = sourceRewriter.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			Modifier publicModifier = methodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
			boolean modifierFound = false;
			for(IExtendedModifier extendedModifier : modifiers) {
				if(extendedModifier.isModifier()) {
					Modifier modifier = (Modifier)extendedModifier;
					if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
						modifierFound = true;
					}
					else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
							modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
						modifierFound = true;
						modifierRewrite.replace(modifier, publicModifier, null);
						try {
							TextEdit sourceEdit = sourceRewriter.rewriteAST();
							ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
							CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
							change.getEdit().addChild(sourceEdit);
							change.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
			}
			if(!modifierFound) {
				modifierRewrite.insertFirst(publicModifier, null);
				try {
					TextEdit sourceEdit = sourceRewriter.rewriteAST();
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					change.getEdit().addChild(sourceEdit);
					change.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected Expression constructExpression(AST ast, DefaultMutableTreeNode node) {
		Object object = node.getUserObject();
		if(object instanceof InfixExpression.Operator) {
			InfixExpression.Operator operator = (InfixExpression.Operator)object;
			InfixExpression infixExpression = ast.newInfixExpression();
			infixExpression.setOperator(operator);
			DefaultMutableTreeNode leftChild = (DefaultMutableTreeNode)node.getChildAt(0);
			DefaultMutableTreeNode rightChild = (DefaultMutableTreeNode)node.getChildAt(1);
			infixExpression.setLeftOperand(constructExpression(ast, leftChild));
			infixExpression.setRightOperand(constructExpression(ast, rightChild));
			return infixExpression;
		}
		else if(object instanceof Expression) {
			Expression expression = (Expression)object;
			return expression;
		}
		return null;
	}

	protected boolean sourceTypeRequiredForExtraction() {
		int nonStaticAccessedMembers = 0;
		for(VariableDeclarationFragment fragment : typeCheckElimination.getAccessedFields()) {
			IVariableBinding fieldBinding = fragment.resolveBinding();
			if((fieldBinding.getModifiers() & Modifier.STATIC) == 0) {
				nonStaticAccessedMembers++;
			}
		}
		for(VariableDeclarationFragment fragment : typeCheckElimination.getAssignedFields()) {
			IVariableBinding fieldBinding = fragment.resolveBinding();
			if((fieldBinding.getModifiers() & Modifier.STATIC) == 0) {
				nonStaticAccessedMembers++;
			}
		}
		for(MethodDeclaration method : typeCheckElimination.getAccessedMethods()) {
			IMethodBinding methodBinding = method.resolveBinding();
			if((methodBinding.getModifiers() & Modifier.STATIC) == 0) {
				nonStaticAccessedMembers++;
			}
		}
		for(IMethodBinding methodBinding : typeCheckElimination.getSuperAccessedMethods()) {
			if((methodBinding.getModifiers() & Modifier.STATIC) == 0) {
				nonStaticAccessedMembers++;
			}
		}
		for(IVariableBinding fieldBinding : typeCheckElimination.getSuperAccessedFieldBindings()) {
			if((fieldBinding.getModifiers() & Modifier.STATIC) == 0) {
				nonStaticAccessedMembers++;
			}
		}
		for(IVariableBinding fieldBinding : typeCheckElimination.getSuperAssignedFieldBindings()) {
			if((fieldBinding.getModifiers() & Modifier.STATIC) == 0) {
				nonStaticAccessedMembers++;
			}
		}
		return nonStaticAccessedMembers > 0;
	}

	protected Expression generateDefaultValue(ASTRewrite sourceRewriter, AST ast, ITypeBinding returnTypeBinding) {
		Expression returnedExpression = null;
		if(returnTypeBinding.isPrimitive()) {
			if(returnTypeBinding.getQualifiedName().equals("boolean")) {
				returnedExpression = ast.newBooleanLiteral(false);
			}
			else if(returnTypeBinding.getQualifiedName().equals("char")) {
				CharacterLiteral characterLiteral = ast.newCharacterLiteral();
				sourceRewriter.set(characterLiteral, CharacterLiteral.ESCAPED_VALUE_PROPERTY, "\u0000", null);
				returnedExpression = characterLiteral;
			}
			else if(returnTypeBinding.getQualifiedName().equals("int") ||
					returnTypeBinding.getQualifiedName().equals("short") ||
					returnTypeBinding.getQualifiedName().equals("byte")) {
				returnedExpression = ast.newNumberLiteral("0");
			}
			else if(returnTypeBinding.getQualifiedName().equals("long")) {
				returnedExpression = ast.newNumberLiteral("0L");
			}
			else if(returnTypeBinding.getQualifiedName().equals("float")) {
				returnedExpression = ast.newNumberLiteral("0.0f");
			}
			else if(returnTypeBinding.getQualifiedName().equals("double")) {
				returnedExpression = ast.newNumberLiteral("0.0d");
			}
			else if(returnTypeBinding.getQualifiedName().equals("void")) {
				returnedExpression = null;
			}
		}
		else {
			returnedExpression = ast.newNullLiteral();
		}
		return returnedExpression;
	}
}
