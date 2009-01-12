package net.arctics.clonk.parser.inireader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4ScriptParser.BufferedScanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class IniReader {

	protected BufferedScanner reader;
	protected IFile iniFile = null;
	protected Map<String, IniEntry> entries = new HashMap<String, IniEntry>();
	
	public IniReader(InputStream stream) {
		try {
			reader = new BufferedScanner(stream, 0);
		} catch (CompilerException e) {
			e.printStackTrace();
		}
	}
	
	public IniReader(IFile file) {
		try {
			reader = new BufferedScanner(file.getContents(), 0);
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (CompilerException e) {
			e.printStackTrace();
		}
		iniFile = file;
	}
	
	public int getPosition() {
		return reader.getPosition();
	}
	
	public IFile getIniFile() {
		return iniFile;
	}

	public void setIniFile(IFile iniFile) {
		this.iniFile = iniFile;
	}

//	/**
//	 * Moves the cursor until a section is found.
//	 * @return the section name without [] or <code>null</code> if no more section is available
//	 */
//	public String nextSection() {
//		String line;
//		do {
//			line = reader.readLine();
//			if (line == null) return null;
////				line = line.trim();
//		} while(!line.startsWith("[") || !line.endsWith("]"));
//		return line.substring(1,line.length() - 1);
//	}
	
	/**
	 * Checks whether this section name is valid.<br>
	 * Clients may override. This implementation always returns <tt>true</tt>.
	 * @param name
	 * @return <tt>true</tt> if valid
	 */
	protected boolean isSectionNameValid(String name) {
		return true;
	}
	
	/**
	 * Checks whether this entry name/value combination is valid.<br>
	 * Clients may override. This implementation always returns unmodified <tt>entry</tt>.
	 * @param entry
	 * @param section 
	 * @return validated entry
	 */
	protected IniEntry validateEntry(IniEntry entry, IniSection section) throws IniParserException {
		return entry;
	}
	
	protected IMarker createMarker(String message, int severity, int offset, int endOffset) {
		if (getIniFile() != null) {
			try {
				IMarker marker = getIniFile().createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.SEVERITY, severity);
				marker.setAttribute(IMarker.TRANSIENT, false);
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.CHAR_START, offset);
				marker.setAttribute(IMarker.CHAR_END, endOffset);
				return marker;
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		else {
			// TODO: notice warnings of extern objects?
			return null;
		}
	}
	
	public void parse() throws CompilerException {
		if (getIniFile() != null) {
			try {
				getIniFile().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		reader.seek(0);
//		IniEntry[] entries = null;
		while(parseSection() != null);
		if (!reader.reachedEOF()) {
			createMarker("Unexpected data.", IMarker.SEVERITY_WARNING, reader.getPosition() - 2, reader.getPosition());
		}
	}
	
	protected IniSection parseSection() throws CompilerException {
		reader.eatWhitespace();
		int start = reader.getPosition();
		// parse head
		if (reader.read() == '[') {
			String name = reader.readStringUntil(']','\n','\r');
			if (reader.read() != ']') {
				createMarker("Parser error: Invalid section head, ']' expected", IMarker.SEVERITY_ERROR, start, reader.getPosition());
				return null;
			}
			else {
				if (!isSectionNameValid(name)) {
					createMarker("Section name '' is unknown.", IMarker.SEVERITY_WARNING, start, reader.getPosition() - 1);
				}
			}
			IniSection section = new IniSection(start, name);
			// parse entries
//			List<IniEntry> entries = new LinkedList<IniEntry>();
			IniEntry entry = null;
			while((entry = parseEntry(section)) != null) {
				entries.put(entry.getKey().toLowerCase(),entry);
			}
			section.setEntries(entries.values().toArray(new IniEntry[entries.size()]));
			return section;
		}
		else {
			reader.unread();
			return null;
		}
	}
	
	protected IniEntry parseEntry(IniSection section) {
		int start = reader.getPosition();
		reader.eatWhitespace();
		if (reader.read() == '[') {
			reader.seek(start);
			return null;
		}
		reader.unread();
		if (reader.reachedEOF()) return null;
		int keyStart = reader.getPosition();
		String key = reader.readWord();
		reader.eatWhitespace();
		if (reader.read() != '=') {
			createMarker("Parse error: expected '='", IMarker.SEVERITY_ERROR, keyStart + key.length(), reader.getPosition());
		}
		reader.eatWhitespace();
		String value = reader.readLine();
		IniEntry entry = new IniEntry(keyStart, key, value);
		try {
			return validateEntry(entry, section);
		} catch (IniParserException e) {
			createMarker(e.getMessage(), e.getSeverity(), e.getOffset(), e.getEndOffset());
			return entry;
		}
	}
	
//	/**
//	 * Moves the cursor until an entry is found.
//	 * @return the key at index 0 and the value at index 1
//	 */
//	public IniEntry nextEntry() {
//		String line;
//		int splitPos, beforePos;
//		do {
//			beforePos = getPosition();
//			line = reader.readLine();
//			if (line == null) return null;
//			if (line.startsWith("[")) {
//				reader.seek(beforePos);
//				return null;
//			}
////				line = line.trim();
//		} while((splitPos = line.indexOf('=')) == -1);
//		return new IniEntry(beforePos, line.substring(0,splitPos).trim(), line.substring(splitPos + 1).trim());
//	}
}
