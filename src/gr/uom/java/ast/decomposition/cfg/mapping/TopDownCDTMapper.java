package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.jdt.core.ICompilationUnit;

public class TopDownCDTMapper {
	private ICompilationUnit iCompilationUnit1;
	private ICompilationUnit iCompilationUnit2;
	private List<CompleteSubTreeMatch> solutions;

	public TopDownCDTMapper(ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
		this.iCompilationUnit1 = iCompilationUnit1;
		this.iCompilationUnit2 = iCompilationUnit2;
		processTopDown(iCompilationUnit1, iCompilationUnit2, root1, root2);
	}

	public void processTopDown(ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
		List<TreeSet<ControlDependenceTreeNodeMatchPair>> matches = findTopDownMatches(root1, root2);
		this.solutions = getMaximumCompleteSubTreeMatches(matches, root1, root2);
		if(solutions.isEmpty()) {
			List<ControlDependenceTreeNode> nodes1 = root1.getNodesInBreadthFirstOrder();
			List<ControlDependenceTreeNode> nodes2 = root2.getNodesInBreadthFirstOrder();
			List<ControlDependenceTreeNodeMatchPair> startPoints = new ArrayList<ControlDependenceTreeNodeMatchPair>();
			for(ControlDependenceTreeNode node1 : nodes1) {
				for(ControlDependenceTreeNode node2 : nodes2) {
					if(!(node1.getNode() instanceof PDGMethodEntryNode) && !(node2.getNode() instanceof PDGMethodEntryNode)) {
						ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
						boolean match = node1.getNode().getASTStatement().subtreeMatch(astNodeMatcher, node2.getNode().getASTStatement());
						if(match && astNodeMatcher.isParameterizable()) {
							ControlDependenceTreeNodeMatchPair pair = new ControlDependenceTreeNodeMatchPair(node1, node2);
							startPoints.add(pair);
						}
					}
				}
			}
			for(ControlDependenceTreeNodeMatchPair startPoint : startPoints) {
				List<TreeSet<ControlDependenceTreeNodeMatchPair>> innerMatches = findTopDownMatches(startPoint.getNode1(), startPoint.getNode2());
				List<CompleteSubTreeMatch> tempSolutions = getMaximumCompleteSubTreeMatches(innerMatches, startPoint.getNode1(), startPoint.getNode2());
				for(CompleteSubTreeMatch tempSolution : tempSolutions) {
					tempSolution.addStartPoint(startPoint);
					if(!isSubsumedByCurrentSolutions(solutions, tempSolution))
						solutions.add(tempSolution);
				}
			}
		}
	}

	public List<CompleteSubTreeMatch> getSolutions() {
		List<CompleteSubTreeMatch> nonOverlappingSolutions = new ArrayList<CompleteSubTreeMatch>();
		for(CompleteSubTreeMatch solution : solutions) {
			if(!overlapsWithCurrentSolutions(nonOverlappingSolutions, solution))
				nonOverlappingSolutions.add(solution);
		}
		return nonOverlappingSolutions;
	}

	private boolean overlapsWithCurrentSolutions(List<CompleteSubTreeMatch> solutions, CompleteSubTreeMatch solution) {
		for(CompleteSubTreeMatch currentSolution : solutions) {
			if(currentSolution.overlaps(solution))
				return true;
		}
		return false;
	}

	private boolean isSubsumedByCurrentSolutions(List<CompleteSubTreeMatch> solutions, CompleteSubTreeMatch newSolution) {
		for(CompleteSubTreeMatch currentSolution : solutions) {
			if(currentSolution.subsumes(newSolution))
				return true;
		}
		return false;
	}

	private List<CompleteSubTreeMatch> getMaximumCompleteSubTreeMatches(List<TreeSet<ControlDependenceTreeNodeMatchPair>> matches,
			ControlDependenceTreeNode parent1, ControlDependenceTreeNode parent2) {
		List<CompleteSubTreeMatch> solutions = new ArrayList<CompleteSubTreeMatch>();
		int maximumSize = 0;
		int subTreeSize1 = parent1.getNodeCount() - 1;
		int subTreeSize2 = parent2.getNodeCount() - 1;
		for(TreeSet<ControlDependenceTreeNodeMatchPair> match : matches) {
			if(match.size() == subTreeSize1 && match.size() == subTreeSize2) {
				if(match.size() > maximumSize) {
					maximumSize = match.size();
					solutions.clear();
					solutions.add(new CompleteSubTreeMatch(match));
				}
				else if(match.size() == maximumSize) {
					solutions.add(new CompleteSubTreeMatch(match));
				}
			}
		}
		return solutions;
	}

	private List<TreeSet<ControlDependenceTreeNodeMatchPair>> findTopDownMatches(ControlDependenceTreeNode treeNode, ControlDependenceTreeNode searchNode) {
		List<ControlDependenceTreeNode> searchChildren = searchNode.getChildren();
		if(searchChildren.isEmpty()) {
			TreeSet<ControlDependenceTreeNodeMatchPair> pair = new TreeSet<ControlDependenceTreeNodeMatchPair>();
			List<TreeSet<ControlDependenceTreeNodeMatchPair>> matchPairs = new ArrayList<TreeSet<ControlDependenceTreeNodeMatchPair>>();
			matchPairs.add(pair);
			return matchPairs;
		}
		else {
			List<TreeSet<ControlDependenceTreeNodeMatchPair>> matches = new ArrayList<TreeSet<ControlDependenceTreeNodeMatchPair>>();
			ControlDependenceTreeNode searchChild = searchChildren.get(0);
			ControlDependenceTreeNode searchNode2 = searchNode.shallowCopy();
			searchNode2.getChildren().remove(searchChild);

			for(ControlDependenceTreeNode treeChild : treeNode.getChildren()) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match = treeChild.getNode().getASTStatement().subtreeMatch(astNodeMatcher, searchChild.getNode().getASTStatement());
				if(match && astNodeMatcher.isParameterizable()) {
					ControlDependenceTreeNode treeNode2 = treeNode.shallowCopy();
					treeNode2.getChildren().remove(treeChild);
					List<TreeSet<ControlDependenceTreeNodeMatchPair>> childMatches = findTopDownMatches(treeChild, searchChild);
					List<TreeSet<ControlDependenceTreeNodeMatchPair>> nodeMatches = findTopDownMatches(treeNode2, searchNode2);
					
					for(TreeSet<ControlDependenceTreeNodeMatchPair> nodeMatchPairs : nodeMatches) {
						for(TreeSet<ControlDependenceTreeNodeMatchPair> childMatchPairs : childMatches) {
							ControlDependenceTreeNodeMatchPair pair = new ControlDependenceTreeNodeMatchPair(treeChild, searchChild);
							TreeSet<ControlDependenceTreeNodeMatchPair> fullMatchPairs = new TreeSet<ControlDependenceTreeNodeMatchPair>();
							fullMatchPairs.add(pair);
							fullMatchPairs.addAll(childMatchPairs);
							fullMatchPairs.addAll(nodeMatchPairs);
							matches.add(fullMatchPairs);
						}
					}
					//apply first-match approach
					break;
				}
			}
			return matches;
		}
	}
}
