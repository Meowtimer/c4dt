package net.arctics.clonk.ui;

import net.arctics.clonk.ClonkCore;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class ClonkPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		// layout.addFastView("org.eclipse.ui.navigator.ProjectExplorer", (float) 0.2);
		// layout.addView("org.eclipse.ui.navigator.ProjectExplorer", IPageLayout.RIGHT, IPageLayout.DEFAULT_VIEW_RATIO, "navigator");

		layout.addActionSet(ClonkCore.id("ui.actionset")); //$NON-NLS-1$
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);

		layout.addShowViewShortcut(ClonkCore.id("views.EngineDeclarationsView")); //$NON-NLS-1$

		// Get the editor area.
		String editorArea = layout.getEditorArea();

		// Top left: Resource Navigator view and Bookmarks view placeholder
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, //$NON-NLS-1$
			editorArea);
		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
		topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);

		// Bottom left: Outline view and Property Sheet view
		IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.50f, //$NON-NLS-1$
			"topLeft"); //$NON-NLS-1$
		bottomLeft.addView(IPageLayout.ID_OUTLINE);
		bottomLeft.addView(ClonkCore.id("views.ClonkFolderView")); //$NON-NLS-1$
		bottomLeft.addView(ClonkCore.id("views.ClonkPreviewView")); //$NON-NLS-1$

		// Bottom right: Task List view
		layout.addView(IPageLayout.ID_PROBLEM_VIEW, IPageLayout.BOTTOM, 0.66f, editorArea);

		layout.addNewWizardShortcut(ClonkCore.id("wizards.NewC4Object")); //$NON-NLS-1$
		layout.addNewWizardShortcut(ClonkCore.id("wizards.NewClonkProject")); //$NON-NLS-1$
		layout.addNewWizardShortcut(ClonkCore.id("wizards.NewScenario")); //$NON-NLS-1$
		layout.addNewWizardShortcut(ClonkCore.id("wizards.NewParticle")); //$NON-NLS-1$
	}

}
