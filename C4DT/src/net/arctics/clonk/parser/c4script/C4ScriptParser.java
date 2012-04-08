package net.arctics.clonk.parser.c4script;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
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
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener.WhatToDo;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.parser.c4script.ast.ArrayExpression;
import net.arctics.clonk.parser.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.BoolLiteral;
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
import net.arctics.clonk.parser.c4script.ast.ForStatement;
import net.arctics.clonk.parser.c4script.ast.FunctionDescription;
import net.arctics.clonk.parser.c4script.ast.GarbageStatement;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.IScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.IStoredTypeInformation;
import net.arctics.clonk.parser.c4script.ast.IfStatement;
import net.arctics.clonk.parser.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.parser.c4script.ast.KeywordStatement;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.MissingStatement;
import net.arctics.clonk.parser.c4script.ast.NewProplist;
import net.arctics.clonk.parser.c4script.ast.NumberLiteral;
import net.arctics.clonk.parser.c4script.ast.Parenthesized;
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;
import net.arctics.clonk.parser.c4script.ast.Tuple;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.parser.c4script.ast.UnaryOp;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement.VarInitialization;
import net.arctics.clonk.parser.c4script.ast.WhileStatement;
import net.arctics.clonk.parser.c4script.ast.Wildcard;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.resource.ClonkBuilder;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4GroupItem;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Display;

/**
 * A C4Script parser. Parses declarations in a script and stores it in a C4ScriptBase object (sold separately).
 * The parser can also perform a second parse phase that creates syntax trees from c4script functions. Those can be used for various purposes, including
 * checking correctness (aiming to detect all kinds of errors like undeclared identifiers, supplying values of wrong type to functions etc.), converting old
 * c4script code to #strict-compliant "new-style" code and forming the base of navigation operations like "Find Declaration", "Find References" etc.
 */
public class C4ScriptParser extends CStyleScanner implements DeclarationObtainmentContext, IEvaluationContext {
	
	private static final boolean DEBUG = false;
	
	/**
	 * Context for parsing a single {@link Function}. There is only one current function context at a time for one parser but
	 * parsing of functions may happen interleaved, for example when during parsing of one function it is decided that another one needs to be parsed before continuing. 
	 * @author madeen
	 *
	 */
	public static class FunctionContext {
		public Declaration currentDeclaration;
		public ID parsedID;
		public Variable parsedVariable;
		public Number parsedNumber;
		public String parsedMemberOperator;
		public int parseExpressionRecursion;
		public int parseStatementRecursion;

		private final Set<ParserErrorCode> disabledErrors = new HashSet<ParserErrorCode>();
		/**
		 * Whether the current statement is not reached
		 */
		public boolean statementReached;
		/**
		 * Stores the current loop the parser is parsing.
		 */
		public LoopType currentLoop;
		/**
		 * Number of unnamed parameters used in activeFunc (Par(5) -> 6 unnamed parameters).
		 * If a complex expression is passed to Par() this variable is set to UNKNOWN_PARAMETERNUM
		 */
		public int numUnnamedParameters;
		/**
		 * Stack of type information lists.
		 */
		public Stack<List<IStoredTypeInformation>> storedTypeInformationListStack = new Stack<List<IStoredTypeInformation>>();
		public ExprElm expressionReportingErrors;
		public void initialize() {
			statementReached = true;
		}
	}

	public static final int MAX_PAR = 10;
	public static final int MAX_NUMVAR = 20;
	public static final int UNKNOWN_PARAMETERNUM = MAX_PAR+1;
	
	private static final Set<ParserErrorCode> NO_DISABLED_ERRORS = Collections.unmodifiableSet(new HashSet<ParserErrorCode>());
	
	protected IScriptParserListener listener;
	/**
	 * Reference to project file the script was read from.
	 */
	protected IFile scriptFile;
	/**
	 * Script container, the parsed declarations are put into
	 */
	protected Script container;
	/**
	 * Cached strict level from #strict directive.
	 */
	protected int strictLevel;
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
	private boolean allowInterleavedFunctionParsing;
	/**
	 * Set of functions already parsed. Won't be parsed again.
	 */
	private Set<Function> parsedFunctions;
	private SpecialScriptRules specialScriptRules;
	private Engine engine;
	private TypeInformationMerger scriptLevelTypeInformationMerger;
	private ClonkBuilder builder;
	
	public SpecialScriptRules getSpecialScriptRules() {
		return specialScriptRules;
	}
	
	public boolean allErrorsDisabled() {
		return allErrorsDisabled;
	}
	
	public void setAllowInterleavedFunctionParsing(boolean allowInterleavedFunctionParsing) {
		this.allowInterleavedFunctionParsing = allowInterleavedFunctionParsing;
	}
	
	public void setBuilder(ClonkBuilder builder) {this.builder = builder;}
	public ClonkBuilder builder() {return builder;}
	
	/**
	 * Returns the expression listener that is notified when an expression or a statement has been parsed.
	 * @return the expression listener
	 */
	public IScriptParserListener listener() {
		return listener;
	}

	/**
	 * Sets the expression listener.
	 * @param listener the new expression listener
	 */
	public void setListener(IScriptParserListener listener) {
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
		if (currentFunctionContext.numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
			Object ev = index.evaluateAtParseTime(currentFunction());
			if (ev instanceof Number) {
				int number = ((Number)ev).intValue();
				currentFunctionContext.numUnnamedParameters = number >= 0 && number < MAX_PAR ? number+1 : UNKNOWN_PARAMETERNUM;
			} else
				currentFunctionContext.numUnnamedParameters = UNKNOWN_PARAMETERNUM;
		}
	}
	
	/**
	 * Push a new type information list onto the stack.
	 * @return The newly created and pushed type information list.
	 */
	public List<IStoredTypeInformation> beginTypeInferenceBlock() {
		List<IStoredTypeInformation> result = new LinkedList<IStoredTypeInformation>();
		currentFunctionContext.storedTypeInformationListStack.push(result);
		return result;
	}
	
	/**
	 * Pop the current type information list from the sack.
	 * @return The popped list.
	 */
	public List<IStoredTypeInformation> endTypeInferenceBlock() {
		return !currentFunctionContext.storedTypeInformationListStack.isEmpty() ? currentFunctionContext.storedTypeInformationListStack.pop() : null;
	}
	
	/**
	 * Store given type information in the associated C4Declaration objects so this information
	 * will be permanent. 
	 * @param list The type information list to apply.
	 * @param soft Whether to only store the types in function-local variables.
	 */
	private final void applyStoredTypeInformationList(List<IStoredTypeInformation> list, boolean soft) {
		if (list == null)
			return;
		for (IStoredTypeInformation info : list) {
			info.apply(soft, this);
		}
	}
	
	/**
	 * Store current type information in the associated C4Declaration objects so this information
	 * will be permanent. 
	 * @param soft Whether to only store the types in function-local variables.
	 */
	public void applyStoredTypeInformationList(boolean soft) {
		applyStoredTypeInformationList(currentFunctionContext.storedTypeInformationListStack.peek(), soft);
	}
	
	/**
	 * Ask the parser to store type information about an expression. No guarantees whether type information will actually be stored.
	 */
	@Override
	public void storeTypeInformation(ExprElm expression, IType type) {
		IStoredTypeInformation requested = requestStoredTypeInformation(expression);
		if (requested != null) {
			if (DEBUG)
				warningWithCode(ParserErrorCode.TypingJudgment, expression, expression.toString(), type.typeName(true));
			requested.storeType(type);
		}
	}
	
	/**
	 * Requests type information for an expression
	 * @param expression the expression
	 * @param list 
	 * @return the type information or null if none has been stored
	 */
	public IStoredTypeInformation requestStoredTypeInformation(ExprElm expression) {
		if (currentFunctionContext.storedTypeInformationListStack.isEmpty())
			return null;
		boolean topMostLayer = true;
		IStoredTypeInformation base = null;
		for (int i = currentFunctionContext.storedTypeInformationListStack.size()-1; i >= 0; i--) {
			for (IStoredTypeInformation info : currentFunctionContext.storedTypeInformationListStack.get(i)) {
				if (info.storesTypeInformationFor(expression, this)) {
					if (!topMostLayer) {
						base = info;
						break;
					}
					else
						return info;
				}
			}
			topMostLayer = false;
		}
		IStoredTypeInformation newlyCreated = expression.createStoredTypeInformation(this);
		if (newlyCreated != null) {
			if (base != null)
				newlyCreated.merge(base);
			currentFunctionContext.storedTypeInformationListStack.peek().add(newlyCreated);
		}
		return newlyCreated;
	}

