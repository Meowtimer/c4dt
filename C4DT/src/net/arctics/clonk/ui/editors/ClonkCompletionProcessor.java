package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Collection;

import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.InitializationFunction;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IFileEditorInput;

public abstract class ClonkCompletionProcessor<EditorType extends ClonkTextEditor> implements IContentAssistProcessor, ICompletionProposalSorter {

	protected EditorType editor;
	protected String prefix, untamperedPrefix;
	protected Image defIcon;

	protected static class CategoryOrdering {
		public int
			FunctionLocalVariables,
			Constants,
			StaticVariables,
			Keywords,
			LocalFunction,
			Functions,
			Fields,
			Definitions,
			NewFunction,
			Callbacks,
			EffectCallbacks,
			Directives;
		public void defaultOrdering() {
			int i = 0;
			FunctionLocalVariables = ++i;
			LocalFunction = ++i;
			Functions = ++i;
			Definitions = ++i;
			Fields = ++i;
			StaticVariables = ++i;
			Constants = ++i;
			NewFunction = ++i;
			Callbacks = ++i;
			EffectCallbacks = ++i;
			Directives = ++i;
			Keywords = ++i;
		}
		{ defaultOrdering(); }
	}
	protected final CategoryOrdering cats = new CategoryOrdering();

	public EditorType editor() { return editor; }
	public ClonkCompletionProcessor(EditorType editor, ContentAssistant assistant) {
		this.editor = editor;
		if (assistant != null)
			assistant.setSorter(this);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		if (editor != null)
			this.defIcon = editor.structure().engine().image(GroupType.DefinitionGroup);
		return null;
	}

	protected void proposalForDefinition(Definition def, String prefix, int offset, Collection<ICompletionProposal> proposals) {
		try {
			if (def == null || def.id() == null)
				return;

			if (prefix != null)
				if (!(
					stringMatchesPrefix(def.name(), prefix) ||
					stringMatchesPrefix(def.id().stringValue(), prefix)
					/* // also check if the user types in the folder name
					(def instanceof Definition && def.definitionFolder() != null &&
					 stringMatchesPrefix(def.definitionFolder().getName(), prefix))*/
				))
					return;
			final String displayString = def.name();
			final int replacementLength = prefix != null ? prefix.length() : 0;

			final ClonkCompletionProposal prop = new ClonkCompletionProposal(def, def.id().stringValue(), offset, replacementLength, def.id().stringValue().length(),
				defIcon, displayString.trim(), null, null, "", editor()); //$NON-NLS-1$
			prop.setCategory(cats.Definitions);
			proposals.add(prop);
		} catch (final Exception e) {}
	}

	protected IFile pivotFile() {
		return ((IFileEditorInput)editor.getEditorInput()).getFile();
	}

	protected void proposalsForIndexedDefinitions(Index index, int offset, int wordOffset, String prefix, Collection<ICompletionProposal> proposals) {
		for (final Definition obj : index.definitionsIgnoringRemoteDuplicates(pivotFile()))
			proposalForDefinition(obj, prefix, wordOffset, proposals);
	}

	protected boolean stringMatchesPrefix(String name, String lowercasedPrefix) {
		return name.toLowerCase().contains(lowercasedPrefix);
	}

	protected ClonkCompletionProposal proposalForFunc(Function func, String prefix, int offset, Collection<ICompletionProposal> proposals, String parentName, boolean brackets) {
		if (func instanceof InitializationFunction)
			return null;
		if (prefix != null)
			if (!stringMatchesPrefix(func.name(), prefix))
				return null;
		final int replacementLength = prefix != null ? prefix.length() : 0;

		final String replacement = func.name() + (brackets ? "()" : ""); //$NON-NLS-1$ //$NON-NLS-2$
		final ClonkCompletionProposal prop = new ClonkCompletionProposal(
			func, replacement, offset, replacementLength,
			UI.functionIcon(func), null/*contextInformation*/, null, " - " + parentName, editor() //$NON-NLS-1$
		);
		prop.setCategory(cats.Functions);
		proposals.add(prop);
		return prop;
	}

	protected ClonkCompletionProposal proposalForVar(Variable var, String prefix, int offset, Collection<ICompletionProposal> proposals) {
		if (prefix != null && !stringMatchesPrefix(var.name(), prefix))
			return null;
		final String displayString = var.name();
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		final ClonkCompletionProposal prop = new ClonkCompletionProposal(
			var,
			var.name(), offset, replacementLength, var.name().length(), UI.variableIcon(var), displayString,
			null, null, " - " + (var.parentDeclaration() != null ? var.parentDeclaration().name() : "<adhoc>"), //$NON-NLS-1$
			editor()
		);
		switch (var.scope()) {
		case CONST:
			prop.setCategory(cats.Constants);
			break;
		case VAR: case PARAMETER:
			prop.setCategory(cats.FunctionLocalVariables);
			break;
		case STATIC:
			prop.setCategory(cats.StaticVariables);
			break;
		case LOCAL:
			prop.setCategory(cats.Fields);
			break;
		default:
			break;
		}
		proposals.add(prop);
		return prop;
	}

	@Override
	public String getErrorMessage() { return null; }

	@Override
	public int compare(ICompletionProposal a, ICompletionProposal b) {
		final ClonkCompletionProposal ca = as(a, ClonkCompletionProposal.class);
		final ClonkCompletionProposal cb = as(b, ClonkCompletionProposal.class);
		if (ca != null && cb != null) {
			int bonus = 0;
			if (untamperedPrefix != null) {
				class Match {
					boolean startsWith, match, local;
					Match(ClonkCompletionProposal proposal) {
						for (final String s : proposal.identifiers())
							if (s.startsWith(untamperedPrefix)) {
								startsWith = true;
								if (s.length() == untamperedPrefix.length()) {
									match = true;
									break;
								}
							}
						local = proposal.declaration() != null && !proposal.declaration().isGlobal();
					}
				}
				final Match ma = new Match(ca), mb = new Match(cb);
				if (ma.match != mb.match)
					// match wins
					return ma.match ? -1 : +1;
				else if (ma.startsWith != mb.startsWith)
					bonus += (ma.startsWith ? -1 : +1) * 2000;
				else if (ma.local != mb.local)
					bonus += (ma.local ? -1 : +1) * 2000;
			}
			int result;
			if (cb.category() != ca.category()) {
				final int diff = Math.abs(cb.category()-ca.category()) * 1000;
				result = cb.category() > ca.category() ? -diff : +diff;
			} else {
				final String idA = ca.primaryComparisonIdentifier();
				final String idB = cb.primaryComparisonIdentifier();
				final boolean bracketStartA = idA.startsWith("["); //$NON-NLS-1$
				final boolean bracketStartB = idB.startsWith("["); //$NON-NLS-1$
				if (bracketStartA != bracketStartB)
					result = bracketStartA ? +1 : -1;
				else
					result = idA.compareToIgnoreCase(idB);
			}
			return bonus + result;
		}
		return 1;
	}

}
