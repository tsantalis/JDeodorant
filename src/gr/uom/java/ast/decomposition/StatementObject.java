package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Statement;

/*
 * StatementObject represents the following AST Statement subclasses:
 * 1.	ExpressionStatement
 * 2.	VariableDeclarationStatement
 * 3.	ConstructorInvocation
 * 4.	SuperConstructorInvocation
 * 5.	ReturnStatement
 * 6.	AssertStatement
 * 7.	BreakStatement
 * 8.	ContinueStatement
 * 9.	SwitchCase
 * 10.	EmptyStatement
 * 11.	ThrowStatement
 */

public class StatementObject extends AbstractStatement {
	
	public StatementObject(Statement statement) {
		super(statement);
	}

	public String toString() {
		return getStatement().toString();
	}

	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toString());
		return stringRepresentation;
	}
	
	public boolean isEquivalent(StatementObject s) {
		return this.getCreations().size() == s.getCreations().size() &&
		this.getFieldInstructions().size() == s.getFieldInstructions().size() &&
		this.getSuperFieldInstructions().size() == s.getSuperFieldInstructions().size() &&
		this.getSuperMethodInvocations().size() == s.getSuperMethodInvocations().size() &&
		this.getLocalVariableDeclarations().size() == s.getLocalVariableDeclarations().size() &&
		this.getLocalVariableInstructions().size() == s.getLocalVariableInstructions().size() &&
		this.getMethodInvocations().size() == s.getMethodInvocations().size() &&
		this.getLiterals().size() == s.getLiterals().size() &&
		this.getInvokedStaticMethods().size() == s.getInvokedStaticMethods().size() &&
		this.equivalentVariableTypes(s);
	}
	
	private boolean equivalentVariableTypes(StatementObject s) {
		List<LocalVariableDeclarationObject> variableDeclarations1 = this.getLocalVariableDeclarations();
		List<LocalVariableDeclarationObject> variableDeclarations2 = s.getLocalVariableDeclarations();
		for(int i=0; i<variableDeclarations1.size(); i++) {
			LocalVariableDeclarationObject variableDeclaration1 = variableDeclarations1.get(i);
			LocalVariableDeclarationObject variableDeclaration2 = variableDeclarations2.get(i);
			if(!variableDeclaration1.getType().equals(variableDeclaration2.getType())) {
				return false;
			}
		}
		
		List<LocalVariableInstructionObject> variableInstructions1 = this.getLocalVariableInstructions();
		List<LocalVariableInstructionObject> variableInstructions2 = s.getLocalVariableInstructions();
		for(int i=0; i<variableInstructions1.size(); i++) {
			LocalVariableInstructionObject variableInstruction1 = variableInstructions1.get(i);
			LocalVariableInstructionObject variableInstruction2 = variableInstructions2.get(i);
			if(!variableInstruction1.getType().equals(variableInstruction2.getType())) {
				return false;
			}
		}
		return true;
	}
}
