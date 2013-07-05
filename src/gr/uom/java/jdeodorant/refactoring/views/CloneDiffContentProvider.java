package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;

import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

public class CloneDiffContentProvider extends ArrayContentProvider
		implements ITreeContentProvider {

	public Object[] getChildren(Object parentElement) {
		CloneStructureNode cloneStructureNode = (CloneStructureNode) parentElement;
		Set<CloneStructureNode> children = cloneStructureNode.getChildren();
		return children.toArray();
	}

	public Object getParent(Object element) {
		CloneStructureNode cloneStructureNode = (CloneStructureNode) element;
		return cloneStructureNode.getParent();
	}

	public boolean hasChildren(Object element) {
		CloneStructureNode cloneStructureNode = (CloneStructureNode) element;
		Set<CloneStructureNode> children = cloneStructureNode.getChildren();
		if (children.size() > 0){
			return true;
		}
		else
			return false;
	}

}
