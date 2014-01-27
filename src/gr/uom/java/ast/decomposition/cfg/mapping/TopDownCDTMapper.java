package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

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
		this.solutions = new ArrayList<CompleteSubTreeMatch>();
		processTopDown(root1, root2);
	}

	private void processTopDown(ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
		List<TreeSet<ControlDependenceTreeNodeMatchPair>> matches = findTopDownMatches(root1, root2);
		List<CompleteSubTreeMatch> tmpSolutions = getMaximumCompleteSubTreeMatches(matches, root1, root2);
		for(CompleteSubTreeMatch tmpSolution : tmpSolutions) {
			ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
			boolean match;
			if((root1.isElseNode() && !root2.isElseNode()) || (!root1.isElseNode() && root2.isElseNode()))
				match = false;
			else if(root1.isElseNode() && root2.isElseNode())
				match = astNodeMatcher.match(root1.getIfParent().getNode(), root2.getIfParent().getNode());
			else
				match = astNodeMatcher.match(root1.getNode(), root2.getNode());
			if(match && astNodeMatcher.isParameterizable()) {
				ControlDependenceTreeNodeMatchPair pair = new ControlDependenceTreeNodeMatchPair(root1, root2, astNodeMatcher);
				tmpSolution.addStartPoint(pair);
				if(!isSubsumedByCurrentSolutions(solutions, tmpSolution))
					solutions.add(tmpSolution);
			}
		}
		if(solutions.isEmpty()) {
			secondPhase(root1, root2);
		}
	}

	private void secondPhase(ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
		List<ControlDependenceTreeNode> nodes1 = root1.getNodesInBreadthFirstOrder();
		List<ControlDependenceTreeNode> nodes2 = root2.getNodesInBreadthFirstOrder();
		List<ControlDependenceTreeNodeMatchPair> startPoints = new ArrayList<ControlDependenceTreeNodeMatchPair>();
		for(ControlDependenceTreeNode node1 : nodes1) {
			for(ControlDependenceTreeNode node2 : nodes2) {
				if(!(node1.getNode() instanceof PDGMethodEntryNode) && !(node2.getNode() instanceof PDGMethodEntryNode)) {
					ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
					boolean match;
					if((node1.isElseNode() && !node2.isElseNode()) || (!node1.isElseNode() && node2.isElseNode()))
						match = false;
					else if(node1.isElseNode() && node2.isElseNode())
						match = astNodeMatcher.match(node1.getIfParent().getNode(), node2.getIfParent().getNode());
					else
						match = astNodeMatcher.match(node1.getNode(), node2.getNode());
					if(match && astNodeMatcher.isParameterizable()) {
						ControlDependenceTreeNodeMatchPair pair = new ControlDependenceTreeNodeMatchPair(node1, node2, astNodeMatcher);
						startPoints.add(pair);
					}
				}
			}
		}
		for(ControlDependenceTreeNodeMatchPair startPoint : startPoints) {
			List<TreeSet<ControlDependenceTreeNodeMatchPair>> innerMatches = findTopDownMatches(startPoint.getNode1(), startPoint.getNode2());
			List<CompleteSubTreeMatch> tmpSolutions = getMaximumCompleteSubTreeMatches(innerMatches, startPoint.getNode1(), startPoint.getNode2());
			for(CompleteSubTreeMatch tmpSolution : tmpSolutions) {
				tmpSolution.addStartPoint(startPoint);
				if(!isSubsumedByCurrentSolutions(solutions, tmpSolution))
					solutions.add(tmpSolution);
			}
		}
	}

	public List<CompleteSubTreeMatch> getSolutions() {
		return solutions;
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
			int ternaryOperatorCount1 = 0;
			int ternaryOperatorCount2 = 0;
			for(ControlDependenceTreeNodeMatchPair pair : match) {
				if(pair.getNode1().isTernary() && !pair.getNode2().isTernary())
					ternaryOperatorCount1++;
				if(pair.getNode2().isTernary() && !pair.getNode1().isTernary())
					ternaryOperatorCount2++;
			}
			if(match.size() == (subTreeSize1 - ternaryOperatorCount2) &&
					match.size() == (subTreeSize2 - ternaryOperatorCount1)) {
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
				boolean match;
				if((treeChild.isElseNode() && !searchChild.isElseNode()) || (!treeChild.isElseNode() && searchChild.isElseNode()))
					match = false;
				else if(treeChild.isElseNode() && searchChild.isElseNode())
					match = astNodeMatcher.match(treeChild.getIfParent().getNode(), searchChild.getIfParent().getNode());
				else
					match = astNodeMatcher.match(treeChild.getNode(), searchChild.getNode());
				if(match && astNodeMatcher.isParameterizable()) {
					ControlDependenceTreeNode treeNode2 = treeNode.shallowCopy();
					treeNode2.getChildren().remove(treeChild);
					List<TreeSet<ControlDependenceTreeNodeMatchPair>> childMatches = findTopDownMatches(treeChild, searchChild);
					List<TreeSet<ControlDependenceTreeNodeMatchPair>> nodeMatches = findTopDownMatches(treeNode2, searchNode2);
					
					for(TreeSet<ControlDependenceTreeNodeMatchPair> nodeMatchPairs : nodeMatches) {
						for(TreeSet<ControlDependenceTreeNodeMatchPair> childMatchPairs : childMatches) {
							ControlDependenceTreeNodeMatchPair pair = new ControlDependenceTreeNodeMatchPair(treeChild, searchChild, astNodeMatcher);
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
