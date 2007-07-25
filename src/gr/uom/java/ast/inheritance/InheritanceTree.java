package gr.uom.java.ast.inheritance;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

public class InheritanceTree {
    private DefaultMutableTreeNode rootNode;

    public InheritanceTree() {
        this.rootNode = null;
    }

    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }

    public DefaultMutableTreeNode getNode(String nodeName) {
        if(rootNode != null) {
            Enumeration e = rootNode.breadthFirstEnumeration();
            while(e.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
                if(node.getUserObject().equals(nodeName)) {
                    return node;
                }
            }
        }
        return null;
    }

    public void addChildToParent(String childNode, String parentNode) {
        DefaultMutableTreeNode cNode = getNode(childNode);
        if(cNode == null) {
            cNode = new DefaultMutableTreeNode(childNode);
        }
        DefaultMutableTreeNode pNode = getNode(parentNode);
        if(pNode == null) {
            pNode = new DefaultMutableTreeNode(parentNode);
            rootNode = pNode;
        }
        pNode.add(cNode);
    }

    public void addChildRootNodeToParent(DefaultMutableTreeNode childRootNode, String parentNode) {
		DefaultMutableTreeNode pNode = getNode(parentNode);
		if(pNode == null) {
			pNode = new DefaultMutableTreeNode(parentNode);
			rootNode = pNode;
		}
		pNode.add(childRootNode);
	}

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o instanceof InheritanceTree) {
            InheritanceTree inheritanceTree = (InheritanceTree)o;
            rootNode.getUserObject().equals(inheritanceTree.rootNode.getUserObject());
        }
        return false;
    }
}
