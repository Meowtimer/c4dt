package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class ScriptWithStorageEditorInputFactory implements IElementFactory {

	public IAdaptable createElement(IMemento memento) {
		String path = memento.getString("path");
		if (path != null) {
			C4ScriptBase script = ClonkCore.getDefault().EXTERN_INDEX.findScriptByPath(path);
			if (script != null)
				return new ScriptWithStorageEditorInput(script);
		}
		return null;
	}

}
