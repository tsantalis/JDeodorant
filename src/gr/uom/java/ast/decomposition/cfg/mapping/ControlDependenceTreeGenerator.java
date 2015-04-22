package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.util.ExpressionExtractor;

public class ControlDependenceTreeGenerator {
	private PDG pdg;
	private ControlDependenceTreeNode root;

	public ControlDependenceTreeGenerator(PDG pdg) {
		this.pdg = pdg;
		//construct CDT from method entry node
		this.root = new ControlDependenceTreeNode(null, pdg.getEntryNode());
		processControlDependences(root);
	}

	public ControlDependenceTreeNode getRoot() {
		return root;
	}

	private void processControlDependences(ControlDependenceTreeNode cdtNode) {
		Set<PDGNode> controlDependentNodes;
		if(cdtNode.getNode() instanceof PDGBlockNode) {
			controlDependentNodes = pdg.getNestedNodesWithinBlockNode((PDGBlockNode)cdtNode.getNode());
		}
		else {
			controlDependentNodes = cdtNode.getNode().getControlDependentNodes();
		}
		for(PDGNode dstNode : controlDependentNodes) {
			if(dstNode instanceof PDGControlPredicateNode) {
				//special handling for symmetrical if statements
				PDGNode nodeControlParent = dstNode.getControlDependenceParent();
				PDGControlDependence nodeIncomingControlDependence = dstNode.getIncomingControlDependence();
				if(nodeControlParent.getCFGNode() instanceof CFGBranchIfNode && nodeIncomingControlDependence.isFalseControlDependence() &&
						numberOfOutgoingFalseControlDependences(nodeControlParent) == 1 &&
						dstNode.getASTStatement().getParent() instanceof IfStatement &&
						dstNode.getASTStatement() instanceof IfStatement) {
					//a case of "if/else if" -> add as a sibling, not as a child
					PDGBlockNode tryNode = pdg.isDirectlyNestedWithinBlockNode(cdtNode.getNode());
					if(tryNode != null) {
						ControlDependenceTreeNode treeNode = searchForNode(tryNode);
						if(treeNode == null) {
							treeNode = new ControlDependenceTreeNode(cdtNode.getParent(), tryNode);
						}
						ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(treeNode, dstNode);
						ControlDependenceTreeNode ifParent = searchForNode(nodeControlParent);
						ifParent.setElseIfChild(tmp);
						tmp.setIfParent(ifParent);
						processControlDependences(tmp);
					}
					else {
						ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(cdtNode.getParent(), dstNode);
						ControlDependenceTreeNode ifParent = searchForNode(nodeControlParent);
						ifParent.setElseIfChild(tmp);
						tmp.setIfParent(ifParent);
						processControlDependences(tmp);
					}
				}
				else {
					PDGBlockNode tryNode = pdg.isDirectlyNestedWithinBlockNode(dstNode);
					if(tryNode != null) {
						ControlDependenceTreeNode treeNode = searchForNode(tryNode);
						if(treeNode == null) {
							treeNode = checkIfTryNodeIsNestedUnderOtherTryNodeOrElse(cdtNode, dstNode, tryNode);	
						}
						ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(treeNode, dstNode);
						processControlDependences(tmp);
					}
					else {
						//check if dstNode is under an else clause and create a "fake else" node
						if(nodeIncomingControlDependence.isFalseControlDependence()) {
							//search if the "fake else" node is already created
							ControlDependenceTreeNode fakeElse = searchForElseNode(nodeControlParent);
							if(fakeElse == null) {
								fakeElse = new ControlDependenceTreeNode(cdtNode.getParent(), null);
								fakeElse.setElseNode(true);
								ControlDependenceTreeNode ifParent = searchForNode(nodeControlParent);
								ifParent.setElseIfChild(fakeElse);
								fakeElse.setIfParent(ifParent);
							}
							ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(fakeElse, dstNode);
							processControlDependences(tmp);
						}
						else {
							ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(cdtNode, dstNode);
							processControlDependences(tmp);
						}
					}
				}
			}
			else {
				//dstNode is not a control predicate
				boolean dstNodeIsTernaryOperator = false;
				if(isExpressionStatementWithConditionalExpression(dstNode)) {
					dstNodeIsTernaryOperator = true;
				}
				//first check if the dstNode is nested under a try block
				PDGBlockNode tryNode = pdg.isDirectlyNestedWithinBlockNode(dstNode);
				if(tryNode != null) {
					ControlDependenceTreeNode treeNode = searchForNode(tryNode);
					if(treeNode == null) {
						treeNode = checkIfTryNodeIsNestedUnderOtherTryNodeOrElse(cdtNode, dstNode, tryNode);
					}
					if(dstNodeIsTernaryOperator) {
						ControlDependenceTreeNode ternary = new ControlDependenceTreeNode(treeNode, dstNode);
						ternary.setTernary(true);
					}
				}
				else {
					//dstNode is not nested under a try node, but is nested under an else node
					if(dstNode.getIncomingControlDependence().isFalseControlDependence()) {
						PDGNode nodeControlParent = dstNode.getControlDependenceParent();
						ControlDependenceTreeNode fakeElse = searchForElseNode(nodeControlParent);
						if(fakeElse == null) {
							fakeElse = new ControlDependenceTreeNode(cdtNode.getParent(), null);
							fakeElse.setElseNode(true);
							ControlDependenceTreeNode ifParent = searchForNode(nodeControlParent);
							ifParent.setElseIfChild(fakeElse);
							fakeElse.setIfParent(ifParent);
						}
						if(dstNodeIsTernaryOperator) {
							ControlDependenceTreeNode ternary = new ControlDependenceTreeNode(fakeElse, dstNode);
							ternary.setTernary(true);
						}
					}
					else {
						if(dstNodeIsTernaryOperator) {
							ControlDependenceTreeNode ternary = new ControlDependenceTreeNode(cdtNode, dstNode);
							ternary.setTernary(true);
						}
					}
				}
			}
		}
	}

