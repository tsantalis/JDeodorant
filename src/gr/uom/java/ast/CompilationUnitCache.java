package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
	private List<ITypeRoot> lockedTypeRoots;
	private LinkedList<File> fileList;
	private List<File> lockedFiles;
	private Set<ICompilationUnit> changedCompilationUnits;
	private Set<ICompilationUnit> addedCompilationUnits;
	private Set<ICompilationUnit> removedCompilationUnits;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	private Map<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>> usedFieldsForMethodArgumentsMap;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	private Map<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>> definedFieldsForMethodArgumentsMap;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	private Map<String, LinkedHashSet<AbstractVariable>> usedFieldsForMethodExpressionMap;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	private Map<String, LinkedHashSet<AbstractVariable>> definedFieldsForMethodExpressionMap;

	public void addUsedFieldForMethodArgument(AbstractVariable field, MethodObject mo, int argPosition) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition)) {
				LinkedHashSet<AbstractVariable> fieldSet = argumentMap.get(argPosition);
				fieldSet.add(field);
			}
			else {
				LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
				fieldSet.add(field);
				argumentMap.put(argPosition, fieldSet);
			}
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			fieldSet.add(field);
			argumentMap.put(argPosition, fieldSet);
			usedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public void setEmptyUsedFieldsForMethodArgument(MethodObject mo, int argPosition) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			argumentMap.put(argPosition, new LinkedHashSet<AbstractVariable>());
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			argumentMap.put(argPosition, fieldSet);
			usedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public void addDefinedFieldForMethodArgument(AbstractVariable field, MethodObject mo, int argPosition) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition)) {
				LinkedHashSet<AbstractVariable> fieldSet = argumentMap.get(argPosition);
				fieldSet.add(field);
			}
			else {
				LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
				fieldSet.add(field);
				argumentMap.put(argPosition, fieldSet);
			}
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			fieldSet.add(field);
			argumentMap.put(argPosition, fieldSet);
			definedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public void setEmptyDefinedFieldsForMethodArgument(MethodObject mo, int argPosition) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			argumentMap.put(argPosition, new LinkedHashSet<AbstractVariable>());
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			argumentMap.put(argPosition, fieldSet);
			definedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public boolean containsMethodArgument(MethodObject mo, int argPosition) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return true;
		}
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return true;
		}
		return false;
	}

	public Set<AbstractVariable> getUsedFieldsForMethodArgument(MethodObject mo, int argPosition) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return argumentMap.get(argPosition);
		}
		return new LinkedHashSet<AbstractVariable>();
	}

	public Set<AbstractVariable> getDefinedFieldsForMethodArgument(MethodObject mo, int argPosition) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return argumentMap.get(argPosition);
		}
		return new LinkedHashSet<AbstractVariable>();
	}

	public void addUsedFieldForMethodExpression(AbstractVariable field, MethodObject mo) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(usedFieldsForMethodExpressionMap.containsKey(methodBindingKey)) {
			LinkedHashSet<AbstractVariable> fields = usedFieldsForMethodExpressionMap.get(methodBindingKey);
			fields.add(field);
		}
		else {
			LinkedHashSet<AbstractVariable> fields = new LinkedHashSet<AbstractVariable>();
			fields.add(field);
			usedFieldsForMethodExpressionMap.put(methodBindingKey, fields);
		}
	}

	public void setEmptyUsedFieldsForMethodExpression(MethodObject mo) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		LinkedHashSet<AbstractVariable> usedFields = new LinkedHashSet<AbstractVariable>();
		usedFieldsForMethodExpressionMap.put(methodBindingKey, usedFields);
	}

	public void addDefinedFieldForMethodExpression(AbstractVariable field, MethodObject mo) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(definedFieldsForMethodExpressionMap.containsKey(methodBindingKey)) {
			LinkedHashSet<AbstractVariable> fields = definedFieldsForMethodExpressionMap.get(methodBindingKey);
			fields.add(field);
		}
		else {
			LinkedHashSet<AbstractVariable> fields = new LinkedHashSet<AbstractVariable>();
			fields.add(field);
			definedFieldsForMethodExpressionMap.put(methodBindingKey, fields);
		}
	}

	public void setEmptyDefinedFieldsForMethodExpression(MethodObject mo) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		LinkedHashSet<AbstractVariable> usedFields = new LinkedHashSet<AbstractVariable>();
		definedFieldsForMethodExpressionMap.put(methodBindingKey, usedFields);
	}

	public boolean containsMethodExpression(MethodObject mo) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		if(usedFieldsForMethodExpressionMap.containsKey(methodBindingKey))
			return true;
		if(definedFieldsForMethodExpressionMap.containsKey(methodBindingKey))
			return true;
		return false;
	}

	public Set<AbstractVariable> getUsedFieldsForMethodExpression(MethodObject mo) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		return usedFieldsForMethodExpressionMap.get(methodBindingKey);
	}

	public Set<AbstractVariable> getDefinedFieldsForMethodExpression(MethodObject mo) {
		String methodBindingKey = mo.getMethodDeclaration().resolveBinding().getKey();
		return definedFieldsForMethodExpressionMap.get(methodBindingKey);
	}

	private CompilationUnitCache() {
		super();
		this.iTypeRootList = new LinkedList<ITypeRoot>();
		this.lockedTypeRoots = new ArrayList<ITypeRoot>();
		this.fileList = new LinkedList<File>();
		this.lockedFiles = new ArrayList<File>();
		this.compilationUnitList = new LinkedList<CompilationUnit>();
		this.changedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		this.addedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		this.removedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		this.usedFieldsForMethodArgumentsMap = new HashMap<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>>();
		this.definedFieldsForMethodArgumentsMap = new HashMap<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>>();
		this.usedFieldsForMethodExpressionMap = new HashMap<String, LinkedHashSet<AbstractVariable>>();
		this.definedFieldsForMethodExpressionMap = new HashMap<String, LinkedHashSet<AbstractVariable>>();
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
					if(!lockedTypeRoots.isEmpty()) {
						int indexToBeRemoved = 0;
						int counter = 0;
						for(ITypeRoot lockedTypeRoot : lockedTypeRoots) {
							if(iTypeRootList.get(counter).equals(lockedTypeRoot)) {
								indexToBeRemoved++;
							}
							counter++;
						}
						iTypeRootList.remove(indexToBeRemoved);
						compilationUnitList.remove(indexToBeRemoved);
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

	private static String getExtension(File f) {
		String fileName = f.getName();
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i+1);
		}
		return extension;
	}

	public CompilationUnit getCompilationUnit(File file) {
		if(getExtension(file).equalsIgnoreCase("class")) {
			//IClassFile classFile = (IClassFile)iTypeRoot;
			//return LibraryClassStorage.getInstance().getCompilationUnit(classFile);
			System.out.println("class file requested");
			return null;
		}
		else {
			if(fileList.contains(file)) {
				int position = fileList.indexOf(file);
				return compilationUnitList.get(position);
			}
			else {
				String content = null;
				try {
					content = new Scanner(file).useDelimiter("\\Z").next();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
		        ASTParser parser = ASTParser.newParser(AST.JLS4);
		        parser.setEnvironment(ASTReader.getEnvironmentInformation().getClasspathEntries(),
		        		ASTReader.getEnvironmentInformation().getSourcepathEntries(), null, true);
		        parser.setKind(ASTParser.K_COMPILATION_UNIT);
		        parser.setSource(content.toCharArray());
		        parser.setResolveBindings(true); // we need bindings later on
				parser.setStatementsRecovery(true);
				parser.setBindingsRecovery(true);
		        CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
				
				int maximumCacheSize = 20;
				if(fileList.size() < maximumCacheSize) {
					fileList.add(file);
					compilationUnitList.add(compilationUnit);
				}
				else {
					if(!lockedFiles.isEmpty()) {
						int indexToBeRemoved = 0;
						int counter = 0;
						for(File lockedFile : lockedFiles) {
							if(fileList.get(counter).equals(lockedFile)) {
								indexToBeRemoved++;
							}
							counter++;
						}
						fileList.remove(indexToBeRemoved);
						compilationUnitList.remove(indexToBeRemoved);
					}
					else {
						fileList.removeFirst();
						compilationUnitList.removeFirst();
					}
					fileList.add(file);
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
		if(!lockedTypeRoots.contains(iTypeRoot))
			lockedTypeRoots.add(iTypeRoot);
	}

	public void lock(File file) {
		if(!lockedFiles.contains(file))
			lockedFiles.add(file);
	}

	public void releaseLock() {
		lockedTypeRoots.clear();
		lockedFiles.clear();
		usedFieldsForMethodArgumentsMap.clear();
		definedFieldsForMethodArgumentsMap.clear();
		usedFieldsForMethodExpressionMap.clear();
		definedFieldsForMethodExpressionMap.clear();
	}

	public void clearAffectedCompilationUnits() {
		changedCompilationUnits.clear();
		addedCompilationUnits.clear();
		removedCompilationUnits.clear();
	}

	public void clearCache() {
		lockedTypeRoots.clear();
		iTypeRootList.clear();
		lockedFiles.clear();
		fileList.clear();
		compilationUnitList.clear();
	}
}
