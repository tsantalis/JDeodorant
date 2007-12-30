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
	
	public FastDistanceMatrix(MySystem virtualSystem, DistanceMatrix originalDistanceMatrix, CandidateRefactoring candidate) {
		String sourceClass = candidate.getSource();
		String targetClass = candidate.getTarget();
		entityMap = new LinkedHashMap<String,Set<String>>();
		classMap = new LinkedHashMap<String,Set<String>>();
		virtualSystemEntityPlacement = new SystemEntityPlacement();
		String sourceMethod = null;
		if(candidate instanceof ExtractAndMoveMethodCandidateRefactoring) {
			ExtractAndMoveMethodCandidateRefactoring extractCandidate = (ExtractAndMoveMethodCandidateRefactoring)candidate;
			sourceMethod = extractCandidate.getSourceMethod().toString();
		}
		
		Iterator<MyClass> classIt = virtualSystem.getClassIterator();
        while(classIt.hasNext()) {
            MyClass myClass = classIt.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while(attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                if(!attribute.isReference()) {
                    entityMap.put(attribute.toString(),attribute.getEntitySet());
                }
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod method = methodIterator.next();
                entityMap.put(method.toString(),method.getEntitySet());
            }
            classMap.put(myClass.getName(),myClass.getEntitySet());
        }
        
        for(String entityName : entityMap.keySet()) {
        	Set<String> entityDependencies = new HashSet<String>();
        	for(String s : entityMap.get(entityName)) {
        		entityDependencies.add(s.substring(0,s.indexOf("::")));
        	}
        	for(String className : classMap.keySet()) {
        		ClassEntityPlacement entityPlacement = virtualSystemEntityPlacement.getClassEntityPlacement(className);
        		if(entityName.startsWith(className + "::")) {
        			Double d = originalDistanceMatrix.getDistance(entityName,className);
        			double distance;
        			if( ((className.startsWith(sourceClass) || className.startsWith(targetClass) || entityDependencies.contains(className)) && (entityName.startsWith(sourceClass + "::") || entityName.startsWith(targetClass + "::"))) 
        					|| ((className.equals(sourceClass) || className.equals(targetClass) || entityDependencies.contains(className)) && (!entityName.startsWith(sourceClass + "::") && !entityName.startsWith(targetClass + "::")) && (entityDependencies.contains(sourceClass) || entityDependencies.contains(targetClass)))
        					|| d == null || (sourceMethod != null && entityName.equals(sourceMethod)) ) {
        				Set<String> tempClassSet = new HashSet<String>();
                        tempClassSet.addAll(classMap.get(className));
                        tempClassSet.remove(entityName);
        				distance = DistanceCalculator.getDistance(entityMap.get(entityName),tempClassSet);
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
        				distance = DistanceCalculator.getDistance(entityMap.get(entityName),classMap.get(className));
        			}
        			else {
        				distance = d;
        			}
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
