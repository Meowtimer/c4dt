package net.arctics.clonk.parser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;
import net.arctics.clonk.util.Utilities;

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
		Object scriptFile = getScript() != null ? getScript().getScriptFile() : getResource();
		if (scriptFile instanceof IFile)
			return new FileEditorInput((IFile) scriptFile);
		if (scriptFile instanceof IStorage && this instanceof C4ScriptBase)
			return new ScriptWithStorageEditorInput((C4ScriptBase) this);
		return null;
	}
	
	public boolean isEditable() {
		return true;
	}
	
	public void pinTo(IFile file) throws CoreException {
		file.setSessionProperty(ClonkCore.STRUCTURE_PROPERTY_ID, this);
	}
	
	public static C4Structure pinned(IFile file, boolean force) throws CoreException {
		C4Structure result = (C4Structure) file.getSessionProperty(ClonkCore.STRUCTURE_PROPERTY_ID);
		if (result == null && force) {
			Class<? extends IniUnit> iniUnitClass = Utilities.getIniUnitClass(file);
			if (iniUnitClass != null) {
				try {
					IniUnit reader = iniUnitClass.getConstructor(IFile.class).newInstance(file);
					reader.parse();
					reader.pinTo(file);
					result = reader;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
}
