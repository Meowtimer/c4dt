package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.foldermap.FolderMapUnit;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniDataBase;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.parser.playercontrols.PlayerControlsUnit;
import net.arctics.clonk.parser.teamsdef.TeamsUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.ide.IDE;

/**
 * Reads Windows ini style configuration files
 */
public class IniUnit extends C4Structure implements Iterable<IniSection>, IHasChildren, ITreeNode {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	/**
	 * Text scanner
	 */
	protected BufferedScanner scanner;
	
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
	
	public IniUnit(Object input) {
		scanner = new BufferedScanner(input);
	}
	
	public IniUnit(String input) {
		this((Object)input);
	}
	
	public BufferedScanner getScanner() {
		return scanner;
	}
	
	/**
	 * Creates an IniReader that reads ini information from a project file
	 * @param file the file
	 */
	public IniUnit(final IFile file) {
		try {
			defaultName = file.getParent().getName();
			scanner = new BufferedScanner(file) {
				@Override
				public void reset() {
					reset(Utilities.stringFromFile(file));
				};
			};
		} catch (Exception e) {
			e.printStackTrace();
		}
		iniFile = file;
	}
	
	public void save(Writer writer) throws IOException {
		for (IniSection section : sectionsList) {
			section.writeTextRepresentation(writer, -1);
		}
	}
	
	/**
	 * Returns the file the configuration was read from
	 * @return the file
	 */
	public IFile getIniFile() {
		return iniFile;
	}
	
	public void setIniFile(IFile file) {
		iniFile = file;
	}
	
	/**
	 * Checks whether this section name is valid.<br>
	 * Default implementation consults the configuration returned from getConfiguration() to determine if the section is valid.
	 * @param name
	 * @return <tt>true</tt> if valid
	 */
	protected boolean isSectionNameValid(String name, IniSection parentSection) {
		if (parentSection != null) {
			return parentSection.getSectionData() == null || parentSection.getSectionData().hasSection(name);
		}
		else {
			IniConfiguration conf = getConfiguration();
			return conf == null || conf.hasSection(name);
		}
	}
	
