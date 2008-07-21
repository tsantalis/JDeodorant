package gr.uom.java.distance;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class MoveMethodFastDistanceMatrix {
	//holds the entity set of each entity
	private Map<Entity, Set<String>> entityMap;

	//holds the entity set of each class
	private Map<MyClass, Set<String>> classMap;
	private SystemEntityPlacement virtualSystemEntityPlacement;
	
	public MoveMethodFastDistanceMatrix(MySystem virtualSystem, DistanceMatrix originalDistanceMatrix,
			MoveMethodCandidateRefactoring candidate) {
		this.entityMap = new LinkedHashMap<Entity, Set<String>>();
		this.classMap = new LinkedHashMap<MyClass, Set<String>>();
		this.virtualSystemEntityPlacement = new SystemEntityPlacement();
		Set<MyMethod> oldMovedMethods = candidate.getOldMovedMethods();
		Set<MyMethod> newMovedMethods = candidate.getNewMovedMethods();
		Set<Entity> changedEntities = candidate.getChangedEntities();
		Set<MyClass> changedClasses = candidate.getChangedClasses();
		
		Iterator<MyClass> classIt = virtualSystem.getClassIterator();
        while(classIt.hasNext()) {
            MyClass myClass = classIt.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while(attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                if(!attribute.isReference()) {
                	if(attribute.getNewEntitySet() != null)
                		entityMap.put(attribute, attribute.getNewEntitySet());
                	else
                		entityMap.put(attribute, attribute.getEntitySet());
                }
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod method = methodIterator.next();
                if(!oldMovedMethods.contains(method)) {
                	if(method.getNewEntitySet() != null)
                		entityMap.put(method, method.getNewEntitySet());
                	else
                		entityMap.put(method, method.getEntitySet());
                }
            }
            if(myClass.getNewEntitySet() != null)
            	classMap.put(myClass, myClass.getNewEntitySet());
            else
            	classMap.put(myClass, myClass.getEntitySet());
        }
        for(MyMethod newMovedMethod : newMovedMethods) {
        	entityMap.put(newMovedMethod, newMovedMethod.getEntitySet());
        }
        
        for(Entity entity : entityMap.keySet()) {
        	Set<String> entitySet = entityMap.get(entity);
        	String entityName = entity.toString();
        	Set<String> entityClassDependencies = new HashSet<String>();
        	for(String s : entitySet) {
        		entityClassDependencies.add(s.substring(0,s.indexOf("::")));
        	}
        	
        	for(MyClass myClass : classMap.keySet()) {
        		Set<String> classEntitySet = classMap.get(myClass);
        		ClassEntityPlacement entityPlacement = virtualSystemEntityPlacement.getClassEntityPlacement(myClass.getName());
        		if(entity.getClassOrigin().equals(myClass.getName())) {
        			double distance;
        			if( ((changedEntities.contains(entity) || entityClassDependencies.contains(myClass.getName())) && changedClasses.contains(myClass)) ||
        					newMovedMethods.contains(entity) ) {
        				Set<String> tempClassSet = new HashSet<String>();
                        tempClassSet.addAll(classEntitySet);
                        tempClassSet.remove(entityName);
        				distance = DistanceCalculator.getDistance(entitySet, tempClassSet);
        			}
        			else
        				distance = originalDistanceMatrix.getDistance(entityName, myClass.getName());
        			entityPlacement.setInnerSum(entityPlacement.getInnerSum() + distance);
        			entityPlacement.setNumberOfInnerEntities(entityPlacement.getNumberOfInnerEntities() + 1);
        		}
        		else {
        			double distance;
        			if( ((changedEntities.contains(entity) || entityClassDependencies.contains(myClass.getName())) && changedClasses.contains(myClass)) ||
        					newMovedMethods.contains(entity) ) {
        				distance = DistanceCalculator.getDistance(entitySet, classEntitySet);
        			}
        			else
        				distance = originalDistanceMatrix.getDistance(entityName, myClass.getName());
        			entityPlacement.setOuterSum(entityPlacement.getOuterSum() + distance);
        			entityPlacement.setNumberOfOuterEntities(entityPlacement.getNumberOfOuterEntities() + 1);
        		}
        	}
        }
	}

	public double getSystemEntityPlacementValue() {
		return virtualSystemEntityPlacement.getSystemEntityPlacementValue();
	}
}
