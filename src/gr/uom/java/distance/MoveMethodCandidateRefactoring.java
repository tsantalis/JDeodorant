package gr.uom.java.distance;

import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class MoveMethodCandidateRefactoring implements CandidateRefactoring {
    private MySystem system;
	private MyClass sourceClass;
    private MyClass targetClass;
    private MyMethod sourceMethod;
    private double entityPlacement;

    public MoveMethodCandidateRefactoring(MySystem system, MyClass sourceClass, MyClass targetClass, MyMethod sourceMethod) {
        this.system = system;
    	this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.sourceMethod = sourceMethod;
    }

    public boolean apply() {
    	if(hasReferenceToTargetClass() && !overridesAbstractMethod()) {
    		MySystem virtualSystem = MySystem.newInstance(system);
    	    virtualApplication(virtualSystem);
    	    DistanceMatrix distanceMatrix = new DistanceMatrix(virtualSystem);
    	    this.entityPlacement = distanceMatrix.getSystemEntityPlacementValue();
    	    return true;
    	}
    	else
    		return false;
    }

    private boolean overridesAbstractMethod() {
    	if(sourceMethod.getMethodObject().overridesAbstractMethod()) {
    		System.out.println(this.toString() + "\toverrides abstract method");
    		return true;
    	}
    	else
    		return false;
    }

    private boolean hasReferenceToTargetClass() {
    	ListIterator<MethodObject> sourceMethodIterator = sourceClass.getClassObject().getMethodIterator();
    	MethodInvocationObject sourceMethodInvocation = sourceMethod.getMethodObject().generateMethodInvocation();
    	int numberOfMethodsInvokingSourceMethod = 0;
    	int numberOfMethodsHavingReferenceToTargetClass = 0;
    	while(sourceMethodIterator.hasNext()) {
    		MethodObject method = sourceMethodIterator.next();
    		if(method.containsMethodInvocation(sourceMethodInvocation)) {
    			boolean hasReferenceToTargetClass = false;
    			numberOfMethodsInvokingSourceMethod++;
    			List<TypeObject> sourceMethodParameterTypes = method.getParameterTypeList();
    	    	for(TypeObject parameterType : sourceMethodParameterTypes) {
    	    		if(parameterType.getClassType().equals(targetClass.getClassObject().getName())) {
    	    			hasReferenceToTargetClass = true;
    	    		}
    	    	}
    	    	List<LocalVariableDeclarationObject> sourceMethodLocalVariableDeclarations = method.getLocalVariableDeclarations();
    	    	for(LocalVariableDeclarationObject localVariableDeclaration : sourceMethodLocalVariableDeclarations) {
    	    		TypeObject type = localVariableDeclaration.getType();
    	    		if(type.getClassType().equals(targetClass.getClassObject().getName())) {
    	    			List<AbstractStatement> methodInvocationStatements = method.getMethodInvocationStatements(sourceMethodInvocation);
    	    			VariableDeclarationStatement variableDeclaration = method.getVariableDeclarationStatement(localVariableDeclaration);
    	    			int sameScopeCounter = 0;
    	    			for(AbstractStatement methodInvocationStatement : methodInvocationStatements) {
    	    				Statement methodInvocation = methodInvocationStatement.getStatement();
    	    				if(methodInvocation.getParent().equals(variableDeclaration.getParent()))
    	    					sameScopeCounter++;
    	    			}
    	    			if(sameScopeCounter == methodInvocationStatements.size())
    	    				hasReferenceToTargetClass = true;
    	    		}
    	    	}
    	    	ListIterator<FieldObject> sourceClassFieldIterator = sourceClass.getClassObject().getFieldIterator();
    	    	while(sourceClassFieldIterator.hasNext()) {
    	    		FieldObject field = sourceClassFieldIterator.next();
    	    		TypeObject type = field.getType();
    	    		if(type.getClassType().equals(targetClass.getClassObject().getName())) {
    	    			hasReferenceToTargetClass = true;
    	    		}
    	    	}
    	    	if(hasReferenceToTargetClass)
    	    		numberOfMethodsHavingReferenceToTargetClass++;
    		}
    	}
    	if(numberOfMethodsInvokingSourceMethod > 0 && numberOfMethodsInvokingSourceMethod != numberOfMethodsHavingReferenceToTargetClass) {
    		System.out.println(this.toString() + "\thas no reference to Target class");
    		return false;
    	}
    	else
    		return true;
    }

    private void virtualApplication(MySystem virtualSystem) {
    	MyMethod oldMethod = sourceMethod;
        MyMethod newMethod = MyMethod.newInstance(oldMethod);
        newMethod.setClassOrigin(targetClass.getName());
        newMethod.removeParameter(targetClass.getName());

        ListIterator<MyAttributeInstruction> instructionIterator = newMethod.getAttributeInstructionIterator();
        while(instructionIterator.hasNext()) {
            MyAttributeInstruction instruction = instructionIterator.next();
            if(instruction.getClassOrigin().equals(oldMethod.getClassOrigin()))
                newMethod.addParameter(instruction.getClassOrigin());
        }
        ListIterator<MyMethodInvocation> invocationIterator = newMethod.getMethodInvocationIterator();
        while(invocationIterator.hasNext()) {
            MyMethodInvocation invocation = invocationIterator.next();
            if(invocation.getClassOrigin().equals(oldMethod.getClassOrigin()))
                newMethod.addParameter(invocation.getClassOrigin());
        }

        MyMethodInvocation oldMethodInvocation = oldMethod.generateMethodInvocation();
        MyMethodInvocation newMethodInvocation = newMethod.generateMethodInvocation();
        virtualSystem.getClass(sourceClass.getName()).removeMethod(oldMethod);
        virtualSystem.getClass(targetClass.getName()).addMethod(newMethod);
        Iterator<MyClass> classIterator = virtualSystem.getClassIterator();
        while(classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while(attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                attribute.replaceMethod(oldMethod,newMethod);
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod myMethod = methodIterator.next();
                myMethod.replaceMethodInvocation(oldMethodInvocation,newMethodInvocation);
            }
        }
    }

    public TypeDeclaration getSourceClassTypeDeclaration() {
        return sourceClass.getClassObject().getTypeDeclaration();
    }

    public TypeDeclaration getTargetClassTypeDeclaration() {
        return targetClass.getClassObject().getTypeDeclaration();
    }

    public MethodDeclaration getSourceMethodDeclaration() {
        return sourceMethod.getMethodObject().getMethodDeclaration();
    }

    public MyClass getSourceClass() {
    	return sourceClass;
    }

    public MyClass getTargetClass() {
    	return targetClass;
    }

    public MyMethod getSourceMethod() {
    	return sourceMethod;
    }

    public String toString() {
        return getSourceEntity() + "->" + getTarget();
    }

	public String getSourceEntity() {
		return sourceMethod.toString();
	}

	public String getTarget() {
		return targetClass.getName();
	}

	public double getEntityPlacement() {
		return entityPlacement;
	}
}
