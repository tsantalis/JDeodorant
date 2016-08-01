package gr.uom.java.ast.decomposition.matching;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;

public class FieldAssignmentReplacedWithSetterInvocationDifference extends ASTNodeDifference {
	
	private AbstractExpression invoker1;
	private AbstractExpression invoker2;
	private AbstractExpression argument1;
	private AbstractExpression argument2;
	private AbstractVariable field1;
	private AbstractVariable field2;
	private String setterMethodName;
	private List<ASTNodeDifference> invokerDifferences;
	private List<ASTNodeDifference> argumentDifferences;
	
	public FieldAssignmentReplacedWithSetterInvocationDifference(AbstractExpression e1, AbstractExpression e2,
			String setterMethodName) {
		super(e1, e2);
		this.setterMethodName = setterMethodName;
		this.invokerDifferences = new ArrayList<ASTNodeDifference>();
		this.argumentDifferences = new ArrayList<ASTNodeDifference>();
	}

	public String getSetterMethodName() {
		return setterMethodName;
	}

	public List<ASTNodeDifference> getInvokerDifferences() {
		return invokerDifferences;
	}
	public void addInvokerDifference(ASTNodeDifference invokerDifference) {
		this.invokerDifferences.add(invokerDifference);
	}
	public List<ASTNodeDifference> getArgumentDifferences() {
		return argumentDifferences;
	}
	public void addArgumentDifference(ASTNodeDifference argumentDifference) {
		this.argumentDifferences.add(argumentDifference);
	}
	public AbstractExpression getInvoker1() {
		return invoker1;
	}
	public void setInvoker1(AbstractExpression invoker1) {
		this.invoker1 = invoker1;
	}
	public AbstractExpression getInvoker2() {
		return invoker2;
	}
	public void setInvoker2(AbstractExpression invoker2) {
		this.invoker2 = invoker2;
	}
	public AbstractExpression getArgument1() {
		return argument1;
	}
	public void setArgument1(AbstractExpression argument1) {
		this.argument1 = argument1;
	}
	public AbstractExpression getArgument2() {
		return argument2;
	}
	public void setArgument2(AbstractExpression argument2) {
		this.argument2 = argument2;
	}
	public AbstractVariable getField1() {
		return field1;
	}
	public void setField1(AbstractVariable field1) {
		this.field1 = field1;
	}
	public AbstractVariable getField2() {
		return field2;
	}
	public void setField2(AbstractVariable field2) {
		this.field2 = field2;
	}
}
