package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ClonkTextHover;

public class C4ScriptTextHover extends ClonkTextHover<C4ScriptEditor> {

	private DeclarationLocator declLocator;
	
	public C4ScriptTextHover(ClonkSourceViewerConfiguration<C4ScriptEditor> clonkSourceViewerConfiguration) {
	    super(clonkSourceViewerConfiguration);
    }

	@Override
	public String getHoverInfo(ITextViewer viewer, IRegion region) {
		return declLocator != null && declLocator.getDeclaration() != null
			? declLocator.getDeclaration().getInfoText()
			: super.getHoverInfo(viewer, region);
	}

	@Override
	public IRegion getHoverRegion(ITextViewer viewer, int offset) {
		try {
			declLocator = new DeclarationLocator(configuration.getEditor(), viewer.getDocument(), new Region(offset, 0));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return declLocator.getIdentRegion();
	}

}
