package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.contentassist.ContentAssistant;

public class ClonkContentAssistant extends ContentAssistant {
	@Override
	public void hide() {
		super.hide();
	}
	@Override
	public boolean isContextInfoPopupActive() {
		return super.isContextInfoPopupActive();
	}
	@Override
	public boolean isProposalPopupActive() {
		return super.isProposalPopupActive();
	}
}