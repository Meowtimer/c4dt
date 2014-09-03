package net.arctics.clonk.ini;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.attempt;
import static net.arctics.clonk.util.Utilities.block;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.walk;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Pair;
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
		sections().forEach(section -> {
			if (!discardEmptySections || section.hasPersistentItems()) {
				section.print(writer, -1);
				writer.append('\n');
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
		final String value_ = defaulting(value, "");
		return
			type.equals(IDLiteral.class) ? new IDLiteral(ID.get(value_)) :
			type.equals(String.class) ? value_ :
			IniEntryValue.class.isAssignableFrom(type) ? block(() -> {
				try {
					final IniEntryValue obj = ((IniEntryValue)type.newInstance());
					obj.setInput(value_, entryData, context);
					return obj;
				} catch (InstantiationException | IllegalAccessException | IniParserException e) {
					//e.printStackTrace();
					return null;
				}
			}) :
			type.isEnum() ? Utilities.enumValueFromString(type, value_) :
			null;
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
			throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownOption, entry.key()), entry.start(), entry.key().length() + entry.start(), null);
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
					e.offsets(
						entry.start() + key.length() + 1,
						entry.start() + key.length() + 1 + value.length()
					);
				}
				throw e;
			}
		} else
			throw new IniParserException(IMarker.SEVERITY_ERROR, String.format("No definition for ini entry '%s'", entry.key()));
	}

	public IniSection sectionWithName(final String name, final boolean create, final IniSectionDefinition sectionData) {
		return as(defaulting(map.get(name), create ? () -> {
			final IniSection s = new IniSection(new SourceLocation(-1, -1), name);
			s.setDefinition(defaulting(sectionData, () -> sectionDataFor(s, null)));
			return addDeclaration(s);
		} : () -> null), IniSection.class);
	}

	protected IniSectionDefinition sectionDataFor(final IniSection section, final IniSection parentSection) {
		return parentSection != null
			? parentSection.definition() != null
				? block(() -> {
					final IniDataBase dataItem = parentSection.definition().entryForKey(section.name());
					return dataItem instanceof IniSectionDefinition ? (IniSectionDefinition)dataItem : null;
				})
				: null
			: configuration() != null
				? configuration().sections().get(section.name())
				: null;
	}

	protected void startParsing() {}
	protected void endParsing() {}

	protected String configurationName() { return null; }

	public IniConfiguration configuration() {
		final String confName = configurationName();
		return confName != null && engine() != null && engine().iniConfigurations() != null
			? engine().iniConfigurations().configurationFor(confName)
			: null;
	}

	public IniSection sectionMatching(final Predicate<IniSection> predicate) {
		return sections().filter(predicate).findFirst().orElse(null);
	}

	public IniItem itemInSection(final String section, final String entry) {
		final IniSection s = as(map.get(section), IniSection.class);
		return s != null ? s.item(entry) : null;
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
		final IniItem nameEntry = section.item(nameEntryName(section));
		if (nameEntry instanceof IniEntry) {
			String val = ((IniEntry) nameEntry).stringValue();
			val = StringTbl.evaluateEntries(this, val, true).evaluated;
			return "["+val+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		} else
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
		return sectionWithName(declarationName, false, null);
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
		Core.id("scenariocfg")       , ScenarioUnit.class, //$NON-NLS-1$
		Core.id("actmap")            , ActMapUnit.class, //$NON-NLS-1$
		Core.id("defcore")           , DefCoreUnit.class, //$NON-NLS-1$
		Core.id("particle")          , ParticleUnit.class, //$NON-NLS-1$
		Core.id("material")          , MaterialUnit.class, //$NON-NLS-1$
		Core.id("plrcontroldef")     , PlayerControlsUnit.class, //$NON-NLS-1$
		Core.id("foldermap")         , FolderMapUnit.class,
		Core.id("teamsdef")          , TeamsUnit.class,
		Core.id("problemhandlingmap"), ProblemHandlingMap.Unit.class
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
		return nature != null ? nature.index().engine() : block(() -> {
			final CustomizationNature custo = CustomizationNature.get(resource().getProject());
			return defaulting(custo != null
				? walk(resource(), IResource::getParent)
					.filter(r -> r.getParent() == custo.getProject())
					.map(IResource::getName).map(Core.instance()::loadEngine)
					.findFirst().orElse(null)
				: null,
				() -> super.engine()
			);
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends Declaration> subDeclarations(final Index contextIndex, final int mask) {
		return (mask & DeclMask.IMPLICIT) != 0 ? (List<? extends Declaration>) this.list : Collections.emptyList();
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
			final IniField annot = f.getAnnotation(IniField.class);
			if (annot != null) {
				final String category = IniData.category(annot, object.getClass());
				if (defaults != null && Utilities.eq(f.get(object), f.get(defaults)))
					continue;
				final IniSectionDefinition dataSection = configuration().sections().get(category);
				if (dataSection != null) {
					final IniDataBase dataItem = dataSection.entryForKey(f.getName());
					if (dataItem instanceof IniEntryDefinition) {
						final IniEntryDefinition entry = (IniEntryDefinition) dataItem;
						final Object value = f.getType() == entry.entryClass() ? f.get(object) : null;
						final Object cvalue = defaulting(value, () -> {
							final Constructor<?> ctor = block(() -> {
								try {
									return entry.entryClass().getConstructor(f.getType());
								} catch (final SecurityException | NoSuchMethodException e) {
									return null;
								}
							});
							return ctor != null ? attempt(() -> ctor.newInstance(f.get(object)), Exception.class, e -> {}) : null;
						});
						if (cvalue != null) {
							final IniSection section = this.sectionWithName(category, true, dataSection);
							final IniEntry complEntry = new IniEntry(0, 0, f.getName(), cvalue);
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
			section = section != null ? as(section.item(p[i]), IniSection.class) : this.sectionWithName(p[i], false, null);
			if (section == null)
				return null;
		}
		final IniEntry entry = section != null ? as(section.item(p[p.length-1]), IniEntry.class) : null;
		return entry != null ? as(entry.value(), cls) : null;
	}

	@Override
	public ASTNode[] subElements() { return sections().toArray(l -> new ASTNode[l]); }
	@Override
	public void setSubElements(ASTNode[] elms) {
		clear();
		ofType(stream(elms), Declaration.class).forEach(this::addDeclaration);
	}

	public String nameEntryName(final IniSection section) { return null; }

	public Predicate<IniSection> nameMatcherPredicate(final String value) {
		return section -> {
			final IniItem entry = section.item(nameEntryName(section));
			return (entry instanceof IniEntry && ((IniEntry)entry).stringValue().equals(value));
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(final T from) {
		if (from instanceof IniSection) {
			final IniSection section = (IniSection) from;
			final IniEntry entry = (IniEntry) section.item(nameEntryName(section.parentSection()));
			return entry != null ? (T) sectionMatching(nameMatcherPredicate(entry.stringValue())) : null;
		} else
			return null;
	}

	public interface INode<T> {
		Stream<T> elements();
		Stream<Pair<String, Object>> attributes();
		String name();
	}

	static class Node<T> implements INode<T> {
		final Function<INode<T>, T> unamplifier;
		final IniSection section;
		public Node(Function<INode<T>, T> unamplifier, IniSection section) {
			super();
			this.unamplifier = unamplifier;
			this.section = section;
		}
		@Override
		public Stream<T> elements() {
			return ofType(section.map.values().stream(), IniSection.class).map(s -> new Node<T>(unamplifier, s)).map(unamplifier);
		}
		@Override
		public Stream<Pair<String, Object>> attributes() {
			return ofType(section.map.values().stream(), IniEntry.class).map(e -> Pair.pair(e.key(), e.value()));
		}
		@Override
		public String name() {
			return section.name();
		}
	}

	public static <T> IniUnit from(T thing, Function<T, INode<T>> amplifier) {
		final IniUnit unit = new IniUnit(null);
		class Constructor {
			public void construct(IniSection parent, T thing) {
				final INode<T> n = amplifier.apply(thing);
				final IniSection s = new IniSection(n.name());
				n.attributes().map(kv -> new IniEntry(-1, -1, kv.first(), kv.second())).forEach(s::putEntry);
				n.elements().forEach(t -> construct(s, t));
			}
		}
		new Constructor().construct(unit, thing);
		return unit;
	}

	public <T> T to(Function<INode<T>, T> unamplifier) {
		return unamplifier.apply(new Node<T>(unamplifier, this));
	}

	public static IniUnit fromFile(IFile file) {
		final IniUnit unit = new IniUnit(file);
		try {
			new IniUnitParser(unit).parse(false);
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		return unit;
	}

}
