package gr.uom.java.distance;

import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.TopicFinder;
import gr.uom.java.ast.util.math.HumaniseCamelCase;
import gr.uom.java.ast.util.math.Stemmer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.Position;

public class ExtractClassCandidateRefactoring extends CandidateRefactoring implements Comparable<ExtractClassCandidateRefactoring>,TopicFinder {

	private MySystem system;
	private MyClass sourceClass;
	private MyClass newSourceClass;
	private MyClass productClass;
	private List<Entity> extractedEntities;
	private double entityPlacement;
	private Map<MyMethod, Boolean> leaveDelegate;
	private String targetClassName;
	private Set<Entity> changedEntities;
	private Set<String> changedClasses;
	private Set<Entity> oldEntities;
	private Set<Entity> newEntities;
	private DistanceMatrix originalDistanceMatrix;
	private Map<String, MyAttribute> oldInstructions;
	private Map<MyMethod, MyMethodInvocation> oldInvocations;
	private Map<MyMethod, MyMethod> new2oldMethods;
	private Map<MyAttribute, String> extractedVariableBindingKeys;
	private Integer userRate;
	private String topic;

	public ExtractClassCandidateRefactoring(MySystem system, MyClass sourceClass, DistanceMatrix originalDistanceMatrix) {
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

		this.changedEntities = new LinkedHashSet<Entity>();
		this.changedClasses = new LinkedHashSet<String>();
		this.oldEntities = new LinkedHashSet<Entity>();
		this.newEntities = new LinkedHashSet<Entity>();
		this.originalDistanceMatrix = originalDistanceMatrix;
		this.oldInstructions = new LinkedHashMap<String, MyAttribute>();
		this.oldInvocations = new LinkedHashMap<MyMethod, MyMethodInvocation>();
		this.new2oldMethods = new LinkedHashMap<MyMethod, MyMethod>();
		this.extractedVariableBindingKeys = new LinkedHashMap<MyAttribute, String>();
	}

	public MyClass getProductClass2() {
		return productClass;
	}

	public Set<Entity> getChangedEntities() {
		return changedEntities;
	}

	public Set<String> getChangedClasses() {
		return changedClasses;
	}

	public Set<Entity> getOldEntities() {
		return oldEntities;
	}

