package net.arctics.clonk.index;

import static java.lang.String.format;
import static net.arctics.clonk.util.Utilities.as;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.FindDeclarationInfo;
import net.arctics.clonk.c4script.IProplistDeclaration;
import net.arctics.clonk.c4script.LocalizedScript;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SystemScript;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.ini.CategoriesValue;
import net.arctics.clonk.ini.DefCoreUnit;
import net.arctics.clonk.ini.IniEntry;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A Clonk object definition.
 * @author madeen
 *
 */
public class Definition extends Script implements IProplistDeclaration {

	/**
	 * Template to construct the info text of an object definition from
	 */
	public static final String INFO_TEXT_TEMPLATE = Messages.DefinitionInfoTextTemplate;

	/**
	 * localized name of the object; key is language code like "DE" and "US"
	 */
	private Map<String, String> localizedNames;

	/**
	 * id of the object
	 */
	protected ID id;

	private transient MetaDefinition metaDefinition;

	/**
	 * Creates a new C4Object
	 * @param id C4ID (e.g. CLNK)
	 * @param name human-readable name
	 */
	protected Definition(final Index index, final ID id, final String name) {
		super(index);
		this.id = id;
		this.name = name;
	}

	public String infoTextIncludingIDAndName() {
		return id() != null ? String.format(Messages.DefinitionIDWithName, localizedName(), id().toString()) : localizedName();
	}

	/**
	 * The id of this object. (e.g. CLNK)
	 * @return the id
	 */
	public ID id() {
		return id;
	}

