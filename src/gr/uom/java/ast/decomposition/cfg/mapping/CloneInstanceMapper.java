package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeSet;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.ClassDeclarationObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.matching.NodePairComparisonCache;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import ca.concordia.jdeodorant.clone.parsers.CloneInstance;
import ca.concordia.jdeodorant.clone.parsers.JavaModelUtility;
import ca.concordia.jdeodorant.clone.parsers.TextDiff;
import ca.concordia.jdeodorant.clone.parsers.TextDiff.Diff;

@SuppressWarnings("restriction")
public class CloneInstanceMapper {

	private List<PDGRegionSubTreeMapper> subTreeMappers;

	public List<PDGRegionSubTreeMapper> getSubTreeMappers() {
		return subTreeMappers;
	}

	public CloneInstanceMapper(CloneInstance instance1, CloneInstance instance2, IJavaProject javaProject, IProgressMonitor monitor) {
		this.subTreeMappers = new ArrayList<PDGRegionSubTreeMapper>();
		try {
			SystemObject systemObject = ASTReader.getSystemObject();
			int firstStartOffset = instance1.getLocationInfo().getUpdatedStartOffset();
			int firstEndOffset = instance1.getLocationInfo().getUpdatedEndOffset();
			IMethod iMethod1 = getIMethod(javaProject, instance1.getPackageName() + "." + instance1.getClassName(),
					instance1.getMethodName(), instance1.getIMethodSignature(), firstStartOffset, firstEndOffset);

			int secondStartOffset = instance2.getLocationInfo().getUpdatedStartOffset();
			int secondEndOffset = instance2.getLocationInfo().getUpdatedEndOffset();
			IMethod iMethod2 = getIMethod(javaProject, instance2.getPackageName() + "." + instance2.getClassName(),
					instance2.getMethodName(), instance2.getIMethodSignature(), secondStartOffset, secondEndOffset);

			AbstractMethodDeclaration methodObject1 = systemObject.getMethodObject(iMethod1);
			AbstractMethodDeclaration methodObject2 = systemObject.getMethodObject(iMethod2);

			if(methodObject1 != null && methodObject2 != null && methodObject1.getMethodBody() != null && methodObject2.getMethodBody() != null) {
				ClassDeclarationObject classObject1 = null;
				ClassDeclarationObject classObject2 = null;

				if (iMethod1.getDeclaringType().isAnonymous()) {
					classObject1 = systemObject.getAnonymousClassDeclaration(iMethod1.getDeclaringType());
				}
				else {
					classObject1 = systemObject.getClassObject(methodObject1.getClassName());
				}

				if (iMethod2.getDeclaringType().isAnonymous()) {
					classObject2 = systemObject.getAnonymousClassDeclaration(iMethod2.getDeclaringType());

				}
				else {
					classObject2 = systemObject.getClassObject(methodObject2.getClassName());
				}

				ITypeRoot typeRoot1 = classObject1.getITypeRoot();
				ITypeRoot typeRoot2 = classObject2.getITypeRoot();
				ICompilationUnit iCompilationUnit1 = (ICompilationUnit)JavaCore.create(classObject1.getIFile());
				ICompilationUnit iCompilationUnit2 = (ICompilationUnit)JavaCore.create(classObject2.getIFile());
				CompilationUnitCache.getInstance().lock(typeRoot1);
				CompilationUnitCache.getInstance().lock(typeRoot2);

				ASTNode node1 = NodeFinder.perform(classObject1.getClassObject().getAbstractTypeDeclaration().getRoot(), firstStartOffset, firstEndOffset - firstStartOffset);
				ExtractStatementsVisitor visitor1 = new ExtractStatementsVisitor(node1);
				node1.accept(visitor1);

				if(visitor1.getStatementsList().size() == 0)
					node1.getParent().accept(visitor1);

				ASTNode node2 = NodeFinder.perform(classObject2.getClassObject().getAbstractTypeDeclaration().getRoot(), secondStartOffset, secondEndOffset - secondStartOffset);
				ExtractStatementsVisitor visitor2 = new ExtractStatementsVisitor(node2);
				node2.accept(visitor2);

				if(visitor2.getStatementsList().size() == 0)
					node2.getParent().accept(visitor2);

				PDG pdg1 = getPDG(iMethod1, monitor);
				PDG pdg2 = null;
				if(!iMethod1.equals(iMethod2)) {
					pdg2 = getPDG(iMethod2, monitor);
				}
				else {
					pdg2 = pdg1;
				}

				// These two contain the entire nesting structure of the methods
				ControlDependenceTreeNode controlDependenceTreePDG1 = new ControlDependenceTreeGenerator(pdg1).getRoot();
				ControlDependenceTreeNode controlDependenceTreePDG2 = new ControlDependenceTreeGenerator(pdg2).getRoot();

				// Get the control predicate nodes inside the ASTNode returned by Eclipse's NodeFinder
				List<ASTNode> controlASTNodes1X = visitor1.getControlStatementsList();
				List<ASTNode> controlASTNodes2X = visitor2.getControlStatementsList();

				// Get the control predicate nodes inside the clone fragments
				List<ASTNode> controlASTNodes1 = new ArrayList<ASTNode>();
				List<ASTNode> controlASTNodes2 = new ArrayList<ASTNode>();

				for (ASTNode astNode: controlASTNodes1X) {
					if(isInside(astNode, firstStartOffset, firstEndOffset, iCompilationUnit1))
						controlASTNodes1.add(astNode);
				}

				for (ASTNode astNode : controlASTNodes2X) {
					if(isInside(astNode, secondStartOffset, secondEndOffset, iCompilationUnit2))
						controlASTNodes2.add(astNode);
				}

				// Get all statement nodes (including control and leaf nodes) inside the ASTNode returned by Eclipse's NodeFinder
				List<ASTNode> ASTNodes1X = visitor1.getStatementsList();
				List<ASTNode> ASTNodes2X = visitor2.getStatementsList();

				// Get all statement nodes inside the clone fragments
				List<ASTNode> ASTNodes1 = new ArrayList<ASTNode>();
				List<ASTNode> ASTNodes2 = new ArrayList<ASTNode>();

				for (ASTNode astNode : ASTNodes1X) {
					if(isInside(astNode, firstStartOffset, firstEndOffset, iCompilationUnit1)) {
						ASTNodes1.add(astNode);	
					}
				}

				for (ASTNode astNode : ASTNodes2X) {
					if(isInside(astNode, secondStartOffset, secondEndOffset, iCompilationUnit2)) {
						ASTNodes2.add(astNode);	
					}
				}
				
				// Get all the control predicate nodes inside the methods containing the clone fragments 
				List<ControlDependenceTreeNode> CDTNodes1 = controlDependenceTreePDG1.getNodesInBreadthFirstOrder();
				List<ControlDependenceTreeNode> CDTNodes2 = controlDependenceTreePDG2.getNodesInBreadthFirstOrder();

				// Get the control dependence tree nodes in the clone fragments 
				List<ControlDependenceTreeNode> subTreeCDTNodes1 = getSubTreeCDTNodes(CDTNodes1, controlASTNodes1);
				List<ControlDependenceTreeNode> subTreeCDTNodes2 = getSubTreeCDTNodes(CDTNodes2, controlASTNodes2);
				
				int numberOfStatementsToBeRefactored = Math.min(ASTNodes1.size(), ASTNodes2.size());
				if(numberOfStatementsToBeRefactored == 0) {
					NodePairComparisonCache.getInstance().clearCache();
					return;
				}
				
				// If one of the clone fragments contain no control predicate nodes, create a dummy CDT 
				if(subTreeCDTNodes1.size() == 0 || subTreeCDTNodes2.size() == 0) {

					if (allStatementsAreInAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(ASTNodes1) || 
							allStatementsAreInAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(ASTNodes2)) {
						NodePairComparisonCache.getInstance().clearCache();
						return;
					}
					// Get the control parent (or method) containing the clone fragments
					ASTNode parent1 = getControlParent(ASTNodes1);
					ASTNode parent2 = getControlParent(ASTNodes2);
					
					// Get the ControlDependenceTreeNode corresponding to the parent nodes.
					// If all the ASTNodes are nested under an "else", it returns the "else" ControlDependenceTreeNode instead of the "if"
					ControlDependenceTreeNode controlDependenceSubTreePDG1X = getSubTreeCDTNode(CDTNodes1, parent1, allNodesNestedUnderElse(ASTNodes1));
					ControlDependenceTreeNode controlDependenceSubTreePDG2X = getSubTreeCDTNode(CDTNodes2, parent2, allNodesNestedUnderElse(ASTNodes2));

					// Get all the control dependence tree nodes under the obtained ControlDependenceTreeNode
					List<ControlDependenceTreeNode> CDTNodesList1 = controlDependenceSubTreePDG1X.getNodesInBreadthFirstOrder();
					List<ControlDependenceTreeNode> CDTNodesList2 = controlDependenceSubTreePDG2X.getNodesInBreadthFirstOrder();

					// If the ControlDependenceTreeNodeX is a method entry node, then remove its children cdt nodes
					if(controlDependenceSubTreePDG1X.getNode() instanceof PDGMethodEntryNode) {
						ListIterator<ControlDependenceTreeNode> iterator = CDTNodesList1.listIterator();
						while(iterator.hasNext()) {
							ControlDependenceTreeNode currentCDTNode = iterator.next();
							PDGNode node = null;
							if(currentCDTNode.isElseNode()) {
								node = currentCDTNode.getIfParent().getNode();
							}
							else {
								node = currentCDTNode.getNode();
							}
							if(!node.equals(controlDependenceSubTreePDG1X.getNode())) {
								iterator.remove();
							}
						}
					}

					if(controlDependenceSubTreePDG2X.getNode() instanceof PDGMethodEntryNode) {
						ListIterator<ControlDependenceTreeNode> iterator = CDTNodesList2.listIterator();
						while(iterator.hasNext()) {
							ControlDependenceTreeNode currentCDTNode = iterator.next();
							PDGNode node = null;
							if(currentCDTNode.isElseNode()) {
								node = currentCDTNode.getIfParent().getNode();
							}
							else {
								node = currentCDTNode.getNode();
							}
							if(!node.equals(controlDependenceSubTreePDG2X.getNode())) {
								iterator.remove();
							}
						}
					}

					// Create CDT subtree with containing only the filtered CDTNodes
					ControlDependenceTreeNode controlDependenceSubTreePDG1 = generateControlDependenceSubTreeWithTheFirstNodeAsRoot(controlDependenceTreePDG1, CDTNodesList1);
					ControlDependenceTreeNode controlDependenceSubTreePDG2 = generateControlDependenceSubTreeWithTheFirstNodeAsRoot(controlDependenceTreePDG2, CDTNodesList2);
					
					PDGRegionSubTreeMapper mapper = new PDGRegionSubTreeMapper(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2,
							controlDependenceSubTreePDG1, controlDependenceSubTreePDG2, ASTNodes1, ASTNodes2, true, monitor);
					subTreeMappers.add(mapper);
				} else { // If we have a control structure
					// Remove the CDT subtree nodes being part of an incomplete if-else-if chain
					List<ControlDependenceTreeNode> subTreeCDTNodes1Copy = new ArrayList<ControlDependenceTreeNode>(subTreeCDTNodes1);
					for(ControlDependenceTreeNode subTreeCDTNode1 : subTreeCDTNodes1Copy) {
						if(subTreeCDTNode1.ifStatementInsideElseIfChain()) {
							List<ControlDependenceTreeNode> ifParents = subTreeCDTNode1.getIfParents();
							List<ControlDependenceTreeNode> elseIfChildren = subTreeCDTNode1.getElseIfChildren();
							List<ControlDependenceTreeNode> treeChain = new ArrayList<ControlDependenceTreeNode>();
							for(ControlDependenceTreeNode ifParent : ifParents) {
								if(subTreeCDTNodes1Copy.contains(ifParent)) {
									treeChain.add(ifParent);
								}
							}
							for(ControlDependenceTreeNode elseIfChild : elseIfChildren) {
								if(subTreeCDTNodes1Copy.contains(elseIfChild)) {
									treeChain.add(elseIfChild);
								}
							}
							if(!subTreeCDTNodes1Copy.containsAll(treeChain)) {
								subTreeCDTNodes1.remove(subTreeCDTNode1);
								subTreeCDTNodes1.removeAll(subTreeCDTNode1.getDescendants());
							}
						}
					}
					
					List<ControlDependenceTreeNode> subTreeCDTNodes2Copy = new ArrayList<ControlDependenceTreeNode>(subTreeCDTNodes2);
					for(ControlDependenceTreeNode subTreeCDTNode2 : subTreeCDTNodes2Copy) {
						if(subTreeCDTNode2.ifStatementInsideElseIfChain()) {
							List<ControlDependenceTreeNode> ifParents = subTreeCDTNode2.getIfParents();
							List<ControlDependenceTreeNode> elseIfChildren = subTreeCDTNode2.getElseIfChildren();
							List<ControlDependenceTreeNode> treeChain = new ArrayList<ControlDependenceTreeNode>();
							for(ControlDependenceTreeNode ifParent : ifParents) {
								if(subTreeCDTNodes2Copy.contains(ifParent)) {
									treeChain.add(ifParent);
								}
							}
							for(ControlDependenceTreeNode elseIfChild : elseIfChildren) {
								if(subTreeCDTNodes2Copy.contains(elseIfChild)) {
									treeChain.add(elseIfChild);
								}
							}
							if(!subTreeCDTNodes2Copy.containsAll(treeChain)) {
								subTreeCDTNodes2.remove(subTreeCDTNode2);
								subTreeCDTNodes2.removeAll(subTreeCDTNode2.getDescendants());
							}
						}
					}

					if(subTreeCDTNodes1.size() > 0 && subTreeCDTNodes2.size() > 0) {
						// Create CDT subtree with containing only the filtered CDTNodes
						ControlDependenceTreeNode controlDependenceSubTreePDG1X = generateControlDependenceSubTree(controlDependenceTreePDG1, subTreeCDTNodes1);
						ControlDependenceTreeNode controlDependenceSubTreePDG2X = generateControlDependenceSubTree(controlDependenceTreePDG2, subTreeCDTNodes2);

						// Nodes of original CDTs in Breadth First order
						List<ControlDependenceTreeNode> CDTNodesList1 = controlDependenceSubTreePDG1X.getNodesInBreadthFirstOrder();
						List<ControlDependenceTreeNode> CDTNodesList2 = controlDependenceSubTreePDG2X.getNodesInBreadthFirstOrder();

						// Do the bottom up mapping and get all the pairs of mapped CDT subtrees
						BottomUpCDTMapper bottomUpCDTMapper = new BottomUpCDTMapper(iCompilationUnit1, iCompilationUnit2, controlDependenceSubTreePDG1X, controlDependenceSubTreePDG2X, false);

						// Get the solutions
						List<CompleteSubTreeMatch> bottomUpSubTreeMatches = bottomUpCDTMapper.getSolutions();
						
						if (bottomUpSubTreeMatches.size() == 0) {
							if(ASTNodes1.size() == pdg1.getTotalNumberOfStatements() && ASTNodes2.size() == pdg2.getTotalNumberOfStatements()) {
								PDGRegionSubTreeMapper mapper = new PDGRegionSubTreeMapper(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2, 
										controlDependenceTreePDG1, controlDependenceTreePDG2, ASTNodes1, ASTNodes2, true, monitor);
								subTreeMappers.add(mapper);
							}
						} else {
							// For each solution in the bottom-up matching, do the PDG mapping 
							for(CompleteSubTreeMatch subTreeMatch : bottomUpSubTreeMatches) {
								TreeSet<ControlDependenceTreeNodeMatchPair> matchPairs = new TreeSet<ControlDependenceTreeNodeMatchPair>(); 
								List<ControlDependenceTreeNode>subTreeMatchNodes1 = new ArrayList<ControlDependenceTreeNode>();
								List<ControlDependenceTreeNode>subTreeMatchNodes2 = new ArrayList<ControlDependenceTreeNode>();

								/*
								 * Filtering the nodes inside subTreeCDTNodes1 and subTreeCDTNodes2, keep only
								 * the nodes in the clone fragments
								 */
								for (ControlDependenceTreeNodeMatchPair matchPair : subTreeMatch.getMatchPairs()) {
									if(subTreeCDTNodes1.contains(matchPair.getNode1()) && subTreeCDTNodes2.contains(matchPair.getNode2())) {
										subTreeMatchNodes1.add(matchPair.getNode1());
										subTreeMatchNodes2.add(matchPair.getNode2());
										matchPairs.add(matchPair);
									}
								}

								// If all the matched pairs are completely inside one of the code fragments 
								boolean fullTreeMatch = (matchPairs.size() == Math.min(subTreeCDTNodes1.size(), subTreeCDTNodes2.size()));
								// Get the nodes of the matched pairs in breadth first order
								List<ControlDependenceTreeNode> orderedSubtreeMatchNodes1 = getCDTNodesInBreadthFirstOrder(CDTNodesList1,subTreeMatchNodes1);
								List<ControlDependenceTreeNode> orderedSubtreeMatchNodes2 = getCDTNodesInBreadthFirstOrder(CDTNodesList2,subTreeMatchNodes2);

								// Generate CDTs from the matched nodes
								ControlDependenceTreeNode controlDependenceSubTreePDG1 = generateControlDependenceSubTree(controlDependenceTreePDG1, orderedSubtreeMatchNodes1);
								// insert unmatched CDT nodes under matched ones
								for(ControlDependenceTreeNode node : controlDependenceTreePDG1.getNodesInBreadthFirstOrder()) {
									if(!orderedSubtreeMatchNodes1.contains(node) && orderedSubtreeMatchNodes1.contains(node.getParent())) {
										insertCDTNodeInTree(node, controlDependenceSubTreePDG1);
										orderedSubtreeMatchNodes1.add(node);
									}
								}
								ControlDependenceTreeNode controlDependenceSubTreePDG2 = generateControlDependenceSubTree(controlDependenceTreePDG2, orderedSubtreeMatchNodes2);
								// insert unmatched CDT nodes under matched ones
								for(ControlDependenceTreeNode node : controlDependenceTreePDG2.getNodesInBreadthFirstOrder()) {
									if(!orderedSubtreeMatchNodes2.contains(node) && orderedSubtreeMatchNodes2.contains(node.getParent())) {
										insertCDTNodeInTree(node, controlDependenceSubTreePDG2);
										orderedSubtreeMatchNodes2.add(node);
									}
								}
								PDGRegionSubTreeMapper mapper = new PDGRegionSubTreeMapper(pdg1, pdg2, iCompilationUnit1, iCompilationUnit2, 
										controlDependenceSubTreePDG1, controlDependenceSubTreePDG2, ASTNodes1, ASTNodes2, fullTreeMatch, monitor);
								subTreeMappers.add(mapper);
							}
						}
					}
				}
				NodePairComparisonCache.getInstance().clearCache();
			}
		}
		catch(JavaModelException e) {
			e.printStackTrace();
		}
	}

