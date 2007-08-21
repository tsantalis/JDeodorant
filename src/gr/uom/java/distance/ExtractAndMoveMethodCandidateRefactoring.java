package gr.uom.java.distance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

public class ExtractAndMoveMethodCandidateRefactoring implements CandidateRefactoring {
	private MySystem system;
	private MyClass sourceClass;
    private MyClass targetClass;
    //this is the method from which the extracted method is going to be extracted
    private MyMethod sourceMethod;
    private LocalVariableDeclarationObject localVariableDeclaration;
    private List<AbstractStatement> statementList;
    private VariableDeclarationStatement variableDeclarationStatement;
    private Set<String> entitySet;
    private double entityPlacement;

    public ExtractAndMoveMethodCandidateRefactoring(MySystem system, MyClass sourceClass, MyClass targetClass, MyMethod sourceMethod, 
    		LocalVariableDeclarationObject localVariableDeclaration, List<AbstractStatement> statementList) {
    	this.system = system;
    	this.sourceClass = sourceClass;
    	this.targetClass = targetClass;
    	this.sourceMethod = sourceMethod;
    	this.localVariableDeclaration = localVariableDeclaration;
    	this.statementList = statementList;
    	this.variableDeclarationStatement = sourceMethod.getMethodObject().getVariableDeclarationStatement(localVariableDeclaration);
    	
    	Set<String> entitySet = sourceMethod.getEntitySet(statementList.get(0));
    	for(int i=1; i<statementList.size(); i++) {
    		Set<String> setI = sourceMethod.getEntitySet(statementList.get(i));
    		entitySet = DistanceCalculator.union(entitySet, setI);
    	}
    	this.entitySet = entitySet;
    }

    public boolean apply() {
    	if(hasReferenceToTargetClass()) {
    		MySystem virtualSystem = MySystem.newInstance(system);
	    	virtualApplication(virtualSystem);
	    	DistanceMatrix distanceMatrix = new DistanceMatrix(virtualSystem);
	    	this.entityPlacement = distanceMatrix.getSystemEntityPlacementValue();
	    	return true;
    	}
    	else
    		return false;
    }

    private boolean hasReferenceToTargetClass() {
    	List<LocalVariableDeclarationObject> sourceMethodLocalVariableDeclarations = sourceMethod.getMethodObject().getLocalVariableDeclarations();
    	for(LocalVariableDeclarationObject localVariableDeclaration : sourceMethodLocalVariableDeclarations) {
    		TypeObject type = localVariableDeclaration.getType();
    		if(type.getClassType().equals(targetClass.getClassObject().getName())) {
    			VariableDeclarationStatement variableDeclaration = sourceMethod.getMethodObject().getVariableDeclarationStatement(localVariableDeclaration);
    			if(variableDeclaration != null) {
	    			ASTNode variableDeclarationParent = variableDeclaration.getParent();
	    			Statement statement = statementList.get(0).getStatement();
					ASTNode statementParent = statement.getParent();
					while(!(statementParent instanceof MethodDeclaration)) {
						if(statementParent.equals(variableDeclarationParent) && variableDeclaration.getStartPosition() < statement.getStartPosition())
							return true;
						statementParent = statementParent.getParent();
					}
    			}
    		}
    	}
    	List<TypeObject> sourceMethodParameterTypes = sourceMethod.getMethodObject().getParameterTypeList();
    	for(TypeObject parameterType : sourceMethodParameterTypes) {
    		if(parameterType.getClassType().equals(targetClass.getClassObject().getName())) {
    			return true;
    		}
    	}
    	ListIterator<FieldObject> sourceClassFieldIterator = sourceClass.getClassObject().getFieldIterator();
    	while(sourceClassFieldIterator.hasNext()) {
    		FieldObject field = sourceClassFieldIterator.next();
    		TypeObject type = field.getType();
    		if(type.getClassType().equals(targetClass.getClassObject().getName())) {
    			return true;
    		}
    	}
    	System.out.println(this.toString() + "\thas no reference to Target class");
    	return false;
    }

