package gr.uom.java.ast;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

public class TypeSearchRequestor extends SearchRequestor {
	
	private Set<IType> subTypes;
	
	public TypeSearchRequestor(Set<IType> subTypes) {
		this.subTypes = subTypes;
	}

	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		Object element = match.getElement();
		if (match.getElement() instanceof IType) {
			IType iType = (IType)element;
			if(!iType.isAnonymous()) {
				subTypes.add(iType);
			}
		}
	}
}
