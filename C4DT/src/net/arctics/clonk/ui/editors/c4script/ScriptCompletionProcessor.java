package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;
import static net.arctics.clonk.util.Utilities.scriptForEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Placeholder;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.ProjectSettings.Typing;
import net.arctics.clonk.c4script.BuiltInDefinitions;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.IHasIncludes;
import net.arctics.clonk.c4script.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SpecialEngineRules;
import net.arctics.clonk.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.IFunctionCall;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.c4script.ast.EntityLocator.RegionDescription;
import net.arctics.clonk.c4script.effect.Effect;
import net.arctics.clonk.c4script.effect.EffectFunction;
import net.arctics.clonk.c4script.typing.FunctionType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeUnification;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IDocumentedDeclaration;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.PrecedingExpression;
import net.arctics.clonk.ui.editors.ProposalsSite;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor.FuncCallInfo;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Handles calculating completion lists shown when invoking Content Assist in C4Script editors.
 * @author madeen
 *
 */
public class ScriptCompletionProcessor extends ClonkCompletionProcessor<C4ScriptEditor> implements ICompletionListener, ICompletionListenerExtension {

	private final ContentAssistant assistant;
	private ProposalCycle proposalCycle = null;

	public ScriptCompletionProcessor(Script script) {
		super(null, null);
		assistant = null;
	}

	public ScriptCompletionProcessor(C4ScriptEditor editor, ContentAssistant assistant) {
		super(editor, assistant);
		this.assistant = assistant;
		if (assistant != null)
			assistant.addCompletionListener(this);
	}

	@Override
	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {}

	@Override
	public void assistSessionStarted(ContentAssistEvent event) {
		final IContentAssistantExtension3 ex3 = as(event.assistant, IContentAssistantExtension3.class);
		final IContentAssistantExtension2 ex2 = as(event.assistant, IContentAssistantExtension2.class);
		if (ex2 != null) {
			ex2.setRepeatedInvocationMode(true);
			ex2.setStatusLineVisible(true);
		}
		if (ex3 != null)
			ex3.setRepeatedInvocationTrigger(iterationBinding());

		proposalCycle = null;
	}

	@Override
	public void assistSessionEnded(ContentAssistEvent event) {
		proposalCycle = null;
	}

	@Override
	public void assistSessionRestarted(ContentAssistEvent event) {}

	/**
	 * Add to an existing list the proposals originating from some {@link Index}. Those proposals are comprised of global functions, static variables and {@link Definition}s.
	 * @param index The {@link Index} to add proposals for
	 * @param offset Caret offset
	 * @param wordOffset Word offset
	 * @param prefix String already typed
	 * @param proposals The list to add the proposals to
	 * @param flags Flags indicating what kind of proposals should be included. {@link DeclMask#STATIC_VARIABLES} needs to be or-ed to flags if {@link Definition} and static variable proposals are to be shown.
	 * @param editorScript Script the proposals are invoked on.
	 */
	private void proposalsForIndex(ProposalsSite pl, Index index) {
		final int declarationsMask = pl.declarationsMask();
		if (pl.function != null) {
			final Scenario s2 = pl.function.scenario();
			if ((declarationsMask & DeclMask.FUNCTIONS) != 0)
				for (final Function func : index.globalFunctions()) {
					final Scenario fScen = func.scenario();
					if (fScen != null && fScen != s2)
						continue;
					proposalForFunc(pl, editor().script(), func, true);
				}
			if ((declarationsMask & DeclMask.STATIC_VARIABLES) != 0)
				for (final Variable var : index.staticVariables()) {
					// ignore static variables from editor script since those are proposed already
					if (var.parentDeclaration() == pl.script)
						continue;
					final Scenario vScen = var.scenario();
					if (vScen != null && vScen != s2)
						continue;
					proposalForVar(pl, index.engine(), var);
				}
		}
		if ((declarationsMask & DeclMask.STATIC_VARIABLES) != 0)
			proposalsForIndexedDefinitions(pl, index);
	}

