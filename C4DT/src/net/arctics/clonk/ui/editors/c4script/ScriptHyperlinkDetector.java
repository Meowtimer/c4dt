package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.c4script.ast.EntityLocator;
import net.arctics.clonk.ui.editors.ClonkHyperlink;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

class ScriptHyperlinkDetector implements IHyperlinkDetector {
	private final ScriptSourceViewerConfiguration configuration;
	public ScriptHyperlinkDetector(ScriptSourceViewerConfiguration configuration) {
		super();
		this.configuration = configuration;
	}
	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region, boolean canShowMultipleHyperlinks) {
		try {
			final EntityLocator locator = new EntityLocator(configuration.editor().script(), viewer.getDocument(),region);
			if (locator.entity() != null)
				return new IHyperlink[] {
					new ClonkHyperlink(locator.expressionRegion(), locator.entity())
				};
			else if (locator.potentialEntities() != null)
				return new IHyperlink[] {
					new ClonkHyperlink(locator.expressionRegion(), locator.potentialEntities())
				};
			return null;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}