package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.ConstructorInvocationObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.LibraryClassStorage;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.VariableDeclarationObject;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementType;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jface.preference.IPreferenceStore;

public class MethodCallAnalyzer {
	private Set<AbstractVariable> definedVariables;
	private Set<AbstractVariable> usedVariables;
	private Set<String> thrownExceptionTypes;
	private Set<VariableDeclarationObject> variableDeclarationsInMethod;
	private int maximumCallGraphAnalysisDepth;
	
	public MethodCallAnalyzer(Set<AbstractVariable> definedVariables,
			Set<AbstractVariable> usedVariables,
			Set<String> thrownExceptionTypes,
			Set<VariableDeclarationObject> variableDeclarationsInMethod) {
		this.definedVariables = definedVariables;
		this.usedVariables = usedVariables;
		this.thrownExceptionTypes = thrownExceptionTypes;
		this.variableDeclarationsInMethod = variableDeclarationsInMethod;
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		this.maximumCallGraphAnalysisDepth = store.getInt(PreferenceConstants.P_MAXIMUM_CALL_GRAPH_ANALYSIS_DEPTH);
	}

	public void processArgumentsOfInternalMethodInvocation(ClassObject classObject, AbstractMethodDeclaration methodObject,
			List<Expression> arguments, IMethodBinding invokedMethodBinding, AbstractVariable variable) {
		if(methodObject != null) {
			CompilationUnitCache cache = CompilationUnitCache.getInstance();
			if(cache.containsMethodExpression(methodObject)) {
				for(AbstractVariable usedField : cache.getUsedFieldsForMethodExpression(methodObject)) {
					AbstractVariable field = null;
					if(variable != null)
						field = composeVariable(variable, usedField);
					else
						field = usedField;
					usedVariables.add(field);
				}
				for(AbstractVariable definedField : cache.getDefinedFieldsForMethodExpression(methodObject)) {
					AbstractVariable field = null;
					if(variable != null)
						field = composeVariable(variable, definedField);
					else
						field = definedField;
					definedVariables.add(field);
				}
				thrownExceptionTypes.addAll(cache.getThrownExceptionTypesForMethodExpression(methodObject));
			}
			else {
				Set<AbstractVariable> usedVariablesBefore = new LinkedHashSet<AbstractVariable>(this.usedVariables);
				Set<AbstractVariable> definedVariablesBefore = new LinkedHashSet<AbstractVariable>(this.definedVariables);
				Set<String> thrownExceptionTypesBefore = new LinkedHashSet<String>(this.thrownExceptionTypes);
				processInternalMethodInvocation(classObject, methodObject, variable, new LinkedHashSet<String>());
				//save in cache
				Set<AbstractVariable> usedVariablesAfter = new LinkedHashSet<AbstractVariable>(this.usedVariables);
				usedVariablesAfter.removeAll(usedVariablesBefore);
				int usedFieldCount = 0;
				for(AbstractVariable usedField : usedVariablesAfter) {
					if(usedField instanceof PlainVariable) {
						cache.addUsedFieldForMethodExpression(usedField, methodObject);
						usedFieldCount++;
					}
					else if(usedField instanceof CompositeVariable) {
						CompositeVariable composite = (CompositeVariable)usedField;
						if(variable != null && composite.startsWithVariable(variable)) {
							//getRightPart() is not correct if variable is a CompositeVariable
							cache.addUsedFieldForMethodExpression(composite.getRightPartAfterPrefix(variable), methodObject);
							usedFieldCount++;
						}
						else {
							cache.addUsedFieldForMethodExpression(usedField, methodObject);
							usedFieldCount++;
						}
					}
				}
				if(usedFieldCount == 0) {
					cache.setEmptyUsedFieldsForMethodExpression(methodObject);
				}
				Set<AbstractVariable> definedVariablesAfter = new LinkedHashSet<AbstractVariable>(this.definedVariables);
				definedVariablesAfter.removeAll(definedVariablesBefore);
				int definedFieldCount = 0;
				for(AbstractVariable definedField : definedVariablesAfter) {
					if(definedField instanceof PlainVariable) {
						cache.addDefinedFieldForMethodExpression(definedField, methodObject);
						definedFieldCount++;
					}
					else if(definedField instanceof CompositeVariable) {
						CompositeVariable composite = (CompositeVariable)definedField;
						if(variable != null && composite.startsWithVariable(variable)) {
							//getRightPart() is not correct if variable is a CompositeVariable
							cache.addDefinedFieldForMethodExpression(composite.getRightPartAfterPrefix(variable), methodObject);
							definedFieldCount++;
						}
						else {
							cache.addDefinedFieldForMethodExpression(definedField, methodObject);
							definedFieldCount++;
						}
					}
				}
				if(definedFieldCount == 0) {
					cache.setEmptyDefinedFieldsForMethodExpression(methodObject);
				}
				LinkedHashSet<String> thrownExceptionTypesAfter = new LinkedHashSet<String>(this.thrownExceptionTypes);
				thrownExceptionTypesAfter.removeAll(thrownExceptionTypesBefore);
				cache.setThrownExceptionTypesForMethodExpression(methodObject, thrownExceptionTypesAfter);
			}
			int argumentPosition = 0;
			for(Expression argument : arguments) {
				if(argument instanceof SimpleName) {
					SimpleName argumentName = (SimpleName)argument;
					VariableDeclaration argumentDeclaration = null;
					for(VariableDeclarationObject variableDeclarationObject : variableDeclarationsInMethod) {
						VariableDeclaration variableDeclaration = variableDeclarationObject.getVariableDeclaration();
						if(variableDeclaration.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
							argumentDeclaration = variableDeclaration;
							break;
						}
					}
					if(argumentDeclaration != null) {
						MethodDeclaration methodDeclaration = methodObject.getMethodDeclaration();
						String methodBindingKey = methodDeclaration.resolveBinding().getKey();
						if(cache.containsMethodArgument(methodBindingKey, argumentPosition)) {
							for(AbstractVariable usedField : cache.getUsedFieldsForMethodArgument(methodBindingKey, argumentPosition)) {
								PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
								AbstractVariable composedVariable = composeVariable(argumentVariable, usedField);
								usedVariables.add(composedVariable);
							}
							for(AbstractVariable definedField : cache.getDefinedFieldsForMethodArgument(methodBindingKey, argumentPosition)) {
								PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
								AbstractVariable composedVariable = composeVariable(argumentVariable, definedField);
								definedVariables.add(composedVariable);
							}
						}
						else {
							ParameterObject parameter = methodObject.getParameter(argumentPosition);
							//analyze only if the argument does not correspond to a varargs parameter
							if(argumentPosition < methodObject.getParameterList().size()) {
								VariableDeclaration parameterDeclaration = parameter.getSingleVariableDeclaration();
								PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
								processArgumentOfInternalMethodInvocation(methodObject, argumentVariable, argumentPosition, parameterDeclaration, new LinkedHashSet<String>());
								//save in cache
								int usedFieldCount = 0;
								for(AbstractVariable usedVariable : usedVariables) {
									if(usedVariable instanceof CompositeVariable) {
										CompositeVariable composite = (CompositeVariable)usedVariable;
										if(composite.getInitialVariable().equals(argumentVariable)) {
											cache.addUsedFieldForMethodArgument(composite.getRightPart(), methodDeclaration, argumentPosition);
											usedFieldCount++;
										}
									}
								}
								if(usedFieldCount == 0) {
									cache.setEmptyUsedFieldsForMethodArgument(methodDeclaration, argumentPosition);
								}
								int definedFieldCount = 0;
								for(AbstractVariable definedVariable : definedVariables) {
									if(definedVariable instanceof CompositeVariable) {
										CompositeVariable composite = (CompositeVariable)definedVariable;
										if(composite.getInitialVariable().equals(argumentVariable)) {
											cache.addDefinedFieldForMethodArgument(composite.getRightPart(), methodDeclaration, argumentPosition);
											definedFieldCount++;
										}
									}
								}
								if(definedFieldCount == 0) {
									cache.setEmptyDefinedFieldsForMethodArgument(methodDeclaration, argumentPosition);
								}
							}
						}
					}
				}
				argumentPosition++;
			}
		}
		else {
			LibraryClassStorage instance = LibraryClassStorage.getInstance();
			if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
				handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), variable, instance);
				int argumentPosition = 0;
				for(Expression argument : arguments) {
					if(argument instanceof SimpleName) {
						SimpleName argumentName = (SimpleName)argument;
						VariableDeclaration argumentDeclaration = null;
						for(VariableDeclarationObject variableDeclarationObject : variableDeclarationsInMethod) {
							VariableDeclaration variableDeclaration = variableDeclarationObject.getVariableDeclaration();
							if(variableDeclaration.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
								argumentDeclaration = variableDeclaration;
								break;
							}
						}
						if(argumentDeclaration != null) {
							if(instance.containsMethodArgument(invokedMethodBinding.getKey(), argumentPosition)) {
								for(AbstractVariable usedField : instance.getUsedFieldsForMethodArgument(invokedMethodBinding.getKey(), argumentPosition)) {
									PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
									AbstractVariable composedVariable = composeVariable(argumentVariable, usedField);
									usedVariables.add(composedVariable);
								}
								for(AbstractVariable definedField : instance.getDefinedFieldsForMethodArgument(invokedMethodBinding.getKey(), argumentPosition)) {
									PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
									AbstractVariable composedVariable = composeVariable(argumentVariable, definedField);
									definedVariables.add(composedVariable);
								}
							}
						}
					}
					argumentPosition++;
				}
			}
			else {
				MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
				if(invokedMethodDeclaration != null) {
					processExternalMethodInvocation(invokedMethodDeclaration, variable, new LinkedHashSet<String>(), 0);
					int argumentPosition = 0;
					for(Expression argument : arguments) {
						if(argument instanceof SimpleName) {
							SimpleName argumentName = (SimpleName)argument;
							VariableDeclaration argumentDeclaration = null;
							for(VariableDeclarationObject variableDeclarationObject : variableDeclarationsInMethod) {
								VariableDeclaration variableDeclaration = variableDeclarationObject.getVariableDeclaration();
								if(variableDeclaration.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
									argumentDeclaration = variableDeclaration;
									break;
								}
							}
							if(argumentDeclaration != null) {
								//analyze only if the argument does not correspond to a varargs parameter
								if(argumentPosition < invokedMethodDeclaration.parameters().size()) {
									VariableDeclaration parameterDeclaration = (SingleVariableDeclaration)invokedMethodDeclaration.parameters().get(argumentPosition);
									PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
									processArgumentOfExternalMethodInvocation(invokedMethodDeclaration, argumentVariable, argumentPosition, parameterDeclaration, new LinkedHashSet<String>(), 0);
									//save in cache
									int usedFieldCount = 0;
									for(AbstractVariable usedVariable : usedVariables) {
										if(usedVariable instanceof CompositeVariable) {
											CompositeVariable composite = (CompositeVariable)usedVariable;
											if(composite.getInitialVariable().equals(argumentVariable)) {
												instance.addUsedFieldForMethodArgument(composite.getRightPart(), invokedMethodDeclaration, argumentPosition);
												usedFieldCount++;
											}
										}
									}
									if(usedFieldCount == 0) {
										instance.setEmptyUsedFieldsForMethodArgument(invokedMethodDeclaration, argumentPosition);
									}
									int definedFieldCount = 0;
									for(AbstractVariable definedVariable : definedVariables) {
										if(definedVariable instanceof CompositeVariable) {
											CompositeVariable composite = (CompositeVariable)definedVariable;
											if(composite.getInitialVariable().equals(argumentVariable)) {
												instance.addDefinedFieldForMethodArgument(composite.getRightPart(), invokedMethodDeclaration, argumentPosition);
												definedFieldCount++;
											}
										}
									}
									if(definedFieldCount == 0) {
										instance.setEmptyDefinedFieldsForMethodArgument(invokedMethodDeclaration, argumentPosition);
									}
								}
							}
						}
						argumentPosition++;
					}
				}
			}
		}
	}

	private void processArgumentOfInternalMethodInvocation(AbstractMethodDeclaration methodObject, AbstractVariable argumentDeclaration, int initialArgumentPosition,
			VariableDeclaration parameterDeclaration, Set<String> processedMethods) {
		SystemObject systemObject = ASTReader.getSystemObject();
		if(methodObject.getMethodBody() == null) {
			IMethodBinding superMethodDeclarationBinding = methodObject.getMethodDeclaration().resolveBinding();
			IType superType = (IType)superMethodDeclarationBinding.getDeclaringClass().getJavaElement();
			processedMethods.add(superMethodDeclarationBinding.getKey());
			Set<IType> subTypes = CompilationUnitCache.getInstance().getSubTypes(superType);
			for(IType subType : subTypes) {
				ClassObject subClassObject = systemObject.getClassObject(subType.getFullyQualifiedName('.'));
				if(subClassObject != null) {
					ListIterator<MethodObject> methodIterator = subClassObject.getMethodIterator();
					while(methodIterator.hasNext()) {
						MethodObject subMethod = methodIterator.next();
						MethodDeclaration subMethodDeclaration = subMethod.getMethodDeclaration();
						if(equalSignature(subMethodDeclaration.resolveBinding(), superMethodDeclarationBinding)) {
							ParameterObject parameterObject = subMethod.getParameter(initialArgumentPosition);
							VariableDeclaration parameterDeclaration2 = parameterObject.getSingleVariableDeclaration();
							if(isUnprocessedMethod(processedMethods, subMethodDeclaration.resolveBinding()))
								processArgumentOfInternalMethodInvocation(subMethod, argumentDeclaration, initialArgumentPosition, parameterDeclaration2, processedMethods);
							break;
						}
					}
				}
			}
		}
		else {
			for(AbstractVariable originalField : methodObject.getDefinedFieldsThroughParameters()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = new CompositeVariable(argumentDeclaration, ((CompositeVariable)originalField).getRightPart());
					definedVariables.add(field);
				}
			}
			for(AbstractVariable originalField : methodObject.getUsedFieldsThroughParameters()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = new CompositeVariable(argumentDeclaration, ((CompositeVariable)originalField).getRightPart());
					usedVariables.add(field);
				}
			}
			processedMethods.add(methodObject.getMethodDeclaration().resolveBinding().getKey());
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = methodObject.getInvokedMethodsThroughParameters();
			for(AbstractVariable originalField : invokedMethodsThroughParameters.keySet()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = null;
					if(originalField instanceof PlainVariable)
						field = argumentDeclaration;
					else
						field = new CompositeVariable(argumentDeclaration, ((CompositeVariable)originalField).getRightPart());
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(originalField);
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
						ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
							if(methodObject2 != null) {
								processInternalMethodInvocation(classObject2, methodObject2, field, new LinkedHashSet<String>());
							}
						}
						else {
							LibraryClassStorage instance = LibraryClassStorage.getInstance();
							IMethodBinding invokedMethodBinding = methodInvocation2.resolveMethodBinding();
							if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
								handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), field, instance);
							}
							else {
								MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
								if(invokedMethodDeclaration != null)
									processExternalMethodInvocation(invokedMethodDeclaration, field, new LinkedHashSet<String>(), 0);
							}
						}
					}
				}
			}
			Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> parametersPassedAsArgumentsInMethodInvocations = methodObject.getParametersPassedAsArgumentsInMethodInvocations();
			for(PlainVariable parameter : parametersPassedAsArgumentsInMethodInvocations.keySet()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(parameter.getVariableBindingKey())) {
					LinkedHashSet<MethodInvocationObject> methodInvocations = parametersPassedAsArgumentsInMethodInvocations.get(parameter);
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
							if(methodObject2 != null && !methodObject2.equals(methodObject)) {
								MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
								int argumentPosition = getArgumentPosition(methodInvocation2.arguments(), parameter);
								ParameterObject parameterObject = methodObject2.getParameter(argumentPosition);
								//fix for method calls with varargs
								if(parameterObject != null) {
									VariableDeclaration parameterDeclaration2 = parameterObject.getSingleVariableDeclaration();
									if(isUnprocessedMethod(processedMethods, methodInvocation2.resolveMethodBinding()))
										processArgumentOfInternalMethodInvocation(methodObject2, argumentDeclaration, argumentPosition, parameterDeclaration2, processedMethods);
								}
							}
						}
					}
				}
			}
			Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> parametersPassedAsArgumentsInSuperMethodInvocations = methodObject.getParametersPassedAsArgumentsInSuperMethodInvocations();
			for(PlainVariable parameter : parametersPassedAsArgumentsInSuperMethodInvocations.keySet()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(parameter.getVariableBindingKey())) {
					LinkedHashSet<SuperMethodInvocationObject> superMethodInvocations = parametersPassedAsArgumentsInSuperMethodInvocations.get(parameter);
					for(SuperMethodInvocationObject superMethodInvocationObject : superMethodInvocations) {
						ClassObject classObject2 = systemObject.getClassObject(superMethodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(superMethodInvocationObject);
							if(methodObject2 != null) {
								SuperMethodInvocation superMethodInvocation = superMethodInvocationObject.getSuperMethodInvocation();
								int argumentPosition = getArgumentPosition(superMethodInvocation.arguments(), parameter);
								ParameterObject parameterObject = methodObject2.getParameter(argumentPosition);
								VariableDeclaration parameterDeclaration2 = parameterObject.getSingleVariableDeclaration();
								if(isUnprocessedMethod(processedMethods, superMethodInvocation.resolveMethodBinding()))
									processArgumentOfInternalMethodInvocation(methodObject2, argumentDeclaration, argumentPosition, parameterDeclaration2, processedMethods);
							}
						}
					}
				}
			}
		}
	}

	private void processArgumentOfExternalMethodInvocation(MethodDeclaration methodDeclaration, AbstractVariable argumentDeclaration, int initialArgumentPosition,
			VariableDeclaration parameterDeclaration, Set<String> processedMethods, int depth) {
		LibraryClassStorage instance = LibraryClassStorage.getInstance();
		IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		Block methodBody = methodDeclaration.getBody();
		if(methodBody != null) {
			IMethod iMethod = (IMethod)methodBinding.getJavaElement();
			IClassFile iClassFile = iMethod.getClassFile();
			ASTInformationGenerator.setCurrentITypeRoot(iClassFile);
			MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);

			for(AbstractVariable originalField : methodBodyObject.getDefinedFieldsThroughParameters()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = new CompositeVariable(argumentDeclaration, ((CompositeVariable)originalField).getRightPart());
					definedVariables.add(field);
				}
			}
			for(AbstractVariable originalField : methodBodyObject.getUsedFieldsThroughParameters()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = new CompositeVariable(argumentDeclaration, ((CompositeVariable)originalField).getRightPart());
					usedVariables.add(field);
				}
			}
			processedMethods.add(methodBinding.getKey());
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = methodBodyObject.getInvokedMethodsThroughParameters();
			for(AbstractVariable originalField : invokedMethodsThroughParameters.keySet()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = null;
					if(originalField instanceof PlainVariable)
						field = argumentDeclaration;
					else
						field = new CompositeVariable(argumentDeclaration, ((CompositeVariable)originalField).getRightPart());
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(originalField);
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
						IMethodBinding invokedMethodBinding = methodInvocation2.resolveMethodBinding();
						if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
							handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), field, instance);
						}
						else {
							MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
							if(invokedMethodDeclaration != null)
								processExternalMethodInvocation(invokedMethodDeclaration, field, new LinkedHashSet<String>(), depth);
						}
					}
				}
			}
		}
	}

	private int getArgumentPosition(List<Expression> arguments, PlainVariable argument) {
		int argumentPostion = 0;
		for(Expression arg : arguments) {
			if(arg instanceof SimpleName) {
				SimpleName argName = (SimpleName)arg;
				if(argName.resolveBinding().getKey().equals(argument.getVariableBindingKey()))
					return argumentPostion;
			}
			argumentPostion++;
		}
		return -1;
	}

	private void processInternalMethodInvocation(ClassObject classObject, AbstractMethodDeclaration methodObject, AbstractVariable variableDeclaration, Set<String> processedMethods) {
		SystemObject systemObject = ASTReader.getSystemObject();
		if(methodObject.isAbstract() || classObject.isInterface()) {
			AbstractTypeDeclaration typeDeclaration = classObject.getAbstractTypeDeclaration();
			IMethodBinding superMethodDeclarationBinding = methodObject.getMethodDeclaration().resolveBinding();
			if(isUnprocessedMethod(processedMethods, superMethodDeclarationBinding)) {
				IType superType = (IType)typeDeclaration.resolveBinding().getJavaElement();
				processedMethods.add(superMethodDeclarationBinding.getKey());
				Set<IType> subTypes = CompilationUnitCache.getInstance().getSubTypes(superType);
				Set<IType> subTypesToBeAnalyzed = new LinkedHashSet<IType>();
				if(variableDeclaration != null) {
					String initialReferenceType = variableDeclaration.getVariableType();
					for(IType subType : subTypes) {
						if(subType.getFullyQualifiedName('.').equals(initialReferenceType)) {
							subTypesToBeAnalyzed.add(subType);
							break;
						}
					}
					if(subTypesToBeAnalyzed.isEmpty())
						subTypesToBeAnalyzed.addAll(subTypes);
				}
				else
					subTypesToBeAnalyzed.addAll(subTypes);
				for(IType subType : subTypesToBeAnalyzed) {
					ClassObject subClassObject = systemObject.getClassObject(subType.getFullyQualifiedName('.'));
					if(subClassObject != null) {
						ListIterator<MethodObject> methodIterator = subClassObject.getMethodIterator();
						while(methodIterator.hasNext()) {
							MethodObject subMethod = methodIterator.next();
							if(equalSignature(subMethod.getMethodDeclaration().resolveBinding(), superMethodDeclarationBinding)) {
								processInternalMethodInvocation(subClassObject, subMethod, variableDeclaration, processedMethods);
								break;
							}
						}
					}
				}
			}
		}
		else {
			for(PlainVariable originalField : methodObject.getDefinedFieldsThroughThisReference()) {
				boolean alreadyContainsOriginalField = false;
				if(variableDeclaration != null && originalField instanceof PlainVariable) {
					if(variableDeclaration.containsPlainVariable((PlainVariable)originalField))
						alreadyContainsOriginalField = true;
				}
				if(!alreadyContainsOriginalField) {
					AbstractVariable field = null;
					if(variableDeclaration != null)
						field = composeVariable(variableDeclaration, originalField);
					else
						field = originalField;
					definedVariables.add(field);
				}
			}
			for(PlainVariable originalField : methodObject.getUsedFieldsThroughThisReference()) {
				boolean alreadyContainsOriginalField = false;
				if(variableDeclaration != null && originalField instanceof PlainVariable) {
					if(variableDeclaration.containsPlainVariable((PlainVariable)originalField))
						alreadyContainsOriginalField = true;
				}
				if(!alreadyContainsOriginalField) {
					AbstractVariable field = null;
					if(variableDeclaration != null)
						field = composeVariable(variableDeclaration, originalField);
					else
						field = originalField;
					usedVariables.add(field);
				}
			}
			thrownExceptionTypes.addAll(methodObject.getExceptionsInThrowStatements());
			thrownExceptionTypes.addAll(methodObject.getExceptionsInJavaDocThrows());
			processedMethods.add(methodObject.getMethodDeclaration().resolveBinding().getKey());
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = methodObject.getInvokedMethodsThroughFields();
			for(AbstractVariable originalField : invokedMethodsThroughFields.keySet()) {
				boolean alreadyContainsOriginalField = false;
				if(variableDeclaration != null && originalField instanceof PlainVariable) {
					if(variableDeclaration.containsPlainVariable((PlainVariable)originalField))
						alreadyContainsOriginalField = true;
				}
				if(!alreadyContainsOriginalField) {
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(originalField);
					AbstractVariable field = null;
					if(variableDeclaration != null)
						field = composeVariable(variableDeclaration, originalField);
					else
						field = originalField;
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
						ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
							if(methodObject2 != null) {
								if(isUnprocessedMethod(processedMethods, methodInvocation2.resolveMethodBinding()))
									processInternalMethodInvocation(classObject2, methodObject2, field, processedMethods);
							}
						}
						else {
							LibraryClassStorage instance = LibraryClassStorage.getInstance();
							IMethodBinding invokedMethodBinding = methodInvocation2.resolveMethodBinding();
							if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
								handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), field, instance);
							}
							else {
								MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
								if(invokedMethodDeclaration != null)
									processExternalMethodInvocation(invokedMethodDeclaration, field, new LinkedHashSet<String>(), 0);
							}
						}
					}
				}
			}
			for(MethodInvocationObject methodInvocationObject : methodObject.getInvokedMethodsThroughThisReference()) {
				MethodObject methodObject2 = classObject.getMethod(methodInvocationObject);
				if(methodObject2 != null) { 
					if(!methodObject2.equals(methodObject)) {
						MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
						if(isUnprocessedMethod(processedMethods, methodInvocation2.resolveMethodBinding()))
							processInternalMethodInvocation(classObject, methodObject2, variableDeclaration, processedMethods);
					}
				}
				else {
					//the invoked method is an inherited method
					ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
					if(classObject2 != null) {
						methodObject2 = classObject2.getMethod(methodInvocationObject);
						if(methodObject2 != null) {
							thrownExceptionTypes.addAll(methodObject2.getExceptionsInThrowStatements());
							thrownExceptionTypes.addAll(methodObject2.getExceptionsInJavaDocThrows());
							//the commented code that follows is causing significant performance deterioration. It's time to reconsider the PDG generation strategy
							/*
							MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
							if(isUnprocessedMethod(processedMethods, methodInvocation2.resolveMethodBinding()))
								processInternalMethodInvocation(classObject2, methodObject2, variableDeclaration, processedMethods);
							*/
						}
					}
				}
			}
			for(SuperMethodInvocationObject superMethodInvocationObject : methodObject.getSuperMethodInvocations()) {
				ClassObject classObject2 = systemObject.getClassObject(superMethodInvocationObject.getOriginClassName());
				if(classObject2 != null) {
					MethodObject methodObject2 = classObject2.getMethod(superMethodInvocationObject);
					if(methodObject2 != null) {
						SuperMethodInvocation superMethodInvocation = superMethodInvocationObject.getSuperMethodInvocation();
						if(isUnprocessedMethod(processedMethods, superMethodInvocation.resolveMethodBinding()))
							processInternalMethodInvocation(classObject2, methodObject2, variableDeclaration, processedMethods);
					}
				}
			}
			for(ConstructorInvocationObject constructorInvocationObject : methodObject.getConstructorInvocations()) {
				ClassObject classObject2 = systemObject.getClassObject(constructorInvocationObject.getOriginClassName());
				if(classObject2 != null) {
					ConstructorObject constructorObject2 = classObject2.getConstructor(constructorInvocationObject);
					if(constructorObject2 != null) {
						ConstructorInvocation constructorInvocation = constructorInvocationObject.getConstructorInvocation();
						if(isUnprocessedMethod(processedMethods, constructorInvocation.resolveConstructorBinding()))
							processInternalMethodInvocation(classObject2, constructorObject2, variableDeclaration, processedMethods);
					}
				}
			}
			for(MethodInvocationObject staticMethodInvocationObject : methodObject.getInvokedStaticMethods()) {
				MethodInvocation staticMethodInvocation = staticMethodInvocationObject.getMethodInvocation();
				ClassObject classObject2 = systemObject.getClassObject(staticMethodInvocationObject.getOriginClassName());
				if(classObject2 != null) {
					MethodObject methodObject2 = classObject2.getMethod(staticMethodInvocationObject);
					if(methodObject2 != null) {
						if(isUnprocessedMethod(processedMethods, staticMethodInvocation.resolveMethodBinding()))
							processInternalMethodInvocation(classObject2, methodObject2, null, processedMethods);
					}
				}
				else {
					LibraryClassStorage instance = LibraryClassStorage.getInstance();
					IMethodBinding invokedMethodBinding = staticMethodInvocation.resolveMethodBinding();
					if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
						handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), null, instance);
					}
					else {
						MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
						if(invokedMethodDeclaration != null)
							processExternalMethodInvocation(invokedMethodDeclaration, null, new LinkedHashSet<String>(), 0);
					}
				}
			}
		}
	}

	private void processExternalMethodInvocation(MethodDeclaration methodDeclaration, AbstractVariable variableDeclaration,
			Set<String> processedMethods, int depth) {
		LibraryClassStorage instance = LibraryClassStorage.getInstance();
		IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		if(instance.isAnalyzed(methodBinding.getKey())) {
			handleAlreadyAnalyzedMethod(methodBinding.getKey(), variableDeclaration, instance);
		}
		else {
			Block methodBody = methodDeclaration.getBody();
			if(methodBody != null) {
				IMethod iMethod = (IMethod)methodBinding.getJavaElement();
				IClassFile iClassFile = iMethod.getClassFile();
				ASTInformationGenerator.setCurrentITypeRoot(iClassFile);
				MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
				LinkedHashSet<PlainVariable> definedFields = new LinkedHashSet<PlainVariable>();
				LinkedHashSet<PlainVariable> usedFields = new LinkedHashSet<PlainVariable>();
				for(PlainVariable originalField : methodBodyObject.getDefinedFieldsThroughThisReference()) {
					AbstractVariable field = composeVariable(variableDeclaration, originalField);
					definedVariables.add(field);
					definedFields.add(originalField);
				}
				for(PlainVariable originalField : methodBodyObject.getUsedFieldsThroughThisReference()) {
					AbstractVariable field = composeVariable(variableDeclaration, originalField);
					usedVariables.add(field);
					usedFields.add(originalField);
				}
				instance.setDefinedFields(methodDeclaration, definedFields);
				instance.setUsedFields(methodDeclaration, usedFields);
				instance.setThrownExceptionTypes(methodDeclaration, new LinkedHashSet<String>(methodBodyObject.getExceptionsInThrowStatements()));
				processedMethods.add(methodDeclaration.resolveBinding().getKey());
				if(depth < maximumCallGraphAnalysisDepth) {
					List<AbstractStatement> statements = methodBodyObject.getCompositeStatement().getStatements();
					//check if the method contains only one throw statement throwing the UnsupportedOperationException, and then analyze the overriding methods in the subclasses
					if(statements.size() == 1 && statements.get(0).getType().equals(StatementType.THROW) && methodBodyObject.getExceptionsInThrowStatements().contains("java.lang.UnsupportedOperationException")) {
						if((methodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
							//method is native
						}
						else {
							if(depth < maximumCallGraphAnalysisDepth) {
								IType superType = (IType)methodDeclaration.resolveBinding().getDeclaringClass().getJavaElement();
								Set<IType> subTypes = instance.getSubTypes(superType);
								IType exactSubType = exactSubType(variableDeclaration, subTypes);
								if(exactSubType != null && !exactSubType.equals(superType)) {
									IClassFile classFile = exactSubType.getClassFile();
									CompilationUnit compilationUnit = instance.getCompilationUnit(classFile);
									Set<MethodDeclaration> matchingSubTypeMethodDeclarations = getMatchingMethodDeclarationsForSubType(methodBinding, exactSubType, compilationUnit);
									for(MethodDeclaration overridingMethod : matchingSubTypeMethodDeclarations) {
										instance.addOverridingMethod(methodDeclaration, overridingMethod);
										processExternalMethodInvocation(overridingMethod, variableDeclaration, processedMethods, depth);
									}
								}
								else {
									for(IType subType : subTypes) {
										if(!subType.equals(superType)) {
											IClassFile classFile = subType.getClassFile();
											CompilationUnit compilationUnit = instance.getCompilationUnit(classFile);
											Set<MethodDeclaration> matchingSubTypeMethodDeclarations = getMatchingMethodDeclarationsForSubType(methodBinding, subType, compilationUnit);
											for(MethodDeclaration overridingMethod : matchingSubTypeMethodDeclarations) {
												instance.addOverridingMethod(methodDeclaration, overridingMethod);
												processExternalMethodInvocation(overridingMethod, variableDeclaration, processedMethods, depth);
											}
										}
									}
								}
							}
						}
					}
					for(MethodInvocationObject methodInvocationObject : methodBodyObject.getInvokedMethodsThroughThisReference()) {
						MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
						IMethodBinding methodBinding2 = methodInvocation.resolveMethodBinding();
						MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(methodBinding2);
						if(invokedMethodDeclaration != null && !invokedMethodDeclaration.equals(methodDeclaration)) {
							if(isUnprocessedMethod(processedMethods, methodBinding2)) {
								if((invokedMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
									//method is native
								}
								else {
									instance.addInvokedMethod(methodDeclaration, invokedMethodDeclaration);
									processExternalMethodInvocation(invokedMethodDeclaration, variableDeclaration, processedMethods, depth+1);
								}
							}
						}
					}
					Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = methodBodyObject.getInvokedMethodsThroughFields();
					for(AbstractVariable originalField : invokedMethodsThroughFields.keySet()) {
						if(originalField instanceof PlainVariable) {
							LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(originalField);
							AbstractVariable field = composeVariable(variableDeclaration, originalField);
							for(MethodInvocationObject methodInvocationObject : methodInvocations) {
								MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
								IMethodBinding methodBinding2 = methodInvocation.resolveMethodBinding();
								MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(methodBinding2);
								if(invokedMethodDeclaration != null && !invokedMethodDeclaration.equals(methodDeclaration)) {
									if(isUnprocessedMethod(processedMethods, methodBinding2)) {
										if((invokedMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
											//method is native
										}
										else {
											instance.addInvokedMethodThroughReference(methodDeclaration, invokedMethodDeclaration, (PlainVariable)originalField);
											processExternalMethodInvocation(invokedMethodDeclaration, field, processedMethods, depth+1);
										}
									}
								}
							}
						}
					}
					for(SuperMethodInvocationObject superMethodInvocationObject : methodBodyObject.getSuperMethodInvocations()) {
						SuperMethodInvocation superMethodInvocation = superMethodInvocationObject.getSuperMethodInvocation();
						IMethodBinding methodBinding2 = superMethodInvocation.resolveMethodBinding();
						MethodDeclaration invokedSuperMethodDeclaration = getInvokedMethodDeclaration(methodBinding2);
						if(invokedSuperMethodDeclaration != null) {
							if(isUnprocessedMethod(processedMethods, methodBinding2)) {
								if((invokedSuperMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
									//method is native
								}
								else {
									instance.addInvokedMethod(methodDeclaration, invokedSuperMethodDeclaration);
									processExternalMethodInvocation(invokedSuperMethodDeclaration, variableDeclaration, processedMethods, depth+1);
								}
							}
						}
					}
					if((methodDeclaration.getModifiers() & Modifier.STATIC) != 0) {
						for(MethodInvocationObject staticMethodInvocationObject : methodBodyObject.getInvokedStaticMethods()) {
							MethodInvocation methodInvocation = staticMethodInvocationObject.getMethodInvocation();
							IMethodBinding methodBinding2 = methodInvocation.resolveMethodBinding();
							MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(methodBinding2);
							if(invokedMethodDeclaration != null && !invokedMethodDeclaration.equals(methodDeclaration)) {
								if(isUnprocessedMethod(processedMethods, methodBinding2)) {
									if((invokedMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
										//method is native
									}
									else {
										instance.addInvokedMethod(methodDeclaration, invokedMethodDeclaration);
										processExternalMethodInvocation(invokedMethodDeclaration, null, processedMethods, depth+1);
									}
								}
							}
						}
					}
				}
			}
			else {
				if((methodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
					//method is native
				}
				else {
					if(depth < maximumCallGraphAnalysisDepth) {
						IType superType = (IType)methodDeclaration.resolveBinding().getDeclaringClass().getJavaElement();
						Set<IType> subTypes = instance.getSubTypes(superType);
						IType exactSubType = exactSubType(variableDeclaration, subTypes);
						if(exactSubType != null && !exactSubType.equals(superType)) {
							IClassFile classFile = exactSubType.getClassFile();
							CompilationUnit compilationUnit = instance.getCompilationUnit(classFile);
							Set<MethodDeclaration> matchingSubTypeMethodDeclarations = getMatchingMethodDeclarationsForSubType(methodBinding, exactSubType, compilationUnit);
							for(MethodDeclaration overridingMethod : matchingSubTypeMethodDeclarations) {
								instance.addOverridingMethod(methodDeclaration, overridingMethod);
								processExternalMethodInvocation(overridingMethod, variableDeclaration, processedMethods, depth);
							}
						}
						else {
							for(IType subType : subTypes) {
								if(!subType.equals(superType)) {
									IClassFile classFile = subType.getClassFile();
									CompilationUnit compilationUnit = instance.getCompilationUnit(classFile);
									Set<MethodDeclaration> matchingSubTypeMethodDeclarations = getMatchingMethodDeclarationsForSubType(methodBinding, subType, compilationUnit);
									for(MethodDeclaration overridingMethod : matchingSubTypeMethodDeclarations) {
										instance.addOverridingMethod(methodDeclaration, overridingMethod);
										processExternalMethodInvocation(overridingMethod, variableDeclaration, processedMethods, depth);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private IType exactSubType(AbstractVariable variableDeclaration, Set<IType> subTypes) {
		if(variableDeclaration != null) {
			PlainVariable plainVariable = null;
			if(variableDeclaration instanceof PlainVariable) {
				plainVariable = (PlainVariable)variableDeclaration;
			}
			else if(variableDeclaration instanceof CompositeVariable) {
				plainVariable = ((CompositeVariable)variableDeclaration).getFinalVariable();
			}
			for(IType subType : subTypes) {
				if(plainVariable.getVariableType().startsWith(subType.getFullyQualifiedName())) {
					return subType;
				}
			}
		}
		return null;
	}

	private MethodDeclaration getInvokedMethodDeclaration(IMethodBinding methodBinding) {
		MethodDeclaration invokedMethodDeclaration = null;
		IMethod iMethod2 = (IMethod)methodBinding.getJavaElement();
		if(iMethod2 != null) {
			IClassFile methodClassFile = iMethod2.getClassFile();
			LibraryClassStorage instance = LibraryClassStorage.getInstance();
			CompilationUnit methodCompilationUnit = instance.getCompilationUnit(methodClassFile);
			Set<TypeDeclaration> methodTypeDeclarations = extractTypeDeclarations(methodCompilationUnit);
			for(TypeDeclaration methodTypeDeclaration : methodTypeDeclarations) {
				ITypeBinding methodTypeDeclarationBinding = methodTypeDeclaration.resolveBinding();
				if(methodTypeDeclarationBinding != null && (methodTypeDeclarationBinding.isEqualTo(methodBinding.getDeclaringClass()) ||
						methodTypeDeclarationBinding.getBinaryName().equals(methodBinding.getDeclaringClass().getBinaryName()))) {
					MethodDeclaration[] methodDeclarations2 = methodTypeDeclaration.getMethods();
					for(MethodDeclaration methodDeclaration2 : methodDeclarations2) {
						if(methodDeclaration2.resolveBinding().isEqualTo(methodBinding) ||
								equalSignature(methodDeclaration2.resolveBinding(), methodBinding)) {
							invokedMethodDeclaration = methodDeclaration2;
							break;
						}
					}
					if(invokedMethodDeclaration != null)
						break;
				}
			}
		}
		return invokedMethodDeclaration;
	}

	private void handleAlreadyAnalyzedMethod(String methodBindingKey, AbstractVariable variableDeclaration, LibraryClassStorage indexer) {
		LinkedHashSet<PlainVariable> recursivelyDefinedFields = 
			indexer.getRecursivelyDefinedFields(methodBindingKey, new LinkedHashSet<String>());
		for(PlainVariable originalField : recursivelyDefinedFields) {
			AbstractVariable field = composeVariable(variableDeclaration, originalField);
			definedVariables.add(field);
		}
		LinkedHashSet<PlainVariable> recursivelyUsedFields = 
			indexer.getRecursivelyUsedFields(methodBindingKey, new LinkedHashSet<String>());
		for(PlainVariable originalField : recursivelyUsedFields) {
			AbstractVariable field = composeVariable(variableDeclaration, originalField);
			usedVariables.add(field);
		}
		Map<String, Set<PlainVariable>> invocationReferenceMap = indexer.getRecursivelyInvocationReferences(methodBindingKey, new LinkedHashSet<String>());
		for(String invokedMethodBindingKey : invocationReferenceMap.keySet()) {
			Set<PlainVariable> invocationReferences = invocationReferenceMap.get(invokedMethodBindingKey);
			if(invocationReferences != null) {
				for(PlainVariable invocationReference : invocationReferences) {
					LinkedHashSet<AbstractVariable> definedFieldsThroughReference = 
							indexer.getRecursivelyDefinedFieldsThroughReference(invokedMethodBindingKey, invocationReference, new LinkedHashSet<String>());
					for(AbstractVariable definedField : definedFieldsThroughReference) {
						AbstractVariable field = composeVariable(variableDeclaration, definedField);
						definedVariables.add(field);
					}
					LinkedHashSet<AbstractVariable> usedFieldsThroughReference = 
							indexer.getRecursivelyUsedFieldsThroughReference(invokedMethodBindingKey, invocationReference, new LinkedHashSet<String>());
					for(AbstractVariable usedField : usedFieldsThroughReference) {
						AbstractVariable field = composeVariable(variableDeclaration, usedField);
						usedVariables.add(field);
					}
				}
			}
		}
		LinkedHashSet<String> thrownExceptionTypes = indexer.getThrownExceptionTypes(methodBindingKey);
		thrownExceptionTypes.addAll(thrownExceptionTypes);
	}

	private AbstractVariable composeVariable(AbstractVariable leftSide, AbstractVariable rightSide) {
		if(leftSide == null || rightSide.isStatic()) {
			return rightSide;
		}
		else if(leftSide instanceof CompositeVariable) {
			CompositeVariable leftSideCompositeVariable = (CompositeVariable)leftSide;
			PlainVariable finalVariable = leftSideCompositeVariable.getFinalVariable();
			CompositeVariable newRightSide = new CompositeVariable(finalVariable, rightSide);
			AbstractVariable newLeftSide = leftSideCompositeVariable.getLeftPart();
			return composeVariable(newLeftSide, newRightSide);
		}
		else {
			return new CompositeVariable(leftSide, rightSide);
		}
	}

	private Set<TypeDeclaration> extractTypeDeclarations(CompilationUnit compilationUnit) {
		Set<TypeDeclaration> typeDeclarations = new LinkedHashSet<TypeDeclaration>();
		if(compilationUnit != null) {
			List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
	        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
	        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
	        		TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
	        		typeDeclarations.add(topLevelTypeDeclaration);
	        		TypeDeclaration[] types = topLevelTypeDeclaration.getTypes();
	        		for(TypeDeclaration type : types) {
	        			typeDeclarations.add(type);
	        		}
	        	}
	        }
		}
		return typeDeclarations;
	}

	private Set<AnonymousClassDeclaration> extractAnonymousClassDeclarations(CompilationUnit compilationUnit) {
		Set<AnonymousClassDeclaration> anonymousClassDeclarations = new LinkedHashSet<AnonymousClassDeclaration>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		if(compilationUnit != null) {
			List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
	        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
	        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
	        		Set<TypeDeclaration> typeDeclarations = new LinkedHashSet<TypeDeclaration>();
	        		TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
	        		typeDeclarations.add(topLevelTypeDeclaration);
	        		TypeDeclaration[] types = topLevelTypeDeclaration.getTypes();
	        		for(TypeDeclaration type : types) {
	        			typeDeclarations.add(type);
	        		}
	        		for(TypeDeclaration typeDeclaration : typeDeclarations) {
	        			MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
			        	for(MethodDeclaration methodDeclaration : methodDeclarations) {
			        		Block methodBody = methodDeclaration.getBody();
			        		if(methodBody != null) {
			        			List<Statement> statements = methodBody.statements();
			        			for(Statement statement : statements) {
			        				List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement);
			        				for(Expression expression : classInstanceCreations) {
			        					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
			        					AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation.getAnonymousClassDeclaration();
			        					if(anonymousClassDeclaration != null) {
			        						anonymousClassDeclarations.add(anonymousClassDeclaration);
			        					}
			        				}
			        			}
			        		}
			        	}
	        		}
	        	}
	        }
		}
		return anonymousClassDeclarations;
	}

	private static boolean equalType(ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		if(typeBinding1.isPrimitive() && typeBinding2.isPrimitive())
			return typeBinding1.isEqualTo(typeBinding2);
		else if(typeBinding1.isArray() && typeBinding2.isArray()) {
			int dimensions1 = typeBinding1.getDimensions();
			int dimensions2 = typeBinding2.getDimensions();
			if(dimensions1 == dimensions2) {
				ITypeBinding elementType1 = typeBinding1.getElementType();
				ITypeBinding elementType2 = typeBinding2.getElementType();
				return equalType(elementType1, elementType2);
			}
			else return false;
		}
		else if(typeBinding1.isWildcardType() && typeBinding2.isWildcardType()) {
			ITypeBinding bound1 = typeBinding1.getBound();
			ITypeBinding bound2 = typeBinding2.getBound();
			if(bound1 != null && bound2 != null)
				return equalType(bound1, bound2);
			else if(bound1 == null && bound2 == null)
				return true;
			else return false;
		}
		else if(typeBinding1.isParameterizedType() && typeBinding2.isParameterizedType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			if(erasure1.isEqualTo(erasure2)) {
				ITypeBinding[] typeArguments1 = typeBinding1.getTypeArguments();
				ITypeBinding[] typeArguments2 = typeBinding2.getTypeArguments();
				if(typeArguments1.length == typeArguments2.length) {
					for(int i=0; i<typeArguments1.length; i++) {
						if(!equalType(typeArguments1[i], typeArguments2[i]))
							return false;
					}
					return true;
				}
				else return false;
			}
			else return false;
		}
		else if(typeBinding1.isParameterizedType() && typeBinding2.isRawType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			return erasure1.isEqualTo(erasure2);
		}
		else if(typeBinding1.isRawType() && typeBinding2.isParameterizedType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			return erasure1.isEqualTo(erasure2);
		}
		else if(typeBinding1.isRawType() && typeBinding2.isRawType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			return erasure1.isEqualTo(erasure2);
		}
		else if(typeBinding1.isClass() && typeBinding2.isClass())
			return typeBinding1.isEqualTo(typeBinding2);
		else if(typeBinding1.isInterface() && typeBinding2.isInterface())
			return typeBinding1.isEqualTo(typeBinding2);
		else if(typeBinding1.isTypeVariable() || typeBinding2.isTypeVariable()) {
			return true;
		}
		return false;
	}

	public static boolean equalSignature(IMethodBinding methodBinding1, IMethodBinding methodBinding2) {
		if(!methodBinding1.getName().equals(methodBinding2.getName()))
			return false;
		ITypeBinding returnType1 = methodBinding1.getReturnType();
		ITypeBinding returnType2 = methodBinding2.getReturnType();
		if(!equalType(returnType1, returnType2))
			return false;
		ITypeBinding[] parameterTypes1 = methodBinding1.getParameterTypes();
		ITypeBinding[] parameterTypes2 = methodBinding2.getParameterTypes();
		if(parameterTypes1.length == parameterTypes2.length) {
			int i = 0;
			for(ITypeBinding typeBinding1 : parameterTypes1) {
				ITypeBinding typeBinding2 = parameterTypes2[i];
				if(!equalType(typeBinding1, typeBinding2))
					return false;
				i++;
			}
		}
		else return false;
		return true;
	}

	public static boolean equalSignatureIgnoringSubclassTypeDifferences(IMethodBinding methodBinding1, IMethodBinding methodBinding2) {
		if(!methodBinding1.getName().equals(methodBinding2.getName()))
			return false;
		ITypeBinding returnType1 = methodBinding1.getReturnType();
		ITypeBinding returnType2 = methodBinding2.getReturnType();
		ITypeBinding returnCommonSuperType = ASTNodeMatcher.commonSuperType(returnType1, returnType2);
		if(!equalType(returnType1, returnType2) && !ASTNodeMatcher.validCommonSuperType(returnCommonSuperType))
			return false;
		ITypeBinding[] parameterTypes1 = methodBinding1.getParameterTypes();
		ITypeBinding[] parameterTypes2 = methodBinding2.getParameterTypes();
		if(parameterTypes1.length == parameterTypes2.length) {
			int i = 0;
			for(ITypeBinding typeBinding1 : parameterTypes1) {
				ITypeBinding typeBinding2 = parameterTypes2[i];
				ITypeBinding parameterCommonSuperType = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
				if(!equalType(typeBinding1, typeBinding2) && !ASTNodeMatcher.validCommonSuperType(parameterCommonSuperType))
					return false;
				i++;
			}
		}
		else return false;
		return true;
	}

	private Set<MethodDeclaration> getMatchingMethodDeclarationsForSubType(IMethodBinding methodBinding,
			IType subclassType, CompilationUnit compilationUnit) {
		Set<MethodDeclaration> matchingMethodDeclarations = new LinkedHashSet<MethodDeclaration>();
		try {
			if(subclassType.isClass()/* || subclassType.isInterface()*/) {
				Set<TypeDeclaration> typeDeclarations = extractTypeDeclarations(compilationUnit);
				for(TypeDeclaration typeDeclaration : typeDeclarations) {
					ITypeBinding typeDeclarationBinding = typeDeclaration.resolveBinding();
					if(typeDeclarationBinding != null && typeDeclarationBinding.getQualifiedName().equals(subclassType.getFullyQualifiedName('.'))) {
						MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
						for(MethodDeclaration methodDeclaration : methodDeclarations) {
							if(equalSignature(methodDeclaration.resolveBinding(), methodBinding)) {
								matchingMethodDeclarations.add(methodDeclaration);
							}
						}
					}
				}
			}
			if(subclassType.isAnonymous()) {
				Set<AnonymousClassDeclaration> anonymousClassDeclarations = extractAnonymousClassDeclarations(compilationUnit);
				for(AnonymousClassDeclaration anonymousClassDeclaration : anonymousClassDeclarations) {
					List<BodyDeclaration> bodyDeclarations = anonymousClassDeclaration.bodyDeclarations();
					for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
						if(bodyDeclaration instanceof MethodDeclaration) {
							MethodDeclaration methodDeclaration = (MethodDeclaration)bodyDeclaration;
							IMethodBinding methodDeclarationBinding = methodDeclaration.resolveBinding();
							if(methodDeclarationBinding != null && equalSignature(methodDeclarationBinding, methodBinding)) {
								matchingMethodDeclarations.add(methodDeclaration);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return matchingMethodDeclarations;
	}

	private boolean isUnprocessedMethod(Set<String> processedMethods, IMethodBinding methodBinding) {
		return !processedMethods.contains(methodBinding.getKey()) && !processedMethods.contains(methodBinding.getMethodDeclaration().getKey());
	}
}
