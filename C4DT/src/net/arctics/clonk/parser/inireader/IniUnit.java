package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.foldermap.FolderMapUnit;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniDataBase;
import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;
import net.arctics.clonk.parser.inireader.IniData.IniSectionDefinition;
import net.arctics.clonk.parser.playercontrols.PlayerControlsUnit;
import net.arctics.clonk.parser.teamsdef.TeamsUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.CustomizationNature;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.ArrayUtil;
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

/**
 * Reads Windows ini style configuration files
 */
public class IniUnit extends Structure implements Iterable<IniSection>, IHasChildren, ITreeNode, IniItem {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private static final String INFO_FORMAT = "%s - %s";
	
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
	 * Parser parsing this IniUnit
	 */
	private IniUnitParser parser;
	
	public IniUnitParser parser() {
		return parser;
	}
	
	/**
	 * Creates an IniReader that reads ini information from a project file
	 * @param file the file
	 */
	public IniUnit(final Object input) {
		if (input instanceof IFile) {
			final IFile file = (IFile) input;
			try {
				defaultName = file.getParent().getName();
				parser = new IniUnitParser(this, file);
			} catch (Exception e) {
				e.printStackTrace();
			}
			iniFile = file;
		} else
			parser = new IniUnitParser(this, input);
	}
	
	public void save(Writer writer) throws IOException {
		for (IniSection section : sectionsList)
			section.writeTextRepresentation(writer, -1);
	}
	
