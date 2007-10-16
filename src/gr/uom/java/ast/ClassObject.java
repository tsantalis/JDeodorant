package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

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

    public List<TypeCheckElimination> generateTypeCheckEliminations() {
    	List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
    	for(MethodObject methodObject : methodList) {
    		MethodBodyObject methodBodyObject = methodObject.getMethodBody();
    		if(methodBodyObject != null) {
    			List<TypeCheckElimination> list = methodBodyObject.generateTypeCheckEliminations();
    			for(TypeCheckElimination typeCheckElimination : list) {
    				Object[] typeCheckStatements = typeCheckElimination.getTypeCheckStatements().toArray();
    				ArrayList<Statement> firstBlockOfStatements = (ArrayList<Statement>)typeCheckStatements[0];
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
    					for(FieldObject field : fieldList) {
							if(field.getName().equals(switchStatementExpressionName)) {
								typeCheckElimination.setTypeField(field.getVariableDeclarationFragment());
								for(MethodObject method : methodList) {
									FieldInstructionObject fieldInstruction = method.isSetter();
									if(fieldInstruction != null && field.equals(fieldInstruction)) {
										typeCheckElimination.setTypeFieldSetterMethod(method.getMethodDeclaration());
										break;
									}
								}
								break;
							}
						}
    				}
    				
    				Set<Expression> typeCheckExpressions = typeCheckElimination.getTypeCheckExpressions();
    				Map<FieldObject, Integer> fieldTypeCounterMap = new LinkedHashMap<FieldObject, Integer>();
    				for(Expression typeCheckExpression : typeCheckExpressions) {
    					if(typeCheckExpression.getParent() instanceof SwitchCase) {
    						if(typeCheckExpression instanceof SimpleName) {
    							SimpleName simpleName = (SimpleName)typeCheckExpression;
    							for(FieldObject field : fieldList) {
        							if(field.getName().equals(simpleName.getIdentifier()) && field.isStatic()) {
        								typeCheckElimination.addStaticType(typeCheckExpression, field.getVariableDeclarationFragment());
        								break;
        							}
        						}
    						}
    					}
    					else if(typeCheckExpression instanceof InfixExpression) {
    						InfixExpression infixExpression = (InfixExpression)typeCheckExpression;
    						if(infixExpression.getOperator().equals(InfixExpression.Operator.EQUALS)) {
    							Expression leftOperand = infixExpression.getLeftOperand();
    							Expression rightOperand = infixExpression.getRightOperand();
    							for(FieldObject field : fieldList) {
    								infixExpressionHandler(leftOperand, field, typeCheckExpression, fieldTypeCounterMap, typeCheckElimination);
    								infixExpressionHandler(rightOperand, field, typeCheckExpression, fieldTypeCounterMap, typeCheckElimination);
    							}
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
									break;
								}
							}
    					}
    				}
    				if(typeCheckElimination.getTypeField() != null && typeCheckElimination.allTypeChecksContainStaticField())
    					typeCheckEliminations.add(typeCheckElimination);
    			}
    		}
    	}
    	return typeCheckEliminations;
    }

	private void infixExpressionHandler(Expression operand, FieldObject field,
			Expression typeCheckExpression, Map<FieldObject, Integer> fieldTypeCounterMap,
			TypeCheckElimination typeCheckElimination) {
		String leftOperandName = null;
		if(operand instanceof SimpleName) {
			SimpleName leftOperandSimpleName = (SimpleName)operand;
			leftOperandName = leftOperandSimpleName.getIdentifier();
		}
		else if(operand instanceof FieldAccess) {
			FieldAccess leftOperandFieldAccess = (FieldAccess)operand;
			leftOperandName = leftOperandFieldAccess.getName().getIdentifier();
		}
		if(field.getName().equals(leftOperandName)) {
			if(field.isStatic()) {
				typeCheckElimination.addStaticType(typeCheckExpression, field.getVariableDeclarationFragment());
			}
			else {
				if(fieldTypeCounterMap.containsKey(field)) {
					fieldTypeCounterMap.put(field, fieldTypeCounterMap.get(field)+1);
				}
				else {
					fieldTypeCounterMap.put(field, 1);
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