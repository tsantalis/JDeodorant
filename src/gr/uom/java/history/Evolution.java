package gr.uom.java.history;

import java.util.Set;
import java.util.Map.Entry;

public interface Evolution {

	public Set<Entry<ProjectVersionPair, String>> getSimilarityEntries();

	public Set<Entry<ProjectVersionPair, String>> getChangeEntries();

	public String getCode(ProjectVersion projectVersion);
}
