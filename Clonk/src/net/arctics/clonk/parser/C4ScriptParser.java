package net.arctics.clonk.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import net.arctics.clonk.parser.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class C4ScriptParser {
	
	protected static class BufferedScanner {
		
		public static final char[] WHITESPACE_DELIMITERS = new char[] { ' ', '	', '\n', '\r' };
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
	    				(0x30 <= readByte && readByte <= 0x39) ||
	    				(0x41 <= readByte && readByte <= 0x5a) ||
	    				(0x61 <= readByte && readByte <= 0x7a) ||
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
	    public void eat(char[] delimiters) {
	    	do {
	    		int readByte = read();
	    		boolean isDelimiter = true;
	    		for(int i = 0; i < delimiters.length;i++) {
	    			if (readByte != delimiters[i]) {
	    				isDelimiter = false;
	    			}
	    			else {
	    				isDelimiter = true;
	    				break;
	    			}
	    		}
	    		if (!isDelimiter) {
	    			unread();
	    			return;
	    		}
	    	} while(!reachedEOF());
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
				String varName = fReader.readWord();
				if (varName.equals("const")) {
					eatWhitespace();
					String constName = fReader.readWord();
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
					var.setObject(container);
//					var.setType(C4Type.)
					container.definedVariables.add(var);
				}
				else {
					container.definedVariables.add(new C4Variable(varName,C4VariableScope.VAR_STATIC));
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
				String varName = fReader.readWord();
				container.definedVariables.add(new C4Variable(varName,C4VariableScope.VAR_LOCAL));
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
		if (word.equalsIgnoreCase("var")) {
			if (!word.equals("var")) {
				String problem = "Syntax error: write '" + word.toLowerCase() + "' instead of '" + word + "'";
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
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
		// get function name
		int funcNameStart = fReader.getPosition();
		String funcName = fReader.readWord();
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
		eatWhitespace();
		offset = fReader.getPosition();
		if (parseFunctionDescription(offset)) offset = fReader.getPosition();
		parseCodeBlock(offset);
		eatWhitespace();
		if (fReader.read() != '}') {
			String problem = "Syntax error: expected '}'";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
		// finish up
		container.definedFunctions.add(activeFunc);
//		functions.add(func);
		return false;
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
		if (parseKeyword(offset)) return true;
		if (parseReturn(offset) || parseVarVariableDeclaration(offset) || parseCall(offset) || parseAssignment(offset) || parseVariable(offset)) {
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
	
	/**
	 * Parses { parseCodeBlock } or parseCode
	 * @param offset
	 * @return
	 * @throws ParsingException 
	 */
	protected boolean parseCodeSegment(int offset) throws ParsingException {
		if (fReader.read() == '{') { // if has block
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseCodeBlock(fReader.getPosition())) fReader.seek(offset);
			eatWhitespace();
			if (fReader.read() != '}') {
				String problem = "Syntax error: expected '}', code blocks have to be closed by '}'"; 
				createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			return true;
		}
		else {
			eatWhitespace();
			if (parseCode(offset)) return true;
			else return false;
		}
	}
	
	private boolean parseAssignment(int offset) throws ParsingException {
		fReader.seek(offset);
		eatWhitespace();
		String varName = fReader.readWord();
		if (varName.length() == 0) {
//			fReader.seek(offset);
			return false;
		}
		eatWhitespace();
		int readByte = fReader.read();
		if (readByte != '=') {
			int secondByte = fReader.read();
			if (secondByte == '=' && ((0x2A <= readByte && readByte <= 0x2F && readByte != 0x2C && readByte != 0x2E) || readByte == '%')) {
				
			}
			else {
				fReader.unread();
				fReader.unread();
				return false;
			}
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
		if (
				parseParenthesisGroup(offset) ||
				parseNumber(offset) ||
				parseAssignment(offset) ||
				parseCall(offset) ||
				parseString(offset) ||
				parseID(offset) ||
				parseVariable(offset) ||
				parseArray(offset))
		{
//			fReader.seek(offset);
			eatWhitespace();
			offset = fReader.getPosition();
			if (parseOperator(offset)) {
				eatWhitespace();
				offset = fReader.getPosition();
				if (parseValue(offset)) {
					return true;
				}
				else {
					String problem = "Syntax error: expected an expression after operator";
					createErrorMarker(fReader.getPosition() - 1, fReader.getPosition() + 1, problem);
					throw new ParsingException(problem);
				}
			}
			else {
				fReader.seek(offset);
				return true; // this seems to be the last sub value in the parent value
			}
		}
		else {
			return false; // there is no value at the specified offset
		}
		
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
	
	private boolean parseNumber(int offset) throws ParsingException {
		fReader.seek(offset);
		int count = 0;
		do {
			int readByte = fReader.read();
			if (0x30 <= readByte && readByte <= 0x39) {
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
				if (count == 0) return false; // well, this seems not to be a number at all
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
	
	private boolean parseObjectFieldOperator(int offset) {
		fReader.seek(offset);
		if (fReader.read() == '-') {
			if (fReader.read() == '>') {
				if (fReader.read() != '~') {
					fReader.unread();
				}
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
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
	 * Parses [Function description|IMGC|canDisplayMenuItem()]
	 * @param offset
	 * @return
	 * @throws ParsingException
	 */
	private boolean parseFunctionDescription(int offset) throws ParsingException {
		fReader.seek(offset);
		if (fReader.read() != '[') return false;
		fReader.readStringUntil(new char[] { '|' }); // description text
		if (fReader.read() != '|') {
			fReader.unread();
			if (fReader.read() == ']') return true;
			else return false;
		}
		int idStart = fReader.getPosition();
		String id = fReader.readStringUntil(new char[] { '|' }); // description ID for icon
		if (id.length() != 4) {
			String problem = "Syntax error: expected an ID";
			createErrorMarker(idStart, idStart + 4, problem);
			throw new ParsingException(problem);
		}
		if (fReader.read() == '|') {
			parseValue(fReader.getPosition());  // menu item condition
		}
		if (fReader.read() == ']') {
			return true;
		}
		else {
			String problem = "Syntax error: expected ']";
			createErrorMarker(fReader.getPosition() -1,fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
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
			offset = fReader.getPosition();
			if (!parseCode(fReader.getPosition())) {
				String problem = "Syntax error: expected code"; 
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			eatWhitespace();
			offset = fReader.getPosition();
			if (!parseValue(fReader.getPosition())) {
				String problem = "Syntax error: expected condition"; 
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
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
			if (!parseCall(offset) && !parseAssignment(offset) && !parseVariable(offset)) {
				String problem = "Syntax error: expected call or assignment or identifier"; 
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
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
	
	private boolean parseID(int offset) throws ParsingException {
		fReader.seek(offset);
		for(int i = 0; i < 4;i++) {
			int readChar = fReader.read();
			if ((0x41 <= readChar && readChar <= 0x5a) ||
					(0x30 <= readChar && readChar <= 0x39) ||
					(readChar == '_')) {
				continue;
			}
			else {
				fReader.unread();
				return false;
			}
		}
		offset = fReader.getPosition();
		if (parseObjectCall(fReader.getPosition())) offset = fReader.getPosition();
		fReader.seek(offset);
		return true;
	}

	private boolean parseParameter(int offset, C4Function function) throws ParsingException {
		fReader.seek(offset);
		String firstWord = fReader.readWord();
		if (firstWord.length() == 0) return false;
		C4Type type = C4Type.UNKNOWN;
		C4Variable var = new C4Variable();
		if (firstWord.startsWith("&")) {
			type = C4Type.REFERENCE;
		}
		else {
			type = C4Type.makeType(firstWord);
		}
		var.setType(type);
		if (type == C4Type.UNKNOWN) {
			var.setName(firstWord);
		}
		else if (type == C4Type.REFERENCE) {
			var.setName(firstWord.substring(1));
		}
		else {
			eatWhitespace();
			String secondWord = fReader.readWord();
			if (secondWord.length() > 0) {
				var.setName(secondWord);
			}
			else {
				String problem = "Syntax error: variable name expected, '" + firstWord + "' is a type not a variable name"; 
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
		}
		function.getParameter().add(var);
		return true;
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
	
	protected void eatWhitespace() {
		fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
		while (parseComment(fReader.getPosition()))
			fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
	}
	
	protected void eatWhitespace(int offset) {
		fReader.seek(offset);
		fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
		while (parseComment(fReader.getPosition()))
			fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
	}
	
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
