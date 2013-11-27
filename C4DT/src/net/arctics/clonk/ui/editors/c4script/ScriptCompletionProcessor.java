package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.ArrayUtil.filter;
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
import net.arctics.clonk.ast.Placeholder;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.BuiltInDefinitions;
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
import net.arctics.clonk.c4script.effect.Effect;
import net.arctics.clonk.c4script.effect.EffectFunction;
import net.arctics.clonk.c4script.typing.IRefinedPrimitiveType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.Maybe;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeAnnotation;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IDocumentedDeclaration;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.ui.editors.DeclarationProposal;
import net.arctics.clonk.ui.editors.ProposalsSite;
import net.arctics.clonk.ui.editors.StructureCompletionProcessor;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Handles calculating completion lists shown when invoking Content Assist in C4Script editors.
 * @author madeen
 *
 */
public class ScriptCompletionProcessor extends StructureCompletionProcessor<ScriptEditingState> implements ICompletionListener, ICompletionListenerExtension {

	private ProposalCycle proposalCycle = null;

	public ScriptCompletionProcessor(ScriptEditingState state) { super(state); }

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
	private void proposalsForIndex(ProposalsSite site, Index index) {
		final int declarationsMask = site.declarationsMask();
		if (site.function != null) {
			final Scenario s2 = site.function.scenario();
			if ((declarationsMask & DeclMask.FUNCTIONS) != 0)
				for (final Function func : index.globalFunctions()) {
					final Scenario fScen = func.scenario();
					if (fScen != null && fScen != s2)
						continue;
					proposalForFunc(site, state().structure(), func);
				}
			if ((declarationsMask & DeclMask.STATIC_VARIABLES) != 0)
				for (final Variable var : index.staticVariables()) {
					// ignore static variables from editor script since those are proposed already
					if (var.parentDeclaration() == site.script)
						continue;
					final Scenario vScen = var.scenario();
					if (vScen != null && vScen != s2)
						continue;
					proposalForVar(site, index.engine(), var);
				}
		}
		if ((declarationsMask & DeclMask.STATIC_VARIABLES) != 0)
			proposalsForIndexedDefinitions(site, index);
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
			if (prefix.length() > 0 && !DeclarationProposal.validPrefix(prefix))
				return null;
		} catch (final BadLocationException e) { }

