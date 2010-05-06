package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.SimpleName;

public class SuperFieldInstructionObject {

	private String ownerClass;
	private TypeObject type;
	private String name;
	private boolean _static;
	//private SimpleName simpleName;
	private ASTInformation simpleName;
	private volatile int hashCode = 0;

	public SuperFieldInstructionObject(String ownerClass, TypeObject type, String name) {
		this.ownerClass = ownerClass;
		this.type = type;
		this.name = name;
		this._static = false;
	}

	public String getOwnerClass() {
		return ownerClass;
	}

	public TypeObject getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public boolean isStatic() {
		return _static;
	}

	public void setStatic(boolean s) {
		_static = s;
	}

	public void setSimpleName(SimpleName simpleName) {
		//this.simpleName = simpleName;
		this.simpleName = ASTInformationGenerator.generateASTInformation(simpleName);
	}

	public SimpleName getSimpleName() {
		//return this.simpleName;
		return (SimpleName)this.simpleName.recoverASTNode();
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}

		if (o instanceof SuperFieldInstructionObject) {
			SuperFieldInstructionObject sfio = (SuperFieldInstructionObject)o;
			return this.ownerClass.equals(sfio.ownerClass) && this.name.equals(sfio.name) && this.type.equals(sfio.type);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + ownerClass.hashCode();
			result = 37*result + name.hashCode();
			result = 37*result + type.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(ownerClass).append("::");
		//sb.append(type).append(" ");
		sb.append(name);
		return sb.toString();
	}
}
