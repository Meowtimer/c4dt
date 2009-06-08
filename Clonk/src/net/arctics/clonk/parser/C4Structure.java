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
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

public abstract class C4Structure extends C4Declaration {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract C4Declaration findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass);
	public C4Declaration findDeclaration(String declarationName) {
		return findDeclaration(declarationName, C4Declaration.class);
	}
	
	public IEditorInput getEditorInput() {
		Object storage = getScript() != null ? getScript().getScriptFile() : getResource();
		if (storage instanceof IFile)
			return new FileEditorInput((IFile) storage);
		if (storage instanceof IStorage && this instanceof C4ScriptBase)
			return new ScriptWithStorageEditorInput((C4ScriptBase) this);
		return null;
	}
	
	public boolean isEditable() {
		return true;
	}
	
	public void pinTo(IFile file) throws CoreException {
		file.setSessionProperty(ClonkCore.C4STRUCTURE_PROPERTY_ID, this);
	}
	
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
	
	public interface IStructureFactory {
		public C4Structure create(IFile file);
	}
	
	private static Collection<IStructureFactory> structureFactories = new LinkedList<IStructureFactory>();
	
	public static void registerStructureFactory(IStructureFactory factory) {
		structureFactories.add(factory);
	}
	
	public static C4Structure createStructureForFile(IFile file) {
		for (IStructureFactory factory : structureFactories) {
			C4Structure result = factory.create(file);
			if (result != null)
				return result;
		}
		return null;
	}
	
	public void commitTo(C4ScriptBase script) {
		// placeholder
	}
	
}