	private List<ControlDependenceTreeNode> getCDTNodesInBreadthFirstOrder(List<ControlDependenceTreeNode> OrderedCDTNodesList, List<ControlDependenceTreeNode> UnorderedCDTNodesList) {
		List<ControlDependenceTreeNode> newCDTNodesList = new ArrayList<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNode CDTNode: OrderedCDTNodesList) {
			if(UnorderedCDTNodesList.contains(CDTNode)) {
				newCDTNodesList.add(CDTNode);
			}
		}
		return newCDTNodesList;
	}

	private ControlDependenceTreeNode generateControlDependenceSubTree(ControlDependenceTreeNode completeTreeRoot, List<ControlDependenceTreeNode> subTreeNodes) {
		ControlDependenceTreeNode oldCDTNode = subTreeNodes.get(0);
		ControlDependenceTreeNode root = new ControlDependenceTreeNode(null, oldCDTNode.getParent().getNode());

		if(oldCDTNode.getParent().isElseNode()) {
			root.setElseNode(true);
			root.setIfParent(oldCDTNode.getParent().getIfParent());
		}
		for(ControlDependenceTreeNode cdtNode : subTreeNodes) {
			insertCDTNodeInTree(cdtNode, root);
		}
		return root;
	}

	private ControlDependenceTreeNode generateControlDependenceSubTreeWithTheFirstNodeAsRoot(ControlDependenceTreeNode completeTreeRoot, List<ControlDependenceTreeNode> subTreeNodes) {
		ControlDependenceTreeNode oldCDTNode = subTreeNodes.get(0);
		ControlDependenceTreeNode root = new ControlDependenceTreeNode(null, oldCDTNode.getNode());

		if(oldCDTNode.isElseNode()) {
			root.setElseNode(true);
			root.setIfParent(oldCDTNode.getIfParent());
		}

		for(int i=1; i<subTreeNodes.size(); i++) {
			ControlDependenceTreeNode cdtNode = subTreeNodes.get(i);
			insertCDTNodeInTree(cdtNode, root);
		}
		return root;
	}

	private void insertCDTNodeInTree(ControlDependenceTreeNode cdtNode, ControlDependenceTreeNode root) {
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
			ControlDependenceTreeNode newIfParent = root.getNode(cdtNode.getIfParent().getNode());
			if (newIfParent!=null) {
				newIfParent.setElseIfChild(newNode);
				newNode.setIfParent(newIfParent);
			}
		}
		else if(cdtNode.getIfParent() != null) {
			ControlDependenceTreeNode newIfParent = root.getNode(cdtNode.getIfParent().getNode());
			if (newIfParent!=null) {
				newNode.setIfParentAndElseIfChild(newIfParent);
			}
		}
	}

	private ControlDependenceTreeNode getSubTreeCDTNode(List<ControlDependenceTreeNode> CDTNodes, ASTNode ASTNode, boolean allNodesUnderElse) {
		ControlDependenceTreeNode subTreeCDTNode = null;
		boolean found = false;
		for(ControlDependenceTreeNode CDTNode : CDTNodes) {
			if(CDTNode.getNode() instanceof PDGMethodEntryNode)
				continue;
			if(CDTNode.getNode() != null && 
					CDTNode.getNode().getASTStatement().equals(ASTNode)) {
				subTreeCDTNode = CDTNode;
				if(allNodesUnderElse)
					subTreeCDTNode = subTreeCDTNode.getElseIfChild();
				found = true;
				break;
			}
		}
		if(!found) {
			for(ControlDependenceTreeNode CDTNode : CDTNodes) {
				if(CDTNode.getNode() instanceof PDGMethodEntryNode)
					continue;
				if(CDTNode.getNode() != null && CDTNode.getNode().getASTStatement().subtreeMatch(new ASTMatcher(), ASTNode)) {
					subTreeCDTNode = CDTNode;
					if(allNodesUnderElse)
						subTreeCDTNode = subTreeCDTNode.getElseIfChild();
					break;
				}
			}
		}	
		if(subTreeCDTNode == null)
			subTreeCDTNode = CDTNodes.get(0);
		return subTreeCDTNode;
	}

	private boolean allNodesNestedUnderElse(List<ASTNode> astNodes) {
		for(ASTNode astNode:astNodes) {
			if(!isNestedUnderElse(astNode))
				return false;
		}
		return true;
	}

	private boolean isNestedUnderElse(ASTNode astNode) {
		if(astNode.getParent() instanceof IfStatement) {
			IfStatement ifParent = (IfStatement)astNode.getParent();
			if(ifParent.getElseStatement()!=null && ifParent.getElseStatement().equals(astNode))
				return true;
		}
		if(astNode.getParent() instanceof Block) {
			Block blockParent = (Block)astNode.getParent();
			if(blockParent.getParent() instanceof IfStatement) {
				IfStatement ifGrandParent = (IfStatement)blockParent.getParent();
				if(ifGrandParent.getElseStatement()!=null && ifGrandParent.getElseStatement().equals(blockParent))
					return true;
			}
		}
		return false;
	}

	private ASTNode getControlParent(List<ASTNode> ASTNodes) {
		Map<ASTNode,List<ASTNode>> parentMap = new LinkedHashMap<ASTNode,List<ASTNode>>();  
		for(ASTNode astNode : ASTNodes) {
			ASTNode astParent = getParent(astNode);
			if(parentMap.containsKey(astParent)) {
				parentMap.get(astParent).add(astNode);
			}
			else {
				List<ASTNode> childNodes = new ArrayList<ASTNode>();
				childNodes.add(astNode);
				parentMap.put(astParent, childNodes);
			}
		}
		List<ASTNode> parentList = new ArrayList<ASTNode>(parentMap.keySet());
		if(parentMap.keySet().size() == 1) {
			return parentList.get(0);
		}
		else if(parentMap.keySet().size() == 2) {
			//check if the second parent key is the only child of the first parent key
			ASTNode secondParent = parentList.get(1);
			List<ASTNode> firstParentChildren = parentMap.get(parentList.get(0));
			if(firstParentChildren.size() == 1 && firstParentChildren.contains(secondParent)) {
				return secondParent;
			}
		}
		return getParent(ASTNodes.get(0));
	}

	private ASTNode getParent(ASTNode controlNode) {
		if(!(controlNode.getParent() instanceof Block))
			return controlNode.getParent();
		while(controlNode.getParent() instanceof Block) {
			controlNode = controlNode.getParent();
		}
		if(controlNode.getParent() instanceof CatchClause) {
			CatchClause catchClause = (CatchClause)controlNode.getParent();
			return catchClause.getParent();
		}
		return controlNode.getParent();
	}

	private boolean allStatementsAreInAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(List<ASTNode> ASTNodes) { 
		for (ASTNode astNode : ASTNodes) {
			if (!isNestedUnderAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(astNode))
				return false;
		}
		return true;
	}
	
	private boolean isNestedUnderAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof AnonymousClassDeclaration || parent instanceof CatchClause ||
					isFinallyBlockOfTryStatement(parent)) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}
	
	private boolean isFinallyBlockOfTryStatement(ASTNode node) {
		ASTNode parent = node.getParent();
		if(parent != null && parent instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)parent;
			Block finallyBlock = tryStatement.getFinally();
			if(node instanceof Block && finallyBlock != null) {
				return finallyBlock.equals((Block)node);
			}
		}
		return false;
	}

	private List<ControlDependenceTreeNode> getSubTreeCDTNodes(
			List<ControlDependenceTreeNode> CDTNodes,
			List<ASTNode> controlASTNodes) {
		List<ControlDependenceTreeNode> subTreeCDTNodes = new ArrayList<ControlDependenceTreeNode>();
		for(ASTNode ASTNode : controlASTNodes) {
			boolean found = false;
			for(ControlDependenceTreeNode CDTNode : CDTNodes) {
				if(CDTNode.getNode() instanceof PDGMethodEntryNode)
					continue;
				if(CDTNode.getNode() != null && CDTNode.getNode().getASTStatement().equals(ASTNode)) {
					subTreeCDTNodes.add(CDTNode);
					if(CDTNode.getNode().getASTStatement() instanceof IfStatement) {
						if(CDTNode.getElseChild() != null)
							subTreeCDTNodes.add(CDTNode.getElseChild());
					}
					found = true;
					break;
				}
			}
			if(!found) {
				for(ControlDependenceTreeNode CDTNode : CDTNodes) {
					if(CDTNode.getNode() instanceof PDGMethodEntryNode)
						continue;
					if(CDTNode.getNode() != null && CDTNode.getNode().getASTStatement().subtreeMatch(new ASTMatcher(), ASTNode)) {
						subTreeCDTNodes.add(CDTNode);
						if(CDTNode.getNode().getASTStatement() instanceof IfStatement) {
							if(CDTNode.getElseChild() != null)
								subTreeCDTNodes.add(CDTNode.getElseChild());
						}
						break;
					}
				}
			}	
		}
		return subTreeCDTNodes;
	}

	private boolean isInside(ASTNode astNode, int startOffset, int endOffset, ICompilationUnit iCompilationUnit) {

		int astNodeStartOffset = astNode.getStartPosition();
		int astNodeLength = astNode.getLength();
		int astNodeEndOffset = astNodeStartOffset + astNodeLength - 1;

		// If the node is completely inside
		if (astNodeStartOffset >= startOffset && astNodeEndOffset <= endOffset)
			return true;

		if (astNodeStartOffset >= startOffset && astNodeStartOffset <= endOffset) {
			IDocument iDocument = JavaModelUtility.getIDocument(iCompilationUnit);
			try {
				String realSourceCode = iDocument.get(astNodeStartOffset, endOffset - astNodeStartOffset + 1);
				String astNodeSourceCode = iDocument.get(astNodeStartOffset, astNodeLength);

				TextDiff td = new TextDiff();
				LinkedList<Diff> diffs = td.diff_main(realSourceCode, astNodeSourceCode, false);
				td.diff_cleanupSemantic(diffs);

				String commentRegularExpression = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)";

				boolean realSourceCodeFound = false;
				for (Diff diff : diffs) {
					switch (diff.operation) {
					case EQUAL:
						if (diff.text.equals(realSourceCode))
							realSourceCodeFound = true;
						break;
					case DELETE:
						return false;
					case INSERT:
						String filtered = diff.text.replaceAll(commentRegularExpression, "").replaceAll("\\s", "").replaceAll("\\}", "").replaceAll("\\)", "").replaceAll(";", "");
						if(realSourceCodeFound && (filtered.isEmpty() || hasOnlyKeyWord(filtered)))
							return true;
						else
							return false;
					}
				}
				return realSourceCodeFound;
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean hasOnlyKeyWord(String filtered) {
		String[] keyWords = new String[] {"return", "break", "continue"};
		for (String keyWord : keyWords)
			if (keyWord.equals(filtered))
				return true;
		return false;
	}

	private PDG getPDG(IMethod iMethod, IProgressMonitor progressMonitor) throws JavaModelException {
		SystemObject systemObject = ASTReader.getSystemObject();
		AbstractMethodDeclaration methodObject = systemObject.getMethodObject(iMethod);
		ClassDeclarationObject classObject = null;

		if (iMethod.getDeclaringType().isAnonymous()) {
			classObject = systemObject.getAnonymousClassDeclaration(iMethod.getDeclaringType());
		}
		else {
			classObject = systemObject.getClassObject(methodObject.getClassName());
		}

		ITypeRoot typeRoot = classObject.getITypeRoot();
		CompilationUnitCache.getInstance().lock(typeRoot);
		CFG cfg = new CFG(methodObject);
		final PDG pdg = new PDG(cfg, classObject.getIFile(), classObject.getFieldsAccessedInsideMethod(methodObject), progressMonitor);
		return pdg;
	}

	private IMethod getIMethod(IJavaProject jProject, String typeName, String methodName, String methodSignature, int start, int end)
			throws JavaModelException {
		IType type = jProject.findType(typeName);
		if(type == null) {
			IPath path = new Path("/" + jProject.getElementName() + "/" + typeName.substring(0, typeName.lastIndexOf(".")));
			IPackageFragment packageFragment = jProject.findPackageFragment(path);
			if (packageFragment != null)
				type = jProject.findPackageFragment(path).getCompilationUnit(typeName.substring(typeName.lastIndexOf(".")+1)+".java").findPrimaryType();
			else
				return null;
		}
		IMethod iMethod = null;
		if(!methodSignature.equals("")) {
			iMethod = getIMethodWithSignature(jProject, type, methodName, methodSignature, start, end);
		}

		if(iMethod == null) {
			iMethod = recursiveGetIMethod(type, jProject, methodName, methodSignature, start, end);
		}
		return iMethod;
	}

	private IMethod recursiveGetIMethod(IType type, IJavaProject jProject, String methodName, String methodSignature, int start, int end) throws JavaModelException {
		IMethod innerMethod = null;
		for(IType innerType:type.getCompilationUnit().getAllTypes()) {
			if(!methodSignature.equals("")) {
				innerMethod = getIMethodWithSignature(jProject, innerType, methodName, methodSignature, start, end);
				if(innerMethod != null)
					return innerMethod;	 
			}
		}
		return null;
	}

	private IMethod getIMethodWithSignature(IJavaProject jProject, IType type, String methodName, String methodSignature, int start, int end)
			throws JavaModelException {
		SystemObject systemObject = ASTReader.getSystemObject();
		List<IMethod> methods = new ArrayList<IMethod>();
		if(type.exists()) {
			for(IMethod method : type.getMethods()) {
				methods.add(method);
			}
		}
		else {
			IJavaElement typeParent = type.getParent();
			if(typeParent != null && typeParent instanceof ICompilationUnit) {
				ICompilationUnit iCompilationUnit = (ICompilationUnit)typeParent;
				IType[] allTypes = iCompilationUnit.getAllTypes();
				for(IType iType : allTypes) {
					for(IMethod iMethod : iType.getMethods()) {
						methods.add(iMethod);
					}
				}
			}
		}
		IMethod iMethod = null;
		for(IMethod method : methods) {
			SourceMethod sm = (SourceMethod) method;
			IJavaElement[] smChildren = sm.getChildren();
			if(smChildren.length != 0) {
				if(method.getSignature().equals(methodSignature) && method.getElementName().equals(methodName)) {
					AbstractMethodDeclaration abstractMethodDeclaration = systemObject.getMethodObject(method);
					MethodDeclaration methodAST = abstractMethodDeclaration.getMethodDeclaration();
					int methodStartPosition = methodAST.getStartPosition();
					int methodEndPosition = methodStartPosition + methodAST.getLength();
					if(methodStartPosition <= start && methodEndPosition >= end) {
						iMethod = method;
						break;
					}
				}

				for(int i=0; i<smChildren.length; i++) {
					if(smChildren[i] instanceof SourceType) {
						SourceType st = (SourceType) smChildren[i];
						for(IMethod im : st.getMethods()) {
							if(im.getSignature().equals(methodSignature) && im.getElementName().equals(methodName)) {
								AbstractMethodDeclaration abstractMethodDeclaration = systemObject.getMethodObject(im);
								MethodDeclaration methodAST = abstractMethodDeclaration.getMethodDeclaration();
								int methodStartPosition = methodAST.getStartPosition();
								int methodEndPosition = methodStartPosition + methodAST.getLength();
								if(methodStartPosition <= start && methodEndPosition >= end) {
									iMethod = im;
									return iMethod;
								}
							}
						}
					}
				}
			}
			else if(method.getSignature().equals(methodSignature) && method.getElementName().equals(methodName)) {
				AbstractMethodDeclaration abstractMethodDeclaration = systemObject.getMethodObject(method);
				MethodDeclaration methodAST = abstractMethodDeclaration.getMethodDeclaration();
				int methodStartPosition = methodAST.getStartPosition();
				int methodEndPosition = methodStartPosition + methodAST.getLength();
				if(methodStartPosition <= start && methodEndPosition >= end) {
					iMethod = method;
					break;
				}
			}
		}
		return iMethod;
	}
}
