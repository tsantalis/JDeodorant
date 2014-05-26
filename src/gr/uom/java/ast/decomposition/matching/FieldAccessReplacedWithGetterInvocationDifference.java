package gr.uom.java.ast.decomposition.matching;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.AbstractExpression;

public class FieldAccessReplacedWithGetterInvocationDifference extends ASTNodeDifference {

	private AbstractExpression invoker1;
	private AbstractExpression invoker2;
	private String getterMethodName;
	private List<ASTNodeDifference> invokerDifferences;
	
	public FieldAccessReplacedWithGetterInvocationDifference(AbstractExpression e1, AbstractExpression e2,
			String getterMethodName) {
		super(e1, e2);
		this.getterMethodName = getterMethodName;
		this.invokerDifferences = new ArrayList<ASTNodeDifference>();
	}
	
	public String getGetterMethodName() {
		return getterMethodName;
	}

	public List<ASTNodeDifference> getInvokerDifferences() {
		return invokerDifferences;
	}
	public void addInvokerDifference(ASTNodeDifference invokerDifference) {
		this.invokerDifferences.add(invokerDifference);
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
}
