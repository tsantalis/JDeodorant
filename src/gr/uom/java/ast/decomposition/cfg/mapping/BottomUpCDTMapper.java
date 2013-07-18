package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;

import java.util.ArrayList;
import java.util.List;
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
		processBottomUp(root1, root2);
	}

	private void processBottomUp(ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
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
				boolean match;
				if((leaf1.isElseNode() && !leaf2.isElseNode()) || (!leaf1.isElseNode() && leaf2.isElseNode()))
					match = false;
				else if(leaf1.isElseNode() && leaf2.isElseNode())
					match = leaf1.getIfParent().getNode().getASTStatement().subtreeMatch(astNodeMatcher, leaf2.getIfParent().getNode().getASTStatement());
				else
					match = leaf1.getNode().getASTStatement().subtreeMatch(astNodeMatcher, leaf2.getNode().getASTStatement());
				if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(leaf1, leaf2)) {
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
		for(ControlDependenceTreeNodeMatchPair matchPair : filteredMatchLeafPairs) {
			TreeSet<ControlDependenceTreeNodeMatchPair> bottomUpMatch = new TreeSet<ControlDependenceTreeNodeMatchPair>();
			findBottomUpMatches(matchPair, bottomUpMatch);
			//check if there exist incomplete 'if/else if/else' chains in the solution
			Set<ControlDependenceTreeNodeMatchPair> pairsToBeRemoved = new TreeSet<ControlDependenceTreeNodeMatchPair>();
			for(ControlDependenceTreeNodeMatchPair pair : bottomUpMatch) {
				if(!isElseIfChainComplete(pair, bottomUpMatch)) {
					pairsToBeRemoved.add(pair);
				}
			}
			bottomUpMatch.removeAll(pairsToBeRemoved);
			CompleteSubTreeMatch subTree = new CompleteSubTreeMatch(bottomUpMatch);
			if(!isSubsumedByCurrentSolutions(solutions, subTree)) {
				solutions.add(subTree);
			}
		}
	}

	private boolean isElseIfChainComplete(ControlDependenceTreeNodeMatchPair matchPair, Set<ControlDependenceTreeNodeMatchPair> allMatchPairs) {
		ControlDependenceTreeNode node1 = matchPair.getNode1();
		ControlDependenceTreeNode node2 = matchPair.getNode2();
		if(node1.ifStatementInsideElseIfChain() && node2.ifStatementInsideElseIfChain()) {
			List<ControlDependenceTreeNode> ifParents1 = node1.getIfParents();
			List<ControlDependenceTreeNode> elseIfChildren1 = node1.getElseIfChildren();
			List<ControlDependenceTreeNode> chain1 = new ArrayList<ControlDependenceTreeNode>();
			chain1.addAll(ifParents1);
			chain1.addAll(elseIfChildren1);

			List<ControlDependenceTreeNode> ifParents2 = node2.getIfParents();
			List<ControlDependenceTreeNode> elseIfChildren2 = node2.getElseIfChildren();
			List<ControlDependenceTreeNode> chain2 = new ArrayList<ControlDependenceTreeNode>();
			chain2.addAll(ifParents2);
			chain2.addAll(elseIfChildren2);

			for(int i=0; i<chain1.size(); i++) {
				ControlDependenceTreeNodeMatchPair newPair = new ControlDependenceTreeNodeMatchPair(chain1.get(i), chain2.get(i));
				if(!allMatchPairs.contains(newPair))
					return false;
			}
		}
		return true;
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

	private void findBottomUpMatches(ControlDependenceTreeNodeMatchPair matchPair, Set<ControlDependenceTreeNodeMatchPair> matches) {
		ControlDependenceTreeNode treeNode = matchPair.getNode1();
		List<ControlDependenceTreeNode> treeSiblings = treeNode.getSiblings();
		ControlDependenceTreeNode searchNode = matchPair.getNode2();
		List<ControlDependenceTreeNode> searchSiblings = searchNode.getSiblings();	
		
		matches.add(matchPair);
		for(ControlDependenceTreeNode treeSibling : treeSiblings) {
			for(ControlDependenceTreeNode searchSibling : searchSiblings) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match;
				if((treeSibling.isElseNode() && !searchSibling.isElseNode()) || (!treeSibling.isElseNode() && searchSibling.isElseNode()))
					match = false;
				else if(treeSibling.isElseNode() && searchSibling.isElseNode())
					match = treeSibling.getIfParent().getNode().getASTStatement().subtreeMatch(astNodeMatcher, searchSibling.getIfParent().getNode().getASTStatement());
				else
					match = treeSibling.getNode().getASTStatement().subtreeMatch(astNodeMatcher, searchSibling.getNode().getASTStatement());
				if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(treeSibling, searchSibling) &&
						!alreadyMapped(matches, treeSibling, searchSibling)) {
					ControlDependenceTreeNodeMatchPair siblingMatchPair = new ControlDependenceTreeNodeMatchPair(treeSibling, searchSibling);
					TopDownCDTMapper topDownMapper = new TopDownCDTMapper(iCompilationUnit1, iCompilationUnit2, treeSibling, searchSibling);
					List<CompleteSubTreeMatch> completeSubTrees = topDownMapper.getSolutions();
					if(completeSubTrees.size() == 1) {
						CompleteSubTreeMatch subTree = completeSubTrees.get(0);
						if(subTree.getMatchPairs().contains(siblingMatchPair))
							matches.addAll(subTree.getMatchPairs());
					}
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
			else if((treeParent.isElseNode() && !searchParent.isElseNode()) || (!treeParent.isElseNode() && searchParent.isElseNode()))
				match = false;
			else if(treeParent.isElseNode() && searchParent.isElseNode())
				match = treeParent.getIfParent().getNode().getASTStatement().subtreeMatch(astNodeMatcher, searchParent.getIfParent().getNode().getASTStatement());
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
