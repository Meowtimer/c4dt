package net.arctics.clonk.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IniReader {
//	private InputStream stream;
	private BufferedReader reader;
	
	public IniReader(InputStream stream) {
//		this.stream = stream;
		reader = new BufferedReader(new InputStreamReader(stream));
	}
	
	/**
	 * Moves the cursor until a section is found.
	 * @return the section name without [] or <code>null</code> if no more section is available
	 */
	public String nextSection() {
		try {
			String line;
			do {
				line = reader.readLine();
				if (line == null) return null;
				line = line.trim();
			} while(!line.startsWith("[") || !line.endsWith("]"));
			return line.substring(1,line.length() - 1);
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Moves the cursor until an entry is found.
	 * @return the key at index 0 and the value at index 1
	 */
	public String[] nextEntry() {
		try {
			String line;
			do {
				line = reader.readLine();
				if (line == null) return null;
				line = line.trim();
			} while(line.indexOf('=') == -1);
			int splitPos = line.indexOf('=');
			String[] result = new String[2];
			result[0] = line.substring(0,splitPos - 1);
			result[1] = line.substring(splitPos + 1);
			return result;
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
