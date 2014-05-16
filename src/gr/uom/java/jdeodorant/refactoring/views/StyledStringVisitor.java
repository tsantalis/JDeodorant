package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.Difference;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextStyle;

public class StyledStringVisitor extends ASTVisitor {

	public static final Color ADVANCED_MATCH_LIGHT_COLOR = new Color(null, 224, 255, 224);
	public static final Color UNMAPPED_LIGHT_COLOR = new Color(null, 255, 224, 224);
	public static final Color ADVANCED_MATCH_COLOR = new Color(null, 156, 255, 156);
	public static final Color UNMAPPED_COLOR = new Color(null, 255, 156, 156);
	public static final Color DIFFERENCE_COLOR = new Color(null, new RGB(255, 255, 200));
	private static final Color STRING_COLOR = new Color(null, new RGB(112, 0, 255));
	private static final Color FIELD_COLOR = new Color(null, new RGB(0, 0, 192));
	private static final Color KEYWORD_COLOR = new Color(null, new RGB(127, 0, 85));
	private static final Font CONSOLAS_ITALIC_FONT = new Font(null, new FontData("consolas", 10, SWT.ITALIC));
	private static final Font CONSOLAS_NORMAL_FONT = new Font(null, new FontData("consolas", 10, SWT.NORMAL));
	private static final Font CONSOLAS_BOLD_FONT = new Font(null, new FontData("consolas", 10, SWT.BOLD));
	//The main field, to which all the text is appended.
	private StyledString styledString;

	//TextStyle Experiment
	private TextStyle keywordStyle;
	private TextStyle stringStyle;
	private TextStyle ordinaryStyle;
	private TextStyle differenceStyle;
	private TextStyle namedConstantStyle;
	private TextStyle nonStaticFieldStyle;
	private TextStyle staticMethodCallStyle;

	private List<ASTNode> astNodesThatAreDifferences;
	
	private ASTNode currentCompositeDiffNode = null;
	

	public StyledStringVisitor(CloneStructureNode node, CloneDiffSide position) {
		this.styledString = new StyledString();
		List<ASTNodeDifference> differences = node.getMapping().getNodeDifferences();
		//TextStyle Experiment
		keywordStyle = initializeKeywordStyle();
		stringStyle = initializeStringStyle();
		ordinaryStyle = initializeOrdinaryStyle();
		differenceStyle = initializeDifferenceStyle();
		namedConstantStyle = initializeNamedConstantStyle();
		nonStaticFieldStyle = initializeNonStaticFieldStyle();
		staticMethodCallStyle = initializeStaticMethodCallStyle();

		if(node.isElseIf()) {
			styledString.append("else", new StyledStringStyler(keywordStyle));
			appendSpace();
		}
		//Use the List of ASTNodeDifferences to recover actual ASTNodes and place them into a new list
		astNodesThatAreDifferences = new ArrayList<ASTNode>();
		generateDifferenceASTNodes(differences, position);
	}
	private void generateDifferenceASTNodes(List<ASTNodeDifference> differences, CloneDiffSide position){
		for (ASTNodeDifference nodeDifference : differences){
			Expression expr = null;
			List<Difference> diffs = nodeDifference.getDifferences();
			List<String> stringDifferences = new ArrayList<String>();
			if (position == CloneDiffSide.LEFT) {
				expr = nodeDifference.getExpression1().getExpression();
				for(Difference diff : diffs) {
					stringDifferences.add(diff.getFirstValue());
				}
			}
			else if (position == CloneDiffSide.RIGHT) {
				expr = nodeDifference.getExpression2().getExpression();
				for(Difference diff : diffs) {
					stringDifferences.add(diff.getSecondValue());
				}
			}
			if (expr instanceof QualifiedName){
				QualifiedName qualifiedName = (QualifiedName) expr;
				if(stringDifferences.contains(qualifiedName.getQualifier().toString())) {
					astNodesThatAreDifferences.add(qualifiedName.getQualifier());
				}
				if(stringDifferences.contains(qualifiedName.getName().toString())) {
					astNodesThatAreDifferences.add(qualifiedName.getName());
				}
				if(stringDifferences.contains(qualifiedName.toString())) {
					astNodesThatAreDifferences.add(qualifiedName);
				}
			}
			else {
				astNodesThatAreDifferences.add(expr);
			}
		}	
	}
	public StyledString getStyledString() {
		return styledString;
	}

