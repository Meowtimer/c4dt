package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IDocumentedDeclaration;
import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.parser.c4script.BuiltInDefinitions;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.VisitCodeFlavour;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.effect.Effect;
import net.arctics.clonk.parser.c4script.effect.EffectFunction;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal.Category;
import net.arctics.clonk.ui.editors.ClonkContentAssistant;
import net.arctics.clonk.ui.editors.ClonkContextInformation;
import net.arctics.clonk.ui.editors.ClonkContextInformationValidator;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor.FuncCallInfo;
import net.arctics.clonk.ui.editors.c4script.EntityLocator.RegionDescription;
import net.arctics.clonk.util.Profiled;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Handles calculating completion lists shown when invoking Content Assist in C4Script editors.
 * @author madeen
 *
 */
public class C4ScriptCompletionProcessor extends ClonkCompletionProcessor<C4ScriptEditor> implements ICompletionListener, ICompletionListenerExtension {

	private static final char[] CONTEXT_INFORMATION_AUTO_ACTIVATION_CHARS = new char[] {'('};

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

	private final ContentAssistant assistant;
	private ExprElm contextExpression;
	private ProposalCycle proposalCycle = ProposalCycle.ALL;
	private Function _activeFunc;
	
	private String untamperedPrefix;

	public C4ScriptCompletionProcessor(C4ScriptEditor editor, ContentAssistant assistant) {
		super(editor, assistant);
		this.assistant = assistant;

		if (assistant != null) {
			assistant.setRepeatedInvocationTrigger(iterationBinding());
			assistant.addCompletionListener(this);
		}

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
	 * @param flags Flags indicating what kind of proposals should be included. {@link IHasSubDeclarations#STATIC_VARIABLES} needs to be or-ed to flags if {@link Definition} and static variable proposals are to be shown.
	 */
	private void proposalsForIndex(Index index, int offset, int wordOffset, String prefix, List<ICompletionProposal> proposals, int flags) {
		if (_activeFunc != null) {
			Scenario s2 = _activeFunc.scenario();
			if ((flags & IHasSubDeclarations.FUNCTIONS) != 0)
				for (Function func : index.globalFunctions()) {
					if (func == null) {
						System.out.println("D:");
						continue;
					}
					Scenario s1 = func.scenario();
					if (s1 != null && s2 != null && s1 != s2)
						continue;
					proposalForFunc(func, prefix, offset, proposals, func.script().name(), true);
				}
			if ((flags & IHasSubDeclarations.STATIC_VARIABLES) != 0)
				for (Variable var : index.staticVariables()) {
					Scenario s1 = var.scenario();
					if (s1 != null && s1 != s2)
						continue;
					proposalForVar(var,prefix,offset,proposals);
				}
		}
		if ((flags & IHasSubDeclarations.STATIC_VARIABLES) != 0)
			proposalsForIndexedDefinitions(index, offset, wordOffset, prefix, proposals);
	}

	@Profiled
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		int wordOffset = offset - 1;
		IDocument doc = viewer.getDocument();
		String prefix = null;
		try {
			ITypedRegion region = doc.getPartition(wordOffset);
			if (region != null && !region.getType().equals(IDocument.DEFAULT_CONTENT_TYPE))
				return null;
			if (wordOffset >= 1 && doc.getChar(wordOffset) == '>' && doc.getChar(wordOffset-1) != '-')
				return null;
			if (wordOffset >= 0 && Character.isWhitespace(doc.getChar(wordOffset)))
				return null;
			while (BufferedScanner.isWordPart(doc.getChar(wordOffset)) || Character.isLetter(doc.getChar(wordOffset)))
				wordOffset--;
			wordOffset++;
			if (wordOffset < offset) {
				prefix = doc.get(wordOffset, offset - wordOffset);
				offset = wordOffset;
			}
		} catch (BadLocationException e) {
			prefix = null;
		}

		this.untamperedPrefix = prefix;
		if (prefix != null)
			prefix = prefix.toLowerCase();
		this.prefix = prefix;

		ClonkProjectNature nature = ClonkProjectNature.get(editor);
		List<String> statusMessages = new ArrayList<String>(4);
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		Index index = nature.index();

		final Function activeFunc = funcAt(doc, offset);
		this._activeFunc = activeFunc;

		statusMessages.add(Messages.C4ScriptCompletionProcessor_ProjectFiles);

		if (proposalCycle == ProposalCycle.ALL || activeFunc == null)
			if (editor().script().index().engine() != null)
				statusMessages.add(Messages.C4ScriptCompletionProcessor_EngineFunctions);

		if (activeFunc == null)
			proposalsOutsideOfFunction(viewer, offset, wordOffset, prefix, proposals, index);
		else
			proposalsInsideOfFunction(offset, wordOffset, doc, prefix, proposals, index, activeFunc);

		StringBuilder statusMessage = new StringBuilder(Messages.C4ScriptCompletionProcessor_ShownData);
		for(String message : statusMessages) {
			statusMessage.append(message);
			if (statusMessages.get(statusMessages.size() - 1) != message) statusMessage.append(", "); //$NON-NLS-1$
		}

		//assistant.setStatusMessage(statusMessage.toString());
		assistant.setStatusMessage(proposalCycleMessage());

		doCycle();

		if (proposals.size() == 0)
			return new ICompletionProposal[] {
				new CompletionProposal("",offset,0,0,null,Messages.C4ScriptCompletionProcessor_NoProposalsAvailable,null,null) //$NON-NLS-1$ 
			};

		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
	
	private void proposalsInsideOfFunction(int offset, int wordOffset,
		IDocument doc, String prefix,
		List<ICompletionProposal> proposals, Index index,
		final Function activeFunc
	) {
		Script editorScript = Utilities.scriptForEditor(editor);
		contextExpression = null;
		internalProposalsInsideOfFunction(offset, wordOffset, doc, prefix, proposals,
				index, activeFunc, editorScript, null);
	}

	@Override
	protected IFile pivotFile() {
		if (editor != null)
			return super.pivotFile();
		else if (_currentEditorScript != null)
			return (IFile) _currentEditorScript.scriptStorage();
		else
			return null;
	}

	// this is all messed up and hacky
	private Script _currentEditorScript;

	private void internalProposalsInsideOfFunction(int offset, int wordOffset,
		IDocument doc, String prefix, List<ICompletionProposal> proposals,
		Index index, final Function activeFunc,
		Script editorScript,
		C4ScriptParser parser
	) {
		List<IHasSubDeclarations> contextStructures = new LinkedList<IHasSubDeclarations>();
		contextStructures.add(editorScript);
		boolean contextStructuresChanged = false;
		_currentEditorScript = editorScript;
		boolean specifiedParser = parser != null;
		Sequence contextSequence = null;
		CallDeclaration innermostCallFunc = null;

		if (editorScript != null) {
			final int preservedOffset = offset - (activeFunc != null?activeFunc.bodyLocation().start():0);
			if (contextExpression == null && !specifiedParser) {
				ExpressionLocator locator = new ExpressionLocator(preservedOffset);
				parser = C4ScriptParser.visitCode(doc, editorScript, activeFunc, locator,
						null, VisitCodeFlavour.AlsoStatements, false);
				contextExpression = locator.expressionAtRegion();
			}
			// only present completion proposals specific to the <expr>->... thingie if cursor inside identifier region of declaration access expression.
			if (contextExpression != null) {
				innermostCallFunc = contextExpression.parentOfType(CallDeclaration.class);
				if (
					contextExpression instanceof MemberOperator ||
					(contextExpression instanceof AccessDeclaration && Utilities.regionContainsOffset(contextExpression.identifierRegion(), preservedOffset))
				)
					// we only care about sequences
					contextSequence = Utilities.as(contextExpression.parent(), Sequence.class);
			}
			if (contextSequence != null) {
				// cut off stuff after ->
				for (int i = contextSequence.subElements().length-1; i >= 0; i--)
					if (contextSequence.subElements()[i] instanceof MemberOperator) {
						if (i < contextSequence.subElements().length-1)
							contextSequence = contextSequence.subSequenceUpTo(contextSequence.subElements()[i+1]);
						break;
					}
				for (IType t : contextSequence.type(parser)) {
					IHasSubDeclarations structure;
					if (t instanceof IHasSubDeclarations)
						structure = (IHasSubDeclarations) t;
					else
						structure = Script.scriptFrom(t);
					if (structure != null) {
						if (!contextStructuresChanged) {
							contextStructures.clear();
							contextStructuresChanged = true;
						}
						contextStructures.add(structure);
					}
				}
				// expression was of no use - discard and show global proposals
				if (!contextStructuresChanged)
					contextSequence = null;
			}
		}

		if (proposalCycle == ProposalCycle.ALL)
			if (editorScript.index().engine() != null && (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))) {
				for (Function func : editorScript.index().engine().functions())
					proposalForFunc(func, prefix, offset, proposals, editorScript.index().engine().name(), true);
				if (contextSequence == null)
					for (Variable var : editorScript.index().engine().variables())
						proposalForVar(var,prefix,offset,proposals);
			}

		if (contextSequence == null && (proposalCycle == ProposalCycle.ALL || proposalCycle == ProposalCycle.LOCAL) && activeFunc != null) {
			for (Variable v : activeFunc.parameters())
				proposalForVar(v, prefix, wordOffset, proposals);
			for (Variable v : activeFunc.localVars())
				proposalForVar(v, prefix, wordOffset, proposals);
		}

		int whatToDisplayFromScripts = 0;
		if (contextSequence == null || MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= IHasSubDeclarations.VARIABLES;
		if (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= IHasSubDeclarations.FUNCTIONS;
		if (contextSequence == null)
			whatToDisplayFromScripts |= IHasSubDeclarations.STATIC_VARIABLES;
		
		if (proposalCycle != ProposalCycle.OBJECT)
			for (Index i : index.relevantIndexes())
				proposalsForIndex(i, offset, wordOffset, prefix, proposals, whatToDisplayFromScripts);
		
		for (IHasSubDeclarations s : contextStructures) {
			proposalsForStructure(s, prefix, offset, wordOffset, proposals, index, whatToDisplayFromScripts);
			if (s instanceof IHasIncludes) {
				Iterable<? extends IHasIncludes> includes = ((IHasIncludes)s).includes(index, editorScript, GatherIncludesOptions.Recursive);
				for (IHasIncludes inc : includes)
					proposalsForStructure(inc, prefix, offset, wordOffset, proposals, index, whatToDisplayFromScripts);
			}
		}
		
		
		if (innermostCallFunc != null) {
			SpecialEngineRules rules = parser.specialEngineRules();
			if (rules != null) {
				SpecialFuncRule funcRule = rules.funcRuleFor(innermostCallFunc.declarationName(), SpecialEngineRules.FUNCTION_PARM_PROPOSALS_CONTRIBUTOR);
				if (funcRule != null) {
					ExprElm parmExpr = innermostCallFunc.findSubElementContaining(contextExpression);
					funcRule.contributeAdditionalProposals(innermostCallFunc, parser, innermostCallFunc.indexOfParm(parmExpr), parmExpr, this, prefix, offset, proposals);
				}
			}
		}
		if (contextSequence == null && proposalCycle == ProposalCycle.ALL) {
			ImageRegistry reg = Core.instance().getImageRegistry();
			if (reg.get("keyword") == null)
				reg.put("keyword", ImageDescriptor.createFromURL(FileLocator.find(Core.instance().getBundle(), new Path("icons/keyword.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
			for(String keyword : BuiltInDefinitions.KEYWORDS) {
				if (prefix != null)
					if (!stringMatchesPrefix(keyword, prefix))
						continue;
				int replacementLength = 0;
				if (prefix != null) replacementLength = prefix.length();
				ClonkCompletionProposal prop = new ClonkCompletionProposal(null, keyword,offset,replacementLength,keyword.length(), reg.get("keyword") , keyword.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, editor()); //$NON-NLS-1$
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
	public static List<ICompletionProposal> computeProposalsForExpression(ExprElm expression, Function function, C4ScriptParser parser, IDocument document) {
		List<ICompletionProposal> result = new LinkedList<ICompletionProposal>();
		C4ScriptCompletionProcessor processor = new C4ScriptCompletionProcessor(null, null);
		Index index = function.index();
		processor.contextExpression = expression;
		processor.internalProposalsInsideOfFunction(expression != null ? expression.end() : 0, 0, document, "", result, index, function, function.script(), parser);
		return result;
	}

	private ClonkCompletionProposal callbackProposal(String prefix, String callback, boolean funcSupplied, List<ICompletionProposal> proposals, int offset, Variable... parmTypes) {
		ImageRegistry reg = Core.instance().getImageRegistry();
		if (reg.get("callback") == null)
			reg.put("callback", ImageDescriptor.createFromURL(FileLocator.find(Core.instance().getBundle(), new Path("icons/callback.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		String repString = funcSupplied ? (callback!=null?callback:"") : Function.scaffoldTextRepresentation(callback, FunctionScope.PUBLIC, parmTypes); //$NON-NLS-1$
		ClonkCompletionProposal prop = new ClonkCompletionProposal(
				null,
				repString, offset, replacementLength, 
				repString.length(), reg.get("callback") , callback, null,null,Messages.C4ScriptCompletionProcessor_Callback, editor()); //$NON-NLS-1$
		proposals.add(prop);
		return prop;
	}

	private void proposalsOutsideOfFunction(ITextViewer viewer, int offset,
			int wordOffset, String prefix,
			List<ICompletionProposal> proposals, Index index) {

		// check whether func keyword precedes location (whole function blocks won't be created then)
		boolean funcSupplied = precededBy(viewer, offset, Keywords.Func);
		boolean directiveExpectingDefinition =
			precededBy(viewer, offset, "#" + Directive.DirectiveType.INCLUDE.toString()) ||
			precededBy(viewer, offset, "#" + Directive.DirectiveType.APPENDTO.toString());

		if (!directiveExpectingDefinition) {
			// propose creating functions for standard callbacks
			for(String callback : editor().script().engine().settings().callbackFunctions()) {
				if (prefix != null)
					if (!stringMatchesPrefix(callback, prefix))
						continue;
				callbackProposal(prefix, callback, funcSupplied, proposals, offset).setCategory(Category.Callbacks);
			}

			// propose to just create function with the name already typed
			if (untamperedPrefix != null && untamperedPrefix.length() > 0)
				callbackProposal(prefix, untamperedPrefix, funcSupplied, proposals, offset).setCategory(Category.NewFunction);

			// propose creating effect functions
			String capitalizedPrefix = StringUtil.capitalize(untamperedPrefix);
			for (String s : EffectFunction.DEFAULT_CALLBACKS) {
				IType parameterTypes[] = Effect.parameterTypesForCallback(s, editor.script(), PrimitiveType.ANY);
				Variable parms[] = new Variable[] {
					new Variable("obj", parameterTypes[0]),
					new Variable("effect", parameterTypes[1])
				};
				callbackProposal(prefix, EffectFunction.functionName(capitalizedPrefix, s),
					funcSupplied, proposals, wordOffset, parms).setCategory(Category.EffectCallbacks);
			}

			if (!funcSupplied) {

				// propose declaration keywords (var, static, ...)
				for(String declarator : BuiltInDefinitions.DECLARATORS) {
					if (prefix != null)
						if (!stringMatchesPrefix(declarator, prefix))
							continue;
					ImageRegistry reg = Core.instance().getImageRegistry();
					if (reg.get("declarator") == null)
						reg.put("declarator", ImageDescriptor.createFromURL(FileLocator.find(Core.instance().getBundle(), new Path("icons/declarator.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					ClonkCompletionProposal prop = new ClonkCompletionProposal(null, declarator,offset,replacementLength,declarator.length(), reg.get("declarator") , declarator.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, editor()); //$NON-NLS-1$
					prop.setCategory(Category.Keywords);
					proposals.add(prop);
				}

				// propose directives (#include, ...)
				for(String directive : BuiltInDefinitions.DIRECTIVES) {
					if (prefix != null)
						if (!stringMatchesPrefix(directive, prefix))
							continue;
					ImageRegistry reg = Core.instance().getImageRegistry();
					if (reg.get("directive") == null)
						reg.put("directive", ImageDescriptor.createFromURL(FileLocator.find(Core.instance().getBundle(), new Path("icons/directive.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					ClonkCompletionProposal prop = new ClonkCompletionProposal(null, directive,offset,replacementLength,directive.length(), reg.get("directive") , directive.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, editor()); //$NON-NLS-1$
					prop.setCategory(Category.Directives);
					proposals.add(prop);
				}
			}
		}

		// propose objects for #include or something
		if (directiveExpectingDefinition) {
			if (prefix == null)
				prefix = "";
			for (Index i : index.relevantIndexes())
				proposalsForIndex(i, offset, wordOffset, prefix, proposals, IHasSubDeclarations.STATIC_VARIABLES);
		}
	}

	private static boolean precededBy(ITextViewer viewer, int offset, String what) {
		try {
			return offset >= 5 && viewer.getDocument().get(offset - what.length() - 1, what.length()).equalsIgnoreCase(what);  
		} catch (BadLocationException e) {
			return false;
		}
	}

	private String proposalCycleMessage() {
		TriggerSequence sequence = iterationBinding();
		return String.format(Messages.C4ScriptCompletionProcessor_PressToShowCycle, sequence.format(), proposalCycle.cycle().description());
	}

	private void proposalsForStructure(IHasSubDeclarations structure, String prefix, int offset, int wordOffset, List<ICompletionProposal> proposals, Index index, int mask) {
		for (Declaration dec : structure.subDeclarations(index, mask)) {
			Function func = as(dec, Function.class);
			Variable var = as(dec, Variable.class);
			if (func != null) {
				if (func.visibility() != FunctionScope.GLOBAL)
					proposalForFunc(func, prefix, offset, proposals, structure.name(), true);
			}
			else if (var != null)
				if (var.scope() != Scope.STATIC && var.scope() != Scope.CONST)
					proposalForVar(var, prefix, wordOffset, proposals);
		}
	}

	protected Function funcAt(IDocument document, int offset) {
		Script thisScript = Utilities.scriptForEditor(editor);
		return thisScript != null ? thisScript.funcAt(new Region(offset,1)) : null;
	}

	private IContextInformation prevInformation;

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		IContextInformation info = null;
		try {
			FuncCallInfo funcCallInfo = editor.innermostFunctionCallParmAtOffset(offset);
			if (funcCallInfo != null) {
				IIndexEntity entity = funcCallInfo.callFunc.quasiCalledFunction(editor.functionAtCursor().declarationObtainmentContext());
				if (entity == null) {
					RegionDescription d = new RegionDescription();
					if (funcCallInfo.locator.initializeRegionDescription(d, editor().script(), new Region(offset, 1))) {
						funcCallInfo.locator.initializeProposedDeclarations(editor().script(), d, null, (ExprElm)funcCallInfo.callFunc);
						if (funcCallInfo.locator.potentialEntities() != null)
							for (IIndexEntity e : funcCallInfo.locator.potentialEntities())
								if (entity == null)
									entity = e;
								else {
									entity = null;
									break;
								}
					}
				}
				Function function = null;
				if (entity instanceof Function)
					function = (Function)entity;
				else if (entity instanceof Variable) {
					IType type = ((Variable)entity).type();
					if (type instanceof FunctionType)
						function = ((FunctionType)type).prototype();
				}
				if (function != null) {
					if (function instanceof IDocumentedDeclaration)
						((IDocumentedDeclaration)function).fetchDocumentation();
					String parmString = function.longParameterString(false, false).trim();
					if (parmString.length() == 0)
						parmString = Messages.C4ScriptCompletionProcessor_NoParameters;
					info = new ClonkContextInformation(
						function.name() + "()", UI.CLONK_ENGINE_ICON, //$NON-NLS-1$
						parmString,
						funcCallInfo.parmIndex, funcCallInfo.parmsStart, funcCallInfo.parmsEnd, function.numParameters()
					);
				}
			}
		} catch (Exception e) { 	    
			e.printStackTrace();
		}
		try {
			// HACK: if changed, hide the old one -.-
			if (!Utilities.objectsEqual(prevInformation, info)) {
				ClonkContentAssistant assistant = this.editor().getContentAssistant();
				//if (!assistant.isProposalPopupActive())
				assistant.hide();
			}
			return info != null ? new IContextInformation[] {info} : null;
		} finally {
			prevInformation = info;
		}
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return "_.>ABCDEFGHIJKLMNOPQRSTVUWXYZabcdefghijklmnopqrstvuwxyz".toCharArray();
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return CONTEXT_INFORMATION_AUTO_ACTIVATION_CHARS;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return new ClonkContextInformationValidator();
	}

	private KeySequence iterationBinding() {
		final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		TriggerSequence binding = bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		if (binding instanceof KeySequence)
			return (KeySequence) binding;
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}
}
