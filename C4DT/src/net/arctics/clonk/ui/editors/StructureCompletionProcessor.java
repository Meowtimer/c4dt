package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.InitializationFunction;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;

public abstract class StructureCompletionProcessor<StateClass extends StructureEditingState<?, ?>> implements IContentAssistProcessor, ICompletionProposalSorter {

	protected Image defIcon;
	protected ProposalsSite pl;
	protected StateClass state;

	protected static class CategoryOrdering {
		public final int PAGE = 20000;
		public final int SUBPAGE = 300;
		public final int BONUS = 10;
		public int
			FunctionLocalVariables,
			SelfField,
			Constants,
			StaticVariables,
			Keywords,
			LocalFunction,
			LocalGlobalDelimiter,
			Functions,
			Fields,
			Definitions,
			NewFunction,
			Callbacks,
			EffectCallbacks,
			Directives;
		public void defaultOrdering() {
			int i = -PAGE;
			FunctionLocalVariables = i += PAGE;
			SelfField              = i += PAGE;
			LocalFunction          = i += PAGE;
			LocalGlobalDelimiter   = i += PAGE;
			Functions              = i += PAGE;
			Definitions            = i += PAGE;
			Fields                 = i += PAGE;
			StaticVariables        = i += PAGE;
			Constants              = i += PAGE;
			NewFunction            = i += PAGE;
			Callbacks              = i += PAGE;
			EffectCallbacks        = i += PAGE;
			Directives             = i += PAGE;
			Keywords               = i += PAGE;
		}
		{ defaultOrdering(); }
	}
	protected final CategoryOrdering cats = new CategoryOrdering();