	/**
	 * Return a copy of the current type information list.
	 * @return The copied list
	 */
	public List<IStoredTypeInformation> copyCurrentTypeInformationList() {
		try {
			List<IStoredTypeInformation> list = new ArrayList<IStoredTypeInformation>(currentFunctionContext.storedTypeInformationListStack.peek().size());
			for (IStoredTypeInformation info : currentFunctionContext.storedTypeInformationListStack.peek()) {
				list.add((IStoredTypeInformation) info.clone());
			}
			return list;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	/**
	 * Push a new type information list on the type information list stack.
	 * @param list
	 */
	public void pushTypeInformationList(List<IStoredTypeInformation> list) {
		currentFunctionContext.storedTypeInformationListStack.push(list);
	}
	
	/**
	 * Query the type of an arbitrary expression. With some luck the parser will be able to give an answer.
	 * @param expression the expression to query the type of
	 * @return
	 */
	public IStoredTypeInformation queryStoredTypeInformation(ExprElm expression) {
		if (currentFunctionContext.storedTypeInformationListStack.isEmpty())
			return null;
		for (int i = currentFunctionContext.storedTypeInformationListStack.size()-1; i >= 0; i--) {
			for (IStoredTypeInformation info : currentFunctionContext.storedTypeInformationListStack.get(i)) {
				if (info.storesTypeInformationFor(expression, this))
					return info;
			}
		}
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
		IStoredTypeInformation info = queryStoredTypeInformation(expression);
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
		return currentFunctionContext.currentDeclaration != null ? currentFunctionContext.currentDeclaration.firstParentDeclarationOfType(Function.class) : null;
	}
	
	/**
	 * Sets the current function. There should be a good reason to call this. 
	 * @param func
	 */
	public void setCurrentFunc(Function func) {
		if (func != currentFunction()) {
			currentFunctionContext.currentDeclaration = func;
			currentFunctionContext.numUnnamedParameters = 0;
		}
	}
	
	/**
	 * Returns the first variable in the parent chain of currentDeclaration
	 * @return
	 */
	public Variable getCurrentVariable() {
		return currentFunctionContext.currentDeclaration != null ? currentFunctionContext.currentDeclaration.firstParentDeclarationOfType(Variable.class) : null;
	}
	
	/**
	 * Returns the declaration that is currently being parsed.
	 * @return
	 */
	@Override
	public Declaration currentDeclaration() {
		return currentFunctionContext.currentDeclaration;
	}
	
	/**
	 * Returns the script object that is being parsed by the parser.
	 * @return
	 */
	@Override
	public final Script containingScript() {
		return container;
	}
	
	/**
	 * Returns the script object as an object if it is one or null if it is not.
	 * @return The script object as  C4Object
	 */
	@Override
	public Definition containerAsDefinition() {
		if (container instanceof Definition)
			return (Definition) container;
		return null;
	}
	
	/**
	 * Creates a script parser. The script is read from the file attached to the script (queried through getScriptFile()).
	 */
	public C4ScriptParser(Script script) {
		this((IFile) script.scriptStorage(), script);
		initialize();
	}
	
	/**
	 * Initialize some state fields. Needs to be called before actual parsing takes place.
	 */
	protected void initialize() {
		if (container != null) {
			engine = container.engine();
			specialScriptRules = engine != null ? container.engine().specialScriptRules() : null;

			if (container.index() instanceof ProjectIndex) {
				ProjectIndex projIndex = (ProjectIndex) container.index();
				ClonkProjectNature nature = projIndex.getNature();
				if (nature != null) {
					errorsDisabledByProjectSettings = nature.settings().getDisabledErrorsSet();
				}
			}

			strictLevel = container.strictLevel();
			container.containsGlobals = false;
		}
		currentFunctionContext.initialize();
		if (scriptFile != null)
			allErrorsDisabled = C4GroupItem.getGroupItemBackingResource(scriptFile) != null;
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
		container = script;
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
		scriptFile = null;
		container = script;
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
		container = script;
		isEngine = container instanceof Engine;
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
			return String.valueOf(currentFunctionContext.parsedNumber);
		else
			return String.valueOf((char)read());
	}

	/**
	 * Parse declarations but not function code. Before calling this it should be ensured that the script is cleared to avoid duplicates.
	 */
	public void parseDeclarations() {
		synchronized (container) {
			strictLevel = container.strictLevel();
			int offset = 0;
			this.seek(offset);
			setAllowInterleavedFunctionParsing(true);
			enableError(ParserErrorCode.StringNotClosed, false); // just one time error when parsing function code
			try {
				eatWhitespace();
				while(!reachedEOF()) {
					if (!parseDeclaration()) {
						eatWhitespace();
						if (!reachedEOF()) {
							int start = this.offset;
							String tokenText = parseTokenAndReturnAsString();
							errorWithCode(ParserErrorCode.UnexpectedToken, start, this.offset, NO_THROW, tokenText);
						}
					}
					eatWhitespace();
				}
			}
			catch (ParsingException e) {
				return;
			}
			enableError(ParserErrorCode.StringNotClosed, true);
		}
		deployMarkers();
	}
	
	/**
	 * Parse function code. Side effects include:
	 * 	-Errors (or things the parser thinks are errors) listed in the Problems view
	 * 	-Types for variables inferred more or less accurately
	 * @throws ParsingException
	 */
	public void parseCodeOfFunctionsAndValidate() throws ParsingException {
		synchronized (container) {
			prepareForFunctionParsing();
			for (Function function : container.functions())
				parseCodeOfFunction(function, false);
			synchronized (parsedFunctions) {
				parsedFunctions = null;
			}
			applyStoredTypeInformationList(scriptLevelTypeInformationMerger.getResult(), false);
			scriptLevelTypeInformationMerger = null;
			currentFunctionContext.currentDeclaration = null;

			for (Directive directive : container.directives())
				directive.validate(this);

			for (Variable variable : container.variables()) {
				ExprElm initialization = variable.initializationExpression();
				if (initialization != null) {
					ExprElm old = currentFunctionContext.expressionReportingErrors;
					currentFunctionContext.expressionReportingErrors = initialization;
					if (variable.scope() == Scope.CONST && !initialization.isConstant()) {
						errorWithCode(ParserErrorCode.ConstantValueExpected, initialization, C4ScriptParser.NO_THROW);
					}
					initialization.reportErrors(this);
					currentFunctionContext.expressionReportingErrors = old;
				}
			}
			container.notDirty();
			distillAdditionalInformation();
		}
		deployMarkers();
	}

	public void prepareForFunctionParsing() {
		if (parsedFunctions == null) {
			strictLevel = container.strictLevel();
			scriptLevelTypeInformationMerger = new TypeInformationMerger();
			parsedFunctions = new HashSet<Function>();
		}
	}

	/**
	 * OC: Get information out of the script that was previously to be found in additional files (like the name of the {@link Definition}). Specifically, parse the Definition() function.
	 */
	public void distillAdditionalInformation() {
		if (container instanceof Definition) {
			
			final Definition obj = (Definition) container;
			obj.chooseLocalizedName(); // ClonkRage Names.txt
			
			// local Name = "Exploder";
			Variable nameLocal = container.findLocalVariable("Name", false); //$NON-NLS-1$
			if (nameLocal != null) {
				ExprElm expr = nameLocal.initializationExpression();
				if (expr != null) {
					obj.setName(expr.evaluateAtParseTime(obj).toString());
				}
			}
		}
	}

	/**
	 * Parse code of one single function. {@link #parseCodeOfFunctionsAndValidate()} calls this for all functions in the script. 
	 * @param function The function to be parsed
	 * @throws ParsingException
	 */
	@Override
	public void parseCodeOfFunction(Function function, boolean withNewContext) throws ParsingException {
		// parser not yet ready to parse functions - deny
		if (parsedFunctions == null)
			return;
		synchronized (parsedFunctions) {
			if (parsedFunctions == null)
				return; // >:o
			// only allow interleaved function parsing when invoked by ClonkBuilder
			if (!allowInterleavedFunctionParsing && withNewContext)
				return;
			// function is weird or does not belong here - ignore
			if (function.body() == null)
				return;
			if (function.script() != container) {
				if (builder != null) {
					builder.parseFunction(function);
				}
				return;
			}
			// already parsed? don't do that again
			if (parsedFunctions.contains(function))
				return;
			else
				parsedFunctions.add(function);

			function.forceType(PrimitiveType.UNKNOWN);

			if (specialScriptRules != null)
				for (SpecialFuncRule eventListener : specialScriptRules.functionEventListeners())
					eventListener.functionAboutToBeParsed(function, this);

			int oldOffset = this.offset;
			FunctionContext oldFunctionContext;
			if (withNewContext) {
				oldFunctionContext = currentFunctionContext;
				currentFunctionContext = new FunctionContext();
				currentFunctionContext.initialize();
			} else
				oldFunctionContext = null;
			Declaration oldDec = currentFunctionContext.currentDeclaration;
			try {
				setCurrentFunc(function);
				assignDefaultParmTypesToFunction(function);
				// reset local vars
				function.resetLocalVarTypes();
				beginTypeInferenceBlock();
				this.seek(function.body().start());
				// parse code block
				int endOfFunc = function.body().end();
				EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
				List<Statement> statements = new LinkedList<Statement>();
				parseStatementBlock(offset, endOfFunc, statements, options, ExpressionsAndStatementsReportingFlavour.AlsoStatements);
				BunchOfStatements bunch = new BunchOfStatements(statements);
				if (function.isOldStyle() && statements.size() > 0)
					function.body().setEnd(statements.get(statements.size()-1).end()+bodyOffset());
				warnAboutPossibleProblemsWithFunctionLocalVariables(function, bunch);
				function.storeBlock(bunch, functionSource(function));
				applyStoredTypeInformationList(false); // apply short-term inference information
				List<IStoredTypeInformation> block = endTypeInferenceBlock();
				if (scriptLevelTypeInformationMerger != null) {
					scriptLevelTypeInformationMerger.inject(block); // collect information from all functions and apply that after having parsed them all
				}
				if (currentFunctionContext.numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
					function.createParameters(currentFunctionContext.numUnnamedParameters);
				}
				else if (currentFunctionContext.numUnnamedParameters == UNKNOWN_PARAMETERNUM && (function.numParameters() == 0 || function.parameter(function.numParameters()-1).isActualParm())) {
					addVarParmsParm(function);
				}
			}
			catch (SilentParsingException e) {
				// not really an error
			}
			catch (ParsingException e) {
				//System.out.println(String.format("ParsingException in %s (%s)", activeFunc.getName(), container.getName()));
				//e.printStackTrace();
				// not very exceptional
			}
			catch (Exception e) {
				// errorWithCode throws ^^;
				e.printStackTrace();
				errorWithCode(ParserErrorCode.InternalError, this.offset, this.offset+1, NO_THROW, e.getMessage());
			}
			finally {
				currentFunctionContext.currentDeclaration = oldDec;
				if (oldFunctionContext != null)
					currentFunctionContext = oldFunctionContext;
				seek(oldOffset);
			}
		}
	}

	private void assignDefaultParmTypesToFunction(Function function) {
		if (specialScriptRules != null) {
			for (SpecialFuncRule funcRule : specialScriptRules.defaultParmTypeAssignerRules()) {
				if (funcRule.assignDefaultParmTypes(this, function))
					break;
			}
		}
	}

	/**
	 * Warn about variables declared inside the given block that have not been referenced elsewhere ({@link Variable#isUsed() == false})
	 * @param func The function the block belongs to.
	 * @param block The {@link Block}
	 */
	public void warnAboutPossibleProblemsWithFunctionLocalVariables(Function func, Block block) {
		if (func == null)
			return;
		for (Variable v : func.localVars()) {
			if (!v.isUsed())
				createWarningAtDeclarationOfVariable(block, v, ParserErrorCode.Unused, v.name());
			Variable shadowed = containingScript().findVariable(v.name());
			// ignore those pesky static variables from scenario scripts
			if (shadowed != null && !(shadowed.parentDeclaration() instanceof Scenario)) 
				createWarningAtDeclarationOfVariable(block, v, ParserErrorCode.IdentShadowed, v.qualifiedName(), shadowed.qualifiedName());
		}
	}

	private boolean createWarningAtDeclarationOfVariable(Block block, Variable variable, ParserErrorCode code, Object... formatArguments) {
		for (VarDeclarationStatement decl : block.allSubExpressionsOfType(VarDeclarationStatement.class)) {
			for (VarInitialization initialization : decl.variableInitializations()) {
				if (initialization.variableBeingInitialized == variable) {
					ExprElm old = currentFunctionContext.expressionReportingErrors;
					currentFunctionContext.expressionReportingErrors = decl;
					warningWithCode(code, initialization, formatArguments);
					currentFunctionContext.expressionReportingErrors = old;
					return true;
				}
			}
		}
		return false;
	}

	private Variable addVarParmsParm(Function func) {
		Variable v = new Variable("...", PrimitiveType.ANY); //$NON-NLS-1$
		v.setParentDeclaration(func);
		v.setScope(Variable.Scope.VAR);
		func.addParameter(v);
		return v;
	}

	/**
	 * Returns a line a region is in as a string
	 * @param region the region the line to be returned is in
	 * @return the line string
	 */
	@Override
	public String lineAtRegion(IRegion region) {
		return this.lineAtRegion(region);
	}

	/**
	 * Parses the declaration at the current this position.
	 * @return whether parsing was successful
	 * @throws ParsingException
	 */
	protected boolean parseDeclaration() throws ParsingException {
		final int startOfDeclaration = this.offset;
		int readByte = read();
		if (readByte == '#') {
			// directive
			String directiveName = this.readStringUntil(BufferedScanner.WHITESPACE_CHARS);
			DirectiveType type = DirectiveType.makeType(directiveName);
			if (type == null) {
				warningWithCode(ParserErrorCode.UnknownDirective, startOfDeclaration, startOfDeclaration + 1 + (directiveName != null ? directiveName.length() : 0));
				this.moveUntil(BufferedScanner.NEWLINE_CHARS);
				return true;
			}
			else {
				String content = parseDirectiveParms();
				Directive directive = new Directive(type, content);
				directive.setLocation(absoluteSourceLocation(startOfDeclaration, this.offset));
				container.addDeclaration(directive);
				if (type == DirectiveType.APPENDTO)
					appendTo = true;
				return true;
			}
		}
		else {
			this.seek(startOfDeclaration);
			FunctionHeader functionHeader = FunctionHeader.parse(this, true);
			String word;
			if (functionHeader != null) {
				if (parseFunctionDeclaration(functionHeader))
					return true;
			}
			else if ((word = readIdent()) != null && parseVariableDeclaration(false, true, Scope.makeScope(word), collectPrecedingComment(startOfDeclaration)) != null)
				return true;
		}
		this.seek(startOfDeclaration);
		return false;
	}

	private String parseDirectiveParms() {
		StringBuffer buffer = new StringBuffer(80);
		while (!reachedEOF() && !BufferedScanner.isLineDelimiterChar((char)peek()) && !parseComment()) {
			buffer.append((char)read());
		}
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
		public PrimitiveType returnType;
		public FunctionHeader(int start, String name, FunctionScope scope, boolean isOldStyle, int nameStart, PrimitiveType returnType) {
			super();
			this.start = start;
			this.name = name;
			this.scope = scope;
			this.isOldStyle = isOldStyle;
			this.nameStart = nameStart;
			this.returnType = returnType;
		}
		public static FunctionHeader parse(C4ScriptParser parser, boolean allowOldStyle) throws ParsingException {
			int initialOffset = parser.offset;
			int nameStart = parser.offset;
			boolean isOldStyle = false;
			String name = null;
			String s = parser.parseIdentifier();
			PrimitiveType returnType = null;
			if (s != null) {
				FunctionScope scope = FunctionScope.makeScope(s);
				if (scope != null) {
					parser.eatWhitespace();
					nameStart = parser.offset;
					s = parser.parseIdentifier();
				} else
					scope = FunctionScope.PUBLIC;
				if (s != null) {
					if (s.equals(Keywords.Func)) {
						parser.eatWhitespace();
						returnType = parser.parseFunctionReturnType();
						nameStart = parser.offset;
						parser.eatWhitespace();
						s = parser.parseIdentifier();
						if (s != null) {
							name = s;
							isOldStyle = false;
						}
					} else {
						name = s;
						isOldStyle = true;
					}
				}
				if (name != null && (allowOldStyle || !isOldStyle)) {
					if (isOldStyle) {
						int backtrack = parser.offset;
						parser.eatWhitespace();
						boolean isProperLabel = parser.read() == ':' && parser.read() != ':';
						parser.seek(backtrack);
						if (isProperLabel)
							return new FunctionHeader(initialOffset, s, scope, true, nameStart, returnType);
					}
					else {
						if (parser.peekAfterWhitespace() == '(')
							return new FunctionHeader(initialOffset, name, scope, false, nameStart, returnType);
					}
				}
			}
			parser.seek(initialOffset);
			return null;
		}
		public void apply(Function func) {
			func.setOldStyle(isOldStyle);
			func.setName(name);
			func.setVisibility(scope);
			func.setReturnType(returnType);
		}
	}

	private List<VarInitialization> parseVariableDeclaration(boolean reportErrors, boolean checkForFinalSemicolon, Scope scope, Comment comment) throws ParsingException {
		if (scope != null) {
			
			final int offset = this.offset;

			List<VarInitialization> createdVariables = new LinkedList<VarInitialization>();
			Function currentFunc = currentFunction();
			
			eatWhitespace();
			switch (scope) {
			case STATIC:
				int scopeSpecifierStart = offset-scope.toKeyword().length();
				int pos = this.offset;
				if (readIdent().equals(Keywords.Const))
					scope = Scope.CONST;
				else
					this.seek(pos);
				if (currentFunc != null) {
					errorWithCode(ParserErrorCode.StaticInsideFunction, scopeSpecifierStart, this.offset, NO_THROW, scope.toKeyword());
					scope = Scope.VAR;
				}
				break;
			case VAR:
				if (currentFunc == null) {
					errorWithCode(ParserErrorCode.VarOutsideFunction, offset-scope.toKeyword().length(), offset, NO_THROW, scope.toKeyword(), Keywords.GlobalNamed, Keywords.LocalNamed);
					scope = Scope.LOCAL;
				}
			}
			
			{
				int rewind = this.offset;
				do {
					eatWhitespace();
					IType typeOfNewVar;
					// when parsing an engine script from (res/engines/...), allow specifying the type directly
					if (isEngine) {
						typeOfNewVar = parseFunctionReturnType();
						if (typeOfNewVar != null)
							eatWhitespace();
					}
					else
						typeOfNewVar = null;

					int s = this.offset;
					String varName = readIdent();
					int e = this.offset;
					Declaration outerDec = currentFunctionContext.currentDeclaration;
					try {
						Variable var = createVarInScope(varName, scope, s, e, comment);
						currentFunctionContext.currentDeclaration = var;
						VarInitialization varInitialization;
						ExprElm initializationExpression = null;
						if (scope == Scope.CONST || currentFunc != null || containingScript().engine().settings().nonConstGlobalVarsAssignment) {
							eatWhitespace();
							if (peek() == '=') {
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

								try {
									if (currentFunc == null) {
										// only set initialization expression outside function so TidyUpCode won't have overlapping edits when trying
										// to tidy up both the function code and the initialization expression separately
										switch (scope) {
										case CONST:
											// never evaluate expressions, it's nice to have them and stuff
											/*if (varInitialization.expression.isConstant()) {
											varInitialization.variableBeingInitialized.setConstValue(varInitialization.expression.evaluateAtParseTime(Utilities.or(getCurrentFunc(), container)));
											break;
										}*/
											// fallthrough
										case LOCAL: case STATIC:
											var.setInitializationExpression(initializationExpression);
											break;
										}
									}
								} catch (Exception ex) {
									ex.printStackTrace();
									errorWithCode(ParserErrorCode.InvalidExpression, initializationExpression);
								}

								typeOfNewVar = initializationExpression instanceof IType
									? (IType)initializationExpression
										: initializationExpression != null
										? initializationExpression.typeInContext(this)
											: PrimitiveType.UNKNOWN;
							} else {
								if (scope == Scope.CONST && !isEngine)
									errorWithCode(ParserErrorCode.ConstantValueExpected, this.offset-1, this.offset, true);
								else if (scope == Scope.STATIC && isEngine)
									var.forceType(PrimitiveType.INT); // most likely
							}
						}
						varInitialization = new VarInitialization(varName, initializationExpression, s-bodyOffset(), var);
						createdVariables.add(varInitialization);
						if (typeOfNewVar != null) {
							switch (scope) {
							case CONST: case STATIC:
								container.containsGlobals = true;
							case LOCAL:
								if (currentFunc == null) {
									varInitialization.variableBeingInitialized.forceType(typeOfNewVar);
									break;
								}
								break;
							case VAR:
								//new AccessVar(varInitialization.variableBeingInitialized).expectedToBeOfType(typeOfNewVar, this, TypeExpectancyMode.Force);
								break;
							}
						}
						rewind = this.offset;
						eatWhitespace();
					} finally {
						currentFunctionContext.currentDeclaration = outerDec;
					}
				} while(read() == ',');
				seek(rewind);
			}
			
			if (checkForFinalSemicolon) {
				int rewind = this.offset;
				eatWhitespace();
				if (read() != ';') {
					seek(rewind);
					errorWithCode(ParserErrorCode.CommaOrSemicolonExpected, this.offset-1, this.offset);
				}
			}
			
			// look for comment following directly and decorate the newly created variables with it
			String inlineComment = textOfInlineComment();
			if (inlineComment != null) {
				inlineComment = inlineComment.trim();
				for (VarInitialization v : createdVariables) {
					v.variableBeingInitialized.setUserDescription(inlineComment);
				}
			}
			
			return createdVariables.size() > 0 ? createdVariables : null;
		} else {
			return null;
		}
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
			return containingScript().findLocalVariable(name, false);
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
		case VAR:
			result.setParentDeclaration(currentFunction());
			currentFunction().localVars().add(result);
			break;
		case CONST: case STATIC: case LOCAL:
			result.setParentDeclaration(containingScript());
			containingScript().addDeclaration(result);
		}
		result.setLocation(absoluteSourceLocation(start, end));
		result.setUserDescription(description != null ? description.text().trim() : null);
		return result;
	}

	private PrimitiveType parseFunctionReturnType() throws ParsingException {
		final int backtrack = this.offset;
		eatWhitespace();
		String str;
		if (peek() == '&') {
			if (!container.engine().settings().supportsRefs) {
				errorWithCode(ParserErrorCode.EngineDoesNotSupportRefs, this.offset, this.offset+1, ABSOLUTE_MARKER_LOCATION|NO_THROW, container.engine().name());
			}
			read();
			return PrimitiveType.REFERENCE;
		}
		else if (isEngine && (str = parseIdentifier()) != null) {
			PrimitiveType t = PrimitiveType.makeType(str, true);
			if (t != PrimitiveType.UNKNOWN)
				return t;
		}
		this.seek(backtrack);
		return null;
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
		
		setCurrentFunc(null);
		if (header.isOldStyle)
			warningWithCode(ParserErrorCode.OldStyleFunc, header.nameStart, header.nameStart+header.name.length());
		Function currentFunc;
		currentFunctionContext.currentDeclaration = currentFunc = newFunction(header.name);
		header.apply(currentFunc);
		currentFunc.setScript(container);
		if (header.scope == FunctionScope.GLOBAL)
			container.containsGlobals = true;
		eatWhitespace();
		int shouldBeBracket = read();
		if (shouldBeBracket != '(') {
			if (header.isOldStyle && shouldBeBracket == ':')
				{} // old style funcs have no named parameters
			else
				tokenExpectedError("("); //$NON-NLS-1$
		} else {
			// get parameters
			do {
				eat(WHITESPACE_CHARS);
				Comment parameterComment = parseCommentObject();
				eat(WHITESPACE_CHARS);
				Variable parm = parseParameter(currentFunc);
				if (parm != null && parameterComment != null)
					parm.setUserDescription(parameterComment.text());
				eatWhitespace();
				int readByte = read();
				if (readByte == ')')
					break; // all parameters parsed
				else if (readByte == ',')
					continue; // parse another parameter
				else
					errorWithCode(ParserErrorCode.TokenExpected, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, String.format(Messages.C4ScriptParser_Or, ")", ","));  //$NON-NLS-1$//$NON-NLS-2$ 
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
		if (token != '{') {
			if (isEngine) {
				if (token != ';')
					tokenExpectedError(";");
				else
					parseBody = false;
			} else if (!header.isOldStyle)
				tokenExpectedError("{");
			else {
				this.seek(endOfHeader);
				blockDepth = -1;
			}
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
				if (FunctionHeader.parse(this, header.isOldStyle) != null || reachedEOF()) {
					if (header.isOldStyle)
						seek(endBody);
					properEnd = true;
					if (blockDepth != -1)
						errorWithCode(ParserErrorCode.MissingBrackets, header.nameStart, header.nameStart+header.name.length(), NO_THROW, blockDepth+1, '}');
					seek(offsetBeforeToken);
				}
				else if ((word = parseIdentifier()) != null && parseVariableDeclaration(false, false, Variable.Scope.makeScope(word), collectPrecedingComment(offsetBeforeToken)) != null)
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
				errorWithCode(ParserErrorCode.TokenExpected, pos-bodyOffset(), pos+1-bodyOffset(), "}"); //$NON-NLS-1$
				return false;
			}
			currentFunc.setBody(startBody != -1 ? absoluteSourceLocation(startBody, endBody) : null);
		} else
			currentFunc.setBody(null);
		eatWhitespace();
		if (desc != null)
			desc.applyDocumentation(currentFunc);
		else {
			// look for comment in the same line as the closing '}' which is common for functions packed into one line
			// hopefully there won't be multi-line functions with such a comment attached at the end
			Comment c = getCommentImmediatelyFollowing();
			if (c != null)
				currentFunc.setUserDescription(c.text());
		}

		// finish up
		currentFunc.setLocation(absoluteSourceLocation(header.nameStart, header.nameStart+header.name.length()));
		currentFunc.setHeader(absoluteSourceLocation(header.start, endOfHeader));
		container.addDeclaration(currentFunc);
		if (!currentFunc.isOldStyle())
			currentFunctionContext.currentDeclaration = null; // to not suppress errors in-between functions
		return true;
	}

	/**
	 * Create a new {@link Function}. Depending on what {@link SpecialScriptRules} the current {@link Engine} has, the function might be some specialized instance ({@link DefinitionFunction} or {@link EffectFunction}for example)
	 * @param nameWillBe What the name of the function will be.
	 * @return The newly created function. Might be of some special class.
	 */
	protected Function newFunction(String nameWillBe) {
	    if (specialScriptRules != null) {
	    	for (SpecialFuncRule funcRule : specialScriptRules.defaultParmTypeAssignerRules()) {
	    		Function f = funcRule.newFunction(nameWillBe);
	    		if (f != null)
	    			return f;
	    	}
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
			if (isHex) {
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
							currentFunctionContext.parsedNumber = Long.parseLong(this.readString(count), 16);
							this.seek(offset+count);
						} else {
							currentFunctionContext.parsedNumber = -1; // unlikely to be parsed
							return false; // well, this seems not to be a number at all
						} 
						return true;
					}
				} while(!reachedEOF());
			}
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
					currentFunctionContext.parsedNumber = -1; // unlikely to be parsed
					return false; // well, this seems not to be a number at all
				} 
			}
		} while(!reachedEOF());
		this.seek(offset);
		String numberString = this.readString(count);
		if (floatingPoint) {
			try {
				currentFunctionContext.parsedNumber = Double.parseDouble(numberString);
			} catch (NumberFormatException e) {
				currentFunctionContext.parsedNumber = Double.MAX_VALUE;
				errorWithCode(ParserErrorCode.NotANumber, offset, offset+count, NO_THROW, numberString);
			}
		} else {
			try {
				currentFunctionContext.parsedNumber = Long.parseLong(numberString);
			} catch (NumberFormatException e) {
				currentFunctionContext.parsedNumber = Integer.MAX_VALUE;
				errorWithCode(ParserErrorCode.NotANumber, offset, offset+count, NO_THROW, numberString);
			}
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
			currentFunctionContext.parsedMemberOperator = "."; //$NON-NLS-1$
			return true;
		}
		else if (firstChar == '-') {
			if (read() == '>') {
				savedOffset = this.offset;
				eatWhitespace();
				if (read() == '~') {
					currentFunctionContext.parsedMemberOperator = "->~"; //$NON-NLS-1$
				}
				else {
					currentFunctionContext.parsedMemberOperator = "->"; //$NON-NLS-1$
					this.seek(savedOffset);
				}
				return true;
			}
		}
		this.seek(savedOffset);
		return false;
	}

	private static final class TempScript extends Script {
		private final String expression;
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

		private TempScript(String expression) {
			super(new Index() {
				private static final long serialVersionUID = 1L;
				private final Engine tempEngine = new Engine("Temp Engine"); //$NON-NLS-1$
				@Override
				public Engine engine() {
					return tempEngine;
				};
			});
			this.expression = expression;
		}

		@Override
		public IStorage scriptStorage() {
			try {
				return new SimpleScriptStorage(expression, expression);
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
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
	 * read operator at some location
	 * @param offset
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
			currentFunctionContext.disabledErrors.remove(error);
		else
			currentFunctionContext.disabledErrors.add(error);
		return result;
	}
	
	private void enableErrors(EnumSet<ParserErrorCode> set, boolean doEnable) {
		if (doEnable) {
			currentFunctionContext.disabledErrors.removeAll(set);
		} else {
			currentFunctionContext.disabledErrors.addAll(set);
		}
	}
	
	/**
	 * Get error enabled status.
	 * @param error The error to check the enabled status of
	 * @return Return whether the error is enabled. 
	 */
	public boolean errorEnabled(ParserErrorCode error) {
		return !(allErrorsDisabled || currentFunctionContext.disabledErrors.contains(error) || errorsDisabledByProjectSettings.contains(error));
	}
	
	private static class LatentMarker {
		public ParserErrorCode code;
		public int start, end;
		public int severity;
		public Object[] args;
		public Object cookie;
		public LatentMarker(ParserErrorCode code, int start, int end, int severity, Object[] args, Object cookie) {
			super();
			this.code = code;
			this.start = start;
			this.end = end;
			this.severity = severity;
			this.args = args;
			this.cookie = cookie;
		}
		public boolean stillApplies(C4ScriptParser parser) {
			switch (code) {
			case TooManyParameters:
				return ((Function)cookie).tooManyParameters((Integer)args[1]);
			default:
				return false;
			}
		}
		public void apply(C4ScriptParser parser) throws ParsingException {
			parser.markerWithCode(code, start, end, NO_THROW, severity, args);
		}
	}
	
	private Collection<LatentMarker> latentMarkers;
	
	/**
	 * Make a note of some potential marker that will or will not be created after having parsed declarations and function code.
	 * Currently, this is only used for {@link ParserErrorCode#TooManyParameters}. When deciding whether to actually add the marker,
	 * the function in question (which might have gotten its parameter list added to) is checked again.
	 * @param code The parser error code
	 * @param region The region
	 * @param severity The severity
	 * @param cookie Cookie consulted when deciding whether to create the marker after parsing function code
	 * @param args Format arguments for the error code message
	 */
	public void addLatentMarker(ParserErrorCode code, IRegion region, int severity, Object cookie, Object... args) {
		addLatentMarker(code, region.getOffset(), region.getOffset()+region.getLength(), severity, cookie, args);
	}
	
	/**
	 * Adds a marker that is not yet sure to apply
	 * @param code parser error code
	 * @param start start of marked region
	 * @param end end of marked region
	 * @param severity severity as IMarker constant
	 * @param cookie additional information (a function or something)
	 * @param args format arguments
	 */
	public void addLatentMarker(ParserErrorCode code, int start, int end, int severity, Object cookie, Object... args) {
		if (latentMarkers == null)
			latentMarkers = new LinkedList<LatentMarker>();
		LatentMarker marker = new LatentMarker(code, start, end, severity, args, cookie);
		latentMarkers.add(marker);
	}
	
	/**
	 * Applies latent markers added as of yet
	 */
	public void applyLatentMarkers() {
		if (latentMarkers != null) {
			for (LatentMarker marker : latentMarkers) {
				if (marker.stillApplies(this)) try {
					marker.apply(this);
				} catch (ParsingException e) { /* does not happen */ }
			}
			latentMarkers = null;
		}
	}
	
	public void warningWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) {
		try {
			markerWithCode(code, errorStart, errorEnd, NO_THROW, IMarker.SEVERITY_WARNING, args);
		} catch (ParsingException e) {
			// ignore
		}
	}
	
	public void warningWithCode(ParserErrorCode code, IRegion errorRegion, Object... args) {
		warningWithCode(code, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), args);
	}
	
	public void errorWithCode(ParserErrorCode code, IRegion errorRegion, Object... args) throws ParsingException {
		errorWithCode(code, errorRegion, 0, args);
	}
	
	public void errorWithCode(ParserErrorCode code, IRegion errorRegion, int flags, Object... args) throws ParsingException {
		errorWithCode(code, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), flags, args);
	}
	
	public void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, int flags, Object... args) throws ParsingException {
		markerWithCode(code, errorStart, errorEnd, flags, IMarker.SEVERITY_ERROR, args);
	}
	
	private void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) throws ParsingException {
		markerWithCode(code, errorStart, errorEnd, NO_THROW, IMarker.SEVERITY_ERROR, args);
	}
	
	public static final int NO_THROW = 1;
	public static final int ABSOLUTE_MARKER_LOCATION = 2;
	
	private static class MarkerInfo {
		public ParserErrorCode code;
		public int start, end;
		public int severity;
		public Object[] args;
		
		private final Declaration cf;
		private final int offset;
		private final ExprElm reporter;
		private final IFile scriptFile;
		private final Script container;
		
		public MarkerInfo(C4ScriptParser parser, ParserErrorCode code, int start, int end, int severity, Object[] args) {
			super();
			this.code = code;
			this.start = start;
			this.end = end;
			this.severity = severity;
			this.args = args;
			
			this.cf = parser.currentDeclaration();
			this.offset = parser.offset;
			this.reporter = parser.getExpressionReportingErrors();
			this.scriptFile = parser.scriptFile;
			this.container = parser.containingScript();
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
			return String.format("%s @(%d, %s)", code.toString(), offset, reporter.toString());
		}
	}
	
	private LinkedList<MarkerInfo> markers = new LinkedList<MarkerInfo>();
	
	private void deployMarkers() {
		if (Core.instance().runsHeadless())
			return;
		final List<MarkerInfo> markersToDeploy = markers;
		markers = new LinkedList<MarkerInfo>();
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				synchronized (markers) {
					for (MarkerInfo m : markersToDeploy) {
						m.deploy();
					}
				}
			}
		});
	}
	
	/**
	 * Create a code marker.
	 * @param code The error code
	 * @param markerStart Start of the marker (relative to function body)
	 * @param markerEnd End of the marker (relative to function body)
	 * @param noThrow true means that no exception will be thrown after creating the marker.
	 * @param severity IMarker severity value
	 * @param args Format arguments used when creating the marker message with the message from the error code as the format.
	 * @return The created marker or null if for some reason it was decided to not create a marker.
	 * @throws ParsingException
	 */
	public void markerWithCode(ParserErrorCode code, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ParsingException {
		if (!errorEnabled(code))
			return;
		if ((flags & ABSOLUTE_MARKER_LOCATION) == 0) {
			int offs = bodyOffset();
			markerStart += offs;
			markerEnd += offs;
		}
		Function cf = currentFunction();
		boolean misplacedErrorOrNoFileToAttachMarkerTo = scriptFile == null || (cf != null && !cf.isOldStyle() && cf.body() != null && this.offset > cf.body().end()+1);
		String problem = code.getErrorString(args);
		if (!misplacedErrorOrNoFileToAttachMarkerTo)
			markers.add(new MarkerInfo(this, code, markerStart, markerEnd, severity, args));
		if ((flags & NO_THROW) == 0 && severity >= IMarker.SEVERITY_ERROR)
			throw misplacedErrorOrNoFileToAttachMarkerTo
				? new SilentParsingException(Reason.SilenceRequested, problem)
				: new ParsingException(problem);
	}
	
	private void tokenExpectedError(String token) throws ParsingException {
		int off = this.offset;
		while (off >= 0 && off < size && buffer.charAt(off) == '\t')
			off--;
		errorWithCode(ParserErrorCode.TokenExpected, off, off+1, ABSOLUTE_MARKER_LOCATION, token);
	}
	
	private boolean parseStaticFieldOperator_() {
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
				errorWithCode(ParserErrorCode.ExpressionExpected, this.offset, this.offset+1);
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
			if (elm == null && parseHexNumber()) {
//				if (parsedNumber < Integer.MIN_VALUE || parsedNumber > Integer.MAX_VALUE)
//					warningWithCode(ErrorCode.OutOfIntRange, elmStart, fReader.getPosition(), String.valueOf(parsedNumber));
				elm = new NumberLiteral(currentFunctionContext.parsedNumber, true);
			}
			
			// id
			if (elm == null && parseID()) {
				elm = new IDLiteral(currentFunctionContext.parsedID);
			}
			
			// number
			if (elm == null && parseNumber()) {
				elm = new NumberLiteral(currentFunctionContext.parsedNumber);
			}
			
			// variable or function
			if (elm == null) {
				String word = readIdent();
				if (word != null && word.length() > 0) {
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
							if (proplDec != null) {
								reportErrorsOf(prototype);
								elm = new NewProplist(proplDec, prototype);
							} else
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
								elm = new BoolLiteral(true);
							else if (word.equals(Keywords.False))
								elm = new BoolLiteral(false);
							else
								// variable
								elm = new AccessVar(word);
						}
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
					if (eatWhitespace() >= 0 && parseID() && eatWhitespace() >= 0 && parseStaticFieldOperator_()) {
						idOffset = this.offset-fieldOperatorStart;
					} else {
						currentFunctionContext.parsedID = null; // reset because that call could have been successful (GetX would be recognized as id)
						seek(idStart);
						idOffset = 0;
					}
					elm = new MemberOperator(currentFunctionContext.parsedMemberOperator.length() == 1, currentFunctionContext.parsedMemberOperator.length() == 3, currentFunctionContext.parsedID, idOffset);
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
							errorWithCode(ParserErrorCode.EmptyParentheses, parenthStartPos, this.offset+1, NO_THROW|ABSOLUTE_MARKER_LOCATION);
						}
						eatWhitespace();
						c = read();
						if (c == ')')
							elm = new Parenthesized(firstExpr);
						else if (c == ',') {
							errorWithCode(ParserErrorCode.TuplesNotAllowed, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION);
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
			if (elm == null && (placeholder = parsePlaceholderString()) != null) {
				elm = new Placeholder(placeholder);
			}
			
			// §{...}
			if (elm == null)
				elm = parseWildcard();
			
			// check if sequence is valid (CreateObject(BLUB)->localvar is not)
			if (elm != null) {
				if (!elm.isValidInSequence(prevElm, this)) {
					elm = null; // blub blub <- first blub is var; second blub is not part of the sequence -.-
					proper = false;
				} else {
					// add to sequence even if not valid so the quickfixer can separate them
					setExprRegionRelativeToFuncBody(elm, elmStart, this.offset);
					elements.add(elm);
					prevElm = elm;
				}
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
			if (result.typeInContext(this) == null) {
				errorWithCode(ParserErrorCode.InvalidExpression, result, NO_THROW);
			}

			if (proper) {
				int saved = this.offset;
				eatWhitespace();
				Operator postop = parseOperator();
				if (postop != null && postop.isPostfix()) {
					UnaryOp op = new UnaryOp(postop, UnaryOp.Placement.Postfix, result);
					setExprRegionRelativeToFuncBody(op, result.start()+bodyOffset(), this.offset);
					return op;
				} else {
					// a binary operator following this sequence
					this.seek(saved);
				}
			}
		} else {
			this.seek(sequenceParseStart);
		}
		
		return result;
		
	}

	private ExprElm parsePropListExpression(boolean reportErrors, ExprElm prevElm) throws ParsingException {
		ProplistDeclaration proplDec = parsePropListDeclaration(reportErrors);
		if (proplDec != null) {
			ExprElm elm = new PropListExpression(proplDec);
			if (currentFunction() != null)
				currentFunction().addOtherDeclaration(proplDec);
			proplDec.setName(elm.toString());
			return elm;
		}
		return null;
	}

	protected ProplistDeclaration parsePropListDeclaration(boolean reportErrors) throws ParsingException {
		int propListStart = offset;
		int c = read();
		if (c == '{') {
			ProplistDeclaration proplistDeclaration = ProplistDeclaration.newAdHocDeclaration();
			proplistDeclaration.setParentDeclaration(currentFunctionContext.currentDeclaration != null ? currentFunctionContext.currentDeclaration : container);
			Declaration oldDec = currentFunctionContext.currentDeclaration;
			currentFunctionContext.currentDeclaration = proplistDeclaration;
			try {
				boolean properlyClosed = false;
				boolean expectingComma = false;
				while (!reachedEOF()) {
					eatWhitespace();
					c = read();
					if (c == ',') {
						if (!expectingComma)
							errorWithCode(ParserErrorCode.UnexpectedToken, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, ","); //$NON-NLS-1$
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
								errorWithCode(ParserErrorCode.UnexpectedToken, this.offset, this.offset+1, ABSOLUTE_MARKER_LOCATION, (char)read());
							}
							eatWhitespace();
							Variable v = new Variable(name, currentFunction() != null ? Scope.VAR : Scope.LOCAL);
							v.setLocation(absoluteSourceLocation(nameStart, nameEnd));
							Declaration outerDec = currentFunctionContext.currentDeclaration;
							currentFunctionContext.currentDeclaration = v;
							ExprElm value = null;
							try {
								v.setParentDeclaration(outerDec);
								value = parseExpression(COMMA_OR_CLOSE_BLOCK, reportErrors);
								if (value == null) {
									errorWithCode(ParserErrorCode.ValueExpected, offset-1, offset);
									value = placeholderExpression(offset);
								}
								v.setInitializationExpression(value);
								v.forceType(value.typeInContext(this));
							} finally {
								currentFunctionContext.currentDeclaration = outerDec;
							}
							proplistDeclaration.addComponent(v);
							expectingComma = true;
						}
						else {
							errorWithCode(ParserErrorCode.TokenExpected, this.offset, this.offset+1, ABSOLUTE_MARKER_LOCATION, Messages.TokenStringOrIdentifier);
							break;
						}
					}
				}
				if (!properlyClosed) {
					errorWithCode(ParserErrorCode.MissingClosingBracket, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, "}"); //$NON-NLS-1$
				}
				proplistDeclaration.setLocation(absoluteSourceLocation(propListStart, offset));
				return proplistDeclaration;
			} finally {
				currentFunctionContext.currentDeclaration = oldDec;
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
					errorWithCode(ParserErrorCode.UnexpectedToken, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION, (char)t);
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
						if (arrayElement != null) {
							arrayElms.add(arrayElement);
						}
						else {
							properlyClosed = false;
							break;
						}
						expectingComma = true;
					}
				}
				if (!properlyClosed) {
					errorWithCode(ParserErrorCode.MissingClosingBracket, this.offset, this.offset+1, "]"); //$NON-NLS-1$
				}
				elm = new ArrayExpression(arrayElms.toArray(new ExprElm[0]));
			}
		} else { 
			unread();
		}
		return elm;
	}

	private void parseRestOfTuple(List<ExprElm> listToAddElementsTo, boolean reportErrors) throws ParsingException {
		boolean expectingComma = false;
		while (!reachedEOF()) {
			eatWhitespace();
			int c = read();
			if (c == ')') {
				if (!expectingComma && listToAddElementsTo.size() > 0)
					listToAddElementsTo.add(ExprElm.nullExpr(this.offset, 0, this));
				break;
			} else if (c == ',') {
				if (!expectingComma) {
					listToAddElementsTo.add(ExprElm.nullExpr(this.offset, 0, this));
				}
				expectingComma = false;
			} else {
				unread();
				if (listToAddElementsTo.size() > 100) {
					errorWithCode(ParserErrorCode.InternalError, this.offset, this.offset, Messages.InternalError_WayTooMuch);
				//	break;
				}
				ExprElm arg = parseExpression(reportErrors);
				if (arg == null) {
					errorWithCode(ParserErrorCode.ExpressionExpected, this.offset, this.offset+1);
					break;
				} else
					listToAddElementsTo.add(arg);
				expectingComma = true;
			}
		}
	}
	
	private ExprElm parseExpression(char[] delimiters, boolean reportErrors) throws ParsingException {
		
		final int offset = this.offset;
		
		final int START = 0;
		final int OPERATOR = 1;
		final int SECONDOPERAND = 2;
		final int DONE = 3;

		currentFunctionContext.parseExpressionRecursion++;
		try {

			ExprElm root = null;
			ExprElm current = null;
			BinaryOp lastOp = null;

			// magical thingie to pass all parameters to inherited
			int exprStart = offset;
			if (parseEllipsis()) {
				root = new Ellipsis();
			} else {
				this.seek(offset);
				eatWhitespace();
				exprStart = this.offset;
				for (int state = START; state != DONE;) {
					switch (state) {
					case START:
						root = parseSequence(reportErrors);
						if (root == null || root.isFinishedProperly()) {
							current = root;
							state = current != null ? OPERATOR : DONE;
						} else {
							state = DONE;
						}
						break;
					case OPERATOR:
						int operatorStartPos = this.offset;
						eatWhitespace();
						// end of expression?
						int c = read();
						for (int i = 0; i < delimiters.length; i++) {
							if (delimiters[i] == c) {
								state = DONE;
								this.seek(operatorStartPos);
								break;
							}
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
							errorWithCode(ParserErrorCode.OperatorNeedsRightSide, lastOp);
							rightSide = placeholderExpression(offset);
						}
						((BinaryOp)current).setRightSide(rightSide);
						lastOp = (BinaryOp)current;
						current = rightSide;
						state = OPERATOR;
						break;
					}
				}
			}
			if (root != null) {
				setExprRegionRelativeToFuncBody(root, exprStart, this.offset);
				// potentially throwing exceptions and stuff
				handleExpressionCreated(reportErrors, root);
			}
			
			return root;

		} finally {
			currentFunctionContext.parseExpressionRecursion--;
		}
	}

	private ExprElm placeholderExpression(final int offset) {
		ExprElm result = new ExprElm();
		setExprRegionRelativeToFuncBody(result, offset, offset+1);
		return result;
	}
	
	private FunctionContext currentFunctionContext = new FunctionContext();

	/**
	 * The expression that is currently reporting errors.
	 * @return The expression reporting errors
	 */
	public ExprElm getExpressionReportingErrors() {
		return currentFunctionContext.expressionReportingErrors;
	}
	
	/**
	 * Convert a region relative to the body offset of the current function to a script-absolute region.
	 * @param flags 
	 * @param region The region to convert
	 * @return The relative region or the passed region, if there is no current function.
	 */
	public IRegion convertRelativeRegionToAbsolute(int flags, IRegion region) {
		int offset = bodyOffset();
		if (offset == 0 || (flags & ABSOLUTE_MARKER_LOCATION) == 0) {
			return region;
		} else {
			return new Region(offset+region.getOffset(), region.getLength());
		}
	}

	private final void handleExpressionCreated(boolean reportErrors, ExprElm root) throws ParsingException {
		root.setAssociatedDeclaration(currentFunctionContext.currentDeclaration);
		root.setFlagsEnabled(ExprElm.STATEMENT_REACHED, currentFunctionContext.statementReached);
		if (reportErrors) {
			reportErrorsOf(root);
		}
		root.setNestingDepth(currentFunctionContext.parseExpressionRecursion);
		if (listener != null && currentFunctionContext.parseExpressionRecursion <= 1) {
			listener.expressionDetected(root, this);
		}
	}

	/**
	 * Let an expression report errors. Calling {@link ExprElm#reportErrors(C4ScriptParser)} indirectly like that ensures
	 * that error markers created will be decorated with information about the expression reporting the error.
	 * @param expression The expression to report errors.
	 * @throws ParsingException
	 * @return The expression parameter is returned to allow for expression chaining. 
	 */
	private <T extends ExprElm> T reportErrorsOf(T expression) throws ParsingException {
		ExprElm saved = currentFunctionContext.expressionReportingErrors;
		currentFunctionContext.expressionReportingErrors = expression;
		try {
			expression.reportErrors(this);
		} finally {
			currentFunctionContext.expressionReportingErrors = saved;
		}
		return expression;
	}
	
	private static final char[] SEMICOLON_DELIMITER = new char[] { ';' };
	private static final char[] OPENING_BLOCK_BRACKET_DELIMITER = new char[] { '{' };
	private static final char[] COMMA_OR_CLOSE_BRACKET = new char[] { ',', ']' };
	private static final char[] COMMA_OR_CLOSE_BLOCK = new char[] { ',', '}' };
	
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
				errorWithCode(ParserErrorCode.StringNotClosed, this.offset-1, this.offset, ABSOLUTE_MARKER_LOCATION);
				break Loop;
			case '"':
				if (!escaped) {
					properEnd = true;
					break Loop;
				}
				break;
			case '\n': case '\r':
				errorWithCode(ParserErrorCode.StringNotClosed, this.offset-1, this.offset, NO_THROW|ABSOLUTE_MARKER_LOCATION);
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
	private Statement parseStatement() throws ParsingException {
		return parseStatement(ParseStatementOption.NoOptions);
	}
	
	/**
	 * Parse a statement with its own type inference block, which will be merged into the passed merger object.
	 * @param merger The merger helper object to merge the exclusively created type information block into.
	 * @return The parsed statement or null if parsing was unsuccessful. 
	 * @throws ParsingException
	 */
	private Statement parseStatementWithOwnTypeInferenceBlock(TypeInformationMerger merger) throws ParsingException {
		// parse comments and attach them to the statement so the comments won't be removed by code reformatting
		List<Comment> prependedComments = new ArrayList<Comment>(3);
		eatWhitespaceReportingComments(prependedComments);
		List<IStoredTypeInformation> block = beginTypeInferenceBlock();
		try {
			Statement s = parseStatement();
			if (s != null)
				s.addAttachments(prependedComments);
			return s;
		} finally {
			block = endTypeInferenceBlock();
			if (listener != null)
				listener.endTypeInferenceBlock(block);
			merger.inject(block);
		}
	}
	
	/**
	 * Parse a statement.
	 * @param options Options on how to parse the statement
	 * @return The parsed statement or null if parsing was unsuccessful.
	 * @throws ParsingException
	 */
	private Statement parseStatement(EnumSet<ParseStatementOption> options) throws ParsingException {
		currentFunctionContext.parseStatementRecursion++;
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
						parseStatementBlock(start, Integer.MAX_VALUE, subStatements, ParseStatementOption.NoOptions, ExpressionsAndStatementsReportingFlavour.AlsoStatements);
						result = new Block(subStatements);
					}
					else if (read == ';') {
						result = new EmptyStatement();
					}
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
							if (!options.contains(ParseStatementOption.InitializationStatement)) {
								result = statementNeedingSemicolon(result);
							}
						}
						else
							result = null;
					}
				}
				else if ((scope = Scope.makeScope(readWord)) != null) {
					List<VarInitialization> initializations = parseVariableDeclaration(true, false, scope, null);
					if (initializations != null) {
						result = new VarDeclarationStatement(initializations, initializations.get(0).variableBeingInitialized.scope());
						if (!options.contains(ParseStatementOption.InitializationStatement)) {
							if (read() != ';') {
								unread();
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
					} else {
						this.seek(afterExpression);
					}
				}
				else
					result = null;
			}

			if (result != null) {
				// inline comment attached to expression so code reformatting does not mess up the user's code too much
				Comment c = getCommentImmediatelyFollowing();
				if (c != null)
					result.setInlineComment(c);
				if (emptyLines > 0)
					result.addAttachment(new Statement.EmptyLinesAttachment(emptyLines));
				
				setExprRegionRelativeToFuncBody(result, start, this.offset);
				handleStatementCreated(result);
			}
			return result;
		} finally {
			currentFunctionContext.parseStatementRecursion--;
		}
		

	}

	private void handleStatementCreated(Statement statement) throws ParsingException {
		statement.setNestingDepth(currentFunctionContext.parseStatementRecursion);
		statement.setFlagsEnabled(ExprElm.STATEMENT_REACHED, currentFunctionContext.statementReached);
		reportErrorsOf(statement);
		if (currentFunctionContext.parseStatementRecursion == 1) {
			if (listener != null) {
				switch (listener.expressionDetected(statement, this)) {
				case Cancel:
					listener = null; // listener doesn't want to hear from me anymore? fine!
					//throw new SilentParsingException(Reason.Cancellation, "Expression Listener Cancellation"); //$NON-NLS-1$
				}
			}
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
	private void parseStatementBlock(int start, int endOfFunc, List<Statement> statements, EnumSet<ParseStatementOption> options, ExpressionsAndStatementsReportingFlavour flavour) throws ParsingException {
		boolean done = false;
		boolean reached = true;
		int garbageStart = -1;
		boolean oldStatementReached = this.currentFunctionContext.statementReached;
		while (!reachedEOF() && this.offset < endOfFunc) {
			this.currentFunctionContext.statementReached = reached;
			int potentialGarbageEnd = offset;
			//eatWhitespace();
			Statement statement = flavour == ExpressionsAndStatementsReportingFlavour.AlsoStatements ? parseStatement(options) : SimpleStatement.wrapExpression(parseExpression());
			if (statement == null) {
				done = currentFunction().isOldStyle() || peek() == '}';
				if (done)
					break;
				if (garbageStart == -1) {
					garbageStart = offset;
				}
				offset++;
				continue;
			} else {
				// garbage recognized before statement: Create a special garbage statement that will report itself
				if (garbageStart != -1)
					garbageStart = maybeAddGarbageStatement(statements, garbageStart, potentialGarbageEnd);
			}
			statements.add(statement);
			boolean statementIsComment = statement instanceof Comment;
			if (reached)
				reached = statement.controlFlow() == ControlFlow.Continue;
			// after first 'real' statement don't expect function description anymore
			if (!statementIsComment)
				options.remove(ParseStatementOption.ExpectFuncDesc);
		}
		if (garbageStart != -1) {
			// contains only garbage ... still add
			maybeAddGarbageStatement(statements, garbageStart, offset);
		}
		if (!done) {
			if (this.offset < endOfFunc)
				errorWithCode(ParserErrorCode.BlockNotClosed, start, start+1);
		} else
			read(); // should be }
		this.currentFunctionContext.statementReached = oldStatementReached;
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
	private Comment getCommentImmediatelyFollowing() {
		int daring = this.offset;
		Comment c = null;
		for (int r = read(); r != -1 && (r == '/' || BufferedScanner.isWhiteSpaceButNotLineDelimiterChar((char) r)); r = read()) {
			if (r == '/') {
				unread();
				c = parseCommentObject();
				break;
			}
		}
		if (c != null)
			return c;
		else {
			this.seek(daring);
			return null;
		}
	}

	/**
	 * Helper class to manage merging type information lists.
	 * @author madeen
	 *
	 */
	public static class TypeInformationMerger {
		
		/**
		 * The result of the merger.
		 */
		private List<IStoredTypeInformation> merged;
		
		private static List<IStoredTypeInformation> mergeTypeInformationLists(List<IStoredTypeInformation> first, List<IStoredTypeInformation> second) {
			for (IStoredTypeInformation info : first) {
				for (Iterator<IStoredTypeInformation> it = second.iterator(); it.hasNext();) {
					IStoredTypeInformation info2 = it.next();
					if (info2.refersToSameExpression(info)) {
						info.merge(info2);
						it.remove();
					}
				}
			}
			first.addAll(second);
			return first;
		}
		
		/**
		 * Inject the given list of type information into the type information list managed by this object.
		 * @param infos The type information to merge in
		 * @return The merged type information list managed by this object.
		 */
		public List<IStoredTypeInformation> inject(List<IStoredTypeInformation> infos) {
			if (merged == null)
				return merged = infos;
			return merged = mergeTypeInformationLists(merged, infos);
		}

		/**
		 * Finish this merger, returning the resulting type information list.
		 * @param finalList The last type information list to merge in. This might also be the result if no other lists have been committed to this object.
		 * @return The resulting list of type information
		 */
		public List<IStoredTypeInformation> finish(List<IStoredTypeInformation> finalList) {
			if (merged == null)
				return finalList;
			return mergeTypeInformationLists(finalList, merged);
		}
		
		/**
		 * Get the result of the merger til now.
		 * @return The merger result
		 */
		public List<IStoredTypeInformation> getResult() {
			return merged;
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
		if (r == null || !r.equals(expected)) {
			tokenExpectedError(expected);
		}
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
		if (keyWord.equals(Keywords.If)) {
			result = parseIf();
		}
		else if (keyWord.equals(Keywords.While)) {
			result = parseWhile();
		}
		else if (keyWord.equals(Keywords.Do)) {
			result = parseDoWhile();
		}
		else if (keyWord.equals(Keywords.For)) {
			result = parseFor();
		}
		else if (keyWord.equals(Keywords.Continue)) {
			result = statementNeedingSemicolon(new ContinueStatement());
			if (currentFunctionContext.currentLoop == null)
				result.setFlagsEnabled(ExprElm.MISPLACED, true);
		}
		else if (keyWord.equals(Keywords.Break)) {
			result = statementNeedingSemicolon(new BreakStatement());
			if (currentFunctionContext.currentLoop == null)
				result.setFlagsEnabled(ExprElm.MISPLACED, true);
		}
		else if (keyWord.equals(Keywords.Return)) {
			result = parseReturn();
		}
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
			if (returnExpr == null) {
				errorWithCode(ParserErrorCode.ValueExpected, this.offset, this.offset+1);				
			}
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
		Variable loopVariable = null;
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
					loopVariable = createVarInScope(varName, Scope.VAR, pos, pos+varName.length(), null);
					handleExpressionCreated(true, accessVar);
					initialization = new SimpleStatement(accessVar);
					setExprRegionRelativeToFuncBody(initialization, pos, pos+varName.length());
					boolean wasEnabled = enableError(ParserErrorCode.NoSideEffects, false);
					currentFunctionContext.parseStatementRecursion++;
					handleStatementCreated(initialization);
					currentFunctionContext.parseStatementRecursion--;
					enableError(ParserErrorCode.NoSideEffects, wasEnabled);
				} else {
					w = null;
				}
			}
			if (w == null) {
				// regularly parse initialization statement
				seek(pos);
				boolean noSideEffectsWasEnabled = enableError(ParserErrorCode.NoSideEffects, false);
				initialization = parseStatement(EnumSet.of(ParseStatementOption.InitializationStatement));
				enableError(ParserErrorCode.NoSideEffects, noSideEffectsWasEnabled);
				if (initialization == null) {
					errorWithCode(ParserErrorCode.ExpectedCode, this.offset, this.offset+1);
				} else if (initialization instanceof VarDeclarationStatement) {
					VarDeclarationStatement decStatement = (VarDeclarationStatement) initialization;
					loopVariable = decStatement.variableInitializations()[0].variableBeingInitialized;
				}
			}
		}

		if (w == null) {
			// determine loop type
			eatWhitespace();
			savedOffset = this.offset;
			if (initialization != null) {
				if (read() == ';') {
					// initialization finished regularly with ';', so no for (... in ...) loop
					savedOffset = this.offset;
				} else {
					unread();
					w = readIdent();
				}
			}
		}
		LoopType loopType;
		if (w != null && w.equals(Keywords.In)) {
			// it's a for (x in array) loop!
			loopType = LoopType.IterateArray;
			eatWhitespace();
			arrayExpr = parseExpression();
			if (arrayExpr == null)
				errorWithCode(ParserErrorCode.ExpressionExpected, savedOffset, this.offset+1);
			else {
				IType t = arrayExpr.typeInContext(this);
				if (!t.canBeAssignedFrom(PrimitiveType.ARRAY))
					warningWithCode(ParserErrorCode.IncompatibleTypes, arrayExpr, t.toString(), PrimitiveType.ARRAY.toString());
				if (loopVariable != null && t instanceof ArrayType) {
					ArrayType arrayType = (ArrayType) t;
					new AccessVar(loopVariable).expectedToBeOfType(arrayType.getGeneralElementType(), this, TypeExpectancyMode.Force);
				}
			}
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
				if (condition == null) {
					errorWithCode(ParserErrorCode.ConditionExpected, savedOffset, this.offset);
				}
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
				if (increment == null) {
					errorWithCode(ParserErrorCode.ExpressionExpected, savedOffset, this.offset+1);
				}
			}
			arrayExpr = null;
		}
		eatWhitespace();
		expect(')');
		eatWhitespace();
		savedOffset = this.offset;
		currentFunctionContext.currentLoop = loopType;
		body = parseStatement();
		if (body == null) {
			errorWithCode(ParserErrorCode.StatementExpected, savedOffset, savedOffset+4);
		}
		switch (loopType) {
		case For:
			loopConditionWarnings(body, condition);
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
	 * Emit warnings about loop conditions that could result in loops never executing or never ending.
	 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
	 * @param condition The loop condition to check
	 */
	private void loopConditionWarnings(Statement body, ExprElm condition) {
		if (body == null || condition == null)
			return;
		Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateAtParseTime(currentFunction()));
		if (Boolean.FALSE.equals(condEv))
			warningWithCode(ParserErrorCode.ConditionAlwaysFalse, condition, condition.toString());
		else if (Boolean.TRUE.equals(condEv)) {
			EnumSet<ControlFlow> flows = body.possibleControlFlows();
			if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
				warningWithCode(ParserErrorCode.InfiniteLoop, body);
		}
	}

	/**
	 * Parse a WhileStatement.
	 * @return The parsed WhileStatement
	 * @throws ParsingException
	 */
	private WhileStatement parseWhile() throws ParsingException {
		int offset;
		WhileStatement result;
		currentFunctionContext.currentLoop = LoopType.While;
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
		if (body == null) {
			errorWithCode(ParserErrorCode.StatementExpected, offset, offset+4);
		}
		loopConditionWarnings(body, condition);
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
		TypeInformationMerger merger = new TypeInformationMerger();
		int offsetBeforeWhitespace = this.offset;
		Statement ifStatement = withMissingFallback(offsetBeforeWhitespace, parseStatementWithOwnTypeInferenceBlock(merger));
		int beforeElse = this.offset;
		eatWhitespace();
		int o = this.offset;
		String nextWord = readIdent();
		Statement elseStatement;
		if (nextWord != null && nextWord.equals(Keywords.Else)) {
			elseStatement = parseStatementWithOwnTypeInferenceBlock(merger);
			if (elseStatement == null) {
				errorWithCode(ParserErrorCode.StatementExpected, o, o+Keywords.Else.length());
			}
		}
		else {
			this.seek(beforeElse); // don't eat comments and stuff after if (...) ...;
			elseStatement = null;
		}
		// merge gathered type information with current list
		currentFunctionContext.storedTypeInformationListStack.push(merger.finish(currentFunctionContext.storedTypeInformationListStack.pop()));
		
		if (!containsConst(condition)) {
			Object condEv = PrimitiveType.BOOL.convert(condition.evaluateAtParseTime(currentFunction()));
			if (condEv != null && condEv != ExprElm.EVALUATION_COMPLEX) {
				warningWithCode(condEv.equals(true) ? ParserErrorCode.ConditionAlwaysTrue : ParserErrorCode.ConditionAlwaysFalse,
						condition, condition.toString());
			}
		}
		result = new IfStatement(condition, ifStatement, elseStatement);
		return result;
	}
	
	private Statement withMissingFallback(int offsetWhereExpected, Statement statement) throws ParsingException {
		return statement != null
			? statement
			: reportErrorsOf(new MissingStatement(offsetWhereExpected-bodyOffset()));
	}

	/**
	 * Check whether the given expression contains a reference to a constant.
	 * @param condition The expression to check
	 * @return Whether the expression contains a constant.
	 */
	private static boolean containsConst(ExprElm condition) {
		if(condition instanceof AccessVar && ((AccessVar)condition).constCondition())
			return true;
		for (ExprElm expression : condition.subElements())
			if(containsConst(expression))
				return true;
		return false;
	}

	/**
	 * Parse an id. On successful parsing, the parsed will be stored in the parsedID field.
	 * @return Whether parsing the id was successful. If false, one can be assured that parsedID will be null.
	 * @throws ParsingException
	 */
	private boolean parseID() throws ParsingException {
		ID id;
		if (offset < size && (id = specialScriptRules.parseId(this)) != null) {
			currentFunctionContext.parsedID = id;
			return true;
		} else {
			currentFunctionContext.parsedID = null; // reset so no old parsed ids get through
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
		
		if (isEngine && parseEllipsis())
			return addVarParmsParm(function);
		
		int s = this.offset;
		String firstWord = readIdent();
		if (firstWord.length() == 0) {
			if (read() == '&') {
				firstWord = "&"; //$NON-NLS-1$
			} else {
				unread();
				return null;
			}
		}
		int e = this.offset;
		Variable var = new Variable(null, Scope.VAR);
		IType type = PrimitiveType.makeType(firstWord);
		if (type == PrimitiveType.REFERENCE && !container.engine().settings().supportsRefs) {
			errorWithCode(ParserErrorCode.EngineDoesNotSupportRefs, s, e, NO_THROW, container.engine().name());
		}
		boolean typeLocked = type != PrimitiveType.UNKNOWN && !isEngine;
		var.forceType(type, typeLocked);
		if (type == PrimitiveType.UNKNOWN) {
			//var.setType(C4Type.ANY);
			var.setName(firstWord);
		}
		else {
			eatWhitespace();
			if (read() == '&') {
				var.forceType(ReferenceType.get(type), typeLocked);
				eatWhitespace();
			} else
				unread();
			int newStart = this.offset;
			String secondWord = readIdent();
			if (secondWord.length() > 0) {
				var.setName(secondWord);
				s = newStart;
				e = this.offset;
			}
			else {
				// type is name
				warningWithCode(ParserErrorCode.TypeAsName, s, e, firstWord);
				var.forceType(PrimitiveType.ANY, typeLocked);
				var.setName(firstWord);
				this.seek(e);
			}
		}
		var.setLocation(new SourceLocation(s, e));
		var.setParentDeclaration(function);
		function.addParameter(var);
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
				errorWithCode(ParserErrorCode.TokenExpected, offset, offset+1, ProplistDeclaration.class.getSimpleName());
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
		synchronized (container) {
			try {
				if (scriptFile != null)
					scriptFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
			container.clearDeclarations();
		}
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
		if (f != null && f.body() != null) {
			return f.body().start();
		} else {
			return 0;
		}
	}
	
	/**
	 * Return the substring of the script that holds the code for function
	 * @param function the function to return the source code of
	 * @return source code
	 */
	protected String functionSource(Function function) {
		if (function == null) {
			return null;
		} else {
			return buffer.substring(function.body().start(), function.body().end());
		}
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
	 * A listener that will be notified if a marker is about to be created.
	 * @author madeen
	 *
	 */
	public interface IMarkerListener {
		/**
		 * Result enum for {@link IMarkerListener#markerEncountered(C4ScriptParser, ParserErrorCode, int, int, int, int, Object...)}
		 * @author madeen
		 *
		 */
		public enum WhatToDo {
			/**
			 * Don't create the marker, the accused is innocent
			 */
			DropCharges,
			/**
			 * Continue creating the marker
			 */
			PassThrough
		}
		/**
		 * Called when a marker is about to be created. The listener gets a chance to do its own processing and possibly order the calling parser to forego creating the actual marker regularly.
		 * @param parser The parser the listener is attached to
		 * @param code the parser error code
		 * @param markerStart start of the marker region
		 * @param markerEnd end of the marker region
		 * @param flags true if the marker wouldn't cause an exception in the parsing process
		 * @param severity IMarker severity value
		 * @param args Arguments used to construct the marker message
		 * @return Returning WhatToDo.DropCharges causes the parser to not create the marker.
		 */
		WhatToDo markerEncountered(C4ScriptParser parser, ParserErrorCode code, int markerStart, int markerEnd, int flags, int severity, Object... args);
	}
	
	/**
	 * Report expressions at the offset the parser is currently at.
	 * This method will try to avoid reparsing the script if the function passed has a cached block that is still valid.
	 * Validity of the cached block is determined by comparing the hash of the original function block source to the hash of the current source fragment specified by the function's body location.
	 * @param funcOrRegion the function to report the code block of. If null, no attempt to avoid reparsing takes place.
	 * @param listener Listener to get informed about reported expressions
	 * @param flavour Whether to only parse statements, or also expressions
	 */
	public void reportExpressionsAndStatements(
		IRegion funcOrRegion,
		final IScriptParserListener listener,
		ExpressionsAndStatementsReportingFlavour flavour,
		boolean reportErrors
	) {
		boolean oldErrorsDisabled = allErrorsDisabled;
		allErrorsDisabled = !reportErrors;
		Function func = funcOrRegion instanceof Function ? (Function)funcOrRegion : null;
		currentFunctionContext.currentDeclaration = func;
		try {
			String functionSource = functionSource(func);
			Block cachedBlock = func != null ? func.codeBlockMatchingSource(functionSource) : null;
			// if block is non-existant or outdated, parse function code and store block
			if (cachedBlock == null) {
				if (func != null)
					func.clearLocalVars();
				strictLevel = containingScript().strictLevel();
				enableErrors(EnumSet.of(
					ParserErrorCode.TokenExpected,
					ParserErrorCode.InvalidExpression,
					ParserErrorCode.BlockNotClosed,
					ParserErrorCode.NotAllowedHere
				), false);
				EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
				beginTypeInferenceBlock();
				LinkedList<Statement> statements = new LinkedList<Statement>();
				parseStatementBlock(offset, Integer.MAX_VALUE, statements, options, flavour);
				cachedBlock = new BunchOfStatements(statements);
				if (func != null) {
					warnAboutPossibleProblemsWithFunctionLocalVariables(func, cachedBlock);
					func.storeBlock(cachedBlock, functionSource);
				}
				applyStoredTypeInformationList(true);
			}
			// traverse block using the listener
			if (cachedBlock != null) {
				if (reportErrors) {
					func.resetLocalVarTypes();
					beginTypeInferenceBlock();
					// traverse with new listener that re-reports errors in addition to forwarding notifications to the actual listener
					// so clients expecting report... to cause marker creation won't be disappoint
					cachedBlock.traverse(new IScriptParserListener() {
						@Override
						public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
							try {
								parser.reportErrorsOf(expression);
							} catch (ParsingException e) {}
							return listener != null ? listener.expressionDetected(expression, parser) : TraversalContinuation.Continue;
						}

						@Override
						public void endTypeInferenceBlock(List<IStoredTypeInformation> typeInfos) {
							if (listener != null) {
								listener.endTypeInferenceBlock(typeInfos);
							}
						}
						
						@Override
						public int minimumParsingRecursion() {
							return listener.minimumParsingRecursion();
						}
					}, this, listener != null ? listener.minimumParsingRecursion() : 1);
					warnAboutPossibleProblemsWithFunctionLocalVariables(func, cachedBlock);
					applyStoredTypeInformationList(true);
				} else {
					// just traverse... this should be faster than reparsing -.-
					cachedBlock.traverse(listener, this, listener.minimumParsingRecursion());
				}
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
	public enum ExpressionsAndStatementsReportingFlavour {
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
		public void markerWithCode(ParserErrorCode code,
				int markerStart, int markerEnd, int flags,
				int severity, Object... args) throws ParsingException {
			if (markerListener != null) {
				if ((flags & ABSOLUTE_MARKER_LOCATION) == 0) {
					markerStart += offsetOfScriptFragment;
					markerEnd += offsetOfScriptFragment;
				}
				if (markerListener.markerEncountered(this, code, markerStart, markerEnd, flags, severity, args) == WhatToDo.DropCharges)
					return;
			}
			super.markerWithCode(code, markerStart, markerEnd, flags, severity, args);
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
	public static C4ScriptParser reportExpressionsAndStatements(
		IDocument doc,
		Script context, IRegion funcOrRegion,
		IScriptParserListener listener, final IMarkerListener markerListener,
		ExpressionsAndStatementsReportingFlavour flavour,
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
		parser.reportExpressionsAndStatements(funcOrRegion, listener, flavour, reportErrors);
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
	public static Statement parseStandaloneStatement(final String statementText, Function context, IScriptParserListener listener, final IMarkerListener markerListener) throws ParsingException {
		if (context == null) {
			Script tempScript = new TempScript(statementText);
			context = new Function("<temp>", null, FunctionScope.GLOBAL); //$NON-NLS-1$
			context.setScript(tempScript);
			context.setBody(new SourceLocation(0, statementText.length()));
		}
		C4ScriptParser tempParser = new ScriptParserWithMarkerListener(statementText, context.script(), markerListener) {
			@Override
			public int bodyOffset() {
				return 0;
			}
		};
		return tempParser.parseStandaloneStatement(statementText, context, listener);
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
	public Statement parseStandaloneStatement(final String statementText, Function context, IScriptParserListener listener) throws ParsingException {
		initScanner(statementText);
		setListener(listener);
		setCurrentFunc(context);
		beginTypeInferenceBlock();
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
		return containingScript();
	}

	@Override
	public int codeFragmentOffset() {
		return bodyOffset();
	}

	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		// yes
	}

	public FunctionContext currentFunctionContext() {
		return currentFunctionContext;
	}
	
}
