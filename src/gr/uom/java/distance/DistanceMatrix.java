package gr.uom.java.distance;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.association.Association;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.ExtractionBlock;
import gr.uom.java.ast.decomposition.StatementObject;

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
    private Integer[][] accessMatrix;
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
        accessMatrix = new Integer[entityList.size()][classList.size()];
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
                    Set<String> entitySet = entityMap.get(entityNames[i]);
                    Set<String> intersection = DistanceCalculator.intersection(entitySet,tempClassSet);
                    Set<String> union = DistanceCalculator.union(entitySet,tempClassSet);
                    accessMatrix[i][j] = intersection.size();
                    if(union.isEmpty())
                    	distanceMatrix[i][j] = 1.0;
                    else
                    	distanceMatrix[i][j] = 1.0 - (double)intersection.size()/(double)union.size();
                    //distanceMatrix[i][j] = DistanceCalculator.getDistance(entityMap.get(entityNames[i]),tempClassSet);
                    entityPlacement.setInnerSum(entityPlacement.getInnerSum() + distanceMatrix[i][j]);
                    entityPlacement.setNumberOfInnerEntities(entityPlacement.getNumberOfInnerEntities() + 1);
                }
                else {
                    Set<String> entitySet = entityMap.get(entityNames[i]);
                    Set<String> classSet = classMap.get(classNames[j]);
                    Set<String> intersection = DistanceCalculator.intersection(entitySet,classSet);
                    Set<String> union = DistanceCalculator.union(entitySet,classSet);
                    accessMatrix[i][j] = intersection.size();
                    if(union.isEmpty())
                    	distanceMatrix[i][j] = 1.0;
                    else
                    	distanceMatrix[i][j] = 1.0 - (double)intersection.size()/(double)union.size();
                	//distanceMatrix[i][j] = DistanceCalculator.getDistance(entityMap.get(entityNames[i]),classMap.get(classNames[j]));
                    entityPlacement.setOuterSum(entityPlacement.getOuterSum() + distanceMatrix[i][j]);
                    entityPlacement.setNumberOfOuterEntities(entityPlacement.getNumberOfOuterEntities() + 1);
                }
                j++;
            }
            i++;
        }
    }

    private List<MoveMethodCandidateRefactoring> identifyConceptualBindings(MyMethod method, Set<String> targetClasses) {
    	List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<MoveMethodCandidateRefactoring>();
    	MethodObject methodObject = method.getMethodObject();
    	String sourceClass = method.getClassOrigin();
    	for(String targetClass : targetClasses) {
    		if(!targetClass.equals(sourceClass)) {
	    		ClassObject targetClassObject = system.getClass(targetClass).getClassObject();
	    		ListIterator<ParameterObject> parameterIterator = methodObject.getParameterListIterator();
	    		while(parameterIterator.hasNext()) {
	    			ParameterObject parameter = parameterIterator.next();
	    			Association association = system.containsAssociationWithMultiplicityBetweenClasses(targetClass, parameter.getType().getClassType());
	    			if(association != null) {
	    				List<MethodInvocationObject> methodInvocations = methodObject.getMethodInvocations();
	    				for(MethodInvocationObject methodInvocation : methodInvocations) {
	    					if(methodInvocation.getOriginClassName().equals(targetClass)) {
	    						List<TypeObject> methodInvocationParameterTypes = methodInvocation.getParameterTypeList();
	    						if(methodInvocationParameterTypes.contains(parameter.getType())) {
	    							List<AbstractStatement> methodInvocationStatements = methodObject.getMethodInvocationStatements(methodInvocation);
	    							boolean parameterIsPassedAsArgument = false;
	    							for(AbstractStatement abstractStatement : methodInvocationStatements) {
	    								if(abstractStatement instanceof StatementObject) {
	    									StatementObject statement = (StatementObject)abstractStatement;
	    									if(statement.containsMethodInvocation(methodInvocation)) {
	    										List<LocalVariableInstructionObject> localVariableInstructions = statement.getLocalVariableInstructions();
	    										for(LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
	    											if(localVariableInstruction.getType().equals(parameter.getType()) && localVariableInstruction.getName().equals(parameter.getName())) {
	    												parameterIsPassedAsArgument = true;
	    												break;
	    											}
	    										}
	    									}
	    								}
	    								else if(abstractStatement instanceof CompositeStatementObject) {
	    									CompositeStatementObject compositeStatement = (CompositeStatementObject)abstractStatement;
	    									List<AbstractExpression> expressions = compositeStatement.getExpressions();
	    									for(AbstractExpression expression : expressions) {
	    										if(expression.containsMethodInvocation(methodInvocation)) {
	    											List<LocalVariableInstructionObject> localVariableInstructions = expression.getLocalVariableInstructions();
	        										for(LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
	        											if(localVariableInstruction.getType().equals(parameter.getType()) && localVariableInstruction.getName().equals(parameter.getName())) {
	        												parameterIsPassedAsArgument = true;
	        												break;
	        											}
	        										}
	    										}
	    									}
	    								}
	    							}
	    							if(parameterIsPassedAsArgument) {
	    								MethodObject invokedMethod = targetClassObject.getMethod(methodInvocation);
	    								List<FieldInstructionObject> fieldInstructions = invokedMethod.getFieldInstructions();
	    								boolean containerFieldIsAccessed = false;
	    								for(FieldInstructionObject fieldInstruction : fieldInstructions) {
	    									if(association.getFieldObject().equals(fieldInstruction)) {
	    										containerFieldIsAccessed = true;
	    										break;
	    									}
	    								}
	    								if(containerFieldIsAccessed) {
	    									MyClass mySourceClass = classList.get(classIndexMap.get(sourceClass));
	    			    					MyClass myTargetClass = classList.get(classIndexMap.get(targetClass));
	    			    					MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system,mySourceClass,myTargetClass,method,this);
	    			    					Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved = candidate.getAdditionalMethodsToBeMoved();
	    			                        Collection<MethodDeclaration> values = additionalMethodsToBeMoved.values();
	    			                        Set<String> methodEntitySet = entityMap.get(method.toString());
	    			                    	Set<String> sourceClassEntitySet = classMap.get(sourceClass);
	    			                    	Set<String> targetClassEntitySet = classMap.get(targetClass);
	    			                    	Set<String> intersectionWithSourceClass = DistanceCalculator.intersection(methodEntitySet, sourceClassEntitySet);
	    			                    	Set<String> intersectionWithTargetClass = DistanceCalculator.intersection(methodEntitySet, targetClassEntitySet);
	    			                        Set<String> entitiesToRemoveFromIntersectionWithSourceClass = new LinkedHashSet<String>();
	    			                        if(!values.isEmpty()) {
	    			                        	for(String s : intersectionWithSourceClass) {
	    			                        		int entityPosition = entityIndexMap.get(s);
	    			                        		Entity e = entityList.get(entityPosition);
	    			                        		if(e instanceof MyMethod) {
	    			                        			MyMethod myInvokedMethod = (MyMethod)e;
	    			                        			if(values.contains(myInvokedMethod.getMethodObject().getMethodDeclaration())) {
	    			                        				entitiesToRemoveFromIntersectionWithSourceClass.add(s);
	    			                        			}
	    			                        		}
	    			                        	}
	    			                        	intersectionWithSourceClass.removeAll(entitiesToRemoveFromIntersectionWithSourceClass);
	    			                        }
	    			                        if(intersectionWithTargetClass.size() >= intersectionWithSourceClass.size()) {
	    			                            if(candidate.isApplicable()) {
	    			                            	candidate.apply();
	    			                            	candidateRefactoringList.add(candidate);
	    			                            }
	    			                        }
	    								}
	    							}
	    						}
	    					}
	    				}
	    			}
	    		}
    		}
    	}
    	return candidateRefactoringList;
    }

    public List<MoveMethodCandidateRefactoring> getMoveMethodCandidateRefactoringsByAccess() {
    	List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<MoveMethodCandidateRefactoring>();
    	for(int i=0; i<distanceMatrix.length; i++) {
    		Entity entity = entityList.get(i);
    		if(entity instanceof MyMethod) {
    			String sourceClass = entity.getClassOrigin();
    			MyMethod method = (MyMethod)entity;
    			Set<String> entitySetI = entityMap.get(entityNames[i]);
    			//ArrayList<String> contains the accessed entities per target class (key)
    			Map<String, ArrayList<String>> accessMap = new LinkedHashMap<String, ArrayList<String>>();
    			for(String e : entitySetI) {
    				String[] tokens = e.split("::");
    				String classOrigin = tokens[0];
    				String entityName = tokens[1];
    				if(accessMap.containsKey(classOrigin)) {
    					ArrayList<String> list = accessMap.get(classOrigin);
    					list.add(entityName);
    				}
    				else {
    					ArrayList<String> list = new ArrayList<String>();
    					list.add(entityName);
    					accessMap.put(classOrigin, list);
    				}
    			}
    			List<MoveMethodCandidateRefactoring> conceptuallyBoundRefactorings = identifyConceptualBindings(method, accessMap.keySet());
    			if(!conceptuallyBoundRefactorings.isEmpty()) {
    				candidateRefactoringList.addAll(conceptuallyBoundRefactorings);
    			}
    			else {
	    			//ArrayList<String> contains the target classes from which key number of entities are accessed
	    			TreeMap<Integer, ArrayList<String>> sortedByAccessMap = new TreeMap<Integer, ArrayList<String>>();
	    			for(String targetClass : accessMap.keySet()) {
	    				int numberOfAccessedEntities = accessMap.get(targetClass).size();
	    				if(sortedByAccessMap.containsKey(numberOfAccessedEntities)) {
	    					ArrayList<String> list = sortedByAccessMap.get(numberOfAccessedEntities);
	    					list.add(targetClass);
	    				}
	    				else {
	    					ArrayList<String> list = new ArrayList<String>();
	    					list.add(targetClass);
	    					sortedByAccessMap.put(numberOfAccessedEntities, list);
	    				}
	    			}
	
	    			boolean candidateFound = false;
	    			boolean sourceClassIsTarget = false;
	    			while(!candidateFound && !sourceClassIsTarget && !sortedByAccessMap.isEmpty()) {
	    				ArrayList<String> targetClasses = sortedByAccessMap.get(sortedByAccessMap.lastKey());
	    				//target classes are sorted by the distance of entity i from them
	    				TreeMap<Double, ArrayList<String>> sortedByDistanceMap = new TreeMap<Double, ArrayList<String>>();
	    				for(String targetClass : targetClasses) {
	    					double distance = distanceMatrix[i][classIndexMap.get(targetClass)];
	    					if(sortedByDistanceMap.containsKey(distance)) {
	    						ArrayList<String> list = sortedByDistanceMap.get(distance);
	    						list.add(targetClass);
	    					}
	    					else {
	    						ArrayList<String> list = new ArrayList<String>();
	        					list.add(targetClass);
	        					sortedByDistanceMap.put(distance, list);
	    					}
	    				}
	    				for(Double distance : sortedByDistanceMap.keySet()) {
	    					ArrayList<String> targetClassesPerDistance = sortedByDistanceMap.get(distance);
	    					for(String targetClass : targetClassesPerDistance) {
		    					if(sourceClass.equals(targetClass)) {
		    						sourceClassIsTarget = true;
		    					}
		    					else {
			    					MyClass mySourceClass = classList.get(classIndexMap.get(sourceClass));
			    					MyClass myTargetClass = classList.get(classIndexMap.get(targetClass));
			    					MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system,mySourceClass,myTargetClass,method,this);
			    					Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved = candidate.getAdditionalMethodsToBeMoved();
			                        Collection<MethodDeclaration> values = additionalMethodsToBeMoved.values();
			                        Set<String> methodEntitySet = entityMap.get(method.toString());
			                    	Set<String> sourceClassEntitySet = classMap.get(sourceClass);
			                    	Set<String> targetClassEntitySet = classMap.get(targetClass);
			                    	Set<String> intersectionWithSourceClass = DistanceCalculator.intersection(methodEntitySet, sourceClassEntitySet);
			                    	Set<String> intersectionWithTargetClass = DistanceCalculator.intersection(methodEntitySet, targetClassEntitySet);
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
			                            if(candidate.isApplicable()) {
			                            	candidate.apply();
			                            	candidateRefactoringList.add(candidate);
			                            	candidateFound = true;
			                            }
			                        }
		    					}
	    					}
	    					if(candidateFound || sourceClassIsTarget)
	    						break;
	    				}
	    				sortedByAccessMap.remove(sortedByAccessMap.lastKey());
	    			}
    			}
    		}
    	}
    	return candidateRefactoringList;
    }

    public List<MoveMethodCandidateRefactoring> getMoveMethodCandidateRefactoringsByDistance() {
    	List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<MoveMethodCandidateRefactoring>();
    	for(int i=0; i<distanceMatrix.length; i++) {
    		Entity entity = entityList.get(i);
    		if(entity instanceof MyMethod) {
    			String sourceClass = entity.getClassOrigin();
    			MyMethod method = (MyMethod)entity;
    			Set<String> entitySetI = entityMap.get(entityNames[i]);
    			//ArrayList<String> contains the accessed entities per target class (key)
    			Map<String, ArrayList<String>> accessMap = new LinkedHashMap<String, ArrayList<String>>();
    			for(String e : entitySetI) {
    				String[] tokens = e.split("::");
    				String classOrigin = tokens[0];
    				String entityName = tokens[1];
    				if(accessMap.containsKey(classOrigin)) {
    					ArrayList<String> list = accessMap.get(classOrigin);
    					list.add(entityName);
    				}
    				else {
    					ArrayList<String> list = new ArrayList<String>();
    					list.add(entityName);
    					accessMap.put(classOrigin, list);
    				}
    			}
    			//ArrayList<String> contains the target classes from which entity i has key distance
    			TreeMap<Double, ArrayList<String>> sortedByDistanceMap = new TreeMap<Double, ArrayList<String>>();
    			for(String targetClass : accessMap.keySet()) {
    				double distance = distanceMatrix[i][classIndexMap.get(targetClass)];
    				if(sortedByDistanceMap.containsKey(distance)) {
    					ArrayList<String> list = sortedByDistanceMap.get(distance);
    					list.add(targetClass);
    				}
    				else {
    					ArrayList<String> list = new ArrayList<String>();
    					list.add(targetClass);
    					sortedByDistanceMap.put(distance, list);
    				}
    			}
    			
    			boolean candidateFound = false;
    			boolean sourceClassIsTarget = false;
    			while(!candidateFound && !sourceClassIsTarget && !sortedByDistanceMap.isEmpty()) {
    				ArrayList<String> targetClasses = sortedByDistanceMap.get(sortedByDistanceMap.firstKey());
    				//target classes are sorted by the number of entities that entity i accesses from them
    				TreeMap<Integer, ArrayList<String>> sortedByAccessMap = new TreeMap<Integer, ArrayList<String>>();
    				for(String targetClass : targetClasses) {
    					int numberOfAccessedEntities = accessMap.get(targetClass).size();
    					if(sortedByAccessMap.containsKey(numberOfAccessedEntities)) {
    						ArrayList<String> list = sortedByAccessMap.get(numberOfAccessedEntities);
    						list.add(targetClass);
    					}
    					else {
    						ArrayList<String> list = new ArrayList<String>();
        					list.add(targetClass);
        					sortedByAccessMap.put(numberOfAccessedEntities, list);
    					}
    				}
    				List<Integer> keyList = new ArrayList<Integer>(sortedByAccessMap.keySet());
    				ListIterator<Integer> keyListIterator = keyList.listIterator(keyList.size());
    				while(keyListIterator.hasPrevious()) {
    					Integer accesses = keyListIterator.previous();
    					ArrayList<String> targetClassesPerAccess = sortedByAccessMap.get(accesses);
    					for(String targetClass : targetClassesPerAccess) {
	    					if(sourceClass.equals(targetClass)) {
	    						sourceClassIsTarget = true;
	    					}
	    					else {
		    					MyClass mySourceClass = classList.get(classIndexMap.get(sourceClass));
		    					MyClass myTargetClass = classList.get(classIndexMap.get(targetClass));
		    					MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system,mySourceClass,myTargetClass,method,this);
		    					Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved = candidate.getAdditionalMethodsToBeMoved();
		                        Collection<MethodDeclaration> values = additionalMethodsToBeMoved.values();
		                        Set<String> methodEntitySet = entityMap.get(method.toString());
		                    	Set<String> sourceClassEntitySet = classMap.get(sourceClass);
		                    	Set<String> targetClassEntitySet = classMap.get(targetClass);
		                    	Set<String> intersectionWithSourceClass = DistanceCalculator.intersection(methodEntitySet, sourceClassEntitySet);
		                    	Set<String> intersectionWithTargetClass = DistanceCalculator.intersection(methodEntitySet, targetClassEntitySet);
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
		                            if(candidate.isApplicable()) {
		                            	candidate.apply();
		                            	candidateRefactoringList.add(candidate);
		                            	candidateFound = true;
		                            }
		                        }
	    					}
    					}
    					if(candidateFound || sourceClassIsTarget)
    						break;
    				}
    				sortedByDistanceMap.remove(sortedByDistanceMap.firstKey());
    			}
    		}
    	}
    	return candidateRefactoringList;
    }

    public List<ExtractAndMoveMethodCandidateRefactoring> getExtractAndMoveMethodCandidateRefactoringsByAccess() {
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

        List<ExtractAndMoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<ExtractAndMoveMethodCandidateRefactoring>();
        Map<String, ArrayList<ExtractAndMoveMethodCandidateRefactoring>> extractedMethodNameMap = new LinkedHashMap<String, ArrayList<ExtractAndMoveMethodCandidateRefactoring>>();
        for(ExtractAndMoveMethodCandidateRefactoring candidate : extractMethodCandidateRefactoringList) {
        	String sourceClass = candidate.getSource();
        	Set<String> entitySetI = candidate.getEntitySet();
        	//ArrayList<String> contains the accessed entities per target class (key)
			Map<String, ArrayList<String>> accessMap = new LinkedHashMap<String, ArrayList<String>>();
			for(String e : entitySetI) {
				String[] tokens = e.split("::");
				String classOrigin = tokens[0];
				String entityName = tokens[1];
				if(accessMap.containsKey(classOrigin)) {
					ArrayList<String> list = accessMap.get(classOrigin);
					list.add(entityName);
				}
				else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(entityName);
					accessMap.put(classOrigin, list);
				}
			}
			//ArrayList<String> contains the target classes from which key number of entities are accessed
			TreeMap<Integer, ArrayList<String>> sortedByAccessMap = new TreeMap<Integer, ArrayList<String>>();
			for(String targetClass : accessMap.keySet()) {
				int numberOfAccessedEntities = accessMap.get(targetClass).size();
				if(sortedByAccessMap.containsKey(numberOfAccessedEntities)) {
					ArrayList<String> list = sortedByAccessMap.get(numberOfAccessedEntities);
					list.add(targetClass);
				}
				else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(targetClass);
					sortedByAccessMap.put(numberOfAccessedEntities, list);
				}
			}
			
			boolean candidateFound = false;
			boolean sourceClassIsTarget = false;
			while(!candidateFound && !sourceClassIsTarget && !sortedByAccessMap.isEmpty()) {
				ArrayList<String> targetClasses = sortedByAccessMap.get(sortedByAccessMap.lastKey());
				//target classes are sorted by the distance of entity i from them
				TreeMap<Double, ArrayList<String>> sortedByDistanceMap = new TreeMap<Double, ArrayList<String>>();
				for(String targetClass : targetClasses) {
					double distance = DistanceCalculator.getDistance(entitySetI, classMap.get(targetClass));
					if(sortedByDistanceMap.containsKey(distance)) {
						ArrayList<String> list = sortedByDistanceMap.get(distance);
						list.add(targetClass);
					}
					else {
						ArrayList<String> list = new ArrayList<String>();
    					list.add(targetClass);
    					sortedByDistanceMap.put(distance, list);
					}
				}
				for(Double distance : sortedByDistanceMap.keySet()) {
					ArrayList<String> targetClassesPerDistance = sortedByDistanceMap.get(distance);
					for(String targetClass : targetClassesPerDistance) {
    					if(sourceClass.equals(targetClass)) {
    						sourceClassIsTarget = true;
    					}
    					else {
    						MyClass myTargetClass = classList.get(classIndexMap.get(targetClass));
    						ExtractAndMoveMethodCandidateRefactoring newCandidate = 
                        		new ExtractAndMoveMethodCandidateRefactoring(system,candidate.getSourceClass(),myTargetClass,
                        		candidate.getSourceMethod(),candidate.getExtractionBlock(),this);
    						if(newCandidate.isApplicable()) {
    							newCandidate.apply();
    							candidateRefactoringList.add(newCandidate);
                        		if(!extractedMethodNameMap.containsKey(newCandidate.getExtractionBlock().getExtractedMethodName()+myTargetClass.getName())) {
        	                		ArrayList<ExtractAndMoveMethodCandidateRefactoring> list = new ArrayList<ExtractAndMoveMethodCandidateRefactoring>();
        	                		list.add(newCandidate);
        	                		extractedMethodNameMap.put(newCandidate.getExtractionBlock().getExtractedMethodName()+myTargetClass.getName(), list);
        	                	}
        	                	else {
        	                		ArrayList<ExtractAndMoveMethodCandidateRefactoring> list = extractedMethodNameMap.get(newCandidate.getExtractionBlock().getExtractedMethodName()+myTargetClass.getName());
        	                		list.add(newCandidate);
        	                	}
                        		candidateFound = true;
    						}
    					}
					}
					if(candidateFound || sourceClassIsTarget)
						break;
				}
				sortedByAccessMap.remove(sortedByAccessMap.lastKey());
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
