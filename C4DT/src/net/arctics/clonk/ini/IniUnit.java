package net.arctics.clonk.ini;

import static net.arctics.clonk.util.Utilities.as;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.AppendableBackedExprWriter;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.CustomizationNature;
import net.arctics.clonk.c4group.C4GroupItem;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ini.IniData.IniConfiguration;
import net.arctics.clonk.ini.IniData.IniDataBase;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.ini.IniData.IniSectionDefinition;
import net.arctics.clonk.parser.Markers;
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
import org.eclipse.jface.text.IDocument;

/**
 * Reads Windows ini style configuration files
 */
public class IniUnit extends Structure implements Iterable<IniSection>, IHasChildren, ITreeNode, IniItem {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private static final String INFO_FORMAT = "%s - %s";

	/**
	 * Input. May be {@link IFile} or {@link String}.
	 */
	protected Object input;

	/**
	 * map to access sections by their name - only useful when sections have different names
	 */
	protected Map<String, IniSection> sectionsMap = new HashMap<String, IniSection>();

	/**
	 * list of all sections regardless of name (for ActMap and similar files)
	 */
	protected List<IniSection> sectionsList = new LinkedList<IniSection>();

	public String defaultName() { return file().getParent().getName(); }

	/**
	 * Creates an IniReader that reads ini information from a project file
	 * @param file the file
	 */
	public IniUnit(final Object input) { this.input = input; }

	public void save(ASTNodePrinter writer, boolean discardEmptySections) {
		boolean started = false;
		for (final IniSection section : sectionsList) {
			if (started)
				writer.append('\n');
			if (!discardEmptySections || section.hasPersistentItems()) {
				started = true;
				section.print(writer, -1);
			}
		}
	}

	public void save(final boolean discardEmptySections) {
		if (file() != null)
			Core.instance().performActionsOnFileDocument(file(), new IDocumentAction<Void>() {
				@Override
				public Void run(IDocument document) {
					final StringWriter writer = new StringWriter();
					save(new AppendableBackedExprWriter(writer), discardEmptySections);
					document.set(writer.toString());
					return null;
				}
			}, true);
		else
			throw new IllegalStateException(String.format("%s has no associated file", toString()));
	}

	/**
	 * Returns the file the configuration was read from
	 * @return the file
	 */
	@Override
	public IFile file() { return as(input, IFile.class); }
	@Override
	public void setFile(IFile file) { input = file; }

	/**
	 * Checks whether this section name is valid.<br>
	 * Default implementation consults the configuration returned from getConfiguration() to determine if the section is valid.
	 * @param name
	 * @return <tt>true</tt> if valid
	 */
	protected boolean isSectionNameValid(String name, IniSection parentSection) {
		if (parentSection != null)
			return parentSection.definition() == null || parentSection.definition().hasSection(name);
		else {
			final IniConfiguration conf = configuration();
			return conf == null || conf.hasSection(name);
		}
	}