	/**
	 * Sets the id property of this object.
	 * This method does not perform necessary changes to DefCore.txt.
	 * @param newId
	 */
	public void setId(final ID newId) {
		if (id.equals(newId))
			return;
		final Index index = this.index();
		index.removeDefinition(this);
		id = newId;
		index.addDefinition(this);

		if (definitionFolder != null)
			try {
				definitionFolder.setPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID, id().stringValue());
			} catch (final CoreException e) {
				e.printStackTrace();
			}
	}

	@Override
	protected Declaration representingDeclaration(final String name, final FindDeclarationInfo info) {
		final Class<?> cls = info.declarationClass;
		final boolean variableRequired = cls != null && Variable.class.isAssignableFrom(cls);
		final Engine ngn = engine();
		final boolean clsCompatible =
			cls == null || cls == Definition.class ||
			(variableRequired && ngn != null && ngn.settings().definitionsHaveProxyVariables);
		return clsCompatible && id != null && id.stringValue().equals(name)
			? (variableRequired ? this.proxyVar() : this)
			: null;
	}

	private static Pattern langNamePairPattern = Pattern.compile("(..):(.*)"); //$NON-NLS-1$

	public void readNames(final String namesText) throws IOException {
		final Matcher matcher = langNamePairPattern.matcher(namesText);
		if (localizedNames == null)
			localizedNames = new HashMap<String, String>();
		else
			localizedNames.clear();
		while (matcher.find())
			localizedNames.put(matcher.group(1), matcher.group(2));
		chooseLocalizedName();
	}

	public void chooseLocalizedName() {
		if (localizedNames != null) {
			final String preferredName = localizedNames.get(ClonkPreferences.languagePref());
			if (preferredName != null)
				setName(preferredName);
		}
	}

	@Override
	public void deriveInformation() {
		super.deriveInformation();
		if (definitionFolder != null)
			try {
				for (final IResource r : definitionFolder.members())
					if (r instanceof IFile)
						processDefinitionFolderFile((IFile)r);
			} catch (CoreException | IOException e) {
				e.printStackTrace();
			}
	}

	public Map<String, String> localizedNames() {
		return localizedNames;
	}

	@Override
	public boolean matchedBy(final Matcher matcher) {
		if (super.matchedBy(matcher))
			return true;
		if (id() != null && matcher.reset(id().stringValue()).lookingAt())
			return true;
		if (localizedNames != null)
			for (final String key : localizedNames.keySet()) {
				final String value = localizedNames.get(key);
				if (matcher.reset(value).lookingAt())
					return true;
			}
		return false;
	}

	@Override
	public  boolean gatherIncludes(final Index contextIndex, final Script origin, final Collection<Script> set, final int options) {
		if (!super.gatherIncludes(contextIndex, origin, set, options))
			return false;
		if ((options & GatherIncludesOptions.NoAppendages) == 0) {
			try {
				final IContainer defFolder = definitionFolder();
				if (defFolder != null)
					for (final IResource r : defFolder.members()) {
						final LocalizedScript ls = as(SystemScript.pinned(r, false), LocalizedScript.class);
						if (ls != null)
							set.add(ls);
					}
			} catch (final CoreException e) {
				e.printStackTrace();
			}
			final Scenario originScenario = origin instanceof Script
				? origin.scenario() : origin instanceof IHasRelatedResource
				? Scenario.containingScenario(((IHasRelatedResource)origin).resource()) : null;
			for (final Index i : contextIndex.relevantIndexes()) {
				final List<Script> appendages = i.appendagesOf(Definition.this);
				if (appendages != null)
					for (final Script s : appendages) {
						final Scenario scriptScenario = s.scenario();
						if (scriptScenario != null && originScenario != scriptScenario)
							continue;// scenario boundary; ignore
						if ((options & GatherIncludesOptions.Recursive) == 0)
							set.add(s);
						else
							s.gatherIncludes(i, origin, set, options | GatherIncludesOptions.NoAppendages);
					}
			}
		}
		return true;
	}

	public synchronized MetaDefinition metaDefinition() {
		if (metaDefinition == null)
			metaDefinition = new MetaDefinition(this);
		return metaDefinition;
	}

	@Override
	public IType simpleType() { return PrimitiveType.OBJECT; }

	private static class ProxyVarSaveReplacement implements IDeserializationResolvable, Serializable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final Definition definition;
		public ProxyVarSaveReplacement(final Definition definition) {
			super();
			this.definition = definition;
		}
		@Override
		public Object resolve(final Index index, final IndexEntity deserializee) { return definition != null ? definition.proxyVar() : null; }
	}

	/**
	 * ProxyVar that is being referenced by {@link AccessVar} expressions (sort of a hack to support long definition names, while for ClonkRage, {@link IDLiteral} is still used)
	 * @author madeen
	 *
	 */
	public final class ProxyVar extends Variable implements IReplacedWhenSaved {
		public ProxyVar() { super(Scope.STATIC, Definition.this.id().stringValue()); }
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		@Override
		public String name() { return id().stringValue(); }
		@Override
		public void setName(final String name) { setId(ID.get(name)); }
		@Override
		public Declaration parentDeclaration() { return Definition.this; }
		@Override
		public Structure topLevelStructure() { return Definition.this; }
		@Override
		public String infoText(final IIndexEntity context) { return Definition.this.infoText(context); }
		@Override
		public IType type() { return Definition.this.metaDefinition(); }
		@Override
		public boolean staticallyTyped() { return true; }
		public final Definition definition() { return Definition.this; }
		@Override
		public Index index() { return Definition.this.index(); }
		@Override
		public Script script() { return Definition.this.script(); }
		@Override
		public Object saveReplacement(final Index context) { return new ProxyVarSaveReplacement(definition()); }
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	protected transient IContainer definitionFolder;
	protected transient IFile scriptFile;
	protected transient IFile defCoreFile;

	protected String relativePath;
	private transient ProxyVar proxyVar;

	@Override
	public Object additionalEntityIdentificationToken() {
		return new Pair<ID, String>(id(), relativePath);
	}

	/**
	 * Helper variable used for long-id definitions.
	 */
	public ProxyVar proxyVar() {
		if (engine() != null && !engine().settings().definitionsHaveProxyVariables)
			return proxyVar = null;
		if (proxyVar == null)
			proxyVar = new ProxyVar();
		return proxyVar;
	}

	public Definition(final Index index, final ID id, final String name, final IContainer container) {
		this(index, id, name);
		setDefinitionFolder(container);
	}

	@Override
	public String name() { return id().stringValue(); }
	public String localizedName() { return name; }

	/**
	 * The member <tt>Script.c</tt>
	 * @return IFile object of <tt>Script.c</tt> file or null if it does not exist
	 */
	@Override
	public IStorage source() { return scriptFile; }
	@Override
	public void setScriptFile(final IFile f) { scriptFile = f; }
	public void setDefCoreFile(final IFile defCoreFile) { this.defCoreFile = defCoreFile; }
	/**
	 * Return DefCore.txt file of this Definition
	 * @return The DefCore.txt file or null if it does not exist for mysterious reasons
	 */
	public IFile defCoreFile() { return defCoreFile; }

	public DefCoreUnit defCore() {
		return as(Structure.pinned(defCoreFile, true, false), DefCoreUnit.class);
	}

	/**
	 * Set the folder the Definition was read from. This will take care of setting session properties on the folder.
	 * @param folder The folder
	 * @throws CoreException
	 */
	public void setDefinitionFolder(final IContainer folder) {
		if (Utilities.eq(folder, definitionFolder))
			return;
		try {
			if (definitionFolder != null && definitionFolder.exists())
				definitionFolder.setSessionProperty(Core.FOLDER_DEFINITION_REFERENCE_ID, null);
			// on setObjectFolder(null): don't actually set objectFolder to null, so ILatestDeclarationVersionProvider machinery still works
			// (see ClonkIndex.getLatestVersion)
			definitionFolder = folder;
			if (folder != null) {
				scriptFile = Script.findScriptFile(definitionFolder);
				defCoreFile = as(Utilities.findMemberCaseInsensitively(folder, "DefCore.txt"), IFile.class);
				folder.setSessionProperty(Core.FOLDER_DEFINITION_REFERENCE_ID, this);
				folder.setPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID, id() != null ? id().stringValue() : null);
				relativePath = folder.getProjectRelativePath().toPortableString();
			}
		} catch (final CoreException ce) {
			ce.printStackTrace();
		}
	}

	/**
	 * The folder the Definition was declared in. Will return null if no folder has been assigned to this Definition yet or if this Definition object denotes a no longer recent version of the definition.
	 * @return the folder object or null due to circumstances listed above
	 */
	public IContainer definitionFolder() {
		return definitionFolder;
	}

	/**
	 * Return the definition that is declared at the given folder.
	 * @param folder The folder to return the Definition object of
	 * @return The Definition object
	 */
	public static Definition at(final IContainer folder) {
		final ProjectIndex index = ProjectIndex.fromResource(folder);
		final Definition obj = index != null ? index.definitionAt(folder) : null;
		// haxxy cleanup: might have been lost by <insert unlikely event>
		if (obj != null)
			obj.definitionFolder = folder;
		return obj;
	}

	/**
	 * Give the definition a chance to refresh its folder reference after having been loaded or some such.
	 * @param project The project passed to the method so the definition has some context from where to get the folder reference
	 * @return Whether refreshing the folder reference was successful
	 */
	public boolean refreshDefinitionFolderReference(final IProject project) {
		final IPath projectPath = new Path(this.relativePath);
		final IResource res = project.findMember(projectPath);
		if (res instanceof IContainer) {
			this.setDefinitionFolder((IContainer)res);
			return true;
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return format(
			"%s %s [%s],",
			localizedName(),
			(id != null && id != ID.NULL ? format("(%s)", id.toString()) : ""),
			relativePath
		);
	}

	@Override
	public IResource resource() { return definitionFolder(); }

	@Override
	public String infoText(final IIndexEntity context) {
		return String.format(INFO_TEXT_TEMPLATE,
			localizedName(),
			super.infoText(context),
			definitionFolder().getFullPath().toOSString()
		);
	}

	/**
	 * Process a file contained in the Definition folder that won't be read into a separate {@link Structure} object.<br/>
	 * At the moment, this effectively only applies to Names.txt.
	 * @param file The (sufficiently simple to not be represented as a {@link Structure}) file to process to add additional information to the Definition
	 * @throws IOException
	 * @throws CoreException
	 */
	public void processDefinitionFolderFile(final IFile file) throws IOException, CoreException {
		if (file.getName().equalsIgnoreCase("Names.txt"))
			readNames(StreamUtil.stringFromFileDocument(file));
	}

	@Override
	public List<Variable> components(final boolean includeAdhocComponents) {
		return this.variables;
	}

	@Override
	public Variable addComponent(final Variable variable, final boolean adhoc) {
		final Variable v = this.findVariable(variable.name());
		if (v != null)
			return v;
		addDeclaration(variable);
		return variable;
	}

	@Override
	public Variable findComponent(final String declarationName) { return findVariable(declarationName); }

	@Override
	public IProplistDeclaration prototype() {
		return null;
	}

	@Override
	public String typeName(final boolean special) {
		final String specialName = this.engine().name().equals("OpenClonk") ? (id != null ? id.stringValue() : null) : localizedName();
		return special && specialName != null ? specialName : PrimitiveType.OBJECT.typeName(false);
	}

	/**
	 * Return the category of the definition as specified in its DefCore.txt file.
	 * @return
	 */
	public CategoriesValue category() {
		final DefCoreUnit defCore = defCore();
		final IniEntry category = defCore != null ? as(defCore.itemInSection("DefCore", "Category"), IniEntry.class) : null;
		return category != null ? as(category.value(), CategoriesValue.class) : null;
	}

	public boolean categorySet(final String category) {
		final CategoriesValue cat = category();
		return cat != null && cat.constants() != null && cat.constants().contains(category);
	}

	@Override
	public String qualifiedName() { return id().stringValue(); }

	static {
		if (!Core.runsHeadless())
			Core.instance().getPreferenceStore().addPropertyChangeListener(event -> {
				if (event.getProperty().equals(ClonkPreferences.PREFERRED_LANGID)) {
					final Sink<Definition> sink = item -> item.chooseLocalizedName();
					for (final IProject proj : ClonkProjectNature.clonkProjectsInWorkspace())
						ClonkProjectNature.get(proj).index().allDefinitions(sink);
				}
			});
	}
}
