package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.ICompilationUnit;

public class BottomUpCDTMapper {
	private ICompilationUnit iCompilationUnit1;
	private ICompilationUnit iCompilationUnit2;
	private List<CompleteSubTreeMatch> solutions;
	private boolean forceTopDownMatchForSibings = true;

	public BottomUpCDTMapper(ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode root1, ControlDependenceTreeNode root2) {
		this.iCompilationUnit1 = iCompilationUnit1;
		this.iCompilationUnit2 = iCompilationUnit2;
		this.solutions = new ArrayList<CompleteSubTreeMatch>();
		processBottomUp(root1, root2);
	}

	public BottomUpCDTMapper(ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode root1, ControlDependenceTreeNode root2, boolean forceTopDownMatchForSibings) {
		this.iCompilationUnit1 = iCompilationUnit1;
		this.iCompilationUnit2 = iCompilationUnit2;
		this.solutions = new ArrayList<CompleteSubTreeMatch>();
		this.forceTopDownMatchForSibings = forceTopDownMatchForSibings;
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
			List<ControlDependenceTreeNodeMatchPair> currentMatchLeafPairs = new ArrayList<ControlDependenceTreeNodeMatchPair>();
			for(ControlDependenceTreeNode leaf2 : leavesWithLeafSiblings2) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match;
				if((leaf1.isElseNode() && !leaf2.isElseNode()) || (!leaf1.isElseNode() && leaf2.isElseNode()))
					match = false;
				else if(leaf1.isElseNode() && leaf2.isElseNode()) {
					if(containsMatch(matchLeafPairs, leaf1.getIfParent(), leaf2.getIfParent()))
						match = astNodeMatcher.match(leaf1.getIfParent().getNode(), leaf2.getIfParent().getNode());
					else
						match = false;
				}
				else
					match = astNodeMatcher.match(leaf1.getNode(), leaf2.getNode());
				if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(leaf1, leaf2)) {
					ControlDependenceTreeNodeMatchPair pair = new ControlDependenceTreeNodeMatchPair(leaf1, leaf2, astNodeMatcher);
					currentMatchLeafPairs.add(pair);
				}
			}
			if(!currentMatchLeafPairs.isEmpty()) {
				Collections.sort(currentMatchLeafPairs, new ControlDependenceTreeNodeMatchPairComparator());
				//best match with minimum differences
				matchLeafPairs.add(currentMatchLeafPairs.get(0));
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
			if(bottomUpMatch.size() > 0) {
				CompleteSubTreeMatch subTree = new CompleteSubTreeMatch(bottomUpMatch);
				if(!isSubsumedByCurrentSolutions(solutions, subTree)) {
					solutions.add(subTree);
				}
			}
		}
		//post-processing
		List<CompleteSubTreeMatch> sortedSolutions = new ArrayList<CompleteSubTreeMatch>(solutions);
		Collections.sort(sortedSolutions, new SubTreeMatchComparator());
		for(int i=0; i<sortedSolutions.size(); i++) {
			CompleteSubTreeMatch solutionI = sortedSolutions.get(i);
			for(int j=i+1; j<sortedSolutions.size(); j++) {
				CompleteSubTreeMatch solutionJ = sortedSolutions.get(j);
				if(solutionI.subsumes(solutionJ)) {
					solutions.remove(solutionJ);
				}
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
	
	private boolean containsMatch(Collection<ControlDependenceTreeNodeMatchPair> matches,
			ControlDependenceTreeNode treeSibling, ControlDependenceTreeNode searchSibling) {
		for(ControlDependenceTreeNodeMatchPair match : matches) {
			if(match.getNode1().equals(treeSibling) && match.getNode2().equals(searchSibling))
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
		ControlDependenceTreeNode searchNode = matchPair.getNode2();
		boolean proceed = true;
		if(matchPair.ifStatementInsideElseIfChain()) {
			List<ControlDependenceTreeNode> treeIfParents = treeNode.getIfParents();
			List<ControlDependenceTreeNode> treeElseIfChildren = treeNode.getElseIfChildren();
			List<ControlDependenceTreeNode> treeChain = new ArrayList<ControlDependenceTreeNode>();
			treeChain.addAll(treeIfParents);
			treeChain.addAll(treeElseIfChildren);
			
			List<ControlDependenceTreeNode> searchIfParents = searchNode.getIfParents();
			List<ControlDependenceTreeNode> searchElseIfChildren = searchNode.getElseIfChildren();
			List<ControlDependenceTreeNode> searchChain = new ArrayList<ControlDependenceTreeNode>();
			searchChain.addAll(searchIfParents);
			searchChain.addAll(searchElseIfChildren);
			
			if(treeChain.size() == searchChain.size()) {
				Set<ControlDependenceTreeNodeMatchPair> elseIfChainMatchedSiblings = new LinkedHashSet<ControlDependenceTreeNodeMatchPair>();
				Set<ControlDependenceTreeNodeMatchPair> elseIfChainTopDownMatches = new TreeSet<ControlDependenceTreeNodeMatchPair>();
				for(ControlDependenceTreeNode treeSibling : treeChain) {
					for(ControlDependenceTreeNode searchSibling : searchChain) {
						ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
						boolean match;
						if((treeSibling.isElseNode() && !searchSibling.isElseNode()) || (!treeSibling.isElseNode() && searchSibling.isElseNode()))
							match = false;
						else if(treeSibling.isElseNode() && searchSibling.isElseNode())
							match = astNodeMatcher.match(treeSibling.getIfParent().getNode(), searchSibling.getIfParent().getNode());
						else
							match = astNodeMatcher.match(treeSibling.getNode(), searchSibling.getNode());
						if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(treeSibling, searchSibling) &&
								!alreadyMapped(matches, treeSibling, searchSibling) && !alreadyMapped(elseIfChainTopDownMatches, treeSibling, searchSibling)) {
							ControlDependenceTreeNodeMatchPair siblingMatchPair = new ControlDependenceTreeNodeMatchPair(treeSibling, searchSibling, astNodeMatcher);
							TopDownCDTMapper topDownMapper = new TopDownCDTMapper(iCompilationUnit1, iCompilationUnit2, treeSibling, searchSibling);
							List<CompleteSubTreeMatch> completeSubTrees = topDownMapper.getSolutions();
							if(completeSubTrees.size() == 1) {
								CompleteSubTreeMatch subTree = completeSubTrees.get(0);
								if(subTree.getMatchPairs().contains(siblingMatchPair)) {
									elseIfChainMatchedSiblings.add(siblingMatchPair);
									elseIfChainTopDownMatches.addAll(subTree.getMatchPairs());
								}
							}
							else if(!forceTopDownMatchForSibings) {
								elseIfChainMatchedSiblings.add(siblingMatchPair);
								elseIfChainTopDownMatches.add(siblingMatchPair);
							}
							//apply first-match approach
							break;
						}
					}
				}
				if(matchPair.getNode1().getLengthOfElseIfChain() == elseIfChainMatchedSiblings.size() &&
						matchPair.getNode2().getLengthOfElseIfChain() == elseIfChainMatchedSiblings.size()) {
					matches.add(matchPair);
					matches.addAll(elseIfChainTopDownMatches);
				}
				else {
					proceed = false;
				}
			}
			//one node is an if statement with a single else, and the other is a ternary operator
			else if(ifStatementWithSingleElseAgainstTernaryOperator(treeNode, searchNode)) {
				matches.add(matchPair);
			}
			else {
				proceed = false;
			}
		}
		
		if(proceed) {
			if(!matchPair.ifStatementInsideElseIfChain())
				matches.add(matchPair);
			List<ControlDependenceTreeNode> treeSiblings = treeNode.getSiblings();
			List<ControlDependenceTreeNode> searchSiblings = searchNode.getSiblings();
			for(ControlDependenceTreeNode treeSibling : treeSiblings) {
				for(ControlDependenceTreeNode searchSibling : searchSiblings) {
					ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
					boolean match;
					if((treeSibling.isElseNode() && !searchSibling.isElseNode()) || (!treeSibling.isElseNode() && searchSibling.isElseNode()))
						match = false;
					else if(treeSibling.isElseNode() && searchSibling.isElseNode()) {
						if(containsMatch(matches, treeSibling.getIfParent(), searchSibling.getIfParent()))
							match = astNodeMatcher.match(treeSibling.getIfParent().getNode(), searchSibling.getIfParent().getNode());
						else
							match = false;
					}
					else
						match = astNodeMatcher.match(treeSibling.getNode(), searchSibling.getNode());
					if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(treeSibling, searchSibling) &&
							!alreadyMapped(matches, treeSibling, searchSibling)) {
						ControlDependenceTreeNodeMatchPair siblingMatchPair = new ControlDependenceTreeNodeMatchPair(treeSibling, searchSibling, astNodeMatcher);
						TopDownCDTMapper topDownMapper = new TopDownCDTMapper(iCompilationUnit1, iCompilationUnit2, treeSibling, searchSibling);
						List<CompleteSubTreeMatch> completeSubTrees = topDownMapper.getSolutions();
						if(completeSubTrees.size() == 1) {
							CompleteSubTreeMatch subTree = completeSubTrees.get(0);
							if(subTree.getMatchPairs().contains(siblingMatchPair)) {
								matches.addAll(subTree.getMatchPairs());
							}
						}
						else if(!forceTopDownMatchForSibings) {
							matches.add(siblingMatchPair);
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
					match = astNodeMatcher.match(treeParent.getIfParent().getNode(), searchParent.getIfParent().getNode());
				else
					match = astNodeMatcher.match(treeParent.getNode(), searchParent.getNode());
				if(match && astNodeMatcher.isParameterizable() && ifStatementsWithEqualElseIfChains(treeParent, searchParent)) {
					ControlDependenceTreeNodeMatchPair newMatchPair = new ControlDependenceTreeNodeMatchPair(treeParent, searchParent, astNodeMatcher);
					findBottomUpMatches(newMatchPair, matches);
				}
			}
		}
	}

	private boolean ifStatementsWithEqualElseIfChains(ControlDependenceTreeNode treeNode, ControlDependenceTreeNode searchNode) {
		if(treeNode.ifStatementInsideElseIfChain() && searchNode.ifStatementInsideElseIfChain()) {
			return treeNode.getLengthOfElseIfChain() == searchNode.getLengthOfElseIfChain();
		}
		return true;
	}

	private boolean ifStatementWithSingleElseAgainstTernaryOperator(ControlDependenceTreeNode treeNode, ControlDependenceTreeNode searchNode) {
		if(treeNode.getIfParent() == null && treeNode.getElseIfChild() != null && treeNode.getElseIfChild().isElseNode() && searchNode.isTernary())
			return true;
		if(searchNode.getIfParent() == null && searchNode.getElseIfChild() != null && searchNode.getElseIfChild().isElseNode() && treeNode.isTernary())
			return true;
		return false;
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
