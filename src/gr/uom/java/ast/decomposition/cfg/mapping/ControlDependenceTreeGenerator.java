package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.core.dom.IfStatement;

import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;

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
		if(cdtNode.getNode() instanceof PDGTryNode) {
			controlDependentNodes = pdg.getNestedNodesWithinTryNode((PDGTryNode)cdtNode.getNode());
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
					PDGTryNode tryNode = pdg.isDirectlyNestedWithinTryNode(cdtNode.getNode());
					if(tryNode != null) {
						ControlDependenceTreeNode treeNode = searchForNode(tryNode);
						if(treeNode == null) {
							treeNode = new ControlDependenceTreeNode(cdtNode.getParent(), tryNode);
							ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(treeNode, dstNode);
							ControlDependenceTreeNode ifParent = searchForNode(nodeControlParent);
							ifParent.setElseIfChild(tmp);
							tmp.setIfParent(ifParent);
							processControlDependences(tmp);
						}
						else {
							ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(treeNode, dstNode);
							ControlDependenceTreeNode ifParent = searchForNode(nodeControlParent);
							ifParent.setElseIfChild(tmp);
							tmp.setIfParent(ifParent);
							processControlDependences(tmp);
						}
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
					PDGTryNode tryNode = pdg.isDirectlyNestedWithinTryNode(dstNode);
					if(tryNode != null) {
						ControlDependenceTreeNode treeNode = searchForNode(tryNode);
						if(treeNode == null) {
							treeNode = new ControlDependenceTreeNode(cdtNode, tryNode);
							ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(treeNode, dstNode);
							processControlDependences(tmp);
						}
						else {
							ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(treeNode, dstNode);
							processControlDependences(tmp);
						}
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
								ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(fakeElse, dstNode);
								processControlDependences(tmp);
							}
							else {
								ControlDependenceTreeNode tmp = new ControlDependenceTreeNode(fakeElse, dstNode);
								processControlDependences(tmp);
							}
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
				PDGTryNode tryNode = pdg.isDirectlyNestedWithinTryNode(dstNode);
				if(tryNode != null) {
					ControlDependenceTreeNode treeNode = searchForNode(tryNode);
					if(treeNode == null) {
						//check if tryNode is nested inside another tryNode
						PDGTryNode otherTryNode = pdg.isDirectlyNestedWithinTryNode(tryNode);
						if(otherTryNode != null) {
							ControlDependenceTreeNode otherTreeNode = searchForNode(otherTryNode);
							if(otherTreeNode != null)
								new ControlDependenceTreeNode(otherTreeNode, tryNode);
							else
								new ControlDependenceTreeNode(cdtNode, tryNode);
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
								new ControlDependenceTreeNode(fakeElse, tryNode);
							}
							else {
								new ControlDependenceTreeNode(fakeElse, tryNode);
							}
						}
						else {
							new ControlDependenceTreeNode(cdtNode, tryNode);
						}
					}
				}
			}
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
}
