package gr.uom.java.history;

public class ProjectVersionPair {
	private ProjectVersion fromVersion;
	private ProjectVersion toVersion;
	
	public ProjectVersionPair(ProjectVersion fromVersion, ProjectVersion toVersion) {
		this.fromVersion = fromVersion;
		this.toVersion = toVersion;
	}

	public ProjectVersion getFromVersion() {
		return fromVersion;
	}

	public ProjectVersion getToVersion() {
		return toVersion;
	}

	public String toString() {
		return fromVersion.toString() + "-" + toVersion.toString();
	}
}
