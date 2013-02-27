package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodeMatcher;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.CStyleScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IASTPositionProvider;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.MalformedDeclaration;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Problem;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.TraversalContinuation;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.parser.c4script.ast.ArrayExpression;
import net.arctics.clonk.parser.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.BreakStatement;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallExpr;
import net.arctics.clonk.parser.c4script.ast.CallInherited;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ContinueStatement;
import net.arctics.clonk.parser.c4script.ast.DoWhileStatement;
import net.arctics.clonk.parser.c4script.ast.Ellipsis;
import net.arctics.clonk.parser.c4script.ast.EmptyStatement;
import net.arctics.clonk.parser.c4script.ast.False;
import net.arctics.clonk.parser.c4script.ast.ForStatement;
import net.arctics.clonk.parser.c4script.ast.FunctionBody;
import net.arctics.clonk.parser.c4script.ast.FunctionDescription;
import net.arctics.clonk.parser.c4script.ast.GarbageStatement;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.IfStatement;
import net.arctics.clonk.parser.c4script.ast.IntegerLiteral;
import net.arctics.clonk.parser.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.parser.c4script.ast.KeywordStatement;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.MissingStatement;
import net.arctics.clonk.parser.c4script.ast.NewProplist;
import net.arctics.clonk.parser.c4script.ast.Nil;
import net.arctics.clonk.parser.c4script.ast.NumberLiteral;
import net.arctics.clonk.parser.c4script.ast.Parenthesized;
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.True;
import net.arctics.clonk.parser.c4script.ast.Tuple;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.ast.UnaryOp;
import net.arctics.clonk.parser.c4script.ast.Unfinished;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarInitialization;
import net.arctics.clonk.parser.c4script.ast.WhileStatement;
import net.arctics.clonk.parser.c4script.ast.Whitespace;
import net.arctics.clonk.parser.c4script.effect.EffectFunction;
import net.arctics.clonk.parser.c4script.statictyping.TypeAnnotation;
import net.arctics.clonk.resource.ClonkBuilder;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ProjectSettings.Typing;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A C4Script parser. Parses declarations in a script and stores it in a {@link Script} object (sold separately). Also parses syntax trees of functions.
 * Those can be used for various purposes, including
 * checking correctness (aiming to detect all kinds of errors like undeclared identifiers, supplying values of wrong type to functions etc.), converting old
 * c4script code to #strict-compliant "new-style" code and forming the base of navigation operations like "Find Declaration", "Find References" etc.
 */
public class C4ScriptParser extends CStyleScanner implements IASTPositionProvider {

	static final EnumSet<Problem> DISABLED_INSTANT_ERRORS = EnumSet.of(
		Problem.TokenExpected,
		Problem.InvalidExpression,
		Problem.BlockNotClosed,
		Problem.NotAllowedHere
	);
	private static final char[] SEMICOLON_DELIMITER = new char[] { ';' };
	private static final char[] OPENING_BLOCK_BRACKET_DELIMITER = new char[] { '{' };
	private static final char[] COMMA_OR_CLOSE_BRACKET = new char[] { ',', ']' };
	private static final char[] COMMA_OR_CLOSE_BLOCK = new char[] { ',', '}' };

	protected Function currentFunction;
	private Declaration currentDeclaration;
	private TypeAnnotation parsedTypeAnnotation;

	/**
	 * Reference to project file the script was read from.
	 */
	protected IFile scriptFile;
	/**
	 * Script container, the parsed declarations are put into
	 */
	protected Script script;
	/**
	 * Whether to parse the script with static typing rules.
	 */
	protected Typing typing;
	protected Typing migrationTyping;
	/**
	 * Set of functions already parsed. Won't be parsed again.
	 */
	private SpecialEngineRules specialEngineRules;
	private ClonkBuilder builder;
	private Engine engine;

	public final SpecialEngineRules specialEngineRules() {return specialEngineRules;}
	public void setBuilder(ClonkBuilder builder) {this.builder = builder;}
	public ClonkBuilder builder() {return builder;}
	public final Typing typing() { return typing; }
	public Script script() { return script; }

	/**
	 * Sets the current function. There should be a good reason to call this.
	 * @param func
	 */
	protected void setCurrentFunction(Function func) {
		if (func != currentFunction) {
			currentFunction = func;
			setCurrentDeclaration(func);
		}
	}

	/**
	 * Returns the first variable in the parent chain of currentDeclaration
	 * @return
	 */
	public Variable currentVariable() {
		return currentDeclaration() != null ? currentDeclaration().parentOfType(Variable.class) : null;
	}

	/**
	 * Returns the declaration that is currently being parsed.
	 * @return
	 */
	public Declaration currentDeclaration() {
		return currentDeclaration;
	}

	/**
	 * @param currentDeclaration the currentDeclaration to set
	 */
	private final void setCurrentDeclaration(Declaration currentDeclaration) {
		this.currentDeclaration = currentDeclaration;
	}

	/**
	 * Returns the script object as an object if it is one or null if it is not.
	 * @return The script object as  C4Object
	 */
	public Definition definition() {
		if (script instanceof Definition)
			return (Definition) script;
		return null;
	}

	/**
	 * Creates a script parser. The script is read from the file attached to the script (queried through getScriptFile()).
	 */
	public C4ScriptParser(Script script) {
		this((IFile) script.source(), script);
		initialize();
	}

	private List<TypeAnnotation> typeAnnotations;

	public List<TypeAnnotation> typeAnnotations() {return typeAnnotations;}

	/**
	 * Initialize some state fields. Needs to be called before actual parsing takes place.
	 */
	protected void initialize() {
		if (script != null) {
			engine = script.engine();
			specialEngineRules = engine != null ? script.engine().specialRules() : null;
			typing = Typing.ParametersOptionallyTyped;
			migrationTyping = null;
			if (script.index() instanceof ProjectIndex) {
				ProjectIndex projIndex = (ProjectIndex) script.index();
				ClonkProjectNature nature = projIndex.nature();
				if (nature != null) {
					typing = nature.settings().typing;
					migrationTyping = nature.settings().migrationTyping;
				}
			}
			script.setTypeAnnotations(null);
		}
	}

	/**
	 * Creates a C4Script parser that parses a file within the project.
	 * Results are stored in <code>object</code>
	 * @param scriptFile
	 * @param obj
	 */
	public C4ScriptParser(IFile scriptFile, Script script) {
		super(scriptFile);
		this.scriptFile = scriptFile;
		this.script = script;
		initialize();
	}

	/**
	 * Creates a C4Script parser object for external files.
	 * Results are stored in <code>object</code>
	 * @param stream
	 * @param size
	 * @param object
	 */
	public C4ScriptParser(InputStream stream, Script script) {
		super(stream);
		this.scriptFile = null;
		this.script = script;
		initialize();
	}

	/**
	 * Creates a C4Script parser that parses an arbitrary string.
	 * @param withString
	 * @param script
	 */
	public C4ScriptParser(String withString, Script script, IFile scriptFile) {
		super(withString);
		this.scriptFile = scriptFile;
		this.script = script;
		initialize();
	}

	/**
	 * Perform a full parsing (that includes cleaning up the current state of the script container, parsing declarations and parsing function code).
	 * @throws ParsingException
	 */
	public void parse() throws ParsingException {
		clean();
		parseDeclarations();
		validate();
	}

	public String parseTokenAndReturnAsString() throws ParsingException {
		String s;
		Number number;
		if ((s = parseString()) != null)
			return '"'+s+'"';
		if ((s = parseIdentifier()) != null)
			return s;
		if ((number = parseNumber()) != null)
			return String.valueOf(number);
		else
			return String.valueOf((char)read());
	}

	public ASTNode parseNode() throws ParsingException {
		ASTNode node = parseDeclaration();
		if (node == null)
			node = parseStatement();
		return node;
	}

	/**
	 * Parse declarations but not function code. Before calling this it should be ensured that the script is {@link #clean()}-ed to avoid duplicates.
	 */
	public void parseDeclarations() {
		if (typing.allowsNonParameterAnnotations() || migrationTyping != null)
			typeAnnotations = new ArrayList<TypeAnnotation>();
		this.seek(0);
		try {
			parseInitialSourceComment();
			eatWhitespace();
			while (!reachedEOF()) {
				if (parseDeclaration() == null)
					readUnexpectedBlock();
				eatWhitespace();
			}
			for (Variable v : script.variables())
				if (v.initializationExpression() != null && v.initializationExpression().parent() == null)
					v.initializationExpression().setParent(v);
		}
		catch (ParsingException e) { return; }
		finally {
			if (markers != null)
				markers.deploy();
		}
	}
	private void readUnexpectedBlock() throws ParsingException {
		eatWhitespace();
		if (!reachedEOF()) {
			int start = this.offset;
			if (peek() == '{') {
				read();
				for (int depth = 1; depth > 0 && !reachedEOF();) {
					eatWhitespace();
					switch (read()) {
					case '}':
						depth--;
						break;
					case '{':
						depth++;
						break;
					}
				}
				error(Problem.UnexpectedBlock, start, this.offset, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION);
			} else {
				String tokenText = parseTokenAndReturnAsString();
				error(Problem.CommaOrSemicolonExpected, this.offset, this.offset+1, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, tokenText);
			}
		}
	}