	/**
	 * Returns the file the configuration was read from
	 * @return the file
	 */
	public IFile iniFile() {
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
		if (parentSection != null)
			return parentSection.sectionData() == null || parentSection.sectionData().hasSection(name);
		else {
			IniConfiguration conf = configuration();
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
		IniConfiguration configuration = configuration();
		if (configuration == null)
			return entry;
		IniSectionDefinition sectionConfig = currentSection.sectionData();
		if (sectionConfig == null)
			return entry; // don't throw errors in unknown section
		if (!sectionConfig.hasEntry(entry.key()))
			throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownOption, entry.key()), entry.start(), entry.key().length() + entry.start());
		IniDataBase dataItem = sectionConfig.entryForKey(entry.key());
		if (dataItem instanceof IniEntryDefinition) {
			IniEntryDefinition entryConfig = (IniEntryDefinition) dataItem;
			try {
				try {
					Object value = configuration.getFactory().create(entryConfig.entryClass(), entry.stringValue(), entryConfig, this);
					return ComplexIniEntry.adaptFrom(entry, value, entryConfig, modifyMarkers);
				}
				catch(IniParserException e) { // add offsets and throw through
					// FIXME: whitespace before and after '=' is not taken into account
					if (e.offset() == 0 || e.endOffset() == 0) {
						String key = entry.key();
						String value = entry.stringValue();
						if (value == null)
							value = ""; //$NON-NLS-1$
						e.setOffset(entry.start() + key.length() + 1);
						e.setEndOffset(entry.start() + key.length() + 1 + value.length());
					}
					throw e;
				}
			} catch (InvalidClassException e) {
				throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.InternalIniParserBug, e.getMessage()),entry.start(),entry.start() + entry.key().length());
			}
		} else
			throw new IniParserException(IMarker.SEVERITY_ERROR, "Fail");
	}
	
	public IniSection requestSection(String name, IniSectionDefinition dataSection) {
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
	
	void clear() {
		sectionsList.clear();
		sectionsMap.clear();
	}

	protected IniSectionDefinition sectionDataFor(IniSection section, IniSection parentSection) {
		if (parentSection != null) {
			if (parentSection.sectionData() != null) {
				IniDataBase dataItem = parentSection.sectionData().entryForKey(section.name());
				return dataItem instanceof IniSectionDefinition ? (IniSectionDefinition)dataItem : null;
			}
			else
				return null;
		} else
			return configuration() != null
			? configuration().getSections().get(section.name())
					: null;
	}
	
	protected void startParsing() {}
	protected void endParsing() {}
	
	public void marker(String markerType, ParserErrorCode error, int start, int end, int markerSeverity, Object... args) {
		error.createMarker(iniFile, this, markerType, start, end, markerSeverity, null, args);
	}
	
	public void marker(ParserErrorCode error, int start, int end, int markerSeverity, Object... args) {
		marker(Core.MARKER_INI_ERROR, error, start, end, markerSeverity, args);
	}
	
	public void markerAtValue(String markerType, ParserErrorCode error, IniEntry entry, int markerSeverity, Object... args) {
		marker(markerType, error, entry.location().start(), entry.location().end(), markerSeverity, args);
	}
	
	protected String configurationName() {
		return null;
	}
	
	public IniConfiguration configuration() {
		String confName = configurationName();
		if (confName != null && engine() != null && engine().iniConfigurations() != null)
			return engine().iniConfigurations().configurationFor(confName);
		else
			return null;
	}

	@Override
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
		return s != null ? s.subItemByKey(entry) : null;
	}
	
	public IniEntry entryInSection(String section, String entry) {
		IniItem item = itemInSection(section, entry);
		return item instanceof IniEntry ? (IniEntry)item : null;
	}

	public IniSection[] sections() {
		return sectionsList.toArray(new IniSection[sectionsList.size()]);
	}

	@Override
	public Object[] children() {
		return sections();
	}

	@Override
	public boolean hasChildren() {
		return !sectionsMap.isEmpty();
	}

	@Override
	public void addChild(ITreeNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<? extends IniItem> childCollection() {
		return sectionsList;
	}

	@Override
	public String nodeName() {
		return iniFile != null ? iniFile.getName() : toString();
	}

	@Override
	public ITreeNode parentNode() {
		return null;
	}

	@Override
	public IPath path() {
		return ITreeNode.Default.getPath(this);
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
	
	@Override
	public IResource resource() {
		return iniFile;
	}
	
	public String sectionToString(IniSection section) {
		return "["+section.name()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IniSection sectionAtOffset(IniSection parent, int offset) {
		IniSection section = null;
		for (IniSection sec : parent == null ? ArrayUtil.iterable(this.sections()) : parent.sections()) {
			int start = sec.location().start();
			if (start > offset)
				break;
			section = sec;
		}
		return section == null ? parent : sectionAtOffset(section, offset);
	}
	
	public IniSection sectionAtOffset(int offset) {
		IniSection sec = sectionAtOffset(null, offset);
		return sec;
	}
	
	@Override
	public Object[] subDeclarationsForOutline() {
		return this.children();
	}
	
	@Override
	public Declaration findLocalDeclaration(String declarationName,
			Class<? extends Declaration> declarationClass) {
		if (declarationClass.isAssignableFrom(IniSection.class))
			return findDeclaration(declarationName);
		return null;
	}
	
	@Override
	public Declaration findDeclaration(String declarationName) {
		return sectionWithName(declarationName);
	}
	
	@Override
	public Structure topLevelStructure() {
		return this;
	}
	
	@Override
	public String toString() {
		if (iniFile() != null)
			return iniFile().getFullPath().toOSString();
		else
			return super.toString();
	}
	
	public static void register() {
		Structure.registerStructureFactory(new IStructureFactory() {
			@Override
			public Structure create(IResource resource, boolean duringBuild) {
				if (resource instanceof IFile)
					try {
						IniUnit unit = createAdequateIniUnit((IFile) resource);
						if (unit != null)
							unit.parser().parse(duringBuild);
						return unit;
					} catch (Exception e) {
						e.printStackTrace();
					}
				return null;
			}
		});
	}
	
	public static IniUnit createAdequateIniUnit(IFile file) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<? extends IniUnit> cls = iniUnitClassForResource(file);
		if (cls == null)
			return null;
		Constructor<? extends IniUnit> ctor = cls.getConstructor(Object.class);
		IniUnit result = ctor.newInstance(file);
		return result;
	}

	private static Map<String, Class<? extends IniUnit>> INIREADER_CLASSES = ArrayUtil.map(false, new Object[] {
		Core.id("scenariocfg")  , ScenarioUnit.class, //$NON-NLS-1$
		Core.id("actmap")       , ActMapUnit.class, //$NON-NLS-1$
		Core.id("defcore")      , DefCoreUnit.class, //$NON-NLS-1$
		Core.id("particle")     , ParticleUnit.class, //$NON-NLS-1$
		Core.id("material")     , MaterialUnit.class, //$NON-NLS-1$
		Core.id("plrcontroldef"), PlayerControlsUnit.class, //$NON-NLS-1$
		Core.id("foldermap")    , FolderMapUnit.class,
		Core.id("teamsdef")     , TeamsUnit.class
	});

	/**
	 * Returns the {@link IniUnit} class that is best suited to parsing the given ini file
	 * @param resource the ini file to return an IniUnit class for
	 * @return the IniUnit class or null if no suitable one could be found
	 */
	public static Class<? extends IniUnit> iniUnitClassForResource(IResource resource) {
		try {
			IContentType contentType = resource.getProject().getContentTypeMatcher().findContentTypeFor(resource.getName());
			if (contentType == null)
				return null;
			Class<? extends IniUnit> cls = INIREADER_CLASSES.get(contentType.getId());
			return cls != null ? cls : null;
		} catch (CoreException ce) { 
			return null;
		}
	}
	
	@Override
	public void validate() {
		// don't bother letting items complain if errors shouldn't be shown anyway (in linked groups)
		if (C4GroupItem.groupItemBackingResource(iniFile) != null)
			return;
		try {
			iniFile.deleteMarkers(Core.MARKER_C4SCRIPT_ERROR, true, 0);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		for (IniSection sec : this.sectionsList)
			sec.validate();
	}
	
	@Override
	public Engine engine() {
		ClonkProjectNature nature = ClonkProjectNature.get(resource());
		if (nature != null)
			return nature.index().engine();
		else {
			CustomizationNature customizationNature = CustomizationNature.get(resource().getProject());
			if (customizationNature != null)
				for (IResource r = resource(); r != customizationNature.getProject(); r = r.getParent())
					if (r.getParent() == customizationNature.getProject())
						return Core.instance().loadEngine(r.getName());
		}
		return super.engine();
	}
	
	@Override
	public Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		if ((mask & OTHER) != 0)
			return this.sectionsList;
		else
			return NO_SUB_DECLARATIONS;
	}

	@Override
	public void writeTextRepresentation(Writer writer, int indentation) throws IOException {
		this.save(writer);
	}

	@Override
	public String key() {
		return name();
	}
	
	@Override
	public String name() {
		String n = super.name();
		if (n == null) {
			if (iniFile != null)
				n = iniFile.getName();
			else
				n = getClass().getName();
			setName(n);
		}
		return n;
	}
	
	@Override
	public String infoText(IIndexEntity context) {
		return String.format(
			INFO_FORMAT,
			this.defaultName, this.iniFile().getProjectRelativePath().toOSString()
		);
	}

}
