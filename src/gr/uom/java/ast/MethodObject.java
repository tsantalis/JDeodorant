package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

public class MethodObject {

    private TypeObject returnType;
    private boolean _abstract;
    private boolean _static;
    private String className;
    private ConstructorObject constructorObject;

    public MethodObject(ConstructorObject co) {
        this.constructorObject = co;
        this._abstract = false;
        this._static = false;
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

    public void setName(String name) {
        this.constructorObject.name = name;
    }

    public String getName() {
        return constructorObject.getName();
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

    public MethodInvocationObject generateMethodInvocation() {
    	return new MethodInvocationObject(this.className, this.constructorObject.name, this.returnType, this.constructorObject.getParameterTypeList());
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
		    		List<MethodInvocationObject> methodInvocations = statementObject.getMethodInvocations();
		    		for(MethodInvocationObject methodInvocationObject : methodInvocations) {
		    			if(methodInvocationObject.getMethodName().equals(methodInvocation.getName().getIdentifier()))
		    				return methodInvocationObject;
		    		}
	    		}
	    	}
    	}
    	return null;
    }

    public boolean overridesAbstractMethod() {
    	IMethodBinding methodBinding = getMethodDeclaration().resolveBinding();
    	ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
    	Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
    	ITypeBinding superClassTypeBinding = declaringClassTypeBinding.getSuperclass();
    	if(superClassTypeBinding != null)
    		typeBindings.add(superClassTypeBinding);
    	ITypeBinding[] interfaceTypeBindings = declaringClassTypeBinding.getInterfaces();
    	for(ITypeBinding interfaceTypeBinding : interfaceTypeBindings)
    		typeBindings.add(interfaceTypeBinding);
    	return overridesAbstractMethod(typeBindings);
    }

    private boolean overridesAbstractMethod(Set<ITypeBinding> typeBindings) {
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
        			if(methodBinding.overrides(interfaceMethodBinding))
        				return true;
        		}
    		}
    		else {
    			if((typeBinding.getModifiers() & Modifier.ABSTRACT) != 0) {
    				IMethodBinding[] superClassMethodBindings = typeBinding.getDeclaredMethods();
    	    		for(IMethodBinding superClassMethodBinding : superClassMethodBindings) {
    	    			if((superClassMethodBinding.getModifiers() & Modifier.ABSTRACT) != 0 && methodBinding.overrides(superClassMethodBinding))
    	    				return true;
    	    		}
    			}
    		}
    	}
    	if(!superTypeBindings.isEmpty()) {
    		return overridesAbstractMethod(superTypeBindings);
    	}
    	else
    		return false;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    public ListIterator<ParameterObject> getParameterListIterator() {
        return constructorObject.getParameterListIterator();
    }

    public List<MethodInvocationObject> getMethodInvocations() {
        return constructorObject.getMethodInvocations();
    }

    public List<FieldInstructionObject> getFieldInstructions() {
        return constructorObject.getFieldInstructions();
    }

    public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
        return constructorObject.getLocalVariableDeclarations();
    }
    
    public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
        return constructorObject.getLocalVariableInstructions();
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
    	return constructorObject.containsMethodInvocation(methodInvocation);
    }

    public List<TypeObject> getParameterTypeList() {
    	return constructorObject.getParameterTypeList();
    }

    public List<String> getParameterList() {
    	return constructorObject.getParameterList();
    }

    public boolean equals(MethodInvocationObject mio) {
    	return this.className.equals(mio.getOriginClassName()) && this.getName().equals(mio.getMethodName()) &&
    		this.returnType.equals(mio.getReturnType()) && this.constructorObject.getParameterTypeList().equals(mio.getParameterTypeList());
    }
    
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof MethodObject) {
            MethodObject methodObject = (MethodObject)o;

            return this.className.equals(methodObject.className) && this.returnType.equals(methodObject.returnType) &&
                this.constructorObject.equals(methodObject.constructorObject);
        }
        return false;
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