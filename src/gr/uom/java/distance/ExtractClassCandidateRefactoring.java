package gr.uom.java.distance;

import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.util.TopicFinder;
import gr.uom.java.ast.visualization.GodClassVisualizationData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.Position;

public class ExtractClassCandidateRefactoring extends CandidateRefactoring implements Comparable<ExtractClassCandidateRefactoring> {

	private MySystem system;
	private MyClass sourceClass;
	private List<Entity> extractedEntities;
	private Map<MyMethod, Boolean> leaveDelegate;
	private String targetClassName;
	private GodClassVisualizationData visualizationData;
	private Integer userRate;
	private List<String> topics;

	public ExtractClassCandidateRefactoring(MySystem system, MyClass sourceClass) {
		super();
		this.system = system;
		this.sourceClass = sourceClass;
		this.extractedEntities = new ArrayList<Entity>();
		this.leaveDelegate = new LinkedHashMap<MyMethod, Boolean>();
		if (system.getClass(sourceClass.getName() + "Product") == null) {
			this.targetClassName = sourceClass.getName() + "Product";
		}
		else {
			this.targetClassName = sourceClass.getName() + "Product2";
		}
		this.topics = new ArrayList<String>();
	}

	public String getTargetClassName() {
		return targetClassName;
	}

	public void setTargetClassName(String targetClassName) {
		this.targetClassName = targetClassName;
	}

	public void addEntity(Entity entity) {
		if(!extractedEntities.contains(entity))
			extractedEntities.add(entity);
	}

	public List<Entity> getExtractedEntities() {
		return extractedEntities;
	}

	public Set<MethodDeclaration> getExtractedMethods() {
		Set<MethodDeclaration> extractedMethods = new LinkedHashSet<MethodDeclaration>();
		for(Entity entity : extractedEntities) {
			if(entity instanceof MyMethod) {
				MyMethod method = (MyMethod)entity;
				extractedMethods.add(method.getMethodObject().getMethodDeclaration());
			}
		}
		return extractedMethods;
	}

	public Set<MethodDeclaration> getDelegateMethods() {
		Set<MethodDeclaration> delegateMethods = new LinkedHashSet<MethodDeclaration>();
		for(MyMethod method : leaveDelegate.keySet()) {
			if(leaveDelegate.get(method) == true)
				delegateMethods.add(method.getMethodObject().getMethodDeclaration());
		}
		return delegateMethods;
	}

	public Set<VariableDeclaration> getExtractedFieldFragments() {
		Set<VariableDeclaration> extractedFieldFragments = new LinkedHashSet<VariableDeclaration>();
		for(Entity entity : extractedEntities) {
			if(entity instanceof MyAttribute) {
				MyAttribute attribute = (MyAttribute)entity;
				extractedFieldFragments.add(attribute.getFieldObject().getVariableDeclaration());
			}
		}
		return extractedFieldFragments;
	}

	public double[][] getJaccardDistanceMatrix() {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		entities.addAll(sourceClass.getAttributeList());
		entities.addAll(sourceClass.getMethodList());
		double[][] jaccardDistanceMatrix = new double[entities.size()][entities.size()];
		for(int i=0; i<jaccardDistanceMatrix.length; i++) {
			for(int j=0; j<jaccardDistanceMatrix.length; j++) {
				if(i != j) {
					jaccardDistanceMatrix[i][j] = DistanceCalculator.getDistance(entities.get(i).getFullEntitySet(), entities.get(j).getFullEntitySet());
				}
				else {
					jaccardDistanceMatrix[i][j] = 0.0;
				}
			}
		}
		return jaccardDistanceMatrix;
	}

	public Map<MyMethod, Boolean> getLeaveDelegate() {
		return leaveDelegate;
	}

	public boolean leaveDelegate(MyMethod method) {
		return system.getSystemObject().containsMethodInvocation(method.getMethodObject().generateMethodInvocation(), sourceClass.getClassObject()) ||
		system.getSystemObject().containsSuperMethodInvocation(method.getMethodObject().generateSuperMethodInvocation());
	}

