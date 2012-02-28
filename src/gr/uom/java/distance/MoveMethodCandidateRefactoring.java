package gr.uom.java.distance;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.Position;

public class MoveMethodCandidateRefactoring extends CandidateRefactoring implements Comparable<MoveMethodCandidateRefactoring> {
    private MySystem system;
	private MyClass sourceClass;
    private MyClass targetClass;
    private MyMethod sourceMethod;
    private double entityPlacement;
    //contains source class methods that do not access any field or method and are accessed only by sourceMethod
    private Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved;
    private DistanceMatrix originalDistanceMatrix;
    private String movedMethodName;
    private Set<Entity> changedEntities;
    private Set<MyClass> changedClasses;
    private Set<MyMethod> oldMovedMethods;
    private Set<MyMethod> newMovedMethods;
    private Integer userRate;

    public MoveMethodCandidateRefactoring(MySystem system, MyClass sourceClass, MyClass targetClass, MyMethod sourceMethod, DistanceMatrix originalDistanceMatrix) {
        this.system = system;
    	this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.sourceMethod = sourceMethod;
        this.originalDistanceMatrix = originalDistanceMatrix;
        this.additionalMethodsToBeMoved = new LinkedHashMap<MethodInvocation, MethodDeclaration>();
        this.movedMethodName = sourceMethod.getMethodName();
        List<MethodInvocationObject> methodInvocations = sourceMethod.getMethodObject().getMethodInvocations();
        for(MethodInvocationObject methodInvocation : methodInvocations) {
        	if(methodInvocation.getOriginClassName().equals(sourceClass.getClassObject().getName()) &&
        			!sourceClass.getClassObject().containsMethodInvocation(methodInvocation, sourceMethod.getMethodObject()) &&
        			!system.getSystemObject().containsMethodInvocation(methodInvocation, sourceClass.getClassObject())) {
        		MethodObject invokedMethod = sourceClass.getClassObject().getMethod(methodInvocation);
        		boolean systemMemberAccessed = false;
        		for(MethodInvocationObject methodInvocationObject : invokedMethod.getMethodInvocations()) {
        			if(system.getSystemObject().getClassObject(methodInvocationObject.getOriginClassName()) != null) {
        				systemMemberAccessed = true;
        				break;
        			}
        		}
        		if(!systemMemberAccessed) {
        			for(FieldInstructionObject fieldInstructionObject : invokedMethod.getFieldInstructions()) {
        				if(system.getSystemObject().getClassObject(fieldInstructionObject.getOwnerClass()) != null) {
        					systemMemberAccessed = true;
        					break;
        				}
        			}
        		}
        		if(!systemMemberAccessed && !additionalMethodsToBeMoved.containsKey(methodInvocation.getMethodInvocation()))
        			additionalMethodsToBeMoved.put(methodInvocation.getMethodInvocation(), invokedMethod.getMethodDeclaration());
        	}
        }
        this.changedEntities = new LinkedHashSet<Entity>();
        this.changedClasses = new LinkedHashSet<MyClass>();
        this.oldMovedMethods = new LinkedHashSet<MyMethod>();
        this.newMovedMethods = new LinkedHashSet<MyMethod>();
    }

	public void apply() {
    	virtualApplication(system);
    	//FastDistanceMatrix fastDistanceMatrix = new FastDistanceMatrix(system, originalDistanceMatrix, this, oldMovedMethods, newMovedMethods);
    	MoveMethodFastDistanceMatrix fastDistanceMatrix = new MoveMethodFastDistanceMatrix(system, originalDistanceMatrix, this);
    	double fastEntityPlacement = fastDistanceMatrix.getSystemEntityPlacementValue();
    	this.entityPlacement = fastEntityPlacement;
    	for(Entity entity : changedEntities) {
    		entity.resetNewEntitySet();
    	}
    	for(MyClass myClass : changedClasses)
    		myClass.resetNewEntitySet();
    }

    public boolean isApplicable() {
    	if(!isSynchronized() && !containsSuperMethodInvocation() && !overridesMethod() && !containsFieldAssignment() && !isTargetClassAnInterface() &&
    			validTargetObject() && !oneToManyRelationshipWithTargetClass() && !containsAssignmentToTargetClassVariable())
    		return true;
    	else
    		return false;
    }

