package gr.uom.java.ast;

import gr.uom.java.ast.inheritance.CompleteInheritanceDetection;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.*;

import org.eclipse.jdt.core.dom.SimpleName;

public class SystemObject {

    private List<ClassObject> classList;
    //Map that has as key the classname and as value
    //the position of className in the classNameList
    private Map<String, Integer> classNameMap;
    private Map<MethodInvocationObject, FieldInstructionObject> getterMap;
    private Map<MethodInvocationObject, FieldInstructionObject> setterMap;
    private Map<MethodInvocationObject, FieldInstructionObject> collectionAdderMap;
    private Map<MethodInvocationObject, MethodInvocationObject> delegateMap;

    public SystemObject() {
        this.classList = new ArrayList<ClassObject>();
        this.classNameMap = new HashMap<String, Integer>();
        this.getterMap = new LinkedHashMap<MethodInvocationObject, FieldInstructionObject>();
        this.setterMap = new LinkedHashMap<MethodInvocationObject, FieldInstructionObject>();
        this.collectionAdderMap = new LinkedHashMap<MethodInvocationObject, FieldInstructionObject>();
        this.delegateMap = new LinkedHashMap<MethodInvocationObject, MethodInvocationObject>();
    }

    public void addClass(ClassObject c) {
        classNameMap.put(c.getName(),classList.size());
        classList.add(c);
    }
    
    public void addGetter(MethodInvocationObject methodInvocation, FieldInstructionObject fieldInstruction) {
    	getterMap.put(methodInvocation, fieldInstruction);
    }
    
    public void addSetter(MethodInvocationObject methodInvocation, FieldInstructionObject fieldInstruction) {
    	setterMap.put(methodInvocation, fieldInstruction);
    }
    
    public void addCollectionAdder(MethodInvocationObject methodInvocation, FieldInstructionObject fieldInstruction) {
    	collectionAdderMap.put(methodInvocation, fieldInstruction);
    }
    
    public void addDelegate(MethodInvocationObject methodInvocation, MethodInvocationObject delegation) {
    	delegateMap.put(methodInvocation, delegation);
    }
    
    public FieldInstructionObject containsGetter(MethodInvocationObject methodInvocation) {
    	return getterMap.get(methodInvocation);
    }
    
    public FieldInstructionObject containsSetter(MethodInvocationObject methodInvocation) {
    	return setterMap.get(methodInvocation);
    }
    
    public FieldInstructionObject containsCollectionAdder(MethodInvocationObject methodInvocation) {
    	return collectionAdderMap.get(methodInvocation);
    }
    
    public MethodInvocationObject containsDelegate(MethodInvocationObject methodInvocation) {
    	return delegateMap.get(methodInvocation);
    }
    
    public MethodObject getMethod(MethodInvocationObject mio) {
    	ClassObject classObject = getClassObject(mio.getOriginClassName());
    	if(classObject != null)
    		return classObject.getMethod(mio);
    	return null;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation, ClassObject excludedClass) {
    	for(ClassObject classObject : classList) {
    		if(!excludedClass.equals(classObject) && classObject.containsMethodInvocation(methodInvocation))
    			return true;
    	}
    	return false;
    }

    public ClassObject getClassObject(String className) {
        Integer pos = classNameMap.get(className);
        if(pos != null)
            return getClassObject(pos);
        else
            return null;
    }

    public ClassObject getClassObject(int pos) {
        return classList.get(pos);
    }

    public ListIterator<ClassObject> getClassListIterator() {
        return classList.listIterator();
    }

    public int getClassNumber() {
        return classList.size();
    }

    public int getPositionInClassList(String className) {
        Integer pos = classNameMap.get(className);
        if(pos != null)
            return pos;
        else
            return -1;
    }

    public List<String> getClassNames() {
        List<String> names = new ArrayList<String>();

        for(int i=0; i<classList.size(); i++) {
            names.add(getClassObject(i).getName());
        }
        return names;
    }

