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
	public final List<ICompletionProposal> proposals;
	public final int offset;
	public final int wordOffset;
	public final IDocument document;
	public final String untamperedPrefix, prefix;
	public final Map<Class<? extends Declaration>, Map<String, ClonkCompletionProposal>> declarationProposals;
	public final Index index;
	public final Script script;
	public void addProposal(ICompletionProposal proposal) {
		final ClonkCompletionProposal ccp = as(proposal, ClonkCompletionProposal.class);
		if (ccp != null && ccp.declaration() != null) {
			Map<String, ClonkCompletionProposal> decs = declarationProposals.get(ccp.declaration().getClass());
			final ClonkCompletionProposal existing = decs != null ? decs.get(ccp.declaration().name()) : null;
			if (existing != null)
				return;
			if (decs == null) {
				decs = new HashMap<String, ClonkCompletionProposal>();
				declarationProposals.put(ccp.declaration().getClass(), decs);
			}
			decs.put(ccp.declaration().name(), ccp);
		}
		proposals.add(ccp);
	}
	public void removeProposalForDeclaration(Declaration declaration) {
		final Map<String, ClonkCompletionProposal> props = declarationProposals.get(declaration.getClass());
		if (props != null) {
			final ClonkCompletionProposal proposal = props.get(declaration.name());
			if (proposal != null && proposal.declaration() == declaration) {
				props.remove(declaration.name());
				proposals.remove(proposal);
			}
		}
	}
	public ProposalsSite(
		int offset, int wordOffset, IDocument document,
		String untamperedPrefix, List<ICompletionProposal> proposals,
		Index index, Function function, Script script
	) {
		super(function);
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
		this.proposals = proposals;
		this.declarationProposals = new HashMap<>();
		this.index = index;
		this.script = script;
	}
	public ProposalsSite setPreceding(PrecedingExpression preceding) {
		this.contextExpression = preceding.contextExpression;
		this.contextSequence   = preceding.contextSequence;
		this.precedingType     = preceding.precedingType;
		return this;
	}
	public ICompletionProposal[] finish(ProposalCycle cycle) {
		if (proposals.size() > 0) {
			if (cycle != ProposalCycle.ALL)
				outcycle(cycle);
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
		}
		else
			return null;
	}
	private void outcycle(ProposalCycle cycle) {
		final Collection<ClonkCompletionProposal> outcycled = new ArrayList<ClonkCompletionProposal>(proposals.size());
		for (final ICompletionProposal cp : proposals) {
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
		proposals.removeAll(outcycled);
	}
}