	static Object createEntryValueFromString(Class<?> type, String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (type.equals(IDLiteral.class))
			return new IDLiteral(ID.get(value));
		else if (type.equals(String.class))
			return value;
		else if (IniEntryValue.class.isAssignableFrom(type))
			try {
				final IniEntryValue obj = ((IniEntryValue)type.newInstance());
				obj.setInput(value, entryData, context);
				return obj;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		else if (type.isEnum())
			return Utilities.enumValueFromString(type, value);
		else
			return null;
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
		final IniConfiguration configuration = configuration();
		if (configuration == null)
			return entry;
		final IniSectionDefinition sectionConfig = ((IniSection)entry.parentDeclaration()).definition();
		if (sectionConfig == null)
			return entry; // don't throw errors in unknown section
		if (!sectionConfig.hasEntry(entry.key()))
			throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownOption, entry.key()), entry.start(), entry.key().length() + entry.start());
		final IniDataBase dataItem = sectionConfig.entryForKey(entry.key());
		if (dataItem instanceof IniEntryDefinition) {
			final IniEntryDefinition entryConfig = (IniEntryDefinition) dataItem;
			try {
				final Object value = createEntryValueFromString(entryConfig.entryClass(), entry.stringValue(), entryConfig, this);
				return entry.update(value, entryConfig);
			}
			catch (final IniParserException e) { // add offsets and throw through
				// FIXME: whitespace before and after '=' is not taken into account
				if (e.offset() == 0 || e.endOffset() == 0) {
					final String key = entry.key();
					String value = entry.stringValue();
					if (value == null)
						value = ""; //$NON-NLS-1$
					e.setOffset(entry.start() + key.length() + 1);
					e.setEndOffset(entry.start() + key.length() + 1 + value.length());
				}
				throw e;
			}
		} else
			throw new IniParserException(IMarker.SEVERITY_ERROR, String.format("No definition for ini entry '%s'", entry.key()));
	}

	public IniSection requestSection(String name, IniSectionDefinition sectionData) {
		IniSection result = sectionsMap.get(name);
		if (result == null) {
			result = new IniSection(name);
			result.setDefinition(sectionData);
			return addDeclaration(result);
		}
		else
			return result;
	}

	void clear() {
		sectionsList.clear();
		sectionsMap.clear();
	}

	protected IniSectionDefinition sectionDataFor(IniSection section, IniSection parentSection) {
		if (parentSection != null) {
			if (parentSection.definition() != null) {
				final IniDataBase dataItem = parentSection.definition().entryForKey(section.name());
				return dataItem instanceof IniSectionDefinition ? (IniSectionDefinition)dataItem : null;
			}
			else
				return null;
		} else
			return configuration() != null
			? configuration().sections().get(section.name())
					: null;
	}

	protected void startParsing() {}
	protected void endParsing() {}

	protected String configurationName() {
		return null;
	}

	public IniConfiguration configuration() {
		final String confName = configurationName();
		if (confName != null && engine() != null && engine().iniConfigurations() != null)
			return engine().iniConfigurations().configurationFor(confName);
		else
			return null;
	}

	@Override
	public Iterator<IniSection> iterator() {
		return sectionsList.iterator();
	}

	public IniSection sectionWithName(String name, boolean create) {
		IniSection s = sectionsMap.get(name);
		if (s == null && create) {
			final IniSection section = new IniSection(new SourceLocation(-1, -1), name);
			section.setParent(null != null ? null : this);
			s = section;
			s.setDefinition(sectionDataFor(s, null));
		}
		return s;
	}

	public IniSection sectionMatching(IPredicate<IniSection> predicate) {
		return Utilities.itemMatching(predicate, sectionsList);
	}

	public IniItem itemInSection(String section, String entry) {
		final IniSection s = sectionsMap.get(section);
		return s != null ? s.itemByKey(entry) : null;
	}

	public IniEntry entryInSection(String section, String entry) {
		final IniItem item = itemInSection(section, entry);
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
		throw new UnsupportedOperationException();
	}

	@Override
	public List<? extends IniItem> childCollection() {
		return sectionsList;
	}

	@Override
	public String nodeName() {
		return file() != null ? file().getName() : toString();
	}

	@Override
	public ITreeNode parentNode() {
		return null;
	}

	@Override
	public IPath path() {
		return ITreeNode.Default.path(this);
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	@Override
	public IResource resource() {
		return file();
	}

	public String sectionToString(IniSection section) {
		return "["+section.name()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IniSection sectionAtOffset(IniSection parent, int offset) {
		IniSection section = null;
		for (final IniSection sec : parent == null ? ArrayUtil.iterable(this.sections()) : parent.sections()) {
			final int start = sec.start();
			if (start > offset)
				break;
			section = sec;
		}
		return section == null ? parent : sectionAtOffset(section, offset);
	}

	public IniSection sectionAtOffset(int offset) {
		final IniSection sec = sectionAtOffset(null, offset);
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
		return sectionWithName(declarationName, false);
	}

	@Override
	public Structure topLevelStructure() {
		return this;
	}

	@Override
	public String toString() {
		if (file() != null)
			return file().getFullPath().toOSString();
		else
			return super.toString();
	}

	public static void register() {
		Structure.registerStructureFactory(new IStructureFactory() {
			@Override
			public Structure create(IResource resource, boolean duringBuild) {
				if (resource instanceof IFile)
					try {
						final IniUnit unit = createAdequateIniUnit((IFile) resource);
						if (unit != null)
							new IniUnitParser(unit).parse(duringBuild);
						return unit;
					} catch (final Exception e) {
						e.printStackTrace();
					}
				return null;
			}
		});
	}

	public static IniUnit createAdequateIniUnit(IFile file) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		final Class<? extends IniUnit> cls = iniUnitClassForResource(file);
		if (cls == null)
			return null;
		final Constructor<? extends IniUnit> ctor = cls.getConstructor(Object.class);
		final IniUnit result = ctor.newInstance(file);
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
			final IContentType contentType = resource.getProject().getContentTypeMatcher().findContentTypeFor(resource.getName());
			if (contentType == null)
				return null;
			final Class<? extends IniUnit> cls = INIREADER_CLASSES.get(contentType.getId());
			return cls != null ? cls : null;
		} catch (final CoreException ce) {
			return null;
		}
	}

	@Override
	public void validate(Markers markers) throws ProblemException {
		// don't bother letting items complain if errors shouldn't be shown anyway (in linked groups)
		if (C4GroupItem.groupItemBackingResource(file()) != null)
			return;
		try {
			file().deleteMarkers(Core.MARKER_C4SCRIPT_ERROR, true, 0);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
		for (final IniSection sec : this.sectionsList)
			sec.validate(markers);
	}

	@Override
	public Engine engine() {
		final ClonkProjectNature nature = ClonkProjectNature.get(resource());
		if (nature != null)
			return nature.index().engine();
		else {
			final CustomizationNature customizationNature = CustomizationNature.get(resource().getProject());
			if (customizationNature != null)
				for (IResource r = resource(); r != customizationNature.getProject(); r = r.getParent())
					if (r.getParent() == customizationNature.getProject())
						return Core.instance().loadEngine(r.getName());
		}
		return super.engine();
	}

	@Override
	public List<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		if ((mask & DeclMask.IMPLICIT) != 0)
			return this.sectionsList;
		else
			return Collections.emptyList();
	}

	@Override
	public void doPrint(ASTNodePrinter writer, int indentation) {
		this.save(writer, false);
	}

	@Override
	public String key() {
		return name();
	}

	@Override
	public String name() {
		String n = super.name();
		if (n == null) {
			if (file() != null)
				n = file().getName();
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
			this.defaultName(), this.file().getProjectRelativePath().toOSString()
		);
	}

	@Override
	public <T extends Declaration> T addDeclaration(T declaration) {
		if (declaration instanceof IniSection) {
			final IniSection section = (IniSection) declaration;
			declaration.setParent(this);
			sectionsMap.put(section.name(), section);
			sectionsList.add(section);
			return declaration;
		}
		return super.addDeclaration(declaration);
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	public void commit(Object object) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		for (final IniSection section : this.sections())
			section.commit(object, true);
	}

	public void parseAndCommitTo(Object obj) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		try {
			new IniUnitParser(this).parse(false);
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		commit(obj);
	}

	public void readObjectFields(Object object, Object defaults) throws IllegalAccessException {
		for (final Field f : object.getClass().getFields()) {
			IniField annot;
			if ((annot = f.getAnnotation(IniField.class)) != null) {
				final String category = IniUnitParser.category(annot, object.getClass());
				if (defaults != null && Utilities.eq(f.get(object), f.get(defaults)))
					continue;
				final IniSectionDefinition dataSection = configuration().sections().get(category);
				if (dataSection != null) {
					final IniDataBase dataItem = dataSection.entryForKey(f.getName());
					if (dataItem instanceof IniEntryDefinition) {
						final IniEntryDefinition entry = (IniEntryDefinition) dataItem;
						Constructor<?> ctor;
						Object value = f.getType() == entry.entryClass() ? f.get(object) : null;
						if (value == null) {
							try {
								ctor = entry.entryClass().getConstructor(f.getType());
							} catch (final SecurityException e) {
								ctor = null;
							} catch (final NoSuchMethodException e) {
								ctor = null;
							}
							if (ctor != null)
								try {
									value = ctor.newInstance(f.get(object));
								} catch (final Exception e) {
									value = null;
								}
						}
						if (value != null) {
							final IniSection section = this.requestSection(category, dataSection);
							final IniEntry complEntry = new IniEntry(0, 0, f.getName(), value);
							complEntry.setDefinition(entry);
							section.putEntry(complEntry);
						}
					}
				}
			}
		}
	}

	public <T> T complexValue(String path, Class<T> cls) {
		final String[] p = path.split("\\.");
		if (p.length < 2)
			return null;
		IniSection section = null;
		for (int i = 0; i < p.length-1; i++) {
			section = section != null ? as(section.itemByKey(p[i]), IniSection.class) : this.sectionWithName(p[i], false);
			if (section == null)
				return null;
		}
		final IniEntry entry = section != null ? as(section.itemByKey(p[p.length-1]), IniEntry.class) : null;
		return entry != null ? as(entry.value(), cls) : null;
	}

}
