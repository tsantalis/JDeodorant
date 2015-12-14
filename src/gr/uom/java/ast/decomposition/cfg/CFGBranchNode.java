package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.TryStatementObject;

public abstract class CFGBranchNode extends CFGNode {

	public CFGBranchNode(AbstractStatement statement) {
		super(statement);
	}

	public Flow getTrueControlFlow() {
		for(GraphEdge edge : outgoingEdges) {
			Flow flow = (Flow)edge;
			if(flow.isTrueControlFlow())
				return flow;
		}
		return null;
	}

	public Flow getFalseControlFlow() {
		for(GraphEdge edge : outgoingEdges) {
			Flow flow = (Flow)edge;
			if(flow.isFalseControlFlow())
				return flow;
		}
		return null;
	}

	protected List<BasicBlock> getNestedBasicBlocksToEnd() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		BasicBlock nextBlock = srcBlock;
		while(nextBlock.getNextBasicBlock() != null) {
			nextBlock = nextBlock.getNextBasicBlock();
			blocksBetween.add(nextBlock);
		}
		return blocksBetween;
	}

	public abstract CFGNode getJoinNode();
	
	public abstract List<BasicBlock> getNestedBasicBlocks();

	public Set<CFGNode> getImmediatelyNestedNodesFromAST() {
		Set<CFGNode> nestedNodes = new LinkedHashSet<CFGNode>();
		AbstractStatement abstractStatement = getStatement();
		if(abstractStatement instanceof CompositeStatementObject) {
			Set<AbstractStatement> nestedStatements = new LinkedHashSet<AbstractStatement>();
			CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
			List<AbstractStatement> statements = composite.getStatements();
			for(AbstractStatement statement : statements) {
				if(statement.getStatement() instanceof Block) {
					CompositeStatementObject blockStatement = (CompositeStatementObject)statement;
					processBlockStatement(nestedStatements, blockStatement);
				}
				else if(statement.getStatement() instanceof LabeledStatement || statement.getStatement() instanceof SynchronizedStatement) {
					CompositeStatementObject labeledStatement = (CompositeStatementObject)statement;
					processLabeledStatement(nestedStatements, labeledStatement);
				}
				else if(statement instanceof TryStatementObject) {
					CompositeStatementObject tryStatement = (CompositeStatementObject)statement;
					processTryStatement(nestedStatements, tryStatement);
				}
				else
					nestedStatements.add(statement);
			}
			List<BasicBlock> nestedBasicBlocks = getNestedBasicBlocks();
			if(this instanceof CFGBranchDoLoopNode)
				nestedBasicBlocks.add(getBasicBlock());
			else
				nestedBasicBlocks.add(0, getBasicBlock());
			for(BasicBlock nestedBlock : nestedBasicBlocks) {
				List<CFGNode> nodes = nestedBlock.getAllNodes();
				for(CFGNode node : nodes) {
					if(nestedStatements.contains(node.getStatement())) {
						nestedNodes.add(node);
					}
				}
			}
		}
		return nestedNodes;
	}

	protected void processBlockStatement(Set<AbstractStatement> nestedStatements, CompositeStatementObject blockStatement) {
		for(AbstractStatement statementInsideBlock : blockStatement.getStatements()) {
			if(statementInsideBlock instanceof TryStatementObject) {
				CompositeStatementObject tryStatement = (CompositeStatementObject)statementInsideBlock;
				processTryStatement(nestedStatements, tryStatement);
			}
			else if(statementInsideBlock.getStatement() instanceof LabeledStatement || statementInsideBlock.getStatement() instanceof SynchronizedStatement) {
				CompositeStatementObject labeledStatement = (CompositeStatementObject)statementInsideBlock;
				processLabeledStatement(nestedStatements, labeledStatement);
			}
			else if(statementInsideBlock.getStatement() instanceof Block) {
				CompositeStatementObject blockStatement2 = (CompositeStatementObject)statementInsideBlock;
				processBlockStatement(nestedStatements, blockStatement2);
			}
			else
				nestedStatements.add(statementInsideBlock);
		}
	}

	protected void processLabeledStatement(Set<AbstractStatement> nestedStatements, CompositeStatementObject labeledStatement) {
		List<AbstractStatement> nestedStatements2 = labeledStatement.getStatements();
		if(!nestedStatements2.isEmpty()) {
			AbstractStatement firstStatement = nestedStatements2.get(0);
			if(firstStatement.getStatement() instanceof Block) {
				CompositeStatementObject blockStatement = (CompositeStatementObject)firstStatement;
				for(AbstractStatement statementInsideBlock : blockStatement.getStatements()) {
					if(statementInsideBlock instanceof TryStatementObject) {
						CompositeStatementObject tryStatement = (CompositeStatementObject)statementInsideBlock;
						processTryStatement(nestedStatements, tryStatement);
					}
					else
						nestedStatements.add(statementInsideBlock);
				}
			}
			else if(firstStatement instanceof TryStatementObject) {
				CompositeStatementObject tryStatement = (CompositeStatementObject)firstStatement;
				processTryStatement(nestedStatements, tryStatement);
			}
			else
				nestedStatements.add(firstStatement);
		}
	}

	protected void processTryStatement(Set<AbstractStatement> nestedStatements, CompositeStatementObject tryStatement) {
		CompositeStatementObject tryBlock = (CompositeStatementObject)tryStatement.getStatements().get(0);
		/*if(((TryStatementObject)tryStatement).hasResources())
			nestedStatements.add(tryStatement);*/
		for(AbstractStatement statementInsideBlock : tryBlock.getStatements()) {
			if(statementInsideBlock instanceof TryStatementObject) {
				CompositeStatementObject nestedTryStatement = (CompositeStatementObject)statementInsideBlock;
				processTryStatement(nestedStatements, nestedTryStatement);
			}
			else
				nestedStatements.add(statementInsideBlock);
		}
	}
}
