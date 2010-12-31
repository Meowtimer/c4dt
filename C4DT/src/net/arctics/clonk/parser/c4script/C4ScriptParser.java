package net.arctics.clonk.parser.c4script;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.CStyleScanner;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SilentParsingException;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.SilentParsingException.Reason;
import net.arctics.clonk.parser.c4script.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener.WhatToDo;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.parser.c4script.ast.ArrayExpression;
import net.arctics.clonk.parser.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.BoolLiteral;
import net.arctics.clonk.parser.c4script.ast.BreakStatement;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ContinueStatement;
import net.arctics.clonk.parser.c4script.ast.ControlFlow;
import net.arctics.clonk.parser.c4script.ast.DoWhileStatement;
import net.arctics.clonk.parser.c4script.ast.Ellipsis;
import net.arctics.clonk.parser.c4script.ast.EmptyStatement;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.GarbageStatement;
import net.arctics.clonk.parser.c4script.ast.ScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.ForStatement;
import net.arctics.clonk.parser.c4script.ast.FunctionDescription;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.IScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.IStoredTypeInformation;
import net.arctics.clonk.parser.c4script.ast.IfStatement;
import net.arctics.clonk.parser.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
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

/**
 * A C4Script parser. Parses declarations in a script and stores it in a C4ScriptBase object (sold separately).
 * The parser can also perform a second parse phase that creates syntax trees from c4script functions. Those can be used for various purposes, including
 * checking correctness (aiming to detect all kinds of errors like undeclared identifiers, supplying values of wrong type to functions etc.), converting old
 * c4script code to #strict-compliant "new-style" code and forming the base of navigation operations like "Find Declaration", "Find References" etc.
 */
public class C4ScriptParser extends CStyleScanner {
	
	public static final int MAX_PAR = 10;
	public static final int MAX_NUMVAR = 20;
	public static final int UNKNOWN_PARAMETERNUM = MAX_PAR+1;
	
	protected IScriptParserListener listener;

	protected IFile scriptFile; // for project intern files
	protected C4ScriptBase container;
	protected C4Declaration currentDeclaration;
	protected int strictLevel;
	
	// parse<Blub>() functions store their results in those
	protected C4ID parsedID;
	protected C4Variable parsedVariable;
	protected long parsedNumber;
	protected String parsedMemberOperator;
	protected String parsedString;
	
	protected int parseExpressionRecursion;
	protected int parseStatementRecursion;
	
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
	 * Whether the current statement is not reached
	 */
	protected boolean statementNotReached;
	/**
	 * matcher for ids obtained from engine configuration id pattern
	 */
	private Matcher idMatcher;

	protected LoopType currentLoop;
	
	/**
	 * Number of unnamed parameters used in activeFunc (Par(5) -> 6 unnamed parameters).
	 * If a complex expression is passed to Par() this variable is set to UNKNOWN_PARAMETERNUM
	 */
	protected int numUnnamedParameters;
	
	private Stack<List<IStoredTypeInformation>> storedTypeInformationListStack = new Stack<List<IStoredTypeInformation>>();
	
	/**
	 * Returns the expression listener that is notified when an expression or a statement has been parsed.
	 * @return the expression listener
	 */
	public IScriptParserListener getListener() {
		return listener;
	}

	/**
	 * Sets the expression listener.
	 * @param listener the new expression listener
	 */
	public void setListener(IScriptParserListener listener) {
		this.listener = listener;
	}
	
	public final boolean hasAppendTo() {
		return appendTo;
	}
	
	public boolean isStatementNotReached() {
		return statementNotReached;
	}
	
	/**
	 * Informs the parser that an unnamed parameter was used by calling the Par() function with the given index expression.
	 * @param index the index expression
	 */
	public void unnamedParamaterUsed(ExprElm index) {
		if (numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
			Object ev = index.evaluateAtParseTime(getContainer());
			if (ev instanceof Number) {
				int number = ((Number)ev).intValue();
				numUnnamedParameters = number >= 0 && number < MAX_PAR ? number+1 : UNKNOWN_PARAMETERNUM;
			} else
				numUnnamedParameters = UNKNOWN_PARAMETERNUM;
		}
	}
	
	public List<IStoredTypeInformation> beginTypeInferenceBlock() {
		List<IStoredTypeInformation> result = new LinkedList<IStoredTypeInformation>();
		storedTypeInformationListStack.push(result);
		return result;
	}
	
	public List<IStoredTypeInformation> endTypeInferenceBlock() {
		return storedTypeInformationListStack.pop();
	}
	
	private final void applyStoredTypeInformationList(List<IStoredTypeInformation> list, boolean soft) {
		if (list == null)
			return;
		for (IStoredTypeInformation info : list) {
			info.apply(soft, this);
		}
	}
	
	public void applyStoredTypeInformationList(boolean soft) {
		applyStoredTypeInformationList(storedTypeInformationListStack.peek(), soft);
	}
	