    public List<TypeCheckElimination> generateTypeCheckEliminations() {
    	List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
    	Map<TypeCheckElimination, List<SimpleName>> staticFieldMap = new LinkedHashMap<TypeCheckElimination, List<SimpleName>>();
    	Map<Integer, ArrayList<TypeCheckElimination>> staticFieldRankMap = new TreeMap<Integer, ArrayList<TypeCheckElimination>>();
    	CompleteInheritanceDetection inheritanceDetection = new CompleteInheritanceDetection(this);
    	for(ClassObject classObject : classList) {
    		List<TypeCheckElimination> eliminations = classObject.generateTypeCheckEliminations(inheritanceDetection);
    		for(TypeCheckElimination elimination : eliminations) {
    			List<SimpleName> staticFields = elimination.getStaticFields();
    			if(!staticFields.isEmpty()) {
    				staticFieldMap.put(elimination, staticFields);
    				int size = staticFields.size();
    				if(staticFieldRankMap.containsKey(size)) {
    					ArrayList<TypeCheckElimination> rank = staticFieldRankMap.get(size);
    					rank.add(elimination);
    				}
    				else {
    					ArrayList<TypeCheckElimination> rank = new ArrayList<TypeCheckElimination>();
    					rank.add(elimination);
    					staticFieldRankMap.put(size, rank);
    				}
    			}
    		}
    		typeCheckEliminations.addAll(eliminations);
    	}
    	List<TypeCheckElimination> sortedEliminations = new ArrayList<TypeCheckElimination>();
    	List<Integer> keyList = new ArrayList<Integer>(staticFieldRankMap.keySet());
    	ListIterator<Integer> keyListIterator = keyList.listIterator(keyList.size());
    	while(keyListIterator.hasPrevious()) {
			Integer states = keyListIterator.previous();
			sortedEliminations.addAll(staticFieldRankMap.get(states));
    	}
    	
    	while(!sortedEliminations.isEmpty()) {
    		TypeCheckElimination selectedElimination = sortedEliminations.get(0);
    		List<TypeCheckElimination> affectedEliminations = new ArrayList<TypeCheckElimination>();
    		affectedEliminations.add(selectedElimination);
    		List<SimpleName> staticFieldUnion = staticFieldMap.get(selectedElimination);
    		for(TypeCheckElimination elimination : sortedEliminations) {
    			List<SimpleName> staticFields = staticFieldMap.get(elimination);
    			if(!selectedElimination.equals(elimination) && nonEmptyIntersection(staticFieldUnion, staticFields)) {
    				staticFieldUnion = constructUnion(staticFieldUnion, staticFields);
    				affectedEliminations.add(elimination);
    			}
    		}
    		if(affectedEliminations.size() > 1) {
    			for(TypeCheckElimination elimination : affectedEliminations) {
    				List<SimpleName> staticFields = staticFieldMap.get(elimination);
    				for(SimpleName simpleName1 : staticFieldUnion) {
    					boolean isContained = false;
    					for(SimpleName simpleName2 : staticFields) {
    						if(simpleName1.resolveBinding().isEqualTo(simpleName2.resolveBinding())) {
    		    				isContained = true;
    		    				break;
    		    			}
    					}
    					if(!isContained)
    						elimination.addAdditionalStaticField(simpleName1);
    				}
    			}
    		}
    		sortedEliminations.removeAll(affectedEliminations);
    	}
    	return typeCheckEliminations;
    }

    private boolean nonEmptyIntersection(List<SimpleName> staticFieldUnion ,List<SimpleName> staticFields) {
    	for(SimpleName simpleName1 : staticFields) {
    		for(SimpleName simpleName2 : staticFieldUnion) {
    			if(simpleName1.resolveBinding().isEqualTo(simpleName2.resolveBinding()))
    				return true;
    		}
    	}
    	return false;
    }

    private List<SimpleName> constructUnion(List<SimpleName> staticFieldUnion ,List<SimpleName> staticFields) {
    	List<SimpleName> initialStaticFields = new ArrayList<SimpleName>(staticFieldUnion);
    	List<SimpleName> staticFieldsToBeAdded = new ArrayList<SimpleName>();
    	for(SimpleName simpleName1 : staticFields) {
    		boolean isContained = false;
    		for(SimpleName simpleName2 : staticFieldUnion) {
    			if(simpleName1.resolveBinding().isEqualTo(simpleName2.resolveBinding())) {
    				isContained = true;
    				break;
    			}
    		}
    		if(!isContained)
    			staticFieldsToBeAdded.add(simpleName1);
    	}
    	initialStaticFields.addAll(staticFieldsToBeAdded);
    	return initialStaticFields;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(ClassObject classObject : classList) {
            sb.append(classObject.toString());
            sb.append("\n--------------------------------------------------------------------------------\n");
        }
        return sb.toString();
    }
}