package gr.uom.java.ast.decomposition.matching.loop;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class EnhancedForLoop extends AbstractLoop
{
	private AbstractControlVariable conditionControlVariable;
	
	public EnhancedForLoop(EnhancedForStatement enhancedForStatement) {
		super(enhancedForStatement);
		this.condition                = enhancedForStatement.getExpression();
		this.conditionControlVariable = generateConditionControlVariable(this.condition);
	}
	
	public AbstractControlVariable getControlVariable()
	{
		return this.conditionControlVariable;
	}
	
	private static AbstractControlVariable generateConditionControlVariable(Expression dataStructure)
	{
		// initialize startValue
		VariableValue startValue = new VariableValue(0);
		// initialize endValue
		VariableValue endValue = null;
		ITypeBinding dataStructureBinding = dataStructure.resolveTypeBinding();
		// if the dataStructure is an array or a mehtodInvocation returning and array (both covered by the first expression) OR
		// the data structure is a collection or the data structure is a methodInvocation returning a collection (both covered by the second expression)
		// These are the only cases supported by the JAVA enhanced for loop
		if (dataStructureBinding.isArray() || AbstractLoopUtilities.isCollection(dataStructureBinding))
		{
			endValue = new VariableValue(VariableValue.ValueType.DATA_STRUCTURE_SIZE);
		}
		// initialize variableUpdaters
		List<VariableUpdater> variableUpdaters = new ArrayList<VariableUpdater>();
		variableUpdaters.add(new VariableUpdater(1));
		if (endValue == null)
		{
			return null;
		}
		return new AbstractControlVariable(startValue, endValue, variableUpdaters, dataStructure);
	}

	public List<ASTNode> getAdditionalFragments()
	{
		List<ASTNode> additionalFragments = new ArrayList<ASTNode>();
		List<VariableUpdater> updaters    = conditionControlVariable.getVariableUpdaters();
		for (VariableUpdater currentVariableUpdater : updaters)
		{
			if (currentVariableUpdater.getUpdater() != null)
			{
				additionalFragments.add(currentVariableUpdater.getUpdater());
			}
		}
		return additionalFragments;
	}
	
	// ********************************************************************************************************************************************************************************
	// matching methods
	// ********************************************************************************************************************************************************************************

	public boolean match(ConditionalLoop otherLoop, ConditionalLoopASTNodeMatcher matcher)
	{
		if (otherLoop.getConditionControlVariables().size() == 1)
		{
			for (AbstractControlVariable currentControlVariable : otherLoop.getConditionControlVariables().values())
			{
				return currentControlVariable.match(this.conditionControlVariable);
			}
		}
		return false;
	}

	public boolean match(EnhancedForLoop otherLoop, ConditionalLoopASTNodeMatcher matcher)
	{
		return this.conditionControlVariable.match(otherLoop.conditionControlVariable);
	}
}