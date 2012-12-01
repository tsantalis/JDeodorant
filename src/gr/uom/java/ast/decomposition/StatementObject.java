package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LiteralObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.decomposition.cfg.mapping.FieldInstructionReplacement;
import gr.uom.java.ast.decomposition.cfg.mapping.LiteralReplacement;
import gr.uom.java.ast.decomposition.cfg.mapping.Replacement;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableDeclarationReplacement;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableInstructionReplacement;

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
		this.equivalentTypes(s);
	}
	
	private boolean equivalentTypes(StatementObject s) {
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
		
		List<FieldInstructionObject> fieldInstructions1 = this.getFieldInstructions();
		List<FieldInstructionObject> fieldInstructions2 = s.getFieldInstructions();
		for(int i=0; i<fieldInstructions1.size(); i++) {
			FieldInstructionObject fieldInstruction1 = fieldInstructions1.get(i);
			FieldInstructionObject fieldInstruction2 = fieldInstructions2.get(i);
			if(!fieldInstruction1.getType().equals(fieldInstruction2.getType())) {
				return false;
			}
		}
		
		List<LiteralObject> literals1 = this.getLiterals();
		List<LiteralObject> literals2 = s.getLiterals();
		for(int i=0; i<literals1.size(); i++) {
			LiteralObject literal1 = literals1.get(i);
			LiteralObject literal2 = literals2.get(i);
			if(!literal1.getType().equals(literal2.getType())) {
				return false;
			}
		}
		return true;
	}

	public List<Replacement> findReplacements(StatementObject s) {
		List<Replacement> replacements = new ArrayList<Replacement>();
		List<LocalVariableDeclarationObject> variableDeclarations1 = this.getLocalVariableDeclarations();
		List<LocalVariableDeclarationObject> variableDeclarations2 = s.getLocalVariableDeclarations();
		for(int i=0; i<variableDeclarations1.size(); i++) {
			LocalVariableDeclarationObject variableDeclaration1 = variableDeclarations1.get(i);
			LocalVariableDeclarationObject variableDeclaration2 = variableDeclarations2.get(i);
			if(!variableDeclaration1.getName().equals(variableDeclaration2.getName())) {
				VariableDeclarationReplacement replacement = new VariableDeclarationReplacement(variableDeclaration1, variableDeclaration2);
				replacements.add(replacement);
			}
		}
		
		List<LocalVariableInstructionObject> variableInstructions1 = this.getLocalVariableInstructions();
		List<LocalVariableInstructionObject> variableInstructions2 = s.getLocalVariableInstructions();
		for(int i=0; i<variableInstructions1.size(); i++) {
			LocalVariableInstructionObject variableInstruction1 = variableInstructions1.get(i);
			LocalVariableInstructionObject variableInstruction2 = variableInstructions2.get(i);
			if(!variableInstruction1.getName().equals(variableInstruction2.getName())) {
				VariableInstructionReplacement replacement = new VariableInstructionReplacement(variableInstruction1, variableInstruction2);
				replacements.add(replacement);
			}
		}
		
		List<FieldInstructionObject> fieldInstructions1 = this.getFieldInstructions();
		List<FieldInstructionObject> fieldInstructions2 = s.getFieldInstructions();
		for(int i=0; i<fieldInstructions1.size(); i++) {
			FieldInstructionObject fieldInstruction1 = fieldInstructions1.get(i);
			FieldInstructionObject fieldInstruction2 = fieldInstructions2.get(i);
			if(!fieldInstruction1.getName().equals(fieldInstruction2.getName())) {
				FieldInstructionReplacement replacement = new FieldInstructionReplacement(fieldInstruction1, fieldInstruction2);
				replacements.add(replacement);
			}
		}
		
		List<LiteralObject> literals1 = this.getLiterals();
		List<LiteralObject> literals2 = s.getLiterals();
		for(int i=0; i< literals1.size(); i++) {
			LiteralObject literal1 = literals1.get(i);
			LiteralObject literal2 = literals2.get(i);
			if(!literal1.getValue().equals(literal2.getValue())) {
				LiteralReplacement replacement = new LiteralReplacement(literal1, literal2);
				replacements.add(replacement);
			}
		}
		return replacements;
	}
}
