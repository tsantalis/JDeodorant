package gr.uom.java.ast.decomposition.cfg;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.CompositeStatementObject;

public class PDGControlPredicateNode extends PDGNode {
	
	public PDGControlPredicateNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod) {
		super(cfgNode);
		determineDefinedAndUsedVariables(variableDeclarationsInMethod);
	}

	private void determineDefinedAndUsedVariables(Set<VariableDeclaration> variableDeclarationsInMethod) {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof CompositeStatementObject) {
			CompositeStatementObject composite = (CompositeStatementObject)cfgNode.getStatement();
			List<AbstractExpression> expressions = composite.getExpressions();
			for(AbstractExpression expression : expressions) {
				List<LocalVariableDeclarationObject> variableDeclarations = expression.getLocalVariableDeclarations();
				for(LocalVariableDeclarationObject variableDeclaration : variableDeclarations) {
					declaredVariables.add(variableDeclaration.getVariableDeclaration());
					definedVariables.add(variableDeclaration.getVariableDeclaration());
				}
				List<LocalVariableInstructionObject> variableInstructions = expression.getLocalVariableInstructions();
				for(LocalVariableInstructionObject variableInstruction : variableInstructions) {
					VariableDeclaration variableDeclaration = null;
					for(VariableDeclaration declaration : variableDeclarationsInMethod) {
						if(declaration.resolveBinding().isEqualTo(variableInstruction.getSimpleName().resolveBinding())) {
							variableDeclaration = declaration;
							break;
						}
					}
					List<Assignment> assignments = expression.getLocalVariableAssignments(variableInstruction);
					List<PostfixExpression> postfixExpressions = expression.getLocalVariablePostfixAssignments(variableInstruction);
					List<PrefixExpression> prefixExpressions = expression.getLocalVariablePrefixAssignments(variableInstruction);
					if(!assignments.isEmpty()) {
						definedVariables.add(variableDeclaration);
						for(Assignment assignment : assignments) {
							Assignment.Operator operator = assignment.getOperator();
							if(!operator.equals(Assignment.Operator.ASSIGN))
								usedVariables.add(variableDeclaration);
						}
					}
					else if(!postfixExpressions.isEmpty()) {
						definedVariables.add(variableDeclaration);
						usedVariables.add(variableDeclaration);
					}
					else if(!prefixExpressions.isEmpty()) {
						definedVariables.add(variableDeclaration);
						usedVariables.add(variableDeclaration);
					}
					else {
						SimpleName variableInstructionName = variableInstruction.getSimpleName();
						if(variableInstructionName.getParent() instanceof MethodInvocation) {
							MethodInvocation methodInvocation = (MethodInvocation)variableInstructionName.getParent();
							if(methodInvocation.getExpression() != null && methodInvocation.getExpression().equals(variableInstructionName)) {
								List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
								MethodInvocationObject methodInvocationObject = null;
								for(MethodInvocationObject mio : methodInvocations) {
									if(mio.getMethodInvocation().equals(methodInvocation)) {
										methodInvocationObject = mio;
										break;
									}
								}
								SystemObject systemObject = ASTReader.getSystemObject();
								ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
								if(classObject != null) {
									MethodObject methodObject = classObject.getMethod(methodInvocationObject);
									if(methodObject != null)
										processInternalMethodInvocation(classObject, methodObject, methodInvocation, variableDeclaration);
								}
								else {
									processExternalMethodInvocation(methodInvocation, variableDeclaration);
								}
							}
						}
						usedVariables.add(variableDeclaration);
					}
				}
				List<FieldInstructionObject> fieldInstructions = expression.getFieldInstructions();
				for(FieldInstructionObject fieldInstruction : fieldInstructions) {
					SystemObject systemObject = ASTReader.getSystemObject();
					ClassObject classObject = systemObject.getClassObject(fieldInstruction.getOwnerClass());
					if(classObject != null) {
						VariableDeclaration fieldDeclaration = null;
						ListIterator<FieldObject> fieldIterator = classObject.getFieldIterator();
						while(fieldIterator.hasNext()) {
							FieldObject fieldObject = fieldIterator.next();
							VariableDeclarationFragment fragment = fieldObject.getVariableDeclarationFragment();
							if(fragment.resolveBinding().isEqualTo(fieldInstruction.getSimpleName().resolveBinding())) {
								fieldDeclaration = fragment;
								break;
							}
						}
						if(fieldDeclaration != null) {
							SimpleName fieldInstructionName = fieldInstruction.getSimpleName();
							boolean stateChangingFieldModification = false;
							List<Assignment> fieldAssignments = expression.getFieldAssignments(fieldInstruction);
							List<PostfixExpression> fieldPostfixAssignments = expression.getFieldPostfixAssignments(fieldInstruction);
							List<PrefixExpression> fieldPrefixAssignments = expression.getFieldPrefixAssignments(fieldInstruction);
							if(!fieldAssignments.isEmpty()) {
								definedVariables.add(fieldDeclaration);
								for(Assignment assignment : fieldAssignments) {
									Assignment.Operator operator = assignment.getOperator();
									if(!operator.equals(Assignment.Operator.ASSIGN))
										usedVariables.add(fieldDeclaration);
								}
								stateChangingFieldModification = true;
							}
							else if(!fieldPostfixAssignments.isEmpty()) {
								definedVariables.add(fieldDeclaration);
								usedVariables.add(fieldDeclaration);
								stateChangingFieldModification = true;
							}
							else if(!fieldPrefixAssignments.isEmpty()) {
								definedVariables.add(fieldDeclaration);
								usedVariables.add(fieldDeclaration);
								stateChangingFieldModification = true;
							}
							else {
								if(fieldInstructionName.getParent() instanceof MethodInvocation) {
									MethodInvocation methodInvocation = (MethodInvocation)fieldInstructionName.getParent();
									if(methodInvocation.getExpression() != null && methodInvocation.getExpression().equals(fieldInstructionName)) {
										List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
										MethodInvocationObject methodInvocationObject = null;
										for(MethodInvocationObject mio : methodInvocations) {
											if(mio.getMethodInvocation().equals(methodInvocation)) {
												methodInvocationObject = mio;
												break;
											}
										}
										ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
										if(classObject2 != null) {
											MethodObject methodObject = classObject2.getMethod(methodInvocationObject);
											if(methodObject != null)
												processInternalMethodInvocation(classObject2, methodObject, methodInvocation, fieldDeclaration);
										}
										else {
											processExternalMethodInvocation(methodInvocation, fieldDeclaration);
										}
									}
								}
								usedVariables.add(fieldDeclaration);
							}
							if(stateChangingFieldModification) {
								processDirectFieldModification(fieldInstructionName, fieldDeclaration, variableDeclarationsInMethod);
							}
						}
					}
				}
				List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
					MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
					if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
						SystemObject systemObject = ASTReader.getSystemObject();
						ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject != null) {
							MethodObject methodObject = classObject.getMethod(methodInvocationObject);
							if(methodObject != null)
								processInternalMethodInvocation(classObject, methodObject, methodInvocation, null);
						}
					}
				}
			}
		}
	}
}