	private ProposalsSite makeProposalsSite(ITextViewer viewer, int offset) {
		int wordOffset;
		final IDocument doc = viewer.getDocument();
		for (wordOffset = offset - 1; wordOffset >= 0; wordOffset--)
			try {
				final char c = doc.getChar(wordOffset);
				if (!(BufferedScanner.isWordPart(c) || Character.isLetter(c)))
					break;
			} catch (final BadLocationException e) {
				break;
			}
		String prefix = null;
		try {
			wordOffset++;
			if (wordOffset < offset) {
				prefix = doc.get(wordOffset, offset - wordOffset);
				offset = wordOffset;
			} else
				prefix = "";
			if (prefix.length() > 0 && !ClonkCompletionProposal.validPrefix(prefix))
				return null;
		} catch (final BadLocationException e) { }

		return new ProposalsSite(
			offset, wordOffset, doc, prefix, new ArrayList<ICompletionProposal>(),
			ClonkProjectNature.get(editor().script().resource()).index(),
			funcAt(doc, offset), scriptForEditor(editor)
		);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		super.computeCompletionProposals(viewer, offset);
		pl = makeProposalsSite(viewer, offset);
		if (pl == null)
			return null;
		if (!(pl.function == null
			? computeProposalsOutsideFunction(viewer, pl)
			: computeProposalsInsideFunction(pl)))
			return null;
		assistant.setStatusMessage(proposalCycleMessage());
		return pl.finish(proposalCycle);
	}

	@SuppressWarnings("unused")
	private void setStatusMessage(final Function activeFunc) {
		assistant.setStatusMessage(proposalCycleMessage());
	}

	private boolean computeProposalsInsideFunction(ProposalsSite pl) {
		pl.pos(pl.offset - (pl.function != null ? pl.function.bodyLocation().start() : 0));
		final ScriptParser parser = pl.script != null ? editor().editingState().updateFunctionFragment(pl.function, pl, true) : null;

		if (!checkProposalConditions(pl))
			return false;

		proposalCycle = proposalCycle == null ? ProposalCycle.ALL : proposalCycle.cycle();

		if (!skipProposalsInFunction(pl.contextExpression)) {
			final boolean restrictedProposals = computeStringProposals(pl) || varInitializationProposals(pl) || proplistKeyProposals(pl);
			if (!restrictedProposals)
				innerProposalsInFunction(pl, parser);
			return true;
		} else
			return false;
	}

	private boolean proplistKeyProposals(ProposalsSite pl) {
		if (pl.contextExpression instanceof PropListExpression)
			return true;
		return false;
	}

	private boolean checkProposalConditions(ProposalsSite pl) {
		try {
			boolean targetCall = false;
			boolean whitespace = false;
			Loop: for (int arrowOffset = pl.wordOffset - 1; arrowOffset >= 1; arrowOffset--) {
				final char c = pl.document.getChar(arrowOffset);
				switch (c) {
				case '.':
					targetCall = true;
					break Loop;
				case '~':
					arrowOffset--;
					//$FALL-THROUGH$
				case '>':
					if (pl.document.getChar(arrowOffset-1) != '-')
						return whitespace || (pl.prefix != null && pl.prefix.length() > 0);
					targetCall = true;
					break Loop;
				case ':':
					if (pl.contextExpression != null && pl.contextExpression.parentOfType(PropListExpression.class) != null)
						return true;
					if (pl.document.getChar(arrowOffset-1) != ':')
						return false;
					targetCall = true;
					break Loop;
				default:
					if (Character.isWhitespace(c)) {
						whitespace = true;
						continue Loop;
					}
					else
						break Loop;
				}
			}
			if (!targetCall && pl.wordOffset >= 0 && Character.isWhitespace(pl.document.getChar(pl.wordOffset)))
				return false;
		} catch (final BadLocationException bl) {
			return false;
		}
		return true;
	}

	private void innerProposalsInFunction(ProposalsSite pl, ScriptParser parser) {
		//if (computeStringProposals(pl) || varInitializationProposals(pl))
		//	return;
		setCategoryOrdering(pl);
		functionLocalProposals(pl);
		definitionProposals(pl);
		engineProposals(pl);
		structureProposals(pl);
		ruleBasedProposals(pl, parser);
		keywordProposals(pl);
	}

	private boolean skipProposalsInFunction(ASTNode contextExpression) {
		return contextExpression instanceof Comment;
	}

