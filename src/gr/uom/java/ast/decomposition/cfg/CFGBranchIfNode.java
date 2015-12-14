package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;

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
				processBlockStatement(nestedStatements, blockStatement);
			}
			else if(trueControlFlowStatement.getStatement() instanceof LabeledStatement || trueControlFlowStatement.getStatement() instanceof SynchronizedStatement) {
				CompositeStatementObject labeledStatement = (CompositeStatementObject)trueControlFlowStatement;
				processLabeledStatement(nestedStatements, labeledStatement);
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
					processBlockStatement(nestedStatements, blockStatement);
				}
				else if(falseControlFlowStatement.getStatement() instanceof LabeledStatement || falseControlFlowStatement.getStatement() instanceof SynchronizedStatement) {
					CompositeStatementObject labeledStatement = (CompositeStatementObject)falseControlFlowStatement;
					processLabeledStatement(nestedStatements, labeledStatement);
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
}
