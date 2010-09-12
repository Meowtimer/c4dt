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

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
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
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.*;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm.TypeExpectancyMode;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * A C4Script parser. Parses declarations in a script and stores it in a C4ScriptBase object (sold separately).
 * The parser can also perform a second parse phase that creates syntax trees from c4script functions. Those can be used for various purposes, including
 * checking correctness (aiming to detect all kinds of errors like undeclared identifiers, supplying values of wrong type to functions etc.), converting old
 * c4script code to #strict-compliant "new-style" code and forming the base of navigation operations like "Find Declaration", "Find References" etc.
 */
public class C4ScriptParser {
	
	public static final int MAX_PAR = 10;
	public static final int MAX_NUMVAR = 20;
	public static final int UNKNOWN_PARAMETERNUM = MAX_PAR+1;
	
	private IExpressionListener expressionListener;

	private BufferedScanner scanner;
	private IFile scriptFile; // for project intern files
	private C4ScriptBase container;
	private C4Function activeFunc;
	private int strictLevel;
	
	// parse<Blub>() functions store their results in those
	private C4ID parsedID;
	private C4Variable parsedVariable;
	private long parsedNumber;
	private String parsedMemberOperator;
	private String parsedString;
	
	private int parseExpressionRecursion;
	private int parseStatementRecursion;
	
	/**
	 * Whether the script contains an #appendto
	 */
	private boolean appendTo;
	/**
	 * Whether to not create any error markers at all - set if script is contained in linked group
	 */
	private boolean allErrorsDisabled;
	/**
	 * Whether the script is an engine script
	 */
	private boolean isEngine;

	private LoopType currentLoop;
	private Comment lastComment;
	
	/**
	 * Number of unnamed parameters used in activeFunc (Par(5) -> 6 unnamed parameters).
	 * If a complex expression is passed to Par() this variable is set to UNKNOWN_PARAMETERNUM
	 */
	private int numUnnamedParameters;
	
	private Stack<List<IStoredTypeInformation>> storedTypeInformationListStack = new Stack<List<IStoredTypeInformation>>();
	
	/**
	 * Returns the expression listener that is notified when an expression or a statement has been parsed.
	 * @return the expression listener
	 */
	public IExpressionListener getExpressionListener() {
		return expressionListener;
	}

	/**
	 * Sets the expression listener.
	 * @param expressionListener the new expression listener
	 */
	public void setExpressionListener(IExpressionListener expressionListener) {
		this.expressionListener = expressionListener;
	}
	
