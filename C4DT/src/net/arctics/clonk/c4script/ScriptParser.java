package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.MalformedDeclaration;
import net.arctics.clonk.ast.Placeholder;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Directive.DirectiveType;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.c4script.ast.ArrayExpression;
import net.arctics.clonk.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.BreakStatement;
import net.arctics.clonk.c4script.ast.BunchOfStatements;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.CallExpr;
import net.arctics.clonk.c4script.ast.CallInherited;
import net.arctics.clonk.c4script.ast.CastExpression;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.ContinueStatement;
import net.arctics.clonk.c4script.ast.DoWhileStatement;
import net.arctics.clonk.c4script.ast.Ellipsis;
import net.arctics.clonk.c4script.ast.EmptyStatement;
import net.arctics.clonk.c4script.ast.False;
import net.arctics.clonk.c4script.ast.ForStatement;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.FunctionDescription;
import net.arctics.clonk.c4script.ast.GarbageStatement;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IfStatement;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.c4script.ast.KeywordStatement;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.MissingStatement;
import net.arctics.clonk.c4script.ast.NewProplist;
import net.arctics.clonk.c4script.ast.Nil;
import net.arctics.clonk.c4script.ast.NumberLiteral;
import net.arctics.clonk.c4script.ast.Parenthesized;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.ReturnStatement;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.True;
import net.arctics.clonk.c4script.ast.Tuple;
import net.arctics.clonk.c4script.ast.UnaryOp;
import net.arctics.clonk.c4script.ast.Unfinished;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.c4script.ast.WhileStatement;
import net.arctics.clonk.c4script.ast.Whitespace;
import net.arctics.clonk.c4script.effect.EffectFunction;
import net.arctics.clonk.c4script.typing.ArrayType;
import net.arctics.clonk.c4script.typing.ErroneousType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.ReferenceType;
import net.arctics.clonk.c4script.typing.TypeAnnotation;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.IVariableFactory;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.CStyleScanner;
import net.arctics.clonk.parser.Markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A C4Script parser. Parses declarations in a script and stores it in a {@link Script} object (sold separately). Also parses syntax trees of functions.
 * Those can be used for various purposes, including
 * checking correctness (aiming to detect all kinds of errors like undeclared identifiers, supplying values of wrong type to functions etc.), converting old
 * c4script code to #strict-compliant "new-style" code and forming the base of navigation operations like "Find Declaration", "Find References" etc.
 */
public class ScriptParser extends CStyleScanner implements IASTPositionProvider, IVariableFactory, Runnable {

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
	private Engine engine;

	public final SpecialEngineRules specialEngineRules() {return specialEngineRules;}
	public final Typing typing() { return typing; }
	public Script script() { return script; }

	/**
	 * Sets the current function. There should be a good reason to call this.
	 * @param func
	 */
	protected void setCurrentFunction(Function func) {
		if (func != currentFunction) {
			currentFunction = func;
			this.currentDeclaration = func;
		}
	}

	/**
	 * Returns the first variable in the parent chain of currentDeclaration
	 * @return
	 */
	public Variable currentVariable() {
		return currentDeclaration() != null ? currentDeclaration().parent(Variable.class) : null;
	}

	/**
	 * Returns the declaration that is currently being parsed.
	 * @return
	 */
	public Declaration currentDeclaration() {
		return currentDeclaration;
	}

	/**
	 * Returns the script object as an object if it is one or null if it is not.
	 * @return The script object as  C4Object
	 */
	public Definition definition() { return as(script, Definition.class); }

	/**
	 * Creates a script parser. The script is read from the file attached to the script (queried through getScriptFile()).
	 */
	public ScriptParser(Script script) {
		this(script.source(), script, null);
		initialize();
	}

	private List<TypeAnnotation> typeAnnotations;

	public List<TypeAnnotation> typeAnnotations() { return typeAnnotations; }

	/**
	 * Initialize some state fields. Needs to be called before actual parsing takes place.
	 */
	protected void initialize() {
		if (script != null) {
			engine = script.engine();
			specialEngineRules = engine != null ? script.engine().specialRules() : null;
			typing = script.typing();
			migrationTyping = null;
			if (script.index() instanceof ProjectIndex) {
				final ProjectIndex projIndex = (ProjectIndex) script.index();
				final ClonkProjectNature nature = projIndex.nature();
				if (nature != null)
					migrationTyping = nature.settings().migrationTyping;
			}
			if (typing.allowsNonParameterAnnotations() || migrationTyping != null)
				typeAnnotations = new ArrayList<TypeAnnotation>();
		}
	}

	/**
	 * Creates a C4Script parser that parses a file within the project.
	 * Results are stored in <code>object</code>
	 * @param scriptFile
	 * @param obj
	 */
	public ScriptParser(Object source, Script script, IFile scriptFile) {
		super(source);
		this.scriptFile = defaulting(scriptFile, as(source, IFile.class));
		this.script = script;
		initialize();
	}

	/**
	 * Perform a full parsing (that includes cleaning up the current state of the script container, parsing declarations and parsing function code).
	 * @throws ProblemException
	 */
	public void parse() throws ProblemException {
		script().setTypeAnnotations(null);
		clear();
		parseDeclarations();
		validate();
		filterLocalTypeAnnotations();
		script().setTypeAnnotations(typeAnnotations);
	}
	private void filterLocalTypeAnnotations() {
		if (typeAnnotations == null)
			return;
		for (final Iterator<TypeAnnotation> it = typeAnnotations.iterator(); it.hasNext();) {
			final TypeAnnotation annot = it.next();
			if (annot.parent() != script() && annot.parent(Script.class) == script())
				it.remove();
		}
	}