		return new ProposalsSite(
			state(), offset, wordOffset, doc, prefix, new ArrayList<ICompletionProposal>(),
			ClonkProjectNature.get(state().structure().resource()).index(),
			state.functionAt(offset), state.structure(), null, null, null
		);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		super.computeCompletionProposals(viewer, offset);
		site = makeProposalsSite(viewer, offset);
		if (site == null)
			return null;
		if (!(site.function == null
			? computeProposalsOutsideFunction(viewer, site)
			: computeProposalsInsideFunction(site)))
			return null;
		state().assistant().setStatusMessage(proposalCycleMessage());
		ICompletionProposal[] proposals = site.finish(proposalCycle);
		if (proposals != null && site.prefix == null || site.prefix.length() == 0)
			proposals = appendWhitespaceLocalGlobalDelimiter(proposals);
		guardedSort(proposals);
		return proposals;
	}

	private ICompletionProposal[] appendWhitespaceLocalGlobalDelimiter(ICompletionProposal[] proposals) {
		if (proposals == null)
			return null;
		for (final ICompletionProposal p : proposals)
			if (p instanceof DeclarationProposal && ((DeclarationProposal)p).category() < cats.LocalGlobalDelimiter) {
				final DeclarationProposal[] w = new DeclarationProposal[3];
				for (int i = 0; i < w.length; i++) {
					w[i] = proposalForText(site, WHITESPACE);
					w[i].setCategory(cats.LocalGlobalDelimiter);
				}
				proposals = concat(proposals, w);
				break;
			}
		return proposals;
	}

	@SuppressWarnings("unused")
	private void setStatusMessage(final Function activeFunc) {
		state().assistant().setStatusMessage(proposalCycleMessage());
	}

	private boolean computeProposalsInsideFunction(ProposalsSite site) {
		site.pos(site.offset - (site.function != null ? site.function.bodyLocation().start() : 0));
		final ScriptEditingState state = state();
		if (site.script != null && state != null)
			state.updateFunctionFragment(site.function, site, true);

		if (!checkProposalConditions(site))
			return false;

		proposalCycle = proposalCycle == null ? ProposalCycle.ALL : proposalCycle.cycle();

		if (!skipProposalsInFunction(site.contextExpression)) {
			final boolean restrictedProposals = computeStringProposals(site) || varInitializationProposals(site) || proplistKeyProposals(site);
			if (!restrictedProposals)
				innerProposalsInFunction(site);
			return true;
		} else
			return false;
	}

	private boolean proplistKeyProposals(ProposalsSite site) {
		if (site.contextExpression instanceof PropListExpression)
			return true;
		return false;
	}

	private boolean checkProposalConditions(ProposalsSite site) {
		try {
			boolean targetCall = false;
			boolean whitespace = false;
			Loop: for (int arrowOffset = site.wordOffset - 1; arrowOffset >= 1; arrowOffset--) {
				final char c = site.document.getChar(arrowOffset);
				switch (c) {
				case '.':
					targetCall = true;
					break Loop;
				case '~':
					arrowOffset--;
					//$FALL-THROUGH$
				case '>':
					if (site.document.getChar(arrowOffset-1) != '-')
						return whitespace || (site.prefix != null && site.prefix.length() > 0);
					targetCall = true;
					break Loop;
				case ':':
					if (site.contextExpression != null && site.contextExpression.parent(PropListExpression.class) != null)
						return true;
					if (site.document.getChar(arrowOffset-1) != ':')
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
			if (!targetCall && site.wordOffset >= 0 && Character.isWhitespace(site.document.getChar(site.wordOffset)))
				return false;
		} catch (final BadLocationException bl) {
			return false;
		}
		return true;
	}

	private void innerProposalsInFunction(ProposalsSite site) {
		setCategoryOrdering(site);

		functionLocalProposals(site);
		structureProposals(site);
		globalIndexProposals(site);
		engineProposals(site);
		ruleBasedProposals(site);
		keywordProposals(site);
		removeProposalForVariableBeingDeclared(site);
	}

	private void removeProposalForVariableBeingDeclared(ProposalsSite site) {
		if (site.contextExpression != null) {
			final VarInitialization init = site.contextExpression.parent(VarInitialization.class);
			if (init != null && init.variable != null)
				site.removeProposalForDeclaration(init.variable);
		}
	}

	private boolean skipProposalsInFunction(ASTNode contextExpression) {
		return contextExpression instanceof Comment;
	}

	private static final Text WHITESPACE = new Text("");

	private void engineProposals(ProposalsSite site) {
		if (noStructureType(site))
			return;
		final Engine ngn = site.script.index().engine();
		if (ngn != null) {
			if ((site.declarationsMask() & DeclMask.FUNCTIONS) != 0)
				for (final Function func : ngn.functions())
					proposalForFunc(site, state().structure(), func);
			if ((site.declarationsMask() & DeclMask.STATIC_VARIABLES) != 0)
				for (final Variable var : ngn.variables())
					proposalForVar(site, site.script.engine(), var);
		}
	}

	private void functionLocalProposals(ProposalsSite site) {
		if ((site.declarationsMask() & DeclMask.FUNCTIONS) != 0)
			if (site.contextSequence == null && site.function != null) {
				for (final Variable v : site.function.parameters())
					proposalForVar(site, site.function, v);
				for (final Variable v : site.function.locals())
					proposalForVar(site, site.function, v);
			}
	}

	private void globalIndexProposals(ProposalsSite site) {
		for (final Index i : site.index.relevantIndexes())
			proposalsForIndex(site, i);
	}

	@SuppressWarnings("serial")
	static final Declaration NO_STRUCTURE_TYPE = new Text("<No structure>") {
		final Declaration hint = new Text("<Nothing>");
		@Override
		public int hashCode() { return name.hashCode(); }
		@Override
		public List<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
			return Arrays.asList(hint);
		}
	};

	private void recursiveProposalsForStructure(
		ProposalsSite site,
		Declaration target, Declaration structure, int distanceToTarget,
		Set<Declaration> catcher
	) {
		if (!catcher.add(structure))
			return;
		final List<DeclarationProposal> props = proposalsForStructure(site, target, structure);
		for (final DeclarationProposal p : props) {
			p.setCategory(p.category()+distanceToTarget*cats.SUBPAGE);
			if (p.declaration() instanceof Variable && distanceToTarget == 0)
				p.setCategory(cats.SelfField);
		}
		if (structure instanceof IHasIncludes) {
			@SuppressWarnings("unchecked")
			final Iterable<? extends IHasIncludes<?>> includes = ((IHasIncludes<IHasIncludes<?>>)structure).includes(site.index, state.structure(), GatherIncludesOptions.Recursive);
			for (final IHasIncludes<?> inc : includes)
				recursiveProposalsForStructure(site, target, (Declaration) inc, distanceToTarget+1, catcher);
		}
	}

	private void structureProposals(ProposalsSite site) {
		final Set<Declaration> proposalTypes = determineProposalTypes(site);
		if (proposalTypes.size() > 0)
			for (final Declaration s : proposalTypes)
				recursiveProposalsForStructure(site, s, s, 0, new HashSet<Declaration>());
		else
			proposeAllTheThings(site);
	}

	public void proposeAllTheThings(ProposalsSite site) {
		final List<DeclarationProposal> old = Arrays.asList(filter(site.proposals, DeclarationProposal.class));
		final List<Index> relevantIndexes = site.index.relevantIndexes();
		final int declarationMask = site.declarationsMask();
		for (final Index x : relevantIndexes)
			for (final Map.Entry<String, List<Declaration>> decs : x.declarationMap().entrySet()) {
				final Declaration d = decs.getValue().get(0);
				if ((declarationMask & DeclMask.FUNCTIONS) != 0 && d instanceof Function && !((Function)d).isGlobal())
					proposalForFunc(site, defaulting(as(site.precedingType, Script.class), site.function.engine()), (Function) d);
				else if ((declarationMask & DeclMask.VARIABLES) != 0 && d instanceof Variable && ((Variable)d).scope() == Scope.LOCAL)
					proposalForVar(site, as(site.precedingType, Script.class), (Variable)d);
			}
		for (final ICompletionProposal p : site.proposals) {
			final DeclarationProposal ccp = as(p, DeclarationProposal.class);
			if (ccp != null && !old.contains(ccp))
				ccp.setImage(UI.halfTransparent(ccp.getImage()));
		}
	}

	private boolean noStructureType(ProposalsSite site) {
		boolean noStructure = true;
		for (final IType t : site.precedingType()) {
			IType ty = t;
			if (ty instanceof IRefinedPrimitiveType)
				ty = ((IRefinedPrimitiveType)ty).simpleType();
			noStructure &=
				eq(ty, PrimitiveType.VOID) ||
				eq(ty, PrimitiveType.ARRAY) ||
				eq(ty, PrimitiveType.BOOL) ||
				eq(ty, PrimitiveType.INT) ||
				eq(ty, PrimitiveType.STRING) ||
				eq(ty, PrimitiveType.NUM) ||
				eq(ty, PrimitiveType.FLOAT);
		}
		return noStructure;
	}

	private Set<Declaration> determineProposalTypes(ProposalsSite site) {
		final Set<Declaration> contextStructures = new HashSet<Declaration>();
		if (site.contextSequence != null) {
			if (noStructureType(site))
				contextStructures.add(NO_STRUCTURE_TYPE);
			else for (final IType t : site.precedingType()) {
				Declaration structure;
				if (t instanceof Declaration)
					structure = (Declaration) t;
				else if (t instanceof MetaDefinition)
					structure = ((MetaDefinition)t).definition();
				else
					structure = as(t, Script.class);
				if (structure != null)
					contextStructures.add(structure);
			}
		}
		else
			contextStructures.add(site.script);
		return contextStructures;
	}

	private void keywordProposals(ProposalsSite site) {
		if (site.contextSequence == null && (site.declarationsMask() & DeclMask.STATIC_VARIABLES) != 0) {
			final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
			final List<String> keywords = new ArrayList<>(BuiltInDefinitions.KEYWORDS);
			if (site.index.typing() != Typing.STATIC)
				keywords.remove(Keywords.Cast);
			for(final String keyword : keywords) {
				if (site.prefix != null && !stringMatchesPrefix(keyword, site.prefix))
					continue;
				final DeclarationProposal prop = new DeclarationProposal(null, null, keyword, site.offset, site.prefix != null ? site.prefix.length() : 0, keyword.length(), keywordImg ,
					keyword, null ,null, ": keyword", site);
				prop.setCategory(cats.Keywords);
				site.addProposal(prop);
			}
		}
	}

	private boolean varInitializationProposals(ProposalsSite site) {
		final VarInitialization vi = as(site.contextExpression, VarInitialization.class);
		final TypeAnnotation annot = as(site.contextExpression, TypeAnnotation.class);
		if (vi == null && annot == null)
			return false;
		if (annot == null && vi != null && (vi.typeAnnotation == null || vi.typeAnnotation.type() != PrimitiveType.ERRONEOUS))
			return true;
		final Typing typing = site.index instanceof ProjectIndex
			? ((ProjectIndex)site.index).nature().settings().typing
			: Typing.INFERRED;
		switch (typing) {
		case STATIC:
			for (final Index ndx : site.index.relevantIndexes())
				proposalsForIndexedDefinitions(site, ndx);
			final Image keywordImg = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
			for (final PrimitiveType t : PrimitiveType.values())
				if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ERRONEOUS && site.index.engine().supportsPrimitiveType(t)) {
					final DeclarationProposal prop = new DeclarationProposal(null, null, t.scriptName(), site.offset, site.prefix != null ? site.prefix.length() : 0 , t.scriptName().length(),
						keywordImg , t.scriptName(), null, null, Messages.C4ScriptCompletionProcessor_Engine, site);
					prop.setCategory(cats.Keywords);
					site.addProposal(prop);
				}
			return true;
		default:
			return true;
		}
	}

	private void ruleBasedProposals(ProposalsSite site) {
		if (site.contextExpression == null)
			return;
		final CallDeclaration innermostCallFunc = site.contextExpression.thisOrParent(CallDeclaration.class);
		if (innermostCallFunc != null) {
			final SpecialEngineRules rules = site.script.engine().specialRules();
			if (rules != null) {
				final SpecialFuncRule funcRule = rules.funcRuleFor(innermostCallFunc.name(), SpecialEngineRules.FUNCTION_PARM_PROPOSALS_CONTRIBUTOR);
				if (funcRule != null) {
					final ASTNode parmExpr = innermostCallFunc.findSubElementContaining(site.contextExpression);
					funcRule.contributeAdditionalProposals(innermostCallFunc, innermostCallFunc.indexOfParm(parmExpr), parmExpr, this, site);
				}
			}
		}
	}

	private void setCategoryOrdering(ProposalsSite site) {
		cats.defaultOrdering();
		if (site.contextExpression == null)
			return;
		CallDeclaration innermostCallFunc = site.contextExpression.parent(CallDeclaration.class);
		// elevate definition proposals for parameters of id type
		Variable parm;
		if (innermostCallFunc != null && site.contextExpression.parent() == innermostCallFunc)
			parm = innermostCallFunc.parmDefinitionForParmExpression(site.contextExpression);
		else if ((innermostCallFunc = as(site.contextExpression, CallDeclaration.class)) != null && innermostCallFunc.params().length == 0 &&
			innermostCallFunc.declaration() instanceof Function && ((Function)innermostCallFunc.declaration()).numParameters() > 0)
			parm = ((Function)innermostCallFunc.declaration()).parameter(0);
		else
			parm = null;
		if (parm != null && parm.type() != PrimitiveType.ANY && parm.type() != PrimitiveType.UNKNOWN &&
			(eq(parm.type(), PrimitiveType.ID) || Maybe.contained(parm.type(), MetaDefinition.class) != null))
				cats.Definitions = cats.SelfField-cats.PAGE/2;
	}

	private boolean computeStringProposals(ProposalsSite site) {
		// only present completion proposals specific to the <expr>->... thingie if cursor inside identifier region of declaration access expression.
		if (site.contextExpression instanceof Placeholder || site.contextExpression instanceof StringLiteral) {
			try {
				if (site.document.getChar(site.offset-1) != '$')
					return true;
			} catch (final BadLocationException e1) {
				return true;
			}
			final Set<String> availableLocalizationStrings = new HashSet<>();
			try {
				for (final IResource r : (site.script.resource() instanceof IContainer ? (IContainer)site.script.resource() : site.script.resource().getParent()).members()) {
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
				if (site.prefix != null && !stringMatchesPrefix(loc, site.prefix))
					continue;
				final DeclarationProposal prop = new DeclarationProposal(null, null, loc, site.offset, site.prefix != null ? site.prefix.length() : 0 , loc.length(),
					keywordImg , loc, null, null, Messages.C4ScriptCompletionProcessor_Engine, site);
				prop.setCategory(cats.Keywords);
				site.addProposal(prop);
			}
			return true;
		} else
			return false;
	}

	/**
	 * Generate a list of proposals for some expression.
	 * This static standalone version internally creates a {@link ScriptCompletionProcessor} instance and lets it do the things it does when invoking Content Assist normally.
	 * It is used for computing the list of similarly named declarations when invoking Quick Fix for unknown identifiers.
	 * @param document The {@link IDocument} the expression was read from
	 * @param function The function containing the expression/function from which local variable definitions are pulled
	 * @param expression The expression preceding the location for which proposals should be generated
	 * @return A list of proposals that (hopefully) represent a valid continuation of the given expression
	 */
	public static List<ICompletionProposal> computeProposalsForExpression
		(IDocument document, Function function, ASTNode expression) {
		final List<ICompletionProposal> result = new LinkedList<ICompletionProposal>();
		new ScriptEditingState(Core.instance().getPreferenceStore()).set(null, function.script(), document);
		final ScriptCompletionProcessor processor = new ScriptCompletionProcessor(
			new ScriptEditingState(Core.instance().getPreferenceStore())
		);
		final ProposalsSite site = new ProposalsSite(
			null, expression != null ? expression.end() : 0,
			0, document, "", result, function.index(), function, function.script(),
			expression, expression.parent(Sequence.class), PrimitiveType.UNKNOWN
		);
		processor.innerProposalsInFunction(site);
		return result;
	}

	private DeclarationProposal callbackProposal(
		ProposalsSite site,
		final String callback,
		final String nameFormat,
		final String displayString,
		final boolean funcSupplied,
		final Variable... parameters
	) {
		final Image img = UI.imageForPath("icons/callback.png"); //$NON-NLS-1$
		int replacementLength = 0;
		if (site.prefix != null)
			replacementLength = site.prefix.length();
		final DeclarationProposal prop = new DeclarationProposal(
			null, null, "", site.wordOffset, replacementLength,  //$NON-NLS-1$
			0, img, callback != null ? callback : displayString,
			null, null, Messages.C4ScriptCompletionProcessor_Callback, site
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
					: Function.scaffoldTextRepresentation(cb, FunctionScope.PUBLIC, state().structure(), parameters); //$NON-NLS-1$
				cursorPosition = replacementString.length()-2;
				super.apply(viewer, trigger, stateMask, offset);
			}
			@Override
			public boolean requiresDocumentReparse() { return true; }
		};
		site.addProposal(prop);
		return prop;
	}

	private boolean computeProposalsOutsideFunction(ITextViewer viewer, ProposalsSite site) {
		proposalCycle = ProposalCycle.ALL;
		// check whether func keyword precedes location (whole function blocks won't be created then)
		final boolean funcSupplied = precededBy(viewer, site.offset, Keywords.Func);
		final boolean directiveExpectingDefinition =
			precededBy(viewer, site.offset, "#" + Directive.DirectiveType.INCLUDE.toString()) || //$NON-NLS-1$
			precededBy(viewer, site.offset, "#" + Directive.DirectiveType.APPENDTO.toString()); //$NON-NLS-1$

		for (final String kw: BuiltInDefinitions.DECLARATORS)
			if (precededBy(viewer, site.offset, kw))
				return false;

		final IDocument doc = viewer.getDocument();
		Check: for (int i = site.offset; i >= 0; i--)
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
			overrideProposals(site, funcSupplied);
			standardCallbackProposals(site, funcSupplied);
			newFunctionProposal(site, funcSupplied);
			effectFunctionProposals(site, funcSupplied);
			if (!funcSupplied) {
				declaratorProposals(site);
				directiveProposals(site);
			}
		} else
			directiveDefinitionArgumentProposals(site);
		return true;
	}

	private void directiveDefinitionArgumentProposals(ProposalsSite site) {
		// propose objects for #include or something
		for (final Index i : site.index.relevantIndexes())
			proposalsForIndex(site, i);
	}

	private void directiveProposals(ProposalsSite site) {
		// propose directives (#include, ...)
		final Image directiveIcon = UI.imageForPath("icons/directive.png"); //$NON-NLS-1$
		for(final Directive directive : Directive.CANONICALS) {
			String txt = directive.type().toString();
			if (site.prefix != null)
				if (!stringMatchesPrefix(txt, site.prefix))
					continue;
			int replacementLength = 0;
			if (site.prefix != null) replacementLength = site.prefix.length();
			txt = "#"+txt+" "; //$NON-NLS-1$ //$NON-NLS-2$
			final DeclarationProposal prop = new DeclarationProposal(
				directive, directive, txt, site.offset, replacementLength, txt.length(),
				directiveIcon, directive.type().toString(), null, null,
				Messages.C4ScriptCompletionProcessor_Engine, site
			);
			prop.setCategory(cats.Directives);
			site.addProposal(prop);
		}
	}

	private void declaratorProposals(ProposalsSite site) {
		// propose declaration keywords (var, static, ...)
		for(final String declarator : BuiltInDefinitions.DECLARATORS) {
			if (site.prefix != null)
				if (!stringMatchesPrefix(declarator, site.prefix))
					continue;
			final Image declaratorImg = UI.imageForPath("icons/declarator.png"); //$NON-NLS-1$
			int replacementLength = 0;
			if (site.prefix != null) replacementLength = site.prefix.length();
			final DeclarationProposal prop = new DeclarationProposal(null, null, declarator,site.offset,replacementLength,declarator.length(), declaratorImg , declarator.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, site); //$NON-NLS-1$
			prop.setCategory(cats.Keywords);
			site.addProposal(prop);
		}
	}

	private void effectFunctionProposals(ProposalsSite site, final boolean funcSupplied) {
		// propose creating effect functions
		for (final String s : EffectFunction.DEFAULT_CALLBACKS) {
			final IType parameterTypes[] = Effect.parameterTypesForCallback(s, state().structure(), PrimitiveType.ANY);
			final Variable parms[] = new Variable[] {
				new Variable("obj", parameterTypes[0]), //$NON-NLS-1$
				new Variable("effect", parameterTypes[1]) //$NON-NLS-1$
			};
			callbackProposal(site, null, EffectFunction.FUNCTION_NAME_PREFIX+"%s"+s, //$NON-NLS-1$
				String.format(Messages.C4ScriptCompletionProcessor_EffectFunctionCallbackProposalDisplayStringFormat, s), funcSupplied, parms).setCategory(cats.EffectCallbacks);
		}
	}

	private void newFunctionProposal(ProposalsSite site, final boolean funcSupplied) {
		// propose to just create function with the name already typed
		if (site.untamperedPrefix != null && site.untamperedPrefix.length() > 0)
			callbackProposal(site, null, "%s", Messages.C4ScriptCompletionProcessor_InsertFunctionScaffoldProposalDisplayString, funcSupplied).setCategory(cats.NewFunction); //$NON-NLS-1$
	}

	private void standardCallbackProposals(ProposalsSite site, final boolean funcSupplied) {
		// propose creating functions for standard callbacks
		for(final String callback : state().structure().engine().settings().callbackFunctions()) {
			if (site.prefix != null)
				if (!stringMatchesPrefix(callback, site.prefix))
					continue;
			callbackProposal(site, callback, "%s", null, funcSupplied).setCategory(cats.Callbacks); //$NON-NLS-1$
		}
	}

	private void overrideProposals(ProposalsSite site, final boolean funcSupplied) {
		// propose overriding inherited functions
		final Script script = state().structure();
		final List<Script> cong = script.conglomerate();
		for (final Script c : cong)
			if (c != script)
				for (final Declaration dec : c.subDeclarations(site.index, DeclMask.FUNCTIONS)) {
					if (!script.seesSubDeclaration(dec))
						continue;
					final Function func = as(dec, Function.class);
					callbackProposal(site, func.name(), "%s", null, funcSupplied, func.parameters().toArray(new Variable[func.numParameters()])).setCategory(cats.Callbacks);
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

	private List<DeclarationProposal> proposalsForStructure(ProposalsSite site, Declaration target, Declaration structure) {
		final List<DeclarationProposal> result = new LinkedList<>();
		for (final Declaration dec : structure.subDeclarations(site.index, site.declarationsMask())) {
			if (!target.seesSubDeclaration(dec))
				continue;
			final Function func = as(dec, Function.class);
			final Variable var = as(dec, Variable.class);
			final Text text = as(dec, Text.class);
			if (func != null && func.visibility() != FunctionScope.GLOBAL) {
				if (target instanceof Script && !((Script)target).seesFunction(func))
					continue;
				final DeclarationProposal pf = proposalForFunc(site, target, func);
				if (pf != null) {
					pf.setCategory(cats.LocalFunction);
					result.add(pf);
				}
			}
			else if (var != null) {
				final DeclarationProposal pv = proposalForVar(site, target, var);
				if (pv != null)
					result.add(pv);
			} else if (text != null) {
				final DeclarationProposal pt = proposalForText(site, text);
				if (pt != null)
					result.add(pt);
			}
		}
		return result;
	}

	private DeclarationProposal proposalForText(ProposalsSite site, Text text) {
		final DeclarationProposal prop = new DeclarationProposal(text, site.script, "", site.offset, 0, 0);
		site.addProposal(prop);
		return prop;
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
			final Function cursorFunc = state().activeEditor().functionAtCursor();
			final ScriptEditingState state = state();
			if (cursorFunc != null && state != null)
				state.updateFunctionFragment(cursorFunc, null, false);
			final ScriptEditingState.Call funcCallInfo = state.innermostFunctionCallParmAtOffset(offset);
			if (funcCallInfo != null) {
				IIndexEntity entity = functionFromCall(funcCallInfo.callFunc);
				if (entity == null)
					entity = state().mergeFunctions(offset, funcCallInfo);
				final Function function = state().functionFromEntity(entity);
				if (function != null) {
					if (function instanceof IDocumentedDeclaration)
						((IDocumentedDeclaration)function).fetchDocumentation();
					Script context;
					final ASTNode pred = funcCallInfo.callPredecessor();
					ContextSelection: {
						if (pred != null) {
							final Function.Typing map = cursorFunc.script().typings().get(cursorFunc);
							if (map != null) {
								final IType t = map.nodeTypes[pred.localIdentifier()];
								if (t != null)
									for (final IType t2 : t)
										if (t2 instanceof Script) {
											context = (Script)t2;
											break ContextSelection;
										}
							}
						}
						context = state().structure();
					}
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
			if (!eq(prevInformation, info))
				state().assistant().hide();
			return info != null ? new IContextInformation[] {info} : null;
		} finally {
			prevInformation = info;
		}
	}

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
