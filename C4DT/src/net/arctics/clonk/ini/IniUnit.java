package net.arctics.clonk.ini;

import static net.arctics.clonk.util.Utilities.as;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
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
public class IniUnit extends IniSection implements IHasChildren, ITreeNode, IniItem {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private static final String INFO_FORMAT = "%s - %s";

	/**
	 * Input. May be {@link IFile} or {@link String}.
	 */
	protected Object input;

	public String defaultName() { return file().getParent().getName(); }

	/**
	 * Creates an IniReader that reads ini information from a project file
	 * @param file the file
	 */
	public IniUnit(final Object input) { super(null); this.input = input; }

	public void save(final ASTNodePrinter writer, final boolean discardEmptySections) {
		sections().forEach(new Consumer<IniSection>() {
			boolean started = false;
			@Override
			public void accept(IniSection section) {
				if (started)
					writer.append('\n');
				if (!discardEmptySections || section.hasPersistentItems()) {
					started = true;
					section.print(writer, -1);
				}
			}
		});
	}

	public void save(final boolean discardEmptySections) {
		if (file() != null)
			Core.instance().performActionsOnFileDocument(file(), document -> {
				final StringWriter writer = new StringWriter();
				save(new AppendableBackedExprWriter(writer), discardEmptySections);
				document.set(writer.toString());
				return null;
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
	public void setFile(final IFile file) { input = file; }

	/**
	 * Checks whether this section name is valid.<br>
	 * Default implementation consults the configuration returned from getConfiguration() to determine if the section is valid.
	 * @param name
	 * @return <tt>true</tt> if valid
	 */
	protected boolean isSectionNameValid(final String name, final IniSection parentSection) {
		if (parentSection != null)
			return parentSection.definition() == null || parentSection.definition().hasSection(name);
		else {
			final IniConfiguration conf = configuration();
			return conf == null || conf.hasSection(name);
		}
	}

	static Object createEntryValueFromString(final Class<?> type, String value, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
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
	protected IniEntry validateEntry(final IniEntry entry, final IniSection section, final boolean modifyMarkers) throws IniParserException {
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

	public IniSection requestSection(final String name, final IniSectionDefinition sectionData) {
		IniSection result = as(map.get(name), IniSection.class);
		if (result == null) {
			result = new IniSection(name);
			result.setDefinition(sectionData);
			return addDeclaration(result);
		}
		else
			return result;
	}

	protected IniSectionDefinition sectionDataFor(final IniSection section, final IniSection parentSection) {
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

	public IniSection sectionWithName(final String name, final boolean create) {
		IniSection s = as(map.get(name), IniSection.class);
		if (s == null && create) {
			final IniSection section = new IniSection(new SourceLocation(-1, -1), name);
			section.setParent(null != null ? null : this);
			s = section;
			s.setDefinition(sectionDataFor(s, null));
		}
		return s;
	}

	public IniSection sectionMatching(final Predicate<IniSection> predicate) {
		return sections().filter(predicate).findFirst().orElse(null);
	}

	public IniItem itemInSection(final String section, final String entry) {
		final IniSection s = as(map.get(section), IniSection.class);
		return s != null ? s.itemByKey(entry) : null;
	}

	public IniEntry entryInSection(final String section, final String entry) {
		final IniItem item = itemInSection(section, entry);
		return item instanceof IniEntry ? (IniEntry)item : null;
	}

	@Override
	public boolean hasChildren() {
		return !map.isEmpty();
	}

	@Override
	public void addChild(final ITreeNode node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<? extends IniItem> childCollection() {
		return list;
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
	public boolean subNodeOf(final ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	@Override
	public IResource resource() {
		return file();
	}

	public String sectionToString(final IniSection section) {
		return "["+section.name()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IniSection sectionAtOffset(int offset) {
		final IniSection s = super.sectionAtOffset(offset);
		return s == this ? null : s;
	}

	@Override
	public Object[] subDeclarationsForOutline() {
		return this.children();
	}

	@Override
	public Declaration findLocalDeclaration(final String declarationName,
			final Class<? extends Declaration> declarationClass) {
		if (declarationClass.isAssignableFrom(IniSection.class))
			return findDeclaration(declarationName);
		return null;
	}

	@Override
	public Declaration findDeclaration(final String declarationName) {
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
		Structure.registerStructureFactory((resource, duringBuild) -> {
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
		});
	}

	public static IniUnit createAdequateIniUnit(final IFile file) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
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
	public static Class<? extends IniUnit> iniUnitClassForResource(final IResource resource) {
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
	public void validate(final Markers markers) throws ProblemException {
		// don't bother letting items complain if errors shouldn't be shown anyway (in linked groups)
		if (C4GroupItem.groupItemBackingResource(file()) != null)
			return;
		try {
			file().deleteMarkers(Core.MARKER_C4SCRIPT_ERROR, true, 0);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
		sections().forEach(sec -> {
			try { sec.validate(markers); }
			catch (final ProblemException prob) {}
		});
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

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends Declaration> subDeclarations(final Index contextIndex, final int mask) {
		if ((mask & DeclMask.IMPLICIT) != 0)
			return (List<? extends Declaration>) this.list;
		else
			return Collections.emptyList();
	}

	@Override
	public void doPrint(final ASTNodePrinter writer, final int indentation) {
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
	public String infoText(final IIndexEntity context) {
		return String.format(
			INFO_FORMAT,
			this.defaultName(), this.file().getProjectRelativePath().toOSString()
		);
	}

	@Override
	public <T extends Declaration> T addDeclaration(final T declaration) {
		if (declaration instanceof IniSection)
			return super.addDeclaration(declaration);
		else
			throw new IllegalArgumentException();
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	public void commit(final Object object) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		this.sections().forEach(section -> section.commit(object, true));
	}

	public void parseAndCommitTo(final Object obj) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		try {
			new IniUnitParser(this).parse(false);
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		commit(obj);
	}

	public void readObjectFields(final Object object, final Object defaults) throws IllegalAccessException {
		for (final Field f : object.getClass().getFields()) {
			IniField annot;
			if ((annot = f.getAnnotation(IniField.class)) != null) {
				final String category = IniData.category(annot, object.getClass());
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

	public <T> T complexValue(final String path, final Class<T> cls) {
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

	@Override
	public ASTNode[] subElements() { return sections().toArray(l -> new ASTNode[l]); }
	@Override
	public void setSubElements(ASTNode[] elms) {
		clear();
		Arrays.stream(elms)
			.filter(e -> e instanceof Declaration)
			.map(e -> (Declaration)e)
			.forEach(this::addDeclaration);
	}

}
