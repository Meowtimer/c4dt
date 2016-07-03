package net.arctics.clonk.ui;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import net.arctics.clonk.Core;

public class ClonkPerspective implements IPerspectiveFactory {
	@Override
	public void createInitialLayout(final IPageLayout layout) {
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addShowViewShortcut(Core.id("views.EngineDeclarationsView")); //$NON-NLS-1$
		final String editorArea = layout.getEditorArea();
		// Top left: Resource Navigator view and Bookmarks view placeholder
		final IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, //$NON-NLS-1$
			editorArea);
		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
		topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		// Bottom left: Outline view
		final IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.50f, //$NON-NLS-1$
			"topLeft"); //$NON-NLS-1$
		bottomLeft.addView(IPageLayout.ID_OUTLINE);

		// Bottom right: Problems
		layout.addView(IPageLayout.ID_PROBLEM_VIEW, IPageLayout.BOTTOM, 0.66f, editorArea);

		layout.addNewWizardShortcut(Core.id("wizards.NewDefinition")); //$NON-NLS-1$
		layout.addNewWizardShortcut(Core.id("wizards.NewClonkProject")); //$NON-NLS-1$
		layout.addNewWizardShortcut(Core.id("wizards.NewScenario")); //$NON-NLS-1$
		layout.addNewWizardShortcut(Core.id("wizards.NewParticle")); //$NON-NLS-1$
	}
}
