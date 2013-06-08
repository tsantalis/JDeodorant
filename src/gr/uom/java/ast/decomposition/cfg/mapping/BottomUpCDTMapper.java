package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.ICompilationUnit;

public class BottomUpCDTMapper {
	private static final int CDT_1 = 1;
	private static final int CDT_2 = 2;
	private ICompilationUnit iCompilationUnit1;
	private ICompilationUnit iCompilationUnit2;
	private List<CompleteSubTreeMatch> solutions;

	public BottomUpCDTMapper(ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
		this.iCompilationUnit1 = iCompilationUnit1;
		this.iCompilationUnit2 = iCompilationUnit2;
		this.solutions = new ArrayList<CompleteSubTreeMatch>();
		processBottomUp(iCompilationUnit1, iCompilationUnit2, root1, root2);
	}

	public void processBottomUp(ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
		List<ControlDependenceTreeNode> leaves1 = root1.getLeaves();
		List<ControlDependenceTreeNode> leaves2 = root2.getLeaves();
		List<ControlDependenceTreeNode> leavesWithLeafSiblings1 = new ArrayList<ControlDependenceTreeNode>();
		List<ControlDependenceTreeNode> leavesWithLeafSiblings2 = new ArrayList<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNode leaf1 : leaves1) {
			if(leaf1.areAllSiblingsLeaves())
				leavesWithLeafSiblings1.add(leaf1);
		}
		for(ControlDependenceTreeNode leaf2 : leaves2) {
			if(leaf2.areAllSiblingsLeaves())
				leavesWithLeafSiblings2.add(leaf2);
		}
		List<ControlDependenceTreeNodeMatchPair> matchLeafPairs = new ArrayList<ControlDependenceTreeNodeMatchPair>();
		for(ControlDependenceTreeNode leaf1 : leavesWithLeafSiblings1) {
			for(ControlDependenceTreeNode leaf2 : leavesWithLeafSiblings2) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match = leaf1.getNode().getASTStatement().subtreeMatch(astNodeMatcher, leaf2.getNode().getASTStatement());
				if(match && astNodeMatcher.isParameterizable()) {
					ControlDependenceTreeNodeMatchPair pair = new ControlDependenceTreeNodeMatchPair(leaf1, leaf2);
					matchLeafPairs.add(pair);
				}
			}
		}
		//filter leaf pairs for which a sibling leaf pair is already added in the list
		List<ControlDependenceTreeNodeMatchPair> filteredMatchLeafPairs = new ArrayList<ControlDependenceTreeNodeMatchPair>();
		for(ControlDependenceTreeNodeMatchPair matchPair : matchLeafPairs) {
			if(!containsSiblingMatch(filteredMatchLeafPairs, matchPair)) {
				filteredMatchLeafPairs.add(matchPair);
			}
		}
		List<TreeSet<ControlDependenceTreeNodeMatchPair>> matches = new ArrayList<TreeSet<ControlDependenceTreeNodeMatchPair>>();
		for(ControlDependenceTreeNodeMatchPair matchPair : filteredMatchLeafPairs) {
			TreeSet<ControlDependenceTreeNodeMatchPair> bottomUpMatch = new TreeSet<ControlDependenceTreeNodeMatchPair>();
			findBottomUpMatches(matchPair, bottomUpMatch);
			mergeOrAdd(matches, bottomUpMatch, matchLeafPairs);
		}
		Set<ControlDependenceTreeNode> nonMatchedNodesCDT1 = findUnMatchedNodes(root1.getNodesInBreadthFirstOrder(), matches, CDT_1);
		Set<ControlDependenceTreeNode> nonMatchedNodesCDT2 = findUnMatchedNodes(root2.getNodesInBreadthFirstOrder(), matches, CDT_2);
		for(TreeSet<ControlDependenceTreeNodeMatchPair> match : matches) {
			//check if the subtrees are complete
			ControlDependenceTreeNodeMatchPair first = match.first();
			
			Set<ControlDependenceTreeNode> nonMatchedNodesSubTrees1 = new LinkedHashSet<ControlDependenceTreeNode>();
			nonMatchedNodesSubTrees1.addAll(first.getNode1().getParent().getNodesInBreadthFirstOrder());
			nonMatchedNodesSubTrees1.remove(first.getNode1().getParent());
			nonMatchedNodesSubTrees1.retainAll(nonMatchedNodesCDT1);
			int totalNodesInNonMatchedSubTrees1 = 0;
			for(ControlDependenceTreeNode nonMatchedNode : nonMatchedNodesSubTrees1) {
				totalNodesInNonMatchedSubTrees1 += nonMatchedNode.getNodeCount();
			}
			
			Set<ControlDependenceTreeNode> nonMatchedNodesSubTrees2 = new LinkedHashSet<ControlDependenceTreeNode>();
			nonMatchedNodesSubTrees2.addAll(first.getNode2().getParent().getNodesInBreadthFirstOrder());
			nonMatchedNodesSubTrees2.remove(first.getNode2().getParent());
			nonMatchedNodesSubTrees2.retainAll(nonMatchedNodesCDT2);
			int totalNodesInNonMatchedSubTrees2 = 0;
			for(ControlDependenceTreeNode nonMatchedNode : nonMatchedNodesSubTrees2) {
				totalNodesInNonMatchedSubTrees2 += nonMatchedNode.getNodeCount();
			}
			
			int subTreeSize1 = first.getNode1().getParent().getNodeCount() - 1 - totalNodesInNonMatchedSubTrees1;
			int subTreeSize2 = first.getNode2().getParent().getNodeCount() - 1 - totalNodesInNonMatchedSubTrees2;
			if(match.size() == subTreeSize1 && match.size() == subTreeSize2) {
				CompleteSubTreeMatch tempSolution = new CompleteSubTreeMatch(match);
				if(!isSubsumedByCurrentSolutions(solutions, tempSolution) && !overlapsWithCurrentSolutions(solutions, tempSolution) &&
						!equalsWithCurrentSolutions(solutions, tempSolution))
					solutions.add(tempSolution);
			}
		}
	}

	private Set<ControlDependenceTreeNode> findUnMatchedNodes(List<ControlDependenceTreeNode> nodes,
			List<TreeSet<ControlDependenceTreeNodeMatchPair>> matches, int cdt) {
		Set<ControlDependenceTreeNode> unMatchedNodes = new LinkedHashSet<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNode node : nodes) {
			boolean found = false;
			for(TreeSet<ControlDependenceTreeNodeMatchPair> match : matches) {
				for(ControlDependenceTreeNodeMatchPair matchPair : match) {
					ControlDependenceTreeNode matchPairNode = null;
					if(cdt == CDT_1)
						matchPairNode = matchPair.getNode1();
					else if(cdt == CDT_2)
						matchPairNode = matchPair.getNode2();
					if(matchPairNode.equals(node)) {
						found = true;
						break;
					}
				}
				if(found)
					break;
			}
			if(!found && node.getParent() != null) {
				unMatchedNodes.add(node);
			}
		}
		return unMatchedNodes;
	}

	private boolean containsSiblingMatch(List<ControlDependenceTreeNodeMatchPair> matchLeafPairs,
			ControlDependenceTreeNodeMatchPair matchLeafPair) {
		for(ControlDependenceTreeNodeMatchPair matchPair : matchLeafPairs) {
			if(matchPair.getNode1().getParent().equals(matchLeafPair.getNode1().getParent()) &&
					matchPair.getNode2().getParent().equals(matchLeafPair.getNode2().getParent()))
				return true;
		}
		return false;
	}

	private boolean equalsWithCurrentSolutions(List<CompleteSubTreeMatch> solutions, CompleteSubTreeMatch solution) {
		for(CompleteSubTreeMatch currentSolution : solutions) {
			if(currentSolution.equals(solution))
				return true;
		}
		return false;
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

	public List<CompleteSubTreeMatch> getSolutions() {
		return solutions;
	}

	private void mergeOrAdd(List<TreeSet<ControlDependenceTreeNodeMatchPair>> matches,
			TreeSet<ControlDependenceTreeNodeMatchPair> bottomUpMatch, List<ControlDependenceTreeNodeMatchPair> leafPairs) {
		if(matches.isEmpty()) {
			matches.add(bottomUpMatch);
		}
		else {
			boolean merged = false;
			ListIterator<TreeSet<ControlDependenceTreeNodeMatchPair>> matchIterator = matches.listIterator();
			while(matchIterator.hasNext()) {
				TreeSet<ControlDependenceTreeNodeMatchPair> match = matchIterator.next();
				TreeSet<ControlDependenceTreeNodeMatchPair> intersection = new TreeSet<ControlDependenceTreeNodeMatchPair>();
				intersection.addAll(bottomUpMatch);
				intersection.retainAll(match);
				if(!intersection.isEmpty() && !conflictingMatches(bottomUpMatch, match)) {
					TreeSet<ControlDependenceTreeNodeMatchPair> mergedMatch = new TreeSet<ControlDependenceTreeNodeMatchPair>();
					mergedMatch.addAll(match);
					mergedMatch.addAll(bottomUpMatch);
					matchIterator.set(mergedMatch);
					merged = true;
					break;
				}
			}
			if(!merged) {
				matches.add(bottomUpMatch);
			}
		}
	}

	private boolean conflictingMatches(TreeSet<ControlDependenceTreeNodeMatchPair> matches1,
			TreeSet<ControlDependenceTreeNodeMatchPair> matches2) {
		//if the two sub-trees have matched the same node to a different one, then they cannot be merged
		for(ControlDependenceTreeNodeMatchPair pair1 : matches1) {
			for(ControlDependenceTreeNodeMatchPair pair2 : matches2) {
				if(pair1.getNode1().equals(pair2.getNode1())) {
					if(!pair1.getNode2().equals(pair2.getNode2()))
						return true;
				}
				if(pair1.getNode2().equals(pair2.getNode2())) {
					if(!pair1.getNode1().equals(pair2.getNode1()))
						return true;
				}
			}
		}
		return false;
	}

	private void findBottomUpMatches(ControlDependenceTreeNodeMatchPair matchPair, Set<ControlDependenceTreeNodeMatchPair> matches) {
		ControlDependenceTreeNode treeNode = matchPair.getNode1();
		List<ControlDependenceTreeNode> treeSiblings = treeNode.getSiblings();
		ControlDependenceTreeNode searchNode = matchPair.getNode2();
		List<ControlDependenceTreeNode> searchSiblings = searchNode.getSiblings();	
		
		matches.add(matchPair);
		for(ControlDependenceTreeNode treeSibling : treeSiblings) {
			for(ControlDependenceTreeNode searchSibling : searchSiblings) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match = treeSibling.getNode().getASTStatement().subtreeMatch(astNodeMatcher, searchSibling.getNode().getASTStatement());
				if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(treeSibling, searchSibling) &&
						!alreadyMapped(matches, treeSibling, searchSibling)) {
					ControlDependenceTreeNodeMatchPair siblingMatchPair = new ControlDependenceTreeNodeMatchPair(treeSibling, searchSibling);
					matches.add(siblingMatchPair);
					//apply first-match approach
					break;
				}
			}
		}
		
		ControlDependenceTreeNode treeParent = treeNode.getParent();
		ControlDependenceTreeNode searchParent = searchNode.getParent();
		if(treeParent != null && searchParent != null) {
			ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
			boolean match;
			if(treeParent.getNode() instanceof PDGMethodEntryNode || searchParent.getNode() instanceof PDGMethodEntryNode)
				match = false;
			else
				match = treeParent.getNode().getASTStatement().subtreeMatch(astNodeMatcher, searchParent.getNode().getASTStatement());
			if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(treeParent, searchParent)) {
				ControlDependenceTreeNodeMatchPair newMatchPair = new ControlDependenceTreeNodeMatchPair(treeParent, searchParent);
				findBottomUpMatches(newMatchPair, matches);
			}
		}
	}

	private boolean ifStatementsWithEqualElseIfChains(ControlDependenceTreeNode treeNode, ControlDependenceTreeNode searchNode) {
		if(treeNode.ifStatementInsideElseIfChain() && searchNode.ifStatementInsideElseIfChain()) {
			return treeNode.getLengthOfElseIfChain() == searchNode.getLengthOfElseIfChain();
		}
		return true;
	}

	private boolean alreadyMapped(Set<ControlDependenceTreeNodeMatchPair> matches,
			ControlDependenceTreeNode treeSibling, ControlDependenceTreeNode searchSibling) {
		for(ControlDependenceTreeNodeMatchPair match : matches) {
			if(match.getNode1().equals(treeSibling) || match.getNode2().equals(searchSibling))
				return true;
		}
		return false;
	}
}