	// Visit Methods
	public boolean visit(ArrayAccess expr) {
		/*
		 * Expression [ Expression ]
		 */
		activateDiffStyle(expr);
		handleExpression((Expression) expr.getArray());
		appendOpenBracket();
		handleExpression((Expression) expr.getIndex());
		appendClosedBracket();
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(ArrayCreation expr) {
		/*
		 	new PrimitiveType [ Expression ] { [ Expression ] } { [ ] }
    		new TypeName [ < Type { , Type } > ] [ Expression ] { [ Expression ] } { [ ] }
    		new PrimitiveType [ ] { [ ] } ArrayInitializer
    		new TypeName [ < Type { , Type } > ] [ ] { [ ] } ArrayInitializer
		 */
		activateDiffStyle(expr);
		styledString.append("new", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		appendSpace();
		if(expr.dimensions().isEmpty()) {
			handleType(expr.getType());
		}
		else {
			handleType(expr.getType().getElementType());
		}
		for (int i = 0; i < expr.dimensions().size(); i++) {
			appendOpenBracket();
			handleExpression((Expression) expr.dimensions().get(i));
			appendClosedBracket();
		}
		if(expr.getInitializer() != null) {
			appendSpace();
			visit(expr.getInitializer());
		}
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(ArrayInitializer expr) {
		/*
		 * ArrayInitializer: { [ Expression { , Expression} [ , ]] }
		 */
		activateDiffStyle(expr);
		appendOpenCurlyBracket();
		for (int i = 0; i < expr.expressions().size(); i++) {
			handleExpression((Expression) expr.expressions().get(i));
			if (i < expr.expressions().size() - 1) {
				appendComma();
			}
		}
		appendClosedCurlyBracket();
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(ArrayType type) {
		/*
		 * ArrayType: Type [ ]
		 */
		activateDiffStyle(type);
		handleType(type.getElementType());
		for (int i = 0; i < type.getDimensions(); i++) {
			appendOpenBracket();
			appendClosedBracket();
		}
		deactivateDiffStyle(type);
		return false;
	}

	public boolean visit(AssertStatement stmnt) {
		/*
		 * assert Expression [ : Expression ] ;
		 */
		styledString.append("assert", new StyledStringStyler(keywordStyle));
		appendSpace();
		handleExpression((Expression) stmnt.getExpression());
		if (stmnt.getMessage() != null) {
			appendSpace();
			appendColon();
			appendSpace();
			handleExpression((Expression) stmnt.getMessage());
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(Assignment expr) {
		/*
		 * Assignment: Expression AssignmentOperator Expression
		 */
		activateDiffStyle(expr);
		handleExpression((Expression) expr.getLeftHandSide());
		appendSpace();
		styledString.append(expr.getOperator().toString(), determineDiffStyle(expr, new StyledStringStyler(ordinaryStyle)));
		appendSpace();
		handleExpression((Expression) expr.getRightHandSide());
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(Block stmnt) {
		/*
		 * { { Statement } }
		 */
		return false;
	}

	public boolean visit(BooleanLiteral expr) {
		/*
		 * BooleanLiteral: true
                           false
		 */
		StyledStringStyler styler = determineDiffStyle(expr, new StyledStringStyler(keywordStyle));
		styledString.append(String.valueOf(expr.booleanValue()), styler);
		return false;
	}

	public boolean visit(BreakStatement stmnt) {
		/*
		 * break [ Identifier ] ;
		 */
		styledString.append("break", new StyledStringStyler(keywordStyle));
		if (stmnt.getLabel() != null) {
			appendSpace();
			handleExpression(stmnt.getLabel());
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(CastExpression expr) {
		/*
		 * CastExpression: ( Type ) Expression
		 */
		activateDiffStyle(expr);
		appendOpenParenthesis();
		handleType(expr.getType());
		appendClosedParenthesis();
		handleExpression((Expression) expr.getExpression());
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(CharacterLiteral expr) {
		/*
		 * Character literal nodes
		 */
		StyledStringStyler styler = determineDiffStyle(expr, new StyledStringStyler(stringStyle));
		styledString.append(expr.getEscapedValue(), styler);
		return false;
	}

	public boolean visit(ClassInstanceCreation expr) {
		/*
		 * 
		 * [ Expression . ] new [ < Type { , Type } > ] Type ( [ Expression { ,
		 * Expression } ] ) [ AnonymousClassDeclaration ]
		 */
		activateDiffStyle(expr);
		if (expr.getExpression() != null) {
			handleExpression(expr.getExpression());
			appendPeriod();
		}
		styledString.append("new", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		appendSpace();
		handleTypeArguments(expr.typeArguments());
		handleType(expr.getType());
		handleParameters(expr.arguments());
		if(expr.getAnonymousClassDeclaration() != null) {
			appendSpace();
			appendOpenCurlyBracket();
			for(int i=0; i<3; i++) {
				appendPeriod();
			}
			appendClosedCurlyBracket();
		}
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(ConditionalExpression expr) {
		/*
		 * ConditionalExpression: Expression ? Expression : Expression
		 */
		activateDiffStyle(expr);
		handleExpression((Expression) expr.getExpression());
		appendSpace();
		appendQuestionMark();
		appendSpace();
		handleExpression((Expression) expr.getThenExpression());
		appendSpace();
		appendColon();
		appendSpace();
		handleExpression((Expression) expr.getElseExpression());
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(ConstructorInvocation stmnt){
		/*
		 *  [ < Type { , Type } > ]
                      this ( [ Expression { , Expression } ] ) ;
		 */
		handleTypeArguments(stmnt.typeArguments());
		styledString.append("this", new StyledStringStyler(keywordStyle));
		handleParameters(stmnt.arguments());
		appendSemicolon();
		return false;
	}
	public boolean visit(ContinueStatement stmnt){
		/*
		 * continue [ Identifier ] ;
		 */
		styledString.append("continue", new StyledStringStyler(keywordStyle));
		if (stmnt.getLabel() != null){
			appendSpace();
			handleExpression(stmnt.getLabel());
		}
		appendSemicolon();
		return false;
	}
	public boolean visit(DoStatement stmnt){
		/*
		 * do Statement while ( Expression ) ;
		 */
		styledString.append("do", new StyledStringStyler(keywordStyle));
		appendSpace();
		styledString.append("while", new StyledStringStyler(keywordStyle));
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}
	public boolean visit(EmptyStatement stmnt){
		/*
		 * EmptyStatement: ;
		 */
		appendSemicolon();
		return false;
	}
	public boolean visit(EnhancedForStatement stmnt) {
		/*
		 *  for ( FormalParameter : Expression )
                        Statement
		 */
		styledString.append("for", new StyledStringStyler(keywordStyle));
		appendSpace();
		appendOpenParenthesis();
		SingleVariableDeclaration variableDeclaration = stmnt.getParameter();
		handleType(variableDeclaration.getType());
		appendSpace();
		handleExpression(variableDeclaration.getName());
		appendSpace();
		appendColon();
		appendSpace();
		handleExpression(stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}
	public boolean visit(ExpressionStatement expr) {
		/*
		 * ExpressionStatement: StatementExpression ;
		 */
		handleExpression((Expression) expr.getExpression());
		appendSemicolon();
		return false;
	}
	public boolean visit(ForStatement stmnt) {
		/*
		 * for (
                        [ ForInit ];
                        [ Expression ] ;
                        [ ForUpdate ] )
                        Statement
		 */
		styledString.append("for", new StyledStringStyler(keywordStyle));
		appendSpace();
		appendOpenParenthesis();
		// Handle Initializers
		for (int i = 0; i < stmnt.initializers().size(); i++) {
			handleExpression((Expression) stmnt.initializers().get(i));
			if(i < stmnt.initializers().size() - 1) {
				appendComma();
			}
		}
		appendSemicolon();
		appendSpace();
		handleExpression(stmnt.getExpression());
		appendSemicolon();
		appendSpace();
		// Handle Updaters
		for (int i = 0; i < stmnt.updaters().size(); i++) {
			handleExpression((Expression) stmnt.updaters().get(i));
			if(i < stmnt.updaters().size() - 1) {
				appendComma();
			}
		}
		appendClosedParenthesis();
		return false;
	}

	public boolean visit(FieldAccess expr) {
		/*
		 * FieldAccess: Expression . Identifier
		 */  
		activateDiffStyle(expr);
		handleExpression(expr.getExpression());
		appendPeriod();
		handleExpression(expr.getName());
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(IfStatement stmnt) {
		/*
		 * if ( Expression ) Statement [ else Statement]
		 */
		styledString.append("if", new StyledStringStyler(keywordStyle));
		appendSpace();
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}

	public boolean visit(InfixExpression expr) {
		/*
		 * InfixExpression: Expression InfixOperator Expression { InfixOperator Expression }
		 */
		activateDiffStyle(expr);
		StyledStringStyler styler = determineDiffStyle(expr, new StyledStringStyler(ordinaryStyle));
		
		Expression leftExpression = expr.getLeftOperand();
		handleExpression(leftExpression);
		appendSpace();
		styledString.append(expr.getOperator().toString(), styler);
		appendSpace();
		Expression rightExpression = expr.getRightOperand();
		handleExpression(rightExpression);
		for (int i = 0; i < expr.extendedOperands().size(); i++){
			appendSpace();
			styledString.append(expr.getOperator().toString(), styler);
			appendSpace();
			handleExpression((Expression) expr.extendedOperands().get(i));
		}
		
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(InstanceofExpression expr) {
		/*
		 * InstanceofExpression: Expression instanceof Type
		 */
		activateDiffStyle(expr);
		
		handleExpression((Expression) expr.getLeftOperand());
		appendSpace();
		styledString.append("instanceof", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		appendSpace();
		handleType((Type) expr.getRightOperand());
		
		deactivateDiffStyle(expr);
		return false;
	}
	public boolean visit(LabeledStatement stmnt){
		/*
		 * Identifier : Statement
		 */
		handleExpression(stmnt.getLabel());
		appendColon();
		return false;
	}
	public boolean visit(MarkerAnnotation annotation) {
		/*
		 * MarkerAnnotation: @ TypeName
		 */
		activateDiffStyle(annotation);
		appendAtSign();
		handleExpression(annotation.getTypeName());
		deactivateDiffStyle(annotation);
		return false;
	}
	public boolean visit(MemberValuePair pair) {
		/*
		 * MemberValuePair: SimpleName = Expression
		 */
		handleExpression(pair.getName());
		appendEquals();
		handleExpression(pair.getValue());
		return false;
	}
	public boolean visit(MethodInvocation expr) {
		/*
		 * MethodInvocation: [ Expression . ]
		                     [ < Type { , Type } > ]
		                     Identifier ( [ Expression { , Expression } ] )
		 */
		activateDiffStyle(expr);
		
		if (expr.getExpression() != null) {
			handleExpression((Expression) expr.getExpression());
			appendPeriod();
		}
		handleTypeArguments(expr.typeArguments());
		handleExpression(expr.getName());
		handleParameters(expr.arguments());
		
		deactivateDiffStyle(expr);
		return false;
	}
	public boolean visit(Modifier modifier) {
		/*
		 * public protected private static abstract final native synchronized transient volatile strictfp
		 */
		styledString.append(modifier.getKeyword().toString(), determineDiffStyle(modifier, new StyledStringStyler(keywordStyle)));
		return false;
	}
	public boolean visit(NormalAnnotation annotation) {
		/*
		 * NormalAnnotation: @ TypeName ( [ MemberValuePair { , MemberValuePair } ] )
		 */
		activateDiffStyle(annotation);
		appendAtSign();
		handleExpression(annotation.getTypeName());
		appendOpenParenthesis();
		for(int i=0; i<annotation.values().size(); i++) {
			visit((MemberValuePair) annotation.values().get(i));
			if(i < annotation.values().size() - 1) {
				appendComma();
			}
		}
		appendClosedParenthesis();
		deactivateDiffStyle(annotation);
		return false;
	}
	public boolean visit(NullLiteral expr) {
		/*
		 * null
		 */
		styledString.append("null", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		return false;
	}

	public boolean visit(NumberLiteral expr) {
		/*
		 * Number literal nodes.
		 */
		styledString.append(expr.getToken(),  determineDiffStyle(expr, new StyledStringStyler(ordinaryStyle)));
		return false;
	}

	public boolean visit(ParameterizedType type) {
		/*
		 * ParameterizedType: Type < Type { , Type } >
		 */
		activateDiffStyle(type);
		handleType(type.getType());
		appendOpenBrace();
		for (int i = 0; i < type.typeArguments().size(); i++) {
			handleType((Type) type.typeArguments().get(i));
			if (i < type.typeArguments().size() - 1) {
				appendComma();
			}
		}
		appendClosedBrace();
		deactivateDiffStyle(type);
		return false;
	}

	public boolean visit(ParenthesizedExpression expr) {
		/*
		 * ParenthesizedExpression: ( Expression )
		 */
		activateDiffStyle(expr);
		appendOpenParenthesis();
		handleExpression((Expression) expr.getExpression());
		appendClosedParenthesis();
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(PostfixExpression expr) {
		/*
		 * PostfixExpression: Expression PostfixOperator
		 */
		activateDiffStyle(expr);
		handleExpression(expr.getOperand());
		styledString.append(expr.getOperator().toString(), determineDiffStyle(expr, new StyledStringStyler(ordinaryStyle)));
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(PrefixExpression expr) {
		/*
		 * PrefixExpression: PrefixOperator Expression
		 */
		activateDiffStyle(expr);
		styledString.append(expr.getOperator().toString(), determineDiffStyle(expr, new StyledStringStyler(ordinaryStyle)));
		handleExpression((Expression) expr.getOperand());
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(PrimitiveType type) {
		/*
		 * PrimitiveType: 
		    byte
		    short
		    char
		    int
		    long
		    float
		    double
		    boolean
		    void
		 */
		styledString.append(type.getPrimitiveTypeCode().toString(), determineDiffStyle(type, new StyledStringStyler(keywordStyle)));
		return false;
	}

	public boolean visit(QualifiedName name) {
		/*
		 * Name . SimpleName
		 */
		activateDiffStyle(name);
		if (name.getQualifier() != null) {
			handleExpression(name.getQualifier());
			appendPeriod();
		}
		handleExpression(name.getName());
		deactivateDiffStyle(name);
		return false;
	}

	public boolean visit(QualifiedType type) {
		/*
		 * QualifiedType: Type . SimpleName
		 */
		activateDiffStyle(type);
		handleType(type.getQualifier());
		appendPeriod();
		handleExpression(type.getName());
		deactivateDiffStyle(type);
		return false;
	}

	public boolean visit(ReturnStatement stmnt) {
		/*
		 * ReturnStatement: return [ Expression ] ;
		 */
		styledString.append("return", new StyledStringStyler(keywordStyle));
		Expression expression = stmnt.getExpression();
		if (expression != null){
			appendSpace();
			handleExpression(expression);
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(SimpleName name) {
		/*
		 * Identifier
		 */
		StyledStringStyler styler = determineDiffStyle(name, new StyledStringStyler(ordinaryStyle));
		if (isNamedConstant(name)){
			styler.appendTextStyle(namedConstantStyle);
		}
		else if(isNonStaticField(name)){
			styler.appendTextStyle(nonStaticFieldStyle);
		}
		else if(isStaticMethodCall(name)) {
			styler.appendTextStyle(staticMethodCallStyle);
		}
		styledString.append(name.toString(), styler);
		return false;
	}

	public boolean visit(SimpleType type) {
		/*
		 * This kind of node is used to convert a name (Name) into a type (Type) by wrapping it. 
		 */
		handleExpression(type.getName());
		return false;
	}
	public boolean visit(SingleMemberAnnotation annotation) {
		/*
		 * SingleMemberAnnotation: @ TypeName ( Expression  )
		 */
		activateDiffStyle(annotation);
		appendAtSign();
		handleExpression(annotation.getTypeName());
		appendOpenParenthesis();
		handleExpression(annotation.getValue());
		appendClosedParenthesis();
		deactivateDiffStyle(annotation);
		return false;
	}
	public boolean visit(StringLiteral expr) {
		/*
		 * String literal nodes.
		 */
		StyledStringStyler styler = determineDiffStyle(expr, new StyledStringStyler(stringStyle));
		styledString.append(expr.toString(), styler);
		return false;
	}
	public boolean visit(SuperConstructorInvocation stmnt){
		/*
		 * [ Expression . ]
         [ < Type { , Type } > ]
         super ( [ Expression { , Expression } ] ) ;
		 */
		if (stmnt.getExpression() != null){
			handleExpression((Expression) stmnt.getExpression());
			appendPeriod();
		}
		handleTypeArguments(stmnt.typeArguments());
		styledString.append("super", new StyledStringStyler(keywordStyle));
		handleParameters(stmnt.arguments());
		appendSemicolon();
		return false;
	}
	public boolean visit(SuperFieldAccess expr) {
		/*
		 * SuperFieldAccess: [ ClassName . ] super . Identifier
		 */
		activateDiffStyle(expr);
		if (expr.getQualifier() != null) {
			handleExpression(expr.getQualifier());
			appendPeriod();
		}
		styledString.append("super", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		appendPeriod();
		handleExpression(expr.getName());
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(SuperMethodInvocation expr) {
		/*
		  SuperMethodInvocation: [ ClassName . ] super . [ < Type { , Type } >
		 	] Identifier ( [ Expression { , Expression } ] )
		 */
		activateDiffStyle(expr);
		if (expr.getQualifier() != null) {
			handleExpression(expr.getQualifier());
			appendPeriod();
		}
		styledString.append("super", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		appendPeriod();
		handleTypeArguments(expr.typeArguments());
		handleExpression((Expression) expr.getName());
		handleParameters(expr.arguments());
		deactivateDiffStyle(expr);
		return false;
	}
	public boolean visit(SwitchCase stmnt){
		/*
		 * case Expression  :
                default :
		 */
		if (stmnt.isDefault()){
			styledString.append("default", new StyledStringStyler(keywordStyle));
			appendColon();
		}
		else {
			styledString.append("case", new StyledStringStyler(keywordStyle));
			appendSpace();
			handleExpression((Expression) stmnt.getExpression());
			appendColon();
		}
		return false;
	}
	public boolean visit(SwitchStatement stmnt){
		/*
		 * switch ( Expression )
                        { { SwitchCase | Statement } } }
		 */
		styledString.append("switch", new StyledStringStyler(keywordStyle));
		appendSpace();
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}
	public boolean visit(SynchronizedStatement stmnt){
		/*
		 * synchronized ( Expression ) Block
		 */
		styledString.append("synchronized", new StyledStringStyler(keywordStyle));
		appendSpace();
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}
	public boolean visit(ThisExpression expr) {
		/*
		 * ThisExpression: [ ClassName . ] this
		 */
		activateDiffStyle(expr);
		if (expr.getQualifier() != null) {
			handleExpression(expr.getQualifier());
			appendPeriod();
		}
		styledString.append("this", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		deactivateDiffStyle(expr);
		return false;
	}
	public boolean visit(ThrowStatement stmnt){
		/*
		 * ThrowStatement: throw Expression ;
		 */
		styledString.append("throw", new StyledStringStyler(keywordStyle));
		appendSpace();
		handleExpression((Expression) stmnt.getExpression());
		appendSemicolon();
		return false;
	}
	public boolean visit(TryStatement stmnt){
		/*
		 * JLS 4:
		 * 	try [ ( Resources ) ]
		        Block
         	    [ { CatchClause } ]
         	    [ finally Block ]
		 */
		styledString.append("try", new StyledStringStyler(keywordStyle));
		if(!stmnt.resources().isEmpty()) {
			appendSpace();
			appendOpenParenthesis();
			for(int i=0; i<stmnt.resources().size(); i++) {
				handleExpression((VariableDeclarationExpression) stmnt.resources().get(i));
				if(i < stmnt.resources().size() - 1) {
					appendSemicolon();
					appendSpace();
				}
			}
			appendClosedParenthesis();
		}
		return false;
	}
	public boolean visit(TypeDeclarationStatement stmnt){
		/*
		 * TypeDeclaration
    		or EnumDeclaration
		 */
		return false;
	}
	public boolean visit(TypeLiteral expr) {
		/*
		 * ( Type | void ) . class
		 */
		activateDiffStyle(expr);
		handleType(expr.getType());
		appendPeriod();
		styledString.append("class", determineDiffStyle(expr, new StyledStringStyler(keywordStyle)));
		deactivateDiffStyle(expr);
		return false;
	}
	public boolean visit(UnionType type){
		/*
		 *  Type | Type { | Type }
		 */
		activateDiffStyle(type);
		for (int i = 0; i < type.types().size(); i++){
			handleType((Type) type.types().get(i));
			if (i < type.types().size() - 1){
				appendPipe();
			}
		}
		deactivateDiffStyle(type);
		return false;
	}
	public boolean visit(VariableDeclarationExpression expr) {
		/*
		  VariableDeclarationExpression:
    		{ ExtendedModifier } Type VariableDeclarationFragment
         	{ , VariableDeclarationFragment }
		 */
		activateDiffStyle(expr);
		// Append modifiers
		for (int i = 0; i < expr.modifiers().size(); i++) {
			handleModifier((IExtendedModifier) expr.modifiers().get(i));
			appendSpace();
		}
		// Append Type
		handleType(expr.getType());
		appendSpace();
		// Visit Fragments
		for (int i = 0; i < expr.fragments().size(); i++) {
			visit((VariableDeclarationFragment) expr.fragments().get(i));
			if(i < expr.fragments().size() - 1) {
				appendComma();
			}
		}
		// No semicolon needed as this is an expression
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(VariableDeclarationFragment expr) {
		/*
		 * Identifier { [] } [ = Expression ]
		 */
		activateDiffStyle(expr);
		handleExpression(expr.getName());
		for (int i = 0; i < expr.getExtraDimensions(); i++) {
			appendOpenBracket();
			appendClosedBracket();
		}
		if (expr.getInitializer() != null) {
			appendEquals();
			handleExpression(expr.getInitializer());
		}
		deactivateDiffStyle(expr);
		return false;
	}

	public boolean visit(VariableDeclarationStatement stmnt) {
		/*
		  { ExtendedModifier } Type VariableDeclarationFragment
        		{ , VariableDeclarationFragment } ;
		 */
		// Append modifiers if applicable
		for (int i = 0; i < stmnt.modifiers().size(); i++) {
			handleModifier((IExtendedModifier) stmnt.modifiers().get(i));
			appendSpace();
		}
		// Append Type
		handleType(stmnt.getType());
		appendSpace();
		// Visit Fragments
		for (int i = 0; i < stmnt.fragments().size(); i++) {
			visit((VariableDeclarationFragment) stmnt.fragments().get(i));
			if(i < stmnt.fragments().size() - 1) {
				appendComma();
			}
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(WhileStatement stmnt) {
		/*
		 * while ( Expression ) Statement
		 */
		styledString.append("while", new StyledStringStyler(keywordStyle));
		appendSpace();
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}

	public boolean visit(WildcardType type) {
		/*
		 * WildcardType: ? [ ( extends | super) Type ]
		 */
		activateDiffStyle(type);
		appendQuestionMark();
		if(type.getBound() != null) {
			if (type.isUpperBound()) {
				styledString.append("extends", determineDiffStyle(type, new StyledStringStyler(keywordStyle)));
			} else {
				styledString.append("super", determineDiffStyle(type, new StyledStringStyler(keywordStyle)));
			}
			handleType(type.getBound());
		}
		deactivateDiffStyle(type);
		return false;
	}

	// Handle expressions and determine which "Visit" to visit
	private void handleExpression(Expression expression) {
		if (expression instanceof ArrayAccess) {
			visit((ArrayAccess) expression);
		} else if (expression instanceof ArrayCreation) {
			visit((ArrayCreation) expression);
		} else if (expression instanceof ArrayInitializer) {
			visit((ArrayInitializer) expression);
		} else if (expression instanceof Assignment) {
			visit((Assignment) expression);
		} else if (expression instanceof BooleanLiteral) {
			visit((BooleanLiteral) expression);
		} else if (expression instanceof CastExpression) {
			visit((CastExpression) expression);
		} else if (expression instanceof CharacterLiteral) {
			visit((CharacterLiteral) expression);
		} else if (expression instanceof ClassInstanceCreation) {
			visit((ClassInstanceCreation) expression);
		} else if (expression instanceof ConditionalExpression) {
			visit((ConditionalExpression) expression);
		} else if (expression instanceof FieldAccess) {
			visit((FieldAccess) expression);
		} else if (expression instanceof InfixExpression) {
			visit((InfixExpression) expression);
		} else if (expression instanceof InstanceofExpression) {
			visit((InstanceofExpression) expression);
		} else if (expression instanceof MethodInvocation) {
			visit((MethodInvocation) expression);
		} else if (expression instanceof NullLiteral) {
			visit((NullLiteral) expression);
		} else if (expression instanceof NumberLiteral) {
			visit((NumberLiteral) expression);
		} else if (expression instanceof ParenthesizedExpression) {
			visit((ParenthesizedExpression) expression);
		} else if (expression instanceof PostfixExpression) {
			visit((PostfixExpression) expression);
		} else if (expression instanceof PrefixExpression) {
			visit((PrefixExpression) expression);
		} else if ((expression instanceof QualifiedName)) {
			visit((QualifiedName) expression);
		} else if (expression instanceof SimpleName) {
			visit((SimpleName) expression);
		} else if (expression instanceof StringLiteral) {
			visit((StringLiteral) expression);
		} else if (expression instanceof SuperFieldAccess) {
			visit((SuperFieldAccess) expression);
		} else if (expression instanceof SuperMethodInvocation) {
			visit((SuperMethodInvocation) expression);
		} else if (expression instanceof ThisExpression) {
			visit((ThisExpression) expression);
		} else if (expression instanceof TypeLiteral) {
			visit((TypeLiteral) expression);
		} else if (expression instanceof VariableDeclarationExpression) {
			visit((VariableDeclarationExpression) expression);
		}
	}

	private void handleType(Type type) {
		if (type instanceof PrimitiveType) {
			visit((PrimitiveType) type);
		} else if (type instanceof ArrayType) {
			visit((ArrayType) type);
		} else if (type instanceof SimpleType) {
			visit((SimpleType) type);
		} else if (type instanceof QualifiedType) {
			visit((QualifiedType) type);
		} else if (type instanceof ParameterizedType) {
			visit((ParameterizedType) type);
		} else if (type instanceof WildcardType) {
			visit((WildcardType) type);
		}
	}
	
	private void handleModifier(IExtendedModifier extendedModifier) {
		if(extendedModifier instanceof Modifier) {
			visit((Modifier) extendedModifier);
		}
		else if(extendedModifier instanceof MarkerAnnotation) {
			visit((MarkerAnnotation) extendedModifier);
		}
		else if(extendedModifier instanceof NormalAnnotation) {
			visit((NormalAnnotation) extendedModifier);
		}
		else if(extendedModifier instanceof SingleMemberAnnotation) {
			visit((SingleMemberAnnotation) extendedModifier);
		}
	}

	private void handleTypeArguments(List args) {
		if (args.size() != 0) {
			appendOpenBrace();
			for (int i = 0; i < args.size(); i++) {
				handleType((Type) args.get(i));
				if (i < args.size() - 1) {
					appendComma();
				}
			}
			appendClosedBrace();
		}
	}

	private void handleParameters(List args) {
		appendOpenParenthesis();
		for (int i = 0; i < args.size(); i++) {
			handleExpression((Expression) args.get(i));
			if (i < args.size() - 1) {
				appendComma();
			}
		}
		appendClosedParenthesis();
	}


	// Helper Methods
	private boolean isDifference(ASTNode node) {
		return astNodesThatAreDifferences.contains(node);
	}
	private boolean isNamedConstant(ASTNode node){
		if (node instanceof SimpleName){
			SimpleName simpleName = (SimpleName) node;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) 
					return true;
			}
		}
		return false;
	}
	private boolean isNonStaticField(ASTNode node){
		if (node instanceof SimpleName){
			SimpleName simpleName = (SimpleName) node;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) 
					return true;
			}
		}
		return false;
	}
	private boolean isStaticMethodCall(ASTNode node) {
		if (node instanceof SimpleName){
			SimpleName simpleName = (SimpleName) node;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.METHOD) {
				IMethodBinding methodBinding = (IMethodBinding)binding;
				if((methodBinding.getModifiers() & Modifier.STATIC) != 0) 
					return true;
			}
		}
		return false;
	}
	private StyledStringStyler determineDiffStyle(ASTNode expr, StyledStringStyler startingStyle) {
		//There are two ways difference formatting can be applied: either, this node itself is a difference, or there is a non-null currentCompositeDiffNode acting as a "super difference" across multiple nodes.
		if (isDifference(expr) || currentCompositeDiffNode != null){
			startingStyle.appendTextStyle(differenceStyle);
		}
		return startingStyle;
	}
	/*
	 * Turns on the currentCompositeDiffNode attribute, which essentially tells future visits that there is a "super difference" in effect.
	 */
	private void activateDiffStyle(ASTNode expr){
		//Change the currentCompositeDiffNode is this node is a difference and no previous node is the super difference.
		if (isDifference(expr) && currentCompositeDiffNode == null){
			currentCompositeDiffNode = expr;
		}
	}
	/*
	 * This method turns off the style if the node passed as parameter is the same node that turned it on.
	 */
	private void deactivateDiffStyle(ASTNode expr){
		if (expr.equals(currentCompositeDiffNode)){
			currentCompositeDiffNode = null;
		}
	}
	private void appendOpenParenthesis() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("(", styler);
	}

	private void appendClosedParenthesis() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(")", styler);
	}

	private void appendOpenBracket() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("[", styler);
	}

	private void appendClosedBracket() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("]", styler);
	}

	private void appendOpenBrace() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("<", styler);
	}

	private void appendClosedBrace() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(">", styler);
	}

	private void appendOpenCurlyBracket() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("{", styler);
	}

	private void appendClosedCurlyBracket() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("}", styler);
	}

	private void appendPeriod() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(".", styler);
	}

	private void appendColon() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(":", styler);
	}

	private void appendSemicolon() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(";", styler);
	}

	private void appendComma() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(", ", styler);
	}

	private void appendEquals() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(" = ", styler);
	}
	private void appendSpace() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(" ", styler);
	}

	private void appendQuestionMark() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("?", styler);
	}
	private void appendPipe() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(" | ", styler);
	}
	private void appendAtSign() {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (currentCompositeDiffNode != null){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append("@", styler);
	}
	/*
	//TextStyle Experiment
	 */
	public static TextStyle initializeKeywordStyle() {
		TextStyle keywordStyle = new TextStyle();
		keywordStyle.font = initializeBoldFont();
		keywordStyle.foreground = KEYWORD_COLOR;
		return keywordStyle;
	}
	private static TextStyle initializeNamedConstantStyle() {
		TextStyle namedConstantStyle = new TextStyle();
		namedConstantStyle.font = initializeItalicFont();
		namedConstantStyle.foreground = FIELD_COLOR;
		return namedConstantStyle;
	}
	private static TextStyle initializeNonStaticFieldStyle() {
		TextStyle fieldStyle = new TextStyle();
		fieldStyle.font = initializeFont();
		fieldStyle.foreground = FIELD_COLOR;
		return fieldStyle;
	}
	private static TextStyle initializeStringStyle() {
		TextStyle stringStyle = new TextStyle();
		stringStyle.font = initializeFont();
		stringStyle.foreground = STRING_COLOR;
		return stringStyle;
	}
	private static TextStyle initializeStaticMethodCallStyle() {
		TextStyle staticMethodCallStyle = new TextStyle();
		staticMethodCallStyle.font = initializeItalicFont();
		return staticMethodCallStyle;
	}
	private static TextStyle initializeOrdinaryStyle() {
		TextStyle ordinaryStyle = new TextStyle();
		ordinaryStyle.font = initializeFont();
		//ordinaryStyle.foreground = new Color(null, new RGB(0, 0, 0));
		return ordinaryStyle;
	}
	private static TextStyle initializeDifferenceStyle() {
		TextStyle differenceStyle = new TextStyle();
		differenceStyle.font = null; //Difference style is appended to styles with an already existing font. A null font prevents the old font from being overwritten.
		differenceStyle.background = DIFFERENCE_COLOR;
		return differenceStyle;
	}
	private static Font initializeFont(){
		//TODO Choose the font based on the user's Eclipse preferences
		return CONSOLAS_NORMAL_FONT;
	}
	private static Font initializeBoldFont(){
		//TODO Choose the font based on the user's Eclipse preferences
		return CONSOLAS_BOLD_FONT;
	}
	private static Font initializeItalicFont(){
		//TODO Choose the font based on the user's Eclipse preferences
		return CONSOLAS_ITALIC_FONT;
	}

}
