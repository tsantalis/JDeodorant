package gr.uom.java.distance;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public abstract class MyAbstractStatement {
	
	private AbstractStatement statement;
	private MyCompositeStatement parent;
	private List<MyMethodInvocation> methodInvocationList;
    private List<MyAttributeInstruction> attributeInstructionList;
    
    public MyAbstractStatement(AbstractStatement statement) {
    	this.statement = statement;
    	this.parent = null;
    	this.methodInvocationList = new ArrayList<MyMethodInvocation>();
        this.attributeInstructionList = new ArrayList<MyAttributeInstruction>();
        SystemObject system = ASTReader.getSystemObject();
        
        List<FieldInstructionObject> fieldInstructions = statement.getFieldInstructions();
        for(FieldInstructionObject fio : fieldInstructions) {
            if(system.getClassObject(fio.getOwnerClass()) != null && !fio.isStatic()) {
                MyAttributeInstruction myAttributeInstruction = new MyAttributeInstruction(fio.getOwnerClass(),fio.getType().toString(),fio.getName());
                
                if(!attributeInstructionList.contains(myAttributeInstruction))
                	attributeInstructionList.add(myAttributeInstruction);
            }
        }

        List<MethodInvocationObject> methodInvocations = statement.getMethodInvocations();
        for(MethodInvocationObject mio : methodInvocations) {
            if(system.getClassObject(mio.getOriginClassName()) != null && !mio.isStatic()) {
            	MethodInvocationObject methodInvocation;
            	if(isAccessor(mio,system)) {	
            	}
            	else if((methodInvocation = recurseDelegations(mio,system)) != null) {
            		if(isAccessor(methodInvocation,system)) {
            		}
            		else {
            			MyMethodInvocation myMethodInvocation = new MyMethodInvocation(methodInvocation.getOriginClassName(),
            				methodInvocation.getMethodName(),methodInvocation.getReturnType().toString(),methodInvocation.getParameterList());            		
            			if(!methodInvocationList.contains(myMethodInvocation))
            				methodInvocationList.add(myMethodInvocation);
            		}
            	}
            }
        }
    }
    
    private boolean isAccessor(MethodInvocationObject methodInvocation, SystemObject system) {
    	FieldInstructionObject fieldInstruction = null;
    	if((fieldInstruction = system.containsGetter(methodInvocation)) != null) {
    	}
    	else if((fieldInstruction = system.containsSetter(methodInvocation)) != null) {
    	}
    	else if((fieldInstruction = system.containsCollectionAdder(methodInvocation)) != null) {
    	}
    	
    	if(fieldInstruction != null && system.getClassObject(fieldInstruction.getOwnerClass()) != null) {
    		MyAttributeInstruction myAttributeInstruction = 
    			new MyAttributeInstruction(fieldInstruction.getOwnerClass(),fieldInstruction.getType().toString(),fieldInstruction.getName());
            
            if(!attributeInstructionList.contains(myAttributeInstruction))
            	attributeInstructionList.add(myAttributeInstruction);
            return true;
    	}
    	return false;
    }
    
    private MethodInvocationObject recurseDelegations(MethodInvocationObject methodInvocation, SystemObject system) {
    	MethodInvocationObject delegation;
    	if((delegation = system.containsDelegate(methodInvocation)) != null && system.getClassObject(delegation.getOriginClassName()) != null && !delegation.equals(methodInvocation) &&
    			!delegationLoop(methodInvocation, delegation, system))
    		return recurseDelegations(delegation, system);
    	else
    		return methodInvocation;
    }
    
    private boolean delegationLoop(MethodInvocationObject methodInvocation, MethodInvocationObject delegation, SystemObject system) {
    	MethodInvocationObject delegation2;
    	if((delegation2 = system.containsDelegate(delegation)) != null && delegation2.equals(methodInvocation)) {
    		return true;
    	}
    	return false;
    }
    
    public MyAbstractStatement(List<MyAbstractStatement> statementList) {
    	this.statement = null;
    	this.parent = null;
    	this.methodInvocationList = new ArrayList<MyMethodInvocation>();
        this.attributeInstructionList = new ArrayList<MyAttributeInstruction>();
        for(MyAbstractStatement myAbstractStatement : statementList) {
        	methodInvocationList.addAll(myAbstractStatement.methodInvocationList);
        	attributeInstructionList.addAll(myAbstractStatement.attributeInstructionList);
        }
    }

    public MyAbstractStatement(MyMethodInvocation methodInvocation) {
    	this.statement = null;
    	this.parent = null;
    	this.methodInvocationList = new ArrayList<MyMethodInvocation>();
        this.attributeInstructionList = new ArrayList<MyAttributeInstruction>();
        this.methodInvocationList.add(methodInvocation);
    }

    protected MyAbstractStatement() {
    	this.parent = null;
    	this.methodInvocationList = new ArrayList<MyMethodInvocation>();
        this.attributeInstructionList = new ArrayList<MyAttributeInstruction>();
    }

    public void setMethodInvocationList(List<MyMethodInvocation> list) {
    	this.methodInvocationList = list;
    }

    public void setAttributeInstructionList(List<MyAttributeInstruction> list) {
    	this.attributeInstructionList = list;
    }

    public boolean containsAttributeInstruction(MyAttributeInstruction instruction) {
    	return attributeInstructionList.contains(instruction);
    }

    public boolean containsMethodInvocation(MyMethodInvocation invocation) {
    	return methodInvocationList.contains(invocation);
    }

    public void addMethodInvocation(MyMethodInvocation myMethodInvocation) {
    	if(!methodInvocationList.contains(myMethodInvocation))
        	methodInvocationList.add(myMethodInvocation);
    }

    public void addAttributeInstruction(MyAttributeInstruction myAttributeInstruction) {
    	if(!attributeInstructionList.contains(myAttributeInstruction))
        	attributeInstructionList.add(myAttributeInstruction);
    }

    public int getNumberOfAttributeInstructions() {
        return this.attributeInstructionList.size();
    }

    public int getNumberOfMethodInvocations() {
        return this.methodInvocationList.size();
    }

    public ListIterator<MyMethodInvocation> getMethodInvocationIterator() {
        return methodInvocationList.listIterator();
    }

    public List<MyMethodInvocation> getMethodInvocationList() {
		return methodInvocationList;
	}

    public ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
        return attributeInstructionList.listIterator();
    }

    public List<MyAttributeInstruction> getAttributeInstructionList() {
		return attributeInstructionList;
	}

    public void setParent(MyCompositeStatement parent) {
    	this.parent = parent;
    }

    public MyCompositeStatement getParent() {
    	return this.parent;
    }

    public AbstractStatement getStatement() {
    	return this.statement;
    }

    public void setStatement(AbstractStatement statement) {
		this.statement = statement;
	}

	public String toString() {
    	return this.statement.toString();
    }

    public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
    	int index = attributeInstructionList.indexOf(myAttributeInstruction);
    	if(index != -1) {
    		MyAttributeInstruction attributeInstruction = attributeInstructionList.get(index);
    		attributeInstruction.setReference(reference);
    	}
    }

    public MyAbstractStatement getAbstractStatement(AbstractStatement statement) {
    	if(this.statement.equals(statement))
    		return this;
    	else
    		return null;
    }

    public Set<String> getEntitySet() {
        Set<String> set = new HashSet<String>();
        ListIterator<MyAttributeInstruction> attributeInstructionIterator = getAttributeInstructionIterator();
        while(attributeInstructionIterator.hasNext()) {
        	MyAttributeInstruction attributeInstruction = attributeInstructionIterator.next();
            if(!attributeInstruction.isReference())
                set.add(attributeInstruction.toString());
        }
        ListIterator<MyMethodInvocation> methodInvocationIterator = getMethodInvocationIterator();
        while(methodInvocationIterator.hasNext()) {
        	MyMethodInvocation methodInvocation = methodInvocationIterator.next();
            set.add(methodInvocation.toString());
        }
        return set;
    }

    public boolean equals(Object o) {
    	if(this == o)
    		return true;
    	
    	if(o instanceof MyAbstractStatement) {
    		MyAbstractStatement myAbstractStatement = (MyAbstractStatement)o;
    		return this.statement.equals(myAbstractStatement.statement);
    	}
    	return false;
    }

    public int hashCode() {
    	return statement.hashCode();
    }
}
