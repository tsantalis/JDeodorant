package ca.concordia.jdeodorant.clone.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CloneGroupList implements Iterable<CloneGroup> {
	
	private final List<CloneGroup> cloneGroups = new ArrayList<CloneGroup>();
	private final Map<Integer, CloneGroup> allCloneGroupsHashCodes = new HashMap<Integer, CloneGroup>();

	public void add(CloneGroup cloneGroup) {
		cloneGroups.add(cloneGroup);
		if (allCloneGroupsHashCodes.containsKey(cloneGroup.hashCode()))
			cloneGroup.setRepeatedOf(allCloneGroupsHashCodes.get(cloneGroup.hashCode()));
		else
			allCloneGroupsHashCodes.put(cloneGroup.hashCode(), cloneGroup);
	}

	public Iterator<CloneGroup> iterator() {
		return cloneGroups.iterator();
	}

}
