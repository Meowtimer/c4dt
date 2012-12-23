package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.as;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.CStyleScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SilentParsingException;
import net.arctics.clonk.parser.SilentParsingException.Reason;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.IMarkerListener.Decision;
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
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ContinueStatement;
import net.arctics.clonk.parser.c4script.ast.ControlFlow;
import net.arctics.clonk.parser.c4script.ast.DoWhileStatement;
import net.arctics.clonk.parser.c4script.ast.Ellipsis;
import net.arctics.clonk.parser.c4script.ast.EmptyStatement;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.False;
import net.arctics.clonk.parser.c4script.ast.ForStatement;
import net.arctics.clonk.parser.c4script.ast.FunctionBody;
import net.arctics.clonk.parser.c4script.ast.FunctionDescription;
import net.arctics.clonk.parser.c4script.ast.GarbageStatement;
import net.arctics.clonk.parser.c4script.ast.IASTVisitor;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.IFunctionCall;
import net.arctics.clonk.parser.c4script.ast.ITypeInfo;
import net.arctics.clonk.parser.c4script.ast.IfStatement;
import net.arctics.clonk.parser.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.parser.c4script.ast.KeywordStatement;
import net.arctics.clonk.parser.c4script.ast.LongLiteral;
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
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.ast.UnaryOp;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarInitialization;
import net.arctics.clonk.parser.c4script.ast.WhileStatement;
import net.arctics.clonk.parser.c4script.ast.Wildcard;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.c4script.effect.EffectFunction;
import net.arctics.clonk.parser.c4script.statictyping.TypeAnnotation;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkBuilder;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ProjectSettings.Typing;
import net.arctics.clonk.resource.c4group.C4GroupItem;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A C4Script parser. Parses declarations in a script and stores it in a C4ScriptBase object (sold separately).
 * The parser can also perform a second parse phase that creates syntax trees from c4script functions. Those can be used for various purposes, including
 * checking correctness (aiming to detect all kinds of errors like undeclared identifiers, supplying values of wrong type to functions etc.), converting old
 * c4script code to #strict-compliant "new-style" code and forming the base of navigation operations like "Find Declaration", "Find References" etc.
 */
public class C4ScriptParser extends CStyleScanner implements DeclarationObtainmentContext, IEvaluationContext {
	
	private static final EnumSet<ParserErrorCode> DISABLED_INSTANT_ERRORS = EnumSet.of(
		ParserErrorCode.TokenExpected,
		ParserErrorCode.InvalidExpression,
		ParserErrorCode.BlockNotClosed,
		ParserErrorCode.NotAllowedHere
	);
	private static final boolean DEBUG = false;
	private static final boolean UNUSEDPARMWARNING = false;
	private static final char[] SEMICOLON_DELIMITER = new char[] { ';' };
	private static final char[] OPENING_BLOCK_BRACKET_DELIMITER = new char[] { '{' };
	private static final char[] COMMA_OR_CLOSE_BRACKET = new char[] { ',', ']' };
	private static final char[] COMMA_OR_CLOSE_BLOCK = new char[] { ',', '}' };
	
	private Function currentFunction;
	private Declaration currentDeclaration;
	private ID parsedID;
	private Number parsedNumber;
	private String parsedMemberOperator;
	private int parseExpressionRecursion;
	private int parseStatementRecursion;
	private TypeAnnotation parsedTypeAnnotation;
	private TypeEnvironment typeEnvironments;

	private final Set<ParserErrorCode> disabledErrors = new HashSet<ParserErrorCode>();
	/**
	 * Whether the current statement is not reached
	 */
	private boolean statementReached;
	/**
	 * Stores the current loop the parser is parsing.
	 */
	private LoopType currentLoop;
	/**
	 * Number of unnamed parameters used in activeFunc (Par(5) -> 6 unnamed parameters).
	 * If a complex expression is passed to Par() this variable is set to UNKNOWN_PARAMETERNUM
	 */
	private int numUnnamedParameters;
	private ExprElm problemReporter;
	
	public ExprElm problemReporter() {return problemReporter;}
	public void setProblemReporter(ExprElm reporter) {problemReporter=reporter;}

	public static final int MAX_PAR = 10;
	public static final int MAX_NUMVAR = 20;
	public static final int UNKNOWN_PARAMETERNUM = MAX_PAR+1;
	
	private static final Set<ParserErrorCode> NO_DISABLED_ERRORS = Collections.unmodifiableSet(new HashSet<ParserErrorCode>());
	
	protected IASTVisitor listener;
	/**
	 * Reference to project file the script was read from.
	 */
	protected IFile scriptFile;
	/**
	 * Script container, the parsed declarations are put into
	 */
	protected Script script;
	/**
	 * Cached strict level from #strict directive.
	 */
	protected int strictLevel;
	/**
	 * Whether to parse the script with static typing rules.
	 */
	protected Typing typing;
	protected Typing migrationTyping;
	/**
	 * Whether the script contains an #appendto
	 */
	protected boolean appendTo;
	/**
	 * Whether to not create any error markers at all - set if script is contained in linked group
	 */
	protected boolean allErrorsDisabled;
	/**
	 * Whether the script is an engine script
	 */
	protected boolean isEngine;
	/**
	 * Cached set of errors disabled by project settings
	 */
	private Set<ParserErrorCode> errorsDisabledByProjectSettings = NO_DISABLED_ERRORS;
	/**
	 * Set of functions already parsed. Won't be parsed again.
	 */
	private SpecialEngineRules specialEngineRules;
	private Engine engine;
	private ClonkBuilder builder;
	private CachedEngineDeclarations cachedEngineDeclarations;
	protected boolean findDefinitionViaCall;
	
	public final boolean findDefinitionViaCall() {return findDefinitionViaCall;}
	public final Engine engine() {return engine;}
	public final SpecialEngineRules specialEngineRules() {return specialEngineRules;}
	public boolean allErrorsDisabled() {return allErrorsDisabled;}
	public void setBuilder(ClonkBuilder builder) {this.builder = builder;}
	public ClonkBuilder builder() {return builder;}
	public final Typing staticTyping() { return typing; }
	
	/**
	 * Returns the expression listener that is notified when an expression or a statement has been parsed.
	 * @return the expression listener
	 */
	public IASTVisitor listener() {
		return listener;
	}

	/**
	 * Sets the expression listener.
	 * @param listener the new expression listener
	 */
	public void setListener(IASTVisitor listener) {
		this.listener = listener;
	}
	
	/**
	 * Return whether the script being parsed has #appendtos. Stored in field for performance.
	 * @return Whether or not.
	 */
	public final boolean hasAppendTo() {
		return appendTo;
	}
	
	/**
	 * Informs the parser that an unnamed parameter was used by calling the Par() function with the given index expression.
	 * @param index the index expression
	 */
	public void unnamedParamaterUsed(ExprElm index) {
		if (numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
			Object ev = index.evaluateAtParseTime(currentFunction());
			if (ev instanceof Number) {
				int number = ((Number)ev).intValue();
				numUnnamedParameters = number >= 0 && number < MAX_PAR ? number+1 : UNKNOWN_PARAMETERNUM;
			} else
				numUnnamedParameters = UNKNOWN_PARAMETERNUM;
		}
	}
	
	/**
	 * Ask the parser to store type information about an expression. No guarantees whether type information will actually be stored.
	 */
	@Override
	public void storeType(ExprElm expression, IType type) {
		ITypeInfo requested = requestTypeInfo(expression);
		if (requested != null) {
			if (DEBUG)
				warning(ParserErrorCode.TypingJudgment, expression, 0, expression.toString(), type.typeName(true));
			requested.storeType(type);
		}
	}
	
	/**
	 * Requests type information for an expression
	 * @param expression the expression
	 * @param list 
	 * @return the type information or null if none has been stored
	 */
	public ITypeInfo requestTypeInfo(ExprElm expression) {
		if (typeEnvironments == null || typing == Typing.Static || typing == Typing.Dynamic)
			return null;
		boolean topMostLayer = true;
		ITypeInfo base = null;
		for (TypeEnvironment list = typeEnvironments; list != null; list = list.up) {
			for (ITypeInfo info : list)
				if (info.storesTypeInformationFor(expression, this))
					if (!topMostLayer) {
						base = info;
						break;
					}
					else
						return info;
			topMostLayer = false;
		}
		ITypeInfo newlyCreated = expression.createTypeInfo(this);
		if (newlyCreated != null) {
			if (base != null)
				newlyCreated.merge(base);
			typeEnvironments.add(newlyCreated);
		}
		return newlyCreated;
	}
	
	public TypeEnvironment newTypeEnvironment() {
		TypeEnvironment l = new TypeEnvironment();
		l.up = this.typeEnvironments;
		this.typeEnvironments = l;
		return l;
	}
	
	public void endTypeEnvironment(boolean inject, boolean ignoreLocals) {
		if (inject && typeEnvironments.up != null)
			typeEnvironments.up.inject(typeEnvironments, ignoreLocals);
		typeEnvironments = typeEnvironments.up;
	}
	
	/**
	 * Query the type of an arbitrary expression. With some luck the parser will be able to give an answer.
	 * @param expression the expression to query the type of
	 * @return The typeinfo or null if nothing was found
	 */
	public ITypeInfo queryTypeInfo(ExprElm expression) {
		if (typeEnvironments == null)
			return null;
		for (TypeEnvironment list = typeEnvironments; list != null; list = list.up)
			for (ITypeInfo info : list)
				if (info.storesTypeInformationFor(expression, this))
					return info;
		return null;
	}
	
	/**
	 * Look up stored type information for the passed expression, defaulting to the specified type if no
	 * information could be found.
	 * @param expression The expression to query the type for
	 * @param defaultType Default type to return if no type was found.
	 * @return Expression type as deduced by usage of the expression or the default type.
	 */
	@Override
	public IType queryTypeOfExpression(ExprElm expression, IType defaultType) {
		ITypeInfo info = queryTypeInfo(expression);
		return info != null ? info.type() : defaultType;
	}
	
	/**
	 * Returns the strict level of the script that was specified using the #strict directive.
	 * @return
	 */
	public int strictLevel() {
		return strictLevel;
	}
	
	/**
	 * Returns the function that is currently being parsed or otherwise considered "current"
	 * @return the current function
	 */
	@Override
	public Function currentFunction() {
		return currentFunction;
	}
	