	private ControlDependenceTreeNode checkIfTryNodeIsNestedUnderOtherTryNodeOrElse(ControlDependenceTreeNode cdtNode, PDGNode dstNode, PDGBlockNode tryNode) {
		//check if tryNode is nested inside another tryNode
		PDGBlockNode otherTryNode = pdg.isDirectlyNestedWithinBlockNode(tryNode);
		if(otherTryNode != null) {
			ControlDependenceTreeNode otherTreeNode = searchForNode(otherTryNode);
			if(otherTreeNode != null)
				return new ControlDependenceTreeNode(otherTreeNode, tryNode);
			else {
				//return new ControlDependenceTreeNode(cdtNode, tryNode);
				return new ControlDependenceTreeNode(
						checkIfTryNodeIsNestedUnderOtherTryNodeOrElse(cdtNode, dstNode, otherTryNode), tryNode);
			}
		}
		//check if tryNode is nested under an else clause
		else if(dstNode.getIncomingControlDependence().isFalseControlDependence()) {
			PDGNode nodeControlParent = dstNode.getControlDependenceParent();
			ControlDependenceTreeNode fakeElse = searchForElseNode(nodeControlParent);
			if(fakeElse == null) {
				fakeElse = new ControlDependenceTreeNode(cdtNode.getParent(), null);
				fakeElse.setElseNode(true);
				ControlDependenceTreeNode ifParent = searchForNode(nodeControlParent);
				ifParent.setElseIfChild(fakeElse);
				fakeElse.setIfParent(ifParent);
			}
			return new ControlDependenceTreeNode(fakeElse, tryNode);
		}
		else {
			return new ControlDependenceTreeNode(cdtNode, tryNode);
		}
	}

	private ControlDependenceTreeNode searchForNode(PDGNode node) {
		return root.getNode(node);
	}

	private ControlDependenceTreeNode searchForElseNode(PDGNode ifParent) {
		return root.getElseNode(ifParent);
	}

	private int numberOfOutgoingFalseControlDependences(PDGNode pdgNode) {
		int count = 0;
		Iterator<GraphEdge> edgeIterator = pdgNode.getOutgoingDependenceIterator();
		while(edgeIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)edgeIterator.next();
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				if(controlDependence.isFalseControlDependence())
					count++;
			}
		}
		return count;
	}

	private boolean isExpressionStatementWithConditionalExpression(PDGNode node) {
		Statement statement = node.getASTStatement();
		if(statement instanceof ExpressionStatement) {
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			List<Expression> conditionalExpressions = expressionExtractor.getConditionalExpressions(statement);
			if(conditionalExpressions.size() == 1) {
				ConditionalExpression conditional = (ConditionalExpression)conditionalExpressions.get(0);
				ASTNode parent = conditional.getParent();
				ASTNode grandParent = parent.getParent();
				if(grandParent != null && grandParent.equals(statement)) {
					return true;
				}
				if(parent instanceof ParenthesizedExpression) {
					if(grandParent != null) {
						if(grandParent.getParent() != null && grandParent.getParent().equals(statement)) {
							return true;
						}
					}
				}
			}
		}
		else if (statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			if (returnStatement.getExpression() instanceof ConditionalExpression) {
				return true;
			}
		}
		return false;
	}
}
