package gr.uom.java.distance;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.visualization.FeatureEnvyVisualizationData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.Position;

public class MoveMethodCandidateRefactoring extends CandidateRefactoring implements Comparable<MoveMethodCandidateRefactoring> {
    private MySystem system;
	private MyClass sourceClass;
    private MyClass targetClass;
    private MyMethod sourceMethod;
    //contains source class methods that do not access any field or method and are accessed only by sourceMethod
    private Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved;
    private String movedMethodName;
    private FeatureEnvyVisualizationData visualizationData;
    private Integer userRate;

    public MoveMethodCandidateRefactoring(MySystem system, MyClass sourceClass, MyClass targetClass, MyMethod sourceMethod) {
        this.system = system;
    	this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.sourceMethod = sourceMethod;
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
        this.visualizationData = new FeatureEnvyVisualizationData(sourceClass.getClassObject(),
				sourceMethod.getMethodObject(), targetClass.getClassObject());
    }

    public boolean isApplicable() {
    	if(!isSynchronized() && !containsSuperMethodInvocation() && !overridesMethod() && !containsFieldAssignment() && !isTargetClassAnInterface() &&
    			validTargetObject() && !oneToManyRelationshipWithTargetClass() && !containsAssignmentToTargetClassVariable() &&
    			!containsMethodCallWithThisExpressionAsArgument() && !isTargetClassAnEnum() && !isSourceClassATestClass() && !targetClassContainsMethodWithSourceMethodSignature() &&
    			!containsNullCheckForTargetObject())
    		return true;
    	else
    		return false;
    }

    public boolean leaveDelegate() {
		return system.getSystemObject().containsMethodInvocation(getSourceMethod().getMethodObject().generateMethodInvocation(), getSourceClass().getClassObject()) ||
		system.getSystemObject().containsSuperMethodInvocation(getSourceMethod().getMethodObject().generateSuperMethodInvocation());
    }

    private boolean targetClassContainsMethodWithSourceMethodSignature() {
    	MethodObject sourceMethod = this.sourceMethod.getMethodObject();
    	for(MethodObject targetMethod : targetClass.getClassObject().getMethodList()) {
    		if(targetMethod.getName().equals(sourceMethod.getName()) &&
    				targetMethod.getReturnType().equals(sourceMethod.getReturnType())) {
    			if(targetMethod.getParameterTypeList().equals(sourceMethod.getParameterTypeList())) {
    				return true;
    			}
    			else {
    				List<TypeObject> sourceParameterTypeListWithoutTargetType = new ArrayList<TypeObject>();
    				for(TypeObject type : sourceMethod.getParameterTypeList()) {
    					if(!type.getClassType().equals(targetClass.getName())) {
    						sourceParameterTypeListWithoutTargetType.add(type);
    					}
    				}
    				if(targetMethod.getParameterTypeList().equals(sourceParameterTypeListWithoutTargetType)) {
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }

    private boolean isSourceClassATestClass() {
    	return sourceClass.getClassObject().containsMethodWithTestAnnotation() || sourceClass.getClassObject().extendsTestCase();
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

    private boolean isTargetClassAnEnum() {
    	if(targetClass.getClassObject().isEnum()) {
    		//System.out.println(this.toString() + "\tTarget class is an enum");
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

    private boolean containsMethodCallWithThisExpressionAsArgument() {
    	if(sourceMethod.getMethodObject().containsMethodCallWithThisExpressionAsArgument()) {
    		return true;
    	}
    	else
    		return false;
    }

    private boolean containsNullCheckForTargetObject() {
    	if(sourceMethod.getMethodObject().containsNullCheckForTargetObject(targetClass.getClassObject())) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    public TypeDeclaration getSourceClassTypeDeclaration() {
        return (TypeDeclaration)sourceClass.getClassObject().getAbstractTypeDeclaration();
    }

    public TypeDeclaration getTargetClassTypeDeclaration() {
        return (TypeDeclaration)targetClass.getClassObject().getAbstractTypeDeclaration();
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

	public Set<String> getEntitySet() {
		return sourceMethod.getEntitySet();
	}

	public List<Position> getPositions() {
		ArrayList<Position> positions = new ArrayList<Position>();
		Position position = new Position(getSourceMethodDeclaration().getStartPosition(), getSourceMethodDeclaration().getLength());
		positions.add(position);
		return positions;
	}

	public String getAnnotationText() {
		return visualizationData.toString();
	}

	public FeatureEnvyVisualizationData getFeatureEnvyVisualizationData() {
		return visualizationData;
	}

	public int getNumberOfDistinctEnviedElements() {
		int counter = 0;
		for(String entity : getEntitySet()) {
			String[] tokens = entity.split("::");
			String classOrigin = tokens[0];
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
		int thisSourceClassDependencies = this.getDistinctSourceDependencies();
		int otherSourceClassDependencies = other.getDistinctSourceDependencies();
		if(thisSourceClassDependencies != otherSourceClassDependencies) {
			return Integer.compare(thisSourceClassDependencies, otherSourceClassDependencies);
		}
		else {
			int thisTargetClassDependencies = this.getDistinctTargetDependencies();
			int otherTargetClassDependencies = other.getDistinctTargetDependencies();
			if(thisTargetClassDependencies != otherTargetClassDependencies) {
				return -Integer.compare(thisTargetClassDependencies, otherTargetClassDependencies);
			}
			else {
				return this.sourceClass.getName().compareTo(other.sourceClass.getName());
			}
		}
	}

	public int getDistinctSourceDependencies() {
		return getFeatureEnvyVisualizationData().getDistinctSourceDependencies();
	}

	public int getDistinctTargetDependencies() {
		return getFeatureEnvyVisualizationData().getDistinctTargetDependencies();
	}
}
