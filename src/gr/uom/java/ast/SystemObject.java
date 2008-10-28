package gr.uom.java.ast;

import gr.uom.java.ast.inheritance.CompleteInheritanceDetection;
import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckEliminationResults;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

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

    public TypeCheckEliminationResults generateTypeCheckEliminations() {
    	TypeCheckEliminationResults typeCheckEliminationResults = new TypeCheckEliminationResults();
    	Map<TypeCheckElimination, List<SimpleName>> staticFieldMap = new LinkedHashMap<TypeCheckElimination, List<SimpleName>>();
    	Map<Integer, ArrayList<TypeCheckElimination>> staticFieldRankMap = new TreeMap<Integer, ArrayList<TypeCheckElimination>>();
    	Map<String, ArrayList<TypeCheckElimination>> inheritanceTreeMap = new LinkedHashMap<String, ArrayList<TypeCheckElimination>>();
    	CompleteInheritanceDetection inheritanceDetection = new CompleteInheritanceDetection(this);
    	for(ClassObject classObject : classList) {
    		List<TypeCheckElimination> eliminations = classObject.generateTypeCheckEliminations();
    		for(TypeCheckElimination elimination : eliminations) {
    			List<SimpleName> staticFields = elimination.getStaticFields();
    			if(!staticFields.isEmpty()) {
    				if(allStaticFieldsWithinSystemBoundary(staticFields)) {
	    				inheritanceHierarchyMatchingWithStaticTypes(elimination, inheritanceDetection);
	    				boolean isValid = false;
	    				if(elimination.getTypeField() != null) {
	    					IVariableBinding typeFieldBinding = elimination.getTypeField().resolveBinding();
	    					ITypeBinding typeFieldTypeBinding = typeFieldBinding.getType();
	    					if((typeFieldTypeBinding.isPrimitive() && typeFieldTypeBinding.getQualifiedName().equals("int")) ||
	    							typeFieldTypeBinding.isEnum()) {
	    						isValid = true;
	    					}
	    				}
	    				else if(elimination.getTypeLocalVariable() != null) {
	    					IVariableBinding typeLocalVariableBinding = elimination.getTypeLocalVariable().resolveBinding();
	    					ITypeBinding typeLocalVariableTypeBinding = typeLocalVariableBinding.getType();
	    					if((typeLocalVariableTypeBinding.isPrimitive() && typeLocalVariableTypeBinding.getQualifiedName().equals("int")) ||
	    							typeLocalVariableTypeBinding.isEnum()) {
	    						isValid = true;
	    					}
	    				}
	    				else if(elimination.getTypeMethodInvocation() != null) {
	    					MethodInvocation typeMethodInvocation = elimination.getTypeMethodInvocation();
	    					IMethodBinding typeMethodInvocationBinding = typeMethodInvocation.resolveMethodBinding();
	    					ITypeBinding typeMethodInvocationDeclaringClass = typeMethodInvocationBinding.getDeclaringClass();
	    					ITypeBinding typeMethodInvocationReturnType = typeMethodInvocationBinding.getReturnType();
	    					ClassObject declaringClassObject = getClassObject(typeMethodInvocationDeclaringClass.getQualifiedName());
	    					if( ((typeMethodInvocationReturnType.isPrimitive() && typeMethodInvocationReturnType.getQualifiedName().equals("int")) ||
	    							typeMethodInvocationReturnType.isEnum()) && declaringClassObject != null ) {
	    						MethodDeclaration invokedMethodDeclaration = null;
	    						ListIterator<MethodObject> methodIterator = declaringClassObject.getMethodIterator();
	    						while(methodIterator.hasNext()) {
	    							MethodObject methodObject = methodIterator.next();
	    							MethodDeclaration methodDeclaration = methodObject.getMethodDeclaration();
	    							if(typeMethodInvocationBinding.isEqualTo(methodDeclaration.resolveBinding())) {
	    								invokedMethodDeclaration = methodDeclaration;
	    								break;
	    							}
	    						}
	    						SimpleName fieldInstruction = MethodDeclarationUtility.isGetter(invokedMethodDeclaration);
	    						if(fieldInstruction != null) {
	    							ListIterator<FieldObject> fieldIterator = declaringClassObject.getFieldIterator();
	    							while(fieldIterator.hasNext()) {
	    								FieldObject fieldObject = fieldIterator.next();
	    								VariableDeclarationFragment fragment = fieldObject.getVariableDeclarationFragment();
	    								if(fieldInstruction.resolveBinding().isEqualTo(fragment.resolveBinding())) {
	    									elimination.setForeignTypeField(fragment);
	    									break;
	    								}
	    							}
	    							isValid = true;
	    						}
	    						else if(invokedMethodDeclaration.getBody() == null) {
	    							InheritanceTree tree = elimination.getInheritanceTreeMatchingWithStaticTypes();
	    							if(tree != null) {
	    								Expression typeMethodInvocationExpression = typeMethodInvocation.getExpression();
	    								ITypeBinding typeCheckClassBinding = elimination.getTypeCheckClass().resolveBinding();
	    								ClassObject typeCheckClassObject = getClassObject(typeCheckClassBinding.getQualifiedName());
	    								SimpleName invoker = null;
	    								if(typeMethodInvocationExpression instanceof SimpleName) {
	    									invoker = (SimpleName)typeMethodInvocationExpression;
	    								}
	    								else if(typeMethodInvocationExpression instanceof FieldAccess) {
	    									FieldAccess fieldAccess = (FieldAccess)typeMethodInvocationExpression;
	    									invoker = fieldAccess.getName();
	    								}
	    								if(invoker != null) {
	    		    						IBinding binding = invoker.resolveBinding();
	    		    						if(binding.getKind() == IBinding.VARIABLE) {
	    		    							IVariableBinding variableBinding = (IVariableBinding)binding;
	    		    							if(variableBinding.isField()) {
	    		    								ListIterator<FieldObject> fieldIterator = typeCheckClassObject.getFieldIterator();
	    		    								while(fieldIterator.hasNext()) {
	    		    									FieldObject fieldObject = fieldIterator.next();
	    		    									VariableDeclarationFragment fragment = fieldObject.getVariableDeclarationFragment();
	    		    									if(variableBinding.isEqualTo(fragment.resolveBinding())) {
	    		    										elimination.setTypeField(fragment);
	    		    										break;
	    		    									}
	    		    								}
	    		    							}
	    		    							else if(variableBinding.isParameter()) {
	    		    								List<SingleVariableDeclaration> parameters = elimination.getTypeCheckMethodParameters();
	    		    								for(SingleVariableDeclaration parameter : parameters) {
	    		    									IVariableBinding parameterVariableBinding = parameter.resolveBinding();
	    		    									if(parameterVariableBinding.isEqualTo(variableBinding)) {
	    		    										elimination.setTypeLocalVariable(parameter);
	    		    										break;
	    		    									}
	    		    								}
	    		    							}
	    		    							else {
	    		    								StatementExtractor statementExtractor = new StatementExtractor();
	    		    								Block typeCheckMethodBody = elimination.getTypeCheckMethod().getBody();
	    		    								List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarations(typeCheckMethodBody);
	    		    								for(Statement vDStatement : variableDeclarationStatements) {
	    		    									VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)vDStatement;
	    		    									List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
	    		    									for(VariableDeclarationFragment fragment : fragments) {
	    		    										IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
	    		    										if(fragmentVariableBinding.isEqualTo(variableBinding)) {
	    		    											elimination.setTypeLocalVariable(fragment);
	    		    											break;
	    		    										}
	    		    									}
	    		    								}
	    		    								List<Statement> enhancedForStatements = statementExtractor.getEnhancedForStatements(typeCheckMethodBody);
	    		    								for(Statement eFStatement : enhancedForStatements) {
	    		    									EnhancedForStatement enhancedForStatement = (EnhancedForStatement)eFStatement;
	    		    									SingleVariableDeclaration formalParameter = enhancedForStatement.getParameter();
	    		    									IVariableBinding parameterVariableBinding = formalParameter.resolveBinding();
	    		    									if(parameterVariableBinding.isEqualTo(variableBinding)) {
	    		    										elimination.setTypeLocalVariable(formalParameter);
	    		    										break;
	    		    									}
	    		    								}
	    		    							}
	    		    							ITypeBinding invokerType = variableBinding.getType();
	    		    							if(invokerType.getQualifiedName().equals(tree.getRootNode().getUserObject())) {
	    		    								elimination.setExistingInheritanceTree(tree);
	    		    								if(inheritanceTreeMap.containsKey(tree.getRootNode().getUserObject())) {
	    		    									ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(tree.getRootNode().getUserObject());
	    		    									typeCheckEliminations.add(elimination);
	    		    								}
	    		    								else {
	    		    									ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
	    		    									typeCheckEliminations.add(elimination);
	    		    									inheritanceTreeMap.put((String)tree.getRootNode().getUserObject(), typeCheckEliminations);
	    		    								}
	    		    							}
	    		    						}
	    		    					}
	    							}
	    						}
	    					}
	    				}
	    				if(isValid) {
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
    			}
    			else {
    				if(elimination.getTypeField() != null) {
    					IVariableBinding typeFieldBinding = elimination.getTypeField().resolveBinding();
    					ITypeBinding typeFieldTypeBinding = typeFieldBinding.getType();
    					InheritanceTree tree = inheritanceDetection.getTree(typeFieldTypeBinding.getQualifiedName());
    					elimination.setExistingInheritanceTree(tree);
    				}
    				else if(elimination.getTypeLocalVariable() != null) {
    					IVariableBinding typeLocalVariableBinding = elimination.getTypeLocalVariable().resolveBinding();
    					ITypeBinding typeLocalVariableTypeBinding = typeLocalVariableBinding.getType();
    					InheritanceTree tree = inheritanceDetection.getTree(typeLocalVariableTypeBinding.getQualifiedName());
    					elimination.setExistingInheritanceTree(tree);
    				}
    				else if(elimination.getTypeMethodInvocation() != null) {
    					MethodInvocation typeMethodInvocation = elimination.getTypeMethodInvocation();
    					IMethodBinding typeMethodInvocationBinding = typeMethodInvocation.resolveMethodBinding();
    					if(typeMethodInvocationBinding.getDeclaringClass().getQualifiedName().equals("java.lang.Object") &&
    							typeMethodInvocationBinding.getName().equals("getClass")) {
    						Expression typeMethodInvocationExpression = typeMethodInvocation.getExpression();
    						ITypeBinding typeCheckClassBinding = elimination.getTypeCheckClass().resolveBinding();
    						ClassObject typeCheckClassObject = getClassObject(typeCheckClassBinding.getQualifiedName());
    						SimpleName invoker = null;
    						if(typeMethodInvocationExpression instanceof SimpleName) {
    							invoker = (SimpleName)typeMethodInvocationExpression;
    						}
    						else if(typeMethodInvocationExpression instanceof FieldAccess) {
    							FieldAccess fieldAccess = (FieldAccess)typeMethodInvocationExpression;
    							invoker = fieldAccess.getName();
    						}
    						if(invoker != null) {
    							IBinding binding = invoker.resolveBinding();
	    						if(binding.getKind() == IBinding.VARIABLE) {
	    							IVariableBinding variableBinding = (IVariableBinding)binding;
	    							if(variableBinding.isField()) {
	    								ListIterator<FieldObject> fieldIterator = typeCheckClassObject.getFieldIterator();
	    								while(fieldIterator.hasNext()) {
	    									FieldObject fieldObject = fieldIterator.next();
	    									VariableDeclarationFragment fragment = fieldObject.getVariableDeclarationFragment();
	    									if(variableBinding.isEqualTo(fragment.resolveBinding())) {
	    										elimination.setTypeField(fragment);
	    										break;
	    									}
	    								}
	    							}
	    							else if(variableBinding.isParameter()) {
	    								List<SingleVariableDeclaration> parameters = elimination.getTypeCheckMethodParameters();
	    								for(SingleVariableDeclaration parameter : parameters) {
	    									IVariableBinding parameterVariableBinding = parameter.resolveBinding();
	    									if(parameterVariableBinding.isEqualTo(variableBinding)) {
	    										elimination.setTypeLocalVariable(parameter);
	    										break;
	    									}
	    								}
	    							}
	    							else {
	    								StatementExtractor statementExtractor = new StatementExtractor();
	    								Block typeCheckMethodBody = elimination.getTypeCheckMethod().getBody();
	    								List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarations(typeCheckMethodBody);
	    								for(Statement vDStatement : variableDeclarationStatements) {
	    									VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)vDStatement;
	    									List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
	    									for(VariableDeclarationFragment fragment : fragments) {
	    										IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
	    										if(fragmentVariableBinding.isEqualTo(variableBinding)) {
	    											elimination.setTypeLocalVariable(fragment);
	    											break;
	    										}
	    									}
	    								}
	    								List<Statement> enhancedForStatements = statementExtractor.getEnhancedForStatements(typeCheckMethodBody);
	    								for(Statement eFStatement : enhancedForStatements) {
	    									EnhancedForStatement enhancedForStatement = (EnhancedForStatement)eFStatement;
	    									SingleVariableDeclaration formalParameter = enhancedForStatement.getParameter();
	    									IVariableBinding parameterVariableBinding = formalParameter.resolveBinding();
	    									if(parameterVariableBinding.isEqualTo(variableBinding)) {
	    										elimination.setTypeLocalVariable(formalParameter);
	    										break;
	    									}
	    								}
	    							}
	    							ITypeBinding invokerType = variableBinding.getType();
	    							InheritanceTree tree = inheritanceDetection.getTree(invokerType.getQualifiedName());
	    							elimination.setExistingInheritanceTree(tree);
	    						}
    						}
    					}
    					else {
	    					ITypeBinding typeMethodInvocationReturnType = typeMethodInvocationBinding.getReturnType();
	    					InheritanceTree tree = inheritanceDetection.getTree(typeMethodInvocationReturnType.getQualifiedName());
	    					elimination.setExistingInheritanceTree(tree);
    					}
    				}
    				if(elimination.getExistingInheritanceTree() != null) {
    					InheritanceTree tree = elimination.getExistingInheritanceTree();
    					if(inheritanceTreeMap.containsKey(tree.getRootNode().getUserObject())) {
							ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(tree.getRootNode().getUserObject());
							typeCheckEliminations.add(elimination);
						}
						else {
							ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
							typeCheckEliminations.add(elimination);
							inheritanceTreeMap.put((String)tree.getRootNode().getUserObject(), typeCheckEliminations);
						}
    				}
    			}
    		}
    	}
    	for(String rootNode : inheritanceTreeMap.keySet()) {
    		ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(rootNode);
    		typeCheckEliminationResults.addGroup(typeCheckEliminations);
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
    		boolean staticFieldUnionIncreased = true;
    		while(staticFieldUnionIncreased) {
    			staticFieldUnionIncreased = false;
	    		for(TypeCheckElimination elimination : sortedEliminations) {
	    			List<SimpleName> staticFields = staticFieldMap.get(elimination);
	    			if(!affectedEliminations.contains(elimination) && nonEmptyIntersection(staticFieldUnion, staticFields)) {
	    				staticFieldUnion = constructUnion(staticFieldUnion, staticFields);
	    				affectedEliminations.add(elimination);
	    				staticFieldUnionIncreased = true;
	    			}
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
    		ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
    		for(TypeCheckElimination elimination : affectedEliminations) {
    			if(!elimination.isTypeCheckMethodStateSetter())
    				typeCheckEliminations.add(elimination);
    		}
    		typeCheckEliminationResults.addGroup(typeCheckEliminations);
    		sortedEliminations.removeAll(affectedEliminations);
    	}
    	identifySuperFieldAccessorMethods(typeCheckEliminationResults.getTypeCheckEliminations());
    	return typeCheckEliminationResults;
    }

    private boolean nonEmptyIntersection(List<SimpleName> staticFieldUnion, List<SimpleName> staticFields) {
    	for(SimpleName simpleName1 : staticFields) {
    		for(SimpleName simpleName2 : staticFieldUnion) {
    			if(simpleName1.resolveBinding().isEqualTo(simpleName2.resolveBinding()))
    				return true;
    		}
    	}
    	return false;
    }

    private List<SimpleName> constructUnion(List<SimpleName> staticFieldUnion, List<SimpleName> staticFields) {
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

	private void inheritanceHierarchyMatchingWithStaticTypes(TypeCheckElimination typeCheckElimination,
			CompleteInheritanceDetection inheritanceDetection) {
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		Set<InheritanceTree> inheritanceTrees = new LinkedHashSet<InheritanceTree>();
		for(String subclassName: subclassNames) {
			Set<InheritanceTree> tempInheritanceTrees = inheritanceDetection.getMatchingTrees(subclassName);
			inheritanceTrees.addAll(tempInheritanceTrees);
		}
		for(InheritanceTree tree : inheritanceTrees) {
			DefaultMutableTreeNode root = tree.getRootNode();
			DefaultMutableTreeNode leaf = root.getFirstLeaf();
			List<String> inheritanceHierarchySubclassNames = new ArrayList<String>();
			while(leaf != null) {
				inheritanceHierarchySubclassNames.add((String)leaf.getUserObject());
				leaf = leaf.getNextLeaf();
			}
			int matchCounter = 0;
			for(SimpleName staticField : staticFields) {
				for(String subclassName : inheritanceHierarchySubclassNames) {
					ClassObject classObject = getClassObject(subclassName);
					TypeDeclaration typeDeclaration = classObject.getTypeDeclaration();
					Javadoc javadoc = typeDeclaration.getJavadoc();
					if(javadoc != null) {
						List<TagElement> tagElements = javadoc.tags();
						for(TagElement tagElement : tagElements) {
							if(tagElement.getTagName() != null && tagElement.getTagName().equals(TagElement.TAG_SEE)) {
								List<ASTNode> fragments = tagElement.fragments();
								for(ASTNode fragment : fragments) {
									if(fragment instanceof MemberRef) {
										MemberRef memberRef = (MemberRef)fragment;
										IBinding staticFieldNameBinding = staticField.resolveBinding();
										ITypeBinding staticFieldNameDeclaringClass = null;
										if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
											staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass();
										}
										if(staticFieldNameBinding.getName().equals(memberRef.getName().getIdentifier()) &&
												staticFieldNameDeclaringClass.getQualifiedName().equals(memberRef.getQualifier().getFullyQualifiedName())) {
											matchCounter++;
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			if(matchCounter == staticFields.size()) {
				typeCheckElimination.setInheritanceTreeMatchingWithStaticTypes(tree);
				return;
			}
		}
	}

	private boolean allStaticFieldsWithinSystemBoundary(List<SimpleName> staticFields) {
		for(SimpleName staticField : staticFields) {
			IBinding binding = staticField.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				ITypeBinding declaringClassTypeBinding = variableBinding.getDeclaringClass();
				if(declaringClassTypeBinding != null) {
					if(getPositionInClassList(declaringClassTypeBinding.getQualifiedName()) == -1)
						return false;
				}
			}
		}
		return true;
	}

	private void identifySuperFieldAccessorMethods(List<TypeCheckElimination> typeCheckEliminations) {
		for(TypeCheckElimination elimination : typeCheckEliminations) {
			Set<IVariableBinding> superAccessedFields = elimination.getSuperAccessedFieldBindings();
			for(IVariableBinding superAccessedField : superAccessedFields) {
				ITypeBinding declaringClassTypeBinding = superAccessedField.getDeclaringClass();
				ClassObject declaringClass = getClassObject(declaringClassTypeBinding.getQualifiedName());
				ListIterator<FieldObject> fieldIterator = declaringClass.getFieldIterator();
				VariableDeclarationFragment fieldFragment = null;
				while(fieldIterator.hasNext()) {
					FieldObject fieldObject = fieldIterator.next();
					VariableDeclarationFragment fragment = fieldObject.getVariableDeclarationFragment();
					if(fragment.resolveBinding().isEqualTo(superAccessedField)) {
						fieldFragment = fragment;
						elimination.addSuperAccessedField(fragment, null);
						break;
					}
				}
				ListIterator<MethodObject> methodIterator = declaringClass.getMethodIterator();
				while(methodIterator.hasNext()) {
					MethodObject methodObject = methodIterator.next();
					MethodDeclaration methodDeclaration = methodObject.getMethodDeclaration();
					SimpleName simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
					if(simpleName != null && simpleName.resolveBinding().isEqualTo(superAccessedField)) {
						elimination.addSuperAccessedFieldBinding(superAccessedField, methodDeclaration.resolveBinding());
						elimination.addSuperAccessedField(fieldFragment, methodDeclaration);
						break;
					}
				}
			}
			Set<IVariableBinding> superAssignedFields = elimination.getSuperAssignedFieldBindings();
			for(IVariableBinding superAssignedField : superAssignedFields) {
				ITypeBinding declaringClassTypeBinding = superAssignedField.getDeclaringClass();
				ClassObject declaringClass = getClassObject(declaringClassTypeBinding.getQualifiedName());
				ListIterator<FieldObject> fieldIterator = declaringClass.getFieldIterator();
				VariableDeclarationFragment fieldFragment = null;
				while(fieldIterator.hasNext()) {
					FieldObject fieldObject = fieldIterator.next();
					VariableDeclarationFragment fragment = fieldObject.getVariableDeclarationFragment();
					if(fragment.resolveBinding().isEqualTo(superAssignedField)) {
						fieldFragment = fragment;
						elimination.addSuperAssignedField(fragment, null);
						break;
					}
				}
				ListIterator<MethodObject> methodIterator = declaringClass.getMethodIterator();
				while(methodIterator.hasNext()) {
					MethodObject methodObject = methodIterator.next();
					MethodDeclaration methodDeclaration = methodObject.getMethodDeclaration();
					SimpleName simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
					if(simpleName != null && simpleName.resolveBinding().isEqualTo(superAssignedField)) {
						elimination.addSuperAssignedFieldBinding(superAssignedField, methodDeclaration.resolveBinding());
						elimination.addSuperAssignedField(fieldFragment, methodDeclaration);
						break;
					}
				}
			}
		}
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