	private void engineProposals(ProposalsSite pl) {
		if (pl.script.index().engine() != null) {
			if ((pl.declarationsMask() & DeclMask.FUNCTIONS) != 0)
				for (final Function func : pl.script.index().engine().functions())
					proposalForFunc(pl, editor().script(), func, true);
			if ((pl.declarationsMask() & DeclMask.STATIC_VARIABLES) != 0)
				for (final Variable var : pl.script.index().engine().variables())
					proposalForVar(pl, pl.script.engine(), var);
		}
	}

	private void functionLocalProposals(ProposalsSite pl) {
		if ((pl.declarationsMask() & DeclMask.FUNCTIONS) != 0)
			if (pl.contextSequence == null && pl.function != null) {
				for (final Variable v : pl.function.parameters())
					proposalForVar(pl, pl.function, v);
				for (final Variable v : pl.function.locals())
					proposalForVar(pl, pl.function, v);
			}
	}

	private void definitionProposals(ProposalsSite pl) {
		if ((pl.declarationsMask() & DeclMask.STATIC_VARIABLES) != 0)
			for (final Index i : pl.index.relevantIndexes())
				proposalsForIndex(pl, i);
	}

	private void structureProposals(ProposalsSite pl) {
		final Set<Declaration> proposalTypes = determineProposalTypes(pl);
		if (proposalTypes.size() > 0)
			for (final Declaration s : proposalTypes) {
				proposalsForStructure(s, pl, s);
				if (s instanceof IHasIncludes) {
					@SuppressWarnings("unchecked")
					final Iterable<? extends IHasIncludes<?>> includes =
						((IHasIncludes<IHasIncludes<?>>)s).includes(pl.index, pl.script, GatherIncludesOptions.Recursive);
					for (final IHasIncludes<?> inc : includes)
						proposalsForStructure((Declaration) inc, pl, s);
				}
			}
		else
			proposeAllTheThings(pl);
	}

	public void proposeAllTheThings(ProposalsSite pl) {
		final List<ClonkCompletionProposal> old = new ArrayList<>(pl.proposals.values());
		final List<Index> relevantIndexes = pl.index.relevantIndexes();
		final int declarationMask = pl.declarationsMask();
		for (final Index x : relevantIndexes)
			for (final Map.Entry<String, List<Declaration>> decs : x.declarationMap().entrySet()) {
				final Declaration d = decs.getValue().get(0);
				if ((declarationMask & DeclMask.FUNCTIONS) != 0 && d instanceof Function && !((Function)d).isGlobal())
					proposalForFunc(pl, as(pl.precedingType, Script.class), (Function) d, true);
				else if ((declarationMask & DeclMask.VARIABLES) != 0 && d instanceof Variable && ((Variable)d).scope() == Scope.LOCAL)
					proposalForVar(pl, as(pl.precedingType, Script.class), (Variable)d);
			}
		for (final ClonkCompletionProposal ccp : pl.proposals.values())
			if (!old.contains(ccp))
				ccp.setImage(UI.halfTransparent(ccp.getImage()));
	}

	private Set<Declaration> determineProposalTypes(ProposalsSite pl) {
		final Set<Declaration> contextStructures = new HashSet<Declaration>();
		if (pl.contextSequence != null)
			for (final IType t : pl.precedingType()) {
				Declaration structure;
				if (t instanceof Declaration)
					structure = (Declaration) t;
				else if (t instanceof MetaDefinition)
					structure = ((MetaDefinition)t).definition();
				else
					structure = Script.scriptFrom(t);
				if (structure != null)
					contextStructures.add(structure);
			}
		else
			contextStructures.add(pl.script);
		return contextStructures;
	}