	public Set<Entity> getNewEntities() {
		return newEntities;
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

	public void apply() {
		virtualApplication(system);
		ExtractClassFastDistanceMatrix fastDistanceMatrix = new ExtractClassFastDistanceMatrix(system, originalDistanceMatrix, this, newSourceClass, productClass);
		double fastEntityPlacement = fastDistanceMatrix.getSystemEntityPlacementValue();
		this.entityPlacement = fastEntityPlacement;
		for(Entity entity : extractedEntities) {
			if(entity instanceof MyMethod) {
				MyMethod method = (MyMethod)entity;
				leaveDelegate.put(method, leaveDelegate(method));
			}
		}
		for(Entity entity : changedEntities) {
			entity.resetNewEntitySet();
		}
		for(String myClass : changedClasses)
			system.getClass(myClass).resetNewEntitySet();
		system.removeClass(productClass);
	}

	private void virtualApplication(MySystem virtualSystem) {
		newSourceClass = virtualSystem.getClass(sourceClass.getName());
		productClass = new MyClass(sourceClass.toString()+"Product");
		virtualSystem.addClass(productClass);
		List<MyMethod> oldMethods = new ArrayList<MyMethod>();
		List<MyAttribute> oldAttributes = new ArrayList<MyAttribute>();
		List<MyMethod> newMethods = new ArrayList<MyMethod>();
		List<MyAttribute> newAttributes = new ArrayList<MyAttribute>();
		/*//debugging
		Iterator<MyClass> classIt = virtualSystem.getClassIterator();
		while(classIt.hasNext()) {
			MyClass aClass = classIt.next();
			//if(true) {
			if ((aClass.getName().equals("CH.ifa.draw.figures.TextFigure") || aClass.getName().equals("CH.ifa.draw.figures.TextFigureSize")) && sourceClass.getName().equals("CH.ifa.draw.applet.DrawApplet")) {
				aClass.setEntityList();
				for (String anEntity : aClass.getEntitySet()) {
					System.out.println("OLD CLASS ENTITY SET "
							+ aClass.toString() + ": " + anEntity);
				}
				Iterator<Entity> entityIt = aClass.getEntityIterator();
				while (entityIt.hasNext()) {
					Entity entity = entityIt.next();
					for (String anEntity : entity.getEntitySet()) {
						System.out.println("OLD ENTITY SET "
								+ aClass.toString() + " -> "
								+ entity.toString() + ": " + anEntity);
					}
				}
			}
		}
		//debugging
		*/		
		newSourceClass.initializeNewEntitySet();
		changedClasses.add(newSourceClass.getName());
		changedClasses.add(productClass.getName());
		for(Entity entity : extractedEntities) {
			if(entity instanceof MyAttribute) {
				MyAttribute attribute = (MyAttribute)entity;
				oldAttributes.add(attribute);
				oldInstructions.put(attribute.toString(), attribute);
				extractedVariableBindingKeys.put(attribute, attribute.getFieldObject().getVariableDeclaration().resolveBinding().getKey());
				MyAttribute newAttribute = MyAttribute.newInstance(attribute);
				newSourceClass.removeAttribute(attribute);				
				newAttribute.setClassOrigin(productClass.getName());
				productClass.addAttribute(newAttribute);
				newAttributes.add(newAttribute);
			}
			else if(entity instanceof MyMethod) {
				MyMethod method = (MyMethod)entity;
				oldMethods.add(method);
				MyMethod newMethod = MyMethod.newInstance(method);
				oldInvocations.put(method, method.generateMethodInvocation());
				newSourceClass.removeMethod(method);				
				newMethod.setClassOrigin(productClass.getName());
				productClass.addMethod(newMethod);
				newMethods.add(newMethod);
				new2oldMethods.put(newMethod, method);
			}
		}
		for(int i=0;i<oldAttributes.size();i++) {
			MyAttribute oldAttribute = oldAttributes.get(i);
			MyAttribute newAttribute = newAttributes.get(i);
			MyAttributeInstruction oldAttributeInstruction = oldAttribute.generateAttributeInstruction();
			MyAttributeInstruction newAttributeInstruction = newAttribute.generateAttributeInstruction();
			Iterator<MyClass> classIterator = virtualSystem.getClassIterator();
			while(classIterator.hasNext()) {
				MyClass myClass = classIterator.next();
				if (!myClass.equals(productClass)) {
					ListIterator<MyMethod> methodIterator = myClass
					.getMethodIterator();
					while (methodIterator.hasNext()) {
						MyMethod myMethod = methodIterator.next();
						if (myMethod
								.containsAttributeInstruction(oldAttributeInstruction) && !oldMethods.contains(myMethod)) {
							myMethod.initializeNewEntitySet();
							changedEntities.add(myMethod);
							myMethod.replaceAttributeInstruction(
									oldAttributeInstruction,
									newAttributeInstruction);
						}
					}
				}
				else {
					for(MyMethod myMethod : newMethods) {
						if (myMethod
								.containsAttributeInstruction(oldAttributeInstruction)) {
							myMethod.initializeNewEntitySet();
							changedEntities.add(myMethod);
							myMethod.replaceAttributeInstruction(
									oldAttributeInstruction,
									newAttributeInstruction);
						}
					}
				}
			}
			oldEntities.add(oldAttribute);
			newEntities.add(newAttribute);
		}

		for(MyMethod aMethod : newMethods) {
			ListIterator<MyMethodInvocation> invocationIterator = aMethod.getMethodInvocationIterator();
			boolean found=false;
			while(invocationIterator.hasNext() && !found) {
				MyMethodInvocation invocation = invocationIterator.next();
				if(invocation.getClassOrigin().equals(newSourceClass.getName())) {
					if(!oldInvocations.containsValue(invocation)) {
						aMethod.addParameter(invocation.getClassOrigin());
						found = true;
					}
				}
			}

			if(containsFieldAssignment(new2oldMethods.get(aMethod))) {
				aMethod.addParameter(newSourceClass.getName());
				found = true;
			}


			ListIterator<MyAttributeInstruction> instructionIterator = aMethod.getAttributeInstructionIterator();
			List<MyAttributeInstruction> instructionsToBeRemoved = new ArrayList<MyAttributeInstruction>();
			while(instructionIterator.hasNext()) {
				MyAttributeInstruction instruction = instructionIterator.next();
				boolean parameterAdded=false;
				if(instruction.getClassOrigin().equals(newSourceClass.getName()) && !oldInstructions.containsKey(instruction.toString())) {
					if(!instruction.getClassType().equals(productClass.getName()) && !found) {
						aMethod.addParameter(instruction.getClassType());
						parameterAdded = true;
					}
					instructionsToBeRemoved.add(instruction);
					ListIterator<MyMethod> sourceMethodIterator = newSourceClass.getMethodIterator();
					while(sourceMethodIterator.hasNext()) {
						MyMethod myMethod = sourceMethodIterator.next();
						if (!oldMethods.contains(myMethod)) {
							MyMethodInvocation myMethodInvocation = new2oldMethods.get(aMethod)
							.generateMethodInvocation();
							if (myMethod
									.containsMethodInvocation(myMethodInvocation) && parameterAdded) {
								MyAttribute mySourceAttribute = null;
								if (newSourceClass
										.getAttribute(instruction) != null) {
									mySourceAttribute = newSourceClass
									.getAttribute(instruction);
								} else if (productClass
										.getAttribute(instruction) != null) {
									mySourceAttribute = productClass
									.getAttribute(instruction);
								}
								if (mySourceAttribute != null) {
									if (mySourceAttribute.getNewEntitySet() == null  && !mySourceAttribute.isReference()) {
										mySourceAttribute
										.initializeNewEntitySet();
										changedEntities
										.add(mySourceAttribute);
										mySourceAttribute
										.addMethod(myMethod);
									}
								}

								myMethod.initializeNewEntitySet();
								changedEntities.add(myMethod);
								myMethod
								.addAttributeInstructionInStatementsOrExpressionsContainingMethodInvocation(
										instruction,
										myMethodInvocation);
							}
						}
					}
				}
			}

			for(MyAttributeInstruction instruction : instructionsToBeRemoved) {
				found = false;
				for(MyAttribute attribute : oldAttributes) {
					if ((instruction.getName().equals(attribute.getName()) && oldInstructions.containsKey(instruction.toString()))) {
						found = true;
					}
				}
				for(MyAttribute sourceAttribute :newSourceClass.getAttributeList()) {
					if(instruction.equals(sourceAttribute.generateAttributeInstruction())) {
						if(sourceAttribute.getNewEntitySet() == null && !sourceAttribute.isReference()) {
							sourceAttribute.initializeNewEntitySet();
							changedEntities.add(sourceAttribute);
						}

						sourceAttribute.removeMethod(new2oldMethods.get(aMethod));
					}
				}
				if(!found) {
					aMethod.removeAttributeInstruction(instruction);
				}
			}
			newEntities.add(aMethod);

		}

		for(int i=0;i<oldMethods.size();i++) {
			MyMethod oldMethod = oldMethods.get(i);
			MyMethod newMethod = newMethods.get(i);
			MyMethodInvocation oldMethodInvocation = oldMethod.generateMethodInvocation();
			MyMethodInvocation newMethodInvocation = newMethod.generateMethodInvocation();
			Iterator<MyClass> classIterator = virtualSystem.getClassIterator();
			while(classIterator.hasNext()) {
				MyClass myClass = classIterator.next();
				if (!myClass.equals(productClass)) {
					ListIterator<MyAttribute> attributeIterator = myClass
					.getAttributeIterator();
					while (attributeIterator.hasNext()) {
						MyAttribute attribute = attributeIterator.next();
						if (attribute.containsMethod(oldMethod) && !oldAttributes.contains(attribute)) {
							attribute.initializeNewEntitySet();
							changedEntities.add(attribute);
							attribute.replaceMethod(oldMethod, newMethod);
						}
					}
					ListIterator<MyMethod> methodIterator = myClass
					.getMethodIterator();
					while (methodIterator.hasNext()) {
						MyMethod myMethod = methodIterator.next();
						if (myMethod
								.containsMethodInvocation(oldMethodInvocation) && !oldMethods.contains(myMethod)) {
							myMethod.initializeNewEntitySet();
							changedEntities.add(myMethod);
							myMethod.replaceMethodInvocation(
									oldMethodInvocation, newMethodInvocation);
						}
					}
				}
				else {
					for(MyAttribute attribute : newAttributes) {
						if (attribute.containsMethod(oldMethod)) {
							attribute.initializeNewEntitySet();
							changedEntities.add(attribute);
							attribute.replaceMethod(oldMethod, newMethod);
						}
					}
					for(MyMethod myMethod : newMethods) {
						if (myMethod
								.containsMethodInvocation(oldMethodInvocation)) {
							myMethod.initializeNewEntitySet();
							changedEntities.add(myMethod);
							myMethod.replaceMethodInvocation(
									oldMethodInvocation, newMethodInvocation);
						}
					}
				}
			}
			oldEntities.add(oldMethod);
		}

		productClass.initializeNewEntitySet();

		/*//debugging
		Iterator<MyClass> classIt2 = virtualSystem.getClassIterator();
		while(classIt2.hasNext()) {
			MyClass aClass = classIt2.next();
			//if(true) {
			if ((aClass.getName().equals("CH.ifa.draw.figures.TextFigure") || aClass.getName().equals("CH.ifa.draw.figures.TextFigureProduct")) && sourceClass.getName().equals("CH.ifa.draw.figures.TextFigureSize")) {
				aClass.setEntityList();
				if (aClass.getNewEntitySet() != null) {
					for (String anEntity : aClass.getNewEntitySet()) {
						System.out.println("*NEW CLASS ENTITY SET "
								+ aClass.toString() + ": " + anEntity);
					}
				}
				else {
					for (String anEntity : aClass.getEntitySet()) {
						System.out.println("NEW CLASS ENTITY SET "
								+ aClass.toString() + ": " + anEntity);
					}
				}
				if (aClass.equals(newSourceClass)) {
					Iterator<Entity> entityIt = aClass.getEntityIterator();
					while (entityIt.hasNext()) {
						Entity entity = entityIt.next();
						if (!(oldMethods.contains(entity) || oldAttributes.contains(entity))) {
							if (entity.getNewEntitySet() != null) {
								for (String anEntity : entity.getNewEntitySet()) {
									System.out.println("*NEW ENTITY SET "
											+ aClass.toString() + " -> "
											+ entity.toString() + ": "
											+ anEntity);
								}
							} else {
								for (String anEntity : entity.getEntitySet()) {
									System.out.println("NEW ENTITY SET "
											+ aClass.toString() + " -> "
											+ entity.toString() + ": "
											+ anEntity);
								}
							}
						}
					}
				}
				else {
					Iterator<Entity> entityIt = aClass.getEntityIterator();
					while (entityIt.hasNext()) {
						Entity entity = entityIt.next();
						if (entity.getNewEntitySet() != null) {
							for (String anEntity : entity.getNewEntitySet()) {
								System.out.println("*NEW ENTITY SET "
										+ aClass.toString() + " -> "
										+ entity.toString() + ": "
										+ anEntity);
							}
						} else {
							for (String anEntity : entity.getEntitySet()) {
								System.out.println("NEW ENTITY SET "
										+ aClass.toString() + " -> "
										+ entity.toString() + ": "
										+ anEntity);
							}
						}
					}
				}
			}
		}
		//debugging	
		 */		/*//debugging
		for(MyMethod method : newMethods) {
			for(String anEntity : method.getEntitySet()) {
				System.out.println("NEW "+method.toString()+": "+anEntity);
			}
		}
		//debugging
		for(MyAttribute attribute : newAttributes) {
			for(String anEntity : attribute.getEntitySet()) {
				System.out.println("NEW "+attribute.toString()+": "+anEntity);
			}
		}*/
	}

	public boolean isApplicable() {
		int methodCounter = 0;
		for (Entity entity : extractedEntities) {
			if(entity instanceof MyMethod) {
				MyMethod method = (MyMethod)entity;
				methodCounter++;
				if (isSynchronized(method) || containsSuperMethodInvocation(method)
						|| overridesMethod(method) || method.isAbstract())
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
		if(extractedEntities.size() == 1 || methodCounter == 0) {
			return false;
		}
		else {
			return true;
		}
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

	private boolean containsFieldAssignment(MyMethod method) {
		if(!method.getMethodObject().getDefinedFieldsThroughThisReference().isEmpty()) {
			//System.out.println(this.toString() + "\tcontains field assignment");
			for(PlainVariable variable : method.getMethodObject().getDefinedFieldsThroughThisReference()) {
				if(!extractedVariableBindingKeys.containsValue(variable.getVariableBindingKey())) {
					return true;
				}
			}
			return false;
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
	public double getEntityPlacement() {
		return entityPlacement;
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
		return sourceClass.getClassObject().getTypeDeclaration();
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
        return sourceClass.toString() + "\t" + extractedEntities.toString() + "\t" + entityPlacement;
    }

	public String getAnnotationText() {
		return "";
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
		return Double.compare(this.entityPlacement, other.entityPlacement);
	}

	public void findTopic(Stemmer stemmer, HumaniseCamelCase humaniser,
			ArrayList<String> stopWords) {
		HashMap<String, Integer> vocabulary = new HashMap<String, Integer>();
		for (Entity entity : this.extractedEntities) {
			String[] tokens = null;
			if (entity instanceof MyAttribute) {
				tokens = humaniser.humanise(((MyAttribute) entity).getName())
						.split("\\s");
			} else {
				tokens = humaniser
						.humanise(((MyMethod) entity).getMethodName()).split(
								"\\s");
			}
			for (String token : tokens) {
				if (!token.toUpperCase().equals(token)) {
					stemmer.add(token.toLowerCase().toCharArray(),
							token.length());
					stemmer.stem();
					if (!stopWords.contains(token)
							&& !stopWords.contains(stemmer.toString()
									.toLowerCase())) {
						if (!vocabulary.containsKey(stemmer.toString()
								.toLowerCase())) {
							vocabulary.put(stemmer.toString().toLowerCase(), 1);
						} else {
							vocabulary.put(stemmer.toString().toLowerCase(),
									vocabulary.get(stemmer.toString()
											.toLowerCase()) + 1);
						}
					}
				} else {
					if (!vocabulary.containsKey(token)) {
						vocabulary.put(token, 1);
					} else {
						vocabulary.put(token, vocabulary.get(token) + 1);
					}
				}
			}
			int max = 0;
			ArrayList<String> frequentTermList = new ArrayList<String>();
			String frequentTerm = "";
			if (!vocabulary.isEmpty()) {
				for (String term : vocabulary.keySet()) {
					if (vocabulary.get(term) >= max) {
						max = vocabulary.get(term);
					}
				}
				for (String term : vocabulary.keySet()) {
					if (vocabulary.get(term) == max) {
						frequentTermList.add(term);
					}
				}
				for (int i = 0; i < frequentTermList.size() - 1; i++) {
					frequentTerm += frequentTermList.get(i) + " + ";
				}
				frequentTerm += frequentTermList
						.get(frequentTermList.size() - 1);
			}
			topic = frequentTerm;
		}
	}

	public String getTopic() {
		return topic;
	}
}