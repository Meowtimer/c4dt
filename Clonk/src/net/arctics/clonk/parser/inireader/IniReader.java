package net.arctics.clonk.parser.inireader;

import java.io.InputStream;
import java.io.InvalidClassException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.util.IHasChildren;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class IniReader implements Iterable<IniSection>, IHasChildren {

	protected BufferedScanner reader;
	protected IFile iniFile = null;
	// map to access sections by their name - only useful when sections have different names 
	protected Map<String, IniSection> sections = new HashMap<String, IniSection>();
	/// list of all sections regardless of name (for ActMap and similar files)
	protected List<IniSection> sectionsList = new LinkedList<IniSection>();
	protected String defaultName;
	
	public IniReader(InputStream stream) {
		reader = new BufferedScanner(stream);
	}
	
	public IniReader(String text) {
		reader = new BufferedScanner(text);
	}
	
	public IniReader(IFile file) {
		try {
			defaultName = file.getParent().getName();
			InputStream stream = file.getContents();
			reader = new BufferedScanner(stream);
			stream.close();
		} catch (Exception e) {
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
		return getConfiguration() == null || getConfiguration().hasSection(name);
	}
	
	/**
	 * Checks whether this entry name/value combination is valid.<br>
	 * Clients may override. This implementation always returns unmodified <tt>entry</tt>.
	 * @param entry
	 * @param section 
	 * @return validated entry
	 */
	protected IniEntry validateEntry(IniEntry entry, IniSection section) throws IniParserException {
		IniConfiguration configuration = getConfiguration();
		if (configuration == null)
			return entry;
		IniDataSection sectionConfig = configuration.getSections().get(section.getName());
		if (sectionConfig == null)
			return entry; // don't throw errors in unknown section
		if (!sectionConfig.hasEntry(entry.getKey())) {
			throw new IniParserException(IMarker.SEVERITY_WARNING, "Unknown option '" + entry.getKey() + "'", entry.getStartPos(), entry.getKey().length() + entry.getStartPos());
		}
		IniDataEntry entryConfig = sectionConfig.getEntries().get(entry.getKey());
		try {
			try {
				Object value = configuration.getFactory().create(entryConfig.getEntryClass(), entry.getValue());
				return ComplexIniEntry.adaptFrom(entry, value, entryConfig);
			}
			catch(IniParserException e) { // add offsets and throw through
				// FIXME: whitespace before and after '=' is not taken into account
				if (e.getOffset() == 0 || e.getEndOffset() == 0) {
					e.setOffset(entry.getStartPos() + entry.getKey().length() + 1);
					e.setEndOffset(entry.getStartPos() + entry.getKey().length() + 1 + entry.getValue().length());
				}
				throw e;
			}
		} catch (InvalidClassException e) {
			throw new IniParserException(IMarker.SEVERITY_WARNING, "There is a bug in the ini scheme. Report the following data to a C4DT developer: " + e.getMessage(),entry.getStartPos(),entry.getStartPos() + entry.getKey().length());
		}
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
	
	public void parse() {
		if (getIniFile() != null) {
			try {
				getIniFile().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		reader.seek(0);
//		IniEntry[] entries = null;
		IniSection section;
		while ((section = parseSection()) != null) {
			sections.put(section.getName(), section);
			sectionsList.add(section);
		}
		if (!reader.reachedEOF()) {
			createMarker("Unexpected data.", IMarker.SEVERITY_WARNING, reader.getPosition() - 2, reader.getPosition());
		}
	}
	
	protected IniSection parseSection() {
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
					createMarker("Unknown section name", IMarker.SEVERITY_WARNING, start, reader.getPosition() - 1);
				}
			}
			IniSection section = new IniSection(start, name);
			// parse entries
//			List<IniEntry> entries = new LinkedList<IniEntry>();
			IniEntry entry = null;
			Map<String, IniEntry> entries = new HashMap<String, IniEntry>(); 
			while ((entry = parseEntry(section)) != null) {
				entries.put(entry.getKey(),entry);
			}
			section.setEntries(entries);
			return section;
		}
		else {
			reader.unread();
			return null;
		}
	}
	
	protected void skipComment() {
		int r = reader.read();
		if (r == ';' || r == '#') {
			reader.readStringUntil('\n');
		}
		else 
			reader.unread();
	}
	
	protected IniEntry parseEntry(IniSection section) {
		skipComment();
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
		reader.eat(new char[] {' ', '\t'});
		String value = reader.readStringUntil(BufferedScanner.NEWLINE_DELIMITERS);
		int valEnd = reader.getPosition();
		reader.eat(BufferedScanner.NEWLINE_DELIMITERS);
		IniEntry entry = new IniEntry(keyStart, valEnd, key, value);
		try {
			return validateEntry(entry, section);
		} catch (IniParserException e) {
			createMarker(e.getMessage(), e.getSeverity(), e.getOffset(), e.getEndOffset());
			return entry;
		}
	}
	
	public IniConfiguration getConfiguration() {
		return null;
	}

	public Iterator<IniSection> iterator() {
		return sectionsList.iterator();
	}
	
	public IniSection sectionWithName(String name) {
		return sections.get(name);
	}
	
	public IniEntry entryInSection(String section, String entry) {
		IniSection s = sections.get(section);
		return s != null ? s.getEntry(entry) : null;
	}

	public IniSection[] getSections() {
		return sectionsList.toArray(new IniSection[sectionsList.size()]);
	}

	public Object[] getChildren() {
		return getSections();
	}

	public boolean hasChildren() {
		return !sections.isEmpty();
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
