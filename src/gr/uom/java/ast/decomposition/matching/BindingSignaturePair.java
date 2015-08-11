package gr.uom.java.ast.decomposition.matching;

import java.util.Set;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class BindingSignaturePair {

	private BindingSignature signature1;
	private BindingSignature signature2;
	private boolean isGap = false;
	
	public BindingSignaturePair(AbstractExpression expression1, AbstractExpression expression2) {
		this.signature1 = new BindingSignature(expression1);
		this.signature2 = new BindingSignature(expression2);
	}
	
	public BindingSignaturePair(Set<PDGNode> statements1, Set<PDGNode> statements2) {
		this.signature1 = new BindingSignature(statements1);
		this.signature2 = new BindingSignature(statements2);
		this.isGap = true;
	}
	
	public BindingSignaturePair(BindingSignature signature1, BindingSignature signature2) {
		this.signature1 = signature1;
		this.signature2 = signature2;
	}
	
	public BindingSignature getSignature1() {
		return signature1;
	}

	public BindingSignature getSignature2() {
		return signature2;
	}

	public void setGap(boolean isGap) {
		this.isGap = isGap;
	}

	public boolean isReverse(BindingSignaturePair other) {
		if(this.signature1.signatureWithoutMethods().equals(other.signature2.signatureWithoutMethods()) &&
				this.signature2.signatureWithoutMethods().equals(other.signature1.signatureWithoutMethods()))
			return true;
		return false;
	}

	public int getWeight() {
		return signature1.getSize() + signature2.getSize();
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof BindingSignaturePair) {
			BindingSignaturePair signaturePair = (BindingSignaturePair)o;
			return this.signature1.equals(signaturePair.signature1) &&
					this.signature2.equals(signaturePair.signature2) &&
					this.isGap == signaturePair.isGap;
		}
		return false;
	}

	public int hashCode() {
		int result = 17;
		result = 37*result + signature1.hashCode();
		result = 37*result + signature2.hashCode();
		result = 37*result + (isGap ? 0 : 1);
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(signature1);
		sb.append("\n");
		sb.append(signature2);
		return sb.toString();
	}
}
