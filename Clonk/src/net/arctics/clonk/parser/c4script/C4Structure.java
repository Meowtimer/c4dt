package net.arctics.clonk.parser.c4script;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

public abstract class C4Structure extends C4Declaration {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract C4Variable findVariable(String variableName);
	public C4Declaration findDeclaration(String declarationName) {
		return findVariable(declarationName);
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
}
