package net.arctics.clonk.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Scanner operating on a string stored in memory. Can be created from a file, an input stream or a raw string
 */
public class BufferedScanner {

	/**
	 * characters that represent whitespace
	 */
	public static final char[] WHITESPACE_CHARS = new char[] { ' ', '\n', '\r', '\t' };
	
	/**
	 * characters that represent a new-line
	 */
	public static final char[] NEWLINE_CHARS = new char[] { '\n', '\r' };
	
	/**
	 * whitespace chars without new line chars
	 */
	public static final char[] WHITESPACE_WITHOUT_NEWLINE_CHARS = new char[] { ' ', '\t' };

	/**
	 * The buffer
	 */
	private String buffer;
	
	/**
	 * Size of the buffer
	 */
	private int size;
	
	/**
	 * Current offset
	 */
	private int offset;

	/**
	 * Create a new scanner that scans the contents of a text file
	 * @param file the text file
	 */
	public BufferedScanner(IFile file) {
		try {
			offset = 0;
			buffer = Utilities.stringFromFileDocument(file);
			size = buffer.length();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BufferedScanner(Reader reader) throws IOException {
		this(Utilities.stringFromReader(reader));
	}
	
	/**
	 * Create a new scanner that scans the contents of a stream
	 * @param stream the input stream
	 */
	public BufferedScanner(InputStream stream) {
		try {
			offset = 0;
			buffer = Utilities.stringFromInputStream(stream);
			size = buffer.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a new scanner that scans a string
	 * @param withString
	 */
	public BufferedScanner(String withString) {
		offset = 0;
		buffer = withString;
		size = buffer.length();
	}

	/**
	 * Read the next character in the buffer
	 * @return the character (can be cast to char) or -1 if the current offset exceeds the size of the buffer
	 */
	public int read() {
		if (offset >= size) {
			offset++; // increment anyway so unread works as expected
			return -1;
		}
		return buffer.charAt(offset++);
	}

	/**
	 * Reverts the last read() call
	 * @return
	 */
	public boolean unread() {
		offset--;
		return true;
	}

	/**
	 * Reads a string of the supplied length from the buffer
	 * @param length the length
	 * @return the read string
	 */
	public String readString(int length) {
		if (offset+length > size) 
			return null;
		String result = buffer.substring(offset, offset+length);
		offset += length;
		return result;
	}

	/**
	 * Returns whether character is part of a regular identifier (e.g. is on of the characters contained in 'A'..'Z', 'a'..'z', '_', '0'..'9')
	 * @param character the character
	 * @return
	 */
	public static boolean isWordPart(int character) {
		return ('A' <= character && character <= 'Z') ||
		('a'<= character && character <= 'z') ||
		(character == '_') ||
		(/*length > 0 &&*/ '0' <= character && character <= '9');
	}
	
	public static boolean isUmlaut(char character) {
		character = Character.toLowerCase(character);
	    return character == 'ß' || character == 'ä' || character == 'ü' || character == 'ö';
    }
	
	public static boolean isWordStart(int character) {
		return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || character == '_';
	}
	
	/**
	 * Reads a code-word. (like regexp class [0-9a-zA-Z_])
	 * @return the code-word
	 */
	public String readIdent() {
		int start = offset;
		int length = 0;
		do {
			int readByte = read();
			if (isWordPart(readByte)) {
				length++;
			}
			else {
				seek(start);
				return readString(length);
			}
		} while(!reachedEOF());
		return readStringAt(start, start+length);
	}

	/**
	 * Reads a string until a char from <code>delimiters</code> occurs
	 * @param delimiters
	 * @return string sequence, without delimiter char
	 */
	public String readStringUntil(char ...delimiters) {
		int start = offset;
		int i = 0;
		int subtract = 0;
		Outer: do {
			int readByte = read();
			for(i = 0; i < delimiters.length;i++) {
				if (readByte == delimiters[i]) {
					subtract = 1;
					break Outer;
				}
			}
		} while(!reachedEOF());
		i = offset - start - subtract; // variable reuse
		seek(start);
		return readString(i);
	}
	
	/**
	 * Reads a string until a newline character occurs
	 * Cursor is after newline char(s)
	 * @return the line without newline char(s)
	 */
	public String readLine() {
		int start = offset;
		String line = readStringUntil(NEWLINE_CHARS);
		if (line == null) {
			return readStringAt(start, offset);
		}
		if (read() == '\r') {
			if (read() != '\n')
				unread();
			return line;
		}
		else {
			unread();
			if (read() == '\n') {
				if (read() != '\r')
					unread();
				return line;
			}
		}
		return line;
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
	 * Moves offset until any other char than <code>charsToEat</code> occurs
	 * @param charsToEat
	 */
	public int eat(char[] charsToEat) {
		if (reachedEOF())
			return 0; // no unreading() when already reached EOF
		int result = 0;
		do {
			int readByte = read();
			boolean doEat = false;
			for (int i = 0; i < charsToEat.length;i++) {
				if (readByte == charsToEat[i]) {
					doEat = true;
					result++;
					break;
				}
			}
			if (!doEat) {
				unread();
				return result;
			}
		} while(!reachedEOF());
		return result;
	}
	
	public int eatUntil(char ...delimiters) {
		if (reachedEOF())
			return 0; // no unreading() when already reached EOF
		int result = 0;
		do {
			int readByte = read();
			boolean isDelimiter = false;
			for (int i = 0; i < delimiters.length;i++) {
				if (readByte == delimiters[i]) {
					isDelimiter = true;
					result++;
					break;
				}
			}
			if (isDelimiter) {
				unread();
				return result;
			}
		} while(!reachedEOF());
		return result;
	}

	/**
	 * Eats all characters that are considered whitespace
	 * @return the amount of whitespace eaten
	 */
	public int eatWhitespace() {
		return eat(WHITESPACE_CHARS);
	}
	
	public static int getTabIndentation(String s, int pos) {
		int tabs = 0;
		for (--pos; pos >= 0 && !isLineDelimiterChar(s.charAt(pos)); pos--) {
			if (s.charAt(pos) == '\t') {
				tabs++;
			} else {
				tabs = 0; // don't count tabs not at the start of the line
			}
		}
		return tabs;
	}
	
	public int getTabIndentation() {
		return getTabIndentation(buffer, getPosition());
	}

	/**
	 * Absolute offset manipulation
	 * @param newPos
	 * @return new offset
	 */
	public int seek(int newPos) {
		offset = newPos;
		//if (offset >= size) offset = size - 1;
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
		return (offset >= size);
	}

	/**
	 * Current offset
	 * @return offset
	 */
	public int getPosition() {
		return offset;
	}

	/**
	 * Reads a string at specified offset. The current offset of the scanner is not modified by this method
	 * @param start start offset of string
	 * @param end end offset of string
	 * @return the read string
	 */
	public String readStringAt(int start, int end) {
		if (start == end)
			return ""; //$NON-NLS-1$
		int p = getPosition();
		seek(start);
		String result = readString(end-start);
		seek(p);
		return result;
	}

	/**
	 * Returns the first character from the current offset that is not whitespace. This method does not alter the current offset
	 * @return
	 */
	public int peekAfterWhitespace() {
		int pos = offset;
		eatWhitespace();
		int result = read();
		seek(pos);
		return result;
	}
	
	/**
	 * Returns whether c is a line delimiter char
	 * @param c the char
	 * @return true if it is one, false if not
	 */
	public static boolean isLineDelimiterChar(char c) {
		for (int i = 0; i < NEWLINE_CHARS.length; i++)
			if (NEWLINE_CHARS[i] == c)
				return true;
		return false;
	}
	
	/**
	 * Returns whether is whitespace but not a line delimiter (' ', '\t')
	 * @param c the character
	 * @return see above
	 */
	public static boolean isWhiteSpaceButNotLineDelimiterChar(char c) {
		return c == ' '	|| c == '\t';
	}
	
	public static boolean isWhiteSpace(char c) {
		return c == '\n' || c == '\r' || isWhiteSpaceButNotLineDelimiterChar(c);
	}

	/**
	 * Returns the line region is contained in as a string
	 * @param region the region
	 * @return the line string
	 */
	public String getLineAt(IRegion region) {
		IRegion lineRegion = getLineRegion(region);
		return buffer.substring(lineRegion.getOffset(), lineRegion.getOffset()+lineRegion.getLength());
	}
	
	/**
	 * Returns the line region is contained in as a region
	 * @param text the string to look for the line in
	 * @param regionInLine the region
	 * @return the line region
	 */
	public static IRegion getLineRegion(String text, IRegion regionInLine) {
		int start, end;
		for (start = regionInLine.getOffset(); start > 0 && !isLineDelimiterChar(text.charAt(start-1)); start--);
		for (end = regionInLine.getOffset()+regionInLine.getLength(); end < text.length()-1 && !isLineDelimiterChar(text.charAt(end+1)); end++);
		return new Region(start, end-start);
	}
	
	public IRegion getLineRegion(IRegion regionInLine) {
		return getLineRegion(this.buffer, regionInLine);
	}

	/**
	 * returns the length of the buffer 
	 * @return the buffer length
	 */
	public int getBufferLength() {
		return buffer.length();
	}
	
	/**
	 * Return the buffer the scanner operates on
	 * @return the buffer
	 */
	public CharSequence getBuffer() {
		return buffer;
	}
	
	@Override
	public String toString() {
		return "offset: " + getPosition() + "; next: " + (char)peek(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Return the next-to-be-read char without modifying the scanner position
	 * @return the next char
	 */
	public int peek() {
		int p = read();
		unread();
		return p;
	}
	
	public String stringAtRegion(IRegion region) {
		return buffer.substring(region.getOffset(), region.getOffset()+region.getLength());
	}
	
}