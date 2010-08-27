package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.ast.util.TypeVisitor;
import gr.uom.java.distance.Entity;
import gr.uom.java.distance.MyAttribute;
import gr.uom.java.distance.MyMethod;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ExtractClassRefactoring extends Refactoring {

	private CompilationUnit sourceCompilationUnit;
	private CompilationUnit targetCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeDeclaration targetTypeDeclaration;
	private IFile sourceFile;
	private IFile targetFile;
	private ASTRewrite sourceRewriter;
	private ASTRewrite targetRewriter;
	private Set<ITypeBinding> requiredTargetImportDeclarationSet;
	private Map<MethodDeclaration, Boolean> leaveDelegate;
	private Map<MyMethod, Boolean> leaveDelegateInput;
	private Map<ICompilationUnit, TextFileChange> textFileChanges;
	private Map<ICompilationUnit, CreateCompilationUnitChange> createCompilationUnitChanges;
	private Map<ICompilationUnit, TextFileChange> fChanges;
	private List<Entity> extractedEntities;
	private List<MethodDeclaration> extractedMethods;
	private List<FieldDeclaration> extractedFields;
	private Map<MethodInvocation, MethodDeclaration> extractedMethodInvocations;
	private Map<IVariableBinding, VariableDeclaration> extractedFieldInstructions;
	private Map<IVariableBinding, VariableDeclaration> sourceFieldInstructions;
	private Map<IMethodBinding, MethodDeclaration> sourceMethodBindings;
	private Document targetDocument;
	private ICompilationUnit targetICompilationUnit;
	private HashMap<MethodDeclaration, LinkedList<String>> additionalArgumentsAddedToMovedMethod;
	private HashMap<MethodDeclaration, LinkedList<String>> additionalParametersAddedToMovedMethod;
	private String targetTypeName;
	private VariableDeclarationFragment fragmentToAddSetter;
	private Set<ITypeBinding> additionalTypeBindingsToBeImportedInTargetClass;

	public ExtractClassRefactoring(CompilationUnit sourceCompilationUnit, 
			TypeDeclaration sourceTypeDeclaration, IFile sourceFile, List<Entity> extractedEntities, Map<MyMethod, Boolean> leaveDelegate, String targetTypeName) {
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.targetCompilationUnit = this.sourceCompilationUnit.getAST().newCompilationUnit();
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.targetTypeDeclaration = this.sourceCompilationUnit.getAST().newTypeDeclaration();
		this.requiredTargetImportDeclarationSet = new LinkedHashSet<ITypeBinding>();
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.additionalArgumentsAddedToMovedMethod = new LinkedHashMap<MethodDeclaration, LinkedList<String>>();
		this.additionalParametersAddedToMovedMethod = new LinkedHashMap<MethodDeclaration, LinkedList<String>>();
		this.textFileChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();
		this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
		this.fChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();
		this.sourceFile = sourceFile;
		this.extractedEntities = extractedEntities;
		this.extractedMethods = new ArrayList<MethodDeclaration>();
		this.extractedFields = new ArrayList<FieldDeclaration>();
		this.extractedMethodInvocations = new HashMap<MethodInvocation, MethodDeclaration>();
		this.extractedFieldInstructions = new HashMap<IVariableBinding, VariableDeclaration>();
		this.leaveDelegateInput = leaveDelegate;
		this.leaveDelegate = new HashMap<MethodDeclaration, Boolean>();
		for(Entity entity : this.extractedEntities) {
			if(entity instanceof MyMethod) {
				MyMethod method = (MyMethod)entity;
				this.extractedMethods.add(method.getMethodObject().getMethodDeclaration());
				if(leaveDelegate.get(method) != null) {
					this.leaveDelegate.put(method.getMethodObject().getMethodDeclaration(), leaveDelegate.get(method));
				}
			}
			else if(entity instanceof MyAttribute) {
				MyAttribute field = (MyAttribute)entity;
				this.extractedFields.add((FieldDeclaration)field.getFieldObject().getVariableDeclaration().getParent());
				this.extractedFieldInstructions.put(field.getFieldObject().getVariableDeclaration().resolveBinding(), field.getFieldObject().getVariableDeclaration());
			}
		}
		sourceFieldInstructions = new HashMap<IVariableBinding, VariableDeclaration>();
		List<FieldDeclaration> sourceFields = Arrays.asList(sourceTypeDeclaration.getFields());
		for(FieldDeclaration field : sourceFields) {
			for(Object fragment : field.fragments()) {
				VariableDeclarationFragment variable = (VariableDeclarationFragment)fragment;
				sourceFieldInstructions.put(variable.resolveBinding(), variable);
			}

		}
		sourceMethodBindings = new HashMap<IMethodBinding, MethodDeclaration>();
		List<MethodDeclaration> sourceMethods = Arrays.asList(sourceTypeDeclaration.getMethods());
		for(MethodDeclaration method : sourceMethods) {
			sourceMethodBindings.put(method.resolveBinding(), method);
		}
		extractMethodInvocations();
		this.targetTypeName = targetTypeName;
		additionalTypeBindingsToBeImportedInTargetClass = new HashSet<ITypeBinding>();
	}

	public IFile getTargetFile() {
		return targetFile;
	}

	public String getTargetTypeName() {
		return targetTypeName;
	}

	public void setTargetTypeName(String targetTypeName) {
		this.targetTypeName = targetTypeName;
	}

	public CompilationUnit getSourceCompilationUnit() {
		return sourceCompilationUnit;
	}

	private void extractMethodInvocations() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(MethodDeclaration method : sourceTypeDeclaration.getMethods()) {
			List<Expression> invocations = expressionExtractor.getMethodInvocations(method.getBody());
			for(Expression expression : invocations) {
				MethodInvocation invocation = null;
				if (expression instanceof MethodInvocation) {
					invocation = (MethodInvocation) expression;
					for(MethodDeclaration method2 : extractedMethods) {
						if(method2.resolveBinding().isEqualTo(invocation.resolveMethodBinding())) {
							extractedMethodInvocations.put(invocation, method2);
							if(isParentAnonymousClassDeclaration(invocation)) {
								this.leaveDelegate.put(method2, Boolean.TRUE);
							}
						}
					}
				}
			}
		}
	}


	public void apply() {
		removeExtractedMembers();
		addNewType();

		addFields();
		addMethods();
		addRequiredTargetImportDeclarations();
		addTargetClassReferenceToSourceClass();
		modifyTargetMemberAccessesInSourceClass();
		for(MethodDeclaration method : leaveDelegate.keySet()) {
			if(leaveDelegate.get(method)) {
				addDelegationInSourceMethod(method);
			}
		}
		if(targetDocument != null) {
			try {
				TextEdit targetEdit = targetRewriter.rewriteAST(targetDocument, null);
				targetEdit.apply(targetDocument);
				CreateCompilationUnitChange createCompilationUnitChange = new CreateCompilationUnitChange(targetICompilationUnit, targetDocument.get(), targetFile.getCharset());
				createCompilationUnitChanges.put(targetICompilationUnit, createCompilationUnitChange);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				TextEdit targetEdit = targetRewriter.rewriteAST();
				TextFileChange textFileChange = textFileChanges.get(targetICompilationUnit);
				if (textFileChange == null) {
					textFileChange = new TextFileChange(targetICompilationUnit.getElementName(), (IFile)targetICompilationUnit.getResource());
					textFileChange.setTextType("java");
					textFileChange.setEdit(targetEdit);
				} else
					textFileChange.getEdit().addChild(targetEdit);
				textFileChanges.put(targetICompilationUnit, textFileChange);
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			TextFileChange change = fChanges.get(sourceICompilationUnit);
			if (change == null) {
				change = new TextFileChange(sourceICompilationUnit.getElementName(), (IFile)sourceICompilationUnit.getResource());
				change.setTextType("java");
				change.setEdit(sourceEdit);
			} else
				change.getEdit().addChild(sourceEdit);
			fChanges.put(sourceICompilationUnit, change);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

	}

	private void removeExtractedMembers() {
		for(MethodDeclaration method : leaveDelegate.keySet()) {
			if (!leaveDelegate.get(method)) {
				sourceRewriter.remove(method, null);
			}
		}
		for(FieldDeclaration field : extractedFields) {
			sourceRewriter.remove(field, null);
		}
	}

	private void addTargetClassReferenceToSourceClass() {
		AST ast = sourceTypeDeclaration.getAST();
		Type targetType = ast.newSimpleType(ast.newName(targetTypeName));
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		String modifiedTargetTypeName = targetTypeName.replaceFirst(""+targetTypeName.charAt(0), ""+targetTypeName.toLowerCase().charAt(0));
		sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName(modifiedTargetTypeName), null);
		ClassInstanceCreation initializer = ast.newClassInstanceCreation();
		sourceRewriter.set(initializer, ClassInstanceCreation.TYPE_PROPERTY, targetType, null);
		sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, initializer, null);
		FieldDeclaration targetReference = ast.newFieldDeclaration(fragment);
		sourceRewriter.set(targetReference, FieldDeclaration.TYPE_PROPERTY, targetType, null);
		ListRewrite modifiers = sourceRewriter.getListRewrite(targetReference, FieldDeclaration.MODIFIERS2_PROPERTY);
		modifiers.insertFirst(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD), null);
		ListRewrite sourceBodyDeclarations = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		sourceBodyDeclarations.insertFirst(targetReference, null);
	}

	private void modifyTargetMemberAccessesInSourceClass() {
		List<MethodDeclaration> methods = Arrays.asList(sourceTypeDeclaration.getMethods());
		AST ast = sourceTypeDeclaration.getAST();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(MethodDeclaration method : methods) {
			if (!extractedMethods.contains(method)) {
				List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(method.getBody());
				for (Expression expression : variableInstructions) {
					SimpleName variableInstruction = (SimpleName) expression;
					if (extractedFieldInstructions.containsKey(variableInstruction.resolveBinding())) {
						String variableName = variableInstruction.toString().replaceFirst(
								""
								+ variableInstruction
								.toString().charAt(0),
								""
								+ variableInstruction
								.toString()
								.toUpperCase()
								.charAt(0));
						if (isParentAssignment(variableInstruction) != null) {
							replaceAssignmentWithSetter(ast.newSimpleName(targetTypeName.replaceFirst(
									"" + targetTypeName.charAt(0), ""
									+ targetTypeName
									.toLowerCase()
									.charAt(0))), ast,
									isParentAssignment(variableInstruction),
									variableName, sourceRewriter);
						} else {
							replaceVariableInstructionWithGetter(ast.newSimpleName(targetTypeName.replaceFirst(
									"" + targetTypeName.charAt(0), ""
									+ targetTypeName
									.toLowerCase()
									.charAt(0))), ast,
									variableInstruction, variableName,
									sourceRewriter);
						}
					}
				}
				List<Expression> invocations = expressionExtractor.getMethodInvocations(method.getBody());
				for (Expression expression : invocations) {
					if (expression instanceof MethodInvocation) {
						MethodInvocation invocation = (MethodInvocation) expression;
						if (extractedMethodInvocations.containsKey(invocation)) {
							sourceRewriter.set(invocation,MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(targetTypeName.replaceFirst(
									""
									+ targetTypeName
									.charAt(0),
									""
									+ targetTypeName
									.toLowerCase()
									.charAt(0))),null);
							if (this.additionalArgumentsAddedToMovedMethod.containsKey(extractedMethodInvocations.get(invocation))) {
								ListRewrite arguments = sourceRewriter.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
								for (String argument : this.additionalArgumentsAddedToMovedMethod.get(extractedMethodInvocations.get(invocation))) {
									if (!argument.equalsIgnoreCase(sourceTypeDeclaration.getName().getIdentifier())) {
										arguments.insertLast(invocation.getAST().newSimpleName(argument), null);
									} else {
										arguments.insertLast(invocation.getAST().newThisExpression(),null);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private Assignment isParentAssignment(ASTNode node) {
		if(node.getParent() instanceof Assignment) {
			Assignment assignment = (Assignment)node.getParent();
			if(assignment.getLeftHandSide().subtreeMatch(new ASTMatcher(), node)) {
				return assignment;
			}
			else {
				return null;
			}
		}
		else if(node.getParent() instanceof MethodDeclaration || node.getParent() instanceof ArrayAccess) {
			return null;
		}
		else {
			return isParentAssignment(node.getParent());
		}
	}

	private void addDelegationInSourceMethod(MethodDeclaration method) {
		AST ast = method.getAST();
		MethodInvocation delegate = ast.newMethodInvocation();

		SimpleName fieldName = ast.newSimpleName(targetTypeName.replaceFirst(""+targetTypeName.charAt(0), ""+targetTypeName.toLowerCase().charAt(0)));
		sourceRewriter.set(delegate, MethodInvocation.EXPRESSION_PROPERTY, fieldName, null);
		sourceRewriter.set(delegate, MethodInvocation.NAME_PROPERTY, method.getName(), null);
		List<SingleVariableDeclaration> parameters = method.parameters();
		ListRewrite arguments = sourceRewriter.getListRewrite(delegate, MethodInvocation.ARGUMENTS_PROPERTY);
		for (SingleVariableDeclaration parameter : parameters) {
			arguments.insertLast(parameter.getName(), null);
		}
		if (additionalArgumentsAddedToMovedMethod.containsKey(method)) {
			for (String argument : additionalArgumentsAddedToMovedMethod.get(method)) {
				if (!argument.equalsIgnoreCase(sourceTypeDeclaration.getName().getIdentifier())) {
					arguments.insertLast(ast.newSimpleName(argument), null);
				} else {
					arguments.insertLast(ast.newThisExpression(), null);
				}
			}
		}
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> returnStatements = statementExtractor.getReturnStatements(method.getBody());
		Statement statement = null;
		if (!returnStatements.isEmpty() && !method.getReturnType2().toString().equals(PrimitiveType.VOID.toString())) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, delegate, null);
			statement = returnStatement;
		}
		else {
			ExpressionStatement expressionStatement = ast.newExpressionStatement(delegate);
			statement = expressionStatement;
		}
		Block block = ast.newBlock();
		ListRewrite blockRewrite = sourceRewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		blockRewrite.insertLast(statement, null);
		sourceRewriter.replace(method.getBody(), block, null);
	}

	private ArrayList<MethodInvocation> addInvocationToReplace(MethodInvocation invocation, ArrayList<MethodInvocation> invocationsToReplace) {
		if((!extractedMethodInvocations.containsKey(invocation) && sourceMethodBindings.containsKey(invocation.resolveMethodBinding())) || isDeclaredInSuperclass(invocation, sourceTypeDeclaration.resolveBinding())) {
			invocationsToReplace.add(invocation);
		}
		Expression invoker = invocation.getExpression();
		if(invoker instanceof MethodInvocation) {
			invocationsToReplace = addInvocationToReplace((MethodInvocation)invoker, invocationsToReplace);
		}
		return invocationsToReplace;
	}

	private void addMethods() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		ListRewrite targetClassBodyRewrite = targetRewriter.getListRewrite(targetTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		HashMap<MethodDeclaration, MethodDeclaration> newMethods = new HashMap<MethodDeclaration, MethodDeclaration>();
		HashMap<MethodDeclaration, ArrayList<MethodInvocation>> targetInvocationsMap = new HashMap<MethodDeclaration, ArrayList<MethodInvocation>>();

		for(MethodDeclaration method : extractedMethods) {
			ArrayList<Assignment> assignmentsToReplace = new ArrayList<Assignment>();
			List<Expression> assignments = expressionExtractor.getAssignments(method.getBody());
			for(Expression expression : assignments) {
				Assignment assignment = (Assignment)expression;
				if(assignment.getLeftHandSide() instanceof FieldAccess) {
					FieldAccess fieldAccess = (FieldAccess)assignment.getLeftHandSide();
					if(!extractedFieldInstructions.containsKey(fieldAccess.resolveFieldBinding()) && sourceFieldInstructions.containsKey(fieldAccess.resolveFieldBinding())) {
						assignmentsToReplace.add(assignment);
					}
				}
				else if(assignment.getLeftHandSide() instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)assignment.getLeftHandSide();
					if(!extractedFieldInstructions.containsKey(simpleName.resolveBinding()) && sourceFieldInstructions.containsKey(simpleName.resolveBinding())) {
						assignmentsToReplace.add(assignment);
					}
				}

			}

			ArrayList<SimpleName> instructionsToReplace = new ArrayList<SimpleName>();
			List<Expression> instructions = expressionExtractor.getVariableInstructions(method.getBody());
			for(Expression expression : instructions) {
				SimpleName instruction = (SimpleName)expression;
				if(isParentAssignment(instruction) == null) {
					if(!extractedFieldInstructions.containsKey(instruction.resolveBinding()) && sourceFieldInstructions.containsKey(instruction.resolveBinding())) {
						instructionsToReplace.add(instruction);
					}
				}
			}

			ArrayList<MethodInvocation> invocationsToReplace = new ArrayList<MethodInvocation>();
			ArrayList<MethodInvocation> targetInvocationsToReplace = new ArrayList<MethodInvocation>();
			List<Expression> invocations = expressionExtractor.getMethodInvocations(method.getBody());
			for(Expression expression : invocations) {
				MethodInvocation invocation = (MethodInvocation)expression;
				invocationsToReplace = addInvocationToReplace(invocation, invocationsToReplace);
				if(extractedMethodInvocations.containsKey(invocation)) {
					targetInvocationsToReplace.add(invocation);
				}
			}
			targetInvocationsMap.put(method, targetInvocationsToReplace);

			ArrayList<ThisExpression> thisExpressionsToReplace = new ArrayList<ThisExpression>();
			List<Expression> thisExpressions = expressionExtractor.getThisExpressions(method.getBody());
			for(Expression expression : thisExpressions) {
				ThisExpression thisExpression = (ThisExpression)expression;
				if(thisExpression.getParent() instanceof FieldAccess) {
					FieldAccess access = (FieldAccess)thisExpression.getParent();
					if(!this.extractedFieldInstructions.containsKey(access.resolveFieldBinding())) {
						thisExpressionsToReplace.add(thisExpression);
					}
				}
				else if(thisExpression.getParent() instanceof MethodInvocation) {
					MethodInvocation invocation = (MethodInvocation)thisExpression.getParent();
					if(this.extractedMethodInvocations.containsKey(invocation) || !(invocation.getExpression() instanceof ThisExpression)) {
						thisExpressionsToReplace.add(thisExpression);
					}
				}
				else {
					thisExpressionsToReplace.add(thisExpression);
				}
			}

			MethodDeclaration newMethod = (MethodDeclaration) ASTNode.copySubtree(
					targetTypeDeclaration.getAST(), method);
			AST ast = newMethod.getAST();
			SimpleName sourceClassParameter = null;
			if (!assignmentsToReplace.isEmpty()) {

				sourceClassParameter = addSourceClassParameterToMovedMethod(newMethod, method, false);

				List<Expression> newAssignments = expressionExtractor.getAssignments(newMethod.getBody());
				for (Expression newAssignment : newAssignments) {
					for (Assignment oldAssignment : assignmentsToReplace) {
						if (newAssignment.subtreeMatch(new ASTMatcher(), oldAssignment)) {
							Assignment assignment = (Assignment) newAssignment;
							String variableName = "";
							if (assignment.getLeftHandSide() instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess) assignment.getLeftHandSide();
								variableName = fieldAccess.getName().toString().replaceFirst(
										""
										+ fieldAccess.getName()
										.toString()
										.charAt(0),
										""
										+ fieldAccess.getName()
										.toString()
										.toUpperCase()
										.charAt(0));

							} else if (assignment.getLeftHandSide() instanceof SimpleName) {
								SimpleName simpleName = (SimpleName) assignment.getLeftHandSide();
								variableName = simpleName.toString().replaceFirst(
										""
										+ simpleName.toString()
										.charAt(0),
										""
										+ simpleName.toString()
										.toUpperCase()
										.charAt(0));
							}
							if(sourceClassContainsSetter(variableName) != null) {
								ListRewrite sourceBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
								addSetter(sourceTypeDeclaration.getAST(), sourceBodyRewrite, sourceClassContainsSetter(variableName), fragmentToAddSetter, variableName, sourceRewriter);
							}
							replaceAssignmentWithSetter(sourceClassParameter, ast, assignment, variableName, targetRewriter);
						}
					}
				}
			}

			if(!instructionsToReplace.isEmpty()) {
				for (SimpleName instruction : instructionsToReplace) {
					FieldDeclaration[] sourceFieldDeclarations = sourceTypeDeclaration.getFields();
					for(FieldDeclaration sourceFieldDeclaration : sourceFieldDeclarations) {
						List<VariableDeclarationFragment> fragments = sourceFieldDeclaration.fragments();
						for(VariableDeclarationFragment fragment : fragments) {
							if(fragment.resolveBinding().isEqualTo(instruction.resolveBinding())) {
								int modifiers = sourceFieldDeclaration.getModifiers();
								List<Expression> newInstructions = expressionExtractor.getVariableInstructions(newMethod.getBody());
								for(Expression newInstruction : newInstructions) {
									if(newInstruction.subtreeMatch(new ASTMatcher(), instruction)) {
										if((modifiers & Modifier.STATIC) != 0) {
											QualifiedName qualifiedName = ast.newQualifiedName(ast.newName(sourceTypeDeclaration.getName().getIdentifier()), ast.newSimpleName(instruction.getIdentifier()));
											targetRewriter.replace(newInstruction, qualifiedName, null);
										}
										else {
											addParameterToMovedMethod(newMethod, instruction, method);
										}
									}
								}
							}
						}
					}
				}
			}

			ArrayList<MethodInvocation> replacedInvocations = new ArrayList<MethodInvocation>();
			Set<String> sourceMethodsWithPublicModifier = new LinkedHashSet<String>();
			if(!invocationsToReplace.isEmpty()) {
				for(Expression expression : invocationsToReplace) {
					if(expression instanceof MethodInvocation) {
						MethodInvocation methodInvocation = (MethodInvocation)expression;
						if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
							IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
							if(methodBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
								for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
									if(sourceMethodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
										SimpleName fieldName = MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
										int modifiers = sourceMethodDeclaration.getModifiers();
										List<Expression> newInvocations = expressionExtractor.getMethodInvocations(newMethod.getBody());
										for (Expression newMethodInvocation : newInvocations) {
											if (newMethodInvocation.subtreeMatch(new ASTMatcher(), methodInvocation)) {
												if((modifiers & Modifier.STATIC) != 0) {
													targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier()), null);
													if(!sourceMethodsWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
														setPublicModifierToSourceMethod(methodInvocation);
														sourceMethodsWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
														replacedInvocations.add(methodInvocation);
													}
												}
												else if(fieldName != null) {
													targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
													if(!extractedFieldInstructions.containsKey(fieldName.resolveBinding())) {
														addParameterToMovedMethod(newMethod, fieldName, method);
														replacedInvocations.add(methodInvocation);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			invocationsToReplace.removeAll(replacedInvocations);

			if(!invocationsToReplace.isEmpty()) {
				boolean foundAnonymousClassDeclaration = false;
				for(MethodInvocation invocation : invocationsToReplace) {
					if(isParentAnonymousClassDeclaration(invocation)) {
						foundAnonymousClassDeclaration = true;
					}
				}
				if (sourceClassParameter == null) {
					sourceClassParameter = addSourceClassParameterToMovedMethod(
							newMethod, method, foundAnonymousClassDeclaration);
				}
				List<Expression> newInvocations = expressionExtractor.getMethodInvocations(newMethod.getBody());
				for (Expression newInvocation : newInvocations) {
					for (MethodInvocation oldInvocation : invocationsToReplace) {
						if (newInvocation.subtreeMatch(new ASTMatcher(), oldInvocation)) {
							MethodInvocation invocation = (MethodInvocation)newInvocation;
							targetRewriter.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, sourceClassParameter, null);
							setPublicModifierToSourceMethod(oldInvocation);
						}
					}
				}
			}

			if(!thisExpressionsToReplace.isEmpty()) {
				if (sourceClassParameter == null) {
					sourceClassParameter = addSourceClassParameterToMovedMethod(
							newMethod, method, false);
				}
				List<Expression> newThisExpressions = expressionExtractor.getThisExpressions(newMethod.getBody());
				for (ThisExpression oldThisExpression : thisExpressionsToReplace) {
					for (Expression newThisExpression : newThisExpressions) {
						if (oldThisExpression.getParent().subtreeMatch(new ASTMatcher(), newThisExpression.getParent())) {
							ThisExpression thisExpression = (ThisExpression)newThisExpression;
							targetRewriter.replace(thisExpression,
									sourceClassParameter, null);
						}
					}
				}
			}

			List<MethodDeclaration> oldMethods = Arrays.asList(sourceTypeDeclaration.getMethods());
			for(MethodDeclaration oldMethod : oldMethods) {
				if(!oldMethod.equals(method)) {
					List<Expression> oldInvocations = expressionExtractor.getMethodInvocations(oldMethod.getBody());
					for(Expression oldExpression : oldInvocations) {
						if (oldExpression instanceof MethodInvocation) {
							MethodInvocation invocation = (MethodInvocation) oldExpression;
							if (extractedMethodInvocations.containsKey(invocation)) {
								ListRewrite modifierRewrite = targetRewriter.getListRewrite(newMethod, MethodDeclaration.MODIFIERS2_PROPERTY);
								Modifier publicModifier = newMethod.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
								boolean modifierFound = false;
								List<IExtendedModifier> modifiers = newMethod.modifiers();
								for (IExtendedModifier extendedModifier : modifiers) {
									if (extendedModifier.isModifier()) {
										Modifier modifier = (Modifier) extendedModifier;
										if (modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
											modifierFound = true;
										} else if (modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD)
													|| modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
											modifierFound = true;
											modifierRewrite.replace(modifier, publicModifier, null);
										}
									}
								}
								if (!modifierFound) {
									modifierRewrite.insertFirst(publicModifier, null);
								}
							}
						}
					}
				}
			}
			newMethods.put(newMethod, method);
		}
		for(MethodDeclaration newMethod : newMethods.keySet()) {
			if (!targetInvocationsMap.isEmpty()) {
				List<Expression> newInvocations = expressionExtractor.getMethodInvocations(newMethod.getBody());
				for (Expression newInvocation : newInvocations) {
					for (MethodDeclaration oldMethod : targetInvocationsMap.keySet()) {
						Set<IMethodBinding> checkedInvocations = new HashSet<IMethodBinding>();
						if (oldMethod.subtreeMatch(new ASTMatcher(), newMethod)) {
							for (MethodInvocation oldInvocation : targetInvocationsMap.get(oldMethod)) {
								if (newInvocation.subtreeMatch(new ASTMatcher(), oldInvocation)) {
									for (MethodDeclaration modifiedMethod : this.additionalArgumentsAddedToMovedMethod.keySet()) {
										if (modifiedMethod.resolveBinding().isEqualTo(oldInvocation.resolveMethodBinding())) {
											MethodInvocation invocation = (MethodInvocation) newInvocation;
											if (!checkedInvocations.contains(oldInvocation.resolveMethodBinding())) {
												AST ast = invocation.getAST();
												ListRewrite arguments = targetRewriter.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
												Set<String> additionalArguments = new TreeSet<String>(this.additionalArgumentsAddedToMovedMethod.get(modifiedMethod));
												TreeSet<String> checkedArguments = new TreeSet<String>();
												for (String argument : additionalArguments) {
													if (!argument.equalsIgnoreCase(sourceTypeDeclaration.getName().getIdentifier())) {
														if (!containsParameter(newMethods.get(newMethod), argument)) {
															if (isParentAnonymousClassDeclaration(oldInvocation)) {
																if (!containsParameter(newMethods.get(newMethod), sourceTypeDeclaration.getName().getIdentifier())) {
																	this.addSourceClassParameterToMovedMethod(newMethod, newMethods.get(newMethod), false);
																}
																MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
																SimpleName fieldName = null;
																MethodDeclaration methodDeclaration = null;
																for (MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
																	fieldName = MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
																	methodDeclaration = sourceMethodDeclaration;
																	if (fieldName != null && fieldName.getIdentifier().equals(argument))
																		break;
																}
																if (fieldName != null) {
																	MethodInvocation methodInvocation = ast.newMethodInvocation();
																	targetRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, methodDeclaration.getName(), null);
																	targetRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier().replaceFirst(
																			Character.toString(sourceTypeDeclaration.getName().getIdentifier().charAt(0)),
																			Character.toString(Character.toLowerCase(sourceTypeDeclaration.getName().getIdentifier().charAt(0))))), null);
																	arguments.insertLast(methodInvocation, null);
																	checkedArguments.add(argument);
																}
															} else {
																this.addParameterToMovedMethod(newMethod, ast.newSimpleName(argument),newMethods.get(newMethod));
															}
														}
													} else {
														if (!containsParameter(newMethods.get(newMethod),sourceTypeDeclaration.getName().getIdentifier())) {
															this.addSourceClassParameterToMovedMethod(newMethod, newMethods.get(newMethod), false);
														}
													}
												}
												for (String argument : this.additionalArgumentsAddedToMovedMethod.get(modifiedMethod)) {
													if (!checkedArguments.contains(argument)) {
														if (!argument.equalsIgnoreCase(sourceTypeDeclaration.getName().getIdentifier())) {
															arguments.insertLast(ast.newSimpleName(argument),null);
															checkedInvocations.add(invocation.resolveMethodBinding());
														} else {
															String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
															arguments.insertLast(ast.newSimpleName(sourceTypeName.replaceFirst(
																	Character.toString(sourceTypeName.charAt(0)), Character.toString(Character.toLowerCase(sourceTypeName.charAt(0))))), null);
															checkedInvocations.add(oldInvocation.resolveMethodBinding());
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			targetClassBodyRewrite.insertLast(newMethod, null);
		}
	}

	private FieldDeclaration sourceClassContainsSetter(String variableName) {
		MethodDeclaration[] sourceMethods = sourceTypeDeclaration.getMethods();
		boolean found = false;
		SimpleName fieldName = null;
		for(MethodDeclaration sourceMethod : sourceMethods) {
			fieldName = MethodDeclarationUtility.isSetter(sourceMethod);
			if(fieldName != null && fieldName.getIdentifier().equalsIgnoreCase(variableName)) {
				found = true;
				break;
			}
		}
		if(found) {
			FieldDeclaration[] sourceFields = sourceTypeDeclaration.getFields();
			for(FieldDeclaration sourceField : sourceFields) {
				List<VariableDeclarationFragment> fragments = sourceField.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					if(fragment.resolveBinding().isEqualTo(fieldName.resolveBinding())) {
						fragmentToAddSetter = fragment;
						return sourceField;
					}
				}
			}
		}
		else {
			FieldDeclaration[] sourceFields = sourceTypeDeclaration.getFields();
			for(FieldDeclaration sourceField : sourceFields) {
				List<VariableDeclarationFragment> fragments = sourceField.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					if(fragment.getName().getIdentifier().equalsIgnoreCase(variableName)) {
						fragmentToAddSetter = fragment;
						return sourceField;
					}
				}
			}
		}
		return null;
	}



	private boolean containsParameter(MethodDeclaration method, String identifier) {
		if(this.additionalParametersAddedToMovedMethod.containsKey(method)) {
			for(String parameter : this.additionalParametersAddedToMovedMethod.get(method)) {
				if(parameter.equalsIgnoreCase(identifier)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isDeclaredInSuperclass(MethodInvocation invocation, ITypeBinding type) {
		if(invocation.resolveMethodBinding().getDeclaringClass().isEqualTo(type.getSuperclass())) {
			return true;
		}
		else {
			type = type.getSuperclass();
			if(type.getName().equals("Object")) {
				return false;
			}
			else {
				return isDeclaredInSuperclass(invocation, type);
			}
		}
	}

	private boolean isParentAnonymousClassDeclaration(ASTNode node) {
		if(node.getParent() instanceof AnonymousClassDeclaration) {
			return true;
		}
		else if(node.getParent() instanceof CompilationUnit) {
			return false;
		}
		else {
			return isParentAnonymousClassDeclaration(node.getParent());
		}
	}

	private void replaceVariableInstructionWithGetter(SimpleName classVariable, AST ast,SimpleName variableInstruction, String variableName, ASTRewrite rewriter) {
		MethodInvocation getterInvocation = ast.newMethodInvocation();
		rewriter.set(getterInvocation, MethodInvocation.EXPRESSION_PROPERTY, classVariable, null);
		rewriter.set(getterInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("get" + variableName), null);
		rewriter.replace(variableInstruction, getterInvocation, null);
	}

	private void replaceAssignmentWithSetter(SimpleName classVariable, AST ast, Assignment assignment, String variableName, ASTRewrite rewriter) {
		MethodInvocation setterInvocation = ast.newMethodInvocation();
		rewriter.set(setterInvocation, MethodInvocation.EXPRESSION_PROPERTY, classVariable, null);
		rewriter.set(setterInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("set" + variableName), null);
		ListRewrite arguments = rewriter.getListRewrite(setterInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		arguments.insertLast(assignment.getRightHandSide(), null);
		rewriter.replace(assignment, setterInvocation, null);
	}

	private SimpleName addSourceClassParameterToMovedMethod(MethodDeclaration newMethodDeclaration, MethodDeclaration oldMethodDeclaration, boolean foundAnonymousClassDeclaration) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		SimpleName typeName = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
		Type parameterType = ast.newSimpleType(typeName);
		targetRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
		String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
		SimpleName parameterName = ast.newSimpleName(sourceTypeName.replaceFirst(Character.toString(sourceTypeName.charAt(0)), Character.toString(Character.toLowerCase(sourceTypeName.charAt(0)))));
		targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, parameterName, null);
		if(foundAnonymousClassDeclaration) {
			ListRewrite modifiers = targetRewriter.getListRewrite(parameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
			modifiers.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
		}
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		parametersRewrite.insertLast(parameter, null);
		LinkedList<String> arguments = null;
		if (!additionalArgumentsAddedToMovedMethod.containsKey(oldMethodDeclaration)) {
			arguments = new LinkedList<String>();
		}
		else {
			arguments = additionalArgumentsAddedToMovedMethod.get(oldMethodDeclaration);
		}
		LinkedList<String> parameters = null;
		if (!additionalParametersAddedToMovedMethod.containsKey(oldMethodDeclaration)) {
			parameters = new LinkedList<String>();
		}
		else {
			parameters = additionalParametersAddedToMovedMethod.get(oldMethodDeclaration);
		}
		if(!arguments.contains(sourceTypeDeclaration.getName().getIdentifier())) {
			arguments.add(sourceTypeDeclaration.getName().getIdentifier());
		}
		if(!parameters.contains(sourceTypeDeclaration.getName().getIdentifier())) {
			parameters.add(sourceTypeDeclaration.getName().getIdentifier());
		}
		this.additionalArgumentsAddedToMovedMethod.put(oldMethodDeclaration, arguments);
		this.additionalParametersAddedToMovedMethod.put(oldMethodDeclaration, parameters);
		setPublicModifierToSourceTypeDeclaration();
		return parameterName;
	}

	private void setPublicModifierToSourceTypeDeclaration() {
		PackageDeclaration sourcePackageDeclaration = sourceCompilationUnit.getPackage();
		PackageDeclaration targetPackageDeclaration = targetCompilationUnit.getPackage();
		if(sourcePackageDeclaration != null && targetPackageDeclaration != null) {
			String sourcePackageName = sourcePackageDeclaration.getName().getFullyQualifiedName();
			String targetPackageName = targetPackageDeclaration.getName().getFullyQualifiedName();
			if(!sourcePackageName.equals(targetPackageName)) {
				ListRewrite modifierRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
				Modifier publicModifier = sourceTypeDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
				boolean modifierFound = false;
				List<IExtendedModifier> modifiers = sourceTypeDeclaration.modifiers();
				for(IExtendedModifier extendedModifier : modifiers) {
					if(extendedModifier.isModifier()) {
						Modifier modifier = (Modifier)extendedModifier;
						if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
							modifierFound = true;
						}
						else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
								modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
							modifierFound = true;
							modifierRewrite.replace(modifier, publicModifier, null);
						}
					}
				}
				if(!modifierFound) {
					modifierRewrite.insertFirst(publicModifier, null);
				}
			}
		}
	}

	private void setPublicModifierToSourceMethod(MethodInvocation methodInvocation) {
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : methodDeclarations) {
			if(methodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
				ListRewrite modifierRewrite = sourceRewriter.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
				Modifier publicModifier = methodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
				boolean modifierFound = false;
				List<IExtendedModifier> modifiers = methodDeclaration.modifiers();
				for(IExtendedModifier extendedModifier : modifiers) {
					if(extendedModifier.isModifier()) {
						Modifier modifier = (Modifier)extendedModifier;
						if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD) ||
								modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
							modifierFound = true;
						}
						else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD)) {
							modifierFound = true;
							modifierRewrite.replace(modifier, publicModifier, null);
						}
					}
				}
				if(!modifierFound) {
					modifierRewrite.insertFirst(publicModifier, null);
				}
			}
		}
	}

	private ParameterizedType createParameterizedType(AST ast, ITypeBinding typeBinding) {
		ITypeBinding erasure = typeBinding.getErasure();
		ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
		ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(erasure.getName())));
		ListRewrite typeArgumentsRewrite = targetRewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		for(ITypeBinding typeArgument : typeArguments) {
			if(typeArgument.isClass())
				typeArgumentsRewrite.insertLast(ast.newSimpleType(ast.newSimpleName(typeArgument.getName())), null);
			else if(typeArgument.isParameterizedType()) {
				typeArgumentsRewrite.insertLast(createParameterizedType(ast, typeArgument), null);
			}
		}
		return parameterizedType;
	}

	private void addParameterToMovedMethod(MethodDeclaration newMethodDeclaration, SimpleName fieldName, MethodDeclaration oldMethodDeclaration) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		Type fieldType = null;
		FieldDeclaration[] fields = sourceTypeDeclaration.getFields();
		for(FieldDeclaration field : fields) {
			List<VariableDeclarationFragment> fragments = field.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(fragment.getName().getIdentifier().equals(fieldName.getIdentifier())) {
					fieldType = field.getType();
					break;
				}
			}
		}
		this.additionalTypeBindingsToBeImportedInTargetClass.add(fieldType.resolveBinding());
		if (!additionalArgumentsAddedToMovedMethod.containsKey(oldMethodDeclaration) || !additionalArgumentsAddedToMovedMethod.get(oldMethodDeclaration).contains(fieldName.getIdentifier())) {
			targetRewriter.set(parameter,SingleVariableDeclaration.TYPE_PROPERTY, fieldType,null);
			targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(fieldName.getIdentifier()), null);
			ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			parametersRewrite.insertLast(parameter, null);
			LinkedList<String> arguments = null;
			if (!additionalArgumentsAddedToMovedMethod.containsKey(oldMethodDeclaration)) {
				arguments = new LinkedList<String>();
			} else {
				arguments = additionalArgumentsAddedToMovedMethod.get(oldMethodDeclaration);
			}
			if (!arguments.contains(fieldName.getIdentifier())) {
				arguments.add(fieldName.getIdentifier());
			}
			this.additionalArgumentsAddedToMovedMethod.put(oldMethodDeclaration, arguments);
			LinkedList<String> parameters = null;
			if (!additionalParametersAddedToMovedMethod.containsKey(oldMethodDeclaration)) {
				parameters = new LinkedList<String>();
			} else {
				parameters = additionalParametersAddedToMovedMethod.get(oldMethodDeclaration);
			}
			if (!parameters.contains(fieldName.getIdentifier())) {
				parameters.add(fieldName.getIdentifier());
			}
			this.additionalParametersAddedToMovedMethod.put(oldMethodDeclaration, parameters);
		}
	}

	private void addFields() {
		AST contextAST = targetTypeDeclaration.getAST();
		ListRewrite targetClassBodyRewrite = targetRewriter.getListRewrite(targetTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		for(FieldDeclaration field : extractedFields) {
			targetClassBodyRewrite.insertLast(field, null);
		}

		for(FieldDeclaration field : extractedFields) {
			for(Object fragment : field.fragments()) {
				VariableDeclarationFragment variable = (VariableDeclarationFragment)fragment;
				String variableName = variable.getName().toString().replaceFirst(""+variable.getName().toString().charAt(0), ""+variable.getName().toString().toUpperCase().charAt(0));
				MethodDeclaration getterMethodDeclaration = contextAST.newMethodDeclaration();
				targetRewriter.set(getterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("get" + variableName), null);
				Type returnType = field.getType();
				targetRewriter.set(getterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
				ListRewrite getterMethodModifiersRewrite = targetRewriter.getListRewrite(getterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
				getterMethodModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);

				ReturnStatement returnStatement = contextAST.newReturnStatement();
				targetRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, variable.getName(), null);
				Block getterMethodBody = contextAST.newBlock();
				ListRewrite getterMethodBodyRewrite = targetRewriter.getListRewrite(getterMethodBody, Block.STATEMENTS_PROPERTY);
				getterMethodBodyRewrite.insertLast(returnStatement, null);
				targetRewriter.set(getterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, getterMethodBody, null);
				targetClassBodyRewrite.insertLast(getterMethodDeclaration, null);

				addSetter(contextAST, targetClassBodyRewrite, field, variable, variableName, targetRewriter);
			}
		}
	}

	private void addSetter(AST contextAST, ListRewrite targetClassBodyRewrite, FieldDeclaration field,
			VariableDeclarationFragment variable, String variableName, ASTRewrite rewriter) {
		MethodDeclaration setterMethodDeclaration = contextAST.newMethodDeclaration();
		rewriter.set(setterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("set" + variableName), null);
		PrimitiveType type = contextAST.newPrimitiveType(PrimitiveType.VOID);
		rewriter.set(setterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
		ListRewrite setterMethodModifiersRewrite = rewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		setterMethodModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
		rewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variable.getName(), null);
		rewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, field.getType(), null);
		ListRewrite setterMethodParameters = rewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		setterMethodParameters.insertLast(parameter, null);


		Assignment assignment = contextAST.newAssignment();
		ThisExpression thisExpression = contextAST.newThisExpression();
		FieldAccess fieldAccess = contextAST.newFieldAccess();
		rewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, thisExpression, null);
		rewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, variable.getName(), null);
		rewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAccess, null);
		rewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
		rewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, variable.getName(), null);
		ExpressionStatement expressionStatement = contextAST.newExpressionStatement(assignment);
		Block setterMethodBody = contextAST.newBlock();
		ListRewrite setterMethodBodyRewrite = rewriter.getListRewrite(setterMethodBody, Block.STATEMENTS_PROPERTY);
		setterMethodBodyRewrite.insertLast(expressionStatement, null);
		rewriter.set(setterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, setterMethodBody, null);
		targetClassBodyRewrite.insertLast(setterMethodDeclaration, null);
	}


	@SuppressWarnings("restriction")
	private void addNewType() {
		IContainer contextContainer = (IContainer)sourceFile.getParent();
		if(contextContainer instanceof IProject) {
			IProject contextProject = (IProject)contextContainer;
			targetFile = contextProject.getFile(this.targetTypeName+".java");
		}
		else if(contextContainer instanceof IFolder) {
			IFolder contextFolder = (IFolder)contextContainer;
			targetFile = contextFolder.getFile(this.targetTypeName+".java");
		}
		targetICompilationUnit = JavaCore.createCompilationUnitFrom(targetFile);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		targetDocument = new Document();
		parser.setSource(targetDocument.get().toCharArray());

		targetCompilationUnit = (CompilationUnit)parser.createAST(null);
		AST targetAST = targetCompilationUnit.getAST();

		this.targetRewriter = ASTRewrite.create(targetAST);
		ListRewrite targetTypesRewrite = targetRewriter.getListRewrite(targetCompilationUnit, CompilationUnit.TYPES_PROPERTY);
		targetTypeDeclaration = null;
		if(sourceCompilationUnit.getPackage() != null) {
			targetRewriter.set(targetCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
		}
		targetTypeDeclaration = targetAST.newTypeDeclaration();
		SimpleName targetName = targetAST.newSimpleName(targetTypeName);
		targetRewriter.set(targetTypeDeclaration, TypeDeclaration.NAME_PROPERTY, targetName, null);
		ListRewrite targetModifiersRewrite = targetRewriter.getListRewrite(targetTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
		targetModifiersRewrite.insertLast(targetAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);

		targetTypesRewrite.insertLast(targetTypeDeclaration, null);
	}

	private void addRequiredTargetImportDeclarations() {
		List<ITypeBinding> typeBindings = new ArrayList<ITypeBinding>();
		TypeVisitor typeVisitor = new TypeVisitor();
		for (MethodDeclaration method : extractedMethods) {
			method.accept(typeVisitor);
			for(ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
				typeBindings.add(typeBinding);
			}
		}

		for(FieldDeclaration field : extractedFields) {
			field.accept(typeVisitor);
			for(ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
				typeBindings.add(typeBinding);
			}
		}

		for(ITypeBinding typeBinding : additionalTypeBindingsToBeImportedInTargetClass) {
			typeBindings.add(typeBinding);
		}

		getSimpleTypeBindings(typeBindings);
		for(ITypeBinding typeBinding : requiredTargetImportDeclarationSet)
			addImportDeclaration(typeBinding);
	}

	private void addImportDeclaration(ITypeBinding typeBinding) {
		String qualifiedName = typeBinding.getQualifiedName();
		String qualifiedPackageName = "";
		if(qualifiedName.contains("."))
			qualifiedPackageName = qualifiedName.substring(0,qualifiedName.lastIndexOf("."));
		PackageDeclaration targetPackageDeclaration = targetCompilationUnit.getPackage();
		String targetPackageDeclarationName = "";
		if(targetPackageDeclaration != null)
			targetPackageDeclarationName = targetPackageDeclaration.getName().getFullyQualifiedName();	
		if(!qualifiedPackageName.equals("") && !qualifiedPackageName.equals("java.lang") && !qualifiedPackageName.equals(targetPackageDeclarationName)) {
			List<ImportDeclaration> importDeclarationList = targetCompilationUnit.imports();
			boolean found = false;
			for(ImportDeclaration importDeclaration : importDeclarationList) {
				if(!importDeclaration.isOnDemand()) {
					if(qualifiedName.equals(importDeclaration.getName().getFullyQualifiedName())) {
						found = true;
						break;
					}
				}
				else {
					if(qualifiedPackageName.equals(importDeclaration.getName().getFullyQualifiedName())) {
						found = true;
						break;
					}
				}
			}
			if(!found) {

				AST ast = targetRewriter.getAST();
				ImportDeclaration importDeclaration = ast.newImportDeclaration();
				targetRewriter.set(importDeclaration, ImportDeclaration.NAME_PROPERTY, ast.newName(qualifiedName), null);
				ListRewrite importRewrite = targetRewriter.getListRewrite(targetCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
				importRewrite.insertLast(importDeclaration, null);
			}
		}
	}

	private void getSimpleTypeBindings(List<ITypeBinding> typeBindings) {
		for(ITypeBinding typeBinding : typeBindings) {
			if(typeBinding.isPrimitive()) {

			}
			else if(typeBinding.isArray()) {
				ITypeBinding elementTypeBinding = typeBinding.getElementType();
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(elementTypeBinding);
				getSimpleTypeBindings(typeBindingList);
			}
			else if(typeBinding.isParameterizedType()) {
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(typeBinding.getTypeDeclaration());
				ITypeBinding[] typeArgumentBindings = typeBinding.getTypeArguments();
				for(ITypeBinding typeArgumentBinding : typeArgumentBindings)
					typeBindingList.add(typeArgumentBinding);
				getSimpleTypeBindings(typeBindingList);
			}
			else if(typeBinding.isWildcardType()) {
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(typeBinding.getBound());
				getSimpleTypeBindings(typeBindingList);
			}
			else {
				if(typeBinding.isNested()) {
					requiredTargetImportDeclarationSet.add(typeBinding.getDeclaringClass());
				}
				requiredTargetImportDeclarationSet.add(typeBinding);
			}
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
	throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking postconditions...", 2);
			apply();
		} finally {
			pm.done();
		}
		return status;
	}


	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
	throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
	OperationCanceledException {
		try {
			pm.beginTask("Creating change...", 1);
			final Collection<Change> changes = new ArrayList<Change>();
			changes.addAll(createCompilationUnitChanges.values());
			changes.addAll(textFileChanges.values());
			changes.addAll(fChanges.values());
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Extracting class from ''{0}''", new Object[] { sourceTypeDeclaration.getName().toString()});
					String comment = null;
					return new RefactoringChangeDescriptor(new ExtractClassRefactoringDescriptor(project, description, comment,
							sourceCompilationUnit, sourceTypeDeclaration, sourceFile, extractedEntities, leaveDelegateInput, targetTypeName));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}

	@Override
	public String getName() {
		return "Extract Class";
	}
}
