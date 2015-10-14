package gr.uom.java.history;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.TypeCheckCodeFragmentAnalyzer;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TypeCheckingEvolution implements Evolution {
	private Map<ProjectVersionPair, Double> typeCheckSimilarityMap;
	private Map<ProjectVersionPair, Double> typeCheckChangeMap;
	private Map<ProjectVersion, String> typeCheckCodeMap;
	
	public TypeCheckingEvolution(ProjectEvolution projectEvolution, TypeCheckElimination selectedTypeCheckElimination, IProgressMonitor monitor) {
		this.typeCheckSimilarityMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.typeCheckChangeMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.typeCheckCodeMap = new LinkedHashMap<ProjectVersion, String>();
		List<Entry<ProjectVersion, IJavaProject>> projectEntries = projectEvolution.getProjectEntries();
		if(monitor != null)
			monitor.beginTask("Comparing state/type checks", projectEntries.size()-1);

		Entry<ProjectVersion, IJavaProject> currentEntry = projectEntries.get(0);
		ProjectVersion currentProjectVersion = currentEntry.getKey();
		IJavaProject currentProject = currentEntry.getValue();
		List<TypeCheckElimination> currentTypeCheckEliminations = generateTypeCheckEliminationsWithinJavaProject(currentProject, selectedTypeCheckElimination);
		StringBuilder currentStringBuilder = new StringBuilder();
		int currentTypeCheckCounter = 1;
		for(TypeCheckElimination elimination : currentTypeCheckEliminations) {
			currentStringBuilder.append("## case " + currentTypeCheckCounter + " ##").append("\n");
			currentStringBuilder.append(elimination.getTypeCheckCodeFragment().toString());
			currentTypeCheckCounter++;
		}
		typeCheckCodeMap.put(currentProjectVersion, currentStringBuilder.toString());
		int currentGroupSize = currentTypeCheckEliminations.size();

		for(int i=1; i<projectEntries.size(); i++) {
			if(monitor != null && monitor.isCanceled())
    			throw new OperationCanceledException();
			Entry<ProjectVersion, IJavaProject> nextEntry = projectEntries.get(i);
			ProjectVersion nextProjectVersion = nextEntry.getKey();
			IJavaProject nextProject = nextEntry.getValue();
			if(monitor != null)
				monitor.subTask("Comparing versions " + currentProjectVersion + " and " + nextProjectVersion);
			List<TypeCheckElimination> nextTypeCheckEliminations = generateTypeCheckEliminationsWithinJavaProject(nextProject, selectedTypeCheckElimination);
			StringBuilder nextStringBuilder = new StringBuilder();
			int nextTypeCheckCounter = 1;
			for(TypeCheckElimination elimination : nextTypeCheckEliminations) {
				nextStringBuilder.append("## case " + nextTypeCheckCounter + " ##").append("\n");
				nextStringBuilder.append(elimination.getTypeCheckCodeFragment().toString());
				nextTypeCheckCounter++;
			}
			typeCheckCodeMap.put(nextProjectVersion, nextStringBuilder.toString());
			int nextGroupSize = nextTypeCheckEliminations.size();

			ProjectVersionPair pair = new ProjectVersionPair(currentProjectVersion, nextProjectVersion);
			if(currentGroupSize != 0 || nextGroupSize != 0) {
				int maxGroupSize = Math.max(currentGroupSize, nextGroupSize);
				double similarity = (double)(maxGroupSize - Math.abs(nextGroupSize-currentGroupSize))/(double)maxGroupSize;
				typeCheckSimilarityMap.put(pair, similarity);
				double change = (double)Math.abs(nextGroupSize-currentGroupSize)/(double)maxGroupSize;
				typeCheckChangeMap.put(pair, change);
			}
			else {
				typeCheckSimilarityMap.put(pair, null);
				typeCheckChangeMap.put(pair, null);
			}
			currentProjectVersion = nextProjectVersion;
			currentGroupSize = nextGroupSize;
			if(monitor != null)
				monitor.worked(1);
		}
		if(monitor != null)
			monitor.done();
	}

	private List<TypeCheckElimination> generateTypeCheckEliminationsWithinJavaProject(IJavaProject javaProject, TypeCheckElimination elimination) {
		List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
		try {
			IPackageFragmentRoot[] iPackageFragmentRoots = javaProject.getPackageFragmentRoots();
			for(IPackageFragmentRoot iPackageFragmentRoot : iPackageFragmentRoots) {
				IJavaElement[] children = iPackageFragmentRoot.getChildren();
				for(IJavaElement child : children) {
					if(child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFragment = (IPackageFragment)child;
						ICompilationUnit[] iCompilationUnits = iPackageFragment.getCompilationUnits();
						for(ICompilationUnit iCompilationUnit : iCompilationUnits) {
							ASTParser parser = ASTParser.newParser(ASTReader.JLS);
					        parser.setKind(ASTParser.K_COMPILATION_UNIT);
					        parser.setSource(iCompilationUnit);
					        parser.setResolveBindings(true); // we need bindings later on
					        CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
							typeCheckEliminations.addAll(generateTypeCheckEliminationsWithinCompilationUnit(compilationUnit, elimination));
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return typeCheckEliminations;
	}

	private List<TypeCheckElimination> generateTypeCheckEliminationsWithinCompilationUnit(CompilationUnit compilationUnit, TypeCheckElimination elimination) {
		List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
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
        			typeCheckEliminations.addAll(generateTypeCheckEliminationsWithinTypeDeclaration(typeDeclaration, elimination));
        		}
        	}
        }
        return typeCheckEliminations;
	}

	private List<TypeCheckElimination> generateTypeCheckEliminationsWithinTypeDeclaration(TypeDeclaration typeDeclaration, TypeCheckElimination originalTypeCheckElimination) {
		List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
		for(MethodDeclaration method : typeDeclaration.getMethods()) {
			Block methodBody = method.getBody();
			if(methodBody != null) {
				List<TypeCheckElimination> list = generateTypeCheckEliminationsWithinMethodBody(methodBody);
				for(TypeCheckElimination typeCheckElimination : list) {
					if(!typeCheckElimination.allTypeCheckBranchesAreEmpty()) {
						TypeCheckCodeFragmentAnalyzer analyzer = new TypeCheckCodeFragmentAnalyzer(typeCheckElimination, typeDeclaration, method, null);
						if((typeCheckElimination.getTypeField() != null || typeCheckElimination.getTypeLocalVariable() != null || typeCheckElimination.getTypeMethodInvocation() != null) &&
								typeCheckElimination.allTypeCheckingsContainStaticFieldOrSubclassType() && typeCheckElimination.isApplicable()) {
							if(originalTypeCheckElimination.matchingStatesOrSubTypes(typeCheckElimination))
								typeCheckEliminations.add(typeCheckElimination);
						}
					}
				}
			}
		}
		return typeCheckEliminations;
	}

	private List<TypeCheckElimination> generateTypeCheckEliminationsWithinMethodBody(Block methodBody) {
		List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> switchStatements = statementExtractor.getSwitchStatements(methodBody);
		for(Statement statement : switchStatements) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			TypeCheckElimination typeCheckElimination = new TypeCheckElimination();
			typeCheckElimination.setTypeCheckCodeFragment(switchStatement);
			List<Statement> statements = switchStatement.statements();
			Expression switchCaseExpression = null;
			boolean isDefaultCase = false;
			Set<Expression> switchCaseExpressions = new LinkedHashSet<Expression>();
			for(Statement statement2 : statements) {
				if(statement2 instanceof SwitchCase) {
					SwitchCase switchCase = (SwitchCase)statement2;
					switchCaseExpression = switchCase.getExpression();
					isDefaultCase = switchCase.isDefault();
					if(!isDefaultCase)
						switchCaseExpressions.add(switchCaseExpression);
				}
				else {
					if(statement2 instanceof Block) {
						Block block = (Block)statement2;
						List<Statement> blockStatements = block.statements();
						for(Statement blockStatement : blockStatements) {
							if(!(blockStatement instanceof BreakStatement)) {
								for(Expression expression : switchCaseExpressions) {
									typeCheckElimination.addTypeCheck(expression, blockStatement);
								}
								if(isDefaultCase) {
									typeCheckElimination.addDefaultCaseStatement(blockStatement);
								}
							}
						}
						List<Statement> branchingStatements = statementExtractor.getBranchingStatements(statement2);
						if(branchingStatements.size() > 0) {
							for(Expression expression : switchCaseExpressions) {
								if(!typeCheckElimination.containsTypeCheckExpression(expression))
									typeCheckElimination.addEmptyTypeCheck(expression);
							}
							switchCaseExpressions.clear();
						}
					}
					else {
						if(!(statement2 instanceof BreakStatement)) {
							for(Expression expression : switchCaseExpressions) {
								typeCheckElimination.addTypeCheck(expression, statement2);
							}
							if(isDefaultCase) {
								typeCheckElimination.addDefaultCaseStatement(statement2);
							}
						}
						List<Statement> branchingStatements = statementExtractor.getBranchingStatements(statement2);
						if(statement2 instanceof BreakStatement || statement2 instanceof ReturnStatement || branchingStatements.size() > 0) {
							for(Expression expression : switchCaseExpressions) {
								if(!typeCheckElimination.containsTypeCheckExpression(expression))
									typeCheckElimination.addEmptyTypeCheck(expression);
							}
							switchCaseExpressions.clear();
						}
					}
				}
			}
			typeCheckEliminations.add(typeCheckElimination);
		}

		List<Statement> ifStatements = statementExtractor.getIfStatements(methodBody);
		TypeCheckElimination typeCheckElimination = new TypeCheckElimination();
		int i = 0;
		for(Statement statement : ifStatements) {
			IfStatement ifStatement = (IfStatement)statement;
			Expression ifExpression = ifStatement.getExpression();
			Statement thenStatement = ifStatement.getThenStatement();
			if(thenStatement instanceof Block) {
				Block block = (Block)thenStatement;
				List<Statement> statements = block.statements();
				for(Statement statement2 : statements) {
					typeCheckElimination.addTypeCheck(ifExpression, statement2);
				}
			}
			else {
				typeCheckElimination.addTypeCheck(ifExpression, thenStatement);
			}
			Statement elseStatement = ifStatement.getElseStatement();
			if(elseStatement != null) {
				if(elseStatement instanceof Block) {
					Block block = (Block)elseStatement;
					List<Statement> statements = block.statements();
					for(Statement statement2 : statements) {
						typeCheckElimination.addDefaultCaseStatement(statement2);
					}
				}
				else if(!(elseStatement instanceof IfStatement)) {
					typeCheckElimination.addDefaultCaseStatement(elseStatement);
				}
			}
			if(ifStatements.size()-1 > i) {
				IfStatement nextIfStatement = (IfStatement)ifStatements.get(i+1);
				if(!ifStatement.getParent().equals(nextIfStatement)) {
					typeCheckElimination.setTypeCheckCodeFragment(ifStatement);
					typeCheckEliminations.add(typeCheckElimination);
					typeCheckElimination = new TypeCheckElimination();
				}
			}
			else {
				typeCheckElimination.setTypeCheckCodeFragment(ifStatement);
				typeCheckEliminations.add(typeCheckElimination);
			}
			i++;
		}
		return typeCheckEliminations;
	}

	public Set<Entry<ProjectVersionPair, Double>> getSimilarityEntries() {
		return typeCheckSimilarityMap.entrySet();
	}

	public Set<Entry<ProjectVersionPair, Double>> getChangeEntries() {
		return typeCheckChangeMap.entrySet();
	}

	public String getCode(ProjectVersion projectVersion) {
		return typeCheckCodeMap.get(projectVersion);
	}
}
