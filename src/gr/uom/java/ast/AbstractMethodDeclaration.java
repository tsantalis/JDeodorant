package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import org.eclipse.jdt.core.dom.MethodDeclaration;

public interface AbstractMethodDeclaration {

    public String getName();

	public Access getAccess();

    public MethodDeclaration getMethodDeclaration();

    public MethodBodyObject getMethodBody();

    public String getClassName();

    public ListIterator<CommentObject> getCommentListIterator();
    
    public ListIterator<ParameterObject> getParameterListIterator();

    public ParameterObject getParameter(int position);

    public List<MethodInvocationObject> getMethodInvocations();

    public List<SuperMethodInvocationObject> getSuperMethodInvocations();

    public List<ConstructorInvocationObject> getConstructorInvocations();

    public List<FieldInstructionObject> getFieldInstructions();

    public List<SuperFieldInstructionObject> getSuperFieldInstructions();

    public List<LocalVariableDeclarationObject> getLocalVariableDeclarations();
    
    public List<LocalVariableInstructionObject> getLocalVariableInstructions();

	public List<CreationObject> getCreations();

	public List<LiteralObject> getLiterals();
	
	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations();

	public Set<String> getExceptionsInThrowStatements();
	
	public Set<String> getExceptionsInJavaDocThrows();

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation);

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction);

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation);

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields();

	public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughFields();

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters();

	public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughParameters();

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables();

	public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference();

	public List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReference();

	public Set<MethodInvocationObject> getInvokedStaticMethods();

	public Set<AbstractVariable> getDefinedFieldsThroughFields();

	public Set<AbstractVariable> getUsedFieldsThroughFields();

	public List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields();

	public List<AbstractVariable> getNonDistinctUsedFieldsThroughFields();

	public Set<AbstractVariable> getDefinedFieldsThroughParameters();

	public Set<AbstractVariable> getUsedFieldsThroughParameters();

	public List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters();

	public List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters();

	public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables();

	public Set<AbstractVariable> getUsedFieldsThroughLocalVariables();

	public Set<PlainVariable> getDefinedFieldsThroughThisReference();

	public List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReference();

	public Set<PlainVariable> getUsedFieldsThroughThisReference();

	public List<PlainVariable> getNonDistinctUsedFieldsThroughThisReference();

	public Set<PlainVariable> getDeclaredLocalVariables();

	public Set<PlainVariable> getDefinedLocalVariables();

	public Set<PlainVariable> getUsedLocalVariables();

	public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations();

	public Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocations();

	public Map<PlainVariable, LinkedHashSet<ConstructorInvocationObject>> getParametersPassedAsArgumentsInConstructorInvocations();

    public boolean containsSuperMethodInvocation();

    public boolean containsSuperFieldAccess();

    public List<TypeObject> getParameterTypeList();

    public List<String> getParameterList();

    public String getSignature();

    public boolean isAbstract();
}
