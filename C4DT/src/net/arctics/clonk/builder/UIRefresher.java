package net.arctics.clonk.builder;

import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.util.UI;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

final class UIRefresher implements Runnable {

	private final Script[] resourcesToBeRefreshed;

	public UIRefresher(Script[] resourcesToBeRefreshed) {
		super();
		this.resourcesToBeRefreshed = resourcesToBeRefreshed;
	}

	@Override
	public void run() {
		IWorkbench w = PlatformUI.getWorkbench();
		for (IWorkbenchWindow window : w.getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages())
				for (IEditorReference ref : page.getEditorReferences()) {
					IEditorPart part = ref.getEditor(false);
					if (part != null && part instanceof StructureTextEditor)
						((StructureTextEditor)part).refreshOutline();
				}
			CommonNavigator projectExplorer = UI.projectExplorer(window);
			if (projectExplorer != null)
				for (Script s : resourcesToBeRefreshed)
					UI.refreshAllProjectExplorers(s.resource());
		}
	}
}