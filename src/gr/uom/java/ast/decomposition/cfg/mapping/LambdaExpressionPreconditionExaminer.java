package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.loop.AbstractLoopUtilities;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class LambdaExpressionPreconditionExaminer {

	private MappingState finalState;
	private Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> commonPassedParameters;
	private TreeSet<PDGNode> nonMappedNodesG1;
	private TreeSet<PDGNode> nonMappedNodesG2;
	private List<PDGExpressionGap> refactorableExpressionGaps;
	private List<PDGNodeBlockGap> refactorableBlockGaps;

	public LambdaExpressionPreconditionExaminer(CloneStructureNode cloneStructureRoot, MappingState finalState, 
			Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> commonPassedParameters, TreeSet<PDGNode> nonMappedNodesG1, TreeSet<PDGNode> nonMappedNodesG2) {
		this.finalState = finalState;
		this.commonPassedParameters = commonPassedParameters;
		this.nonMappedNodesG1 = nonMappedNodesG1;
		this.nonMappedNodesG2 = nonMappedNodesG2;
		this.refactorableExpressionGaps = new ArrayList<PDGExpressionGap>();
		this.refactorableBlockGaps = new ArrayList<PDGNodeBlockGap>();
		checkCloneStructureNodeForGaps(cloneStructureRoot);
		List<PDGNodeBlockGap> newRefactorableBlockGaps = new ArrayList<PDGNodeBlockGap>(refactorableBlockGaps);
		for(int i=0; i<newRefactorableBlockGaps.size()-1; i++) {
			PDGNodeBlockGap blockGap1 = newRefactorableBlockGaps.get(i);
			for(int j=i+1; j<newRefactorableBlockGaps.size(); j++) {
				PDGNodeBlockGap blockGap2 = newRefactorableBlockGaps.get(j);
				if(blockGap1.sharesCommonStatements(blockGap2)) {
					PDGNodeBlockGap merged = blockGap1.merge(blockGap2);
					if(merged != null) {
						checkRefactorableBlockGap(merged);
					}
				}
			}
		}
		List<PDGNodeBlockGap> blockGapsToBeRemoved = new ArrayList<PDGNodeBlockGap>();
		for(PDGNodeBlockGap blockGap : refactorableBlockGaps) {
			List<PDGNodeBlockGap> otherBlockGaps = new ArrayList<PDGNodeBlockGap>(refactorableBlockGaps);
			otherBlockGaps.remove(blockGap);
			if(blockGap.isSubsumed(otherBlockGaps)) {
				blockGapsToBeRemoved.add(blockGap);
			}
		}
		this.refactorableBlockGaps.removeAll(blockGapsToBeRemoved);
	}

	public List<PDGExpressionGap> getRefactorableExpressionGaps() {
		return refactorableExpressionGaps;
	}

	public List<PDGNodeBlockGap> getRefactorableBlockGaps() {
		return refactorableBlockGaps;
	}

	public void discardBlockGaps(List<PDGNodeBlockGap> blockGaps) {
		refactorableBlockGaps.removeAll(blockGaps);
	}

	public Set<VariableBindingKeyPair> getLocalVariablesReturnedByBlockGaps() {
		Set<VariableBindingKeyPair> variablesReturnedByBlockGaps = new LinkedHashSet<VariableBindingKeyPair>();
		for(PDGNodeBlockGap blockGap : refactorableBlockGaps) {
			VariableBindingPair returnedVariableBinding = blockGap.getReturnedVariableBinding();
			if(returnedVariableBinding != null) {
				variablesReturnedByBlockGaps.add(returnedVariableBinding.getVariableBindingKeyPair());
			}
		}
		return variablesReturnedByBlockGaps;
	}

	private void checkCloneStructureNodeForGaps(CloneStructureNode node) {
		if(node.getMapping() != null) {
			List<PDGExpressionGap> expressionGaps = node.getExpressionGaps();
			for(PDGExpressionGap expressionGap : expressionGaps) {
				checkRefactorableExpressionGap(expressionGap);
			}
		}
		List<PDGNodeBlockGap> blockGaps = node.getSequentialBlockGaps();
		for(PDGNodeBlockGap blockGap : blockGaps) {
			checkRefactorableBlockGap(blockGap);
		}
		for(CloneStructureNode child : node.getChildren()) {
			checkCloneStructureNodeForGaps(child);
		}
	}

	private void checkRefactorableExpressionGap(PDGExpressionGap expressionGap) {
		Set<IVariableBinding> variableBindings1 = expressionGap.getUsedVariableBindingsG1();
		Set<IVariableBinding> variableBindings2 = expressionGap.getUsedVariableBindingsG2();
		Set<VariableBindingPair> parameterTypeBindings = findParametersForLambdaExpression(variableBindings1, variableBindings2);
		if(allVariableBindingsFound(variableBindings1, variableBindings2, parameterTypeBindings)) {
			boolean nonEffectivelyFinalLocalVariableIsDefinedAndUsedInMappedStatements = false;
			for(VariableBindingPair pair : parameterTypeBindings) {
				if(introduceParameter(pair) || variableIsDeclaredInNonMappedNodes(pair)) {
					expressionGap.addParameterBinding(pair);
				}
				IVariableBinding binding1 = pair.getBinding1();
				IVariableBinding binding2 = pair.getBinding2();
				if(((!binding1.isEffectivelyFinal() && (binding1.getModifiers() & Modifier.FINAL) == 0) ||
						(!binding2.isEffectivelyFinal() && (binding2.getModifiers() & Modifier.FINAL) == 0)) &&
						!variableIsDeclaredInMappedNodes(pair)) {
					expressionGap.addNonEffectivelyFinalLocalVariableBinding(pair);
					if(expressionGap.variableIsDefinedAndUsedInBlockGap(pair, finalState.getMappedNodesG1(), finalState.getMappedNodesG2())) {
						nonEffectivelyFinalLocalVariableIsDefinedAndUsedInMappedStatements = true;
					}
				}
			}
			if(!nonEffectivelyFinalLocalVariableIsDefinedAndUsedInMappedStatements) {
				refactorableExpressionGaps.add(expressionGap);
			}
		}
	}

	private void checkRefactorableBlockGap(PDGNodeBlockGap blockGap) {
		Set<IVariableBinding> variableBindings1 = blockGap.getUsedVariableBindingsG1();
		Set<IVariableBinding> variableBindings2 = blockGap.getUsedVariableBindingsG2();
		Set<VariableBindingPair> parameterTypeBindings = findParametersForLambdaExpression(variableBindings1, variableBindings2);
		if(allVariableBindingsFound(variableBindings1, variableBindings2, parameterTypeBindings)) {
			Set<IVariableBinding> variablesToBeReturnedG1 = blockGap.getVariablesToBeReturnedG1();
			Set<IVariableBinding> variablesToBeReturnedG2 = blockGap.getVariablesToBeReturnedG2();
			ITypeBinding returnTypeBinding1 = blockGap.getReturnTypeBindingFromReturnStatementG1();
			ITypeBinding returnTypeBinding2 = blockGap.getReturnTypeBindingFromReturnStatementG2();
			if(validReturnedVariables(variablesToBeReturnedG1, variablesToBeReturnedG2) && validReturnTypeBinding(returnTypeBinding1, returnTypeBinding2)) {
				boolean nonEffectivelyFinalLocalVariableIsDefinedAndUsedInBlockGap = false;
				for(VariableBindingPair pair : parameterTypeBindings) {
					if(introduceParameter(pair)) {
						if(blockGap.variableIsDefinedButNotUsedInBlockGap(pair) && variableIsDeclaredInMappedNodes(pair) &&
								!variableIsInitializedInMappedNodes(pair) && !variableIsDefinedInMappedNodesBeforeGap(pair, blockGap)) {
							continue;
						}		
						blockGap.addParameterBinding(pair);
					}
					IVariableBinding binding1 = pair.getBinding1();
					IVariableBinding binding2 = pair.getBinding2();
					if(((!binding1.isEffectivelyFinal() && (binding1.getModifiers() & Modifier.FINAL) == 0) ||
							(!binding2.isEffectivelyFinal() && (binding2.getModifiers() & Modifier.FINAL) == 0)) &&
							!variableIsDeclaredInMappedNodes(pair)) {
						blockGap.addNonEffectivelyFinalLocalVariableBinding(pair);
						if(blockGap.variableIsDefinedAndUsedInBlockGap(pair)) {
							nonEffectivelyFinalLocalVariableIsDefinedAndUsedInBlockGap = true;
						}
					}
				}
				if(variablesDefinedInMappedNodesBeforeGapAndUsedInBlockGap(blockGap.getNonEffectivelyFinalLocalVariableBindings(), blockGap)) {
					nonEffectivelyFinalLocalVariableIsDefinedAndUsedInBlockGap = true;
				}
				if(variablesToBeReturnedG1.size() == 1 && variablesToBeReturnedG2.size() == 1) {
					IVariableBinding returnedVariable1 = variablesToBeReturnedG1.iterator().next();
					IVariableBinding returnedVariable2 = variablesToBeReturnedG2.iterator().next();
					VariableBindingPair pair = new VariableBindingPair(returnedVariable1, returnedVariable2);
					blockGap.setReturnedVariableBinding(pair);
				}
				if(!nonEffectivelyFinalLocalVariableIsDefinedAndUsedInBlockGap) {
					refactorableBlockGaps.add(blockGap);
				}
			}
			else if(blockGap.isForwardsExpandable()) {
				checkRefactorableBlockGap(blockGap);
			}
		}
		else if(blockGap.isBackwardsExpandable()){
			checkRefactorableBlockGap(blockGap);
		}
	}

	private boolean introduceParameter(VariableBindingPair pair) {
		if(!pair.getBinding1().isParameter() && !pair.getBinding2().isParameter()) {
			boolean foundInCommonPassedParameters = false;
			for(VariableBindingKeyPair keyPair : commonPassedParameters.keySet()) {
				if(pair.getBinding1().getKey().equals(keyPair.getKey1()) && pair.getBinding2().getKey().equals(keyPair.getKey2())) {
					foundInCommonPassedParameters = true;
					break;
				}
			}
			if(!foundInCommonPassedParameters)
				return true;
		}
		return false;
	}

	private boolean allVariableBindingsFound(Set<IVariableBinding> variableBindings1, Set<IVariableBinding> variableBindings2,
			Set<VariableBindingPair> parameterTypeBindings) {
		boolean allVariableBindings1Found = true;
		for(IVariableBinding variableBinding1 : variableBindings1) {
			boolean found = false;
			for(VariableBindingPair pair : parameterTypeBindings) {
				if(pair.getBinding1().isEqualTo(variableBinding1)) {
					found = true;
					break;
				}
			}
			if(!found) {
				allVariableBindings1Found = false;
				break;
			}
		}
		boolean allVariableBindings2Found = true;
		for(IVariableBinding variableBinding2 : variableBindings2) {
			boolean found = false;
			for(VariableBindingPair pair : parameterTypeBindings) {
				if(pair.getBinding2().isEqualTo(variableBinding2)) {
					found = true;
					break;
				}
			}
			if(!found) {
				allVariableBindings2Found = false;
				break;
			}
		}
		boolean allPairsHaveSameType = true;
		for(VariableBindingPair pair : parameterTypeBindings) {
			ITypeBinding commonSuperType = ASTNodeMatcher.commonSuperType(pair.getBinding1().getType(), pair.getBinding2().getType());
			if(!(pair.getBinding1().getType().isEqualTo(pair.getBinding2().getType()) ||
					pair.getBinding1().getType().getQualifiedName().equals(pair.getBinding2().getType().getQualifiedName()) ||
					ASTNodeMatcher.validCommonSuperType(commonSuperType))) {
				allPairsHaveSameType = false;
				break;
			}
		}
		return allVariableBindings1Found && allVariableBindings2Found && allPairsHaveSameType;
	}

	private boolean validReturnedVariables(Set<IVariableBinding> variablesToBeReturnedG1, Set<IVariableBinding> variablesToBeReturnedG2) {
		if(variablesToBeReturnedG1.size() == 0 && variablesToBeReturnedG2.size() == 0) {
			return true;
		}
		else if(variablesToBeReturnedG1.size() == 1 && variablesToBeReturnedG2.size() == 1) {
			IVariableBinding returnedVariable1 = variablesToBeReturnedG1.iterator().next();
			IVariableBinding returnedVariable2 = variablesToBeReturnedG2.iterator().next();
			ITypeBinding commonSuperType = ASTNodeMatcher.commonSuperType(returnedVariable1.getType(), returnedVariable2.getType());
			if((returnedVariable1.getType().isEqualTo(returnedVariable2.getType()) && returnedVariable1.getType().getQualifiedName().equals(returnedVariable2.getType().getQualifiedName())) ||
					ASTNodeMatcher.validCommonSuperType(commonSuperType)) {
				return true;
			}
		}
		return false;
	}

	private boolean validReturnTypeBinding(ITypeBinding returnTypeBinding1, ITypeBinding returnTypeBinding2) {
		if(returnTypeBinding1 == null && returnTypeBinding2 == null) {
			return true;
		}
		else if(returnTypeBinding1 != null && returnTypeBinding2 != null) {
			ITypeBinding commonSuperType = ASTNodeMatcher.commonSuperType(returnTypeBinding1, returnTypeBinding2);
			if((returnTypeBinding1.isEqualTo(returnTypeBinding2) && returnTypeBinding1.getQualifiedName().equals(returnTypeBinding2.getQualifiedName())) ||
					ASTNodeMatcher.validCommonSuperType(commonSuperType)) {
				return true;
			}
		}
		return false;
	}

	private Set<VariableBindingPair> findParametersForLambdaExpression(Set<IVariableBinding> variableBindings1, Set<IVariableBinding> variableBindings2) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<VariableBindingPair> parameterTypeBindings = new LinkedHashSet<VariableBindingPair>();
		for(IVariableBinding variableBinding1 : variableBindings1) {
			VariableBindingPair pair = null;
			for(VariableBindingKeyPair keyPair: commonPassedParameters.keySet()) {
				ArrayList<VariableDeclaration> variableDeclarations = commonPassedParameters.get(keyPair);
				if(variableBinding1.isEqualTo(variableDeclarations.get(0).resolveBinding())) {
					pair = new VariableBindingPair(variableBinding1, variableDeclarations.get(1).resolveBinding(), variableDeclarations.get(1));
					break;
				}
			}
			if(pair == null) {
				for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
					PDGNode node1 = mapping.getNodeG1();
					PDGNode node2 = mapping.getNodeG2();
					Iterator<AbstractVariable> declaredVariableIterator1 = node1.getDeclaredVariableIterator();
					while(declaredVariableIterator1.hasNext()) {
						AbstractVariable variable1 = declaredVariableIterator1.next();
						if(variable1 instanceof PlainVariable) {
							PlainVariable plainVariable1 = (PlainVariable)variable1;
							if(plainVariable1.getVariableBindingKey().equals(variableBinding1.getKey())) {
								Iterator<AbstractVariable> declaredVariableIterator2 = node2.getDeclaredVariableIterator();
								while(declaredVariableIterator2.hasNext()) {
									AbstractVariable variable2 = declaredVariableIterator2.next();
									if(variable2 instanceof PlainVariable) {
										PlainVariable plainVariable2 = (PlainVariable)variable2;
										List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(node2.getASTStatement());
										for(Expression expression : variableInstructions) {
											SimpleName simpleName = (SimpleName)expression;
											IBinding binding = simpleName.resolveBinding();
											if(binding.getKind() == IBinding.VARIABLE) {
												IVariableBinding variableBinding = (IVariableBinding)binding;
												if(binding.getKey().equals(plainVariable2.getVariableBindingKey()) &&
														!alreadyMatchedLambdaParameter(parameterTypeBindings, variableBinding1, variableBinding) &&
														((variableBinding1.getType().isEqualTo(variableBinding.getType()) &&
														variableBinding1.getType().getQualifiedName().equals(variableBinding.getType().getQualifiedName())) ||
														ASTNodeMatcher.commonSuperType(variableBinding1.getType(), variableBinding.getType()) != null)) {
													pair = new VariableBindingPair(variableBinding1, variableBinding, AbstractLoopUtilities.getVariableDeclaration(simpleName));
													break;
												}
											}
										}
										if(pair != null) {
											break;
										}
									}
								}
								if(pair != null) {
									break;
								}
							}
						}
					}
					if(pair != null) {
						break;
					}
				}
			}
			if(pair != null) {
				parameterTypeBindings.add(pair);
			}
		}
		for(IVariableBinding variableBinding2 : variableBindings2) {
			VariableBindingPair pair = null;
			for(VariableBindingKeyPair keyPair: commonPassedParameters.keySet()) {
				ArrayList<VariableDeclaration> variableDeclarations = commonPassedParameters.get(keyPair);
				if(variableBinding2.isEqualTo(variableDeclarations.get(1).resolveBinding())) {
					pair = new VariableBindingPair(variableDeclarations.get(0).resolveBinding(), variableBinding2, variableDeclarations.get(0));
					break;
				}
			}
			if(pair == null) {
				for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
					PDGNode node1 = mapping.getNodeG1();
					PDGNode node2 = mapping.getNodeG2();
					Iterator<AbstractVariable> declaredVariableIterator2 = node2.getDeclaredVariableIterator();
					while(declaredVariableIterator2.hasNext()) {
						AbstractVariable variable2 = declaredVariableIterator2.next();
						if(variable2 instanceof PlainVariable) {
							PlainVariable plainVariable2 = (PlainVariable)variable2;
							if(plainVariable2.getVariableBindingKey().equals(variableBinding2.getKey())) {
								Iterator<AbstractVariable> declaredVariableIterator1 = node1.getDeclaredVariableIterator();
								while(declaredVariableIterator1.hasNext()) {
									AbstractVariable variable1 = declaredVariableIterator1.next();
									if(variable1 instanceof PlainVariable) {
										PlainVariable plainVariable1 = (PlainVariable)variable1;
										List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(node1.getASTStatement());
										for(Expression expression : variableInstructions) {
											SimpleName simpleName = (SimpleName)expression;
											IBinding binding = simpleName.resolveBinding();
											if(binding.getKind() == IBinding.VARIABLE) {
												IVariableBinding variableBinding = (IVariableBinding)binding;
												if(binding.getKey().equals(plainVariable1.getVariableBindingKey()) &&
														!alreadyMatchedLambdaParameter(parameterTypeBindings, variableBinding, variableBinding2) &&
														((variableBinding.getType().isEqualTo(variableBinding2.getType()) &&
														variableBinding.getType().getQualifiedName().equals(variableBinding2.getType().getQualifiedName())) ||
														ASTNodeMatcher.commonSuperType(variableBinding.getType(), variableBinding2.getType()) != null)) {
													pair = new VariableBindingPair(variableBinding, variableBinding2, AbstractLoopUtilities.getVariableDeclaration(simpleName));
													break;
												}
											}
										}
										if(pair != null) {
											break;
										}
									}
								}
								if(pair != null) {
									break;
								}
							}
						}
					}
					if(pair != null) {
						break;
					}
				}
			}
			if(pair != null) {
				parameterTypeBindings.add(pair);
			}
		}
		return parameterTypeBindings;
	}

	private boolean alreadyMatchedLambdaParameter(Set<VariableBindingPair> parameterTypeBindings,
			IVariableBinding binding1, IVariableBinding binding2) {
		for(VariableBindingPair pair : parameterTypeBindings) {
			if(pair.getBinding1().isEqualTo(binding1) || pair.getBinding2().isEqualTo(binding2))
				return true;
		}
		return false;
	}

	private boolean variableIsDeclaredInNonMappedNodes(VariableBindingPair pair) {
		IVariableBinding binding1 = pair.getBinding1();
		PlainVariable variable1 = new PlainVariable(binding1.getKey(), binding1.getName(),
				binding1.getType().getQualifiedName(), binding1.isField(), binding1.isParameter(), (binding1.getModifiers() & Modifier.STATIC) != 0);
		IVariableBinding binding2 = pair.getBinding2();
		PlainVariable variable2 = new PlainVariable(binding2.getKey(), binding2.getName(),
				binding2.getType().getQualifiedName(), binding2.isField(), binding2.isParameter(), (binding2.getModifiers() & Modifier.STATIC) != 0);
		boolean variable1DeclaredInNonMappedNode = false;
		for(PDGNode nonMappedNodeG1 : nonMappedNodesG1) {
			if(nonMappedNodeG1.declaresLocalVariable(variable1)) {
				variable1DeclaredInNonMappedNode = true;
				break;
			}
		}
		boolean variable2DeclaredInNonMappedNode = false;
		for(PDGNode nonMappedNodeG2 : nonMappedNodesG2) {
			if(nonMappedNodeG2.declaresLocalVariable(variable2)) {
				variable2DeclaredInNonMappedNode = true;
				break;
			}
		}
		return variable1DeclaredInNonMappedNode && variable2DeclaredInNonMappedNode;
	}

	private boolean variableIsDeclaredInMappedNodes(VariableBindingPair pair) {
		IVariableBinding binding1 = pair.getBinding1();
		PlainVariable variable1 = new PlainVariable(binding1.getKey(), binding1.getName(),
				binding1.getType().getQualifiedName(), binding1.isField(), binding1.isParameter(), (binding1.getModifiers() & Modifier.STATIC) != 0);
		IVariableBinding binding2 = pair.getBinding2();
		PlainVariable variable2 = new PlainVariable(binding2.getKey(), binding2.getName(),
				binding2.getType().getQualifiedName(), binding2.isField(), binding2.isParameter(), (binding2.getModifiers() & Modifier.STATIC) != 0);
		for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
			PDGNode node1 = mapping.getNodeG1();
			PDGNode node2 = mapping.getNodeG2();
			if(node1.declaresLocalVariable(variable1) && node2.declaresLocalVariable(variable2)) {
				return true;
			}
		}
		return false;
	}

	private boolean variableIsDefinedInMappedNodesBeforeGap(VariableBindingPair pair, PDGNodeBlockGap blockGap) {
		IVariableBinding binding1 = pair.getBinding1();
		PlainVariable variable1 = new PlainVariable(binding1.getKey(), binding1.getName(),
				binding1.getType().getQualifiedName(), binding1.isField(), binding1.isParameter(), (binding1.getModifiers() & Modifier.STATIC) != 0);
		IVariableBinding binding2 = pair.getBinding2();
		PlainVariable variable2 = new PlainVariable(binding2.getKey(), binding2.getName(),
				binding2.getType().getQualifiedName(), binding2.isField(), binding2.isParameter(), (binding2.getModifiers() & Modifier.STATIC) != 0);
		int blockGapFirstNodeId1 = blockGap.getNodesG1().isEmpty() ? finalState.getMappedNodesG1().last().getId() : blockGap.getNodesG1().first().getId();
		int blockGapFirstNodeId2 = blockGap.getNodesG2().isEmpty() ? finalState.getMappedNodesG2().last().getId() : blockGap.getNodesG2().first().getId();
		for(PDGNodeMapping mapping : finalState.getSortedNodeMappings()) {
			PDGNode node1 = mapping.getNodeG1();
			PDGNode node2 = mapping.getNodeG2();
			if(node1.getId() < blockGapFirstNodeId1 && node2.getId() < blockGapFirstNodeId2 &&
					node1.definesLocalVariable(variable1) && node2.definesLocalVariable(variable2) &&
					!node1.declaresLocalVariable(variable1) && !node2.declaresLocalVariable(variable2)) {
				return true;
			}
		}
		return false;
	}

	private boolean variablesDefinedInMappedNodesBeforeGapAndUsedInBlockGap(Set<VariableBindingPair> nonEffectivelyFinalLocalVariableBindings, PDGNodeBlockGap blockGap) {
		boolean allVariablesDefinedInMappedNodes = true;
		boolean allVariablesUsedInBlockGap = true;
		for(VariableBindingPair pair : nonEffectivelyFinalLocalVariableBindings) {
			IVariableBinding binding1 = pair.getBinding1();
			PlainVariable variable1 = new PlainVariable(binding1.getKey(), binding1.getName(),
					binding1.getType().getQualifiedName(), binding1.isField(), binding1.isParameter(), (binding1.getModifiers() & Modifier.STATIC) != 0);
			IVariableBinding binding2 = pair.getBinding2();
			PlainVariable variable2 = new PlainVariable(binding2.getKey(), binding2.getName(),
					binding2.getType().getQualifiedName(), binding2.isField(), binding2.isParameter(), (binding2.getModifiers() & Modifier.STATIC) != 0);
			int blockGapFirstNodeId1 = blockGap.getNodesG1().isEmpty() ? finalState.getMappedNodesG1().last().getId() : blockGap.getNodesG1().first().getId();
			int blockGapFirstNodeId2 = blockGap.getNodesG2().isEmpty() ? finalState.getMappedNodesG2().last().getId() : blockGap.getNodesG2().first().getId();
			boolean definedVariableFound = false;
			for(PDGNodeMapping mapping : finalState.getSortedNodeMappings()) {
				PDGNode node1 = mapping.getNodeG1();
				PDGNode node2 = mapping.getNodeG2();
				if(node1.getId() < blockGapFirstNodeId1 && node2.getId() < blockGapFirstNodeId2 &&
						node1.definesLocalVariable(variable1) || node2.definesLocalVariable(variable2)) {
					definedVariableFound = true;
				}
			}
			if(!definedVariableFound) {
				allVariablesDefinedInMappedNodes = false;
				break;
			}
			if(!blockGap.variableIsUsedInBlockGap(pair)) {
				allVariablesUsedInBlockGap = false;
				break;
			}
		}
		return nonEffectivelyFinalLocalVariableBindings.size() > 0 && allVariablesDefinedInMappedNodes && allVariablesUsedInBlockGap;
	}

	private boolean variableIsInitializedInMappedNodes(VariableBindingPair pair) {
		IVariableBinding binding1 = pair.getBinding1();
		PlainVariable variable1 = new PlainVariable(binding1.getKey(), binding1.getName(),
				binding1.getType().getQualifiedName(), binding1.isField(), binding1.isParameter(), (binding1.getModifiers() & Modifier.STATIC) != 0);
		IVariableBinding binding2 = pair.getBinding2();
		PlainVariable variable2 = new PlainVariable(binding2.getKey(), binding2.getName(),
				binding2.getType().getQualifiedName(), binding2.isField(), binding2.isParameter(), (binding2.getModifiers() & Modifier.STATIC) != 0);
		for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
			PDGNode node1 = mapping.getNodeG1();
			PDGNode node2 = mapping.getNodeG2();
			if(node1.declaresLocalVariable(variable1) && node2.declaresLocalVariable(variable2)) {
				boolean variable1IsInitialized = false;
				List<LocalVariableDeclarationObject> localVariableDeclarations1 = node1.getStatement().getLocalVariableDeclarations();
				for(LocalVariableDeclarationObject localVariableDeclaration : localVariableDeclarations1) {
					if(localVariableDeclaration.getVariableBindingKey().equals(binding1.getKey())) {
						if(localVariableDeclaration.getVariableDeclaration().getInitializer() != null) {
							variable1IsInitialized = true;
							break;
						}
					}
				}
				boolean variable2IsInitialized = false;
				List<LocalVariableDeclarationObject> localVariableDeclarations2 = node2.getStatement().getLocalVariableDeclarations();
				for(LocalVariableDeclarationObject localVariableDeclaration : localVariableDeclarations2) {
					if(localVariableDeclaration.getVariableBindingKey().equals(binding2.getKey())) {
						if(localVariableDeclaration.getVariableDeclaration().getInitializer() != null) {
							variable2IsInitialized = true;
							break;
						}
					}
				}
				return variable1IsInitialized && variable2IsInitialized;
			}
		}
		return false;
	}
}
