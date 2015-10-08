package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class ExtractStatementsVisitor extends ASTVisitor {
	private List<ASTNode> controlStatementsList = new ArrayList<ASTNode>();
	private List<ASTNode> statementsList = new ArrayList<ASTNode>();
	private ASTNode startNode;

	public ExtractStatementsVisitor(ASTNode node) {
		this.startNode = node;
	}

	public List<ASTNode> getStatementsList() {
		return statementsList;
	}
	
	public List<ASTNode> getControlStatementsList() {
		return controlStatementsList;
	}

	public boolean visit(AnonymousClassDeclaration node) {
		//check if start node is inside the AnonymousClassDeclaration
		if(isStartNodeNestedUnderAnonymousClassDeclaration(node))
			return true;
		return false;
	}
	
	private boolean isStartNodeNestedUnderAnonymousClassDeclaration(AnonymousClassDeclaration node) {
		ASTNode parent = startNode.getParent();
		while(parent != null) {
			if(parent.equals(node)) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}
	
	public boolean visit(DoStatement node) {
		controlStatementsList.add(node);
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(EnhancedForStatement node) {
		controlStatementsList.add(node);
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(ForStatement node) {
		controlStatementsList.add(node);
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(IfStatement node) {
		controlStatementsList.add(node);
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(SwitchStatement node) {
		controlStatementsList.add(node);
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(TryStatement node) {
		controlStatementsList.add(node);
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(WhileStatement node) {
		controlStatementsList.add(node);
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(AssertStatement node) {
		statementsList.add(node);
		
		return false;
	}

	public boolean visit(Block node) {
		if(node.getParent() instanceof TryStatement) {
			TryStatement statement = (TryStatement) node.getParent();
			Block finallyBlock = statement.getFinally();
			if(finallyBlock != null && finallyBlock.equals(node))
				return false;
		}
		return true;
	}

	public boolean visit(BreakStatement node) {
		statementsList.add(node);
		
		return false;
	}

	public boolean visit(CatchClause node) {
		return false;
	}

	public boolean visit(ConstructorInvocation node) {
		statementsList.add(node);
		
		return false;
	}
	
	public boolean visit(ContinueStatement node) {
		statementsList.add(node);
		
		return false;
	}
	
	public boolean visit(EmptyStatement node) {
		statementsList.add(node);
		
		return false;
	}
	
	public boolean visit(ExpressionStatement node) {
		statementsList.add(node);
		
		return false;
	}

	public boolean visit(LabeledStatement node) {
		statementsList.add(node);
		
		return true;
	}

	public boolean visit(ReturnStatement node) {
		statementsList.add(node);
		
		return false;
	}

	public boolean visit(SuperConstructorInvocation node) {
		statementsList.add(node);
		
		return false;
	}

	public boolean visit(SwitchCase node) {
		statementsList.add(node);
		
		return false;
	}
	
	public boolean visit(SynchronizedStatement node) {
		statementsList.add(node);
		controlStatementsList.add(node);
		
		return true;
	}

	public boolean visit(ThrowStatement node) {
		statementsList.add(node);
		
		return false;
	}

	public boolean visit(TypeDeclarationStatement node) {
		statementsList.add(node);
		
		return false;
	}

	public boolean visit(VariableDeclarationStatement node) {
		statementsList.add(node);
		
		return false;
	}
}
