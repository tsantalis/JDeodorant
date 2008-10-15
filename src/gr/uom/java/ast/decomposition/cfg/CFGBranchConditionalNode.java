package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.TryStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;

public class CFGBranchConditionalNode extends CFGBranchNode {
	private CFGNode joinNode;
	
	public CFGBranchConditionalNode(AbstractStatement statement) {
		super(statement);
	}

	public void setJoinNode(CFGNode joinNode) {
		this.joinNode = joinNode;
	}

	public CFGNode getJoinNode() {
		return joinNode;
	}

	public List<BasicBlock> getNestedBasicBlocks() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		if(joinNode != null) {
			CFGNode dstNode = joinNode;
			if(dstNode.getBasicBlock().getId() < srcBlock.getId() && joinNode instanceof CFGBranchLoopNode) {
				CFGBranchLoopNode loopNode = (CFGBranchLoopNode)joinNode;
				Flow falseControlFlow = loopNode.getFalseControlFlow();
				if(falseControlFlow != null)
					dstNode = (CFGNode)falseControlFlow.dst;
				else
					return getNestedBasicBlocksToEnd();
			}
			BasicBlock dstBlock = dstNode.getBasicBlock();
			if(srcBlock.getId() < dstBlock.getId()) {
				BasicBlock nextBlock = srcBlock;
				while(!nextBlock.getNextBasicBlock().equals(dstBlock)) {
					nextBlock = nextBlock.getNextBasicBlock();
					blocksBetween.add(nextBlock);
				}
			}
			else if(srcBlock.getId() >= dstBlock.getId()) {
				return getNestedBasicBlocksToEnd();
			}
		}
		else {
			return getNestedBasicBlocksToEnd();
		}
		return blocksBetween;
	}

	public Set<CFGNode> getImmediatelyNestedNodesInTrueControlFlow() {
		Set<CFGNode> nestedNodes = new LinkedHashSet<CFGNode>();
		AbstractStatement abstractStatement = getStatement();
		if(abstractStatement instanceof CompositeStatementObject) {
			Set<AbstractStatement> nestedStatements = new LinkedHashSet<AbstractStatement>();
			CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
			List<AbstractStatement> statements = composite.getStatements();
			AbstractStatement trueControlFlowStatement = statements.get(0);
			if(trueControlFlowStatement.getStatement() instanceof Block) {
				CompositeStatementObject blockStatement = (CompositeStatementObject)trueControlFlowStatement;
				for(AbstractStatement statementInsideBlock : blockStatement.getStatements()) {
					if(statementInsideBlock.getStatement() instanceof TryStatement) {
						CompositeStatementObject tryStatement = (CompositeStatementObject)statementInsideBlock;
						processTryStatement(nestedStatements, tryStatement);
					}
					else
						nestedStatements.add(statementInsideBlock);
				}
			}
			else if(trueControlFlowStatement.getStatement() instanceof TryStatement) {
				CompositeStatementObject tryStatement = (CompositeStatementObject)trueControlFlowStatement;
				processTryStatement(nestedStatements, tryStatement);
			}
			else
				nestedStatements.add(trueControlFlowStatement);
			List<BasicBlock> nestedBasicBlocks = getNestedBasicBlocks();
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

	public Set<CFGNode> getImmediatelyNestedNodesInFalseControlFlow() {
		Set<CFGNode> nestedNodes = new LinkedHashSet<CFGNode>();
		AbstractStatement abstractStatement = getStatement();
		if(abstractStatement instanceof CompositeStatementObject) {
			Set<AbstractStatement> nestedStatements = new LinkedHashSet<AbstractStatement>();
			CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
			List<AbstractStatement> statements = composite.getStatements();
			if(statements.size() == 2) {
				AbstractStatement falseControlFlowStatement = statements.get(1);
				if(falseControlFlowStatement.getStatement() instanceof Block) {
					CompositeStatementObject blockStatement = (CompositeStatementObject)falseControlFlowStatement;
					for(AbstractStatement statementInsideBlock : blockStatement.getStatements()) {
						if(statementInsideBlock.getStatement() instanceof TryStatement) {
							CompositeStatementObject tryStatement = (CompositeStatementObject)statementInsideBlock;
							processTryStatement(nestedStatements, tryStatement);
						}
						else
							nestedStatements.add(statementInsideBlock);
					}
				}
				else if(falseControlFlowStatement.getStatement() instanceof TryStatement) {
					CompositeStatementObject tryStatement = (CompositeStatementObject)falseControlFlowStatement;
					processTryStatement(nestedStatements, tryStatement);
				}
				else
					nestedStatements.add(falseControlFlowStatement);
				List<BasicBlock> nestedBasicBlocks = getNestedBasicBlocks();
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
		}
		return nestedNodes;
	}

	private void processTryStatement(Set<AbstractStatement> nestedStatements, CompositeStatementObject tryStatement) {
		CompositeStatementObject tryBlock = (CompositeStatementObject)tryStatement.getStatements().get(0);
		for(AbstractStatement statementInsideBlock : tryBlock.getStatements()) {
			if(statementInsideBlock.getStatement() instanceof TryStatement) {
				CompositeStatementObject nestedTryStatement = (CompositeStatementObject)statementInsideBlock;
				processTryStatement(nestedStatements, nestedTryStatement);
			}
			else
				nestedStatements.add(statementInsideBlock);
		}
	}
}
