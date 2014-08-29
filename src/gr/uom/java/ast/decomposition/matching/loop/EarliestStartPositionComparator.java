package gr.uom.java.ast.decomposition.matching.loop;

import java.util.Comparator;

import org.eclipse.jdt.core.dom.ASTNode;

public class EarliestStartPositionComparator implements Comparator<ASTNode>
{
	public int compare(ASTNode node1, ASTNode node2)
	{
		return Integer.compare(node1.getStartPosition(), node2.getStartPosition());
	}
}
