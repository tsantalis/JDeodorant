package gr.uom.java.ast.inheritance;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class TypeBindingInheritanceDetection {
	private List<InheritanceTree> inheritanceTreeList;

	public TypeBindingInheritanceDetection(List<ITypeBinding> typeBindings) {
		this.inheritanceTreeList = new ArrayList<InheritanceTree>();
		generateInheritanceTrees(typeBindings);
	}

	public List<InheritanceTree> getInheritanceTreeList() {
		return inheritanceTreeList;
	}

	private void generateInheritanceTrees(List<ITypeBinding> typeBindings) {
		for(ITypeBinding typeBinding : typeBindings) {
			ITypeBinding superType = extendsOrImplements(typeBinding, typeBindings);
			if(superType != null && !typeBinding.getQualifiedName().equals(superType.getQualifiedName())) {
				String childName = typeBinding.getQualifiedName();
				String parentName = superType.getQualifiedName();
				InheritanceTree childTree = getTree(childName);
				InheritanceTree parentTree = getTree(parentName);
				if(childTree == null && parentTree == null) {
					InheritanceTree tree = new InheritanceTree();
					tree.addChildToParent(childName, parentName);
					inheritanceTreeList.add(tree);
				}
				else if(childTree == null) {
					parentTree.addChildToParent(childName, parentName);
				}
				else if(parentTree == null) {
					childTree.addChildToParent(childName, parentName);
				}
				else if( !childTree.equals(parentTree) ) {
					parentTree.addChildRootNodeToParent(childTree.getRootNode(), parentName);
					inheritanceTreeList.remove(childTree);
				}
			}
			else if(getTree(typeBinding.getQualifiedName()) == null) {
				inheritanceTreeList.add(new InheritanceTree(typeBinding.getQualifiedName()));
			}
		}
	}

	private static ITypeBinding extendsOrImplements(ITypeBinding typeBinding, List<ITypeBinding> superTypes) {
		ITypeBinding extendedSuperClass = typeBinding.getSuperclass();
		if(extendedSuperClass != null) {
			for(ITypeBinding superType : superTypes) {
				if(extendedSuperClass.getQualifiedName().equals(superType.getQualifiedName()))
					return extendedSuperClass;
			}
		}
		ITypeBinding[] implementedInterfaces = typeBinding.getInterfaces();
		for(ITypeBinding implementedInterface : implementedInterfaces) {
			for(ITypeBinding superType : superTypes) {
				if(implementedInterface.getQualifiedName().equals(superType.getQualifiedName()))
					return implementedInterface;
			}
		}
		return null;
	}
	//returns the first tree that contains the node with name nodeName
	public InheritanceTree getTree(String nodeName) {
		for(InheritanceTree tree : inheritanceTreeList) {
			if(tree.getNode(nodeName) != null)
				return tree;
		}
		return null;
	}

	public Set<String> getLeavesInDeepestLevels() {
		Set<String> leavesInDeepestLevels = new LinkedHashSet<String>();
		for(InheritanceTree tree : inheritanceTreeList) {
			TreeMap<Integer, Set<String>> levelMap = tree.getLeavesByLevel();
			Integer lastLevel = levelMap.lastKey();
			if(lastLevel > 0) {
				leavesInDeepestLevels.addAll(levelMap.get(lastLevel));
			}
		}
		return leavesInDeepestLevels;
	}
}
