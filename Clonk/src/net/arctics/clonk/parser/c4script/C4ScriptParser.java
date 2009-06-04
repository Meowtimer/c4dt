package net.arctics.clonk.parser.c4script;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import net.arctics.clonk.ClonkCore;
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
import net.arctics.clonk.parser.c4script.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.*;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.util.Pair;
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
	
	/**
	 * Keywords of C4Script
	 */
	public interface Keywords {

		public static final String Func = "func";

		public static final String Private = "private";
		public static final String Protected = "protected";
		public static final String Public = "public";
		public static final String Global = "global";
		public static final String Const = "const";

		public static final String If = "if";
		public static final String Else = "else";
		public static final String While = "while";
		public static final String For = "for";
		public static final String In = "in";
		public static final String Return = "return";
		public static final String Par = "Par";
		public static final String Goto = "goto";
		public static final String Break = "break";
		public static final String Continue = "continue";
		public static final String Inherited = "inherited";
		public static final String SafeInherited = "_inherited";

		public static final String Image = "Image";
		public static final String Contents = "Contents";
		public static final String Condition = "Condition";

		public static final String GlobalNamed = "static";
		public static final String LocalNamed = "local";
		public static final String VarNamed = "var";

		public static final String True = "true";
		public static final String False = "false";
	}
	
	/**
	 * Contains cached engine functions that have special meaning.
	 */
	public static class CachedEngineFuncs {
		public static final C4Function Par = ClonkCore.getDefault().ENGINE_OBJECT.findFunction("Par");
		public static final C4Function Var = ClonkCore.getDefault().ENGINE_OBJECT.findFunction("Var");
	}
	
	private IExpressionListener expressionListener;

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

	private BufferedScanner fReader;
	private IFile fScript; // for project intern files
	private C4ScriptBase container;
	private C4Function activeFunc;
	private int strictLevel;
	
	// parse<Blub>() functions store their results in those
	private C4ID parsedID;
	private C4Variable parsedVariable;
	private long parsedNumber;
	private String parsedObjectFieldOperator;
	private String parsedString;
	
	private int parseExpressionRecursion;
	private int parseStatementRecursion;
	private int blockDepth;
	
	private LoopType currentLoop;
	private Comment lastComment;
	
	public static final int MAX_PAR = 10;
	public static final int MAX_NUMVAR = 20;
	public static final int UNKNOWN_PARAMETERNUM = MAX_PAR+1;
	
	/*
	 * Number of unnamed parameters used in activeFunc (Par(5) -> 6 unnamed parameters).
	 * If a complex expression is passed to Par() this variable is set to UNKNOWN_PARAMETERNUM
	 */
	private int numUnnamedParameters;
	
	private List<Pair<C4Type, C4Object>> unnamedVarTypeInformation;

	/**
	 * Informs the parser that an unnamed parameter was used by calling the Par() function with the given index expression.
	 * @param index the index expression
	 */
	public void unnamedParamaterUsed(ExprElm index) {
		if (numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
			if (index instanceof ExprNumber) {
				int number = ((ExprNumber)index).intValue();
				numUnnamedParameters = Math.max(number+1, numUnnamedParameters);
			} else
				numUnnamedParameters = UNKNOWN_PARAMETERNUM;
		}
	}
	
	private Stack<List<IStoredTypeInformation>> storedTypeInformationStack = new Stack<List<IStoredTypeInformation>>();
	
	public void beginTypeInferenceBlock() {
		storedTypeInformationStack.push(new LinkedList<IStoredTypeInformation>());
	}
	
	public List<IStoredTypeInformation> endTypeInferenceBlock() {
		return storedTypeInformationStack.pop();
	}
	
	public void applyStoredTypeInformationList(boolean soft) {
		for (IStoredTypeInformation info : storedTypeInformationStack.peek()) {
			info.apply(soft);
		}
	}
	
	/**
	 * Ask the parser to store type information about an expression. No guarantees whether type information will actually be stored.
	 */
	public void storeTypeInformation(ExprElm expression, C4Type type, C4Object objectType) {
		IStoredTypeInformation requested = requestStoredTypeInformation(expression);
		if (requested != null) {
			requested.storeObjectType(objectType);
			requested.storeType(type);
		}
	}
	
	public IStoredTypeInformation requestStoredTypeInformation(ExprElm expression) {
		if (storedTypeInformationStack.isEmpty())
			return null;
		for (IStoredTypeInformation info : storedTypeInformationStack.peek()) {
			if (info.expressionRelevant(expression))
				return info;
		}
		IStoredTypeInformation newlyCreated = expression.createStoredTypeInformation();
		if (newlyCreated != null)
			storedTypeInformationStack.peek().add(newlyCreated);
		return newlyCreated;
	}
	
	public List<IStoredTypeInformation> copyCurrentTypeInformationList() throws CloneNotSupportedException {
		List<IStoredTypeInformation> list = new ArrayList<IStoredTypeInformation>(storedTypeInformationStack.peek().size());
		for (IStoredTypeInformation info : storedTypeInformationStack.peek()) {
			list.add((IStoredTypeInformation) info.clone());
		}
		return list;
	}
	
	public void pushTypeInformationList(List<IStoredTypeInformation> list) {
		storedTypeInformationStack.push(list);
	}
	
	/**
	 * Query the type of an arbitrary expression. With some luck the parser will be able to give an answer.
	 * At the moment, only Var() calls are supported.
	 * @param expression the expression to query the type of
	 * @return
	 */
	public IStoredTypeInformation queryStoredTypeInformation(ExprElm expression, boolean wholeStack) {
		if (storedTypeInformationStack.isEmpty())
			return null;
		for (int i = storedTypeInformationStack.size()-1, levels = wholeStack ? storedTypeInformationStack.size() : 1; levels > 0; levels--,i--) {
			for (IStoredTypeInformation info : storedTypeInformationStack.get(i)) {
				if (info.expressionRelevant(expression))
					return info;
			}
		}
		return null;
	}
	
	/**
	 * Same as queryTypeOfEexpression but for object types.
	 */
	public C4Object queryObjectTypeOfExpression(ExprElm expression) {
		IStoredTypeInformation info = queryStoredTypeInformation(expression, true);
		return info != null ? info.getObjectType() : null;
	}
	
	public C4Type queryTypeOfExpression(ExprElm expression, C4Type defaultType) {
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
			if (unnamedVarTypeInformation != null)
				for (int i = 0; i < unnamedVarTypeInformation.size(); i++)
					unnamedVarTypeInformation.set(i, null);
		}
	}
	
	/**
	 * Returns the script object that is being parsed by the parser.
	 * @return
	 */
	public C4ScriptBase getContainer() {
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
		fScript = scriptFile;
		fReader = new BufferedScanner(fScript);
		container = script;
	}

	/**
	 * Creates a C4Script parser object for extern files.
	 * Results are stored in <code>object</code>
	 * @param stream
	 * @param size
	 * @param object
	 * @throws CompilerException
	 */
	public C4ScriptParser(InputStream stream, C4ScriptBase script) {
		fScript = null;
		fReader = new BufferedScanner(stream);
		container = script;
	}
	
	/**
	 * Creates a C4Script parser that parses an arbitrary string.
	 * @param withString
	 * @param script
	 */
	public C4ScriptParser(String withString, C4ScriptBase script) {
		fScript = null;
		fReader = new BufferedScanner(withString);
		container = script;
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
			fReader.seek(offset);
			try {
				eatWhitespace();
				while(!fReader.reachedEOF()) {
					if (!parseDeclaration(fReader.getPosition())) {
						eatWhitespace();
						if (!fReader.reachedEOF()) {
							int start = fReader.getPosition();
							String tokenText = parseTokenAndReturnAsString(start);
							errorWithCode(ParserErrorCode.UnexpectedToken, start, fReader.getPosition(), true, tokenText);
						}
					}
					eatWhitespace();
				}
			}
			catch (ParsingException e) {
				return;
			}
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
			strictLevel = container.strictLevel();
			for (C4Function function : container.functions()) {
				parseCodeOfFunction(function);
			}
		}
	}

	/**
	 * Parse code of one single function. parseCodeOfFunctions() calls this for all functions in the script. 
	 * @param function
	 * @throws ParsingException
	 */
	public void parseCodeOfFunction(C4Function function) throws ParsingException {
		try {
			setActiveFunc(function);
			beginTypeInferenceBlock();
			parseCodeBlock(function.getBody().getStart());
			applyStoredTypeInformationList(false);
			endTypeInferenceBlock();
			if (numUnnamedParameters < UNKNOWN_PARAMETERNUM) {
				activeFunc.createParameters(numUnnamedParameters);
			}
		}
		catch (SilentParsingException e) {
			// not really an error
		}
		catch (ParsingException e) {
			System.out.println(String.format("ParsingException in %s (%s)", activeFunc.getName(), container.getName()));
			e.printStackTrace();
			// not very exceptional
		}
		catch (Exception e) {
			// errorWithCode throws ^^;
			errorWithCode(ParserErrorCode.InternalError, fReader.getPosition(), fReader.getPosition()+1, true, e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Returns a line a region is in as a string
	 * @param region the region the line to be returned is in
	 * @return the line string
	 */
	public String getLineAt(IRegion region) {
		return fReader.getLineAt(region);
	}
	
	/**
	 * Returns a substring of the script denoted by a region
	 * @param region the region
	 * @return the substring
	 */
	public String getSubstringOfScript(IRegion region) {
		return fReader.readStringAt(region.getOffset(), region.getOffset()+region.getLength()+1);
	}
	
	/**
	 * Returns a line region for a region in the script
	 * @param region the region the line to be returned is in
	 * @return the line region
	 */
	public IRegion getLineRegion(IRegion regionInLine) {
		return fReader.getLineRegion(regionInLine);
	}

	/**
	 * Parses the declaration at offset.
	 * @param offset offset at which the declaration starts
	 * @return whether parsing was sucessful
	 * @throws ParsingException
	 */
	protected boolean parseDeclaration(int offset) throws ParsingException {
		fReader.seek(offset);
		int readByte = fReader.read();
		if (readByte == '#') {
			// ok, now is a directive
			String directiveName = fReader.readStringUntil(BufferedScanner.WHITESPACE_DELIMITERS);
			C4DirectiveType type = C4DirectiveType.makeType(directiveName);
			if (type == null) {
				warningWithCode(ParserErrorCode.UnknownDirective, offset, offset + directiveName.length());
				fReader.moveUntil(BufferedScanner.NEWLINE_DELIMITERS);
				return true;
			}
			else {
				String content = fReader.readStringUntil(BufferedScanner.NEWLINE_DELIMITERS);
				if (content != null)
					content = content.trim();
				C4Directive directive = new C4Directive(type, content);
				directive.setLocation(new SourceLocation(offset, fReader.getPosition()));
				container.addField(directive);
				return true;
			}
		}
		else {
			fReader.seek(offset);
			String word = fReader.readWord();
			if (looksLikeStartOfFunction(word)) {
				if (parseFunctionDeclaration(word, offset, fReader.getPosition())) return true;
			}
			else if (word.equals(Keywords.GlobalNamed) || word.equals(Keywords.LocalNamed)) {
				if (parseVariableDeclaration(offset)) return true;
			}
			else {
				// old-style function declaration without visibility
				eatWhitespace();
				if (fReader.read() == ':' && fReader.read() != ':') { // no :: -.-
					fReader.seek(offset); // just let parseFunctionDeclaration parse the name again
					if (parseFunctionDeclaration(Keywords.Public, offset, offset)) // just assume public
						return true;
				}
			}
		}
		fReader.seek(offset);
		return false;
	}

	/**
	 * Returns whether a parsed word looks like it might be the start of a function declaration (func, private, protected etc.)
	 * @param word
	 * @return
	 */
	private boolean looksLikeStartOfFunction(String word) {
		return word.equals(Keywords.Public) || word.equals(Keywords.Protected) || word.equals(Keywords.Private) || word.equals(Keywords.Global) || word.equals(Keywords.Func);
	}

	private boolean parseVariableDeclaration(int offset) throws ParsingException {
		String desc = getTextOfLastComment(offset);
		fReader.seek(offset);

		String word = fReader.readWord();
		if (word.equals(Keywords.GlobalNamed)) {
			eatWhitespace();
			int pos = fReader.getPosition();
			boolean constDecl = false; 
			if (fReader.readWord().equals(Keywords.Const)) {
				constDecl = true;
			} else {
				fReader.seek(pos);
			}
			do {
				eatWhitespace();
				int s = fReader.getPosition();
				String varName = fReader.readWord();
				int e = fReader.getPosition();
				if (constDecl) {
					eatWhitespace();
					if (fReader.read() != '=') {
						tokenExpectedError("=");
					}
					eatWhitespace();
					offset = fReader.getPosition();
					ExprElm constantValue = parseExpression(offset, false);
					if (constantValue == null)
						constantValue = ERROR_PLACEHOLDER_EXPR;
					if (!constantValue.isConstant()) {
						errorWithCode(ParserErrorCode.ConstantValueExpected, constantValue, true);
					}
					C4Variable var = createVariable(C4VariableScope.VAR_CONST, desc, s, e, varName);
					var.inferTypeFromAssignment(constantValue, this);
				}
				else {
					createVariable(C4VariableScope.VAR_STATIC, desc, s, e, varName);
				}
				eatWhitespace();
			} while(fReader.read() == ',');
			fReader.unread();
			if (fReader.read() != ';') {
				errorWithCode(ParserErrorCode.CommaOrSemicolonExpected, fReader.getPosition()-1, fReader.getPosition());
			}
			return true;
		}
		else if (word.equals(Keywords.LocalNamed)) {
			do {
				eatWhitespace();
				int s = fReader.getPosition();
				String varName = fReader.readWord();
				int e = fReader.getPosition();
				createVariable(C4VariableScope.VAR_LOCAL, desc, s, e, varName);
				eatWhitespace();
			} while (fReader.read() == ',');
			fReader.unread();
			if (fReader.read() != ';') {
				errorWithCode(ParserErrorCode.CommaOrSemicolonExpected, fReader.getPosition()-1, fReader.getPosition());
			}
			return true;
		}
		// local iVar, iX;
		// static const pObj = parseValue, iMat = 2;
		return false;
	}

	private C4Variable createVariable(C4VariableScope scope, String desc, int start, int end,
			String varName) {
		C4Variable var = new C4Variable(varName, scope);
		var.setLocation(new SourceLocation(start, end));
		var.setScript(container);
		var.setUserDescription(desc);
		container.addField(var);
		return var;
	}
	
	private C4Variable findVar(String name, C4VariableScope scope) {
		switch (scope) {
		case VAR_VAR:
			return activeFunc.findVariable(name);
		case VAR_CONST: case VAR_STATIC:
			C4Declaration globalField = getContainer().getIndex().findGlobalDeclaration(name);
			if (globalField instanceof C4Variable)
				return (C4Variable) globalField;
			return null;
		case VAR_LOCAL:
			return getContainer().findLocalVariable(name, false);
		default:
			return null;
		}
	}
	
	private C4Variable createVarInScope(String varName, C4VariableScope scope, SourceLocation location) {
		C4Variable result = new C4Variable(varName, scope);
		switch (scope) {
		case VAR_VAR:
			result.setParentDeclaration(activeFunc);
			activeFunc.getLocalVars().add(result);
			break;
		case VAR_CONST: case VAR_STATIC: case VAR_LOCAL:
			result.setParentDeclaration(getContainer());
			getContainer().addField(result);
		}
		result.setLocation(location);
		return result;
	}

	private boolean parseVariableDeclarationInFunc(int offset, boolean declaration) throws ParsingException {
		parsedVariable = null;
		fReader.seek(offset);

		String word = fReader.readWord();
		C4VariableScope scope = C4VariableScope.makeScope(word);
		if (scope != null) {
			do {
				eatWhitespace();
				int nameStart = fReader.getPosition();
				String varName = fReader.readWord();
				int nameEnd = fReader.getPosition();
				if (declaration) {
					// construct C4Variable object and register it
					C4Variable previousDeclaration = findVar(varName, scope); 
					C4Variable var = previousDeclaration != null ? previousDeclaration : createVarInScope(varName, scope, new SourceLocation(nameStart, nameEnd));
					parsedVariable = var;
				}
				// check if there is initial content
				eatWhitespace();
				C4Variable var = activeFunc.findVariable(varName);
				parsedVariable = var;
				if (fReader.read() == '=') {
					eatWhitespace();
					offset = fReader.getPosition();
					ExprElm val = parseExpression(offset, !declaration);
					if (!declaration) {
						if (val == null)
							errorWithCode(ParserErrorCode.ValueExpected, fReader.getPosition()-1, fReader.getPosition());
						else {
							var.inferTypeFromAssignment(val, this);
						}
					}
				}
				else {
					fReader.unread();
				}

			} while(fReader.read() == ',');
			fReader.unread();
			return true;
		}
		else {
			fReader.seek(offset);
			return false;
		}
	}

	private C4Type parseFunctionReturnType(int offset) {
		fReader.seek(offset);
		eatWhitespace();
		int readByte = fReader.read();
		if (readByte == '&') {
			return C4Type.REFERENCE;
		}
		fReader.seek(offset);
		return C4Type.ANY;
	}
	
	private int consumeFunctionCodeOrReturnReadChar(int offset) throws ParsingException {
		eatWhitespace();
		offset = fReader.getPosition();
		if (parseVariableDeclarationInFunc(offset, true))
			return 0;
		Token t = parseToken(offset);
		if (t == Token.Symbol)
			return parsedString != null ? parsedString.charAt(0) : 0;
		return 0;
	}

	/**
	 * for optimization reasons
	 * @param firstWord
	 * @param offset
	 * @return
	 * @throws ParsingException 
	 */
	private boolean parseFunctionDeclaration(String firstWord, int startOfFirstWord, int offset) throws ParsingException {
		int endOfHeader;
		fReader.seek(offset);
		String desc = getTextOfLastComment(startOfFirstWord);
		eatWhitespace();
		activeFunc = new C4Function();
		activeFunc.setScript(container);
		activeFunc.setUserDescription(desc);
		int startName = 0, endName = 0, startBody = 0, endBody = 0;
		boolean suspectOldStyle = false;
		String funcName = null;
		C4Type retType = C4Type.ANY;
		if (!firstWord.equals(Keywords.Func)) {
			activeFunc.setVisibility(C4FunctionScope.makeScope(firstWord));
			startName = fReader.getPosition();
			String shouldBeFunc = fReader.readWord();
			if (!shouldBeFunc.equals(Keywords.Func)) {
//				String problem = "Syntax error: expected 'func'";
//				createErrorMarker(offset, fReader.getPosition(), problem);
//				throw new ParsingException(problem);
				suspectOldStyle = true; // suspicious
				funcName = shouldBeFunc;
				endName = fReader.getPosition();
				warningWithCode(ParserErrorCode.OldStyleFunc, startName, endName);
			}
		}
		else {
			activeFunc.setVisibility(C4FunctionScope.FUNC_PUBLIC);
			// well, so common it can hardly be worth a warning
			//createWarningMarker(offset - firstWord.length(), offset, "Function declarations should define a scope. (public,protected,private,global)");
		}
		if (!suspectOldStyle) {
			retType = parseFunctionReturnType(fReader.getPosition());
			eatWhitespace();
			startName = fReader.getPosition();
			funcName = fReader.readWord();
			if (funcName == null || funcName.length() == 0)
				errorWithCode(ParserErrorCode.NameExpected, fReader.getPosition()-1, fReader.getPosition());
			endName = fReader.getPosition();
		}
		for(C4Function otherFunc : container.functions()) {
			if (otherFunc.getName().equals(funcName)) {
				warningWithCode(ParserErrorCode.FunctionRedeclared, startName, fReader.getPosition());
				break;
			}
		}
		activeFunc.setName(funcName);
		activeFunc.setReturnType(retType);
		activeFunc.setOldStyle(suspectOldStyle);
		eatWhitespace();
		int shouldBeBracket = fReader.read();
		if (shouldBeBracket != '(') {
			if (suspectOldStyle && shouldBeBracket == ':') {
				// old style funcs have no named parameters
			} else {
				tokenExpectedError("(");
			}
		} else {
			// get parameters
			do {
				eatWhitespace();
				offset = fReader.getPosition();
				if (parseParameter(offset, activeFunc)) offset = fReader.getPosition(); 
				eatWhitespace(offset);
				int readByte = fReader.read();
				if (readByte == ')')
					break; // all parameters parsed
				else if (readByte == ',')
					continue; // parse another parameter
				else {
					errorWithCode(ParserErrorCode.TokenExpected, fReader.getPosition()-1, fReader.getPosition(), (Object) new String[] {")", ","});
				}
			} while(!fReader.reachedEOF());
		}
		endOfHeader = fReader.getPosition();
		lastComment = null;
		eatWhitespace();
		if (lastComment != null)
			activeFunc.setUserDescription(lastComment.getComment());
		// parse code block
		if (fReader.read() != '{') {
			if (suspectOldStyle) {
				//fReader.seek(endOfHeader); // don't eat comments, they are statements
				fReader.seek(endOfHeader);
				//parseFunctionDescription(fReader.getPosition());
				startBody = fReader.getPosition();
				// body goes from here to start of next function...
				do {
					eatWhitespace();
					endBody = fReader.getPosition();
					String word = fReader.readWord();
					if (word != null && word.length() > 0) {
						if (looksLikeStartOfFunction(word) || looksLikeVarDeclaration(word)) {
							fReader.seek(endBody);
							break;
						} else {
							eatWhitespace();
							if (fReader.read() == ':' && fReader.read() != ':') {
								fReader.seek(endBody);
								break;
							} else {
								fReader.seek(endBody);
							}
						}
					}
					// just move on
					consumeFunctionCodeOrReturnReadChar(fReader.getPosition());

					endBody = fReader.getPosition(); // blub
				} while (!fReader.reachedEOF());
			} else {
				errorWithCode(ParserErrorCode.TokenExpected, fReader.getPosition()-1, fReader.getPosition(), "{");
			}
		} else {
			// body in {...}
			blockDepth = 0;
			offset = fReader.getPosition();
			startBody = offset;
			eatWhitespace();

			// new two pass strategy to be able to check if functions and variables exist
			// first pass: skip the code, just remember where it is
			boolean foundLast;
			do {
				int c = consumeFunctionCodeOrReturnReadChar(fReader.getPosition());
				if (c == '}')
					blockDepth--;
				else if (c == '{')
					blockDepth++;
				foundLast = blockDepth == -1;
			} while (!(foundLast || fReader.reachedEOF()));
			if (foundLast)
				fReader.unread(); // go back to last '}'

			endBody = fReader.getPosition();
			eatWhitespace();
			if (fReader.read() != '}') {
//				System.out.println(activeFunc.getName());
				//int pos = Math.min(fReader.getPosition()-1, fReader.getBufferLength()-1);
				int pos = Math.min(fReader.getPosition(), fReader.getBufferLength()-1);
				errorWithCode(ParserErrorCode.TokenExpected, pos, pos+1, "}");
				return false;
			}
		}
		// finish up
		activeFunc.setLocation(new SourceLocation(startName,endName));
		activeFunc.setBody(new SourceLocation(startBody,endBody));
		activeFunc.setHeader(new SourceLocation(startOfFirstWord, endOfHeader));
		container.addField(activeFunc);
		if (!activeFunc.isOldStyle())
			activeFunc = null; // to not suppress errors in-between functions
		//		functions.add(func);
		return true;
	}

	private String getTextOfLastComment(int declarationOffset) {
		String desc = (lastComment != null && lastComment.precedesOffset(declarationOffset, fReader.getBuffer())) ? lastComment.getComment().trim() : "";
		lastComment = null;
		return desc;
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
	private boolean parseCodeBlock(int offset) throws ParsingException {
		fReader.seek(offset);
		int endOfFunc = activeFunc.getBody().getEnd();
		EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc, ParseStatementOption.ParseEmptyLines);
		boolean lastWasReturn = false;
		int oldStyleEnd = endOfFunc;
		while(!fReader.reachedEOF() && fReader.getPosition() < endOfFunc) {
			Statement statement = parseStatement(fReader.getPosition(), options);
			if (statement == null)
				break;
			boolean statementIsComment = statement instanceof Comment;
			if (lastWasReturn) {
				// warn about statements after final return
				if (!statementIsComment)
					warningWithCode(ParserErrorCode.NeverReached, statement);
			}
			else {
				lastWasReturn = statement.isReturn();
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
	
	/**
	 * Parses { parseCodeBlock } or parseCode
	 * @param offset
	 * @return
	 * @throws ParsingException 
	 */
	protected boolean parseCodeSegment(int offset) throws ParsingException {
		int c = fReader.read();
		if (c == '{') { // if has block
			blockDepth++;
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseCodeBlock(fReader.getPosition())) fReader.seek(offset);
			eatWhitespace();
			if (fReader.read() != '}') {
				errorWithCode(ParserErrorCode.BlockNotClosed, fReader.getPosition()-1, fReader.getPosition());
			}
			blockDepth--;
			return true;
		} else if (c == ';') {
			return true; // empty statement
		} else {
			eatWhitespace();
			if (parseStatement(offset) != null)
				return true;
			else return false;
		}
	}

	private void warnAboutTupleInReturnExpr(ExprElm expr, boolean tupleIsError) throws ParsingException {
		if (expr == null)
			return;
		if (expr instanceof ExprTuple) {
			if (tupleIsError)
				errorWithCode(ParserErrorCode.TuplesNotAllowed, expr);
			else
				warningWithCode(ParserErrorCode.ReturnAsFunction, expr);
		}
		ExprElm[] subElms = expr.getSubElements();
		for (ExprElm e : subElms) {
			warnAboutTupleInReturnExpr(e, true);
		}
	}
	
	private boolean parseHexNumber(int offset) throws ParsingException {
		fReader.seek(offset);
		boolean isHex = fReader.read() == '0' && fReader.read() == 'x';
		if (!isHex)
			fReader.seek(offset);
		else {
			offset += 2;
			int count = 0;
			if (isHex) {
				do {
					int readByte = fReader.read();
					if (('0' <= readByte && readByte <= '9') || ('A' <= readByte && readByte <= 'F') ||  ('a' <= readByte && readByte <= 'f'))  {
						count++;
						continue;
					}
					else {
						fReader.unread();
						if (count > 0) {
							fReader.seek(offset);
							parsedNumber = Long.parseLong(fReader.readString(count), 16);
							fReader.seek(offset+count);
						} else {
							parsedNumber = -1; // unlikely to be parsed
							return false; // well, this seems not to be a number at all
						} 
						return true;
					}
				} while(!fReader.reachedEOF());
			}
		}
		return isHex;
	}
	
	private boolean parseNumber(int offset) throws ParsingException {
		fReader.seek(offset);
		int count = 0;
		do {
			int readByte = fReader.read();
			if ('0' <= readByte && readByte <= '9') {
				count++;
				continue;
			}
			else {
				fReader.unread();
				if (count > 0) {
					break;
				} else {
					parsedNumber = -1; // unlikely to be parsed
					return false; // well, this seems not to be a number at all
				} 
			}
		} while(!fReader.reachedEOF());
		fReader.seek(offset);
		parsedNumber = Long.parseLong(fReader.readString(count));
		fReader.seek(offset+count);
		return true;
	}
	
	private boolean parseEllipsis(int offset) {
		fReader.seek(offset);
		String e = fReader.readString(3);
		if (e != null && e.equals("..."))
			return true;
		fReader.seek(offset);
		return false;
	}
	
	private boolean parseObjectFieldOperator(int offset) {
		fReader.seek(offset);
		parsedObjectFieldOperator = fReader.readString(2);
		if (parsedObjectFieldOperator == null) {
			return false;
		}
		if (parsedObjectFieldOperator.equals("->")) {
			offset = fReader.getPosition();
			eatWhitespace();
			if (fReader.read() == '~')
				parsedObjectFieldOperator = parsedObjectFieldOperator + "~";
			else
				fReader.seek(offset);
			return true;
		}
		fReader.seek(offset);
		return false;
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
	
	public Token parseToken(int offset) throws ParsingException {
		fReader.seek(offset);
		if (parseString(offset))
			return Token.String;
		String word = fReader.readWord();
		if (word.length() > 0) {
			parsedString = word;
			return Token.Word;
		}
		if (parseID(offset))
			return Token.ID;
		if (parseNumber(offset))
			return Token.Number;
		C4ScriptOperator op;
		if ((op = parseOperator(offset)) != null) {
			parsedString = op.getOperatorName();
			return Token.Operator;
		}
		parsedString = fReader.readString(1);
		return Token.Symbol;
	}

	public String lastTokenAsString(Token token) {
		switch (token) {
		case ID: return parsedID.getName();
		case Number: return String.valueOf(parsedNumber);
		case String: return "\""+parsedString+"\"";
		case Word: case Symbol: case Operator: return parsedString;
		}
		return "";
	}
	
	public String parseTokenAndReturnAsString(int offset) throws ParsingException {
		return lastTokenAsString(parseToken(offset));
	}

	/**
	 * read operator at some location
	 * @param offset
	 * @return the operator referenced in the code at offset
	 */
	private C4ScriptOperator parseOperator(int offset) {
		fReader.seek(offset);

		final char[] chars = new char[] { (char)fReader.read(), (char)fReader.read()  };
		String s = new String(chars);
		
		// never to be read as an operator
		if (s.equals("->")) {
			fReader.seek(offset);
			return null;
		}

		C4ScriptOperator result = C4ScriptOperator.getOperator(s);
		if (result != null) {
			// new_variable should not be parsed as ne w_variable -.-
			if (result == C4ScriptOperator.ne || result == C4ScriptOperator.eq) {
				int followingChar = fReader.read();
				if (BufferedScanner.isWordPart(followingChar)) {
					fReader.seek(offset);
					return null;
				} else
					fReader.unread();
			}
			return result;
		}

		s = s.substring(0, 1);
		result = C4ScriptOperator.getOperator(s);
		if (result != null) {
			fReader.unread();
			return result;
		}

		fReader.seek(offset);
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
	
	void warningWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) {
		String problem = String.format(code.getErrorString(args), args);
		createWarningMarker(errorStart, errorEnd, problem);
	}
	
	void warningWithCode(ParserErrorCode code, IRegion errorRegion, Object... args) {
		warningWithCode(code, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), args);
	}
	
	void errorWithCode(ParserErrorCode code, IRegion errorRegion, Object... args) throws ParsingException {
		errorWithCode(code, errorRegion, false, args);
	}
	
	void errorWithCode(ParserErrorCode code, IRegion errorRegion, boolean noThrow, Object... args) throws ParsingException {
		errorWithCode(code, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), noThrow, args);
	}
	
	void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, boolean noThrow, Object... args) throws ParsingException {
		if (errorDisabled(code))// || (errorHook != null && errorHook.handle(code, args)))
			return;
		String problem = code.getErrorString(args);
		boolean silence = fScript == null || (activeFunc != null && activeFunc.getBody() != null && fReader.getPosition() > activeFunc.getBody().getEnd());
		if (!silence)
			createErrorMarker(errorStart, errorEnd, problem);
		if (!noThrow)
			throw silence
				? new SilentParsingException(problem)
				: new ParsingException(problem);
	}
	
	private void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) throws ParsingException {
		errorWithCode(code, errorStart, errorEnd, false, args);
	}
	
	private void tokenExpectedError(String token) throws ParsingException {
		errorWithCode(ParserErrorCode.TokenExpected, fReader.getPosition()-1, fReader.getPosition(), false, token);
	}
	
	private boolean parseStaticFieldOperator_(int offset) {
		fReader.seek(offset);
		String o = fReader.readString(2);
		if (o.equals("::"))
			return true;
		fReader.seek(offset);
		return false;
	}
	
	public ExprElm parseExpressionWithoutOperators(int offset, boolean reportErrors) throws ParsingException {
		fReader.seek(offset);
		this.eatWhitespace();
		int sequenceStart = fReader.getPosition();
		C4ScriptOperator preop = parseOperator(fReader.getPosition());
		ExprElm result = null;
		if (preop != null && preop.isPrefix()) {
			ExprElm followingExpr = parseExpressionWithoutOperators(fReader.getPosition(), reportErrors);
			if (followingExpr == null) {
				errorWithCode(ParserErrorCode.ExpressionExpected, fReader.getPosition(), fReader.getPosition()+1);
			}
			result = new ExprUnaryOp(preop, ExprUnaryOp.Placement.Prefix, followingExpr);
		} else
			fReader.seek(sequenceStart); // don't skip operators that aren't prefixy
		if (result != null) {
			result.setExprRegion(sequenceStart, fReader.getPosition());
			return result;
		}
		Vector<ExprElm> elements = new Vector<ExprElm>(5);
		ExprElm elm;
		ExprElm prevElm = null;
		boolean dontCheckForPostOp = false;
		int noWhitespaceEating = sequenceStart;
		do {
			elm = null;
			
			noWhitespaceEating = fReader.getPosition();
			this.eatWhitespace();
			int elmStart = fReader.getPosition();

			// operator always ends a sequence without operators
			if (parseOperator(fReader.getPosition()) != null) {// || fReader.readWord().equals(Keywords.In)) {
				fReader.seek(elmStart);
				break;
			}
			// kind of a hack; stop at 'in' but only if there were other things before it
			if (elements.size() > 0 && fReader.readWord().equals(Keywords.In)) {
				fReader.seek(elmStart);
				break;
			}
			fReader.seek(elmStart); // nothing special to end the sequence; make sure we start from the beginning
			
			// id
			if (parseID(fReader.getPosition())) {
				elm = new ExprID(parsedID);
			}
			
			// hex number
			if (elm == null && parseHexNumber(fReader.getPosition())) {
//				if (parsedNumber < Integer.MIN_VALUE || parsedNumber > Integer.MAX_VALUE)
//					warningWithCode(ErrorCode.OutOfIntRange, elmStart, fReader.getPosition(), String.valueOf(parsedNumber));
				elm = new ExprNumber(parsedNumber, true);
			}
			
			// number
			if (elm == null && parseNumber(fReader.getPosition())) {
				if (parsedNumber < Integer.MIN_VALUE || parsedNumber > Integer.MAX_VALUE)
					warningWithCode(ParserErrorCode.OutOfIntRange, elmStart, fReader.getPosition(), String.valueOf(parsedNumber));
				elm = new ExprNumber(parsedNumber);
			}
			
			// string
			if (elm == null && parseString(fReader.getPosition())) {
				elm = new ExprString(parsedString);
			}
			
			// variable or function
			if (elm == null) {
				String word = fReader.readWord();
				if (word != null && word.length() > 0) {
					int beforeSpace = fReader.getPosition();
					this.eatWhitespace();
					if (fReader.read() == '(') {
						// function call
						List<ExprElm> args = new LinkedList<ExprElm>();
						parseRestOfTuple(fReader.getPosition(), args, reportErrors);
						elm = new ExprCallFunc(word, args.toArray(new ExprElm[args.size()]));
					} else {
						fReader.seek(beforeSpace);
						// bool
						if (word.equals(Keywords.True))
							elm = new ExprBool(true);
						else if (word.equals(Keywords.False))
							elm = new ExprBool(false);
						else
							// variable
							elm = new ExprAccessVar(word);
					}
				}
			}
			
			// array
			if (elm == null) {
				int c = fReader.read();
				if (c == '[') {
					if (prevElm != null) {
						// array access
						ExprElm arg = parseExpression(fReader.getPosition(), reportErrors);
						this.eatWhitespace();
						if (fReader.read() != ']') {
							tokenExpectedError("]");
						}
						elm = new ExprAccessArray(arg);
					} else {
						// array creation
						Vector<ExprElm> arrayElms = new Vector<ExprElm>(10);
						boolean properlyClosed = false;
						boolean expectingComma = false;
						while (!fReader.reachedEOF()) {
							this.eatWhitespace();
							c = fReader.read();
							if (c == ',') {
								if (!expectingComma)
									arrayElms.add(null);
								expectingComma = false;
							} else if (c == ']') {
								properlyClosed = true;
								break;
							} else {
								fReader.unread();
								arrayElms.add(parseExpression(fReader.getPosition(), COMMA_OR_CLOSE_BRACKET, reportErrors));
								expectingComma = true;
							}
						}
						if (!properlyClosed) {
							errorWithCode(ParserErrorCode.MissingClosingBracket, fReader.getPosition()-1, fReader.getPosition(), "]");
						}
						elm = new ExprArray(arrayElms.toArray(new ExprElm[0]));
						
					}
				} else { 
					fReader.unread();
				}
			}
		
			// ->
			if (elm == null) {
				int fieldOperatorStart = fReader.getPosition();
				if (parseObjectFieldOperator(fReader.getPosition())) {
					eatWhitespace();
					int idOffset = fReader.getPosition()-fieldOperatorStart;
					if (parseID(fReader.getPosition())) {
						eatWhitespace();
						if (!parseStaticFieldOperator_(fReader.getPosition())) {
							errorWithCode(ParserErrorCode.TokenExpected, fReader.getPosition(), fReader.getPosition()+2, "::");
						}
					} else
						idOffset = 0;
					elm = new ExprObjectCall(parsedObjectFieldOperator.length() == 3, parsedID, idOffset);
				}
			}
			
			// (<expr>)
			if (elm == null) {
				int c = fReader.read();
				if (c == '(') {
					ExprElm firstExpr = parseExpression(fReader.getPosition(), reportErrors);
					if (firstExpr == null) {
						firstExpr = ExprElm.NULL_EXPR;
						// might be disabled
						errorWithCode(ParserErrorCode.EmptyParentheses, fReader.getPosition()-1, fReader.getPosition());
					}
					c = fReader.read();
					if (c == ')')
						elm = new ExprParenthesized(firstExpr);
					else if (c == ',') {
						errorWithCode(ParserErrorCode.TuplesNotAllowed, fReader.getPosition()-1, fReader.getPosition());
						// tuple (just for multiple parameters for return)
						List<ExprElm> tupleElms = new LinkedList<ExprElm>();
						tupleElms.add(firstExpr);
						parseRestOfTuple(fReader.getPosition(), tupleElms, reportErrors);
						elm = new ExprTuple(tupleElms.toArray(new ExprElm[0]));
					} else
						errorWithCode(ParserErrorCode.TokenExpected, fReader.getPosition()-1, fReader.getPosition(), ")");
				} else {
					fReader.unread();
				}
			}
			
			if (elm == null && parsePlaceholderString(fReader.getPosition())) {
				elm = new ExprPlaceholder(parsedString);
			}
			
			// check if sequence is valid (CreateObject(BLUB)->localvar is not)
			if (elm != null) {
				if (prevElm != null && !elm.isValidInSequence(prevElm, this)) {
					elm = null; // blub blub <- first blub is var; second blub is not part of the sequence -.-
					//fReader.seek(elmStart);
					dontCheckForPostOp = true;
					errorWithCode(ParserErrorCode.NotAllowedHere, elmStart, fReader.getPosition(), fReader.readStringAt(elmStart, fReader.getPosition()));
				} else {
					elm.setExprRegion(elmStart, fReader.getPosition());
					elements.add(elm);
					prevElm = elm;
				}
			}

		} while (elm != null);
		fReader.seek(noWhitespaceEating);
		if (elements.size() == 1) {
			// no need for sequences containing one element
			result = elements.elementAt(elements.size()-1);
		}
		else if (elements.size() > 1) {
			result = new ExprSequence(elements.toArray(new ExprElm[0]));
		} else {
			return null;
		}
		
		result.setExprRegion(sequenceStart, fReader.getPosition());
		if (result.getType(this) == null) {
			errorWithCode(ParserErrorCode.InvalidExpression, result);
		}
		
		if (!dontCheckForPostOp) {
			this.eatWhitespace();
			int saved = fReader.getPosition();
			C4ScriptOperator postop = parseOperator(fReader.getPosition());
			if (postop != null) {
				if (postop.isPostfix()) {
					ExprUnaryOp op = new ExprUnaryOp(postop, ExprUnaryOp.Placement.Postfix, result);
					op.setExprRegion(result.getExprStart(), fReader.getPosition());
					return op;
				} else {
					// a binary operator following this sequence
					fReader.seek(saved);
				}
			}
		}
		return result;
		
	}

	private void parseRestOfTuple(int offset, List<ExprElm> listToAddElementsTo, boolean reportErrors) throws ParsingException {
		fReader.seek(offset);
		boolean expectingComma = false;
		while (!fReader.reachedEOF()) {
			this.eatWhitespace();
			int c = fReader.read();
			if (c == ')') {
				if (!expectingComma && listToAddElementsTo.size() > 0)
					listToAddElementsTo.add(ExprElm.NULL_EXPR);
				break;
			} else if (c == ',') {
				if (!expectingComma) {
					listToAddElementsTo.add(ExprElm.NULL_EXPR);
				}
				expectingComma = false;
			} else {
				fReader.unread();
				if (listToAddElementsTo.size() > 100) {
					errorWithCode(ParserErrorCode.InternalError, fReader.getPosition(), fReader.getPosition(), "Way too much");
				//	break;
				}
				ExprElm arg = parseExpression(fReader.getPosition(), reportErrors);
				if (arg == null) {
					errorWithCode(ParserErrorCode.ExpressionExpected, fReader.getPosition(), fReader.getPosition()+1);
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
		@Override
		public int getExprStart() {
			return fReader.getPosition();
		}
		@Override
		public int getExprEnd() {
			return fReader.getPosition()+1;
		}
	};
	
	private ExprElm parseExpression(int offset, char[] delimiters, boolean reportErrors) throws ParsingException {
		final int START = 0;
		final int OPERATOR = 1;
		final int SECONDOPERAND = 2;
		final int DONE = 3;

		parseExpressionRecursion++;
		try {

			ExprElm root = null;
			ExprElm current = null;
			ExprBinaryOp lastOp = null;

			// magical thingie to pass all parameters to inherited
			if (parseEllipsis(offset)) {
				return new ExprEllipsis();
			}

			fReader.seek(offset);
			this.eatWhitespace();
			//int exprStart = fReader.getPosition();
			for (int state = START; state != DONE;) {
				this.eatWhitespace();
				switch (state) {
				case START:
					root = parseExpressionWithoutOperators(fReader.getPosition(), reportErrors);
					current = root;
					state = current != null ? OPERATOR : DONE;
					break;
				case OPERATOR:

					// end of expression?
					int c = fReader.read();
					for (int i = 0; i < delimiters.length; i++) {
						if (delimiters[i] == c) {
							state = DONE;
							fReader.unread();
							break;
						}
					}

					if (state != DONE) {
						fReader.unread();

						int operatorPos = fReader.getPosition();
						C4ScriptOperator op = parseOperator(fReader.getPosition());
						if (op != null && op.isBinary()) {
							int priorOfNewOp = op.getPriority();
							ExprElm newLeftSide = null;
							ExprBinaryOp theOp = null;
							for (ExprElm opFromBottom = current.getParent(); opFromBottom instanceof ExprBinaryOp; opFromBottom = opFromBottom.getParent()) {
								ExprBinaryOp oneOp = (ExprBinaryOp) opFromBottom;
								if (priorOfNewOp > oneOp.getOperator().getPriority() || (priorOfNewOp == oneOp.getOperator().getPriority() && op.isRightAssociative())) {
									theOp = oneOp;
									break;
								}
							}
							if (theOp != null) {
								newLeftSide = theOp.getRightSide();
								current = lastOp = new ExprBinaryOp(op);
								theOp.setRightSide(current);
							} else {
								newLeftSide = root;
								current = root = lastOp = new ExprBinaryOp(op);
							}
							lastOp.setLeftSide(newLeftSide);
							lastOp.setExprRegion(operatorPos, fReader.getPosition());
							state = SECONDOPERAND;
						} else {
							fReader.seek(operatorPos); // in case there was an operator but not a binary one
							state = DONE;
						}
					}
					break;
				case SECONDOPERAND:
					ExprElm rightSide = parseExpressionWithoutOperators(fReader.getPosition(), reportErrors);
					if (rightSide == null)
						errorWithCode(ParserErrorCode.OperatorNeedsRightSide, lastOp);
					((ExprBinaryOp)current).setRightSide(rightSide);
					lastOp = (ExprBinaryOp)current;
					current = rightSide;
					state = OPERATOR;
					break;

				}
			}
			if (root != null) {
				//root.setExprRegion(exprStart, fReader.getPosition());
				// potentially throwing exceptions and stuff
				if (reportErrors)
					root.reportErrors(this);
				if (expressionListener != null && parseExpressionRecursion == 1)
					expressionListener.expressionDetected(root, this);
			}
			
			return root;

		} finally {
			parseExpressionRecursion--;
		}
		
	}
	
	private static final char[] SEMICOLON_DELIMITER = new char[] { ';' };
	private static final char[] COMMA_OR_CLOSE_BRACKET = new char[] { ',', ']' };
	
	private ExprElm parseExpression(int offset, boolean reportErrors) throws ParsingException {
		return parseExpression(offset, SEMICOLON_DELIMITER, reportErrors);
	}
	
	private ExprElm parseExpression(int offset) throws ParsingException {
		return parseExpression(offset, true);
	}
	
	private boolean parseString(int offset) throws ParsingException {
		fReader.seek(offset);
		int delimiter = fReader.read();
		if (delimiter != '"') {
			fReader.unread();
			return false;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(fReader.readString(1));
			builder.append(fReader.readStringUntil((char)delimiter));
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		if (fReader.read() != '"') {
			errorWithCode(ParserErrorCode.StringNotClosed, offset, fReader.getPosition()-1);
		}
		parsedString = builder.toString();
		return true;
	}
	
	private boolean parsePlaceholderString(int offset) throws ParsingException {
		fReader.seek(offset);
		int delimiter = fReader.read();
		if (delimiter != '$') {
			fReader.unread();
			return false;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(fReader.readString(1));
			builder.append(fReader.readStringUntil((char)delimiter));
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		if (fReader.read() != '$') {
			throw new ParsingException("Internal parsing error.");
		}
		parsedString = builder.toString();
		return true;
	}
	
	public enum ParseStatementOption {
		InitializationStatement,
		ExpectFuncDesc,
		ParseEmptyLines;
		
		public static final EnumSet<ParseStatementOption> NoOptions = EnumSet.noneOf(ParseStatementOption.class);
	}
	
	private Statement parseStatement(int offset) throws ParsingException {
		return parseStatement(offset, ParseStatementOption.NoOptions);
	}
	
	private Statement parseStatementWithOwnTypeInferenceBlock(int offset, TypeInformationMerger merger) throws ParsingException {
		beginTypeInferenceBlock();
		Statement s = parseStatement(offset);
		merger.inject(endTypeInferenceBlock());
		return s;
	}
	
	private Statement parseStatementAndMergeTypeInformation(int offset) throws ParsingException {
		TypeInformationMerger merger = new TypeInformationMerger();
		Statement s = parseStatementWithOwnTypeInferenceBlock(offset, merger);
		storedTypeInformationStack.push(merger.finish(storedTypeInformationStack.pop()));
		return s;
	}
	
	private Statement parseStatement(int offset, EnumSet<ParseStatementOption> options) throws ParsingException {
		parseStatementRecursion++;
		try {
			fReader.seek(offset);
			fReader.eatWhitespace();
			int start = fReader.getPosition();
			Statement result;
			C4VariableScope scope;
			
			// comment statement oO
			result = parseCommentObject(fReader.getPosition());
			
			/*
			if (options.contains(ParseStatementOption.ParseEmptyLines)) {
				int numLines = 0;
				for (int delim = fReader.read(); BufferedScanner.isLineDelimiterChar((char) delim) ? true : !fReader.unread();) {
					if (delim == '\n')
						numLines++;
				}
				if (numLines > 0)
					result = new EmptyLines(numLines);
			}*/

			if (result == null) {
				String readWord = fReader.readWord();
				if (readWord == null || readWord.length() == 0) {
					int read = fReader.read();
					if (read == '{' && !options.contains(ParseStatementOption.InitializationStatement)) {
						List<Statement> subStatements = new LinkedList<Statement>();
						boolean foundClosingBracket;
						for (fReader.eatWhitespace(); !(foundClosingBracket = fReader.read() == '}') && !fReader.reachedEOF(); fReader.eatWhitespace()) {
							fReader.unread();
							Statement subStatement = parseStatement(fReader.getPosition());
							if (subStatement != null)
								subStatements.add(subStatement);
							else
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
						String funcDesc = fReader.readStringUntil(']');
						fReader.read();
						result = new FunctionDescription(funcDesc);
					}
					else {
						fReader.unread();
						ExprElm expression = parseExpression(offset);
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
					result = parseKeyword(offset, readWord);
				else
					result = null;
			}

			// just an expression that needs to be wrapped as a statement
			if (result == null) {
				fReader.seek(start);
				ExprElm expression = parseExpression(fReader.getPosition());
				if (expression != null) {
					result = new SimpleStatement(expression);
					if (!options.contains(ParseStatementOption.InitializationStatement))
						checkForSemicolon();
				}
				else
					result = null;
			}

			if (result != null) {
				result.setExprRegion(start, fReader.getPosition());
				result.reportErrors(this);
				if (!options.contains(ParseStatementOption.InitializationStatement)) {
					result.warnIfNoSideEffects(this);
					if (result instanceof SimpleStatement && ((SimpleStatement)result).getExpression() instanceof ExprBinaryOp)
						((ExprBinaryOp)((SimpleStatement)result).getExpression()).checkTopLevelAssignment(this);
				}
				if (parseStatementRecursion == 1) {
					if (expressionListener != null) {
						expressionListener.expressionDetected(result, this);
					}
				}
			}
			
			int daring = fReader.getPosition();
			Comment c = null;
			for (int r = fReader.read(); r != -1 && (r == '/' || BufferedScanner.isWhiteSpaceButNotLineDelimiterChar((char) r)); r = fReader.read()) {
				if (r == '/') {
					c = parseCommentObject(fReader.getPosition()-1);
					break;
				}
			}
			if (c != null)
				result.setInlineComment(c);
			else
				fReader.seek(daring);
			return result;
		} finally {
			parseStatementRecursion--;
		}
		

	}

	private Statement parseVarDeclarationInStatement(
			EnumSet<ParseStatementOption> options, C4VariableScope scope)
			throws ParsingException {
		int offset;
		Statement result;
		List<Pair<String, ExprElm>> initializations = new LinkedList<Pair<String, ExprElm>>();
		do {
			eatWhitespace();
			String varName = fReader.readWord();
			// check if there is initial content
			eatWhitespace();
			C4Variable var = findVar(varName, scope);
			parsedVariable = var;
			ExprElm val;
			if (fReader.read() == '=') {
				eatWhitespace();
				offset = fReader.getPosition();
				val = parseExpression(offset);
				if (val == null)
					errorWithCode(ParserErrorCode.ValueExpected, fReader.getPosition()-1, fReader.getPosition());
				else {
					storeTypeInformation(new ExprAccessVar(var), val.getType(this), val.guessObjectType(this));
					var.inferTypeFromAssignment(val, this);
				}
			}
			else {
				val = null;
				fReader.unread();
			}
			initializations.add(new Pair<String, ExprElm>(varName, val));
		} while(fReader.read() == ',');
		fReader.unread();
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

		public List<IStoredTypeInformation> getMerged() {
			return merged;
		}

		public List<IStoredTypeInformation> finish(List<IStoredTypeInformation> finalList) {
			if (merged == null)
				return finalList;
			return mergeTypeInformationLists(finalList, merged);
		}
	}

	private Statement parseKeyword(int offset, String readWord) throws ParsingException {
		Statement result = null;
		if (readWord.equals(Keywords.If)) {
			eatWhitespace();
			if (fReader.read() != '(') {
				tokenExpectedError("(");
			}
			eatWhitespace();
			ExprElm condition = parseExpression(fReader.getPosition());
			if (condition == null)
				condition = ExprElm.NULL_EXPR; // if () is valid
			eatWhitespace();
			if (fReader.read() != ')') {
				tokenExpectedError(")");
			}
			eatWhitespace(); // FIXME: eats comments so when transforming code the comments will be gone
			TypeInformationMerger merger = new TypeInformationMerger();
			Statement ifStatement = parseStatementWithOwnTypeInferenceBlock(fReader.getPosition(), merger);
			if (ifStatement == null) {
				errorWithCode(ParserErrorCode.StatementExpected, offset, offset+Keywords.If.length());
			}
			int beforeElse = fReader.getPosition();
			eatWhitespace();
			offset = fReader.getPosition();
			String nextWord = fReader.readWord();
			Statement elseStatement;
			if (nextWord != null && nextWord.equals(Keywords.Else)) {
				eatWhitespace();
				offset = fReader.getPosition();
				elseStatement = parseStatementWithOwnTypeInferenceBlock(fReader.getPosition(), merger);
				if (elseStatement == null) {
					errorWithCode(ParserErrorCode.StatementExpected, offset, offset+Keywords.Else.length());
				}	
			}
			else {
				fReader.seek(beforeElse); // don't eat comments and stuff after if (...) ...;
				elseStatement = null;
			}
			// merge gathered type information with current list
			storedTypeInformationStack.push(merger.finish(storedTypeInformationStack.pop()));
			result = new IfStatement(condition, ifStatement, elseStatement);
		}
		else if (readWord.equals(Keywords.While)) {
			currentLoop = LoopType.While;
			//			if (!readWord.equals(readWord.toLowerCase())) {
			//				String problem = "Syntax error: you should only use lower case letters in keywords. ('" + readWord.toLowerCase() + "' instead of '" + readWord + "')"; 
			//				createErrorMarker(fReader.getPosition() - readWord.length(), fReader.getPosition(), problem);
			//				throw new ParsingException(problem);
			//			}
			eatWhitespace();
			if (fReader.read() != '(') {
				tokenExpectedError("(");
			}
			eatWhitespace();
			ExprElm condition = parseExpression(fReader.getPosition());
			if (condition == null)
				condition = ExprElm.NULL_EXPR; // while () is valid
			eatWhitespace();
			if (fReader.read() != ')') {
				tokenExpectedError(")");
			}
			eatWhitespace();
			offset = fReader.getPosition();
			Statement body = parseStatementAndMergeTypeInformation(fReader.getPosition());
			if (body == null) {
				errorWithCode(ParserErrorCode.StatementExpected, offset, offset+4);
			}
			result = new WhileStatement(condition, body);
		}
		else if (readWord.equals(Keywords.For)) {
			eatWhitespace();
			if (fReader.read() != '(') {
				tokenExpectedError("(");					
			}
			eatWhitespace();

			// initialization
			offset = fReader.getPosition();
			C4Variable loopVariable = null;
			Statement initialization, body;
			ExprElm arrayExpr, condition, increment;
			if (fReader.read() == ';') {
				// any of the for statements is optional
				initialization = null;
			} else {
				fReader.unread();
				initialization = parseStatement(fReader.getPosition(), EnumSet.of(ParseStatementOption.InitializationStatement));
				if (initialization == null) {
					errorWithCode(ParserErrorCode.ExpectedCode, fReader.getPosition(), fReader.getPosition()+1);
				}
				loopVariable = parsedVariable; // let's just assume it's the right one
			}

			// determine loop type
			eatWhitespace();
			offset = fReader.getPosition();
			String w;
			if (initialization != null) {
				if (fReader.read() == ';') { // initialization finished regularly with ';'
					offset = fReader.getPosition();
					w = null; // implies there can be no 'in'
				} else {
					fReader.unread();
					w = fReader.readWord();
				}
			}
			else
				w = null; // if there is no initialization statement at all there can also be no 'in'
			LoopType loopType;
			if (w != null && w.equals(Keywords.In)) {
				// it's a for (x in array) loop!
				loopType = LoopType.IterateArray;
				eatWhitespace();
				arrayExpr = parseExpression(fReader.getPosition());
				if (arrayExpr == null)
					errorWithCode(ParserErrorCode.ExpressionExpected, offset, fReader.getPosition()+1);
				else {
					C4Type t = arrayExpr.getType(this);
					if (!t.canBeAssignedFrom(C4Type.ARRAY))
						warningWithCode(ParserErrorCode.IncompatibleTypes, arrayExpr, t.toString(), C4Type.ARRAY.toString());
					if (loopVariable != null)
						loopVariable.inferTypeFromAssignment(arrayExpr.getExemplaryArrayElement(this), this);
				}
				condition = null;
				increment = null;
			} else {
				loopType = LoopType.For;
				fReader.seek(offset); // if a word !equaling("in") was read

				if (fReader.read() == ';') {
					// any " optional "
					fReader.unread(); // is expected
					condition = null;
				} else {
					fReader.unread();
					condition = parseExpression(fReader.getPosition());
					if (condition == null) {
						errorWithCode(ParserErrorCode.ConditionExpected, offset, fReader.getPosition());
					}
				}
				eatWhitespace();
				offset = fReader.getPosition();
				if (fReader.read() != ';') {
					tokenExpectedError(";");
				}
				eatWhitespace();
				offset = fReader.getPosition();
				if (fReader.read() == ')') {
					// " optional "
					fReader.unread(); // is expected
					increment = null;
				} else {
					fReader.unread();
					increment = parseExpression(fReader.getPosition());
					if (increment == null) {
						errorWithCode(ParserErrorCode.ExpressionExpected, offset, fReader.getPosition()+1);
					}
				}
				arrayExpr = null;
			}
			eatWhitespace();
			if (fReader.read() != ')') {
				tokenExpectedError(")");
			}
			eatWhitespace();
			offset = fReader.getPosition();
			currentLoop = loopType;
			body = parseStatementAndMergeTypeInformation(fReader.getPosition());
			if (body == null) {
				errorWithCode(ParserErrorCode.StatementExpected, offset, offset+4);
			}
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
		}
		else if (readWord.equals(Keywords.Continue)) {
			if (currentLoop == null)
				errorWithCode(ParserErrorCode.KeywordInWrongPlace, fReader.getPosition()-readWord.length(), fReader.getPosition(), true, readWord);
			checkForSemicolon();
			result = new ContinueStatement();
		}
		else if (readWord.equals(Keywords.Break)) {
			if (currentLoop == null)
				errorWithCode(ParserErrorCode.KeywordInWrongPlace, fReader.getPosition()-readWord.length(), fReader.getPosition(), true, readWord);
			checkForSemicolon();
			result = new BreakStatement();
		}
		else if (readWord.equals(Keywords.Return)) {
			eatWhitespace();
			int next = fReader.read();
			ExprElm returnExpr;
			if (next == ';') {
				fReader.unread();
				returnExpr = null;
			}
			else {
				fReader.unread();
				offset = fReader.getPosition();
				disableError(ParserErrorCode.TuplesNotAllowed);
				disableError(ParserErrorCode.EmptyParentheses);
				returnExpr = parseExpression(fReader.getPosition());
				if (returnExpr == null) {
					errorWithCode(ParserErrorCode.ValueExpected, fReader.getPosition() - 1, fReader.getPosition());				
				}
				warnAboutTupleInReturnExpr(returnExpr, false);
				enableError(ParserErrorCode.TuplesNotAllowed);
				enableError(ParserErrorCode.EmptyParentheses);
			}
			result = new ReturnStatement(returnExpr);
			checkForSemicolon();
		}
		else if (activeFunc.isOldStyle() && (looksLikeStartOfFunction(readWord) || fReader.peekAfterWhitespace() == ':')) {
			// whoops, too far
			return null;
		}
		else
			result = null;

		return result;
	}

	private void checkForSemicolon() throws ParsingException {
		eatWhitespace();
		int readChar = fReader.read();
		if (readChar != ';')
			tokenExpectedError(";");
	}
	
	private boolean parseID(int offset) throws ParsingException {
		parsedID = null; // reset so no old parsed ids get through
		fReader.seek(offset);
		String word = fReader.readWord();
		if (word != null && word.length() != 4) {
			fReader.seek(offset);
			return false;
		}
		if (!Utilities.looksLikeID(word)) {
			fReader.seek(offset);
			return false;
		}
		parsedID = C4ID.getID(word);
		return true;
	}

	private boolean parseParameter(int offset, C4Function function) throws ParsingException {
		fReader.seek(offset);
		int s = fReader.getPosition();
		String firstWord = fReader.readWord();
		if (firstWord.length() == 0) {
			if (fReader.read() == '&') {
				firstWord = "&";
			} else {
				fReader.unread();
				return false;
			}
		}
		int e = fReader.getPosition();
		C4Variable var = new C4Variable(null, C4VariableScope.VAR_VAR);
		C4Type type = C4Type.makeType(firstWord);
		var.setType(type, type != C4Type.UNKNOWN);
		if (type == C4Type.UNKNOWN) {
			//var.setType(C4Type.ANY);
			var.setName(firstWord);
		}
		else {
			eatWhitespace();
			if (fReader.read() == '&') {
				var.setByRef(true);
				eatWhitespace();
			} else
				fReader.unread();
			int newStart = fReader.getPosition();
			String secondWord = fReader.readWord();
			if (secondWord.length() > 0) {
				var.setName(secondWord);
				s = newStart;
				e = fReader.getPosition();
			}
			else {
				// type is name
				warningWithCode(ParserErrorCode.TypeAsName, s, e, firstWord);
				var.setType(C4Type.ANY);
				var.setName(firstWord);
				fReader.seek(e);
			}
		}
		var.setLocation(new SourceLocation(s, e));
		var.setParentDeclaration(function);
		function.getParameters().add(var);
		return true;
	}
	
	protected void eatWhitespace(int offset) {
		fReader.seek(offset);
		while ((fReader.eatWhitespace() > 0 || parseComment(fReader.getPosition())));
	}
	
	protected void eatWhitespace() {
		eatWhitespace(fReader.getPosition());
	}
	
	protected Comment parseCommentObject(int offset) {
		fReader.seek(offset);
		String sequence = fReader.readString(2);
		if (sequence == null) {
			return null;
		}
		else if (sequence.equals("//")) {
			String commentText = fReader.readStringUntil(BufferedScanner.NEWLINE_DELIMITERS);
			//fReader.eat(BufferedScanner.NEWLINE_DELIMITERS);
			return new Comment(commentText, false);
		}
		else if (sequence.equals("/*")) {
			int startMultiline = fReader.getPosition();
			while (!fReader.reachedEOF()) {
				if (fReader.read() == '*') {
					if (fReader.read() == '/') {
						String commentText = fReader.readStringAt(startMultiline, fReader.getPosition()-2);
						return new Comment(commentText, true); // genug gefressen
					}
					else {
						fReader.unread();
					}
				}
			}
			String commentText = fReader.readStringAt(startMultiline, fReader.getPosition());
			return new Comment(commentText, true);
		}
		else {
			fReader.move(-2);
			return null;
		}
	}
	
	protected boolean parseComment(int offset) {
		Comment c = parseCommentObject(offset);
		if (c != null) {
			c.setExprRegion(offset, fReader.getPosition());
			lastComment = c;
			return true;
		}
		return false;
	}
	
	private IMarker createMarker(int start, int end, String message, int severity) {
		if (fScript == null) return null;
		try {
			IMarker marker = fScript.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.TRANSIENT, false);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.CHAR_START, start);
			marker.setAttribute(IMarker.CHAR_END, end);
			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	private IMarker createErrorMarker(int start, int end, String message) {
		return createMarker(start, end, message, IMarker.SEVERITY_ERROR);
	}
	
	private IMarker createWarningMarker(int start, int end, String message) {
		return createMarker(start, end, message, IMarker.SEVERITY_WARNING);
	}

	public void clean() {
		synchronized (container) {
			try {
				if (fScript != null)
					fScript.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
			container.clearFields();
		}
	}
	
	public static C4ScriptParser reportExpressionsAndStatements(IDocument doc, IRegion region, C4ScriptBase context, C4Function func, IExpressionListener listener) throws BadLocationException, ParsingException {
		return reportExpressionsAndStatements(doc, region.getOffset(), region.getOffset()+region.getLength(), context, func, listener);
	}
	
	public static C4ScriptParser reportExpressionsAndStatements(IDocument doc, int statementStart, int statementEnd, C4ScriptBase context, C4Function func, IExpressionListener listener) throws ParsingException {
		String expr;
		try {
			expr = doc.get(statementStart, Math.min(statementEnd-statementStart, doc.getLength()-statementStart));
		} catch (BadLocationException e) {
			expr = ""; // well...
		}
		C4ScriptParser parser = new C4ScriptParser(expr, context);
		parser.activeFunc = func;
		parser.setExpressionListener(listener);
		parser.disableError(ParserErrorCode.TokenExpected);
		parser.disableError(ParserErrorCode.InvalidExpression);
		parser.disableError(ParserErrorCode.BlockNotClosed);
		parser.disableError(ParserErrorCode.NotAllowedHere);
		try {
			EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
			parser.beginTypeInferenceBlock();
			while (!parser.fReader.reachedEOF()) {
				Statement statement = parser.parseStatement(parser.fReader.getPosition(), options);
				if (statement == null)
					break;
				if (!(statement instanceof Comment))
					options.remove(ParseStatementOption.ExpectFuncDesc);
			}
			//parser.endTypeInferenceBlock(); not here for type information might still be needed
		} 
		catch (SilentParsingException e) {
			// silent...
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		parser.applyStoredTypeInformationList(true);
		return parser;
	}
	
	public static ExprElm parseStandaloneExpression(final String expression, C4ScriptBase context) throws ParsingException {
		if (context == null) {
			context = new C4ScriptBase() {
				private static final long serialVersionUID = 1L;

				@Override
				public ClonkIndex getIndex() {
					return ClonkCore.getDefault().EXTERN_INDEX;
				}

				@Override
				public Object getScriptFile() {
					try {
						return new SimpleScriptStorage(expression, expression);
					} catch (UnsupportedEncodingException e) {
						return null;
					}
				}
			
			};
		}
		C4ScriptParser tempParser = new C4ScriptParser(expression, context);
		return tempParser.parseExpression(0);
	}

}
