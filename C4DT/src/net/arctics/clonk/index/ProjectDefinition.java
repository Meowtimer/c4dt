package net.arctics.clonk.index;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidParameterException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Object definition inside a project.
 */
public class ProjectDefinition extends Definition implements Serializable {

	private static final long serialVersionUID = -7978767061460505544L;

	protected transient IContainer objectFolder;
	protected String relativePath;
	private transient ClonkIndex index;

	private transient Variable staticVariable;

	public Variable getStaticVariable() {
		if (getEngine() != null && !getEngine().getCurrentSettings().definitionsHaveStaticVariables)
			return staticVariable = null;
		if (staticVariable == null) {
			staticVariable = new Variable() {
				private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

				@Override
				public String getName() {
					return ProjectDefinition.this.getName();
				}

				@Override
				public Declaration getParentDeclaration() {
					return ProjectDefinition.this;
				}

				@Override
				public Structure getTopLevelStructure() {
					return ProjectDefinition.this;
				}

				@Override
				public String getInfoText() {
					return ProjectDefinition.this.getInfoText();
				}

				@Override
				public Definition getObjectType() {
					return null;
				}

				@Override
				public IType getType() {
					return ProjectDefinition.this.getObjectType();
				}
				
				@Override
				public boolean typeIsInvariant() {
					return true;
				};

			};
		}
		return staticVariable;
	}

	public ProjectDefinition(ID id, String name, IContainer container) {
		super(id, name);
		try {
			setObjectFolder(container);
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
	public IFile getScriptStorage() {
		if (this.objectFolder == null)
			return null;
		IResource res = Utilities.findMemberCaseInsensitively(this.objectFolder, "Script.c"); //$NON-NLS-1$
		if (res == null || !(res instanceof IFile)) return null;
		else return (IFile) res;
	}

	@Override
	public String getScriptText() {
		try {
			return StreamUtil.stringFromFileDocument(getScriptStorage());
		} catch (Exception e) {
			return null;
		}
	}

	public IFile getDefCoreFile() {
		IResource res = Utilities.findMemberCaseInsensitively(this.objectFolder, "DefCore.txt"); //$NON-NLS-1$
		if (res == null || !(res instanceof IFile)) return null;
		else return (IFile) res;
	}

	/**
	 * The ObjectName.c4d IResource
	 * @return the folder object
	 */
	public IContainer getObjectFolder() {
		return objectFolder;
	}

	/**
	 * Removes this object from index.
	 * The file on the harddisk is not deleted. (delete it by IResource.delete(true,null))
	 */
	public void delete() {
		ClonkProjectNature.get(objectFolder.getProject()).getIndex().removeObject(this);
	}

	public void setObjectFolder(IContainer folder) throws CoreException {
		if (objectFolder == folder)
			return;
		if (objectFolder != null && objectFolder.exists())
			objectFolder.setSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID, null);
		objectFolder = folder;
		if (folder != null) {
			folder.setSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID, this);
			if (getId() != null)
				folder.setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, getId().getName());
			else
				folder.setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, null);
			relativePath = folder.getFullPath().toPortableString();
			index = ClonkProjectNature.get(objectFolder).getIndex();
		}
	}

	@Override
	public ClonkIndex getIndex() {
		return index;
	}

	@Override
	public void setId(ID newId) {
		super.setId(newId);
		if (objectFolder != null)
			try {
				objectFolder.setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, getId().getName());
			} catch (CoreException e) {
				e.printStackTrace();
			}
	}

	public static ProjectDefinition objectCorrespondingTo(IContainer folder) {
		ProjectDefinition obj = (Utilities.getIndex(folder) != null) ? Utilities.getIndex(folder).getObject(folder) : null;
		// haxxy cleanup: might have been lost by <insert unlikely event>
		if (obj != null)
			obj.objectFolder = folder;
		return obj;
	}

	public boolean refreshFolderReference(IProject project) throws CoreException {
		Path path = new Path(this.relativePath);
		IPath projectPath = path.removeFirstSegments(1);
		IResource res = project.findMember(projectPath);
		if (res instanceof IContainer) {
			this.setObjectFolder((IContainer)res);
			return true;
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return super.toString() + " [" + relativePath + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IResource getResource() {
		return getObjectFolder();
	}

	@Override
	public String getInfoText() {
		return String.format(INFO_TEXT_TEMPLATE, getName(), super.getInfoText(), getObjectFolder().getFullPath().toOSString());
		//return getName() + ": " + super.getInfoText();
	}

	// for processing files whose contents won't be saved in a separate c4structure thingie
	public void processFile(IFile file) throws IOException, CoreException {
		if (file.getName().equalsIgnoreCase("Names.txt")) { //$NON-NLS-1$
			readNames(StreamUtil.stringFromFileDocument(file));
		}
		else if (file.getName().equalsIgnoreCase("Graphics.png") || file.getName().equalsIgnoreCase("Graphics.bmp")) { //$NON-NLS-1$ //$NON-NLS-2$
			setCachedPicture(null); // obsolete
		}
	}

}