	/**
	 * Ask the parser to store type information about an expression. No guarantees whether type information will actually be stored.
	 */
	public void storeTypeInformation(ExprElm expression, IType type) {
		IStoredTypeInformation requested = requestStoredTypeInformation(expression);
		if (requested != null) {
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
		if (storedTypeInformationListStack.isEmpty())
			return null;
		for (int i = storedTypeInformationListStack.size()-1; i >= 0; i--) {
			for (IStoredTypeInformation info : storedTypeInformationListStack.get(i)) {
				if (info.expressionRelevant(expression, this))
					return info;
			}
		}
		IStoredTypeInformation newlyCreated = expression.createStoredTypeInformation(this);
		if (newlyCreated != null)
			storedTypeInformationListStack.peek().add(newlyCreated);
		return newlyCreated;
	}

	public List<IStoredTypeInformation> copyCurrentTypeInformationList() {
		try {
			List<IStoredTypeInformation> list = new ArrayList<IStoredTypeInformation>(storedTypeInformationListStack.peek().size());
			for (IStoredTypeInformation info : storedTypeInformationListStack.peek()) {
				list.add((IStoredTypeInformation) info.clone());
			}
			return list;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public void pushTypeInformationList(List<IStoredTypeInformation> list) {
		storedTypeInformationListStack.push(list);
	}
	
	/**
	 * Query the type of an arbitrary expression. With some luck the parser will be able to give an answer.
	 * @param expression the expression to query the type of
	 * @return
	 */
	public IStoredTypeInformation queryStoredTypeInformation(ExprElm expression) {
		if (storedTypeInformationListStack.isEmpty())
			return null;
		for (int i = storedTypeInformationListStack.size()-1; i >= 0; i--) {
			for (IStoredTypeInformation info : storedTypeInformationListStack.get(i)) {
				if (info.expressionRelevant(expression, this))
					return info;
			}
		}
		return null;
	}
	
	public IType queryTypeOfExpression(ExprElm expression, C4Type defaultType) {
		IStoredTypeInformation info = queryStoredTypeInformation(expression);
		return info != null ? info.getType() : defaultType;
	}
	
	/**
	 * Returns the strict level of the script that was specified using the #strict directive.
	 * @return
	 */
	public int getStrictLevel() {
		return strictLevel;
	}
	
	/**
	 * Returns the function that is currently being parsed or otherwise considered "current"
	 * @return the current function
	 */
	public C4Function getCurrentFunc() {
		return currentDeclaration != null ? currentDeclaration.getFirstParentDeclarationOfType(C4Function.class) : null;
	}
	
	/**
	 * Sets the current function. There should be a good reason to call this. 
	 * @param func
	 */
	public void setCurrentFunc(C4Function func) {
		if (func != getCurrentFunc()) {
			currentDeclaration = func;
			numUnnamedParameters = 0;
		}
	}
	
	public C4Variable getCurrentVariableBeingDeclared() {
		return currentDeclaration != null ? currentDeclaration.getFirstParentDeclarationOfType(C4Variable.class) : null;
	}
	
	public C4Declaration getCurrentDeclaration() {
		return currentDeclaration;
	}
	
	/**
	 * Returns the script object that is being parsed by the parser.
	 * @return
	 */
	public final C4ScriptBase getContainer() {
		return container;
	}
	
	/**
	 * Returns the script object as an object if it is one or null if it is not.
	 * @return The script object as  C4Object
	 */
	public C4Object getContainerObject() {
		if (container instanceof C4Object)
			return (C4Object) container;
		return null;
	}
	
	/**
	 * Creates a script parser. The script is read from the file attached to the script (queried through getScriptFile()).
	 */
	public C4ScriptParser(C4ScriptBase script) {
		this((IFile) script.getScriptStorage(), script);
		getInfos();
	}
	
	private static final Pattern DEFAULT_ID_PATTERN = Pattern.compile("");
	
	private void getInfos() {
		if (container != null && container.getEngine() != null) {
			idMatcher = container.getEngine().getCurrentSettings().getCompiledIdPattern().matcher(buffer);
		} else {
			idMatcher = DEFAULT_ID_PATTERN.matcher(buffer);
		}
	}

	/**
	 * Creates a C4Script parser that parses a file within the project.
	 * Results are stored in <code>object</code>
	 * @param scriptFile
	 * @param obj
	 * @throws CompilerException
	 */
	public C4ScriptParser(IFile scriptFile, C4ScriptBase script) {
		super(scriptFile);
		this.scriptFile = scriptFile;
		container = script;
		allErrorsDisabled = C4GroupItem.isLinkedResource(scriptFile);
		getInfos();
	}

	/**
	 * Creates a C4Script parser object for external files.
	 * Results are stored in <code>object</code>
	 * @param stream
	 * @param size
	 * @param object
	 * @throws CompilerException
	 */
	public C4ScriptParser(InputStream stream, C4ScriptBase script) {
		super(stream);
		scriptFile = null;
		container = script;
		getInfos();
	}
	
	/**
	 * Creates a C4Script parser that parses an arbitrary string.
	 * @param withString
	 * @param script
	 */
	public C4ScriptParser(String withString, C4ScriptBase script, IFile scriptFile) {
		super(withString);
		this.scriptFile = scriptFile;
		container = script;
		isEngine = container instanceof C4Engine;
		getInfos();
	}
	
	/**
	 * Perform a full parsing (that includes cleaning up the current state of the script container, parsing declarations and parsing function code).
	 * @throws ParsingException
	 */
	public void parse() throws ParsingException {
		synchronized (container) {
			clean();
			parseDeclarations();
			parseCodeOfFunctionsAndValidate();
		}
	}

	/**
	 * Parse declarations but not function code. Before calling this it should be ensured that the script is cleared to avoid duplicates.
	 */
	public void parseDeclarations() {
		synchronized (container) {
			strictLevel = container.getStrictLevel();
			int offset = 0;
			this.seek(offset);
			enableError(ParserErrorCode.StringNotClosed, false); // just one time error when parsing function code
			try {
				eatWhitespace();
				while(!reachedEOF()) {
					if (!parseDeclaration()) {
						eatWhitespace();
						if (!reachedEOF()) {
							int start = this.offset;
							String tokenText = parseTokenAndReturnAsString();
							errorWithCode(ParserErrorCode.UnexpectedToken, start, this.offset, true, tokenText);
						}
					}
					eatWhitespace();
				}
			}
			catch (ParsingException e) {
				return;
			}
			enableError(ParserErrorCode.StringNotClosed, true);
			container.setDirty(false);
		}
	}
	
	/**
	 * Parse function code. Side effects include:
	 * 	-Errors (or things the parser thinks are errors) listed in the Problems view
	 * 	-Types for variables inferred more or less accurately
	 * @throws ParsingException
	 */
	public void parseCodeOfFunctionsAndValidate() throws ParsingException {
		synchronized (container) {
			
			strictLevel = container.getStrictLevel();
			TypeInformationMerger merger = new TypeInformationMerger();
			for (C4Function function : container.functions()) {
				parseCodeOfFunction(function, merger);
			}
			applyStoredTypeInformationList(merger.getResult(), false);
			currentDeclaration = null;

			for (C4Directive directive : container.directives()) {
				directive.validate(this);
			}
			
			for (C4Variable variable : container.variables()) {
				ExprElm initialization = variable.getInitializationExpression();
				if (initialization != null) {
					initialization.reportErrors(this);
				}
			}
			
			container.setDirty(false);
			distillAdditionalInformation();
		}
	}

	/**
	 * OC: get information out of the script that was previously to be found in additional files (like the name of the definition)
	 */
	public void distillAdditionalInformation() {
		if (container instanceof C4Object) {
			
			C4Object obj = (C4Object) container;
			obj.chooseLocalizedName(); // ClonkRage Names.txt
			
			C4Variable nameLocal = container.findLocalVariable("Name", false);
			if (nameLocal != null) {
				ExprElm expr = nameLocal.getInitializationExpression();
				if (expr != null) {
					obj.setName(expr.evaluateAtParseTime(container).toString());
				} else if (nameLocal.getDefaultValue() instanceof String) {
					obj.setName((String)nameLocal.getDefaultValue());
				}
			}
			
			// find SetProperty call in Definition func
			C4Function definitionFunc = container.findLocalFunction(Keywords.DefinitionFunc, false);
			if (definitionFunc != null && definitionFunc.getBody() != null) { // could also be engine function without body
				this.seek(definitionFunc.getBody().getStart());
				boolean old = allErrorsDisabled;
				allErrorsDisabled = true;
				try {
					reportExpressionsAndStatements(definitionFunc, new ScriptParserListener() {
						@Override
						public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
							// look if it's a plain SetProperty("Name", <value>)
							if (expression instanceof CallFunc) {
								CallFunc callFunc = (CallFunc) expression;
								if (
									callFunc.getDeclarationName().equals("SetProperty") && //$NON-NLS-1$
									callFunc.getParams().length >= 2 &&
									callFunc.getParams()[0] instanceof StringLiteral &&
									((StringLiteral)callFunc.getParams()[0]).getLiteral().equals("Name") //$NON-NLS-1$
								) {
									Object v = callFunc.getParams()[1].evaluateAtParseTime(container);
									if (v instanceof String) {
										container.setName((String) v);
									}
									return TraversalContinuation.Cancel;
								}
							}
							return TraversalContinuation.Continue;
						}
					}, ExpressionsAndStatementsReportingFlavour.AlsoStatements);
					C4Variable v = container.findVariable("Name");
					if (v != null) {
						Object ev = v.evaluateInitializationExpression(container);
						if (ev instanceof String) {
							container.setName((String)ev);
						}
					}
				} finally {
					allErrorsDisabled = old;
				}
			}
		}
	}

	/**
	 * Parse code of one single function. parseCodeOfFunctions() calls this for all functions in the script. 
	 * @param function
	 * @throws ParsingException
	 */
	public void parseCodeOfFunction(C4Function function, TypeInformationMerger merger) throws ParsingException {
		if (function.getBody() == null)
			return;
		try {
			setCurrentFunc(function);
			// reset local vars
			for (C4Variable v : function.getLocalVars()) {
				v.forceType(C4Type.UNKNOWN);
			}
			beginTypeInferenceBlock();
			this.seek(function.getBody().getStart());
			// parse code block
			int endOfFunc = getCurrentFunc().getBody().getEnd();
			EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
			List<Statement> statements = new LinkedList<Statement>();
			parseStatementBlock(offset, endOfFunc, statements, options, ExpressionsAndStatementsReportingFlavour.AlsoStatements);
			BunchOfStatements bunch = new BunchOfStatements(statements);
			if (getCurrentFunc().isOldStyle() && statements.size() > 0)
				getCurrentFunc().getBody().setEnd(statements.get(statements.size()-1).getExprEnd());
			warnAboutUnusedFunctionVariables(bunch);
			
			applyStoredTypeInformationList(false); // apply short-term inference information
			List<IStoredTypeInformation> block = endTypeInferenceBlock();
			if (merger != null) {
				merger.inject(block); // collect information from all functions and apply that after having parsed them all
			}
			if (numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
				getCurrentFunc().createParameters(numUnnamedParameters);
			}
			else if (numUnnamedParameters == UNKNOWN_PARAMETERNUM && (getCurrentFunc().getParameters().size() == 0 || getCurrentFunc().getParameters().get(getCurrentFunc().getParameters().size()-1).isActualParm())) {
				addVarParmsParm(getCurrentFunc());
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
			errorWithCode(ParserErrorCode.InternalError, this.offset, this.offset+1, true, e.getMessage());
		}
	}

	public void warnAboutUnusedFunctionVariables(BunchOfStatements bunch) {
		for (C4Variable v : getCurrentFunc().getLocalVars()) {
			if (!v.isUsed()) {
				for (VarDeclarationStatement decl : bunch.allSubExpressionsOfType(VarDeclarationStatement.class)) {
					for (VarInitialization initialization : decl.getVarInitializations()) {
						if (initialization.variableBeingInitialized == v) {
							ExprElm old = expressionReportingErrors;
							expressionReportingErrors = decl;
							warningWithCode(ParserErrorCode.Unused, initialization, v.getName());
							expressionReportingErrors = old;
							break;
						}
					}
				}
			}
		}
	}

	private void addVarParmsParm(C4Function func) {
		C4Variable v = new C4Variable("...", C4Type.ANY); //$NON-NLS-1$
		v.setParentDeclaration(func);
		func.getParameters().add(v);
	}

	/**
	 * Returns a line a region is in as a string
	 * @param region the region the line to be returned is in
	 * @return the line string
	 */
	public String getLineAt(IRegion region) {
		return this.getLineAt(region);
	}
	
	/**
	 * Returns a substring of the script denoted by a region
	 * @param region the region
	 * @return the substring
	 */
	public String getSubstringOfScript(IRegion region) {
		return this.readStringAt(region.getOffset(), region.getOffset()+region.getLength()+1);
	}

	/**
	 * Parses the declaration at the current this position.
	 * @return whether parsing was successful
	 * @throws ParsingException
	 */
	protected boolean parseDeclaration() throws ParsingException {
		final int offset = this.offset;
		int readByte = read();
		if (readByte == '#') {
			// directive
			String directiveName = this.readStringUntil(BufferedScanner.WHITESPACE_CHARS);
			C4DirectiveType type = C4DirectiveType.makeType(directiveName);
			if (type == null) {
				warningWithCode(ParserErrorCode.UnknownDirective, offset, offset + directiveName.length());
				this.moveUntil(BufferedScanner.NEWLINE_CHARS);
				return true;
			}
			else {
				String content = parseDirectiveParms();
				C4Directive directive = new C4Directive(type, content);
				directive.setLocation(new SourceLocation(offset, this.offset));
				container.addDeclaration(directive);
				if (type == C4DirectiveType.APPENDTO)
					appendTo = true;
				return true;
			}
		}
		else {
			this.seek(offset);
			String word = readIdent();
			if (looksLikeStartOfFunction(word)) {
				if (parseFunctionDeclaration(word, offset))
					return true;
			}
			else if (parseVariableDeclaration(word)) {
				return true;
			}
			else {
				// old-style function declaration without visibility
				eatWhitespace();
				if (read() == ':' && read() != ':') { // no :: -.-
					this.seek(offset); // just let parseFunctionDeclaration parse the name again
					if (parseFunctionDeclaration(Keywords.Public, offset)) // just assume public
						return true;
				}
			}
		}
		this.seek(offset);
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

	/**
	 * Returns whether a parsed word looks like it might be the start of a function declaration (func, private, protected etc.)
	 * @param word
	 * @return
	 */
	private boolean looksLikeStartOfFunction(String word) {
		return word.equals(Keywords.Public) || word.equals(Keywords.Protected) || word.equals(Keywords.Private) || word.equals(Keywords.Global) || word.equals(Keywords.Func);
	}

	private boolean parseVariableDeclaration(String word) throws ParsingException {
		final int offset = this.offset;
		String desc = getTextOfLastComment(offset);

		List<C4Variable> createdVariables = new LinkedList<C4Variable>();
		C4VariableScope scope = C4VariableScope.makeScope(word);
		if (scope == C4VariableScope.VAR) {
			errorWithCode(ParserErrorCode.VarOutsideFunction, offset-scope.toKeyword().length(), offset, true, scope.toKeyword(), Keywords.GlobalNamed, Keywords.LocalNamed);
			scope = C4VariableScope.LOCAL;
		}
		if (scope == C4VariableScope.STATIC || scope == C4VariableScope.LOCAL) {
			eatWhitespace();
			int pos = this.offset;
			if (scope == C4VariableScope.STATIC) {
				if (readIdent().equals(Keywords.Const)) {
					scope = C4VariableScope.CONST;
				} else {
					this.seek(pos);
				}
			}
			do {
				eatWhitespace();
				IType typeOfNewVar;
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
				C4Variable var = null;
				C4Declaration outerDec = currentDeclaration;
				try {
					if (scope == C4VariableScope.CONST || getContainer().getEngine().getCurrentSettings().nonConstGlobalVarsAssignment) {
						eatWhitespace();
						if (peek() == ';' || peek() == ',') {
							if (scope == C4VariableScope.CONST && !isEngine)
								errorWithCode(ParserErrorCode.ConstantValueExpected, this.offset-1, this.offset, true);
							else {
								createdVariables.add(var = createVarInScope(varName, scope, new SourceLocation(s, e), desc));
								currentDeclaration = var;
								if (scope == C4VariableScope.STATIC) {
									var.forceType(C4Type.INT); // most likely
								}
							}
						}
						else {
							expect('=');
							eatWhitespace();
							
							ExprElm varInitialization;
							
							currentDeclaration = var = createVarInScope(varName, scope, new SourceLocation(s, e), desc);
							
							// parse initialization value with all errors disabled so no false errors 
							boolean old = allErrorsDisabled;
							allErrorsDisabled = true;
							try {
								varInitialization = parseExpression(false);
								if (varInitialization == null)
									varInitialization = ERROR_PLACEHOLDER_EXPR;
							} finally {
								allErrorsDisabled = old;
							}
							
							boolean dontBotherEvaluatingConst = false;
							if (scope == C4VariableScope.CONST && !varInitialization.isConstant()) {
								errorWithCode(ParserErrorCode.ConstantValueExpected, varInitialization, true);
								dontBotherEvaluatingConst = true;
							}
							if (!dontBotherEvaluatingConst) {
								try {
									if (scope == C4VariableScope.CONST)
										var.setConstValue(varInitialization.evaluateAtParseTime(getContainer()));
									else
										var.setInitializationExpression(varInitialization);
								} catch (Exception ex) {
									ex.printStackTrace();
									errorWithCode(ParserErrorCode.InvalidExpression, varInitialization);
								}
							}
							createdVariables.add(var);
							typeOfNewVar = varInitialization instanceof IType ? (IType)varInitialization : varInitialization.getType(this);
						}
					}
					else {
						createdVariables.add(var = createVarInScope( varName, C4VariableScope.STATIC, new SourceLocation( s,  e),  desc));
					}
					if (typeOfNewVar != null)
						var.forceType(typeOfNewVar);
					eatWhitespace();
				} finally {
					currentDeclaration = outerDec;
				}
			} while(read() == ',');
			unread();
			if (read() != ';') {
				errorWithCode(ParserErrorCode.CommaOrSemicolonExpected, this.offset-1, this.offset);
			}
		}
		else if (word.equals(Keywords.LocalNamed)) {
			do {
				eatWhitespace();
				int s = this.offset;
				String varName = readIdent();
				int e = this.offset;
				createdVariables.add(createVarInScope( varName, C4VariableScope.LOCAL, new SourceLocation( s,  e),  desc));
				eatWhitespace();
			} while (read() == ',');
			unread();
			if (read() != ';') {
				errorWithCode(ParserErrorCode.CommaOrSemicolonExpected, this.offset-1, this.offset);
			}
		}
		
		// look for comment following directly and decorate the newly created variables with it
		String inlineComment = getTextOfInlineComment();
		if (inlineComment != null) {
			inlineComment = inlineComment.trim();
			for (C4Variable v : createdVariables) {
				v.setUserDescription(inlineComment);
			}
		}
		
		return createdVariables.size() > 0;
	}
	
	private C4Variable findVar(String name, C4VariableScope scope) {
		switch (scope) {
		case VAR:
			return getCurrentFunc().findVariable(name);
		case CONST: case STATIC:
			C4Declaration globalDeclaration = getContainer().getIndex() != null ? getContainer().getIndex().findGlobalDeclaration(name) : null;
			if (globalDeclaration instanceof C4Variable)
				return (C4Variable) globalDeclaration;
			return null;
		case LOCAL:
			return getContainer().findLocalVariable(name, false);
		default:
			return null;
		}
	}
	
	private C4Variable createVarInScope(String varName, C4VariableScope scope, SourceLocation location, String description) {
		C4Variable result = new C4Variable(varName, scope);
		switch (scope) {
		case VAR:
			result.setParentDeclaration(getCurrentFunc());
			getCurrentFunc().getLocalVars().add(result);
			break;
		case CONST: case STATIC: case LOCAL:
			result.setParentDeclaration(getContainer());
			getContainer().addDeclaration(result);
		}
		result.setLocation(location);
		result.setUserDescription(description);
		return result;
	}

	private boolean parseVariableDeclarationInFunc(boolean declaration) throws ParsingException {
		final int offset = this.offset;
		parsedVariable = null;

		String word = readIdent();
		C4VariableScope scope = C4VariableScope.makeScope(word);
		if (scope != null) {
			do {
				eatWhitespace();
				int nameStart = this.offset;
				if (!parseIdentifier()) {
					tokenExpectedError("Identifier");
				}
				String varName = parsedString;
				int nameEnd = this.offset;
				if (declaration) {
					// construct C4Variable object and register it
					C4Variable previousDeclaration = findVar(varName, scope);
					/*if (previousDeclaration == null) {
						if (scope == C4VariableScope.VAR_VAR) {
							if (findVar(varName, C4VariableScope.VAR_LOCAL) != null)
								warningWithCode(ParserErrorCode.IdentShadowed, nameStart, nameEnd, varName, Keywords.LocalNamed + " " + varName);
						}
					}*/
					C4Variable var = previousDeclaration != null ? previousDeclaration : createVarInScope(varName, scope, new SourceLocation(nameStart, nameEnd), null);
					parsedVariable = var;
				}
				// check if there is initial content
				eatWhitespace();
				C4Variable var = getCurrentFunc().findVariable(varName);
				parsedVariable = var;
				if (read() == '=') {
					eatWhitespace();
					try {
						boolean old_ = allErrorsDisabled;
						allErrorsDisabled = true;
						ExprElm val;
						try {
							val = parseExpression(!declaration);
						} finally {
							allErrorsDisabled = old_;
						}
						if (!declaration) {
							if (val == null)
								errorWithCode(ParserErrorCode.ValueExpected, this.offset-1, this.offset, true);
							else {
								var.inferTypeFromAssignment(val, this);
							}
						}
					} catch (ParsingException e) {} // an exception thrown from here will halt the whole parsing process
				}
				else {
					unread();
				}

			} while(read() == ',');
			unread();
			return true;
		}
		else {
			this.seek(offset);
			return false;
		}
	}

	private C4Type parseFunctionReturnType() throws ParsingException {
		final int offset = this.offset;
		eatWhitespace();
		if (peek() == '&') {
			if (!container.getEngine().getCurrentSettings().supportsRefs) {
				errorWithCode(ParserErrorCode.EngineDoesNotSupportRefs, this.offset, this.offset+1, true, container.getEngine().getName());
			}
			read();
			return C4Type.REFERENCE;
		}
		else if (isEngine && parseIdentifier()) {
			C4Type t = C4Type.makeType(parsedString, isEngine);
			if (t != C4Type.UNKNOWN)
				return t;
		}
		this.seek(offset);
		return null;
	}
	
	private int consumeFunctionCodeOrReturnReadChar() throws ParsingException {
		eatWhitespace();
		if (parseVariableDeclarationInFunc(true))
			return 0;
		Token t = parseToken();
		if (t == Token.Symbol)
			return parsedString != null ? parsedString.charAt(0) : 0;
		return 0;
	}

	/**
	 * for optimization reasons
	 * @param firstWord
	 * @return
	 * @throws ParsingException 
	 */
	private boolean parseFunctionDeclaration(String firstWord, int startOfFirstWord) throws ParsingException {
		int endOfHeader;
		String desc = getTextOfLastComment(startOfFirstWord);
		eatWhitespace();
		currentDeclaration = newFunction();
		getCurrentFunc().setScript(container);
		getCurrentFunc().setUserDescription(desc);
		int startName = 0, endName = 0, startBody = 0, endBody = 0;
		boolean suspectOldStyle = false;
		String funcName = null;
		C4Type retType = C4Type.ANY;
		
		if (!firstWord.equals(Keywords.Func)) {
			getCurrentFunc().setVisibility(C4FunctionScope.makeScope(firstWord));
			startName = this.offset;
			String shouldBeFunc = readIdent();
			if (!shouldBeFunc.equals(Keywords.Func)) {
				suspectOldStyle = true; // suspicious
				funcName = shouldBeFunc;
				endName = this.offset;
				warningWithCode(ParserErrorCode.OldStyleFunc, startName, endName);
			}
		}
		else {
			getCurrentFunc().setVisibility(C4FunctionScope.PUBLIC);
		}
		if (!suspectOldStyle) {
			retType = parseFunctionReturnType();
			if (retType == null)
				retType = C4Type.ANY;
			eatWhitespace();
			startName = this.offset;
			funcName = readIdent();
			if (funcName == null || funcName.length() == 0)
				errorWithCode(ParserErrorCode.NameExpected, this.offset-1, this.offset);
			endName = this.offset;
		}
		getCurrentFunc().setName(funcName);
		getCurrentFunc().setReturnType(retType);
		getCurrentFunc().setOldStyle(suspectOldStyle);
		eatWhitespace();
		int shouldBeBracket = read();
		if (shouldBeBracket != '(') {
			if (suspectOldStyle && shouldBeBracket == ':') {
				// old style funcs have no named parameters
			} else {
				tokenExpectedError("("); //$NON-NLS-1$
			}
		} else {
			// get parameters
			do {
				eatWhitespace();
				parseParameter(getCurrentFunc());
				eatWhitespace();
				int readByte = read();
				if (readByte == ')')
					break; // all parameters parsed
				else if (readByte == ',')
					continue; // parse another parameter
				else {
					errorWithCode(ParserErrorCode.TokenExpected, this.offset-1, this.offset, String.format(Messages.C4ScriptParser_Or, ")", ",")); //$NON-NLS-2$ //$NON-NLS-3$
				}
			} while(!reachedEOF());
		}
		endOfHeader = this.offset;
		lastComment = null;
		eatWhitespace();
		if (lastComment != null)
			getCurrentFunc().setUserDescription(lastComment.getComment());
		// parse code block
		int token = read();
		if (token != '{') {
			if (suspectOldStyle) {
				this.seek(endOfHeader);
				startBody = this.offset;
				// body goes from here to start of next function...
				do {
					eatWhitespace();
					endBody = this.offset;
					String word = readIdent();
					if (word != null && word.length() > 0) {
						if (looksLikeStartOfFunction(word) || looksLikeVarDeclaration(word)) {
							this.seek(endBody);
							break;
						} else {
							eatWhitespace();
							if (read() == ':' && read() != ':') {
								this.seek(endBody);
								break;
							} else {
								this.seek(endBody);
							}
						}
					}
					// just move on
					consumeFunctionCodeOrReturnReadChar();

					endBody = this.offset; // blub
				} while (!reachedEOF());
			} else {
				if (isEngine) {
					// engine functions don't need a body
					if (token != ';')
						errorWithCode(ParserErrorCode.TokenExpected, this.offset-1, this.offset, ";"); //$NON-NLS-1$
					startBody = endBody = -1;
				}
				else {
					errorWithCode(ParserErrorCode.TokenExpected, this.offset-1, this.offset, "{"); //$NON-NLS-1$
				}
			}
		} else {
			// body in {...}
			int blockDepth = 0;
			startBody = this.offset;
			eatWhitespace();

			// new two pass strategy to be able to check if functions and variables exist
			// first pass: skip the code, just remember where it is
			boolean foundLast;
			do {
				int c = consumeFunctionCodeOrReturnReadChar();
				if (c == '}')
					blockDepth--;
				else if (c == '{')
					blockDepth++;
				foundLast = blockDepth == -1;
			} while (!(foundLast || reachedEOF()));
			if (foundLast)
				unread(); // go back to last '}'

			endBody = this.offset;
			eatWhitespace();
			if (read() != '}') {
				int pos = Math.min(this.offset, getBufferLength()-1);
				errorWithCode(ParserErrorCode.TokenExpected, pos, pos+1, "}"); //$NON-NLS-1$
				return false;
			}
			// look for comment in the same line as the closing '}' which is common for functions packed into one line
			// hopefully there won't be multi-line functions with such a comment attached at the end
			Comment c = getCommentImmediatelyFollowing();
			if (c != null)
				getCurrentFunc().setUserDescription(c.getComment());
		}
		// finish up
		getCurrentFunc().setLocation(new SourceLocation(startName,endName));
		getCurrentFunc().setBody(startBody != -1 ? new SourceLocation(startBody,endBody) : null);
		getCurrentFunc().setHeader(new SourceLocation(startOfFirstWord, endOfHeader));
		container.addDeclaration(getCurrentFunc());
		if (!getCurrentFunc().isOldStyle())
			currentDeclaration = null; // to not suppress errors in-between functions
		return true;
	}

	protected C4Function newFunction() {
	    return new C4Function();
    }

	private String getTextOfLastComment(int declarationOffset) {
		String desc = (lastComment != null && lastComment.precedesOffset(declarationOffset, getBuffer())) ? lastComment.getComment().trim() : null; //$NON-NLS-1$
		lastComment = null;
		return desc;
	}
	
	private String getTextOfInlineComment() {
		int pos = this.offset;
		this.eat(BufferedScanner.WHITESPACE_WITHOUT_NEWLINE_CHARS);
		if (this.eat(BufferedScanner.NEWLINE_CHARS) == 0) {
			Comment c = parseCommentObject();
			if (c != null)
				return c.getComment();
		}
		this.seek(pos);
		return null;
	}

	private boolean looksLikeVarDeclaration(String word) {
		return word.equals(Keywords.GlobalNamed) || word.equals(Keywords.LocalNamed);
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
		}
		return isHex;
	}
	
	private boolean parseNumber() throws ParsingException {
		final int offset = this.offset;
		int count = 0;
		do {
			int readByte = read();
			if ('0' <= readByte && readByte <= '9') {
				count++;
				continue;
			}
			else {
				unread();
				if (count > 0) {
					break;
				} else {
					parsedNumber = -1; // unlikely to be parsed
					return false; // well, this seems not to be a number at all
				} 
			}
		} while(!reachedEOF());
		this.seek(offset);
		String numberString = this.readString(count);
		try {
			parsedNumber = Long.parseLong(numberString);
		} catch (NumberFormatException e) {
			parsedNumber = Integer.MAX_VALUE;
			errorWithCode(ParserErrorCode.NotANumber, offset, offset+count, true, numberString);
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
	
	private boolean parseMemberOperator() {
		int offset = this.offset;
		int firstChar = read();
		if (firstChar == '.') {
			parsedMemberOperator = "."; //$NON-NLS-1$
			return true;
		}
		else if (firstChar == '-') {
			if (read() == '>') {
				offset = this.offset;
				eatWhitespace();
				if (read() == '~')
					parsedMemberOperator = "->~"; //$NON-NLS-1$
				else {
					parsedMemberOperator = "->"; //$NON-NLS-1$
					this.seek(offset);
				}
				return true;
			}
		}
		this.seek(offset);
		return false;
	}

	private static final class TempScript extends C4ScriptBase {
		private final String expression;
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		private static final ClonkIndex tempIndex = new ClonkIndex() {
			private static final long serialVersionUID = 1L;
			private final C4Engine tempEngine = new C4Engine("Temp Engine");
			@Override
			public C4Engine getEngine() {
				return tempEngine;
			};
		};

		private TempScript(String expression) {
			this.expression = expression;
		}

		@Override
		public ClonkIndex getIndex() {
			return tempIndex;
		}

		@Override
		public IStorage getScriptStorage() {
			try {
				return new SimpleScriptStorage(expression, expression);
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
	}

	/**
	 * loop types
	 */
	public enum LoopType {
		For,
		IterateArray, // for (x in y)
		While
	}

	public enum Token {
		String,
		Word,
		ID,
		Number,
		Operator,
		Symbol
	}
	
	public Token parseToken() throws ParsingException {
		if (parseString())
			return Token.String;
		String word = readIdent();
		if (word.length() > 0) {
			parsedString = word;
			return Token.Word;
		}
		if (parseID())
			return Token.ID;
		if (parseNumber())
			return Token.Number;
		C4ScriptOperator op;
		if ((op = parseOperator()) != null) {
			parsedString = op.getOperatorName();
			return Token.Operator;
		}
		parsedString = this.readString(1);
		return Token.Symbol;
	}

	public String lastTokenAsString(Token token) {
		switch (token) {
		case ID: return parsedID.getName();
		case Number: return String.valueOf(parsedNumber);
		case String: return "\""+parsedString+"\""; //$NON-NLS-1$ //$NON-NLS-2$
		case Word: case Symbol: case Operator: return parsedString;
		}
		return ""; //$NON-NLS-1$
	}
	
	public String parseTokenAndReturnAsString() throws ParsingException {
		return lastTokenAsString(parseToken());
	}

	/**
	 * read operator at some location
	 * @param offset
	 * @return the operator referenced in the code at offset
	 */
	private C4ScriptOperator parseOperator() {
		final int offset = this.offset;
		final char[] chars = new char[] { (char)read(), (char)read()  };
		String s = new String(chars);
		
		// never to be read as an operator
		if (s.equals("->")) { //$NON-NLS-1$
			this.seek(offset);
			return null;
		}

		C4ScriptOperator result = C4ScriptOperator.getOperator(s);
		if (result != null) {
			// new_variable should not be parsed as ne w_variable -.-
			if (result == C4ScriptOperator.ne || result == C4ScriptOperator.eq) {
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
		result = C4ScriptOperator.getOperator(s);
		if (result != null) {
			unread();
			return result;
		}

		this.seek(offset);
		return null;
	}
	
	private Set<ParserErrorCode> disabledErrors = new HashSet<ParserErrorCode>();
	
	private void enableError(ParserErrorCode error, boolean doEnable) {
		if (doEnable)
			disabledErrors.remove(error);
		else
			disabledErrors.add(error);
	}
	
	public boolean errorDisabled(ParserErrorCode error) {
		return allErrorsDisabled || disabledErrors.contains(error);
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
				return ((C4Function)cookie).tooManyParameters((Integer)args[1]);
			default:
				return false;
			}
		}
		public void apply(C4ScriptParser parser) throws ParsingException {
			parser.markerWithCode(code, start, end, true, severity, args);
		}
	}
	
	private Collection<LatentMarker> latentMarkers;
	
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
	
	public IMarker warningWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) {
		try {
			return markerWithCode(code, errorStart, errorEnd, true, IMarker.SEVERITY_WARNING, args);
		} catch (ParsingException e) {
			// ignore
			return null;
		}
	}
	
	public IMarker warningWithCode(ParserErrorCode code, IRegion errorRegion, Object... args) {
		return warningWithCode(code, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), args);
	}
	
	public IMarker errorWithCode(ParserErrorCode code, IRegion errorRegion, Object... args) throws ParsingException {
		return errorWithCode(code, errorRegion, false, args);
	}
	
	public IMarker errorWithCode(ParserErrorCode code, IRegion errorRegion, boolean noThrow, Object... args) throws ParsingException {
		return errorWithCode(code, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), noThrow, args);
	}
	
	public IMarker errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, boolean noThrow, Object... args) throws ParsingException {
		return markerWithCode(code, errorStart, errorEnd, noThrow, IMarker.SEVERITY_ERROR, args);
	}
	
	private IMarker errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) throws ParsingException {
		return markerWithCode(code, errorStart, errorEnd, false, IMarker.SEVERITY_ERROR, args);
	}
	
	public IMarker markerWithCode(ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) throws ParsingException {
		if (errorDisabled(code)) {
			return null;
		}
		IMarker result = null;
		boolean misplacedErrorOrNoFileToAttachMarkerTo = scriptFile == null || (getCurrentFunc() != null && getCurrentFunc().getBody() != null && this.offset > getCurrentFunc().getBody().getEnd()+1);
		String problem = code.getErrorString(args);
		if (!misplacedErrorOrNoFileToAttachMarkerTo && !noThrow) {
			result = code.createMarker(scriptFile, getContainer(), ClonkCore.MARKER_C4SCRIPT_ERROR, markerStart, markerEnd, severity, expressionReportingErrors, args);
			IRegion exprLocation = getLocationOfExpressionReportingErrors();
			if (exprLocation != null) {
				ParserErrorCode.setExpressionLocation(result, exprLocation);
			}
			problem = result.getAttribute(IMarker.MESSAGE, "<Fail>");
		} else {
			problem = code.getErrorString(args);
		}
		if (!noThrow && severity >= IMarker.SEVERITY_ERROR) {
			throw misplacedErrorOrNoFileToAttachMarkerTo
				? new SilentParsingException(Reason.SilenceRequested, problem)
				: new ParsingException(problem);
		}
		return result;
	}
	
	private void tokenExpectedError(String token) throws ParsingException {
		errorWithCode(ParserErrorCode.TokenExpected, this.offset-1, this.offset, false, token);
	}
	
	private boolean parseStaticFieldOperator_() {
		final int offset = this.offset;
		String o = this.readString(2);
		if (o != null && o.equals("::")) //$NON-NLS-1$
			return true;
		this.seek(offset);
		return false;
	}
	
	public ExprElm parseExpressionWithoutOperators(boolean reportErrors) throws ParsingException {
		int beforeWhitespaceStart = this.offset;
		eatWhitespace();
		int sequenceStart = this.offset;
		C4ScriptOperator preop = parseOperator();
		ExprElm result = null;
		if (preop != null && preop.isPrefix()) {
			ExprElm followingExpr = parseExpressionWithoutOperators(reportErrors);
			if (followingExpr == null) {
				errorWithCode(ParserErrorCode.ExpressionExpected, this.offset, this.offset+1);
			}
			result = new UnaryOp(preop, UnaryOp.Placement.Prefix, followingExpr);
		} else
			this.seek(sequenceStart); // don't skip operators that aren't prefixy
		if (result != null) {
			result.setExprRegion(sequenceStart, this.offset);
			return result;
		}
		Vector<ExprElm> elements = new Vector<ExprElm>(5);
		ExprElm elm;
		ExprElm prevElm = null;
		int noWhitespaceEating = sequenceStart;
		boolean proper = true;
		do {
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
			if (elements.size() > 0 && readIdent().equals(Keywords.In)) {
				this.seek(elmStart);
				break;
			}
			this.seek(elmStart); // nothing special to end the sequence; make sure we start from the beginning
			
			// hex number
			if (elm == null && parseHexNumber()) {
//				if (parsedNumber < Integer.MIN_VALUE || parsedNumber > Integer.MAX_VALUE)
//					warningWithCode(ErrorCode.OutOfIntRange, elmStart, fReader.getPosition(), String.valueOf(parsedNumber));
				elm = new NumberLiteral(parsedNumber, true);
			}
			
			// id
			if (elm == null && parseID()) {
				elm = new IDLiteral(parsedID);
			}
			
			// number
			if (elm == null && parseNumber()) {
				elm = new NumberLiteral(parsedNumber);
			}
			
			// variable or function
			if (elm == null) {
				String word = readIdent();
				if (word != null && word.length() > 0) {
					int beforeSpace = this.offset;
					eatWhitespace();
					if (read() == '(') {
						int s = this.offset;
						// function call
						List<ExprElm> args = new LinkedList<ExprElm>();
						parseRestOfTuple(args, reportErrors);
						CallFunc callFunc = new CallFunc(word, args.toArray(new ExprElm[args.size()]));
						callFunc.setParmsRegion(s, this.offset-1);
						elm = callFunc;
					} else {
						this.seek(beforeSpace);
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
			
			// string
			if (elm == null && parseString()) {
				elm = new StringLiteral(parsedString);
			}
			
			// array
			if (elm == null) {
				elm = parseArrayExpression(reportErrors, prevElm);
			}
			
			if (elm == null) {
				elm = parsePropListExpression(reportErrors, prevElm);
			}
		
			// ->
			if (elm == null) {
				int fieldOperatorStart = this.offset;
				if (parseMemberOperator()) {
					eatWhitespace();
					int idStart = this.offset;
					int idOffset;
					if (parseID() && eatWhitespace() >= 0 && parseStaticFieldOperator_()) {
						idOffset = this.offset-fieldOperatorStart;
					} else {
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
					ExprElm firstExpr = parseExpression(reportErrors);
					if (firstExpr == null) {
						firstExpr = ExprElm.nullExpr(this.offset, 0);
						// might be disabled
						errorWithCode(ParserErrorCode.EmptyParentheses, parenthStartPos, this.offset+1, true);
					}
					eatWhitespace();
					c = read();
					if (c == ')')
						elm = new Parenthesized(firstExpr);
					else if (c == ',') {
						errorWithCode(ParserErrorCode.TuplesNotAllowed, this.offset-1, this.offset);
						// tuple (just for multiple parameters for return)
						List<ExprElm> tupleElms = new LinkedList<ExprElm>();
						tupleElms.add(firstExpr);
						parseRestOfTuple(tupleElms, reportErrors);
						elm = new Tuple(tupleElms.toArray(new ExprElm[0]));
					} else
						errorWithCode(ParserErrorCode.TokenExpected, this.offset-1, this.offset, ")"); //$NON-NLS-1$
				} else {
					unread();
				}
			}
			
			if (elm == null && parsePlaceholderString()) {
				elm = new Placeholder(parsedString);
			}
			
			// check if sequence is valid (CreateObject(BLUB)->localvar is not)
			if (elm != null) {
				if (!elm.isValidInSequence(prevElm, this)) {
					elm = null; // blub blub <- first blub is var; second blub is not part of the sequence -.-
				/*	final ExprElm old = expressionReportingErrors;
					expressionReportingErrors = elm;
					try {
						errorWithCode(ParserErrorCode.NotAllowedHere, elmStart, getPosition(), true, this.readStringAt(elmStart, getPosition()));
					} finally {
						expressionReportingErrors = old;
					} */
					//this.seek(elmStart);
					proper = false;
				} else {
					// add to sequence even if not valid so the quickfixer can separate them
					elm.setExprRegion(elmStart, this.offset);
					elements.add(elm);
					prevElm = elm;
				}
			}

		} while (elm != null);
		this.seek(noWhitespaceEating);
		if (elements.size() == 1) {
			// no need for sequences containing one element
			result = elements.elementAt(elements.size()-1);
		}
		else if (elements.size() > 1) {
			result = new Sequence(elements.toArray(new ExprElm[0]));
		} else {
			result = null;
		}
		if (result != null) {
			result.setFinishedProperly(proper);

			result.setExprRegion(sequenceStart, this.offset);
			if (result.getType(this) == null) {
				errorWithCode(ParserErrorCode.InvalidExpression, result);
			}

			if (proper) {
				int saved = this.offset;
				eatWhitespace();
				C4ScriptOperator postop = parseOperator();
				if (postop != null && postop.isPostfix()) {
					UnaryOp op = new UnaryOp(postop, UnaryOp.Placement.Postfix, result);
					op.setExprRegion(result.getExprStart(), this.offset);
					return op;
				} else {
					// a binary operator following this sequence
					this.seek(saved);
				}
			}
		} else {
			this.seek(beforeWhitespaceStart);
		}
		
		return result;
		
	}

	private ExprElm parsePropListExpression(boolean reportErrors, ExprElm prevElm) throws ParsingException {
		ExprElm elm = null;
		int c = read();
		if (c == '{') {
			ProplistDeclaration proplistDeclaration = new ProplistDeclaration(new ArrayList<C4Variable>(10));
			proplistDeclaration.setParentDeclaration(currentDeclaration);
			C4Declaration oldDec = currentDeclaration;
			currentDeclaration = proplistDeclaration;
			try {
				boolean properlyClosed = false;
				boolean expectingComma = false;
				while (!reachedEOF()) {
					eatWhitespace();
					c = read();
					if (c == ',') {
						if (!expectingComma)
							errorWithCode(ParserErrorCode.UnexpectedToken, this.offset-1, this.offset, ","); //$NON-NLS-1$
						expectingComma = false;
					} else if (c == '}') {
						properlyClosed = true;
						break;
					} else {
						unread();
						int nameStart = this.offset;
						if (parseString() || parseIdentifier()) {
							String name = parsedString;
							int nameEnd = this.offset;
							eatWhitespace();
							int c_ = read();
							if (c_ != ':' && c_ != '=') {
								unread();
								errorWithCode(ParserErrorCode.UnexpectedToken, this.offset, this.offset+1, (char)read());
							}
							eatWhitespace();
							C4Variable v = new C4Variable(name, getCurrentFunc() != null ? C4VariableScope.VAR : C4VariableScope.LOCAL);
							v.setLocation(new SourceLocation(nameStart, nameEnd));
							//System.out.println("var " + v.getName() + " parent: " + currentDeclaration().toString() + " - " + getContainer().toString());
							C4Declaration outerDec = currentDeclaration;
							currentDeclaration = v;
							try {
								v.setParentDeclaration(outerDec);
								ExprElm expr = parseExpression(COMMA_OR_CLOSE_BLOCK, reportErrors);
								if (expr == null) {
									errorWithCode(ParserErrorCode.ValueExpected, offset-1, offset);
								}
								v.setInitializationExpression(expr);
								v.forceType(expr.getType(this));
							} finally {
								currentDeclaration = outerDec;
							}
							proplistDeclaration.getComponents().add(v);
							expectingComma = true;
						}
						else {
							errorWithCode(ParserErrorCode.TokenExpected, this.offset, this.offset+1, Messages.TokenStringOrIdentifier);
							break;
						}
					}
				}
				if (!properlyClosed) {
					errorWithCode(ParserErrorCode.MissingClosingBracket, this.offset-1, this.offset, "}"); //$NON-NLS-1$
				}
				elm = new PropListExpression(proplistDeclaration);
			} finally {
				currentDeclaration = oldDec;
			}
			if (getCurrentFunc() != null) {
				getCurrentFunc().addOtherDeclaration(proplistDeclaration);
			}
		}
		else
			unread();
		return elm;
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
					errorWithCode(ParserErrorCode.UnexpectedToken, this.offset-1, this.offset, new Character((char) t).toString());
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
					listToAddElementsTo.add(ExprElm.nullExpr(this.offset, 0));
				break;
			} else if (c == ',') {
				if (!expectingComma) {
					listToAddElementsTo.add(ExprElm.nullExpr(this.offset, 0));
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
//					break;
				} else
					listToAddElementsTo.add(arg);
				expectingComma = true;
			}
		}
	}
	
	/*
	 * ERROR_PLACEHOLDER_EXPR: is always at the reader's current location
	 */
	private final ExprElm ERROR_PLACEHOLDER_EXPR = new ExprElm() {
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		@Override
		public int getExprStart() {
			return C4ScriptParser.this.offset;
		}
		@Override
		public int getExprEnd() {
			return C4ScriptParser.this.offset+1;
		}
	};
	
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
			if (parseEllipsis()) {
				root = new Ellipsis();
			} else {
				this.seek(offset);
				eatWhitespace();
				exprStart = this.offset;
				for (int state = START; state != DONE;) {
					switch (state) {
					case START:
						root = parseExpressionWithoutOperators(reportErrors);
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
							C4ScriptOperator op = parseOperator();
							if (op != null && op.isBinary()) {
								int priorOfNewOp = op.getPriority();
								ExprElm newLeftSide = null;
								BinaryOp theOp = null;
								for (ExprElm opFromBottom = current.getParent(); opFromBottom instanceof BinaryOp; opFromBottom = opFromBottom.getParent()) {
									BinaryOp oneOp = (BinaryOp) opFromBottom;
									if (priorOfNewOp > oneOp.getOperator().getPriority() || (priorOfNewOp == oneOp.getOperator().getPriority() && op.isRightAssociative())) {
										theOp = oneOp;
										break;
									}
								}
								if (theOp != null) {
									newLeftSide = theOp.getRightSide();
									current = lastOp = new BinaryOp(op);
									theOp.setRightSide(current);
								} else {
									newLeftSide = root;
									current = root = lastOp = new BinaryOp(op);
								}
								lastOp.setLeftSide(newLeftSide);
								lastOp.setExprRegion(operatorStartPos, this.offset);
								state = SECONDOPERAND;
							} else {
								this.seek(operatorStartPos); // in case there was an operator but not a binary one
								state = DONE;
							}
						}
						break;
					case SECONDOPERAND:
						ExprElm rightSide = parseExpressionWithoutOperators(reportErrors);
						if (rightSide == null)
							errorWithCode(ParserErrorCode.OperatorNeedsRightSide, lastOp);
						((BinaryOp)current).setRightSide(rightSide);
						lastOp = (BinaryOp)current;
						current = rightSide;
						state = OPERATOR;
						break;
					}
				}
			}
			if (root != null) {
				root.setExprRegion(exprStart, this.offset);
				// potentially throwing exceptions and stuff
				handleExpressionCreated(reportErrors, root);
			}
			
			return root;

		} finally {
			parseExpressionRecursion--;
		}
	}
	
	private transient ExprElm expressionReportingErrors;
	
	public IRegion getLocationOfExpressionReportingErrors() {
		if (expressionReportingErrors != null) {
			if (offsetOfScriptFragment() == 0) {
				return expressionReportingErrors;
			} else {
				return new Region(
					offsetOfScriptFragment()+expressionReportingErrors.getExprStart(),
					expressionReportingErrors.getLength()
				);
			}
		} else {
			return null;
		}
	}

	private final void handleExpressionCreated(boolean reportErrors, ExprElm root) throws ParsingException {
		root.setAssociatedDeclaration(currentDeclaration);
		if (reportErrors) {
			reportErrorsOf(root);
		}
		if (listener != null && parseExpressionRecursion <= 1)
			listener.expressionDetected(root, this);
	}

	private void reportErrorsOf(ExprElm expression) throws ParsingException {
		ExprElm saved = expressionReportingErrors;
		expressionReportingErrors = expression;
		try {
			expression.reportErrors(this);
		} finally {
			expressionReportingErrors = saved;
		}
	}
	
	private void reportErrorsWithErrorDisabled(ExprElm expression, ParserErrorCode code) throws ParsingException {
		ExprElm saved = expressionReportingErrors;
		expressionReportingErrors = expression;
		boolean wasDisabled = errorDisabled(code);
		enableError(code, false);
		try {
			expression.reportErrors(this);
		} finally {
			expressionReportingErrors = saved;
			enableError(code, !wasDisabled);
		}
	}
	
	private static final char[] SEMICOLON_DELIMITER = new char[] { ';' };
	private static final char[] COMMA_OR_CLOSE_BRACKET = new char[] { ',', ']' };
	private static final char[] COMMA_OR_CLOSE_BLOCK = new char[] { ',', '}' };
	
	private ExprElm parseExpression(boolean reportErrors) throws ParsingException {
		return parseExpression(SEMICOLON_DELIMITER, reportErrors);
	}
	
	private ExprElm parseExpression() throws ParsingException {
		return parseExpression(true);
	}
	
	private static final char[] getQuotesAndNewLineChars() {
		char[] result = new char[1+BufferedScanner.NEWLINE_CHARS.length];
		result[0] = '"';
		System.arraycopy(BufferedScanner.NEWLINE_CHARS, 0, result, 1, BufferedScanner.NEWLINE_CHARS.length);
		return result;
	}
	private static final char[] QUOTES_AND_NEWLINE_CHARS = getQuotesAndNewLineChars();
	
	private boolean parseString() throws ParsingException {
		int quotes = read();
		if (quotes != '"') {
			unread();
			return false;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(this.readString(1));
			builder.append(this.readStringUntil(QUOTES_AND_NEWLINE_CHARS));
			if (BufferedScanner.isLineDelimiterChar((char) peek())) {
				errorWithCode(ParserErrorCode.StringNotClosed, this.offset-1, this.offset, true);
				return true;
			}
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		if (read() != '"') {
			errorWithCode(ParserErrorCode.StringNotClosed, this.offset-1, this.offset);
		}
		parsedString = builder.toString();
		return true;
	}
	
	private boolean parseIdentifier() throws ParsingException {
		String word = readIdent();
		if (word != null && word.length() > 0) {
			parsedString = word;
			return true;
		}
		return false;
	}
	
	private boolean parsePlaceholderString() throws ParsingException {
		int delimiter = read();
		if (delimiter != '$') {
			unread();
			return false;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(this.readString(1));
			builder.append(this.readStringUntil((char)delimiter));
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		if (read() != '$') {
			throw new ParsingException(Messages.InternalParserError);
		}
		parsedString = builder.toString();
		return true;
	}
	
	public enum ParseStatementOption {
		InitializationStatement,
		ExpectFuncDesc;
		
		public static final EnumSet<ParseStatementOption> NoOptions = EnumSet.noneOf(ParseStatementOption.class);
	}
	
	private Statement parseStatement() throws ParsingException {
		return parseStatement(ParseStatementOption.NoOptions);
	}
	
	private Statement parseStatementWithOwnTypeInferenceBlock(TypeInformationMerger merger) throws ParsingException {
		List<IStoredTypeInformation> block = beginTypeInferenceBlock();
		try {
			Statement s = parseStatement();
			return s;
		} finally {
			block = endTypeInferenceBlock();
			if (listener != null)
				listener.endTypeInferenceBlock(block);
			merger.inject(block);
		}
	}
	
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
			C4VariableScope scope;
			
			// comment statement oO
			result = parseCommentObject();

			if (result == null) {
				String readWord = readIdent();
				if (readWord == null || readWord.length() == 0) {
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
								checkForSemicolon();
							}
						}
						else
							result = null;
					}
				}
				else if ((scope = C4VariableScope.makeScope(readWord)) != null) {
					result = parseVarDeclarationInStatement(options, scope);
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
						this.seek(expression.getExprEnd());
					}
				}
				else
					result = null;
			}

			if (result != null) {
				result.setExprRegion(start, this.offset);
				reportErrorsOf(result);
				
				// inline comment attached to expression so code reformatting does not mess up the user's code too much
				Comment c = getCommentImmediatelyFollowing();
				if (c != null)
					result.setInlineComment(c);
				if (emptyLines > 0)
					result.addAttachment(new Statement.EmptyLinesAttachment(emptyLines));
				
				if (parseStatementRecursion == 1) {
					if (listener != null) {
						switch (listener.expressionDetected(result, this)) {
						case Cancel:
							throw new SilentParsingException(Reason.Cancellation, "Expression Listener Cancellation"); //$NON-NLS-1$
						}
					}
				}
			}
			return result;
		} finally {
			parseStatementRecursion--;
		}
		

	}

	private void parseStatementBlock(int start, int endOfFunc, List<Statement> statements, EnumSet<ParseStatementOption> options, ExpressionsAndStatementsReportingFlavour flavour) throws ParsingException {
		boolean foundClosingBracket = false;
		boolean notReached = false;
		int garbageStart = -1;
		boolean oldStatementNotReached = this.statementNotReached;
		while (!reachedEOF() && this.offset < endOfFunc) {
			this.statementNotReached = notReached;
			int potentialGarbageEnd = offset;
			eatWhitespace();
			foundClosingBracket = peek() == '}';
			if (foundClosingBracket)
				break;
			Statement statement = flavour == ExpressionsAndStatementsReportingFlavour.AlsoStatements ? parseStatement(options) : SimpleStatement.wrapExpression(parseExpression());
			if (statement == null) {
				if (garbageStart == -1) {
					garbageStart = offset;
				}
				offset++;
				continue;
			} else {
				// garbage recognized before statement: Create a special garbage statement that will report itself
				if (garbageStart != -1) {
					garbageStart = maybeAddGarbageStatement(statements, garbageStart, potentialGarbageEnd);
				}
			}
			statements.add(statement);
			boolean statementIsComment = statement instanceof Comment;
			if (!notReached) {
				notReached = statement.getControlFlow() != ControlFlow.Continue;
			}
			// after first 'real' statement don't expect function description anymore
			if (!statementIsComment) {
				options.remove(ParseStatementOption.ExpectFuncDesc);				
			}
			eatWhitespace();
		}
		if (garbageStart != -1) {
			// contains only garbage ... still add
			maybeAddGarbageStatement(statements, garbageStart, offset);
		}
		if (!foundClosingBracket) {
			if (this.offset < endOfFunc)
				errorWithCode(ParserErrorCode.BlockNotClosed, start, start+1);
		} else {
			read(); // should be }
		}
		this.statementNotReached = oldStatementNotReached;
	}

	private int maybeAddGarbageStatement(List<Statement> statements,
			int garbageStart, int potentialGarbageEnd) throws ParsingException {
		String garbageString = buffer.substring(garbageStart, potentialGarbageEnd);
		garbageString = modifyGarbage(garbageString);
		if (garbageString != null) {
			GarbageStatement garbage = new GarbageStatement(garbageString, garbageStart);
			garbageStart = -1;
			statements.add(garbage);
			handleExpressionCreated(true, garbage);
		}
		return garbageStart;
	}

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

	private VarDeclarationStatement parseVarDeclarationInStatement(EnumSet<ParseStatementOption> options, C4VariableScope scope) throws ParsingException {
		VarDeclarationStatement result;
		List<VarInitialization> initializations = new LinkedList<VarInitialization>();
		do {
			eatWhitespace();
			int varNameStart = this.offset;
			String varName = readIdent();
			int varNameEnd = this.offset;
			// check if there is initial content
			eatWhitespace();
			C4Variable var = findVar(varName, scope);
			if (var == null) {
				// happens when parsing only the body of a function for computing context information in an editor and such
				var = createVarInScope(varName, C4VariableScope.VAR, new SourceLocation(varNameStart, varNameEnd), null);
			} else {
				var.setLocation(new SourceLocation(offsetOfScriptFragment()+varNameStart, offsetOfScriptFragment()+varNameEnd));
			}
			parsedVariable = var;
			ExprElm val;
			if (read() == '=') {
				eatWhitespace();
				val = parseExpression();
				if (val == null)
					errorWithCode(ParserErrorCode.ValueExpected, this.offset, this.offset+1);
				else {					new AccessVar(var).expectedToBeOfType(val.getType(this), this, TypeExpectancyMode.Force);
				}
			}
			else {
				val = null;
				unread();
			}
			VarInitialization initialization = new VarInitialization(varName, val, varNameStart);
			initialization.variableBeingInitialized = var;
			initializations.add(initialization);
		} while(read() == ',');
		unread();
		result = new VarDeclarationStatement(initializations, scope);
		if (!options.contains(ParseStatementOption.InitializationStatement))
			checkForSemicolon();
		return result;
	}
	
	public static class TypeInformationMerger {
		private List<IStoredTypeInformation> merged;
		
		private static List<IStoredTypeInformation> mergeTypeInformationLists(List<IStoredTypeInformation> first, List<IStoredTypeInformation> second) {
			for (IStoredTypeInformation info : first) {
				for (Iterator<IStoredTypeInformation> it = second.iterator(); it.hasNext();) {
					IStoredTypeInformation info2 = it.next();
					if (info2.sameExpression(info)) {
						info.merge(info2);
						it.remove();
					}
				}
			}
			first.addAll(second);
			return first;
		}
		
		public List<IStoredTypeInformation> inject(List<IStoredTypeInformation> infos) {
			if (merged == null)
				return merged = infos;
			return merged = mergeTypeInformationLists(merged, infos);
		}

		public List<IStoredTypeInformation> finish(List<IStoredTypeInformation> finalList) {
			if (merged == null)
				return finalList;
			return mergeTypeInformationLists(finalList, merged);
		}
		
		public List<IStoredTypeInformation> getResult() {
			return merged;
		}
	}
	
	private void expect(char expected) throws ParsingException {
		if (read() != expected) {
			unread();
			tokenExpectedError(String.valueOf(expected));
		}
	}
	
	private void expect(String expected) throws ParsingException {
		String r = readIdent();
		if (r == null || !r.equals(expected)) {
			tokenExpectedError(expected);
		}
	}
	
	private void checkForSemicolon() throws ParsingException {
		eatWhitespace();
		expect(';');
	}

	private Statement parseKeyword(String readWord) throws ParsingException {
		Statement result = null;
		if (readWord.equals(Keywords.If)) {
			result = parseIf();
		}
		else if (readWord.equals(Keywords.While)) {
			result = parseWhile();
		}
		else if (readWord.equals(Keywords.Do)) {
			result = parseDoWhile();
		}
		else if (readWord.equals(Keywords.For)) {
			result = parseFor();
		}
		else if (readWord.equals(Keywords.Continue)) {
			if (currentLoop == null)
				errorWithCode(ParserErrorCode.KeywordInWrongPlace, this.offset-readWord.length(), this.offset, true, readWord);
			checkForSemicolon();
			result = new ContinueStatement();
		}
		else if (readWord.equals(Keywords.Break)) {
			if (currentLoop == null)
				errorWithCode(ParserErrorCode.KeywordInWrongPlace, this.offset-readWord.length(), this.offset, true, readWord);
			checkForSemicolon();
			result = new BreakStatement();
		}
		else if (readWord.equals(Keywords.Return)) {
			result = parseReturn();
		}
		else if (getCurrentFunc() != null && getCurrentFunc().isOldStyle() && (looksLikeStartOfFunction(readWord) || peekAfterWhitespace() == ':')) {
			// whoops, too far
			return null;
		}
		else
			result = null;

		return result;
	}

	private Statement parseReturn() throws ParsingException {
		Statement result;
		eatWhitespace();
		int next = read();
		ExprElm returnExpr;
		if (next == ';') {
			unread();
			returnExpr = null;
		}
		else {
			unread();
			enableError(ParserErrorCode.TuplesNotAllowed, false);
			if (getStrictLevel() < 2)
				enableError(ParserErrorCode.EmptyParentheses, false);
			returnExpr = parseExpression();
			if (returnExpr == null) {
				errorWithCode(ParserErrorCode.ValueExpected, this.offset, this.offset+1);				
			}
			enableError(ParserErrorCode.TuplesNotAllowed, true);
			enableError(ParserErrorCode.EmptyParentheses, true);
		}
		result = new ReturnStatement(returnExpr);
		checkForSemicolon();
		return result;
	}

	private Statement parseDoWhile() throws ParsingException {
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

	private Statement parseFor() throws ParsingException {
		int offset;
		Statement result;
		eatWhitespace();
		expect('(');
		eatWhitespace();

		// initialization
		offset = this.offset;
		C4Variable loopVariable = null;
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
			if (!varName.equals("") && !varName.equals(Keywords.VarNamed)) { //$NON-NLS-1$
				eatWhitespace();
				w = readIdent();
				if (!w.equals(Keywords.In)) {
					w = null;
					this.seek(pos);
				}
				else {
					// too much manual setting of stuff
					AccessVar accessVar = new AccessVar(varName);
					accessVar.setExprRegion(pos, pos+varName.length());
					if (accessVar.obtainDeclaration(this) == null) {
						createVarInScope(varName, C4VariableScope.VAR, new SourceLocation(offsetOfScriptFragment()+pos, offsetOfScriptFragment()+pos+varName.length()), null);
					}
					handleExpressionCreated(true, accessVar);
					initialization = new SimpleStatement(accessVar);
					initialization.setExprRegion(pos, pos+varName.length());
					reportErrorsWithErrorDisabled(initialization, ParserErrorCode.NoSideEffects);
				}
			}
			else {
				this.seek(pos);
			}
			if (w == null) {
				boolean noSideEffectsWasEnabled = errorDisabled(ParserErrorCode.NoSideEffects);
				enableError(ParserErrorCode.NoSideEffects, false);
				initialization = parseStatement(EnumSet.of(ParseStatementOption.InitializationStatement));
				enableError(ParserErrorCode.NoSideEffects, noSideEffectsWasEnabled);
				if (initialization == null) {
					errorWithCode(ParserErrorCode.ExpectedCode, this.offset, this.offset+1);
				}
				loopVariable = parsedVariable; // let's just assume it's the right one
			}
		}

		if (w == null) {
			// determine loop type
			eatWhitespace();
			offset = this.offset;
			if (initialization != null) {
				if (read() == ';') { // initialization finished regularly with ';'
					offset = this.offset;
					w = null; // implies there can be no 'in'
				} else {
					unread();
					w = readIdent();
				}
			}
			else
				w = null; // if there is no initialization statement at all there can also be no 'in'
		}
		LoopType loopType;
		if (w != null && w.equals(Keywords.In)) {
			// it's a for (x in array) loop!
			loopType = LoopType.IterateArray;
			eatWhitespace();
			arrayExpr = parseExpression();
			if (arrayExpr == null)
				errorWithCode(ParserErrorCode.ExpressionExpected, offset, this.offset+1);
			else {
				IType t = arrayExpr.getType(this);
				if (!t.canBeAssignedFrom(C4Type.ARRAY))
					warningWithCode(ParserErrorCode.IncompatibleTypes, arrayExpr, t.toString(), C4Type.ARRAY.toString());
				if (loopVariable != null && t instanceof ArrayType) {
					ArrayType arrayType = (ArrayType) t;
					new AccessVar(loopVariable).expectedToBeOfType(arrayType.getElementType(), this, TypeExpectancyMode.Force);
				}
			}
			condition = null;
			increment = null;
		} else {
			loopType = LoopType.For;
			this.seek(offset); // if a word !equaling("in") was read
			eatWhitespace();
			if (read() == ';') {
				// any " optional "
				unread(); // is expected
				condition = null;
			} else {
				unread();
				condition = parseExpression();
				if (condition == null) {
					errorWithCode(ParserErrorCode.ConditionExpected, offset, this.offset);
				}
			}
			eatWhitespace();
			offset = this.offset;
			expect(';');
			eatWhitespace();
			offset = this.offset;
			if (read() == ')') {
				// " optional "
				unread(); // is expected
				increment = null;
			} else {
				unread();
				increment = parseExpression();
				if (increment == null) {
					errorWithCode(ParserErrorCode.ExpressionExpected, offset, this.offset+1);
				}
			}
			arrayExpr = null;
		}
		eatWhitespace();
		expect(')');
		eatWhitespace();
		offset = this.offset;
		currentLoop = loopType;
		body = parseStatement();
		if (body == null) {
			errorWithCode(ParserErrorCode.StatementExpected, offset, offset+4);
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

	private void loopConditionWarnings(Statement body, ExprElm condition) {
		Object condEv = C4Type.BOOL.convert(condition == null ? true : condition.evaluateAtParseTime(getContainer()));
		if (Boolean.FALSE.equals(condEv))
			warningWithCode(ParserErrorCode.ConditionAlwaysFalse, condition, condition.toString());
		else if (Boolean.TRUE.equals(condEv)) {
			EnumSet<ControlFlow> flows = body.getPossibleControlFlows();
			if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
				warningWithCode(ParserErrorCode.InfiniteLoop, body);
		}
	}

	private Statement parseWhile() throws ParsingException {
		int offset;
		Statement result;
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
			condition = ExprElm.nullExpr(this.offset, 0); // while () is valid
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
	
	/*private class ConditionTypeInformationExtractor extends ExpressionListener implements Sink<List<IStoredTypeInformation>> {
		private ExprElm condition;
		public ConditionTypeInformationExtractor(ExprElm condition) {
			this.condition = condition;
		}
		@Override
		public void receivedObject(List<IStoredTypeInformation> item) {
			condition.traverse(this);
		}
		@Override
		public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
			if (expression )
		}
	}*/

	private Statement parseIf() throws ParsingException {
		final int offset = this.offset;
		Statement result;
		eatWhitespace();
		expect('(');
		eatWhitespace();
		ExprElm condition = parseExpression();
		if (condition == null)
			condition = ExprElm.nullExpr(this.offset, 0); // if () is valid
		eatWhitespace();
		expect(')');
		eatWhitespace(); // FIXME: eats comments so when transforming code the comments will be gone
		TypeInformationMerger merger = new TypeInformationMerger();
		Statement ifStatement = parseStatementWithOwnTypeInferenceBlock(merger);
		if (ifStatement == null) {
			errorWithCode(ParserErrorCode.StatementExpected, offset, offset+Keywords.If.length());
		}
		int beforeElse = this.offset;
		eatWhitespace();
		String nextWord = readIdent();
		Statement elseStatement;
		if (nextWord != null && nextWord.equals(Keywords.Else)) {
			eatWhitespace();
			int o = this.offset;
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
		storedTypeInformationListStack.push(merger.finish(storedTypeInformationListStack.pop()));
		
		if (!containsConst(condition)) {
			Object condEv = C4Type.BOOL.convert(condition.evaluateAtParseTime(getContainer()));
			if (condEv != null && condEv != ExprElm.EVALUATION_COMPLEX) {
				warningWithCode(condEv.equals(true) ? ParserErrorCode.ConditionAlwaysTrue : ParserErrorCode.ConditionAlwaysFalse,
						condition, condition.toString());
			}
		}
		
		result = new IfStatement(condition, ifStatement, elseStatement);
		return result;
	}
	
	private static boolean containsConst(ExprElm condition) {
		if(condition instanceof AccessVar && ((AccessVar)condition).constCondition())
			return true;
		for (ExprElm expression : condition.getSubElements())
			if(containsConst(expression))
				return true;
		return false;
	}

	private boolean parseID() throws ParsingException {
		if (idMatcher.pattern() != IDENTIFIER_PATTERN && idMatcher.reset(buffer.substring(offset)).lookingAt()) {
			String idString = idMatcher.group();
			offset += idString.length();
			if (isWordPart(peek()) || NUMERAL_PATTERN.matcher(idString).matches()) {
				offset -= idString.length();
				parsedID = null;
				return false;
			}
			parsedID = C4ID.getID(idString);
			return true;
		} else {
			parsedID = null; // reset so no old parsed ids get through
			return false;
		}
	}

	private boolean parseParameter(C4Function function) throws ParsingException {
		
		if (isEngine && parseEllipsis()) {
			addVarParmsParm(function);
			return true;
		}
		
		int s = this.offset;
		String firstWord = readIdent();
		if (firstWord.length() == 0) {
			if (read() == '&') {
				firstWord = "&"; //$NON-NLS-1$
			} else {
				unread();
				return false;
			}
		}
		int e = this.offset;
		C4Variable var = new C4Variable(null, C4VariableScope.VAR);
		IType type = C4Type.makeType(firstWord);
		if (type == C4Type.REFERENCE && !container.getEngine().getCurrentSettings().supportsRefs) {
			errorWithCode(ParserErrorCode.EngineDoesNotSupportRefs, s, e, true, container.getEngine().getName());
		}
		boolean typeLocked = type != C4Type.UNKNOWN && !isEngine;
		var.forceType(type, typeLocked);
		if (type == C4Type.UNKNOWN) {
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
				var.forceType(C4Type.ANY, typeLocked);
				var.setName(firstWord);
				this.seek(e);
			}
		}
		var.setLocation(new SourceLocation(s, e));
		var.setParentDeclaration(function);
		function.getParameters().add(var);
		return true;
	}

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
	
	// overridden by internal helper class that operates on just a substring of the whole script
	// used for setting the right location for variables that are created while parsing the body of a function
	protected int offsetOfScriptFragment() {
		return 0;
	}
	
	/**
	 * Modify garbage string based on special considerations
	 * @param garbage The expression string recognized as garbage
	 * @return Actual garbage string to be wrapped in a GarbageStatement. Null if no GarbageStatement should be created
	 */
	protected String modifyGarbage(String garbage) {
		return garbage; // normal parser accepts teh garbage
	}
	
	public interface IMarkerListener {
		public enum WhatToDo {
			DropCharges,
			PassThrough
		}
		WhatToDo markerEncountered(C4ScriptParser parser, ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args);
	}
	
	private void reportExpressionsAndStatements(C4Function func, IScriptParserListener listener, ExpressionsAndStatementsReportingFlavour flavour) {
		currentDeclaration = func;
		setListener(listener);
		strictLevel = getContainer().getStrictLevel();
		enableError(ParserErrorCode.TokenExpected, false);
		enableError(ParserErrorCode.InvalidExpression, false);
		enableError(ParserErrorCode.BlockNotClosed, false);
		enableError(ParserErrorCode.NotAllowedHere, false);
		try {
			EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
			beginTypeInferenceBlock();
			parseStatementBlock(offset, Integer.MAX_VALUE, new LinkedList<Statement>(), options, flavour);
			//endTypeInferenceBlock(); not here for type information might still be needed
		} 
		catch (ParsingException e) {
			// silent...
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		applyStoredTypeInformationList(true);
	}
	
	public enum ExpressionsAndStatementsReportingFlavour {
		OnlyExpressions,
		AlsoStatements
	}
	
	public static C4ScriptParser reportExpressionsAndStatementsWithSpecificFlavour(IDocument doc, IRegion region, C4ScriptBase context, C4Function func, IScriptParserListener listener, IMarkerListener markerListener, ExpressionsAndStatementsReportingFlavour flavour)  {
		return reportExpressionsAndStatementsWithSpecificFlavour(doc, region.getOffset(), region.getOffset()+region.getLength(), context, func, listener, markerListener, flavour);
	}
	
	public static C4ScriptParser reportExpressionsAndStatements(IDocument doc, IRegion region, C4ScriptBase context, C4Function func, IScriptParserListener listener, IMarkerListener markerListener)  {
		return reportExpressionsAndStatementsWithSpecificFlavour(doc, region, context, func, listener, markerListener, ExpressionsAndStatementsReportingFlavour.AlsoStatements);
	}
	
	private static class ScriptParserWithMarkerListener extends C4ScriptParser {
		private IMarkerListener markerListener;
		public ScriptParserWithMarkerListener(String withString, C4ScriptBase script, IMarkerListener markerListener) {
			super(withString, script, null);
			this.markerListener = markerListener;
		}
		@Override
		public IMarker markerWithCode(ParserErrorCode code,
				int markerStart, int markerEnd, boolean noThrow,
				int severity, Object... args) throws ParsingException {
			if (markerListener != null) {
				if (markerListener.markerEncountered(this, code, markerStart+offsetOfScriptFragment(), markerEnd+offsetOfScriptFragment(), noThrow, severity, args) == WhatToDo.DropCharges)
					return null;
			}
			return super.markerWithCode(code, markerStart, markerEnd, noThrow, severity, args);
		}
	}
	
	public static C4ScriptParser reportExpressionsAndStatementsWithSpecificFlavour(
		IDocument doc,
		final int statementStart, int statementEnd,
		C4ScriptBase context, C4Function func,
		IScriptParserListener listener, final IMarkerListener markerListener,
		ExpressionsAndStatementsReportingFlavour flavour
	) { 
		String statements;
		try {
			// totally important to add the ")". Makes completion proposals work. DO NOT REMOVE!1
			statements = doc.get(statementStart, Math.min(statementEnd-statementStart, doc.getLength()-statementStart)) + ")"; //$NON-NLS-1$
		} catch (BadLocationException e) {
			statements = ""; // well... //$NON-NLS-1$
		}
		C4ScriptParser parser = new ScriptParserWithMarkerListener(statements, context, markerListener) {
			@Override
			protected int offsetOfScriptFragment() {
				return statementStart;
			}
			@Override
			protected String modifyGarbage(String garbage) {
				if (garbage.equals(")")) {
					return null;
				} else if (garbage.endsWith(")")) {
					return garbage.substring(0, garbage.length()-1);
				} else {
					return garbage;
				}
			}
		};
		parser.reportExpressionsAndStatements(func, listener, flavour);
		return parser;
	}
	
	public static Statement parseStandaloneStatement(final String expression, C4Function context, IScriptParserListener listener, final IMarkerListener markerListener) throws ParsingException {
		if (context == null) {
			C4ScriptBase tempScript = new TempScript(expression);
			context = new C4Function("<temp>", null, C4FunctionScope.GLOBAL); //$NON-NLS-1$
			context.setScript(tempScript);
			context.setBody(new SourceLocation(0, expression.length()));
		}
		C4ScriptParser tempParser = new ScriptParserWithMarkerListener(expression, context.getScript(), markerListener);
		tempParser.setListener(listener);
		tempParser.setCurrentFunc(context);
		tempParser.beginTypeInferenceBlock();
		tempParser.enableError(ParserErrorCode.NotFinished, false);
		
		List<Statement> statements = new LinkedList<Statement>();
		Statement statement;
		do {
			statement = tempParser.parseStatement();
			if (statement != null)
				statements.add(statement);
			else
				break;
		} while (true);
		return statements.size() == 1 ? statements.get(0) : new BunchOfStatements(statements);
	}
	
	public static Statement parseStandaloneStatement(final String expression, C4Function context, IScriptParserListener listener) throws ParsingException {
		return parseStandaloneStatement(expression, context, listener, null);
	}

	public String scriptSubstringAtRegion(IRegion region) {
		return this.readStringAt(region.getOffset(), region.getOffset()+region.getLength());
	}

	public int getParseStatementRecursion() {
		return parseStatementRecursion;
	}
	
}
