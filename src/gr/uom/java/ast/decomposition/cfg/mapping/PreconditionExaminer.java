package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractMethodFragment;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CatchClauseObject;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.TryStatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.CFGBreakNode;
import gr.uom.java.ast.decomposition.cfg.CFGContinueNode;
import gr.uom.java.ast.decomposition.cfg.CFGExitNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.CFGThrowNode;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.GraphNode;
import gr.uom.java.ast.decomposition.cfg.MethodCallAnalyzer;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGAbstractDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGAntiDependence;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGExpression;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGOutputDependence;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.DualExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.DualExpressionWithCommonSuperTypePreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolationType;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ReturnedVariablePreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.StatementPreconditionViolation;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.BindingSignaturePair;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import gr.uom.java.ast.decomposition.matching.FieldAssignmentReplacedWithSetterInvocationDifference;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PreconditionExaminer {
	private PDG pdg1;
	private PDG pdg2;
	private ICompilationUnit iCompilationUnit1;
	private ICompilationUnit iCompilationUnit2;
	private TreeSet<PDGNode> mappedNodesG1;
	private TreeSet<PDGNode> mappedNodesG2;
	private TreeSet<PDGNode> nonMappedNodesG1;
	private TreeSet<PDGNode> nonMappedNodesG2;
	private Map<VariableBindingKeyPair, ArrayList<AbstractVariable>> commonPassedParameters;
	private Map<VariableBindingKeyPair, ArrayList<AbstractVariable>> declaredLocalVariablesInMappedNodes;
	private Set<AbstractVariable> passedParametersG1;
	private Set<AbstractVariable> passedParametersG2;
	private Set<AbstractVariable> directlyAccessedLocalFieldsG1;
	private Set<AbstractVariable> directlyAccessedLocalFieldsG2;
	private Set<AbstractVariable> indirectlyAccessedLocalFieldsG1;
	private Set<AbstractVariable> indirectlyAccessedLocalFieldsG2;
	private Set<MethodObject> accessedLocalMethodsG1;
	private Set<MethodObject> accessedLocalMethodsG2;
	private Set<AbstractVariable> declaredVariablesInMappedNodesUsedByNonMappedNodesG1;
	private Set<AbstractVariable> declaredVariablesInMappedNodesUsedByNonMappedNodesG2;
	private List<PreconditionViolation> preconditionViolations;
	private Set<BindingSignaturePair> renamedVariables;
	private Set<PlainVariable> variablesToBeReturnedG1;
	private Set<PlainVariable> variablesToBeReturnedG2;
	private TreeSet<PDGNode> nonMappedPDGNodesG1MovableBefore;
	private TreeSet<PDGNode> nonMappedPDGNodesG1MovableAfter;
	private TreeSet<PDGNode> nonMappedPDGNodesG1MovableBeforeAndAfter;
	private TreeSet<PDGNode> nonMappedPDGNodesG2MovableBefore;
	private TreeSet<PDGNode> nonMappedPDGNodesG2MovableAfter;
	private TreeSet<PDGNode> nonMappedPDGNodesG2MovableBeforeAndAfter;
	private TreeSet<PDGNode> additionallyMatchedNodesG1;
	private TreeSet<PDGNode> additionallyMatchedNodesG2;
	private Set<AbstractVariable> declaredLocalVariablesInAdditionallyMatchedNodesG1;
	private Set<AbstractVariable> declaredLocalVariablesInAdditionallyMatchedNodesG2;
	private CloneStructureNode cloneStructureRoot;
	private MappingState finalState;
	private TreeSet<PDGNode> allNodesInSubTreePDG1;
	private TreeSet<PDGNode> allNodesInSubTreePDG2;
	
	public PreconditionExaminer(PDG pdg1, PDG pdg2,
			ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			CloneStructureNode cloneStructureRoot, MappingState finalState,
			TreeSet<PDGNode> allNodesInSubTreePDG1, TreeSet<PDGNode> allNodesInSubTreePDG2) {
		this.pdg1 = pdg1;
		this.pdg2 = pdg2;
		this.iCompilationUnit1 = iCompilationUnit1;
		this.iCompilationUnit2 = iCompilationUnit2;
		this.cloneStructureRoot = cloneStructureRoot;
		this.finalState = finalState;
		this.allNodesInSubTreePDG1 = allNodesInSubTreePDG1;
		this.allNodesInSubTreePDG2 = allNodesInSubTreePDG2;
		this.nonMappedNodesG1 = new TreeSet<PDGNode>();
		this.nonMappedNodesG2 = new TreeSet<PDGNode>();
		this.commonPassedParameters = new LinkedHashMap<VariableBindingKeyPair, ArrayList<AbstractVariable>>();
		this.declaredLocalVariablesInMappedNodes = new LinkedHashMap<VariableBindingKeyPair, ArrayList<AbstractVariable>>();
		this.passedParametersG1 = new LinkedHashSet<AbstractVariable>();
		this.passedParametersG2 = new LinkedHashSet<AbstractVariable>();
		this.directlyAccessedLocalFieldsG1 = new LinkedHashSet<AbstractVariable>();
		this.directlyAccessedLocalFieldsG2 = new LinkedHashSet<AbstractVariable>();
		this.indirectlyAccessedLocalFieldsG1 = new LinkedHashSet<AbstractVariable>();
		this.indirectlyAccessedLocalFieldsG2 = new LinkedHashSet<AbstractVariable>();
		this.accessedLocalMethodsG1 = new LinkedHashSet<MethodObject>();
		this.accessedLocalMethodsG2 = new LinkedHashSet<MethodObject>();
		this.declaredVariablesInMappedNodesUsedByNonMappedNodesG1 = new LinkedHashSet<AbstractVariable>();
		this.declaredVariablesInMappedNodesUsedByNonMappedNodesG2 = new LinkedHashSet<AbstractVariable>();
		this.preconditionViolations = new ArrayList<PreconditionViolation>();
		this.renamedVariables = new LinkedHashSet<BindingSignaturePair>();
		this.nonMappedPDGNodesG1MovableBefore = new TreeSet<PDGNode>();
		this.nonMappedPDGNodesG1MovableAfter = new TreeSet<PDGNode>();
		this.nonMappedPDGNodesG1MovableBeforeAndAfter = new TreeSet<PDGNode>();
		this.nonMappedPDGNodesG2MovableBefore = new TreeSet<PDGNode>();
		this.nonMappedPDGNodesG2MovableAfter = new TreeSet<PDGNode>();
		this.nonMappedPDGNodesG2MovableBeforeAndAfter = new TreeSet<PDGNode>();
		this.mappedNodesG1 = new TreeSet<PDGNode>();
		this.mappedNodesG2 = new TreeSet<PDGNode>();
		this.additionallyMatchedNodesG1 = new TreeSet<PDGNode>();
		this.additionallyMatchedNodesG2 = new TreeSet<PDGNode>();
		this.declaredLocalVariablesInAdditionallyMatchedNodesG1 = new LinkedHashSet<AbstractVariable>();
		this.declaredLocalVariablesInAdditionallyMatchedNodesG2 = new LinkedHashSet<AbstractVariable>();
		if(getMaximumStateWithMinimumDifferences() != null) {
			this.mappedNodesG1 = getMaximumStateWithMinimumDifferences().getMappedNodesG1();
			this.mappedNodesG2 = getMaximumStateWithMinimumDifferences().getMappedNodesG2();
			findNonMappedNodes(getAllNodesInSubTreePDG1(), mappedNodesG1, nonMappedNodesG1);
			findNonMappedNodes(getAllNodesInSubTreePDG2(), mappedNodesG2, nonMappedNodesG2);
			for(PDGNode nodeG1 : nonMappedNodesG1) {
				boolean advancedMatch = getCloneStructureRoot().isGapNodeG1InAdditionalMatches(nodeG1);
				List<ASTNodeDifference> differencesForAdvancedMatch = new ArrayList<ASTNodeDifference>();
				if(advancedMatch) {
					additionallyMatchedNodesG1.add(nodeG1);
					for(ASTNodeDifference difference : getNodeDifferences()) {
						if(isExpressionUnderStatement(difference.getExpression1().getExpression(), nodeG1.getASTStatement())) {
							differencesForAdvancedMatch.add(difference);
						}
					}
					List<AbstractVariable> nonAnonymousDeclaredVariablesG1 = new ArrayList<AbstractVariable>();
					Iterator<AbstractVariable> declaredVariableIteratorG1 = nodeG1.getDeclaredVariableIterator();
					while(declaredVariableIteratorG1.hasNext()) {
						AbstractVariable declaredVariableG1 = declaredVariableIteratorG1.next();
						String key1 = declaredVariableG1.getVariableBindingKey();
						String declaringType1 = key1.substring(0, key1.indexOf(";"));
						if(!declaringType1.contains("$")) {
							nonAnonymousDeclaredVariablesG1.add(declaredVariableG1);
						}
					}
					declaredLocalVariablesInAdditionallyMatchedNodesG1.addAll(nonAnonymousDeclaredVariablesG1);
				}
				PDGNodeGap nodeGap = new PDGNodeGap(nodeG1, null, advancedMatch, differencesForAdvancedMatch);
				CloneStructureNode node = new CloneStructureNode(nodeGap);
				PDGBlockNode tryNode = pdg1.isDirectlyNestedWithinBlockNode(nodeG1);
				if(tryNode != null) {
					CloneStructureNode cloneStructureTry = getCloneStructureRoot().findNodeG1(tryNode);
					if(cloneStructureTry != null) {
						node.setParent(cloneStructureTry);
					}
				}
				else {
					getCloneStructureRoot().addGapChild(node);
				}
			}
			nonMappedNodesG1.removeAll(additionallyMatchedNodesG1);
			mappedNodesG1.addAll(additionallyMatchedNodesG1);
			for(PDGNode nodeG2 : nonMappedNodesG2) {
				boolean advancedMatch = getCloneStructureRoot().isGapNodeG2InAdditionalMatches(nodeG2);
				List<ASTNodeDifference> differencesForAdvancedMatch = new ArrayList<ASTNodeDifference>();
				if(advancedMatch) {
					additionallyMatchedNodesG2.add(nodeG2);
					for(ASTNodeDifference difference : getNodeDifferences()) {
						if(isExpressionUnderStatement(difference.getExpression2().getExpression(), nodeG2.getASTStatement())) {
							differencesForAdvancedMatch.add(difference);
						}
					}
					List<AbstractVariable> nonAnonymousDeclaredVariablesG2 = new ArrayList<AbstractVariable>();
					Iterator<AbstractVariable> declaredVariableIteratorG2 = nodeG2.getDeclaredVariableIterator();
					while(declaredVariableIteratorG2.hasNext()) {
						AbstractVariable declaredVariableG2 = declaredVariableIteratorG2.next();
						String key2 = declaredVariableG2.getVariableBindingKey();
						String declaringType2 = key2.substring(0, key2.indexOf(";"));
						if(!declaringType2.contains("$")) {
							nonAnonymousDeclaredVariablesG2.add(declaredVariableG2);
						}
					}
					declaredLocalVariablesInAdditionallyMatchedNodesG2.addAll(nonAnonymousDeclaredVariablesG2);
				}
				PDGNodeGap nodeGap = new PDGNodeGap(null, nodeG2, advancedMatch, differencesForAdvancedMatch);
				CloneStructureNode node = new CloneStructureNode(nodeGap);
				PDGBlockNode tryNode = pdg2.isDirectlyNestedWithinBlockNode(nodeG2);
				if(tryNode != null) {
					CloneStructureNode cloneStructureTry = getCloneStructureRoot().findNodeG2(tryNode);
					if(cloneStructureTry != null) {
						node.setParent(cloneStructureTry);
					}
				}
				else {
					getCloneStructureRoot().addGapChild(node);
				}
			}
			nonMappedNodesG2.removeAll(additionallyMatchedNodesG2);
			mappedNodesG2.addAll(additionallyMatchedNodesG2);
			findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(pdg1, mappedNodesG1, nonMappedNodesG1, declaredVariablesInMappedNodesUsedByNonMappedNodesG1);
			findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(pdg2, mappedNodesG2, nonMappedNodesG2, declaredVariablesInMappedNodesUsedByNonMappedNodesG2);
			this.renamedVariables = findRenamedVariables();
			findPassedParameters();
			List<Expression> expressions1 = new ArrayList<Expression>();
			List<Expression> expressions2 = new ArrayList<Expression>();
			List<AbstractExpression> fieldAccessReplacedWithGetterExpressions1 = new ArrayList<AbstractExpression>();
			List<AbstractExpression> fieldAccessReplacedWithGetterExpressions2 = new ArrayList<AbstractExpression>();
			for(ASTNodeDifference nodeDifference : getNodeDifferences()) {
				if(!nodeDifference.containsDifferenceType(DifferenceType.FIELD_ACCESS_REPLACED_WITH_GETTER)) {
					Expression expression1 = nodeDifference.getExpression1().getExpression();
					Expression expr1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
					if(expression1.equals(expr1)) {
						expressions1.add(expression1);
					}
					else {
						expressions1.add(expr1);
					}
					Expression expression2 = nodeDifference.getExpression2().getExpression();
					Expression expr2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
					if(expression2.equals(expr2)) {
						expressions2.add(expression2);
					}
					else {
						expressions2.add(expr2);
					}
				}
				else {
					fieldAccessReplacedWithGetterExpressions1.add(nodeDifference.getExpression1());
					fieldAccessReplacedWithGetterExpressions2.add(nodeDifference.getExpression2());
				}
			}
			ITypeBinding commonSuperclass = ASTNodeMatcher.commonSuperType(pdg1.getMethod().getMethodDeclaration().resolveBinding().getDeclaringClass(),
					pdg2.getMethod().getMethodDeclaration().resolveBinding().getDeclaringClass());
			findLocallyAccessedFields(pdg1, mappedNodesG1, commonSuperclass, directlyAccessedLocalFieldsG1, indirectlyAccessedLocalFieldsG1, accessedLocalMethodsG1,
					expressions1, fieldAccessReplacedWithGetterExpressions1);
			findLocallyAccessedFields(pdg2, mappedNodesG2, commonSuperclass, directlyAccessedLocalFieldsG2, indirectlyAccessedLocalFieldsG2, accessedLocalMethodsG2,
					expressions2, fieldAccessReplacedWithGetterExpressions2);
			this.variablesToBeReturnedG1 = variablesToBeReturned(pdg1, getRemovableNodesG1());
			this.variablesToBeReturnedG2 = variablesToBeReturned(pdg2, getRemovableNodesG2());
			checkCloneStructureNodeForPreconditions(getCloneStructureRoot());
			processNonMappedNodesMovableBeforeAndAfter();
			checkPreconditionsAboutReturnedVariables();
			checkIfAllPossibleExecutionFlowsEndInReturn();
		}
	}

	private CloneStructureNode getCloneStructureRoot() {
		return cloneStructureRoot;
	}

	private MappingState getMaximumStateWithMinimumDifferences() {
		return finalState;
	}

	private TreeSet<PDGNode> getAllNodesInSubTreePDG1() {
		return allNodesInSubTreePDG1;
	}

	private TreeSet<PDGNode> getAllNodesInSubTreePDG2() {
		return allNodesInSubTreePDG2;
	}

	private void findNonMappedNodes(TreeSet<PDGNode> allNodes, Set<PDGNode> mappedNodes, Set<PDGNode> nonMappedNodes) {
		for(PDGNode pdgNode : allNodes) {
			if(!mappedNodes.contains(pdgNode)) {
				nonMappedNodes.add(pdgNode);
			}
		}
	}

	private void findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(PDG pdg, Set<PDGNode> mappedNodes, Set<PDGNode> unmappedNodes, Set<AbstractVariable> variables) {
		Set<PDGNode> nodes = new TreeSet<PDGNode>();
		nodes.addAll(mappedNodes);
		nodes.addAll(unmappedNodes);
		for(PDGNode mappedNode : nodes) {
			for(Iterator<AbstractVariable> declaredVariableIterator = mappedNode.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				for(GraphNode node : pdg.getNodes()) {
					PDGNode pdgNode = (PDGNode)node;
					if(!mappedNodes.contains(pdgNode) && !pdgNode.equals(mappedNode)) {
						if(pdgNode.usesLocalVariable(declaredVariable) || pdgNode.definesLocalVariable(declaredVariable)) {
							variables.add(declaredVariable);
							break;
						}
					}
				}
			}
			//special handling for parameters
			ListIterator<ParameterObject> parameterIterator = pdg.getMethod().getParameterListIterator();
			while(parameterIterator.hasNext()) {
				ParameterObject parameter = parameterIterator.next();
				VariableDeclaration variableDeclaration = parameter.getVariableDeclaration();
				AbstractVariable definedVariable = null;
				for(Iterator<AbstractVariable> definedVariableIterator = mappedNode.getDefinedVariableIterator(); definedVariableIterator.hasNext();) {
					AbstractVariable variable = definedVariableIterator.next();
					if(variable instanceof PlainVariable && variable.getVariableBindingKey().equals(variableDeclaration.resolveBinding().getKey())) {
						definedVariable = variable;
						break;
					}
				}
				if(definedVariable != null) {
					for(GraphNode node : pdg.getNodes()) {
						PDGNode pdgNode = (PDGNode)node;
						if(!mappedNodes.contains(pdgNode) && !pdgNode.equals(mappedNode)) {
							if(pdgNode.usesLocalVariable(definedVariable) || pdgNode.definesLocalVariable(definedVariable)) {
								variables.add(definedVariable);
								break;
							}
						}
					}
				}
			}
		}
	}

	private void findPassedParameters() {
		Set<AbstractVariable> passedParametersG1 = extractPassedParameters(pdg1, mappedNodesG1);
		Set<AbstractVariable> passedParametersG2 = extractPassedParameters(pdg2, mappedNodesG2);
		Set<AbstractVariable> parametersToBeRemovedG1 = new LinkedHashSet<AbstractVariable>();
		Set<AbstractVariable> parametersToBeRemovedG2 = new LinkedHashSet<AbstractVariable>();
		for(PDGNodeMapping nodeMapping : getMaximumStateWithMinimumDifferences().getNodeMappings()) {
			PDGNode nodeG1 = nodeMapping.getNodeG1();
			PDGNode nodeG2 = nodeMapping.getNodeG2();
			List<AbstractVariable> nonAnonymousDeclaredVariablesG1 = new ArrayList<AbstractVariable>();
			Iterator<AbstractVariable> declaredVariableIteratorG1 = nodeG1.getDeclaredVariableIterator();
			while(declaredVariableIteratorG1.hasNext()) {
				AbstractVariable declaredVariableG1 = declaredVariableIteratorG1.next();
				String key1 = declaredVariableG1.getVariableBindingKey();
				String declaringType1 = key1.substring(0, key1.indexOf(";"));
				if(!declaringType1.contains("$")) {
					nonAnonymousDeclaredVariablesG1.add(declaredVariableG1);
				}
			}
			List<AbstractVariable> nonAnonymousDeclaredVariablesG2 = new ArrayList<AbstractVariable>();
			Iterator<AbstractVariable> declaredVariableIteratorG2 = nodeG2.getDeclaredVariableIterator();
			while(declaredVariableIteratorG2.hasNext()) {
				AbstractVariable declaredVariableG2 = declaredVariableIteratorG2.next();
				String key2 = declaredVariableG2.getVariableBindingKey();
				String declaringType2 = key2.substring(0, key2.indexOf(";"));
				if(!declaringType2.contains("$")) {
					nonAnonymousDeclaredVariablesG2.add(declaredVariableG2);
				}
			}
			int min = Math.min(nonAnonymousDeclaredVariablesG1.size(), nonAnonymousDeclaredVariablesG2.size());
			for(int i=0; i<min; i++) {
				AbstractVariable declaredVariableG1 = nonAnonymousDeclaredVariablesG1.get(i);
				AbstractVariable declaredVariableG2 = nonAnonymousDeclaredVariablesG2.get(i);
				ArrayList<AbstractVariable> declaredVariables = new ArrayList<AbstractVariable>();
				declaredVariables.add(declaredVariableG1);
				declaredVariables.add(declaredVariableG2);
				VariableBindingKeyPair keyPair = new VariableBindingKeyPair(declaredVariableG1.getVariableBindingKey(),
						declaredVariableG2.getVariableBindingKey());
				declaredLocalVariablesInMappedNodes.put(keyPair, declaredVariables);
			}
			Set<AbstractVariable> dataDependences1 = nodeG1.incomingDataDependencesFromNodesDeclaringVariables();
			Set<AbstractVariable> dataDependences2 = nodeG2.incomingDataDependencesFromNodesDeclaringVariables();
			dataDependences1.retainAll(passedParametersG1);
			dataDependences2.retainAll(passedParametersG2);
			List<AbstractVariable> variables1 = new ArrayList<AbstractVariable>(dataDependences1);
			List<AbstractVariable> variables2 = new ArrayList<AbstractVariable>(dataDependences2);
			if(dataDependences1.size() == dataDependences2.size()) {
				List<String> variableNames1 = new ArrayList<String>();
				List<String> variableNames2 = new ArrayList<String>();
				for(int i=0; i<variables1.size(); i++) {
					variableNames1.add(variables1.get(i).getVariableName());
					AbstractVariable variable2 = variables2.get(i);
					String renamedVariableName = findRenamedVariableName(variable2);
					if(renamedVariableName != null)
						variableNames2.add(renamedVariableName);
					else
						variableNames2.add(variable2.getVariableName());
				}
				if(variableNames1.containsAll(variableNames2) && variableNames2.containsAll(variableNames1) &&
						variableNames1.size() > 0 && variableNames2.size() > 0) {
					//sort variables based on their names
					List<AbstractVariable> sortedVariables1 = new ArrayList<AbstractVariable>();
					List<AbstractVariable> sortedVariables2 = new ArrayList<AbstractVariable>();
					for(int i=0; i<variables1.size(); i++) {
						AbstractVariable variable1 = variables1.get(i);
						sortedVariables1.add(variable1);
						for(int j=0; j<variables2.size(); j++) {
							AbstractVariable variable2 = variables2.get(j);
							String renamedVariableName = findRenamedVariableName(variable2);
							if((variable2.getVariableName().equals(variable1.getVariableName()) ||
									variable1.getVariableName().equals(renamedVariableName)) &&
									variable2.getVariableType().equals(variable1.getVariableType())) {
								sortedVariables2.add(variable2);
								break;
							}
						}
					}
					if(sortedVariables1.size() == sortedVariables2.size()) {
						variables1 = sortedVariables1;
						variables2 = sortedVariables2;
					}
				}
				else {
					//sort variables based on their types (if a variable has the same type with multiple variables, apply first match)
					List<AbstractVariable> sortedVariables1 = new ArrayList<AbstractVariable>();
					List<AbstractVariable> sortedVariables2 = new ArrayList<AbstractVariable>();
					sortVariables(variables1, variables2, sortedVariables1, sortedVariables2);
					if(sortedVariables1.size() == sortedVariables2.size()) {
						variables1 = sortedVariables1;
						variables2 = sortedVariables2;
					}
				}
			}
			else {
				//there is a different number of incoming dependencies
				List<AbstractVariable> sortedVariables1 = new ArrayList<AbstractVariable>();
				List<AbstractVariable> sortedVariables2 = new ArrayList<AbstractVariable>();
				sortVariables(variables1, variables2, sortedVariables1, sortedVariables2);
				if(sortedVariables1.size() == sortedVariables2.size()) {
					variables1 = sortedVariables1;
					variables2 = sortedVariables2;
				}
			}
			for(int i=0; i<variables1.size(); i++) {
				AbstractVariable variable1 = variables1.get(i);
				AbstractVariable variable2 = variables2.get(i);
				if(passedParametersG1.contains(variable1) && passedParametersG2.contains(variable2)) {
					ArrayList<AbstractVariable> variableDeclarations = new ArrayList<AbstractVariable>();
					variableDeclarations.add(variable1);
					variableDeclarations.add(variable2);
					VariableBindingKeyPair keyPair = new VariableBindingKeyPair(variable1.getVariableBindingKey(),
							variable2.getVariableBindingKey());
					commonPassedParameters.put(keyPair, variableDeclarations);
					parametersToBeRemovedG1.add(variable1);
					parametersToBeRemovedG2.add(variable2);
				}
			}
		}
		passedParametersG1.removeAll(parametersToBeRemovedG1);
		passedParametersG2.removeAll(parametersToBeRemovedG2);
		this.passedParametersG1.addAll(passedParametersG1);
		this.passedParametersG2.addAll(passedParametersG2);
	}

	private void sortVariables(List<AbstractVariable> variables1, List<AbstractVariable> variables2,
			List<AbstractVariable> sortedVariables1, List<AbstractVariable> sortedVariables2) {
		boolean requireVariableNameMatch = getRenamedVariables().isEmpty();
		for(int i=0; i<variables1.size(); i++) {
			AbstractVariable variable1 = variables1.get(i);
			boolean found = false;
			for(int j=0; j<variables2.size(); j++) {
				AbstractVariable variable2 = variables2.get(j);
				if(requireVariableNameMatch) {
					if(variable2.getVariableName().equals(variable1.getVariableName()) && variable2.getVariableType().equals(variable1.getVariableType()) &&
							!sortedVariables2.contains(variable2)) {
						sortedVariables2.add(variable2);
						found = true;
						break;
					}
				}
				else {
					String renamedVariableName = findRenamedVariableName(variable2);
					if(renamedVariableName != null) {
						if(variable2.getVariableType().equals(variable1.getVariableType()) && variable1.getVariableName().equals(renamedVariableName) &&
								!sortedVariables2.contains(variable2)) {
							sortedVariables2.add(variable2);
							found = true;
							break;
						}
					}
					else {
						if(variable2.getVariableType().equals(variable1.getVariableType()) && !sortedVariables2.contains(variable2)) {
							sortedVariables2.add(variable2);
							found = true;
							break;
						}
					}
				}
			}
			if(found) {
				sortedVariables1.add(variable1);
			}
		}
	}

	private String findRenamedVariableName(AbstractVariable variable) {
		Set<BindingSignaturePair> renamedVariables = getRenamedVariables();
		String renamedVariableName = null;
		for(BindingSignaturePair pair : renamedVariables) {
			if(pair.getSignature2().containsOnlyBinding(variable.getVariableBindingKey())) {
				String signature1 = pair.getSignature1().toString();
				renamedVariableName = signature1.substring(signature1.lastIndexOf("#") + 1,
						signature1.lastIndexOf("]"));
				break;
			}
		}
		return renamedVariableName;
	}

	private Set<AbstractVariable> extractPassedParameters(PDG pdg, Set<PDGNode> mappedNodes) {
		Set<AbstractVariable> passedParameters = new LinkedHashSet<AbstractVariable>();
		for(GraphEdge edge : pdg.getEdges()) {
			PDGDependence dependence = (PDGDependence)edge;
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			PDGNode dstPDGNode = (PDGNode)dependence.getDst();
			if(dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				if(!mappedNodes.contains(srcPDGNode) && mappedNodes.contains(dstPDGNode)) {
					passedParameters.add(dataDependence.getData());
				}
			}
		}
		return passedParameters;
	}

	private void findLocallyAccessedFields(PDG pdg, Set<PDGNode> mappedNodes, ITypeBinding commonSuperclass,
			Set<AbstractVariable> directlyAccessedFields, Set<AbstractVariable> indirectlyAccessedFields, Set<MethodObject> accessedMethods,
			List<Expression> expressionsInDifferences, List<AbstractExpression> fieldAccessReplacedWithGetterExpressions) {
		Set<MethodInvocation> methodInvocationsToBeExcluded = new LinkedHashSet<MethodInvocation>();
		for(Expression expression : expressionsInDifferences) {
			if(expression instanceof MethodInvocation) {
				methodInvocationsToBeExcluded.add((MethodInvocation)expression);
			}
		}
		Set<AbstractVariable> fieldsWithGetterToBeIncluded = new LinkedHashSet<AbstractVariable>();
		for(AbstractExpression expression : fieldAccessReplacedWithGetterExpressions) {
			fieldsWithGetterToBeIncluded.addAll(expression.getUsedFieldsThroughThisReference());
		}
		Set<PlainVariable> usedLocalFields = new LinkedHashSet<PlainVariable>();
		Set<MethodInvocationObject> accessedLocalMethods = new LinkedHashSet<MethodInvocationObject>();
		for(PDGNode pdgNode : mappedNodes) {
			AbstractStatement abstractStatement = pdgNode.getStatement();
			if(abstractStatement instanceof StatementObject) {
				StatementObject statement = (StatementObject)abstractStatement;
				usedLocalFields.addAll(statement.getUsedFieldsThroughThisReference());
				usedLocalFields.addAll(statement.getDefinedFieldsThroughThisReference());
				accessedLocalMethods.addAll(statement.getInvokedMethodsThroughThisReference());
				accessedLocalMethods.addAll(statement.getInvokedStaticMethods());
			}
			else if(abstractStatement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
				usedLocalFields.addAll(composite.getUsedFieldsThroughThisReferenceInExpressions());
				usedLocalFields.addAll(composite.getDefinedFieldsThroughThisReferenceInExpressions());
				accessedLocalMethods.addAll(composite.getInvokedMethodsThroughThisReferenceInExpressions());
				accessedLocalMethods.addAll(composite.getInvokedStaticMethodsInExpressions());
				if(composite instanceof TryStatementObject) {
					TryStatementObject tryStatement = (TryStatementObject)composite;
					List<CatchClauseObject> catchClauses = tryStatement.getCatchClauses();
					for(CatchClauseObject catchClause : catchClauses) {
						usedLocalFields.addAll(catchClause.getBody().getUsedFieldsThroughThisReference());
						usedLocalFields.addAll(catchClause.getBody().getDefinedFieldsThroughThisReference());
						accessedLocalMethods.addAll(catchClause.getBody().getInvokedMethodsThroughThisReference());
						accessedLocalMethods.addAll(catchClause.getBody().getInvokedStaticMethods());
					}
					if(tryStatement.getFinallyClause() != null) {
						usedLocalFields.addAll(tryStatement.getFinallyClause().getUsedFieldsThroughThisReference());
						usedLocalFields.addAll(tryStatement.getFinallyClause().getDefinedFieldsThroughThisReference());
						accessedLocalMethods.addAll(tryStatement.getFinallyClause().getInvokedMethodsThroughThisReference());
						accessedLocalMethods.addAll(tryStatement.getFinallyClause().getInvokedStaticMethods());
					}
				}
			}
		}
		ITypeBinding declaringClassTypeBinding = pdg.getMethod().getMethodDeclaration().resolveBinding().getDeclaringClass();
		Set<VariableDeclaration> fieldsAccessedInMethod = pdg.getFieldsAccessedInMethod();
		for(PlainVariable variable : usedLocalFields) {
			for(VariableDeclaration fieldDeclaration : fieldsAccessedInMethod) {
				if(variable.getVariableBindingKey().equals(fieldDeclaration.resolveBinding().getKey())) {
					ITypeBinding fieldDeclaringClassTypeBinding = fieldDeclaration.resolveBinding().getDeclaringClass();
					Set<ITypeBinding> superTypes = getAllSuperTypesUpToCommonSuperclass(declaringClassTypeBinding, commonSuperclass);
					boolean fieldFoundInSuperType = false;
					for(ITypeBinding typeBinding : superTypes) {
						if(typeBinding.isEqualTo(fieldDeclaringClassTypeBinding)) {
							fieldFoundInSuperType = true;
							break;
						}
					}
					if(fieldDeclaringClassTypeBinding.isEqualTo(declaringClassTypeBinding) || fieldFoundInSuperType) {
						directlyAccessedFields.add(variable);
						if(fieldsWithGetterToBeIncluded.contains(variable)) {
							SystemObject system = ASTReader.getSystemObject();
							ClassObject accessedClass = system.getClassObject(fieldDeclaringClassTypeBinding.getQualifiedName());
							if(accessedClass != null) {
								ListIterator<MethodObject> it = accessedClass.getMethodIterator();
								while(it.hasNext()) {
									MethodObject method = it.next();
									FieldInstructionObject getterFieldInstruction = method.isGetter();
									if(getterFieldInstruction != null) {
										if(variable.getVariableBindingKey().equals(getterFieldInstruction.getSimpleName().resolveBinding().getKey())) {
											accessedMethods.add(method);
											break;
										}
									}
								}
							}
						}
						break;
					}
				}
			}
		}
		for(MethodInvocationObject invocation : accessedLocalMethods) {
			ITypeBinding invokedMethodDeclaringClassTypeBinding = invocation.getMethodInvocation().resolveMethodBinding().getDeclaringClass();
			Set<ITypeBinding> superTypes = getAllSuperTypesUpToCommonSuperclass(declaringClassTypeBinding, commonSuperclass);
			boolean invokedMethodFoundInSuperType = false;
			for(ITypeBinding typeBinding : superTypes) {
				if(typeBinding.isEqualTo(invokedMethodDeclaringClassTypeBinding)) {
					invokedMethodFoundInSuperType = true;
					break;
				}
			}
			if((invokedMethodDeclaringClassTypeBinding.isEqualTo(declaringClassTypeBinding) || invokedMethodFoundInSuperType) &&
					!methodInvocationsToBeExcluded.contains(invocation.getMethodInvocation())) {
				//exclude recursive method calls
				if(!pdg.getMethod().getMethodDeclaration().resolveBinding().isEqualTo(invocation.getMethodInvocation().resolveMethodBinding())) {
					SystemObject system = ASTReader.getSystemObject();
					MethodObject calledMethod = system.getMethod(invocation);
					if(calledMethod != null) {
						accessedMethods.add(calledMethod);
						FieldInstructionObject getterFieldInstruction = calledMethod.isGetter();
						if(getterFieldInstruction != null) {
							Set<PlainVariable> usedFields = calledMethod.getUsedFieldsThroughThisReference();
							for(PlainVariable plainVariable : usedFields) {
								if(plainVariable.getVariableBindingKey().equals(getterFieldInstruction.getSimpleName().resolveBinding().getKey())) {
									directlyAccessedFields.add(plainVariable);
									break;
								}
							}
						}
						ClassObject calledClass = system.getClassObject(calledMethod.getClassName());
						getAdditionalLocallyAccessedFieldsAndMethods(calledMethod, calledClass, indirectlyAccessedFields, accessedMethods);
					}
				}
			}
		}
	}
	
	private static Set<ITypeBinding> getAllSuperTypesUpToCommonSuperclass(ITypeBinding typeBinding, ITypeBinding commonSuperclass) {
		Set<ITypeBinding> superTypes = new LinkedHashSet<ITypeBinding>();
		ITypeBinding superTypeBinding = typeBinding.getSuperclass();
		if(superTypeBinding != null && !superTypeBinding.isEqualTo(commonSuperclass)) {
			superTypes.add(superTypeBinding);
			superTypes.addAll(getAllSuperTypesUpToCommonSuperclass(superTypeBinding, commonSuperclass));
		}
		ITypeBinding[] superInterfaces = typeBinding.getInterfaces();
		for(ITypeBinding superInterface : superInterfaces) {
			if(!superInterface.isEqualTo(commonSuperclass)) {
				superTypes.add(superInterface);
				superTypes.addAll(getAllSuperTypesUpToCommonSuperclass(superInterface, commonSuperclass));
			}
		}
		return superTypes;
	}

	private void getAdditionalLocallyAccessedFieldsAndMethods(MethodObject calledMethod, ClassObject calledClass,
			Set<AbstractVariable> accessedFields, Set<MethodObject> accessedMethods) {
		Set<PlainVariable> usedLocalFields = new LinkedHashSet<PlainVariable>();
		Set<MethodInvocationObject> accessedLocalMethods = new LinkedHashSet<MethodInvocationObject>();
		usedLocalFields.addAll(calledMethod.getUsedFieldsThroughThisReference());
		usedLocalFields.addAll(calledMethod.getDefinedFieldsThroughThisReference());
		accessedLocalMethods.addAll(calledMethod.getInvokedMethodsThroughThisReference());
		accessedLocalMethods.addAll(calledMethod.getInvokedStaticMethods());
		ITypeBinding declaringClassTypeBinding = calledMethod.getMethodDeclaration().resolveBinding().getDeclaringClass();
		Set<FieldObject> fieldsAccessedInMethod = calledClass.getFieldsAccessedInsideMethod(calledMethod);
		for(PlainVariable variable : usedLocalFields) {
			for(FieldObject fieldDeclaration : fieldsAccessedInMethod) {
				IVariableBinding fieldBinding = fieldDeclaration.getVariableDeclaration().resolveBinding();
				if(variable.getVariableBindingKey().equals(fieldBinding.getKey()) &&
						fieldBinding.getDeclaringClass().isEqualTo(declaringClassTypeBinding)) {
					accessedFields.add(variable);
					break;
				}
			}
		}
		for(MethodInvocationObject invocation : accessedLocalMethods) {
			if(invocation.getMethodInvocation().resolveMethodBinding().getDeclaringClass().isEqualTo(declaringClassTypeBinding)) {
				SystemObject system = ASTReader.getSystemObject();
				MethodObject calledMethod2 = system.getMethod(invocation);
				if(calledMethod2 != null && !accessedMethods.contains(calledMethod2)) {
					accessedMethods.add(calledMethod2);
					ClassObject calledClass2 = system.getClassObject(calledMethod2.getClassName());
					getAdditionalLocallyAccessedFieldsAndMethods(calledMethod2, calledClass2, accessedFields, accessedMethods);
				}
			}
		}
	}

	public PDG getPDG1() {
		return pdg1;
	}

	public PDG getPDG2() {
		return pdg2;
	}

	public String getMethodName1() {
		return pdg1.getMethod().getName();
	}

	public String getMethodName2() {
		return pdg2.getMethod().getName();
	}

	public TreeSet<PDGNode> getRemovableNodesG1() {
		return mappedNodesG1;
	}

	public TreeSet<PDGNode> getRemovableNodesG2() {
		return mappedNodesG2;
	}

	public TreeSet<PDGNode> getRemainingNodesG1() {
		return nonMappedNodesG1;
	}

	public TreeSet<PDGNode> getRemainingNodesG2() {
		return nonMappedNodesG2;
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG1MovableBefore() {
		return nonMappedPDGNodesG1MovableBefore;
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG1MovableAfter() {
		return nonMappedPDGNodesG1MovableAfter;
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG2MovableBefore() {
		return nonMappedPDGNodesG2MovableBefore;
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG2MovableAfter() {
		return nonMappedPDGNodesG2MovableAfter;
	}

	public TreeSet<PDGNode> getAdditionallyMatchedNodesG1() {
		return additionallyMatchedNodesG1;
	}

	public TreeSet<PDGNode> getAdditionallyMatchedNodesG2() {
		return additionallyMatchedNodesG2;
	}

	public Set<VariableDeclaration> getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG1() {
		Set<VariableDeclaration> declaredVariablesG1 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsInMethod1 = pdg1.getVariableDeclarationsInMethod();
		for(AbstractVariable variable1 : this.declaredVariablesInMappedNodesUsedByNonMappedNodesG1) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable1.getVariableBindingKey())) {
					declaredVariablesG1.add(variableDeclaration);
					break;
				}
			}
		}
		return declaredVariablesG1;
	}

	public Set<VariableDeclaration> getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG2() {
		Set<VariableDeclaration> declaredVariablesG2 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsInMethod2 = pdg2.getVariableDeclarationsInMethod();
		for(AbstractVariable variable2 : this.declaredVariablesInMappedNodesUsedByNonMappedNodesG2) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable2.getVariableBindingKey())) {
					declaredVariablesG2.add(variableDeclaration);
					break;
				}
			}
		}
		return declaredVariablesG2;
	}

	public Set<AbstractVariable> getDirectlyAccessedLocalFieldsG1() {
		return directlyAccessedLocalFieldsG1;
	}

	public Set<AbstractVariable> getDirectlyAccessedLocalFieldsG2() {
		return directlyAccessedLocalFieldsG2;
	}

	public Set<AbstractVariable> getIndirectlyAccessedLocalFieldsG1() {
		return indirectlyAccessedLocalFieldsG1;
	}

	public Set<AbstractVariable> getIndirectlyAccessedLocalFieldsG2() {
		return indirectlyAccessedLocalFieldsG2;
	}

	public Set<MethodObject> getAccessedLocalMethodsG1() {
		return accessedLocalMethodsG1;
	}

	public Set<MethodObject> getAccessedLocalMethodsG2() {
		return accessedLocalMethodsG2;
	}

	public Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> getDeclaredLocalVariablesInMappedNodes() {
		Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> declaredVariables = new LinkedHashMap<VariableBindingKeyPair, ArrayList<VariableDeclaration>>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = pdg1.getVariableDeclarationsAndAccessedFieldsInMethod();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = pdg2.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(VariableBindingKeyPair key : this.declaredLocalVariablesInMappedNodes.keySet()) {
			ArrayList<AbstractVariable> value = this.declaredLocalVariablesInMappedNodes.get(key);
			AbstractVariable variableDeclaration1 = value.get(0);
			AbstractVariable variableDeclaration2 = value.get(1);
			ArrayList<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration1.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration2.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			declaredVariables.put(key, variableDeclarations);
		}
		return declaredVariables;
	}
	
	public Set<VariableDeclaration> getDeclaredLocalVariablesInAdditionallyMatchedNodesG1() {
		Set<VariableDeclaration> declaredVariables = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = pdg1.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(AbstractVariable variable : this.declaredLocalVariablesInAdditionallyMatchedNodesG1) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable.getVariableBindingKey())) {
					declaredVariables.add(variableDeclaration);
					break;
				}
			}
		}
		return declaredVariables;
	}

	public Set<String> getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG1() {
		Set<String> declaredVariableKeys = new LinkedHashSet<String>();
		for(AbstractVariable variable : this.declaredLocalVariablesInAdditionallyMatchedNodesG1) {
			declaredVariableKeys.add(variable.getVariableBindingKey());
		}
		return declaredVariableKeys;
	}

	public Set<VariableDeclaration> getDeclaredLocalVariablesInAdditionallyMatchedNodesG2() {
		Set<VariableDeclaration> declaredVariables = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = pdg2.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(AbstractVariable variable : this.declaredLocalVariablesInAdditionallyMatchedNodesG2) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable.getVariableBindingKey())) {
					declaredVariables.add(variableDeclaration);
					break;
				}
			}
		}
		return declaredVariables;
	}

	public Set<String> getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG2() {
		Set<String> declaredVariableKeys = new LinkedHashSet<String>();
		for(AbstractVariable variable : this.declaredLocalVariablesInAdditionallyMatchedNodesG2) {
			declaredVariableKeys.add(variable.getVariableBindingKey());
		}
		return declaredVariableKeys;
	}

	public Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> getCommonPassedParameters() {
		Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> commonPassedParameters = new LinkedHashMap<VariableBindingKeyPair, ArrayList<VariableDeclaration>>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = pdg1.getVariableDeclarationsAndAccessedFieldsInMethod();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = pdg2.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(VariableBindingKeyPair key : this.commonPassedParameters.keySet()) {
			ArrayList<AbstractVariable> value = this.commonPassedParameters.get(key);
			AbstractVariable variableDeclaration1 = value.get(0);
			AbstractVariable variableDeclaration2 = value.get(1);
			ArrayList<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration1.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration2.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			commonPassedParameters.put(key, variableDeclarations);
		}
		return commonPassedParameters;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return getMaximumStateWithMinimumDifferences().getNodeDifferences();
	}

	public List<ASTNodeDifference> getNonOverlappingNodeDifferences() {
		return getMaximumStateWithMinimumDifferences().getNonOverlappingNodeDifferences();
	}

	private Set<BindingSignaturePair> findRenamedVariables() {
		Set<BindingSignaturePair> variableNameMismatches = new LinkedHashSet<BindingSignaturePair>();
		for(ASTNodeDifference nodeDifference : getNodeDifferences()) {
			List<Difference> diffs = nodeDifference.getDifferences();
			for(Difference diff : diffs) {
				if(diff.getType().equals(DifferenceType.VARIABLE_NAME_MISMATCH)) {
					Expression expression1 = nodeDifference.getExpression1().getExpression();
					Expression expression2 = nodeDifference.getExpression2().getExpression();
					if(expression1 instanceof SimpleName && expression2 instanceof SimpleName) {
						SimpleName simpleName1 = (SimpleName)expression1;
						SimpleName simpleName2 = (SimpleName)expression2;
						IBinding binding1 = simpleName1.resolveBinding();
						IBinding binding2 = simpleName2.resolveBinding();
						if(binding1.getKind() == IBinding.VARIABLE && binding2.getKind() == IBinding.VARIABLE) {
							IVariableBinding variableBinding1 = (IVariableBinding)binding1;
							IVariableBinding variableBinding2 = (IVariableBinding)binding2;
							IMethodBinding declaringMethod1 = variableBinding1.getDeclaringMethod();
							IMethodBinding declaringMethod2 = variableBinding2.getDeclaringMethod();
							IMethodBinding  method1 = pdg1.getMethod().getMethodDeclaration().resolveBinding();
							IMethodBinding  method2 = pdg2.getMethod().getMethodDeclaration().resolveBinding();
							if(declaringMethod1 != null && declaringMethod1.isEqualTo(method1) &&
									declaringMethod2 != null && declaringMethod2.isEqualTo(method2)) {
								variableNameMismatches.add(nodeDifference.getBindingSignaturePair());
							}
						}
					}
				}
			}
		}
		Set<BindingSignaturePair> inconsistentRenames = new LinkedHashSet<BindingSignaturePair>();
		for(PDGNodeMapping nodeMapping : getMaximumStateWithMinimumDifferences().getNodeMappings()) {
			List<ASTNodeDifference> nodeDifferences = nodeMapping.getNodeDifferences();
			Set<BindingSignaturePair> localVariableNameMismatches = new LinkedHashSet<BindingSignaturePair>();
			List<AbstractExpression> expressions1 = new ArrayList<AbstractExpression>();
			List<AbstractExpression> expressions2 = new ArrayList<AbstractExpression>();
			for(ASTNodeDifference nodeDifference : nodeDifferences) {
				if(!nodeDifference.containsDifferenceType(DifferenceType.VARIABLE_NAME_MISMATCH)) {
					expressions1.add(nodeDifference.getExpression1());
					expressions2.add(nodeDifference.getExpression2());
				}
				else {
					localVariableNameMismatches.add(nodeDifference.getBindingSignaturePair());
				}
			}
			PDGNode nodeG1 = nodeMapping.getNodeG1();
			PDGNode nodeG2 = nodeMapping.getNodeG2();
			Set<PDGNode> mappedNodesWithoutAdditionallyMappedNodesG1 = new TreeSet<PDGNode>(mappedNodesG1);
			mappedNodesWithoutAdditionallyMappedNodesG1.removeAll(additionallyMatchedNodesG1);
			List<AbstractMethodFragment> additionallyMatchedFragments1 = getAdditionallyMatchedFragmentsNotBeingUnderMappedStatement(
					nodeMapping.getAdditionallyMatchedFragments1(), mappedNodesWithoutAdditionallyMappedNodesG1);
			
			Set<PDGNode> mappedNodesWithoutAdditionallyMappedNodesG2 = new TreeSet<PDGNode>(mappedNodesG2);
			mappedNodesWithoutAdditionallyMappedNodesG2.removeAll(additionallyMatchedNodesG2);
			List<AbstractMethodFragment> additionallyMatchedFragments2 = getAdditionallyMatchedFragmentsNotBeingUnderMappedStatement(
					nodeMapping.getAdditionallyMatchedFragments2(), mappedNodesWithoutAdditionallyMappedNodesG2);
			
			Set<PlainVariable> variables1 = getVariables(nodeG1, additionallyMatchedFragments1, expressions1);
			Set<PlainVariable> variables2 = getVariables(nodeG2, additionallyMatchedFragments2, expressions2);
			for(PlainVariable plainVariable1 : variables1) {
				BindingSignaturePair pair1 = getBindingSignaturePairForVariable1(plainVariable1, variableNameMismatches);
				if(pair1 != null) {
					boolean matchingPairFound = false;
					for(PlainVariable plainVariable2 : variables2) {
						BindingSignaturePair pair2 = getBindingSignaturePairForVariable2(plainVariable2, variableNameMismatches);
						if(pair2 != null && pair2.equals(pair1) && localVariableNameMismatches.contains(pair1)) {
							matchingPairFound = true;
							break;
						}
					}
					if(!matchingPairFound) {
						inconsistentRenames.add(pair1);
					}
				}
			}
			for(PlainVariable plainVariable2 : variables2) {
				BindingSignaturePair pair2 = getBindingSignaturePairForVariable2(plainVariable2, variableNameMismatches);
				if(pair2 != null) {
					boolean matchingPairFound = false;
					for(PlainVariable plainVariable1 : variables1) {
						BindingSignaturePair pair1 = getBindingSignaturePairForVariable1(plainVariable1, variableNameMismatches);
						if(pair1 != null && pair1.equals(pair2) && localVariableNameMismatches.contains(pair2)) {
							matchingPairFound = true;
							break;
						}
					}
					if(!matchingPairFound) {
						inconsistentRenames.add(pair2);
					}
				}
			}
		}
		Set<BindingSignaturePair> variables = new LinkedHashSet<BindingSignaturePair>();
		variables.addAll(variableNameMismatches);
		variables.removeAll(inconsistentRenames);
		return variables;
	}

	private List<AbstractMethodFragment> getAdditionallyMatchedFragmentsNotBeingUnderMappedStatement(
			List<AbstractMethodFragment> originalFragments, Set<PDGNode> mappedStatements) {
		List<AbstractMethodFragment> fragments = new ArrayList<AbstractMethodFragment>();
		for(AbstractMethodFragment fragment : originalFragments) {
			boolean isUnderMappedStatement = false;
			if(fragment instanceof AbstractExpression) {
				AbstractExpression expression = (AbstractExpression)fragment;
				Expression expr = expression.getExpression();
				for(PDGNode node : mappedStatements) {
					if(isExpressionUnderStatement(expr, node.getASTStatement())) {
						isUnderMappedStatement = true;
						break;
					}
				}
			}
			else if(fragment instanceof StatementObject) {
				StatementObject statement = (StatementObject)fragment;
				Statement stmt = statement.getStatement();
				for(PDGNode node : mappedStatements) {
					if(stmt.equals(node.getASTStatement())) {
						isUnderMappedStatement = true;
						break;
					}
				}
			}
			if(!isUnderMappedStatement) {
				fragments.add(fragment);
			}
		}
		return fragments;
	}

	private Set<PlainVariable> getVariables(PDGNode node, List<AbstractMethodFragment> additionallyMatchedFragments,
			List<AbstractExpression> expressionsInDifferences) {
		Set<PlainVariable> variablesToBeExcluded = new LinkedHashSet<PlainVariable>();
		for(AbstractExpression expression : expressionsInDifferences) {
			variablesToBeExcluded.addAll(expression.getDefinedLocalVariables());
			variablesToBeExcluded.addAll(expression.getUsedLocalVariables());
		}
		Set<PlainVariable> variables = new LinkedHashSet<PlainVariable>();
		Iterator<AbstractVariable> definedVariableIterator = node.getDefinedVariableIterator();
		while(definedVariableIterator.hasNext()) {
			AbstractVariable variable = definedVariableIterator.next();
			if(variable instanceof PlainVariable && !variablesToBeExcluded.contains(variable)) {
				variables.add((PlainVariable)variable);
			}
		}
		Iterator<AbstractVariable> usedVariableIterator = node.getUsedVariableIterator();
		while(usedVariableIterator.hasNext()) {
			AbstractVariable variable = usedVariableIterator.next();
			if(variable instanceof PlainVariable && !variablesToBeExcluded.contains(variable)) {
				variables.add((PlainVariable)variable);
			}
		}
		for(AbstractMethodFragment methodFragment : additionallyMatchedFragments) {
			Set<PlainVariable> usedLocalVariables = methodFragment.getUsedLocalVariables();
			for(PlainVariable variable : usedLocalVariables) {
				if(variable instanceof PlainVariable && !variablesToBeExcluded.contains(variable)) {
					variables.add(variable);
				}
			}
			Set<PlainVariable> definedLocalVariables = methodFragment.getDefinedLocalVariables();
			for(PlainVariable variable : definedLocalVariables) {
				if(variable instanceof PlainVariable && !variablesToBeExcluded.contains(variable)) {
					variables.add(variable);
				}
			}
			Set<PlainVariable> declaredLocalVariables = methodFragment.getDeclaredLocalVariables();
			for(PlainVariable variable : declaredLocalVariables) {
				if(variable instanceof PlainVariable && !variablesToBeExcluded.contains(variable)) {
					variables.add(variable);
				}
			}
		}
		return variables;
	}

	private BindingSignaturePair getBindingSignaturePairForVariable1(PlainVariable plainVariable, Set<BindingSignaturePair> variableNameMismatches) {
		for(BindingSignaturePair pair : variableNameMismatches) {
			if(pair.getSignature1().containsOnlyBinding(plainVariable.getVariableBindingKey())) {
				return pair;
			}
		}
		return null;
	}

	private BindingSignaturePair getBindingSignaturePairForVariable2(PlainVariable plainVariable, Set<BindingSignaturePair> variableNameMismatches) {
		for(BindingSignaturePair pair : variableNameMismatches) {
			if(pair.getSignature2().containsOnlyBinding(plainVariable.getVariableBindingKey())) {
				return pair;
			}
		}
		return null;
	}

	public List<PreconditionViolation> getPreconditionViolations() {
		return preconditionViolations;
	}

	public Set<BindingSignaturePair> getRenamedVariables() {
		return renamedVariables;
	}

	public Set<PlainVariable> getVariablesToBeReturnedG1() {
		return variablesToBeReturnedG1;
	}

	public Set<PlainVariable> getVariablesToBeReturnedG2() {
		return variablesToBeReturnedG2;
	}

	private Set<PlainVariable> variablesToBeReturned(PDG pdg, Set<PDGNode> mappedNodes) {
		Set<PDGNode> remainingNodes = new TreeSet<PDGNode>();
		Iterator<GraphNode> iterator = pdg.getNodeIterator();
		while(iterator.hasNext()) {
			PDGNode pdgNode = (PDGNode)iterator.next();
			if(!mappedNodes.contains(pdgNode)) {
				remainingNodes.add(pdgNode);
			}
		}
		Set<PlainVariable> variablesToBeReturned = new LinkedHashSet<PlainVariable>();
		for(PDGNode remainingNode : remainingNodes) {
			Iterator<GraphEdge> incomingDependenceIt = remainingNode.getIncomingDependenceIterator();
			while(incomingDependenceIt.hasNext()) {
				PDGDependence dependence = (PDGDependence)incomingDependenceIt.next();
				if(dependence instanceof PDGDataDependence) {
					PDGDataDependence dataDependence = (PDGDataDependence)dependence;
					PDGNode srcNode = (PDGNode)dataDependence.getSrc();
					if(mappedNodes.contains(srcNode) && dataDependence.getData() instanceof PlainVariable) {
						PlainVariable variable = (PlainVariable)dataDependence.getData();
						if(!variable.isField())
							variablesToBeReturned.add(variable);
					}
				}
			}
		}
		for(PDGNode mappedNode : mappedNodes) {
			Iterator<GraphEdge> incomingDependenceIt = mappedNode.getIncomingDependenceIterator();
			while(incomingDependenceIt.hasNext()) {
				PDGDependence dependence = (PDGDependence)incomingDependenceIt.next();
				if(dependence instanceof PDGDataDependence) {
					PDGDataDependence dataDependence = (PDGDataDependence)dependence;
					PDGNode srcNode = (PDGNode)dataDependence.getSrc();
					if(mappedNodes.contains(srcNode) && dataDependence.getData() instanceof PlainVariable) {
						PlainVariable variable = (PlainVariable)dataDependence.getData();
						if(dataDependence.isLoopCarried() && !mappedNodes.contains(dataDependence.getLoop().getPDGNode())) {
							if(!variable.isField() && !srcNode.declaresLocalVariable(variable))
								variablesToBeReturned.add(variable);
						}
					}
				}
			}
		}
		return variablesToBeReturned;
	}

	private void checkPreconditionsAboutReturnedVariables() {
		//if the returned variables are more than one, the precondition is violated
		if(variablesToBeReturnedG1.size() > 1 || variablesToBeReturnedG2.size() > 1) {
			PreconditionViolation violation = new ReturnedVariablePreconditionViolation(variablesToBeReturnedG1, variablesToBeReturnedG2,
					PreconditionViolationType.MULTIPLE_RETURNED_VARIABLES);
			preconditionViolations.add(violation);
		}
		else if(variablesToBeReturnedG1.size() == 1 && variablesToBeReturnedG2.size() == 1) {
			PlainVariable returnedVariable1 = variablesToBeReturnedG1.iterator().next();
			PlainVariable returnedVariable2 = variablesToBeReturnedG2.iterator().next();
			if(!returnedVariable1.getVariableType().equals(returnedVariable2.getVariableType())) {
				PreconditionViolation violation = new ReturnedVariablePreconditionViolation(variablesToBeReturnedG1, variablesToBeReturnedG2,
						PreconditionViolationType.SINGLE_RETURNED_VARIABLE_WITH_DIFFERENT_TYPES);
				preconditionViolations.add(violation);
			}
		}
		else if((variablesToBeReturnedG1.size() == 1 && variablesToBeReturnedG2.size() == 0) ||
				(variablesToBeReturnedG1.size() == 0 && variablesToBeReturnedG2.size() == 1)) {
			PreconditionViolation violation = new ReturnedVariablePreconditionViolation(variablesToBeReturnedG1, variablesToBeReturnedG2,
					PreconditionViolationType.UNEQUAL_NUMBER_OF_RETURNED_VARIABLES);
			preconditionViolations.add(violation);
		}
	}

	private void checkIfAllPossibleExecutionFlowsEndInReturn() {
		Set<PDGNode> allConditionalReturnStatements1 = extractConditionalReturnStatements(pdg1.getNodes());
		Set<PDGNode> allConditionalReturnStatements2 = extractConditionalReturnStatements(pdg2.getNodes());
		Set<PDGNode> mappedConditionalReturnStatements1 = extractConditionalReturnStatements(mappedNodesG1);
		Set<PDGNode> mappedConditionalReturnStatements2 = extractConditionalReturnStatements(mappedNodesG2);
		if(allConditionalReturnStatements1.size() > mappedConditionalReturnStatements1.size() &&
				allConditionalReturnStatements2.size() > mappedConditionalReturnStatements2.size()) {
			for(PDGNodeMapping nodeMapping : getMaximumStateWithMinimumDifferences().getNodeMappings()) {
				PDGNode node1 = nodeMapping.getNodeG1();
				PDGNode node2 = nodeMapping.getNodeG2();
				if(mappedConditionalReturnStatements1.contains(node1)) {
					PreconditionViolation violation = new StatementPreconditionViolation(node1.getStatement(),
							PreconditionViolationType.CONDITIONAL_RETURN_STATEMENT);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
				}
				if(mappedConditionalReturnStatements2.contains(node2)) {
					PreconditionViolation violation = new StatementPreconditionViolation(node2.getStatement(),
							PreconditionViolationType.CONDITIONAL_RETURN_STATEMENT);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
				}
			}
		}
	}

	private Set<PDGNode> extractConditionalReturnStatements(Set<? extends GraphNode> nodes) {
		Set<PDGNode> conditionalReturnStatements = new TreeSet<PDGNode>();
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			CFGNode cfgNode = pdgNode.getCFGNode();
			if(cfgNode instanceof CFGExitNode) {
				ReturnStatement returnStatement = (ReturnStatement)cfgNode.getASTStatement();
				Expression expression = returnStatement.getExpression();
				if(expression != null && expression instanceof BooleanLiteral) {
					PDGNode controlParentNode = pdgNode.getControlDependenceParent();
					if(controlParentNode instanceof PDGControlPredicateNode) {
						conditionalReturnStatements.add(pdgNode);
					}
				}
			}
		}
		return conditionalReturnStatements;
	}

	private void conditionalReturnStatement(NodeMapping nodeMapping, PDGNode node) {
		CFGNode cfgNode = node.getCFGNode();
		if(cfgNode instanceof CFGExitNode) {
			ReturnStatement returnStatement = (ReturnStatement)cfgNode.getASTStatement();
			if(returnStatement.getExpression() == null) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.CONDITIONAL_RETURN_STATEMENT);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
		}
	}

	private void branchStatementWithInnermostLoop(NodeMapping nodeMapping, PDGNode node, Set<PDGNode> mappedNodes) {
		CFGNode cfgNode = node.getCFGNode();
		if(cfgNode instanceof CFGBreakNode) {
			CFGBreakNode breakNode = (CFGBreakNode)cfgNode;
			CFGNode innerMostLoopNode = breakNode.getInnerMostLoopNode();
			if(innerMostLoopNode != null && !mappedNodes.contains(innerMostLoopNode.getPDGNode())) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.BREAK_STATEMENT_WITHOUT_LOOP);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
		}
		else if(cfgNode instanceof CFGContinueNode) {
			CFGContinueNode continueNode = (CFGContinueNode)cfgNode;
			CFGNode innerMostLoopNode = continueNode.getInnerMostLoopNode();
			if(innerMostLoopNode != null && !mappedNodes.contains(innerMostLoopNode.getPDGNode())) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.CONTINUE_STATEMENT_WITHOUT_LOOP);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
		}
	}

	private void checkCloneStructureNodeForPreconditions(CloneStructureNode node) {
		if(node.getMapping() != null)
			checkPreconditions(node);
		for(CloneStructureNode child : node.getChildren()) {
			checkCloneStructureNodeForPreconditions(child);
		}
	}

	private void checkPreconditions(CloneStructureNode node) {
		TreeSet<PDGNode> removableNodesG1 = getRemovableNodesG1();
		TreeSet<PDGNode> removableNodesG2 = getRemovableNodesG2();
		NodeMapping nodeMapping = node.getMapping();
		for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
			boolean isDifferenceInConditionalExpressionOfAdvancedLoopMatch = false;
			if(nodeMapping instanceof PDGNodeMapping) {
				PDGNodeMapping pdgNodeMapping = (PDGNodeMapping)nodeMapping;
				if(pdgNodeMapping.isDifferenceInConditionalExpressionOfAdvancedLoopMatch(difference))
					isDifferenceInConditionalExpressionOfAdvancedLoopMatch = true;
			}
			AbstractExpression abstractExpression1 = difference.getExpression1();
			Expression expression1 = abstractExpression1.getExpression();
			AbstractExpression abstractExpression2 = difference.getExpression2();
			Expression expression2 = abstractExpression2.getExpression();
			if(!renamedVariables.contains(difference.getBindingSignaturePair()) && !isVariableWithTypeMismatchDifference(expression1, expression2, difference) &&
					!isDifferenceInConditionalExpressionOfAdvancedLoopMatch) {
				PreconditionViolationType violationType1 = isParameterizableExpression(pdg1, removableNodesG1, abstractExpression1, iCompilationUnit1);
				if(violationType1 != null) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression1(), violationType1);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
					IMethodBinding methodBinding = getMethodBinding(expression1);
					if(methodBinding != null) {
						int methodModifiers = methodBinding.getModifiers();
						if((methodModifiers & Modifier.PRIVATE) != 0) {
							String message = "Inline private method " + methodBinding.getName();
							violation.addSuggestion(message);
						}
					}
				}
				PreconditionViolationType violationType2 = isParameterizableExpression(pdg2, removableNodesG2, abstractExpression2, iCompilationUnit2);
				if(violationType2 != null) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression2(), violationType2);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
					IMethodBinding methodBinding = getMethodBinding(expression2);
					if(methodBinding != null) {
						int methodModifiers = methodBinding.getModifiers();
						if((methodModifiers & Modifier.PRIVATE) != 0) {
							String message = "Inline private method " + methodBinding.getName();
							violation.addSuggestion(message);
						}
					}
				}
				if(isFieldUpdate(abstractExpression1)) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression1(),
							PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_FIELD_UPDATE);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
				}
				if(isFieldUpdate(abstractExpression2)) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression2(),
							PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_FIELD_UPDATE);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
				}
				if(isVoidMethodCall(abstractExpression1) && !(difference instanceof FieldAssignmentReplacedWithSetterInvocationDifference)) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression1(),
							PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_VOID_METHOD_CALL);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
				}
				if(isVoidMethodCall(abstractExpression2) && !(difference instanceof FieldAssignmentReplacedWithSetterInvocationDifference)) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression2(),
							PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_VOID_METHOD_CALL);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
				}
			}
			if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
				if(nodeMapping instanceof PDGNodeMapping) {
					PDGNodeMapping pdgNodeMapping = (PDGNodeMapping)nodeMapping;
					Set<IMethodBinding> methods1 = new LinkedHashSet<IMethodBinding>();
					ITypeBinding typeBinding1 = difference.getExpression1().getExpression().resolveTypeBinding();
					findMethodsCalledFromType(typeBinding1, pdgNodeMapping.getNodeG1(), methods1);
					
					Set<IMethodBinding> methods2 = new LinkedHashSet<IMethodBinding>();
					ITypeBinding typeBinding2 = difference.getExpression2().getExpression().resolveTypeBinding();
					findMethodsCalledFromType(typeBinding2, pdgNodeMapping.getNodeG2(), methods2);
					
					if(!typeBinding1.isEqualTo(typeBinding2)) {
						ITypeBinding commonSuperType = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
						if(commonSuperType != null) {
							Set<String> commonSuperTypeMembers = new LinkedHashSet<String>();
							for(IMethodBinding methodBinding1 : methods1) {
								for(IMethodBinding methodBinding2 : methods2) {
									if(MethodCallAnalyzer.equalSignature(methodBinding1, methodBinding2)) {
										Set<IMethodBinding> declaredMethods = getDeclaredMethods(commonSuperType);
										boolean commonSuperTypeMethodFound = false;
										for(IMethodBinding commonSuperTypeMethod : declaredMethods) {
											if(MethodCallAnalyzer.equalSignature(methodBinding1, commonSuperTypeMethod)) {
												commonSuperTypeMethodFound = true;
												break;
											}
										}
										if(!commonSuperTypeMethodFound) {
											commonSuperTypeMembers.add(methodBinding1.toString());
										}
										break;
									}
								}
							}
							if(!commonSuperTypeMembers.isEmpty()) {
								PreconditionViolation violation = new DualExpressionWithCommonSuperTypePreconditionViolation(difference.getExpression1(), difference.getExpression2(),
										PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_MISSING_MEMBERS_IN_THE_COMMON_SUPERCLASS,
										commonSuperType.getQualifiedName(), commonSuperTypeMembers);
								nodeMapping.addPreconditionViolation(violation);
								preconditionViolations.add(violation);
							}
						}
					}
				}
			}
			if(difference.containsDifferenceType(DifferenceType.VARIABLE_TYPE_MISMATCH)) {
				PreconditionViolation violation = new DualExpressionPreconditionViolation(difference.getExpression1(), difference.getExpression2(),
						PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
				ITypeBinding typeBinding1 = expression1.resolveTypeBinding();
				ITypeBinding typeBinding2 = expression2.resolveTypeBinding();
				if(!typeBinding1.isPrimitive() && !typeBinding2.isPrimitive()) {
					String message = "Make classes " + typeBinding1.getQualifiedName() + " and " + typeBinding2.getQualifiedName() + " extend a common superclass";
					violation.addSuggestion(message);
				}
			}
		}
		if(nodeMapping instanceof PDGNodeGap) {
			if(nodeMapping.getNodeG1() != null && !nodeMapping.isAdvancedMatch()) {
				processNonMappedNode(pdg1, nodeMapping, nodeMapping.getNodeG1(), removableNodesG1, nonMappedPDGNodesG1MovableBeforeAndAfter,
						nonMappedPDGNodesG1MovableBefore, nonMappedPDGNodesG1MovableAfter, variablesToBeReturnedG1);
			}
			if(nodeMapping.getNodeG2() != null && !nodeMapping.isAdvancedMatch()) {
				processNonMappedNode(pdg2, nodeMapping, nodeMapping.getNodeG2(), removableNodesG2, nonMappedPDGNodesG2MovableBeforeAndAfter,
						nonMappedPDGNodesG2MovableBefore, nonMappedPDGNodesG2MovableAfter, variablesToBeReturnedG2);
			}
		}
		if(nodeMapping instanceof PDGNodeMapping) {
			branchStatementWithInnermostLoop(nodeMapping, nodeMapping.getNodeG1(), removableNodesG1);
			branchStatementWithInnermostLoop(nodeMapping, nodeMapping.getNodeG2(), removableNodesG2);
			//skip examining the conditional return precondition, if the number of examined nodes is equal to the number of PDG nodes
			if(getAllNodesInSubTreePDG1().size() != pdg1.getNodes().size()) {
				conditionalReturnStatement(nodeMapping, nodeMapping.getNodeG1());
			}
			if(getAllNodesInSubTreePDG2().size() != pdg2.getNodes().size()) {
				conditionalReturnStatement(nodeMapping, nodeMapping.getNodeG2());
			}
		}
	}

	private Set<IMethodBinding> getDeclaredMethods(ITypeBinding typeBinding) {
		Set<IMethodBinding> declaredMethods = new LinkedHashSet<IMethodBinding>();
		//first add the directly declared methods
		for(IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
			declaredMethods.add(methodBinding);
		}
		ITypeBinding superclassTypeBinding = typeBinding.getSuperclass();
		if(superclassTypeBinding != null) {
			declaredMethods.addAll(getDeclaredMethods(superclassTypeBinding));
		}
		ITypeBinding[] interfaces = typeBinding.getInterfaces();
		for(ITypeBinding interfaceTypeBinding : interfaces) {
			declaredMethods.addAll(getDeclaredMethods(interfaceTypeBinding));
		}
		return declaredMethods;
	}

	private boolean isVariableWithTypeMismatchDifference(Expression expression1, Expression expression2, ASTNodeDifference difference) {
		if(expression1 instanceof SimpleName && expression2 instanceof SimpleName) {
			SimpleName simpleName1 = (SimpleName)expression1;
			SimpleName simpleName2 = (SimpleName)expression2;
			IBinding binding1 = simpleName1.resolveBinding();
			IBinding binding2 = simpleName2.resolveBinding();
			//check if both simpleNames refer to variables
			if(binding1.getKind() == IBinding.VARIABLE && binding2.getKind() == IBinding.VARIABLE) {
				List<Difference> differences = difference.getDifferences();
				if(differences.size() == 1) {
					Difference diff = differences.get(0);
					if(diff.getType().equals(DifferenceType.SUBCLASS_TYPE_MISMATCH) || diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void findMethodsCalledFromType(ITypeBinding typeBinding, PDGNode pdgNode, Set<IMethodBinding> methods) {
		Set<MethodInvocationObject> accessedMethods = new LinkedHashSet<MethodInvocationObject>();
		AbstractStatement abstractStatement = pdgNode.getStatement();
		if(abstractStatement instanceof StatementObject) {
			StatementObject statement = (StatementObject)abstractStatement;
			accessedMethods.addAll(statement.getMethodInvocations());
		}
		else if(abstractStatement instanceof CompositeStatementObject) {
			CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
			accessedMethods.addAll(composite.getMethodInvocationsInExpressions());
			if(composite instanceof TryStatementObject) {
				TryStatementObject tryStatement = (TryStatementObject)composite;
				List<CatchClauseObject> catchClauses = tryStatement.getCatchClauses();
				for(CatchClauseObject catchClause : catchClauses) {
					accessedMethods.addAll(catchClause.getBody().getMethodInvocations());
				}
				if(tryStatement.getFinallyClause() != null) {
					accessedMethods.addAll(tryStatement.getFinallyClause().getMethodInvocations());
				}
			}
		}
		for(MethodInvocationObject invocation : accessedMethods) {
			IMethodBinding methodBinding = invocation.getMethodInvocation().resolveMethodBinding();
			if(methodBinding.getDeclaringClass().isEqualTo(typeBinding)) {
				methods.add(methodBinding);
			}
		}
	}

	private void processNonMappedNode(PDG pdg, NodeMapping nodeMapping, PDGNode node, TreeSet<PDGNode> removableNodes,
			TreeSet<PDGNode> movableBeforeAndAfter, TreeSet<PDGNode> movableBefore, TreeSet<PDGNode> movableAfter, Set<PlainVariable> returnedVariables) {
		boolean movableNonMappedNodeBeforeFirstMappedNode = movableNonMappedNodeBeforeFirstMappedNode(removableNodes, node);
		boolean movableNonMappedNodeAfterLastMappedNode = movableNonMappedNodeAfterLastMappedNode(removableNodes, node, returnedVariables);
		if(!movableNonMappedNodeBeforeFirstMappedNode && !movableNonMappedNodeAfterLastMappedNode) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
		else if(movableNonMappedNodeBeforeFirstMappedNode && movableNonMappedNodeAfterLastMappedNode) {
			if(controlParentExaminesVariableUsedInNonMappedNode(node, removableNodes)) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
			else {
				movableBeforeAndAfter.add(node);
			}
		}
		else if(movableNonMappedNodeBeforeFirstMappedNode) {
			if(controlParentExaminesVariableUsedInNonMappedNode(node, removableNodes)) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
			else {
				movableBefore.add(node);
			}
		}
		else if(movableNonMappedNodeAfterLastMappedNode) {
			if(controlParentExaminesVariableUsedInNonMappedNode(node, removableNodes)) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
			else {
				movableAfter.add(node);
			}
		}
		if(node.throwsException()) {
			PDGBlockNode blockNode = pdg.isNestedWithinBlockNode(node);
			if(blockNode != null && blockNode instanceof PDGTryNode && removableNodes.contains(blockNode)) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.UNMATCHED_EXCEPTION_THROWING_STATEMENT_NESTED_WITHIN_MATCHED_TRY_BLOCK);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
		}
		CFGNode cfgNode = node.getCFGNode();
		if(cfgNode instanceof CFGBreakNode) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_BREAK_STATEMENT);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
		else if(cfgNode instanceof CFGContinueNode) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_CONTINUE_STATEMENT);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
		else if(cfgNode instanceof CFGExitNode) {
			if(!movableAfter.contains(node)) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.UNMATCHED_RETURN_STATEMENT);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
		}
		else if(cfgNode instanceof CFGThrowNode) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_THROW_STATEMENT);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
	}
	private boolean controlParentExaminesVariableUsedInNonMappedNode(PDGNode node, TreeSet<PDGNode> removableNodes) {
		TreeSet<PDGNode> removableControlParents = new TreeSet<PDGNode>();
		for(PDGNode removableNode : removableNodes) {
			if(node.isControlDependentOnNode(removableNode)) {
				removableControlParents.add(removableNode);
			}
		}
		Iterator<AbstractVariable> iterator = node.getUsedVariableIterator();
		while(iterator.hasNext()) {
			AbstractVariable variable = iterator.next();
			if(variable instanceof PlainVariable) {
				PlainVariable plainVariable = (PlainVariable)variable;
				if(controlParentExaminesVariableInCondition(plainVariable, removableControlParents)) {
					return true;
				}
			}
		}
		return false;
	}
	private boolean controlParentExaminesVariableUsedInDifferenceExpression(PDGExpression expression, PDGNode nodeContainingExpression, TreeSet<PDGNode> removableNodes) {
		TreeSet<PDGNode> removableControlParents = new TreeSet<PDGNode>();
		for(PDGNode removableNode : removableNodes) {
			if(nodeContainingExpression.isControlDependentOnNode(removableNode)) {
				removableControlParents.add(removableNode);
			}
		}
		Iterator<AbstractVariable> iterator = expression.getUsedVariableIterator();
		while(iterator.hasNext()) {
			AbstractVariable variable = iterator.next();
			if(variable instanceof PlainVariable) {
				PlainVariable plainVariable = (PlainVariable)variable;
				if(controlParentExaminesVariableInCondition(plainVariable, removableControlParents)) {
					return true;
				}
			}
		}
		return false;
	}
	private boolean controlParentExaminesVariableInCondition(PlainVariable plainVariable, TreeSet<PDGNode> removableControlParents) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<ASTNodeDifference> differences = getNodeDifferences();
		for(PDGNode controlParent : removableControlParents) {
			if(controlParent.getCFGNode() instanceof CFGBranchIfNode) {
				CFGBranchIfNode ifNode = (CFGBranchIfNode)controlParent.getCFGNode();
				CompositeStatementObject composite = (CompositeStatementObject)ifNode.getStatement();
				List<AbstractExpression> expressions = composite.getExpressions();
				List<Expression> allSimpleNamesInLeftOperands = new ArrayList<Expression>();
				Expression conditionalExpression = expressions.get(0).getExpression();
				List<Expression> infixExpressions = expressionExtractor.getInfixExpressions(conditionalExpression);
				for(Expression expression : infixExpressions) {
					InfixExpression infixExpression = (InfixExpression)expression;
					allSimpleNamesInLeftOperands.addAll(expressionExtractor.getVariableInstructions(infixExpression.getLeftOperand()));
				}
				List<Expression> instanceofExpressions = expressionExtractor.getInstanceofExpressions(conditionalExpression);
				for(Expression expression : instanceofExpressions) {
					InstanceofExpression instanceofExpression = (InstanceofExpression)expression;
					allSimpleNamesInLeftOperands.addAll(expressionExtractor.getVariableInstructions(instanceofExpression.getLeftOperand()));
				}
				for(Expression expression : allSimpleNamesInLeftOperands) {
					SimpleName simpleName = (SimpleName)expression;
					boolean foundInDifferences = false;
					for(ASTNodeDifference difference : differences) {
						Expression expr1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression1().getExpression());
						Expression expr2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression2().getExpression());
						if(isExpressionWithinExpression(simpleName, expr1) || isExpressionWithinExpression(simpleName, expr2)) {
							foundInDifferences = true;
							break;
						}
					}
					if(!foundInDifferences) {
						IBinding binding = simpleName.resolveBinding();
						if(binding.getKind() == IBinding.VARIABLE) {
							IVariableBinding variableBinding = (IVariableBinding)binding;
							if(variableBinding.getKey().equals(plainVariable.getVariableBindingKey())) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	private void processNonMappedNodesMovableBeforeAndAfter() {
		examineIfNonMappedNodesUpdateTheSameVariable(nonMappedPDGNodesG1MovableBeforeAndAfter);
		for(PDGNode nodeG1 : nonMappedPDGNodesG1MovableBeforeAndAfter) {
			boolean movableNonMappedNodeBeforeNonMappedNodesMovableAfter = movableNonMappedNodeBeforeNonMappedNodesMovableAfter(nonMappedPDGNodesG1MovableAfter, nodeG1);
			if(movableNonMappedNodeBeforeNonMappedNodesMovableAfter) {
				nonMappedPDGNodesG1MovableBefore.add(nodeG1);
			}
			else {
				nonMappedPDGNodesG1MovableAfter.add(nodeG1);
			}
		}
		examineIfNonMappedNodesUpdateTheSameVariable(nonMappedPDGNodesG2MovableBeforeAndAfter);
		for(PDGNode nodeG2 : nonMappedPDGNodesG2MovableBeforeAndAfter) {
			boolean movableNonMappedNodeBeforeNonMappedNodesMovableAfter = movableNonMappedNodeBeforeNonMappedNodesMovableAfter(nonMappedPDGNodesG2MovableAfter, nodeG2);
			if(movableNonMappedNodeBeforeNonMappedNodesMovableAfter) {
				nonMappedPDGNodesG2MovableBefore.add(nodeG2);
			}
			else {
				nonMappedPDGNodesG2MovableAfter.add(nodeG2);
			}
		}
	}
	private void examineIfNonMappedNodesUpdateTheSameVariable(TreeSet<PDGNode> nonMappedNodes) {
		Map<PlainVariable, Set<PDGNode>> nodesUpdatingTheSameVariable = new LinkedHashMap<PlainVariable, Set<PDGNode>>();
		for(PDGNode node : nonMappedNodes) {
			AbstractStatement abstractStatement = node.getStatement();
			Statement statement = node.getASTStatement();
			if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof Assignment) {
					Assignment assignment = (Assignment)expression;
					Expression leftHandSide = assignment.getLeftHandSide();
					if(leftHandSide instanceof SimpleName) {
						SimpleName simpleName = (SimpleName)leftHandSide;
						if(isUpdated(simpleName)) {
							PlainVariable plainVariable = null;
							for(PlainVariable variable : abstractStatement.getDefinedLocalVariables()) {
								if(simpleName.resolveBinding().getKey().equals(variable.getVariableBindingKey())) {
									plainVariable = variable;
									break;
								}
							}
							if(plainVariable != null) {
								if(nodesUpdatingTheSameVariable.containsKey(plainVariable)) {
									Set<PDGNode> nodes = nodesUpdatingTheSameVariable.get(plainVariable);
									nodes.add(node);
								}
								else {
									Set<PDGNode> nodes = new TreeSet<PDGNode>();
									nodes.add(node);
									nodesUpdatingTheSameVariable.put(plainVariable, nodes);
								}
							}
						}
					}
				}
			}
		}
		for(PlainVariable variable : nodesUpdatingTheSameVariable.keySet()) {
			Set<PDGNode> nodes = nodesUpdatingTheSameVariable.get(variable);
			if(nodes.size() > 1) {
				nonMappedNodes.removeAll(nodes);
				for(PDGNode node : nodes) {
					CloneStructureNode cloneStructureNode1 = getCloneStructureRoot().findNodeG1(node);
					if(cloneStructureNode1 != null && cloneStructureNode1.getMapping().getNodeG1().getASTStatement().equals(node.getASTStatement())) {
						NodeMapping nodeMapping = cloneStructureNode1.getMapping();
						PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
								PreconditionViolationType.MULTIPLE_UNMATCHED_STATEMENTS_UPDATE_THE_SAME_VARIABLE);
						nodeMapping.addPreconditionViolation(violation);
						preconditionViolations.add(violation);
					}
					CloneStructureNode cloneStructureNode2 = getCloneStructureRoot().findNodeG2(node);
					if(cloneStructureNode2 != null && cloneStructureNode2.getMapping().getNodeG2().getASTStatement().equals(node.getASTStatement())) {
						NodeMapping nodeMapping = cloneStructureNode2.getMapping();
						PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
								PreconditionViolationType.MULTIPLE_UNMATCHED_STATEMENTS_UPDATE_THE_SAME_VARIABLE);
						nodeMapping.addPreconditionViolation(violation);
						preconditionViolations.add(violation);
					}
				}
			}
		}
	}
	
	private boolean movableNonMappedNodeBeforeNonMappedNodesMovableAfter(TreeSet<PDGNode> nonMappedNodes, PDGNode nonMappedNode) {
		Iterator<GraphEdge> incomingDependenceIterator = nonMappedNode.getIncomingDependenceIterator();
		while(incomingDependenceIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)incomingDependenceIterator.next();
			if(dependence instanceof PDGAbstractDataDependence) {
				PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)dataDependence.getSrc();
				if(nonMappedNodes.contains(srcPDGNode)) {
					return false;
				}
				//examine if it is a self-loop edge due to a loop-carried dependence
				if(srcPDGNode.equals(nonMappedNode)) {
					if(dataDependence.isLoopCarried() && nonMappedNodes.contains(dataDependence.getLoop().getPDGNode())) {
						return false;
					}
				}
			}
		}
		for(PDGNode dstPDGNode : nonMappedNodes) {
			if(dstPDGNode.isControlDependentOnNode(nonMappedNode)) {
				return false;
			}
		}
		return true;
	}
	//precondition: non-mapped statement can be moved before the first mapped statement
	private boolean movableNonMappedNodeBeforeFirstMappedNode(TreeSet<PDGNode> mappedNodes, PDGNode nonMappedNode) {
		Iterator<GraphEdge> incomingDependenceIterator = nonMappedNode.getIncomingDependenceIterator();
		while(incomingDependenceIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)incomingDependenceIterator.next();
			if(dependence instanceof PDGAbstractDataDependence) {
				PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)dataDependence.getSrc();
				if(mappedNodes.contains(srcPDGNode)) {
					return false;
				}
				//examine if it is a self-loop edge due to a loop-carried dependence
				if(srcPDGNode.equals(nonMappedNode)) {
					if(dataDependence.isLoopCarried() && mappedNodes.contains(dataDependence.getLoop().getPDGNode())) {
						return false;
					}
				}
			}
		}
		return true;
	}
	//precondition: non-mapped statement can be moved after the last mapped statement
	private boolean movableNonMappedNodeAfterLastMappedNode(TreeSet<PDGNode> mappedNodes, PDGNode nonMappedNode, Set<PlainVariable> returnedVariables) {
		Iterator<GraphEdge> outgoingDependenceIterator = nonMappedNode.getOutgoingDependenceIterator();
		while(outgoingDependenceIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
			if(dependence instanceof PDGAbstractDataDependence) {
				PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence)dependence;
				PDGNode dstPDGNode = (PDGNode)dataDependence.getDst();
				if(mappedNodes.contains(dstPDGNode)) {
					return false;
				}
				//examine if it is a self-loop edge due to a loop-carried dependence
				if(dstPDGNode.equals(nonMappedNode)) {
					if(dataDependence.isLoopCarried() && mappedNodes.contains(dataDependence.getLoop().getPDGNode())) {
						return false;
					}
				}
			}
		}
		Iterator<GraphEdge> incomingDependenceIterator = nonMappedNode.getIncomingDependenceIterator();
		while(incomingDependenceIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)incomingDependenceIterator.next();
			if(dependence instanceof PDGAbstractDataDependence) {
				PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)dataDependence.getSrc();
				if(mappedNodes.contains(srcPDGNode)) {
					AbstractVariable data = dataDependence.getData();
					if(data instanceof PlainVariable) {
						PlainVariable plainVariable = (PlainVariable)data;
						if(!plainVariable.isField() && !returnedVariables.contains(plainVariable)) {
							return false;
						}
					}
					else if(data instanceof CompositeVariable) {
						CompositeVariable composite = (CompositeVariable)data;
						PlainVariable initial = composite.getInitialVariable();
						if(!initial.isField() && !returnedVariables.contains(initial)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private boolean isVoidMethodCall(AbstractExpression initialAbstractExpression) {
		Expression initialExpression = initialAbstractExpression.getExpression();
		Expression expr = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(initialExpression);
		if(expr instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)expr;
			ITypeBinding returnTypeBinding = methodInvocation.resolveMethodBinding().getReturnType();
			if(returnTypeBinding.getQualifiedName().equals("void")) {
				return true;
			}
		}
		else if(expr instanceof SuperMethodInvocation) {
			SuperMethodInvocation methodInvocation = (SuperMethodInvocation)expr;
			ITypeBinding returnTypeBinding = methodInvocation.resolveMethodBinding().getReturnType();
			if(returnTypeBinding.getQualifiedName().equals("void")) {
				return true;
			}
		}
		return false;
	}

	private boolean isFieldUpdate(AbstractExpression expression) {
		Expression expr = expression.getExpression();
		return isField(expr) && isUpdated(expr);
	}

	private boolean isUpdated(Expression expr) {
		if(expr.getParent() instanceof Assignment) {
			Assignment assignment = (Assignment)expr.getParent();
			if(assignment.getLeftHandSide().equals(expr)) {
				return true;
			}
		}
		else if(expr.getParent() instanceof PostfixExpression) {
			return true;
		}
		else if(expr.getParent() instanceof PrefixExpression) {
			PrefixExpression prefix = (PrefixExpression)expr.getParent();
			if(prefix.getOperator().equals(PrefixExpression.Operator.INCREMENT) ||
					prefix.getOperator().equals(PrefixExpression.Operator.DECREMENT)) {
				return true;
			}
		}
		return false;
	}

	private boolean isField(Expression expr) {
		boolean expressionIsField = false;
		if(expr instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expr;
			if(simpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)simpleName.resolveBinding();
				expressionIsField = variableBinding.isField();
			}
		}
		else if(expr instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)expr;
			SimpleName simpleName = fieldAccess.getName();
			if(simpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)simpleName.resolveBinding();
				expressionIsField = variableBinding.isField();
			}
		}
		return expressionIsField;
	}
	//precondition: differences in expressions should be parameterizable
	private PreconditionViolationType isParameterizableExpression(PDG pdg, TreeSet<PDGNode> mappedNodes, AbstractExpression initialAbstractExpression, ICompilationUnit iCompilationUnit) {
		Set<VariableDeclaration> variableDeclarationsInMethod = pdg.getVariableDeclarationsInMethod();
		Expression initialExpression = initialAbstractExpression.getExpression();
		Expression expr = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(initialExpression);
		PDGExpression pdgExpression;
		if(!expr.equals(initialExpression)) {
			ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
			AbstractExpression tempExpression = new AbstractExpression(expr);
			pdgExpression = new PDGExpression(tempExpression, variableDeclarationsInMethod);
		}
		else {
			pdgExpression = new PDGExpression(initialAbstractExpression, variableDeclarationsInMethod);
		}
		boolean expressionIsVariableName = false;
		if(expr instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expr;
			if(simpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
				expressionIsVariableName = true;
			}
		}
		else if(expr instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)expr;
			SimpleName simpleName = fieldAccess.getName();
			if(fieldAccess.getExpression() instanceof ThisExpression &&
					simpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
				expressionIsVariableName = true;
			}
		}
		//find mapped node containing the expression
		PDGNode nodeContainingExpression = null;
		for(PDGNode node : mappedNodes) {
			if(isExpressionUnderStatement(expr, node.getASTStatement())) {
				nodeContainingExpression = node;
				break;
			}
		}
		if(nodeContainingExpression != null) {
			TreeSet<PDGNode> nodes = new TreeSet<PDGNode>(mappedNodes);
			nodes.remove(nodeContainingExpression);
			Iterator<GraphEdge> incomingDependenceIterator = nodeContainingExpression.getIncomingDependenceIterator();
			while(incomingDependenceIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)incomingDependenceIterator.next();
				if(dependence instanceof PDGAbstractDataDependence) {
					PDGAbstractDataDependence abstractDependence = (PDGAbstractDataDependence)dependence;
					PDGNode srcPDGNode = (PDGNode)abstractDependence.getSrc();
					if(nodes.contains(srcPDGNode) && !isAdvancedMatchNode(srcPDGNode, expr)) {
						if(dependence instanceof PDGDataDependence) {
							PDGDataDependence dataDependence = (PDGDataDependence)dependence;
							//check if pdgExpression is using dataDependence.data
							if(pdgExpression.usesLocalVariable(dataDependence.getData()))
								return PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED;
						}
						else if(dependence instanceof PDGAntiDependence) {
							PDGAntiDependence antiDependence = (PDGAntiDependence)dependence;
							//check if pdgExpression is defining dataDependence.data
							if(pdgExpression.definesLocalVariable(antiDependence.getData()))
								return PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED;
						}
						else if(dependence instanceof PDGOutputDependence) {
							PDGOutputDependence outputDependence = (PDGOutputDependence)dependence;
							//check if pdgExpression is defining dataDependence.data
							if(pdgExpression.definesLocalVariable(outputDependence.getData()))
								return PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED;
						}
					}
					//examine if it is a self-loop edge due to a loop-carried dependence
					if(srcPDGNode.equals(nodeContainingExpression)) {
						PDGNode loopNode = abstractDependence.getLoop().getPDGNode();
						if(abstractDependence.isLoopCarried() && nodes.contains(loopNode) && !isAdvancedMatchNode(loopNode, expr)) {
							if(pdgExpression.definesLocalVariable(abstractDependence.getData()) ||
									pdgExpression.usesLocalVariable(abstractDependence.getData())) {
								return PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED;
							}
						}
					}
				}
			}
			if(!expressionIsVariableName && controlParentExaminesVariableUsedInDifferenceExpression(pdgExpression, nodeContainingExpression, nodes)) {
				return PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED;
			}
			if(pdgExpression.throwsException()) {
				PDGBlockNode blockNode = pdg.isNestedWithinBlockNode(nodeContainingExpression);
				if(blockNode != null && blockNode instanceof PDGTryNode && mappedNodes.contains(blockNode)) {
					return PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_METHOD_CALL_THROWING_EXCEPTION_WITHIN_MATCHED_TRY_BLOCK;
				}
			}
		}
		else {
			//the expression is within the catch/finally blocks of a try statement
			for(PDGNode mappedNode : mappedNodes) {
				Iterator<AbstractVariable> definedVariableIterator = mappedNode.getDefinedVariableIterator();
				while(definedVariableIterator.hasNext()) {
					AbstractVariable definedVariable = definedVariableIterator.next();
					if(pdgExpression.usesLocalVariable(definedVariable) || pdgExpression.definesLocalVariable(definedVariable))
						return PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED;
				}
				Iterator<AbstractVariable> usedVariableIterator = mappedNode.getUsedVariableIterator();
				while(usedVariableIterator.hasNext()) {
					AbstractVariable usedVariable = usedVariableIterator.next();
					if(pdgExpression.definesLocalVariable(usedVariable))
						return PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED;
				}
			}
		}
		return null;
	}

	private boolean isAdvancedMatchNode(PDGNode node, Expression expressionToBeParameterized) {
		if(this.additionallyMatchedNodesG1.contains(node) || this.additionallyMatchedNodesG2.contains(node))
			return true;
		for(PDGNodeMapping nodeMapping : getMaximumStateWithMinimumDifferences().getNodeMappings()) {
			if(nodeMapping.isAdvancedMatch() && (nodeMapping.getNodeG1().equals(node) || nodeMapping.getNodeG2().equals(node))) {
				for(AbstractMethodFragment fragment1 : nodeMapping.getAdditionallyMatchedFragments1()) {
					if(fragment1 instanceof AbstractExpression) {
						AbstractExpression expression1 = (AbstractExpression)fragment1;
						if(isExpressionWithinExpression(expressionToBeParameterized, expression1.getExpression())) {
							return true;
						}
					}
					else if(fragment1 instanceof StatementObject) {
						StatementObject statement1 = (StatementObject)fragment1;
						if(isExpressionUnderStatement(expressionToBeParameterized, statement1.getStatement())) {
							return true;
						}
					}
				}
				for(AbstractMethodFragment fragment2 : nodeMapping.getAdditionallyMatchedFragments2()) {
					if(fragment2 instanceof AbstractExpression) {
						AbstractExpression expression2 = (AbstractExpression)fragment2;
						if(isExpressionWithinExpression(expressionToBeParameterized, expression2.getExpression())) {
							return true;
						}
					}
					else if(fragment2 instanceof StatementObject) {
						StatementObject statement2 = (StatementObject)fragment2;
						if(isExpressionUnderStatement(expressionToBeParameterized, statement2.getStatement())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isExpressionUnderStatement(ASTNode expression, Statement statement) {
		ASTNode parent = expression.getParent();
		if(parent.equals(statement))
			return true;
		if(!(parent instanceof Statement))
			return isExpressionUnderStatement(parent, statement);
		else
			return false;
	}
	
	private boolean isExpressionWithinExpression(ASTNode expression, Expression parentExpression) {
		if(expression.equals(parentExpression))
			return true;
		ASTNode parent = expression.getParent();
		if(!(parent instanceof Statement))
			return isExpressionWithinExpression(parent, parentExpression);
		else
			return false;
	}
	
	private IMethodBinding getMethodBinding(Expression expression) {
		if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.METHOD) {
				return (IMethodBinding)binding;
			}
		}
		else if(expression instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)expression;
			return methodInvocation.resolveMethodBinding();
		}
		else if(expression instanceof SuperMethodInvocation) {
			SuperMethodInvocation methodInvocation = (SuperMethodInvocation)expression;
			return methodInvocation.resolveMethodBinding();
		}
		return null;
	}
	
	public CloneType getCloneType() {
		if(getMaximumStateWithMinimumDifferences() != null) {
			int nodeDifferences = getNodeDifferences().size();
			if(nodeDifferences == 0 && nonMappedNodesG1.size() == 0 && nonMappedNodesG2.size() == 0) {
				return CloneType.TYPE_1;
			}
			if(nodeDifferences > 0 && nonMappedNodesG1.size() == 0 && nonMappedNodesG2.size() == 0) {
				return CloneType.TYPE_2;
			}
			if(nonMappedNodesG1.size() > 0 || nonMappedNodesG2.size() > 0) {
				if(isType3(getCloneStructureRoot())) {
					return CloneType.TYPE_3;
				}
				else {
					return CloneType.TYPE_2;
				}
			}
		}
		return CloneType.UNKNOWN;
	}
	
	private boolean isType3(CloneStructureNode node) {
		Map<Integer, PDGNodeGap> gapMap = new LinkedHashMap<Integer, PDGNodeGap>();
		int counter = 0;
		for(CloneStructureNode child : node.getChildren()) {
			if(child.getMapping() instanceof PDGNodeGap) {
				gapMap.put(counter, (PDGNodeGap)child.getMapping());
			}
			counter++;
		}
		if(!gapMap.isEmpty()) {
			int gaps1 = 0;
			int gaps2 = 0;
			for(Integer key : gapMap.keySet()) {
				PDGNodeGap nodeGap = gapMap.get(key);
				if(nodeGap.getNodeG1() != null) {
					gaps1++;
				}
				if(nodeGap.getNodeG2() != null) {
					gaps2++;
				}
			}
			if(gaps1 != gaps2) {
				return true;
			}
		}
		for(CloneStructureNode child : node.getChildren()) {
			boolean type3 = isType3(child);
			if(type3) {
				return true;
			}
		}
		return false;
	}
}
