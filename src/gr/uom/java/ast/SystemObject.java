package gr.uom.java.ast;

import gr.uom.java.ast.inheritance.CompleteInheritanceDetection;
import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckEliminationGroup;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
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
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
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
    
    public void addClasses(List<ClassObject> classObjects) {
    	for(ClassObject classObject : classObjects)
    		addClass(classObject);
    }
    
    public void replaceClass(ClassObject c) {
    	int position = getPositionInClassList(c.getName());
    	if(position != -1) {
    		classList.set(position, c);
    	}
    	else {
    		addClass(c);
    	}
    }
    
    public void removeClasses(IFile file) {
    	List<ClassObject> classesToBeRemoved = new ArrayList<ClassObject>();
    	for(ClassObject classObject : classList) {
    		if(classObject.getIFile().equals(file))
    			classesToBeRemoved.add(classObject);
    	}
    	for(ClassObject classObject : classesToBeRemoved) {
    		removeClass(classObject);
    	}
    }
    
    public void removeClass(ClassObject c) {
    	int position = getPositionInClassList(c.getName());
    	if(position != -1) {
    		for(int i=position+1; i<classList.size(); i++) {
    			ClassObject classObject = classList.get(i);
    			classNameMap.put(classObject.getName(), classNameMap.get(classObject.getName())-1);
    		}
    		classNameMap.remove(c.getName());
    		classList.remove(c);
    	}
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

    public MethodObject getMethod(SuperMethodInvocationObject smio) {
    	ClassObject classObject = getClassObject(smio.getOriginClassName());
    	if(classObject != null)
    		return classObject.getMethod(smio);
    	return null;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation, ClassObject excludedClass) {
    	for(ClassObject classObject : classList) {
    		if(!excludedClass.equals(classObject) && classObject.containsMethodInvocation(methodInvocation))
    			return true;
    	}
    	return false;
    }

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction, ClassObject excludedClass) {
    	for(ClassObject classObject : classList) {
    		if(!excludedClass.equals(classObject) && classObject.containsFieldInstruction(fieldInstruction))
    			return true;
    	}
    	return false;
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
    	for(ClassObject classObject : classList) {
    		if(classObject.containsSuperMethodInvocation(superMethodInvocation))
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

    public Set<ClassObject> getClassObjects() {
    	Set<ClassObject> classObjectSet = new LinkedHashSet<ClassObject>();
    	classObjectSet.addAll(classList);
    	return classObjectSet;
    }

    public Set<ClassObject> getClassObjects(IPackageFragmentRoot packageFragmentRoot) {
    	Set<ClassObject> classObjectSet = new LinkedHashSet<ClassObject>();
    	try {
    		IJavaElement[] children = packageFragmentRoot.getChildren();
    		for(IJavaElement child : children) {
    			if(child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
    				IPackageFragment packageFragment = (IPackageFragment)child;
    				classObjectSet.addAll(getClassObjects(packageFragment));
    			}
    		}
    	} catch(JavaModelException e) {
			e.printStackTrace();
		}
    	return classObjectSet;
    }

    public Set<ClassObject> getClassObjects(IPackageFragment packageFragment) {
    	Set<ClassObject> classObjectSet = new LinkedHashSet<ClassObject>();
    	try {
    		ICompilationUnit[] packageCompilationUnits = packageFragment.getCompilationUnits();
			for(ICompilationUnit iCompilationUnit : packageCompilationUnits) {
				classObjectSet.addAll(getClassObjects(iCompilationUnit));
			}
    	} catch(JavaModelException e) {
			e.printStackTrace();
		}
    	return classObjectSet;
    }

    public Set<ClassObject> getClassObjects(ICompilationUnit compilationUnit) {
    	Set<ClassObject> classObjectSet = new LinkedHashSet<ClassObject>();
		try {
			IType[] topLevelTypes = compilationUnit.getTypes();
			for(IType type : topLevelTypes) {
				classObjectSet.addAll(getClassObjects(type));
			}
		} catch(JavaModelException e) {
			e.printStackTrace();
		}
    	return classObjectSet;
    }

    public Set<ClassObject> getClassObjects(IType type) {
    	Set<ClassObject> classObjectSet = new LinkedHashSet<ClassObject>();
    	String typeQualifiedName = type.getFullyQualifiedName('.');
    	ClassObject classObject = getClassObject(typeQualifiedName);
    	if(classObject != null)
    		classObjectSet.add(classObject);
    	try {
			IType[] nestedTypes = type.getTypes();
			for(IType nestedType : nestedTypes) {
				classObjectSet.addAll(getClassObjects(nestedType));
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
    	return classObjectSet;
    }

    public AnonymousClassDeclarationObject getAnonymousClassDeclaration(IType declaringType) {
    	try {
			if(declaringType.isAnonymous()) {
				int occurrenceCount = declaringType.getOccurrenceCount();
				IJavaElement declaringTypeParent = declaringType.getParent();
				IMethod methodContainingAnonymousClass = null;
				if(declaringTypeParent instanceof IMethod) {
					methodContainingAnonymousClass = (IMethod)declaringTypeParent;
				}
				if(methodContainingAnonymousClass != null) {
					AbstractMethodDeclaration md = getMethodObject(methodContainingAnonymousClass);
					List<AnonymousClassDeclarationObject> anonymousClassDeclarations = md.getAnonymousClassDeclarations();
					if(occurrenceCount - 1 < anonymousClassDeclarations.size()) {
						return anonymousClassDeclarations.get(occurrenceCount - 1);
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
    	return null;
    }

    public AbstractMethodDeclaration getMethodObject(IMethod method) {
    	try {
    		IType declaringType = method.getDeclaringType();
    		String fullyQualifiedName = declaringType.getFullyQualifiedName('.');
    		ClassObject classObject = getClassObject(fullyQualifiedName);
    		if(classObject != null) {
    			ListIterator<MethodObject> mi = classObject.getMethodIterator();
    			while(mi.hasNext()) {
    				MethodObject mo = mi.next();
    				IMethod resolvedMethod = (IMethod)mo.getMethodDeclaration().resolveBinding().getJavaElement();
    				if(method.isSimilar(resolvedMethod) && method.getSourceRange().equals(resolvedMethod.getSourceRange()))
    					return mo;
    			}
    			ListIterator<ConstructorObject> ci = classObject.getConstructorIterator();
    			while(ci.hasNext()) {
    				ConstructorObject co = ci.next();
    				IMethod resolvedMethod = (IMethod)co.getMethodDeclaration().resolveBinding().getJavaElement();
    				if(method.isSimilar(resolvedMethod) && method.getSourceRange().equals(resolvedMethod.getSourceRange()))
    					return co;
    			}
    		}
    		//check if declaringType is an anonymous class declaration
    		if(declaringType.isAnonymous()) {
    			IJavaElement declaringTypeParent = declaringType.getParent();
    			IMethod methodContainingAnonymousClass = null;
    			if(declaringTypeParent instanceof IMethod) {
    				methodContainingAnonymousClass = (IMethod)declaringTypeParent;
    			}
    			if(methodContainingAnonymousClass != null) {
    				AbstractMethodDeclaration md = getMethodObject(methodContainingAnonymousClass);
    				List<AnonymousClassDeclarationObject> anonymousClassDeclarations = md.getAnonymousClassDeclarations();
    				for(AnonymousClassDeclarationObject anonymous : anonymousClassDeclarations) {
    					ListIterator<MethodObject> mi2 = anonymous.getMethodIterator();
    					while(mi2.hasNext()) {
    						MethodObject mo2 = mi2.next();
    						IMethod resolvedMethod = (IMethod)mo2.getMethodDeclaration().resolveBinding().getJavaElement();
    						if(method.isSimilar(resolvedMethod) && method.getSourceRange().equals(resolvedMethod.getSourceRange()))
    							return mo2;
    					}
    				}
    			}
    		}
    	} catch (JavaModelException e) {
    		e.printStackTrace();
    	}
    	return null;
    }

    public List<String> getClassNames() {
        List<String> names = new ArrayList<String>();
        for(int i=0; i<classList.size(); i++) {
            names.add(getClassObject(i).getName());
        }
        return names;
    }

    public List<TypeCheckEliminationGroup> generateTypeCheckEliminations(Set<ClassObject> classObjectsToBeExamined, IProgressMonitor monitor) {
    	if(monitor != null)
    		monitor.beginTask("Identification of Type Checking code smells", classObjectsToBeExamined.size());
    	List<TypeCheckElimination> typeCheckEliminationResults = new ArrayList<TypeCheckElimination>();
    	List<TypeCheckEliminationGroup> typeCheckEliminationGroups = new ArrayList<TypeCheckEliminationGroup>();
    	Map<TypeCheckElimination, List<SimpleName>> staticFieldMap = new LinkedHashMap<TypeCheckElimination, List<SimpleName>>();
    	Map<Integer, ArrayList<TypeCheckElimination>> staticFieldRankMap = new TreeMap<Integer, ArrayList<TypeCheckElimination>>();
    	Map<String, ArrayList<TypeCheckElimination>> inheritanceTreeMap = new LinkedHashMap<String, ArrayList<TypeCheckElimination>>();
    	CompleteInheritanceDetection inheritanceDetection = new CompleteInheritanceDetection(this);
    	for(ClassObject classObject : classObjectsToBeExamined) {
    		if(monitor != null && monitor.isCanceled())
    			throw new OperationCanceledException();
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
	    					if(validTypeBinding(typeFieldTypeBinding)) {
	    						isValid = true;
	    					}
	    				}
	    				else if(elimination.getTypeLocalVariable() != null) {
	    					IVariableBinding typeLocalVariableBinding = elimination.getTypeLocalVariable().resolveBinding();
	    					ITypeBinding typeLocalVariableTypeBinding = typeLocalVariableBinding.getType();
	    					if(validTypeBinding(typeLocalVariableTypeBinding)) {
	    						isValid = true;
	    					}
	    				}
	    				else if(elimination.getTypeMethodInvocation() != null) {
	    					MethodInvocation typeMethodInvocation = elimination.getTypeMethodInvocation();
	    					IMethodBinding typeMethodInvocationBinding = typeMethodInvocation.resolveMethodBinding();
	    					ITypeBinding typeMethodInvocationDeclaringClass = typeMethodInvocationBinding.getDeclaringClass();
	    					ITypeBinding typeMethodInvocationReturnType = typeMethodInvocationBinding.getReturnType();
	    					ClassObject declaringClassObject = getClassObject(typeMethodInvocationDeclaringClass.getQualifiedName());
	    					if(validTypeBinding(typeMethodInvocationReturnType) && declaringClassObject != null) {
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
	    							ITypeBinding invokerTypeBinding = handleTypeMethodInvocation(typeMethodInvocation, elimination);
	    							if(invokerTypeBinding != null) {
	    								if(tree != null) {
	    									if(invokerTypeBinding.getQualifiedName().equals(tree.getRootNode().getUserObject())) {
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
	    								else {
	    									InheritanceTree tree2 = inheritanceDetection.getTree(invokerTypeBinding.getQualifiedName());
	    									if(tree2 != null) {
	    										elimination.setExistingInheritanceTree(tree2);
	    										if(inheritanceTreeMap.containsKey(tree2.getRootNode().getUserObject())) {
	    											ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(tree2.getRootNode().getUserObject());
	    											typeCheckEliminations.add(elimination);
	    										}
	    										else {
	    											ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
	    											typeCheckEliminations.add(elimination);
	    											inheritanceTreeMap.put((String)tree2.getRootNode().getUserObject(), typeCheckEliminations);
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
    						ITypeBinding invokerTypeBinding = handleTypeMethodInvocation(typeMethodInvocation, elimination);
    						if(invokerTypeBinding != null) {
    							InheritanceTree tree = inheritanceDetection.getTree(invokerTypeBinding.getQualifiedName());
    							elimination.setExistingInheritanceTree(tree);
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
    		if(monitor != null)
    			monitor.worked(1);
    	}
    	for(String rootNode : inheritanceTreeMap.keySet()) {
    		ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(rootNode);
    		if(typeCheckEliminations.size() > 0) {
    			typeCheckEliminationResults.addAll(typeCheckEliminations);
    			typeCheckEliminationGroups.add(handleGroup(typeCheckEliminations));
    		}
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
    		if(typeCheckEliminations.size() > 0) {
    			typeCheckEliminationResults.addAll(typeCheckEliminations);
    			typeCheckEliminationGroups.add(handleGroup(typeCheckEliminations));
    		}
    		sortedEliminations.removeAll(affectedEliminations);
    	}
    	identifySuperFieldAccessorMethods(typeCheckEliminationResults);
    	if(monitor != null)
    		monitor.done();
    	return typeCheckEliminationGroups;
    }

	private boolean validTypeBinding(ITypeBinding typeBinding) {
		return typeBinding.isPrimitive() || typeBinding.isEnum() || typeBinding.getQualifiedName().equals("java.lang.String");
	}

    private ITypeBinding handleTypeMethodInvocation(MethodInvocation typeMethodInvocation, TypeCheckElimination elimination) {
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
    		if(binding != null && binding.getKind() == IBinding.VARIABLE) {
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
    				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
    				Block typeCheckMethodBody = elimination.getTypeCheckMethod().getBody();
					List<VariableDeclarationFragment> variableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
					List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(typeCheckMethodBody);
					for(Statement statement : variableDeclarationStatements) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
						List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
						variableDeclarationFragments.addAll(fragments);
					}
					List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(typeCheckMethodBody);
					for(Expression expression : variableDeclarationExpressions) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
						List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
						variableDeclarationFragments.addAll(fragments);
					}
					for(VariableDeclarationFragment fragment : variableDeclarationFragments) {
						IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
						if(fragmentVariableBinding.isEqualTo(variableBinding)) {
							elimination.setTypeLocalVariable(fragment);
							break;
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
    			ITypeBinding invokerTypeBinding = variableBinding.getType();
    			return invokerTypeBinding;
    		}
    	}
    	return null;
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
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		String abstractClassType = typeCheckElimination.getAbstractClassType();
		InheritanceTree tree = null;
		if(abstractClassType != null)
			tree = inheritanceDetection.getTree(abstractClassType);
		if(tree != null) {
			DefaultMutableTreeNode rootNode = tree.getRootNode();
			DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
			List<String> inheritanceHierarchySubclassNames = new ArrayList<String>();
			while(leaf != null) {
				inheritanceHierarchySubclassNames.add((String)leaf.getUserObject());
				leaf = leaf.getNextLeaf();
			}
			int matchCounter = 0;
			for(SimpleName staticField : staticFields) {
				for(String subclassName : inheritanceHierarchySubclassNames) {
					ClassObject classObject = getClassObject(subclassName);
					AbstractTypeDeclaration abstractTypeDeclaration = classObject.getAbstractTypeDeclaration();
					if(abstractTypeDeclaration instanceof TypeDeclaration) {
						TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
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
											if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
												IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
												staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass();
											}
											if(staticFieldNameBinding.getName().equals(memberRef.getName().getIdentifier()) &&
													staticFieldNameDeclaringClass.getQualifiedName().equals(memberRef.getQualifier().getFullyQualifiedName())) {
												typeCheckElimination.putStaticFieldSubclassTypeMapping(staticField, subclassName);
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
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				ITypeBinding declaringClassTypeBinding = variableBinding.getDeclaringClass();
				if(declaringClassTypeBinding != null) {
					if(getPositionInClassList(declaringClassTypeBinding.getQualifiedName()) == -1 && !declaringClassTypeBinding.isEnum())
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
				if(declaringClass != null) {
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
			}
			Set<IVariableBinding> superAssignedFields = elimination.getSuperAssignedFieldBindings();
			for(IVariableBinding superAssignedField : superAssignedFields) {
				ITypeBinding declaringClassTypeBinding = superAssignedField.getDeclaringClass();
				ClassObject declaringClass = getClassObject(declaringClassTypeBinding.getQualifiedName());
				if(declaringClass != null) {
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
	}

	private TypeCheckEliminationGroup handleGroup(List<TypeCheckElimination> typeCheckEliminations) {
		TypeCheckEliminationGroup typeCheckEliminationGroup = new TypeCheckEliminationGroup();
		Map<String, ArrayList<TypeCheckElimination>> typeDeclarationMap = new HashMap<String, ArrayList<TypeCheckElimination>>();
		double averageNumberOfStatementsInGroupSum = 0;
		for(TypeCheckElimination elimination : typeCheckEliminations) {
			TypeDeclaration typeCheckClass = elimination.getTypeCheckClass();
			String bindingKey = typeCheckClass.resolveBinding().getKey();
			if(typeDeclarationMap.containsKey(bindingKey)) {
				ArrayList<TypeCheckElimination> tempTypeCheckEliminations = typeDeclarationMap.get(bindingKey);
				tempTypeCheckEliminations.add(elimination);
			}
			else {
				ArrayList<TypeCheckElimination> tempTypeCheckEliminations = new ArrayList<TypeCheckElimination>();
				tempTypeCheckEliminations.add(elimination);
				typeDeclarationMap.put(bindingKey, tempTypeCheckEliminations);
			}
			double avgNumberOfStatements = elimination.getAverageNumberOfStatements();
			averageNumberOfStatementsInGroupSum += avgNumberOfStatements;
			typeCheckEliminationGroup.addCandidate(elimination);
		}
		double averageGroupSizeAtClassLevelSum = 0;
		for(String bindingKey : typeDeclarationMap.keySet()) {
			ArrayList<TypeCheckElimination> tempTypeCheckEliminations = typeDeclarationMap.get(bindingKey);
			averageGroupSizeAtClassLevelSum += tempTypeCheckEliminations.size();
			for(TypeCheckElimination elimination : tempTypeCheckEliminations) {
				elimination.setGroupSizeAtClassLevel(tempTypeCheckEliminations.size());
			}
		}
		typeCheckEliminationGroup.setGroupSizeAtSystemLevel(typeCheckEliminations.size());
		typeCheckEliminationGroup.setAverageGroupSizeAtClassLevel(averageGroupSizeAtClassLevelSum/typeDeclarationMap.keySet().size());
		typeCheckEliminationGroup.setAverageNumberOfStatementsInGroup(averageNumberOfStatementsInGroupSum/typeCheckEliminations.size());
		return typeCheckEliminationGroup;
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