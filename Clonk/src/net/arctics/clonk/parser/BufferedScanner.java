/**
 * 
 */
package net.arctics.clonk.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class BufferedScanner {

	public static final char[] WHITESPACE_DELIMITERS = new char[] { ' ', '\n', '\r', '\t' };
	public static final char[] NEWLINE_DELIMITERS = new char[] { '\n', '\r' };

	private String buffer;
	private int size;
	private int offset;

	private static String stringFromInputStream(InputStream stream) throws IOException {
		InputStreamReader inputStreamReader = new InputStreamReader(stream);
		StringBuilder stringBuilder;
		try {
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			try {
				stringBuilder = new StringBuilder();
				char[] buffer = new char[1024];
				int read;
				while ((read = bufferedReader.read(buffer)) > 0) {
					stringBuilder.append(buffer, 0, read);
				}

			} finally {
				bufferedReader.close();
			}
		} finally {
			inputStreamReader.close();
		}
		return stringBuilder.toString();
	}

	public BufferedScanner(IFile file) {
		try {
			InputStream contents = file.getContents();
			try {
				offset = 0;
				buffer = stringFromInputStream(contents);
				size = buffer.length();
			} finally {
				contents.close();
			}
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BufferedScanner(InputStream stream, long fileSize) {
		try {
			offset = 0;
			buffer = stringFromInputStream(stream);
			size = buffer.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public BufferedScanner(String withString) {
		offset = 0;
		buffer = withString;
		size = buffer.length();
	}

	public int read() {
		if (offset >= size) {
			offset++; // increment anyway so unread works as expected
			return -1;
		}
		return buffer.charAt(offset++);
	}

	public boolean unread() {
		offset--;
		return true;
	}

	public String readString(int length) {
		if (offset+length > size) 
			return null;
		String result = buffer.substring(offset, offset+length);
		offset += length;
		return result;
	}

	public static boolean isWordPart(int character) {
		return ('A' <= character && character <= 'Z') ||
		('a'<= character && character <= 'z') ||
		(character == '_') ||
		(/*length > 0 &&*/ '0' <= character && character <= '9');
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
		String line = readStringUntil(NEWLINE_DELIMITERS);
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
	 * Moves offset until any other char than <code>delimiters</code> occurs
	 * @param delimiters
	 */
	public int eat(char[] delimiters) {
		if (reachedEOF())
			return 0; // no unreading() when already reached EOF
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

	public String readStringAt(int start, int end) {
		int p = getPosition();
		seek(start);
		String result = readString(end-start);
		seek(p);
		return result;
	}

	public int peekAfterWhitespace() {
		int pos = offset;
		eatWhitespace();
		int result = read();
		seek(pos);
		return result;
	}
	
	public static boolean isLineDelimiterChar(char c) {
		for (int i = 0; i < NEWLINE_DELIMITERS.length; i++)
			if (NEWLINE_DELIMITERS[i] == c)
				return true;
		return false;
	}
	
	public static boolean isWhiteSpaceButNotLineDelimiterChar(char c) {
		return c == ' '	|| c == '\t';
	}

	public String getLineAt(IRegion region) {
		IRegion lineRegion = getLineRegion(region);
		return buffer.substring(lineRegion.getOffset(), lineRegion.getOffset()+lineRegion.getLength());
	}
	
	public IRegion getLineRegion(IRegion regionInLine) {
		int start, end;
		for (start = regionInLine.getOffset(); start > 0 && !isLineDelimiterChar(buffer.charAt(start-1)); start--);
		for (end = regionInLine.getOffset()+regionInLine.getLength(); end < buffer.length()-1 && !isLineDelimiterChar(buffer.charAt(end+1)); end++);
		return new Region(start, end-start);
	}

	public int getBufferLength() {
		return buffer.length();
	}
}