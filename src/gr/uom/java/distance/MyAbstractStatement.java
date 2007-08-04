package gr.uom.java.distance;

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
    
    public MyAbstractStatement(AbstractStatement statement, SystemObject system) {
    	this.statement = statement;
    	this.parent = null;
    	this.methodInvocationList = new ArrayList<MyMethodInvocation>();
        this.attributeInstructionList = new ArrayList<MyAttributeInstruction>();
        
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
            if(system.getClassObject(mio.getOriginClassName()) != null) {
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
    	
    	if(fieldInstruction != null) {
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
    	if((delegation = system.containsDelegate(methodInvocation)) != null)
    		return recurseDelegations(delegation, system);
    	else
    		return methodInvocation;
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

    protected MyAbstractStatement(AbstractStatement statement) {
    	this.statement = statement;
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

    public MyMethodInvocation getMethodInvocation(int pos) {
        return methodInvocationList.get(pos);
    }

    public MyAttributeInstruction getAttributeInstruction(int pos) {
        return attributeInstructionList.get(pos);
    }

    public ListIterator<MyMethodInvocation> getMethodInvocationIterator() {
        return methodInvocationList.listIterator();
    }

    public ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
        return attributeInstructionList.listIterator();
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

    public String toString() {
    	return this.statement.toString();
    }

    public void replaceMethodInvocationWithAttributeInstruction(MyMethodInvocation methodInvocation, MyAttributeInstruction attributeInstruction) {
    	if(methodInvocationList.contains(methodInvocation)) {
    		methodInvocationList.remove(methodInvocation);
    		if(!attributeInstructionList.contains(attributeInstruction))
    			attributeInstructionList.add(attributeInstruction);
    	}
    }

    public void replaceMethodInvocation(MyMethodInvocation oldMethodInvocation, MyMethodInvocation newMethodInvocation) {
        if(methodInvocationList.contains(oldMethodInvocation)) {
            int index = methodInvocationList.indexOf(oldMethodInvocation);
            methodInvocationList.remove(index);
            methodInvocationList.add(index,newMethodInvocation);
        }
    }

    public void replaceAttributeInstruction(MyAttributeInstruction oldInstruction, MyAttributeInstruction newInstruction) {
        if(attributeInstructionList.contains(oldInstruction)) {
            int index = attributeInstructionList.indexOf(oldInstruction);
            attributeInstructionList.remove(index);
            attributeInstructionList.add(index,newInstruction);
        }
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

    public Set<String> getEntitySet(AbstractStatement statement) {
    	if(this.statement.equals(statement))
    		return getEntitySet();
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
}
