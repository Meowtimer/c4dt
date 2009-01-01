package net.arctics.clonk.parser.defcore;

import java.io.InputStream;

import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4ScriptParser.BufferedScanner;

public class IniReader {

	private BufferedScanner reader;
	
	public static class IniEntry {
		private int startPos;
		private String key;
		private String value;
		
		protected IniEntry(int pos, String k, String v) {
			startPos = pos;
			key = k;
			value = v;
		}

		public int getStartPos() {
			return startPos;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
	
	public IniReader(InputStream stream) {
		try {
			reader = new BufferedScanner(stream, 0);
		} catch (CompilerException e) {
			e.printStackTrace();
		}
	}
	
	public int getPosition() {
		return reader.getPosition();
	}
	
	/**
	 * Moves the cursor until a section is found.
	 * @return the section name without [] or <code>null</code> if no more section is available
	 */
	public String nextSection() {
		String line;
		do {
			line = reader.readLine();
			if (line == null) return null;
//				line = line.trim();
		} while(!line.startsWith("[") || !line.endsWith("]"));
		return line.substring(1,line.length() - 1);
	}
	
	/**
	 * Moves the cursor until an entry is found.
	 * @return the key at index 0 and the value at index 1
	 */
	public IniEntry nextEntry() {
		String line;
		int splitPos, beforePos;
		do {
			beforePos = getPosition();
			line = reader.readLine();
			if (line == null) return null;
			if (line.startsWith("[")) {
				reader.seek(beforePos);
				return null;
			}
//				line = line.trim();
		} while((splitPos = line.indexOf('=')) == -1);
		return new IniEntry(beforePos, line.substring(0,splitPos), line.substring(splitPos + 1));
	}
}
