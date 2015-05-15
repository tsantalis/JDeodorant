package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class LambdaExpressionPreconditionExaminer {

	private MappingState finalState;
	private Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> commonPassedParameters;
	private List<PDGNodeBlockGap> refactorableBlockGaps;

	public LambdaExpressionPreconditionExaminer(CloneStructureNode cloneStructureRoot, MappingState finalState, 
			Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> commonPassedParameters) {
		this.finalState = finalState;
		this.commonPassedParameters = commonPassedParameters;
		this.refactorableBlockGaps = new ArrayList<PDGNodeBlockGap>();
		checkCloneStructureNodeForBlockGaps(cloneStructureRoot);
	}

	public List<PDGNodeBlockGap> getRefactorableBlockGaps() {
		return refactorableBlockGaps;
	}

	private void checkCloneStructureNodeForBlockGaps(CloneStructureNode node) {
		if(node.getMapping() != null) {
			List<PDGNodeBlockGap> blockGaps = node.getSequentialBlockGaps();
			for(PDGNodeBlockGap blockGap : blockGaps) {
				checkRefactorableBlockGap(blockGap);
			}
		}
		for(CloneStructureNode child : node.getChildren()) {
			checkCloneStructureNodeForBlockGaps(child);
		}
	}

	private void checkRefactorableBlockGap(PDGNodeBlockGap blockGap) {
		Set<IVariableBinding> variableBindings1 = blockGap.getUsedVariableBindingsG1();
		Set<IVariableBinding> variableBindings2 = blockGap.getUsedVariableBindingsG2();
		Set<VariableBindingPair> parameterTypeBindings = findParametersForLambdaExpression(variableBindings1, variableBindings2);
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
		if(allVariableBindings1Found && allVariableBindings2Found) {
			Set<IVariableBinding> variablesToBeReturnedG1 = blockGap.getVariablesToBeReturnedG1();
			Set<IVariableBinding> variablesToBeReturnedG2 = blockGap.getVariablesToBeReturnedG2();
			if(validReturnedVariables(variablesToBeReturnedG1, variablesToBeReturnedG2)) {
				blockGap.setParameterBindings(parameterTypeBindings);
				if(variablesToBeReturnedG1.size() == 1 && variablesToBeReturnedG2.size() == 1) {
					IVariableBinding returnedVariable1 = variablesToBeReturnedG1.iterator().next();
					IVariableBinding returnedVariable2 = variablesToBeReturnedG2.iterator().next();
					VariableBindingPair pair = new VariableBindingPair(returnedVariable1, returnedVariable2);
					blockGap.setReturnedVariableBinding(pair);
				}
				refactorableBlockGaps.add(blockGap);
			}
			else if(blockGap.isExpandable()) {
				checkRefactorableBlockGap(blockGap);
			}
		}
	}

	private boolean validReturnedVariables(Set<IVariableBinding> variablesToBeReturnedG1, Set<IVariableBinding> variablesToBeReturnedG2) {
		if(variablesToBeReturnedG1.size() == 0 && variablesToBeReturnedG2.size() == 0) {
			return true;
		}
		else if(variablesToBeReturnedG1.size() == 1 && variablesToBeReturnedG2.size() == 1) {
			IVariableBinding returnedVariable1 = variablesToBeReturnedG1.iterator().next();
			IVariableBinding returnedVariable2 = variablesToBeReturnedG2.iterator().next();
			if(returnedVariable1.getType().isEqualTo(returnedVariable2.getType())) {
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
					pair = new VariableBindingPair(variableBinding1, variableDeclarations.get(1).resolveBinding());
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
														(variableBinding1.getType().isEqualTo(variableBinding.getType()) ||
														ASTNodeMatcher.commonSuperType(variableBinding1.getType(), variableBinding.getType()) != null)) {
													pair = new VariableBindingPair(variableBinding1, variableBinding);
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
					pair = new VariableBindingPair(variableDeclarations.get(0).resolveBinding(), variableBinding2);
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
														(variableBinding.getType().isEqualTo(variableBinding2.getType()) ||
														ASTNodeMatcher.commonSuperType(variableBinding.getType(), variableBinding2.getType()) != null)) {
													pair = new VariableBindingPair(variableBinding, variableBinding2);
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

}
