package gr.uom.java.distance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Position;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.ExtractionBlock;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.ASTExtractionBlock;

public class ExtractAndMoveMethodCandidateRefactoring extends CandidateRefactoring {
	private MySystem system;
	private MyClass sourceClass;
    private MyClass targetClass;
    //this is the method from which the extracted method is going to be extracted
    private MyMethod sourceMethod;
    private ExtractionBlock extractionBlock;
    private MyMethodBody newMethodBody;
    private List<MyAbstractStatement> extractionStatementList;
    private Set<String> entitySet;
    private double entityPlacement;
    private DistanceMatrix originalDistanceMatrix;

    public ExtractAndMoveMethodCandidateRefactoring(MySystem system, MyClass sourceClass, MyClass targetClass,
    		MyMethod sourceMethod, ExtractionBlock extractionBlock, DistanceMatrix originalDistanceMatrix) {
    	this.system = system;
    	this.sourceClass = sourceClass;
    	this.targetClass = targetClass;
    	this.sourceMethod = sourceMethod;
    	this.extractionBlock = extractionBlock;
    	this.originalDistanceMatrix = originalDistanceMatrix;
    	
    	this.newMethodBody = null;
    	List<MyAbstractStatement> myAbstractStatementList = new ArrayList<MyAbstractStatement>();
    	for(AbstractStatement abstractStatement : extractionBlock.getStatementsForExtraction()) {
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
    	this.extractionStatementList = myAbstractStatementList;
    	if(extractionBlock.getParentStatementForCopy() == null) {
	    	newMethodBody = new MyMethodBody(myAbstractStatementList);
    	}
    	else {
    		MyAbstractStatement myParentStatement = sourceMethod.getAbstractStatement(extractionBlock.getParentStatementForCopy());
    		MyCompositeStatement myCompositeStatement = (MyCompositeStatement)myParentStatement;
    		MyCompositeStatement copiedCompositeStatement = MyCompositeStatement.newInstance(myCompositeStatement);
    		Map<MyCompositeStatement, ArrayList<MyAbstractStatement>> map = splitStatementsByParent(myAbstractStatementList);
    		for(MyCompositeStatement key : map.keySet()) {
    			copiedCompositeStatement.removeAllStatementsExceptFromSiblingStatements(map.get(key));
    		}
    		List<MyAbstractStatement> statementList = new ArrayList<MyAbstractStatement>();
    		statementList.add(copiedCompositeStatement);
    		newMethodBody = new MyMethodBody(statementList);
    	}
    	this.entitySet = newMethodBody.getEntitySet();
    }

    private Map<MyCompositeStatement, ArrayList<MyAbstractStatement>> splitStatementsByParent(List<MyAbstractStatement> myAbstractStatementList) {
    	Map<MyCompositeStatement, ArrayList<MyAbstractStatement>> map = new LinkedHashMap<MyCompositeStatement, ArrayList<MyAbstractStatement>>();
    	for(MyAbstractStatement myAbstractStatement: myAbstractStatementList) {
    		MyCompositeStatement parent = myAbstractStatement.getParent();
    		if(map.containsKey(parent)) {
    			List<MyAbstractStatement> list = map.get(parent);
    			list.add(myAbstractStatement);
    		}
    		else {
    			ArrayList<MyAbstractStatement> list = new ArrayList<MyAbstractStatement>();
    			list.add(myAbstractStatement);
    			map.put(parent, list);
    		}
    	}
    	return map;
    }

    public boolean apply() {
    	if(!containsSuperMethodInvocation() && !containsBranchingStatement() && !containsFieldAssignment() && !isTargetClassAnInterface() && !isTargetClassAnInnerClass() &&
    			canBeMoved() && hasReferenceToTargetClass()) {
    		MySystem virtualSystem = MySystem.newInstance(system);
	    	virtualApplication(virtualSystem);
	    	FastDistanceMatrix fastDistanceMatrix = new FastDistanceMatrix(virtualSystem, originalDistanceMatrix, this);
	    	double fastEntityPlacement = fastDistanceMatrix.getSystemEntityPlacementValue();
	    	//DistanceMatrix distanceMatrix = new DistanceMatrix(virtualSystem);
	    	//double entityPlacement = distanceMatrix.getSystemEntityPlacementValue();
	    	this.entityPlacement = fastEntityPlacement;
	    	return true;
    	}
    	else
    		return false;
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

    private boolean isTargetClassAnInnerClass() {
    	if(targetClass.getClassObject().isInnerClass()) {
    		//System.out.println(this.toString() + "\tTarget class is an inner class");
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    private boolean canBeMoved() {
    	if(extractionBlock.canBeMovedTo(sourceClass.getClassObject(), targetClass.getClassObject(), sourceMethod.getMethodObject())) {
    		return true;
    	}
    	else {
    		//System.out.println(this.toString() + "\tcannot be moved");
    		return false;
    	}
    }

    private boolean containsFieldAssignment() {
    	for(AbstractStatement abstractStatement : extractionBlock.getStatementsForExtraction()) {
    		List<FieldInstructionObject> fieldInstructions = abstractStatement.getFieldInstructions();
    		for(FieldInstructionObject fieldInstruction : fieldInstructions) {
    			if(abstractStatement instanceof StatementObject) {
    				StatementObject statement = (StatementObject)abstractStatement;
    				if(statement.isFieldAssignment(fieldInstruction)) {
    					//System.out.println(this.toString() + "\tcontains field assignment");
    	    			return true;
    				}
    			}
    			else if(abstractStatement instanceof CompositeStatementObject) {
    				CompositeStatementObject compositeStatement = (CompositeStatementObject)abstractStatement;
    				List<AbstractStatement> fieldAssignments = compositeStatement.getFieldAssignments(fieldInstruction);
    				if(!fieldAssignments.isEmpty()) {
    	    			//System.out.println(this.toString() + "\tcontains field assignment");
    	    			return true;
    	    		}
    			}
    		}
    	}
    	return false;
    }

    private boolean containsSuperMethodInvocation() {
    	ExpressionExtractor expressionExtractor = new ExpressionExtractor();
    	for(AbstractStatement abstractStatement : extractionBlock.getStatementsForExtraction()) {
    		List<Expression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(abstractStatement.getStatement());
    		if(!superMethodInvocations.isEmpty()) {
    			//System.out.println(this.toString() + "\tcontains super method invocation");
    			return true;
    		}
    	}
    	if(extractionBlock.getParentStatementForCopy() != null) {
    		Statement statement = extractionBlock.getParentStatementForCopy().getStatement();
    		if(statement instanceof IfStatement) {
    			IfStatement ifStatement = (IfStatement)statement;
    			List<Expression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(ifStatement.getExpression());
    			if(!superMethodInvocations.isEmpty()) {
        			//System.out.println(this.toString() + "\tcontains super method invocation");
        			return true;
        		}
    		}
    	}
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
	    			Statement statement = extractionBlock.getStatementsForExtraction().get(0).getStatement();
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
    	//System.out.println(this.toString() + "\thas no reference to Target class");
    	return false;
    }

    private boolean containsBranchingStatement() {
    	List<Statement> statementList = new ArrayList<Statement>();
    	StatementExtractor statementExtractor = new StatementExtractor();
    	for(AbstractStatement abstractStatement : extractionBlock.getStatementsForExtraction()) {
    		if(abstractStatement.getStatement().getNodeType() != ASTNode.SWITCH_STATEMENT &&
    			abstractStatement.getStatement().getNodeType() != ASTNode.WHILE_STATEMENT &&
    			abstractStatement.getStatement().getNodeType() != ASTNode.FOR_STATEMENT &&
    			abstractStatement.getStatement().getNodeType() != ASTNode.DO_STATEMENT &&
    			abstractStatement.getStatement().getNodeType() != ASTNode.ENHANCED_FOR_STATEMENT)
    			statementList.addAll(statementExtractor.getBranchingStatements(abstractStatement.getStatement()));
    	}
    	if(!statementList.isEmpty()) {
    		//System.out.println(this.toString() + "\tcontains branching statement");
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    private void virtualApplication(MySystem virtualSystem) {
    	List<LocalVariableInstructionObject> discreteLocalVariableInstructions = new ArrayList<LocalVariableInstructionObject>();
		for(AbstractStatement abstractStatement : extractionBlock.getStatementsForExtraction()) {
			List<LocalVariableInstructionObject> list = abstractStatement.getLocalVariableInstructions();
			for(LocalVariableInstructionObject instruction : list) {
				if(!discreteLocalVariableInstructions.contains(instruction))
					discreteLocalVariableInstructions.add(instruction);
			}
		}
    	
		List<String> parameterList = new ArrayList<String>();
		for(LocalVariableInstructionObject instruction : discreteLocalVariableInstructions) {
			if(!extractionBlock.getReturnVariableDeclaration().equals(instruction.generateLocalVariableDeclaration()) &&
					!extractionBlock.getAdditionalRequiredVariableDeclarations().contains(instruction.generateLocalVariableDeclaration()))
				parameterList.add(instruction.getType().toString());
		}
		
    	MyMethod newMethod = new MyMethod(sourceMethod.getClassOrigin(),extractionBlock.getExtractedMethodName(),
    			extractionBlock.getReturnVariableDeclaration().getType().toString(),parameterList);
    	newMethod.setMethodBody(newMethodBody);
    	
    	MyMethodInvocation newMethodInvocation = newMethod.generateMethodInvocation();
    	
    	MyMethod newSourceMethod = virtualSystem.getClass(sourceClass.getName()).getMethod(sourceMethod);
    	if(extractionBlock.getParentStatementForCopy() == null) {
    		newSourceMethod.replaceSiblingStatementsWithMethodInvocation(extractionStatementList, new MyStatement(newMethodInvocation));
    	}
    	else {
    		MyAbstractStatement parentStatement = newSourceMethod.getAbstractStatement(extractionBlock.getParentStatementForCopy());
    		newSourceMethod.insertMethodInvocationBeforeStatement(parentStatement, new MyStatement(newMethodInvocation));
    		for(MyAbstractStatement statementToRemove : extractionStatementList)
    			newSourceMethod.removeStatement(statementToRemove);
    	}
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

    public void setDistinctExtractedMethodName() {
    	String sourceMethodName = sourceMethod.getMethodName();
    	extractionBlock.setExtractedMethodName(extractionBlock.getReturnVariableDeclaration().getName() +
    	sourceMethodName.replaceFirst(Character.toString(sourceMethodName.charAt(0)), Character.toString(Character.toUpperCase(sourceMethodName.charAt(0)))));
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

    public List<Statement> getStatementsForExtraction() {
    	List<Statement> list = new ArrayList<Statement>();
    	for(AbstractStatement abstractStatement : extractionBlock.getStatementsForExtraction()) {
    		list.add(abstractStatement.getStatement());
    	}
    	return list;
    }

    private VariableDeclarationStatement getReturnVariableDeclarationStatement() {
    	return extractionBlock.getReturnVariableDeclarationStatement();
    }

    private VariableDeclarationFragment getReturnVariableDeclarationFragment() {
    	VariableDeclarationStatement variableDeclarationStatement = extractionBlock.getReturnVariableDeclarationStatement();
    	List<VariableDeclarationFragment> fragmentList = variableDeclarationStatement.fragments();
    	for(VariableDeclarationFragment fragment : fragmentList) {
    		if(fragment.getName().getIdentifier().equals(extractionBlock.getReturnVariableDeclaration().getName()))
    			return fragment;
    	}
    	return null;
    }
    
    private VariableDeclarationFragment getVariableDeclarationFragment(LocalVariableDeclarationObject lvdo, VariableDeclarationStatement variableDeclarationStatement) {
    	List<VariableDeclarationFragment> fragmentList = variableDeclarationStatement.fragments();
    	for(VariableDeclarationFragment fragment : fragmentList) {
    		if(fragment.getName().getIdentifier().equals(lvdo.getName()))
    			return fragment;
    	}
    	return null;
    }

    public ASTExtractionBlock getASTExtractionBlock() {
    	List<VariableDeclarationStatement> allVariableDeclarationStatements = new ArrayList<VariableDeclarationStatement>();
    	List<VariableDeclarationExpression> allVariableDeclarationExpressions = new ArrayList<VariableDeclarationExpression>();
    	List<LocalVariableInstructionObject> discreteLocalVariableInstructions = new ArrayList<LocalVariableInstructionObject>();
		for(AbstractStatement abstractStatement : extractionBlock.getStatementsForExtraction()) {
			List<LocalVariableInstructionObject> instructionList = abstractStatement.getLocalVariableInstructions();
			for(LocalVariableInstructionObject instruction : instructionList) {
				if(!discreteLocalVariableInstructions.contains(instruction)) {
					discreteLocalVariableInstructions.add(instruction);
					VariableDeclarationStatement variableDeclarationStatement = sourceMethod.getMethodObject().getVariableDeclarationStatement(instruction.generateLocalVariableDeclaration());
					if(variableDeclarationStatement != null) {
						allVariableDeclarationStatements.add(variableDeclarationStatement);
					}
					else {
						VariableDeclarationExpression variableDeclarationExpression = sourceMethod.getMethodObject().getVariableDeclarationExpression(instruction.generateLocalVariableDeclaration());
						if(variableDeclarationExpression != null)
							allVariableDeclarationExpressions.add(variableDeclarationExpression);
					}
				}
			}
		}
    	ASTExtractionBlock astExtractionBlock = new ASTExtractionBlock(extractionBlock.getExtractedMethodName(), getReturnVariableDeclarationFragment(),
    		getReturnVariableDeclarationStatement(), getStatementsForExtraction(), allVariableDeclarationStatements, allVariableDeclarationExpressions, extractionBlock.getAssignmentOperators());
    	if(extractionBlock.getParentStatementForCopy() != null)
    		astExtractionBlock.setParentStatementForCopy(extractionBlock.getParentStatementForCopy().getStatement());
    	for(LocalVariableDeclarationObject lvdo : extractionBlock.getAdditionalRequiredVariableDeclarations()) {
    		VariableDeclarationStatement variableDeclarationStatement = extractionBlock.getAdditionalRequiredVariableDeclarationStatement(lvdo);
    		astExtractionBlock.addRequiredVariableDeclarationStatement(getVariableDeclarationFragment(lvdo, variableDeclarationStatement), variableDeclarationStatement);
    	}
    	return astExtractionBlock;
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

    public ExtractionBlock getExtractionBlock() {
    	return extractionBlock;
    }

    public Set<String> getEntitySet() {
    	return entitySet;
    }

    public String toString() {
        return getSourceEntity() + "->" + getTarget();
    }

	public String getSourceEntity() {
		LocalVariableDeclarationObject localVariableDeclaration = extractionBlock.getReturnVariableDeclaration();
		return sourceClass.getName() + "::" + extractionBlock.getExtractedMethodName() + "():" + localVariableDeclaration.getType();
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

	public List<Position> getPositions() {
		ArrayList<Position> positions = new ArrayList<Position>();
		List<Statement> statementList = getStatementsForExtraction();
		for(Statement statement : statementList) {
			Position position = new Position(statement.getStartPosition(), statement.getLength());
			positions.add(position);
		}
		return positions;
	}

	public boolean isSubRefactoringOf(CandidateRefactoring refactoring) {
		if(this == refactoring)
			return false;
		if(refactoring instanceof MoveMethodCandidateRefactoring) {
			MoveMethodCandidateRefactoring moveMethodRefactoring = (MoveMethodCandidateRefactoring)refactoring;
			if(this.sourceMethod.equals(moveMethodRefactoring.getSourceMethod()))
				return true;
		}
		else if(refactoring instanceof ExtractAndMoveMethodCandidateRefactoring) {
			ExtractAndMoveMethodCandidateRefactoring extractAndMoveRefactoring = (ExtractAndMoveMethodCandidateRefactoring)refactoring;
			List<MyAbstractStatement> otherStatementList = extractAndMoveRefactoring.extractionStatementList;
			for(MyAbstractStatement otherStatement : otherStatementList) {
				List<MyAbstractStatement> thisStatementList = this.extractionStatementList;
				for(MyAbstractStatement thisStatement : thisStatementList) {
					if(thisStatement.equals(otherStatement)) {
						AbstractStatement thisParentStatementForCopy = this.extractionBlock.getParentStatementForCopy();
						AbstractStatement otherParentStatementForCopy = extractAndMoveRefactoring.extractionBlock.getParentStatementForCopy();
						if(otherParentStatementForCopy != null) {
							if(thisParentStatementForCopy == null) {
								return true;
							}
							else {
								AbstractStatement thisParent = thisParentStatementForCopy.getParent();
								while(thisParent != null) {
									if(thisParent.equals(otherParentStatementForCopy))
										return true;
									thisParent = thisParent.getParent();
								}
							}
						}
					}
					MyCompositeStatement thisParent = thisStatement.getParent();
					while(thisParent != null) {
						if(thisParent.equals(otherStatement))
							return true;
						thisParent = thisParent.getParent();
					}
				}
			}
		}
		return false;
	}
}
