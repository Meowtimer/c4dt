package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.c4script.typing.TypeUnification.unifyNoChoice;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

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
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.Placeholder;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ast.TraversalContinuation;
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
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SpecialEngineRules;
import net.arctics.clonk.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.IFunctionCall;
import net.arctics.clonk.c4script.ast.MemberOperator;
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
	private ProposalCycle proposalCycle = ProposalCycle.ALL;
	private Function _activeFunc;
	private ProblemReportingStrategy typingStrategy;

	private void setTypingStrategyFromScript(Script script) {
		if (script.index().nature() != null)
			typingStrategy = script.index().nature().instantiateProblemReportingStrategies(Capabilities.TYPING).get(0);
	}

	public ScriptCompletionProcessor(Script script) {
		super(null, null);
		assistant = null;
		setTypingStrategyFromScript(script);
	}

	public ScriptCompletionProcessor(C4ScriptEditor editor, ContentAssistant assistant) {
		super(editor, assistant);
		if (editor != null)
			setTypingStrategyFromScript(editor.script());
		this.assistant = assistant;
		if (assistant != null) {
			assistant.setRepeatedInvocationTrigger(iterationBinding());
			assistant.addCompletionListener(this);
		}
	}

	@Override
	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {}

	@Override
	public void assistSessionStarted(ContentAssistEvent event) { proposalCycle = ProposalCycle.ALL; }

	@Override
	public void assistSessionEnded(ContentAssistEvent event) {}

	@Override
	public void assistSessionRestarted(ContentAssistEvent event) {
		// needs to be reversed because it gets cycled after computing the proposals...
		proposalCycle = proposalCycle.reverseCycle();
	}

	protected void doCycle() {
		proposalCycle = proposalCycle.cycle();
	}

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
	private void proposalsForIndex(Index index, int offset, int wordOffset, String prefix, List<ICompletionProposal> proposals, int flags, Script editorScript) {
		if (_activeFunc != null) {
			final Scenario s2 = _activeFunc.scenario();
			if ((flags & DeclMask.FUNCTIONS) != 0)
				for (final Function func : index.globalFunctions()) {
					final Scenario fScen = func.scenario();
					if (fScen != null && fScen != s2)
						continue;
					proposalForFunc(func, prefix, offset, proposals, true);
				}
			if ((flags & DeclMask.STATIC_VARIABLES) != 0)
				for (final Variable var : index.staticVariables()) {
					// ignore static variables from editor script since those are proposed already
					if (var.parentDeclaration() == editorScript)
						continue;
					final Scenario vScen = var.scenario();
					if (vScen != null && vScen != s2)
						continue;
					proposalForVar(var,prefix,offset,proposals);
				}
		}
		if ((flags & DeclMask.STATIC_VARIABLES) != 0)
			proposalsForIndexedDefinitions(index, offset, wordOffset, prefix, proposals);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		super.computeCompletionProposals(viewer, offset);
		int wordOffset = offset - 1;
		final IDocument doc = viewer.getDocument();
		String prefix = null;
		try {
			while (BufferedScanner.isWordPart(doc.getChar(wordOffset)) || Character.isLetter(doc.getChar(wordOffset)))
				wordOffset--;
			wordOffset++;
			if (wordOffset < offset) {
				prefix = doc.get(wordOffset, offset - wordOffset);
				offset = wordOffset;
			} else
				prefix = "";
			if (prefix.length() > 0 && !ClonkCompletionProposal.validPrefix(prefix))
				return null;
		} catch (final BadLocationException e) { }

		this.untamperedPrefix = prefix;
		if (prefix != null)
			prefix = prefix.toLowerCase();
		this.prefix = prefix;

		final ClonkProjectNature nature = ClonkProjectNature.get(editor);
		final List<String> statusMessages = new ArrayList<String>(4);
		final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		final Index index = nature.index();

		final Function activeFunc = funcAt(doc, offset);
		this._activeFunc = activeFunc;

		statusMessages.add(Messages.C4ScriptCompletionProcessor_ProjectFiles);

		if (proposalCycle == ProposalCycle.ALL || activeFunc == null)
			if (editor().script().index().engine() != null)
				statusMessages.add(Messages.C4ScriptCompletionProcessor_EngineFunctions);

		final boolean returnProposals = activeFunc == null
			? proposalsOutsideOfFunction(viewer, offset, wordOffset, prefix, proposals, index)
			: computeProposalsInFunction(offset, wordOffset, doc, prefix, proposals, index, activeFunc);
		if (!returnProposals)
			return null;

		final StringBuilder statusMessage = new StringBuilder(Messages.C4ScriptCompletionProcessor_ShownData);
		for(final String message : statusMessages) {
			statusMessage.append(message);
			if (statusMessages.get(statusMessages.size() - 1) != message) statusMessage.append(", "); //$NON-NLS-1$
		}

		//assistant.setStatusMessage(statusMessage.toString());
		assistant.setStatusMessage(proposalCycleMessage());

		doCycle();

		if (proposals.size() > 0)
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
		else
			return null;
	}

	class PrecedingExpressionTypeExtractor extends ExpressionLocator<ProblemReporter> {
		public ASTNode contextExpression;
		public Sequence contextSequence;
		public IType precedingType;
		public void pos(int pos) { this.exprRegion = new Region(pos, 0); exprAtRegion = null; }
		public PrecedingExpressionTypeExtractor() { super(-1); }
		@Override
		public TraversalContinuation visitNode(ASTNode expression, ProblemReporter context) {
			final ASTNode old = exprAtRegion;
			final TraversalContinuation c = super.visitNode(expression, context);
			if (old != exprAtRegion) {
				contextExpression = exprAtRegion;
				if (
					contextExpression instanceof MemberOperator ||
					(contextExpression instanceof AccessDeclaration && Utilities.regionContainsOffset(contextExpression.identifierRegion(), exprRegion.getOffset()))
				) {
					// we only care about sequences
					final ASTNode pred = contextExpression.predecessorInSequence();
					contextSequence = pred != null ? Utilities.as(contextExpression.parent(), Sequence.class) : null;
					if (contextSequence != null)
						contextSequence = contextSequence.subSequenceIncluding(contextExpression);
					precedingType = pred != null ? context.typeOf(pred) : null;
				}
			}
			return c;
		}
		public IType precedingType() { return defaulting(precedingType, PrimitiveType.UNKNOWN); }
	}

	final PrecedingExpressionTypeExtractor typeExtractor = new PrecedingExpressionTypeExtractor();

	private boolean computeProposalsInFunction(int offset, int wordOffset,
		IDocument doc, String prefix,
		List<ICompletionProposal> proposals, Index index,
		final Function activeFunc
	) {
		if (!checkProposalConditions(wordOffset, doc))
			return false;
		final Script editorScript = Utilities.scriptForEditor(editor);
		final int preservedOffset = offset - (activeFunc != null?activeFunc.bodyLocation().start():0);

		typeExtractor.pos(preservedOffset);
		ScriptParser parser = null;
		if (editorScript != null)
			if (parser == null) {
				final ScriptEditingState editingState = editor().editingState();
				parser = editingState.updateFunctionFragment(activeFunc, typeExtractor, true);
			}

		if (!skipProposalsInFunction(typeExtractor.contextExpression)) {
			innerProposalsInFunction(
				offset, wordOffset, doc, prefix,
				proposals, index, activeFunc, editorScript, parser,
				typeExtractor.contextSequence, typeExtractor.contextExpression, typeExtractor.precedingType()
			);
			return true;
		} else
			return false;
	}

	private boolean checkProposalConditions(int wordOffset, IDocument doc) {
		try {
			boolean targetCall = false;
			Loop: for (int arrowOffset = wordOffset - 1; arrowOffset >= 1; arrowOffset--) {
				final char c = doc.getChar(arrowOffset);
				switch (c) {
				case '.':
					targetCall = true;
					break Loop;
				case '>':
					if (doc.getChar(arrowOffset-1) != '-')
						return false;
					targetCall = true;
					break Loop;
				case ':':
					if (doc.getChar(arrowOffset-1) != ':')
						return false;
					targetCall = true;
					break Loop;
				default:
					if (Character.isWhitespace(c))
						continue Loop;
					else
						break Loop;
				}
			}
			if (!targetCall && wordOffset >= 0 && Character.isWhitespace(doc.getChar(wordOffset)))
				return false;
		} catch (final BadLocationException bl) {
			return false;
		}
		return true;
	}

	private void innerProposalsInFunction(int offset, int wordOffset, IDocument doc, String prefix, List<ICompletionProposal> proposals, Index index, final Function activeFunc, Script editorScript, ScriptParser parser, final Sequence contextSequence, final ASTNode contextExpression, final IType sequenceType) {
		if (DEBUG)
			System.out.println(String.format("%s: %s %s %s",
				prefix,
				contextExpression != null ? contextExpression.printed() : "null",
				contextSequence != null ? contextSequence.printed() : "null",
				sequenceType != null ? sequenceType.typeName(true) : "null"
			));
		final int whatToDisplayFromScripts = declarationMask(contextSequence);
		if (computeStringProposals(offset, doc, prefix, proposals, editorScript, contextExpression))
			return;
		setCategoryOrdering(contextExpression);
		if (varInitializationProposals(offset, wordOffset, prefix, proposals, index, contextExpression))
			return;
		if (unifyNoChoice(PrimitiveType.PROPLIST, sequenceType) != null) {
			engineProposals(offset, prefix, proposals, editorScript, contextSequence);
			functionLocalProposals(wordOffset, prefix, proposals, activeFunc, contextSequence);
			definitionProposals(offset, wordOffset, prefix, proposals, index, editorScript, whatToDisplayFromScripts);
			structureProposals(offset, wordOffset, prefix, proposals, index, editorScript, contextSequence, sequenceType, whatToDisplayFromScripts);
		}
		ruleBasedProposals(offset, prefix, proposals, parser, contextExpression);
		keywordProposals(offset, prefix, proposals, contextSequence);
	}

	private boolean skipProposalsInFunction(ASTNode contextExpression) {
		return contextExpression instanceof Comment;
	}

	private void engineProposals(int offset, String prefix, List<ICompletionProposal> proposals, Script editorScript, final Sequence contextSequence) {
		if (proposalCycle == ProposalCycle.ALL)
			if (editorScript.index().engine() != null && (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))) {
				for (final Function func : editorScript.index().engine().functions())
					proposalForFunc(func, prefix, offset, proposals, true);
				if (contextSequence == null)
					for (final Variable var : editorScript.index().engine().variables())
						proposalForVar(var,prefix,offset,proposals);
			}
	}

	private void functionLocalProposals(int wordOffset, String prefix, List<ICompletionProposal> proposals, final Function activeFunc, final Sequence contextSequence) {
		if (contextSequence == null && (proposalCycle == ProposalCycle.ALL || proposalCycle == ProposalCycle.LOCAL) && activeFunc != null) {
			for (final Variable v : activeFunc.parameters())
				proposalForVar(v, prefix, wordOffset, proposals);
			for (final Variable v : activeFunc.locals())
				proposalForVar(v, prefix, wordOffset, proposals);
		}
	}

	private int declarationMask(final Sequence contextSequence) {
		int whatToDisplayFromScripts = 0;
		if (contextSequence == null || MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= DeclMask.VARIABLES;
		if (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= DeclMask.FUNCTIONS;
		if (contextSequence == null)
			whatToDisplayFromScripts |= DeclMask.STATIC_VARIABLES;
		return whatToDisplayFromScripts;
	}

	private void definitionProposals(int offset, int wordOffset, String prefix, List<ICompletionProposal> proposals, Index index, Script editorScript, int whatToDisplayFromScripts) {
		if (proposalCycle != ProposalCycle.OBJECT)
			for (final Index i : index.relevantIndexes())
				proposalsForIndex(i, offset, wordOffset, prefix, proposals, whatToDisplayFromScripts, editorScript);
	}

	private void structureProposals(int offset, int wordOffset, String prefix, List<ICompletionProposal> proposals, Index index, Script editorScript, final Sequence contextSequence, final IType sequenceType, int whatToDisplayFromScripts) {
		final Set<Declaration> proposalTypes = determineProposalTypes(editorScript, contextSequence, sequenceType);
		if (proposalTypes.size() > 0)
			for (final Declaration s : proposalTypes) {
				proposalsForStructure(s, prefix, offset, wordOffset, proposals, index, whatToDisplayFromScripts, s);
				if (s instanceof IHasIncludes) {
					@SuppressWarnings("unchecked")
					final Iterable<? extends IHasIncludes<?>> includes =
						((IHasIncludes<IHasIncludes<?>>)s).includes(index, editorScript, GatherIncludesOptions.Recursive);
					for (final IHasIncludes<?> inc : includes)
						proposalsForStructure((Declaration) inc, prefix, offset, wordOffset, proposals, index, whatToDisplayFromScripts, s);
				}
			}
		else
			proposeAllTheThings(offset, prefix, proposals, index);
	}

	public void proposeAllTheThings(int offset, String prefix, List<ICompletionProposal> proposals, Index index) {
		final List<ICompletionProposal> old = new ArrayList<>(proposals);
		final List<Index> relevantIndexes = index.relevantIndexes();
		for (final Index x : relevantIndexes)
			for (final Map.Entry<String, List<Declaration>> decs : x.declarationMap().entrySet()) {
				final Declaration d = decs.getValue().get(0);
				if (d instanceof Function && !((Function)d).isGlobal())
					proposalForFunc((Function) d, prefix, offset, proposals, true);
				else if (d instanceof Variable && ((Variable)d).scope() == Scope.LOCAL)
					proposalForVar((Variable)d, prefix, offset, proposals);
			}
		for (final ICompletionProposal p : proposals)
			if (p instanceof ClonkCompletionProposal && !old.contains(p)) {
				final ClonkCompletionProposal ccp = (ClonkCompletionProposal)p;
				ccp.setImage(UI.halfTransparent(ccp.getImage()));
			}
	}

	private Set<Declaration> determineProposalTypes(Script editorScript, final Sequence contextSequence, final IType sequenceType) {
		final Set<Declaration> contextStructures = new HashSet<Declaration>();
		if (contextSequence != null)
			for (final IType t : sequenceType) {
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
			contextStructures.add(editorScript);
		return contextStructures;
	}

	private void keywordProposals(int offset, String prefix, List<ICompletionProposal> proposals, final Sequence contextSequence) {
		if (contextSequence == null && proposalCycle == ProposalCycle.ALL) {
			final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
			for(final String keyword : BuiltInDefinitions.KEYWORDS) {
				if (prefix != null && !stringMatchesPrefix(keyword, prefix))
					continue;
				final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, keyword, offset, prefix != null ? prefix.length() : 0, keyword.length(), keywordImg ,
					keyword, null ,null, ": keyword", editor());
				prop.setCategory(cats.Keywords);
				proposals.add(prop);
			}
		}
	}

	private boolean varInitializationProposals(int offset, int wordOffset, String prefix, List<ICompletionProposal> proposals, Index index, final ASTNode contextExpression) {
		if (contextExpression instanceof VarInitialization) {
			final VarInitialization vi = (VarInitialization)contextExpression;
			Typing typing = Typing.PARAMETERS_OPTIONALLY_TYPED;
			if (index instanceof ProjectIndex)
				typing = ((ProjectIndex)index).nature().settings().typing;
			switch (typing) {
			case STATIC:
				if (eq(vi.type, PrimitiveType.ERRONEOUS)) {
					proposalsForIndexedDefinitions(index, offset, wordOffset, prefix, proposals);
					final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
					for (final PrimitiveType t : PrimitiveType.values())
						if (t != PrimitiveType.UNKNOWN && index.engine().supportsPrimitiveType(t)) {
							final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, t.scriptName(), offset, prefix != null ? prefix.length() : 0 , t.scriptName().length(),
								keywordImg , t.scriptName(), null, null, Messages.C4ScriptCompletionProcessor_Engine, editor());
							prop.setCategory(cats.Keywords);
							proposals.add(prop);
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

	private void ruleBasedProposals(int offset, String prefix, List<ICompletionProposal> proposals, ScriptParser parser, ASTNode contextExpression) {
		if (contextExpression == null)
			return;
		final CallDeclaration innermostCallFunc = contextExpression.thisOrParentOfType(CallDeclaration.class);
		if (innermostCallFunc != null) {
			final SpecialEngineRules rules = parser.specialEngineRules();
			if (rules != null) {
				final SpecialFuncRule funcRule = rules.funcRuleFor(innermostCallFunc.name(), SpecialEngineRules.FUNCTION_PARM_PROPOSALS_CONTRIBUTOR);
				if (funcRule != null) {
					final ASTNode parmExpr = innermostCallFunc.findSubElementContaining(contextExpression);
					funcRule.contributeAdditionalProposals(innermostCallFunc, typingStrategy.localReporter(parser.script(), parser.fragmentOffset(), null), innermostCallFunc.indexOfParm(parmExpr), parmExpr, this, prefix, offset, proposals);
				}
			}
		}
	}

	private void setCategoryOrdering(ASTNode contextExpression) {
		cats.defaultOrdering();
		if (contextExpression == null)
			return;
		CallDeclaration innermostCallFunc = contextExpression.parentOfType(CallDeclaration.class);
		// elevate definition proposals for parameters of id type
		Variable parm;
		if (innermostCallFunc != null && contextExpression.parent() == innermostCallFunc)
			parm = innermostCallFunc.parmDefinitionForParmExpression(contextExpression);
		else if ((innermostCallFunc = as(contextExpression, CallDeclaration.class)) != null && innermostCallFunc.params().length == 0 &&
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

	private boolean computeStringProposals(int offset, IDocument doc, String prefix, List<ICompletionProposal> proposals, Script editorScript, ASTNode contextExpression) {
		// only present completion proposals specific to the <expr>->... thingie if cursor inside identifier region of declaration access expression.
		if (contextExpression instanceof Placeholder || contextExpression instanceof StringLiteral) {
			try {
				if (doc.getChar(offset-1) != '$')
					return true;
			} catch (final BadLocationException e1) {
				return true;
			}
			final Set<String> availableLocalizationStrings = new HashSet<>();
			try {
				for (final IResource r : (editorScript.resource() instanceof IContainer ? (IContainer)editorScript.resource() : editorScript.resource().getParent()).members()) {
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
				if (prefix != null && !stringMatchesPrefix(loc, prefix))
					continue;
				final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, loc, offset, prefix != null ? prefix.length() : 0 , loc.length(),
					keywordImg , loc, null, null, Messages.C4ScriptCompletionProcessor_Engine, editor());
				prop.setCategory(cats.Keywords);
				proposals.add(prop);
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
	public static List<ICompletionProposal> computeProposalsForExpression(ASTNode expression, Function function, ScriptParser parser, IDocument document) {
		final List<ICompletionProposal> result = new LinkedList<ICompletionProposal>();
		final ScriptCompletionProcessor processor = new ScriptCompletionProcessor(parser.script());
		processor.innerProposalsInFunction(
			expression != null ? expression.end() : 0,
			0, document, "", result, function.index(), function, function.script(), parser, expression.parentOfType(Sequence.class), expression, PrimitiveType.UNKNOWN);
		return result;
	}

	private ClonkCompletionProposal callbackProposal(
		final String prefix,
		final String callback,
		final String nameFormat,
		final String displayString,
		final boolean funcSupplied,
		final List<ICompletionProposal> proposals,
		final int offset,
		final Variable... parameters
	) {
		final Image img = UI.imageForPath("icons/callback.png"); //$NON-NLS-1$
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		final ClonkCompletionProposal prop = new ClonkCompletionProposal(
			null, "", offset, replacementLength,  //$NON-NLS-1$
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
					: Function.scaffoldTextRepresentation(cb, FunctionScope.PUBLIC, editor().script().index(), parameters); //$NON-NLS-1$
				cursorPosition = replacementString.length()-2;
				super.apply(viewer, trigger, stateMask, offset);
			}
			@Override
			public boolean requiresDocumentReparse() { return true; }
		};
		proposals.add(prop);
		return prop;
	}

	private boolean proposalsOutsideOfFunction(
		ITextViewer viewer, int offset,
		int wordOffset, String prefix,
		List<ICompletionProposal> proposals, Index index
	) {
		// check whether func keyword precedes location (whole function blocks won't be created then)
		final boolean funcSupplied = precededBy(viewer, offset, Keywords.Func);
		final boolean directiveExpectingDefinition =
			precededBy(viewer, offset, "#" + Directive.DirectiveType.INCLUDE.toString()) || //$NON-NLS-1$
			precededBy(viewer, offset, "#" + Directive.DirectiveType.APPENDTO.toString()); //$NON-NLS-1$

		for (final String kw: BuiltInDefinitions.DECLARATORS)
			if (precededBy(viewer, offset, kw))
				return false;

		final IDocument doc = viewer.getDocument();
		Check: for (int i = offset; i >= 0; i--)
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

			// propose overriding inherited functions
			final Script script = editor().script();
			final List<Script> cong = script.conglomerate();
			for (final Script c : cong)
				if (c != script)
					for (final Declaration dec : c.subDeclarations(index, DeclMask.FUNCTIONS)) {
						if (!script.seesSubDeclaration(dec))
							continue;
						final Function func = as(dec, Function.class);
						callbackProposal(prefix, func.name(), "%s", null, funcSupplied, proposals, offset, func.parameters().toArray(new Variable[func.numParameters()])).setCategory(cats.Callbacks);
					}

			// propose creating functions for standard callbacks
			for(final String callback : script.engine().settings().callbackFunctions()) {
				if (prefix != null)
					if (!stringMatchesPrefix(callback, prefix))
						continue;
				callbackProposal(prefix, callback, "%s", null, funcSupplied, proposals, offset).setCategory(cats.Callbacks); //$NON-NLS-1$
			}
			// propose to just create function with the name already typed
			if (untamperedPrefix != null && untamperedPrefix.length() > 0)
				callbackProposal(prefix, null, "%s", Messages.C4ScriptCompletionProcessor_InsertFunctionScaffoldProposalDisplayString, funcSupplied, proposals, offset).setCategory(cats.NewFunction); //$NON-NLS-1$

			// propose creating effect functions
			for (final String s : EffectFunction.DEFAULT_CALLBACKS) {
				final IType parameterTypes[] = Effect.parameterTypesForCallback(s, editor.script(), PrimitiveType.ANY);
				final Variable parms[] = new Variable[] {
					new Variable("obj", parameterTypes[0]), //$NON-NLS-1$
					new Variable("effect", parameterTypes[1]) //$NON-NLS-1$
				};
				callbackProposal(prefix, null, EffectFunction.FUNCTION_NAME_PREFIX+"%s"+s, //$NON-NLS-1$
					String.format(Messages.C4ScriptCompletionProcessor_EffectFunctionCallbackProposalDisplayStringFormat, s), funcSupplied, proposals, wordOffset, parms).setCategory(cats.EffectCallbacks);
			}

			if (!funcSupplied) {
				// propose declaration keywords (var, static, ...)
				for(final String declarator : BuiltInDefinitions.DECLARATORS) {
					if (prefix != null)
						if (!stringMatchesPrefix(declarator, prefix))
							continue;
					final Image declaratorImg = UI.imageForPath("icons/declarator.png"); //$NON-NLS-1$
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, declarator,offset,replacementLength,declarator.length(), declaratorImg , declarator.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, editor()); //$NON-NLS-1$
					prop.setCategory(cats.Keywords);
					proposals.add(prop);
				}
				// propose directives (#include, ...)
				final Image directiveIcon = UI.imageForPath("icons/directive.png"); //$NON-NLS-1$
				for(final Directive directive : Directive.CANONICALS) {
					String txt = directive.type().toString();
					if (prefix != null)
						if (!stringMatchesPrefix(txt, prefix))
							continue;
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					txt = "#"+txt+" "; //$NON-NLS-1$ //$NON-NLS-2$
					final ClonkCompletionProposal prop = new ClonkCompletionProposal(
						directive, txt, offset, replacementLength, txt.length(),
						directiveIcon, directive.type().toString(), null, null,
						Messages.C4ScriptCompletionProcessor_Engine, editor()
					);
					prop.setCategory(cats.Directives);
					proposals.add(prop);
				}
			}
		}
		// propose objects for #include or something
		if (directiveExpectingDefinition) {
			final Script editorScript = Utilities.scriptForEditor(editor);
			if (prefix == null)
				prefix = ""; //$NON-NLS-1$
			for (final Index i : index.relevantIndexes())
				proposalsForIndex(i, offset, wordOffset, prefix, proposals, DeclMask.STATIC_VARIABLES, editorScript);
		}
		return true;
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
		return String.format(Messages.C4ScriptCompletionProcessor_PressToShowCycle, sequence.format(), proposalCycle.cycle().description());
	}

	private void proposalsForStructure(Declaration structure, String prefix, int offset, int wordOffset, List<ICompletionProposal> proposals, Index index, int mask, Declaration target) {
		for (final Declaration dec : structure.subDeclarations(index, mask)) {
			if (!target.seesSubDeclaration(dec))
				continue;
			final Function func = as(dec, Function.class);
			final Variable var = as(dec, Variable.class);
			if (func != null && func.visibility() != FunctionScope.GLOBAL) {
				if (target instanceof Script && !((Script)target).seesFunction(func))
					continue;
				final ClonkCompletionProposal prop = proposalForFunc(func, prefix, offset, proposals, true);
				if (prop != null)
					prop.setCategory(cats.LocalFunction);
			}
			else if (var != null)
				proposalForVar(var, prefix, wordOffset, proposals);
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
					info = new ScriptContextInformation(
						function.name() + "()", UI.CLONK_ENGINE_ICON, //$NON-NLS-1$
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
			? ":_.>ABCDEFGHIJKLMNOPQRSTVUWXYZabcdefghijklmnopqrstvuwxyz$".toCharArray() //$NON-NLS-1$
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
