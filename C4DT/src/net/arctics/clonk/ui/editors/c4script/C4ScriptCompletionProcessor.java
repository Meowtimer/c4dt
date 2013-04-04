package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IDocumentedDeclaration;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.DeclMask;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.BuiltInDefinitions;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.FunctionFragmentParser;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.ast.VarInitialization;
import net.arctics.clonk.parser.c4script.effect.Effect;
import net.arctics.clonk.parser.c4script.effect.EffectFunction;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ProjectSettings.Typing;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor.FuncCallInfo;
import net.arctics.clonk.ui.editors.c4script.C4ScriptSourceViewerConfiguration.C4ScriptContentAssistant;
import net.arctics.clonk.ui.editors.c4script.EntityLocator.RegionDescription;
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
public class C4ScriptCompletionProcessor extends ClonkCompletionProcessor<C4ScriptEditor> implements ICompletionListener, ICompletionListenerExtension {

	private final ContentAssistant assistant;
	private ASTNode contextExpression;
	private ProposalCycle proposalCycle = ProposalCycle.ALL;
	private Function _activeFunc;
	private Script _currentEditorScript;
	private String untamperedPrefix;
	private ProblemReportingStrategy typingStrategy;
	private ProblemReportingContext typingContext;

	private void setTypingStrategyFromScript(Script script) {
		if (script.index().nature() != null)
			typingStrategy = script.index().nature().instantiateProblemReportingStrategies(Capabilities.TYPING).get(0);
	}

	public C4ScriptCompletionProcessor(Script script) {
		super(null, null);
		assistant = null;
		setTypingStrategyFromScript(script);
	}

	public C4ScriptCompletionProcessor(C4ScriptEditor editor, ContentAssistant assistant) {
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
					final Scenario s1 = func.scenario();
					if (s1 != null && s2 != null && s1 != s2)
						continue;
					proposalForFunc(func, prefix, offset, proposals, func.script().name(), true);
				}
			if ((flags & DeclMask.STATIC_VARIABLES) != 0)
				for (final Variable var : index.staticVariables()) {
					// ignore static variables from editor script since those are proposed already
					if (var.parentDeclaration() == editorScript)
						continue;
					final Scenario s1 = var.scenario();
					if (s1 != null && s1 != s2)
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
			}
			final String brombeeren = doc.get(offset, viewer.getSelectedRange().x-offset);
			if (brombeeren.length() > 0 && !ClonkCompletionProposal.VALID_PREFIX_PATTERN.matcher(brombeeren).matches())
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

//		try {
//			ITypedRegion region = doc.getPartition(wordOffset);
//			if (region != null && !region.getType().equals(IDocument.DEFAULT_CONTENT_TYPE))
//				return null;
//		} catch (BadLocationException e) {}

		final boolean returnProposals = activeFunc == null
			? proposalsOutsideOfFunction(viewer, offset, wordOffset, prefix, proposals, index)
			: proposalsInsideOfFunction(offset, wordOffset, doc, prefix, proposals, index, activeFunc);
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

	private boolean proposalsInsideOfFunction(int offset, int wordOffset,
		IDocument doc, String prefix,
		List<ICompletionProposal> proposals, Index index,
		final Function activeFunc
	) {
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
		final Script editorScript = Utilities.scriptForEditor(editor);
		contextExpression = null;
		internalProposalsInsideOfFunction(offset, wordOffset, doc, prefix, proposals,
				index, activeFunc, editorScript, null);
		return true;
	}

	@Override
	protected IFile pivotFile() {
		if (editor != null)
			return super.pivotFile();
		else if (_currentEditorScript != null)
			return (IFile) _currentEditorScript.source();
		else
			return null;
	}

	private FunctionFragmentParser updateFunctionFragment(IDocument doc, Script editorScript, Function activeFunc) {
		final FunctionFragmentParser fparser = new FunctionFragmentParser(doc, editorScript, activeFunc, null);
		if (fparser.update())
			(typingContext = typingStrategy.localTypingContext(fparser.script(), fparser.fragmentOffset(), null)).visitFunction(activeFunc);
		return fparser;
	}

