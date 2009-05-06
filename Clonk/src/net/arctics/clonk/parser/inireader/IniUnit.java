package net.arctics.clonk.parser.inireader;

import java.io.InputStream;
import java.io.InvalidClassException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniSectionData;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Reads Windows ini style configuration files
 */
public class IniUnit extends C4Declaration implements Iterable<IniSection>, IHasChildren, ITreeNode {

	private static final long serialVersionUID = 1L;

	/**
	 * Text scanner
	 */
	protected BufferedScanner reader;
	
	/**
	 * The configuration file
	 */
	protected IFile iniFile = null;
	
	/**
	 * map to access sections by their name - only useful when sections have different names
	 */
	protected Map<String, IniSection> sectionsMap = new HashMap<String, IniSection>();
	
	/**
	 * list of all sections regardless of name (for ActMap and similar files)
	 */
	protected List<IniSection> sectionsList = new LinkedList<IniSection>();
	
	/**
	 * Name of the configuration that is to be used when no name was explicitly defined in the file. (?)
	 */
	protected String defaultName;
	
	/**
	 * Temporary reference to the section being currently parsed.
	 */
	protected IniSection currentSection;
	
	/**
	 * Creates an IniReader that reads ini information from a stream
	 * @param stream the stream
	 */
	public IniUnit(InputStream stream) {
		reader = new BufferedScanner(stream);
	}
	
	/**
	 * Creates an IniReader that reads ini information from a string
	 * @param text the string
	 */
	public IniUnit(String text) {
		reader = new BufferedScanner(text);
	}
	
	/**
	 * Creates an IniReader that reads ini information from a project file
	 * @param file the file
	 */
	public IniUnit(IFile file) {
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
	
	/**
	 * Returns the file the configuration was read from
	 * @return the file
	 */
	public IFile getIniFile() {
		return iniFile;
	}
	
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
		IniSectionData sectionConfig = currentSection.getSectionData();
		if (sectionConfig == null)
			return entry; // don't throw errors in unknown section
		if (!sectionConfig.hasEntry(entry.getKey())) {
			throw new IniParserException(IMarker.SEVERITY_WARNING, "Unknown option '" + entry.getKey() + "'", entry.getStartPos(), entry.getKey().length() + entry.getStartPos());
		}
		IniDataEntry entryConfig = sectionConfig.getEntry(entry.getKey());
		try {
			try {
				Object value = configuration.getFactory().create(entryConfig.getEntryClass(), entry.getValue());
				return ComplexIniEntry.adaptFrom(entry, value, entryConfig);
			}
			catch(IniParserException e) { // add offsets and throw through
				// FIXME: whitespace before and after '=' is not taken into account
				if (e.getOffset() == 0 || e.getEndOffset() == 0) {
					String key = entry.getKey();
					String value = entry.getValue();
					if (value == null)
						value = "";
					e.setOffset(entry.getStartPos() + key.length() + 1);
					e.setEndOffset(entry.getStartPos() + key.length() + 1 + value.length());
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
		this.clear();
		reader.seek(0);
		IniSection section;
		while ((section = parseSection()) != null) {
			sectionsMap.put(section.getName(), section);
			sectionsList.add(section);
		}
		if (!reader.reachedEOF()) {
			createMarker("Unexpected data.", IMarker.SEVERITY_WARNING, reader.getPosition() - 2, reader.getPosition());
		}
	}
	
	private void clear() {
		sectionsList.clear();
		sectionsMap.clear();
	}

	protected IniSectionData getSectionDataFor(IniSection section) {
		return getConfiguration() != null
			? getConfiguration().getSections().get(section.getName())
			: null;
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
			int end = reader.getPosition();
			IniSection section = new IniSection(new SourceLocation(start, end), name);
			section.setParentDeclaration(this);
			// parse entries
//			List<IniEntry> entries = new LinkedList<IniEntry>();
			IniEntry entry = null;
			Map<String, IniEntry> entries = new HashMap<String, IniEntry>();
			currentSection = section;
			currentSection.setSectionData(getSectionDataFor(section));
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
		entry.setParentDeclaration(section);
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
		return sectionsMap.get(name);
	}
	
	public IniEntry entryInSection(String section, String entry) {
		IniSection s = sectionsMap.get(section);
		return s != null ? s.getEntry(entry) : null;
	}

	public IniSection[] getSections() {
		return sectionsList.toArray(new IniSection[sectionsList.size()]);
	}

	public Object[] getChildren() {
		return getSections();
	}

	public boolean hasChildren() {
		return !sectionsMap.isEmpty();
	}

	public void commitTo(C4ScriptBase script) {
		// placeholder
	}

	public void addChild(ITreeNode node) {
		// TODO Auto-generated method stub
		
	}

	public List<? extends ITreeNode> getChildCollection() {
		return sectionsList;
	}

	public String getNodeName() {
		return iniFile != null ? iniFile.getName() : toString();
	}

	public ITreeNode getParentNode() {
		return null;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
	
	@Override
	public IResource getResource() {
		return iniFile;
	}
	
	public String sectionToString(IniSection section) {
		return "["+section.getName()+"]";
	}

	public IniSection sectionAtOffset(int offset, int addIfOverOffset) {
		IniSection section = null;
		for (IniSection sec : this.getSections()) {
			int start = sec.getLocation().getStart();
			if (start > offset)
				start += addIfOverOffset;
			if (start > offset)
				break;
			section = sec;
		}
		return section;
	}
	
	@Override
	public Object[] getSubDeclarationsForOutline() {
		return this.getChildren();
	}
	
}
