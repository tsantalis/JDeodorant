package gr.uom.java.distance;

import java.util.Iterator;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MoveMethodCandidateRefactoring implements CandidateRefactoring {
    private MyClass sourceClass;
    private MyClass targetClass;
    private MyMethod sourceMethod;
    private double entityPlacement;

    public MoveMethodCandidateRefactoring(MySystem system, MyClass sourceClass, MyClass targetClass, MyMethod sourceMethod) {
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.sourceMethod = sourceMethod;
        
        MySystem virtualSystem = MySystem.newInstance(system);
	    virtualApplication(virtualSystem);
	    DistanceMatrix distanceMatrix = new DistanceMatrix(virtualSystem);
	    this.entityPlacement = distanceMatrix.getSystemEntityPlacementValue();
    }

    public void virtualApplication(MySystem virtualSystem) {
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