	private void internalProposalsInsideOfFunction(int offset, int wordOffset,
		IDocument doc, String prefix, List<ICompletionProposal> proposals,
		Index index, final Function activeFunc,
		Script editorScript,
		C4ScriptParser parser
	) {
		final List<Declaration> contextStructures = new LinkedList<Declaration>();
		_currentEditorScript = editorScript;
		final boolean specifiedParser = parser != null;
		Sequence contextSequence = null;
		CallDeclaration innermostCallFunc = null;

		if (editorScript != null) {
			final int preservedOffset = offset - (activeFunc != null?activeFunc.bodyLocation().start():0);
			typingContext = null;
			if (!specifiedParser)
				parser = updateFunctionFragment(doc, editorScript, activeFunc);
			if (contextExpression == null) {
				final ExpressionLocator locator = new ExpressionLocator(preservedOffset);
				activeFunc.traverse(locator, this);
				contextExpression = locator.expressionAtRegion();
				if (contextExpression != null && contextExpression.start() == preservedOffset && contextExpression.predecessorInSequence() != null)
					contextExpression = contextExpression.predecessorInSequence();
			}
			if (typingContext == null && parser != null)
				typingContext = typingStrategy.localTypingContext(parser.script(), parser.fragmentOffset(), null);
			// only present completion proposals specific to the <expr>->... thingie if cursor inside identifier region of declaration access expression.
			if (contextExpression != null) {
				if (contextExpression instanceof Placeholder || contextExpression instanceof StringLiteral) {
					try {
						if (doc.getChar(offset-1) != '$')
							return;
					} catch (final BadLocationException e1) {
						return;
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
					return;
				}
				innermostCallFunc = contextExpression.parentOfType(CallDeclaration.class);
				cats.defaultOrdering();
				if (innermostCallFunc != null && innermostCallFunc == contextExpression.parent()) {
					// elevate definition proposals for parameters of id type
					final Variable parm = innermostCallFunc.parmDefinitionForParmExpression(contextExpression);
					if (parm != null && parm.type() != PrimitiveType.ANY && parm.type() != PrimitiveType.UNKNOWN)
						if (TypeUnification.unifyNoChoice(parm.type(), PrimitiveType.ID) != null)
							cats.Definitions = -1;
				}
				if (
					contextExpression instanceof MemberOperator ||
					(contextExpression instanceof AccessDeclaration && Utilities.regionContainsOffset(contextExpression.identifierRegion(), preservedOffset))
				) {
					// we only care about sequences
					contextSequence = Utilities.as(contextExpression.parent(), Sequence.class);
					if (contextSequence != null)
						contextSequence = contextSequence.subSequenceIncluding(contextExpression);
				}
			}
			if (contextSequence != null)
				for (final IType t : defaulting(typingContext.typeOf(contextSequence), PrimitiveType.UNKNOWN)) {
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
		}

		if (contextExpression instanceof VarInitialization) {
			final VarInitialization vi = (VarInitialization)contextExpression;
			Typing typing = Typing.ParametersOptionallyTyped;
			if (index instanceof ProjectIndex)
				typing = ((ProjectIndex)index).nature().settings().typing;
			switch (typing) {
			case Static:
				if (vi.type == PrimitiveType.ERRONEOUS) {
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
			return;
		}

		if (proposalCycle == ProposalCycle.ALL)
			if (editorScript.index().engine() != null && (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))) {
				for (final Function func : editorScript.index().engine().functions())
					proposalForFunc(func, prefix, offset, proposals, editorScript.index().engine().name(), true);
				if (contextSequence == null)
					for (final Variable var : editorScript.index().engine().variables())
						proposalForVar(var,prefix,offset,proposals);
			}

		if (contextSequence == null && (proposalCycle == ProposalCycle.ALL || proposalCycle == ProposalCycle.LOCAL) && activeFunc != null) {
			for (final Variable v : activeFunc.parameters())
				proposalForVar(v, prefix, wordOffset, proposals);
			for (final Variable v : activeFunc.locals())
				proposalForVar(v, prefix, wordOffset, proposals);
		}

		int whatToDisplayFromScripts = 0;
		if (contextSequence == null || MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= DeclMask.VARIABLES;
		if (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= DeclMask.FUNCTIONS;
		if (contextSequence == null)
			whatToDisplayFromScripts |= DeclMask.STATIC_VARIABLES;

		if (proposalCycle != ProposalCycle.OBJECT)
			for (final Index i : index.relevantIndexes())
				proposalsForIndex(i, offset, wordOffset, prefix, proposals, whatToDisplayFromScripts, editorScript);

		for (final Declaration s : contextStructures) {
			proposalsForStructure(s, prefix, offset, wordOffset, proposals, index, whatToDisplayFromScripts, s);
			if (s instanceof IHasIncludes) {
				@SuppressWarnings("unchecked")
				final Iterable<? extends IHasIncludes<?>> includes =
					((IHasIncludes<IHasIncludes<?>>)s).includes(index, editorScript, GatherIncludesOptions.Recursive);
				for (final IHasIncludes<?> inc : includes)
					proposalsForStructure((Declaration) inc, prefix, offset, wordOffset, proposals, index, whatToDisplayFromScripts, s);
			}
		}


		if (innermostCallFunc != null) {
			final SpecialEngineRules rules = parser.specialEngineRules();
			if (rules != null) {
				final SpecialFuncRule funcRule = rules.funcRuleFor(innermostCallFunc.name(), SpecialEngineRules.FUNCTION_PARM_PROPOSALS_CONTRIBUTOR);
				if (funcRule != null) {
					final ASTNode parmExpr = innermostCallFunc.findSubElementContaining(contextExpression);
					funcRule.contributeAdditionalProposals(innermostCallFunc, typingStrategy.localTypingContext(parser.script(), parser.fragmentOffset(), null), innermostCallFunc.indexOfParm(parmExpr), parmExpr, this, prefix, offset, proposals);
				}
			}
		}
		if (contextSequence == null && proposalCycle == ProposalCycle.ALL) {
			final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
			for(final String keyword : BuiltInDefinitions.KEYWORDS) {
				if (prefix != null && !stringMatchesPrefix(keyword, prefix))
					continue;
				final ClonkCompletionProposal prop = new ClonkCompletionProposal(null, keyword, offset, prefix != null ? prefix.length() : 0, keyword.length(), keywordImg ,
					keyword, null ,null, Messages.C4ScriptCompletionProcessor_Engine, editor());
				prop.setCategory(cats.Keywords);
				proposals.add(prop);
			}
		}
	}

	/**
	 * Generate a list of proposals for some expression.
	 * This static standalone version internally creates a {@link C4ScriptCompletionProcessor} instance and lets it do the things it does when invoking Content Assist normally.
	 * It is used for computing the list of similarly named declarations when invoking Quick Fix for unknown identifiers.
	 * @param expression The expression preceding the location for which proposals should be generated
	 * @param function The function containing the expression/function from which local variable definitions are pulled
	 * @param parser Parser serving as context
	 * @param document The {@link IDocument} the expression was read from
	 * @return A list of proposals that (hopefully) represent a valid continuation of the given expression
	 */
	public static List<ICompletionProposal> computeProposalsForExpression(ASTNode expression, Function function, C4ScriptParser parser, IDocument document) {
		final List<ICompletionProposal> result = new LinkedList<ICompletionProposal>();
		final C4ScriptCompletionProcessor processor = new C4ScriptCompletionProcessor(parser.script());
		final Index index = function.index();
		processor.contextExpression = expression;
		processor.internalProposalsInsideOfFunction(expression != null ? expression.end() : 0, 0, document, "", result, index, function, function.script(), parser); //$NON-NLS-1$
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
			if (func != null) {
				if (func.visibility() != FunctionScope.GLOBAL) {
					if (func.name().equals("InstanceType"))
						System.out.println("here");
					if (target instanceof Script && !((Script)target).seesFunction(func))
						continue;
					final ClonkCompletionProposal prop = proposalForFunc(func, prefix, offset, proposals, structure.name(), true);
					if (prop != null)
						prop.setCategory(cats.LocalFunction);
				}
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
	private final C4ScriptContextInformationValidator contextInformationValidator = new C4ScriptContextInformationValidator();

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		IContextInformation info = null;
		try {
			final Function cursorFunc = editor().functionAtCursor();
			if (cursorFunc != null)
				updateFunctionFragment(viewer.getDocument(), editor().script(), cursorFunc);
			final FuncCallInfo funcCallInfo = editor.innermostFunctionCallParmAtOffset(offset);
			if (funcCallInfo != null) {
				IIndexEntity entity = funcCallInfo.callFunc.quasiCalledFunction(TypeUtil.problemReportingContext(editor.functionAtCursor()));
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
					info = new C4ScriptContextInformation(
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
			if (!Utilities.objectsEqual(prevInformation, info))
				hideProposals();
			return info != null ? new IContextInformation[] {info} : null;
		} finally {
			prevInformation = info;
		}
	}

	public void hideProposals() {
		final C4ScriptContentAssistant assistant = as(this.editor().contentAssistant(), C4ScriptContentAssistant.class);
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
