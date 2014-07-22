package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.matching.NodePairComparisonCache;

public class PDGMapper {
	private List<CompleteSubTreeMatch> bottomUpSubTreeMatches;
	private List<PDGSubTreeMapper> subTreeMappers;
	
	public PDGMapper(PDG pdg1, PDG pdg2, IProgressMonitor monitor) {
		this.subTreeMappers = new ArrayList<PDGSubTreeMapper>();
		ControlDependenceTreeNode controlDependenceTreePDG1 = new ControlDependenceTreeGenerator(pdg1).getRoot();
		ControlDependenceTreeNode controlDependenceTreePDG2 = new ControlDependenceTreeGenerator(pdg2).getRoot();
		CompilationUnit cu1 = (CompilationUnit)pdg1.getMethod().getMethodDeclaration().getRoot();
		ICompilationUnit iCompilationUnit1 = (ICompilationUnit)cu1.getJavaElement();
		CompilationUnit cu2 = (CompilationUnit)pdg2.getMethod().getMethodDeclaration().getRoot();
		ICompilationUnit iCompilationUnit2 = (ICompilationUnit)cu2.getJavaElement();
		
		BottomUpCDTMapper bottomUpCDTMapper = new BottomUpCDTMapper(iCompilationUnit1, iCompilationUnit2, controlDependenceTreePDG1, controlDependenceTreePDG2);
		this.bottomUpSubTreeMatches = bottomUpCDTMapper.getSolutions();
		
		for(CompleteSubTreeMatch subTreeMatch : bottomUpSubTreeMatches) {
			int size1 = controlDependenceTreePDG1.getNodeCount() - 1;
			int size2 = controlDependenceTreePDG2.getNodeCount() - 1;
			int subTreeSize = subTreeMatch.getMatchPairs().size();
			int ternaryOperatorCount1 = 0;
			int ternaryOperatorCount2 = 0;
			for(ControlDependenceTreeNodeMatchPair pair : subTreeMatch.getMatchPairs()) {
				if(pair.getNode1().isTernary() && !pair.getNode2().isTernary())
					ternaryOperatorCount1++;
				if(pair.getNode2().isTernary() && !pair.getNode1().isTernary())
					ternaryOperatorCount2++;
			}
			int treeSize1 = size1 - ternaryOperatorCount2;
			int treeSize2 = size2 - ternaryOperatorCount1;
			if(subTreeSize == treeSize1 && subTreeSize == treeSize2) {
				PDGSubTreeMapper mapper = new PDGSubTreeMapper(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2, controlDependenceTreePDG1, controlDependenceTreePDG2, true, monitor);
				if(!mapper.getCloneStructureRoot().getChildren().isEmpty())
					subTreeMappers.add(mapper);
			}
			else {
				ControlDependenceTreeNode controlDependenceSubTreePDG1 = generateControlDependenceSubTree(controlDependenceTreePDG1, subTreeMatch.getControlDependenceTreeNodes1());
				ControlDependenceTreeNode controlDependenceSubTreePDG2 = generateControlDependenceSubTree(controlDependenceTreePDG2, subTreeMatch.getControlDependenceTreeNodes2());
				TreeSet<ControlDependenceTreeNodeMatchPair> matchPairs = subTreeMatch.getMatchPairs();
				boolean fullTreeMatch = matchPairs.size() == Math.min(treeSize1, treeSize2);
				PDGSubTreeMapper mapper = new PDGSubTreeMapper(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2, controlDependenceSubTreePDG1, controlDependenceSubTreePDG2, fullTreeMatch, monitor);
				if(!mapper.getCloneStructureRoot().getChildren().isEmpty())
					subTreeMappers.add(mapper);
			}
		}
		if(bottomUpSubTreeMatches.isEmpty()) {
			//create subtrees containing only the method entry node
			ControlDependenceTreeNode controlDependenceSubTreePDG1 = new ControlDependenceTreeNode(null, controlDependenceTreePDG1.getNode());
			ControlDependenceTreeNode controlDependenceSubTreePDG2 = new ControlDependenceTreeNode(null, controlDependenceTreePDG2.getNode());
			PDGSubTreeMapper mapper = new PDGSubTreeMapper(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2, controlDependenceSubTreePDG1, controlDependenceSubTreePDG2, true, monitor);
			if(!mapper.getCloneStructureRoot().getChildren().isEmpty())
				subTreeMappers.add(mapper);
		}
		NodePairComparisonCache.getInstance().clearCache();
	}

	private ControlDependenceTreeNode generateControlDependenceSubTree(ControlDependenceTreeNode completeTreeRoot, List<ControlDependenceTreeNode> subTreeNodes) {
		ControlDependenceTreeNode oldCDTNode = subTreeNodes.get(0);
		ControlDependenceTreeNode root = new ControlDependenceTreeNode(null, oldCDTNode.getParent().getNode());
		if(oldCDTNode.getParent().isElseNode()) {
			root.setElseNode(true);
			root.setIfParent(oldCDTNode.getParent().getIfParent());
		}
		for(ControlDependenceTreeNode cdtNode : subTreeNodes) {
			ControlDependenceTreeNode parent;
			if(cdtNode.getParent().isElseNode()) {
				parent = root.getElseNode(cdtNode.getParent().getIfParent().getNode());
			}
			else {
				parent = root.getNode(cdtNode.getParent().getNode());
			}
			ControlDependenceTreeNode newNode = new ControlDependenceTreeNode(parent, cdtNode.getNode());
			if(cdtNode.isElseNode()) {
				newNode.setElseNode(true);
				newNode.setIfParent(root.getNode(cdtNode.getIfParent().getNode()));
			}
			else if(cdtNode.getIfParent() != null) {
				ControlDependenceTreeNode newIfParent = root.getNode(cdtNode.getIfParent().getNode());
				if(newIfParent != null) {
					newNode.setIfParentAndElseIfChild(newIfParent);
				}
			}
		}
		return root;
	}

	public List<CompleteSubTreeMatch> getBottomUpSubTreeMatches() {
		return bottomUpSubTreeMatches;
	}

	public List<PDGSubTreeMapper> getSubTreeMappers() {
		return subTreeMappers;
	}
}
