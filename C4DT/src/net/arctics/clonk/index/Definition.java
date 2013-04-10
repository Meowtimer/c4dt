package net.arctics.clonk.index;

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
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.IProplistDeclaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.inireader.CategoriesValue;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

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
	protected Definition(Index index, ID id, String name) {
		super(index);
		this.id = id;
		this.name = name;
	}

	public String infoTextIncludingIDAndName() {
		return id() != null ? String.format(Messages.DefinitionIDWithName, name(), id().toString()) : name();
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
	public void setId(ID newId) {
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
	protected Declaration representingDeclaration(String name, FindDeclarationInfo info) {
		final Class<?> cls = info.declarationClass;
		boolean variableRequired = false;
		if (
			cls == null ||
			cls == Definition.class ||
			(engine() != null && engine().settings().definitionsHaveProxyVariables && (variableRequired = Variable.class.isAssignableFrom(cls)))
		)
			if (id != null && id.stringValue().equals(name))
				return variableRequired ? this.proxyVar() : this;
		return null;
	}

	private static Pattern langNamePairPattern = Pattern.compile("(..):(.*)"); //$NON-NLS-1$

	public void readNames(String namesText) throws IOException {
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

	public Map<String, String> localizedNames() {
		return localizedNames;
	}

	@Override
	public boolean matchedBy(Matcher matcher) {
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
	public  boolean gatherIncludes(Index contextIndex, Object origin, final Collection<Script> set, final int options) {
		if (!super.gatherIncludes(contextIndex, origin, set, options))
			return false;
		final Scenario originScenario = origin instanceof Script
			? ((Script)origin).scenario()
				: origin instanceof IHasRelatedResource
			? Scenario.containingScenario(((IHasRelatedResource)origin).resource())
				: null;
		if ((options & GatherIncludesOptions.NoAppendages) == 0)
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
		return true;
	}

	public synchronized MetaDefinition metaDefinition() {
		if (metaDefinition == null)
			metaDefinition = new MetaDefinition(this);
		return metaDefinition;
	}

	@Override
	public IType simpleType() { return PrimitiveType.OBJECT; }

	private static class ProxyVarSaveReplacement implements ISerializationResolvable, Serializable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final Definition definition;
		public ProxyVarSaveReplacement(Definition definition) {
			super();
			this.definition = definition;
		}
		@Override
		public Object resolve(Index index) { return definition != null ? definition.proxyVar() : null; }
	}

	/**
	 * ProxyVar that is being referenced by {@link AccessVar} expressions (sort of a hack to support long definition names, while for ClonkRage, {@link IDLiteral} is still used)
	 * @author madeen
	 *
	 */
	public final class ProxyVar extends Variable implements IReplacedWhenSaved {
		public ProxyVar() { super(Definition.this.name(), Scope.STATIC); }
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		@Override
		public String name() { return id().stringValue(); }
		@Override
		public void setName(String name) { setId(ID.get(name)); }
		@Override
		public Declaration parentDeclaration() { return Definition.this; }
		@Override
		public Structure topLevelStructure() { return Definition.this; }
		@Override
		public String infoText(IIndexEntity context) { return Definition.this.infoText(context); }
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
		public Object saveReplacement(Index context) { return new ProxyVarSaveReplacement(definition()); }
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

	public Definition(Index index, ID id, String name, IContainer container) {
		this(index, id, name);
		try {
			setDefinitionFolder(container);
		} catch (final CoreException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * The member <tt>Script.c</tt>
	 * @return IFile object of <tt>Script.c</tt> file or null if it does not exist
	 */
	@Override
	public IStorage source() { return scriptFile; }
	@Override
	public void setScriptFile(IFile f) { scriptFile = f; }
	public void setDefCoreFile(IFile defCoreFile) { this.defCoreFile = defCoreFile; }
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
	public void setDefinitionFolder(IContainer folder) throws CoreException {
		if (Utilities.eq(folder, definitionFolder))
			return;
		if (definitionFolder != null && definitionFolder.exists())
			definitionFolder.setSessionProperty(Core.FOLDER_DEFINITION_REFERENCE_ID, null);
		// on setObjectFolder(null): don't actually set objectFolder to null, so ILatestDeclarationVersionProvider machinery still works
		// (see ClonkIndex.getLatestVersion)
		definitionFolder = folder;
		if (folder != null) {
			scriptFile = as(Utilities.findMemberCaseInsensitively(definitionFolder, "Script.c"), IFile.class);
			defCoreFile = as(Utilities.findMemberCaseInsensitively(folder, "DefCore.txt"), IFile.class);
			folder.setSessionProperty(Core.FOLDER_DEFINITION_REFERENCE_ID, this);
			if (id() != null)
				folder.setPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID, id().stringValue());
			else
				folder.setPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID, null);
			relativePath = folder.getProjectRelativePath().toPortableString();
			//index = ClonkProjectNature.get(definitionFolder).getIndex();
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
	public static Definition definitionCorrespondingToFolder(IContainer folder) {
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
	public boolean refreshDefinitionFolderReference(IProject project) {
		final IPath projectPath = new Path(this.relativePath);
		final IResource res = project.findMember(projectPath);
		if (res instanceof IContainer) {
			try {
				this.setDefinitionFolder((IContainer)res);
			} catch (final CoreException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return (name() + (id != null && id != ID.NULL ? " (" + id.toString() + ")" : "")) + " [" + relativePath + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IResource resource() { return definitionFolder(); }

	@Override
	public String infoText(IIndexEntity context) {
		return String.format(INFO_TEXT_TEMPLATE, name(), super.infoText(context), definitionFolder().getFullPath().toOSString());
	}

	/**
	 * Process a file contained in the Definition folder that won't be read into a separate {@link Structure} object.<br/>
	 * At the moment, this effectively only applies to Names.txt.
	 * @param file The (sufficiently simple to not be represented as a {@link Structure}) file to process to add additional information to the Definition
	 * @throws IOException
	 * @throws CoreException
	 */
	public void processDefinitionFolderFile(IFile file) throws IOException, CoreException {
		if (file.getName().equalsIgnoreCase("Names.txt"))
			readNames(StreamUtil.stringFromFileDocument(file));
	}

	@Override
	public boolean isAdHoc() {
		return false;
	}

	@Override
	public List<Variable> components(boolean includeAdhocComponents) {
		return this.definedVariables;
	}

	@Override
	public Variable addComponent(Variable variable, boolean adhoc) {
		final Variable v = this.findVariable(variable.name());
		if (v != null)
			return v;
		addDeclaration(variable);
		return variable;
	}

	@Override
	public Variable findComponent(String declarationName) {
		return findVariable(declarationName);
	}

	@Override
	public IProplistDeclaration prototype() {
		return null;
	}

	@Override
	public String typeName(boolean special) {
		return special ? name() : id != null ? id.stringValue() : PrimitiveType.OBJECT.typeName(false);
	}

	/**
	 * Return the category of the definition as specified in its DefCore.txt file.
	 * @return
	 */
	public CategoriesValue category() {
		final DefCoreUnit defCore = defCore();
		final ComplexIniEntry category = defCore != null ? as(defCore.itemInSection("DefCore", "Category"), ComplexIniEntry.class) : null;
		return category != null ? as(category.value(), CategoriesValue.class) : null;
	}

	public boolean categorySet(String category) {
		final CategoriesValue cat = category();
		return cat != null && cat.constants() != null && cat.constants().contains(category);
	}
	
	@Override
	public String qualifiedName() { return id().stringValue(); }

	static {
		if (!Core.instance().runsHeadless()) {
			Core.instance().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(ClonkPreferences.PREFERRED_LANGID)) {
						final Sink<Definition> sink = new Sink<Definition>() {
							@Override
							public void receivedObject(Definition item) {
								item.chooseLocalizedName();
							}
						};
						for (final IProject proj : ClonkProjectNature.clonkProjectsInWorkspace())
							ClonkProjectNature.get(proj).index().allDefinitions(sink);
					}
				}
			});
		}
	}
}
