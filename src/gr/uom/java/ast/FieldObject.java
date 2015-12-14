package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldObject extends VariableDeclarationObject {

    private String name;
    private TypeObject type;
    private List<CommentObject> commentList;
    private boolean _static;
    private Access access;
    private String className;
    //private VariableDeclarationFragment fragment;
    private ASTInformation fragment;
    private volatile int hashCode = 0;

    public FieldObject(TypeObject type, String name) {
        this.type = type;
        this.name = name;
        this._static = false;
        this.access = Access.NONE;
        this.commentList = new ArrayList<CommentObject>();
    }

    public void setVariableDeclarationFragment(VariableDeclarationFragment fragment) {
    	//this.fragment = fragment;
    	this.variableBindingKey = fragment.resolveBinding().getKey();
    	this.fragment = ASTInformationGenerator.generateASTInformation(fragment);
    }

    public VariableDeclarationFragment getVariableDeclarationFragment() {
    	//return this.fragment;
    	ASTNode node = this.fragment.recoverASTNode();
    	if(node instanceof SimpleName) {
    		return (VariableDeclarationFragment)node.getParent();
    	}
    	else {
    		return (VariableDeclarationFragment)node;
    	}
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public TypeObject getType() {
        return type;
    }

	public boolean addComment(CommentObject comment) {
		return commentList.add(comment);
	}

	public boolean addComments(List<CommentObject> comments) {
		return commentList.addAll(comments);
	}

	public ListIterator<CommentObject> getCommentListIterator() {
		return commentList.listIterator();
	}

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public FieldInstructionObject generateFieldInstruction() {
    	FieldInstructionObject fieldInstruction = new FieldInstructionObject(this.className, this.type, this.name, this.variableBindingKey);
    	fieldInstruction.setStatic(this._static);
    	return fieldInstruction;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof FieldObject) {
            FieldObject fieldObject = (FieldObject)o;
            return this.className.equals(fieldObject.className) &&
            	this.name.equals(fieldObject.name) && this.type.equals(fieldObject.type) &&
            	this.variableBindingKey.equals(fieldObject.variableBindingKey);
        }
        return false;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    public boolean equals(FieldInstructionObject fio) {
        return this.className.equals(fio.getOwnerClass()) &&
        this.name.equals(fio.getName()) && this.type.equals(fio.getType()) && this.variableBindingKey.equals(fio.getVariableBindingKey());
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + className.hashCode();
    		result = 37*result + name.hashCode();
    		result = 37*result + type.hashCode();
    		result = 37*result + variableBindingKey.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        if(_static)
            sb.append("static").append(" ");
        sb.append(type.toString()).append(" ");
        sb.append(name);
        return sb.toString();
    }

	public VariableDeclaration getVariableDeclaration() {
		return getVariableDeclarationFragment();
	}
}