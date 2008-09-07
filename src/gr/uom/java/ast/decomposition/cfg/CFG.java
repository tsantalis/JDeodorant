package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class CFG extends Graph {
	
	public CFG(MethodBodyObject methodBody) {
		CompositeStatementObject composite = methodBody.getCompositeStatement();
		process(new ArrayList<CFGNode>(), composite);
		GraphNode.resetNodeNum();
	}
	
	private List<CFGNode> process(List<CFGNode> previousNodes, CompositeStatementObject composite) {
		for(AbstractStatement abstractStatement : composite.getStatements()) {
			if(abstractStatement instanceof StatementObject) {
				StatementObject statement = (StatementObject)abstractStatement;
				//special handling of break, continue, return
				CFGNode currentNode = new CFGNode(statement);
				nodes.add(currentNode);
				createTopDownFlow(previousNodes, currentNode);
				ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
				currentNodes.add(currentNode);
				previousNodes = currentNodes;
			}
			else if(abstractStatement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatement = (CompositeStatementObject)abstractStatement;
				if(compositeStatement.getStatement() instanceof Block) {
					previousNodes = process(previousNodes, compositeStatement);
				}
				else if(isLoop(compositeStatement)) {
					CFGBranchNode currentNode = new CFGBranchNode(compositeStatement);
					nodes.add(currentNode);
					createTopDownFlow(previousNodes, currentNode);
					previousNodes = new ArrayList<CFGNode>();
					ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
					currentNodes.add(currentNode);
					previousNodes.addAll(process(currentNodes, compositeStatement));
					for(CFGNode previousNode : previousNodes) {
						Flow flow = new Flow(previousNode, currentNode);
						if(previousNode instanceof CFGBranchNode) {
							flow.setFalseControlFlow(true);
						}
						flow.setLoopbackFlow(true);
						edges.add(flow);
					}
					previousNodes = currentNodes;
				}
				else if(compositeStatement.getStatement() instanceof DoStatement) {
					List<CFGNode> tmpNodes = previousNodes;
					previousNodes = process(previousNodes, compositeStatement);
					CFGBranchNode currentNode = new CFGBranchNode(compositeStatement);
					nodes.add(currentNode);
					createTopDownFlow(previousNodes, currentNode);
					CFGNode topNode = getCommonNextNode(tmpNodes);
					Flow flow = new Flow(currentNode, topNode);
					flow.setTrueControlFlow(true);
					flow.setLoopbackFlow(true);
					edges.add(flow);
					ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
					currentNodes.add(currentNode);
					previousNodes = currentNodes;
				}
				else if(compositeStatement.getStatement() instanceof IfStatement) {
					previousNodes = processIfStatement(previousNodes, compositeStatement);
				}
			}
		}
		return previousNodes;
	}

	private List<CFGNode> processIfStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement) {
		CFGBranchNode currentNode = new CFGBranchNode(compositeStatement);
		nodes.add(currentNode);
		createTopDownFlow(previousNodes, currentNode);
		previousNodes = new ArrayList<CFGNode>();
		List<AbstractStatement> ifStatementList = compositeStatement.getStatements();
		AbstractStatement thenClause = ifStatementList.get(0);
		if(thenClause instanceof StatementObject) {
			StatementObject thenClauseStatement = (StatementObject)thenClause;
			CFGNode thenClauseNode = new CFGNode(thenClauseStatement);
			nodes.add(thenClauseNode);
			ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
			currentNodes.add(currentNode);
			createTopDownFlow(currentNodes, thenClauseNode);
			previousNodes.add(thenClauseNode);
		}
		else if(thenClause instanceof CompositeStatementObject) {
			CompositeStatementObject thenClauseCompositeStatement = (CompositeStatementObject)thenClause;
			ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
			currentNodes.add(currentNode);
			if(thenClauseCompositeStatement.getStatement() instanceof IfStatement)
				previousNodes.addAll(processIfStatement(currentNodes, thenClauseCompositeStatement));
			else
				previousNodes.addAll(process(currentNodes, thenClauseCompositeStatement));
		}
		if(ifStatementList.size() == 2) {
			AbstractStatement elseClause = ifStatementList.get(1);
			if(elseClause instanceof StatementObject) {
				StatementObject elseClauseStatement = (StatementObject)elseClause;
				CFGNode elseClauseNode = new CFGNode(elseClauseStatement);
				nodes.add(elseClauseNode);
				ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
				currentNodes.add(currentNode);
				createTopDownFlow(currentNodes, elseClauseNode);
				previousNodes.add(elseClauseNode);
			}
			else if(elseClause instanceof CompositeStatementObject) {
				CompositeStatementObject elseClauseCompositeStatement = (CompositeStatementObject)elseClause;
				ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
				currentNodes.add(currentNode);
				if(elseClauseCompositeStatement.getStatement() instanceof IfStatement)
					previousNodes.addAll(processIfStatement(currentNodes, elseClauseCompositeStatement));
				else
					previousNodes.addAll(process(currentNodes, elseClauseCompositeStatement));
			}
		}
		else {
			previousNodes.add(currentNode);
		}
		return previousNodes;
	}

	private void createTopDownFlow(List<CFGNode> previousNodes, CFGNode currentNode) {
		for(CFGNode previousNode : previousNodes) {
			Flow flow = new Flow(previousNode, currentNode);
			if(previousNode instanceof CFGBranchNode) {
				if(currentNode.getId() == previousNode.getId() + 1 &&
						!(previousNode.getASTStatement() instanceof DoStatement))
					flow.setTrueControlFlow(true);
				else
					flow.setFalseControlFlow(true);
			}
			edges.add(flow);
		}
	}

	private boolean isLoop(CompositeStatementObject compositeStatement) {
		if(compositeStatement.getStatement() instanceof WhileStatement ||
				compositeStatement.getStatement() instanceof ForStatement ||
				compositeStatement.getStatement() instanceof EnhancedForStatement)
			return true;
		return false;
	}

	private CFGNode getCommonNextNode(List<CFGNode> nodes) {
		HashMap<CFGNode, Integer> nextNodeCounterMap = new HashMap<CFGNode, Integer>();
		for(CFGNode node : nodes) {
			for(GraphEdge edge : node.outgoingEdges) {
				CFGNode nextNode = (CFGNode)edge.dst;
				if(nextNodeCounterMap.containsKey(nextNode))
					nextNodeCounterMap.put(nextNode, nextNodeCounterMap.get(nextNode)+1);
				else
					nextNodeCounterMap.put(nextNode, 1);
			}
		}
		for(CFGNode key : nextNodeCounterMap.keySet()) {
			if(nextNodeCounterMap.get(key) == nodes.size())
				return key;
		}
		return null;
	}
}
