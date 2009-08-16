package gr.uom.java.ast;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.ArrayList;
import java.util.List;

public class ASTReader {

	private static SystemObject systemObject;

	public ASTReader(IJavaProject iJavaProject) {
		systemObject = new SystemObject();
		try {
			IPackageFragmentRoot[] iPackageFragmentRoots = iJavaProject.getPackageFragmentRoots();
			for(IPackageFragmentRoot iPackageFragmentRoot : iPackageFragmentRoots) {
				IJavaElement[] children = iPackageFragmentRoot.getChildren();
				for(IJavaElement child : children) {
					if(child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFragment = (IPackageFragment)child;
						ICompilationUnit[] iCompilationUnits = iPackageFragment.getCompilationUnits();
						for(ICompilationUnit iCompilationUnit : iCompilationUnits) {
							parseAST(iCompilationUnit);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	public ASTReader(IPackageFragment packageFragment) {
		systemObject = new SystemObject();
		try {
			ICompilationUnit[] compilationUnits = packageFragment.getCompilationUnits();
			for(ICompilationUnit iCompilationUnit : compilationUnits) {
				parseAST(iCompilationUnit);
			}
			IPackageFragmentRoot iPackageFragmentRoot = (IPackageFragmentRoot)packageFragment.getParent();
			IJavaElement[] children = iPackageFragmentRoot.getChildren();
			for(IJavaElement child : children) {
				if(child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					IPackageFragment rootPackageFragment = (IPackageFragment)child;
					if(!rootPackageFragment.getElementName().equals(packageFragment.getElementName()) &&
							rootPackageFragment.getElementName().contains(packageFragment.getElementName())) {
						ICompilationUnit[] subPackageCompilationUnits = rootPackageFragment.getCompilationUnits();
						for(ICompilationUnit iCompilationUnit : subPackageCompilationUnits) {
							parseAST(iCompilationUnit);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void parseAST(ICompilationUnit iCompilationUnit) {
		ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
		IFile iFile = (IFile)iCompilationUnit.getResource();
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
		        	
		        	if((modifiers & Modifier.STATIC) != 0)
		        		classObject.setStatic(true);
		        	
		        	Type superclassType = typeDeclaration.getSuperclassType();
		        	if(superclassType != null) {
		        		ITypeBinding binding = superclassType.resolveBinding();
		        		classObject.setSuperclass(binding.getQualifiedName());
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
		        			fieldObject.setClassName(classObject.getName());
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
		        			methodObject.setClassName(classObject.getName());
		        			
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
		        	systemObject.addClass(classObject);
        		}
        	}
        }	
	}

    public static SystemObject getSystemObject() {
		return systemObject;
	}
}