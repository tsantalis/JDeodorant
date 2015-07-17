package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;

public class PDGSubTreeMapper extends DivideAndConquerMatcher {
	
	public PDGSubTreeMapper(PDG pdg1, PDG pdg2,
			ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode controlDependenceSubTreePDG1,
			ControlDependenceTreeNode controlDependenceSubTreePDG2,
			boolean fullTreeMatch, IProgressMonitor monitor) {
		super(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2, controlDependenceSubTreePDG1, controlDependenceSubTreePDG2, fullTreeMatch, monitor);
		//creates CloneStructureRoot
		matchBasedOnControlDependenceTreeStructure();
		this.preconditionExaminer = new PreconditionExaminer(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2,
				getCloneStructureRoot(), getMaximumStateWithMinimumDifferences(), getAllNodesInSubTreePDG1(), getAllNodesInSubTreePDG2());
	}

	protected Set<PDGNode> getNodesInRegion1(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel, ControlDependenceTreeNode controlDependenceTreeRoot) {
		return getNodesInRegion(pdg, controlPredicate, controlPredicateNodesInCurrentLevel, controlPredicateNodesInNextLevel, controlDependenceTreeRoot);
	}

	protected Set<PDGNode> getNodesInRegion2(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel, ControlDependenceTreeNode controlDependenceTreeRoot) {
		return getNodesInRegion(pdg, controlPredicate, controlPredicateNodesInCurrentLevel, controlPredicateNodesInNextLevel, controlDependenceTreeRoot);
	}

	protected Set<PDGNode> getElseNodesOfSymmetricalIfStatement1(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel) {
		return getElseNodesOfSymmetricalIfStatement(pdg, controlPredicate, controlPredicateNodesInCurrentLevel, controlPredicateNodesInNextLevel);
	}

	protected Set<PDGNode> getElseNodesOfSymmetricalIfStatement2(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel) {
		return getElseNodesOfSymmetricalIfStatement(pdg, controlPredicate, controlPredicateNodesInCurrentLevel, controlPredicateNodesInNextLevel);
	}

	protected List<ControlDependenceTreeNode> getIfParentChildren1(ControlDependenceTreeNode cdtNode) {
		return getIfParentChildren(cdtNode);
	}

	protected List<ControlDependenceTreeNode> getIfParentChildren2(ControlDependenceTreeNode cdtNode) {
		return getIfParentChildren(cdtNode);
	}

	private Set<PDGNode> getNodesInRegion(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel, ControlDependenceTreeNode controlDependenceTreeRoot) {
		Set<PDGNode> nodesInRegion = new TreeSet<PDGNode>();
		if(!(controlPredicate instanceof PDGMethodEntryNode) &&
				!controlPredicate.equals(controlDependenceTreeRoot.getNode()))
			nodesInRegion.add(controlPredicate);
		if(controlPredicate instanceof PDGBlockNode) {
			Set<PDGNode> nestedNodesWithinTryNode = pdg.getNestedNodesWithinBlockNode((PDGBlockNode)controlPredicate);
			for(PDGNode nestedNode : nestedNodesWithinTryNode) {
				if(!controlPredicateNodesInNextLevel.contains(nestedNode) && !controlPredicateNodesInCurrentLevel.contains(nestedNode)) {
					if(!(nestedNode instanceof PDGControlPredicateNode) && !(nestedNode instanceof PDGBlockNode))
						nodesInRegion.add(nestedNode);
				}
			}
		}
		else {
			Iterator<GraphEdge> edgeIterator = controlPredicate.getOutgoingDependenceIterator();
			while(edgeIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)edgeIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGNode pdgNode = (PDGNode)dependence.getDst();
					PDGBlockNode tryNode = pdg.isDirectlyNestedWithinBlockNode(pdgNode);
					if(!controlPredicateNodesInNextLevel.contains(pdgNode) && !controlPredicateNodesInCurrentLevel.contains(pdgNode) && tryNode == null) {
						if(!(pdgNode instanceof PDGControlPredicateNode) && !(pdgNode instanceof PDGBlockNode))
							nodesInRegion.add(pdgNode);
					}
				}
			}
		}
		return nodesInRegion;
	}
	
	private Set<PDGNode> getElseNodesOfSymmetricalIfStatement(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel) {
		Set<PDGNode> nodesInRegion = new TreeSet<PDGNode>();
		Iterator<GraphEdge> edgeIterator = controlPredicate.getOutgoingDependenceIterator();
		while(edgeIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)edgeIterator.next();
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence pdgControlDependence = (PDGControlDependence)dependence;
				if(pdgControlDependence.isFalseControlDependence()) {
					PDGNode pdgNode = (PDGNode)dependence.getDst();
					PDGBlockNode tryNode = pdg.isDirectlyNestedWithinBlockNode(pdgNode);
					if(!controlPredicateNodesInNextLevel.contains(pdgNode) && !controlPredicateNodesInCurrentLevel.contains(pdgNode) && tryNode == null) {
						if(!(pdgNode instanceof PDGControlPredicateNode))
							nodesInRegion.add(pdgNode);
					}
				}
			}
		}
		return nodesInRegion;
	}

	private List<ControlDependenceTreeNode> getIfParentChildren(ControlDependenceTreeNode cdtNode) {
		List<ControlDependenceTreeNode> children = new ArrayList<ControlDependenceTreeNode>();
		if(cdtNode != null && cdtNode.isElseNode()) {
			ControlDependenceTreeNode ifParent = cdtNode.getIfParent();
			if(ifParent != null) {
				children.addAll(ifParent.getChildren());
			}
		}
		return children;
	}
}
