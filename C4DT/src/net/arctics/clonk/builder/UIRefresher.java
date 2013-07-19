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
		final IWorkbench w = PlatformUI.getWorkbench();
		for (final IWorkbenchWindow window : w.getWorkbenchWindows()) {
			for (final IWorkbenchPage page : window.getPages())
				for (final IEditorReference ref : page.getEditorReferences()) {
					final IEditorPart part = ref.getEditor(false);
					if (part != null && part instanceof StructureTextEditor) {
						final StructureTextEditor ed = (StructureTextEditor)part;
						ed.refreshOutline();
					}
				}
			final CommonNavigator projectExplorer = UI.projectExplorer(window);
			if (projectExplorer != null)
				for (final Script s : resourcesToBeRefreshed)
					UI.refreshAllProjectExplorers(s.resource());
		}
	}
}