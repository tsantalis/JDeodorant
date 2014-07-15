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

public class StatementCollector extends ASTVisitor {
	private List<ASTNode> controlStatementList = new ArrayList<ASTNode>();
	private List<ASTNode> statementList = new ArrayList<ASTNode>();

	public List<ASTNode> getStatementList() {
		return statementList;
	}
	
	public List<ASTNode> getControlStatementList() {
		return controlStatementList;
	}

	public boolean visit(AnonymousClassDeclaration node) {
		return true;
	}
	
	public boolean visit(DoStatement node) {
		controlStatementList.add(node);
		statementList.add(node);
		return true;
	}

	public boolean visit(EnhancedForStatement node) {
		controlStatementList.add(node);
		statementList.add(node);
		return true;
	}

	public boolean visit(ForStatement node) {
		controlStatementList.add(node);
		statementList.add(node);
		return true;
	}

	public boolean visit(IfStatement node) {
		controlStatementList.add(node);
		statementList.add(node);
		return true;
	}

	public boolean visit(SwitchStatement node) {
		controlStatementList.add(node);
		statementList.add(node);
		return true;
	}

	public boolean visit(TryStatement node) {
		controlStatementList.add(node);
		statementList.add(node);
		return true;
	}

	public boolean visit(WhileStatement node) {
		controlStatementList.add(node);
		statementList.add(node);
		return true;
	}

	public boolean visit(AssertStatement node) {
		statementList.add(node);
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
		statementList.add(node);
		return false;
	}

	public boolean visit(CatchClause node) {
		return false;
	}

	public boolean visit(ConstructorInvocation node) {
		statementList.add(node);
		return false;
	}
	
	public boolean visit(ContinueStatement node) {
		statementList.add(node);
		return false;
	}
	
	public boolean visit(EmptyStatement node) {
		statementList.add(node);
		return false;
	}
	
	public boolean visit(ExpressionStatement node) {
		statementList.add(node);
		return false;
	}

	public boolean visit(LabeledStatement node) {
		statementList.add(node);
		return true;
	}

	public boolean visit(ReturnStatement node) {
		statementList.add(node);
		return false;
	}

	public boolean visit(SuperConstructorInvocation node) {
		statementList.add(node);
		return false;
	}

	public boolean visit(SwitchCase node) {
		statementList.add(node);
		return false;
	}
	
	public boolean visit(SynchronizedStatement node) {
		statementList.add(node);
		controlStatementList.add(node);
		return true;
	}

	public boolean visit(ThrowStatement node) {
		statementList.add(node);
		return false;
	}

	public boolean visit(TypeDeclarationStatement node) {
		statementList.add(node);
		return false;
	}

	public boolean visit(VariableDeclarationStatement node) {
		statementList.add(node);
		return false;
	}
}
