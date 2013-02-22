package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.GraphNode;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class PDGMapper {
	private PDG pdg1;
	private PDG pdg2;
	private List<MappingState> maximumStates;
	private MappingState maximumStateWithMinimumDifferences;
	private Set<PDGNode> nonMappedNodesG1;
	private Set<PDGNode> nonMappedNodesG2;
	private Map<String, ArrayList<VariableDeclaration>> commonPassedParameters;
	private Set<VariableDeclaration> passedParametersG1;
	private Set<VariableDeclaration> passedParametersG2;
	private IProgressMonitor monitor;
	
	public PDGMapper(PDG pdg1, PDG pdg2, IProgressMonitor monitor) {
		this.pdg1 = pdg1;
		this.pdg2 = pdg2;
		this.maximumStates = new ArrayList<MappingState>();
		this.nonMappedNodesG1 = new LinkedHashSet<PDGNode>();
		this.nonMappedNodesG2 = new LinkedHashSet<PDGNode>();
		this.commonPassedParameters = new LinkedHashMap<String, ArrayList<VariableDeclaration>>();
		this.passedParametersG1 = new LinkedHashSet<VariableDeclaration>();
		this.passedParametersG2 = new LinkedHashSet<VariableDeclaration>();
		this.monitor = monitor;
		processPDGNodes();
		this.maximumStateWithMinimumDifferences = findMaximumStateWithMinimumDifferences();
		Iterator<GraphNode> iterator1 = pdg1.getNodeIterator();
		while(iterator1.hasNext()) {
			PDGNode pdgNode = (PDGNode)iterator1.next();
			if(!maximumStateWithMinimumDifferences.containsNodeG1(pdgNode)) {
				nonMappedNodesG1.add(pdgNode);
			}
		}
		Iterator<GraphNode> iterator2 = pdg2.getNodeIterator();
		while(iterator2.hasNext()) {
			PDGNode pdgNode = (PDGNode)iterator2.next();
			if(!maximumStateWithMinimumDifferences.containsNodeG2(pdgNode)) {
				nonMappedNodesG2.add(pdgNode);
			}
		}
		findPassedParameters();
	}

	private MappingState findMaximumStateWithMinimumDifferences() {
		MappingState maximumStateWithMinimumDifferences = null;
		if(maximumStates.size() == 1) {
			maximumStateWithMinimumDifferences = maximumStates.get(0);
		}
		else {
			int minimum = maximumStates.get(0).getDifferenceCount();
			maximumStateWithMinimumDifferences = maximumStates.get(0);
			for(int i=1; i<maximumStates.size(); i++) {
				MappingState currentState = maximumStates.get(i);
				if(currentState.getDifferenceCount() < minimum) {
					minimum = currentState.getDifferenceCount();
					maximumStateWithMinimumDifferences = currentState;
				}
			}
		}
		return maximumStateWithMinimumDifferences;
	}

	private void findPassedParameters() {
		Set<AbstractVariable> passedParametersG1 = new LinkedHashSet<AbstractVariable>();
		for(GraphEdge edge : pdg1.getEdges()) {
			PDGDependence dependence = (PDGDependence)edge;
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			PDGNode dstPDGNode = (PDGNode)dependence.getDst();
			if(dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				if(!maximumStateWithMinimumDifferences.containsNodeG1(srcPDGNode) &&
						maximumStateWithMinimumDifferences.containsNodeG1(dstPDGNode)) {
					passedParametersG1.add(dataDependence.getData());
				}
			}
		}
		Set<AbstractVariable> passedParametersG2 = new LinkedHashSet<AbstractVariable>();
		for(GraphEdge edge : pdg2.getEdges()) {
			PDGDependence dependence = (PDGDependence)edge;
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			PDGNode dstPDGNode = (PDGNode)dependence.getDst();
			if(dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				if(!maximumStateWithMinimumDifferences.containsNodeG2(srcPDGNode) &&
						maximumStateWithMinimumDifferences.containsNodeG2(dstPDGNode)) {
					passedParametersG2.add(dataDependence.getData());
				}
			}
		}
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = pdg1.getVariableDeclarationsAndAccessedFieldsInMethod();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = pdg2.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(PDGEdgeMapping edgeMapping : maximumStateWithMinimumDifferences.getEdgeMappings()) {
			PDGDependence edgeG1 = edgeMapping.getEdgeG1();
			PDGDependence edgeG2 = edgeMapping.getEdgeG2();
			if(edgeG1 instanceof PDGDataDependence && edgeG2 instanceof PDGDataDependence) {
				PDGDataDependence dataEdgeG1 = (PDGDataDependence)edgeG1;
				PDGDataDependence dataEdgeG2 = (PDGDataDependence)edgeG2;
				if(passedParametersG1.contains(dataEdgeG1.getData()) && passedParametersG2.contains(dataEdgeG2.getData())) {
					ArrayList<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
					for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
						if(variableDeclaration.resolveBinding().getKey().equals(dataEdgeG1.getData().getVariableBindingKey())) {
							variableDeclarations.add(variableDeclaration);
							break;
						}
					}
					for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
						if(variableDeclaration.resolveBinding().getKey().equals(dataEdgeG2.getData().getVariableBindingKey())) {
							variableDeclarations.add(variableDeclaration);
							break;
						}
					}
					commonPassedParameters.put(dataEdgeG1.getData().toString(), variableDeclarations);
					passedParametersG1.remove(dataEdgeG1.getData());
					passedParametersG2.remove(dataEdgeG2.getData());
				}
			}
		}
		for(AbstractVariable variable1 : passedParametersG1) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable1.getVariableBindingKey())) {
					this.passedParametersG1.add(variableDeclaration);
					break;
				}
			}
		}
		for(AbstractVariable variable2 : passedParametersG2) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable2.getVariableBindingKey())) {
					this.passedParametersG2.add(variableDeclaration);
					break;
				}
			}
		}
	}

	public PDG getPDG1() {
		return pdg1;
	}

	public PDG getPDG2() {
		return pdg2;
	}

	public MappingState getMaximumStateWithMinimumDifferences() {
		return maximumStateWithMinimumDifferences;
	}

	public Set<PDGNode> getNonMappedNodesG1() {
		return nonMappedNodesG1;
	}

	public Set<PDGNode> getNonMappedNodesG2() {
		return nonMappedNodesG2;
	}

	public Map<String, ArrayList<VariableDeclaration>> getCommonPassedParameters() {
		return commonPassedParameters;
	}

	public Set<VariableDeclaration> getPassedParametersG1() {
		return passedParametersG1;
	}

	public Set<VariableDeclaration> getPassedParametersG2() {
		return passedParametersG2;
	}

	private void processPDGNodes() {
		if(monitor != null)
			monitor.beginTask("Mapping Program Dependence Graphs", pdg1.getTotalNumberOfStatements() * pdg2.getTotalNumberOfStatements());
		List<MappingState> finalStates = new ArrayList<MappingState>();
		Iterator<GraphNode> nodeIterator1 = pdg1.getNodeIterator();
		while(nodeIterator1.hasNext()) {
			PDGNode node1 = (PDGNode)nodeIterator1.next();
			Iterator<GraphNode> nodeIterator2 = pdg2.getNodeIterator();
			List<MappingState> currentStates = new ArrayList<MappingState>();
			while(nodeIterator2.hasNext()) {
				PDGNode node2 = (PDGNode)nodeIterator2.next();
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher();
				boolean match = node1.getASTStatement().subtreeMatch(astNodeMatcher, node2.getASTStatement());
				if(match) {
					PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher.getDifferences());
					if(finalStates.isEmpty()) {
						MappingState state = new MappingState(null, mapping);
						List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
						for(MappingState temp : maxStates) {
							if(!currentStates.contains(temp)) {
								currentStates.add(temp);
							}
						}
					}
					else {
						for(MappingState previousState : finalStates) {
							MappingState state = new MappingState(previousState, mapping);
							List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
							for(MappingState temp : maxStates) {
								if(!currentStates.contains(temp)) {
									currentStates.add(temp);
								}
							}
						}
					}
				}
				if(monitor != null)
					monitor.worked(1);
			}
			if(!currentStates.isEmpty())
				finalStates = getMaximumStates(currentStates);
		}
		maximumStates = finalStates;
		if(monitor != null)
			monitor.done();
	}
	
	private List<MappingState> getMaximumStates(List<MappingState> currentStates) {
		int max = 0;
		List<MappingState> maximumStates = new ArrayList<MappingState>();
		for(MappingState state : currentStates) {
			if(state.getSize() > max) {
				max = state.getSize();
				maximumStates.clear();
				maximumStates.add(state);
			}
			else if(state.getSize() == max) {
				maximumStates.add(state);
			}
		}
		return maximumStates;
	}
}
