package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ClassObject {

	private String name;
	private List<ConstructorObject> constructorList;
	private List<MethodObject> methodList;
	private List<FieldObject> fieldList;
	private TypeObject superclass;
	private List<TypeObject> interfaceList;
	private boolean _abstract;
    private boolean _interface;
    private boolean _static;
    private Access access;
    //private TypeDeclaration typeDeclaration;
    private ASTInformation typeDeclaration;
    private IFile iFile;

    public ClassObject() {
		this.constructorList = new ArrayList<ConstructorObject>();
		this.methodList = new ArrayList<MethodObject>();
		this.interfaceList = new ArrayList<TypeObject>();
		this.fieldList = new ArrayList<FieldObject>();
		this._abstract = false;
        this._interface = false;
        this._static = false;
        this.access = Access.NONE;
    }

    /*public boolean isInnerClass() {
    	if(typeDeclaration.getParent() instanceof TypeDeclaration)
    		return true;
    	else if(typeDeclaration.getParent() instanceof CompilationUnit)
    		return false;
    	return false;
    }

    public TypeDeclaration getOuterClass() {
    	if(typeDeclaration.getParent() instanceof TypeDeclaration)
    		return (TypeDeclaration)typeDeclaration.getParent();
    	else if(typeDeclaration.getParent() instanceof CompilationUnit)
    		return null;
    	return null;
    }*/

    public void setTypeDeclaration(TypeDeclaration typeDeclaration) {
    	//this.typeDeclaration = typeDeclaration;
    	this.typeDeclaration = ASTInformationGenerator.generateASTInformation(typeDeclaration);
    }

    public TypeDeclaration getTypeDeclaration() {
    	//return this.typeDeclaration;
    	return (TypeDeclaration)this.typeDeclaration.recoverASTNode();
    }

    public ITypeRoot getITypeRoot() {
    	return typeDeclaration.getITypeRoot();
    }

    public IFile getIFile() {
		return iFile;
	}

	public void setIFile(IFile file) {
		iFile = file;
	}

	public boolean isFriend(String className) {
		if(superclass != null) {
			if(superclass.getClassType().equals(className))
				return true;
		}
		for(TypeObject interfaceType : interfaceList) {
			if(interfaceType.getClassType().equals(className))
				return true;
		}
		for(FieldObject field : fieldList) {
			TypeObject fieldType = field.getType();
			if(checkFriendship(fieldType, className))
				return true;
		}
		for(ConstructorObject constructor : constructorList) {
			ListIterator<ParameterObject> parameterIterator = constructor.getParameterListIterator();
			while(parameterIterator.hasNext()) {
				ParameterObject parameter = parameterIterator.next();
				TypeObject parameterType = parameter.getType();
				if(checkFriendship(parameterType, className))
					return true;
			}
			for(CreationObject creation : constructor.getCreations()) {
				TypeObject creationType = creation.getType();
				if(checkFriendship(creationType, className))
					return true;
			}
		}
		for(MethodObject method : methodList) {
			TypeObject returnType = method.getReturnType();
			if(checkFriendship(returnType, className))
				return true;
			ListIterator<ParameterObject> parameterIterator = method.getParameterListIterator();
			while(parameterIterator.hasNext()) {
				ParameterObject parameter = parameterIterator.next();
				TypeObject parameterType = parameter.getType();
				if(checkFriendship(parameterType, className))
					return true;
			}
			for(CreationObject creation : method.getCreations()) {
				TypeObject creationType = creation.getType();
				if(checkFriendship(creationType, className))
					return true;
			}
		}
		if(superclass != null) {
			ClassObject superclassObject = ASTReader.getSystemObject().getClassObject(superclass.getClassType());
			if(superclassObject != null)
				return superclassObject.isFriend(className);
		}
		return false;
	}

	private boolean checkFriendship(TypeObject type, String className) {
		if(type.getClassType().equals(className))
			return true;
		if(type.getGenericType() != null && type.getGenericType().contains(className))
			return true;
		return false;
	}

	public boolean containsMethodWithTestAnnotation() {
    	for(MethodObject method : methodList) {
    		if(method.hasTestAnnotation())
    			return true;
    	}
    	return false;
    }

    public MethodObject getMethod(MethodInvocationObject mio) {
        ListIterator<MethodObject> mi = getMethodIterator();
        while(mi.hasNext()) {
            MethodObject mo = mi.next();
            if(mo.equals(mio))
                return mo;
        }
        return null;
    }

    public MethodObject getMethod(SuperMethodInvocationObject smio) {
        ListIterator<MethodObject> mi = getMethodIterator();
        while(mi.hasNext()) {
            MethodObject mo = mi.next();
            if(mo.equals(smio))
                return mo;
        }
        return null;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
    	for(MethodObject method : methodList) {
    		if(method.containsMethodInvocation(methodInvocation))
    			return true;
    	}
    	return false;
    }

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction) {
    	for(MethodObject method : methodList) {
    		if(method.containsFieldInstruction(fieldInstruction))
    			return true;
    	}
    	return false;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation, MethodObject excludedMethod) {
    	for(MethodObject method : methodList) {
    		if(!method.equals(excludedMethod) && method.containsMethodInvocation(methodInvocation))
    			return true;
    	}
    	return false;
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
    	for(MethodObject method : methodList) {
    		if(method.containsSuperMethodInvocation(superMethodInvocation))
    			return true;
    	}
    	return false;
    }

    public boolean hasFieldType(String className) {
        ListIterator<FieldObject> fi = getFieldIterator();
        while(fi.hasNext()) {
            FieldObject fo = fi.next();
            if(fo.getType().getClassType().equals(className))
                return true;
        }
        return false;
    }

    public List<TypeCheckElimination> generateTypeCheckEliminations() {
    	List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
    	for(MethodObject methodObject : methodList) {
    		MethodBodyObject methodBodyObject = methodObject.getMethodBody();
    		if(methodBodyObject != null) {
    			List<TypeCheckElimination> list = methodBodyObject.generateTypeCheckEliminations();
    			for(TypeCheckElimination typeCheckElimination : list) {
    				if(!typeCheckElimination.allTypeCheckBranchesAreEmpty()) {
    					//TypeCheckCodeFragmentAnalyzer analyzer = new TypeCheckCodeFragmentAnalyzer(typeCheckElimination, typeDeclaration, methodObject.getMethodDeclaration());
    					TypeCheckCodeFragmentAnalyzer analyzer = new TypeCheckCodeFragmentAnalyzer(typeCheckElimination, getTypeDeclaration(), methodObject.getMethodDeclaration(), iFile);
    					if((typeCheckElimination.getTypeField() != null || typeCheckElimination.getTypeLocalVariable() != null || typeCheckElimination.getTypeMethodInvocation() != null) &&
    							typeCheckElimination.allTypeCheckingsContainStaticFieldOrSubclassType() && typeCheckElimination.isApplicable()) {
    						typeCheckEliminations.add(typeCheckElimination);
    					}
    				}
    			}
    		}
    	}
    	return typeCheckEliminations;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public void setSuperclass(TypeObject superclass) {
		this.superclass = superclass;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean addMethod(MethodObject method) {
		return methodList.add(method);
	}
	
	public boolean addInterface(TypeObject i) {
		return interfaceList.add(i);
	}
	
	public boolean addConstructor(ConstructorObject c) {
		return constructorList.add(c);
	}
	
	public boolean addField(FieldObject f) {
		return fieldList.add(f);
	}
	
	public ListIterator<ConstructorObject> getConstructorIterator() {
		return constructorList.listIterator();
	}
	
	public ListIterator<MethodObject> getMethodIterator() {
		return methodList.listIterator();
	}
	
	public ListIterator<TypeObject> getInterfaceIterator() {
		return interfaceList.listIterator();
	}

    public ListIterator<TypeObject> getSuperclassIterator() {
		List<TypeObject> superclassList = new ArrayList<TypeObject>(interfaceList);
		superclassList.add(superclass);
		return superclassList.listIterator();
	}

	public ListIterator<FieldObject> getFieldIterator() {
		return fieldList.listIterator();
	}

	public Set<FieldObject> getFieldsAccessedInsideMethod(MethodObject method) {
		Set<FieldObject> fields = new LinkedHashSet<FieldObject>();
		for(FieldInstructionObject fieldInstruction : method.getFieldInstructions()) {
			for(FieldObject field : fieldList) {
				if(field.equals(fieldInstruction)) {
					if(!fields.contains(field))
						fields.add(field);
					break;
				}
			}
		}
		return fields;
	}

	public String getName() {
		return name;
	}

	public TypeObject getSuperclass() {
		return superclass;
	}
	
	public void setAbstract(boolean abstr) {
		this._abstract = abstr;
	}
	
	public boolean isAbstract() {
		return this._abstract;
	}

    public void setInterface(boolean i) {
        this._interface = i;
    }

    public boolean isInterface() {
        return this._interface;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public int getNumberOfMethods() {
    	return methodList.size();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        if(_static)
            sb.append("static").append(" ");
        if(_interface)
            sb.append("interface").append(" ");
        else if(_abstract)
            sb.append("abstract class").append(" ");
        else
            sb.append("class").append(" ");
        sb.append(name).append(" ");
        sb.append("extends ").append(superclass);
        if(!interfaceList.isEmpty()) {
            sb.append(" ").append("implements ");
            for(int i=0; i<interfaceList.size()-1; i++)
                sb.append(interfaceList.get(i)).append(", ");
            sb.append(interfaceList.get(interfaceList.size()-1));
        }
        sb.append("\n\n").append("Fields:");
        for(FieldObject field : fieldList)
            sb.append("\n").append(field.toString());

        sb.append("\n\n").append("Constructors:");
        for(ConstructorObject constructor : constructorList)
            sb.append("\n").append(constructor.toString());

        sb.append("\n\n").append("Methods:");
        for(MethodObject method : methodList)
            sb.append("\n").append(method.toString());

        return sb.toString();
    }
}