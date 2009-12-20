package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ExternIndex;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class ScriptWithStorageEditorInputFactory implements IElementFactory {

	public IAdaptable createElement(IMemento memento) {
		String path = memento.getString("path"); //$NON-NLS-1$
		if (path != null) try {
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			String projName = path.substring(0, path.indexOf('/'));
			IProject p = workspaceRoot.getProject(projName);
			ExternIndex index;
			ClonkProjectNature nature;
			if (p != null && ((nature = ClonkProjectNature.get(p)) != null)) {
				index = nature.getIndex();
				path = path.substring(projName.length()+1);
			} else {
				index = ClonkCore.getDefault().getExternIndex();
			}
			C4ScriptBase script = index.findScriptByPath(path);
			if (script != null)
				return new ScriptWithStorageEditorInput(script);
		} catch (Exception e) {
			/* whatever */
		}
		return null;
	}

}