    private void virtualApplication(MySystem virtualSystem) {
    	List<MyAbstractStatement> myAbstractStatementList = new ArrayList<MyAbstractStatement>();
    	for(AbstractStatement abstractStatement : statementList) {
    		MyAbstractStatement myAbstractStatement = sourceMethod.getAbstractStatement(abstractStatement);
    		if(myAbstractStatement instanceof MyStatement) {
    			MyStatement myStatement = (MyStatement)myAbstractStatement;
    			myAbstractStatementList.add(MyStatement.newInstance(myStatement));
    		}
    		else if(myAbstractStatement instanceof MyCompositeStatement) {
    			MyCompositeStatement myCompositeStatement = (MyCompositeStatement)myAbstractStatement;
    			myAbstractStatementList.add(MyCompositeStatement.newInstance(myCompositeStatement));
    		}
    	}
    	
    	MyMethodBody newMethodBody = new MyMethodBody(myAbstractStatementList);
    	
    	List<LocalVariableInstructionObject> discreteLocalVariableInstructions = new ArrayList<LocalVariableInstructionObject>();
		for(AbstractStatement abstractStatement : statementList) {
			List<LocalVariableInstructionObject> list = abstractStatement.getLocalVariableInstructions();
			for(LocalVariableInstructionObject instruction : list) {
				if(!discreteLocalVariableInstructions.contains(instruction))
					discreteLocalVariableInstructions.add(instruction);
			}
		}
    	
		List<String> parameterList = new ArrayList<String>();
		for(LocalVariableInstructionObject instruction : discreteLocalVariableInstructions) {
			if(!localVariableDeclaration.equals(instruction))
				parameterList.add(instruction.getType().toString());
		}
		
    	MyMethod newMethod = new MyMethod(sourceMethod.getClassOrigin(),localVariableDeclaration.getName(),
    		localVariableDeclaration.getType().toString(),parameterList);
    	newMethod.setMethodBody(newMethodBody);
    	
    	MyMethodInvocation newMethodInvocation = newMethod.generateMethodInvocation();
    	
    	MyMethod newSourceMethod = virtualSystem.getClass(sourceClass.getName()).getMethod(sourceMethod);
    	newSourceMethod.replaceStatementsWithMethodInvocation(statementList, new MyStatement(newMethodInvocation));
    	virtualSystem.getClass(sourceClass.getName()).addMethod(newMethod);
    	
    	ListIterator<MyAttributeInstruction> attributeInstructionIterator = newMethod.getAttributeInstructionIterator();
    	while(attributeInstructionIterator.hasNext()) {
    		MyAttributeInstruction myAttributeInstruction = attributeInstructionIterator.next();
    		MyAttribute myAttribute = virtualSystem.getClass(myAttributeInstruction.getClassOrigin()).getAttribute(myAttributeInstruction);
    		if(!newSourceMethod.containsAttributeInstruction(myAttributeInstruction))
    			myAttribute.removeMethod(newSourceMethod);
    		myAttribute.addMethod(newMethod);
    	}
    	
    	if(!sourceClass.equals(targetClass))
    		moveMethod(virtualSystem, newMethod);
    }
    
    private void moveMethod(MySystem virtualSystem, MyMethod oldMethod) {
        MyMethod newMethod = MyMethod.newInstance(oldMethod);
        newMethod.setClassOrigin(targetClass.getName());
        newMethod.removeParameter(targetClass.getName());

        ListIterator<MyAttributeInstruction> instructionIterator = newMethod.getAttributeInstructionIterator();
        List<MyAttributeInstruction> instructionsToBeRemoved = new ArrayList<MyAttributeInstruction>();
        while(instructionIterator.hasNext()) {
            MyAttributeInstruction instruction = instructionIterator.next();
            if(instruction.getClassOrigin().equals(oldMethod.getClassOrigin())) {
                newMethod.addParameter(instruction.getClassType());
                instructionsToBeRemoved.add(instruction);
            }
        }
        for(MyAttributeInstruction instruction : instructionsToBeRemoved) {
        	newMethod.removeAttributeInstruction(instruction);
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
                if(attribute.getClassOrigin().equals(sourceClass.getName()))
                	attribute.removeMethod(oldMethod);
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

    public List<Statement> getStatementList() {
    	List<Statement> list = new ArrayList<Statement>();
    	for(AbstractStatement abstractStatement : statementList) {
    		list.add(abstractStatement.getStatement());
    	}
    	return list;
    }

    public VariableDeclarationStatement getVariableDeclarationStatement() {
    	return variableDeclarationStatement;
    }
    
    public VariableDeclarationFragment getVariableDeclarationFragment() {
    	List<VariableDeclarationFragment> fragmentList = variableDeclarationStatement.fragments();
    	for(VariableDeclarationFragment fragment : fragmentList) {
    		if(fragment.getName().getIdentifier().equals(localVariableDeclaration.getName()))
    			return fragment;
    	}
    	return null;
    }

    public List<VariableDeclarationStatement> getVariableDeclarationStatements() {
    	List<VariableDeclarationStatement> list = new ArrayList<VariableDeclarationStatement>();
    	List<LocalVariableInstructionObject> discreteLocalVariableInstructions = new ArrayList<LocalVariableInstructionObject>();
		for(AbstractStatement abstractStatement : statementList) {
			List<LocalVariableInstructionObject> instructionList = abstractStatement.getLocalVariableInstructions();
			for(LocalVariableInstructionObject instruction : instructionList) {
				if(!discreteLocalVariableInstructions.contains(instruction)) {
					discreteLocalVariableInstructions.add(instruction);
					VariableDeclarationStatement variableDeclarationStatement = 
						sourceMethod.getMethodObject().getVariableDeclarationStatement(new LocalVariableDeclarationObject(instruction.getType(), instruction.getName()));
					if(variableDeclarationStatement != null)
						list.add(variableDeclarationStatement);
				}
			}
		}
		return list;
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

    public LocalVariableDeclarationObject getLocalVariableDeclaration() {
    	return localVariableDeclaration;
    }

    public List<AbstractStatement> getAbstractStatementList() {
    	return statementList;
    }

    public Set<String> getEntitySet() {
    	return entitySet;
    }

    public String toString() {
        return getSourceEntity() + "->" + getTarget();
    }

	public String getSourceEntity() {
		return sourceClass.getName() + "::" + localVariableDeclaration.getName() + "():" + localVariableDeclaration.getType();
	}

	public String getTarget() {
		return targetClass.getName();
	}

	public double getEntityPlacement() {
		return entityPlacement;
	}
}