    public boolean leaveDelegate() {
		return system.getSystemObject().containsMethodInvocation(getSourceMethod().getMethodObject().generateMethodInvocation(), getSourceClass().getClassObject()) ||
		system.getSystemObject().containsSuperMethodInvocation(getSourceMethod().getMethodObject().generateSuperMethodInvocation());
    }

    private boolean isTargetClassAnInterface() {
    	if(targetClass.getClassObject().isInterface()) {
    		//System.out.println(this.toString() + "\tTarget class is an interface");
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    private boolean validTargetObject() {
    	if(sourceMethod.getMethodObject().validTargetObject(sourceClass.getClassObject(), targetClass.getClassObject())) {
    		return true;
    	}
    	else {
    		//System.out.println(this.toString() + "\tdoes not contain a valid target object");
    		return false;
    	}
    }

    private boolean oneToManyRelationshipWithTargetClass() {
    	if(sourceMethod.getMethodObject().oneToManyRelationshipWithTargetClass(system.getAssociationsOfClass(sourceClass.getClassObject()), targetClass.getClassObject())) {
    		//System.out.println(this.toString() + "\thas one-to-many relationship with target class");
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    private boolean overridesMethod() {
    	if(sourceMethod.getMethodObject().overridesMethod()) {
    		//System.out.println(this.toString() + "\toverrides method of superclass");
    		return true;
    	}
    	else
    		return false;
    }

    private boolean containsFieldAssignment() {
    	if(!sourceMethod.getMethodObject().getDefinedFieldsThroughThisReference().isEmpty()) {
    		//System.out.println(this.toString() + "\tcontains field assignment");
    		return true;
    	}
    	else
    		return false;
    }
    
    private boolean containsAssignmentToTargetClassVariable() {
    	Set<PlainVariable> definedVariables = sourceMethod.getMethodObject().getDefinedLocalVariables();
    	for(PlainVariable variable : definedVariables) {
    		if(variable.isParameter() && variable.getVariableType().equals(targetClass.getName()))
    			return true;
    	}
    	return false;
    }

    private boolean containsSuperMethodInvocation() {
    	if(sourceMethod.getMethodObject().containsSuperMethodInvocation() || sourceMethod.getMethodObject().containsSuperFieldAccess()) {
    		//System.out.println(this.toString() + "\tcontains super method invocation");
    		return true;
    	}
    	else
    		return false;
    }

    private boolean isSynchronized() {
    	if(sourceMethod.getMethodObject().isSynchronized()) {
    		//System.out.println(this.toString() + "\tis synchronized");
    		return true;
    	}
    	else
    		return false;
    }

    private void virtualApplication(MySystem virtualSystem) {
    	MyMethod oldMethod = sourceMethod;
        MyMethod newMethod = MyMethod.newInstance(oldMethod);
        newMethod.setClassOrigin(targetClass.getName());
        newMethod.removeParameter(targetClass.getName());

        ListIterator<MyAttributeInstruction> instructionIterator = newMethod.getAttributeInstructionIterator();
        List<MyAttributeInstruction> instructionsToBeRemoved = new ArrayList<MyAttributeInstruction>();
        while(instructionIterator.hasNext()) {
            MyAttributeInstruction instruction = instructionIterator.next();
            if(instruction.getClassOrigin().equals(oldMethod.getClassOrigin())) {
            	if(!instruction.getClassType().equals(targetClass.getName()))
            		newMethod.addParameter(instruction.getClassType());
                instructionsToBeRemoved.add(instruction);
                MyClass virtualSourceClass = virtualSystem.getClass(sourceClass.getName());
                ListIterator<MyMethod> sourceMethodIterator = virtualSourceClass.getMethodIterator();
                MyMethodInvocation oldMethodInvocation = oldMethod.generateMethodInvocation();
                while(sourceMethodIterator.hasNext()) {
                	MyMethod myMethod = sourceMethodIterator.next();
                	//if a method of the source class invokes the method to be moved,
                	//the fields accessed by the method to be moved should be passed as arguments to the modified method invocation of the moved method
                	if(myMethod.containsMethodInvocation(oldMethodInvocation)) {
                		MyAttribute mySourceAttribute = virtualSourceClass.getAttribute(instruction);
                		mySourceAttribute.initializeNewEntitySet();
                		changedEntities.add(mySourceAttribute);
                		mySourceAttribute.addMethod(myMethod);
                		myMethod.initializeNewEntitySet();
                		changedEntities.add(myMethod);
                		myMethod.addAttributeInstructionInStatementsOrExpressionsContainingMethodInvocation(instruction, oldMethodInvocation);
                	}
                }
            }
        }
        for(MyAttributeInstruction instruction : instructionsToBeRemoved) {
        	newMethod.removeAttributeInstruction(instruction);
        }
        ListIterator<MyMethodInvocation> invocationIterator = newMethod.getMethodInvocationIterator();
        while(invocationIterator.hasNext()) {
            MyMethodInvocation invocation = invocationIterator.next();
            if(invocation.getClassOrigin().equals(oldMethod.getClassOrigin()) && !belongsToAdditionalMethodsToBeMoved(invocation)) {
                newMethod.addParameter(invocation.getClassOrigin());
                break;
            }
        }
        
        if(virtualSystem.getClass(targetClass.getName()).getMethod(newMethod) != null) {
        	String sourceClassName = null;
        	if(sourceClass.getName().contains("."))
        		sourceClassName = sourceClass.getName().substring(sourceClass.getName().lastIndexOf(".")+1, sourceClass.getName().length());
        	else
        		sourceClassName = sourceClass.getName();
        	movedMethodName = newMethod.getMethodName() + sourceClassName;
        	newMethod.setMethodName(movedMethodName);
        }
        
        MyMethodInvocation oldMethodInvocation = oldMethod.generateMethodInvocation();
        MyMethodInvocation newMethodInvocation = newMethod.generateMethodInvocation();
        MyClass mySourceClass = virtualSystem.getClass(sourceClass.getName());
        mySourceClass.initializeNewEntitySet();
        changedClasses.add(mySourceClass);
        mySourceClass.removeMethod(oldMethod);
        
        MyClass myTargetClass = virtualSystem.getClass(targetClass.getName());
        myTargetClass.initializeNewEntitySet();
        changedClasses.add(myTargetClass);
        myTargetClass.addMethod(newMethod);
        
        Iterator<MyClass> classIterator = virtualSystem.getClassIterator();
        while(classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while(attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                if(attribute.containsMethod(oldMethod)) {
                	attribute.initializeNewEntitySet();
                	changedEntities.add(attribute);
	                if(attribute.getClassOrigin().equals(sourceClass.getName()))
	                	attribute.removeMethod(oldMethod);
	                attribute.replaceMethod(oldMethod,newMethod);
                }
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod myMethod = methodIterator.next();
                if(myMethod.containsMethodInvocation(oldMethodInvocation)) {
                	myMethod.initializeNewEntitySet();
                	changedEntities.add(myMethod);
                	myMethod.replaceMethodInvocation(oldMethodInvocation,newMethodInvocation);
                }
            }
        }
        oldMovedMethods.add(oldMethod);
        newMovedMethods.add(newMethod);
        
        List<MyMethod> methodsToBeMoved = new ArrayList<MyMethod>();
        Collection<MethodDeclaration> methodDeclarationsToBeMoved = additionalMethodsToBeMoved.values();
        ListIterator<MyMethod> sourceClassMethodIterator = sourceClass.getMethodIterator();
        if(!methodDeclarationsToBeMoved.isEmpty()) {
	        while(sourceClassMethodIterator.hasNext()) {
	        	MyMethod sourceMethod = sourceClassMethodIterator.next();
	        	if(methodDeclarationsToBeMoved.contains(sourceMethod.getMethodObject().getMethodDeclaration()) && !methodsToBeMoved.contains(sourceMethod))
	        		methodsToBeMoved.add(sourceMethod);
	        }
        }
        for(MyMethod oldMyMethod : methodsToBeMoved) {
        	MyMethod newMyMethod = MyMethod.newInstance(oldMyMethod);
        	newMyMethod.setClassOrigin(targetClass.getName());
        	MyMethodInvocation oldMyMethodInvocation = oldMyMethod.generateMethodInvocation();
        	MyMethodInvocation newMyMethodInvocation = newMyMethod.generateMethodInvocation();
        	//virtualSystem.getClass(sourceClass.getName()).removeMethod(oldMyMethod);
            //virtualSystem.getClass(targetClass.getName()).addMethod(newMyMethod);
        	mySourceClass.removeMethod(oldMyMethod);
        	myTargetClass.addMethod(newMyMethod);
            newMethod.replaceMethodInvocation(oldMyMethodInvocation, newMyMethodInvocation);
            oldMovedMethods.add(oldMyMethod);
            newMovedMethods.add(newMyMethod);
        }
    }

    private boolean belongsToAdditionalMethodsToBeMoved(MyMethodInvocation methodInvocation) {
    	for(MethodDeclaration methodDeclaration : additionalMethodsToBeMoved.values()) {
    		if(methodDeclaration.getName().getIdentifier().equals(methodInvocation.getMethodName())) {
    			List<SingleVariableDeclaration> methodDeclarationParameters = methodDeclaration.parameters();
    			List<String> methodInvocationTypeParameters = methodInvocation.getParameterList();
    			if(methodDeclarationParameters.size() == methodInvocationTypeParameters.size()) {
    				int numberOfSameTypeParameters = 0;
    				for(int i=0; i<methodDeclarationParameters.size(); i++) {
    					if(methodDeclarationParameters.get(i).getType().resolveBinding().getQualifiedName().equals(methodInvocationTypeParameters.get(i)))
    						numberOfSameTypeParameters++;
    				}
    				if(numberOfSameTypeParameters == methodDeclarationParameters.size())
    					return true;
    			}
    		}
    	}
    	return false;
    }

    public TypeDeclaration getSourceClassTypeDeclaration() {
        return sourceClass.getClassObject().getTypeDeclaration();
    }

    public TypeDeclaration getTargetClassTypeDeclaration() {
        return targetClass.getClassObject().getTypeDeclaration();
    }

	public IFile getSourceIFile() {
		return sourceClass.getClassObject().getIFile();
	}

	public IFile getTargetIFile() {
		return targetClass.getClassObject().getIFile();
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

    public Map<MethodInvocation, MethodDeclaration> getAdditionalMethodsToBeMoved() {
    	return additionalMethodsToBeMoved;
    }

    public String getMovedMethodName() {
		return movedMethodName;
	}

	public void setMovedMethodName(String movedMethodName) {
		this.movedMethodName = movedMethodName;
	}

    public Set<Entity> getChangedEntities() {
		return changedEntities;
	}

	public Set<MyClass> getChangedClasses() {
		return changedClasses;
	}

	public Set<MyMethod> getOldMovedMethods() {
		return oldMovedMethods;
	}

	public Set<MyMethod> getNewMovedMethods() {
		return newMovedMethods;
	}

	public String toString() {
        return getSourceEntity() + "->" + getTarget();
    }

	public String getSourceEntity() {
		StringBuilder sb = new StringBuilder();
        sb.append(sourceMethod.getClassOrigin()).append("::");
        sb.append(movedMethodName);
        List<String> parameterList = sourceMethod.getParameterList();
        sb.append("(");
        if(!parameterList.isEmpty()) {
            for(int i=0; i<parameterList.size()-1; i++)
                sb.append(parameterList.get(i)).append(", ");
            sb.append(parameterList.get(parameterList.size()-1));
        }
        sb.append(")");
        if(sourceMethod.getReturnType() != null)
            sb.append(":").append(sourceMethod.getReturnType());
        return sb.toString();
	}

	public String getSource() {
		return sourceClass.getName();
	}

	public String getTarget() {
		return targetClass.getName();
	}

	public double getEntityPlacement() {
		return entityPlacement;
	}

	public Set<String> getEntitySet() {
		return sourceMethod.getEntitySet();
	}

	public List<Position> getPositions() {
		ArrayList<Position> positions = new ArrayList<Position>();
		Position position = new Position(getSourceMethodDeclaration().getStartPosition(), getSourceMethodDeclaration().getLength());
		positions.add(position);
		return positions;
	}

	public int getNumberOfDistinctEnviedElements() {
		int counter = 0;
		for(String entity : getEntitySet()) {
			String[] tokens = entity.split("::");
			String classOrigin = tokens[0];
			String entityName = tokens[1];
			if(classOrigin.equals(targetClass.getName()))
				counter++;
		}
		return counter;
	}

	public Integer getUserRate() {
		return userRate;
	}

	public void setUserRate(Integer userRate) {
		this.userRate = userRate;
	}

	public int compareTo(MoveMethodCandidateRefactoring other) {
		return Double.compare(this.entityPlacement, other.entityPlacement);
	}
}
