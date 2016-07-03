package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ui.editors.c4script.ProposalCycle;

public class ProposalsSite extends PrecedingExpression {
	
	public final StructureEditingState<?, ?> state;
	public final List<ICompletionProposal> proposals;
	public final int offset;
	public final int wordOffset;
	public final IDocument document;
	public String untamperedPrefix, prefix;
	public final Map<Class<? extends Declaration>, Map<String, DeclarationProposal>> declarationProposals;
	public final Index index;
	public final Script script;
	
	public void addProposal(final ICompletionProposal proposal) {
		final DeclarationProposal ccp = as(proposal, DeclarationProposal.class);
		if (ccp != null && (ccp.declaration() instanceof Variable || ccp.declaration() instanceof Function)) {
			Map<String, DeclarationProposal> decs = declarationProposals.get(ccp.declaration().getClass());
			final DeclarationProposal existing = decs != null ? decs.get(ccp.declaration().name()) : null;
			if (existing != null)
				return;
			if (decs == null) {
				decs = new HashMap<String, DeclarationProposal>();
				declarationProposals.put(ccp.declaration().getClass(), decs);
			}
			decs.put(ccp.declaration().name(), ccp);
		}
		proposals.add(proposal);
	}

	public void removeProposalForDeclaration(final Declaration declaration) {
		final Map<String, DeclarationProposal> props = declarationProposals.get(declaration.getClass());
		if (props != null) {
			final DeclarationProposal proposal = props.get(declaration.name());
			if (proposal != null && proposal.declaration() == declaration) {
				props.remove(declaration.name());
				proposals.remove(proposal);
			}
		}
	}

	public ProposalsSite(
		final StructureEditingState<?, ?> state,
		final int offset, final int wordOffset, final IDocument document,
		final String untamperedPrefix, final List<ICompletionProposal> proposals,
		final Index index, final Function function, final Script script,
		final ASTNode contextExpression, final Sequence contextSequence, final IType precedingType
	) {
		super(function, contextExpression, contextSequence, precedingType);
		this.state = state;
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

	public ICompletionProposal[] finish(final ProposalCycle cycle) {
		if (proposals.size() > 0) {
			if (cycle != ProposalCycle.ALL)
				outcycle(cycle);
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
		}
		else
			return null;
	}

	private void outcycle(final ProposalCycle cycle) {
		final Collection<DeclarationProposal> outcycled = new ArrayList<DeclarationProposal>(proposals.size());
		for (final ICompletionProposal cp : proposals) {
			final DeclarationProposal ccp = as(cp, DeclarationProposal.class);
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

	public String updatePrefix(final int currentOffset) {
		try {
			untamperedPrefix = document.get(offset, currentOffset - offset);
		} catch (final BadLocationException e) {
			e.printStackTrace();
			return "";
		}
		return prefix = untamperedPrefix.toLowerCase();
	}

}