package gr.uom.java.ast;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class ASTReader {

	private static SystemObject systemObject;
	private static IJavaProject examinedProject;
	private static EnvironmentInformation environmentInformation;

	public ASTReader(EnvironmentInformation environment) {
		systemObject = new SystemObject();
		environmentInformation = environment;
		/*for(String javaFilePath : environment.getJavaFilePaths()) {
			File javaFile = new File(javaFilePath);
			if(javaFile.exists()) {
				systemObject.addClasses(parseAST(javaFile));
			}
		}*/
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setEnvironment(environment.getClasspathEntries(),
				environment.getSourcepathEntries(), null, true);
		parser.setResolveBindings(true);
        parser.setStatementsRecovery(true);
        parser.setBindingsRecovery(true);
        String[] bindingKeys = new String[environment.getJavaFilePaths().length];
        int i=0;
        for(String javaFilePath : environment.getJavaFilePaths()){
            bindingKeys[i] = createBindingKeyFromClassFile(javaFilePath);
            i++;
        }
        ASTRequestor requestor = new ASTRequestor();
        parser.createASTs(environment.getJavaFilePaths(), null, bindingKeys, requestor, null);
	}

	public static String createBindingKeyFromClassFile(String filePath) {
		String classString = null;
		try {
			classString = new Scanner(new File(filePath)).useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int packageDeclarationStart = classString.indexOf("package");
		int packageDeclarationEnd = classString.indexOf(";", packageDeclarationStart);
		String packageDeclarationLine = classString.substring(packageDeclarationStart,packageDeclarationEnd);
		String packageName = packageDeclarationLine.substring(packageDeclarationLine.lastIndexOf("package")+7, packageDeclarationLine.length());
		packageName = packageName.replaceAll("\\s", "");
		String className = filePath.substring(filePath.lastIndexOf(File.separator)+1, filePath.indexOf(".java"));
		String fullyQualifiedClassName = packageName+"."+className;
		return BindingKey.createTypeBindingKey(fullyQualifiedClassName);
	}

	public ASTReader(IJavaProject iJavaProject, IProgressMonitor monitor) {
		if(monitor != null)
			monitor.beginTask("Parsing selected Java Project", getNumberOfCompilationUnits(iJavaProject));
		systemObject = new SystemObject();
		examinedProject = iJavaProject;
		try {
			IPackageFragmentRoot[] iPackageFragmentRoots = iJavaProject.getPackageFragmentRoots();
			for(IPackageFragmentRoot iPackageFragmentRoot : iPackageFragmentRoots) {
				IJavaElement[] children = iPackageFragmentRoot.getChildren();
				for(IJavaElement child : children) {
					if(child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFragment = (IPackageFragment)child;
						ICompilationUnit[] iCompilationUnits = iPackageFragment.getCompilationUnits();
						for(ICompilationUnit iCompilationUnit : iCompilationUnits) {
							if(monitor != null && monitor.isCanceled())
				    			throw new OperationCanceledException();
							systemObject.addClasses(parseAST(iCompilationUnit));
							if(monitor != null)
								monitor.worked(1);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		if(monitor != null)
			monitor.done();
	}

	public ASTReader(IJavaProject iJavaProject, SystemObject existingSystemObject, IProgressMonitor monitor) {
		Set<ICompilationUnit> changedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		Set<ICompilationUnit> addedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		Set<ICompilationUnit> removedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
		CompilationUnitCache instance = CompilationUnitCache.getInstance();
		for(ICompilationUnit changedCompilationUnit : instance.getChangedCompilationUnits()) {
			if(changedCompilationUnit.getJavaProject().equals(iJavaProject))
				changedCompilationUnits.add(changedCompilationUnit);
		}
		for(ICompilationUnit addedCompilationUnit : instance.getAddedCompilationUnits()) {
			if(addedCompilationUnit.getJavaProject().equals(iJavaProject))
				addedCompilationUnits.add(addedCompilationUnit);
		}
		for(ICompilationUnit removedCompilationUnit : instance.getRemovedCompilationUnits()) {
			if(removedCompilationUnit.getJavaProject().equals(iJavaProject))
				removedCompilationUnits.add(removedCompilationUnit);
		}
		if(monitor != null)
			monitor.beginTask("Parsing changed/added Compilation Units",
					changedCompilationUnits.size() + addedCompilationUnits.size());
		systemObject = existingSystemObject;
		examinedProject = iJavaProject;
		for(ICompilationUnit removedCompilationUnit : removedCompilationUnits) {
			IFile removedCompilationUnitFile = (IFile)removedCompilationUnit.getResource();
			systemObject.removeClasses(removedCompilationUnitFile);
		}
		for(ICompilationUnit changedCompilationUnit : changedCompilationUnits) {
			List<ClassObject> changedClassObjects = parseAST(changedCompilationUnit);
			for(ClassObject changedClassObject : changedClassObjects) {
				systemObject.replaceClass(changedClassObject);
			}
			if(monitor != null)
				monitor.worked(1);
		}
		for(ICompilationUnit addedCompilationUnit : addedCompilationUnits) {
			List<ClassObject> addedClassObjects = parseAST(addedCompilationUnit);
			for(ClassObject addedClassObject : addedClassObjects) {
				systemObject.addClass(addedClassObject);
			}
			if(monitor != null)
				monitor.worked(1);
		}
		instance.clearAffectedCompilationUnits();
		if(monitor != null)
			monitor.done();
	}

	public static int getNumberOfCompilationUnits(IJavaProject iJavaProject) {
		int numberOfCompilationUnits = 0;
		try {
			IPackageFragmentRoot[] iPackageFragmentRoots = iJavaProject.getPackageFragmentRoots();
			for(IPackageFragmentRoot iPackageFragmentRoot : iPackageFragmentRoots) {
				IJavaElement[] children = iPackageFragmentRoot.getChildren();
				for(IJavaElement child : children) {
					if(child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFragment = (IPackageFragment)child;
						ICompilationUnit[] iCompilationUnits = iPackageFragment.getCompilationUnits();
						numberOfCompilationUnits += iCompilationUnits.length;
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return numberOfCompilationUnits;
	}

	private List<TypeDeclaration> getRecursivelyInnerTypes(TypeDeclaration typeDeclaration) {
		List<TypeDeclaration> innerTypeDeclarations = new ArrayList<TypeDeclaration>();
		TypeDeclaration[] types = typeDeclaration.getTypes();
		for(TypeDeclaration type : types) {
			innerTypeDeclarations.add(type);
			innerTypeDeclarations.addAll(getRecursivelyInnerTypes(type));
		}
		return innerTypeDeclarations;
	}

	private List<ClassObject> parseAST(ICompilationUnit iCompilationUnit) {
		ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
		IFile iFile = (IFile)iCompilationUnit.getResource();
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(iCompilationUnit);
        parser.setResolveBindings(true); // we need bindings later on
        CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
        
        return parseAST(compilationUnit, iFile);
	}

	private List<ClassObject> parseAST(File javaFile) {
		ASTInformationGenerator.setCurrentFile(javaFile);
		String content = null;
		try {
			content = new Scanner(javaFile).useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setEnvironment(getEnvironmentInformation().getClasspathEntries(),
        		getEnvironmentInformation().getSourcepathEntries(), null, true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(content.toCharArray());
        parser.setResolveBindings(true); // we need bindings later on
        parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);
        CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
        
        return parseAST(compilationUnit, null);
	}

	private List<ClassObject> parseAST(CompilationUnit compilationUnit, IFile iFile) {
		List<ClassObject> classObjects = new ArrayList<ClassObject>();
        List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
        		TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
        		List<TypeDeclaration> typeDeclarations = new ArrayList<TypeDeclaration>();
        		typeDeclarations.add(topLevelTypeDeclaration);
        		typeDeclarations.addAll(getRecursivelyInnerTypes(topLevelTypeDeclaration));
        		for(TypeDeclaration typeDeclaration : typeDeclarations) {
	        		final ClassObject classObject = new ClassObject();
		        	classObject.setIFile(iFile);
		        	classObject.setName(typeDeclaration.resolveBinding().getQualifiedName());
		        	classObject.setTypeDeclaration(typeDeclaration);
		        	
		        	if(typeDeclaration.isInterface()) {
		        		classObject.setInterface(true);
		        	}
		        	
		        	int modifiers = typeDeclaration.getModifiers();
		        	if((modifiers & Modifier.ABSTRACT) != 0)
		        		classObject.setAbstract(true);
		        	
		        	if((modifiers & Modifier.PUBLIC) != 0)
		        		classObject.setAccess(Access.PUBLIC);
		        	else if((modifiers & Modifier.PROTECTED) != 0)
		        		classObject.setAccess(Access.PROTECTED);
		        	else if((modifiers & Modifier.PRIVATE) != 0)
		        		classObject.setAccess(Access.PRIVATE);
		        	else
		        		classObject.setAccess(Access.NONE);
		        	
		        	if((modifiers & Modifier.STATIC) != 0)
		        		classObject.setStatic(true);
		        	
		        	Type superclassType = typeDeclaration.getSuperclassType();
		        	if(superclassType != null) {
		        		ITypeBinding binding = superclassType.resolveBinding();
		        		String qualifiedName = binding.getQualifiedName();
	        			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
		        		classObject.setSuperclass(typeObject);
		        	}
		        	
		        	List<Type> superInterfaceTypes = typeDeclaration.superInterfaceTypes();
		        	for(Type interfaceType : superInterfaceTypes) {
		        		ITypeBinding binding = interfaceType.resolveBinding();
		        		String qualifiedName = binding.getQualifiedName();
	        			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
		        		classObject.addInterface(typeObject);
		        	}
		        	
		        	FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
		        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
		        		Type fieldType = fieldDeclaration.getType();
		        		ITypeBinding binding = fieldType.resolveBinding();
		        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
		        		for(VariableDeclarationFragment fragment : fragments) {
		        			String qualifiedName = binding.getQualifiedName();
		        			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
		        			typeObject.setArrayDimension(typeObject.getArrayDimension() + fragment.getExtraDimensions());
		        			FieldObject fieldObject = new FieldObject(typeObject, fragment.getName().getIdentifier());
		        			fieldObject.setClassName(classObject.getName());
		        			fieldObject.setVariableDeclarationFragment(fragment);
		        			
		        			int fieldModifiers = fieldDeclaration.getModifiers();
		        			if((fieldModifiers & Modifier.PUBLIC) != 0)
		                		fieldObject.setAccess(Access.PUBLIC);
		                	else if((fieldModifiers & Modifier.PROTECTED) != 0)
		                		fieldObject.setAccess(Access.PROTECTED);
		                	else if((fieldModifiers & Modifier.PRIVATE) != 0)
		                		fieldObject.setAccess(Access.PRIVATE);
		                	else
		                		fieldObject.setAccess(Access.NONE);
		                	
		                	if((fieldModifiers & Modifier.STATIC) != 0)
		                		fieldObject.setStatic(true);
		                	
		        			classObject.addField(fieldObject);
		        		}
		        	}
		        	
		        	MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
		        	for(MethodDeclaration methodDeclaration : methodDeclarations) {
		        		String methodName = methodDeclaration.getName().getIdentifier();
		        		final ConstructorObject constructorObject = new ConstructorObject();
		        		constructorObject.setMethodDeclaration(methodDeclaration);
		        		constructorObject.setName(methodName);
		        		constructorObject.setClassName(classObject.getName());
		        		
		        		int methodModifiers = methodDeclaration.getModifiers();
		        		if((methodModifiers & Modifier.PUBLIC) != 0)
		        			constructorObject.setAccess(Access.PUBLIC);
		            	else if((methodModifiers & Modifier.PROTECTED) != 0)
		            		constructorObject.setAccess(Access.PROTECTED);
		            	else if((methodModifiers & Modifier.PRIVATE) != 0)
		            		constructorObject.setAccess(Access.PRIVATE);
		            	else
		            		constructorObject.setAccess(Access.NONE);
		        		
		        		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		        		for(SingleVariableDeclaration parameter : parameters) {
		        			Type parameterType = parameter.getType();
		        			ITypeBinding binding = parameterType.resolveBinding();
		        			String qualifiedName = binding.getQualifiedName();
		        			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
		        			typeObject.setArrayDimension(typeObject.getArrayDimension() + parameter.getExtraDimensions());
		        			if(parameter.isVarargs()) {
		        				typeObject.setArrayDimension(1);
		        			}
		        			ParameterObject parameterObject = new ParameterObject(typeObject, parameter.getName().getIdentifier());
		        			parameterObject.setSingleVariableDeclaration(parameter);
		        			constructorObject.addParameter(parameterObject);
		        		}
		        		
		        		Block methodBody = methodDeclaration.getBody();
		        		if(methodBody != null) {
		        			MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
		        			constructorObject.setMethodBody(methodBodyObject);
		        		}
		        		
		        		if(methodDeclaration.isConstructor()) {
		        			classObject.addConstructor(constructorObject);
		        		}
		        		else {
		        			MethodObject methodObject = new MethodObject(constructorObject);
		        			List<IExtendedModifier> extendedModifiers = methodDeclaration.modifiers();
			        		for(IExtendedModifier extendedModifier : extendedModifiers) {
			        			if(extendedModifier.isAnnotation()) {
			        				Annotation annotation = (Annotation)extendedModifier;
			        				if(annotation.getTypeName().getFullyQualifiedName().equals("Test")) {
			        					methodObject.setTestAnnotation(true);
			        					break;
			        				}
			        			}
			        		}
		        			Type returnType = methodDeclaration.getReturnType2();
		        			ITypeBinding binding = returnType.resolveBinding();
		        			String qualifiedName = binding.getQualifiedName();
		        			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
		        			methodObject.setReturnType(typeObject);
		        			
		        			if((methodModifiers & Modifier.ABSTRACT) != 0)
		        				methodObject.setAbstract(true);
		        			if((methodModifiers & Modifier.STATIC) != 0)
		        				methodObject.setStatic(true);
		        			if((methodModifiers & Modifier.SYNCHRONIZED) != 0)
		        				methodObject.setSynchronized(true);
		        			if((methodModifiers & Modifier.NATIVE) != 0)
		        				methodObject.setNative(true);
		        			
		        			classObject.addMethod(methodObject);
		        			FieldInstructionObject fieldInstruction = methodObject.isGetter();
		        			if(fieldInstruction != null)
		        				systemObject.addGetter(methodObject.generateMethodInvocation(), fieldInstruction);
		        			fieldInstruction = methodObject.isSetter();
		        			if(fieldInstruction != null)
		        				systemObject.addSetter(methodObject.generateMethodInvocation(), fieldInstruction);
		        			fieldInstruction = methodObject.isCollectionAdder();
		        			if(fieldInstruction != null)
		        				systemObject.addCollectionAdder(methodObject.generateMethodInvocation(), fieldInstruction);
		        			MethodInvocationObject methodInvocation = methodObject.isDelegate();
		        			if(methodInvocation != null)
		        				systemObject.addDelegate(methodObject.generateMethodInvocation(), methodInvocation);
		        		}
		        	}
		        	classObjects.add(classObject);
        		}
        	}
        }
        return classObjects;
	}

    public static SystemObject getSystemObject() {
		return systemObject;
	}

	public static IJavaProject getExaminedProject() {
		return examinedProject;
	}

	public static EnvironmentInformation getEnvironmentInformation() {
		return environmentInformation;
	}

	private class ASTRequestor extends FileASTRequestor {
		private final IBinding[] bindings = new IBinding[1];

	    public void acceptBinding(String bindingKey, IBinding binding) {
	        bindings[0] = binding;
	    }
		public void acceptAST(String sourceFilePath, CompilationUnit ast) {
			File javaFile = new File(sourceFilePath);
        	ASTInformationGenerator.setCurrentFile(javaFile);
        	systemObject.addClasses(parseAST(ast, null));
		}
	}
}