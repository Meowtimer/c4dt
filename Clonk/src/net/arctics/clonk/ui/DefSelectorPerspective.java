package net.arctics.clonk.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class DefSelectorPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT, 1.0f, layout.getEditorArea());
	}

}
