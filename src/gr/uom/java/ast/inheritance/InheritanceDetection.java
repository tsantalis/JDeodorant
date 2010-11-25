package gr.uom.java.ast.inheritance;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.TypeObject;

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
            TypeObject superclassType = classObject.getSuperclass();
            if(superclassType != null) {
            	String superclass = superclassType.getClassType();
            	if(system.getClassObject(superclass) != null) {
            		InheritanceTree childTree = getTree(classObject.getName());
            		InheritanceTree parentTree = getTree(superclass);
            		if(childTree == null && parentTree == null) {
            			InheritanceTree tree = new InheritanceTree();
            			tree.addChildToParent(classObject.getName(), superclass);
            			inheritanceTreeList.add(tree);
            		}
            		else if(childTree == null) {
            			parentTree.addChildToParent(classObject.getName(), superclass);
            		}
            		else if(parentTree == null) {
            			childTree.addChildToParent(classObject.getName(), superclass);
            		}
            		else if( !childTree.equals(parentTree) ) {
            			parentTree.addChildRootNodeToParent(childTree.getRootNode(), superclass);
            			inheritanceTreeList.remove(childTree);
            		}
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
