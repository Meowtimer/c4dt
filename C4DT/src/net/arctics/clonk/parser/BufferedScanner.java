package net.arctics.clonk.parser;

import java.io.CharArrayReader;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.ICharacterScanner;

/**
 * Scanner operating on a string stored in memory. Can be created from a file, an input stream or a raw string
 */
public class BufferedScanner implements ICharacterScanner {

	public static final byte TABINDENTATIONMODE = -1;
	public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");
	public static final Pattern NUMERAL_PATTERN = Pattern.compile("[0-9]+");

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
	protected char[] buffer;

	/**
	 * Size of the buffer
	 */
	protected int size;

	/**
	 * Current offset
	 */
	protected int offset;

	/**
	 * Indentation mode: -1 for Tab, number for number of whitespace characters
	 */
	protected byte indentationMode = -1;

	/**
	 * Figure out what indentation the {@link #buffer()} is written in.
	 * @return The value assigned to {@link #indentationMode}
	 */
	protected byte figureOutIndentation() {
		Set<BigInteger> leadingWhitespaceCounts = null;
		for (final String line : StringUtil.lines(new CharArrayReader(buffer)))
			if (line.length() > 0 && line.charAt(0) == '\t')
				// one tab is enough to conclude that
				return indentationMode = TABINDENTATIONMODE;
			else {
				int numWhitespace;
				for (numWhitespace = 0; numWhitespace < line.length() && line.charAt(numWhitespace) == ' '; numWhitespace++);
				if (numWhitespace > 0) {
					if (leadingWhitespaceCounts == null)
						leadingWhitespaceCounts = new HashSet<BigInteger>();
					leadingWhitespaceCounts.add(BigInteger.valueOf(numWhitespace));
				}
			}
		if (leadingWhitespaceCounts != null) {
			BigInteger gcd = null;
			for (final BigInteger wc : leadingWhitespaceCounts)
				gcd = gcd == null ? wc : wc.gcd(gcd);
			return indentationMode = gcd.byteValue();
		} else
			return indentationMode = TABINDENTATIONMODE;
	}

	/**
	 * Create a new scanner that scans a string
	 * @param withString
	 */
	public BufferedScanner(String withString) {
		init(withString);
	}

	protected final void init(String withString) {
		offset = 0;
		buffer = withString.toCharArray();
		size = buffer.length;
	}

	public BufferedScanner(Object source) {
		this(stringFromSource(source));
	}

	private static String stringFromSource(Object source) {
		if (source instanceof IStorage)
			return StreamUtil.stringFromStorage((IStorage) source);
		else if (source instanceof Reader)
			return StreamUtil.stringFromReader((Reader)source);
		else if (source instanceof InputStream)
			return StreamUtil.stringFromInputStream((InputStream)source);
		else if (source instanceof String)
			return (String)source;
		else if (source instanceof File)
			return StreamUtil.stringFromFile((File) source);
		else
			throw new IllegalArgumentException(String.format("source: %s", source));
	}

	/**
	 * Read the next character in the buffer
	 * @return the character (can be cast to char) or -1 if the current offset exceeds the size of the buffer
	 */
	@Override
	public final int read() {
		if (offset >= size) {
			offset++; // increment anyway so unread works as expected
			return -1;
		}
		return buffer[offset++];
	}

	/**
	 * Reverts the last read() call
	 * @return
	 */
	@Override
	public final void unread() {
		offset--;
	}

	/**
	 * Reads a string of the supplied length from the buffer
	 * @param length the length
	 * @return the read string
	 */
	public final String readString(int length) {
		if (offset+length > size)
			return null;
		final String result = new String(buffer, offset, length);
		offset += length;
		return result;
	}

