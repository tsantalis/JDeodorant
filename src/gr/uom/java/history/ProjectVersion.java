package gr.uom.java.history;

public class ProjectVersion implements Comparable<Object> {
	private String[] tokens;
	
	public ProjectVersion(String version) {
		this.tokens = version.split("[\\._\\-]");
	}

	public int compareTo(Object o) {
		ProjectVersion version = (ProjectVersion)o;
		int max = Math.max(this.tokens.length, version.tokens.length);
		for(int i=0; i<max; i++) {
			String token1, token2;
			if(i < this.tokens.length)
				token1 = this.tokens[i];
			else
				token1 = "0";
			if(i < version.tokens.length)
				token2 = version.tokens[i];
			else
				token2 = "0";
			try {
				int num1 = Integer.parseInt(token1);
				int num2 = Integer.parseInt(token2);
				if(num1 < num2)
					return -1;
				else if(num1 > num2)
					return 1;
			}
			catch(NumberFormatException e) {
				if(Character.isDigit(token1.charAt(0)) && Character.isDigit(token2.charAt(0))) {
					String prefix1 = null;
					String prefix2 = null;
					for(int j=0; j<token1.length(); j++) {
						if(!Character.isDigit(token1.charAt(j))) {
							prefix1 = token1.substring(0, j);
							break;
						}
					}
					for(int j=0; j<token2.length(); j++) {
						if(!Character.isDigit(token2.charAt(j))) {
							prefix2 = token2.substring(0, j);
							break;
						}
					}
					int num1, num2;
					if(prefix1 == null)
						num1 = Integer.parseInt(token1);
					else
						num1 = Integer.parseInt(prefix1);
					if(prefix2 == null)
						num2 = Integer.parseInt(token2);
					else
						num2 = Integer.parseInt(prefix2);
					if(num1 < num2)
						return -1;
					else if(num1 > num2)
						return 1;
					else
						return token1.compareTo(token2);
				}
				else {
					int compare = token1.compareTo(token2);
					if(compare < 0)
						return -1;
					else if(compare > 0)
						return 1;
				}
			}
		}
		return 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i=0;
		for(String token : tokens) {
			sb.append(token);
			if(i<tokens.length-1)
				sb.append(".");
			i++;
		}
		return sb.toString();
	}
}
