package net.arctics.clonk.ui.editors.c4script;

import java.util.List;

import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Index;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class ProposalsLocation extends PrecedingExpression {
	public final int offset;
	public final int wordOffset;
	public final IDocument document;
	public final String prefix;
	public final List<ICompletionProposal> proposals;
	public final Index index;
	public final Function function;
	public final Script script;
	public Integer declarationsMask;
	public ProposalsLocation(
		int offset, int wordOffset, IDocument document,
		String prefix, List<ICompletionProposal> proposals,
		Index index, Function function, Script script
	) {
		this.offset = offset;
		this.wordOffset = wordOffset;
		this.document = document;
		this.prefix = prefix;
		this.proposals = proposals;
		this.index = index;
		this.function = function;
		this.script = script;
	}
	public ProposalsLocation setPreceding(PrecedingExpression preceding) {
		this.contextExpression = preceding.contextExpression;
		this.contextSequence   = preceding.contextSequence;
		this.precedingType     = preceding.precedingType;
		return this;
	}
	public ProposalsLocation setDeclarationsMask(Integer declarationsMask) {
		this.declarationsMask = declarationsMask;
		return this;
	}
	@Override
	public int declarationsMask() {
		if (declarationsMask != null)
			return declarationsMask;
		else
			return super.declarationsMask();
	}
}