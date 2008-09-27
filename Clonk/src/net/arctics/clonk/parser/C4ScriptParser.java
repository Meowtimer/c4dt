package net.arctics.clonk.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.parser.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class C4ScriptParser implements IResourceDeltaVisitor {
	
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
		
		public int read() {
			if (offset >= size) return -1;
			return buffer[offset++];
		}
		
		public void unread() {
			offset--;
		}
		
	    public String readString(int length) {
	    	if (offset+length >= size) return null;
	    	try {
				return new String(buffer,offset,length,"ISO-8859-1");
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
	    				(0x30 < readByte && readByte < 0x39) ||
	    				(0x41 < readByte && readByte < 0x5a) ||
	    				(0x61 < readByte && readByte < 0x7a) ||
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
	    		for(int i = 0; i < delimiters.length;i++) {
	    			if (readByte != delimiters[i]) {
	    				unread();
	    				return;
	    			}
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
	private IFile fScript;
	
	private List<C4Directive> directives = new LinkedList<C4Directive>();
	private List<C4Function> functions = new LinkedList<C4Function>();
	private List<C4Variable> variables = new LinkedList<C4Variable>();
	
	public C4ScriptParser(IFile script) throws CompilerException {
		fScript = script;
		fReader = new BufferedScanner(fScript);
	}
	
	public C4ScriptParser() {
		
	}
	
	public void parse() {
		int offset = 0;
		try {
			while(!fReader.reachedEOF()) {
				if (parseDeclaration(offset)) offset = fReader.getPosition();
				eatWhitespace(offset); // this eats comments too
				offset = fReader.getPosition();
//				if (parseComment(offset)) offset = fReader.getPosition();
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
				directives.add(new C4Directive(type, content));
				return true;
			}
		}
		else {
			fReader.seek(offset);
			String word = fReader.readWord();
			if (word.equalsIgnoreCase("public") || word.equalsIgnoreCase("protected") || word.equalsIgnoreCase("private") || word.equalsIgnoreCase("global")) {
				if (parseFunctionDeclaration(word, offset)) return true;
			}
			else if (word.equalsIgnoreCase("static") || word.equalsIgnoreCase("local")) {
				if (parseVariableDeclaration(offset)) return true;
			}
		}
		return false;
	}
	
	private boolean parseVariableDeclaration(int offset) {
		// local iVar, iX;
		// static pObj = parseValue;
		return false;
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
		C4Function func = new C4Function();
		func.setVisibility(C4FunctionScope.makeScope(firstWord));
		if (!fReader.readWord().equalsIgnoreCase("func")) {
			String problem = "Syntax error: expected 'func'";
			createErrorMarker(offset, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
		eatWhitespace();
		// get function name
		int funcNameStart = fReader.getPosition();
		String funcName = fReader.readWord();
		for(C4Function otherFunc : functions) {
			if (otherFunc.getName().equalsIgnoreCase(funcName)) {
				createWarningMarker(funcNameStart, fReader.getPosition(), "Function overload: this function is already declared in this script");
				break;
			}
		}
		func.setName(funcName);
		eatWhitespace();
		if (fReader.read() != '(') {
			String problem = "Syntax error: expected '('";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		}
		// get parameter
		do {
			eatWhitespace();
			if (!parseParameter(fReader.getPosition(), func)) break;
			eatWhitespace();
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
		functions.add(func);
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
		
		if (parseCall(offset) || parseAssignment(offset) || parseVariable(offset)) {
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
		return false;
	}
	
	/**
	 * Parses { parseCodeBlock } or parseCode
	 * @param offset
	 * @return
	 * @throws ParsingException 
	 */
	private boolean parseCodeSegment(int offset) throws ParsingException {
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
		eatWhitespace();
		String varName = fReader.readWord();
		if (varName.length() == 0) {
//			fReader.seek(offset);
			return false;
		}
		eatWhitespace();
		if (fReader.read() != '=') return false;
		eatWhitespace();
		offset = fReader.getPosition();
		if (!parseValue(fReader.getPosition())) {
			String problem = "Syntax error: expected a value after '='";
			createErrorMarker(offset, offset + 3, problem);
			throw new ParsingException(problem);
		}
		return true;
	}
	
	private boolean parseCall(int offset) throws ParsingException {
		fReader.seek(offset);
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
				if (parseValue(offset)) {
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
						createErrorMarker(fReader.getPosition() - 1, fReader.getPosition() + 1, problem);
						throw new ParsingException(problem);
					}
				}
				else {
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
				parseObjectCall(offset);
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
				parseCall(offset) ||
				parseString(offset) ||
				parseID(offset) ||
				parseVariable(offset) ||
				parseArray(offset))
		{
			fReader.seek(offset);
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
		if (fReader.read() != '(') return false;
		eatWhitespace();
		parseValue(fReader.getPosition());
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
			if (0x30 < readByte && readByte < 0x39) {
				count++;
				continue;
			}
			else if ((0x41 < readByte && readByte < 0x5a) || (0x61 < readByte && readByte < 0x7a)) {
				if (count == 0) return false; // well, this seems not to be a number at all
				String problem = "Syntax error: erroneous Ident";
				createErrorMarker(offset, fReader.getPosition(), problem);
				throw new ParsingException(problem);
			}
			else {
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
		
		parsePrefixOperator(offset);
		eatWhitespace();
		String varName = fReader.readWord();
		if (varName.length() == 0) {
//			fReader.seek(offset);
			return false;
		}
		offset = fReader.getPosition();
		eatWhitespace();
		if (parseArrayAccess(offset)) offset = fReader.getPosition();
		eatWhitespace(offset);
		if (!parsePostfixOperator(offset)) // entweder
			parseObjectCall(offset);       // oder
				
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
			return false;
		}
	}
	
	private boolean parseOperator(int offset) {
		
		// + - S= * % ... all operators that combines 2 values to 1
		// not: =
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
		if (fReader.read() != '[') return false;
		int readByte = 0;
		do {
			eatWhitespace();
			parseValue(fReader.getPosition());
			eatWhitespace();
			readByte = fReader.read();
			if (readByte == ']') {
				return true;
			}
			if (readByte == ',') {
				continue;
			}
			String problem = "Syntax error: expected ',' or ']'";
			createErrorMarker(fReader.getPosition() - 1, fReader.getPosition(), problem);
			throw new ParsingException(problem);
		} while(!fReader.reachedEOF());
		return true;
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
		if (fReader.read() != '"') return false;
		StringBuilder builder = new StringBuilder();
		do {
			if (builder.length() > 0) builder.append(fReader.readString(1));
			builder.append(fReader.readStringUntil(new char[] { '"' }));
		} while ((builder.charAt(builder.length() - 1) == '\\'));
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
			if (!parseCode(fReader.getPosition())) {
				// throw? find ;?
			}
			eatWhitespace();
			if (!parseValue(fReader.getPosition())) {
				// throw?
			}
			eatWhitespace();
			if (fReader.read() != ';') {
				// throw
			}
			eatWhitespace();
			if (!parseCode(fReader.getPosition())) {
				// throw?
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
			if ((0x41 < i && i < 0x5a) ||
					(0x30 < i && i < 0x39) ||
					(i == '_')) {
				continue;
			}
			else {
				return false;
			}
		}
		parseObjectCall(fReader.getPosition());
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
		String sequence = fReader.readString(2);
		if (sequence.equals("//")) {
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
		try {
			IMarker marker = fScript.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.TRANSIENT, true);
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
		try {
			IMarker marker = fScript.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			marker.setAttribute(IMarker.TRANSIENT, true);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.CHAR_START, start);
			marker.setAttribute(IMarker.CHAR_END, end);
			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the directives
	 */
	public List<C4Directive> getDirectives() {
		return directives;
	}

	/**
	 * @return the functions
	 */
	public List<C4Function> getFunctions() {
		return functions;
	}

	/**
	 * @return the variables
	 */
	public List<C4Variable> getVariables() {
		return variables;
	}
	
	@SuppressWarnings("unused")
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) 
			return false;
		
		IResourceDelta[] deltas = delta.getAffectedChildren();
		IResource res = delta.getResource();
		int flags = delta.getFlags();
		int kind = delta.getKind();
		if (delta.getResource() instanceof IFile)
			try {
				new C4ScriptParser((IFile) delta.getResource());
			} catch (CompilerException e) {
				e.printStackTrace();
			}
		if (delta.getResource() instanceof IFolder || delta.getResource() instanceof IProject)
			return true;
		else
			return false;
	}
}
