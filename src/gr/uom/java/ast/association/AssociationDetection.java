package gr.uom.java.ast.association;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.tree.DefaultMutableTreeNode;

public class AssociationDetection {
	private List<Association> associationList;
	
	public AssociationDetection(SystemObject system) {
		this.associationList = new ArrayList<Association>();
		generateAssociations(system);
	}
	
	public boolean containsFieldObject(FieldObject field) {
		for(Association association : associationList)
			if(association.getFieldObject().equals(field))
				return true;
		return false;
	}
	
	private void generateAssociations(SystemObject systemObject) {
		ListIterator<ClassObject> classIt = systemObject.getClassListIterator();
        while(classIt.hasNext()) {
            ClassObject classObject = classIt.next();
            
            ListIterator<FieldObject> fieldIt = classObject.getFieldIterator();
            while(fieldIt.hasNext()) {
                FieldObject fieldObject = fieldIt.next();
                String type = fieldObject.getType().getClassType();
                //cover also other collections in the future
				if(type.equals("java.util.List") || type.equals("java.util.ArrayList") || type.equals("java.util.Vector")) {
					String genericType = fieldObject.getType().getGenericType();
					if(genericType != null) {
						for(String className : systemObject.getClassNames()) {
							if(genericType.contains(className)) {
								Association association = new Association(fieldObject, classObject.getName(), className);
								association.setContainer(true);
								if(!associationList.contains(association))
									associationList.add(association);
							}
						}
					}
					else {
						Association association = checkCollectionAttribute(systemObject,classObject, fieldObject);
						if(association != null && !associationList.contains(association))
							associationList.add(association);
					}
				} else if(systemObject.getClassObject(type) != null) {
					Association association = new Association(fieldObject, classObject.getName(), type);
					if(fieldObject.getType().getArrayDimension() > 0)
						association.setContainer(true);
					if (!associationList.contains(association))
						associationList.add(association);
				}
            }
        }
	}
	
	private Association checkCollectionAttribute(SystemObject so, ClassObject co, FieldObject fo) {
        ListIterator<MethodObject> methodIt = co.getMethodIterator();
        while(methodIt.hasNext()) {
            MethodObject mo = methodIt.next();
            List<FieldInstructionObject> fieldInstructions = mo.getFieldInstructions();
            for(FieldInstructionObject fio : fieldInstructions) {
                if(fo.equals(fio)) {
                    List<String> parameterList = mo.getParameterList();
                    List<MethodInvocationObject> methodInvocations = mo.getMethodInvocations();
                    for(MethodInvocationObject mio : methodInvocations) {
                        if(mio.getOriginClassName().startsWith("java.util.List") ||
                                mio.getOriginClassName().startsWith("java.util.ArrayList") || mio.getOriginClassName().startsWith("java.util.Vector")) {
                            if(mio.getMethodName().equals("add") || mio.getMethodName().equals("addElement")) {
                                if(parameterList.size() == 1 && so.getClassObject(parameterList.get(0)) != null) {
                                    Association association = new Association(fo, co.getName(),parameterList.get(0));
                                    association.setContainer(true);
                                    return association;
                                }
                                else if(parameterList.size() > 1) {
                                    //System.out.println(mSignature);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
	
	public boolean checkForContainerAssociation(String from, String to) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(from);
        recurse(root);
        DefaultMutableTreeNode firstLeaf = root.getFirstLeaf();
        if(checkFullPathForContainerAssociation(firstLeaf,to))
            return true;
        DefaultMutableTreeNode leaf = firstLeaf.getNextLeaf();
        while(leaf != null) {
            if(checkFullPathForContainerAssociation(leaf,to))
                return true;
            leaf = leaf.getNextLeaf();
        }
        return false;
    }

    private boolean checkFullPathForContainerAssociation(DefaultMutableTreeNode leaf, String nodeName) {
        if(leaf.getUserObject().equals(nodeName))
            return checkPathForContainerAssociation(leaf);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)leaf.getParent();
        while(node != null) {
            if(node.getUserObject().equals(nodeName))
                return checkPathForContainerAssociation(node);
            node = (DefaultMutableTreeNode)node.getParent();
        }
        return false;
    }

    // node is the target class of move operation
    private boolean checkPathForContainerAssociation(DefaultMutableTreeNode node) {
        while(node.getParent() != null) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
            Association association = getAssociation((String)node.getUserObject(), (String)parent.getUserObject());
            if(association.isContainer())
                return true;
            node = parent;
        }
        return false;
    }

    private void recurse(DefaultMutableTreeNode root) {
        List<Association> endingAssociations = getAssociationsEndingTo((String)root.getUserObject());
        for(Association association : endingAssociations) {
            Object[] pathFromRoot = root.getUserObjectPath();
            boolean cycle = false;
            for(Object node : pathFromRoot) {
                if(association.getFrom().equals(node)) {
                    cycle = true;
                    break;
                }
            }
            if(!cycle) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(association.getFrom());
                root.add(node);
                recurse(node);
            }
        }
    }

    private List<Association> getAssociationsEndingTo(String to) {
        List<Association> list = new ArrayList<Association>();
        for(Association association : associationList) {
            if(association.getTo().equals(to))
                list.add(association);
        }
        return list;
    }

    private Association getAssociation(String from, String to) {
         for(Association association : associationList) {
            if(association.getFrom().equals(from) && association.getTo().equals(to))
                return association;
         }
        return null;
    }
}
