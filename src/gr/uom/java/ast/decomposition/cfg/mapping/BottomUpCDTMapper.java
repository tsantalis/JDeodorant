package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.ICompilationUnit;

public class BottomUpCDTMapper {
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
			if(leaf1.leafSiblings())
				leavesWithLeafSiblings1.add(leaf1);
		}
		for(ControlDependenceTreeNode leaf2 : leaves2) {
			if(leaf2.leafSiblings())
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
		for(TreeSet<ControlDependenceTreeNodeMatchPair> match : matches) {
			//check if the subtrees are complete
			ControlDependenceTreeNodeMatchPair first = match.first();
			int subTreeSize1 = first.getNode1().getParent().getNodeCount() - 1;
			int subTreeSize2 = first.getNode2().getParent().getNodeCount() - 1;
			if(match.size() == subTreeSize1 && match.size() == subTreeSize2) {
				CompleteSubTreeMatch tempSolution = new CompleteSubTreeMatch(match);
				if(!isSubsumedByCurrentSolutions(solutions, tempSolution) && !overlapsWithCurrentSolutions(solutions, tempSolution) &&
						!equalsWithCurrentSolutions(solutions, tempSolution))
					solutions.add(tempSolution);
			}
		}
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
			TreeSet<ControlDependenceTreeNodeMatchPair> bottomUpMatchWithoutLeafPairs =
					new TreeSet<ControlDependenceTreeNodeMatchPair>(bottomUpMatch);
			bottomUpMatchWithoutLeafPairs.removeAll(leafPairs);
			ListIterator<TreeSet<ControlDependenceTreeNodeMatchPair>> matchIterator = matches.listIterator();
			while(matchIterator.hasNext()) {
				TreeSet<ControlDependenceTreeNodeMatchPair> match = matchIterator.next();
				TreeSet<ControlDependenceTreeNodeMatchPair> matchWithoutLeafPairs =
						new TreeSet<ControlDependenceTreeNodeMatchPair>(match);
				matchWithoutLeafPairs.removeAll(leafPairs);
				if( (bottomUpMatchWithoutLeafPairs.containsAll(matchWithoutLeafPairs) && !matchWithoutLeafPairs.isEmpty()) ||
						(matchWithoutLeafPairs.containsAll(bottomUpMatchWithoutLeafPairs) && !bottomUpMatchWithoutLeafPairs.isEmpty()) ) {
					TreeSet<ControlDependenceTreeNodeMatchPair> mergedMatch =
							new TreeSet<ControlDependenceTreeNodeMatchPair>();
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
				if(match && astNodeMatcher.isParameterizable() && !alreadyMapped(matches, treeSibling, searchSibling)) {
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
			if(match && astNodeMatcher.isParameterizable()) {
				ControlDependenceTreeNodeMatchPair newMatchPair = new ControlDependenceTreeNodeMatchPair(treeParent, searchParent);
				findBottomUpMatches(newMatchPair, matches);
			}
		}
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
