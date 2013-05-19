package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ui.editors.c4script.ProposalCycle;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class ProposalsSite extends PrecedingExpression {
	private final List<ICompletionProposal> _proposals;
	public final int offset;
	public final int wordOffset;
	public final IDocument document;
	public final String untamperedPrefix, prefix;
	public final Map<Declaration, ClonkCompletionProposal> proposals;
	public final Index index;
	public final Function function;
	public final Script script;
	public void addProposal(ICompletionProposal proposal) {
		final ClonkCompletionProposal ccp = as(proposal, ClonkCompletionProposal.class);
		if (ccp != null && ccp.declaration() != null)
			if (proposals.containsKey(ccp.declaration()))
				return;
			else
				proposals.put(ccp.declaration(), ccp);
		_proposals.add(ccp);
	}
	public ProposalsSite(
		int offset, int wordOffset, IDocument document,
		String untamperedPrefix, List<ICompletionProposal> proposals,
		Index index, Function function, Script script
	) {
		this.offset = offset;
		this.wordOffset = wordOffset;
		this.document = document;
		this.untamperedPrefix = untamperedPrefix;
		if (untamperedPrefix != null) {
			String tamper = untamperedPrefix;
			if (tamper.startsWith("~"))
				tamper = tamper.substring(1);
			tamper = tamper.toLowerCase();
			this.prefix = tamper;
		} else
			this.prefix = null;
		this._proposals = proposals;
		this.proposals = new HashMap<Declaration, ClonkCompletionProposal>();
		this.index = index;
		this.function = function;
		this.script = script;
	}
	public ProposalsSite setPreceding(PrecedingExpression preceding) {
		this.contextExpression = preceding.contextExpression;
		this.contextSequence   = preceding.contextSequence;
		this.precedingType     = preceding.precedingType;
		return this;
	}
	public ICompletionProposal[] finish(ProposalCycle cycle) {
		if (_proposals.size() > 0) {
			if (cycle != ProposalCycle.ALL)
				outcycle(cycle);
			return _proposals.toArray(new ICompletionProposal[_proposals.size()]);
		}
		else
			return null;
	}
	private void outcycle(ProposalCycle cycle) {
		final Collection<ClonkCompletionProposal> outcycled = new ArrayList<ClonkCompletionProposal>(_proposals.size());
		for (final ICompletionProposal cp : _proposals) {
			final ClonkCompletionProposal ccp = as(cp, ClonkCompletionProposal.class);
			if (ccp != null)
				switch (cycle) {
				case OBJECT:
					if (ccp.declaration() != null && ccp.declaration().isGlobal())
						outcycled.add(ccp);
					break;
				default:
					break;
				}
		}
		_proposals.removeAll(outcycled);
	}
}