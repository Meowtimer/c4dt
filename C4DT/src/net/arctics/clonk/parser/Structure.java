package net.arctics.clonk.parser;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.resource.ClonkBuilder;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

/**
 * Declaration that contains sub declarations and describes more complex structures (like DefCores and scripts).
 * Provides support for being pinned to files in the project tree using {@link IResource} session properties.
 */
public abstract class Structure extends Declaration implements ILatestDeclarationVersionProvider {
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	/**
	 * Finds a declaration inside this structure
	 * @param declarationName the name of the declaration
	 * @param declarationClass the class of the declaration
	 * @return the declaration or null if it couldn't be found
	 */
	public Declaration findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}
	
	/**
	 * Finds a declaration without requiring a specific class
	 * @param declarationName the name of the declaration
	 * @return the declaration or null if it couldn't be found
	 */
	public Declaration findDeclaration(String declarationName) {
		return findDeclaration(declarationName, Declaration.class);
	}
	
	public abstract Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass);
	
	/**
	 * Returns an editor input for this structure
	 * @return the editor input
	 */
	public IEditorInput getEditorInput() {
		Object storage = getScript() != null ? getScript().getScriptStorage() : getResource();
		if (storage instanceof IFile)
			return new FileEditorInput((IFile) storage);
		if (storage instanceof IStorage && this instanceof ScriptBase)
			return new ScriptWithStorageEditorInput((ScriptBase) this);
		return null;
	}
	
	/**
	 * Returns whether this structure is editable
	 * @return
	 */
	public boolean isEditable() {
		return true;
	}
	
	/**
	 * Pins this structure to a file (should be the file the structure was read from)
	 * @param resource the file
	 * @throws CoreException
	 */
	public void pinTo(IResource resource) throws CoreException {
		resource.setSessionProperty(ClonkCore.FILE_STRUCTURE_REFERENCE_ID, this);
	}
	
	/**
	 * Returns the structure pinned to a file creating it if force is true and the structure does not already exist
	 * @param file the file to return the pinned structure of
	 * @param force whether to create the structure if it does not yet exist
	 * @return the structure
	 * @throws CoreException
	 */
	public static Structure pinned(IResource file, boolean force, boolean duringBuild) throws CoreException {
		Structure result = (Structure) file.getSessionProperty(ClonkCore.FILE_STRUCTURE_REFERENCE_ID);
		if (result == null && force) {
			result = createStructureForFile(file, duringBuild);
			if (result != null)
				result.pinTo(file);
		}
		return result;
	}
	
	/**
	 * Remove the structure pinned to the given file
	 * @param file the file to remove the reference from
	 * @return the previously pinned structure or null if there was none 
	 * @throws CoreException
	 */
	public static Structure unPinFrom(IFile file) throws CoreException {
		Structure pinned = pinned(file, false, false);
		if (pinned != null)
			file.setSessionProperty(ClonkCore.FILE_STRUCTURE_REFERENCE_ID, null);
		return pinned;
	}
	
	/**
	 * Gives a hint whether this structure is in some way out of sync with the file it's defined in
	 * @return true if out of sync, false if not
	 */
	public boolean dirty() {
		return false;
	}
	
	/**
	 * Mark the Structure as being out of sync with the file it's defined in.
	 * No guarantees given as to whether the return value of {@link #dirty()} and calling this method will be consistent (a.k.a: Those methods are declared empty in Structure and only {@link ScriptBase} overrides them).m
	 * @param dirty Dirty flag
	 */
	public void setDirty(boolean dirty) {}
	
	/**
	 * factory for creating structures
	 *
	 */
	public interface IStructureFactory {
		/**
		 * Create a structure for the given resource.
		 * @param resource The resource to create a Structure for
		 * @param duringBuild Flag to indicate whether the creation request happens during a {@link ClonkBuilder} run.
		 * @return The created structure or null if the factory decides to not be responsible for this kind of file.
		 */
		public Structure create(IResource resource, boolean duringBuild);
	}
	
	/**
	 * Registered structure factories that are queried if a structure for a file is to be created
	 */
	private static Collection<IStructureFactory> structureFactories = new LinkedList<IStructureFactory>();
	
	/**
	 * Registers a new structure factory
	 * @param factory the factory
	 */
	public static void registerStructureFactory(IStructureFactory factory) {
		structureFactories.add(factory);
	}
	
	/**
	 * Creates a structure for a file. This is achieved by querying registered structure factories
	 * @param file file
	 * @return the newly created structure or null if no suitable factory could be found
	 */
	public static Structure createStructureForFile(IResource file, boolean duringBuild) {
		for (IStructureFactory factory : structureFactories) {
			Structure result = factory.create(file, duringBuild);
			if (result != null)
				return result;
		}
		return null;
	}
	
	/**
	 * Commits data of this structure to the script. Primarily for {@link Definition}s which are defined by various files in addition to the script file.
	 * @param script the script to commit to
	 * @param builder Builders gonna build
	 */
	public void commitTo(ScriptBase script, ClonkBuilder builder) {
		// placeholder
	}
	
	/**
	 * Returns whether changing a structure (by editing the corresponding file) causes a reparsing of the associated script if there is one.
	 * @return Whether a script reparse is required or not.
	 */
	public boolean requiresScriptReparse() {
		return false;
	}
	
	/**
	 * Called by the {@link ClonkBuilder} in phase 2 to give Structure files a chance to complain about things like missing functions (which ought to have been created as of now since the {@link ClonkBuilder} is in phase 2)
	 */
	public void validate() {}
	
	@SuppressWarnings("unchecked")
	public <T extends Declaration> T getLatestVersion(T from) {
		return (T) findLocalDeclaration(from.getName(), from.getClass());
	}
	
}
