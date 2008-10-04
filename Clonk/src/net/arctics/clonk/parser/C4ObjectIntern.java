package net.arctics.clonk.parser;

import java.security.InvalidParameterException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class C4ObjectIntern extends C4Object {

	private IContainer objectFolder;
	
	public C4ObjectIntern(C4ID id, String name, IContainer container) {
		super(id, name);
		if (container.exists()) {
			try {
				objectFolder = container;
				container.setSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID, this);
			} catch (CoreException e) {
				e.printStackTrace();
			}
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
		id = newId;
		if (persistent) {
			// TODO edit DefCore.txt
			throw new InvalidParameterException("Parameter 'persistent' is not yet implemented");
		}
	}
	
	/**
	 * The member <tt>Script.c</tt>
	 * @return IFile object of <tt>Script.c</tt> file or null if it does not exist
	 */
	public IFile getScript() {
		IResource res = this.objectFolder.findMember("Script.c");
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
		Utilities.getProject(objectFolder.getProject()).getIndexedData().removeObject(this);
	}
	
}