	private void parseInitialSourceComment() {
		eat(WHITESPACE_CHARS);
		Comment sourceComment = null;
		for (Comment c; (c = parseComment()) != null;)
			sourceComment = c;
		if (sourceComment != null)
			script.setSourceComment(sourceComment.text().replaceAll("\\r?\\n", "<br/>")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Parse function code. Side effects include:
	 * 	-Errors (or things the parser thinks are errors) listed in the Problems view
	 * 	-Types for variables inferred more or less accurately
	 * @throws ParsingException
	 */
	public void validate() throws ParsingException {
		setCurrentDeclaration(null);
		for (Directive directive : script.directives())
			directive.validate(this);
		distillAdditionalInformation();
		if (markers != null)
			markers.deploy();
	}

	/**
	 * OC: Get information out of the script that was previously to be found in additional files (like the name of the {@link Definition}). Specifically, parse the Definition() function.
	 */
	public void distillAdditionalInformation() {
		if (script instanceof Definition) {
			final Definition obj = (Definition) script;
			obj.chooseLocalizedName(); // ClonkRage Names.txt
			// local Name = "Exploder";
			Variable nameLocal = script.findLocalVariable("Name", false); //$NON-NLS-1$
			if (nameLocal != null) {
				ASTNode expr = nameLocal.initializationExpression();
				if (expr != null)
					obj.setName(expr.evaluateStatic(nameLocal.initializationExpression().parentOfType(Function.class)).toString());
			}
		}
	}

	/**
	 * Parse code of one single function. {@link #validate()} calls this for all functions in the script.
	 * @param function The function to be parsed
	 * @throws ParsingException
	 */
	private void parseFunctionBody(Function function) throws ParsingException {
		int bodyStart = this.offset;
		if (!function.staticallyTyped())
			function.assignType(PrimitiveType.UNKNOWN, false);

		// reset local vars
		function.resetLocalVarTypes();
		// parse code block
		EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
		List<ASTNode> statements = new LinkedList<ASTNode>();
		function.setBodyLocation(new SourceLocation(bodyStart, Integer.MAX_VALUE));
		parseStatementBlock(offset, statements, options, function.isOldStyle());
		FunctionBody bunch = new FunctionBody(function, statements);
		if (function.isOldStyle() && statements.size() > 0)
			function.bodyLocation().setEnd(statements.get(statements.size() - 1).end() + sectionOffset());
		else
			function.setBodyLocation(new SourceLocation(bodyStart, this.offset-1));
		function.storeBody(bunch, functionSource(function));
	}

	private Variable addVarParmsParm(Function func) {
		Variable v = new Variable("...", PrimitiveType.ANY); //$NON-NLS-1$
		v.setParent(func);
		v.setScope(Variable.Scope.PARAMETER);
		func.addParameter(v);
		return v;
	}

	@Override
	protected Comment parseComment() {
		int offset = this.offset;
		Comment c = super.parseComment();
		if (c != null) {
			if (lastComment != null && lastComment.precedesOffset(offset, buffer))
				c.previousComment = lastComment;
			setRelativeLocation(c, offset, this.offset);
			c.setAbsoluteOffset(offset);
			lastComment = c;
			return c;
		}
		return null;
	}

	public final void setRelativeLocation(ASTNode expr, int start, int end) {
		int bodyOffset = sectionOffset();
		expr.setLocation(start-bodyOffset, end-bodyOffset);
	}

	/**
	 * Parses the declaration at the current this position.
	 * @return whether parsing was successful
	 * @throws ParsingException
	 */
	protected Declaration parseDeclaration() throws ParsingException {
		final int startOfDeclaration = this.offset;
		Declaration result = null;

		if (peek() == '#') {
			read();
			// directive
			String directiveName = parseIdentifier();
			DirectiveType type = DirectiveType.makeType(directiveName);
			if (type == null) {
				warning(Problem.UnknownDirective, startOfDeclaration, startOfDeclaration + 1 + (directiveName != null ? directiveName.length() : 0), 0, directiveName);
				this.moveUntil(BufferedScanner.NEWLINE_CHARS);
				result = new MalformedDeclaration(readStringAt(startOfDeclaration, this.offset));
				result.setLocation(startOfDeclaration, this.offset);
			}
			else {
				String content = parseDirectiveParms();
				Directive directive = new Directive(type, content);
				directive.setLocation(absoluteSourceLocation(startOfDeclaration, this.offset));
				script.addDeclaration(directive);
				result = directive;
			}
		}

		if (result == null) {
			FunctionHeader functionHeader = FunctionHeader.parse(this, true);
			if (functionHeader != null) {
				Function f = parseFunctionDeclaration(functionHeader);
				if (f != null)
					result = f;
			}
		}

		if (result == null) {
			String word = readIdent();
			Scope scope = word != null ? Scope.makeScope(word) : null;
			if (scope != null) {
				List<VarInitialization> vars = parseVariableDeclaration(false, true, scope, collectPrecedingComment(startOfDeclaration));
				if (vars != null) {
					for (VarInitialization vi : vars)
						if (vi.expression != null)
							synthesizeInitializationFunction(vi);
					result = new Variables(vars);
				}
			}
		}

		if (result == null)
			this.seek(startOfDeclaration);
		return result;
	}

	protected InitializationFunction synthesizeInitializationFunction(VarInitialization vi) {
		InitializationFunction synth = new InitializationFunction(vi.variable);
		final SourceLocation expressionLocation = absoluteSourceLocationFromExpr(vi.expression);
		final int es = expressionLocation.start();
		vi.expression.traverse(new IASTVisitor<Void>() {
			@Override
			public TraversalContinuation visitNode(ASTNode node, Void parser) {
				node.setLocation(node.start()-es, node.end()-es);
				CallDeclaration cd = as(node, CallDeclaration.class);
				if (cd != null)
					cd.setParmsRegion(cd.parmsStart()-es, cd.parmsEnd()-es);
				return TraversalContinuation.Continue;
			}
		}, null);
		synth.setBodyLocation(expressionLocation);
		synth.storeBody(vi.expression, readStringAt(expressionLocation));
		synth.setName(vi.variable.name()+"=");
		synth.setLocation(vi.start(), expressionLocation.end());
		synth.setVisibility(vi.variable.scope() == Scope.STATIC ? FunctionScope.GLOBAL : FunctionScope.PRIVATE);
		script.addDeclaration(synth);
		return synth;
	}

	private String parseDirectiveParms() {
		StringBuffer buffer = new StringBuffer(80);
		while (!reachedEOF() && !BufferedScanner.isLineDelimiterChar((char)peek()) && parseComment() == null)
			buffer.append((char)read());
		// do let the comment be eaten
		return buffer.length() != 0
			? buffer.toString().trim()
				: null;
	}

	private static class FunctionHeader {
		public final String name;
		public final FunctionScope scope;
		public final boolean isOldStyle;
		public final int nameStart;
		public final int start;
		public final IType returnType;
		public final TypeAnnotation typeAnnotation;
		public final Comment desc;
		public FunctionHeader(int start, String name, FunctionScope scope, boolean isOldStyle, int nameStart, IType returnType, TypeAnnotation typeAnnotation, Comment desc) {
			super();
			this.start = start;
			this.name = name;
			this.scope = scope;
			this.isOldStyle = isOldStyle;
			this.nameStart = nameStart;
			this.returnType = returnType;
			this.typeAnnotation = typeAnnotation;
			this.desc = desc;
		}
		public static FunctionHeader parse(C4ScriptParser parser, boolean allowOldStyle) throws ParsingException {
			Comment desc = parser.collectPrecedingComment(parser.offset);
			int initialOffset = parser.offset;
			int nameStart = parser.offset;
			boolean isOldStyle = false;
			String name = null;
			String s = parser.parseIdentifier();
			IType returnType = null;
			TypeAnnotation typeAnnotation = null;
			if (s != null) {
				FunctionScope scope = FunctionScope.makeScope(s);
				if (scope != null) {
					parser.eatWhitespace();
					nameStart = parser.offset;
					s = parser.parseIdentifier();
				} else
					scope = FunctionScope.PUBLIC;
				if (s != null)
					if (s.equals(Keywords.Func)) {
						parser.eatWhitespace();
						int bt = parser.offset;
						if (parser.typing != Typing.Dynamic) {
							returnType = parser.parseTypeAnnotation(true, true);
							typeAnnotation = parser.parsedTypeAnnotation;
						}
						switch (parser.typing) {
						case Static:
							if (returnType == null) {
								returnType = PrimitiveType.ANY;
								parser.seek(bt);
								parser.error(Problem.TypeExpected, bt,bt+1, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION);
							}
							break;
						case ParametersOptionallyTyped:
							if (parser.engine != parser.script && returnType != PrimitiveType.REFERENCE) {
								returnType = null;
								parser.seek(bt);
							}
							break;
						default:
							break;
						}
						parser.eatWhitespace();
						nameStart = parser.offset;
						s = parser.parseIdentifier();
						if (s == null) {
							parser.seek(bt);
							returnType = null;
							s = parser.parseIdentifier();
						}
						if (s != null) {
							name = s;
							isOldStyle = false;
						}
					} else {
						name = s;
						isOldStyle = true;
					}
				if (name != null && (allowOldStyle || !isOldStyle))
					if (isOldStyle) {
						int backtrack = parser.offset;
						parser.eatWhitespace();
						boolean isProperLabel = parser.read() == ':' && parser.read() != ':';
						parser.seek(backtrack);
						if (isProperLabel)
							return new FunctionHeader(initialOffset, s, scope, true, nameStart, returnType, typeAnnotation, desc);
					} else if (parser.peekAfterWhitespace() == '(')
						return new FunctionHeader(initialOffset, name, scope, false, nameStart, returnType, typeAnnotation, desc);
			}
			parser.seek(initialOffset);
			return null;
		}
		public void apply(Function func) {
			func.setOldStyle(isOldStyle);
			func.setName(name);
			func.setVisibility(scope);
			func.assignType(returnType, returnType != null);
			func.setNameStart(nameStart);
			if (typeAnnotation != null)
				typeAnnotation.setTarget(func);
		}
	}

	private List<VarInitialization> parseVariableDeclaration(boolean reportErrors, boolean checkForFinalSemicolon, Scope scope, Comment comment) throws ParsingException {
		if (scope == null)
			return null;

		final int backtrack = this.offset;

		List<VarInitialization> createdVariables = null;
		Function currentFunc = currentFunction;

		eatWhitespace();
		switch (scope) {
		case STATIC:
			int pos = this.offset;
			if (readIdent().equals(Keywords.Const))
				scope = Scope.CONST;
			else
				this.seek(pos);
			break;
		case VAR:
			if (currentFunc == null) {
				error(Problem.VarOutsideFunction, backtrack-scope.toKeyword().length(), backtrack, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, scope.toKeyword(), Keywords.GlobalNamed, Keywords.LocalNamed);
				scope = Scope.LOCAL;
			}
			break;
		default:
			break;
		}

		{
			int rewind = this.offset;
			do {
				eatWhitespace();
				parsedTypeAnnotation = null;
				IType staticType;
				TypeAnnotation typeAnnotation;
				int bt = this.offset;
				int typeExpectedAt = -1;
				// when parsing an engine script from (res/engines/...), allow specifying the type directly
				if (script == engine || typing.allowsNonParameterAnnotations()) {
					staticType = parseTypeAnnotation(true, false);
					if (staticType != null) {
						typeAnnotation = parsedTypeAnnotation;
						eatWhitespace();
					}
					else if (typing == Typing.Static) {
						typeAnnotation = null;
						typeExpectedAt = this.offset;
						staticType = PrimitiveType.ERRONEOUS;
					} else
						typeAnnotation = null;
				}
				else {
					typeAnnotation = placeholderTypeAnnotationIfMigrating(this.offset);
					staticType = null;
				}

				int s = this.offset;
				String varName = readIdent();
				if (s > bt && (
					varName.length() == 0 ||
					// ugh - for (var object in ...) workaround
					(migrationTyping == Typing.Static && varName.equals(Keywords.In))
					)) {
					seek(s = bt);
					typeAnnotation = null;
					staticType = null;
					varName = readIdent();
				}
				if (varName.length() == 0) {
					seek(backtrack);
					return null;
				} else if (typeExpectedAt != -1)
					typeRequiredAt(typeExpectedAt);

				Declaration outerDec = currentDeclaration();
				try {
					Variable var = createVarInScope(currentFunction, varName, scope, bt, this.offset, comment);
					if (typeAnnotation != null)
						typeAnnotation.setTarget(var);
					if (staticType != null)
						var.assignType(staticType, true);
					if (parsedTypeAnnotation != null)
						parsedTypeAnnotation.setTarget(var);
					setCurrentDeclaration(var);
					VarInitialization varInitialization;
					ASTNode initializationExpression = null;
					{
						eatWhitespace();
						if (peek() == '=') {
							if (scope != Variable.Scope.CONST && currentFunc == null && !engine.settings().supportsNonConstGlobalVarAssignment)
								error(Problem.NonConstGlobalVarAssignment, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW);
							read();
							eatWhitespace();
							initializationExpression = parseExpression(reportErrors);
							var.setInitializationExpression(initializationExpression);
						} else if (scope == Scope.CONST && script != engine)
							error(Problem.ConstantValueExpected, this.offset-1, this.offset, Markers.NO_THROW);
						else if (scope == Scope.STATIC && script == engine)
							var.forceType(PrimitiveType.INT); // most likely
					}
					varInitialization = new VarInitialization(varName, initializationExpression, bt-sectionOffset(), this.offset-sectionOffset(), var);
					varInitialization.type = staticType;
					if (createdVariables == null)
						createdVariables = new LinkedList<VarInitialization>();
					createdVariables.add(varInitialization);
					rewind = this.offset;
					eatWhitespace();
				} finally {
					setCurrentDeclaration(outerDec);
				}
			} while(read() == ',');
			seek(rewind);
		}

		if (checkForFinalSemicolon) {
			int rewind = this.offset;
			eatWhitespace();
			if (read() != ';') {
				seek(rewind);
				error(Problem.CommaOrSemicolonExpected, this.offset-1, this.offset, Markers.NO_THROW);
			}
		}

		// look for comment following directly and decorate the newly created variables with it
		String inlineComment = textOfInlineComment();
		if (inlineComment != null) {
			inlineComment = inlineComment.trim();
			for (VarInitialization v : createdVariables)
				v.variable.setUserDescription(inlineComment);
		}

		return createdVariables != null && createdVariables.size() > 0 ? createdVariables : null;
	}

	private TypeAnnotation placeholderTypeAnnotationIfMigrating(int offset) {
		TypeAnnotation typeAnnotation;
		if (migrationTyping != null && migrationTyping.allowsNonParameterAnnotations()) {
			typeAnnotation = new TypeAnnotation(offset, offset);
			if (typeAnnotations != null)
				typeAnnotations.add(typeAnnotation);
		} else
			typeAnnotation = null;
		return typeAnnotation;
	}

	private void typeRequiredAt(int typeExpectedAt) throws ParsingException {
		error(Problem.TypeExpected, typeExpectedAt, typeExpectedAt+1,  Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW);
	}

	private Variable findVar(Function function, String name, Scope scope) {
		switch (scope) {
		case VAR:
			return function != null ? function.findVariable(name) : null;
		case CONST: case STATIC: case LOCAL:
			return script().findLocalVariable(name, false);
		default:
			return null;
		}
	}

	public Variable createVarInScope(Function function, String varName, Scope scope, int start, int end, Comment description) {
		Variable result = findVar(function, varName, scope);
		if (result != null)
			return result;

		result = newVariable(varName, scope);
		switch (scope) {
		case PARAMETER:
			result.setParent(function);
			function.parameters().add(result);
			break;
		case VAR:
			result.setParent(function);
			function.locals().add(result);
			break;
		case CONST: case STATIC: case LOCAL:
			result.setParent(script());
			script().addDeclaration(result);
		}
		result.setLocation(start, end);
		result.setUserDescription(description != null ? description.text().trim() : null);
		return result;
	}

	protected Variable newVariable(String varName, Scope scope) {
		return new Variable(varName, scope);
	}

	private IType parseTypeAnnotation(boolean topLevel, boolean required) throws ParsingException {
		if (topLevel)
			parsedTypeAnnotation = null;
		final int backtrack = this.offset;
		int start = this.offset;
		String str;
		IType t = null;
		ID id;
		if (peek() == '&') {
			if (!script.engine().settings().supportsRefs)
				error(Problem.PrimitiveTypeNotSupported, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW,
					'&', script.engine().name());
			read();
			t = PrimitiveType.REFERENCE;
		}
		else if ((str = parseIdentifier()) != null || ((id = parseID()) != null && (str = id.stringValue()) != null)) {
			PrimitiveType pt;
			t = pt = PrimitiveType.fromString(str, script == engine||typing==Typing.Static);
			if (pt != null && !script.engine().supportsPrimitiveType(pt))
				t = null;
			else if (t == null && typing.allowsNonParameterAnnotations())
				if (script.index() != null && engine.acceptsId(str))
					t = script.index().definitionNearestTo(script.scriptFile(), ID.get(str));
			if (t != null) {
				int p = offset;
				eatWhitespace();
				RefinementIndicator: switch (read()) {
				case '&':
					t = ReferenceType.make(t);
					break;
				case '[':
					if (typing == Typing.Static || migrationTyping == Typing.Static)
						if (t == PrimitiveType.ARRAY) {
							eatWhitespace();
							IType elementType = parseTypeAnnotation(false, true);
							expect(']');
							if (elementType != null)
								t = new ArrayType(elementType, ArrayType.NO_PRESUMED_LENGTH);
							break RefinementIndicator;
						}
					break;
					//$FALL-THROUGH$
				default:
					seek(p);
				}
				while (true) {
					int s = this.offset;
					eatWhitespace();
					if (read() != '|') {
						seek(s);
						break;
					} else
						eatWhitespace();
					IType option = parseTypeAnnotation(false, true);
					if (option != null)
						t = TypeUnification.unify(t, option);
					else
						break;
					eatWhitespace();
				}
				if (topLevel)
					if (typeAnnotations != null) {
						parsedTypeAnnotation = new TypeAnnotation(backtrack, this.offset);
						parsedTypeAnnotation.setType(t);
						typeAnnotations.add(parsedTypeAnnotation);
					}
			}
		}
		if (t == null) {
			if (typing == Typing.Static)
				if (required) {
					error(Problem.InvalidType, start, offset, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, readStringAt(start, offset));
					return null;
				}
			if (migrationTyping == Typing.Static)
				if (topLevel && typeAnnotations != null)
					// placeholder annotation
					typeAnnotations.add(parsedTypeAnnotation = new TypeAnnotation(backtrack, backtrack));
			this.seek(backtrack);
		}
		return t;
	}

	/**
	 * Parse a function declaration.
	 * @param firstWord The first word that led to the conclusion that a function declaration is up next
	 * @return Whether parsing of the function declaration succeeded
	 * @throws ParsingException
	 */
	private Function parseFunctionDeclaration(FunctionHeader header) throws ParsingException {
		int endOfHeader;
		eatWhitespace();
		setCurrentFunction(null);
		if (header.isOldStyle)
			warning(Problem.OldStyleFunc, header.nameStart, header.nameStart+header.name.length(), 0);
		Function func;
		setCurrentFunction(func = newFunction(header.name));
		header.apply(func);
		func.setScript(script);
		eatWhitespace();
		int shouldBeBracket = read();
		if (shouldBeBracket != '(') {
			if (header.isOldStyle && shouldBeBracket == ':')
				{} // old style funcs have no named parameters
			else
				tokenExpectedError("("); //$NON-NLS-1$
		} else {
			// get parameters
			boolean parmExpected = false;
			do {
				eat(WHITESPACE_CHARS);
				Comment parameterCommentPre = parseComment();
				eat(WHITESPACE_CHARS);
				Variable parm = parseParameter(func);
				eat(WHITESPACE_CHARS);
				Comment parameterCommentPost = parseComment();
				eat(WHITESPACE_CHARS);
				if (parm != null) {
					StringBuilder commentBuilder = new StringBuilder(30);
					if (parameterCommentPre != null)
						commentBuilder.append(parameterCommentPre.text());
					if (parameterCommentPost != null) {
						if (parameterCommentPre != null)
							commentBuilder.append("\n"); //$NON-NLS-1$
						commentBuilder.append(parameterCommentPost.text());
					}
					parm.setUserDescription(commentBuilder.toString());
				} else if (parmExpected)
					error(Problem.NameExpected, this.offset, offset+1, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION);
				parmExpected = false;
				int readByte = read();
				if (readByte == ')')
					break; // all parameters parsed
				else if (readByte == ',')
					parmExpected = true;
				else
					error(Problem.UnexpectedToken, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION, (char)readByte);  //$NON-NLS-1$//$NON-NLS-2$
			} while(!reachedEOF());
		}
		endOfHeader = this.offset;
		lastComment = null;
		eatWhitespace();
		if (lastComment != null)
			func.setUserDescription(lastComment.text());

		// check initial opening bracket which is mandatory for NET2 funcs
		int token = read();
		boolean parseBody = true;
		if (token != '{')
			if (script == engine) {
				if (token != ';')
					tokenExpectedError(";"); //$NON-NLS-1$
				else
					parseBody = false;
			} else if (!header.isOldStyle)
				tokenExpectedError("{"); //$NON-NLS-1$
			else
				this.unread();

		// body
		if (parseBody)
			parseFunctionBody(func);
		else
			func.setBodyLocation(null);
		int funEnd = this.offset;
		eatWhitespace();
		if (header.desc != null)
			header.desc.applyDocumentation(func);
		else {
			// look for comment in the same line as the closing '}' which is common for functions packed into one line
			// hopefully there won't be multi-line functions with such a comment attached at the end
			Comment c = commentImmediatelyFollowing();
			if (c != null) {
				func.setUserDescription(c.text());
				funEnd = this.offset;
			}
		}

		// finish up
		func.setLocation(absoluteSourceLocation(header.start, funEnd));
		func.setHeader(absoluteSourceLocation(header.start, endOfHeader));
		Function existingFunction = script.findLocalFunction(func.name(), false);
		if (existingFunction != null && existingFunction.isGlobal() == func.isGlobal())
			warning(Problem.DuplicateDeclaration, func, Markers.ABSOLUTE_MARKER_LOCATION, func.name());
		script.addDeclaration(func);
		setCurrentFunction(null); // to not suppress errors in-between functions
		return func;
	}

	/**
	 * Create a new {@link Function}. Depending on what {@link SpecialEngineRules} the current {@link Engine} has, the function might be some specialized instance ({@link DefinitionFunction} or {@link EffectFunction}for example)
	 * @param nameWillBe What the name of the function will be.
	 * @return The newly created function. Might be of some special class.
	 */
	protected Function newFunction(String nameWillBe) {
		if (specialEngineRules != null)
			for (SpecialFuncRule funcRule : specialEngineRules.defaultParmTypeAssignerRules()) {
				Function f = funcRule.newFunction(nameWillBe);
				if (f != null)
					return f;
			}
		return new Function();
	}

	private Comment collectPrecedingComment(int absoluteOffset) {
		Comment c = (lastComment != null && lastComment.precedesOffset(absoluteOffset, buffer)) ? lastComment : null;
		lastComment = null;
		return c;
	}

	private String textOfInlineComment() {
		int pos = this.offset;
		this.eat(BufferedScanner.WHITESPACE_WITHOUT_NEWLINE_CHARS);
		if (this.eat(BufferedScanner.NEWLINE_CHARS) == 0) {
			Comment c = parseComment();
			if (c != null)
				return c.text();
		}
		this.seek(pos);
		return null;
	}

	private Number parseHexNumber() throws ParsingException {
		int offset = this.offset;
		boolean isHex = read() == '0' && read() == 'x';
		if (!isHex) {
			this.seek(offset);
			return null;
		}
		else {
			Number number;
			offset += 2;
			int count = 0;
			if (isHex)
				do {
					int readByte = read();
					if (
						('0' <= readByte && readByte <= '9') ||
						('A' <= readByte && readByte <= 'F') ||
						('a' <= readByte && readByte <= 'f')
					) {
						count++;
						continue;
					}
					else {
						unread();
						if (count > 0) {
							this.seek(offset);
							number = Long.parseLong(this.readString(count), 16);
							this.seek(offset+count);
							return number;
						} else
							return null;
					}
				} while(!reachedEOF());
			return null;
		}
	}

	private Number parseNumber() throws ParsingException {
		Number number;
		final int offset = this.offset;
		int count = 0;
		boolean floatingPoint = false;
		do {
			int readByte = read();
			if ('0' <= readByte && readByte <= '9') {
				count++;
				continue;
			}
			else if (count > 0 && readByte == '.' && !floatingPoint) {
				count++;
				floatingPoint = true;
				continue;
			}
			else {
				unread();
				if (count > 0)
					break;
				else
					return null; // well, this seems not to be a number at all
			}
		} while(!reachedEOF());
		this.seek(offset);
		String numberString = this.readString(count);
		if (floatingPoint)
			try {
				number = Double.parseDouble(numberString);
			} catch (NumberFormatException e) {
				number = Double.MAX_VALUE;
				error(Problem.NotANumber, offset, offset+count, Markers.NO_THROW, numberString);
			}
		else
			try {
				number = Long.parseLong(numberString);
			} catch (NumberFormatException e) {
				number = Integer.MAX_VALUE;
				error(Problem.NotANumber, offset, offset+count, Markers.NO_THROW, numberString);
			}
		this.seek(offset+count);
		return number;
	}

	private boolean parseEllipsis() {
		int offset = this.offset;
		String e = this.readString(3);
		if (e != null && e.equals("...")) //$NON-NLS-1$
			return true;
		this.seek(offset);
		return false;
	}

	private String parseMemberOperator() throws ParsingException {
		int savedOffset = this.offset;
		int firstChar = read();
		if (firstChar == '.')
			return "."; //$NON-NLS-1$
		else if (firstChar == '-')
			if (read() == '>') {
				savedOffset = this.offset;
				eatWhitespace();
				if (read() == '~')
					return "->~"; //$NON-NLS-1$
				else {
					this.seek(savedOffset);
					return "->"; //$NON-NLS-1$
				}
			}
		this.seek(savedOffset);
		return null;
	}

	/**
	 * Loop types.
	 */
	public enum LoopType {
		/**
		 * for (...;...;...) ...
		 */
		For,
		/**
		 * for (... in ...) ...
		 */
		IterateArray,
		/**
		 * while (...) ...
		 */
		While
	}

	/**
	 * Parse an {@link Operator} at the current location.
	 * @return the operator referenced in the code at offset
	 */
	private Operator parseOperator() {
		final int offset = this.offset;
		final char[] chars = new char[] { (char)read(), (char)read()  };
		String s = new String(chars);

		// never to be read as an operator
		if (s.equals("->")) { //$NON-NLS-1$
			this.seek(offset);
			return null;
		}

		Operator result = Operator.getOperator(s);
		if (result != null) {
			// new_variable should not be parsed as ne w_variable -.-
			if (result == Operator.ne || result == Operator.eq) {
				int followingChar = read();
				if (BufferedScanner.isWordPart(followingChar)) {
					this.seek(offset);
					return null;
				} else
					unread();
			}
			return result;
		}

		s = s.substring(0, 1);
		result = Operator.getOperator(s);
		if (result != null) {
			unread();
			return result;
		}

		this.seek(offset);
		return null;
	}

	private void warning(Problem code, int errorStart, int errorEnd, int flags, Object... args) {
		try {
			marker(code, errorStart, errorEnd, flags|Markers.NO_THROW, IMarker.SEVERITY_WARNING, args);
		} catch (ParsingException e) {
			// won't happen
		}
	}
	private void warning(Problem code, IRegion region, int flags, Object... args) {
		warning(code, region.getOffset(), region.getOffset()+region.getLength(), flags, args);
	}
	private void error(Problem code, int errorStart, int errorEnd, int flags, Object... args) throws ParsingException {
		marker(code, errorStart, errorEnd, flags, IMarker.SEVERITY_ERROR, args);
	}

	private Markers markers;

	public void setMarkers(Markers markers) {
		this.markers = markers;
	}

	public Markers markers() {
		if (markers != null)
			return markers;
		else if (builder != null)
			return builder.markers();
		else {
			markers = new Markers();
			markers.applyProjectSettings(script.index());
			return markers;
		}
	}

	/**
	 * Create a code marker.
	 * @param code The error code
	 * @param markerStart Start of the marker (relative to function body)
	 * @param markerEnd End of the marker (relative to function body)
	 * @param noThrow true means that no exception will be thrown after creating the marker.
	 * @param severity IMarker severity value
	 * @param args Format arguments used when creating the marker message with the message from the error code as the format.
	 * @throws ParsingException
	 */
	public void marker(Problem code, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ParsingException {
		markers().marker(this, code, null, markerStart, markerEnd, flags, severity, args);
	}

	public IMarker todo(String todoText, int markerStart, int markerEnd, int priority) {
		return markers().todo(file(), null, todoText, markerStart, markerEnd, priority);
	}

	private void tokenExpectedError(String token) throws ParsingException {
		int off = this.offset;
		while (off >= 0 && off < size && buffer[off] == '\t')
			off--;
		error(Problem.TokenExpected, off, off+1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, token);
	}

	private boolean parseStaticFieldOperator() {
		final int offset = this.offset;
		final int a = read(), b = read();
		if (a == ':' && b == ':')
			return true;
		else {
			this.seek(offset);
			return false;
		}
	}

	private transient ASTNode[] _elementsReusable = new ASTNode[4];
	private transient boolean _elementsInUse = false;

	private ASTNode parseSequence(boolean reportErrors) throws ParsingException {
		ASTNode[] _elements;
		boolean useReusable = !_elementsInUse;
		if (useReusable) {
			_elements = _elementsReusable;
			_elementsInUse = true;
		} else
			_elements = new ASTNode[4];

		int sequenceParseStart = this.offset;
		eatWhitespace();
		int sequenceStart = this.offset;
		Operator preop = parseOperator();
		ASTNode result = null;
		if (preop != null && preop.isPrefix()) {
			ASTNode followingExpr = parseSequence(reportErrors);
			if (followingExpr == null) {
				error(Problem.ExpressionExpected, this.offset, this.offset+1, Markers.NO_THROW);
				followingExpr = placeholderExpression(offset);
			}
			result = new UnaryOp(preop, UnaryOp.Placement.Prefix, followingExpr);
			setRelativeLocation(result, sequenceStart, this.offset);
			return result;
		} else
			this.seek(sequenceStart); // don't skip operators that aren't prefixy
		int num = 0;
		ASTNode elm;
		ASTNode prevElm = null;
		int noWhitespaceEating = sequenceStart;
		boolean proper = true;
		boolean noNewProplist = false;
		Number number;
		ID id;
		Loop: do {
			elm = null;

			noWhitespaceEating = this.offset;
			eatWhitespace();
			int elmStart = this.offset;

			// operator always ends a sequence without operators
			if (parseOperator() != null) {// || fReader.readWord().equals(Keywords.In)) {
				this.seek(elmStart);
				break;
			}
			// kind of a hack; stop at 'in' but only if there were other things before it
			if (num > 0 && Keywords.In.equals(readIdent())) {
				this.seek(elmStart);
				break;
			}
			this.seek(elmStart); // nothing special to end the sequence; make sure we start from the beginning

			// hex number
			if (elm == null && (number = parseHexNumber()) != null)
				//				if (parsedNumber < Integer.MIN_VALUE || parsedNumber > Integer.MAX_VALUE)
				//					warningWithCode(ErrorCode.OutOfIntRange, elmStart, fReader.getPosition(), String.valueOf(parsedNumber));
				elm = new IntegerLiteral(number.longValue(), true);

			// id
			if (elm == null && (id = parseID()) != null)
				elm = new IDLiteral(id);

			// number
			if (elm == null && (number = parseNumber()) != null)
				elm = NumberLiteral.from(number);

			// variable or function
			if (elm == null) {
				String word = readIdent();
				if (word != null && word.length() > 0)
					// tricky new keyword parsing that also respects use of new as regular identifier
					if (!noNewProplist && word.equals(Keywords.New)) {
						// don't report errors here since there is the possibility that 'new' will be interpreted as variable name in which case this expression will be parsed again
						ASTNode prototype = parseExpression(OPENING_BLOCK_BRACKET_DELIMITER, false);
						boolean treatNewAsVarName = false;
						if (prototype == null)
							treatNewAsVarName = true;
						else {
							eatWhitespace();
							ProplistDeclaration proplDec = parsePropListDeclaration(reportErrors);
							if (proplDec != null)
								elm = new NewProplist(proplDec, prototype);
							else
								treatNewAsVarName = true;
						}
						if (treatNewAsVarName) {
							// oh wait, just a variable named new with some expression following it
							this.seek(noWhitespaceEating);
							elm = ASTNode.NULL_EXPR; // just to satisfy loop condition
							noNewProplist = true;
							continue Loop;
						}
					}
					else {
						int beforeWhitespace = this.offset;
						this.eatWhitespace();
						if (read() == '(') {
							int s = this.offset;
							// function call
							List<ASTNode> args = new LinkedList<ASTNode>();
							parseRestOfTuple(args, reportErrors);
							CallDeclaration callFunc;
							ASTNode[] a = args.toArray(new ASTNode[args.size()]);
							if (word.equals(Keywords.Inherited))
								callFunc = new CallInherited(false, a);
							else if (word.equals(Keywords.SafeInherited))
								callFunc = new CallInherited(true, a);
							else
								callFunc = new CallDeclaration(word, a);
							callFunc.setParmsRegion(s-sectionOffset(), this.offset-1-sectionOffset());
							elm = callFunc;
						} else {
							this.seek(beforeWhitespace);
							// bool
							if (word.equals(Keywords.True))
								elm = new True();
							else if (word.equals(Keywords.False))
								elm = new False();
							else if (word.equals(Keywords.Nil))
								elm = new Nil();
							else
								// variable
								elm = new AccessVar(word);
						}
					}
			}

			// string
			String s;
			if (elm == null && (s = parseString()) != null)
				elm = new StringLiteral(s);

			// array
			if (elm == null)
				elm = parseArrayExpression(reportErrors, prevElm);

			if (elm == null)
				elm = parsePropListExpression(reportErrors, prevElm);

			// ->
			if (elm == null) {
				int fieldOperatorStart = this.offset;
				String memberOperator = parseMemberOperator();
				if (memberOperator != null) {
					int idStart = this.offset;
					int idOffset;
					eatWhitespace();
					idOffset = offset;
					if ((id = parseID()) != null && eatWhitespace() >= 0 && parseStaticFieldOperator())
						idOffset -= fieldOperatorStart;
					else {
						id = null;
						seek(idStart);
						idOffset = 0;
					}
					elm = new MemberOperator(memberOperator.length() == 1, memberOperator.length() == 3, id, idOffset);
				}
			}

			// (<expr>)
			if (elm == null) {
				int c = read();
				if (c == '(') {
					if (prevElm != null) {
						// CallExpr
						List<ASTNode> tupleElms = new LinkedList<ASTNode>();
						parseRestOfTuple(tupleElms, reportErrors);
						elm = new CallExpr(tupleElms.toArray(new ASTNode[tupleElms.size()]));
					} else {
						ASTNode firstExpr = parseExpression(reportErrors);
						if (firstExpr == null)
							firstExpr = whitespace(this.offset, 0);
							// might be disabled
						eatWhitespace();
						c = read();
						if (c == ')')
							elm = new Parenthesized(firstExpr);
						else if (c == ',') {
							// tuple (just for multiple parameters for return)
							List<ASTNode> tupleElms = new LinkedList<ASTNode>();
							tupleElms.add(firstExpr);
							parseRestOfTuple(tupleElms, reportErrors);
							elm = new Tuple(tupleElms.toArray(new ASTNode[0]));
						} else
							tokenExpectedError(")"); //$NON-NLS-1$
					}
				} else
					unread();
			}

			String placeholder;
			if (elm == null && (placeholder = parsePlaceholderString()) != null)
				elm = makePlaceholder(placeholder);

			// check if sequence is valid (CreateObject(BLUB)->localvar is not)
			if (elm != null)
				if (!(prevElm instanceof Placeholder || elm.isValidInSequence(prevElm))) {
					elm = null; // blub blub <- first blub is var; second blub is not part of the sequence -.-
					proper = false;
				} else {
					// add to sequence even if not valid so the quickfixer can separate them
					setRelativeLocation(elm, elmStart, this.offset);
					if (num == _elements.length) {
						ASTNode[] n = new ASTNode[_elements.length+10];
						System.arraycopy(_elements, 0, n, 0, _elements.length);
						_elements = n;
					}
					_elements[num++] = elm;
					prevElm = elm;
				}

			noNewProplist = false;

		} while (elm != null);
		this.seek(noWhitespaceEating);
		ASTNode lastElm;
		if (num == 1) {
			// no need for sequences containing one element
			result = _elements[0];
			lastElm = result;
		} else if (num > 1) {
			result = new Sequence(_elements, num);
			lastElm = _elements[num-1];
		} else {
			result = null;
			lastElm = null;
		}
		if (result != null) {
			proper &= lastElm == null || lastElm.isValidAtEndOfSequence();
			setRelativeLocation(result, sequenceStart, this.offset);
			if (proper) {
				int saved = this.offset;
				eatWhitespace();
				Operator postop = parseOperator();
				if (postop != null && postop.isPostfix()) {
					UnaryOp op = new UnaryOp(postop, UnaryOp.Placement.Postfix, result);
					setRelativeLocation(op, result.start()+sectionOffset(), this.offset);
					return op;
				} else
					// a binary operator following this sequence
					this.seek(saved);
			}
		} else
			this.seek(sequenceParseStart);

		if (useReusable)
			_elementsInUse = false;
		return result;
	}

	protected Placeholder makePlaceholder(String placeholder) throws ParsingException {
		return new Placeholder(placeholder);
	}

	private ASTNode parsePropListExpression(boolean reportErrors, ASTNode prevElm) throws ParsingException {
		ProplistDeclaration proplDec = parsePropListDeclaration(reportErrors);
		if (proplDec != null) {
			ASTNode elm = new PropListExpression(proplDec);
			if (currentFunction != null)
				currentFunction.addOtherDeclaration(proplDec);
			//proplDec.setName(elm.toString());
			return elm;
		}
		return null;
	}

	protected ProplistDeclaration parsePropListDeclaration(boolean reportErrors) throws ParsingException {
		int propListStart = offset;
		int c = read();
		if (c == '{') {
			ProplistDeclaration proplistDeclaration = ProplistDeclaration.newAdHocDeclaration();
			proplistDeclaration.setParent(currentDeclaration() != null ? currentDeclaration() : script);
			Declaration oldDec = currentDeclaration();
			setCurrentDeclaration(proplistDeclaration);
			try {
				boolean properlyClosed = false;
				boolean expectingComma = false;
				while (!reachedEOF()) {
					eatWhitespace();
					c = read();
					if (c == ',') {
						if (!expectingComma)
							error(Problem.UnexpectedToken, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION, ","); //$NON-NLS-1$
						expectingComma = false;
					} else if (c == '}') {
						properlyClosed = true;
						break;
					} else {
						unread();
						int nameStart = this.offset;
						String name;
						if ((name = parseString()) != null || (name = parseIdentifier()) != null) {
							int nameEnd = this.offset;
							eatWhitespace();
							int c_ = read();
							if (c_ != ':' && c_ != '=') {
								unread();
								error(Problem.UnexpectedToken, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION, (char)read());
							}
							eatWhitespace();
							Variable v = new Variable(name, currentFunction != null ? Scope.VAR : Scope.LOCAL);
							v.setLocation(absoluteSourceLocation(nameStart, nameEnd));
							Declaration outerDec = currentDeclaration();
							setCurrentDeclaration(v);
							ASTNode value = null;
							try {
								v.setParent(outerDec);
								value = parseExpression(COMMA_OR_CLOSE_BLOCK, reportErrors);
								if (value == null) {
									error(Problem.ValueExpected, offset-1, offset, Markers.NO_THROW);
									value = placeholderExpression(offset);
								}
								v.setInitializationExpression(value);
								//v.forceType(value.type(this));
							} finally {
								setCurrentDeclaration(outerDec);
							}
							proplistDeclaration.addComponent(v, false);
							expectingComma = true;
						}
						else {
							error(Problem.TokenExpected, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION, Messages.TokenStringOrIdentifier);
							break;
						}
					}
				}
				if (!properlyClosed)
					error(Problem.MissingClosingBracket, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION, "}"); //$NON-NLS-1$
				proplistDeclaration.setLocation(absoluteSourceLocation(propListStart, offset));
				return proplistDeclaration;
			} finally {
				setCurrentDeclaration(oldDec);
			}
		}
		else
			unread();
		return null;
	}

	public final SourceLocation absoluteSourceLocation(int start, int end) {
		return new SourceLocation(start+offsetOfScriptFragment, end+offsetOfScriptFragment);
	}

	public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
		int bodyOffset = sectionOffset();
		return absoluteSourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
	}

	private ASTNode parseArrayExpression(boolean reportErrors, ASTNode prevElm) throws ParsingException {
		ASTNode elm = null;
		int c = read();
		if (c == '[') {
			if (prevElm != null) {
				// array access
				ASTNode arg = parseExpression(reportErrors);
				eatWhitespace();
				int t;
				switch (t = read()) {
				case ':':
					ASTNode arg2 = parseExpression(reportErrors);
					eatWhitespace();
					expect(']');
					elm = new ArraySliceExpression(arg, arg2);
					break;
				case ']':
					elm = new ArrayElementExpression(arg);
					break;
				default:
					error(Problem.UnexpectedToken, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION, (char)t);
				}
			} else {
				// array creation
				Vector<ASTNode> arrayElms = new Vector<ASTNode>(10);
				boolean properlyClosed = false;
				boolean expectingComma = false;
				Loop: while (!reachedEOF()) {
					eatWhitespace();
					c = read();
					switch (c) {
					case ',':
						if (!expectingComma)
							arrayElms.add(null);
						else
							expectingComma = false;
						break;
					case ']':
						properlyClosed = true;
						break Loop;
					case ';':
						properlyClosed = false;
						break Loop;
					default:
						unread();
						ASTNode arrayElement = parseExpression(COMMA_OR_CLOSE_BRACKET, reportErrors);
						if (arrayElement != null) {
							if (expectingComma) {
								ASTNode last = arrayElms.get(arrayElms.size()-1);
								if (last != null)
									arrayElms.set(arrayElms.size()-1, new Unfinished(last));
								expectingComma = false;
							}
							arrayElms.add(arrayElement);
							expectingComma = true;
						}
						else {
							tokenExpectedError("]");
							break Loop;
						}
					}
				}
				if (!properlyClosed)
					error(Problem.MissingClosingBracket, this.offset, this.offset+1, Markers.NO_THROW, "]"); //$NON-NLS-1$
				elm = new ArrayExpression(arrayElms.toArray(new ASTNode[0]));
			}
		} else
			unread();
		return elm;
	}

	private void parseRestOfTuple(List<ASTNode> listToAddElementsTo, boolean reportErrors) throws ParsingException {
		boolean expectingComma = false;
		int lastStart = this.offset;
		Loop: while (!reachedEOF()) {
			eatWhitespace();
			switch (read()) {
			case ')':
				if (!expectingComma && listToAddElementsTo.size() > 0)
					listToAddElementsTo.add(whitespace(lastStart, this.offset-lastStart-1));
				break Loop;
			case ',':
				if (!expectingComma)
					listToAddElementsTo.add(whitespace(lastStart, this.offset-lastStart-1));
				expectingComma = false;
				break;
			default:
				unread();
				if (expectingComma)
					tokenExpectedError(",");
				if (listToAddElementsTo.size() > 100)
					error(Problem.InternalError, this.offset, this.offset, 0, Messages.InternalError_WayTooMuch);
				//	break;
				ASTNode arg = parseTupleElement(reportErrors);
				if (arg == null) {
					error(Problem.ExpressionExpected, this.offset, this.offset+1, Markers.NO_THROW);
					break Loop;
				} else {
					if (arg instanceof SimpleStatement) {
						arg = ((SimpleStatement)arg).expression();
						arg.setParent(null);
					}
					listToAddElementsTo.add(arg);
				}
				expectingComma = true;
			}
			lastStart = this.offset;
		}
	}

	protected ASTNode parseTupleElement(boolean reportErrors) throws ParsingException {
		return parseExpression(reportErrors);
	}

	protected ASTNode parseExpression(char[] delimiters, boolean reportErrors) throws ParsingException {

		final int offset = this.offset;

		final int START = 0;
		final int OPERATOR = 1;
		final int SECONDOPERAND = 2;
		final int DONE = 3;

		ASTNode root = null;
		ASTNode current = null;
		BinaryOp lastOp = null;

		// magical thingie to pass all parameters to inherited
		int exprStart = offset;
		if (parseEllipsis())
			root = new Ellipsis();
		else {
			this.seek(offset);
			eatWhitespace();
			exprStart = this.offset;
			for (int state = START; state != DONE;)
				switch (state) {
				case START:
					root = parseSequence(reportErrors);
					if (root != null) {
						current = root;
						state = OPERATOR;
					} else
						state = DONE;
					break;
				case OPERATOR:
					int operatorStartPos = this.offset;
					eatWhitespace();
					// end of expression?
					int c = read();
					for (int i = 0; i < delimiters.length; i++)
						if (delimiters[i] == c) {
							state = DONE;
							this.seek(operatorStartPos);
							break;
						}

					if (state != DONE) {
						unread(); // unread c
						Operator op = parseOperator();
						if (op != null && op.isBinary()) {
							int priorOfNewOp = op.priority();
							ASTNode newLeftSide = null;
							BinaryOp theOp = null;
							for (ASTNode opFromBottom = current.parent(); opFromBottom instanceof BinaryOp; opFromBottom = opFromBottom.parent()) {
								BinaryOp oneOp = (BinaryOp) opFromBottom;
								if (priorOfNewOp > oneOp.operator().priority() || (priorOfNewOp == oneOp.operator().priority() && op.isRightAssociative())) {
									theOp = oneOp;
									break;
								}
							}
							if (theOp != null) {
								newLeftSide = theOp.rightSide();
								current = lastOp = new BinaryOp(op);
								theOp.setRightSide(current);
							} else {
								newLeftSide = root;
								current = root = lastOp = new BinaryOp(op);
							}
							lastOp.setLeftSide(newLeftSide);
							setRelativeLocation(lastOp, operatorStartPos, this.offset);
							state = SECONDOPERAND;
						} else {
							this.seek(operatorStartPos); // in case there was an operator but not a binary one
							state = DONE;
						}
					}
					break;
				case SECONDOPERAND:
					ASTNode rightSide = parseSequence(reportErrors);
					if (rightSide == null) {
						error(Problem.OperatorNeedsRightSide, offset, offset+1, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION);
						rightSide = placeholderExpression(offset);
					}
					((BinaryOp)current).setRightSide(rightSide);
					lastOp = (BinaryOp)current;
					current = rightSide;
					state = OPERATOR;
					break;
				}
		}
		if (root != null)
			setRelativeLocation(root, exprStart, this.offset);

		return root;

	}

	private ASTNode placeholderExpression(final int offset) {
		ASTNode result = new ASTNode();
		setRelativeLocation(result, offset, offset+1);
		return result;
	}

	/**
	 * Convert a region relative to the body offset of the current function to a script-absolute region.
	 * @param flags
	 * @param region The region to convert
	 * @return The relative region or the passed region, if there is no current function.
	 */
	public IRegion convertRelativeRegionToAbsolute(int flags, IRegion region) {
		int offset = sectionOffset();
		if (offset == 0 || (flags & Markers.ABSOLUTE_MARKER_LOCATION) == 0)
			return region;
		else
			return new Region(offset+region.getOffset(), region.getLength());
	}

	protected ASTNode parseExpression(boolean reportErrors) throws ParsingException {
		return parseExpression(SEMICOLON_DELIMITER, reportErrors);
	}

	protected ASTNode parseExpression() throws ParsingException {
		return parseExpression(true);
	}

	/**
	 * Parse a string literal and store it in the {@link FunctionContext#parsedString} field.
	 * @return Whether parsing was successful.
	 * @throws ParsingException
	 */
	private String parseString() throws ParsingException {
		int quotes = read();
		if (quotes != '"') {
			unread();
			return null;
		}
		int start = offset;
		boolean escaped = false;
		boolean properEnd = false;
		Loop: do {
			int c = read();
			switch (c) {
			case -1:
				error(Problem.StringNotClosed, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION);
				break Loop;
			case '"':
				if (!escaped) {
					properEnd = true;
					break Loop;
				}
				break;
			case '\n': case '\r':
				error(Problem.StringNotClosed, this.offset-1, this.offset, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION);
				break Loop;
			case '\\':
				escaped = !escaped;
				continue Loop;
			}
			escaped = false;
		} while (true);
		return readStringAt(start, offset - (properEnd?1:0));
	}

	/**
	 * Parse an identifier and store it in the {@link FunctionContext#parsedString} field.
	 * @return Whether parsing the identifier was successful.
	 * @throws ParsingException
	 */
	private String parseIdentifier() throws ParsingException {
		String word = readIdent();
		if (word != null && word.length() > 0)
			return word;
		else
			return null;
	}

	/**
	 * Parse a $...$ placeholder.
	 * @return Whether there was a placeholder at the current offset.
	 * @throws ParsingException
	 */
	private String parsePlaceholderString() throws ParsingException {
		if (read() != '$') {
			unread();
			return null;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(this.readString(1));
			builder.append(this.readStringUntil('$'));
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		expect('$');
		return builder.toString();
	}

	/**
	 * Options specifying how a statement is to be parsed.
	 * @author madeen
	 *
	 */
	public enum ParseStatementOption {
		/**
		 * The statement is expected to be an initialization statement
		 */
		InitializationStatement,
		/**
		 * The statement could be a function desc. Without this option, a function description is likely to be interpreted as array expression.
		 */
		ExpectFuncDesc;

		/**
		 * Empty ParseStatementOption set.
		 */
		public static final EnumSet<ParseStatementOption> NoOptions = EnumSet.noneOf(ParseStatementOption.class);
	}

	/**
	 * Parse a statement without specifying options.
	 * @return The parsed statement or null if parsing was unsuccessful.
	 * @throws ParsingException
	 */
	protected Statement parseStatement() throws ParsingException {
		return parseStatement(ParseStatementOption.NoOptions);
	}

	private Statement parseStatementWithPrependedComments() throws ParsingException {
		// parse comments and attach them to the statement so the comments won't be removed by code reformatting
		List<Comment> prependedComments = collectComments();
		Statement s = parseStatement();
		if (s != null && prependedComments != null)
			s.addAttachments(prependedComments);
		return s;
	}

	/**
	 * Parse a statement.
	 * @param options Options on how to parse the statement
	 * @return The parsed statement or null if parsing was unsuccessful.
	 * @throws ParsingException
	 */
	private Statement parseStatement(EnumSet<ParseStatementOption> options) throws ParsingException {
		int emptyLines = -1;
		int delim;
		for (; (delim = peek()) != -1 && BufferedScanner.isWhiteSpace((char) delim); read()) {
			char c = (char) delim;
			if (c == '\n')
				emptyLines++;
		}

		//eatWhitespace();
		int start = this.offset;
		Statement result;
		Scope scope;

		// comment statement oO
		result = parseComment();

		if (result == null) {
			String readWord;
			int rewind = this.offset;
			// new oldstyle-func begun
			if (currentFunction != null && currentFunction.isOldStyle() && FunctionHeader.parse(this, true) != null) {
				this.seek(rewind);
				return null;
			}
			else if ((readWord = readIdent()) == null || readWord.length() == 0) {
				int read = read();
				if (read == '{' && !options.contains(ParseStatementOption.InitializationStatement)) {
					List<ASTNode> subStatements = new LinkedList<>();
					parseStatementBlock(start, subStatements, ParseStatementOption.NoOptions, false);
					result = new Block(subStatements);
				}
				else if (read == ';')
					result = new EmptyStatement();
				else if (read == '[' && options.contains(ParseStatementOption.ExpectFuncDesc)) {
					String funcDesc = this.readStringUntil(']');
					read();
					result = new FunctionDescription(funcDesc);
				}
				else {
					unread();
					ASTNode expression = parseExpression();
					if (expression != null) {
						result = new SimpleStatement(expression);
						if (!options.contains(ParseStatementOption.InitializationStatement))
							result = needsSemicolon(result);
					}
					else
						result = null;
				}
			}
			else if ((scope = Scope.makeScope(readWord)) != null) {
				List<VarInitialization> initializations = parseVariableDeclaration(true, false, scope, null);
				if (initializations != null) {
					result = new VarDeclarationStatement(initializations, initializations.get(0).variable.scope());
					if (!options.contains(ParseStatementOption.InitializationStatement)) {
						rewind = this.offset;
						eatWhitespace();
						if (read() != ';') {
							seek(rewind);
							result = new Unfinished(result);
						}
					}
				}
			}
			else if (!options.contains(ParseStatementOption.InitializationStatement))
				result = parseKeyword(readWord);
			else
				result = null;
		}

		// just an expression that needs to be wrapped as a statement
		if (result == null) {
			this.seek(start);
			ASTNode expression = parseExpression();
			int afterExpression = this.offset;
			if (expression != null) {
				result = new SimpleStatement(expression);
				if (!options.contains(ParseStatementOption.InitializationStatement)) {
					int beforeWhitespace = this.offset;
					eatWhitespace();
					if (read() != ';') {
						result = new Unfinished(result);
						this.seek(beforeWhitespace);
					}
				} else
					this.seek(afterExpression);
			}
			else
				result = null;
		}

		if (result != null) {
			// inline comment attached to expression so code reformatting does not mess up the user's code too much
			Comment c = commentImmediatelyFollowing();
			if (c != null)
				result.setInlineComment(c);
			if (emptyLines > 0)
				result.addAttachment(new Statement.EmptyLinesAttachment(emptyLines));

			setRelativeLocation(result, start, this.offset);
		}
		return result;
	}

	/**
	 * Parse a statement block and add parsed statements to the passed list.
	 * @param start Start of the block. Used for error reporting if the end of the block cannot be found.
	 * @param endOfFunc Position after which to stop the statement parsing loop, even if the regular block end hasn't been found.
	 * @param statements List the parsed statements will be added to.
	 * @param options Option enum set specifying how the statements are to be parsed
	 * @param flavour Whether parsing statements or only expressions
	 * @throws ParsingException
	 */
	protected void parseStatementBlock(int start, List<ASTNode> statements, EnumSet<ParseStatementOption> options, boolean oldStyle) throws ParsingException {
		boolean done = false;
		int garbageStart = -1;
		while (!reachedEOF()) {
			int potentialGarbageEnd = offset;
			//eatWhitespace();
			Statement statement = parseStatement(options);
			if (statement == null) {
				done = oldStyle || peek() == '}';
				if (done)
					break;
				if (garbageStart == -1)
					garbageStart = offset;
				offset++;
				continue;
			} else // garbage recognized before statement: Create a special garbage statement that will report itself
				if (garbageStart != -1)
					garbageStart = maybeAddGarbageStatement(statements, garbageStart, potentialGarbageEnd);
			statements.add(statement);
			boolean statementIsComment = statement instanceof Comment;
			// after first 'real' statement don't expect function description anymore
			if (!statementIsComment)
				options.remove(ParseStatementOption.ExpectFuncDesc);
		}
		if (garbageStart != -1)
			// contains only garbage ... still add
			maybeAddGarbageStatement(statements, garbageStart, offset);
		if (!done) {
			//if (this.offset < endOfFunc)
			//error(ParserErrorCode.BlockNotClosed, start, start+1, NO_THROW);
		} else if (!oldStyle)
			read(); // should be }
	}

	private int maybeAddGarbageStatement(List<ASTNode> statements, int garbageStart, int potentialGarbageEnd) throws ParsingException {
		String garbageString = new String(buffer, garbageStart, Math.min(potentialGarbageEnd, buffer.length-garbageStart));
		garbageString = modifyGarbage(garbageString);
		if (garbageString != null && garbageString.length() > 0) {
			GarbageStatement garbage = new GarbageStatement(garbageString, garbageStart-sectionOffset());
			garbageStart = -1;
			statements.add(garbage);
		}
		return garbageStart;
	}

	/**
	 * Parse a comment following some expression. Will return null if there is a line break in between the expression and the comment.
	 * @return The parsed comment.
	 */
	private Comment commentImmediatelyFollowing() {
		int daring = this.offset;
		Comment c = null;
		for (int r = read(); r != -1 && (r == '/' || BufferedScanner.isWhiteSpaceButNotLineDelimiterChar((char) r)); r = read())
			if (r == '/') {
				unread();
				c = parseComment();
				break;
			}
		if (c != null)
			return c;
		else {
			this.seek(daring);
			return null;
		}
	}

	/**
	 * Expect a certain character.
	 * @param expected The character expected
	 * @throws ParsingException
	 */
	private void expect(char expected) throws ParsingException {
		if (read() != expected) {
			unread();
			tokenExpectedError(String.valueOf(expected));
		}
	}

	/**
	 * Expect a certain identifier at the current offset.
	 * @param expected The identifier expected
	 * @throws ParsingException
	 */
	private void expect(String expected) throws ParsingException {
		String r = readIdent();
		if (r == null || !r.equals(expected))
			tokenExpectedError(expected);
	}

	private boolean parseSemicolonOrReturnFalse() {
		int old = offset;
		eatWhitespace();
		if (read() == ';')
			return true;
		else {
			seek(old);
			return false;
		}
	}

	private Statement needsSemicolon(Statement statement) {
		if (!parseSemicolonOrReturnFalse())
			statement = new Unfinished(statement);
		return statement;
	}

	/**
	 * Parse a statement that is initiated with a keyword. This includes for/while/do while loops,
	 * loop control flow statements (break/continue) and return.
	 * @param keyWord The keyword that has already been parsed and decides on the kind of statement to parse.
	 * @return The parsed KeywordStatement or null if the keyword was not recognized
	 * @throws ParsingException
	 */
	private Statement parseKeyword(String keyWord) throws ParsingException {
		Statement result = null;
		if (keyWord.equals(Keywords.If))
			result = parseIf();
		else if (keyWord.equals(Keywords.While))
			result = parseWhile();
		else if (keyWord.equals(Keywords.Do))
			result = parseDoWhile();
		else if (keyWord.equals(Keywords.For))
			result = parseFor();
		else if (keyWord.equals(Keywords.Continue))
			result = needsSemicolon(new ContinueStatement());
		else if (keyWord.equals(Keywords.Break))
			result = needsSemicolon(new BreakStatement());
		else if (keyWord.equals(Keywords.Return))
			result = parseReturn();
		else
			result = null;

		return result;
	}

	/**
	 * Parse a return statement.
	 * @return The parsed return statement
	 * @throws ParsingException
	 */
	private Statement parseReturn() throws ParsingException {
		Statement result;
		eatWhitespace();
		ASTNode returnExpr;
		if (peek() == ';')
			returnExpr = null;
		else {
			returnExpr = parseExpression();
			if (returnExpr == null)
				error(Problem.ValueExpected, this.offset, this.offset+1, Markers.NO_THROW);
		}
		result = new ReturnStatement(returnExpr);
		if (!parseSemicolonOrReturnFalse())
			result = new Unfinished(result);
		return result;
	}

	/**
	 * Parse a do {...} while statement.
	 * @return The parsed DoWhileStatement
	 * @throws ParsingException
	 */
	private DoWhileStatement parseDoWhile() throws ParsingException {
		Statement block = parseStatement();
		eatWhitespace();
		expect(Keywords.While);
		eatWhitespace();
		expect('(');
		eatWhitespace();
		ASTNode cond = parseExpression();
		eatWhitespace();
		expect(')');
		//expect(';');
		return new DoWhileStatement(cond, block);
	}

	/**
	 * Parse a for statement. The result is either a {@link ForStatement} or an {@link IterateArrayStatement}.
	 * @return The parsed for loop.
	 * @throws ParsingException
	 */
	private KeywordStatement parseFor() throws ParsingException {
		int savedOffset;
		KeywordStatement result;

		eatWhitespace();
		expect('(');
		eatWhitespace();

		// initialization
		savedOffset = this.offset;
		Statement initialization = null, body;
		ASTNode arrayExpr, condition, increment;
		String w = null;
		if (read() == ';') {
			// any of the for statements is optional
			//initialization = null;
		} else {
			unread();
			// special treatment for case for (e in a) -> implicit declaration of e
			int pos = this.offset;
			String varName = readIdent();
			if (!(varName.equals("") || varName.equals(Keywords.VarNamed))) { //$NON-NLS-1$
				eatWhitespace();
				w = readIdent();
				if (w.equals(Keywords.In)) {
					// too much manual setting of stuff
					AccessVar accessVar = new AccessVar(varName);
					setRelativeLocation(accessVar, pos, pos+varName.length());
					initialization = new SimpleStatement(accessVar);
					setRelativeLocation(initialization, pos, pos+varName.length());
				} else
					w = null;
			}
			if (w == null) {
				// regularly parse initialization statement
				seek(pos);
				initialization = parseStatement(EnumSet.of(ParseStatementOption.InitializationStatement));
				if (initialization == null)
					error(Problem.ExpectedCode, this.offset, this.offset+1, Markers.NO_THROW);
			}
		}

		if (w == null) {
			// determine loop type
			eatWhitespace();
			savedOffset = this.offset;
			if (initialization != null)
				if (read() == ';')
					// initialization finished regularly with ';', so no for (... in ...) loop
					savedOffset = this.offset;
				else {
					unread();
					w = readIdent();
				}
		}
		LoopType loopType;
		if (w != null && w.equals(Keywords.In)) {
			// it's a for (x in array) loop!
			loopType = LoopType.IterateArray;
			eatWhitespace();
			arrayExpr = parseExpression();
			if (arrayExpr == null)
				error(Problem.ExpressionExpected, savedOffset, this.offset, Markers.ABSOLUTE_MARKER_LOCATION);
			condition = null;
			increment = null;
		} else {
			loopType = LoopType.For;
			this.seek(savedOffset); // if a word !equaling("in") was read
			eatWhitespace();
			if (read() == ';') {
				// any " optional "
				unread(); // is expected
				condition = null;
			} else {
				unread();
				condition = parseExpression();
				if (condition == null)
					error(Problem.ConditionExpected, savedOffset, this.offset, Markers.ABSOLUTE_MARKER_LOCATION);
			}
			eatWhitespace();
			savedOffset = this.offset;
			expect(';');
			eatWhitespace();
			savedOffset = this.offset;
			if (read() == ')') {
				// " optional "
				unread(); // is expected
				increment = null;
			} else {
				unread();
				increment = parseExpression();
				if (increment == null)
					error(Problem.ExpressionExpected, savedOffset, this.offset+1, Markers.NO_THROW);
			}
			arrayExpr = null;
		}
		eatWhitespace();
		expect(')');
		eatWhitespace();
		savedOffset = this.offset;
		body = parseStatement();
		if (body == null)
			error(Problem.StatementExpected, savedOffset, savedOffset+4, Markers.NO_THROW);
		switch (loopType) {
		case For:
			result = new ForStatement(initialization, condition, increment, body);
			break;
		case IterateArray:
			result = new IterateArrayStatement(initialization, arrayExpr, body);
			break;
		default:
			result = null;
		}
		return result;
	}

	/**
	 * Parse a WhileStatement.
	 * @return The parsed WhileStatement
	 * @throws ParsingException
	 */
	private WhileStatement parseWhile() throws ParsingException {
		int offset;
		WhileStatement result;
		eatWhitespace();
		expect('(');
		eatWhitespace();
		ASTNode condition = parseExpression();
		if (condition == null)
			condition = whitespace(this.offset, 0); // while () is valid
		eatWhitespace();
		expect(')');
		eatWhitespace();
		offset = this.offset;
		Statement body = parseStatement();
		if (body == null)
			error(Problem.StatementExpected, offset, offset+4, Markers.NO_THROW);
		result = new WhileStatement(condition, body);
		return result;
	}

	/**
	 * Parse an IfStatement.
	 * @return The IfStatement
	 * @throws ParsingException
	 */
	private IfStatement parseIf() throws ParsingException {
		IfStatement result;
		eatWhitespace();
		expect('(');
		eatWhitespace();
		ASTNode condition = parseExpression();
		if (condition == null)
			condition = whitespace(this.offset, 0); // if () is valid
		eatWhitespace();
		expect(')');
		int offsetBeforeWhitespace = this.offset;
		Statement ifStatement = withMissingFallback(offsetBeforeWhitespace, parseStatementWithPrependedComments());
		int beforeElse = this.offset;
		eatWhitespace();
		int o = this.offset;
		String nextWord = readIdent();
		Statement elseStatement;
		if (nextWord != null && nextWord.equals(Keywords.Else)) {
			elseStatement = parseStatementWithPrependedComments();
			if (elseStatement == null)
				error(Problem.StatementExpected, o, o+Keywords.Else.length(), Markers.NO_THROW);
		}
		else {
			this.seek(beforeElse); // don't eat comments and stuff after if (...) ...;
			elseStatement = null;
		}
		result = new IfStatement(condition, ifStatement, elseStatement);
		return result;
	}

	private Statement withMissingFallback(int offsetWhereExpected, Statement statement) throws ParsingException {
		return statement != null
			? statement
				: new MissingStatement(offsetWhereExpected-sectionOffset());
	}

	/**
	 * Parse an id. On successful parsing, the parsed will be stored in the parsedID field.
	 * @return Whether parsing the id was successful. If false, one can be assured that parsedID will be null.
	 * @throws ParsingException
	 */
	private ID parseID() throws ParsingException {
		ID id;
		if (offset < size && (id = specialEngineRules != null ? specialEngineRules.parseId(this) : null) != null)
			return id;
		else
			return null;
	}

	/**
	 * Parse a parameter at the current offset.
	 * @param function The function to create the parameter in
	 * @return Whether parsing the parameter was successful
	 * @throws ParsingException
	 */
	private Variable parseParameter(Function function) throws ParsingException {

		int backtrack = this.offset;
		eatWhitespace();
		if (script == engine && parseEllipsis())
			return addVarParmsParm(function);
		if (peek() == ')') {
			seek(backtrack);
			return null;
		}

		int typeStart = this.offset;
		IType type = parseTypeAnnotation(true, false);
		int typeEnd = this.offset;
		eatWhitespace();
		int nameStart = this.offset;
		String parmName = readIdent();
		if (parmName.length() == 0) {
			type = null;
			seek(nameStart = backtrack);
			eatWhitespace();
			int ta = this.offset;
			parmName = readIdent();
			if (parmName.length() == 0)
				return null;
			parsedTypeAnnotation = placeholderTypeAnnotationIfMigrating(ta);
		}
		switch (typing) {
		case Static:
			if (type == null)
				typeRequiredAt(typeStart);
			break;
		case Dynamic:
			if (type != null)
				error(Problem.NotSupported, typeStart, typeEnd, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, readStringAt(typeStart, typeEnd), engine.name() + " with no type annotations");
			break;
		default:
			break;
		}
		Variable var = new Variable(null, Scope.PARAMETER);
		if (parsedTypeAnnotation != null)
			parsedTypeAnnotation.setTarget(var);
		if (type != null) {
			if ((type == PrimitiveType.REFERENCE || type instanceof ReferenceType) && !engine.supportsPrimitiveType(PrimitiveType.REFERENCE))
				error(Problem.PrimitiveTypeNotSupported, offset-1, offset, Markers.NO_THROW, PrimitiveType.REFERENCE.typeName(true), script.engine().name());
			var.forceType(type, true);
		}
		var.setName(parmName);
		var.setLocation(new SourceLocation(nameStart, this.offset));
		var.setParent(function);
		if (parsedTypeAnnotation != null)
			parsedTypeAnnotation.setTarget(var);
		function.addParameter(var);
		eatWhitespace();
		return var;
	}

	/**
	 * Delete declarations inside the script container assigned to the parser and remove markers.
	 */
	public void clean() {
		try {
			if (scriptFile != null) {
				scriptFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
				scriptFile.deleteMarkers(IMarker.TASK, true, IResource.DEPTH_ONE);
			}
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		script.clearDeclarations();
	}

	/**
	 * Set by derived parsers that operate on just a substring of the whole script.
	 * Used for setting the right location for variables that are created while parsing the body of a function
	 * @return Offset of the script fragment this parser processes in the complete script
	 */
	protected int offsetOfScriptFragment;

	/**
	 * Subtracted from the location of ExprElms created so their location will be relative to the body of the function they are contained in.
	 */
	public int sectionOffset() {
		Function f = currentFunction;
		if (f != null && f.bodyLocation() != null)
			return f.bodyLocation().start();
		else
			return 0;
	}

	/**
	 * Return the substring of the script that holds the code for function
	 * @param function the function to return the source code of
	 * @return source code
	 */
	protected String functionSource(Function function) {
		if (function == null)
			return null;
		else
			return new String(buffer, function.bodyLocation().start(), function.bodyLocation().getLength());
	}

	/**
	 * Modify garbage string based on special considerations
	 * @param garbage The expression string recognized as garbage
	 * @return Actual garbage string to be wrapped in a GarbageStatement. Null if no GarbageStatement should be created
	 */
	protected String modifyGarbage(String garbage) {
		return garbage; // normal parser accepts teh garbage
	}

	/**
	 * Create a new expression to signify some non-expression at a given location.
	 * @param start The start of the location to mark as 'missing an expression'
	 * @param length The length of the null-expression
	 * @return The constructed whitespace expression
	 */
	public final ASTNode whitespace(int start, int length) {
		ASTNode result = new Whitespace();
		setRelativeLocation(result, start, start+length);
		return result;
	}

	public static ASTNode parse(final String source, Engine engine) throws ParsingException {
		return ScriptsHelper.parseStandaloneNode(source, null, null, null, engine, null);
	}

	public static ASTNode matchingExpr(final String statementText, Engine engine) {
		try {
			return ASTNodeMatcher.matchingExpr(parse(statementText, engine));
		} catch (ParsingException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Instruct this parser to parse a standalone-statement in some newly passed string. Shouldn't be called when expecting the parser to continue keeping
	 * track of its preceding state, since buffer and scanner offset will be reset.
	 * @param statementText The statement text to parse
	 * @param context Function context. If null, some temporary context will be created internally.
	 * @param visitor Script parser visitor
	 * @return The {@link Statement}, or a {@link BunchOfStatements} if more than one statement could be parsed from statementText. Possibly null, if erroneous text was passed.
	 * @throws ParsingException
	 */
	public <T> ASTNode parseStandaloneStatement(final String statementText, Function function) throws ParsingException {
		init(statementText);
		setCurrentFunction(function);
		markers().enableError(Problem.NotFinished, false);

		List<ASTNode> statements = new LinkedList<ASTNode>();
		Statement statement;
		do {
			statement = parseStatement();
			if (statement != null)
				statements.add(statement);
			else
				break;
		} while (true);
		//reportProblemsOf(statements, true);
		return statements.size() == 1 ? statements.get(0) : new BunchOfStatements(statements);
	}

	@Override
	public IFile file() { return scriptFile; }
	@Override
	public Declaration container() { return script(); }
	@Override
	public int fragmentOffset() { return offsetOfScriptFragment; }
}
