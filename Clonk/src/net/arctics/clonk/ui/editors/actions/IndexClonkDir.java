package net.arctics.clonk.ui.editors.actions;

import java.io.File;
import java.util.ResourceBundle;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ClonkIndexer;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class IndexClonkDir extends TextEditorAction {

	public IndexClonkDir(ResourceBundle bundle, String prefix,
			ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		IEditorInput input = (IEditorInput)getTextEditor().getEditorInput();
		IResource adapt = (IResource) input.getAdapter(IResource.class);
		IProject project = adapt.getProject();
		if (project != null) {
			ClonkProjectNature nature = null;
			try {
				nature = (ClonkProjectNature) project.getNature("net.arctics.clonk.clonknature");
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (nature != null) {
				ClonkIndexer indexer = nature.getIndexer();
				IEclipsePreferences prefs = new ProjectScope(project).getNode(ClonkCore.PLUGIN_ID);
				String path = prefs.get("clonkpath", null);
				if (path != null) {
					indexer.indexClonkDirectory(new File(path));
				}
			}
		}
		super.run();
	}

}