	private void keywordProposals(ProposalsSite pl) {
		if (pl.contextSequence == null && (pl.declarationsMask() & DeclMask.STATIC_VARIABLES) != 0) {
			final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
			for(final String keyword : BuiltInDefinitions.KEYWORDS) {
				if (pl.prefix != null && !stringMatchesPrefix(keyword, pl.prefix))
					continue;
				final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, null, keyword, pl.offset, pl.prefix != null ? pl.prefix.length() : 0, keyword.length(), keywordImg ,
					keyword, null ,null, ": keyword", editor());
				prop.setCategory(cats.Keywords);
				pl.addProposal(prop);
			}
		}
	}

	private boolean varInitializationProposals(ProposalsSite pl) {
		if (pl.contextExpression instanceof VarInitialization) {
			final VarInitialization vi = (VarInitialization)pl.contextExpression;
			Typing typing = Typing.PARAMETERS_OPTIONALLY_TYPED;
			if (pl.index instanceof ProjectIndex)
				typing = ((ProjectIndex)pl.index).nature().settings().typing;
			switch (typing) {
			case STATIC:
				if (eq(vi.type, PrimitiveType.ERRONEOUS)) {
					for (final Index ndx : pl.index.relevantIndexes())
						proposalsForIndexedDefinitions(pl, ndx);
					final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
					for (final PrimitiveType t : PrimitiveType.values())
						if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ERRONEOUS && pl.index.engine().supportsPrimitiveType(t)) {
							final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, null, t.scriptName(), pl.offset, pl.prefix != null ? pl.prefix.length() : 0 , t.scriptName().length(),
								keywordImg , t.scriptName(), null, null, Messages.C4ScriptCompletionProcessor_Engine, editor());
							prop.setCategory(cats.Keywords);
							pl.addProposal(prop);
						}
				}
				break;
			default:
				break;
			}
			return true;
		} else
			return false;
	}

	private void ruleBasedProposals(ProposalsSite pl, ScriptParser parser) {
		if (pl.contextExpression == null)
			return;
		final CallDeclaration innermostCallFunc = pl.contextExpression.thisOrParentOfType(CallDeclaration.class);
		if (innermostCallFunc != null) {
			final SpecialEngineRules rules = parser.specialEngineRules();
			if (rules != null) {
				final SpecialFuncRule funcRule = rules.funcRuleFor(innermostCallFunc.name(), SpecialEngineRules.FUNCTION_PARM_PROPOSALS_CONTRIBUTOR);
				if (funcRule != null) {
					final ASTNode parmExpr = innermostCallFunc.findSubElementContaining(pl.contextExpression);
					funcRule.contributeAdditionalProposals(innermostCallFunc, editor().editingState().typingStrategy().localReporter(parser.script(), parser.fragmentOffset()), innermostCallFunc.indexOfParm(parmExpr), parmExpr, this, pl);
				}
			}
		}
	}

	private void setCategoryOrdering(ProposalsSite pl) {
		cats.defaultOrdering();
		if (pl.contextExpression == null)
			return;
		CallDeclaration innermostCallFunc = pl.contextExpression.parentOfType(CallDeclaration.class);
		// elevate definition proposals for parameters of id type
		Variable parm;
		if (innermostCallFunc != null && pl.contextExpression.parent() == innermostCallFunc)
			parm = innermostCallFunc.parmDefinitionForParmExpression(pl.contextExpression);
		else if ((innermostCallFunc = as(pl.contextExpression, CallDeclaration.class)) != null && innermostCallFunc.params().length == 0 &&
			innermostCallFunc.declaration() instanceof Function && ((Function)innermostCallFunc.declaration()).numParameters() > 0)
			parm = ((Function)innermostCallFunc.declaration()).parameter(0);
		else
			parm = null;
		if (parm != null && parm.type() != PrimitiveType.ANY && parm.type() != PrimitiveType.UNKNOWN)
			if (TypeUnification.unifyNoChoice(parm.type(), PrimitiveType.ID) != null) {
				if (DEBUG)
					System.out.println("Elevate definitions");
				cats.Definitions = -1;
			}
	}

	private boolean computeStringProposals(ProposalsSite pl) {
		// only present completion proposals specific to the <expr>->... thingie if cursor inside identifier region of declaration access expression.
		if (pl.contextExpression instanceof Placeholder || pl.contextExpression instanceof StringLiteral) {
			try {
				if (pl.document.getChar(pl.offset-1) != '$')
					return true;
			} catch (final BadLocationException e1) {
				return true;
			}
			final Set<String> availableLocalizationStrings = new HashSet<>();
			try {
				for (final IResource r : (pl.script.resource() instanceof IContainer ? (IContainer)pl.script.resource() : pl.script.resource().getParent()).members()) {
					if (!(r instanceof IFile))
						continue;
					final IFile f = (IFile) r;
					final Matcher m = StringTbl.PATTERN.matcher(r.getName());
					if (m.matches()) {
						final StringTbl tbl = (StringTbl)Structure.pinned(f, true, false);
						if (tbl != null)
							availableLocalizationStrings.addAll(tbl.map().keySet());
					}
				}
			} catch (final CoreException e) {
				e.printStackTrace();
			}
			final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
			for (final String loc : availableLocalizationStrings) {
				if (pl.prefix != null && !stringMatchesPrefix(loc, pl.prefix))
					continue;
				final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, null, loc, pl.offset, pl.prefix != null ? pl.prefix.length() : 0 , loc.length(),
					keywordImg , loc, null, null, Messages.C4ScriptCompletionProcessor_Engine, editor());
				prop.setCategory(cats.Keywords);
				pl.addProposal(prop);
			}
			return true;
		} else
			return false;
	}

	/**
	 * Generate a list of proposals for some expression.
	 * This static standalone version internally creates a {@link ScriptCompletionProcessor} instance and lets it do the things it does when invoking Content Assist normally.
	 * It is used for computing the list of similarly named declarations when invoking Quick Fix for unknown identifiers.
	 * @param expression The expression preceding the location for which proposals should be generated
	 * @param function The function containing the expression/function from which local variable definitions are pulled
	 * @param parser Parser serving as context
	 * @param document The {@link IDocument} the expression was read from
	 * @return A list of proposals that (hopefully) represent a valid continuation of the given expression
	 */
	public static List<ICompletionProposal> computeProposalsForExpression
		(ASTNode expression, Function function, ScriptParser parser, IDocument document) {
		final List<ICompletionProposal> result = new LinkedList<ICompletionProposal>();
		final ScriptCompletionProcessor processor = new ScriptCompletionProcessor(parser.script());
		final ProposalsSite pl = new ProposalsSite(
			expression != null ? expression.end() : 0,
			0, document, "", result, function.index(), function, parser.script()
		).setPreceding(new PrecedingExpression(expression, expression.parentOfType(Sequence.class), PrimitiveType.UNKNOWN));
		processor.innerProposalsInFunction(pl, parser);
		return result;
	}

	private ClonkCompletionProposal callbackProposal(
		ProposalsSite pl,
		final String callback,
		final String nameFormat,
		final String displayString,
		final boolean funcSupplied,
		final Variable... parameters
	) {
		final Image img = UI.imageForPath("icons/callback.png"); //$NON-NLS-1$
		int replacementLength = 0;
		if (pl.prefix != null)
			replacementLength = pl.prefix.length();
		final ClonkCompletionProposal prop = new ClonkCompletionProposal(
			null, null, "", pl.wordOffset, replacementLength,  //$NON-NLS-1$
			0, img, callback != null ? callback : displayString,
			null, null, Messages.C4ScriptCompletionProcessor_Callback, editor()
		) {
			@Override
			public boolean validate(IDocument document, int offset, DocumentEvent event) {
				final int replaceOffset = replacementOffset();
				try {
					final String prefix = document.get(replaceOffset, offset - replaceOffset).toLowerCase();
					for (final String kw : BuiltInDefinitions.DECLARATORS)
						if (kw.toLowerCase().equals(prefix))
							return false;
					return callback == null || stringMatchesPrefix(callback, prefix);
				} catch (final BadLocationException e) {
					return false;
				}
			}
			@Override
			public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
				final int replaceOffset = replacementOffset();
				String cb = callback;
				if (cb == null)
					try {
						cb = viewer.getDocument().get(replaceOffset, offset - replaceOffset);
					} catch (final BadLocationException e) {
						e.printStackTrace();
						return;
					}
				cb = String.format(nameFormat, cb);
				replacementString = funcSupplied
					? cb
					: Function.scaffoldTextRepresentation(cb, FunctionScope.PUBLIC, editor().script(), parameters); //$NON-NLS-1$
				cursorPosition = replacementString.length()-2;
				super.apply(viewer, trigger, stateMask, offset);
			}
			@Override
			public boolean requiresDocumentReparse() { return true; }
		};
		pl.addProposal(prop);
		return prop;
	}

	private boolean computeProposalsOutsideFunction(ITextViewer viewer, ProposalsSite pl) {
		proposalCycle = ProposalCycle.ALL;
		// check whether func keyword precedes location (whole function blocks won't be created then)
		final boolean funcSupplied = precededBy(viewer, pl.offset, Keywords.Func);
		final boolean directiveExpectingDefinition =
			precededBy(viewer, pl.offset, "#" + Directive.DirectiveType.INCLUDE.toString()) || //$NON-NLS-1$
			precededBy(viewer, pl.offset, "#" + Directive.DirectiveType.APPENDTO.toString()); //$NON-NLS-1$

		for (final String kw: BuiltInDefinitions.DECLARATORS)
			if (precededBy(viewer, pl.offset, kw))
				return false;

		final IDocument doc = viewer.getDocument();
		Check: for (int i = pl.offset; i >= 0; i--)
			try {
				switch (doc.getChar(i)) {
				case '(': case ',': case '=': case ':':
					return false;
				case '\n': case '\r':
					break Check;
				}
			} catch (final BadLocationException e) {
				break;
			}

		if (!directiveExpectingDefinition) {
			overrideProposals(pl, funcSupplied);
			standardCallbackProposals(pl, funcSupplied);
			newFunctionProposal(pl, funcSupplied);
			effectFunctionProposals(pl, funcSupplied);
			if (!funcSupplied) {
				declaratorProposals(pl);
				directiveProposals(pl);
			}
		} else
			directiveDefinitionArgumentProposals(pl);
		return true;
	}

	private void directiveDefinitionArgumentProposals(ProposalsSite pl) {
		// propose objects for #include or something
		for (final Index i : pl.index.relevantIndexes())
			proposalsForIndex(pl, i);
	}

	private void directiveProposals(ProposalsSite pl) {
		// propose directives (#include, ...)
		final Image directiveIcon = UI.imageForPath("icons/directive.png"); //$NON-NLS-1$
		for(final Directive directive : Directive.CANONICALS) {
			String txt = directive.type().toString();
			if (pl.prefix != null)
				if (!stringMatchesPrefix(txt, pl.prefix))
					continue;
			int replacementLength = 0;
			if (pl.prefix != null) replacementLength = pl.prefix.length();
			txt = "#"+txt+" "; //$NON-NLS-1$ //$NON-NLS-2$
			final ClonkCompletionProposal prop = new ClonkCompletionProposal(
				directive, directive, txt, pl.offset, replacementLength, txt.length(),
				directiveIcon, directive.type().toString(), null, null,
				Messages.C4ScriptCompletionProcessor_Engine, editor()
			);
			prop.setCategory(cats.Directives);
			pl.addProposal(prop);
		}
	}

	private void declaratorProposals(ProposalsSite pl) {
		// propose declaration keywords (var, static, ...)
		for(final String declarator : BuiltInDefinitions.DECLARATORS) {
			if (pl.prefix != null)
				if (!stringMatchesPrefix(declarator, pl.prefix))
					continue;
			final Image declaratorImg = UI.imageForPath("icons/declarator.png"); //$NON-NLS-1$
			int replacementLength = 0;
			if (pl.prefix != null) replacementLength = pl.prefix.length();
			final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, null, declarator,pl.offset,replacementLength,declarator.length(), declaratorImg , declarator.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, editor()); //$NON-NLS-1$
			prop.setCategory(cats.Keywords);
			pl.addProposal(prop);
		}
	}

	private void effectFunctionProposals(ProposalsSite pl, final boolean funcSupplied) {
		// propose creating effect functions
		for (final String s : EffectFunction.DEFAULT_CALLBACKS) {
			final IType parameterTypes[] = Effect.parameterTypesForCallback(s, editor.script(), PrimitiveType.ANY);
			final Variable parms[] = new Variable[] {
				new Variable("obj", parameterTypes[0]), //$NON-NLS-1$
				new Variable("effect", parameterTypes[1]) //$NON-NLS-1$
			};
			callbackProposal(pl, null, EffectFunction.FUNCTION_NAME_PREFIX+"%s"+s, //$NON-NLS-1$
				String.format(Messages.C4ScriptCompletionProcessor_EffectFunctionCallbackProposalDisplayStringFormat, s), funcSupplied, parms).setCategory(cats.EffectCallbacks);
		}
	}

	private void newFunctionProposal(ProposalsSite pl, final boolean funcSupplied) {
		// propose to just create function with the name already typed
		if (pl.untamperedPrefix != null && pl.untamperedPrefix.length() > 0)
			callbackProposal(pl, null, "%s", Messages.C4ScriptCompletionProcessor_InsertFunctionScaffoldProposalDisplayString, funcSupplied).setCategory(cats.NewFunction); //$NON-NLS-1$
	}

	private void standardCallbackProposals(ProposalsSite pl, final boolean funcSupplied) {
		// propose creating functions for standard callbacks
		for(final String callback : editor().script().engine().settings().callbackFunctions()) {
			if (pl.prefix != null)
				if (!stringMatchesPrefix(callback, pl.prefix))
					continue;
			callbackProposal(pl, callback, "%s", null, funcSupplied).setCategory(cats.Callbacks); //$NON-NLS-1$
		}
	}

	private void overrideProposals(ProposalsSite pl, final boolean funcSupplied) {
		// propose overriding inherited functions
		final Script script = editor().script();
		final List<Script> cong = script.conglomerate();
		for (final Script c : cong)
			if (c != script)
				for (final Declaration dec : c.subDeclarations(pl.index, DeclMask.FUNCTIONS)) {
					if (!script.seesSubDeclaration(dec))
						continue;
					final Function func = as(dec, Function.class);
					callbackProposal(pl, func.name(), "%s", null, funcSupplied, func.parameters().toArray(new Variable[func.numParameters()])).setCategory(cats.Callbacks);
				}
	}

	private static boolean precededBy(ITextViewer viewer, int offset, String what) {
		try {
			do
				if (offset >= what.length() + 1 && viewer.getDocument().get(offset - what.length() - 1, what.length()).equalsIgnoreCase(what))
					return true;
			while (offset-- > 0 && Character.isWhitespace(viewer.getDocument().getChar(offset)));
			return false;
		} catch (final BadLocationException e) {
			return false;
		}
	}

	private String proposalCycleMessage() {
		final TriggerSequence sequence = iterationBinding();
		if (proposalCycle != null)
			return String.format(Messages.C4ScriptCompletionProcessor_PressToShowCycle, sequence.format(), proposalCycle.cycle().description());
		else
			return "";
	}

	private void proposalsForStructure(Declaration structure, ProposalsSite pl, Declaration target) {
		for (final Declaration dec : structure.subDeclarations(pl.index, pl.declarationsMask())) {
			if (!target.seesSubDeclaration(dec))
				continue;
			final Function func = as(dec, Function.class);
			final Variable var = as(dec, Variable.class);
			if (func != null && func.visibility() != FunctionScope.GLOBAL) {
				if (target instanceof Script && !((Script)target).seesFunction(func))
					continue;
				final ClonkCompletionProposal prop = proposalForFunc(pl, target, func, true);
				if (prop != null)
					prop.setCategory(cats.LocalFunction);
			}
			else if (var != null)
				proposalForVar(pl, target, var);
		}
	}

	protected Function funcAt(IDocument document, int offset) {
		final Script thisScript = Utilities.scriptForEditor(editor);
		return thisScript != null ? thisScript.funcAt(new Region(offset,1)) : null;
	}

	private IContextInformation prevInformation;
	private final ScriptContextInformationValidator contextInformationValidator = new ScriptContextInformationValidator();

	Function functionFromCall(IFunctionCall call) {
		if (call instanceof CallDeclaration)
			return as(((CallDeclaration) call).declaration(), Function.class);
		else
			return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		IContextInformation info = null;
		try {
			final Function cursorFunc = editor().functionAtCursor();
			if (cursorFunc != null)
				editor().editingState().updateFunctionFragment(cursorFunc, null, false);
			final FuncCallInfo funcCallInfo = editor.innermostFunctionCallParmAtOffset(offset);
			if (funcCallInfo != null) {
				IIndexEntity entity = functionFromCall(funcCallInfo.callFunc);
				if (entity == null) {
					final RegionDescription d = new RegionDescription();
					if (funcCallInfo.locator.initializeRegionDescription(d, editor().script(), new Region(offset, 1))) {
						funcCallInfo.locator.initializeProposedDeclarations(editor().script(), d, null, (ASTNode)funcCallInfo.callFunc);
						Function commono = null;
						final Set<? extends IIndexEntity> potentials = funcCallInfo.locator.potentialEntities();
						if (potentials != null)
							if (potentials.size() == 1)
								entity = potentials.iterator().next();
							else for (final IIndexEntity e : potentials) {
								if (commono == null)
									commono = new Function(Messages.C4ScriptCompletionProcessor_MultipleCandidates, FunctionScope.PRIVATE);
								entity = commono;
								final Function f = functionFromEntity(e);
								if (f != null)
									for (int i = 0; i < f.numParameters(); i++) {
										final Variable fpar = f.parameter(i);
										final Variable cpar = commono.numParameters() > i
											? commono.parameter(i)
												: commono.addParameter(new Variable(fpar.name(), fpar.type()));
											cpar.forceType(TypeUnification.unify(cpar.type(), fpar.type()));
											if (!Arrays.asList(cpar.name().split("/")).contains(fpar.name())) //$NON-NLS-1$
												cpar.setName(cpar.name()+"/"+fpar.name()); //$NON-NLS-1$
									}
							}
					}
				}
				final Function function = functionFromEntity(entity);
				if (function != null) {
					if (function instanceof IDocumentedDeclaration)
						((IDocumentedDeclaration)function).fetchDocumentation();
					Script context;
					final ASTNode pred = funcCallInfo.callPredecessor();
					if (pred != null) {
						final IType[] map = cursorFunc.script().typings().functionASTTypes.get(cursorFunc.name());
						/*cursorFunc.body().traverse(new IASTVisitor<Void>() {
							@Override
							public TraversalContinuation visitNode(ASTNode node, Void context) {
								if (node.localIdentifier() > 0)
									System.out.println(String.format("%03d - %s: %s", node.localIdentifier(), node.printed(), defaulting(map[node.localIdentifier()], PrimitiveType.UNKNOWN).typeName(true)));
								return TraversalContinuation.Continue;
							}
						}, null);*/
						context = defaulting(as(map[pred.localIdentifier()], Script.class), cursorFunc.script());
					} else
						context = editor().script();
					info = new ScriptContextInformation(
						context, function.name() + "()", UI.CLONK_ENGINE_ICON, //$NON-NLS-1$
						function, funcCallInfo.parmIndex,
						funcCallInfo.parmsStart, funcCallInfo.parmsEnd
					);
					contextInformationValidator.install(info, viewer, offset);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		try {
			// HACK: if changed, hide the old one -.-
			if (!Utilities.eq(prevInformation, info))
				hideProposals();
			return info != null ? new IContextInformation[] {info} : null;
		} finally {
			prevInformation = info;
		}
	}

	public void hideProposals() {
		final ScriptContentAssistant assistant = as(this.editor().contentAssistant(), ScriptContentAssistant.class);
		if (assistant != null)
			assistant.hide();
	}

	protected Function functionFromEntity(IIndexEntity entity) {
		Function function = null;
		if (entity instanceof Function)
			function = (Function)entity;
		else if (entity instanceof Variable) {
			final IType type = ((Variable)entity).type();
			if (type instanceof FunctionType)
				function = ((FunctionType)type).prototype();
		}
		return function;
	}

	private final static char[][] proposalAutoActivationCharacters = new char[2][];
	private static char[] contextInformationAutoActivationCharacters;

	private static void configureActivation() {
		proposalAutoActivationCharacters[1] = ClonkPreferences.toggle(ClonkPreferences.INSTANT_C4SCRIPT_COMPLETIONS, false)
			? "~:_.>ABCDEFGHIJKLMNOPQRSTVUWXYZabcdefghijklmnopqrstvuwxyz$".toCharArray() //$NON-NLS-1$
			: new char[0];
		proposalAutoActivationCharacters[0] = new char[0];
		contextInformationAutoActivationCharacters = new char[] {'('};
	}

	static {
		final IPreferenceStore prefStore = Core.instance().getPreferenceStore();
		if (prefStore != null)
			prefStore.addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(ClonkPreferences.INSTANT_C4SCRIPT_COMPLETIONS))
						configureActivation();
				}
			});
		configureActivation();
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() { return proposalAutoActivationCharacters[editor().functionAtCursor() != null ? 1 : 0]; }
	@Override
	public char[] getContextInformationAutoActivationCharacters() { return contextInformationAutoActivationCharacters; }
	@Override
	public IContextInformationValidator getContextInformationValidator() { return contextInformationValidator; }
	@Override
	public String getErrorMessage() { return null; }

	private KeySequence iterationBinding() {
		final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		final TriggerSequence binding = bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		if (binding instanceof KeySequence)
			return (KeySequence) binding;
		return null;
	}

}
