package net.arctics.clonk.index;

import java.io.Serializable;
import java.security.InvalidParameterException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ID;
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
public class C4ObjectIntern extends C4Object implements Serializable {

	private static final long serialVersionUID = -7978767061460505544L;
	
	protected transient IContainer objectFolder;
	protected String relativePath;
	
	public C4ObjectIntern(C4ID id, String name, IContainer container) {
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
			throw new InvalidParameterException("Parameter 'persistent' is not yet implemented");
		}
	}
	
	/**
	 * Sets the id property of this object.
	 * This method changes resources when <code>persistent</code> is true. (but that is not implemented yet)
	 * @param newId
	 * @param persistent not implemented yet - set it to false always
	 */
	public void setId(C4ID newId, boolean persistent) {
		setId(newId);
		if (persistent) {
//			DefCoreParser defCoreParser = new DefCoreParser(getScriptFile());
//			IniEntry idEntry = defCoreParser.entryInSection("DefCore", "id");
//			if (idEntry != null) {
//				idEntry.setValue(value)
//			}
			// TODO edit DefCore.txt
			throw new InvalidParameterException("Parameter 'persistent' is not yet implemented");
		}
	}
	
	/**
	 * The member <tt>Script.c</tt>
	 * @return IFile object of <tt>Script.c</tt> file or null if it does not exist
	 */
	public IFile getScriptFile() {
		IResource res = this.objectFolder.findMember("Script.c");
		if (res == null || !(res instanceof IFile)) return null;
		else return (IFile) res;
	}
	
	public IFile getDefCoreFile() {
		IResource res = this.objectFolder.findMember("DefCore.txt");
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
		Utilities.getClonkNature(objectFolder.getProject()).getIndex().removeObject(this);
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
		}
	}
	
	@Override
	public ClonkIndex getIndex() {
		return Utilities.getClonkNature(this).getIndex();
	}
	
	@Override
	public void setId(C4ID newId) {
		super.setId(newId);
		if (objectFolder != null)
			try {
				objectFolder.setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, getId().getName());
			} catch (CoreException e) {
				e.printStackTrace();
			}
	}
	
	public static C4ObjectIntern objectCorrespondingTo(IContainer folder) {
		return (Utilities.getIndex(folder) != null) ? Utilities.getIndex(folder).getObject(folder) : null;
	}
	
	public void refreshFolderReference(IProject project) throws CoreException {
		Path path = new Path(this.relativePath);
		IPath projectPath = path.removeFirstSegments(1);
		IResource res = project.findMember(projectPath);
		if (res instanceof IContainer)
			this.setObjectFolder((IContainer)res);
	}
	
	@Override
	public String toString() {
		return super.toString() + " [" + relativePath + "]";
	}
	
	@Override
	public IResource getResource() {
		return getObjectFolder();
	}
	
	@Override
	public String getInfoText() {
		return getName() + ": " + super.getInfoText();
	}

}