	public final boolean hasAppendTo() {
		return appendTo;
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
	
	public void applyStoredTypeInformationList(boolean soft) {
		for (IStoredTypeInformation info : storedTypeInformationListStack.peek()) {
			info.apply(soft);
		}
	}
	
	/**
	 * Ask the parser to store type information about an expression. No guarantees whether type information will actually be stored.
	 */
	public void storeTypeInformation(ExprElm expression, IType type, List<IStoredTypeInformation> list) {
		IStoredTypeInformation requested = requestStoredTypeInformation(expression, list);
		if (requested != null) {
			requested.storeType(type);
		}
	}
	
	public void storeTypeInformation(ExprElm expression, IType type) {
		storeTypeInformation(expression, type, storedTypeInformationListStack.peek());
	}
	
	/**
	 * Requests type information for an expression
	 * @param expression the expression
	 * @param list 
	 * @return the type information or null if none has been stored
	 */
	public IStoredTypeInformation requestStoredTypeInformation(ExprElm expression, List<IStoredTypeInformation> list) {
		for (IStoredTypeInformation info : list) {
			if (info.expressionRelevant(expression, this))
				return info;
		}
		IStoredTypeInformation newlyCreated = expression.createStoredTypeInformation(this);
		if (newlyCreated != null)
			list.add(newlyCreated);
		return newlyCreated;
	}
	
	public IStoredTypeInformation requestStoredTypeInformation(ExprElm expression) {
		if (storedTypeInformationListStack.isEmpty())
			return null;
		return requestStoredTypeInformation(expression, storedTypeInformationListStack.peek());
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
	public IStoredTypeInformation queryStoredTypeInformation(ExprElm expression, boolean wholeStack) {
		if (storedTypeInformationListStack.isEmpty())
			return null;
		for (int i = storedTypeInformationListStack.size()-1, levels = wholeStack ? storedTypeInformationListStack.size() : 1; levels > 0; levels--,i--) {
			for (IStoredTypeInformation info : storedTypeInformationListStack.get(i)) {
				if (info.expressionRelevant(expression, this))
					return info;
			}
		}
		return null;
	}
	
	public IType queryTypeOfExpression(ExprElm expression, C4Type defaultType) {
		IStoredTypeInformation info = queryStoredTypeInformation(expression, true);
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
	 * Returns the function that is currently being parsed or otherwise considered "active"
	 * @return the active function
	 */
	public C4Function getActiveFunc() {
		return activeFunc;
	}
	
	/**
	 * Sets the active function. There should be a good reason to call this. 
	 * @param func
	 */
	public void setActiveFunc(C4Function func) {
		if (func != activeFunc) {
			activeFunc = func;
			numUnnamedParameters = 0;
		}
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
		this((IFile) script.getScriptFile(), script);
	}

	/**
	 * Creates a C4Script parser that parses a file within the project.
	 * Results are stored in <code>object</code>
	 * @param scriptFile
	 * @param obj
	 * @throws CompilerException
	 */
	public C4ScriptParser(IFile scriptFile, C4ScriptBase script) {
		this.scriptFile = scriptFile;
		scanner = new BufferedScanner(scriptFile);
		container = script;
		allErrorsDisabled = C4GroupItem.isLinkedResource(scriptFile); 
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
		scriptFile = null;
		scanner = new BufferedScanner(stream);
		container = script;
	}
	
	/**
	 * Creates a C4Script parser that parses an arbitrary string.
	 * @param withString
	 * @param script
	 */
	public C4ScriptParser(String withString, C4ScriptBase script, IFile scriptFile) {
		this.scriptFile = scriptFile;
		scanner = new BufferedScanner(withString);
		container = script;
		isEngine = container instanceof C4Engine;
	}
	
	/**
	 * Perform a full parsing (that includes cleaning up the current state of the script container, parsing declarations and parsing function code).
	 * @throws ParsingException
	 */
	public void parse() throws ParsingException {
		synchronized (container) {
			clean();
			parseDeclarations();
			parseCodeOfFunctions();
		}
	}

	/**
	 * Parse declarations but not function code. Before calling this it should be ensured that the script is cleared to avoid duplicates.
	 */
	public void parseDeclarations() {
		synchronized (container) {
			int offset = 0;
			scanner.seek(offset);
			disableError(ParserErrorCode.StringNotClosed); // just one time error when parsing function code
			try {
				eatWhitespace();
				while(!scanner.reachedEOF()) {
					if (!parseDeclaration()) {
						eatWhitespace();
						if (!scanner.reachedEOF()) {
							int start = scanner.getPosition();
							String tokenText = parseTokenAndReturnAsString();
							errorWithCode(ParserErrorCode.UnexpectedToken, start, scanner.getPosition(), true, tokenText);
						}
					}
					eatWhitespace();
				}
			}
			catch (ParsingException e) {
				return;
			}
			enableError(ParserErrorCode.StringNotClosed);
			container.setDirty(false);
		}
	}
	
	/**
	 * Parse function code. Side effects include:
	 * 	-Errors (or things the parser thinks are errors) listed in the Problems view
	 * 	-Types for variables inferred more or less accurately
	 * @throws ParsingException
	 */
	public void parseCodeOfFunctions() throws ParsingException {
		synchronized (container) {
			strictLevel = container.getStrictLevel();
			for (C4Function function : container.functions()) {
				parseCodeOfFunction(function);
			}
			for (C4Directive directive : container.directives()) {
				directive.validate(this);
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
			((C4Object)container).chooseLocalizedName();
			C4Function definitionFunc = container.findFunction(Keywords.DefinitionFunc);
			if (definitionFunc != null && definitionFunc.getBody() != null) { // could also be engine function without body
				scanner.seek(definitionFunc.getBody().getStart());
				boolean old = allErrorsDisabled;
				allErrorsDisabled = true;
				reportExpressionsAndStatements(definitionFunc, new ExpressionListener() {
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
								Object v = callFunc.getParams()[1].evaluateAtParseTime(getContainer());
								if (v instanceof String) {
									getContainer().setName((String) v);
								}
								return TraversalContinuation.Cancel;
							}
						}
						return TraversalContinuation.Continue;
					}
				});
				allErrorsDisabled = old;
			}
		}
	}

	/**
	 * Parse code of one single function. parseCodeOfFunctions() calls this for all functions in the script. 
	 * @param function
	 * @throws ParsingException
	 */
	public void parseCodeOfFunction(C4Function function) throws ParsingException {
		if (function.getBody() == null)
			return;
		try {
			setActiveFunc(function);
			// reset local vars
			for (C4Variable v : function.getLocalVars()) {
				v.forceType(C4Type.UNKNOWN);
				v.setObjectType(null);
			}
			beginTypeInferenceBlock();
			scanner.seek(function.getBody().getStart());
			parseCodeBlock();
			applyStoredTypeInformationList(false);
			endTypeInferenceBlock();
			if (numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
				activeFunc.createParameters(numUnnamedParameters);
			}
			else if (numUnnamedParameters == UNKNOWN_PARAMETERNUM && (activeFunc.getParameters().size() == 0 || activeFunc.getParameters().get(activeFunc.getParameters().size()-1).isActualParm())) {
				addVarParmsParm(activeFunc);
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
			errorWithCode(ParserErrorCode.InternalError, scanner.getPosition(), scanner.getPosition()+1, true, e.getMessage());
			e.printStackTrace();
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
		return scanner.getLineAt(region);
	}
	
	/**
	 * Returns a substring of the script denoted by a region
	 * @param region the region
	 * @return the substring
	 */
	public String getSubstringOfScript(IRegion region) {
		return scanner.readStringAt(region.getOffset(), region.getOffset()+region.getLength()+1);
	}
	
	/**
	 * Returns a line region for a region in the script
	 * @param region the region the line to be returned is in
	 * @return the line region
	 */
	public IRegion getLineRegion(IRegion regionInLine) {
		return scanner.getLineRegion(regionInLine);
	}

	/**
	 * Parses the declaration at the current scanner position.
	 * @return whether parsing was successful
	 * @throws ParsingException
	 */
	protected boolean parseDeclaration() throws ParsingException {
		final int offset = scanner.getPosition();
		int readByte = scanner.read();
		if (readByte == '#') {
			// directive
			String directiveName = scanner.readStringUntil(BufferedScanner.WHITESPACE_CHARS);
			C4DirectiveType type = C4DirectiveType.makeType(directiveName);
			if (type == null) {
				warningWithCode(ParserErrorCode.UnknownDirective, offset, offset + directiveName.length());
				scanner.moveUntil(BufferedScanner.NEWLINE_CHARS);
				return true;
			}
			else {
				String content = parseDirectiveParms();
				C4Directive directive = new C4Directive(type, content);
				directive.setLocation(new SourceLocation(offset, scanner.getPosition()));
				container.addDeclaration(directive);
				if (type == C4DirectiveType.APPENDTO)
					appendTo = true;
				return true;
			}
		}
		else {
			scanner.seek(offset);
			String word = scanner.readIdent();
			if (looksLikeStartOfFunction(word)) {
				if (parseFunctionDeclaration(word, offset))
					return true;
			}
			else if (word.equals(Keywords.GlobalNamed) || word.equals(Keywords.LocalNamed)) {
				if (parseVariableDeclaration(word))
					return true;
			}
			else {
				// old-style function declaration without visibility
				eatWhitespace();
				if (scanner.read() == ':' && scanner.read() != ':') { // no :: -.-
					scanner.seek(offset); // just let parseFunctionDeclaration parse the name again
					if (parseFunctionDeclaration(Keywords.Public, offset)) // just assume public
						return true;
				}
			}
		}
		scanner.seek(offset);
		return false;
	}

	private String parseDirectiveParms() {
		StringBuffer buffer = new StringBuffer(80);
		while (!scanner.reachedEOF() && !BufferedScanner.isLineDelimiterChar((char)scanner.peek()) && !parseComment()) {
			buffer.append((char)scanner.read());
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
		final int offset = scanner.getPosition();
		String desc = getTextOfLastComment(offset);

		List<C4Variable> createdVariables = new LinkedList<C4Variable>();
		C4VariableScope scope = C4VariableScope.makeScope(word);
		if (scope == C4VariableScope.STATIC || scope == C4VariableScope.LOCAL) {
			eatWhitespace();
			int pos = scanner.getPosition();
			if (scope == C4VariableScope.STATIC) {
				if (scanner.readIdent().equals(Keywords.Const)) {
					scope = C4VariableScope.CONST;
				} else {
					scanner.seek(pos);
				}
			}
			do {
				eatWhitespace();
				C4Type t;
				if (isEngine) {
					t = parseFunctionReturnType();
					if (t != null)
						eatWhitespace();
				}
				else
					t = null;
				int s = scanner.getPosition();
				String varName = scanner.readIdent();
				int e = scanner.getPosition();
				C4Variable var = null;
				if (scope == C4VariableScope.CONST || getContainer().getEngine().getCurrentSettings().nonConstGlobalVarsAssignment) {
					eatWhitespace();
					if (scanner.peek() == ';' || scanner.peek() == ',') {
						if (scope == C4VariableScope.CONST && !isEngine)
							errorWithCode(ParserErrorCode.ConstantValueExpected, scanner.getPosition()-1, scanner.getPosition(), true);
						else {
							createdVariables.add(var = createVariable(scope, desc, s, e, varName));
							if (scope == C4VariableScope.STATIC) {
								var.forceType(C4Type.INT); // most likely
							}
						}
					}
					else {
						expect('=');
						eatWhitespace();
						boolean old = allErrorsDisabled;
						allErrorsDisabled = true;
						ExprElm constantValue = parseExpression(true);
						allErrorsDisabled = old;
						if (constantValue == null)
							constantValue = ERROR_PLACEHOLDER_EXPR;
						boolean dontBotherEvaluatingConst = false;
						if (scope == C4VariableScope.CONST && !constantValue.isConstant()) {
							errorWithCode(ParserErrorCode.ConstantValueExpected, constantValue, true);
							dontBotherEvaluatingConst = true;
						}
						var = createVariable(C4VariableScope.CONST, desc, s, e, varName);
						if (!dontBotherEvaluatingConst) {
							try {
								if (scope == C4VariableScope.CONST)
									var.setConstValue(constantValue.evaluateAtParseTime(getContainer()));
								else
									var.setScriptScopeInitializationExpression(constantValue);
							} catch (Exception ex) {
								ex.printStackTrace();
								errorWithCode(ParserErrorCode.InvalidExpression, constantValue);
							}
						}
						createdVariables.add(var);
						var.inferTypeFromAssignment(constantValue, this);
					}
				}
				else {
					createdVariables.add(var = createVariable(C4VariableScope.STATIC, desc, s, e, varName));
				}
				if (t != null)
					var.forceType(t);
				eatWhitespace();
			} while(scanner.read() == ',');
			scanner.unread();
			if (scanner.read() != ';') {
				errorWithCode(ParserErrorCode.CommaOrSemicolonExpected, scanner.getPosition()-1, scanner.getPosition());
			}
		}
		else if (word.equals(Keywords.LocalNamed)) {
			do {
				eatWhitespace();
				int s = scanner.getPosition();
				String varName = scanner.readIdent();
				int e = scanner.getPosition();
				createdVariables.add(createVariable(C4VariableScope.LOCAL, desc, s, e, varName));
				eatWhitespace();
			} while (scanner.read() == ',');
			scanner.unread();
			if (scanner.read() != ';') {
				errorWithCode(ParserErrorCode.CommaOrSemicolonExpected, scanner.getPosition()-1, scanner.getPosition());
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

	private C4Variable createVariable(C4VariableScope scope, String desc, int start, int end, String varName) {
		C4Variable var = new C4Variable(varName, scope);
		var.setLocation(new SourceLocation(start, end));
		var.setScript(container);
		var.setUserDescription(desc);
		container.addDeclaration(var);
		return var;
	}
	
	private C4Variable findVar(String name, C4VariableScope scope) {
		switch (scope) {
		case VAR:
			return activeFunc.findVariable(name);
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
	
	private C4Variable createVarInScope(String varName, C4VariableScope scope, SourceLocation location) {
		C4Variable result = new C4Variable(varName, scope);
		switch (scope) {
		case VAR:
			result.setParentDeclaration(activeFunc);
			activeFunc.getLocalVars().add(result);
			break;
		case CONST: case STATIC: case LOCAL:
			result.setParentDeclaration(getContainer());
			getContainer().addDeclaration(result);
		}
		result.setLocation(location);
		return result;
	}

	private boolean parseVariableDeclarationInFunc(boolean declaration) {
		final int offset = scanner.getPosition();
		parsedVariable = null;

		String word = scanner.readIdent();
		C4VariableScope scope = C4VariableScope.makeScope(word);
		if (scope != null) {
			do {
				eatWhitespace();
				int nameStart = scanner.getPosition();
				String varName = scanner.readIdent();
				int nameEnd = scanner.getPosition();
				if (declaration) {
					// construct C4Variable object and register it
					C4Variable previousDeclaration = findVar(varName, scope);
					/*if (previousDeclaration == null) {
						if (scope == C4VariableScope.VAR_VAR) {
							if (findVar(varName, C4VariableScope.VAR_LOCAL) != null)
								warningWithCode(ParserErrorCode.IdentShadowed, nameStart, nameEnd, varName, Keywords.LocalNamed + " " + varName);
						}
					}*/
					C4Variable var = previousDeclaration != null ? previousDeclaration : createVarInScope(varName, scope, new SourceLocation(nameStart, nameEnd));
					parsedVariable = var;
				}
				// check if there is initial content
				eatWhitespace();
				C4Variable var = activeFunc.findVariable(varName);
				parsedVariable = var;
				if (scanner.read() == '=') {
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
								errorWithCode(ParserErrorCode.ValueExpected, scanner.getPosition()-1, scanner.getPosition(), true);
							else {
								var.inferTypeFromAssignment(val, this);
							}
						}
					} catch (ParsingException e) {} // an exception thrown from here will halt the whole parsing process
				}
				else {
					scanner.unread();
				}

			} while(scanner.read() == ',');
			scanner.unread();
			return true;
		}
		else {
			scanner.seek(offset);
			return false;
		}
	}

	private C4Type parseFunctionReturnType() throws ParsingException {
		final int offset = scanner.getPosition();
		eatWhitespace();
		if (scanner.peek() == '&') {
			if (!container.getEngine().getCurrentSettings().supportsRefs) {
				errorWithCode(ParserErrorCode.EngineDoesNotSupportRefs, scanner.getPosition(), scanner.getPosition()+1, container.getEngine().getName());
			}
			scanner.read();
			return C4Type.REFERENCE;
		}
		else if (isEngine && parseIdentifier()) {
			C4Type t = C4Type.makeType(parsedString, isEngine);
			if (t != C4Type.UNKNOWN)
				return t;
		}
		scanner.seek(offset);
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
		activeFunc = newFunction();
		activeFunc.setScript(container);
		activeFunc.setUserDescription(desc);
		int startName = 0, endName = 0, startBody = 0, endBody = 0;
		boolean suspectOldStyle = false;
		String funcName = null;
		C4Type retType = C4Type.ANY;
		
		if (!firstWord.equals(Keywords.Func)) {
			activeFunc.setVisibility(C4FunctionScope.makeScope(firstWord));
			startName = scanner.getPosition();
			String shouldBeFunc = scanner.readIdent();
			if (!shouldBeFunc.equals(Keywords.Func)) {
				suspectOldStyle = true; // suspicious
				funcName = shouldBeFunc;
				endName = scanner.getPosition();
				warningWithCode(ParserErrorCode.OldStyleFunc, startName, endName);
			}
		}
		else {
			activeFunc.setVisibility(C4FunctionScope.PUBLIC);
		}
		if (!suspectOldStyle) {
			retType = parseFunctionReturnType();
			if (retType == null)
				retType = C4Type.ANY;
			eatWhitespace();
			startName = scanner.getPosition();
			funcName = scanner.readIdent();
			if (funcName == null || funcName.length() == 0)
				errorWithCode(ParserErrorCode.NameExpected, scanner.getPosition()-1, scanner.getPosition());
			endName = scanner.getPosition();
		}
		activeFunc.setName(funcName);
		activeFunc.setReturnType(retType);
		activeFunc.setOldStyle(suspectOldStyle);
		eatWhitespace();
		int shouldBeBracket = scanner.read();
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
				parseParameter(activeFunc);
				eatWhitespace();
				int readByte = scanner.read();
				if (readByte == ')')
					break; // all parameters parsed
				else if (readByte == ',')
					continue; // parse another parameter
				else {
					errorWithCode(ParserErrorCode.TokenExpected, scanner.getPosition()-1, scanner.getPosition(), (Object) new String[] {")", ","}); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} while(!scanner.reachedEOF());
		}
		endOfHeader = scanner.getPosition();
		lastComment = null;
		eatWhitespace();
		if (lastComment != null)
			activeFunc.setUserDescription(lastComment.getComment());
		// parse code block
		int token = scanner.read();
		if (token != '{') {
			if (suspectOldStyle) {
				scanner.seek(endOfHeader);
				startBody = scanner.getPosition();
				// body goes from here to start of next function...
				do {
					eatWhitespace();
					endBody = scanner.getPosition();
					String word = scanner.readIdent();
					if (word != null && word.length() > 0) {
						if (looksLikeStartOfFunction(word) || looksLikeVarDeclaration(word)) {
							scanner.seek(endBody);
							break;
						} else {
							eatWhitespace();
							if (scanner.read() == ':' && scanner.read() != ':') {
								scanner.seek(endBody);
								break;
							} else {
								scanner.seek(endBody);
							}
						}
					}
					// just move on
					consumeFunctionCodeOrReturnReadChar();

					endBody = scanner.getPosition(); // blub
				} while (!scanner.reachedEOF());
			} else {
				if (isEngine) {
					// engine functions don't need a body
					if (token != ';')
						errorWithCode(ParserErrorCode.TokenExpected, scanner.getPosition()-1, scanner.getPosition(), ";"); //$NON-NLS-1$
					startBody = endBody = -1;
				}
				else {
					errorWithCode(ParserErrorCode.TokenExpected, scanner.getPosition()-1, scanner.getPosition(), "{"); //$NON-NLS-1$
				}
			}
		} else {
			// body in {...}
			int blockDepth = 0;
			startBody = scanner.getPosition();
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
			} while (!(foundLast || scanner.reachedEOF()));
			if (foundLast)
				scanner.unread(); // go back to last '}'

			endBody = scanner.getPosition();
			eatWhitespace();
			if (scanner.read() != '}') {
				int pos = Math.min(scanner.getPosition(), scanner.getBufferLength()-1);
				errorWithCode(ParserErrorCode.TokenExpected, pos, pos+1, "}"); //$NON-NLS-1$
				return false;
			}
			// look for comment in the same line as the closing '}' which is common for functions packed into one line
			// hopefully there won't be multi-line functions with such a comment attached at the end
			Comment c = getCommentImmediatelyFollowing();
			if (c != null)
				activeFunc.setUserDescription(c.getComment());
		}
		// finish up
		activeFunc.setLocation(new SourceLocation(startName,endName));
		activeFunc.setBody(startBody != -1 ? new SourceLocation(startBody,endBody) : null);
		activeFunc.setHeader(new SourceLocation(startOfFirstWord, endOfHeader));
		container.addDeclaration(activeFunc);
		if (!activeFunc.isOldStyle())
			activeFunc = null; // to not suppress errors in-between functions
		return true;
	}

	protected C4Function newFunction() {
	    return new C4Function();
    }

	private String getTextOfLastComment(int declarationOffset) {
		String desc = (lastComment != null && lastComment.precedesOffset(declarationOffset, scanner.getBuffer())) ? lastComment.getComment().trim() : null; //$NON-NLS-1$
		lastComment = null;
		return desc;
	}
	
	private String getTextOfInlineComment() {
		int pos = scanner.getPosition();
		scanner.eat(BufferedScanner.WHITESPACE_WITHOUT_NEWLINE_CHARS);
		if (scanner.eat(BufferedScanner.NEWLINE_CHARS) == 0) {
			Comment c = parseCommentObject();
			if (c != null)
				return c.getComment();
		}
		scanner.seek(pos);
		return null;
	}

	private boolean looksLikeVarDeclaration(String word) {
		return word.equals(Keywords.GlobalNamed) || word.equals(Keywords.LocalNamed);
	}

	/**
	 * Parses all commands
	 * For use in keyword() { <code>parseCodeBlock<code> }
	 * @param offset
	 * @return
	 * @throws ParsingException 
	 */
	private boolean parseCodeBlock() throws ParsingException {
		int endOfFunc = activeFunc.getBody().getEnd();
		EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
		boolean notReached = false;
		int oldStyleEnd = endOfFunc;
		while(!scanner.reachedEOF() && scanner.getPosition() < endOfFunc) {
			Statement statement = parseStatement(options);
			if (statement == null)
				break;
			boolean statementIsComment = statement instanceof Comment;
			if (notReached) {
				// warn about statements after final return
				if (!statementIsComment)
					warningWithCode(ParserErrorCode.NeverReached, statement);
			}
			else {
				notReached = statement.getControlFlow() == ControlFlow.Return;
			}
			// after first 'real' statement don't expect function description anymore
			if (!statementIsComment) {
				options.remove(ParseStatementOption.ExpectFuncDesc);
				oldStyleEnd = statement.getExprEnd();
			}
		}
		if (activeFunc.isOldStyle())
			activeFunc.getBody().setEnd(oldStyleEnd);
		return true;
	}

	private void warnAboutTupleInReturnExpr(ExprElm expr, boolean tupleIsError) throws ParsingException {
		if (expr == null)
			return;
		if (expr instanceof Tuple) {
			if (tupleIsError) {
				errorWithCode(ParserErrorCode.TuplesNotAllowed, expr);
			} else {
				if (getStrictLevel() >= 2)
					errorWithCode(ParserErrorCode.ReturnAsFunction, expr, true);
			}
		}
		ExprElm[] subElms = expr.getSubElements();
		for (ExprElm e : subElms) {
			warnAboutTupleInReturnExpr(e, true);
		}
	}
	
	private boolean parseHexNumber() throws ParsingException {
		int offset = scanner.getPosition();
		boolean isHex = scanner.read() == '0' && scanner.read() == 'x';
		if (!isHex)
			scanner.seek(offset);
		else {
			offset += 2;
			int count = 0;
			if (isHex) {
				do {
					int readByte = scanner.read();
					if (('0' <= readByte && readByte <= '9') || ('A' <= readByte && readByte <= 'F') ||  ('a' <= readByte && readByte <= 'f'))  {
						count++;
						continue;
					}
					else {
						scanner.unread();
						if (count > 0) {
							scanner.seek(offset);
							parsedNumber = Long.parseLong(scanner.readString(count), 16);
							scanner.seek(offset+count);
						} else {
							parsedNumber = -1; // unlikely to be parsed
							return false; // well, this seems not to be a number at all
						} 
						return true;
					}
				} while(!scanner.reachedEOF());
			}
		}
		return isHex;
	}
	
	private boolean parseNumber() throws ParsingException {
		final int offset = scanner.getPosition();
		int count = 0;
		do {
			int readByte = scanner.read();
			if ('0' <= readByte && readByte <= '9') {
				count++;
				continue;
			}
			else {
				scanner.unread();
				if (count > 0) {
					break;
				} else {
					parsedNumber = -1; // unlikely to be parsed
					return false; // well, this seems not to be a number at all
				} 
			}
		} while(!scanner.reachedEOF());
		scanner.seek(offset);
		parsedNumber = Long.parseLong(scanner.readString(count));
		scanner.seek(offset+count);
		return true;
	}
	
	private boolean parseEllipsis() {
		int offset = scanner.getPosition();
		String e = scanner.readString(3);
		if (e != null && e.equals("...")) //$NON-NLS-1$
			return true;
		scanner.seek(offset);
		return false;
	}
	
	private boolean parseMemberOperator() {
		int offset = scanner.getPosition();
		int firstChar = scanner.read();
		if (firstChar == '.') {
			parsedMemberOperator = "."; //$NON-NLS-1$
			return true;
		}
		else if (firstChar == '-') {
			if (scanner.read() == '>') {
				offset = scanner.getPosition();
				eatWhitespace();
				if (scanner.read() == '~')
					parsedMemberOperator = "->~"; //$NON-NLS-1$
				else {
					parsedMemberOperator = "->"; //$NON-NLS-1$
					scanner.seek(offset);
				}
				return true;
			}
		}
		scanner.seek(offset);
		return false;
	}

	private static final class TempScript extends C4ScriptBase {
		private final String expression;
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		private static final ClonkIndex tempIndex = new ClonkIndex();

		private TempScript(String expression) {
			this.expression = expression;
		}

		@Override
		public ClonkIndex getIndex() {
			return tempIndex;
		}

		@Override
		public Object getScriptFile() {
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
		String word = scanner.readIdent();
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
		parsedString = scanner.readString(1);
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
		final int offset = scanner.getPosition();
		final char[] chars = new char[] { (char)scanner.read(), (char)scanner.read()  };
		String s = new String(chars);
		
		// never to be read as an operator
		if (s.equals("->")) { //$NON-NLS-1$
			scanner.seek(offset);
			return null;
		}

		C4ScriptOperator result = C4ScriptOperator.getOperator(s);
		if (result != null) {
			// new_variable should not be parsed as ne w_variable -.-
			if (result == C4ScriptOperator.ne || result == C4ScriptOperator.eq) {
				int followingChar = scanner.read();
				if (BufferedScanner.isWordPart(followingChar)) {
					scanner.seek(offset);
					return null;
				} else
					scanner.unread();
			}
			return result;
		}

		s = s.substring(0, 1);
		result = C4ScriptOperator.getOperator(s);
		if (result != null) {
			scanner.unread();
			return result;
		}

		scanner.seek(offset);
		return null;
	}
	
	private Set<ParserErrorCode> disabledErrors = new HashSet<ParserErrorCode>();
	
	private void disableError(ParserErrorCode error) {
		disabledErrors.add(error);
	}
	
	private void enableError(ParserErrorCode error) {
		disabledErrors.remove(error);
	}
	
	public boolean errorDisabled(ParserErrorCode error) {
		return disabledErrors.contains(error);
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
					markerWithCode(marker.code, marker.start, marker.end, true, marker.severity, marker.args);
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
	
	protected IMarker markerWithCode(ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) throws ParsingException {
		if (errorDisabled(code))
			return null;
		IMarker result = null;
		boolean silence = scriptFile == null || (activeFunc != null && activeFunc.getBody() != null && scanner.getPosition() > activeFunc.getBody().getEnd()+1);
		String problem = code.getErrorString(args);
		if (!silence && !allErrorsDisabled) {
			result = code.createMarker(scriptFile, getContainer(), ClonkCore.MARKER_C4SCRIPT_ERROR, markerStart, markerEnd, severity, problem);
			if (expressionReportingErrors != null) {
				ParserErrorCode.setExpressionLocation(result, expressionReportingErrors);
			}
		}
		if (!noThrow && severity >= IMarker.SEVERITY_ERROR)
			throw silence
				? new SilentParsingException(Reason.SilenceRequested, problem)
				: new ParsingException(problem);
		return result;
	}
	
	private void tokenExpectedError(String token) throws ParsingException {
		errorWithCode(ParserErrorCode.TokenExpected, scanner.getPosition()-1, scanner.getPosition(), false, token);
	}
	
	private boolean parseStaticFieldOperator_() {
		final int offset = scanner.getPosition();
		String o = scanner.readString(2);
		if (o.equals("::")) //$NON-NLS-1$
			return true;
		scanner.seek(offset);
		return false;
	}
	
	public ExprElm parseExpressionWithoutOperators(boolean reportErrors) throws ParsingException {
		this.eatWhitespace();
		int sequenceStart = scanner.getPosition();
		C4ScriptOperator preop = parseOperator();
		ExprElm result = null;
		if (preop != null && preop.isPrefix()) {
			ExprElm followingExpr = parseExpressionWithoutOperators(reportErrors);
			if (followingExpr == null) {
				errorWithCode(ParserErrorCode.ExpressionExpected, scanner.getPosition(), scanner.getPosition()+1);
			}
			result = new UnaryOp(preop, UnaryOp.Placement.Prefix, followingExpr);
		} else
			scanner.seek(sequenceStart); // don't skip operators that aren't prefixy
		if (result != null) {
			result.setExprRegion(sequenceStart, scanner.getPosition());
			return result;
		}
		Vector<ExprElm> elements = new Vector<ExprElm>(5);
		ExprElm elm;
		ExprElm prevElm = null;
		boolean dontCheckForPostOp = false;
		int noWhitespaceEating = sequenceStart;
		do {
			elm = null;
			
			noWhitespaceEating = scanner.getPosition();
			this.eatWhitespace();
			int elmStart = scanner.getPosition();

			// operator always ends a sequence without operators
			if (parseOperator() != null) {// || fReader.readWord().equals(Keywords.In)) {
				scanner.seek(elmStart);
				break;
			}
			// kind of a hack; stop at 'in' but only if there were other things before it
			if (elements.size() > 0 && scanner.readIdent().equals(Keywords.In)) {
				scanner.seek(elmStart);
				break;
			}
			scanner.seek(elmStart); // nothing special to end the sequence; make sure we start from the beginning
			
			// id
			if (parseID()) {
				elm = new IDLiteral(parsedID);
			}
			
			// hex number
			if (elm == null && parseHexNumber()) {
//				if (parsedNumber < Integer.MIN_VALUE || parsedNumber > Integer.MAX_VALUE)
//					warningWithCode(ErrorCode.OutOfIntRange, elmStart, fReader.getPosition(), String.valueOf(parsedNumber));
				elm = new C4ScriptExprTree.NumberLiteral(parsedNumber, true);
			}
			
			// number
			if (elm == null && parseNumber()) {
				elm = new C4ScriptExprTree.NumberLiteral(parsedNumber);
			}
			
			// string
			if (elm == null && parseString()) {
				elm = new StringLiteral(parsedString);
			}
			
			// variable or function
			if (elm == null) {
				String word = scanner.readIdent();
				if (word != null && word.length() > 0) {
					int beforeSpace = scanner.getPosition();
					this.eatWhitespace();
					if (scanner.read() == '(') {
						int s = scanner.getPosition();
						// function call
						List<ExprElm> args = new LinkedList<ExprElm>();
						parseRestOfTuple(args, reportErrors);
						CallFunc callFunc = new CallFunc(word, args.toArray(new ExprElm[args.size()]));
						callFunc.setParmsRegion(s, scanner.getPosition()-1);
						elm = callFunc;
					} else {
						scanner.seek(beforeSpace);
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
			
			// array
			if (elm == null) {
				elm = parseArrayExpression(reportErrors, prevElm);
			}
			
			if (elm == null) {
				elm = parsePropListExpression(reportErrors, prevElm);
			}
		
			// ->
			if (elm == null) {
				int fieldOperatorStart = scanner.getPosition();
				if (parseMemberOperator()) {
					eatWhitespace();
					int idOffset = scanner.getPosition()-fieldOperatorStart;
					if (parseID()) {
						eatWhitespace();
						if (!parseStaticFieldOperator_()) {
							errorWithCode(ParserErrorCode.TokenExpected, scanner.getPosition(), scanner.getPosition()+2, "::"); //$NON-NLS-1$
						}
					} else
						idOffset = 0;
					elm = new MemberOperator(parsedMemberOperator.length() == 1, parsedMemberOperator.length() == 3, parsedID, idOffset);
				}
			}
			
			// (<expr>)
			if (elm == null) {
				int parenthStartPos = scanner.getPosition();
				int c = scanner.read();
				if (c == '(') {
					ExprElm firstExpr = parseExpression(reportErrors);
					if (firstExpr == null) {
						firstExpr = ExprElm.nullExpr(scanner.getPosition(), 0);
						// might be disabled
						errorWithCode(ParserErrorCode.EmptyParentheses, parenthStartPos, scanner.getPosition()+1, true);
					}
					c = scanner.read();
					if (c == ')')
						elm = new Parenthesized(firstExpr);
					else if (c == ',') {
						errorWithCode(ParserErrorCode.TuplesNotAllowed, scanner.getPosition()-1, scanner.getPosition());
						// tuple (just for multiple parameters for return)
						List<ExprElm> tupleElms = new LinkedList<ExprElm>();
						tupleElms.add(firstExpr);
						parseRestOfTuple(tupleElms, reportErrors);
						elm = new Tuple(tupleElms.toArray(new ExprElm[0]));
					} else
						errorWithCode(ParserErrorCode.TokenExpected, scanner.getPosition()-1, scanner.getPosition(), ")"); //$NON-NLS-1$
				} else {
					scanner.unread();
				}
			}
			
			if (elm == null && parsePlaceholderString()) {
				elm = new Placeholder(parsedString);
			}
			
			// check if sequence is valid (CreateObject(BLUB)->localvar is not)
			if (elm != null) {
				if (!elm.isValidInSequence(prevElm, this)) {
					elm = null; // blub blub <- first blub is var; second blub is not part of the sequence -.-
					//fReader.seek(elmStart);
					dontCheckForPostOp = true;
					errorWithCode(ParserErrorCode.NotAllowedHere, elmStart, scanner.getPosition(), scanner.readStringAt(elmStart, scanner.getPosition()));
				} else {
					elm.setExprRegion(elmStart, scanner.getPosition());
					elements.add(elm);
					prevElm = elm;
				}
			}

		} while (elm != null);
		scanner.seek(noWhitespaceEating);
		if (elements.size() == 1) {
			// no need for sequences containing one element
			result = elements.elementAt(elements.size()-1);
		}
		else if (elements.size() > 1) {
			result = new Sequence(elements.toArray(new ExprElm[0]));
		} else {
			return null;
		}
		
		result.setExprRegion(sequenceStart, scanner.getPosition());
		if (result.getType(this) == null) {
			errorWithCode(ParserErrorCode.InvalidExpression, result);
		}
		
		if (!dontCheckForPostOp) {
			this.eatWhitespace();
			int saved = scanner.getPosition();
			C4ScriptOperator postop = parseOperator();
			if (postop != null) {
				if (postop.isPostfix()) {
					UnaryOp op = new UnaryOp(postop, UnaryOp.Placement.Postfix, result);
					op.setExprRegion(result.getExprStart(), scanner.getPosition());
					return op;
				} else {
					// a binary operator following this sequence
					scanner.seek(saved);
				}
			}
		}
		return result;
		
	}

	@SuppressWarnings("unchecked")
	private ExprElm parsePropListExpression(boolean reportErrors, ExprElm prevElm) throws ParsingException {
		ExprElm elm = null;
		int c = scanner.read();
		if (c == '{') {
			Vector<Pair<String, ExprElm>> propListElms = new Vector<Pair<String, ExprElm>>(10);
			boolean properlyClosed = false;
			boolean expectingComma = false;
			while (!scanner.reachedEOF()) {
				this.eatWhitespace();
				c = scanner.read();
				if (c == ',') {
					if (!expectingComma)
						errorWithCode(ParserErrorCode.UnexpectedToken, scanner.getPosition()-1, scanner.getPosition(), ","); //$NON-NLS-1$
					expectingComma = false;
				} else if (c == '}') {
					properlyClosed = true;
					break;
				} else {
					scanner.unread();
					if (parseString() || parseIdentifier()) {
						String name = parsedString;
						eatWhitespace();
						int c_ = scanner.read();
						if (c_ != ':' && c_ != '=') {
							scanner.unread();
							errorWithCode(ParserErrorCode.UnexpectedToken, scanner.getPosition(), scanner.getPosition()+1, (char)scanner.read());
						}
						eatWhitespace();
						ExprElm expr = parseExpression(COMMA_OR_CLOSE_BLOCK, reportErrors);
						propListElms.add(new Pair<String, ExprElm>(name, expr));
						expectingComma = true;
					}
					else {
						errorWithCode(ParserErrorCode.TokenExpected, scanner.getPosition(), scanner.getPosition()+1, Messages.TokenStringOrIdentifier);
						break;
					}
				}
			}
			if (!properlyClosed) {
				errorWithCode(ParserErrorCode.MissingClosingBracket, scanner.getPosition()-1, scanner.getPosition(), "}"); //$NON-NLS-1$
			}
			elm = new PropListExpression(propListElms.toArray((Pair<String, ExprElm>[])new Pair[propListElms.size()]));
		}
		else
			scanner.unread();
		return elm;
	}

	private ExprElm parseArrayExpression(boolean reportErrors, ExprElm prevElm) throws ParsingException {
		ExprElm elm = null;
		int c = scanner.read();
		if (c == '[') {
			if (prevElm != null) {
				// array access
				ExprElm arg = parseExpression(reportErrors);
				this.eatWhitespace();
				int t;
				switch (t = scanner.read()) {
				case ':':
					ExprElm arg2 = parseExpression(reportErrors);
					this.eatWhitespace();
					expect(']');
					elm = new ArraySliceExpression(arg, arg2);
					break;
				case ']':
					elm = new ArrayElementExpression(arg);
					break;
				default:
					errorWithCode(ParserErrorCode.UnexpectedToken, scanner.getPosition()-1, scanner.getPosition(), new Character((char) t).toString());
				}
			} else {
				// array creation
				Vector<ExprElm> arrayElms = new Vector<ExprElm>(10);
				boolean properlyClosed = false;
				boolean expectingComma = false;
				while (!scanner.reachedEOF()) {
					this.eatWhitespace();
					c = scanner.read();
					if (c == ',') {
						if (!expectingComma)
							arrayElms.add(null);
						expectingComma = false;
					} else if (c == ']') {
						properlyClosed = true;
						break;
					} else {
						scanner.unread();
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
					errorWithCode(ParserErrorCode.MissingClosingBracket, scanner.getPosition(), scanner.getPosition()+1, "]"); //$NON-NLS-1$
				}
				elm = new ArrayExpression(arrayElms.toArray(new ExprElm[0]));
				
			}
		} else { 
			scanner.unread();
		}
		return elm;
	}

	private void parseRestOfTuple(List<ExprElm> listToAddElementsTo, boolean reportErrors) throws ParsingException {
		boolean expectingComma = false;
		while (!scanner.reachedEOF()) {
			this.eatWhitespace();
			int c = scanner.read();
			if (c == ')') {
				if (!expectingComma && listToAddElementsTo.size() > 0)
					listToAddElementsTo.add(ExprElm.nullExpr(scanner.getPosition(), 0));
				break;
			} else if (c == ',') {
				if (!expectingComma) {
					listToAddElementsTo.add(ExprElm.nullExpr(scanner.getPosition(), 0));
				}
				expectingComma = false;
			} else {
				scanner.unread();
				if (listToAddElementsTo.size() > 100) {
					errorWithCode(ParserErrorCode.InternalError, scanner.getPosition(), scanner.getPosition(), Messages.InternalError_WayTooMuch);
				//	break;
				}
				ExprElm arg = parseExpression(reportErrors);
				if (arg == null) {
					errorWithCode(ParserErrorCode.ExpressionExpected, scanner.getPosition(), scanner.getPosition()+1);
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
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		@Override
		public int getExprStart() {
			return scanner.getPosition();
		}
		@Override
		public int getExprEnd() {
			return scanner.getPosition()+1;
		}
	};
	
	private ExprElm parseExpression(char[] delimiters, boolean reportErrors) throws ParsingException {
		
		final int offset = scanner.getPosition();
		
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
			if (parseEllipsis()) {
				root = new Ellipsis();
			} else {
				scanner.seek(offset);
				this.eatWhitespace();
				//int exprStart = fReader.getPosition();
				for (int state = START; state != DONE;) {
					this.eatWhitespace();
					switch (state) {
					case START:
						root = parseExpressionWithoutOperators(reportErrors);
						current = root;
						state = current != null ? OPERATOR : DONE;
						break;
					case OPERATOR:

						// end of expression?
						int c = scanner.read();
						for (int i = 0; i < delimiters.length; i++) {
							if (delimiters[i] == c) {
								state = DONE;
								scanner.unread();
								break;
							}
						}

						if (state != DONE) {
							scanner.unread();

							int operatorPos = scanner.getPosition();
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
								lastOp.setExprRegion(operatorPos, scanner.getPosition());
								state = SECONDOPERAND;
							} else {
								scanner.seek(operatorPos); // in case there was an operator but not a binary one
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
				//root.setExprRegion(exprStart, fReader.getPosition());
				// potentially throwing exceptions and stuff
				handleExpressionCreated(reportErrors, root);
			}
			
			return root;

		} finally {
			parseExpressionRecursion--;
		}
	}
	
	private transient ExprElm expressionReportingErrors;

	private final void handleExpressionCreated(boolean reportErrors, ExprElm root) throws ParsingException {
		if (reportErrors) {
			reportErrorsOf(root);
		}
		if (expressionListener != null && parseExpressionRecursion <= 1)
			expressionListener.expressionDetected(root, this);
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
		int quotes = scanner.read();
		if (quotes != '"') {
			scanner.unread();
			return false;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(scanner.readString(1));
			builder.append(scanner.readStringUntil(QUOTES_AND_NEWLINE_CHARS));
			if (BufferedScanner.isLineDelimiterChar((char) scanner.peek())) {
				errorWithCode(ParserErrorCode.StringNotClosed, scanner.getPosition()-1, scanner.getPosition(), true);
				return true;
			}
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		if (scanner.read() != '"') {
			errorWithCode(ParserErrorCode.StringNotClosed, scanner.getPosition()-1, scanner.getPosition());
		}
		parsedString = builder.toString();
		return true;
	}
	
	private boolean parseIdentifier() throws ParsingException {
		String word = scanner.readIdent();
		if (word != null && word.length() > 0) {
			parsedString = word;
			return true;
		}
		return false;
	}
	
	private boolean parsePlaceholderString() throws ParsingException {
		int delimiter = scanner.read();
		if (delimiter != '$') {
			scanner.unread();
			return false;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(scanner.readString(1));
			builder.append(scanner.readStringUntil((char)delimiter));
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		if (scanner.read() != '$') {
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
	
	private Statement parseStatementWithOwnTypeInferenceBlock(TypeInformationMerger merger, Sink<List<IStoredTypeInformation>> blockSink) throws ParsingException {
		List<IStoredTypeInformation> block = beginTypeInferenceBlock();
		if (blockSink != null)
			blockSink.receivedObject(block);
		try {
			Statement s = parseStatement();
			return s;
		} finally {
			block = endTypeInferenceBlock();
			if (expressionListener != null)
				expressionListener.endTypeInferenceBlock(block);
			merger.inject(block);
		}
	}
	
	private Statement parseStatementAndMergeTypeInformation() throws ParsingException {
		TypeInformationMerger merger = new TypeInformationMerger();
		Statement s = parseStatementWithOwnTypeInferenceBlock(merger, null);
		storedTypeInformationListStack.push(merger.finish(storedTypeInformationListStack.pop()));
		return s;
	}
	
	private Statement parseStatement(EnumSet<ParseStatementOption> options) throws ParsingException {
		parseStatementRecursion++;
		try {
			
			int emptyLines = -1;
			int delim;
			for (; (delim = scanner.peek()) != -1 && BufferedScanner.isWhiteSpace((char) delim); scanner.read()) {
				char c = (char) delim;
				if (c == '\n')
					emptyLines++;
			}
			
			//scanner.eatWhitespace();
			int start = scanner.getPosition();
			Statement result;
			C4VariableScope scope;
			
			// comment statement oO
			result = parseCommentObject();

			if (result == null) {
				String readWord = scanner.readIdent();
				if (readWord == null || readWord.length() == 0) {
					int read = scanner.read();
					if (read == '{' && !options.contains(ParseStatementOption.InitializationStatement)) {
						List<Statement> subStatements = new LinkedList<Statement>();
						boolean foundClosingBracket;
						boolean notReached = false;
						for (scanner.eatWhitespace(); !(foundClosingBracket = scanner.read() == '}') && !scanner.reachedEOF(); scanner.eatWhitespace()) {
							scanner.unread();
							Statement subStatement = parseStatement();
							if (subStatement != null) {
								subStatements.add(subStatement);
								if (notReached) {
									if (!(subStatement instanceof Comment))
										warningWithCode(ParserErrorCode.NeverReached, subStatement);
								}
								else
									notReached = subStatement.getControlFlow() != ControlFlow.Continue;
							} else
								errorWithCode(ParserErrorCode.StatementExpected, this.ERROR_PLACEHOLDER_EXPR);
						}
						if (!foundClosingBracket)
							errorWithCode(ParserErrorCode.BlockNotClosed, start, start+1);
						result = new Block(subStatements);
					}
					else if (read == ';') {
						result = new EmptyStatement();
					}
					else if (read == '[' && options.contains(ParseStatementOption.ExpectFuncDesc)) {
						String funcDesc = scanner.readStringUntil(']');
						scanner.read();
						result = new FunctionDescription(funcDesc);
					}
					else {
						scanner.unread();
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
				scanner.seek(start);
				ExprElm expression = parseExpression();
				if (expression != null) {
					result = new SimpleStatement(expression);
					if (!options.contains(ParseStatementOption.InitializationStatement))
						checkForSemicolon();
				}
				else
					result = null;
			}

			if (result != null) {
				result.setExprRegion(start, scanner.getPosition());
				reportErrorsOf(result);
				if (!options.contains(ParseStatementOption.InitializationStatement)) {
					result.warnIfNoSideEffects(this);
					if (result instanceof SimpleStatement && ((SimpleStatement)result).getExpression() instanceof BinaryOp)
						((BinaryOp)((SimpleStatement)result).getExpression()).checkTopLevelAssignment(this);
				}
				if (parseStatementRecursion == 1) {
					if (expressionListener != null) {
						switch (expressionListener.expressionDetected(result, this)) {
						case Cancel:
							throw new SilentParsingException(Reason.Cancellation, "Expression Listener Cancellation"); //$NON-NLS-1$
						}
					}
				}
				
				// inline comment attached to expression so code reformatting does not mess up the user's code too much
				Comment c = getCommentImmediatelyFollowing();
				if (c != null)
					result.setInlineComment(c);
				if (emptyLines > 0)
					result.addAttachment(new Statement.EmptyLinesAttachment(emptyLines));
			}
			return result;
		} finally {
			parseStatementRecursion--;
		}
		

	}

	private Comment getCommentImmediatelyFollowing() {
		int daring = scanner.getPosition();
		Comment c = null;
		for (int r = scanner.read(); r != -1 && (r == '/' || BufferedScanner.isWhiteSpaceButNotLineDelimiterChar((char) r)); r = scanner.read()) {
			if (r == '/') {
				scanner.unread();
				c = parseCommentObject();
				break;
			}
		}
		if (c != null)
			return c;
		else {
			scanner.seek(daring);
			return null;
		}
	}

	private VarDeclarationStatement parseVarDeclarationInStatement(EnumSet<ParseStatementOption> options, C4VariableScope scope) throws ParsingException {
		VarDeclarationStatement result;
		List<Pair<String, ExprElm>> initializations = new LinkedList<Pair<String, ExprElm>>();
		do {
			eatWhitespace();
			int varNameStart = scanner.getPosition();
			String varName = scanner.readIdent();
			// check if there is initial content
			eatWhitespace();
			C4Variable var = findVar(varName, scope);
			if (var == null) {
				// happens when parsing only the body of a function for computing context information in an editor and such
				var = createVariable(C4VariableScope.VAR, null, varNameStart, scanner.getPosition(), varName);
			}
			parsedVariable = var;
			ExprElm val;
			if (scanner.read() == '=') {
				eatWhitespace();
				val = parseExpression();
				if (val == null)
					errorWithCode(ParserErrorCode.ValueExpected, scanner.getPosition(), scanner.getPosition()+1);
				else {
					new AccessVar(var).expectedToBeOfType(val.getType(this), this, TypeExpectancyMode.Force);
				}
			}
			else {
				val = null;
				scanner.unread();
			}
			initializations.add(new Pair<String, ExprElm>(varName, val));
		} while(scanner.read() == ',');
		scanner.unread();
		result = new VarDeclarationStatement(initializations, scope);
		if (!options.contains(ParseStatementOption.InitializationStatement))
			checkForSemicolon();
		return result;
	}
	
	private static class TypeInformationMerger {
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
	}
	
	private void expect(char expected) throws ParsingException {
		if (scanner.read() != expected) {
			scanner.unread();
			tokenExpectedError(String.valueOf(expected));
		}
	}
	
	private void expect(String expected) throws ParsingException {
		String r = scanner.readIdent();
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
				errorWithCode(ParserErrorCode.KeywordInWrongPlace, scanner.getPosition()-readWord.length(), scanner.getPosition(), true, readWord);
			checkForSemicolon();
			result = new ContinueStatement();
		}
		else if (readWord.equals(Keywords.Break)) {
			if (currentLoop == null)
				errorWithCode(ParserErrorCode.KeywordInWrongPlace, scanner.getPosition()-readWord.length(), scanner.getPosition(), true, readWord);
			checkForSemicolon();
			result = new BreakStatement();
		}
		else if (readWord.equals(Keywords.Return)) {
			result = parseReturn();
		}
		else if (activeFunc.isOldStyle() && (looksLikeStartOfFunction(readWord) || scanner.peekAfterWhitespace() == ':')) {
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
		int next = scanner.read();
		ExprElm returnExpr;
		if (next == ';') {
			scanner.unread();
			returnExpr = null;
		}
		else {
			scanner.unread();
			disableError(ParserErrorCode.TuplesNotAllowed);
			if (getStrictLevel() < 2)
				disableError(ParserErrorCode.EmptyParentheses);
			returnExpr = parseExpression();
			if (returnExpr == null) {
				errorWithCode(ParserErrorCode.ValueExpected, scanner.getPosition() - 1, scanner.getPosition());				
			}
			warnAboutTupleInReturnExpr(returnExpr, false);
			enableError(ParserErrorCode.TuplesNotAllowed);
			enableError(ParserErrorCode.EmptyParentheses);
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
		offset = scanner.getPosition();
		C4Variable loopVariable = null;
		Statement initialization = null, body;
		ExprElm arrayExpr, condition, increment;
		String w = null;
		if (scanner.read() == ';') {
			// any of the for statements is optional
			//initialization = null;
		} else {
			scanner.unread();
			// special treatment for case for (e in a) -> implicit declaration of e
			int pos = scanner.getPosition();
			String varName = scanner.readIdent();
			if (!varName.equals("") && !varName.equals(Keywords.VarNamed)) { //$NON-NLS-1$
				eatWhitespace();
				w = scanner.readIdent();
				if (!w.equals(Keywords.In)) {
					w = null;
					scanner.seek(pos);
				}
				else {
					// too much manual setting of stuff
					AccessVar accessVar = new AccessVar(varName);
					accessVar.setExprRegion(pos, pos+varName.length());
					if (accessVar.getDeclImpl(this) == null) {
						createVarInScope(varName, C4VariableScope.VAR, new SourceLocation(offsetOfScriptFragment()+pos, offsetOfScriptFragment()+pos+varName.length()));
					}
					handleExpressionCreated(true, accessVar);
					initialization = new SimpleStatement(accessVar);
					initialization.setExprRegion(pos, pos+varName.length());
					reportErrorsOf(initialization);
				}
			}
			else {
				scanner.seek(pos);
			}
			if (w == null) {
				initialization = parseStatement(EnumSet.of(ParseStatementOption.InitializationStatement));
				if (initialization == null) {
					errorWithCode(ParserErrorCode.ExpectedCode, scanner.getPosition(), scanner.getPosition()+1);
				}
				loopVariable = parsedVariable; // let's just assume it's the right one
			}
		}

		if (w == null) {
			// determine loop type
			eatWhitespace();
			offset = scanner.getPosition();
			if (initialization != null) {
				if (scanner.read() == ';') { // initialization finished regularly with ';'
					offset = scanner.getPosition();
					w = null; // implies there can be no 'in'
				} else {
					scanner.unread();
					w = scanner.readIdent();
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
				errorWithCode(ParserErrorCode.ExpressionExpected, offset, scanner.getPosition()+1);
			else {
				IType t = arrayExpr.getType(this);
				if (!t.canBeAssignedFrom(C4Type.ARRAY))
					warningWithCode(ParserErrorCode.IncompatibleTypes, arrayExpr, t.toString(), C4Type.ARRAY.toString());
				if (loopVariable != null && t instanceof C4ArrayType) {
					C4ArrayType arrayType = (C4ArrayType) t;
					new AccessVar(loopVariable).expectedToBeOfType(arrayType.getElementType(), this, TypeExpectancyMode.Force);
				}
			}
			condition = null;
			increment = null;
		} else {
			loopType = LoopType.For;
			scanner.seek(offset); // if a word !equaling("in") was read

			if (scanner.read() == ';') {
				// any " optional "
				scanner.unread(); // is expected
				condition = null;
			} else {
				scanner.unread();
				condition = parseExpression();
				if (condition == null) {
					errorWithCode(ParserErrorCode.ConditionExpected, offset, scanner.getPosition());
				}
			}
			eatWhitespace();
			offset = scanner.getPosition();
			expect(';');
			eatWhitespace();
			offset = scanner.getPosition();
			if (scanner.read() == ')') {
				// " optional "
				scanner.unread(); // is expected
				increment = null;
			} else {
				scanner.unread();
				increment = parseExpression();
				if (increment == null) {
					errorWithCode(ParserErrorCode.ExpressionExpected, offset, scanner.getPosition()+1);
				}
			}
			arrayExpr = null;
		}
		eatWhitespace();
		expect(')');
		eatWhitespace();
		offset = scanner.getPosition();
		currentLoop = loopType;
		body = parseStatementAndMergeTypeInformation();
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
			condition = ExprElm.nullExpr(scanner.getPosition(), 0); // while () is valid
		eatWhitespace();
		expect(')');
		eatWhitespace();
		offset = scanner.getPosition();
		Statement body = parseStatementAndMergeTypeInformation();
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
		final int offset = scanner.getPosition();
		Statement result;
		eatWhitespace();
		expect('(');
		eatWhitespace();
		ExprElm condition = parseExpression();
		if (condition == null)
			condition = ExprElm.nullExpr(scanner.getPosition(), 0); // if () is valid
		eatWhitespace();
		expect(')');
		eatWhitespace(); // FIXME: eats comments so when transforming code the comments will be gone
		TypeInformationMerger merger = new TypeInformationMerger();
		Statement ifStatement = parseStatementWithOwnTypeInferenceBlock(merger, new Sink<List<IStoredTypeInformation>>() {
			@Override
			public void receivedObject(List<IStoredTypeInformation> item) {
				
			}
		});
		if (ifStatement == null) {
			errorWithCode(ParserErrorCode.StatementExpected, offset, offset+Keywords.If.length());
		}
		int beforeElse = scanner.getPosition();
		eatWhitespace();
		String nextWord = scanner.readIdent();
		Statement elseStatement;
		if (nextWord != null && nextWord.equals(Keywords.Else)) {
			eatWhitespace();
			int o = scanner.getPosition();
			elseStatement = parseStatementWithOwnTypeInferenceBlock(merger, null);
			if (elseStatement == null) {
				errorWithCode(ParserErrorCode.StatementExpected, o, o+Keywords.Else.length());
			}	
		}
		else {
			scanner.seek(beforeElse); // don't eat comments and stuff after if (...) ...;
			elseStatement = null;
		}
		// merge gathered type information with current list
		storedTypeInformationListStack.push(merger.finish(storedTypeInformationListStack.pop()));
		
		if (!(condition instanceof AccessVar && ((AccessVar)condition).constCondition())) {
			Object condEv = C4Type.BOOL.convert(condition.evaluateAtParseTime(getContainer()));
			if (condEv != null && condEv != ExprElm.EVALUATION_COMPLEX) {
				warningWithCode(condEv.equals(true) ? ParserErrorCode.ConditionAlwaysTrue : ParserErrorCode.ConditionAlwaysFalse,
						condition, condition.toString());
			}
		}
		
		result = new IfStatement(condition, ifStatement, elseStatement);
		return result;
	}
	
	private boolean parseID() throws ParsingException {
		final int offset = scanner.getPosition();
		parsedID = null; // reset so no old parsed ids get through
		String word = null;
		if (scanner.read() == ':' && getContainer().getEngine().getCurrentSettings().colonIDSyntax) {
			word = scanner.readIdent();
		}
		else {
			scanner.unread();
			word = scanner.readIdent();
			if (word != null && word.length() != 4) {
				scanner.seek(offset);
				return false;
			}
			if (!Utilities.looksLikeID(word)) {
				scanner.seek(offset);
				return false;
			}
		}
		parsedID = C4ID.getID(word);
		return true;
	}

	private boolean parseParameter(C4Function function) throws ParsingException {
		
		if (isEngine && parseEllipsis()) {
			addVarParmsParm(function);
			return true;
		}
		
		int s = scanner.getPosition();
		String firstWord = scanner.readIdent();
		if (firstWord.length() == 0) {
			if (scanner.read() == '&') {
				firstWord = "&"; //$NON-NLS-1$
			} else {
				scanner.unread();
				return false;
			}
		}
		int e = scanner.getPosition();
		C4Variable var = new C4Variable(null, C4VariableScope.VAR);
		C4Type type = C4Type.makeType(firstWord);
		var.forceType(type, type != C4Type.UNKNOWN && !isEngine);
		if (type == C4Type.UNKNOWN) {
			//var.setType(C4Type.ANY);
			var.setName(firstWord);
		}
		else {
			eatWhitespace();
			if (scanner.read() == '&') {
				var.setByRef(true);
				eatWhitespace();
			} else
				scanner.unread();
			int newStart = scanner.getPosition();
			String secondWord = scanner.readIdent();
			if (secondWord.length() > 0) {
				var.setName(secondWord);
				s = newStart;
				e = scanner.getPosition();
			}
			else {
				// type is name
				warningWithCode(ParserErrorCode.TypeAsName, s, e, firstWord);
				var.forceType(C4Type.ANY);
				var.setName(firstWord);
				scanner.seek(e);
			}
		}
		var.setLocation(new SourceLocation(s, e));
		var.setParentDeclaration(function);
		function.getParameters().add(var);
		return true;
	}
	
	protected void eatWhitespace() {
		while ((scanner.eatWhitespace() > 0 || parseComment()));
	}
	
	protected Comment parseCommentObject() {
		String sequence = scanner.readString(2);
		if (sequence == null) {
			return null;
		}
		else if (sequence.equals("//")) { //$NON-NLS-1$
			String commentText = scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
			//fReader.eat(BufferedScanner.NEWLINE_DELIMITERS);
			return new Comment(commentText, false);
		}
		else if (sequence.equals("/*")) { //$NON-NLS-1$
			int startMultiline = scanner.getPosition();
			while (!scanner.reachedEOF()) {
				if (scanner.read() == '*') {
					if (scanner.read() == '/') {
						String commentText = scanner.readStringAt(startMultiline, scanner.getPosition()-2);
						return new Comment(commentText, true); // genug gefressen
					}
					else {
						scanner.unread();
					}
				}
			}
			String commentText = scanner.readStringAt(startMultiline, scanner.getPosition());
			return new Comment(commentText, true);
		}
		else {
			scanner.move(-2);
			return null;
		}
	}
	
	protected boolean parseComment() {
		int offset = scanner.getPosition();
		Comment c = parseCommentObject();
		if (c != null) {
			c.setExprRegion(offset, scanner.getPosition());
			lastComment = c;
			return true;
		}
		return false;
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
	
	public interface IMarkerListener {
		void markerEncountered(ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args);
	}
	
	private void reportExpressionsAndStatements(C4Function func, IExpressionListener listener) {
		activeFunc = func;
		setExpressionListener(listener);
		strictLevel = getContainer().getStrictLevel();
		disableError(ParserErrorCode.TokenExpected);
		disableError(ParserErrorCode.InvalidExpression);
		disableError(ParserErrorCode.BlockNotClosed);
		disableError(ParserErrorCode.NotAllowedHere);
		try {
			EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
			beginTypeInferenceBlock();
			while (!scanner.reachedEOF()) {
				Statement statement = parseStatement(options);
				if (statement == null)
					break;
				if (!(statement instanceof Comment))
					options.remove(ParseStatementOption.ExpectFuncDesc);
			}
			//endTypeInferenceBlock(); not here for type information might still be needed
		} 
		catch (ParsingException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		applyStoredTypeInformationList(true);
	}
	
	public static C4ScriptParser reportExpressionsAndStatements(IDocument doc, IRegion region, C4ScriptBase context, C4Function func, IExpressionListener listener, IMarkerListener markerListener)  {
		return reportExpressionsAndStatements(doc, region.getOffset(), region.getOffset()+region.getLength(), context, func, listener, markerListener);
	}
	
	private static class ScriptParserWithMarkerListener extends C4ScriptParser {
		private IMarkerListener markerListener;
		public ScriptParserWithMarkerListener(String withString, C4ScriptBase script, IMarkerListener markerListener) {
			super(withString, script, null);
			this.markerListener = markerListener;
		}
		@Override
		protected IMarker markerWithCode(ParserErrorCode code,
				int markerStart, int markerEnd, boolean noThrow,
				int severity, Object... args) throws ParsingException {
			if (markerListener != null)
				markerListener.markerEncountered(code, markerStart+offsetOfScriptFragment(), markerEnd+offsetOfScriptFragment(), noThrow, severity, args);
			return super.markerWithCode(code, markerStart, markerEnd, noThrow, severity, args);
		}
	}
	
	public static C4ScriptParser reportExpressionsAndStatements(IDocument doc, final int statementStart, int statementEnd, C4ScriptBase context, C4Function func, IExpressionListener listener, final IMarkerListener markerListener) { 
		String statements;
		try {
			statements = doc.get(statementStart, Math.min(statementEnd-statementStart, doc.getLength()-statementStart)) + ")"; //$NON-NLS-1$
		} catch (BadLocationException e) {
			statements = ""; // well... //$NON-NLS-1$
		}
		C4ScriptParser parser = new ScriptParserWithMarkerListener(statements, context, markerListener) {
			@Override
			protected int offsetOfScriptFragment() {
				return statementStart;
			}
		};
		parser.reportExpressionsAndStatements(func, listener);
		return parser;
	}
	
	public static Statement parseStandaloneStatement(final String expression, C4Function context, IExpressionListener listener, final IMarkerListener markerListener) throws ParsingException {
		if (context == null) {
			C4ScriptBase tempScript = new TempScript(expression);
			context = new C4Function("<temp>", null, C4FunctionScope.GLOBAL); //$NON-NLS-1$
			context.setScript(tempScript);
			context.setBody(new SourceLocation(0, expression.length()));
		}
		C4ScriptParser tempParser = new ScriptParserWithMarkerListener(expression, context.getScript(), markerListener);
		tempParser.setExpressionListener(listener);
		tempParser.setActiveFunc(context);
		tempParser.beginTypeInferenceBlock();
		
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
	
	public static Statement parseStandaloneStatement(final String expression, C4Function context, IExpressionListener listener) throws ParsingException {
		return parseStandaloneStatement(expression, context, listener, null);
	}

}
