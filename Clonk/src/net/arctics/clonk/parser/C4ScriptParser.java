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
import org.eclipse.core.resources.IMarker;
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
		parseCode(fReader.getPosition());
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
	
	private boolean parseCode(int offset) {
		// a complete code block without reading { }
		// function call
		// zuweisung
		// post-prefix
		return false;
	}
	
	private boolean parseCall(int offset) {
		// parse all parameter with parseValue
		// do type checking where possible
		return false;
	}
	
	private boolean parseValue(int offset) {
		// recursive
		// calls parseCall
		// calls parseString
		// calls parseVariable
		// calls parseOperator
		return false;
	}
	
	private boolean parseVariable(int offset) {
		// iVar
		// think of post- and prefixes: iVar++
		return false;
	}
	
	private boolean parseOperator(int offset) {
		// + - S= * % ... all operators that combines 2 values to 1
		// not: =
		// think of: return(-iVar);
		return false;
	}
	
	private boolean parseFunctionDescription(int offset) {
		// [blublu|IMGC|...]
		return false;
	}
	
	private boolean parseString(int offset) {
		// "hallo fritz */*/* "
		return false;
	}
	
	private boolean parseKeyword(int offset) {
		// if (parseValue) { parseCode } else if (parseValue) { parseCode } else { parseCode }
		// while (parseValue) { parseCode }
		// for ( ; ; ) { parseCode } // that is special
		return false;
	}
	
	private boolean parseID(int offset) {
		// CLNK
		return false;
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
			eatWhitespace(fReader.getPosition());
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
		if (parseComment(fReader.getPosition()))
			fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
	}
	
	protected void eatWhitespace(int offset) {
		fReader.seek(offset);
		fReader.eat(BufferedScanner.WHITESPACE_DELIMITERS);
		if (parseComment(fReader.getPosition()))
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
}