	/**
	 * Checks whether this entry name/value combination is valid.<br>
	 * Clients may override. This implementation always returns unmodified <tt>entry</tt>.
	 * @param entry
	 * @param section 
	 * @param modifyMarkers 
	 * @return validated entry
	 */
	protected IniEntry validateEntry(IniEntry entry, IniSection section, boolean modifyMarkers) throws IniParserException {
		IniConfiguration configuration = getConfiguration();
		if (configuration == null)
			return entry;
		IniDataSection sectionConfig = currentSection.getSectionData();
		if (sectionConfig == null)
			return entry; // don't throw errors in unknown section
		if (!sectionConfig.hasEntry(entry.getKey())) {
			throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownOption, entry.getKey()), entry.getStartPos(), entry.getKey().length() + entry.getStartPos()); //$NON-NLS-2$
		}
		IniDataBase dataItem = sectionConfig.getEntry(entry.getKey());
		if (dataItem instanceof IniDataEntry) {
			IniDataEntry entryConfig = (IniDataEntry) dataItem;
			try {
				try {
					Object value = configuration.getFactory().create(entryConfig.getEntryClass(), entry.getValue(), entryConfig, this);
					return ComplexIniEntry.adaptFrom(entry, value, entryConfig, modifyMarkers);
				}
				catch(IniParserException e) { // add offsets and throw through
					// FIXME: whitespace before and after '=' is not taken into account
					if (e.getOffset() == 0 || e.getEndOffset() == 0) {
						String key = entry.getKey();
						String value = entry.getValue();
						if (value == null)
							value = ""; //$NON-NLS-1$
						e.setOffset(entry.getStartPos() + key.length() + 1);
						e.setEndOffset(entry.getStartPos() + key.length() + 1 + value.length());
					}
					throw e;
				}
			} catch (InvalidClassException e) {
				throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.InternalIniParserBug, e.getMessage()),entry.getStartPos(),entry.getStartPos() + entry.getKey().length());
			}
		}
		else {
			throw new IniParserException(IMarker.SEVERITY_ERROR, "Fail");
		}
	}
	
	public IniSection requestSection(String name, IniDataSection dataSection) {
		IniSection result = sectionsMap.get(name);
		if (result == null) {
			result = new IniSection(null, name);
			result.setSubItems(new HashMap<String, IniItem>(), new LinkedList<IniItem>());
			result.setParentDeclaration(this);
			result.setSectionData(dataSection);
			sectionsMap.put(name, result);
			sectionsList.add(result);
		}
		return result;
	}
	
	public void parse(boolean modifyMarkers) {
		scanner.reset();
		if (modifyMarkers && getIniFile() != null) {
			try {
				getIniFile().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
				// deactivate creating markers if it's contained in a linked group
				modifyMarkers = !C4GroupItem.isLinkedResource(getIniFile());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		this.clear();
		scanner.seek(0);
		IniSection section;
		while ((section = parseSection(modifyMarkers, null)) != null) {
			sectionsMap.put(section.getName(), section);
			sectionsList.add(section);
		}
	}
	
	private void clear() {
		sectionsList.clear();
		sectionsMap.clear();
	}

	protected IniDataSection getSectionDataFor(IniSection section, IniSection parentSection) {
		if (parentSection != null) {
			if (parentSection.getSectionData() != null) {
				IniDataBase dataItem = parentSection.getSectionData().getEntry(section.getName());
				return dataItem instanceof IniDataSection ? (IniDataSection)dataItem : null;
			}
			else
				return null;
		}
		else {
			return getConfiguration() != null
			? getConfiguration().getSections().get(section.getName())
					: null;
		}
	}
	
	protected IniSection parseSection(boolean modifyMarkers, IniSection parentSection) {
		int targetIndentation = parentSection != null ? parentSection.getIndentation()+1 : 0;
		while (skipComment());
		int start = scanner.getPosition();
		// parse head
		int indentation = scanner.getTabIndentation();
		if (scanner.read() == '[' && indentation == targetIndentation) {
			String name = scanner.readStringUntil(']','\n','\r');
			if (scanner.read() != ']') {
				if (modifyMarkers)
					marker(ParserErrorCode.TokenExpected, start, scanner.getPosition(), IMarker.SEVERITY_ERROR, (Object)"]"); //$NON-NLS-1$
				return null;
			}
			else {
				if (!isSectionNameValid(name, parentSection)) {
					if (modifyMarkers)
						marker(ParserErrorCode.UnknownSection, start, scanner.getPosition()-1, IMarker.SEVERITY_WARNING, name);
				}
				scanner.eat(BufferedScanner.NEWLINE_CHARS); // ignore rest of section line
			}
			int end = scanner.getPosition();
			IniSection section = new IniSection(new SourceLocation(start, end), name);
			section.setParentDeclaration(parentSection != null ? parentSection : this);
			section.setIndentation(indentation);
			// parse entries
			IniItem item= null;
			Map<String, IniItem> itemMap = new HashMap<String, IniItem>();
			List<IniItem> itemList = new LinkedList<IniItem>();
			currentSection = section;
			currentSection.setSectionData(getSectionDataFor(section, parentSection));
			while ((item = parseSectionOrEntry(section, modifyMarkers, section)) != null) {
				itemMap.put(item.getKey(),item);
				itemList.add(item);
			}
			section.setSubItems(itemMap, itemList);
			return section;
		}
		else {
			scanner.unread();
			return null;
		}
	}
	
	protected boolean skipComment() {
		scanner.eatWhitespace();
		int _r;
		for (_r = scanner.read(); _r == 0; _r = scanner.read());
		char r = (char) _r;
		if (r == ';' || r == '#') {
			scanner.readStringUntil('\n');
			scanner.eatWhitespace();
			return true;
		}
		else if (r == '/') {
			switch (scanner.read()) {
			case '/':
				scanner.eatUntil(BufferedScanner.NEWLINE_CHARS);
				scanner.eatWhitespace();
				return true;
			case '*':
				for (; !scanner.reachedEOF();) {
					if (scanner.read() == '*') {
						if (scanner.read() == '/')
							break;
						else
							scanner.unread();
					}
				}
				scanner.eatWhitespace();
				return true;
			default:
				scanner.unread(); scanner.unread();
				return false;
			}
		}
		else {
			scanner.unread();
			return false;
		}
	}
	
	public void marker(String markerType, ParserErrorCode error, int start, int end, int markerSeverity, Object... args) {
		error.createMarker(iniFile, this, markerType, start, end, markerSeverity, null, args);
	}
	
	public void marker(ParserErrorCode error, int start, int end, int markerSeverity, Object... args) {
		marker(ClonkCore.MARKER_INI_ERROR, error, start, end, markerSeverity, args);
	}
	
	public void markerAtValue(String markerType, ParserErrorCode error, IniEntry entry, int markerSeverity, Object... args) {
		marker(markerType, error, entry.getLocation().getStart(), entry.getLocation().getEnd(), markerSeverity, args);
	}
	
	protected IniEntry parseEntry(IniSection section, boolean modifyMarkers, IniSection parentSection) {
		int targetIndentation = parentSection != null ? parentSection.getIndentation() : 0;
		while (skipComment());
		int start = scanner.getPosition();
		scanner.eatWhitespace();
		int indentation = scanner.getTabIndentation();
		if (indentation != targetIndentation || scanner.read() == '[') {
			scanner.seek(start);
			return null;
		}
		scanner.unread();
		if (scanner.reachedEOF()) return null;
		int keyStart = scanner.getPosition();
		String key = scanner.readIdent();
		scanner.eatWhitespace();
		if (scanner.read() != '=') {
			if (modifyMarkers)
				marker(ParserErrorCode.TokenExpected, keyStart+key.length(), scanner.getPosition(), IMarker.SEVERITY_ERROR, (Object)"="); //$NON-NLS-1$
		}
		scanner.eat(new char[] {' ', '\t'});
		String value = scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
		int valEnd = scanner.getPosition();
		int commentStart = value != null ? value.indexOf('#') : -1;
		if (commentStart != -1) {
			valEnd -= value.length()-commentStart;
			value = value.substring(0, commentStart);
		}
		scanner.eat(BufferedScanner.NEWLINE_CHARS);
		IniEntry entry = new IniEntry(keyStart, valEnd, key, value);
		entry.setParentDeclaration(section);
		try {
			return validateEntry(entry, section, modifyMarkers);
		} catch (IniParserException e) {
			if (modifyMarkers)
				marker(ParserErrorCode.GenericError, e.getOffset(), e.getEndOffset(), e.getSeverity(), (Object)e.getMessage());
			return entry;
		}
	}
	
	protected IniItem parseSectionOrEntry(IniSection section, boolean modifyMarkers, IniSection parentSection) {
		IniEntry entry = parseEntry(section, modifyMarkers, parentSection);
		if (entry == null) {
			IniSection s = parseSection(modifyMarkers, parentSection);
			if (s != null)
				return s;
			else
				return null;
		}
		else
			return entry;
	}
	
	protected String getConfigurationName() {
		return null;
	}
	
	public IniConfiguration getConfiguration() {
		String confName = getConfigurationName();
		if (confName != null && getEngine() != null && getEngine().getIniConfigurations() != null)
			return getEngine().getIniConfigurations().getConfigurationFor(confName);
		else
			return null;
	}

	public Iterator<IniSection> iterator() {
		return sectionsList.iterator();
	}
	
	public IniSection sectionWithName(String name) {
		return sectionsMap.get(name);
	}
	
	public IniSection sectionMatching(IPredicate<IniSection> predicate) {
		return Utilities.itemMatching(predicate, sectionsList);
	}
	
	public IniItem itemInSection(String section, String entry) {
		IniSection s = sectionsMap.get(section);
		return s != null ? s.getSubItem(entry) : null;
	}
	
	public IniEntry entryInSection(String section, String entry) {
		IniItem item = itemInSection(section, entry);
		return item instanceof IniEntry ? (IniEntry)item : null;
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
		return "["+section.getName()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IniSection sectionAtOffset(IniSection parent, int offset) {
		IniSection section = null;
		for (IniSection sec : parent == null ? Utilities.arrayIterable(this.getSections()) : parent.getSections()) {
			int start = sec.getLocation().getStart();
			if (start > offset) {
				break;
			}
			section = sec;
		}
		return section == null ? parent : sectionAtOffset(section, offset);
	}
	
	public IniSection sectionAtOffset(int offset) {
		IniSection sec = sectionAtOffset(null, offset);
		/*if (sec != null && scanner != null) {
			int indentation = sec.getIndentation() - scanner.getTabIndentation(offset);
			for (; indentation > 0 && sec.getParentDeclaration() instanceof IniSection; indentation--, sec = (IniSection) sec.getParentDeclaration());
		}*/
		return sec;
	}
	
	@Override
	public Object[] getSubDeclarationsForOutline() {
		return this.getChildren();
	}
	
	@Override
	public C4Declaration findLocalDeclaration(String declarationName,
			Class<? extends C4Declaration> declarationClass) {
		if (declarationClass.isAssignableFrom(IniSection.class))
			return findDeclaration(declarationName);
		return null;
	}
	
	@Override
	public C4Declaration findDeclaration(String declarationName) {
		return sectionWithName(declarationName);
	}
	
	@Override
	public C4Structure getTopLevelStructure() {
		return this;
	}
	
	@Override
	public String toString() {
		if (getIniFile() != null)
			return getIniFile().getFullPath().toOSString();
		else
			return super.toString();
	}
	
	public static void register() {
		C4Structure.registerStructureFactory(new IStructureFactory() {
			public C4Structure create(IResource resource, boolean duringBuild) {
				Class<? extends IniUnit> iniUnitClass = getIniUnitClass(resource);
				if (iniUnitClass != null) {
					try {
						IniUnit reader = iniUnitClass.getConstructor(IFile.class).newInstance(resource);
						reader.parse(duringBuild);
						return reader;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		});
	}
	
	public static IniUnit createAdequateIniUnit(IFile file) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		return createAdequateIniUnit(file, false, file);
	}

	public static IniUnit createAdequateIniUnit(IFile file, boolean defaultToBaseClass, Object arg) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<? extends IniUnit> cls = getIniUnitClass(file);
		if (cls == null) {
			if (defaultToBaseClass)
				cls = IniUnit.class;
			else
				return null;
		}
		Class<?> neededArgType =
			arg instanceof String
				? String.class
				: arg instanceof IFile
					? IFile.class
					: InputStream.class;
		Constructor<? extends IniUnit> ctor = cls.getConstructor(neededArgType);
		IniUnit result = ctor.newInstance(arg);
		result.setIniFile(file);
		return result;
	}

	private static Map<String, Class<? extends IniUnit>> INIREADER_CLASSES = Utilities.map(new Object[] {
		ClonkCore.id("scenariocfg")  , ScenarioUnit.class, //$NON-NLS-1$
		ClonkCore.id("actmap")       , ActMapUnit.class, //$NON-NLS-1$
		ClonkCore.id("defcore")      , DefCoreUnit.class, //$NON-NLS-1$
		ClonkCore.id("particle")     , ParticleUnit.class, //$NON-NLS-1$
		ClonkCore.id("material")     , MaterialUnit.class, //$NON-NLS-1$
		ClonkCore.id("plrcontroldef"), PlayerControlsUnit.class, //$NON-NLS-1$
		ClonkCore.id("foldermap")    , FolderMapUnit.class,
		ClonkCore.id("teamsdef")     , TeamsUnit.class
	});

	/**
	 * Returns the IniUnit class that is best suited to parsing the given ini file
	 * @param resource the ini file to return an IniUnit class for
	 * @return the IniUnit class or null if no suitable one could be found
	 */
	public static Class<? extends IniUnit> getIniUnitClass(IResource resource) {
		IContentType contentType = resource instanceof IFile ? IDE.getContentType((IFile) resource) : null;
		if (contentType == null)
			return null;
		Class<? extends IniUnit> cls = INIREADER_CLASSES.get(contentType.getId());
		return cls != null ? cls : null;
	}
	
	@Override
	public void validate() {
		// don't bother letting items complain if errors shouldn't be shown anyway (in linked groups)
		if (C4GroupItem.isLinkedResource(iniFile))
			return;
		try {
			iniFile.deleteMarkers(ClonkCore.MARKER_C4SCRIPT_ERROR, true, 0);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		for (IniSection sec : this.sectionsList) {
			sec.validate();
		}
	}
	
	@Override
	public C4Engine getEngine() {
		ClonkProjectNature nature = ClonkProjectNature.get(getResource());
		return nature != null ? nature.getIndex().getEngine() : super.getEngine();
	}
	
	@Override
	public Iterable<? extends C4Declaration> allSubDeclarations() {
		return this.sectionsList;
	}

}
