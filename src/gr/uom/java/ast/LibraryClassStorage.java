package gr.uom.java.ast;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class LibraryClassStorage {
	private static LibraryClassStorage instance;
	private Map<IClassFile, CompilationUnit> compilationUnitMap;
	private Set<IClassFile> unMatchedClassFiles;
	private Map<MethodDeclaration, LinkedHashSet<MethodDeclaration>> methodInvocationMap;
	private Map<MethodDeclaration, LinkedHashSet<VariableDeclaration>> definedFieldMap;
	private Map<MethodDeclaration, LinkedHashSet<VariableDeclaration>> usedFieldMap;
	
	private LibraryClassStorage() {
		this.compilationUnitMap = new HashMap<IClassFile, CompilationUnit>();
		this.unMatchedClassFiles = new LinkedHashSet<IClassFile>();
		this.methodInvocationMap = new HashMap<MethodDeclaration, LinkedHashSet<MethodDeclaration>>();
		this.definedFieldMap = new HashMap<MethodDeclaration, LinkedHashSet<VariableDeclaration>>();
		this.usedFieldMap = new HashMap<MethodDeclaration, LinkedHashSet<VariableDeclaration>>();
	}
	
	public static LibraryClassStorage getInstance() {
		if(instance == null) {
			instance = new LibraryClassStorage();
		}
		return instance;
	}
	
	public CompilationUnit getCompilationUnit(IClassFile classFile) {
		if(compilationUnitMap.containsKey(classFile)) {
			CompilationUnit compilationUnit = compilationUnitMap.get(classFile);
			return compilationUnit;
		}
		else {
			CompilationUnit compilationUnit = null;
			try {
				if(!unMatchedClassFiles.contains(classFile)) {
					ASTParser parser = ASTParser.newParser(AST.JLS3);
					parser.setSource(classFile);
					parser.setResolveBindings(true);
					compilationUnit = (CompilationUnit)parser.createAST(null);
					compilationUnitMap.put(classFile, compilationUnit);
				}
			}
			catch(IllegalStateException e) {
				unMatchedClassFiles.add(classFile);
			}
			return compilationUnit;
		}
	}
	
	public void addInvokedMethod(MethodDeclaration originalMethod, MethodDeclaration invokedMethod) {
		if(methodInvocationMap.containsKey(originalMethod)) {
			LinkedHashSet<MethodDeclaration> invokedMethods = methodInvocationMap.get(originalMethod);
			invokedMethods.add(invokedMethod);
		}
		else {
			LinkedHashSet<MethodDeclaration> invokedMethods = new LinkedHashSet<MethodDeclaration>();
			invokedMethods.add(invokedMethod);
			methodInvocationMap.put(originalMethod, invokedMethods);
		}
	}
	
	public void setDefinedFields(MethodDeclaration method, LinkedHashSet<VariableDeclaration> fields) {
		definedFieldMap.put(method, fields);
	}
	
	public void setUsedFields(MethodDeclaration method, LinkedHashSet<VariableDeclaration> fields) {
		usedFieldMap.put(method, fields);
	}
	
	public boolean isAnalyzed(MethodDeclaration method) {
		if(definedFieldMap.containsKey(method) && usedFieldMap.containsKey(method))
			return true;
		else
			return false;
	}
	
	public LinkedHashSet<VariableDeclaration> getRecursivelyDefinedFields(MethodDeclaration method,
			Set<MethodDeclaration> processedMethods) {
		LinkedHashSet<VariableDeclaration> definedFields = new LinkedHashSet<VariableDeclaration>();
		definedFields.addAll(definedFieldMap.get(method));
		processedMethods.add(method);
		LinkedHashSet<MethodDeclaration> invokedMethods = methodInvocationMap.get(method);
		if(invokedMethods != null) {
			for(MethodDeclaration invokedMethod : invokedMethods) {
				if(invokedMethod.getBody() != null && !processedMethods.contains(invokedMethod))
					definedFields.addAll(getRecursivelyDefinedFields(invokedMethod, processedMethods));
			}
		}
		return definedFields;
	}
	
	public LinkedHashSet<VariableDeclaration> getRecursivelyUsedFields(MethodDeclaration method,
			Set<MethodDeclaration> processedMethods) {
		LinkedHashSet<VariableDeclaration> usedFields = new LinkedHashSet<VariableDeclaration>();
		usedFields.addAll(usedFieldMap.get(method));
		processedMethods.add(method);
		LinkedHashSet<MethodDeclaration> invokedMethods = methodInvocationMap.get(method);
		if(invokedMethods != null) {
			for(MethodDeclaration invokedMethod : invokedMethods) {
				if(invokedMethod.getBody() != null && !processedMethods.contains(invokedMethod))
					usedFields.addAll(getRecursivelyUsedFields(invokedMethod, processedMethods));
			}
		}
		return usedFields;
	}
}
