package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ClassObject {

	private String name;
	private List<ConstructorObject> constructorList;
	private List<MethodObject> methodList;
	private List<FieldObject> fieldList;
	private String superclass;
	private List<String> interfaceList;
	private boolean _abstract;
    private boolean _interface;
    private boolean _static;
    private Access access;
    private TypeDeclaration typeDeclaration;

    public ClassObject() {
		this.constructorList = new ArrayList<ConstructorObject>();
		this.methodList = new ArrayList<MethodObject>();
		this.interfaceList = new ArrayList<String>();
		this.fieldList = new ArrayList<FieldObject>();
		this._abstract = false;
        this._interface = false;
        this._static = false;
        this.access = Access.NONE;
    }

    public boolean isInnerClass() {
    	if(typeDeclaration.getParent() instanceof TypeDeclaration)
    		return true;
    	else if(typeDeclaration.getParent() instanceof CompilationUnit)
    		return false;
    	return false;
    }

    public TypeDeclaration getOuterClass() {
    	if(typeDeclaration.getParent() instanceof TypeDeclaration)
    		return (TypeDeclaration)typeDeclaration.getParent();
    	else if(typeDeclaration.getParent() instanceof CompilationUnit)
    		return null;
    	return null;
    }

    public void setTypeDeclaration(TypeDeclaration typeDeclaration) {
    	this.typeDeclaration = typeDeclaration;
    }

    public TypeDeclaration getTypeDeclaration() {
    	return this.typeDeclaration;
    }

    public MethodObject getMethod(MethodInvocationObject mio) {
        ListIterator<MethodObject> mi = getMethodIterator();
        while(mi.hasNext()) {
            MethodObject mo = mi.next();
            if(mo.equals(mio))
                return mo;
        }
        return null;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
    	for(MethodObject method : methodList) {
    		if(method.containsMethodInvocation(methodInvocation))
    			return true;
    	}
    	return false;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation, MethodObject excludedMethod) {
    	for(MethodObject method : methodList) {
    		if(!method.equals(excludedMethod) && method.containsMethodInvocation(methodInvocation))
    			return true;
    	}
    	return false;
    }

    public boolean hasFieldType(String className) {
        ListIterator<FieldObject> fi = getFieldIterator();
        while(fi.hasNext()) {
            FieldObject fo = fi.next();
            if(fo.getType().getClassType().equals(className))
                return true;
        }
        return false;
    }

    public List<TypeCheckElimination> generateTypeCheckEliminations(List<InheritanceTree> inheritanceTreeList) {
    	List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
    	for(MethodObject methodObject : methodList) {
    		MethodBodyObject methodBodyObject = methodObject.getMethodBody();
    		if(methodBodyObject != null) {
    			List<TypeCheckElimination> list = methodBodyObject.generateTypeCheckEliminations();
    			for(TypeCheckElimination typeCheckElimination : list) {
    				typeCheckElimination.setTypeCheckMethod(methodObject.getMethodDeclaration());
    				List<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
    				ArrayList<Statement> firstBlockOfStatements = typeCheckStatements.get(0);
    				Statement firstStatementOfBlock = firstBlockOfStatements.get(0);
    				if(firstStatementOfBlock.getParent() instanceof SwitchStatement) {
    					SwitchStatement switchStatement = (SwitchStatement)firstStatementOfBlock.getParent();
    					Expression switchStatementExpression = switchStatement.getExpression();
    					String switchStatementExpressionName = null;
    					if(switchStatementExpression instanceof SimpleName) {
    						SimpleName simpleName = (SimpleName)switchStatementExpression;
    						switchStatementExpressionName = simpleName.getIdentifier();
    					}
    					else if(switchStatementExpression instanceof FieldAccess) {
    						FieldAccess fieldAccess = (FieldAccess)switchStatementExpression;
    						switchStatementExpressionName = fieldAccess.getName().getIdentifier();
    					}
    					else if(switchStatementExpression instanceof MethodInvocation) {
    						MethodInvocation methodInvocation = (MethodInvocation)switchStatementExpression;
    						for(MethodObject method : methodList) {
    							FieldInstructionObject fieldInstruction = method.isGetter();
    							if(fieldInstruction != null && method.getMethodDeclaration().resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding()))
    								switchStatementExpressionName = fieldInstruction.getName();
    						}
    						if(switchStatementExpressionName == null) {
    							IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
    							if((methodInvocationBinding.getModifiers() & Modifier.ABSTRACT) != 0) {
	    							for(InheritanceTree tree : inheritanceTreeList) {
	    								DefaultMutableTreeNode root = tree.getRootNode();
	    								String rootClassName = (String)root.getUserObject();
	    								ITypeBinding declaringClassTypeBinding = methodInvocationBinding.getDeclaringClass();
	    								if(rootClassName.equals(declaringClassTypeBinding.getQualifiedName())) {
	    									typeCheckElimination.setTypeMethodInvocation(methodInvocation);
	    									typeCheckElimination.setExistingInheritanceTree(tree);
	    									break;
	    								}
	    							}
    							}
    						}
    					}
    					for(FieldObject field : fieldList) {
							if(field.getName().equals(switchStatementExpressionName)) {
								typeCheckElimination.setTypeField(field.getVariableDeclarationFragment());
								for(MethodObject method : methodList) {
									FieldInstructionObject fieldInstruction = method.isSetter();
									if(fieldInstruction != null && field.equals(fieldInstruction)) {
										typeCheckElimination.setTypeFieldSetterMethod(method.getMethodDeclaration());
									}
									fieldInstruction = method.isGetter();
									if(fieldInstruction != null && field.equals(fieldInstruction)) {
										typeCheckElimination.setTypeFieldGetterMethod(method.getMethodDeclaration());
									}
								}
								break;
							}
						}
    				}
    				
    				Set<Expression> typeCheckExpressions = typeCheckElimination.getTypeCheckExpressions();
    				Map<FieldObject, Integer> fieldTypeCounterMap = new LinkedHashMap<FieldObject, Integer>();
    				Map<MethodInvocation, Integer> typeMethodInvocationCounterMap = new LinkedHashMap<MethodInvocation, Integer>();
    				for(Expression typeCheckExpression : typeCheckExpressions) {
    					if(typeCheckExpression.getParent() instanceof SwitchCase) {
    						if(typeCheckExpression instanceof SimpleName) {
    							SimpleName simpleName = (SimpleName)typeCheckExpression;
    							IBinding binding = simpleName.resolveBinding();
    							if(binding.getKind() == IBinding.VARIABLE) {
    								IVariableBinding variableBinding = (IVariableBinding)binding;
    								if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
    									typeCheckElimination.addStaticType(typeCheckExpression, simpleName);
    								}
    							}
    						}
    						else if(typeCheckExpression instanceof QualifiedName) {
    							QualifiedName qualifiedName = (QualifiedName)typeCheckExpression;
    							IBinding binding = qualifiedName.resolveBinding();
    							if(binding.getKind() == IBinding.VARIABLE) {
    								IVariableBinding variableBinding = (IVariableBinding)binding;
    								if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
    									typeCheckElimination.addStaticType(typeCheckExpression, qualifiedName.getName());
    								}
    							}
    						}
    						else if(typeCheckExpression instanceof FieldAccess) {
    							FieldAccess fieldAccess = (FieldAccess)typeCheckExpression;
    							IVariableBinding variableBinding = fieldAccess.resolveFieldBinding();
    							if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
    								typeCheckElimination.addStaticType(typeCheckExpression, fieldAccess.getName());
    							}
    						}
    					}
    					else if(typeCheckExpression instanceof InfixExpression) {
    						InfixExpression infixExpression = (InfixExpression)typeCheckExpression;
    						if(infixExpression.getOperator().equals(InfixExpression.Operator.EQUALS)) {
    							Expression leftOperand = infixExpression.getLeftOperand();
    							Expression rightOperand = infixExpression.getRightOperand();
    							infixExpressionHandler(leftOperand, typeCheckExpression, fieldTypeCounterMap, typeMethodInvocationCounterMap, typeCheckElimination, inheritanceTreeList);
    							infixExpressionHandler(rightOperand, typeCheckExpression, fieldTypeCounterMap, typeMethodInvocationCounterMap, typeCheckElimination, inheritanceTreeList);
    						}
    					}
    				}
    				for(FieldObject field : fieldTypeCounterMap.keySet()) {
    					if(fieldTypeCounterMap.get(field) == typeCheckExpressions.size()) {
    						typeCheckElimination.setTypeField(field.getVariableDeclarationFragment());
    						for(MethodObject method : methodList) {
								FieldInstructionObject fieldInstruction = method.isSetter();
								if(fieldInstruction != null && field.equals(fieldInstruction)) {
									typeCheckElimination.setTypeFieldSetterMethod(method.getMethodDeclaration());
								}
								fieldInstruction = method.isGetter();
								if(fieldInstruction != null && field.equals(fieldInstruction)) {
									typeCheckElimination.setTypeFieldGetterMethod(method.getMethodDeclaration());
								}
							}
    					}
    				}
    				for(MethodInvocation methodInvocation : typeMethodInvocationCounterMap.keySet()) {
    					if(typeMethodInvocationCounterMap.get(methodInvocation) == typeCheckExpressions.size()) {
    						typeCheckElimination.setTypeMethodInvocation(methodInvocation);
    						IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
    						for(InheritanceTree tree : inheritanceTreeList) {
    							DefaultMutableTreeNode root = tree.getRootNode();
    							String rootClassName = (String)root.getUserObject();
    							ITypeBinding declaringClassTypeBinding = methodInvocationBinding.getDeclaringClass();
    							if(rootClassName.equals(declaringClassTypeBinding.getQualifiedName())) {
    								typeCheckElimination.setExistingInheritanceTree(tree);
    								break;
    							}
    						}
    					}
    				}
    				if((typeCheckElimination.getTypeField() != null || typeCheckElimination.getTypeMethodInvocation() != null) && typeCheckElimination.allTypeChecksContainStaticField()) {
    					ExpressionExtractor expressionExtractor = new ExpressionExtractor();
    					List<ArrayList<Statement>> allTypeCheckStatements = typeCheckElimination.getTypeCheckStatements();
    					StatementExtractor statementExtractor = new StatementExtractor();
						List<Statement> variableDeclarationStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment = statementExtractor.getVariableDeclarations(methodBodyObject.getCompositeStatement().getStatement());
						for(ArrayList<Statement> typeCheckStatementList : allTypeCheckStatements) {
							for(Statement statement : typeCheckStatementList) {
								variableDeclarationStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.removeAll(statementExtractor.getVariableDeclarations(statement));
    						}
						}
						for(ArrayList<Statement> typeCheckStatementList : allTypeCheckStatements) {
    						for(Statement statement : typeCheckStatementList) {
    							List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(statement);
								for(Expression variableInstruction : variableInstructions) {
    								SimpleName simpleName = (SimpleName)variableInstruction;
    								IBinding binding = simpleName.resolveBinding();
    								if(binding.getKind() == IBinding.VARIABLE) {
    									IVariableBinding variableBinding = (IVariableBinding)binding;
    									if(variableBinding.isField()) {
    										if(variableBinding.getDeclaringClass() != null && 
    												variableBinding.getDeclaringClass().getQualifiedName().equals(this.name)) {
    											for(FieldObject field : fieldList) {
    												if(field.getName().equals(simpleName.getIdentifier()))
    													typeCheckElimination.addAccessedField(field.getVariableDeclarationFragment());
    											}
    										}
    									}
    									else if(variableBinding.isParameter()) {
    										ListIterator<ParameterObject> parameterIterator = methodObject.getParameterListIterator();
    										while(parameterIterator.hasNext()) {
    											ParameterObject parameter = parameterIterator.next();
    											if(parameter.getName().equals(simpleName.getIdentifier()))
    												typeCheckElimination.addAccessedParameter(parameter.getSingleVariableDeclaration());
    										}
    									}
    									else {
    										for(Statement vDStatement : variableDeclarationStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
    											VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)vDStatement;
    											List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
    											for(VariableDeclarationFragment fragment : fragments) {
    												if(fragment.getName().getIdentifier().equals(simpleName.getIdentifier())) {
    													typeCheckElimination.addAccessedLocalVariable(fragment);
    													break;
    												}
    											}    											
    										}
    									}
    								}
    							}
    						}
    					}
    					typeCheckEliminations.add(typeCheckElimination);
    				}
    			}
    		}
    	}
    	return typeCheckEliminations;
    }

	private void infixExpressionHandler(Expression operand, Expression typeCheckExpression, Map<FieldObject, Integer> fieldTypeCounterMap,
			Map<MethodInvocation, Integer> typeMethodInvocationCounterMap, TypeCheckElimination typeCheckElimination, List<InheritanceTree> inheritanceTreeList) {
		SimpleName leftOperandName = null;
		if(operand instanceof SimpleName) {
			SimpleName leftOperandSimpleName = (SimpleName)operand;
			leftOperandName = leftOperandSimpleName;
		}
		else if(operand instanceof QualifiedName) {
			QualifiedName leftOperandQualifiedName = (QualifiedName)operand;
			leftOperandName = leftOperandQualifiedName.getName();
		}
		else if(operand instanceof FieldAccess) {
			FieldAccess leftOperandFieldAccess = (FieldAccess)operand;
			leftOperandName = leftOperandFieldAccess.getName();
		}
		else if(operand instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)operand;
			for(MethodObject method : methodList) {
				FieldInstructionObject fieldInstruction = method.isGetter();
				if(fieldInstruction != null && method.getMethodDeclaration().resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					leftOperandName = MethodDeclarationUtility.isGetter(method.getMethodDeclaration());
				}
			}
			if(leftOperandName == null) {
				IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
				for(InheritanceTree tree : inheritanceTreeList) {
					DefaultMutableTreeNode root = tree.getRootNode();
					String rootClassName = (String)root.getUserObject();
					ITypeBinding declaringClassTypeBinding = methodInvocationBinding.getDeclaringClass();
					if(rootClassName.equals(declaringClassTypeBinding.getQualifiedName())) {
						boolean found = false;
						for(MethodInvocation key : typeMethodInvocationCounterMap.keySet()) {
							if(key.toString().equals(methodInvocation.toString())) {
								typeMethodInvocationCounterMap.put(key, typeMethodInvocationCounterMap.get(key)+1);
								found = true;
							}
						}
						if(!found)
							typeMethodInvocationCounterMap.put(methodInvocation, 1);
						break;
					}
				}
			}
		}
		if(leftOperandName != null) {
			IBinding binding = leftOperandName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
					typeCheckElimination.addStaticType(typeCheckExpression, leftOperandName);
				}
				else {
					for(FieldObject field : fieldList) {
						if(field.getName().equals(leftOperandName.getIdentifier())) {
							if(fieldTypeCounterMap.containsKey(field)) {
								fieldTypeCounterMap.put(field, fieldTypeCounterMap.get(field)+1);
							}
							else {
								fieldTypeCounterMap.put(field, 1);
							}
						}
					}
				}
			}
		}
	}

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public void setSuperclass(String superclass) {
		this.superclass = superclass;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean addMethod(MethodObject method) {
		return methodList.add(method);
	}
	
	public boolean addInterface(String i) {
		return interfaceList.add(i);
	}
	
	public boolean addConstructor(ConstructorObject c) {
		return constructorList.add(c);
	}
	
	public boolean addField(FieldObject f) {
		return fieldList.add(f);
	}
	
	public ListIterator<ConstructorObject> getConstructorIterator() {
		return constructorList.listIterator();
	}
	
	public ListIterator<MethodObject> getMethodIterator() {
		return methodList.listIterator();
	}
	
	public ListIterator<String> getInterfaceIterator() {
		return interfaceList.listIterator();
	}

    public ListIterator<String> getSuperclassIterator() {
		List<String> superclassList = new ArrayList<String>(interfaceList);
		superclassList.add(superclass);
		return superclassList.listIterator();
	}

	public ListIterator<FieldObject> getFieldIterator() {
		return fieldList.listIterator();
	}

	public String getName() {
		return name;
	}

	public String getSuperclass() {
		return superclass;
	}
	
	public void setAbstract(boolean abstr) {
		this._abstract = abstr;
	}
	
	public boolean isAbstract() {
		return this._abstract;
	}

    public void setInterface(boolean i) {
        this._interface = i;
    }

    public boolean isInterface() {
        return this._interface;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }
    
    public boolean implementsInterface(String i) {
		return interfaceList.contains(i);
	}

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        if(_static)
            sb.append("static").append(" ");
        if(_interface)
            sb.append("interface").append(" ");
        else if(_abstract)
            sb.append("abstract class").append(" ");
        else
            sb.append("class").append(" ");
        sb.append(name).append(" ");
        sb.append("extends ").append(superclass);
        if(!interfaceList.isEmpty()) {
            sb.append(" ").append("implements ");
            for(int i=0; i<interfaceList.size()-1; i++)
                sb.append(interfaceList.get(i)).append(", ");
            sb.append(interfaceList.get(interfaceList.size()-1));
        }
        sb.append("\n\n").append("Fields:");
        for(FieldObject field : fieldList)
            sb.append("\n").append(field.toString());

        sb.append("\n\n").append("Constructors:");
        for(ConstructorObject constructor : constructorList)
            sb.append("\n").append(constructor.toString());

        sb.append("\n\n").append("Methods:");
        for(MethodObject method : methodList)
            sb.append("\n").append(method.toString());

        return sb.toString();
    }
}