	public StateClass state() { return state; }
	public StructureCompletionProcessor(StateClass state) { this.state = state; }

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		if (state != null)
			this.defIcon = state.structure().engine().image(GroupType.DefinitionGroup);
		return null;
	}

	protected void proposalForDefinition(ProposalsSite pl, Definition def) {
		try {
			if (def == null || def.id() == null)
				return;

			if (pl.prefix != null)
				if (!(
					stringMatchesPrefix(def.localizedName(), pl.prefix) ||
					stringMatchesPrefix(def.name(), pl.prefix)
					/* // also check if the user types in the folder name
					(def instanceof Definition && def.definitionFolder() != null &&
					 stringMatchesPrefix(def.definitionFolder().getName(), prefix))*/
				))
					return;
			final String displayString = definitionDisplayString(def);
			final int replacementLength = pl.prefix != null ? pl.prefix.length() : 0;

			final DeclarationProposal prop = new DeclarationProposal(def, def, def.id().stringValue(), pl.offset, replacementLength, def.id().stringValue().length(),
				defIcon, displayString.trim(), null, null, String.format(": %s", PrimitiveType.ID.typeName(true)), state()); //$NON-NLS-1$
			prop.setCategory(cats.Definitions);
			pl.addProposal(prop);
		} catch (final Exception e) {}
	}

	private String definitionDisplayString(Definition def) {
		if (def.engine().settings().definitionsHaveProxyVariables)
			return def.id().stringValue();
		else
			return String.format("%s (%s)", def.localizedName(), def.id().stringValue());
	}

	protected IFile pivotFile() { return state().structure().file(); }

	protected void proposalsForIndexedDefinitions(ProposalsSite pl, Index index) {
		for (final Definition obj : index.definitionsIgnoringRemoteDuplicates(pivotFile()))
			proposalForDefinition(pl, obj);
	}

	protected boolean stringMatchesPrefix(String name, String lowercasedPrefix) {
		return name.toLowerCase().contains(lowercasedPrefix);
	}

	protected DeclarationProposal proposalForFunc(ProposalsSite pl, Declaration target, Function func) {
		if (func instanceof InitializationFunction)
			return null;
		if (pl.prefix != null)
			if (!stringMatchesPrefix(func.name(), pl.prefix))
				return null;
		final int replacementLength = pl.prefix != null ? pl.prefix.length() : 0;
		final String replacement = func.name(); //$NON-NLS-1$ //$NON-NLS-2$
		final IType returnType = func.returnType(target.script());
		final String postInfo = returnType == PrimitiveType.UNKNOWN ? "" : ": " + returnType.typeName(true);
		final DeclarationProposal prop = new DeclarationProposal(
			func, target, replacement, pl.offset, replacementLength,
			UI.functionIcon(func), null/*contextInformation*/, null, postInfo, state() //$NON-NLS-1$
		);
		prop.setCategory(cats.Functions);
		pl.addProposal(prop);
		return prop;
	}

	protected DeclarationProposal proposalForVar(ProposalsSite pl, Declaration target, Variable var) {
		if (pl.prefix != null && !stringMatchesPrefix(var.name(), pl.prefix))
			return null;
		final String displayString = var.name();
		int replacementLength = 0;
		if (pl.prefix != null)
			replacementLength = pl.prefix.length();
		final DeclarationProposal prop = new DeclarationProposal(
			var, target,
			var.name(), pl.offset, replacementLength, var.name().length(), UI.variableIcon(var), displayString,
			null, null, ": " + var.type(defaulting(as(target, Script.class), pl.script)).typeName(true), //$NON-NLS-1$
			state()
		);
		setVariableCategory(var, prop);
		pl.addProposal(prop);
		return prop;
	}

	private void setVariableCategory(Variable var, final DeclarationProposal prop) {
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
	}

	@Override
	public String getErrorMessage() { return null; }

	@SuppressWarnings("serial")
	protected static class Text extends Declaration {
		public Text(String value) { setName(value); }
		@Override
		public int hashCode() { return name.hashCode(); }
	}

	@Override
	public int compare(ICompletionProposal a, ICompletionProposal b) {
		final DeclarationProposal ca = as(a, DeclarationProposal.class);
		final DeclarationProposal cb = as(b, DeclarationProposal.class);
		if (ca != null && cb != null) {
			if ((ca.declaration() instanceof Text || cb.declaration() instanceof Text) && ca.category() != cb.category()) {
				final int diff = cats.BONUS * Math.abs(cb.category()-ca.category());
				return cb.category() > ca.category() ? -diff : +diff;
			}
			int bonus = 0;
			final String pfx = pl != null ? pl.untamperedPrefix : "";
			final String pfxlo = pfx.toLowerCase();
			if (pfx != null) {
				class Match {
					boolean startsWith, startsWithNonCase, match, matchNoCase, local;
					Match(DeclarationProposal proposal) {
						for (final String s : proposal.identifiers())
							if (s.length() > 0)
								if (s.startsWith(pfx)) {
									startsWith = true;
									if (s.length() == pfx.length()) {
										match = true;
										break;
									}
								}
								else if (s.toLowerCase().startsWith(pfxlo)) {
									startsWithNonCase = true;
									if (s.length() == pfxlo.length()) {
										matchNoCase = true;
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
					bonus += cats.BONUS * 4 * (ma.startsWith ? -1 : +1);
				else if (ma.matchNoCase != mb.matchNoCase)
					bonus += ma.matchNoCase ? -1 : +1;
				else if (ma.startsWithNonCase != mb.startsWithNonCase)
					bonus += cats.BONUS * 2 * (ma.startsWithNonCase ? -1 : +1);
				else if (ma.local != mb.local)
					bonus += cats.BONUS * 1 * (ma.local ? -1 : +1);
			}
			int result;
			if (cb.category() != ca.category())
				result = ca.category()-cb.category();
			else {
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

	static class AutoActivationCharacters implements IPropertyChangeListener {
		char[][] forProposals = new char[2][];
		char[] forContextInformation;
		private void configureActivation() {
			forProposals[1] = ClonkPreferences.toggle(ClonkPreferences.INSTANT_C4SCRIPT_COMPLETIONS, false)
				? "~:_.>ABCDEFGHIJKLMNOPQRSTVUWXYZabcdefghijklmnopqrstvuwxyz$".toCharArray() //$NON-NLS-1$
				: new char[0];
			forProposals[0] = new char[0];
			forContextInformation = new char[] {'('};
		}
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(ClonkPreferences.INSTANT_C4SCRIPT_COMPLETIONS))
				configureActivation();
		}
		{ configureActivation(); }
	}
	static AutoActivationCharacters autoActivationCharacters;
	static {
		final IPreferenceStore prefStore = Core.instance().getPreferenceStore();
		if (prefStore != null)
			prefStore.addPropertyChangeListener(autoActivationCharacters = new AutoActivationCharacters());
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() { return autoActivationCharacters.forContextInformation; }
	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		final ASTNode section = state().activeEditor().section();
		return autoActivationCharacters.forProposals[section != null ? 1 : 0];
	}

}
