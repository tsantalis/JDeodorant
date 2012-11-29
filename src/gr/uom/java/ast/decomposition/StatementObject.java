package gr.uom.java.ast.decomposition;

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
		this.getInvokedStaticMethods().size() == s.getInvokedStaticMethods().size();
	}
}
