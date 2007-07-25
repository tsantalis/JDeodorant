package gr.uom.java.ast.inheritance;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.SystemObject;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

public class InheritanceDetection {
    private List<InheritanceTree> inheritanceTreeList;

    public InheritanceDetection(SystemObject system) {
        this.inheritanceTreeList = new ArrayList<InheritanceTree>();
        generateInheritanceTrees(system);
    }

    public List<InheritanceTree> getInheritanceTreeList() {
        return inheritanceTreeList;
    }

    private void generateInheritanceTrees(SystemObject system) {
        ListIterator<ClassObject> classIterator = system.getClassListIterator();
        while(classIterator.hasNext()) {
            ClassObject classObject = classIterator.next();
            if(system.getClassObject(classObject.getSuperclass()) != null) {
                InheritanceTree childTree = getTree(classObject.getName());
                InheritanceTree parentTree = getTree(classObject.getSuperclass());
                if(childTree == null && parentTree == null) {
                    InheritanceTree tree = new InheritanceTree();
                    tree.addChildToParent(classObject.getName(), classObject.getSuperclass());
                    inheritanceTreeList.add(tree);
                }
                else if(childTree == null) {
                    parentTree.addChildToParent(classObject.getName(), classObject.getSuperclass());
                }
                else if(parentTree == null) {
                    childTree.addChildToParent(classObject.getName(), classObject.getSuperclass());
                }
                else if( !childTree.equals(parentTree) ) {
                    parentTree.addChildRootNodeToParent(childTree.getRootNode(),classObject.getSuperclass());
                    inheritanceTreeList.remove(childTree);
                }
            }
        }
    }
    //returns the first tree that contains the node with name nodeName
	public InheritanceTree getTree(String nodeName) {
		for(InheritanceTree tree : inheritanceTreeList) {
			if(tree.getNode(nodeName) != null)
				return tree;
		}
		return null;
	}
}
