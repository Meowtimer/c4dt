package net.arctics.clonk.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.arctics.clonk.parser.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class C4ScriptParser {

	protected static class BufferedScanner {

		public static final char[] WHITESPACE_DELIMITERS = new char[] { ' ', '	', '\n', '\r', '\t' };
		public static final char[] NEWLINE_DELIMITERS = new char[] { '\n', '\r' };

		private byte[] buffer;
		private int size;
		private InputStream contents;
		private int offset;

		public BufferedScanner(IFile file) throws CompilerException {
			try {
				contents = file.getContents();
				long longSize = new File(file.getLocation().toString()).length();
				if (longSize > Integer.MAX_VALUE) {
					throw new CompilerException("Script file is too large. Unable to parse such a file");
				}
				size = (int)longSize;
				offset = 0;
				buffer = new byte[size];
				contents.read(buffer);
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public BufferedScanner(InputStream stream, long fileSize) throws CompilerException {
			try {
				contents = stream;
				if (fileSize > Integer.MAX_VALUE) {
					throw new CompilerException("Script file is too large. Unable to parse such a file");
				}
				size = (int)fileSize;
				offset = 0;
				buffer = new byte[size];
				contents.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public int read() {
			if (offset >= size) return -1;
			return buffer[offset++];
		}

		public void unread() {
			offset--;
		}

		public String readString(int length) {
			if (offset+length >= size) 
				return null;
			try {
				String result = new String(buffer,offset,length,"ISO-8859-1");
				offset += length;
				return result;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return "Encoding 'ISO-8859-1' is not available on this system.";
			}
		}

		/**
		 * Reads a code-word. (like regexp class [0-9a-zA-Z_])
		 * @return the code-word
		 */
		public String readWord() {
			int start = offset;
			int length = 0;
			do {
				int readByte = read();
				if (
						('0' <= readByte && readByte <= '9') ||
						('A' <= readByte && readByte <= 'Z') ||
						('a'<= readByte && readByte <= 'z') ||
						(readByte == '_')) {
					length++;
				}
				else {
					seek(start);
					return readString(length);
				}
			} while(!reachedEOF());
			return null;
		}

		/**
		 * Reads a string until a char from <code>delimiters</code> occurs
		 * @param delimiters
		 * @return string sequence, without delimiter char
		 */
		public String readStringUntil(char[] delimiters) {
			int start = offset;
			int i = 0;
			do {
				int readByte = read();
				for(i = 0; i < delimiters.length;i++) {
					if (readByte == delimiters[i]) {
						i = offset - start - 1; // variable reuse
						seek(start);
						return readString(i);
					}
				}
			} while(!reachedEOF());
			return null;
		}

		/**
		 * Moves offset until a char from <code>delimiters</code> occurs
		 * @param delimiters
		 */
		public void moveUntil(char[] delimiters) {
			do {
				int readByte = read();
				for(int i = 0; i < delimiters.length;i++) {
					if (readByte == delimiters[i]) {
						return;
					}
				}
			} while(!reachedEOF());
		}

		/**
		 * Moves offset until any other char than <code>delimiters</code> occurs
		 * @param delimiters
		 */
		public int eat(char[] delimiters) {
			int result = 0;
			do {
				int readByte = read();
				boolean isDelimiter = true;
				for(int i = 0; i < delimiters.length;i++) {
					if (readByte != delimiters[i]) {
						isDelimiter = false;
					}
					else {
						isDelimiter = true;
						result++;
						break;
					}
				}
				if (!isDelimiter) {
					unread();
					return result;
				}
			} while(!reachedEOF());
			return result;
		}

		public int eatWhitespace() {
			return eat(WHITESPACE_DELIMITERS);
		}

		/**
		 * Absolute offset manipulation
		 * @param newPos
		 * @return new offset
		 */
		public int seek(int newPos) {
			offset = newPos;
			if (offset >= size) offset = size - 1;
			return offset;
		}

		/**
		 * Relative offset manipulation
		 * @param distance
		 * @return new offset
		 */
		public int move(int distance) {
			offset += distance;
			if (offset >= size) offset = size - 1;
			return offset;
		}

		/**
		 * If end of file reached
		 * @return whether eof reached
		 */
		public boolean reachedEOF() {
			return (offset >= size-1);
		}

		/**
		 * Current offset
		 * @return offset
		 */
		public int getPosition() {
			return offset;
		}

		public String readStringAt(int start, int end) {
			int p = getPosition();
			seek(start);
			String result = readString(end-start);
			seek(p);
			return result;
		}
	}

	public static class ParsingException extends Exception {

		private static final long serialVersionUID = 5596886615974079864L;

		public ParsingException(String msg) {
			super(msg);
		}
	}

	private BufferedScanner fReader;
	private IFile fScript; // for project intern files
	private C4Object container;

	private InputStream stream; // for extern files

	//	private List<C4Directive> directives = new LinkedList<C4Directive>();
	//	private List<C4Function> functions = new LinkedList<C4Function>();
	//	private List<C4Variable> variables = new LinkedList<C4Variable>();

	private C4Function activeFunc;

	/**
	 * Creates a C4Script parser object.
	 * Results are stored in <code>object</code>
	 * @param script
	 * @param obj
	 * @throws CompilerException
	 */
	public C4ScriptParser(IFile script, C4Object object) throws CompilerException {
		fScript = script;
		fReader = new BufferedScanner(fScript);
		container = object;
	}

	/**
	 * Creates a C4Script parser object for extern files.
	 * Results are stored in <code>object</code>
	 * @param stream
	 * @param size
	 * @param object
	 * @throws CompilerException
	 */
	public C4ScriptParser(InputStream stream, long size, C4Object object) throws CompilerException {
		fScript = null;
		this.stream = stream;
		fReader = new BufferedScanner(this.stream, size);
		container = object;
	}

	public void parse() {
		try {
			fScript.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		container.clearFields(); // guess it's a good place to delete old stuff
		int offset = 0;
		fReader.seek(offset);
		try {
			eatWhitespace();
			while(!fReader.reachedEOF()) {
				if (!parseDeclaration(fReader.getPosition())) return;
				eatWhitespace();
			}
		}
		catch (ParsingException e) {
			return;
		}
	}

	protected boolean parseDeclaration(int offset) throws ParsingException {
		fReader.seek(offset);
		int readByte = fReader.read();
		if (readByte == '#') {
			// ok, now is a directive
			String directiveName = fReader.readStringUntil(BufferedScanner.WHITESPACE_DELIMITERS);
			C4DirectiveType type = C4DirectiveType.makeType(directiveName);
			if (type == null) {
				createErrorMarker(offset, offset + directiveName.length(), "Unknown directive name '" + directiveName + "'.");
				fReader.moveUntil(BufferedScanner.NEWLINE_DELIMITERS);
				return true;
			}
			else {
				String content = fReader.readStringUntil(BufferedScanner.NEWLINE_DELIMITERS);
				container.definedDirectives.add(new C4Directive(type, content));
				return true;
			}
		}
		else {
			fReader.seek(offset);
			String word = fReader.readWord();
			if (word.equalsIgnoreCase("public") || word.equalsIgnoreCase("protected") || word.equalsIgnoreCase("private") || word.equalsIgnoreCase("global") || word.equals("func")) {
				if (parseFunctionDeclaration(word, fReader.getPosition())) return true;
			}
			else if (word.equalsIgnoreCase("static") || word.equalsIgnoreCase("local")) {
				if (parseVariableDeclaration(offset)) return true;
			}
		}
		return false;
	}

	private boolean parseVariableDeclaration(int offset) throws ParsingException {
		fReader.seek(offset);

		String word = fReader.readWord();
		if (word.equalsIgnoreCase("static")) {
			if (!word.equals("static")) {
				String problem = "Syntax error: write '" + word.toLowerCase() + "' instead of '" + word + "'";
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			do {
				eatWhitespace();
				int s = fReader.getPosition();
				String varName = fReader.readWord();
				int e = fReader.getPosition();
				if (varName.equals("const")) {
					eatWhitespace();
					s = fReader.getPosition();
					String constName = fReader.readWord();
					e = fReader.getPosition();
					eatWhitespace();
					if (fReader.read() != '=') {
						String problem = "Syntax error: '=' expected";
						createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
						throw new ParsingException(problem);
					}
					eatWhitespace();
					offset = fReader.getPosition();
					if (!parseID(offset) && !parseNumber(offset) && !parseString(offset)) {
						String problem = "Syntax error: constant value expected (ID, number, string)";
						createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
						throw new ParsingException(problem);
					}
					C4Variable var = new C4Variable(constName,C4VariableScope.VAR_STATIC);
					var.setLocation(new SourceLocation(s, e));
					var.setObject(container);
					//					var.setType(C4Type.)
					container.addField(var);
				}
				else {
					C4Variable var = new C4Variable(varName, C4VariableScope.VAR_STATIC);
					var.setLocation(new SourceLocation(s, e));
					var.setObject(container);
					container.addField(var);
				}
				eatWhitespace();
			} while(fReader.read() == ',');
			fReader.unread();
			if (fReader.read() != ';') {
				String problem = "Syntax error: expected ';' or ','";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			return true;
		}
		else if (word.equalsIgnoreCase("local")) {
			if (!word.equals("local")) {
				String problem = "Syntax error: write '" + word.toLowerCase() + "' instead of '" + word + "'";
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			do {
				eatWhitespace();
				int s = fReader.getPosition();
				String varName = fReader.readWord();
				int e = fReader.getPosition();
				C4Variable v = new C4Variable(varName, C4VariableScope.VAR_LOCAL);
				v.setLocation(new SourceLocation(s, e));
				v.setObject(container);
				container.addField(v);
				eatWhitespace();
			} while(fReader.read() == ',');
			fReader.unread();
			if (fReader.read() != ';') {
				String problem = "Syntax error: expected ';' or ','";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			return true;
		}
		// local iVar, iX;
		// static const pObj = parseValue, iMat = 2;
		return false;
	}

	private boolean parseVarVariableDeclaration(int offset) throws ParsingException {
		fReader.seek(offset);

		String word = fReader.readWord();
		if (word.equals("var")) {
			// there is a function called Var
//			if (!word.equals("var")) {
//				String problem = "Syntax error: write '" + word.toLowerCase() + "' instead of '" + word + "'";
//				createErrorMarker(offset, fReader.getPosition(), problem);
//				throw new ParsingException(problem);
//			}
			do {
				eatWhitespace();
				String varName = fReader.readWord();
				// construct C4Variable object and register it
				C4Variable var = new C4Variable(varName,C4VariableScope.VAR_VAR);
				var.setObject(container);
				activeFunc.getLocalVars().add(var);
				// check if there is initial content
				eatWhitespace();
				if (fReader.read() == '=') {
					eatWhitespace();
					offset = fReader.getPosition();
					if (!parseValue(offset)) {
						String problem = "Syntax error: value expected";
						createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
						throw new ParsingException(problem);
					}
				}
				else {
					fReader.unread();
				}
			} while(fReader.read() == ',');
			fReader.unread();
			//			if (fReader.read() != ';') {
			//				String problem = "Syntax error: expected ';' or ','";
			//				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			//				throw new ParsingException(problem);
			//			}
			return true;
		}
		else {
			fReader.seek(offset);
			return false;
		}
	}

	@SuppressWarnings("unused")
	private boolean parseFunctionDeclaration(int offset) throws ParsingException {
		fReader.seek(offset);
		return parseFunctionDeclaration(fReader.readWord(),fReader.getPosition());
	}

	/**
	 * for optimization reasons
	 * @param firstWord
	 * @param offset
	 * @return
	 * @throws ParsingException 
	 */
	private boolean parseFunctionDeclaration(String firstWord, int offset) throws ParsingException {
		fReader.seek(offset);
		eatWhitespace();
		activeFunc = new C4Function();
		activeFunc.setObject(container);
		int startName, endName, startBody, endBody;
		if (!firstWord.equals("func")) {
			activeFunc.setVisibility(C4FunctionScope.makeScope(firstWord));
			if (!fReader.readWord().equalsIgnoreCase("func")) {
				String problem = "Syntax error: expected 'func'";
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		}
		else {
			activeFunc.setVisibility(C4FunctionScope.FUNC_PUBLIC);
			createWarningMarker(offset - firstWord.length(), fReader.getPosition(), "Function declarations should define a scope. (public,protected,private,global)");
		}
		eatWhitespace();
		startName = fReader.getPosition();
		// get function name
		int funcNameStart = fReader.getPosition();
		String funcName = fReader.readWord();
		endName = fReader.getPosition();
		for(C4Function otherFunc : container.definedFunctions) {
			if (otherFunc.getName().equalsIgnoreCase(funcName)) {
				createWarningMarker(funcNameStart, fReader.getPosition(), "Function overload: this function is already declared in this script");
				break;
			}
		}
		activeFunc.setName(funcName);
		eatWhitespace();
		if (fReader.read() != '(') {
			String problem = "Syntax error: expected '('";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
		// get parameter
		do {
			eatWhitespace();
			offset = fReader.getPosition();
			if (parseParameter(offset, activeFunc)) offset = fReader.getPosition(); 
			eatWhitespace(offset);
			int readByte = fReader.read();
			if (readByte == ')') break; // all parameter parsed
			else if (readByte == ',') continue; // parse another parameter
			else {
				String problem = "Syntax error: expected ')' or ','";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		} while(!fReader.reachedEOF());
		eatWhitespace();
		// parse code block
		if (fReader.read() != '{') {
			String problem = "Syntax error: expected '{'";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
		blockDepth = 0;
		eatWhitespace();
		offset = fReader.getPosition();
		if (parseFunctionDescription(offset)) offset = fReader.getPosition();
		startBody = offset;
		try {
			if (parseCodeBlock(offset)) {
			}
			else { // nonsense since parseCodeBlock is always true
			}
		} catch (ParsingException e) {
			// in case of errors inside the body still try to find the end of the function so that the rest of the script can be parsed 
			do {
				if (parseToken(fReader.getPosition()) == null) {
					int c = fReader.read();
					if (c == '}')
						blockDepth--;
					else if (c == '{')
						blockDepth++;
				}
			} while (blockDepth > -1 && !fReader.reachedEOF());
			if (!fReader.reachedEOF())
				fReader.unread(); // go back to last '}
		}
		endBody = fReader.getPosition();
		eatWhitespace();
		if (fReader.read() != '}') {
			String problem = "Syntax error: expected '}'";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
		// finish up
		activeFunc.setLocation(new SourceLocation(startName,endName));
		activeFunc.setBody(new SourceLocation(startBody,endBody));
		container.addField(activeFunc);
		//		functions.add(func);
		return true;
	}

	/**
	 * Parses one command
	 * i.e. if (condition) <code>parseCode</code>
	 * @param offset
	 * @return
	 * @throws ParsingException 
	 */
	private boolean parseCode(int offset) throws ParsingException {
		fReader.seek(offset);
		this.eatWhitespace();
		offset = fReader.getPosition();
		try {
		if (parseKeyword(offset)) return true;
		if (parseReturn(offset) || parseVarVariableDeclaration(offset) || parseValue(offset)) {
			if (fReader.read() == ';') {
				return true;
			}
			else {
				String problem = "Syntax error: expected ';'"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		}
		else {
			return false;
		}
		} catch (Exception e) {
			// something exceptional happened
			if (!(e instanceof ParsingException)) {
				e.printStackTrace();
				errorWithCode(ErrorCode.InternalError, offset, fReader.getPosition(), e.toString());
			} else
				throw (ParsingException)e;
			return false;
		}
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
		while(parseCode(fReader.getPosition())) {
			eatWhitespace();
		}
		// a complete code block without reading { }
		// function call
		// zuweisung
		// post-prefix
		return true;
	}

	private int blockDepth;
	
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
				String problem = "Syntax error: expected '}', code blocks have to be closed by '}'"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			blockDepth--;
			return true;
		} else if (c == ';') {
				return true; // empty statement
		} else {
			eatWhitespace();
			if (parseCode(offset)) return true;
			else return false;
		}
	}

	private boolean parseAssignment(int offset) throws ParsingException {
		fReader.seek(offset);
		if (!parseCall(offset)) {
			fReader.seek(offset);
			String varName = fReader.readWord();
			if (varName.length() == 0) {
				//				fReader.seek(offset);
				return false;
			}
		}
		eatWhitespace();

		int readByte = fReader.read();
		int secondByte = fReader.read();
		if (readByte != '=') {

			if (secondByte == '=' && ((0x2A <= readByte && readByte <= 0x2F && readByte != 0x2C && readByte != 0x2E) || readByte == '%')) {

			}
			else {
				fReader.unread();
				fReader.unread();
				return false;
			}
		}
		else {

		}
		eatWhitespace();
		offset = fReader.getPosition();
		if (!parseValue(fReader.getPosition())) {
			String problem = "Syntax error: expected a value after '='";
			createErrorMarker(offset, offset + 3, problem);
			throw new ParsingException(problem);
		}
		// *= /= %= += -= =
		return true;
	}

	private boolean parseReturn(int offset) throws ParsingException {
		fReader.seek(offset);
		String word = fReader.readWord();
		if (!word.equals("return")) {
			fReader.seek(offset);
			return false;
		}
		eatWhitespace();
		int next = fReader.read();
		if (next == ';') {
			fReader.unread();
		}
		else if (next == '(') {
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseValue(offset)) {
				createWarningMarker(offset, offset + 1, "Discouraged syntax: use 'return;' instead of 'return();' (since CR, #strict 2))");
			}
			else {
				offset = fReader.getPosition();
			}
			fReader.seek(offset);
			eatWhitespace();
			int readByte = fReader.read();
			// FIXME throw warning or error when ',' occurs
			if (readByte != ')') {
				String problem = "Syntax error: ')' expected";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		}
		else {
			fReader.unread();
			offset = fReader.getPosition();
			if (!parseValue(offset)) {
				String problem = "Syntax error: value expected";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		}
		return true;
	}

	private boolean parseCall(int offset) throws ParsingException {
		fReader.seek(offset);

		if (parsePrefixOperator(offset)) offset = fReader.getPosition();
		eatWhitespace(offset);

		String funcName = fReader.readWord();
		if (funcName.length() == 0) {
			return false;
		}
		else {
			eatWhitespace();
			if (fReader.read() != '(') return false;
			offset = fReader.getPosition();
			boolean forceValue = false;
			do {
				eatWhitespace(offset);
				if (parseValue(fReader.getPosition())) {
					eatWhitespace();
					int readByte = fReader.read();
					if (readByte == ',') {
						offset = fReader.getPosition();
						forceValue = true; // now another value must come
						continue;
					}
					else if (readByte == ')') {
						fReader.unread();
						break;
					}
					else {
						String problem = "Syntax error: expected ',' or ')' instead of '" + new String(new int[] { readByte },0,1) + "'";
						createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
						throw new ParsingException(problem);
					}
				}
				else {
					fReader.seek(offset);
					if (!forceValue) break; // this call has no parameters
					else {
						String problem = "Syntax error: expected another parameter";
						createErrorMarker(fReader.getPosition() - 1, fReader.getPosition() + 1, problem);
						throw new ParsingException(problem);
					}
				}
			} while(!fReader.reachedEOF());
			if (fReader.read() == ')') {
				offset = fReader.getPosition();
				if (parseArrayAccess(fReader.getPosition())) offset = fReader.getPosition(); 
				if (parseObjectCall(offset)) offset = fReader.getPosition();
				fReader.seek(offset);
				return true;
			}
			else {
				String problem = "Syntax error: expected ')'";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}

		}
		// parse all parameter with parseValue
		// do type checking where possible
	}

	private boolean parseValue(int offset) throws ParsingException {
		fReader.seek(offset);
		ExprElm elm = parseExpression(offset);
//		if (elm != null) {
//			StringBuilder output = new StringBuilder();
//			elm.print(output);
//			System.out.println(output.toString());
//		}
		return elm != null;
		//		if (
		//				parseParenthesisGroup(offset) ||
		//				parseNumber(offset) ||
		//				parseAssignment(offset) ||
		//				parseCall(offset) ||
		//				parseString(offset) ||
		//				parseID(offset) ||
		//				parseVariable(offset) ||
		//				parseArray(offset))
		//		{
		////			fReader.seek(offset);
		//			eatWhitespace();
		//			offset = fReader.getPosition();
		//			if (parseOperator(offset)) {
		//				eatWhitespace();
		//				offset = fReader.getPosition();
		//				if (parseValue(offset)) {
		//					return true;
		//				}
		//				else {
		//					String problem = "Syntax error: expected an expression after operator";
		//					createErrorMarker(fReader.getPosition() - 1, fReader.getPosition() + 1, problem);
		//					throw new ParsingException(problem);
		//				}
		//			}
		//			else {
		//				fReader.seek(offset);
		//				return true; // this seems to be the last sub value in the parent value
		//			}
		//		}
		//		else {
		//			return false; // there is no value at the specified offset
		//		}

		// try parseParenthesisGroup || parseCall || parseString || parseVariable
		//   if fail return fail
		//   else:
		// now parseOperator has to be successful
		//   if fail return success
		//   else:
		// now try parseValue again
		//   if fail throw syntax error
		//   else return success

		// recursive
		// calls parseCall
		// calls parseString
		// calls parseVariable
		// calls parseOperator
		// calls parseParenthesisGroup
	}

	private boolean parseParenthesisGroup(int offset) throws ParsingException {
		fReader.seek(offset);
		if (fReader.read() != '(') {
			fReader.unread();
			return false;
		}
		eatWhitespace();
		if (!parseValue(fReader.getPosition())) return false;
		eatWhitespace();
		if (fReader.read() != ')') {
			String problem = "Syntax error: expected ')'";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition() + 1, problem);
			throw new ParsingException(problem);
		}
		else {
			return true;
		}
	}

	private int parsedNumber;

	private boolean parseNumber(int offset) throws ParsingException {
		fReader.seek(offset);
		int count = 0;
		do {
			int readByte = fReader.read();
			if ('0' <= readByte && readByte <= '9') {
				count++;
				continue;
			}
			//			else if ((0x41 <= readByte && readByte <= 0x5a) || (0x61 <= readByte && readByte <= 0x7a)) {
			//				if (count == 0) return false; // well, this seems not to be a number at all
			//				String problem = "Syntax error: erroneous Ident";
			//				createErrorMarker(offset, fReader.getPosition(), problem);
			//				throw new ParsingException(problem);
			//			}
			else {
				fReader.unread();
				if (count > 0) {
					fReader.seek(offset);
					parsedNumber = Integer.parseInt(fReader.readString(count));
					fReader.seek(offset+count);
				} else {
					parsedNumber = -1; // unlikely to be parsed
					return false; // well, this seems not to be a number at all
				} 
				return true;
			}
		} while(!fReader.reachedEOF());
		return true; // TODO correct number finish?
	}

	/**
	 * Parses iVar inclusive all post- and prefixes
	 * Parses iVar->call() (implies iVar->call()->call()->call()...)
	 * @param offset
	 * @return
	 * @throws ParsingException
	 */
	private boolean parseVariable(int offset) throws ParsingException {
		fReader.seek(offset);

		if (parsePrefixOperator(offset)) offset = fReader.getPosition();
		eatWhitespace(offset);
		String varName = fReader.readWord();
		if (varName.length() == 0) {
			fReader.seek(offset);
			return false;
		}
		offset = fReader.getPosition();
		eatWhitespace();
		if (parseArrayAccess(offset)) offset = fReader.getPosition();
		eatWhitespace(offset);
		if (!parsePostfixOperator(offset))  {// entweder
			if (parseObjectCall(offset)) offset = fReader.getPosition();       // oder
		}
		else {
			offset = fReader.getPosition();
		}


		fReader.seek(offset);
		return true;
		// iVar
		// think of post- and prefixes: iVar++
		// think of: -iVar
		// think of: !bVar
	}

	/**
	 * Parses ->call(...)
	 * @param offset
	 * @return
	 * @throws ParsingException
	 */
	private boolean parseObjectCall(int offset) throws ParsingException {
		fReader.seek(offset);
		if (parseObjectFieldOperator(offset)) {
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseCall(offset)) {
				String problem = "Syntax error: expected a method name";
				createErrorMarker(fReader.getPosition(), fReader.getPosition() + 2, problem);
				throw new ParsingException(problem);
			}
			return true;
		}
		fReader.seek(offset);
		return false;
	}

	private boolean parsePostfixOperator(int offset) throws ParsingException {
		fReader.seek(offset);
		String next = fReader.readString(2);
		if (next.equals("--") || next.equals("++")) {
			return true;
		}
		return false;
	}

	//	private boolean parsePostfixOperator(int offset) throws ParsingException {
	//		fReader.seek(offset);
	//		int readByte = fReader.read();
	//		if (readByte == '-' || readByte == '+') {
	//			int secondByte = fReader.read();
	//			if (secondByte == '-' || secondByte == '+') {
	//				if (readByte != secondByte) {
	//					String problem = "Syntax error: postfix operators are either -- or ++";
	//					createErrorMarker(fReader.getPosition() - 2, fReader.getPosition(), problem);
	//					throw new ParsingException(problem);
	//				}
	//				else {
	//					return true;
	//				}
	//			}
	//			else if (secondByte == '>') {
	//				if (readByte == '+') {
	//					String problem = "Syntax error: invalid operator '+>'";
	//					createErrorMarker(fReader.getPosition() - 2, fReader.getPosition(), problem);
	//					throw new ParsingException(problem);
	//				}
	//				else {
	//					int thirdByte = fReader.read();
	//					if (thirdByte == '~') {
	//						if (!parseValue(fReader.getPosition())) {
	//							String problem = "Syntax error: expected a method name";
	//							createErrorMarker(fReader.getPosition(), fReader.getPosition() + 2, problem);
	//							throw new ParsingException(problem);
	//						}
	//						return true;
	//					}
	//					else {
	//						fReader.unread();
	//						if (!parseValue(fReader.getPosition())) {
	//							String problem = "Syntax error: expected a method name";
	//							createErrorMarker(fReader.getPosition(), fReader.getPosition() + 2, problem);
	//							throw new ParsingException(problem);
	//						}
	//						return true;
	//					}
	//				}
	//			}
	//			else {
	//				return false;
	//			}
	//		}
	//		else if (readByte == ':') {
	//			int secondByte = fReader.read();
	//			if (secondByte == ':') {
	//				if (!parseValue(fReader.getPosition())) {
	//					String problem = "Syntax error: expected a method name";
	//					createErrorMarker(fReader.getPosition(), fReader.getPosition() + 2, problem);
	//					throw new ParsingException(problem);
	//				}
	//				return true;
	//			}
	//			else {
	//				String problem = "Syntax error: expected ':";
	//				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
	//				throw new ParsingException(problem);
	//			}
	//		}
	//		else {
	//			return false;
	//		}
	//		// this is either ++ or --
	//	}

	private String parsedObjectFieldOperator;
	
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
	 * Finds :: function calls
	 * @deprecated :: operator does not exist?!
	 */
	private boolean parseStaticFieldOperator(int offset) throws ParsingException {
		fReader.seek(offset);
		if (fReader.read() == ':') {
			if (fReader.read() == ':') {
				return true;
			}
			else {
				String problem = "Syntax error: expected ':";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		}
		return false;
	}

	private boolean parsePrefixOperator(int offset) throws ParsingException {
		fReader.seek(offset);
		int readByte = fReader.read();
		if (readByte == '-' || readByte == '+') {
			int secondByte = fReader.read();
			if (secondByte == '-' || secondByte == '+') {
				if (readByte != secondByte) {
					String problem = "Syntax error: prefix operators are either -- or ++";
					createErrorMarker(fReader.getPosition() - 2, fReader.getPosition(), problem);
					throw new ParsingException(problem);
				}
				return true;
			}
			else {
				fReader.unread();
				return true;
			}
		}
		else if (readByte == '!') {
			return true;
		}
		else if (readByte == '~') {
			return true;
		}
		else {
			fReader.unread();
			return false;
		}
	}

	/**
	 * Trys to find an operator that is not an assignment operator
	 * @param offset
	 * @return
	 */
	private boolean parseOperator(int offset) {
		fReader.seek(offset);

		final char[] singleChars = new char[] { '~','!','/','*','%','-','+','<','>','&','^','|'};
		final char[][] doubleChars = new char[][] { { '-','-'}, {'+','+'}, {'*','*'}, {'<','<'}, {'>','>'}, {'<','='}, {'>','='}, {'=','='}, {'!','='}, {'S','='}, {'e','q'}, {'n','e'}, {'&','&'}, {'|','|'} };

		char readChar = (char)fReader.read();
		char secondChar = (char)fReader.read();
		for(char[] charPair : doubleChars) {
			if (charPair[0] == readChar) {
				if (charPair[1] == secondChar) return true;
			}
		}
		if (secondChar == '=') return false; // assigment
		fReader.unread();
		for(char singleChar : singleChars) {
			if (readChar == singleChar) return true;
		}
		fReader.seek(offset);
		// + - S= * % ... all operators that combines 2 values to 1
		// no assignments: =, *=, ...
		return false;
	}

	/**
	 * 
	 * @author madeen
	 * an operator
	 */
	private enum Operator {
		Tilde,
		Not,

		Divide,
		Multiply,
		Modulo,
		Subtract,
		Add,

		Smaller,
		SmallerEqual,
		Larger,
		LargerEqual,

		Equal,
		NotEqual,
		StringEqual,
		eq,
		ne,

		And,
		Or,
		BitAnd,
		BitNot,
		BitOr,

		Decrement,
		Increment,

		ShiftLeft,
		ShiftRight,

		Assign,
		AssignAdd,
		AssignSubtract,
		AssignMultiply,
		AssignDivide,
		AssignModulo,
		AssignOr,
		AssignAnd;

		public boolean isUnary() {
			return this == Not || this == Increment || this == Decrement || this == Add || this == Subtract;
		}

		public boolean isBinary() {
			return !isUnary() || this == Add || this == Subtract; // :D
		}

		public boolean isPostfix() {
			return this == Increment || this == Decrement;
		}
		public boolean isPrefix() {
			return this == Increment || this == Decrement || this == Not || this == Add || this == Subtract;
		}
		public int priority() {
			int o = this.ordinal();
			if (o >= 0 && o <= 1)
				return 6;
			if (o >= 2 && o <= 4)
				return 13;
			if (o >= 5 && o <= 6)
				return 12;
			if (o >= 7 && o <= 10)
				return 10;
			if (o >= 11 && o <= 15)
				return 9;
			if (o >= 16 && o <= 20)
				return 6;
			if (o >= 21 && o <= 22)
				return -1000;
			if (o >= 23 && o <= 24)
				return 11;
			if (o >= 25 && o <= 32)
				return 2;
			return 0;
		}

		public boolean rightAssociative() {
			int o = ordinal();
			// assignment operators
			return (o >= 25 && o <= 32);
		}

		public String operatorName() {
			return operatorToStringMap.get(this);
		}
	}
	
	public enum Token {
		String,
		Word,
		ID,
		Number
	}
	
	public Token parseToken(int offset) throws ParsingException {
		fReader.seek(offset);
		eatWhitespace();
		if (parseString(offset))
			return Token.String;
		if (fReader.readWord().length() > 0)
			return Token.Word;
		if (parseID(offset))
			return Token.ID;
		if (parseNumber(offset))
			return Token.Number;
		return null;
	}

	private static HashMap<String, Operator> createOperatorHashMap(String[] operatorNames) {
		HashMap<String, Operator> result = new HashMap<String, Operator>(Operator.values().length);
		for (int i = 0 ; i < operatorNames.length; i++) {
			result.put(operatorNames[i], Operator.values()[i]);
		}
		return result;
	}

	private static final HashMap<String, Operator> stringToOperatorMap;
	private static final HashMap<Operator, String> operatorToStringMap;
	
	static {
		stringToOperatorMap = createOperatorHashMap(new String[] {
				"~",
				"!",
				"/",
				"*",
				"%",
				"-",
				"+",
				"<",
				"<=",
				">",
				">=",
				"==",
				"!=",
				"S=",
				"eq",
				"ne",
				"&&",
				"||",
				"&",
				"^",
				"|",
				"--",
				"++",
				"<<",
				">>",
				"=",
				"+=",
				"-=",
				"*=",
				"/=",
				"%="
		});
		// i want both directions!
		operatorToStringMap = new HashMap<Operator, String>();
		for (String s : stringToOperatorMap.keySet()) {
			operatorToStringMap.put(stringToOperatorMap.get(s), s);
		}
	} 

	/**
	 * read operator at some location
	 * @param offset
	 * @return the operator referenced in the code at offset
	 */
	private Operator parseOperator_(int offset) {
		fReader.seek(offset);

		final char[] chars = new char[] { (char)fReader.read(), (char)fReader.read()  };
		String s = new String(chars);
		
		// never to be read as an operator
		if (s.equals("->")) {
			fReader.seek(offset);
			return null;
		}

		Operator result = stringToOperatorMap.get(s);
		if (result != null)
			return result;

		s = s.substring(0, 1);
		result = stringToOperatorMap.get(s);
		if (result != null) {
			fReader.unread();
			return result;
		}

		fReader.seek(offset);
		return null;
	}
	
	// classes to build an object representation of an expression

	/**
	 * @author madeen
	 * base class for making expression trees
	 */
	public abstract static class ExprElm {
		private ExprElm parent;

		public ExprElm getParent() {
			return parent;
		}

		public void setParent(ExprElm parent) {
			this.parent = parent;
		}

		public void print(StringBuilder output) {
			output.append("Implement me");
		}

		public boolean checkValidInSequence(ExprElm predecessor) {
			return true;
		}
		public C4Type getType() {
			return C4Type.UNKNOWN;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			print(b);
			return b.toString();
		}

	}

	public class ExprObjectCall extends ExprElm {
		private boolean hasTilde;
		private C4ID id;

		public ExprObjectCall(boolean hasTilde, C4ID id) {
			super();
			this.hasTilde = hasTilde;
			this.id = id;
		}

		public void print(StringBuilder output) {
			if (hasTilde)
				output.append("->~");
			else
				output.append("->");
			if (id != null) {
				output.append(id.getName());
				output.append("::");
			}
		}

		public C4ID getId() {
			return id;
		}

		public void setId(C4ID id) {
			this.id = id;
		}

		@Override
		public boolean checkValidInSequence(ExprElm predecessor) {
			return predecessor != null && (predecessor.getType() != C4Type.ARRAY && predecessor.getType() != null && predecessor.getType() != C4Type.UNKNOWN);
		}

		@Override
		public C4Type getType() {
			return null; // invalid as an expression
		}
		
	}
	
	public abstract static class ExprValue extends ExprElm {

		@Override
		public C4Type getType() {
			return C4Type.ANY;
		}
		
	}

	public static class ExprSequence extends ExprValue {
		protected final ExprElm[] elements;
		public ExprSequence(ExprElm[] elms) {
			elements = elms;
		}
		public void print(StringBuilder output) {
			for (ExprElm e : elements) {
				e.print(output);
			}
		}
		public C4Type getType() {
			return (elements == null || elements.length == 0) ? C4Type.UNKNOWN : elements[elements.length-1].getType();
		}

	}

	public static class ExprAccessField extends ExprValue {
		private C4Field field;
		protected final String fieldName;

		public ExprAccessField(String fieldName) {
			this.fieldName = fieldName;
		}
		public void print(StringBuilder output) {
			output.append(fieldName);
		}
	}

	public static class ExprAccessVar extends ExprAccessField {
		public ExprAccessVar(String varName) {
			super(varName);
		}

		@Override
		public boolean checkValidInSequence(ExprElm predecessor) {
			if (predecessor == null)
				return true;
			return false;
		}
		

	}

	public static class ExprCallFunc extends ExprAccessField {
		private final ExprElm[] params;
		public ExprCallFunc(String funcName, ExprElm[] parms) {
			super(funcName);
			params = parms;
		}
		public void print(StringBuilder output) {
			super.print(output);
			output.append("(");
			for (int i = 0; i < params.length; i++) {
				if (params[i] != null)
					params[i].print(output);
				if (i < params.length-1)
					output.append(", ");
			}
			output.append(")");
		}
	}

	public static class ExprOperator extends ExprValue {
		private final Operator operator;

		public ExprOperator(Operator operator) {
			super();
			this.operator = operator;
		}

		public Operator getOperator() {
			return operator;
		}

	}

	public static class ExprBinaryOp extends ExprOperator {
		private ExprElm leftSide, rightSide;

		public ExprBinaryOp(Operator operator, ExprElm leftSide, ExprElm rightSide) {
			super(operator);
			setLeftSide(leftSide);
			setRightSide(rightSide);
		}

		public ExprBinaryOp(Operator op) {
			super(op);
		}

		public ExprElm getLeftSide() {
			return leftSide;
		}

		public ExprElm getRightSide() {
			return rightSide;
		}

		public void setLeftSide(ExprElm leftSide) {
			this.leftSide = leftSide;
			leftSide.setParent(this);
		}

		public void setRightSide(ExprElm rightSide) {
			this.rightSide = rightSide;
			rightSide.setParent(this);
		}

		public void print(StringBuilder output) {
			leftSide.print(output);
			output.append(" ");
			output.append(getOperator().operatorName());
			output.append(" ");
			rightSide.print(output);
		}

	}
	
	public static class ExprParenthesized extends ExprValue {
		private ExprElm innerExpr;

		public ExprParenthesized(ExprElm innerExpr) {
			super();
			this.innerExpr = innerExpr;
		}
		public void print(StringBuilder output) {
			output.append("(");
			innerExpr.print(output);
			output.append(")");
		}
		public C4Type getType() {
			return innerExpr.getType();
		}
	}
	
	public static class ExprUnaryOp extends ExprOperator {
		
		public enum Placement {
			Prefix,
			Postfix
		}
		
		private final Placement placement;
		private final ExprElm argument;
		
		public ExprUnaryOp(Operator operator, Placement placement, ExprElm argument) {
			super(operator);
			this.placement = placement;
			this.argument = argument;
			this.argument.setParent(this);
		}
		
		public void print(StringBuilder output) {
			if (placement == Placement.Postfix) {
				argument.print(output);
				output.append(getOperator().operatorName());
			} else {
				output.append(getOperator().operatorName());
				argument.print(output);
			}
		}

		public ExprElm getArgument() {
			return argument;
		}
		
	}
	
	public static class ExprLiteral<T> extends ExprValue {
		private final T literal;

		public ExprLiteral(T literal) {
			super();
			this.literal = literal;
		}

		public T getLiteral() {
			return literal;
		}
		
	}
	
	public static class ExprNumber extends ExprLiteral<Integer> {

		public ExprNumber(int value) {
			super(new Integer(value));
		}
		
		public int intValue() {
			return getLiteral().intValue();
		}
		
		public void print(StringBuilder output) {
			output.append(intValue());
		}
		
		public C4Type getType() {
			return C4Type.INT;
		}
		
	}
	
	public static final class ExprString extends ExprLiteral<String> {
		public ExprString(String literal) {
			super(literal);
		}

		public String stringValue() {
			return getLiteral();
		}
		public void print(StringBuilder output) {
			output.append("\"");
			output.append(stringValue());
			output.append("\"");
		}

		@Override
		public C4Type getType() {
			return C4Type.STRING;
		}
		
	}
	
	public static final class ExprID extends ExprLiteral<C4ID> {
		public ExprID(C4ID literal) {
			super(literal);
		}

		public C4ID idValue() {
			return getLiteral();
		}
		
		public void print(StringBuilder output) {
			output.append(idValue().getName());
		}

		@Override
		public C4Type getType() {
			return C4Type.ID;
		}
		
	}
	
	public static final class ExprArrayAccess extends ExprUnaryOp {

		public ExprArrayAccess(ExprElm argument) {
			super(null, Placement.Postfix, argument);
			// TODO Auto-generated constructor stub
		}
		
		public void print(StringBuilder output) {
			output.append("[");
			getArgument().print(output);
			output.append("]");
		}

		@Override
		public boolean checkValidInSequence(ExprElm predecessor) {
			return predecessor != null;
		}
	
	}
	
	public static class ExprArray extends ExprSequence {
		public ExprArray(ExprElm[] elms) {
			super(elms);
		}

		public void print(StringBuilder output) {
			output.append("[");
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] != null)
					elements[i].print(output);
				if (i < elements.length-1)
					output.append(", ");
			}
			output.append("]");
		}
		
		@Override
		public C4Type getType() {
			return C4Type.ARRAY;
		}

		@Override
		public boolean checkValidInSequence(ExprElm predecessor) {
			return predecessor == null;
		}
		
		
	}
	
	public static class ExprEllipsis extends ExprElm {

		public ExprEllipsis() {
			super();
		}
		
		public void print(StringBuilder output) {
			output.append("...");
		}
		
	}
	
	public enum ErrorCode {
		TokenExpected, NotAllowedHere, MissingClosingBracket, InvalidExpression, InternalError, ExpressionExpected,
	}
	
	private static String[] errorStrings = new String[] {
		"'%s' expected",
		"'%s' not allowed here",
		"Missing '%s'",
		"Invalid expression",
		"Internal error: %s",
		"Expression expected"
	};
	
	private void errorWithCode(ErrorCode code, int errorStart, int errorEnd, Object... args) throws ParsingException {
		String problem = String.format(errorStrings[code.ordinal()], args);
		createErrorMarker(errorStart, errorEnd, problem);
		throw new ParsingException(problem);
	}
	
	private boolean parseStaticFieldOperator_(int offset) {
		fReader.seek(offset);
		String o = fReader.readString(2);
		if (o.equals("::"))
			return true;
		fReader.seek(offset);
		return false;
	}
	
	private ExprElm parseExpressionWithoutOperators(int offset) throws ParsingException {
		fReader.seek(offset);
		this.eatWhitespace();
		Operator preop = parseOperator_(fReader.offset);
		if (preop != null && preop.isPrefix()) {
			return new ExprUnaryOp(preop, ExprUnaryOp.Placement.Prefix, parseExpressionWithoutOperators(fReader.offset));
		}
		if (parseNumber(fReader.offset)) {
			return new ExprNumber(parsedNumber);
		}
		if (parseID(fReader.offset)) {
			return new ExprID(parsedID);
		}
		if (parseString(fReader.getPosition())) {
			return new ExprString(parsedString);
		}
		Vector<ExprElm> elements = new Vector<ExprElm>(5);
		ExprElm elm;
		ExprElm prevElm = null;
		int sequenceStart = fReader.getPosition();
		boolean dontCheckForPostOp = false;
		do {
			elm = null;
			
			this.eatWhitespace();
			int elmStart = fReader.getPosition();

			// operator always ends a sequence without operators
			if (parseOperator_(fReader.getPosition()) != null) {
				fReader.seek(elmStart);
				break;
			}
			// variable or function
			String word = fReader.readWord();
			if (word != null && word.length() > 0) {
				this.eatWhitespace();
				if (fReader.read() == '(') {
					// function call
					Vector<ExprElm> args = new Vector<ExprElm>();
					boolean expectingComma = false;
					while (!fReader.reachedEOF()) {
						this.eatWhitespace();
						int c = fReader.read();
						if (c == ')') {
							if (!expectingComma)
								args.add(null);
							break;
						} else if (c == ',') {
							if (!expectingComma) {
								args.add(null);
							}
							expectingComma = false;
						} else {
							fReader.unread();
							if (args.size() > 100)
								errorWithCode(ErrorCode.InternalError, fReader.getPosition(), fReader.getPosition(), "Way too much");
							ExprElm arg = parseExpression(fReader.getPosition());
							if (arg == null)
								errorWithCode(ErrorCode.ExpressionExpected, fReader.getPosition(), fReader.getPosition()+1);
							else
								args.add(arg);
							expectingComma = true;
						}
					}
					elm = new ExprCallFunc(word, args.toArray(new ExprElm[0]));
				} else {
					fReader.unread();
					// variable
					elm = new ExprAccessVar(word);
				}
			}
			
			// array
			if (elm == null) {
				int c = fReader.read();
				if (c == '[') {
					if (prevElm != null) {
						// array access
						ExprElm arg = parseExpression(fReader.getPosition());
						this.eatWhitespace();
						if (fReader.read() != ']') {
							errorWithCode(ErrorCode.TokenExpected, fReader.getPosition()-1, fReader.getPosition(), "]");
						}
						elm = new ExprArrayAccess(arg);
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
								arrayElms.add(parseExpression(fReader.getPosition(), COMMA_OR_CLOSE_BRACKET));
								expectingComma = true;
							}
						}
						if (!properlyClosed) {
							errorWithCode(ErrorCode.MissingClosingBracket, fReader.getPosition()-1, fReader.getPosition(), "]");
						}
						elm = new ExprArray(arrayElms.toArray(new ExprElm[0]));
						
					}
				} else { 
					fReader.unread();
				}
			}
		
			// ->
			if (elm == null) {
				if (parseObjectFieldOperator(fReader.getPosition())) {
					elm = new ExprObjectCall(parsedObjectFieldOperator.length() == 3, null);
					eatWhitespace();
					if (parseID(fReader.getPosition())) {
						((ExprObjectCall)elm).setId(parsedID);
						eatWhitespace();
						if (!parseStaticFieldOperator_(fReader.getPosition())) {
							errorWithCode(ErrorCode.TokenExpected, fReader.getPosition(), fReader.getPosition()+2, "::");
						}
					}
				}
			}
			
			// (<expr>)
			if (elm == null) {
				int c = fReader.read();
				if (c == '(') {
					elm = new ExprParenthesized(parseExpression(fReader.getPosition()));
					c = fReader.read();
					if (c != ')')
						errorWithCode(ErrorCode.TokenExpected, fReader.getPosition()-1, fReader.getPosition(), ")");
				} else {
					fReader.unread();
				}
			}
			
			// check if sequence is valid (CreateObject(BLUB)->localvar is not)
			if (elm != null) {
				if (!elm.checkValidInSequence(prevElm)) {
					elm = null; // blub blub <- first blub is var; second blub is not part of the sequence -.-
					fReader.seek(elmStart);
					dontCheckForPostOp = true;
					//errorWithCode(ErrorCode.NotAllowedHere, elmStart, fReader.getPosition(), fReader.readStringAt(elmStart, fReader.getPosition()));
				} else {
					elements.add(elm);
					prevElm = elm;
				}
			}

		} while (elm != null);
		if (elements.size() > 0) {
			ExprElm seq = new ExprSequence(elements.toArray(new ExprElm[0]));
			if (seq.getType() == null) {
				errorWithCode(ErrorCode.InvalidExpression, sequenceStart, fReader.getPosition(), fReader.readStringAt(sequenceStart, fReader.getPosition()));
			}
			if (!dontCheckForPostOp) {
				this.eatWhitespace();
				int saved = fReader.getPosition();
				Operator postop = parseOperator_(fReader.getPosition());
				if (postop != null) {
					if (postop.isPostfix()) {
						return new ExprUnaryOp(postop, ExprUnaryOp.Placement.Postfix, seq);
					} else {
						// a binary operator following this sequence
						fReader.seek(saved);
					}
				}
			}
			return seq;
		} else {
			return null;
		}
	}
	
	private ExprElm parseExpression(int offset, char[] delimiters) throws ParsingException {
		final int START = 0;
		final int OPERATOR = 1;
		final int SECONDOPERAND = 2;
		final int DONE = 3;
		
		ExprElm root = null;
		ExprElm current = null;
		ExprBinaryOp lastOp = null;
		
		// magical thingie to pass all parameters to inherited
		if (parseEllipsis(offset)) {
			return new ExprEllipsis();
		}
		
		fReader.seek(offset);
		for (int state = START; state != DONE;) {
			this.eatWhitespace();
			switch (state) {
			case START:
				root = parseExpressionWithoutOperators(fReader.getPosition());
				current = root;
				state = OPERATOR;
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

					Operator op = parseOperator_(fReader.getPosition());
					if (op != null) {
						int prior = op.priority();
						ExprElm newLeftSide = null;
						if (lastOp == null || prior > lastOp.getOperator().priority() || (prior == lastOp.getOperator().priority() && op.rightAssociative())) {
							newLeftSide = current;
							current = new ExprBinaryOp(op);
							if (newLeftSide.getParent() != null)
								((ExprBinaryOp)newLeftSide.getParent()).setRightSide(current);
							else
								root = current;
							if (lastOp != null)
								lastOp.setRightSide(current);
							lastOp = (ExprBinaryOp)current;
						} else {
							newLeftSide = lastOp;
							lastOp = new ExprBinaryOp(op);
							if (newLeftSide.getParent() != null)
								((ExprBinaryOp)newLeftSide.getParent()).setRightSide(lastOp);
							else
								root = lastOp;
							current = lastOp;
						}
						lastOp.setLeftSide(newLeftSide);
						state = SECONDOPERAND;
					} else
						state = DONE;
				}
				break;
			case SECONDOPERAND:
				ExprElm rightSide = parseExpressionWithoutOperators(fReader.getPosition());
				if (rightSide == null)
					errorWithCode(ErrorCode.InvalidExpression, fReader.getPosition()-1, fReader.getPosition());
				((ExprBinaryOp)current).setRightSide(rightSide);
				lastOp = (ExprBinaryOp)current;
				current = rightSide;
				state = OPERATOR;
				break;
				
			}
		}
		
		return root;
		
	}
	
	private static final char[] SEMICOLON_DELIMITER = new char[] { ';' };
	private static final char[] COMMA_OR_CLOSE_BRACKET = new char[] { ',', ']' };
	
	private ExprElm parseExpression(int offset) throws ParsingException {
		return parseExpression(offset, SEMICOLON_DELIMITER);
	}
	
	/**
	 * Parses [Function description|IMGC|canDisplayMenuItem()]
	 * @param offset
	 * @return
	 * @throws ParsingException
	 */
	private boolean parseFunctionDescription(int offset) throws ParsingException {
		fReader.seek(offset);
		if (fReader.read() != '[') return false;
		String descriptionString = fReader.readStringUntil(new char[] { ']' });
		if (descriptionString == null) {
			fReader.seek(offset);
			return false;
		}
		
		if (fReader.read() == ']') {
			return true;
		}
		else {
			String problem = "Syntax error: expected ']";
			createErrorMarker(fReader.getPosition() -1,fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
//		fReader.readStringUntil(new char[] { '|' }); // description text
//		if (fReader.read() != '|') {
//			fReader.unread();
//			if (fReader.read() == ']') return true;
//			else return false;
//		}
//		int idStart = fReader.getPosition();
//		String id = fReader.readStringUntil(new char[] { '|' }); // description ID for icon
//		if (id != null) { // char not found
//			if (id.length() != 4) {
//				String problem = "Syntax error: expected an ID";
//				createErrorMarker(idStart, idStart + 4, problem);
//				throw new ParsingException(problem);
//			}
//			if (fReader.read() == '|') {
//				parseValue(fReader.getPosition());  // menu item condition
//			}
//		}
//

		// [blublu|IMGC|...]
	}
	
	private boolean parseArray(int offset) throws ParsingException {
		fReader.seek(offset);
		if (fReader.read() != '[') {
			fReader.unread();
			return false;
		}
		int readByte = 0;
		do {
			eatWhitespace();
			if (!parseValue(fReader.getPosition())) {
				String problem = "Syntax error: expected value";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			readByte = fReader.read();
			if (readByte == ']') {
				return true;
			}
			if (readByte != ',') {
				String problem = "Syntax error: expected ',' or ']'";
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		} while(!fReader.reachedEOF());
		return false;
	}
	
	private boolean parseArrayAccess(int offset) throws ParsingException {
		fReader.seek(offset);
		if (fReader.read() != '[') return false;
		eatWhitespace();
		parseValue(fReader.getPosition());
		eatWhitespace();
		if (fReader.read() != ']') {
			String problem = "Syntax error: expected ']'";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
		return true;
	}
	
	private String parsedString;
	
	private boolean parseString(int offset) throws ParsingException {
		fReader.seek(offset);
		if (fReader.read() != '"') {
			fReader.unread();
			return false;
		}
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(fReader.readString(1));
			builder.append(fReader.readStringUntil(new char[] { '"' }));
		} while (builder.length() != 0 && (builder.charAt(builder.length() - 1) == '\\'));
		if (fReader.read() != '"') {
			throw new ParsingException("Internal parsing error.");
		}
		parsedString = builder.toString();
		return true;
	}
	
	private boolean parseKeyword(int offset) throws ParsingException {
		fReader.seek(offset);
		String readWord = fReader.readWord();
		if (readWord.equalsIgnoreCase("if")) {
			if (!readWord.equals(readWord.toLowerCase())) {
				String problem = "Syntax error: you should only use lower case letters in keywords. ('" + readWord.toLowerCase() + "' instead of '" + readWord + "')"; 
				createErrorMarker(fReader.getPosition() - readWord.length(), fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			if (fReader.read() != '(') {
				String problem = "Syntax error: expected '('"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			parseValue(fReader.getPosition()); // if () is valid
			eatWhitespace();
			if (fReader.read() != ')') {
				String problem = "Syntax error: expected ')'"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseCodeSegment(fReader.getPosition())) {
				String problem = "Syntax error: expected a command"; 
				createErrorMarker(offset, offset + 4, problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			offset = fReader.getPosition();
			String nextWord = fReader.readWord();
			if (nextWord.equalsIgnoreCase("else")) {
				if (!nextWord.equals(nextWord.toLowerCase())) {
					String problem = "Syntax error: you should only use lower case letters in keywords. ('" + nextWord.toLowerCase() + "' instead of '" + nextWord + "')"; 
					createErrorMarker(fReader.getPosition() - nextWord.length(), fReader.getPosition(), problem);
					throw new ParsingException(problem);
				}
				eatWhitespace();
				offset = fReader.getPosition();
				if (!parseCodeSegment(fReader.getPosition())) {
					String problem = "Syntax error: expected a command"; 
					createErrorMarker(offset, offset + 4, problem);
					throw new ParsingException(problem);
				}	
			}
			else {
				fReader.seek(offset);
			}
			return true;
		}
		else if (readWord.equalsIgnoreCase("while")) {
			if (!readWord.equals(readWord.toLowerCase())) {
				String problem = "Syntax error: you should only use lower case letters in keywords. ('" + readWord.toLowerCase() + "' instead of '" + readWord + "')"; 
				createErrorMarker(fReader.getPosition() - readWord.length(), fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			if (fReader.read() != '(') {
				String problem = "Syntax error: expected '('"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			parseValue(fReader.getPosition()); // while () is valid
			eatWhitespace();
			if (fReader.read() != ')') {
				String problem = "Syntax error: expected ')'"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseCodeSegment(fReader.getPosition())) {
				String problem = "Syntax error: expected a command"; 
				createErrorMarker(offset, offset + 4, problem);
				throw new ParsingException(problem);
			}
			return true;
		}
		else if (readWord.equalsIgnoreCase("for")) {
			if (!readWord.equals(readWord.toLowerCase())) {
				String problem = "Syntax error: you should only use lower case letters in keywords. ('" + readWord.toLowerCase() + "' instead of '" + readWord + "')"; 
				createErrorMarker(fReader.getPosition() - readWord.length(), fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			if (fReader.read() != '(') {
				String problem = "Syntax error: expected '('"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			
			// initialization
			offset = fReader.getPosition();
			boolean noInitialization = false;
			if (fReader.read() == ';') {
				// any of the for statements is optional
				noInitialization = true;
			} else {
				fReader.unread();
				if (!(parseVarVariableDeclaration(fReader.getPosition()) || parseValue(fReader.getPosition()))) {
					String problem = "Syntax error: expected code"; 
					createErrorMarker(offset, fReader.getPosition(), problem);
					throw new ParsingException(problem);
				}
			}
			
			// determine loop type
			eatWhitespace();
			offset = fReader.getPosition();
			String w;
			if (!noInitialization) {
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
			if (w != null && w.equals("in")) {
				// it's a for (x in array) loop!
				eatWhitespace();
				if (!parseValue(fReader.getPosition())) {
					errorWithCode(ErrorCode.ExpressionExpected, offset, fReader.getPosition()+1);
				}
			} else {
				
				fReader.seek(offset); // if a word !equaling("in") was read

				if (fReader.read() == ';') {
					// any " optional "
					fReader.unread(); // is expected
				} else {
					fReader.unread();
					if (!parseValue(fReader.getPosition())) {
						String problem = "Syntax error: expected condition"; 
						createErrorMarker(offset, fReader.getPosition(), problem);
						throw new ParsingException(problem);
					}
				}
				eatWhitespace();
				offset = fReader.getPosition();
				if (fReader.read() != ';') {
					String problem = "Syntax error: expected ';'"; 
					createErrorMarker(offset, fReader.getPosition(), problem);
					throw new ParsingException(problem);
				}
				eatWhitespace();
				offset = fReader.getPosition();
				if (fReader.read() == ')') {
					// " optional "
					fReader.unread(); // is expected
				} else {
					fReader.unread();
					if (!parseValue(offset)) {
						String problem = "Syntax error: expected call or assignment or identifier"; 
						createErrorMarker(offset, fReader.getPosition()+1, problem);
						throw new ParsingException(problem);
					}
				}
			}
			eatWhitespace();
			if (fReader.read() != ')') {
				String problem = "Syntax error: expected ')'"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseCodeSegment(fReader.getPosition())) {
				String problem = "Syntax error: expected a command"; 
				createErrorMarker(offset, offset + 4, problem);
				throw new ParsingException(problem);
			}
			return true;
		}
		else {
			return false;
		}
		// if (parseValue) { parseCode } else if (parseValue) { parseCode } else { parseCode }
		// while (parseValue) { parseCode }
		// for ( ; ; ) { parseCode } // that is special
	}
	
	private C4ID parsedID;
	
	private boolean parseID(int offset) throws ParsingException {
		fReader.seek(offset);
		String word = fReader.readWord();
		if (word != null && word.length() != 4) {
			fReader.seek(offset);
			return false;
		}
		for(int i = 0; i < 4;i++) {
			int readChar = word.charAt(i);
			if (('A' <= readChar && readChar <= 'Z') ||
					('0' <= readChar && readChar <= '9') ||
					(readChar == '_')) {
				continue;
			}
			else {
				fReader.seek(offset);
				return false;
			}
		}
		parsedID = C4ID.getID(word);
		offset = fReader.getPosition();
		if (parseObjectCall(fReader.getPosition())) offset = fReader.getPosition();
		fReader.seek(offset);
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
		C4Type type = C4Type.UNKNOWN;
		C4Variable var = new C4Variable();
		if (firstWord.equals("&")) {
			type = C4Type.REFERENCE;
		}
		else {
			type = C4Type.makeType(firstWord);
		}
		var.setType(type);
		if (type == C4Type.UNKNOWN) {
			var.setName(firstWord);
		}
		else {
			eatWhitespace();
			s = fReader.getPosition();
			String secondWord = fReader.readWord();
			e = fReader.getPosition();
			if (secondWord.length() > 0) {
				var.setName(secondWord);
			}
			else {
				String problem = "Syntax error: variable name expected, '" + firstWord + "' is a type not a variable name"; 
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		}
		var.setLocation(new SourceLocation(s, e));
		function.getParameter().add(var);
		return true;
	}
	
	protected void eatWhitespace(int offset) {
		fReader.seek(offset);
		while ((fReader.eatWhitespace() > 0 || parseComment(fReader.getPosition())));
	}
	
	protected void eatWhitespace() {
		eatWhitespace(fReader.getPosition());
	}
	
	protected boolean parseComment(int offset) {
		fReader.seek(offset);
		String sequence = fReader.readString(2);
		if (sequence == null) {
			return false;
		}
		else if (sequence.equals("//")) {
			fReader.moveUntil(BufferedScanner.NEWLINE_DELIMITERS);
			fReader.eat(BufferedScanner.NEWLINE_DELIMITERS);
			return true;
		}
		else if (sequence.equals("/*")) {
			while (!fReader.reachedEOF()) {
				if (fReader.read() == '*') {
					if (fReader.read() == '/') {
						return true; // genug gefressen
					}
					else {
						fReader.unread();
					}
				}
			}
			return true;
		}
		else {
			fReader.move(-2);
			return false;
		}
	}
	
//	noticed those methods too late ^^;
//	protected void eatWhitespace() {
//		fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
//		while (parseComment(fReader.getPosition()))
//			fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
//	}
//	
//	protected void eatWhitespace(int offset) {
//		fReader.seek(offset);
//		fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
//		while (parseComment(fReader.getPosition()))
//			fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
//	}
	
	private IMarker createErrorMarker(int start, int end, String message) {
		if (fScript == null) return null;
		try {
			IMarker marker = fScript.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
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
	
	private IMarker createWarningMarker(int start, int end, String message) {
		if (fScript == null) return null;
		try {
			IMarker marker = fScript.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
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

//	/**
//	 * @return the directives
//	 */
//	public List<C4Directive> getDirectives() {
//		return directives;
//	}
//
//	/**
//	 * @return the functions
//	 */
//	public List<C4Function> getFunctions() {
//		return functions;
//	}
//
//	/**
//	 * @return the variables
//	 */
//	public List<C4Variable> getVariables() {
//		return variables;
//	}
	
//	public boolean visit(IResourceDelta delta) throws CoreException {
//		if (delta == null) 
//			return false;
//		
//		IResourceDelta[] deltas = delta.getAffectedChildren();
//		IResource res = delta.getResource();
//		int flags = delta.getFlags();
//		int kind = delta.getKind();
//		if (delta.getResource() instanceof IFile)
//			try {
//				C4ScriptParser parser = new C4ScriptParser((IFile) delta.getResource());
//				parser.parse();
//			} catch (CompilerException e) {
//				e.printStackTrace();
//			}
//		if (delta.getResource() instanceof IFolder || delta.getResource() instanceof IProject)
//			return true;
//		else
//			return false;
//	}
//
//	public boolean visit(IResource resource) throws CoreException {
//		if (resource == null)
//			return false;
//		if (resource instanceof IFile) {
//			if (resource.getName().endsWith(".c")) {
//				try {
//					C4ScriptParser parser = new C4ScriptParser((IFile) resource);
//					parser.parse();
//				} catch (CompilerException e) {
//					e.printStackTrace();
//				}
//			}
//			return false;
//		}
//		if (resource instanceof IContainer) return true;
//		else return false;
//	}
}
