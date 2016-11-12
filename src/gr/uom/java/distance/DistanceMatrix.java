package gr.uom.java.distance;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.association.Association;
import gr.uom.java.ast.util.math.Cluster;
import gr.uom.java.ast.util.math.Clustering;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.preference.IPreferenceStore;

public class DistanceMatrix {
    private Map<String,Integer> entityIndexMap;
    private Map<String,Integer> classIndexMap;
    private List<Entity> entityList;
    private List<MyClass> classList;
    //holds the entity set of each entity
    private Map<String,Set<String>> entityMap;
    //holds the entity set of each class
    private Map<String,Set<String>> classMap;
    private MySystem system;
    private int maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate;
    private int maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate;

    public DistanceMatrix(MySystem system) {
        this.system = system;
        entityIndexMap = new LinkedHashMap<String,Integer>();
        classIndexMap = new LinkedHashMap<String,Integer>();
        entityList = new ArrayList<Entity>();
        classList = new ArrayList<MyClass>();
        entityMap = new LinkedHashMap<String,Set<String>>();
        classMap = new LinkedHashMap<String,Set<String>>();
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		this.maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate = store.getInt(PreferenceConstants.P_MAXIMUM_NUMBER_OF_SOURCE_CLASS_MEMBERS_ACCESSED_BY_MOVE_METHOD_CANDIDATE);
		this.maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate = store.getInt(PreferenceConstants.P_MAXIMUM_NUMBER_OF_SOURCE_CLASS_MEMBERS_ACCESSED_BY_EXTRACT_CLASS_CANDIDATE);
		generateDistances();
    }

    private void generateDistances() {
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

        String[] entityNames = new String[entityList.size()];
        String[] classNames = new String[classList.size()];
        
        int i = 0;
        for(Entity entity : entityList) {
            entityNames[i] = entity.toString();
            entityIndexMap.put(entityNames[i],i);
            int j = 0;
            for(MyClass myClass : classList) {
                classNames[j] = myClass.getName();
                if(!classIndexMap.containsKey(classNames[j]))
                    classIndexMap.put(classNames[j],j);
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
	    								MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system,mySourceClass,myTargetClass,method);
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
	    										int sourceClassDependencies = candidate.getDistinctSourceDependencies();
    					    					int targetClassDependencies = candidate.getDistinctTargetDependencies();
    					    					if(sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate &&
    					    							sourceClassDependencies < targetClassDependencies) {
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
    	if(monitor != null)
    		monitor.beginTask("Identification of Move Method refactoring opportunities", entityList.size());
    	for(int i=0; i<entityList.size(); i++) {
    		if(monitor != null && monitor.isCanceled())
    			throw new OperationCanceledException();
    		Entity entity = entityList.get(i);
    		if(entity instanceof MyMethod) {
    			String sourceClass = entity.getClassOrigin();
    			if(classNamesToBeExamined.contains(sourceClass)) {
    				MyMethod method = (MyMethod)entity;
    				Set<String> entitySetI = entityMap.get(entity.toString());
    				Map<String, ArrayList<String>> accessMap = computeAccessMap(entitySetI);
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
							for(String targetClass : targetClasses) {
								if(sourceClass.equals(targetClass)) {
									sourceClassIsTarget = true;
								}
								else {
									MyClass mySourceClass = classList.get(classIndexMap.get(sourceClass));
									MyClass myTargetClass = classList.get(classIndexMap.get(targetClass));
									MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system,mySourceClass,myTargetClass,method);
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
											int sourceClassDependencies = candidate.getDistinctSourceDependencies();
					    					int targetClassDependencies = candidate.getDistinctTargetDependencies();
					    					if(sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate &&
					    							sourceClassDependencies < targetClassDependencies) {
					    						candidateRefactoringList.add(candidate);
					    					}
											candidateFound = true;
										}
									}
								}
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

	private Map<String, ArrayList<String>> computeAccessMap(Set<String> entitySetI) {
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
		for(String key1 : accessMap.keySet()) {
			ClassObject classObject = ASTReader.getSystemObject().getClassObject(key1);
			if(classObject != null && classObject.getSuperclass() != null) {
				for(String key2 : accessMap.keySet()) {
					if(classObject.getSuperclass().getClassType().equals(key2)) {
						ArrayList<String> list = accessMap.get(key1);
						list.addAll(accessMap.get(key2));
					}
				}
			}
		}
		return accessMap;
	}

    public List<ExtractClassCandidateRefactoring> getExtractClassCandidateRefactorings(Set<String> classNamesToBeExamined, IProgressMonitor monitor) {
    	List<ExtractClassCandidateRefactoring> candidateList = new ArrayList<ExtractClassCandidateRefactoring>();
    	Iterator<MyClass> classIt = system.getClassIterator();
    	ArrayList<MyClass> oldClasses = new ArrayList<MyClass>();

    	while(classIt.hasNext()) {
    		MyClass myClass = classIt.next();
    		if(classNamesToBeExamined.contains(myClass.getName())) {
    			oldClasses.add(myClass);
    		}
    	}
    	if(monitor != null)
    		monitor.beginTask("Identification of Extract Class refactoring opportunities", oldClasses.size());

    	for(MyClass sourceClass : oldClasses) {
    		if(monitor != null && monitor.isCanceled())
    			throw new OperationCanceledException();
    		if (!sourceClass.getMethodList().isEmpty() && !sourceClass.getAttributeList().isEmpty()) {
    			double[][] distanceMatrix = getJaccardDistanceMatrix(sourceClass);
				Clustering clustering = Clustering.getInstance(0, distanceMatrix);
				ArrayList<Entity> entities = new ArrayList<Entity>();
				entities.addAll(sourceClass.getAttributeList());
				entities.addAll(sourceClass.getMethodList());
				HashSet<Cluster> clusters = clustering.clustering(entities);
				for (Cluster cluster : clusters) {
    				ExtractClassCandidateRefactoring candidate = new ExtractClassCandidateRefactoring(system, sourceClass, cluster.getEntities());
    				if (candidate.isApplicable()) {
    					int sourceClassDependencies = candidate.getDistinctSourceDependencies();
    					int extractedClassDependencies = candidate.getDistinctTargetDependencies();
    					if(sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate &&
    							sourceClassDependencies < extractedClassDependencies) {
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

	public double[][] getJaccardDistanceMatrix(MyClass sourceClass) {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		entities.addAll(sourceClass.getAttributeList());
		entities.addAll(sourceClass.getMethodList());
		double[][] jaccardDistanceMatrix = new double[entities.size()][entities.size()];
		for(int i=0; i<jaccardDistanceMatrix.length; i++) {
			for(int j=0; j<jaccardDistanceMatrix.length; j++) {
				if(i != j) {
					jaccardDistanceMatrix[i][j] = DistanceCalculator.getDistance(entities.get(i).getFullEntitySet(), entities.get(j).getFullEntitySet());
				}
				else {
					jaccardDistanceMatrix[i][j] = 0.0;
				}
			}
		}
		return jaccardDistanceMatrix;
	}

}
