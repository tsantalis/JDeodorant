package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class ReachingAliasSet {

	private List<LinkedHashSet<VariableDeclaration>> aliasSets;
	
	public ReachingAliasSet() {
		this.aliasSets = new ArrayList<LinkedHashSet<VariableDeclaration>>();
	}
	
	private ReachingAliasSet(List<LinkedHashSet<VariableDeclaration>> aliasSets) {
		this.aliasSets = aliasSets;
	}
	
	public void insertAlias(VariableDeclaration leftHandSideReference, VariableDeclaration rightHandSideReference) {
		boolean rightHandSideReferenceFound = false;
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			if(aliasSet.contains(rightHandSideReference)) {
				rightHandSideReferenceFound = true;
				aliasSet.add(leftHandSideReference);
			}
		}
		if(!rightHandSideReferenceFound) {
			LinkedHashSet<VariableDeclaration> aliasSet = new LinkedHashSet<VariableDeclaration>();
			aliasSet.add(leftHandSideReference);
			aliasSet.add(rightHandSideReference);
			aliasSets.add(aliasSet);
		}
		List<LinkedHashSet<VariableDeclaration>> aliasSetsToBeRemoved = new ArrayList<LinkedHashSet<VariableDeclaration>>();
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			if(aliasSet.contains(leftHandSideReference)) {
				if(!aliasSet.contains(rightHandSideReference))
					aliasSet.remove(leftHandSideReference);
				if(aliasSet.size() == 1)
					aliasSetsToBeRemoved.add(aliasSet);
			}
		}
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSetsToBeRemoved) {
			aliasSets.remove(aliasSet);
		}
	}
	
	public void removeAlias(VariableDeclaration leftHandSideReference) {
		List<LinkedHashSet<VariableDeclaration>> aliasSetsToBeRemoved = new ArrayList<LinkedHashSet<VariableDeclaration>>();
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			if(aliasSet.contains(leftHandSideReference)) {
				aliasSet.remove(leftHandSideReference);
				if(aliasSet.size() == 1)
					aliasSetsToBeRemoved.add(aliasSet);
			}
		}
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSetsToBeRemoved) {
			aliasSets.remove(aliasSet);
		}
	}
	
	public boolean containsAlias(VariableDeclaration variableDeclaration) {
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			if(aliasSet.contains(variableDeclaration))
				return true;
		}
		return false;
	}
	
	public boolean containsAlias(AbstractVariable variable) {
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			for(VariableDeclaration alias : aliasSet) {
				if(alias.resolveBinding().getKey().equals(variable.getVariableBindingKey()))
					return true;
			}
		}
		return false;
	}
	
	public Set<VariableDeclaration> getAliases(VariableDeclaration variableDeclaration) {
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			if(aliasSet.contains(variableDeclaration)) {
				Set<VariableDeclaration> aliases = new LinkedHashSet<VariableDeclaration>();
				for(VariableDeclaration alias : aliasSet) {
					if(!alias.equals(variableDeclaration))
						aliases.add(alias);
				}
				return aliases;
			}
		}
		return null;
	}
	
	public Set<VariableDeclaration> getAliases(AbstractVariable variable) {
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			boolean containsVariable = false;
			for(VariableDeclaration alias : aliasSet) {
				if(alias.resolveBinding().getKey().equals(variable.getVariableBindingKey())) {
					containsVariable = true;
					break;
				}
			}
			if(containsVariable) {
				Set<VariableDeclaration> aliases = new LinkedHashSet<VariableDeclaration>();
				for(VariableDeclaration alias : aliasSet) {
					if(!alias.resolveBinding().getKey().equals(variable.getVariableBindingKey()))
						aliases.add(alias);
				}
				return aliases;
			}
		}
		return null;
	}
	
	public ReachingAliasSet copy() {
		List<LinkedHashSet<VariableDeclaration>> aliasSetsCopy = new ArrayList<LinkedHashSet<VariableDeclaration>>();
		for(LinkedHashSet<VariableDeclaration> aliasSet : aliasSets) {
			LinkedHashSet<VariableDeclaration> aliasSetCopy = new LinkedHashSet<VariableDeclaration>(aliasSet);
			aliasSetsCopy.add(aliasSetCopy);
		}
		return new ReachingAliasSet(aliasSetsCopy);
	}
	
	public String toString() {
		return aliasSets.toString();
	}
}