	/**
	 * Sets the current function. There should be a good reason to call this. 
	 * @param func
	 */
	@Override
	public void setCurrentFunction(Function func) {
		if (func != currentFunction) {
			currentFunction = func;
			setCurrentDeclaration(func);
			numUnnamedParameters = 0;
		}
	}
	
	/**
	 * Returns the first variable in the parent chain of currentDeclaration
	 * @return
	 */
	public Variable currentVariable() {
		return currentDeclaration() != null ? currentDeclaration().firstParentDeclarationOfType(Variable.class) : null;
	}
	
	/**
	 * Returns the declaration that is currently being parsed.
	 * @return
	 */
	@Override
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
	@Override
	public Definition definition() {
		if (script instanceof Definition)
			return (Definition) script;
		return null;
	}
	
	/**
	 * Creates a script parser. The script is read from the file attached to the script (queried through getScriptFile()).
	 */
	public C4ScriptParser(Script script) {
		this((IFile) script.scriptStorage(), script);
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
			cachedEngineDeclarations = engine.cachedDeclarations();
			typing = Typing.ParametersOptionallyTyped;
			migrationTyping = null;
			if (script.index() instanceof ProjectIndex) {
				ProjectIndex projIndex = (ProjectIndex) script.index();
				ClonkProjectNature nature = projIndex.nature();
				if (nature != null) {
					errorsDisabledByProjectSettings = nature.settings().getDisabledErrorsSet();
					typing = nature.settings().typing;
					migrationTyping = nature.settings().migrationTyping;
				}
			}
			strictLevel = script.strictLevel();
			script.containsGlobals = false;
			script.setTypeAnnotations(null);
		}
		statementReached = true;
		if (scriptFile != null)
			allErrorsDisabled = C4GroupItem.groupItemBackingResource(scriptFile) != null;
	}
	
	@Override
	public CachedEngineDeclarations cachedEngineDeclarations() {
		return cachedEngineDeclarations;
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
		this.isEngine = script instanceof Engine;
		initialize();
	}
	
	/**
	 * Perform a full parsing (that includes cleaning up the current state of the script container, parsing declarations and parsing function code).
	 * @throws ParsingException
	 */
	public void parse() throws ParsingException {
		clean();
		parseDeclarations();
		parseCodeOfFunctionsAndValidate();
	}
	
	public String parseTokenAndReturnAsString() throws ParsingException {
		String s;
		if ((s = parseString()) != null)
			return '"'+s+'"';
		if ((s = parseIdentifier()) != null)
			return s;
		if (parseNumber())
			return String.valueOf(parsedNumber);
		else
			return String.valueOf((char)read());
	}
	
	/**
	 * Parse declarations but not function code. Before calling this it should be ensured that the script is {@link #clean()}-ed to avoid duplicates.
	 */
	public void parseDeclarations() {
		strictLevel = script.strictLevel();
		if (typing.allowsNonParameterAnnotations() || migrationTyping != null)
			typeAnnotations = new ArrayList<TypeAnnotation>();
		this.seek(0);
		enableError(ParserErrorCode.StringNotClosed, false); // just one time error when parsing function code
		try {
			parseInitialSourceComment();
			eatWhitespace();
			while (!reachedEOF()) {
				if (!parseDeclaration()) {
					eatWhitespace();
					if (!reachedEOF()) {
						int start = this.offset;
						String tokenText = parseTokenAndReturnAsString();
						error(ParserErrorCode.UnexpectedToken, start, this.offset, NO_THROW, tokenText);
					}
				}
				eatWhitespace();
			}
		}
		catch (ParsingException e) {
			return;
		}
		finally {
			enableError(ParserErrorCode.StringNotClosed, true);
			if (markers != null)
				markers.deploy();
		}
	}

	private void parseInitialSourceComment() {
		eat(WHITESPACE_CHARS);
		Comment sourceComment = null;
		for (Comment c; (c = parseCommentObject()) != null;)
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
	public void parseCodeOfFunctionsAndValidate() throws ParsingException {
		prepareForFunctionParsing();
		for (Function function : script.functions())
			parseCodeOfFunction(function);
		setCurrentDeclaration(null);

		for (Directive directive : script.directives())
			directive.validate(this);
		script.notDirty();
		distillAdditionalInformation();
		if (markers != null)
			markers.deploy();
	}

	public void prepareForFunctionParsing() {
		strictLevel = script.strictLevel();
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
				ExprElm expr = nameLocal.initializationExpression();
				if (expr != null)
					obj.setName(expr.evaluateAtParseTime(obj).toString());
			}
		}
	}

	/**
	 * Parse code of one single function. {@link #parseCodeOfFunctionsAndValidate()} calls this for all functions in the script. 
	 * @param function The function to be parsed
	 * @throws ParsingException
	 */
	private void parseCodeOfFunction(Function function) throws ParsingException {
		if (function.bodyLocation() == null)
			return;
		if (!function.staticallyTyped())
			function.assignType(PrimitiveType.UNKNOWN, false);

		if (specialEngineRules != null)
			for (SpecialFuncRule eventListener : specialEngineRules.functionEventListeners())
				eventListener.functionAboutToBeParsed(function, this);

		int oldOffset = this.offset;
		Declaration oldDec = currentDeclaration();
		try {
			setCurrentFunction(function);
			// reset local vars
			function.resetLocalVarTypes();
			this.seek(function.bodyLocation().start());
			// parse code block
			int endOfFunc = function.bodyLocation().end();
			EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
			List<Statement> statements = new LinkedList<Statement>();
			parseStatementBlock(offset, endOfFunc, statements, options, VisitCodeFlavour.AlsoStatements);
			FunctionBody bunch = new FunctionBody(function, statements);
			if (function.isOldStyle() && statements.size() > 0)
				function.bodyLocation().setEnd(statements.get(statements.size() - 1).end() + bodyOffset());
			if (builder == null)
				reportProblemsOf(statements, false);
			function.storeBody(bunch, functionSource(function));
			if (numUnnamedParameters < UNKNOWN_PARAMETERNUM)
				function.createParameters(numUnnamedParameters);
			else if (numUnnamedParameters == UNKNOWN_PARAMETERNUM && (function.numParameters() == 0 || function.parameter(function.numParameters() - 1).isActualParm()))
				addVarParmsParm(function);
		} catch (SilentParsingException e) {
			// not really an error
		} catch (ParsingException e) {
			// System.out.println(String.format("ParsingException in %s (%s)",
			// activeFunc.getName(), container.getName()));
			// e.printStackTrace();
			// not very exceptional
		} catch (Exception e) {
			// errorWithCode throws ^^;
			e.printStackTrace();
			error(ParserErrorCode.InternalError, this.offset, this.offset + 1, NO_THROW, e.getMessage());
		} finally {
			setCurrentDeclaration(oldDec);
			currentFunction = null;
			seek(oldOffset);
		}
	}

	private void assignDefaultParmTypesToFunction(Function function) {
		if (specialEngineRules != null)
			for (SpecialFuncRule funcRule : specialEngineRules.defaultParmTypeAssignerRules())
				if (funcRule.assignDefaultParmTypes(this, function))
					return;
	}

	/**
	 * Warn about variables declared inside the given block that have not been referenced elsewhere ({@link Variable#isUsed() == false})
	 * @param func The function the block belongs to.
	 * @param block The {@link Block}
	 */
	public void warnAboutPossibleProblemsWithFunctionLocalVariables(Function func, Iterable<Statement> statements) {
		if (func == null)
			return;
		if (UNUSEDPARMWARNING)
			for (Variable p : func.parameters())
				if (!p.isUsed())
					warning(ParserErrorCode.UnusedParameter, p.location(), ABSOLUTE_MARKER_LOCATION, p.name());
		if (func.localVars() != null)
			for (Variable v : func.localVars()) {
				if (!v.isUsed())
					createWarningAtDeclarationOfVariable(statements, v, ParserErrorCode.Unused, v.name());
				Variable shadowed = script().findVariable(v.name());
				// ignore those pesky static variables from scenario scripts
				if (shadowed != null && !(shadowed.parentDeclaration() instanceof Scenario))
					createWarningAtDeclarationOfVariable(statements, v, ParserErrorCode.IdentShadowed, v.qualifiedName(), shadowed.qualifiedName());
			}
	}

	private boolean createWarningAtDeclarationOfVariable(
		Iterable<Statement> statements,
		Variable variable,
		ParserErrorCode code,
		Object... args
	) {
		for (Statement s : statements)
			for (VarDeclarationStatement decl : s.collectionExpressionsOfType(VarDeclarationStatement.class))
				for (VarInitialization initialization : decl.variableInitializations())
					if (initialization.variable == variable) {
						ExprElm old = problemReporter;
						problemReporter = decl;
						warning(code, initialization, 0, args);
						problemReporter = old;
						return true;
					}
		return false;
	}

	private Variable addVarParmsParm(Function func) {
		Variable v = new Variable("...", PrimitiveType.ANY); //$NON-NLS-1$
		v.setParentDeclaration(func);
		v.setScope(Variable.Scope.PARAMETER);
		func.addParameter(v);
		return v;
	}

	/**
	 * Parses the declaration at the current this position.
	 * @return whether parsing was successful
	 * @throws ParsingException
	 */
	protected boolean parseDeclaration() throws ParsingException {
		final int startOfDeclaration = this.offset;
		int readByte = read();
		boolean success = false;
		if (readByte == '#') {
			// directive
			String directiveName = this.readStringUntil(BufferedScanner.WHITESPACE_CHARS);
			DirectiveType type = DirectiveType.makeType(directiveName);
			if (type == null) {
				warning(ParserErrorCode.UnknownDirective, startOfDeclaration, startOfDeclaration + 1 + (directiveName != null ? directiveName.length() : 0), 0, directiveName);
				this.moveUntil(BufferedScanner.NEWLINE_CHARS);
				success = true;
			}
			else {
				String content = parseDirectiveParms();
				Directive directive = new Directive(type, content);
				directive.setLocation(absoluteSourceLocation(startOfDeclaration, this.offset));
				script.addDeclaration(directive);
				if (type == DirectiveType.APPENDTO)
					appendTo = true;
				success = true;
			}
		}
		else {
			this.seek(startOfDeclaration);
			
			if (!success) {
				FunctionHeader functionHeader = FunctionHeader.parse(this, true);
				if (functionHeader != null)
					if (parseFunctionDeclaration(functionHeader))
						success = true;
			}
			
			if (!success) {
				String word = readIdent();
				Scope scope = word != null ? Scope.makeScope(word) : null;
				if (scope != null) {
					List<VarInitialization> v = parseVariableDeclaration(false, true, scope, collectPrecedingComment(startOfDeclaration));
					if (v != null)
						success = true;
				}
			}
		}
		if (!success)
			this.seek(startOfDeclaration);
		return success;
	}

	private String parseDirectiveParms() {
		StringBuffer buffer = new StringBuffer(80);
		while (!reachedEOF() && !BufferedScanner.isLineDelimiterChar((char)peek()) && !parseComment())
			buffer.append((char)read());
		// do let the comment be eaten
		return buffer.length() != 0
			? buffer.toString().trim()
			: null;
	}

	private static class FunctionHeader {
		public String name;
		public FunctionScope scope;
		public boolean isOldStyle;
		public int nameStart;
		public int start;
		public IType returnType;
		public TypeAnnotation typeAnnotation;
		public FunctionHeader(int start, String name, FunctionScope scope, boolean isOldStyle, int nameStart, IType returnType, TypeAnnotation typeAnnotation) {
			super();
			this.start = start;
			this.name = name;
			this.scope = scope;
			this.isOldStyle = isOldStyle;
			this.nameStart = nameStart;
			this.returnType = returnType;
			this.typeAnnotation = typeAnnotation;
		}
		public static FunctionHeader parse(C4ScriptParser parser, boolean allowOldStyle) throws ParsingException {
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
								parser.error(ParserErrorCode.TypeExpected, bt,bt+1, NO_THROW|ABSOLUTE_MARKER_LOCATION);
							}
							break;
						case ParametersOptionallyTyped:
							if (!parser.isEngine && returnType != PrimitiveType.REFERENCE) {
								returnType = PrimitiveType.ANY;
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
							return new FunctionHeader(initialOffset, s, scope, true, nameStart, returnType, typeAnnotation);
					} else if (parser.peekAfterWhitespace() == '(')
						return new FunctionHeader(initialOffset, name, scope, false, nameStart, returnType, typeAnnotation);
			}
			parser.seek(initialOffset);
			return null;
		}
		public void apply(Function func) {
			func.setOldStyle(isOldStyle);
			func.setName(name);
			func.setVisibility(scope);
			func.assignType(returnType, returnType != null);
			if (typeAnnotation != null)
				typeAnnotation.setTarget(func);
		}
	}

	private List<VarInitialization> parseVariableDeclaration(boolean reportErrors, boolean checkForFinalSemicolon, Scope scope, Comment comment) throws ParsingException {
		if (scope == null)
			return null;
			
		final int backtrack = this.offset;

		List<VarInitialization> createdVariables = null;
		Function currentFunc = currentFunction();
		
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
				error(ParserErrorCode.VarOutsideFunction, backtrack-scope.toKeyword().length(), backtrack, NO_THROW|ABSOLUTE_MARKER_LOCATION, scope.toKeyword(), Keywords.GlobalNamed, Keywords.LocalNamed);
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
				if (isEngine || typing.allowsNonParameterAnnotations()) {
					staticType = parseTypeAnnotation(true, false);
					if (staticType != null) {
						typeAnnotation = parsedTypeAnnotation;
						eatWhitespace();
					}
					else if (typing == Typing.Static) {
						typeAnnotation = null;
						typeExpectedAt = this.offset;
						staticType = PrimitiveType.INT;
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
					Variable var = createVarInScope(varName, scope, bt, this.offset, comment);
					if (typeAnnotation != null)
						typeAnnotation.setTarget(var);
					if (staticType != null)
						var.assignType(staticType, true);
					if (parsedTypeAnnotation != null)
						parsedTypeAnnotation.setTarget(var);
					setCurrentDeclaration(var);
					VarInitialization varInitialization;
					ExprElm initializationExpression = null;
					{
						eatWhitespace();
						if (peek() == '=') {
							if (scope != Variable.Scope.CONST && currentFunc == null && !engine.settings().supportsNonConstGlobalVarAssignment)
								error(ParserErrorCode.NonConstGlobalVarAssignment, this.offset, this.offset+1, ABSOLUTE_MARKER_LOCATION|NO_THROW);
							read();
							eatWhitespace();

							// parse initialization value with all errors disabled so no false errors 
							boolean old = allErrorsDisabled;
							allErrorsDisabled |= !reportErrors;
							try {
								initializationExpression = parseExpression(reportErrors);
							} finally {
								allErrorsDisabled = old;
							}
							var.setInitializationExpression(initializationExpression);
						} else if (scope == Scope.CONST && !isEngine)
							error(ParserErrorCode.ConstantValueExpected, this.offset-1, this.offset, NO_THROW);
						else if (scope == Scope.STATIC && isEngine)
							var.forceType(PrimitiveType.INT); // most likely
					}
					varInitialization = new VarInitialization(varName, initializationExpression, bt-bodyOffset(), this.offset-bodyOffset(), var);
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
				error(ParserErrorCode.CommaOrSemicolonExpected, this.offset-1, this.offset, NO_THROW);
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
		error(ParserErrorCode.TypeExpected, typeExpectedAt, typeExpectedAt+1,  ABSOLUTE_MARKER_LOCATION|NO_THROW);
	}
	
	private Variable findVar(String name, Scope scope) {
		switch (scope) {
		case VAR:
			return currentFunction().findVariable(name);
		case CONST: case STATIC: case LOCAL:
			/*
			C4Declaration globalDeclaration = getContainer().getIndex() != null ? getContainer().getIndex().findGlobalDeclaration(name) : null;
			if (globalDeclaration instanceof C4Variable)
				return (C4Variable) globalDeclaration;
			// not yet in index - search locally
		case LOCAL: */
			return script().findLocalVariable(name, false);
		default:
			return null;
		}
	}
	
	public Variable createVarInScope(String varName, Scope scope, int start, int end, Comment description) {
		Variable result = findVar(varName, scope);
		if (result != null)
			return result;
		
		result = new Variable(varName, scope);
		switch (scope) {
		case PARAMETER:
			result.setParentDeclaration(currentFunction());
			currentFunction().parameters().add(result);
			break;
		case VAR:
			result.setParentDeclaration(currentFunction());
			currentFunction().localVars().add(result);
			break;
		case CONST: case STATIC: case LOCAL:
			result.setParentDeclaration(script());
			script().addDeclaration(result);
		}
		result.setLocation(absoluteSourceLocation(start, end));
		result.setUserDescription(description != null ? description.text().trim() : null);
		return result;
	}

	private IType parseTypeAnnotation(boolean topLevel, boolean required) throws ParsingException {
		if (topLevel)
			parsedTypeAnnotation = null;
		final int backtrack = this.offset;
		int start = this.offset;
		String str;
		IType t = null;
		if (peek() == '&') {
			if (!script.engine().settings().supportsRefs)
				error(ParserErrorCode.PrimitiveTypeNotSupported, this.offset, this.offset+1, ABSOLUTE_MARKER_LOCATION|NO_THROW,
					'&', script.engine().name());
			read();
			t = PrimitiveType.REFERENCE;
		}
		else if ((str = parseIdentifier()) != null || (parseID() && (str = parsedID.stringValue()) != null)) {
			PrimitiveType pt;
			t = pt = PrimitiveType.fromString(str, isEngine||typing==Typing.Static);
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
					error(ParserErrorCode.InvalidType, start, offset, NO_THROW|ABSOLUTE_MARKER_LOCATION, readStringAt(start, offset));
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
	private boolean parseFunctionDeclaration(FunctionHeader header) throws ParsingException {
		int endOfHeader;
		Comment desc = collectPrecedingComment(header.start);
		eatWhitespace();
		int startBody = 0, endBody = 0;
		
		setCurrentFunction(null);
		if (header.isOldStyle)
			warning(ParserErrorCode.OldStyleFunc, header.nameStart, header.nameStart+header.name.length(), 0);
		Function currentFunc;
		setCurrentFunction(currentFunc = newFunction(header.name));
		header.apply(currentFunc);
		currentFunc.setScript(script);
		if (header.scope == FunctionScope.GLOBAL)
			script.containsGlobals = true;
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
				Comment parameterCommentPre = parseCommentObject();
				eat(WHITESPACE_CHARS);
				Variable parm = parseParameter(currentFunc);
				eat(WHITESPACE_CHARS);
				Comment parameterCommentPost = parseCommentObject();
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
					error(ParserErrorCode.NameExpected, this.offset, offset+1, NO_THROW|ABSOLUTE_MARKER_LOCATION);
				parmExpected = false;
				int readByte = read();
				if (readByte == ')')
					break; // all parameters parsed
				else if (readByte == ',')
					parmExpected = true;
				else
					error(ParserErrorCode.UnexpectedToken, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, (char)readByte);  //$NON-NLS-1$//$NON-NLS-2$ 
			} while(!reachedEOF());
		}
		endOfHeader = this.offset;
		lastComment = null;
		eatWhitespace();
		if (lastComment != null)
			currentFunc.setUserDescription(lastComment.text());
		
		// check initial opening bracket which is mandatory for NET2 funcs
		int token = read();
		int blockDepth = 0;
		boolean parseBody = true;
		if (token != '{')
			if (isEngine) {
				if (token != ';')
					tokenExpectedError(";"); //$NON-NLS-1$
				else
					parseBody = false;
			} else if (!header.isOldStyle)
				tokenExpectedError("{"); //$NON-NLS-1$
			else {
				this.seek(endOfHeader);
				blockDepth = -1;
			}
		
		// body
		if (parseBody) {
			startBody = this.offset;
			boolean properEnd = false;
			do {
				if (header.isOldStyle)
					endBody = this.offset;
				eatWhitespace();
				int offsetBeforeToken = this.offset;
				String word;
				Scope scope;
				if (FunctionHeader.parse(this, header.isOldStyle) != null || reachedEOF()) {
					if (header.isOldStyle)
						seek(endBody);
					properEnd = true;
					if (blockDepth != -1)
						error(ParserErrorCode.MissingBrackets, header.nameStart, header.nameStart+header.name.length(), NO_THROW, blockDepth+1, '}');
					seek(offsetBeforeToken);
				}
				else if (
					(word = parseIdentifier()) != null &&
					(scope = Scope.makeScope(word)) != null &&
					scope != Scope.VAR &&
					parseVariableDeclaration(false, false, scope, collectPrecedingComment(offsetBeforeToken)) != null
				)
					/* boy, am i glad to have parsed this variable declaration */;
				else if (parseString() == null) {
					int c = read();
					if (c == '{')
						blockDepth++;
					else if (c == '}')
						properEnd = --blockDepth == -1 && !header.isOldStyle;
				}
			} while (!properEnd && !reachedEOF());
			if (!header.isOldStyle) {
				endBody = this.offset;
				if (properEnd)
					endBody--;
			}
			if (!properEnd && !(header.isOldStyle && reachedEOF())) {
				int pos = Math.min(this.offset, this.size-1);
				error(ParserErrorCode.TokenExpected, pos-bodyOffset(), pos+1-bodyOffset(), NO_THROW, "}"); //$NON-NLS-1$
				return false;
			}
			currentFunc.setBodyLocation(startBody != -1 ? absoluteSourceLocation(startBody, endBody) : null);
		} else
			currentFunc.setBodyLocation(null);
		eatWhitespace();
		if (desc != null)
			desc.applyDocumentation(currentFunc);
		else {
			// look for comment in the same line as the closing '}' which is common for functions packed into one line
			// hopefully there won't be multi-line functions with such a comment attached at the end
			Comment c = commentImmediatelyFollowing();
			if (c != null)
				currentFunc.setUserDescription(c.text());
		}

		// finish up
		currentFunc.setLocation(absoluteSourceLocation(header.nameStart, header.nameStart+header.name.length()));
		currentFunc.setHeader(absoluteSourceLocation(header.start, endOfHeader));
		Function existingFunction = script.findLocalFunction(currentFunc.name(), false);
		if (existingFunction != null && existingFunction.isGlobal() == currentFunc.isGlobal())
			warning(ParserErrorCode.DuplicateDeclaration, currentFunc.location(), ABSOLUTE_MARKER_LOCATION, currentFunc.name());
		script.addDeclaration(currentFunc);
		if (!currentFunc.isOldStyle())
			setCurrentFunction(null); // to not suppress errors in-between functions
		return true;
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
		Comment c = (lastComment != null && lastComment.precedesOffset(absoluteOffset, buffer())) ? lastComment : null; 
		lastComment = null;
		return c;
	}
	
	private String textOfInlineComment() {
		int pos = this.offset;
		this.eat(BufferedScanner.WHITESPACE_WITHOUT_NEWLINE_CHARS);
		if (this.eat(BufferedScanner.NEWLINE_CHARS) == 0) {
			Comment c = parseCommentObject();
			if (c != null)
				return c.text();
		}
		this.seek(pos);
		return null;
	}

	private boolean parseHexNumber() throws ParsingException {
		int offset = this.offset;
		boolean isHex = read() == '0' && read() == 'x';
		if (!isHex)
			this.seek(offset);
		else {
			offset += 2;
			int count = 0;
			if (isHex)
				do {
					int readByte = read();
					if (('0' <= readByte && readByte <= '9') || ('A' <= readByte && readByte <= 'F') ||  ('a' <= readByte && readByte <= 'f'))  {
						count++;
						continue;
					}
					else {
						unread();
						if (count > 0) {
							this.seek(offset);
							parsedNumber = Long.parseLong(this.readString(count), 16);
							this.seek(offset+count);
						} else {
							parsedNumber = -1; // unlikely to be parsed
							return false; // well, this seems not to be a number at all
						} 
						return true;
					}
				} while(!reachedEOF());
		}
		return isHex;
	}
	
	private boolean parseNumber() throws ParsingException {
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
				else {
					parsedNumber = -1; // unlikely to be parsed
					return false; // well, this seems not to be a number at all
				} 
			}
		} while(!reachedEOF());
		this.seek(offset);
		String numberString = this.readString(count);
		if (floatingPoint)
			try {
				parsedNumber = Double.parseDouble(numberString);
			} catch (NumberFormatException e) {
				parsedNumber = Double.MAX_VALUE;
				error(ParserErrorCode.NotANumber, offset, offset+count, NO_THROW, numberString);
			}
		else
			try {
				parsedNumber = Long.parseLong(numberString);
			} catch (NumberFormatException e) {
				parsedNumber = Integer.MAX_VALUE;
				error(ParserErrorCode.NotANumber, offset, offset+count, NO_THROW, numberString);
			}
		this.seek(offset+count);
		return true;
	}
	
	private boolean parseEllipsis() {
		int offset = this.offset;
		String e = this.readString(3);
		if (e != null && e.equals("...")) //$NON-NLS-1$
			return true;
		this.seek(offset);
		return false;
	}
	
	private boolean parseMemberOperator() throws ParsingException {
		int savedOffset = this.offset;
		int firstChar = read();
		if (firstChar == '.') {
			parsedMemberOperator = "."; //$NON-NLS-1$
			return true;
		}
		else if (firstChar == '-')
			if (read() == '>') {
				savedOffset = this.offset;
				eatWhitespace();
				if (read() == '~')
					parsedMemberOperator = "->~"; //$NON-NLS-1$
				else {
					parsedMemberOperator = "->"; //$NON-NLS-1$
					this.seek(savedOffset);
				}
				return true;
			}
		this.seek(savedOffset);
		return false;
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

	private boolean enableError(ParserErrorCode error, boolean doEnable) {
		boolean result = errorEnabled(error);
		if (doEnable)
			disabledErrors.remove(error);
		else
			disabledErrors.add(error);
		return result;
	}
	
	private void enableErrors(EnumSet<ParserErrorCode> set, boolean doEnable) {
		if (doEnable)
			disabledErrors.removeAll(set);
		else
			disabledErrors.addAll(set);
	}
	
	/**
	 * Get error enabled status.
	 * @param error The error to check the enabled status of
	 * @return Return whether the error is enabled. 
	 */
	public boolean errorEnabled(ParserErrorCode error) {
		return !(allErrorsDisabled || disabledErrors.contains(error) || errorsDisabledByProjectSettings.contains(error));
	}
	
	public void warning(ParserErrorCode code, int errorStart, int errorEnd, int flags, Object... args) {
		try {
			marker(code, errorStart, errorEnd, flags|NO_THROW, IMarker.SEVERITY_WARNING, args);
		} catch (ParsingException e) {
			// won't happen
		}
	}
	public void warning(ParserErrorCode code, IRegion region, int flags, Object... args) {
		warning(code, region.getOffset(), region.getOffset()+region.getLength(), flags, args);
	}
	public void error(ParserErrorCode code, IRegion errorRegion, int flags, Object... args) throws ParsingException {
		error(code, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), flags, args);
	}
	public void error(ParserErrorCode code, int errorStart, int errorEnd, int flags, Object... args) throws ParsingException {
		marker(code, errorStart, errorEnd, flags, IMarker.SEVERITY_ERROR, args);
	}
	
	public static final int NO_THROW = 1;
	public static final int ABSOLUTE_MARKER_LOCATION = 2;
	
	public static class Marker {
		public ParserErrorCode code;
		public int start, end;
		public int severity;
		public Object[] args;
		
		private final Declaration cf;
		private final int offset;
		private final ExprElm reporter;
		private final IFile scriptFile;
		private final Script container;
		
		public Marker(C4ScriptParser parser, ParserErrorCode code, int start, int end, int severity, Object[] args) {
			super();
			this.code = code;
			this.start = start;
			this.end = end;
			this.severity = severity;
			this.args = args;
			
			this.cf = parser.currentDeclaration();
			this.offset = parser.offset;
			this.reporter = parser.expressionReportingErrors();
			this.scriptFile = parser.scriptFile;
			this.container = parser.script();
		}
		public IMarker deploy() {
			IMarker result = code.createMarker(scriptFile, container, Core.MARKER_C4SCRIPT_ERROR, start, end, severity, reporter, args);
			if (cf != null)
				ParserErrorCode.setDeclarationTag(result, cf.makeNameUniqueToParent());
			IRegion exprLocation = reporter;
			if (exprLocation != null)
				ParserErrorCode.setExpressionLocation(result, exprLocation);
			//result.getAttribute(IMarker.MESSAGE, "<Fail>"); //$NON-NLS-1$
			return result;
		}
		@Override
		public String toString() {
			return String.format("%s @(%d, %s)", code.toString(), offset, reporter.toString()); //$NON-NLS-1$
		}
	}
	
	public static class Markers extends LinkedList<Marker> {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public void deploy() {
			if (Core.instance().runsHeadless())
				return;
			final List<Marker> markersToDeploy;
			synchronized (this) {
				markersToDeploy = new ArrayList<Marker>(this);
				this.clear();
			}
			for (Marker m : markersToDeploy)
				m.deploy();
		}
		@Override
		public boolean add(Marker e) {
			synchronized (this) {
				return super.add(e);
			}
		}
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
		else
			return markers = new Markers();
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
	public void marker(ParserErrorCode code, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ParsingException {
		if (!errorEnabled(code))
			return;
		// C4ScriptST: Incompatible type warnings always errors
		if (typing == Typing.Static && code == ParserErrorCode.IncompatibleTypes)
			severity = IMarker.SEVERITY_ERROR; 
		if ((flags & ABSOLUTE_MARKER_LOCATION) == 0) {
			int offs = bodyOffset();
			markerStart += offs;
			markerEnd += offs;
		}
		Function cf = currentFunction();
		boolean misplacedErrorOrNoFileToAttachMarkerTo =
			scriptFile == null ||
			(cf != null && !cf.isOldStyle() && cf.bodyLocation() != null && markerStart > cf.bodyLocation().end()+1);
		String problem = code.makeErrorString(args);
		if (!misplacedErrorOrNoFileToAttachMarkerTo) {
			Markers markers = markers();
			synchronized (markers) {
				markers.add(new Marker(this, code, markerStart, markerEnd, severity, args));
			}
		}
		if ((flags & NO_THROW) == 0 && severity >= IMarker.SEVERITY_ERROR)
			throw misplacedErrorOrNoFileToAttachMarkerTo
				? new SilentParsingException(Reason.SilenceRequested, problem, this)
				: new ParsingException(problem, this);
	}
	
	public IMarker todo(String todoText, int markerStart, int markerEnd, int priority) {
		if (scriptFile != null)
			try {
				IMarker marker = scriptFile.createMarker(IMarker.TASK);
				marker.setAttribute(IMarker.CHAR_START, markerStart+bodyOffset());
				marker.setAttribute(IMarker.CHAR_END, markerEnd+bodyOffset());
				marker.setAttribute(IMarker.MESSAGE, todoText);
				marker.setAttribute(IMarker.LOCATION, currentDeclaration() != null ? currentDeclaration().qualifiedName() : ""); //$NON-NLS-1$
				marker.setAttribute(IMarker.PRIORITY, priority);
				return marker;
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		else
			return null;
	}
	
	private void tokenExpectedError(String token) throws ParsingException {
		int off = this.offset;
		while (off >= 0 && off < size && buffer.charAt(off) == '\t')
			off--;
		error(ParserErrorCode.TokenExpected, off, off+1, ABSOLUTE_MARKER_LOCATION, token);
	}
	
	private boolean parseStaticFieldOperator() {
		final int offset = this.offset;
		String o = this.readString(2);
		if (o != null && o.equals("::")) //$NON-NLS-1$
			return true;
		this.seek(offset);
		return false;
	}
	
	private ExprElm parseSequence(boolean reportErrors) throws ParsingException {
		int sequenceParseStart = this.offset;
		eatWhitespace();
		int sequenceStart = this.offset;
		Operator preop = parseOperator();
		ExprElm result = null;
		if (preop != null && preop.isPrefix()) {
			ExprElm followingExpr = parseSequence(reportErrors);
			if (followingExpr == null) {
				error(ParserErrorCode.ExpressionExpected, this.offset, this.offset+1, NO_THROW);
				followingExpr = placeholderExpression(offset);
			}
			result = new UnaryOp(preop, UnaryOp.Placement.Prefix, followingExpr);
			setExprRegionRelativeToFuncBody(result, sequenceStart, this.offset);
			return result;
		} else
			this.seek(sequenceStart); // don't skip operators that aren't prefixy
		ArrayList<ExprElm> elements = new ArrayList<ExprElm>(5);
		ExprElm elm;
		ExprElm prevElm = null;
		int noWhitespaceEating = sequenceStart;
		boolean proper = true;
		boolean noNewProplist = false;
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
			if (elements.size() > 0 && Keywords.In.equals(readIdent())) {
				this.seek(elmStart);
				break;
			}
			this.seek(elmStart); // nothing special to end the sequence; make sure we start from the beginning
			
			// hex number
			if (elm == null && parseHexNumber())
				//				if (parsedNumber < Integer.MIN_VALUE || parsedNumber > Integer.MAX_VALUE)
//					warningWithCode(ErrorCode.OutOfIntRange, elmStart, fReader.getPosition(), String.valueOf(parsedNumber));
				elm = new LongLiteral(parsedNumber.longValue(), true);
			
			// id
			if (elm == null && parseID())
				elm = new IDLiteral(parsedID);
			
			// number
			if (elm == null && parseNumber())
				elm = NumberLiteral.from(parsedNumber);
			
			// variable or function
			if (elm == null) {
				String word = readIdent();
				if (word != null && word.length() > 0)
					// tricky new keyword parsing that also respects use of new as regular identifier
					if (!noNewProplist && word.equals(Keywords.New)) {
						// don't report errors here since there is the possibility that 'new' will be interpreted as variable name in which case this expression will be parsed again
						ExprElm prototype = parseExpression(OPENING_BLOCK_BRACKET_DELIMITER, false);
						boolean treatNewAsVarName = false;
						if (prototype == null)
							treatNewAsVarName = true;
						else {
							prototype.setFinishedProperly(true); // :/
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
							elm = ExprElm.NULL_EXPR; // just to satisfy loop condition
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
							List<ExprElm> args = new LinkedList<ExprElm>();
							parseRestOfTuple(args, reportErrors);
							CallDeclaration callFunc = new CallDeclaration(word, args.toArray(new ExprElm[args.size()]));
							callFunc.setParmsRegion(s-bodyOffset(), this.offset-1-bodyOffset());
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
				if (parseMemberOperator()) {
					int idStart = this.offset;
					int idOffset;
					eatWhitespace();
					idOffset = offset;
					if (parseID() && eatWhitespace() >= 0 && parseStaticFieldOperator())
						idOffset -= fieldOperatorStart;
					else {
						parsedID = null; // reset because that call could have been successful (GetX would be recognized as id)
						seek(idStart);
						idOffset = 0;
					}
					elm = new MemberOperator(parsedMemberOperator.length() == 1, parsedMemberOperator.length() == 3, parsedID, idOffset);
				}
			}

			// (<expr>)
			if (elm == null) {
				int parenthStartPos = this.offset;
				int c = read();
				if (c == '(') {
					if (prevElm != null) {
						// CallExpr
						List<ExprElm> tupleElms = new LinkedList<ExprElm>();
						parseRestOfTuple(tupleElms, reportErrors);
						elm = new CallExpr(tupleElms.toArray(new ExprElm[tupleElms.size()]));
					} else {
						ExprElm firstExpr = parseExpression(reportErrors);
						if (firstExpr == null) {
							firstExpr = ExprElm.nullExpr(this.offset, 0, this);
							// might be disabled
							error(ParserErrorCode.EmptyParentheses, parenthStartPos, this.offset+1, NO_THROW|ABSOLUTE_MARKER_LOCATION);
						}
						eatWhitespace();
						c = read();
						if (c == ')')
							elm = new Parenthesized(firstExpr);
						else if (c == ',') {
							error(ParserErrorCode.TuplesNotAllowed, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION);
							// tuple (just for multiple parameters for return)
							List<ExprElm> tupleElms = new LinkedList<ExprElm>();
							tupleElms.add(firstExpr);
							parseRestOfTuple(tupleElms, reportErrors);
							elm = new Tuple(tupleElms.toArray(new ExprElm[0]));
						} else
							tokenExpectedError(")"); //$NON-NLS-1$
					}
				} else
					unread();
			}
			
			String placeholder;
			if (elm == null && (placeholder = parsePlaceholderString()) != null)
				elm = makePlaceholder(placeholder);
			
			// §{...}
			if (elm == null)
				elm = parseWildcard();
			
			// check if sequence is valid (CreateObject(BLUB)->localvar is not)
			if (elm != null)
				if (!elm.isValidInSequence(prevElm, this)) {
					elm = null; // blub blub <- first blub is var; second blub is not part of the sequence -.-
					proper = false;
				} else {
					// add to sequence even if not valid so the quickfixer can separate them
					setExprRegionRelativeToFuncBody(elm, elmStart, this.offset);
					elements.add(elm);
					prevElm = elm;
				}
			
			noNewProplist = false;

		} while (elm != null);
		this.seek(noWhitespaceEating);
		ExprElm lastElm;
		if (elements.size() == 1) {
			// no need for sequences containing one element
			result = elements.get(elements.size()-1);
			lastElm = result;
		} else if (elements.size() > 1) {
			result = new Sequence(elements.toArray(new ExprElm[0]));
			lastElm = elements.get(elements.size()-1);
		} else {
			result = null;
			lastElm = null;
		}
		if (result != null) {
			proper &= lastElm == null || lastElm.isValidAtEndOfSequence(this);
			result.setFinishedProperly(proper);
			setExprRegionRelativeToFuncBody(result, sequenceStart, this.offset);
			if (proper) {
				int saved = this.offset;
				eatWhitespace();
				Operator postop = parseOperator();
				if (postop != null && postop.isPostfix()) {
					UnaryOp op = new UnaryOp(postop, UnaryOp.Placement.Postfix, result);
					setExprRegionRelativeToFuncBody(op, result.start()+bodyOffset(), this.offset);
					return op;
				} else
					// a binary operator following this sequence
					this.seek(saved);
			}
		} else
			this.seek(sequenceParseStart);
		
		return result;
		
	}
	
	protected Placeholder makePlaceholder(String placeholder) throws ParsingException {
		return new Placeholder(placeholder);
	}

	private ExprElm parsePropListExpression(boolean reportErrors, ExprElm prevElm) throws ParsingException {
		ProplistDeclaration proplDec = parsePropListDeclaration(reportErrors);
		if (proplDec != null) {
			ExprElm elm = new PropListExpression(proplDec);
			if (currentFunction() != null)
				currentFunction().addOtherDeclaration(proplDec);
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
			proplistDeclaration.setParentDeclaration(currentDeclaration() != null ? currentDeclaration() : script);
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
							error(ParserErrorCode.UnexpectedToken, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, ","); //$NON-NLS-1$
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
								error(ParserErrorCode.UnexpectedToken, this.offset, this.offset+1, ABSOLUTE_MARKER_LOCATION, (char)read());
							}
							eatWhitespace();
							Variable v = new Variable(name, currentFunction() != null ? Scope.VAR : Scope.LOCAL);
							v.setLocation(absoluteSourceLocation(nameStart, nameEnd));
							Declaration outerDec = currentDeclaration();
							setCurrentDeclaration(v);
							ExprElm value = null;
							try {
								v.setParentDeclaration(outerDec);
								value = parseExpression(COMMA_OR_CLOSE_BLOCK, reportErrors);
								if (value == null) {
									error(ParserErrorCode.ValueExpected, offset-1, offset, NO_THROW);
									value = placeholderExpression(offset);
								}
								v.setInitializationExpression(value);
								v.forceType(value.type(this));
							} finally {
								setCurrentDeclaration(outerDec);
							}
							proplistDeclaration.addComponent(v, false);
							expectingComma = true;
						}
						else {
							error(ParserErrorCode.TokenExpected, this.offset, this.offset+1, ABSOLUTE_MARKER_LOCATION, Messages.TokenStringOrIdentifier);
							break;
						}
					}
				}
				if (!properlyClosed)
					error(ParserErrorCode.MissingClosingBracket, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, "}"); //$NON-NLS-1$
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
	
	@Override
	public SourceLocation absoluteSourceLocationFromExpr(ExprElm expression) {
		int bodyOffset = bodyOffset();
		return absoluteSourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
	}

	private ExprElm parseArrayExpression(boolean reportErrors, ExprElm prevElm) throws ParsingException {
		ExprElm elm = null;
		int c = read();
		if (c == '[') {
			if (prevElm != null) {
				// array access
				ExprElm arg = parseExpression(reportErrors);
				eatWhitespace();
				int t;
				switch (t = read()) {
				case ':':
					ExprElm arg2 = parseExpression(reportErrors);
					eatWhitespace();
					expect(']');
					elm = new ArraySliceExpression(arg, arg2);
					break;
				case ']':
					elm = new ArrayElementExpression(arg);
					break;
				default:
					error(ParserErrorCode.UnexpectedToken, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, (char)t);
				}
			} else {
				// array creation
				Vector<ExprElm> arrayElms = new Vector<ExprElm>(10);
				boolean properlyClosed = false;
				boolean expectingComma = false;
				while (!reachedEOF()) {
					eatWhitespace();
					c = read();
					if (c == ',') {
						if (!expectingComma)
							arrayElms.add(null);
						expectingComma = false;
					} else if (c == ']') {
						properlyClosed = true;
						break;
					} else {
						unread();
						ExprElm arrayElement = parseExpression(COMMA_OR_CLOSE_BRACKET, reportErrors);
						if (arrayElement != null)
							arrayElms.add(arrayElement);
						else {
							properlyClosed = false;
							break;
						}
						expectingComma = true;
					}
				}
				if (!properlyClosed)
					error(ParserErrorCode.MissingClosingBracket, this.offset, this.offset+1, NO_THROW, "]"); //$NON-NLS-1$
				elm = new ArrayExpression(arrayElms.toArray(new ExprElm[0]));
			}
		} else
			unread();
		return elm;
	}

	private void parseRestOfTuple(List<ExprElm> listToAddElementsTo, boolean reportErrors) throws ParsingException {
		boolean expectingComma = false;
		Loop: while (!reachedEOF()) {
			eatWhitespace();
			switch (read()) {
			case ')':
				if (!expectingComma && listToAddElementsTo.size() > 0)
					listToAddElementsTo.add(ExprElm.nullExpr(this.offset, 0, this));
				break Loop;
			case ',':
				if (!expectingComma)
					listToAddElementsTo.add(ExprElm.nullExpr(this.offset, 0, this));
				expectingComma = false;
				break;
			default:
				unread();
				if (listToAddElementsTo.size() > 100)
					error(ParserErrorCode.InternalError, this.offset, this.offset, 0, Messages.InternalError_WayTooMuch);
				//	break;
				ExprElm arg = parseTupleElement(reportErrors);
				if (arg == null) {
					error(ParserErrorCode.ExpressionExpected, this.offset, this.offset+1, NO_THROW);
					break Loop;
				} else {
					if (arg instanceof SimpleStatement) {
						arg = ((SimpleStatement)arg).expression();
						arg.setParent(null);
					}
					arg.setFinishedProperly(true);
					listToAddElementsTo.add(arg);
				}
				expectingComma = true;
			}
		}
	}
	
	protected ExprElm parseTupleElement(boolean reportErrors) throws ParsingException {
		return parseExpression(reportErrors);
	}
	
	private ExprElm parseExpression(char[] delimiters, boolean reportErrors) throws ParsingException {
		
		final int offset = this.offset;
		
		final int START = 0;
		final int OPERATOR = 1;
		final int SECONDOPERAND = 2;
		final int DONE = 3;

		parseExpressionRecursion++;
		try {

			ExprElm root = null;
			ExprElm current = null;
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
						if (root == null || root.isFinishedProperly()) {
							current = root;
							state = current != null ? OPERATOR : DONE;
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
								ExprElm newLeftSide = null;
								BinaryOp theOp = null;
								for (ExprElm opFromBottom = current.parent(); opFromBottom instanceof BinaryOp; opFromBottom = opFromBottom.parent()) {
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
								setExprRegionRelativeToFuncBody(lastOp, operatorStartPos, this.offset);
								state = SECONDOPERAND;
							} else {
								this.seek(operatorStartPos); // in case there was an operator but not a binary one
								state = DONE;
							}
						}
						break;
					case SECONDOPERAND:
						ExprElm rightSide = parseSequence(reportErrors);
						if (rightSide == null) {
							error(ParserErrorCode.OperatorNeedsRightSide, lastOp, NO_THROW);
							rightSide = placeholderExpression(offset);
						}
						((BinaryOp)current).setRightSide(rightSide);
						lastOp = (BinaryOp)current;
						current = rightSide;
						state = OPERATOR;
						break;
					}
			}
			if (root != null) {
				setExprRegionRelativeToFuncBody(root, exprStart, this.offset);
				// potentially throwing exceptions and stuff
				handleExpressionCreated(reportErrors, root);
			}
			
			return root;

		} finally {
			parseExpressionRecursion--;
		}
	}

	private ExprElm placeholderExpression(final int offset) {
		ExprElm result = new ExprElm();
		setExprRegionRelativeToFuncBody(result, offset, offset+1);
		return result;
	}

	/**
	 * The expression that is currently reporting errors.
	 * @return The expression reporting errors
	 */
	public ExprElm expressionReportingErrors() {
		return problemReporter;
	}
	
	/**
	 * Convert a region relative to the body offset of the current function to a script-absolute region.
	 * @param flags 
	 * @param region The region to convert
	 * @return The relative region or the passed region, if there is no current function.
	 */
	public IRegion convertRelativeRegionToAbsolute(int flags, IRegion region) {
		int offset = bodyOffset();
		if (offset == 0 || (flags & ABSOLUTE_MARKER_LOCATION) == 0)
			return region;
		else
			return new Region(offset+region.getOffset(), region.getLength());
	}

	private final void handleExpressionCreated(boolean reportErrors, ExprElm root) throws ParsingException {
		root.setAssociatedDeclaration(currentDeclaration());
		root.setFlagsEnabled(ExprElm.STATEMENT_REACHED, statementReached);
		if (listener != null && parseExpressionRecursion <= 1)
			listener.visitExpression(root, this);
	}
	
	/**
	 * Let an expression report errors. Calling {@link ExprElm#reportProblems(C4ScriptParser)} indirectly like that ensures
	 * that error markers created will be decorated with information about the expression reporting the error.
	 * @param expression The expression to report errors.
	 * @throws ParsingException
	 * @return The expression parameter is returned to allow for expression chaining. 
	 */
	public <T extends ExprElm> T reportProblemsOf(T expression, boolean recursive) {
		if (expression == null)
			return null;
		ExprElm saved = problemReporter;
		problemReporter = expression;
		try {
			if (recursive && !expression.skipReportingProblemsForSubElements())
				for (ExprElm e : expression.subElements())
					if (e != null)
						reportProblemsOf(e, true);
			try {
				expression.reportProblems(this);
			} catch (Exception s) {
				// silent
			}
		} finally {
			problemReporter = saved;
		}
		return expression;
	}

	public void reportProblemsOf(Iterable<Statement> statements, boolean onlyTypeLocals) {
		newTypeEnvironment();
		Function f = currentFunction();
		if (f != null)
			for (Variable p : f.parameters())
				if (p.type() == PrimitiveType.UNKNOWN) {
					ITypeInfo varTypeInfo = requestTypeInfo(new AccessVar(p));
					if (varTypeInfo != null)
						varTypeInfo.storeType(p.parameterType());
				}
		for (Statement s : statements)
			reportProblemsOf(s, true);
		typeEnvironments.apply(this, onlyTypeLocals);
		endTypeEnvironment(true, true);
		warnAboutPossibleProblemsWithFunctionLocalVariables(currentFunction(), statements);
	}
	
	private final Object reportingMonitor = new Object();

	public void reportProblems() {
		newTypeEnvironment();
		for (Variable v : script.variables())
			synchronized (reportingMonitor) {
				setCurrentFunction(null);
				ExprElm init = v.initializationExpression();
				if (init != null) {
					Function owningFunc = as(init.owningDeclaration(), Function.class);
					if (owningFunc == null) {
						newTypeEnvironment();
						reportProblemsOf(init, true);
						new AccessVar(v).expectedToBeOfType(init.type(this), this, TypeExpectancyMode.Force);
						endTypeEnvironment(true, true);
					}
					if (v.scope() == Scope.CONST && !init.isConstant())
						try {
							error(ParserErrorCode.ConstantValueExpected, owningFunc == null ? init : owningFunc.bodyLocation().add(init), ABSOLUTE_MARKER_LOCATION|NO_THROW, v.name());
						} catch (ParsingException e) {}
				}
			}
		for (Function f : script.functions())
			synchronized (reportingMonitor) {
				setCurrentFunction(f);
				reportProblemsOfFunction(f);
			}
		typeEnvironments.apply(this, false);
		for (Variable v : script.variables())
			if (v.scope() == Scope.CONST)
				v.lockType();
		endTypeEnvironment(false, false);
	}
	
	@Override
	public void reportProblems(Function function) {
		if (builder == null)
			return;
		if (function == null || function.isEngineDeclaration())
			return;
		synchronized (reportingMonitor) {
			statementReached = true;
			reportProblemsOfFunction(function);
		}
	}

	private void reportProblemsOfFunction(Function function) {
		if (function == null || function.body() == null)
			return;
		if (function.script() == script) {
			assignDefaultParmTypesToFunction(function);
			if (builder != null) {
				Set<Function> reporters = builder.problemReporters();
				if (reporters != null)
					synchronized (reporters) {
						if (reporters.contains(function))
							return;
						else
							reporters.add(function);
					}
				else
					return;
			}
			synchronized (reportingMonitor) {
				Function old = currentFunction();
				setCurrentFunction(function);
				reportProblemsOf(iterable(function.body().statements()), false);
				setCurrentFunction(old);
			}
		} else if (builder != null) {
			C4ScriptParser other = builder.parserFor(function.script());
			if (other != null)
				other.reportProblemsOfFunction(function);
		}
	}

	private ExprElm parseExpression(boolean reportErrors) throws ParsingException {
		return parseExpression(SEMICOLON_DELIMITER, reportErrors);
	}
	
	private ExprElm parseExpression() throws ParsingException {
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
				error(ParserErrorCode.StringNotClosed, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION);
				break Loop;
			case '"':
				if (!escaped) {
					properEnd = true;
					break Loop;
				}
				break;
			case '\n': case '\r':
				error(ParserErrorCode.StringNotClosed, this.offset-1, this.offset, NO_THROW|ABSOLUTE_MARKER_LOCATION);
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
		parseStatementRecursion++;
		try {
			
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
			result = parseCommentObject();

			if (result == null) {
				String readWord;
				// new oldstyle-func begun
				if (currentFunction() != null && currentFunction().isOldStyle() && FunctionHeader.parse(this, true) != null)
					result = null;
				else if ((readWord = readIdent()) == null || readWord.length() == 0) {
					int read = read();
					if (read == '{' && !options.contains(ParseStatementOption.InitializationStatement)) {
						List<Statement> subStatements = new LinkedList<Statement>();
						parseStatementBlock(start, Integer.MAX_VALUE, subStatements, ParseStatementOption.NoOptions, VisitCodeFlavour.AlsoStatements);
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
						ExprElm expression = parseExpression();
						if (expression != null) {
							result = new SimpleStatement(expression);
							if (!options.contains(ParseStatementOption.InitializationStatement))
								result = statementNeedingSemicolon(result);
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
							int rewind = this.offset;
							eatWhitespace();
							if (read() != ';') {
								seek(rewind);
								result.setFinishedProperly(false);
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
				ExprElm expression = parseExpression();
				int afterExpression = this.offset;
				if (expression != null) {
					result = new SimpleStatement(expression);
					if (expression.isFinishedProperly() && !options.contains(ParseStatementOption.InitializationStatement)) {
						int beforeWhitespace = this.offset;
						eatWhitespace();
						if (read() != ';') {
							result.setFinishedProperly(false);
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
				
				setExprRegionRelativeToFuncBody(result, start, this.offset);
				handleStatementCreated(result);
			}
			return result;
		} finally {
			parseStatementRecursion--;
		}
		

	}

	private void handleStatementCreated(Statement statement) throws ParsingException {
		statement.setFlagsEnabled(ExprElm.STATEMENT_REACHED, statementReached);
		if (parseStatementRecursion == 1)
			if (listener != null)
				switch (listener.visitExpression(statement, this)) {
				case Cancel:
					listener = null; // listener doesn't want to hear from me anymore? fine!
					//throw new SilentParsingException(Reason.Cancellation, "Expression Listener Cancellation"); //$NON-NLS-1$
				default:
					break;
				}
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
	private void parseStatementBlock(int start, int endOfFunc, List<Statement> statements, EnumSet<ParseStatementOption> options, VisitCodeFlavour flavour) throws ParsingException {
		boolean done = false;
		boolean reached = true;
		int garbageStart = -1;
		boolean oldStatementReached = this.statementReached;
		while (!reachedEOF() && this.offset < endOfFunc) {
			this.statementReached = reached;
			int potentialGarbageEnd = offset;
			//eatWhitespace();
			Statement statement = flavour == VisitCodeFlavour.AlsoStatements
				? parseStatement(options)
				: SimpleStatement.wrapExpression(parseExpression());
			if (statement == null) {
				Function currentFunction = currentFunction();
				done = currentFunction == null || currentFunction.isOldStyle() || peek() == '}';
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
			if (reached)
				reached = statement.controlFlow() == ControlFlow.Continue;
			// after first 'real' statement don't expect function description anymore
			if (!statementIsComment)
				options.remove(ParseStatementOption.ExpectFuncDesc);
		}
		if (garbageStart != -1)
			// contains only garbage ... still add
			maybeAddGarbageStatement(statements, garbageStart, offset);
		if (!done) {
			if (this.offset < endOfFunc)
				error(ParserErrorCode.BlockNotClosed, start, start+1, NO_THROW);
		} else
			read(); // should be }
		this.statementReached = oldStatementReached;
	}

	private int maybeAddGarbageStatement(List<Statement> statements,
			int garbageStart, int potentialGarbageEnd) throws ParsingException {
		String garbageString = buffer.substring(garbageStart, Math.min(potentialGarbageEnd, buffer.length()));
		garbageString = modifyGarbage(garbageString);
		if (garbageString != null && garbageString.length() > 0) {
			GarbageStatement garbage = new GarbageStatement(garbageString, garbageStart-bodyOffset());
			garbageStart = -1;
			statements.add(garbage);
			handleExpressionCreated(true, garbage);
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
				c = parseCommentObject();
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

	private <T extends Statement> T statementNeedingSemicolon(T statement) {
		statement.setFinishedProperly(parseSemicolonOrReturnFalse());
		return statement;
	}
	
	/**
	 * Parse a statement that is initiated with a keyword. This includes for/while/do while loops,
	 * loop control flow statements (break/continue) and return.
	 * @param keyWord The keyword that has already been parsed and decides on the kind of statement to parse.
	 * @return The parsed KeywordStatement or null if the keyword was not recognized
	 * @throws ParsingException
	 */
	private KeywordStatement parseKeyword(String keyWord) throws ParsingException {
		KeywordStatement result = null;
		if (keyWord.equals(Keywords.If))
			result = parseIf();
		else if (keyWord.equals(Keywords.While))
			result = parseWhile();
		else if (keyWord.equals(Keywords.Do))
			result = parseDoWhile();
		else if (keyWord.equals(Keywords.For))
			result = parseFor();
		else if (keyWord.equals(Keywords.Continue)) {
			result = statementNeedingSemicolon(new ContinueStatement());
			if (currentLoop == null)
				result.setFlagsEnabled(ExprElm.MISPLACED, true);
		}
		else if (keyWord.equals(Keywords.Break)) {
			result = statementNeedingSemicolon(new BreakStatement());
			if (currentLoop == null)
				result.setFlagsEnabled(ExprElm.MISPLACED, true);
		}
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
	private ReturnStatement parseReturn() throws ParsingException {
		ReturnStatement result;
		eatWhitespace();
		ExprElm returnExpr;
		if (peek() == ';')
			returnExpr = null;
		else {
			enableError(ParserErrorCode.TuplesNotAllowed, false);
			if (strictLevel() < 2)
				enableError(ParserErrorCode.EmptyParentheses, false);
			returnExpr = parseExpression();
			if (returnExpr == null)
				error(ParserErrorCode.ValueExpected, this.offset, this.offset+1, NO_THROW);
			enableError(ParserErrorCode.TuplesNotAllowed, true);
			enableError(ParserErrorCode.EmptyParentheses, true);
		}
		result = new ReturnStatement(returnExpr);
		result.setFinishedProperly(parseSemicolonOrReturnFalse());
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
		ExprElm cond = parseExpression();
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
		ExprElm arrayExpr, condition, increment;
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
					setExprRegionRelativeToFuncBody(accessVar, pos, pos+varName.length());
					handleExpressionCreated(true, accessVar);
					initialization = new SimpleStatement(accessVar);
					setExprRegionRelativeToFuncBody(initialization, pos, pos+varName.length());
					parseStatementRecursion++;
					handleStatementCreated(initialization);
					parseStatementRecursion--;
				} else
					w = null;
			}
			if (w == null) {
				// regularly parse initialization statement
				seek(pos);
				initialization = parseStatement(EnumSet.of(ParseStatementOption.InitializationStatement));
				if (initialization == null)
					error(ParserErrorCode.ExpectedCode, this.offset, this.offset+1, NO_THROW);
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
				error(ParserErrorCode.ExpressionExpected, savedOffset, this.offset, ABSOLUTE_MARKER_LOCATION);
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
					error(ParserErrorCode.ConditionExpected, savedOffset, this.offset, ABSOLUTE_MARKER_LOCATION);
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
					error(ParserErrorCode.ExpressionExpected, savedOffset, this.offset+1, NO_THROW);
			}
			arrayExpr = null;
		}
		eatWhitespace();
		expect(')');
		eatWhitespace();
		savedOffset = this.offset;
		currentLoop = loopType;
		body = parseStatement();
		if (body == null)
			error(ParserErrorCode.StatementExpected, savedOffset, savedOffset+4, NO_THROW);
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
		currentLoop = LoopType.While;
		//			if (!readWord.equals(readWord.toLowerCase())) {
		//				String problem = "Syntax error: you should only use lower case letters in keywords. ('" + readWord.toLowerCase() + "' instead of '" + readWord + "')"; 
		//				createErrorMarker(fReader.getPosition() - readWord.length(), fReader.getPosition(), problem);
		//				throw new ParsingException(problem);
		//			}
		eatWhitespace();
		expect('(');
		eatWhitespace();
		ExprElm condition = parseExpression();
		if (condition == null)
			condition = ExprElm.nullExpr(this.offset, 0, this); // while () is valid
		eatWhitespace();
		expect(')');
		eatWhitespace();
		offset = this.offset;
		Statement body = parseStatement();
		if (body == null)
			error(ParserErrorCode.StatementExpected, offset, offset+4, NO_THROW);
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
		ExprElm condition = parseExpression();
		if (condition == null)
			condition = ExprElm.nullExpr(this.offset, 0, this); // if () is valid
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
				error(ParserErrorCode.StatementExpected, o, o+Keywords.Else.length(), NO_THROW);
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
			: new MissingStatement(offsetWhereExpected-bodyOffset());
	}

	/**
	 * Parse an id. On successful parsing, the parsed will be stored in the parsedID field.
	 * @return Whether parsing the id was successful. If false, one can be assured that parsedID will be null.
	 * @throws ParsingException
	 */
	private boolean parseID() throws ParsingException {
		ID id;
		if (offset < size && (id = specialEngineRules != null ? specialEngineRules.parseId(this) : null) != null) {
			parsedID = id;
			return true;
		} else {
			parsedID = null; // reset so no old parsed ids get through
			return false;
		}
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
		if (isEngine && parseEllipsis()) {
			seek(backtrack);
			return addVarParmsParm(function);
		}
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
				error(ParserErrorCode.NotSupported, typeStart, typeEnd, NO_THROW|ABSOLUTE_MARKER_LOCATION, readStringAt(typeStart, typeEnd), engine().name() + " with no type annotations");
			break;
		default:
			break;
		}
		Variable var = new Variable(null, Scope.PARAMETER);
		if (parsedTypeAnnotation != null)
			parsedTypeAnnotation.setTarget(var);
		if (type != null) {
			if ((type == PrimitiveType.REFERENCE || type instanceof ReferenceType) && !engine.supportsPrimitiveType(PrimitiveType.REFERENCE))
				error(ParserErrorCode.PrimitiveTypeNotSupported, offset-1, offset, NO_THROW, PrimitiveType.REFERENCE.typeName(true), script.engine().name());
			var.forceType(type, true);
		}
		var.setName(parmName);
		var.setLocation(new SourceLocation(nameStart, this.offset));
		var.setParentDeclaration(function);
		if (parsedTypeAnnotation != null)
			parsedTypeAnnotation.setTarget(var);
		function.addParameter(var);
		eatWhitespace();
		return var;
	}
	
	private boolean testForString(String s) {
		String t = readString(s.length());
		return t != null && t.equals(s);
	}
	
	private Wildcard parseWildcard() throws ParsingException {
		int savedOffset = this.offset;
		if (testForString("§")) { //$NON-NLS-1$
			ProplistDeclaration proplDec = parsePropListDeclaration(false);
			if (proplDec != null)
				return new Wildcard(proplDec);
			else {
				error(ParserErrorCode.TokenExpected, offset, offset+1, 0, ProplistDeclaration.class.getSimpleName());
				return null;
			}
		} else {
			this.seek(savedOffset);
			return null;
		}
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
	@Override
	public int bodyOffset() {
		Function f = currentFunction();
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
			return buffer.substring(function.bodyLocation().start(), function.bodyLocation().end());
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
	 * Report expressions at the offset the parser is currently at.
	 * This method will try to avoid reparsing the script if the function passed has a cached block that is still valid.
	 * Validity of the cached block is determined by comparing the hash of the original function block source to the hash of the current source fragment specified by the function's body location.
	 * @param funcOrRegion the function to report the code block of. If null, no attempt to avoid reparsing takes place.
	 * @param listener Listener to get informed about reported expressions
	 * @param flavour Whether to only parse statements, or also expressions
	 */
	public void visitCode(
		IRegion funcOrRegion,
		final IASTVisitor listener,
		VisitCodeFlavour flavour,
		boolean reportErrors
	) {
		boolean oldErrorsDisabled = allErrorsDisabled;
		allErrorsDisabled = !reportErrors;
		Function func = funcOrRegion instanceof Function ? (Function)funcOrRegion : null;
		setCurrentFunction(func);
		try {
			String functionSource = functionSource(func);
			FunctionBody cachedBlock = func != null ? func.bodyMatchingSource(functionSource) : null;
			// if block is non-existent or outdated, parse function code and store block
			if (cachedBlock == null) {
				if (func != null)
					func.clearLocalVars();
				strictLevel = script().strictLevel();
				enableErrors(DISABLED_INSTANT_ERRORS, false);
				EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
				LinkedList<Statement> statements = new LinkedList<Statement>();
				parseStatementBlock(offset, Integer.MAX_VALUE, statements, options, flavour);
				cachedBlock = new FunctionBody(func, statements);
				if (func != null)
					func.storeBody(cachedBlock, functionSource);
			}
			// traverse block using the listener
			if (cachedBlock != null) {
				if (func != null) {
					for (Variable parm : func.parameters())
						parm.assignType(PrimitiveType.UNKNOWN);
					for (Variable var : func.localVars())
						var.assignType(PrimitiveType.UNKNOWN);
					func.assignType(PrimitiveType.UNKNOWN, false);
				}
				if (ClonkPreferences.toggle(ClonkPreferences.ANALYZE_CODE, true))
					reportProblemsOf(iterable(cachedBlock.statements()), true);
				// just traverse... this should be faster than reparsing -.-
				if (listener != null)
					cachedBlock.traverse(listener, this);
			}
		}
		catch (ParsingException e) {
			//e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			allErrorsDisabled = oldErrorsDisabled;
		}
	}
	
	/**
	 * Enum to specify kind of expression reporting
	 * @author madeen
	 *
	 */
	public enum VisitCodeFlavour {
		/**
		 * Report only expressions (call {@link C4ScriptParser#parseExpression()})
		 */
		OnlyExpressions,
		/**
		 * Report statements as well (call {@link C4ScriptParser#parseStatement()})
		 */
		AlsoStatements
	}
	
	/**
	 * Script parser that notifies a marker listener about markers about to be created
	 * @author madeen
	 *
	 */
	private static class ScriptParserWithMarkerListener extends C4ScriptParser {
		/**
		 * The associated marker listener
		 */
		private final IMarkerListener markerListener;
		/**
		 * Create the parser
		 * @param withString Script to parse
		 * @param script Script object representing the script to be parsed
		 * @param markerListener the marker listener
		 */
		public ScriptParserWithMarkerListener(String withString, Script script, IMarkerListener markerListener) {
			super(withString, script, null);
			this.markerListener = markerListener;
		}
		/**
		 * Overridden to notify the {@link #markerListener} and possibly cancel creating the marker if the listener says so.
		 */
		@Override
		public void marker(ParserErrorCode code,
				int markerStart, int markerEnd, int flags,
				int severity, Object... args) throws ParsingException {
			if (markerListener != null) {
				if ((flags & ABSOLUTE_MARKER_LOCATION) == 0) {
					markerStart += offsetOfScriptFragment;
					markerEnd += offsetOfScriptFragment;
				}
				if (markerListener.markerEncountered(this, code, markerStart, markerEnd, flags, severity, args) == Decision.DropCharges)
					return;
			}
			super.marker(code, markerStart, markerEnd, flags, severity, args);
		}
	}
	
	/**
	 * Report expressions and statements of a function or some free-floating code fragment using
	 * a newly created parser that is returned after reporting is finished.
	 * If funcOrRegion is a {@link Function}, reparsing takes only place if the script the function body
	 * was parsed from has been changed. 
	 * @param doc The document the script stems from
	 * @param context Script object
	 * @param funcOrRegion Region to parse. The special case of parsing a function applies if this is a C4Function object.
	 * @param listener
	 * @param markerListener
	 * @param flavour
	 * @param reportErrors
	 * @return The parser that did the reporting/(not necessarily parsing) work
	 */
	public static C4ScriptParser visitCode(
		IDocument doc,
		Script context, IRegion funcOrRegion,
		IASTVisitor listener, final IMarkerListener markerListener,
		VisitCodeFlavour flavour,
		boolean reportErrors
	) { 
		String statements_;
		final int statementStart = funcOrRegion.getOffset();
		final int statementEnd = funcOrRegion.getOffset()+funcOrRegion.getLength();
		try {
			// totally important to add the ")". Makes completion proposals work. DO NOT REMOVE!1 - actually, I removed it and it's okay
			statements_ = doc.get(statementStart, Math.min(statementEnd-statementStart, doc.getLength()-statementStart));
		} catch (BadLocationException e) {
			statements_ = ""; // well... //$NON-NLS-1$
		}
		final String statements = statements_;
		C4ScriptParser parser = new ScriptParserWithMarkerListener(statements, context, markerListener) {
			@Override
			protected void initialize() {
				super.initialize();
				offsetOfScriptFragment = statementStart;
				findDefinitionViaCall = true;
			}
			@Override
			public int bodyOffset() {
				return 0;
			}
			@Override
			protected String functionSource(Function function) {
				return statements;
			}
		};
		parser.visitCode(funcOrRegion, listener, flavour, reportErrors);
		return parser;
	}
	
	/**
	 * Parse a stand-alone statement with an optional function context.
	 * @param statementText The statement text to parse
	 * @param context Function context. If null, some temporary context will be created internally.
	 * @param listener Script parser listener
	 * @param markerListener Marker listener
	 * @return The statement or a BunchOfStatement if more than one statement could be parsed from statementText. Possibly null, if erroneous text was passed.
	 * @throws ParsingException
	 */
	public static Statement parseStandaloneStatement(
		final String statementText,
		Function context,
		IASTVisitor listener,
		final IMarkerListener markerListener,
		Engine engine
	) throws ParsingException {
		if (context == null) {
			Script tempScript = new TempScript(statementText, engine);
			context = new Function("<temp>", null, FunctionScope.GLOBAL); //$NON-NLS-1$
			context.setScript(tempScript);
			context.setBodyLocation(new SourceLocation(0, statementText.length()));
		}
		C4ScriptParser tempParser = new ScriptParserWithMarkerListener(statementText, context.script(), markerListener) {
			@Override
			public int bodyOffset() {
				return 0;
			}
			@Override
			protected ExprElm parseTupleElement(boolean reportErrors) throws ParsingException {
				Statement s = parseStatement();
				if (s instanceof SimpleStatement)
					return ((SimpleStatement)s).expression();
				else
					return s;
			}
		};
		return tempParser.parseStandaloneStatement(statementText, context, listener);
	}
	
	public static Statement parse(final String statementText, Engine engine) throws ParsingException {
		return parseStandaloneStatement(statementText, null, null, null, engine);
	}
	
	public static ExprElm matchingExpr(final String statementText, Engine engine) {
		try {
			return parse(statementText, engine).matchingExpr();
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
	 * @param listener Script parser listener
	 * @return The {@link Statement}, or a {@link BunchOfStatements} if more than one statement could be parsed from statementText. Possibly null, if erroneous text was passed.
	 * @throws ParsingException
	 */
	public Statement parseStandaloneStatement(final String statementText, Function context, IASTVisitor listener) throws ParsingException {
		init(statementText);
		setListener(listener);
		setCurrentFunction(context);
		enableError(ParserErrorCode.NotFinished, false);
		
		List<Statement> statements = new LinkedList<Statement>();
		Statement statement;
		do {
			statement = parseStatement();
			if (statement != null)
				statements.add(statement);
			else
				break;
		} while (true);
		reportProblemsOf(statements, true);
		return statements.size() == 1 ? statements.get(0) : new BunchOfStatements(statements);
	}

	@Override
	public Object valueForVariable(String varName) {
		return "Yes"; //$NON-NLS-1$
	}

	@Override
	public Object[] arguments() {
		return new Object[0];
	}

	@Override
	public Function function() {
		return currentFunction();
	}

	@Override
	public Script script() {
		return script;
	}

	@Override
	public int codeFragmentOffset() {
		return bodyOffset();
	}

	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		// yes
	}
	
	private final Stack<IFunctionCall> functionCall = new Stack<IFunctionCall>();
	@Override
	public void pushCurrentFunctionCall(IFunctionCall call) { functionCall.push(call); }
	@Override
	public void popCurrentFunctionCall() { functionCall.pop(); }
	@Override
	public IFunctionCall currentFunctionCall() { return functionCall.isEmpty() ? null : functionCall.peek(); }

}
