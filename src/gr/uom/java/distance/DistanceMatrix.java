package gr.uom.java.distance;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.association.Association;
import gr.uom.java.ast.util.math.Cluster;
import gr.uom.java.ast.util.math.Clustering;

import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

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
    }

    public void generateDistances(IProgressMonitor monitor) {
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
        
        if(monitor != null)
        	monitor.beginTask("Calculating distances", entityList.size()*classList.size());
        int i = 0;
        for(Entity entity : entityList) {
            entityNames[i] = entity.toString();
            entityIndexMap.put(entityNames[i],i);
            int j = 0;
            for(MyClass myClass : classList) {
            	if(monitor != null && monitor.isCanceled())
        			throw new OperationCanceledException();
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
                if(monitor != null)
                	monitor.worked(1);
            }
            i++;
        }
        if(monitor != null)
        	monitor.done();
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
	    						MethodInvocation invocation = methodInvocation.getMethodInvocation();
	    						boolean parameterIsPassedAsArgument = false;
	    						List<Expression> invocationArguments = invocation.arguments();
	    						for(Expression expression : invocationArguments) {
	    							if(expression instanceof SimpleName) {
	    								SimpleName argumentName = (SimpleName)expression;
	    								if(parameter.getSingleVariableDeclaration().resolveBinding().isEqualTo(argumentName.resolveBinding()))
	    									parameterIsPassedAsArgument = true;
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
    	return candidateRefactoringList;
    }

    private boolean targetClassInheritedByAnotherCandidateTargetClass(String targetClass, Set<String> candidateTargetClasses) {
    	for(String candidateTargetClass : candidateTargetClasses) {
    		if(!candidateTargetClass.equals(targetClass)) {
    			MyClass currentSuperclass = classList.get(classIndexMap.get(candidateTargetClass));
    			String superclass = null;
    			while((superclass = currentSuperclass.getSuperclass()) != null) {
    				if(superclass.equals(targetClass))
    					return true;
    				currentSuperclass = classList.get(classIndexMap.get(superclass));
    			}
    		}
    	}
    	return false;
    }

    public List<MoveMethodCandidateRefactoring> getMoveMethodCandidateRefactoringsByAccess(Set<String> classNamesToBeExamined, IProgressMonitor monitor) {
    	List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<MoveMethodCandidateRefactoring>();
    	double entityPlacement0 = this.getSystemEntityPlacementValue();
    	if(monitor != null)
    		monitor.beginTask("Identification and virtual application of Move Method refactoring opportunities", distanceMatrix.length);
    	for(int i=0; i<distanceMatrix.length; i++) {
    		if(monitor != null && monitor.isCanceled())
    			throw new OperationCanceledException();
    		Entity entity = entityList.get(i);
    		if(entity instanceof MyMethod) {
    			String sourceClass = entity.getClassOrigin();
    			if(classNamesToBeExamined.contains(sourceClass)) {
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
    										if(candidate.isApplicable() && !targetClassInheritedByAnotherCandidateTargetClass(targetClass, accessMap.keySet())) {
    											candidate.apply();
    											double entityPlacement1 = candidate.getEntityPlacement();
    					    					double d = entityPlacement1 - entityPlacement0;
    					    					if (d < 0) {
    					    						candidateRefactoringList.add(candidate);
    					    					}
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
    		if(monitor != null)
    			monitor.worked(1);
    	}
    	if(monitor != null)
    		monitor.done();
    	return candidateRefactoringList;
    }

    public List<MoveMethodCandidateRefactoring> getMoveMethodCandidateRefactoringsByDistance() {
    	List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<MoveMethodCandidateRefactoring>();
    	double entityPlacement0 = this.getSystemEntityPlacementValue();
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
		                            	double entityPlacement1 = candidate.getEntityPlacement();
		            					double d = entityPlacement1 - entityPlacement0;
		            					if (d < 0) {
		            						candidateRefactoringList.add(candidate);
		            					}
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

    public List<ExtractClassCandidateRefactoring> getExtractClassCandidateRefactorings(Set<String> classNamesToBeExamined, IProgressMonitor monitor) {
    	List<ExtractClassCandidateRefactoring> candidateList = new ArrayList<ExtractClassCandidateRefactoring>();
    	double entityPlacement0 = this.getSystemEntityPlacementValue();
    	Iterator<MyClass> classIt = system.getClassIterator();
    	ArrayList<MyClass> oldClasses = new ArrayList<MyClass>();

    	while(classIt.hasNext()) {
    		MyClass myClass = classIt.next();
    		if(classNamesToBeExamined.contains(myClass.getName())) {
    			oldClasses.add(myClass);
    		}
    	}
    	if(monitor != null)
    		monitor.beginTask("Identification and virtual application of Extract Class refactoring opportunities", oldClasses.size());

    	for(MyClass sourceClass : oldClasses) {
    		if(monitor != null && monitor.isCanceled())
    			throw new OperationCanceledException();
    		if (!sourceClass.getMethodList().isEmpty() && !sourceClass.getAttributeList().isEmpty()) {
    			ExtractClassCandidateRefactoring candidate = new ExtractClassCandidateRefactoring(system, sourceClass, this);
    			double[][] distanceMatrix = candidate.getJaccardDistanceMatrix();
				Clustering clustering = Clustering.getInstance(0, distanceMatrix);
				ArrayList<Entity> entities = new ArrayList<Entity>();
				entities.addAll(sourceClass.getAttributeList());
				entities.addAll(sourceClass.getMethodList());
				HashSet<Cluster> clusters = clustering.clustering(entities);
				for (Cluster cluster : clusters) {
    				candidate = new ExtractClassCandidateRefactoring(system, sourceClass, this);
    				for (Entity entity : cluster.getEntities()) {
    					candidate.addEntity(entity);
    				}
    				if (candidate.isApplicable()) {
    					candidate.apply();
    					double entityPlacement1 = candidate.getEntityPlacement();
    					double d = entityPlacement1 - entityPlacement0;
    					if (d < 0) {
    						candidateList.add(candidate);
    					}
    				}
    			}
    			// Clustering End
    		}
    		if(monitor != null)
    			monitor.worked(1);
    	}
    	if(monitor != null)
    		monitor.done();
    	return candidateList;
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
