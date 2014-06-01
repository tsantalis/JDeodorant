package gr.uom.java.ast.decomposition.cfg.mapping;

public class VariableBindingKeyPair {
	private String key1;
	private String key2;
	
	public VariableBindingKeyPair(String key1, String key2) {
		this.key1 = key1;
		this.key2 = key2;
	}

	public String getKey1() {
		return key1;
	}

	public String getKey2() {
		return key2;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof VariableBindingKeyPair) {
			VariableBindingKeyPair keyPair = (VariableBindingKeyPair)o;
			return this.key1.equals(keyPair.key1) &&
					this.key2.equals(keyPair.key2);
		}
		return false;
	}

	public int hashCode() {
		int result = 17;
		result = 37*result + key1.hashCode();
		result = 37*result + key2.hashCode();
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key1);
		sb.append("\n");
		sb.append(key2);
		return sb.toString();
	}
}
