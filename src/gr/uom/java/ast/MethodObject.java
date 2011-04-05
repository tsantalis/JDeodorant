package gr.uom.java.ast;

import gr.uom.java.ast.association.Association;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodObject {

    private TypeObject returnType;
    private boolean _abstract;
    private boolean _static;
    private boolean _synchronized;
    private boolean _native;
    private ConstructorObject constructorObject;
    private boolean testAnnotation;
    private volatile int hashCode = 0;

    public MethodObject(ConstructorObject co) {
        this.constructorObject = co;
        this._abstract = false;
        this._static = false;
        this._synchronized = false;
        this._native = false;
        this.testAnnotation = false;
    }

    public void setReturnType(TypeObject returnType) {
        this.returnType = returnType;
    }

    public TypeObject getReturnType() {
        return returnType;
    }

    public void setAbstract(boolean abstr) {
        this._abstract = abstr;
    }

    public boolean isAbstract() {
        return this._abstract;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public boolean isSynchronized() {
    	return this._synchronized;
    }

    public void setSynchronized(boolean s) {
    	this._synchronized = s;
    }

    public boolean isNative() {
    	return this._native;
    }

    public void setNative(boolean n) {
    	this._native = n;
    }

    public void setName(String name) {
        this.constructorObject.name = name;
    }

    public String getName() {
        return constructorObject.getName();
    }

    public boolean hasTestAnnotation() {
		return testAnnotation;
	}

	public void setTestAnnotation(boolean testAnnotation) {
		this.testAnnotation = testAnnotation;
	}

	public Access getAccess() {
        return constructorObject.getAccess();
    }

    public MethodDeclaration getMethodDeclaration() {
    	return constructorObject.getMethodDeclaration();
    }

    public MethodBodyObject getMethodBody() {
    	return constructorObject.getMethodBody();
    }

    public void setMethodBody(MethodBodyObject methodBody) {
    	constructorObject.setMethodBody(methodBody);
    }

    public MethodInvocationObject generateMethodInvocation() {
    	return new MethodInvocationObject(this.constructorObject.className, this.constructorObject.name, this.returnType, this.constructorObject.getParameterTypeList());
    }

    public SuperMethodInvocationObject generateSuperMethodInvocation() {
    	return new SuperMethodInvocationObject(this.constructorObject.className, this.constructorObject.name, this.returnType, this.constructorObject.getParameterTypeList());
    }

    public FieldInstructionObject isGetter() {
    	if(getMethodBody() != null) {
	    	List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
	    	if(abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
	    		StatementObject statementObject = (StatementObject)abstractStatements.get(0);
	    		Statement statement = statementObject.getStatement();
	    		if(statement instanceof ReturnStatement) {
	    			ReturnStatement returnStatement = (ReturnStatement) statement;
	    			if((returnStatement.getExpression() instanceof SimpleName || returnStatement.getExpression() instanceof FieldAccess) && statementObject.getFieldInstructions().size() == 1 && statementObject.getMethodInvocations().size() == 0 &&
		    				statementObject.getLocalVariableDeclarations().size() == 0 && statementObject.getLocalVariableInstructions().size() == 0 && this.constructorObject.parameterList.size() == 0) {
	    				return statementObject.getFieldInstructions().get(0);
	    			}
	    		}
	    	}
    	}
    	return null;
    }

    public FieldInstructionObject isSetter() {
    	if(getMethodBody() != null) {
	    	List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
	    	if(abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
	    		StatementObject statementObject = (StatementObject)abstractStatements.get(0);
	    		Statement statement = statementObject.getStatement();
	    		if(statement instanceof ExpressionStatement) {
	    			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
	    			if(expressionStatement.getExpression() instanceof Assignment && statementObject.getFieldInstructions().size() == 1 && statementObject.getMethodInvocations().size() == 0 &&
	        				statementObject.getLocalVariableDeclarations().size() == 0 && statementObject.getLocalVariableInstructions().size() == 1 && this.constructorObject.parameterList.size() == 1) {
	    				Assignment assignment = (Assignment)expressionStatement.getExpression();
	    				if((assignment.getLeftHandSide() instanceof SimpleName || assignment.getLeftHandSide() instanceof FieldAccess) && assignment.getRightHandSide() instanceof SimpleName)
	    					return statementObject.getFieldInstructions().get(0);
	    			}
	    		}
	    	}
    	}
    	return null;
    }

    public FieldInstructionObject isCollectionAdder() {
    	if(getMethodBody() != null) {
	    	List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
	    	if(abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
	    		StatementObject statementObject = (StatementObject)abstractStatements.get(0);
	    		if(statementObject.getFieldInstructions().size() == 1 && statementObject.getMethodInvocations().size() == 1 &&
	    				statementObject.getLocalVariableDeclarations().size() == 0 && statementObject.getLocalVariableInstructions().size() == 1 && this.constructorObject.parameterList.size() == 1) {
	    			String methodName = statementObject.getMethodInvocations().get(0).getMethodName();
	    			String originClassName = statementObject.getMethodInvocations().get(0).getOriginClassName();
	    			List<String> acceptableOriginClassNames = new ArrayList<String>();
	    			acceptableOriginClassNames.add("java.util.Collection");
	    			acceptableOriginClassNames.add("java.util.AbstractCollection");
	    			acceptableOriginClassNames.add("java.util.List");
	    			acceptableOriginClassNames.add("java.util.AbstractList");
	    			acceptableOriginClassNames.add("java.util.ArrayList");
	    			acceptableOriginClassNames.add("java.util.LinkedList");
	    			acceptableOriginClassNames.add("java.util.Set");
	    			acceptableOriginClassNames.add("java.util.AbstractSet");
	    			acceptableOriginClassNames.add("java.util.HashSet");
	    			acceptableOriginClassNames.add("java.util.LinkedHashSet");
	    			acceptableOriginClassNames.add("java.util.SortedSet");
	    			acceptableOriginClassNames.add("java.util.TreeSet");
	    			acceptableOriginClassNames.add("java.util.Vector");
	    			if(methodName.equals("add") || methodName.equals("addElement") || methodName.equals("addAll")) {
	    				if(acceptableOriginClassNames.contains(originClassName))
	    					return statementObject.getFieldInstructions().get(0);
	    			}
	    		}
	    	}
    	}
    	return null;
    }

    public MethodInvocationObject isDelegate() {
    	if(getMethodBody() != null) {
    		MethodDeclaration methodDeclaration = getMethodDeclaration();
    		TypeDeclaration parentClass = (TypeDeclaration)methodDeclaration.getParent();
	    	List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
	    	if(abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
	    		StatementObject statementObject = (StatementObject)abstractStatements.get(0);
	    		Statement statement = statementObject.getStatement();
	    		MethodInvocation methodInvocation = null;
	    		if(statement instanceof ReturnStatement) {
	    			ReturnStatement returnStatement = (ReturnStatement)statement;
	    			if(returnStatement.getExpression() instanceof MethodInvocation) {
	    				methodInvocation = (MethodInvocation)returnStatement.getExpression();
	    			}
	    		}
	    		else if(statement instanceof ExpressionStatement) {
	    			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
	    			if(expressionStatement.getExpression() instanceof MethodInvocation) {
	    				methodInvocation = (MethodInvocation)expressionStatement.getExpression();
	    			}
	    		}
	    		if(methodInvocation != null) {
	    			Expression methodInvocationExpression = methodInvocation.getExpression();
	    			List<MethodInvocationObject> methodInvocations = statementObject.getMethodInvocations();
	    			if(methodInvocationExpression instanceof MethodInvocation) {
	    				MethodInvocation previousChainedMethodInvocation = (MethodInvocation)methodInvocationExpression;
	    				MethodDeclaration[] parentClassMethods = parentClass.getMethods();
	    				boolean isDelegationChain = false;
		    			boolean foundInParentClass = false;
	    				for(MethodDeclaration parentClassMethod : parentClassMethods) {
	    					if(parentClassMethod.resolveBinding().isEqualTo(previousChainedMethodInvocation.resolveMethodBinding())) {
	    						foundInParentClass = true;
	    						SimpleName getterField = MethodDeclarationUtility.isGetter(parentClassMethod);
	    						if(getterField == null)
	    							isDelegationChain = true;
	    						break;
	    					}
	    				}
	    				if(!isDelegationChain && foundInParentClass) {
				    		for(MethodInvocationObject methodInvocationObject : methodInvocations) {
				    			if(methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
				    				return methodInvocationObject;
				    			}
				    		}
		    			}
	    			}
	    			else if(methodInvocationExpression instanceof FieldAccess) {
	    				FieldAccess fieldAccess = (FieldAccess)methodInvocationExpression;
	    				IVariableBinding variableBinding = fieldAccess.resolveFieldBinding();
	    				if(variableBinding.getDeclaringClass().isEqualTo(parentClass.resolveBinding()) ||
	    						parentClass.resolveBinding().isSubTypeCompatible(variableBinding.getDeclaringClass())) {
		    				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
				    			if(methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
				    				return methodInvocationObject;
				    			}
				    		}
	    				}
	    			}
	    			else if(methodInvocationExpression instanceof SimpleName) {
	    				SimpleName simpleName = (SimpleName)methodInvocationExpression;
	    				IBinding binding = simpleName.resolveBinding();
	    				if(binding.getKind() == IBinding.VARIABLE) {
	    					IVariableBinding variableBinding = (IVariableBinding)binding;
	    					if(variableBinding.isField() || variableBinding.isParameter()) {
	    						for(MethodInvocationObject methodInvocationObject : methodInvocations) {
	    							if(methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
	    								return methodInvocationObject;
	    							}
	    						}
	    					}
	    				}
	    			}
	    			else if(methodInvocationExpression instanceof ThisExpression) {
	    				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
			    			if(methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
			    				return methodInvocationObject;
			    			}
			    		}
	    			}
	    			else if(methodInvocationExpression == null) {
	    				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
			    			if(methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
			    				return methodInvocationObject;
			    			}
			    		}
	    			}
	    		}
	    	}
    	}
    	return null;
    }

    public boolean validTargetObject(ClassObject sourceClass, ClassObject targetClass) {
    	ITypeBinding targetClassBinding = targetClass.getTypeDeclaration().resolveBinding();
    	List<LocalVariableInstructionObject> localVariableInstructions = getLocalVariableInstructions();
    	for(LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
    		if(localVariableInstruction.getType().getClassType().equals(targetClass.getName())) {
    			for(LocalVariableDeclarationObject variableDeclaration : getLocalVariableDeclarations()) {
    				if(variableDeclaration.getVariableDeclaration().resolveBinding().isEqualTo(
    						localVariableInstruction.getSimpleName().resolveBinding()))
    					return false;
    			}
    		}
    	}
    	for(LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
    		if(localVariableInstruction.getType().getClassType().equals(targetClass.getName())) {
    			ListIterator<ParameterObject> parameterIterator = getParameterListIterator();
				while(parameterIterator.hasNext()) {
					ParameterObject parameter = parameterIterator.next();
					if(localVariableInstruction.getName().equals(parameter.getName()) && parameter.getType().getArrayDimension() == 0)
						return true;
				}
    		}
    	}
    	List<FieldInstructionObject> fieldInstructions = getFieldInstructions();
    	for(FieldInstructionObject fieldInstruction : fieldInstructions) {
    		ITypeBinding fieldTypeBinding = fieldInstruction.getSimpleName().resolveTypeBinding();
    		if(fieldInstruction.getType().getClassType().equals(targetClass.getName()) || fieldTypeBinding.isEqualTo(targetClassBinding) ||
    				targetClassBinding.isEqualTo(fieldTypeBinding.getSuperclass())) {
				ListIterator<FieldObject> fieldIterator = sourceClass.getFieldIterator();
				while(fieldIterator.hasNext()) {
					FieldObject field = fieldIterator.next();
					if(fieldInstruction.getName().equals(field.getName()) && field.getType().getArrayDimension() == 0)
						return true;
				}
    		}
    	}
    	List<MethodInvocationObject> methodInvocations = getMethodInvocations();
    	for(MethodInvocationObject methodInvocation : methodInvocations) {
    		if(methodInvocation.getOriginClassName().equals(sourceClass.getName())) {
    			MethodObject invokedMethod = sourceClass.getMethod(methodInvocation);
    			FieldInstructionObject fieldInstruction = invokedMethod.isGetter();
    			if(fieldInstruction != null && fieldInstruction.getType().getClassType().equals(targetClass.getName()))
    				return true;
    			MethodInvocationObject delegation = invokedMethod.isDelegate();
    			if(delegation != null && delegation.getOriginClassName().equals(targetClass.getName()))
    				return true;
    		}
    	}
    	return false;
    }

    public boolean oneToManyRelationshipWithTargetClass(List<Association> associations, ClassObject targetClass) {
    	List<FieldInstructionObject> fieldInstructions = getFieldInstructions();
    	for(Association association : associations) {
    		FieldObject fieldObject = association.getFieldObject();
    		for(FieldInstructionObject fieldInstruction : fieldInstructions) {
    			if(fieldObject.equals(fieldInstruction) && targetClass.getName().equals(association.getTo()) &&
    					association.isContainer())
    				return true;
    		}
    	}
    	return false;
    }

    public boolean overridesMethod() {
    	IMethodBinding methodBinding = getMethodDeclaration().resolveBinding();
    	ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
    	Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
    	ITypeBinding superClassTypeBinding = declaringClassTypeBinding.getSuperclass();
    	if(superClassTypeBinding != null)
    		typeBindings.add(superClassTypeBinding);
    	ITypeBinding[] interfaceTypeBindings = declaringClassTypeBinding.getInterfaces();
    	for(ITypeBinding interfaceTypeBinding : interfaceTypeBindings)
    		typeBindings.add(interfaceTypeBinding);
    	return overridesMethod(typeBindings);
    }

    private boolean overridesMethod(Set<ITypeBinding> typeBindings) {
    	IMethodBinding methodBinding = getMethodDeclaration().resolveBinding();
    	Set<ITypeBinding> superTypeBindings = new LinkedHashSet<ITypeBinding>();
    	for(ITypeBinding typeBinding : typeBindings) {
    		ITypeBinding superClassTypeBinding = typeBinding.getSuperclass();
        	if(superClassTypeBinding != null)
        		superTypeBindings.add(superClassTypeBinding);
        	ITypeBinding[] interfaceTypeBindings = typeBinding.getInterfaces();
        	for(ITypeBinding interfaceTypeBinding : interfaceTypeBindings)
        		superTypeBindings.add(interfaceTypeBinding);
    		if(typeBinding.isInterface()) {
    			IMethodBinding[] interfaceMethodBindings = typeBinding.getDeclaredMethods();
        		for(IMethodBinding interfaceMethodBinding : interfaceMethodBindings) {
        			if(methodBinding.overrides(interfaceMethodBinding) || methodBinding.toString().equals(interfaceMethodBinding.toString()))
        				return true;
        		}
    		}
    		else {
    			IMethodBinding[] superClassMethodBindings = typeBinding.getDeclaredMethods();
    	    	for(IMethodBinding superClassMethodBinding : superClassMethodBindings) {
    	    		if(methodBinding.overrides(superClassMethodBinding) || methodBinding.toString().equals(superClassMethodBinding.toString()))
    	    			return true;
    	    	}
    		}
    	}
    	if(!superTypeBindings.isEmpty()) {
    		return overridesMethod(superTypeBindings);
    	}
    	else
    		return false;
    }

    public void setClassName(String className) {
    	constructorObject.setClassName(className);
    }

    public String getClassName() {
        return constructorObject.getClassName();
    }

    public ListIterator<ParameterObject> getParameterListIterator() {
        return constructorObject.getParameterListIterator();
    }

    public ParameterObject getParameter(int position) {
    	return constructorObject.getParameter(position);
    }

    public List<MethodInvocationObject> getMethodInvocations() {
        return constructorObject.getMethodInvocations();
    }

    public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
        return constructorObject.getSuperMethodInvocations();
    }

    public List<FieldInstructionObject> getFieldInstructions() {
        return constructorObject.getFieldInstructions();
    }

    public List<SuperFieldInstructionObject> getSuperFieldInstructions() {
    	return constructorObject.getSuperFieldInstructions();
    }

    public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
        return constructorObject.getLocalVariableDeclarations();
    }
    
    public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
        return constructorObject.getLocalVariableInstructions();
    }

	public List<CreationObject> getCreations() {
		return constructorObject.getCreations();
	}

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
    	return constructorObject.containsMethodInvocation(methodInvocation);
    }

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction) {
    	return constructorObject.containsFieldInstruction(fieldInstruction);
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
    	return constructorObject.containsSuperMethodInvocation(superMethodInvocation);
    }

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields() {
		return constructorObject.getInvokedMethodsThroughFields();
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters() {
		return constructorObject.getInvokedMethodsThroughParameters();
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables() {
		return constructorObject.getInvokedMethodsThroughLocalVariables();
	}

	public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference() {
		return constructorObject.getInvokedMethodsThroughThisReference();
	}

	public Set<MethodInvocationObject> getInvokedStaticMethods() {
		return constructorObject.getInvokedStaticMethods();
	}

	public Set<AbstractVariable> getDefinedFieldsThroughFields() {
		return constructorObject.getDefinedFieldsThroughFields();
	}

	public Set<AbstractVariable> getUsedFieldsThroughFields() {
		return constructorObject.getUsedFieldsThroughFields();
	}

	public Set<AbstractVariable> getDefinedFieldsThroughParameters() {
		return constructorObject.getDefinedFieldsThroughParameters();
	}

	public Set<AbstractVariable> getUsedFieldsThroughParameters() {
		return constructorObject.getUsedFieldsThroughParameters();
	}

	public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables() {
		return constructorObject.getDefinedFieldsThroughLocalVariables();
	}

	public Set<AbstractVariable> getUsedFieldsThroughLocalVariables() {
		return constructorObject.getUsedFieldsThroughLocalVariables();
	}

	public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
		return constructorObject.getDefinedFieldsThroughThisReference();
	}

	public Set<PlainVariable> getUsedFieldsThroughThisReference() {
		return constructorObject.getUsedFieldsThroughThisReference();
	}

	public Set<PlainVariable> getDeclaredLocalVariables() {
		return constructorObject.getDeclaredLocalVariables();
	}

	public Set<PlainVariable> getDefinedLocalVariables() {
		return constructorObject.getDefinedLocalVariables();
	}

	public Set<PlainVariable> getUsedLocalVariables() {
		return constructorObject.getUsedLocalVariables();
	}

	public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations() {
		return constructorObject.getParametersPassedAsArgumentsInMethodInvocations();
	}

	public Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocations() {
		return constructorObject.getParametersPassedAsArgumentsInSuperMethodInvocations();
	}

    public boolean containsSuperMethodInvocation() {
    	return constructorObject.containsSuperMethodInvocation();
    }

    public boolean containsSuperFieldAccess() {
    	return constructorObject.containsSuperFieldAccess();
    }

    public List<TypeObject> getParameterTypeList() {
    	return constructorObject.getParameterTypeList();
    }

    public List<String> getParameterList() {
    	return constructorObject.getParameterList();
    }

    public boolean equals(MethodInvocationObject mio) {
    	return this.getClassName().equals(mio.getOriginClassName()) && this.getName().equals(mio.getMethodName()) &&
    		this.returnType.equals(mio.getReturnType()) && this.constructorObject.getParameterTypeList().equals(mio.getParameterTypeList());
    }

    public boolean equals(SuperMethodInvocationObject smio) {
    	return this.getClassName().equals(smio.getOriginClassName()) && this.getName().equals(smio.getMethodName()) &&
    		this.returnType.equals(smio.getReturnType()) && this.constructorObject.getParameterTypeList().equals(smio.getParameterTypeList());
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof MethodObject) {
            MethodObject methodObject = (MethodObject)o;

            return this.returnType.equals(methodObject.returnType) &&
                this.constructorObject.equals(methodObject.constructorObject);
        }
        return false;
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + returnType.hashCode();
    		result = 37*result + constructorObject.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!constructorObject.access.equals(Access.NONE))
            sb.append(constructorObject.access.toString()).append(" ");
        if(_abstract)
            sb.append("abstract").append(" ");
        if(_static)
            sb.append("static").append(" ");
        sb.append(returnType.toString()).append(" ");
        sb.append(constructorObject.name);
        sb.append("(");
        if(!constructorObject.parameterList.isEmpty()) {
            for(int i=0; i<constructorObject.parameterList.size()-1; i++)
                sb.append(constructorObject.parameterList.get(i).toString()).append(", ");
            sb.append(constructorObject.parameterList.get(constructorObject.parameterList.size()-1).toString());
        }
        sb.append(")");
        sb.append("\n").append(constructorObject.methodBody.toString());
        return sb.toString();
    }
}