	@Override
	public void run() {
		try {
			parse();
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
	}

	public String parseTokenAndReturnAsString() throws ProblemException{
		String s;
		Number number;
		ID id;
		if ((id = parseID()) != null)
			return id.stringValue();
		if ((s = parseString()) != null)
			return '"'+s+'"';
		if ((s = parseIdentifier()) != null)
			return s;
		if ((number = parseNumber()) != null)
			return String.valueOf(number);
		else
			return String.valueOf((char)read());
	}

	public ASTNode parseNode() throws ProblemException {
		ASTNode node = parseDeclaration();
		if (node == null)
			node = parseStatement();
		return node;
	}

	/**
	 * Parse declarations but not function code. Before calling this it should be ensured that the script is {@link #clear()}-ed to avoid duplicates.
	 */
	public void parseDeclarations() {
		script().setTypeAnnotations(null);
		this.seek(0);
		try {
			parseInitialSourceComment();
			eatWhitespace();
			while (!reachedEOF()) {
				if (parseDeclaration() == null)
					readUnexpectedBlock();
				eatWhitespace();
			}
		}
		catch (final ProblemException e) { return; }
		finally {
			script().setTypeAnnotations(typeAnnotations);
		}
	}
	private void readUnexpectedBlock() throws ProblemException {
		eatWhitespace();
		if (!reachedEOF()) {
			final int start = this.offset;
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
				final String tokenText = parseTokenAndReturnAsString();
				error(Problem.CommaOrSemicolonExpected, this.offset-1, this.offset, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, tokenText);
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
	 * @throws ProblemException
	 */
	public void validate() throws ProblemException {
		this.currentDeclaration = null;
		for (final Directive directive : script.directives())
			directive.validate(this);
		distillAdditionalInformation();
	}

	/**
	 * OC: Get information out of the script that was previously to be found in additional files (like the name of the {@link Definition}). Specifically, parse the Definition() function.
	 */
	public void distillAdditionalInformation() {
		if (script instanceof Definition) {
			final Definition obj = (Definition) script;
			obj.chooseLocalizedName(); // ClonkRage Names.txt
			// local Name = "Exploder";
			final Variable nameLocal = script.findLocalVariable("Name", false); //$NON-NLS-1$
			if (nameLocal != null) {
				final ASTNode expr = nameLocal.initializationExpression();
				if (expr != null)
					obj.setName(expr.evaluateStatic(nameLocal.initializationExpression().parent(Function.class)).toString());
			}
		}
	}

	/**
	 * Parse code of one single function. {@link #validate()} calls this for all functions in the script.
	 * @param function The function to be parsed
	 * @throws ProblemException
	 */
	private void parseFunctionBody(Function function) throws ProblemException {
		try {
			final int bodyStart = this.offset;
			if (!function.staticallyTyped())
				function.assignType(PrimitiveType.UNKNOWN, false);

			// reset local vars
			function.resetLocalVarTypes();
			// parse code block
			final EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
			final List<ASTNode> statements = new LinkedList<ASTNode>();
			function.setBodyLocation(new SourceLocation(bodyStart, Integer.MAX_VALUE));
			parseStatementBlock(offset, statements, options, function.isOldStyle());
			final FunctionBody bunch = new FunctionBody(function, statements);
			if (function.isOldStyle() && statements.size() > 0)
				function.bodyLocation().setEnd(statements.get(statements.size() - 1).end() + sectionOffset());
			else
				function.setBodyLocation(new SourceLocation(bodyStart, this.offset-1));
			function.storeBody(bunch, functionSource(function));
		} catch (final Exception e) {
			function.storeBody(new FunctionBody(function), functionSource(function));
		}
	}

	private Variable addVarParmsParm(Function func) {
		final Variable v = new Variable("...", PrimitiveType.ANY); //$NON-NLS-1$
		v.setParent(func);
		v.setScope(Variable.Scope.PARAMETER);
		func.addParameter(v);
		return v;
	}

	@Override
	protected Comment parseComment() {
		final int offset = this.offset;
		final Comment c = super.parseComment();
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
		final int bodyOffset = sectionOffset();
		expr.setLocation(start-bodyOffset, end-bodyOffset);
	}

	/**
	 * Parses the declaration at the current this position.
	 * @return whether parsing was successful
	 * @throws ProblemException
	 */
	protected Declaration parseDeclaration() throws ProblemException {
		final int rewind = this.offset;

		final Declaration directive = parseDirective();
		if (directive != null)
			return directive;

		final FunctionHeader functionHeader = FunctionHeader.parse(this, true);
		if (functionHeader != null) {
			final Function f = parseFunctionDeclaration(functionHeader);
			if (f != null)
				return f;
		}

		final String word = readIdent();
		final Scope scope = word != null ? Scope.makeScope(word) : null;
		if (scope != null) {
			final List<VarInitialization> vars = parseVariableDeclaration(true, scope, collectPrecedingComment(rewind));
			if (vars != null) {
				for (final VarInitialization vi : vars)
					if (vi.expression != null)
						synthesizeInitializationFunction(vi);
				return new Variables(vars);
			}
		}

		this.seek(rewind);
		return null;
	}

	private Declaration parseDirective() throws ProblemException {
		final int s = this.offset;
		if (read() != '#') {
			unread();
			return null;
		}
		Declaration result;
		// directive
		final String directiveName = parseIdentifier();
		final DirectiveType type = DirectiveType.makeType(directiveName);
		if (type == null) {
			warning(Problem.UnknownDirective, s, s + 1 + (directiveName != null ? directiveName.length() : 0), 0, directiveName);
			this.moveUntil(BufferedScanner.NEWLINE_CHARS);
			result = new MalformedDeclaration(readStringAt(s, this.offset));
			result.setLocation(s, this.offset);
		}
		else {
			if (type == DirectiveType.STRICT && !engine.settings().supportsStrictDirective)
				error(Problem.NotSupported, s, this.offset, Markers.NO_THROW, "#"+DirectiveType.STRICT, engine.name());
			eat(WHITESPACE_WITHOUT_NEWLINE_CHARS);
			final int cs = offset;
			final String content = parseDirectiveParms();
			final Directive directive = new Directive(type, content, cs-s);
			directive.setLocation(absoluteSourceLocation(s, this.offset));
			script.addDeclaration(directive);
			result = directive;
		}
		return result;
	}

	protected InitializationFunction synthesizeInitializationFunction(VarInitialization vi) {
		final InitializationFunction synth = new InitializationFunction(vi.variable);
		final SourceLocation expressionLocation = absoluteSourceLocationFromExpr(vi.expression);
		final int es = expressionLocation.start();
		vi.expression.traverse(new IASTVisitor<Void>() {
			@Override
			public TraversalContinuation visitNode(ASTNode node, Void parser) {
				node.setLocation(node.start()-es, node.end()-es);
				final CallDeclaration cd = as(node, CallDeclaration.class);
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

	private String parseDirectiveParms() throws ProblemException {
		switch (peek()) {
		case '\n': case '\r':
			return null;
		default:
			return parseTokenAndReturnAsString();
		}
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
		public static FunctionHeader parse(ScriptParser parser, boolean allowOldStyle) throws ProblemException {
			final Comment desc = parser.collectPrecedingComment(parser.offset);
			final int initialOffset = parser.offset;
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
						final int bt = parser.offset;
						if (parser.typing != Typing.DYNAMIC) {
							typeAnnotation = parser.parseTypeAnnotation(true, true);
							returnType = typeAnnotation != null ? typeAnnotation.type() : null;
						}
						switch (parser.typing) {
						case STATIC:
							if (returnType == null) {
								returnType = PrimitiveType.ANY;
								parser.seek(bt);
								parser.error(Problem.TypeExpected, bt,bt+1, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION);
							}
							break;
						case INFERRED:
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
						final int backtrack = parser.offset;
						parser.eatWhitespace();
						final boolean isProperLabel = parser.read() == ':' && parser.read() != ':';
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

	private List<VarInitialization> parseVariableDeclaration(boolean checkForFinalSemicolon, Scope scope, Comment comment) throws ProblemException {
		if (scope == null)
			return null;

		final int backtrack = this.offset;
		final List<VarInitialization> createdVariables = new LinkedList<VarInitialization>();
		eatWhitespace();
		scope = adjustScope(scope, backtrack);
		int rewind;

		rewind = this.offset;
		do {
			final VarInitialization vi = parseVarInitialization(scope, comment, backtrack);
			if (vi == null)
				break;
			createdVariables.add(vi);
			rewind = this.offset;
			eatWhitespace();
		} while(read() == ',');
		seek(rewind);

		if (checkForFinalSemicolon) {
			rewind = this.offset;
			eatWhitespace();
			if (read() != ';') {
				seek(rewind);
				error(Problem.CommaOrSemicolonExpected, this.offset-1, this.offset, Markers.NO_THROW);
			}
		}

		// look for comment following directly and decorate the newly created variables with it
		assignInlineComment(createdVariables);
		return createdVariables != null && createdVariables.size() > 0 ? createdVariables : null;
	}

	private void assignInlineComment(final List<VarInitialization> createdVariables) {
		String inlineComment = textOfInlineComment();
		if (inlineComment != null) {
			inlineComment = inlineComment.trim();
			for (final VarInitialization v : createdVariables)
				v.variable.setUserDescription(inlineComment);
		}
	}

	private Scope adjustScope(Scope scope, final int backtrack) throws ProblemException {
		switch (scope) {
		case STATIC:
			final int pos = this.offset;
			if (readIdent().equals(Keywords.Const))
				scope = Scope.CONST;
			else
				this.seek(pos);
			break;
		case VAR:
			if (currentFunction == null) {
				error(Problem.VarOutsideFunction, backtrack-scope.toKeyword().length(), backtrack, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, scope.toKeyword(), Keywords.GlobalNamed, Keywords.LocalNamed);
				scope = Scope.LOCAL;
			}
			break;
		default:
			break;
		}
		return scope;
	}

	protected TypeAnnotation typeAnnotation(int s, int e, IType type) {
		return new TypeAnnotation(-sectionOffset()+s, -sectionOffset()+e, type);
	}

	private VarInitialization parseVarInitialization(Scope scope, Comment comment, final int backtrack) throws ProblemException {
		eatWhitespace();
		TypeAnnotation staticType;
		final int bt = this.offset;
		int typeExpectedAt = -1;
		if (typing.allowsNonParameterAnnotations()) {
			staticType = parseTypeAnnotation(true, false);
			if (staticType != null)
				eatWhitespace();
			else if (typing == Typing.STATIC) {
				typeExpectedAt = this.offset;
				staticType = typeAnnotation(offset, offset, PrimitiveType.ERRONEOUS);
			} else
				staticType = null;
		} else
			staticType = placeholderTypeAnnotationIfMigrating(this.offset);

		final int s = this.offset;
		String varName = readIdent();
		if (s > bt)
			if (varName.length() == 0) {
				if (typing == Typing.STATIC) {
					staticType = typeAnnotation(offset, offset, PrimitiveType.ERRONEOUS);
					error(Problem.TypeExpected, bt, this.offset, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW);
				} else
					staticType = null;
				seek(bt);
				varName = readIdent();
			} else if (migrationTyping == Typing.STATIC && varName.equals(Keywords.In)) {
				// ugh - for (var object in ...) workaround
				staticType = null;
				seek(bt);
				varName = readIdent();
			}
		if (varName.length() == 0) {
			seek(backtrack);
			return null;
		} else if (typeExpectedAt != -1)
			typeRequiredAt(typeExpectedAt);

		final Declaration outerDec = currentDeclaration();
		try {
			final Variable var = script.createVarInScope(this, currentFunction, varName, scope,
				fragmentOffset()+bt, fragmentOffset()+this.offset, comment);
			if (staticType != null)
				staticType.setTarget(var);
			if (staticType != null) {
				var.assignType(staticType.type(), true);
				staticType.setTarget(var);
			}
			this.currentDeclaration = var;
			VarInitialization varInitialization;
			ASTNode initializationExpression = null;
			{
				eatWhitespace();
				if (peek() == '=') {
					if (scope != Variable.Scope.CONST && currentFunction == null && !engine.settings().supportsNonConstGlobalVarAssignment)
						error(Problem.NonConstGlobalVarAssignment, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW);
					read();
					eatWhitespace();
					initializationExpression = parseExpression();
					var.setInitializationExpression(initializationExpression);
				} else if (scope == Scope.CONST && script != engine)
					error(Problem.ConstantValueExpected, this.offset-1, this.offset, Markers.NO_THROW);
				else if (scope == Scope.STATIC && script == engine)
					var.forceType(PrimitiveType.INT); // most likely
			}
			varInitialization = new VarInitialization(varName, initializationExpression, bt-sectionOffset(), this.offset-sectionOffset(), var, staticType);
			return varInitialization;
		} finally {
			this.currentDeclaration = outerDec;
		}
	}

	private TypeAnnotation placeholderTypeAnnotationIfMigrating(int offset) {
		TypeAnnotation typeAnnotation;
		if (migrationTyping != null && migrationTyping.allowsNonParameterAnnotations()) {
			typeAnnotation = typeAnnotation(offset, offset, null);
			if (typeAnnotations != null && sectionOffset() != 0)
				typeAnnotations.add(typeAnnotation);
		} else
			typeAnnotation = null;
		return typeAnnotation;
	}

	private void typeRequiredAt(int typeExpectedAt) throws ProblemException {
		error(Problem.TypeExpected, typeExpectedAt, typeExpectedAt+1,  Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW);
	}

	@Override
	public Variable newVariable(String varName, Scope scope) {
		return new Variable(varName, scope);
	}

	private TypeAnnotation parseTypeAnnotation(boolean topLevel, boolean required) throws ProblemException {
		required |= typing == Typing.STATIC && migrationTyping == null;
		final int start = this.offset;
		String str;
		TypeAnnotation t = null;
		ID id;
		if (peek() == '&') {
			if (!script.engine().settings().supportsRefs)
				error(Problem.PrimitiveTypeNotSupported, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW,
					'&', script.engine().name());
			read();
			t = typeAnnotation(offset-1, offset, PrimitiveType.REFERENCE);
		}
		else if (peek() == '$') {
			final String placeholderString = parsePlaceholderString();
			final Placeholder p = makePlaceholder(placeholderString);
			return typeAnnotation(start, this.offset, p);
		}
		else if ((str = parseIdentifier()) != null || ((id = parseID()) != null && (str = id.stringValue()) != null)) {
			final PrimitiveType pt = PrimitiveType.fromString(str, typing==Typing.STATIC);
			if (pt != null && script.engine().supportsPrimitiveType(pt))
				/* give explicit parameter types authority boost -
				 * they won't unify with Definitions so that parameters
				 * explicitly accepting any object won't be restricted to specific
				 * definitions
				 */
				t = typeAnnotation(start, offset, pt.unified());
			else if (typing.allowsNonParameterAnnotations())
				if (script.index() != null && engine.acceptsId(str)) {
					final Definition def = script.index().definitionNearestTo(script.file(), ID.get(str));
					if (def != null)
						t = typeAnnotation(start, offset, def);
				}
			if (t != null) {
				final List<TypeAnnotation> subAnnotations = new LinkedList<>();
				final int p = offset;
				eatWhitespace();
				RefinementIndicator: switch (read()) {
				case '&':
					t = typeAnnotation(start, offset, ReferenceType.make(t.type()));
					break;
				case '[':
					if (typing == Typing.STATIC || migrationTyping == Typing.STATIC)
						eatWhitespace();
					final TypeAnnotation elementType = parseTypeAnnotation(false, true);
					expect(']');
					if (elementType != null) {
						subAnnotations.add(elementType);
						if (eq(t.type(), PrimitiveType.ARRAY))
							t.setType(new ArrayType(elementType.type()));
						else if (eq(t.type(), PrimitiveType.ID))
							if (elementType.type() instanceof Definition)
								t.setType(((Definition)elementType.type()).metaDefinition());
							else
								error(Problem.IncompatibleTypes, elementType.start(), elementType.end(), Markers.NO_THROW,
									elementType.type().typeName(true), "definition");
					}
					break RefinementIndicator;
				default:
					seek(p);
				}
				while (true) {
					final int s = this.offset;
					eatWhitespace();
					if (read() != '|') {
						seek(s);
						break;
					} else
						eatWhitespace();
					final TypeAnnotation option = parseTypeAnnotation(false, true);
					if (option != null) {
						subAnnotations.add(option);
						t.setType(typing.unify(t.type(), option.type()));
					}
					else
						break;
					eatWhitespace();
				}
				t.setSubAnnotations(subAnnotations.toArray(new TypeAnnotation[subAnnotations.size()]));
				if (typeAnnotations != null && topLevel)
					typeAnnotations.add(t);
			}
		}
		if (t == null) {
			if (typing == Typing.STATIC) {
				if (required) {
					error(Problem.InvalidType, start, offset, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, readStringAt(start, offset));
					return typeAnnotation(start, offset, new ErroneousType(readStringAt(start, offset)));
				}
				if (topLevel && typeAnnotations != null && sectionOffset() == 0)
					// placeholder annotation
					typeAnnotations.add(typeAnnotation(start, start, null));
			}
			this.seek(start);
		} else
			t.setEnd(-sectionOffset()+offset);
		return t;
	}

	/**
	 * Parse a function declaration.
	 * @param header Description of the alread-parsed function header as {@link FunctionHeader}
	 * @return The parse function or null if something went wrong
	 * @throws ProblemException
	 */
	private Function parseFunctionDeclaration(FunctionHeader header) throws ProblemException {
		int endOfHeader;
		eatWhitespace();
		setCurrentFunction(null);
		if (header.isOldStyle)
			warning(Problem.OldStyleFunc, header.nameStart, header.nameStart+header.name.length(), 0);
		Function func;
		setCurrentFunction(func = newFunction(header.name));
		header.apply(func);
		func.setParent(script);
		eatWhitespace();
		final int parameterIndicator = read();
		switch (parameterIndicator) {
		case '(':
			parseParameters(func);
			break;
		case ':':
			if (header.isOldStyle)
				break; // old-style funcs have no named parameters
			//$FALL-THROUGH$
		default:
			tokenExpectedError("("); //$NON-NLS-1$
			break;
		}
		endOfHeader = this.offset;
		lastComment = null;
		eatWhitespace();
		if (lastComment != null)
			func.setUserDescription(lastComment.text());

		// check initial opening bracket which is mandatory for NET2 funcs
		final int token = read();
		boolean parseBody = true;
		if (token != '{')
			if (typing == Typing.STATIC && token == ';')
				parseBody = false;
			else if (!header.isOldStyle)
				tokenExpectedError("{"); //$NON-NLS-1$
			else
				unread();

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
			final Comment c = commentImmediatelyFollowing();
			if (c != null) {
				func.setUserDescription(c.text());
				funEnd = this.offset;
			}
		}

		// finish up
		func.setLocation(absoluteSourceLocation(header.start, funEnd));
		func.setHeader(absoluteSourceLocation(header.start, endOfHeader));
		final Function existingFunction = script.findLocalFunction(func.name(), false);
		if (existingFunction != null && existingFunction.isGlobal() == func.isGlobal())
			warning(Problem.DuplicateDeclaration, func, Markers.ABSOLUTE_MARKER_LOCATION, func.name());
		script.addDeclaration(func);
		setCurrentFunction(null); // to not suppress errors in-between functions
		return func;
	}

	private void parseParameters(Function func) throws ProblemException {
		// get parameters
		boolean parmExpected = false;
		do {
			eat(WHITESPACE_CHARS);
			final Comment parameterCommentPre = parseComment();
			eat(WHITESPACE_CHARS);
			final Variable parm = parseParameter(func);
			eat(WHITESPACE_CHARS);
			final Comment parameterCommentPost = parseComment();
			eat(WHITESPACE_CHARS);
			if (parm != null) {
				final StringBuilder commentBuilder = new StringBuilder(30);
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
			final int readByte = read();
			if (readByte == ')')
				break; // all parameters parsed
			else if (readByte == ',')
				parmExpected = true;
			else
				error(Problem.UnexpectedToken, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION, (char)readByte);  //$NON-NLS-1$//$NON-NLS-2$
		} while(!reachedEOF());
	}

	/**
	 * Create a new {@link Function}. Depending on what {@link SpecialEngineRules} the current {@link Engine} has, the function might be some specialized instance ({@link DefinitionFunction} or {@link EffectFunction}for example)
	 * @param nameWillBe What the name of the function will be.
	 * @return The newly created function. Might be of some special class.
	 */
	protected Function newFunction(String nameWillBe) {
		if (specialEngineRules != null)
			for (final SpecialFuncRule funcRule : specialEngineRules.defaultParmTypeAssignerRules()) {
				final Function f = funcRule.newFunction(nameWillBe);
				if (f != null)
					return f;
			}
		return new Function();
	}

	private Comment collectPrecedingComment(int absoluteOffset) {
		final Comment c = (lastComment != null && lastComment.precedesOffset(absoluteOffset, buffer)) ? lastComment : null;
		lastComment = null;
		return c;
	}

	private String textOfInlineComment() {
		final int pos = this.offset;
		this.eat(BufferedScanner.WHITESPACE_WITHOUT_NEWLINE_CHARS);
		if (this.eat(BufferedScanner.NEWLINE_CHARS) == 0) {
			final Comment c = parseComment();
			if (c != null)
				return c.text();
		}
		this.seek(pos);
		return null;
	}

	private Number parseHexNumber() throws ProblemException {
		int offset = this.offset;
		final boolean isHex = read() == '0' && read() == 'x';
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
					final int readByte = read();
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

	private Number parseNumber() throws ProblemException {
		Number number;
		final int offset = this.offset;
		int count = 0;
		boolean floatingPoint = false;
		do {
			final int readByte = read();
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
		final String numberString = this.readString(count);
		if (floatingPoint)
			try {
				number = Double.parseDouble(numberString);
			} catch (final NumberFormatException e) {
				number = Double.MAX_VALUE;
				error(Problem.NotANumber, offset, offset+count, Markers.NO_THROW, numberString);
			}
		else
			try {
				number = Long.parseLong(numberString);
			} catch (final NumberFormatException e) {
				number = Integer.MAX_VALUE;
				error(Problem.NotANumber, offset, offset+count, Markers.NO_THROW, numberString);
			}
		this.seek(offset+count);
		return number;
	}

	private boolean parseEllipsis() {
		final int offset = this.offset;
		final String e = this.readString(3);
		if (e != null && e.equals("...")) //$NON-NLS-1$
			return true;
		this.seek(offset);
		return false;
	}

	private String parseMemberOperator() throws ProblemException {
		int savedOffset = this.offset;
		final int firstChar = read();
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
		Operator best = null;
		if (offset + 2 < size && buffer[offset] == '-' && buffer[offset+1] == '>')
			return null;
		OpLoop: for (final Operator op : Operator.values()) {
			final String n = op.operatorName();
			if (offset+n.length() <= size) {
				for (int i = 0; i < n.length(); i++)
					if (n.charAt(i) != buffer[offset+i])
						continue OpLoop;
				if (best == null || best.operatorName().length() < n.length())
					best = op;
			}
		}
		if (best != null) {
			offset += best.operatorName().length();
			if ((best == Operator.ne || best == Operator.eq) && BufferedScanner.isWordPart(peek())) {
				offset -= best.operatorName().length();
				return null;
			}
			return best;
		} else
			return null;
	}

	private void warning(Problem code, int errorStart, int errorEnd, int flags, Object... args) {
		try {
			marker(code, errorStart, errorEnd, flags|Markers.NO_THROW, IMarker.SEVERITY_WARNING, args);
		} catch (final ProblemException e) {
			// won't happen
		}
	}
	private void warning(Problem code, IRegion region, int flags, Object... args) {
		warning(code, region.getOffset(), region.getOffset()+region.getLength(), flags, args);
	}
	private void error(Problem code, int errorStart, int errorEnd, int flags, Object... args) throws ProblemException {
		marker(code, errorStart, errorEnd, flags, IMarker.SEVERITY_ERROR, args);
	}

	private Markers markers;

	public void setMarkers(Markers markers) {
		this.markers = markers;
	}

	protected Markers markers() {
		if (markers != null)
			return markers;
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
	 * @throws ProblemException
	 */
	public void marker(Problem code, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ProblemException {
		markers().marker(this, code, null, markerStart, markerEnd, flags, severity, args);
	}

	public IMarker todo(String todoText, int markerStart, int markerEnd, int priority) {
		return markers().todo(file(), null, todoText, markerStart, markerEnd, priority);
	}

	private void tokenExpectedError(String token) throws ProblemException {
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

	private ASTNode parseSequence() throws ProblemException {
		ASTNode[] _elements;
		final boolean useReusable = !_elementsInUse;
		if (useReusable) {
			_elements = _elementsReusable;
			_elementsInUse = true;
		} else
			_elements = new ASTNode[4];

		final int sequenceParseStart = this.offset;
		eatWhitespace();
		final int sequenceStart = this.offset;
		final Operator preop = parseOperator();
		ASTNode result = null;
		if (preop != null && preop.isPrefix()) {
			ASTNode followingExpr = parseSequence();
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
			final int elmStart = this.offset;

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
				final String word = readIdent();
				if (word != null && word.length() > 0)
					// tricky new keyword parsing that also respects use of new as regular identifier
					if (!noNewProplist && word.equals(Keywords.New)) {
						// don't report errors here since there is the possibility that 'new' will be interpreted as variable name in which case this expression will be parsed again
						final ASTNode prototype = parseExpression(OPENING_BLOCK_BRACKET_DELIMITER);
						boolean treatNewAsVarName = false;
						if (prototype == null)
							treatNewAsVarName = true;
						else {
							eatWhitespace();
							final ProplistDeclaration proplDec = parsePropListDeclaration();
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
					else if (typing == Typing.STATIC && word.equals(Keywords.Cast)) {
						eatWhitespace();
						expect('[');
						eatWhitespace();
						final TypeAnnotation targetType = parseTypeAnnotation(true, true);
						eatWhitespace();
						expect(']');
						eatWhitespace();
						final ASTNode expr = parseExpression();
						elm = new CastExpression(targetType, expr);
					}
					else {
						final int beforeWhitespace = this.offset;
						this.eatWhitespace();
						if (read() == '(') {
							final int s = this.offset;
							// function call
							final List<ASTNode> args = new LinkedList<ASTNode>();
							parseRestOfTuple(args);
							CallDeclaration callFunc;
							final ASTNode[] a = args.toArray(new ASTNode[args.size()]);
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
				elm = parseArrayExpression(prevElm);

			if (elm == null)
				elm = parsePropListExpression(prevElm);

			// ->
			if (elm == null) {
				final int fieldOperatorStart = this.offset;
				final String memberOperator = parseMemberOperator();
				if (memberOperator != null) {
					final int idStart = this.offset;
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
						final List<ASTNode> tupleElms = new LinkedList<ASTNode>();
						parseRestOfTuple(tupleElms);
						elm = new CallExpr(tupleElms.toArray(new ASTNode[tupleElms.size()]));
					} else {
						ASTNode firstExpr = parseExpression();
						if (firstExpr == null)
							firstExpr = whitespace(this.offset, 0);
							// might be disabled
						eatWhitespace();
						c = read();
						if (c == ')')
							elm = new Parenthesized(firstExpr);
						else if (c == ',') {
							// tuple (just for multiple parameters for return)
							final List<ASTNode> tupleElms = new LinkedList<ASTNode>();
							tupleElms.add(firstExpr);
							parseRestOfTuple(tupleElms);
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
						final ASTNode[] n = new ASTNode[_elements.length+10];
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
				final int saved = this.offset;
				eatWhitespace();
				final Operator postop = parseOperator();
				if (postop != null && postop.isPostfix()) {
					final UnaryOp op = new UnaryOp(postop, UnaryOp.Placement.Postfix, result);
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

	protected Placeholder makePlaceholder(String placeholder) throws ProblemException {
		return new Placeholder(placeholder);
	}

	private ASTNode parsePropListExpression(ASTNode prevElm) throws ProblemException {
		final int off = this.offset;
		final ProplistDeclaration proplDec = parsePropListDeclaration();
		if (proplDec != null) {
			final ASTNode elm = new PropListExpression(proplDec);
			final int sectionOffset = sectionOffset();
			elm.setLocation(off-sectionOffset, this.offset-sectionOffset);
			return elm;
		}
		return null;
	}

	protected ProplistDeclaration parsePropListDeclaration() throws ProblemException {
		final int propListStart = offset;
		int c = read();
		if (c == '{') {
			final ProplistDeclaration pl = new ProplistDeclaration((String)null);
			pl.setParent(script);
			final Declaration oldDec = currentDeclaration();
			this.currentDeclaration = pl;
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
						final int nameStart = this.offset;
						String name;
						if ((name = parseString()) != null || (name = parseIdentifier()) != null) {
							final int nameEnd = this.offset;
							eatWhitespace();
							final int c_ = read();
							if (c_ != ':' && c_ != '=') {
								unread();
								error(Problem.UnexpectedToken, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, (char)c_);
							} else {
								eatWhitespace();
								final Variable v = new Variable(name, currentFunction != null ? Scope.VAR : Scope.LOCAL);
								v.setLocation(absoluteSourceLocation(nameStart, nameEnd));
								final Declaration outerDec = currentDeclaration();
								this.currentDeclaration = v;
								ASTNode value = null;
								try {
									v.setParent(outerDec);
									value = parseExpression(COMMA_OR_CLOSE_BLOCK);
									if (value == null) {
										error(Problem.ValueExpected, offset-1, offset, Markers.NO_THROW);
										value = placeholderExpression(offset);
									}
									v.setInitializationExpression(value);
									//v.forceType(value.type(this));
								} finally {
									this.currentDeclaration = outerDec;
								}
								pl.addComponent(v, false);
								expectingComma = true;
							}
						}
						else {
							error(Problem.TokenExpected, this.offset, this.offset+1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, Messages.TokenStringOrIdentifier);
							break;
						}
					}
				}
				if (!properlyClosed)
					error(Problem.MissingClosingBracket, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, "}"); //$NON-NLS-1$
				pl.setLocation(absoluteSourceLocation(propListStart, offset));
				script.addDeclaration(pl);
				return pl;
			} finally { this.currentDeclaration = oldDec; }
		}
		else
			unread();
		return null;
	}

	public final SourceLocation absoluteSourceLocation(int start, int end) {
		return new SourceLocation(start+offsetOfScriptFragment, end+offsetOfScriptFragment);
	}

	public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
		final int bodyOffset = sectionOffset();
		return absoluteSourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
	}

	private ASTNode parseArrayExpression(ASTNode prevElm) throws ProblemException {
		ASTNode elm = null;
		int c = read();
		if (c == '[') {
			if (prevElm != null) {
				// array access
				final ASTNode arg = parseExpression();
				eatWhitespace();
				int t;
				switch (t = read()) {
				case ':':
					final ASTNode arg2 = parseExpression();
					eatWhitespace();
					expect(']');
					elm = new ArraySliceExpression(arg, arg2);
					break;
				case ']':
					elm = new ArrayElementExpression(arg);
					break;
				default:
					error(Problem.UnexpectedToken, this.offset-1, this.offset, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, (char)t);
				}
			} else {
				// array creation
				final Vector<ASTNode> arrayElms = new Vector<ASTNode>(10);
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
						final ASTNode arrayElement = parseExpression(COMMA_OR_CLOSE_BRACKET);
						if (arrayElement != null) {
							if (expectingComma) {
								final ASTNode last = arrayElms.get(arrayElms.size()-1);
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

	private void parseRestOfTuple(List<ASTNode> listToAddElementsTo) throws ProblemException {
		boolean expectingComma = false;
		int lastStart = this.offset;
		Loop: while (!reachedEOF()) {
			eatWhitespace();
			switch (read()) {
			case ';':
				error(Problem.UnexpectedToken, this.offset-1, this.offset, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, ';');
				//$FALL-THROUGH$
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
					error(Problem.InternalError, this.offset, this.offset, Markers.ABSOLUTE_MARKER_LOCATION, Messages.InternalError_WayTooMuch);
				//	break;
				ASTNode arg = parseTupleElement();
				if (arg == null) {
					error(Problem.ExpressionExpected, this.offset, this.offset+1, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION);
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

	protected ASTNode parseTupleElement() throws ProblemException {
		return parseExpression();
	}

	protected ASTNode parseExpression(char[] delimiters) throws ProblemException {

		final int offset = this.offset;

		final int START = 0;
		final int OPERATOR = 1;
		final int SECONDOPERAND = 2;
		final int DONE = 3;

		ASTNode root = null;
		ASTNode current = null;
		BinaryOp lastOp = null;

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
					root = parseSequence();
					if (root != null) {
						current = root;
						state = OPERATOR;
					} else
						state = DONE;
					break;
				case OPERATOR:
					final int operatorStartPos = this.offset;
					eatWhitespace();
					// end of expression?
					final int c = read();
					for (int i = 0; i < delimiters.length; i++)
						if (delimiters[i] == c) {
							state = DONE;
							this.seek(operatorStartPos);
							break;
						}

					if (state != DONE) {
						unread(); // unread c
						final Operator op = parseOperator();
						if (op != null && op.isBinary()) {
							final int priorOfNewOp = op.priority();
							ASTNode newLeftSide = null;
							BinaryOp theOp = null;
							for (ASTNode opFromBottom = current.parent(); opFromBottom instanceof BinaryOp; opFromBottom = opFromBottom.parent()) {
								final BinaryOp oneOp = (BinaryOp) opFromBottom;
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
					ASTNode rightSide = parseSequence();
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
		final ASTNode result = new ASTNode();
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
		final int offset = sectionOffset();
		if (offset == 0 || (flags & Markers.ABSOLUTE_MARKER_LOCATION) == 0)
			return region;
		else
			return new Region(offset+region.getOffset(), region.getLength());
	}

	protected ASTNode parseExpression() throws ProblemException {
		return parseExpression(SEMICOLON_DELIMITER);
	}

	/**
	 * Parse a string literal and store it in the {@link FunctionContext#parsedString} field.
	 * @return Whether parsing was successful.
	 * @throws ProblemException
	 */
	private String parseString() throws ProblemException {
		final int quotes = read();
		if (quotes != '"') {
			unread();
			return null;
		}
		final int start = offset;
		boolean escaped = false;
		boolean properEnd = false;
		Loop: do {
			final int c = read();
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
	 * @throws ProblemException
	 */
	private String parseIdentifier() throws ProblemException {
		final String word = readIdent();
		if (word != null && word.length() > 0)
			return word;
		else
			return null;
	}

	/**
	 * Parse a $...$ placeholder.
	 * @return Whether there was a placeholder at the current offset.
	 * @throws ProblemException
	 */
	private String parsePlaceholderString() throws ProblemException {
		if (read() != '$') {
			unread();
			return null;
		}
		final StringBuilder builder = new StringBuilder();
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
	 * @throws ProblemException
	 */
	protected Statement parseStatement() throws ProblemException {
		return parseStatement(ParseStatementOption.NoOptions);
	}

	private Statement parseStatementWithPrependedComments() throws ProblemException {
		// parse comments and attach them to the statement so the comments won't be removed by code reformatting
		final List<Comment> prependedComments = collectComments();
		final Statement s = parseStatement();
		if (s != null && prependedComments != null)
			s.addAttachments(prependedComments);
		return s;
	}

	/**
	 * Parse a statement.
	 * @param options Options on how to parse the statement
	 * @return The parsed statement or null if parsing was unsuccessful.
	 * @throws ProblemException
	 */
	private Statement parseStatement(EnumSet<ParseStatementOption> options) throws ProblemException {
		int emptyLines = -1;
		int delim;
		for (; (delim = peek()) != -1 && BufferedScanner.isWhiteSpace((char) delim); read()) {
			final char c = (char) delim;
			if (c == '\n')
				emptyLines++;
		}

		//eatWhitespace();
		final int start = this.offset;
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
				final int read = read();
				if (read == '{' && !options.contains(ParseStatementOption.InitializationStatement)) {
					final List<ASTNode> subStatements = new LinkedList<>();
					parseStatementBlock(start, subStatements, ParseStatementOption.NoOptions, false);
					result = new Block(subStatements);
				}
				else if (read == ';')
					result = new EmptyStatement();
				else if (read == '[' && options.contains(ParseStatementOption.ExpectFuncDesc)) {
					final String funcDesc = this.readStringUntil(']');
					read();
					result = new FunctionDescription(funcDesc);
				}
				else {
					unread();
					final ASTNode expression = parseExpression();
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
				final List<VarInitialization> initializations = parseVariableDeclaration(false, scope, null);
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
				result = parseKeywordStatement(readWord);
			else
				result = null;
		}

		// just an expression that needs to be wrapped as a statement
		if (result == null) {
			this.seek(start);
			final ASTNode expression = parseExpression();
			final int afterExpression = this.offset;
			if (expression != null) {
				result = new SimpleStatement(expression);
				if (!options.contains(ParseStatementOption.InitializationStatement)) {
					final int beforeWhitespace = this.offset;
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
			final Comment c = commentImmediatelyFollowing();
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
	 * @throws ProblemException
	 */
	protected void parseStatementBlock(int start, List<ASTNode> statements, EnumSet<ParseStatementOption> options, boolean oldStyle) throws ProblemException {
		boolean done = false;
		int garbageStart = -1;
		while (!reachedEOF()) {
			final int potentialGarbageEnd = offset;
			//eatWhitespace();
			final Statement statement = parseStatement(options);
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
			final boolean statementIsComment = statement instanceof Comment;
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

	private int maybeAddGarbageStatement(List<ASTNode> statements, int garbageStart, int potentialGarbageEnd) throws ProblemException {
		String garbageString = new String(buffer, garbageStart, Math.min(potentialGarbageEnd, buffer.length-garbageStart));
		garbageString = modifyGarbage(garbageString);
		if (garbageString != null && garbageString.length() > 0) {
			final GarbageStatement garbage = new GarbageStatement(garbageString, garbageStart-sectionOffset());
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
		final int daring = this.offset;
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
	 * @throws ProblemException
	 */
	private void expect(char expected) throws ProblemException {
		if (read() != expected) {
			unread();
			tokenExpectedError(String.valueOf(expected));
		}
	}

	/**
	 * Expect a certain identifier at the current offset.
	 * @param expected The identifier expected
	 * @throws ProblemException
	 */
	private void expect(String expected) throws ProblemException {
		final String r = readIdent();
		if (r == null || !r.equals(expected))
			tokenExpectedError(expected);
	}

	private boolean parseSemicolonOrReturnFalse() {
		final int old = offset;
		eatWhitespace();
		switch (read()) {
		case ';':
			return true;
		default:
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
	 * @throws ProblemException
	 */
	private Statement parseKeywordStatement(String keyWord) throws ProblemException {
		switch (keyWord) {
		case Keywords.If:
			return parseIf();
		case Keywords.While:
			return parseWhile();
		case Keywords.Do:
			return parseDoWhile();
		case Keywords.For:
			return parseFor();
		case Keywords.Continue:
			return needsSemicolon(new ContinueStatement());
		case Keywords.Break:
			return needsSemicolon(new BreakStatement());
		case Keywords.Return:
			return parseReturn();
		default:
			return null;
		}
	}

	/**
	 * Parse a return statement.
	 * @return The parsed return statement
	 * @throws ProblemException
	 */
	private Statement parseReturn() throws ProblemException {
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
	 * @throws ProblemException
	 */
	private DoWhileStatement parseDoWhile() throws ProblemException {
		final Statement block = parseStatement();
		eatWhitespace();
		expect(Keywords.While);
		eatWhitespace();
		expect('(');
		eatWhitespace();
		final ASTNode cond = parseExpression();
		eatWhitespace();
		expect(')');
		//expect(';');
		return new DoWhileStatement(cond, block);
	}

	/**
	 * Parse a for statement. The result is either a {@link ForStatement} or an {@link IterateArrayStatement}.
	 * @return The parsed for loop.
	 * @throws ProblemException
	 */
	private KeywordStatement parseFor() throws ProblemException {
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
			final int pos = this.offset;
			final String varName = readIdent();
			if (!(varName.equals("") || varName.equals(Keywords.VarNamed))) { //$NON-NLS-1$
				eatWhitespace();
				w = readIdent();
				if (w.equals(Keywords.In)) {
					final AccessVar accessVar = new AccessVar(varName);
					setRelativeLocation(accessVar, pos, pos+varName.length());
					initialization = new SimpleStatement(accessVar);
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
					error(Problem.ConditionExpected, savedOffset, this.offset, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW);
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
	 * @throws ProblemException
	 */
	private WhileStatement parseWhile() throws ProblemException {
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
		final Statement body = parseStatement();
		if (body == null)
			error(Problem.StatementExpected, offset, offset+4, Markers.NO_THROW);
		result = new WhileStatement(condition, body);
		return result;
	}

	/**
	 * Parse an IfStatement.
	 * @return The IfStatement
	 * @throws ProblemException
	 */
	private IfStatement parseIf() throws ProblemException {
		IfStatement result;
		eatWhitespace();
		expect('(');
		eatWhitespace();
		ASTNode condition = parseExpression();
		if (condition == null)
			condition = whitespace(this.offset, 0); // if () is valid
		eatWhitespace();
		expect(')');
		final int offsetBeforeWhitespace = this.offset;
		final Statement ifStatement = withMissingFallback(offsetBeforeWhitespace, parseStatementWithPrependedComments());
		final int beforeElse = this.offset;
		eatWhitespace();
		final int o = this.offset;
		final String nextWord = readIdent();
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

	private Statement withMissingFallback(int offsetWhereExpected, Statement statement) throws ProblemException {
		return statement != null
			? statement
				: new MissingStatement(offsetWhereExpected-sectionOffset());
	}

	/**
	 * Parse an id. On successful parsing, the parsed will be stored in the parsedID field.
	 * @return Whether parsing the id was successful. If false, one can be assured that parsedID will be null.
	 * @throws ProblemException
	 */
	private ID parseID() throws ProblemException {
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
	 * @throws ProblemException
	 */
	private Variable parseParameter(Function function) throws ProblemException {

		final int backtrack = this.offset;
		eatWhitespace();
		if (script == engine && parseEllipsis())
			return addVarParmsParm(function);
		if (peek() == ')') {
			seek(backtrack);
			return null;
		}

		final int typeStart = this.offset;
		TypeAnnotation type = parseTypeAnnotation(true, false);
		final int typeEnd = this.offset;
		eatWhitespace();
		int nameStart = this.offset;
		String parmName = readIdent();
		if (parmName.length() == 0) {
			type = null;
			seek(nameStart = backtrack);
			eatWhitespace();
			final int ta = this.offset;
			parmName = readIdent();
			if (parmName.length() == 0)
				return null;
			type = placeholderTypeAnnotationIfMigrating(ta);
		}
		switch (typing) {
		case STATIC:
			if (type == null)
				typeRequiredAt(typeStart);
			break;
		case DYNAMIC:
			if (type != null)
				error(Problem.NotSupported, typeStart, typeEnd, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, readStringAt(typeStart, typeEnd), engine.name() + " with no type annotations");
			break;
		default:
			break;
		}
		final Variable var = new Variable(null, Scope.PARAMETER);
		if (type != null)
			type.setTarget(var);
		if (type != null) {
			if ((type.type() == PrimitiveType.REFERENCE || type.type() instanceof ReferenceType) && !engine.supportsPrimitiveType(PrimitiveType.REFERENCE))
				error(Problem.PrimitiveTypeNotSupported, offset-1, offset, Markers.NO_THROW, PrimitiveType.REFERENCE.typeName(true), script.engine().name());
			var.forceType(type.type(), true);
		}
		var.setName(parmName);
		var.setLocation(new SourceLocation(nameStart-function.start(), this.offset-function.start()));
		var.setParent(function);
		if (type != null)
			type.setTarget(var);
		function.addParameter(var);
		eatWhitespace();
		return var;
	}

	/**
	 * Delete declarations inside the script container assigned to the parser and remove markers.
	 */
	public void clear() {
		Markers.clearMarkers(scriptFile);
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
		final Function f = currentFunction;
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
		final ASTNode result = new Whitespace();
		setRelativeLocation(result, start, start+length);
		return result;
	}

	/**
	 * Instruct this parser to parse a standalone-statement in some newly passed string. Shouldn't be called when expecting the parser to continue keeping
	 * track of its preceding state, since buffer and scanner offset will be reset.
	 * @param statementText The statement text to parse
	 * @param context Function context. If null, some temporary context will be created internally.
	 * @param visitor Script parser visitor
	 * @return The {@link Statement}, or a {@link BunchOfStatements} if more than one statement could be parsed from statementText. Possibly null, if erroneous text was passed.
	 * @throws ProblemException
	 */
	public <T> ASTNode parseStandaloneStatement(final String statementText, Function function) throws ProblemException {
		init(statementText);
		setCurrentFunction(function);
		markers().enableError(Problem.NotFinished, false);

		final List<ASTNode> statements = new LinkedList<ASTNode>();
		Statement statement;
		do {
			statement = parseStatement();
			if (statement != null) {
				statement.setParent(function);
				statements.add(statement);
			}
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
