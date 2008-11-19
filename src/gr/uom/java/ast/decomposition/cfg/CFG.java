package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class CFG extends Graph {
	private static final int PUSH_NEW_LIST = 0;
	private static final int JOIN_TOP_LIST = 1;
	private static final int PLACE_NEW_LIST_SECOND_FROM_TOP = 2;
	private static final int JOIN_SECOND_FROM_TOP_LIST = 3;
	private MethodObject method;
	private Stack<List<CFGBranchConditionalNode>> unjoinedConditionalNodes;
	private BasicBlockCFG basicBlockCFG;
	
	public CFG(MethodObject method) {
		this.method = method;
		this.unjoinedConditionalNodes = new Stack<List<CFGBranchConditionalNode>>();
		MethodBodyObject methodBody = method.getMethodBody();
		if(methodBody != null) {
			CompositeStatementObject composite = methodBody.getCompositeStatement();
			process(new ArrayList<CFGNode>(), composite);
			GraphNode.resetNodeNum();
			this.basicBlockCFG = new BasicBlockCFG(this);
		}
	}

	public MethodObject getMethod() {
		return method;
	}

	public BasicBlockCFG getBasicBlockCFG() {
		return basicBlockCFG;
	}

	public List<BasicBlock> getBasicBlocks() {
		return basicBlockCFG.getBasicBlocks();
	}

	private List<CFGNode> process(List<CFGNode> previousNodes, CompositeStatementObject composite) {
		if(composite.getStatement() instanceof TryStatement) {
			AbstractStatement firstStatement = composite.getStatements().get(0);
			composite = (CompositeStatementObject)firstStatement;
		}
		int i = 0;
		for(AbstractStatement abstractStatement : composite.getStatements()) {
			if(abstractStatement instanceof StatementObject) {
				StatementObject statement = (StatementObject)abstractStatement;
				previousNodes = processNonCompositeStatement(previousNodes, statement);
			}
			else if(abstractStatement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatement = (CompositeStatementObject)abstractStatement;
				if(compositeStatement.getStatement() instanceof Block) {
					previousNodes = process(previousNodes, compositeStatement);
				}
				else if(compositeStatement.getStatement() instanceof TryStatement) {
					AbstractStatement firstStatement = compositeStatement.getStatements().get(0);
					previousNodes = process(previousNodes, (CompositeStatementObject)firstStatement);
				}
				else if(isLoop(compositeStatement)) {
					CFGBranchNode currentNode = new CFGBranchLoopNode(compositeStatement);
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
					if(previousNodes.size() > 1) {
						List<CFGBranchConditionalNode> conditionalNodes = unjoinedConditionalNodes.pop();
						for(CFGBranchConditionalNode conditionalNode : conditionalNodes) {
							conditionalNode.setJoinNode(currentNode);
						}
					}
					previousNodes = currentNodes;
				}
				else if(compositeStatement.getStatement() instanceof DoStatement) {
					List<CFGNode> tmpNodes = previousNodes;
					previousNodes = process(previousNodes, compositeStatement);
					CFGBranchNode currentNode = new CFGBranchDoLoopNode(compositeStatement);
					nodes.add(currentNode);
					createTopDownFlow(previousNodes, currentNode);
					CFGNode topNode = getCommonNextNode(tmpNodes);
					if(topNode == null)
						topNode = (CFGNode)nodes.toArray()[0];
					Flow flow = new Flow(currentNode, topNode);
					flow.setTrueControlFlow(true);
					flow.setLoopbackFlow(true);
					edges.add(flow);
					ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
					currentNodes.add(currentNode);
					previousNodes = currentNodes;
				}
				else if(compositeStatement.getStatement() instanceof IfStatement) {
					int action = PUSH_NEW_LIST;
					List<AbstractStatement> statements = new ArrayList<AbstractStatement>(composite.getStatements());
					CompositeStatementObject parent = statements.get(0).getParent();
					if(parent.getStatement() instanceof Block)
						parent = parent.getParent();
					int position = i;
					while(parent != null && parent.getStatement() instanceof TryStatement) {
						CompositeStatementObject tryStatement = parent;
						CompositeStatementObject tryStatementParent = tryStatement.getParent();
						List<AbstractStatement> tryParentStatements = new ArrayList<AbstractStatement>(tryStatementParent.getStatements());
						if(tryStatementParent.getStatement() instanceof Block)
							tryStatementParent = tryStatementParent.getParent();
						int positionOfTryStatementInParent = 0;
						int j = 0;
						for(AbstractStatement statement : tryParentStatements) {
							if(statement.equals(tryStatement)) {
								positionOfTryStatementInParent = j;
								break;
							}
							j++;
						}
						tryParentStatements.remove(tryStatement);
						tryParentStatements.addAll(positionOfTryStatementInParent, statements);
						statements = tryParentStatements;
						parent = tryStatementParent;
						position = positionOfTryStatementInParent + position;
					}
					if(statements.size() == 1) {
						action = JOIN_TOP_LIST;
						if(parent != null) {
							if(isLoop(parent))
								action = PUSH_NEW_LIST;
							else if(parent.getStatement() instanceof DoStatement)
								action = PLACE_NEW_LIST_SECOND_FROM_TOP;
						}
					}
					else if(statements.size() > 1) {
						AbstractStatement previousStatement = null;
						if(position >= 1)
							previousStatement = statements.get(position-1);
						int j = 0;
						while(previousStatement != null && previousStatement.getStatement() instanceof TryStatement) {
							CompositeStatementObject tryStatement = (CompositeStatementObject)previousStatement;
							AbstractStatement firstStatement = tryStatement.getStatements().get(0);
							if(firstStatement instanceof CompositeStatementObject) {
								CompositeStatementObject tryBlock = (CompositeStatementObject)firstStatement;
								List<AbstractStatement> tryBlockStatements = tryBlock.getStatements();
								if(tryBlockStatements.size() > 0) {
									//previous statement is the last statement of this try block
									previousStatement = tryBlockStatements.get(tryBlockStatements.size()-1);
								}
								else {
									//try block is empty and previous statement is the statement before this try block
									if(position >= 2+j)
										previousStatement = statements.get(position-2-j);
									else
										previousStatement = null;
								}
							}
							j++;
						}
						if(statements.get(statements.size()-1).equals(compositeStatement)) {
							//current if statement is the last statement of the composite statement
							if(previousStatement != null && previousStatement.getStatement() instanceof IfStatement) {
								action = JOIN_SECOND_FROM_TOP_LIST;
								if(parent != null && (isLoop(parent) || parent.getStatement() instanceof DoStatement))
									action = PLACE_NEW_LIST_SECOND_FROM_TOP;
							}
							else {
								action = JOIN_TOP_LIST;
								if(parent != null && (isLoop(parent) || parent.getStatement() instanceof DoStatement))
									action = PUSH_NEW_LIST;
							}
						}
						else {
							if(previousStatement != null && previousStatement.getStatement() instanceof IfStatement)
								action = PLACE_NEW_LIST_SECOND_FROM_TOP;
							else {
								action = PUSH_NEW_LIST;
								if(parent != null && parent.getStatement() instanceof DoStatement &&
										statements.get(0).getStatement() instanceof IfStatement)
									action = PLACE_NEW_LIST_SECOND_FROM_TOP;
							}
						}
					}
					previousNodes = processIfStatement(previousNodes, compositeStatement, action);
				}
			}
			i++;
		}
		return previousNodes;
	}

	private List<CFGNode> processNonCompositeStatement(List<CFGNode> previousNodes, StatementObject statement) {
		//special handling of break, continue, return
		CFGNode currentNode = null;
		if(statement.getStatement() instanceof ReturnStatement)
			currentNode = new CFGExitNode(statement);
		else
			currentNode = new CFGNode(statement);
		nodes.add(currentNode);
		createTopDownFlow(previousNodes, currentNode);
		ArrayList<CFGNode> currentNodes = new ArrayList<CFGNode>();
		currentNodes.add(currentNode);
		previousNodes = currentNodes;
		return previousNodes;
	}

	private List<CFGNode> processIfStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement, int action) {
		CFGBranchConditionalNode currentNode = new CFGBranchConditionalNode(compositeStatement);
		if(action == JOIN_TOP_LIST && !unjoinedConditionalNodes.empty()) {
			List<CFGBranchConditionalNode> topList = unjoinedConditionalNodes.peek();
			topList.add(currentNode);
		}
		else if(action == JOIN_SECOND_FROM_TOP_LIST) {
			if(unjoinedConditionalNodes.size() > 1) {
				List<CFGBranchConditionalNode> list = unjoinedConditionalNodes.elementAt(unjoinedConditionalNodes.size()-2);
				list.add(currentNode);
			}
			else {
				List<CFGBranchConditionalNode> topList = unjoinedConditionalNodes.pop();
				List<CFGBranchConditionalNode> list = new ArrayList<CFGBranchConditionalNode>();
				list.add(currentNode);
				unjoinedConditionalNodes.push(list);
				unjoinedConditionalNodes.push(topList);
			}
		}
		else if(action == PLACE_NEW_LIST_SECOND_FROM_TOP && !unjoinedConditionalNodes.empty()) {
			List<CFGBranchConditionalNode> topList = unjoinedConditionalNodes.pop();
			List<CFGBranchConditionalNode> list = new ArrayList<CFGBranchConditionalNode>();
			list.add(currentNode);
			unjoinedConditionalNodes.push(list);
			unjoinedConditionalNodes.push(topList);
		}
		else {
			List<CFGBranchConditionalNode> list = new ArrayList<CFGBranchConditionalNode>();
			list.add(currentNode);
			unjoinedConditionalNodes.push(list);
		}
		
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
				previousNodes.addAll(processIfStatement(currentNodes, thenClauseCompositeStatement, JOIN_TOP_LIST));
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
					previousNodes.addAll(processIfStatement(currentNodes, elseClauseCompositeStatement, JOIN_TOP_LIST));
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
						!(previousNode instanceof CFGBranchDoLoopNode))
					flow.setTrueControlFlow(true);
				else
					flow.setFalseControlFlow(true);
			}
			edges.add(flow);
		}
		if(previousNodes.size() > 1) {
			List<CFGBranchConditionalNode> conditionalNodes = unjoinedConditionalNodes.pop();
			for(CFGBranchConditionalNode conditionalNode : conditionalNodes) {
				conditionalNode.setJoinNode(currentNode);
			}
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
