package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGSlice;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jface.text.Position;

public class ASTSlice {
	private TypeDeclaration sourceTypeDeclaration;
	private MethodDeclaration sourceMethodDeclaration;
	private Set<PDGNode> sliceNodes;
	private Set<Statement> sliceStatements;
	private Statement statementCriterion;
	private VariableDeclaration localVariableCriterion;
	private Set<VariableDeclaration> passedParameters;
	private String extractedMethodName;
	
	public ASTSlice(PDGSlice pdgSlice) {
		this.sourceMethodDeclaration = pdgSlice.getMethod().getMethodDeclaration();
		this.sourceTypeDeclaration = (TypeDeclaration)sourceMethodDeclaration.getParent();
		this.sliceNodes = pdgSlice.getSliceNodes();
		this.sliceStatements = new LinkedHashSet<Statement>();
		for(PDGNode node : sliceNodes) {
			sliceStatements.add(node.getASTStatement());
		}
		this.statementCriterion = pdgSlice.getNodeCriterion().getASTStatement();
		this.localVariableCriterion = pdgSlice.getLocalVariableCriterion();
		this.passedParameters = pdgSlice.getPassedParameters();
		this.extractedMethodName = localVariableCriterion.getName().getIdentifier();
	}
	
	public TypeDeclaration getSourceTypeDeclaration() {
		return sourceTypeDeclaration;
	}

	public MethodDeclaration getSourceMethodDeclaration() {
		return sourceMethodDeclaration;
	}

	public Statement getStatementCriterion() {
		return statementCriterion;
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

	public String getExtractedMethodName() {
		return extractedMethodName;
	}

	public void setExtractedMethodName(String extractedMethodName) {
		this.extractedMethodName = extractedMethodName;
	}

	public List<Position> getHighlightPositions() {
		List<Position> positions = new ArrayList<Position>();
		for(Statement statement : sliceStatements) {
			if(statement instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement)statement;
				Expression ifExpression = ifStatement.getExpression();
				Position position = new Position(ifExpression.getStartPosition(), ifExpression.getLength());
				positions.add(position);
				
			}
			else if(statement instanceof WhileStatement) {
				WhileStatement whileStatement = (WhileStatement)statement;
				Expression whileExpression = whileStatement.getExpression();
				Position position = new Position(whileExpression.getStartPosition(), whileExpression.getLength());
				positions.add(position);
			}
			else if(statement instanceof ForStatement) {
				ForStatement forStatement = (ForStatement)statement;
				List<Expression> initializers = forStatement.initializers();
				for(Expression expression : initializers) {
					Position initializerPosition = new Position(expression.getStartPosition(), expression.getLength());
					positions.add(initializerPosition);
				}
				Expression forExpression = forStatement.getExpression();
				Position position = new Position(forExpression.getStartPosition(), forExpression.getLength());
				positions.add(position);
				List<Expression> updaters = forStatement.updaters();
				for(Expression expression : updaters) {
					Position updaterPosition = new Position(expression.getStartPosition(), expression.getLength());
					positions.add(updaterPosition);
				}
			}
			else if(statement instanceof EnhancedForStatement) {
				EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
				SingleVariableDeclaration parameter = enhancedForStatement.getParameter();
				Position parameterPosition = new Position(parameter.getStartPosition(), parameter.getLength());
				positions.add(parameterPosition);
				Expression expression = enhancedForStatement.getExpression();
				Position expressionPosition = new Position(expression.getStartPosition(), expression.getLength());
				positions.add(expressionPosition);
			}
			else if(statement instanceof DoStatement) {
				DoStatement doStatement = (DoStatement)statement;
				Expression doExpression = doStatement.getExpression();
				Position position = new Position(doExpression.getStartPosition(), doExpression.getLength());
				positions.add(position);
			}
			else {
				Position position = new Position(statement.getStartPosition(), statement.getLength());
				positions.add(position);
			}
		}
		return positions;
	}
}
