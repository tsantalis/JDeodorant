package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Block;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.TryStatementObject;

public class CFGBranchIfNode extends CFGBranchConditionalNode {

	public CFGBranchIfNode(AbstractStatement statement) {
		super(statement);
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
					if(statementInsideBlock instanceof TryStatementObject) {
						CompositeStatementObject tryStatement = (CompositeStatementObject)statementInsideBlock;
						processTryStatement(nestedStatements, tryStatement);
					}
					else
						nestedStatements.add(statementInsideBlock);
				}
			}
			else if(trueControlFlowStatement instanceof TryStatementObject) {
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
						if(statementInsideBlock instanceof TryStatementObject) {
							CompositeStatementObject tryStatement = (CompositeStatementObject)statementInsideBlock;
							processTryStatement(nestedStatements, tryStatement);
						}
						else
							nestedStatements.add(statementInsideBlock);
					}
				}
				else if(falseControlFlowStatement instanceof TryStatementObject) {
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
