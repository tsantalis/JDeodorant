package gr.uom.java.distance;

import gr.uom.java.ast.decomposition.ExtractionBlock;

import java.util.*;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class DistanceMatrix {
    private Map<String,Integer> entityIndexMap;
    private Map<String,Integer> classIndexMap;
    private List<Entity> entityList;
    private List<MyClass> classList;
    //holds the entity set of each entity
    private Map<String,Set<String>> entityMap;

    //holds the entity set of each class
    private Map<String,Set<String>> classMap;
    private Double[][] distanceMatrix;
    private String[] entityNames;
    private String[] classNames;
    private SystemEntityPlacement systemEntityPlacement;
    private MySystem system;

    public DistanceMatrix(MySystem system) {
        this.system = system;
        entityIndexMap = new LinkedHashMap<String,Integer>();
        classIndexMap = new LinkedHashMap<String,Integer>();
        entityList = new ArrayList<Entity>();
        classList = new ArrayList<MyClass>();
        entityMap = new LinkedHashMap<String,Set<String>>();
        classMap = new LinkedHashMap<String,Set<String>>();

        Iterator<MyClass> classIt = system.getClassIterator();
        while(classIt.hasNext()) {
            MyClass myClass = classIt.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while(attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                if(!attribute.isReference()) {
                    entityList.add(attribute);
                    entityMap.put(attribute.toString(),attribute.getEntitySet());
                }
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod method = methodIterator.next();
                entityList.add(method);
                entityMap.put(method.toString(),method.getEntitySet());
            }
            classList.add(myClass);
            classMap.put(myClass.getName(),myClass.getEntitySet());
        }

        entityNames = new String[entityList.size()];
        classNames = new String[classList.size()];
        distanceMatrix = new Double[entityList.size()][classList.size()];
        systemEntityPlacement = new SystemEntityPlacement();

        int i = 0;
        for(Entity entity : entityList) {
            entityNames[i] = entity.toString();
            entityIndexMap.put(entityNames[i],i);
            int j = 0;
            for(MyClass myClass : classList) {
                classNames[j] = myClass.getName();
                if(!classIndexMap.containsKey(classNames[j]))
                    classIndexMap.put(classNames[j],j);
                ClassEntityPlacement entityPlacement = systemEntityPlacement.getClassEntityPlacement(myClass.getName());
                if(entity.getClassOrigin().equals(myClass.getName())) {
                    Set<String> tempClassSet = new HashSet<String>();
                    tempClassSet.addAll(classMap.get(classNames[j]));
                    tempClassSet.remove(entityNames[i]);
                    distanceMatrix[i][j] = DistanceCalculator.getDistance(entityMap.get(entityNames[i]),tempClassSet);
                    entityPlacement.setInnerSum(entityPlacement.getInnerSum() + distanceMatrix[i][j]);
                    entityPlacement.setNumberOfInnerEntities(entityPlacement.getNumberOfInnerEntities() + 1);
                }
                else {
                    distanceMatrix[i][j] = DistanceCalculator.getDistance(entityMap.get(entityNames[i]),classMap.get(classNames[j]));
                    entityPlacement.setOuterSum(entityPlacement.getOuterSum() + distanceMatrix[i][j]);
                    entityPlacement.setNumberOfOuterEntities(entityPlacement.getNumberOfOuterEntities() + 1);
                }
                j++;
            }
            i++;
        }
    }

    public List<MoveMethodCandidateRefactoring> getMoveMethodCandidateRefactorings() {
    	List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<MoveMethodCandidateRefactoring>();
        Map<Integer,Double> minValueMap = new LinkedHashMap<Integer,Double>();
        for(int i=0; i<distanceMatrix.length; i++) {
            double minValue = 1;
            for(int j=0; j<distanceMatrix[i].length; j++) {
                if(distanceMatrix[i][j] < minValue) {
                    minValue = distanceMatrix[i][j];
                }
            }
            minValueMap.put(i,minValue);
        }

        for(Integer i : minValueMap.keySet()) {
            double minValue = minValueMap.get(i);
            String sourceClass = entityList.get(i).getClassOrigin();
            if(minValue != 1) {
                for(int j=0; j<distanceMatrix[i].length; j++) {
                    if(distanceMatrix[i][j] == minValue && !sourceClass.equals(classNames[j])) {
                        Entity entity = entityList.get(i);
                        MyClass targetClass = classList.get(j);
                        if(entity instanceof MyMethod) {
                        	MyMethod method = (MyMethod)entity;
                        	Set<String> methodEntitySet = entityMap.get(method.toString());
                        	Set<String> sourceClassEntitySet = classMap.get(sourceClass);
                        	Set<String> targetClassEntitySet = classMap.get(targetClass.getName());
                        	Set<String> intersectionWithSourceClass = DistanceCalculator.intersection(methodEntitySet, sourceClassEntitySet);
                        	Set<String> intersectionWithTargetClass = DistanceCalculator.intersection(methodEntitySet, targetClassEntitySet);
                            MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system,system.getClass(sourceClass),targetClass,method,this);
                            Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved = candidate.getAdditionalMethodsToBeMoved();
                            Collection<MethodDeclaration> values = additionalMethodsToBeMoved.values();
                            Set<String> entitiesToRemoveFromIntersectionWithSourceClass = new LinkedHashSet<String>();
                            if(!values.isEmpty()) {
                            	for(String s : intersectionWithSourceClass) {
                            		int entityPosition = entityIndexMap.get(s);
                            		Entity e = entityList.get(entityPosition);
                            		if(e instanceof MyMethod) {
                            			MyMethod invokedMethod = (MyMethod)e;
                            			if(values.contains(invokedMethod.getMethodObject().getMethodDeclaration())) {
                            				entitiesToRemoveFromIntersectionWithSourceClass.add(s);
                            			}
                            		}
                            	}
                            	intersectionWithSourceClass.removeAll(entitiesToRemoveFromIntersectionWithSourceClass);
                            }
                            if(intersectionWithTargetClass.size() >= intersectionWithSourceClass.size()) {
	                            boolean applicable = candidate.apply();
	                            if(applicable)
	                            	candidateRefactoringList.add(candidate);
                            }
                        }
                    }
                }
            }
        }
        return candidateRefactoringList;
    }

    public List<ExtractAndMoveMethodCandidateRefactoring> getExtractAndMoveMethodCandidateRefactorings() {
    	List<ExtractAndMoveMethodCandidateRefactoring> extractMethodCandidateRefactoringList = new ArrayList<ExtractAndMoveMethodCandidateRefactoring>();
    	Iterator<MyClass> classIt = system.getClassIterator();
        while(classIt.hasNext()) {
            MyClass myClass = classIt.next();
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod method = methodIterator.next();
                if(method.getMethodObject().getMethodBody() != null) {
	                List<ExtractionBlock> extractionBlockList = method.getMethodObject().getMethodBody().generateExtractionBlocks();
	                for(ExtractionBlock block : extractionBlockList) {
	                	ExtractAndMoveMethodCandidateRefactoring candidate = 
	                		new ExtractAndMoveMethodCandidateRefactoring(system,myClass,myClass,method,block,this);
	                	extractMethodCandidateRefactoringList.add(candidate);
	                }
                }
            }
        }

        Double[][] blockDistanceMatrix = new Double[extractMethodCandidateRefactoringList.size()][classList.size()];
        
        for(int i=0; i<extractMethodCandidateRefactoringList.size(); i++) {
        	ExtractAndMoveMethodCandidateRefactoring candidate = extractMethodCandidateRefactoringList.get(i);
        	for(int j=0; j<classList.size(); j++) {
        		blockDistanceMatrix[i][j] = DistanceCalculator.getDistance(candidate.getEntitySet(), classMap.get(classNames[j]));
        	}
        }
        
    	List<ExtractAndMoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<ExtractAndMoveMethodCandidateRefactoring>();;
    	Map<Integer,Double> minValueMap = new LinkedHashMap<Integer,Double>();
        for(int i=0; i<blockDistanceMatrix.length; i++) {
            double minValue = 1;
            for(int j=0; j<blockDistanceMatrix[i].length; j++) {
                if(blockDistanceMatrix[i][j] < minValue) {
                    minValue = blockDistanceMatrix[i][j];
                }
            }
            minValueMap.put(i,minValue);
        }
        
        Map<String, ArrayList<ExtractAndMoveMethodCandidateRefactoring>> extractedMethodNameMap = new LinkedHashMap<String, ArrayList<ExtractAndMoveMethodCandidateRefactoring>>();
        for(Integer i : minValueMap.keySet()) {
            double minValue = minValueMap.get(i);
            ExtractAndMoveMethodCandidateRefactoring candidate = extractMethodCandidateRefactoringList.get(i);
            String rowClass = candidate.getSourceClass().getName();
            if(minValue != 1) {
                for(int j=0; j<blockDistanceMatrix[i].length; j++) {
                    if(blockDistanceMatrix[i][j] == minValue) {
                        if(!rowClass.equals(classNames[j])) {
                        	MyClass targetClass = classList.get(j);
                        	ExtractAndMoveMethodCandidateRefactoring newCandidate = 
                        		new ExtractAndMoveMethodCandidateRefactoring(system,candidate.getSourceClass(),targetClass,
                        		candidate.getSourceMethod(),candidate.getExtractionBlock(),this);
                        	boolean applicable = newCandidate.apply();
                        	if(applicable) {
                        		candidateRefactoringList.add(newCandidate);
                        		if(!extractedMethodNameMap.containsKey(newCandidate.getExtractionBlock().getExtractedMethodName()+targetClass.getName())) {
        	                		ArrayList<ExtractAndMoveMethodCandidateRefactoring> list = new ArrayList<ExtractAndMoveMethodCandidateRefactoring>();
        	                		list.add(newCandidate);
        	                		extractedMethodNameMap.put(newCandidate.getExtractionBlock().getExtractedMethodName()+targetClass.getName(), list);
        	                	}
        	                	else {
        	                		ArrayList<ExtractAndMoveMethodCandidateRefactoring> list = extractedMethodNameMap.get(newCandidate.getExtractionBlock().getExtractedMethodName()+targetClass.getName());
        	                		list.add(newCandidate);
        	                	}
                        	}
                        }
                    }
                }
            }
        }

        for(String extractedMethodName : extractedMethodNameMap.keySet()) {
        	List<ExtractAndMoveMethodCandidateRefactoring> list = extractedMethodNameMap.get(extractedMethodName);
        	if(list.size() > 1) {
        		for(ExtractAndMoveMethodCandidateRefactoring candidate : list) {
        			candidate.setDistinctExtractedMethodName();
        		}
        	}
        }
        return candidateRefactoringList;
    }

    public String[] getClassNames() {
        return classNames;
    }

    public String[] getEntityNames() {
        return entityNames;
    }
    
    public Double[][] getDistanceMatrix() {
        return distanceMatrix;
    }

    public Double getDistance(String entityName, String className) {
    	if(entityIndexMap.containsKey(entityName))
    		return distanceMatrix[entityIndexMap.get(entityName)][classIndexMap.get(className)];
    	else
    		return null;
    }

    public SystemEntityPlacement getSystemEntityPlacement() {
        return systemEntityPlacement;
    }

    public double getSystemEntityPlacementValue() {
        return systemEntityPlacement.getSystemEntityPlacementValue();
    }

	public List<MyClass> getClassList() {
		return classList;
	} 
}
