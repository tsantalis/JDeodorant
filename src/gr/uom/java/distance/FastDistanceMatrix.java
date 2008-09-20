package gr.uom.java.distance;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class FastDistanceMatrix {
	//holds the entity set of each entity
	private Map<String,Set<String>> entityMap;

	//holds the entity set of each class
	private Map<String,Set<String>> classMap;
	private SystemEntityPlacement virtualSystemEntityPlacement;
	
	public FastDistanceMatrix(MySystem virtualSystem, DistanceMatrix originalDistanceMatrix, CandidateRefactoring candidate,
			Set<MyMethod> oldMovedMethods, Set<MyMethod> newMovedMethods) {
		String sourceClass = candidate.getSource();
		String targetClass = candidate.getTarget();
		entityMap = new LinkedHashMap<String,Set<String>>();
		classMap = new LinkedHashMap<String,Set<String>>();
		virtualSystemEntityPlacement = new SystemEntityPlacement();
		String sourceMethod = null;
		/*if(candidate instanceof ExtractAndMoveMethodCandidateRefactoring) {
			ExtractAndMoveMethodCandidateRefactoring extractCandidate = (ExtractAndMoveMethodCandidateRefactoring)candidate;
			sourceMethod = extractCandidate.getSourceMethod().toString();
		}*/
		
		Iterator<MyClass> classIt = virtualSystem.getClassIterator();
        while(classIt.hasNext()) {
            MyClass myClass = classIt.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while(attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                if(!attribute.isReference()) {
                	if(attribute.getNewEntitySet() != null)
                		entityMap.put(attribute.toString(),attribute.getNewEntitySet());
                	else
                		entityMap.put(attribute.toString(),attribute.getEntitySet());
                }
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod method = methodIterator.next();
                if(!oldMovedMethods.contains(method)) {
                	if(method.getNewEntitySet() != null)
                		entityMap.put(method.toString(),method.getNewEntitySet());
                	else
                		entityMap.put(method.toString(),method.getEntitySet());
                }
            }
            if(myClass.getNewEntitySet() != null)
            	classMap.put(myClass.getName(),myClass.getNewEntitySet());
            else
            	classMap.put(myClass.getName(),myClass.getEntitySet());
        }
        for(MyMethod newMovedMethod : newMovedMethods) {
        	entityMap.put(newMovedMethod.toString(),newMovedMethod.getEntitySet());
        }
        
        int recalculated = 0;
        int false_rec = 0;
        for(String entityName : entityMap.keySet()) {
        	Set<String> entitySet = entityMap.get(entityName);
        	Set<String> entityDependencies = new HashSet<String>();
        	for(String s : entityMap.get(entityName)) {
        		entityDependencies.add(s.substring(0,s.indexOf("::")));
        	}
        	for(String className : classMap.keySet()) {
        		Set<String> classEntitySet = classMap.get(className);
        		ClassEntityPlacement entityPlacement = virtualSystemEntityPlacement.getClassEntityPlacement(className);
        		if(entityName.startsWith(className + "::")) {
        			Double d = originalDistanceMatrix.getDistance(entityName,className);
        			double distance;
        			if( ((className.startsWith(sourceClass) || className.startsWith(targetClass) || entityDependencies.contains(className)) && (entityName.startsWith(sourceClass + "::") || entityName.startsWith(targetClass + "::"))) 
        					|| ((className.equals(sourceClass) || className.equals(targetClass) || entityDependencies.contains(className)) && (!entityName.startsWith(sourceClass + "::") && !entityName.startsWith(targetClass + "::")) && (entityDependencies.contains(sourceClass) || entityDependencies.contains(targetClass)))
        					|| d == null || (sourceMethod != null && entityName.equals(sourceMethod)) ) {
        				Set<String> tempClassSet = new HashSet<String>();
                        tempClassSet.addAll(classEntitySet);
                        tempClassSet.remove(entityName);
        				distance = DistanceCalculator.getDistance(entitySet,tempClassSet);
        				recalculated++;
        				if(d!=null && d==distance)
        					false_rec++;
        			}
        			else {
        				distance = d;
        			}
        			entityPlacement.setInnerSum(entityPlacement.getInnerSum() + distance);
        			entityPlacement.setNumberOfInnerEntities(entityPlacement.getNumberOfInnerEntities() + 1);
        		}
        		else {
        			Double d = originalDistanceMatrix.getDistance(entityName,className);
        			double distance;
        			if( ((className.startsWith(sourceClass) || className.startsWith(targetClass) || entityDependencies.contains(className)) && (entityName.startsWith(sourceClass + "::") || entityName.startsWith(targetClass + "::"))) 
        					|| ((className.equals(sourceClass) || className.equals(targetClass) || entityDependencies.contains(className)) && (!entityName.startsWith(sourceClass + "::") && !entityName.startsWith(targetClass + "::")) && (entityDependencies.contains(sourceClass) || entityDependencies.contains(targetClass)))
        					|| d == null || (sourceMethod != null && entityName.equals(sourceMethod)) ) {
        				distance = DistanceCalculator.getDistance(entitySet,classEntitySet);
        				recalculated++;
        				if(d!=null && d==distance)
        					false_rec++;
        			}
        			else {
        				distance = d;
        			}
        			entityPlacement.setOuterSum(entityPlacement.getOuterSum() + distance);
        			entityPlacement.setNumberOfOuterEntities(entityPlacement.getNumberOfOuterEntities() + 1);
        		}
        	}
        }
        System.out.println(recalculated + "\t" + false_rec);
	}

	public double getSystemEntityPlacementValue() {
		return virtualSystemEntityPlacement.getSystemEntityPlacementValue();
	}
}
