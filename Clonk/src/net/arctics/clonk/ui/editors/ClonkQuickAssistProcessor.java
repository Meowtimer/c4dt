package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;

public class ClonkQuickAssistProcessor implements IQuickAssistProcessor  {

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(
			IQuickAssistInvocationContext invocationContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}
