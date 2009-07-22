package net.arctics.clonk.parser;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

/**
 * Declaration that contains sub declarations and describes more complex structures (like DefCores and scripts).
 * Provides support for being pinned to files in the project tree.
 */
public abstract class C4Structure extends C4Declaration {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Finds a declaration inside this structure
	 * @param declarationName the name of the declaration
	 * @param declarationClass the class of the declaration
	 * @return the declaration or null if it couldn't be found
	 */
	public abstract C4Declaration findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass);
	
	/**
	 * Finds a declaration without requiring a specific class
	 * @param declarationName the name of the declaration
	 * @return the declaration or null if it couldn't be found
	 */
	public C4Declaration findDeclaration(String declarationName) {
		return findDeclaration(declarationName, C4Declaration.class);
	}
	
	/**
	 * Returns an editor input for this structure
	 * @return the editor input
	 */
	public IEditorInput getEditorInput() {
		Object storage = getScript() != null ? getScript().getScriptFile() : getResource();
		if (storage instanceof IFile)
			return new FileEditorInput((IFile) storage);
		if (storage instanceof IStorage && this instanceof C4ScriptBase)
			return new ScriptWithStorageEditorInput((C4ScriptBase) this);
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
	 * @param file the file
	 * @throws CoreException
	 */
	public void pinTo(IFile file) throws CoreException {
		file.setSessionProperty(ClonkCore.C4STRUCTURE_PROPERTY_ID, this);
	}
	
	/**
	 * Returns the structure pinned to a file creating it if it does not already exist and force is true
	 * @param file the file to return the pinned structure of
	 * @param force whether to create the structure if it does not yet exist
	 * @return the structure
	 * @throws CoreException
	 */
	public static C4Structure pinned(IFile file, boolean force) throws CoreException {
		C4Structure result = (C4Structure) file.getSessionProperty(ClonkCore.C4STRUCTURE_PROPERTY_ID);
		if (result == null && force) {
			result = createStructureForFile(file);
			if (result != null)
				result.pinTo(file);
		}
		return result;
	}
	
	/**
	 * Gives a hint whether this structure is in some way out of sync with the file it's defined in
	 * @return true if out of sync, false if not
	 */
	public boolean dirty() {
		return false;
	}
	
	/**
	 * factory for creating structures
	 *
	 */
	public interface IStructureFactory {
		public C4Structure create(IFile file);
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
	public static C4Structure createStructureForFile(IFile file) {
		for (IStructureFactory factory : structureFactories) {
			C4Structure result = factory.create(file);
			if (result != null)
				return result;
		}
		return null;
	}
	
	/**
	 * Commits data of this structure to the script. Mainly for objects.
	 * @param script the script to commit to
	 */
	public void commitTo(C4ScriptBase script) {
		// placeholder
	}
	
}
