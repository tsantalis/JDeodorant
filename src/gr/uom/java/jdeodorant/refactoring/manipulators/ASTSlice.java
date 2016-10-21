package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.cfg.BasicBlock;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGObjectSliceUnion;
import gr.uom.java.ast.decomposition.cfg.PDGSlice;
import gr.uom.java.ast.decomposition.cfg.PDGSliceUnion;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jface.text.Position;

public class ASTSlice {
	private TypeDeclaration sourceTypeDeclaration;
	private MethodDeclaration sourceMethodDeclaration;
	private Set<PDGNode> sliceNodes;
	private Set<Statement> sliceStatements;
	private Set<Statement> removableStatements;
	private Set<Statement> duplicatedStatements;
	private VariableDeclaration localVariableCriterion;
	private Set<VariableDeclaration> passedParameters;
	private Statement variableCriterionDeclarationStatement;
	private Statement extractedMethodInvocationInsertionStatement;
	private String extractedMethodName;
	private boolean declarationOfVariableCriterionBelongsToSliceNodes;
	private boolean declarationOfVariableCriterionBelongsToRemovableNodes;
	private IFile iFile;
	private BasicBlock boundaryBlock;
	private boolean isObjectSlice;
	private int methodSize;
	private Integer userRate;
	
	public ASTSlice(PDGSlice pdgSlice) {
		this.sourceMethodDeclaration = pdgSlice.getMethod().getMethodDeclaration();
		this.sourceTypeDeclaration = (TypeDeclaration)sourceMethodDeclaration.getParent();
		this.sliceNodes = pdgSlice.getSliceNodes();
		this.sliceStatements = new LinkedHashSet<Statement>();
		for(PDGNode node : sliceNodes) {
			sliceStatements.add(node.getASTStatement());
		}
		this.removableStatements = new LinkedHashSet<Statement>();
		for(PDGNode node : pdgSlice.getRemovableNodes()) {
			removableStatements.add(node.getASTStatement());
		}
		this.duplicatedStatements = new LinkedHashSet<Statement>(sliceStatements);
		this.duplicatedStatements.removeAll(removableStatements);
		Set<VariableDeclaration> variableDeclarationsAndAccessedFields = pdgSlice.getVariableDeclarationsAndAccessedFieldsInMethod();
		AbstractVariable criterion = pdgSlice.getLocalVariableCriterion();
		for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFields) {
			if(variableDeclaration.resolveBinding().getKey().equals(criterion.getVariableBindingKey())) {
				this.localVariableCriterion = variableDeclaration;
				break;
			}
		}
		this.passedParameters = new LinkedHashSet<VariableDeclaration>();
		for(AbstractVariable variable : pdgSlice.getPassedParameters()) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFields) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable.getVariableBindingKey())) {
					passedParameters.add(variableDeclaration);
					break;
				}
			}
		}
		PDGNode declarationOfVariableCriterionNode = pdgSlice.getDeclarationOfVariableCriterion();
		if(declarationOfVariableCriterionNode != null)
			this.variableCriterionDeclarationStatement = declarationOfVariableCriterionNode.getASTStatement();
		this.extractedMethodInvocationInsertionStatement = pdgSlice.getExtractedMethodInvocationInsertionNode().getASTStatement();
		this.extractedMethodName = localVariableCriterion.getName().getIdentifier();
		this.declarationOfVariableCriterionBelongsToSliceNodes = pdgSlice.declarationOfVariableCriterionBelongsToSliceNodes();
		this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgSlice.declarationOfVariableCriterionBelongsToRemovableNodes();
		this.iFile = pdgSlice.getIFile();
		this.boundaryBlock = pdgSlice.getBoundaryBlock();
		this.isObjectSlice = false;
		this.methodSize = pdgSlice.getMethodSize();
	}

	public ASTSlice(PDGSliceUnion pdgSliceUnion) {
		this.sourceMethodDeclaration = pdgSliceUnion.getMethod().getMethodDeclaration();
		this.sourceTypeDeclaration = (TypeDeclaration)sourceMethodDeclaration.getParent();
		this.sliceNodes = pdgSliceUnion.getSliceNodes();
		this.sliceStatements = new LinkedHashSet<Statement>();
		for(PDGNode node : sliceNodes) {
			sliceStatements.add(node.getASTStatement());
		}
		this.removableStatements = new LinkedHashSet<Statement>();
		for(PDGNode node : pdgSliceUnion.getRemovableNodes()) {
			removableStatements.add(node.getASTStatement());
		}
		this.duplicatedStatements = new LinkedHashSet<Statement>(sliceStatements);
		this.duplicatedStatements.removeAll(removableStatements);
		Set<VariableDeclaration> variableDeclarationsAndAccessedFields = pdgSliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod();
		AbstractVariable criterion = pdgSliceUnion.getLocalVariableCriterion();
		for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFields) {
			if(variableDeclaration.resolveBinding().getKey().equals(criterion.getVariableBindingKey())) {
				this.localVariableCriterion = variableDeclaration;
				break;
			}
		}
		this.passedParameters = new LinkedHashSet<VariableDeclaration>();
		for(AbstractVariable variable : pdgSliceUnion.getPassedParameters()) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFields) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable.getVariableBindingKey())) {
					passedParameters.add(variableDeclaration);
					break;
				}
			}
		}
		PDGNode declarationOfVariableCriterionNode = pdgSliceUnion.getDeclarationOfVariableCriterion();
		if(declarationOfVariableCriterionNode != null)
			this.variableCriterionDeclarationStatement = declarationOfVariableCriterionNode.getASTStatement();
		this.extractedMethodInvocationInsertionStatement = pdgSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement();
		this.extractedMethodName = localVariableCriterion.getName().getIdentifier();
		this.declarationOfVariableCriterionBelongsToSliceNodes = pdgSliceUnion.declarationOfVariableCriterionBelongsToSliceNodes();
		this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgSliceUnion.declarationOfVariableCriterionBelongsToRemovableNodes();
		this.iFile = pdgSliceUnion.getIFile();
		this.boundaryBlock = pdgSliceUnion.getBoundaryBlock();
		this.isObjectSlice = false;
		this.methodSize = pdgSliceUnion.getMethodSize();
	}

	public ASTSlice(PDGObjectSliceUnion pdgObjectSliceUnion) {
		this.sourceMethodDeclaration = pdgObjectSliceUnion.getMethod().getMethodDeclaration();
		this.sourceTypeDeclaration = (TypeDeclaration)sourceMethodDeclaration.getParent();
		this.sliceNodes = pdgObjectSliceUnion.getSliceNodes();
		this.sliceStatements = new LinkedHashSet<Statement>();
		for(PDGNode node : sliceNodes) {
			sliceStatements.add(node.getASTStatement());
		}
		this.removableStatements = new LinkedHashSet<Statement>();
		for(PDGNode node : pdgObjectSliceUnion.getRemovableNodes()) {
			removableStatements.add(node.getASTStatement());
		}
		this.duplicatedStatements = new LinkedHashSet<Statement>(sliceStatements);
		this.duplicatedStatements.removeAll(removableStatements);
		Set<VariableDeclaration> variableDeclarationsAndAccessedFields = pdgObjectSliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod();
		AbstractVariable criterion = pdgObjectSliceUnion.getObjectReference();
		for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFields) {
			if(variableDeclaration.resolveBinding().getKey().equals(criterion.getVariableBindingKey())) {
				this.localVariableCriterion = variableDeclaration;
				break;
			}
		}
		this.passedParameters = new LinkedHashSet<VariableDeclaration>();
		for(AbstractVariable variable : pdgObjectSliceUnion.getPassedParameters()) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFields) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable.getVariableBindingKey())) {
					passedParameters.add(variableDeclaration);
					break;
				}
			}
		}
		PDGNode declarationOfObjectReferenceNode = pdgObjectSliceUnion.getDeclarationOfObjectReference();
		if(declarationOfObjectReferenceNode != null)
			this.variableCriterionDeclarationStatement = declarationOfObjectReferenceNode.getASTStatement();
		this.extractedMethodInvocationInsertionStatement = pdgObjectSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement();
		this.extractedMethodName = localVariableCriterion.getName().getIdentifier();
		this.declarationOfVariableCriterionBelongsToSliceNodes = pdgObjectSliceUnion.declarationOfObjectReferenceBelongsToSliceNodes();
		this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgObjectSliceUnion.declarationOfObjectReferenceBelongsToRemovableNodes();
		this.iFile = pdgObjectSliceUnion.getIFile();
		this.boundaryBlock = pdgObjectSliceUnion.getBoundaryBlock();
		this.isObjectSlice = true;
		this.methodSize = pdgObjectSliceUnion.getMethodSize();
	}

	public boolean isVariableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement() {
		Statement variableCriterionDeclarationStatement = getVariableCriterionDeclarationStatement();
		if(variableCriterionDeclarationStatement != null) {
			int depthOfNestingForVariableCriterionDeclarationStatement = depthOfNesting(variableCriterionDeclarationStatement);
			Statement extractedMethodInvocationInsertionStatement = getExtractedMethodInvocationInsertionStatement();
			int depthOfNestingForExtractedMethodInvocationInsertionStatement = depthOfNesting(extractedMethodInvocationInsertionStatement);
			if(depthOfNestingForVariableCriterionDeclarationStatement > depthOfNestingForExtractedMethodInvocationInsertionStatement)
				return true;
			if(depthOfNestingForVariableCriterionDeclarationStatement == depthOfNestingForExtractedMethodInvocationInsertionStatement &&
					variableCriterionDeclarationStatement instanceof TryStatement)
				return true;
		}
		return false;
	}

	private int depthOfNesting(Statement statement) {
		int depthOfNesting = 0;
		ASTNode parent = statement;
		while(!(parent instanceof MethodDeclaration)) {
			depthOfNesting++;
			parent = parent.getParent();
		}
		return depthOfNesting;
	}

	public TypeDeclaration getSourceTypeDeclaration() {
		return sourceTypeDeclaration;
	}

	public MethodDeclaration getSourceMethodDeclaration() {
		return sourceMethodDeclaration;
	}

	public VariableDeclaration getLocalVariableCriterion() {
		return localVariableCriterion;
	}

	public Set<VariableDeclaration> getPassedParameters() {
		return passedParameters;
	}

	public Set<PDGNode> getSliceNodes() {
		return sliceNodes;
	}

	public Set<Statement> getSliceStatements() {
		return sliceStatements;
	}

	public Set<Statement> getRemovableStatements() {
		return removableStatements;
	}

	public Statement getVariableCriterionDeclarationStatement() {
		return variableCriterionDeclarationStatement;
	}

	public Statement getExtractedMethodInvocationInsertionStatement() {
		return extractedMethodInvocationInsertionStatement;
	}

	public String getExtractedMethodName() {
		return extractedMethodName;
	}

	public void setExtractedMethodName(String extractedMethodName) {
		this.extractedMethodName = extractedMethodName;
	}

	public boolean declarationOfVariableCriterionBelongsToSliceNodes() {
		return declarationOfVariableCriterionBelongsToSliceNodes;
	}

	public boolean declarationOfVariableCriterionBelongsToRemovableNodes() {
		return declarationOfVariableCriterionBelongsToRemovableNodes;
	}

	public IFile getIFile() {
		return iFile;
	}

	public BasicBlock getBoundaryBlock() {
		return boundaryBlock;
	}

	public boolean isObjectSlice() {
		return isObjectSlice;
	}

	public int getMethodSize() {
		return methodSize;
	}

	public Object[] getHighlightPositions() {
		Map<Position, String> annotationMap = new LinkedHashMap<Position, String>();
		Map<Position, Boolean> duplicationMap = new LinkedHashMap<Position, Boolean>();
		List<PDGNode> sliceNodeList = new ArrayList<PDGNode>(sliceNodes);
		int i = 0;
		for(Statement statement : sliceStatements) {
			PDGNode sliceNode = sliceNodeList.get(i);
			if(statement instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement)statement;
				Expression ifExpression = ifStatement.getExpression();
				Position position = new Position(ifExpression.getStartPosition(), ifExpression.getLength());
				annotationMap.put(position, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(position, true);
				else
					duplicationMap.put(position, false);
			}
			else if(statement instanceof WhileStatement) {
				WhileStatement whileStatement = (WhileStatement)statement;
				Expression whileExpression = whileStatement.getExpression();
				Position position = new Position(whileExpression.getStartPosition(), whileExpression.getLength());
				annotationMap.put(position, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(position, true);
				else
					duplicationMap.put(position, false);
			}
			else if(statement instanceof ForStatement) {
				ForStatement forStatement = (ForStatement)statement;
				List<Expression> initializers = forStatement.initializers();
				for(Expression expression : initializers) {
					Position initializerPosition = new Position(expression.getStartPosition(), expression.getLength());
					annotationMap.put(initializerPosition, sliceNode.getAnnotation());
					if(duplicatedStatements.contains(statement))
						duplicationMap.put(initializerPosition, true);
					else
						duplicationMap.put(initializerPosition, false);
				}
				Expression forExpression = forStatement.getExpression();
				if(forExpression != null) {
					Position position = new Position(forExpression.getStartPosition(), forExpression.getLength());
					annotationMap.put(position, sliceNode.getAnnotation());
					if(duplicatedStatements.contains(statement))
						duplicationMap.put(position, true);
					else
						duplicationMap.put(position, false);
				}
				List<Expression> updaters = forStatement.updaters();
				for(Expression expression : updaters) {
					Position updaterPosition = new Position(expression.getStartPosition(), expression.getLength());
					annotationMap.put(updaterPosition, sliceNode.getAnnotation());
					if(duplicatedStatements.contains(statement))
						duplicationMap.put(updaterPosition, true);
					else
						duplicationMap.put(updaterPosition, false);
				}
			}
			else if(statement instanceof EnhancedForStatement) {
				EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
				SingleVariableDeclaration parameter = enhancedForStatement.getParameter();
				Position parameterPosition = new Position(parameter.getStartPosition(), parameter.getLength());
				annotationMap.put(parameterPosition, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(parameterPosition, true);
				else
					duplicationMap.put(parameterPosition, false);
				Expression expression = enhancedForStatement.getExpression();
				Position expressionPosition = new Position(expression.getStartPosition(), expression.getLength());
				annotationMap.put(expressionPosition, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(expressionPosition, true);
				else
					duplicationMap.put(expressionPosition, false);
			}
			else if(statement instanceof DoStatement) {
				DoStatement doStatement = (DoStatement)statement;
				Expression doExpression = doStatement.getExpression();
				Position position = new Position(doExpression.getStartPosition(), doExpression.getLength());
				annotationMap.put(position, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(position, true);
				else
					duplicationMap.put(position, false);
			}
			else if(statement instanceof SwitchStatement) {
				SwitchStatement switchStatement = (SwitchStatement)statement;
				Expression switchExpression = switchStatement.getExpression();
				Position position = new Position(switchExpression.getStartPosition(), switchExpression.getLength());
				annotationMap.put(position, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(position, true);
				else
					duplicationMap.put(position, false);
			}
			else if(statement instanceof TryStatement) {
				TryStatement tryStatement = (TryStatement)statement;
				Position position = new Position(tryStatement.getStartPosition(), 3);
				annotationMap.put(position, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(position, true);
				else
					duplicationMap.put(position, false);
				List<VariableDeclarationExpression> resources = tryStatement.resources();
				for(VariableDeclarationExpression expression : resources) {
					Position resourcePosition = new Position(expression.getStartPosition(), expression.getLength());
					annotationMap.put(resourcePosition, sliceNode.getAnnotation());
					if(duplicatedStatements.contains(statement))
						duplicationMap.put(resourcePosition, true);
					else
						duplicationMap.put(resourcePosition, false);
				}
			}
			else {
				Position position = new Position(statement.getStartPosition(), statement.getLength());
				annotationMap.put(position, sliceNode.getAnnotation());
				if(duplicatedStatements.contains(statement))
					duplicationMap.put(position, true);
				else
					duplicationMap.put(position, false);
			}
			i++;
		}
		return new Object[] {annotationMap, duplicationMap};
	}

	public String sliceToString() {
		StringBuilder sb = new StringBuilder();
		for(PDGNode sliceNode : sliceNodes) {
			sb.append(sliceNode.getStatement().toString());
		}
		return sb.toString();
	}

	public String toString() {
		int numberOfSliceStatements = getNumberOfSliceStatements();
		int numberOfDuplicatedStatements = getNumberOfDuplicatedStatements();
		return getSourceTypeDeclaration().resolveBinding().getQualifiedName() + "\t" +
		getSourceMethodDeclaration().resolveBinding().toString() + "\t" +
		getLocalVariableCriterion().getName().getIdentifier() + "\t" +
		"B" + getBoundaryBlock().getId() + "\t" +
		numberOfDuplicatedStatements + "/" + numberOfSliceStatements;
	}

	public int getNumberOfSliceStatements() {
		return getSliceStatements().size();
	}

	public int getNumberOfDuplicatedStatements() {
		int numberOfSliceStatements = getNumberOfSliceStatements();
		int numberOfRemovableStatements = getRemovableStatements().size();
		int numberOfDuplicatedStatements = numberOfSliceStatements - numberOfRemovableStatements;
		return numberOfDuplicatedStatements;
	}

	public Integer getUserRate() {
		return userRate;
	}

	public void setUserRate(Integer userRate) {
		this.userRate = userRate;
	}
}
