package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.Difference;

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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
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

	// private List<ASTNode> leaves = new ArrayList<ASTNode>();
	private StyledString styledString;

	/*
	private Styler keywordFormatting;
	private Styler stringFormatting;
	private Styler ordinaryFormatting;
	private Styler differenceFormatting;
	 */

	//TextStyle Experiment
	private TextStyle keywordStyle;
	private TextStyle stringStyle;
	private TextStyle ordinaryStyle;
	private TextStyle differenceStyle;
	private TextStyle namedConstantStyle;

	private List<ASTNode> astNodesThatAreDifferences;

	public StyledStringVisitor(List<ASTNodeDifference> differences, NodeVisualComparePosition position) {
		this.styledString = new StyledString();

		//TextStyle Experiment
		keywordStyle = initializeKeywordStyle();
		stringStyle = initializeStringStyle();
		ordinaryStyle = initializeOrdinaryStyle();
		differenceStyle = initializeDifferenceStyle();
		namedConstantStyle = initializeNamedConstantStyle();

		//Use the List of ASTNodeDifferences to recover actual ASTNodes and place them into a new list
		astNodesThatAreDifferences = new ArrayList<ASTNode>();
		generateDifferenceASTNodes(differences, position);
	}
	private void generateDifferenceASTNodes(List<ASTNodeDifference> differences, NodeVisualComparePosition position){
		for (ASTNodeDifference nodeDifference : differences){
			Expression expr = null;
			List<Difference> diffs = nodeDifference.getDifferences();
			List<String> stringDifferences = new ArrayList<String>();
			if (position == NodeVisualComparePosition.LEFT) {
				expr = nodeDifference.getExpression1().getExpression();
				for(Difference diff : diffs) {
					stringDifferences.add(diff.getFirstValue());
				}
			}
			else if (position == NodeVisualComparePosition.RIGHT) {
				expr = nodeDifference.getExpression2().getExpression();
				for(Difference diff : diffs) {
					stringDifferences.add(diff.getSecondValue());
				}
			}
			if (expr instanceof QualifiedName){
				QualifiedName qualifiedName = (QualifiedName) expr;
				if(stringDifferences.contains(qualifiedName.getQualifier().toString()))
					astNodesThatAreDifferences.add(qualifiedName.getQualifier());
				if(stringDifferences.contains(qualifiedName.getName().toString()))
					astNodesThatAreDifferences.add(qualifiedName.getName());

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
		 * ArrayAccess: Expression [ Expression ]
		 */
		handleExpression((Expression) expr.getArray());
		appendOpenBracket();
		handleExpression((Expression) expr.getIndex());
		appendClosedBracket();
		return false;
	}

	public boolean visit(ArrayCreation expr) {
		styledString.append("new", new StyledStringStyler(keywordStyle));
		appendSpace();
		handleType(expr.getType());
		for (int i = 0; i < expr.dimensions().size(); i++) {
			appendOpenBracket();
			handleExpression((Expression) expr.dimensions().get(i));
			appendClosedBracket();
		}
		return false;
	}

	public boolean visit(ArrayInitializer expr) {
		/*
		 * ArrayInitializer: { [ Expression { , Expression} [ , ]] }
		 */
		appendOpenCurlyBracket();
		for (int i = 0; i < expr.expressions().size(); i++) {
			handleExpression((Expression) expr.expressions().get(i));
			if (i < expr.expressions().size() - 1) {
				appendComma();
			}

		}
		appendClosedCurlyBracket();
		return false;
	}

	public boolean visit(ArrayType type) {
		handleType(type.getElementType());
		return false;
	}

	public boolean visit(AssertStatement stmnt) {
		/*
		 * assert Expression [ : Expression ] ;
		 */
		styledString.append("assert", new StyledStringStyler(keywordStyle));
		handleExpression((Expression) stmnt.getExpression());
		if (stmnt.getMessage() != null) {
			appendColon();
			handleExpression((Expression) stmnt.getExpression());
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(Assignment expr) {
		handleExpression((Expression) expr.getLeftHandSide());
		appendSpace();
		styledString.append(expr.getOperator().toString(), new StyledStringStyler(ordinaryStyle));
		appendSpace();
		handleExpression((Expression) expr.getRightHandSide());
		return false;
	}

	public boolean visit(Block stmnt) {
		/*
		 * { { Statement } }
		 */
		return false;
	}

	public boolean visit(BooleanLiteral expr) {
		styledString.append(String.valueOf(expr.booleanValue()), new StyledStringStyler(keywordStyle));
		return false;
	}

	public boolean visit(BreakStatement stmnt) {
		/*
		 * break [ Identifier ] ;
		 */
		styledString.append("break", new StyledStringStyler(keywordStyle));
		if (stmnt.getLabel() != null) {
			visit(stmnt.getLabel());
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(CastExpression expr) {
		appendOpenParenthesis();
		handleType(expr.getType());
		appendClosedParenthesis();
		handleExpression((Expression) expr.getExpression());
		return false;
	}

	public boolean visit(CharacterLiteral expr) {
		/*
		 * 
		 */
		StyledStringStyler styler = new StyledStringStyler(stringStyle);
		if (isDifference(expr)){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(expr.getEscapedValue(), styler);
		return false;
	}

	public boolean visit(ClassInstanceCreation expr) {
		/*
		 * 
		 * [ Expression . ] new [ < Type { , Type } > ] Type ( [ Expression { ,
		 * Expression } ] ) [ AnonymousClassDeclaration ]
		 */
		// if ()
		styledString.append("new", new StyledStringStyler(keywordStyle));
		appendSpace();
		handleType(expr.getType());
		// Get arguments (each is an Expression) and handle them one by one.
		handleParameters(expr.arguments());
		return false;
	}

	public boolean visit(ConditionalExpression expr) {
		/*
		 * ConditionalExpression: Expression ? Expression : Expression
		 */
		handleExpression((Expression) expr.getExpression());
		appendQuestionMark();
		handleExpression((Expression) expr.getThenExpression());
		appendColon();
		handleExpression((Expression) expr.getElseExpression());
		return false;
	}

	public boolean visit(ConstructorInvocation stmnt){
		/*
		 *  [ < Type { , Type } > ]
                      this ( [ Expression { , Expression } ] ) ;
		 */
		if (stmnt.typeArguments().size() != 0){
			appendOpenBrace();
			for (int i = 0; i < stmnt.typeArguments().size(); i++){
				handleType((Type) stmnt.typeArguments().get(i));
				if (i < stmnt.typeArguments().size() - 1) {
					appendComma();
				}
			}
			appendClosedBrace();
		}
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
			visit(stmnt.getLabel());
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
		appendSemicolon();
		return false;
	}
	public boolean visit(ExpressionStatement expr) {
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
		appendOpenParenthesis();
		// Handle Initializers
		for (int i = 0; i < stmnt.initializers().size(); i++) {
			handleExpression((Expression) stmnt.initializers().get(i));
		}
		appendSemicolon();
		handleExpression(stmnt.getExpression());
		appendSemicolon();
		// Handle Updaters
		for (int i = 0; i < stmnt.updaters().size(); i++) {
			handleExpression((Expression) stmnt.updaters().get(i));
		}
		appendClosedParenthesis();
		return false;
	}

	public boolean visit(FieldAccess expr) {
		handleExpression(expr.getExpression());
		appendPeriod();
		visit(expr.getName());
		return false;
	}

	public boolean visit(IfStatement expr) {
		/*
		 * if ( Expression ) Statement [ else Statement]
		 */
		styledString.append("if", new StyledStringStyler(keywordStyle));
		styledString.append(" (", new StyledStringStyler(ordinaryStyle));
		handleExpression((Expression) expr.getExpression());
		styledString.append(")", new StyledStringStyler(ordinaryStyle));
		// append ")"
		return false;
	}

	public boolean visit(InfixExpression expr) {
		Expression leftExpression = expr.getLeftOperand();
		handleExpression(leftExpression);
		styledString.append(" ", new StyledStringStyler(ordinaryStyle));
		styledString.append(expr.getOperator().toString());
		styledString.append(" ", new StyledStringStyler(ordinaryStyle));
		Expression rightExpression = expr.getRightOperand();
		handleExpression(rightExpression);
		return false;
	}

	public boolean visit(InstanceofExpression expr) {
		/*
		 * InstanceofExpression: Expression instanceof Type
		 */
		handleExpression((Expression) expr.getLeftOperand());
		appendSpace();
		styledString.append("instanceof", new StyledStringStyler(keywordStyle));
		appendSpace();
		handleType((Type) expr.getRightOperand());
		return false;
	}
	public boolean visit(LabeledStatement stmnt){
		/*
		 * Identifier : Statement
		 */
		visit(stmnt.getLabel());
		return false;
	}
	public boolean visit(MethodInvocation expr) {
		if (expr.getExpression() != null) {
			handleExpression((Expression) expr.getExpression());
			appendPeriod();
		}
		visit(expr.getName());
		handleParameters(expr.arguments());
		return false;
	}

	public boolean visit(Modifier modifier) {
		styledString
		.append(modifier.getKeyword().toString(), new StyledStringStyler(keywordStyle));
		return false;
	}

	public boolean visit(Name expr) {
		handleExpression(expr);
		return false;
	}

	public boolean visit(NullLiteral expr) {
		/*
		 * null
		 */
		styledString.append("null", new StyledStringStyler(keywordStyle));
		return false;
	}

	public boolean visit(NumberLiteral expr) {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (isDifference(expr)){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(expr.getToken(), styler);
		return false;
	}

	public boolean visit(ParameterizedType type) {
		/*
		 * ParameterizedType: Type < Type { , Type } >
		 */
		handleType(type.getType());
		appendOpenBrace();
		for (int i = 0; i < type.typeArguments().size(); i++) {
			handleType((Type) type.typeArguments());
			if (i < type.typeArguments().size() - 1) {
				appendComma();
			}
		}
		appendClosedBrace();
		return false;
	}

	public boolean visit(ParenthesizedExpression expr) {
		/*
		 * ParenthesizedExpression: ( Expression )
		 */
		appendOpenParenthesis();
		handleExpression((Expression) expr.getExpression());
		appendClosedParenthesis();
		return false;
	}

	public boolean visit(PostfixExpression expr) {
		handleExpression(expr.getOperand());
		styledString.append(expr.getOperator().toString(), new StyledStringStyler(ordinaryStyle));
		return false;
	}

	public boolean visit(PrefixExpression expr) {
		/*
		 * PrefixExpression: PrefixOperator Expression
		 */
		styledString.append(expr.getOperator().toString(), new StyledStringStyler(ordinaryStyle));
		handleExpression((Expression) expr.getOperand());
		return false;
	}

	public boolean visit(PrimitiveType type) {
		styledString.append(type.getPrimitiveTypeCode().toString(),
				new StyledStringStyler(keywordStyle));
		return false;
	}

	public boolean visit(QualifiedName name) {
		if (name.getQualifier() != null) {
			visit(name.getQualifier());
			styledString.append(".", new StyledStringStyler(ordinaryStyle));
		}
		visit(name.getName());
		// styledString.append(".", ordinaryFormatting);

		// Why is this true??
		return false;
	}

	public boolean visit(QualifiedType type) {
		/*
		 * QualifiedType: Type . SimpleName
		 */
		handleType(type.getQualifier());
		visit(type.getName());
		return false;
	}

	public boolean visit(ReturnStatement statement) {
		styledString.append("return", new StyledStringStyler(keywordStyle));
		Expression expression = statement.getExpression();
		if (expression != null){
			appendSpace();
			handleExpression(expression);
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(SimpleName name) {
		StyledStringStyler styler = null;
		if (isDifference(name)){
			styler = new StyledStringStyler(differenceStyle);
		}
		else{
			styler = new StyledStringStyler(ordinaryStyle);
		}
		
		if (isNamedConstant(name)){
			styler.appendTextStyle(namedConstantStyle);
		}
		
		styledString.append(name.toString(), styler);
		return false;
	}

	public boolean visit(SimpleType type) {
		StyledStringStyler styler = new StyledStringStyler(ordinaryStyle);
		if (isDifference(type)){
			styler.appendTextStyle(differenceStyle);
		}
		styledString.append(type.toString(), styler);
		return false;
	}

	public boolean visit(StringLiteral expr) {
		StyledStringStyler styler = new StyledStringStyler(stringStyle);
		if (isDifference(expr)){
			styler.appendTextStyle(differenceStyle);
		}
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
		if (stmnt.typeArguments().size() != 0){
			appendOpenBrace();
			for (int i = 0; i < stmnt.typeArguments().size(); i++) {
				handleType((Type) stmnt.typeArguments().get(i));
				if (i < stmnt.typeArguments().size() - 1) {
					appendComma();
				}
			}
			appendClosedBrace();
		}
		styledString.append("super", new StyledStringStyler(keywordStyle));
		handleParameters(stmnt.arguments());
		return false;
	}
	public boolean visit(SuperFieldAccess expr) {
		/*
		 * SuperFieldAccess: [ ClassName . ] super . Identifier
		 */
		if (expr.getQualifier() != null) {
			handleExpression(expr.getQualifier());
			appendPeriod();
		}
		styledString.append("super", new StyledStringStyler(keywordStyle));
		appendPeriod();
		visit(expr.getName());
		return false;
	}

	public boolean visit(SuperMethodInvocation expr) {
		/*
		  SuperMethodInvocation: [ ClassName . ] super . [ < Type { , Type } >
		 	] Identifier ( [ Expression { , Expression } ] )
		 */
		if (expr.getQualifier() != null) {
			handleExpression(expr.getQualifier());
			appendPeriod();
		}
		styledString.append("super", new StyledStringStyler(keywordStyle));
		appendPeriod();
		if (expr.typeArguments().size() != 0) {
			appendOpenBrace();
			for (int i = 0; i < expr.typeArguments().size(); i++) {
				handleType((Type) expr.typeArguments().get(i));
				if (i < expr.typeArguments().size() - 1) {
					appendComma();
				}
			}
			appendClosedBrace();
		}
		handleExpression((Expression) expr.getName());
		handleParameters(expr.arguments());
		return false;
	}
	public boolean visit(SwitchCase stmnt){
		/*
		 * case Expression  :
                default :
		 */
		if (stmnt.isDefault()){
			styledString.append("default", new StyledStringStyler(keywordStyle));
		}
		else {
			styledString.append("case", new StyledStringStyler(keywordStyle));
			handleExpression((Expression) stmnt.getExpression());
			appendSemicolon();
		}

		return false;
	}
	public boolean visit(SwitchStatement stmnt){
		/*
		 * switch ( Expression )
                        { { SwitchCase | Statement } } }
		 */
		styledString.append("switch", new StyledStringStyler(keywordStyle));
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}
	public boolean visit(SynchronizedStatement stmnt){
		/*
		 * synchronized ( Expression ) Block
		 */
		//TODO Verify that children get visited
		styledString.append("synchronized", new StyledStringStyler(keywordStyle));
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}
	public boolean visit(ThisExpression expr) {
		styledString.append("this", new StyledStringStyler(keywordStyle));
		return false;
	}
	public boolean visit(ThrowStatement stmnt){
		styledString.append("throw", new StyledStringStyler(keywordStyle));
		handleExpression((Expression) stmnt.getExpression());
		appendSemicolon();
		return false;
	}
	public boolean visit(TryStatement stmnt){
		/*
		 * JLS 3:
		 * 	try Block
         	[ { CatchClause } ]
         	[ finally Block ]
		 */
		styledString.append("try", new StyledStringStyler(keywordStyle));
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
		handleType(expr.getType());
		appendPeriod();
		styledString.append("class", new StyledStringStyler(keywordStyle));
		return false;
	}
	public boolean visit(UnionType type){
		/*
		 *  Type | Type { | Type }
		 */
		for (int i = 0; i < type.types().size(); i++){
			handleType((Type) type.types().get(i));
			if (i < type.types().size() - 1){
				appendPipe();
			}
		}
		return false;
	}
	public boolean visit(VariableDeclarationExpression expr) {
		/*
		  VariableDeclarationExpression:
    		{ ExtendedModifier } Type VariableDeclarationFragment
         	{ , VariableDeclarationFragment }
		 */
		// Append modifiers
		for (int i = 0; i < expr.modifiers().size(); i++) {
			visit((Modifier) expr.modifiers().get(i));
			appendSpace();
		}
		// Append Type
		handleType(expr.getType());
		appendSpace();
		// Visit Fragments
		for (int i = 0; i < expr.fragments().size(); i++) {
			visit((VariableDeclarationFragment) expr.fragments().get(i));
		}
		// No semicolon needed as this is an expression
		return false;
	}

	public boolean visit(VariableDeclarationFragment expr) {
		/*
		 * Identifier { [] } [ = Expression ]
		 */
		visit(expr.getName());
		if (expr.getInitializer() != null) {
			appendEquals();
			handleExpression(expr.getInitializer());
		}
		return false;
	}

	public boolean visit(VariableDeclarationStatement stmnt) {
		/*
		  { ExtendedModifier } Type VariableDeclarationFragment
        		{ , VariableDeclarationFragment } ;
		 */
		// Append modifiers if applicable
		if (stmnt.modifiers() != null) {
			for (int i = 0; i < stmnt.modifiers().size(); i++) {
				visit((Modifier) stmnt.modifiers().get(i));
				appendSpace();
			}
		}
		// Append Type
		handleType(stmnt.getType());
		// Special Situations
		// If it's an ArrayType, draw brackets.
		if (stmnt.getType() instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) stmnt.getType();
			for (int i = 0; i < arrayType.getDimensions(); i++) {
				appendOpenBracket();
				appendClosedBracket();
			}
		}
		appendSpace();
		// Visit Fragments
		for (int i = 0; i < stmnt.fragments().size(); i++) {
			visit((VariableDeclarationFragment) stmnt.fragments().get(i));
		}
		appendSemicolon();
		return false;
	}

	public boolean visit(WhileStatement stmnt) {
		/*
		 * while ( Expression ) Statement
		 */
		styledString.append("while", new StyledStringStyler(keywordStyle));
		appendOpenParenthesis();
		handleExpression((Expression) stmnt.getExpression());
		appendClosedParenthesis();
		return false;
	}

	public boolean visit(WildcardType type) {
		/*
		 * WildcardType: ? [ ( extends | super) Type ]
		 */
		appendOpenBrace();
		appendQuestionMark();
		if (type.isUpperBound()) {
			styledString.append("extends", new StyledStringStyler(keywordStyle));
		} else {
			styledString.append("super", new StyledStringStyler(keywordStyle));
		}
		handleType(type.getBound());
		appendClosedBrace();
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
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if((variableBinding.getModifiers() & Modifier.FINAL) != 0 && (variableBinding.getModifiers() & Modifier.STATIC) != 0) 
					return true;
			}
		}
		return false;
	}


	private void appendOpenParenthesis() {
		styledString.append("(", new StyledStringStyler(ordinaryStyle));
	}

	private void appendClosedParenthesis() {
		styledString.append(")", new StyledStringStyler(ordinaryStyle));
	}

	private void appendOpenBracket() {
		styledString.append("[", new StyledStringStyler(ordinaryStyle));
	}

	private void appendClosedBracket() {
		styledString.append("]", new StyledStringStyler(ordinaryStyle));
	}

	private void appendOpenBrace() {
		styledString.append("<", new StyledStringStyler(ordinaryStyle));
	}

	private void appendClosedBrace() {
		styledString.append(">", new StyledStringStyler(ordinaryStyle));
	}

	private void appendOpenCurlyBracket() {
		styledString.append("{", new StyledStringStyler(ordinaryStyle));
	}

	private void appendClosedCurlyBracket() {
		styledString.append("}", new StyledStringStyler(ordinaryStyle));
	}

	private void appendPeriod() {
		styledString.append(".", new StyledStringStyler(ordinaryStyle));
	}

	private void appendColon() {
		styledString.append(" : ", new StyledStringStyler(ordinaryStyle));
	}

	private void appendSemicolon() {
		styledString.append(";", new StyledStringStyler(ordinaryStyle));
	}

	private void appendComma() {
		styledString.append(", ", new StyledStringStyler(ordinaryStyle));
	}

	private void appendEquals() {
		styledString.append(" = ", new StyledStringStyler(ordinaryStyle));
	}

	private void appendSpace() {
		styledString.append(" ", new StyledStringStyler(ordinaryStyle));
	}

	private void appendQuestionMark() {
		styledString.append("?", new StyledStringStyler(ordinaryStyle));
	}
	private void appendPipe() {
		styledString.append(" | ", new StyledStringStyler(ordinaryStyle));
	}

	/*
	//TextStyle Experiment
	 */
	private TextStyle initializeKeywordStyle() {
		TextStyle keywordStyle = new TextStyle();
		keywordStyle.font = initializeBoldFont();
		keywordStyle.foreground = new Color(null, new RGB(127, 0, 85));
		return keywordStyle;
	}
	private TextStyle initializeNamedConstantStyle() {
		TextStyle namedConstantStyle = new TextStyle();
		namedConstantStyle.font = initializeItalicFont();
		namedConstantStyle.foreground = new Color(null, new RGB(0, 0, 192));
		return namedConstantStyle;
	}
	private TextStyle initializeStringStyle() {
		TextStyle stringStyle = new TextStyle();
		stringStyle.font = initializeFont();
		stringStyle.foreground = new Color(null, new RGB(112, 0, 255));
		return stringStyle;
	}
	private TextStyle initializeOrdinaryStyle() {
		TextStyle ordinaryStyle = new TextStyle();
		ordinaryStyle.font = initializeFont();
		ordinaryStyle.foreground = new Color(null, new RGB(0, 0, 0));
		return ordinaryStyle;
	}
	private TextStyle initializeDifferenceStyle() {
		TextStyle differenceStyle = new TextStyle();
		differenceStyle.font = null; //Difference style is appended to styles with an already existing font. A null font prevents the old font from being overwritten.
		differenceStyle.background = new Color(null, new RGB(255, 255, 200));
		return differenceStyle;
	}
	private Font initializeFont(){
		//TODO Choose the font based on the user's Eclipse preferences
		return new Font(null, new FontData("consolas", 10, SWT.NORMAL));
	}
	private Font initializeBoldFont(){
		//TODO Choose the font based on the user's Eclipse preferences
		return new Font(null, new FontData("consolas", 10, SWT.BOLD));
	}
	private Font initializeItalicFont(){
		//TODO Choose the font based on the user's Eclipse preferences
		return new Font(null, new FontData("consolas", 10, SWT.ITALIC));
	}

}
