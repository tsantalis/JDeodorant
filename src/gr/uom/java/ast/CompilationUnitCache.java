package gr.uom.java.ast;

import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.preference.IPreferenceStore;

public class CompilationUnitCache extends Indexer {

	private static CompilationUnitCache instance;
	private LinkedList<ITypeRoot> iTypeRootList;
	private LinkedList<CompilationUnit> compilationUnitList;
	private ITypeRoot lockedTypeRoot;
	private Set<ICompilationUnit> changedCompilationUnits;
	private Set<ICompilationUnit> addedCompilationUnits;
	private Set<ICompilationUnit> removedCompilationUnits;

	private CompilationUnitCache() {
		super();
		this.iTypeRootList = new LinkedList<ITypeRoot>();
		this.compilationUnitList = new LinkedList<CompilationUnit>();
		this.changedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		this.addedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		this.removedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
	}

	public static CompilationUnitCache getInstance() {
		if(instance == null) {
			instance = new CompilationUnitCache();
		}
		return instance;
	}

	public CompilationUnit getCompilationUnit(ITypeRoot iTypeRoot) {
		if(iTypeRoot instanceof IClassFile) {
			IClassFile classFile = (IClassFile)iTypeRoot;
			return LibraryClassStorage.getInstance().getCompilationUnit(classFile);
		}
		else {
			if(iTypeRootList.contains(iTypeRoot)) {
				int position = iTypeRootList.indexOf(iTypeRoot);
				return compilationUnitList.get(position);
			}
			else {
				ASTParser parser = ASTParser.newParser(AST.JLS4);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource(iTypeRoot);
				parser.setResolveBindings(true);
				CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
				
				IPreferenceStore store = Activator.getDefault().getPreferenceStore();
				int maximumCacheSize = store.getInt(PreferenceConstants.P_PROJECT_COMPILATION_UNIT_CACHE_SIZE);
				if(iTypeRootList.size() < maximumCacheSize) {
					iTypeRootList.add(iTypeRoot);
					compilationUnitList.add(compilationUnit);
				}
				else {
					if(lockedTypeRoot != null) {
						ITypeRoot firstTypeRoot = iTypeRootList.get(0);
						if(lockedTypeRoot.equals(firstTypeRoot)) {
							iTypeRootList.remove(1);
							compilationUnitList.remove(1);
						}
						else {
							iTypeRootList.removeFirst();
							compilationUnitList.removeFirst();
						}
					}
					else {
						iTypeRootList.removeFirst();
						compilationUnitList.removeFirst();
					}
					iTypeRootList.add(iTypeRoot);
					compilationUnitList.add(compilationUnit);
				}
				return compilationUnit;
			}
		}
	}

	public void compilationUnitChanged(ICompilationUnit compilationUnit) {
		changedCompilationUnits.add(compilationUnit);
	}

	public void compilationUnitAdded(ICompilationUnit compilationUnit) {
		addedCompilationUnits.add(compilationUnit);
	}

	public void compilationUnitRemoved(ICompilationUnit compilationUnit) {
		addedCompilationUnits.remove(compilationUnit);
		removedCompilationUnits.add(compilationUnit);
	}

	public Set<ICompilationUnit> getChangedCompilationUnits() {
		return changedCompilationUnits;
	}

	public Set<ICompilationUnit> getAddedCompilationUnits() {
		return addedCompilationUnits;
	}

	public Set<ICompilationUnit> getRemovedCompilationUnits() {
		return removedCompilationUnits;
	}

	public void lock(ITypeRoot iTypeRoot) {
		lockedTypeRoot = iTypeRoot;
	}

	public void releaseLock() {
		lockedTypeRoot = null;
	}

	public void clearAffectedCompilationUnits() {
		changedCompilationUnits.clear();
		addedCompilationUnits.clear();
		removedCompilationUnits.clear();
	}

	public void clearCache() {
		iTypeRootList.clear();
		compilationUnitList.clear();
	}
}