	public final CharSequence bufferSequence(int start) {
		return CharBuffer.wrap(buffer).subSequence(start, size);
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

	public static boolean isWordStart(int character) {
		return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || character == '_';
	}

	/**
	 * Reads a code-word. (like regexp class [0-9a-zA-Z_])
	 * @return the code-word
	 */
	public final String readIdent() {
		final int start = offset;
		int length = 0;
		do {
			final int readByte = read();
			final boolean win = offset == start+1
				? isWordStart(readByte)
				: isWordPart(readByte);
			if (win)
				length++;
			else {
				seek(start);
				return length == 0 ? "" : readString(length);
			}
		} while(!reachedEOF());
		return readStringAt(start, start+length);
	}

	/**
	 * Reads a string until a char from <code>delimiters</code> occurs
	 * @param delimiters
	 * @return string sequence, without delimiter char
	 */
	public final String readStringUntil(char ...delimiters) {
		final int start = offset;
		int subtract = 0;
		Outer: do {
			final int readByte = read();
			for (int i = 0; i < delimiters.length; i++)
				if (readByte == delimiters[i]) {
					subtract = 1;
					break Outer;
				}
		} while(!reachedEOF());
		final int stringLength = offset - start - subtract;
		seek(start);
		return readString(stringLength);
	}

	public final int skipUntil(char... delimiters) {
		int subtract = 0;
		int len;
		Outer: for (len = 0; !reachedEOF(); len++) {
			final int readByte = read();
			for(int i = 0; i < delimiters.length;i++)
				if (readByte == delimiters[i]) {
					subtract = 1;
					break Outer;
				}
		}
		seek(this.offset-subtract);
		return len;
	}

	public final boolean skipSingleLineEnding() {
		if (read() == '\r') {
			if (read() != '\n')
				unread();
			return true;
		} else {
			unread();
			return false;
		}
	}

	/**
	 * Reads a string until a newline character occurs
	 * Cursor is after newline char(s)
	 * @return the line without newline char(s)
	 */
	public final String readLine() {
		final int start = offset;
		final String line = readStringUntil(NEWLINE_CHARS);
		if (line == null)
			return readStringAt(start, offset);
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
	public final void moveUntil(char[] delimiters) {
		do {
			final int readByte = read();
			for(int i = 0; i < delimiters.length;i++)
				if (readByte == delimiters[i])
					return;
		} while(!reachedEOF());
	}

	/**
	 * Moves offset until any other char than <code>charsToEat</code> occurs
	 * @param charsToEat
	 */
	public final int eat(char[] charsToEat) {
		if (reachedEOF())
			return 0; // no unreading() when already reached EOF
		int result = 0;
		do {
			final int readByte = read();
			boolean doEat = false;
			for (int i = 0; i < charsToEat.length;i++)
				if (readByte == charsToEat[i]) {
					doEat = true;
					result++;
					break;
				}
			if (!doEat) {
				unread();
				return result;
			}
		} while(!reachedEOF());
		return result;
	}

	public final int eatUntil(char ...delimiters) {
		if (reachedEOF())
			return 0; // no unreading() when already reached EOF
		int result = 0;
		do {
			final int readByte = read();
			boolean isDelimiter = false;
			for (int i = 0; i < delimiters.length;i++)
				if (readByte == delimiters[i]) {
					isDelimiter = true;
					result++;
					break;
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

	/**
	 * Return indentation of the line inside the given string at the given position using a specific indentation mode.
	 * @param s The string the line is contained in to return the indentation of
	 * @param pos The position inside the string
	 * @param indentationMode Indentation mode: {@link #TABINDENTATIONMODE} for tab indentation, number for number-of-whitespace-characters indentation
	 * @return The indentation at the specified position.
	 */
	public static int indentationOfStringAtPos(CharSequence s, int pos, byte indentationMode) {
		if (pos >= s.length())
			pos = s.length();
		switch (indentationMode) {
		case TABINDENTATIONMODE:
			int tabs = 0;
			for (--pos; pos >= 0 && !isLineDelimiterChar(s.charAt(pos)); pos--)
				if (pos < s.length() && s.charAt(pos) == '\t')
					tabs++;
				else
					tabs = 0; // don't count tabs not at the start of the line
			return tabs;
		default:
			int whitespace = 0;
			if (indentationMode > 0) {
				for (--pos; pos >= 0 && !isLineDelimiterChar(s.charAt(pos)); pos--)
					if (pos < s.length() && s.charAt(pos) == ' ')
						whitespace++;
					else
						whitespace = 0; // don't count tabs not at the start of the line
				return whitespace / indentationMode;
			}
			else
				return 0;
		}

	}

	public final int indentationAt(int offset) {
		return indentationOfStringAtPos(CharBuffer.wrap(buffer), offset, indentationMode);
	}

	public final int currentIndentation() {
		return indentationOfStringAtPos(CharBuffer.wrap(buffer), tell(), indentationMode);
	}

	/**
	 * Absolute offset manipulation
	 * @param newPos
	 * @return new offset
	 */
	public final int seek(int newPos) {
		offset = newPos;
		return offset;
	}

	/**
	 * Advance the current position by the given delta.
	 * @param delta The delta to advance the current position by
	 * @return The new position
	 */
	public final int advance(int delta) {
		return offset += delta;
	}

	/**
	 * Relative offset manipulation
	 * @param distance
	 * @return new offset
	 */
	public final int move(int distance) {
		offset += distance;
		if (offset >= size) offset = size - 1;
		return offset;
	}

	/**
	 * True if {@link #tell()} >= {@link #bufferSize()}
	 * @return whether eof reached
	 */
	public final boolean reachedEOF() {
		return offset >= size;
	}

	/**
	 * Current offset
	 * @return offset
	 */
	public final int tell() {
		return offset;
	}

	/**
	 * Reads a string at specified offset. The current offset of the scanner is not modified by this method
	 * @param start start offset of string
	 * @param end end offset of string
	 * @return the read string
	 */
	public final String readStringAt(int start, int end) {
		if (start == end)
			return ""; //$NON-NLS-1$
		final int p = tell();
		seek(start);
		final String result = readString(end-start);
		seek(p);
		return result;
	}

	public final String readStringAt(IRegion region) {
		return readStringAt(region.getOffset(), region.getOffset()+region.getLength());
	}

	/**
	 * Returns whether c is a line delimiter char
	 * @param c the char
	 * @return true if it is one, false if not
	 */
	public static boolean isLineDelimiterChar(char c) {
		return c == '\n' || c == '\r';
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
		return isLineDelimiterChar(c) || isWhiteSpaceButNotLineDelimiterChar(c);
	}

	/**
	 * Returns the line region is contained in as a string
	 * @param region the region
	 * @return the line string
	 */
	public final String lineAtRegion(IRegion region) {
		final IRegion lineRegion = regionOfLineContainingRegion(region);
		return new String(buffer, lineRegion.getOffset(), lineRegion.getLength());
	}

	/**
	 * Returns a substring of the script denoted by a region
	 * @param region the region
	 * @return the substring
	 */
	public final String bufferSubstringAtRegion(IRegion region) {
		return this.readStringAt(region.getOffset(), region.getOffset()+region.getLength());
	}

	/**
	 * Returns the line region is contained in as a region
	 * @param text the string to look for the line in
	 * @param regionInLine the region
	 * @return the line region
	 */
	public static IRegion regionOfLineContainingRegion(char[] text, IRegion regionInLine) {
		int start, end;
		for (start = regionInLine.getOffset(); start > 0 && start < text.length && !isLineDelimiterChar(text[start-1]); start--);
		for (end = regionInLine.getOffset()+regionInLine.getLength(); end+1 < text.length && !isLineDelimiterChar(text[end+1]); end++);
		return new Region(start, end-start+1);
	}

	public final IRegion regionOfLineContainingRegion(IRegion regionInLine) {
		return regionOfLineContainingRegion(this.buffer, regionInLine);
	}

	/**
	 * returns the size of the buffer
	 * @return the buffer size
	 */
	public final int bufferSize() {
		return size;
	}

	@Override
	public String toString() {
		return "offset: " + tell() + "; next: " + peekString(100); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Return the next-to-be-read char without modifying the scanner position
	 * @return the next char
	 */
	public final int peek() {
		final int p = read();
		unread();
		return p;
	}

	/**
	 * Returns the first character from the current offset that is not whitespace. This method does not alter the current offset
	 * @return
	 */
	public final int peekAfterWhitespace() {
		final int pos = offset;
		eatWhitespace();
		final int result = read();
		seek(pos);
		return result;
	}

	public final String peekString(int length) {
		final int pos = offset;
		final String result = readString(Math.min(length, size-offset));
		seek(pos);
		return result;
	}

	public final String stringAtRegion(IRegion region) {
		return new String(buffer, region.getOffset(), +region.getLength());
	}

	public final void reset(String text) {
		if (text == null)
			text = "";
		buffer = text.toCharArray();
		offset = 0;
		size = buffer.length;
	}

	public void reset() {
		offset = 0;
	}

	@Override
	public final char[][] getLegalLineDelimiters() {
		return new char[][] {{'\n'}, {'\r', '\n'}};
	}

	@Override
	public final int getColumn() {
		return indentationAt(offset);
	}

}