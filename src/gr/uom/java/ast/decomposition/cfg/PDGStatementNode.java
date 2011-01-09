package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclaration;

import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class PDGStatementNode extends PDGNode {
	
	public PDGStatementNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod,
			Set<VariableDeclaration> fieldsAccessedInMethod) {
		super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
		determineDefinedAndUsedVariables();
	}

	private void determineDefinedAndUsedVariables() {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof StatementObject) {
			StatementObject statement = (StatementObject)cfgNode.getStatement();
			List<CreationObject> creations = statement.getCreations();
			for(CreationObject creation : creations) {
				createdTypes.add(creation.getType());
			}
			for(PlainVariable variable : statement.getDeclaredLocalVariables()) {
				declaredVariables.add(variable);
				definedVariables.add(variable);
			}
			for(PlainVariable variable : statement.getDefinedLocalVariables()) {
				definedVariables.add(variable);
			}
			for(PlainVariable variable : statement.getUsedLocalVariables()) {
				usedVariables.add(variable);
			}
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables = statement.getInvokedMethodsThroughLocalVariables();
			for(AbstractVariable variable : invokedMethodsThroughLocalVariables.keySet()) {
				LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughLocalVariables.get(variable);
				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
					thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
					processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
				}
			}
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = statement.getInvokedMethodsThroughParameters();
			for(AbstractVariable variable : invokedMethodsThroughParameters.keySet()) {
				LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(variable);
				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
					thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
					processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
				}
			}
			
			for(PlainVariable field : statement.getDefinedFieldsThroughThisReference()) {
				definedVariables.add(field);
			}
			for(PlainVariable field : statement.getUsedFieldsThroughThisReference()) {
				usedVariables.add(field);
			}
			for(AbstractVariable field : statement.getDefinedFieldsThroughFields()) {
				definedVariables.add(field);
			}
			for(AbstractVariable field : statement.getUsedFieldsThroughFields()) {
				usedVariables.add(field);
			}
			for(AbstractVariable field : statement.getDefinedFieldsThroughParameters()) {
				definedVariables.add(field);
			}
			for(AbstractVariable field : statement.getUsedFieldsThroughParameters()) {
				usedVariables.add(field);
			}
			for(AbstractVariable field : statement.getDefinedFieldsThroughLocalVariables()) {
				definedVariables.add(field);
			}
			for(AbstractVariable field : statement.getUsedFieldsThroughLocalVariables()) {
				usedVariables.add(field);
			}
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = statement.getInvokedMethodsThroughFields();
			for(AbstractVariable variable : invokedMethodsThroughFields.keySet()) {
				LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(variable);
				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
					thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
					processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
				}
			}
			for(MethodInvocationObject methodInvocationObject : statement.getInvokedMethodsThroughThisReference()) {
				thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
				processArgumentsOfInternalMethodInvocation(methodInvocationObject, null);
			}
			for(MethodInvocationObject methodInvocationObject : statement.getInvokedStaticMethods()) {
				thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
				processArgumentsOfInternalMethodInvocation(methodInvocationObject, null);
			}
			List<SuperMethodInvocationObject> superMethodInvocations = statement.getSuperMethodInvocations();
			for(SuperMethodInvocationObject superMethodInvocationObject : superMethodInvocations) {
				thrownExceptionTypes.addAll(superMethodInvocationObject.getThrownExceptions());
			}
		}
	}
}
