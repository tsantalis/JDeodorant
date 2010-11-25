package gr.uom.java.ast.inheritance;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.TypeObject;

public class CompleteInheritanceDetection {
	private Map<String, LinkedHashSet<String>> subclassMap;
	
	public CompleteInheritanceDetection(SystemObject system) {
		this.subclassMap = new LinkedHashMap<String, LinkedHashSet<String>>();
		generateInheritanceHierarchies(system);
	}
	
	private void addSubclassToSuperclass(String superclass, String subclass) {
		if(subclassMap.containsKey(superclass)) {
			LinkedHashSet<String> subclasses = subclassMap.get(superclass);
			subclasses.add(subclass);
		}
		else {
			LinkedHashSet<String> subclasses = new LinkedHashSet<String>();
			subclasses.add(subclass);
			subclassMap.put(superclass, subclasses);
		}
	}
	
	private void generateInheritanceHierarchies(SystemObject system) {
		ListIterator<ClassObject> classIterator = system.getClassListIterator();
        while(classIterator.hasNext()) {
            ClassObject classObject = classIterator.next();
            TypeObject superclassType = classObject.getSuperclass();
            if(superclassType != null) {
            	String superclass = superclassType.getClassType();
            	if(system.getClassObject(superclass) != null) {
            		addSubclassToSuperclass(superclass, classObject.getName());
            	}
            }
            ListIterator<TypeObject> interfaceIterator = classObject.getInterfaceIterator();
            while(interfaceIterator.hasNext()) {
            	TypeObject superInterface = interfaceIterator.next();
            	if(system.getClassObject(superInterface.getClassType()) != null) {
                	addSubclassToSuperclass(superInterface.getClassType(), classObject.getName());
                }
            }
        }
	}
	
	public InheritanceTree getTree(String className) {
		if(subclassMap.containsKey(className)) {
			InheritanceTree tree = new InheritanceTree();
			recursivelyConstructTree(tree, className);
			return tree;
		}
		else {
			return null;
		}
	}
	
	private void recursivelyConstructTree(InheritanceTree tree, String className) {
		if(subclassMap.containsKey(className)) {
			LinkedHashSet<String> subclasses = subclassMap.get(className);
			for(String subclass : subclasses) {
				tree.addChildToParent(subclass, className);
				recursivelyConstructTree(tree, subclass);
			}
		}
	}
	
	public Set<String> getRoots() {
		return subclassMap.keySet();
	}
	
	public Set<InheritanceTree> getMatchingTrees(String subclassName) {
		Set<InheritanceTree> inheritanceTrees = new LinkedHashSet<InheritanceTree>();
		for(String superclass : subclassMap.keySet()) {
			LinkedHashSet<String> subclasses = subclassMap.get(superclass);
			boolean matchingInheritanceHierarchy = false;
			for(String subclass : subclasses) {
				if((subclass.contains(".") && subclass.endsWith("." + subclassName)) || subclass.equals(subclassName)) {
					matchingInheritanceHierarchy = true;
					break;
				}
			}
			if(matchingInheritanceHierarchy)
				inheritanceTrees.add(getTree(superclass));
		}
		return inheritanceTrees;
	}
}