	public boolean isApplicable() {
		int methodCounter = 0;
		for (Entity entity : extractedEntities) {
			if(entity instanceof MyMethod) {
				MyMethod method = (MyMethod)entity;
				methodCounter++;
				if (isSynchronized(method) || containsSuperMethodInvocation(method) ||
						overridesMethod(method) || method.isAbstract() || containsFieldAccessOfEnclosingClass(method) ||
						isReadObject(method) || isWriteObject(method))
					return false;
			}
			else if(entity instanceof MyAttribute) {
				MyAttribute attribute = (MyAttribute)entity;
				if(!attribute.getAccess().equals("private")) {
					if(system.getSystemObject().containsFieldInstruction(attribute.getFieldObject().generateFieldInstruction(), sourceClass.getClassObject()))
						return false;
				}
			}
		}
		if(extractedEntities.size() <=2 || methodCounter == 0 || !validRemainingMembersInSourceClass()) {
			return false;
		}
		else {
			return true;
		}
	}

	private boolean validRemainingMembersInSourceClass() {
		for(MyMethod sourceMethod : sourceClass.getMethodList()) {
			if(!extractedEntities.contains(sourceMethod)) {
				MethodObject methodObject = sourceMethod.getMethodObject();
				if(!methodObject.isStatic() && !methodObject.isAbstract() && methodObject.isGetter() == null && methodObject.isSetter() == null && methodObject.isDelegate() == null &&
						!isReadObject(methodObject) && !isWriteObject(methodObject) && !isEquals(methodObject) && !isHashCode(methodObject) && !isClone(methodObject) && !isCompareTo(methodObject)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isReadObject(MyMethod method) {
		return isReadObject(method.getMethodObject());
	}

	private boolean isReadObject(MethodObject methodObject) {
		List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
		return methodObject.getName().equals("readObject") && parameterTypeList.size() == 1 && parameterTypeList.get(0).getClassType().equals("java.io.ObjectInputStream");
	}

	private boolean isWriteObject(MyMethod method) {
		return isWriteObject(method.getMethodObject());
	}

	private boolean isWriteObject(MethodObject methodObject) {
		List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
		return methodObject.getName().equals("writeObject") && parameterTypeList.size() == 1 && parameterTypeList.get(0).getClassType().equals("java.io.ObjectOutputStream");
	}

	private boolean isEquals(MethodObject methodObject) {
		List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
		return methodObject.getName().equals("equals") && methodObject.getReturnType().getClassType().equals("boolean") &&
				parameterTypeList.size() == 1 && parameterTypeList.get(0).getClassType().equals("java.lang.Object");
	}

	private boolean isHashCode(MethodObject methodObject) {
		List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
		return methodObject.getName().equals("hashCode") && methodObject.getReturnType().getClassType().equals("int") && parameterTypeList.size() == 0;
	}

	private boolean isClone(MethodObject methodObject) {
		List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
		return methodObject.getName().equals("clone") && methodObject.getReturnType().getClassType().equals("java.lang.Object") && parameterTypeList.size() == 0;
	}

	private boolean isCompareTo(MethodObject methodObject) {
		List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
		return methodObject.getName().equals("compareTo") && methodObject.getReturnType().getClassType().equals("int") && parameterTypeList.size() == 1;
	}

	private boolean containsFieldAccessOfEnclosingClass(MyMethod method) {
		if(method.getMethodObject().containsFieldAccessOfEnclosingClass()) {
			return true;
		}
		else
			return false;
	}

	private boolean overridesMethod(MyMethod method) {
		if(method.getMethodObject().overridesMethod()) {
			//System.out.println(this.toString() + "\toverrides method of superclass");
			return true;
		}
		else
			return false;
	}

	private boolean containsSuperMethodInvocation(MyMethod method) {
		if(method.getMethodObject().containsSuperMethodInvocation()) {
			//System.out.println(this.toString() + "\tcontains super method invocation");
			return true;
		}
		else
			return false;
	}

	private boolean isSynchronized(MyMethod method) {
		if(method.getMethodObject().isSynchronized()) {
			//System.out.println(this.toString() + "\tis synchronized");
			return true;
		}
		else
			return false;
	}

	@Override
	public Set<String> getEntitySet() {
		return sourceClass.getEntitySet();
	}
	@Override
	public List<Position> getPositions() {
		ArrayList<Position> positions = new ArrayList<Position>();
		for(Entity entity : extractedEntities) {
			if(entity instanceof MyMethod) {
				MyMethod method = (MyMethod)entity;
				Position position = new Position(method.getMethodObject().getMethodDeclaration().getStartPosition(), method.getMethodObject().getMethodDeclaration().getLength());
				positions.add(position);
			} else if(entity instanceof MyAttribute) {
				MyAttribute attribute = (MyAttribute)entity;
				VariableDeclarationFragment fragment = attribute.getFieldObject().getVariableDeclarationFragment();
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				Position position = null;
				if(fieldDeclaration.fragments().size() > 1) {
					position = new Position(fragment.getStartPosition(), fragment.getLength());
				}
				else {
					position = new Position(fieldDeclaration.getStartPosition(), fieldDeclaration.getLength());
				}
				positions.add(position);
			}
		}
		return positions;
	}

	@Override
	public String getSource() {
		return sourceClass.getName();
	}

	@Override
	public TypeDeclaration getSourceClassTypeDeclaration() {
		return (TypeDeclaration)sourceClass.getClassObject().getAbstractTypeDeclaration();
	}

	@Override
	public String getSourceEntity() {
		return sourceClass.toString();
	}

	@Override
	public String getTarget() {
		return null;
	}

	@Override
	public TypeDeclaration getTargetClassTypeDeclaration() {
		return null;
	}

	public String toString() {
        return sourceClass.toString() + "\t" + extractedEntities.toString();
    }

	public String getAnnotationText() {
		GodClassVisualizationData data = getGodClassVisualizationData();
		return data.toString();
	}

	public GodClassVisualizationData getGodClassVisualizationData() {
		if(visualizationData == null) {
			Set<MethodObject> extractedMethods = new LinkedHashSet<MethodObject>();
			Set<FieldObject> extractedFields = new LinkedHashSet<FieldObject>();
			for(Entity entity : extractedEntities) {
				if(entity instanceof MyMethod) {
					MyMethod myMethod = (MyMethod)entity;
					extractedMethods.add(myMethod.getMethodObject());
				}
				else if(entity instanceof MyAttribute) {
					MyAttribute myAttribute = (MyAttribute)entity;
					extractedFields.add(myAttribute.getFieldObject());
				}
			}
			this.visualizationData = new GodClassVisualizationData(sourceClass.getClassObject(), extractedMethods, extractedFields);
		}
		return visualizationData;
	}

	@Override
	public IFile getSourceIFile() {
		return sourceClass.getClassObject().getIFile();
	}

	@Override
	public IFile getTargetIFile() {
		return null;
	}

	public Integer getUserRate() {
		return userRate;
	}

	public void setUserRate(Integer userRate) {
		this.userRate = userRate;
	}

	public int compareTo(ExtractClassCandidateRefactoring other) {
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

	public void findTopics() {
		List<String> codeElements = new ArrayList<String>();
		for (Entity entity : this.extractedEntities) {
			if (entity instanceof MyAttribute) {
				MyAttribute attribute = (MyAttribute) entity;
				codeElements.add(attribute.getName());
			}
			else if (entity instanceof MyMethod) {
				MyMethod method = (MyMethod) entity;
				codeElements.add(method.getMethodName());
			}
		}
		this.topics = TopicFinder.findTopics(codeElements);
	}

	public List<String> getTopics() {
		return topics;
	}

	public int getDistinctSourceDependencies() {
		return getGodClassVisualizationData().getDistinctSourceDependencies();
	}

	public int getDistinctTargetDependencies() {
		return getGodClassVisualizationData().getDistinctTargetDependencies();
	}
}