package net.arctics.clonk.index;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;

/**
 * A Clonk object definition.
 * @author madeen
 *
 */
public class Definition extends Script {

	/**
	 * Template to construct the info text of an object definition from
	 */
	public static final String INFO_TEXT_TEMPLATE = Messages.C4Object_InfoTextTemplate;

	/**
	 * localized name of the object; key is language code like "DE" and "US"
	 */
	private Map<String, String> localizedNames;

	/**
	 * id of the object
	 */
	protected ID id;

	/**
	 * Cached picture from Graphics.png
	 */
	private transient Image cachedPicture;
	private transient ConstrainedProplist objectType;

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

	public String idWithName() {
		return id() != null ? String.format(Messages.C4Object_IDWithName, name(), id().toString()) : name();
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
		Index index = this.getIndex();
		index.removeDefinition(this);
		id = newId;
		index.addDefinition(this);
		
		if (definitionFolder != null) {
			try {
				definitionFolder.setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, id().stringValue());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected Declaration representingDeclaration(String name, FindDeclarationInfo info) {
		Class<?> cls = info.getDeclarationClass();
		boolean variableRequired = false;
		if (
				cls == null ||
				cls == Definition.class ||
				(engine() != null && engine().currentSettings().definitionsHaveProxyVariables && (variableRequired = Variable.class.isAssignableFrom(cls)))
		) {
			if (id != null && id.stringValue().equals(name))
				return variableRequired ? this.proxyVar() : this;
		}
		return null;
	}

	private static Pattern langNamePairPattern = Pattern.compile("(..):(.*)"); //$NON-NLS-1$

	public void readNames(String namesText) throws IOException {
		Matcher matcher = langNamePairPattern.matcher(namesText);
		if (localizedNames == null)
			localizedNames = new HashMap<String, String>();
		else
			localizedNames.clear();
		while (matcher.find()) {
			localizedNames.put(matcher.group(1), matcher.group(2));
		}
		chooseLocalizedName();
	}

	public void chooseLocalizedName() {
		if (localizedNames != null) {
			String preferredName = localizedNames.get(ClonkPreferences.getLanguagePref());
			if (preferredName != null)
				setName(preferredName);
		}
	}

	public Map<String, String> localizedNames() {
		return localizedNames;
	}

	@Override
	public boolean nameMatches(Matcher matcher) {
		if (super.nameMatches(matcher))
			return true;
		if (id() != null && matcher.reset(id().stringValue()).lookingAt())
			return true;
		if (localizedNames != null) {
			for (String key : localizedNames.keySet()) {
				String value = localizedNames.get(key);
				if (matcher.reset(value).lookingAt())
					return true;
			}
		}
		return false;
	}

	@Override
	public  boolean gatherIncludes(final Set<IHasIncludes> set, final boolean recursive) {
		if (!super.gatherIncludes(set, recursive))
			return false;
		if (index != null) {
			List<Script> appendages = index.appendagesOf(Definition.this);
			if (appendages != null)
				for (Script s : appendages)
					if (!recursive)
						set.add(s);
					else
						s.gatherIncludes(set, true);
		}
		return true;
	}

	@Override
	protected void finalize() throws Throwable {
		if (cachedPicture != null)
			cachedPicture.dispose();
		super.finalize();
	}

	public Image cachedPicture() {
		return cachedPicture;
	}

	public void setCachedPicture(Image cachedPicture) {
		this.cachedPicture = cachedPicture;
	}
	
	public ConstrainedProplist objectType() {
		if (objectType == null)
			objectType = new ConstrainedProplist(this, ConstraintKind.Exact, true, false);
		return objectType;
	}
	
	@Override
	public IType staticType() {
		return PrimitiveType.ID;
	}
	
	/**
	 * ProxyVar that is being referenced by {@link AccessVar} expressions (sort of a hack to support long definition names, while for ClonkRage, {@link IDLiteral} is still used)
	 * @author madeen
	 *
	 */
	public final class ProxyVar extends Variable {
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		@Override
		public String name() {
			return id().stringValue();
		}
		
		@Override
		public void setName(String name) {
			setId(ID.get(name));
		}

		@Override
		public Declaration getParentDeclaration() {
			return Definition.this;
		}

		@Override
		public Structure getTopLevelStructure() {
			return Definition.this;
		}

		@Override
		public String infoText() {
			return Definition.this.infoText();
		}

		@Override
		public Definition getObjectType() {
			return null;
		}

		@Override
		public IType getType() {
			return Definition.this.objectType();
		}

		@Override
		public boolean typeIsInvariant() {
			return true;
		}
		
		public final Definition definition() {
			return Definition.this;
		}
		
		@Override
		public Index getIndex() {
			return Definition.this.getIndex();
		}
		
		@Override
		public Script getScript() {
			return Definition.this.getScript();
		}
		
	}

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	protected transient IContainer definitionFolder;
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
		if (engine() != null && !engine().currentSettings().definitionsHaveProxyVariables)
			return proxyVar = null;
		if (proxyVar == null)
			proxyVar = new ProxyVar();
		return proxyVar;
	}

	public Definition(Index index, ID id, String name, IContainer container) {
		this(index, id, name);
		try {
			setDefinitionFolder(container);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Resets the name of this object
	 * @param newName
	 * @param persistent not implemented yet - set it to false always
	 */
	public void setName(String newName, boolean persistent) {
		name = newName;
		if (persistent) {
			// TODO edit DefCore.txt
			throw new InvalidParameterException("Parameter 'persistent' is not yet implemented"); //$NON-NLS-1$
		}
	}

	/**
	 * Sets the id property of this object.
	 * This method changes resources when <code>persistent</code> is true. (but that is not implemented yet)
	 * @param newId
	 * @param persistent not implemented yet - set it to false always
	 */
	public void setId(ID newId, boolean persistent) {
		setId(newId);
		if (persistent) {
			//			DefCoreParser defCoreParser = new DefCoreParser(getScriptFile());
			//			IniEntry idEntry = defCoreParser.entryInSection("DefCore", "id");
			//			if (idEntry != null) {
			//				idEntry.setValue(value)
			//			}
			// TODO edit DefCore.txt
			throw new InvalidParameterException("Parameter 'persistent' is not yet implemented"); //$NON-NLS-1$
		}
	}

	/**
	 * The member <tt>Script.c</tt>
	 * @return IFile object of <tt>Script.c</tt> file or null if it does not exist
	 */
	@Override
	public IFile scriptStorage() {
		if (this.definitionFolder == null)
			return null;
		IResource res = Utilities.findMemberCaseInsensitively(this.definitionFolder, "Script.c"); //$NON-NLS-1$
		if (res == null || !(res instanceof IFile)) return null;
		else return (IFile) res;
	}

	@Override
	public String scriptText() {
		try {
			return StreamUtil.stringFromFileDocument(scriptStorage());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Return DefCore.txt file of this Definition
	 * @return The DefCore.txt file or null if it does not exist for mysterious reasons
	 */
	public IFile defCoreFile() {
		IResource res = Utilities.findMemberCaseInsensitively(this.definitionFolder, "DefCore.txt"); //$NON-NLS-1$
		if (res == null || !(res instanceof IFile))
			return null;
		else
			return (IFile) res;
	}

	/**
	 * Removes this object from index.
	 * The file on the harddisk is not deleted. (delete it by IResource.delete(true,null))
	 */
	public void delete() {
		ClonkProjectNature.get(definitionFolder.getProject()).getIndex().removeDefinition(this);
	}

	/**
	 * Set the folder the Definition was read from. This will take care of setting session properties on the folder.
	 * @param folder The folder
	 * @throws CoreException
	 */
	public void setDefinitionFolder(IContainer folder) throws CoreException {
		if (Utilities.objectsEqual(folder, definitionFolder))
			return;
		if (definitionFolder != null && definitionFolder.exists())
			definitionFolder.setSessionProperty(ClonkCore.FOLDER_DEFINITION_REFERENCE_ID, null);
		// on setObjectFolder(null): don't actually set objectFolder to null, so ILatestDeclarationVersionProvider machinery still works
		// (see ClonkIndex.getLatestVersion)
		definitionFolder = folder != null ? folder : definitionFolder;
		if (folder != null) {
			folder.setSessionProperty(ClonkCore.FOLDER_DEFINITION_REFERENCE_ID, this);
			if (id() != null)
				folder.setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, id().stringValue());
			else
				folder.setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, null);
			relativePath = folder.getFullPath().toPortableString();
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
		Definition obj = (Utilities.getIndex(folder) != null) ? Utilities.getIndex(folder).getDefinition(folder) : null;
		// haxxy cleanup: might have been lost by <insert unlikely event>
		if (obj != null)
			obj.definitionFolder = folder;
		return obj;
	}

	/**
	 * Give the definition a chance to refresh its folder reference after having been loaded or some such.
	 * @param project The project passed to the method so the definition has some context from where to get the folder reference
	 * @return Whether refreshing the folder reference was successful
	 * @throws CoreException
	 */
	public boolean refreshDefinitionFolderReference(IProject project) throws CoreException {
		Path path = new Path(this.relativePath);
		IPath projectPath = path.removeFirstSegments(1);
		IResource res = project.findMember(projectPath);
		if (res instanceof IContainer) {
			this.setDefinitionFolder((IContainer)res);
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
	public IResource resource() {
		return definitionFolder();
	}

	@Override
	public String infoText() {
		return String.format(INFO_TEXT_TEMPLATE, name(), super.infoText(), definitionFolder().getFullPath().toOSString());
		//return getName() + ": " + super.getInfoText();
	}

	/**
	 * Process a file contained in the Definition folder that won't be read into a separate {@link Structure} object.<br/>
	 * At the moment, this effectively only applies to Names.txt.
	 * @param file The (sufficiently simple to not be represented as a {@link Structure}) file to process to add additional information to the Definition
	 * @throws IOException
	 * @throws CoreException
	 */
	public void processDefinitionFolderFile(IFile file) throws IOException, CoreException {
		if (file.getName().equalsIgnoreCase("Names.txt")) { //$NON-NLS-1$
			readNames(StreamUtil.stringFromFileDocument(file));
		}
		else if (file.getName().equalsIgnoreCase("Graphics.png") || file.getName().equalsIgnoreCase("Graphics.bmp")) { //$NON-NLS-1$ //$NON-NLS-2$
			setCachedPicture(null); // obsolete
		}
	}

}
