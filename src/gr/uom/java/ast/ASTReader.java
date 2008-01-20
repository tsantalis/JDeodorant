package gr.uom.java.ast;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ASTReader {

	private SystemObject systemObject;
	private Map<TypeDeclaration, IFile> fileMap;
	private Map<TypeDeclaration, CompilationUnit> compilationUnitMap;

	public ASTReader(IProject iProject) {
		this.systemObject = new SystemObject();
		this.fileMap = new LinkedHashMap<TypeDeclaration, IFile>();
		this.compilationUnitMap = new LinkedHashMap<TypeDeclaration, CompilationUnit>();
		recurse(iProject);
	}
	
	private void recurse(IResource resource) {
		try {
			if(resource.getType() == IResource.PROJECT) {
				IResource[] members = ((IProject)resource).members();
				for(IResource member : members) {
					if(member.getType() == IResource.FOLDER)
						recurse(member);
					else if(member.getType() == IResource.FILE && member.getFileExtension() != null && member.getFileExtension().equalsIgnoreCase("java"))
						parseAST((IFile)member);
				}
			}
			else if(resource.getType() == IResource.FOLDER) {
				IResource[] members = ((IFolder)resource).members();
				for(IResource member : members) {
					if(member.getType() == IResource.FOLDER)
						recurse(member);
					else if(member.getType() == IResource.FILE && member.getFileExtension() != null && member.getFileExtension().equalsIgnoreCase("java"))
						parseAST((IFile)member);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private void parseAST(IFile iFile) {
        IJavaElement iJavaElement = JavaCore.create(iFile);
        ICompilationUnit iCompilationUnit = (ICompilationUnit)iJavaElement;
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(iCompilationUnit);
        parser.setResolveBindings(true); // we need bindings later on
        CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
        
        List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
        		TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
        		List<TypeDeclaration> typeDeclarations = new ArrayList<TypeDeclaration>();
        		typeDeclarations.add(topLevelTypeDeclaration);
        		TypeDeclaration[] types = topLevelTypeDeclaration.getTypes();
        		for(TypeDeclaration type : types) {
        			typeDeclarations.add(type);
        		}
        		for(TypeDeclaration typeDeclaration : typeDeclarations) {
	        		final ClassObject classObject = new ClassObject();
	        		boolean extendsTestCase = false;
		        	fileMap.put(typeDeclaration, iFile);
		        	compilationUnitMap.put(typeDeclaration, compilationUnit);
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
		        	
		        	if((modifiers & Modifier.STATIC) != 0)
		        		classObject.setStatic(true);
		        	
		        	Type superclassType = typeDeclaration.getSuperclassType();
		        	if(superclassType != null) {
		        		ITypeBinding binding = superclassType.resolveBinding();
		        		classObject.setSuperclass(binding.getQualifiedName());
		        		if(binding.getQualifiedName().equals("junit.framework.TestCase"))
		        			extendsTestCase = true;
		        	}
		        	
		        	List<Type> superInterfaceTypes = typeDeclaration.superInterfaceTypes();
		        	for(Type interfaceType : superInterfaceTypes) {
		        		ITypeBinding binding = interfaceType.resolveBinding();
		        		classObject.addInterface(binding.getQualifiedName());
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
		        			fieldObject.setVariableDeclarationFragment(fragment);
		        			
		        			int fieldModifiers = fieldDeclaration.getModifiers();
		        			if((fieldModifiers & Modifier.PUBLIC) != 0)
		                		fieldObject.setAccess(Access.PUBLIC);
		                	else if((fieldModifiers & Modifier.PROTECTED) != 0)
		                		fieldObject.setAccess(Access.PROTECTED);
		                	else if((fieldModifiers & Modifier.PRIVATE) != 0)
		                		fieldObject.setAccess(Access.PRIVATE);
		                	
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
		        		
		        		int methodModifiers = methodDeclaration.getModifiers();
		        		if((methodModifiers & Modifier.PUBLIC) != 0)
		        			constructorObject.setAccess(Access.PUBLIC);
		            	else if((methodModifiers & Modifier.PROTECTED) != 0)
		            		constructorObject.setAccess(Access.PROTECTED);
		            	else if((methodModifiers & Modifier.PRIVATE) != 0)
		            		constructorObject.setAccess(Access.PRIVATE);
		        		
		        		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		        		for(SingleVariableDeclaration parameter : parameters) {
		        			Type parameterType = parameter.getType();
		        			ITypeBinding binding = parameterType.resolveBinding();
		        			String qualifiedName = binding.getQualifiedName();
		        			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
		        			typeObject.setArrayDimension(typeObject.getArrayDimension() + parameter.getExtraDimensions());
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
		        			Type returnType = methodDeclaration.getReturnType2();
		        			ITypeBinding binding = returnType.resolveBinding();
		        			String qualifiedName = binding.getQualifiedName();
		        			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
		        			methodObject.setReturnType(typeObject);
		        			methodObject.setClassName(classObject.getName());
		        			
		        			if((methodModifiers & Modifier.ABSTRACT) != 0)
		        				methodObject.setAbstract(true);
		        			if((methodModifiers & Modifier.STATIC) != 0)
		        				methodObject.setStatic(true);
		        			
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
		        	if(!extendsTestCase)
		        		systemObject.addClass(classObject);
        		}
        	}
        }	
	}

	public IFile getFile(TypeDeclaration typeDeclaration) {
		return fileMap.get(typeDeclaration);
	}

	public CompilationUnit getCompilationUnit(TypeDeclaration typeDeclaration) {
		return compilationUnitMap.get(typeDeclaration);
	}

    public SystemObject getSystemObject() {
		return systemObject;
	}
}