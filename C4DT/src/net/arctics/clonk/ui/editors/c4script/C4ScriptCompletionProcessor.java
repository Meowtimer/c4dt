package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.BuiltInDefinitions;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.EffectFunction;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IStoredTypeInformation;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal.Category;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor.FuncCallInfo;
import net.arctics.clonk.ui.editors.c4script.EntityLocator.RegionDescription;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Gen;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StringUtil;
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
public class C4ScriptCompletionProcessor extends ClonkCompletionProcessor<C4ScriptEditor> {

	private static final char[] CONTEXT_INFORMATION_AUTO_ACTIVATION_CHARS = new char[] {'('};
	private static final char[] COMPLETION_INFORMATION_AUTO_ACTIVATION_CHARS = new char[] {
		//'.' Zapper does not want
	};

	private final class ClonkCompletionListener implements ICompletionListener, ICompletionListenerExtension {

		@Override
		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		}

		@Override
		public void assistSessionStarted(ContentAssistEvent event) {
			proposalCycle = ProposalCycle.ALL;
		}

		@Override
		public void assistSessionEnded(ContentAssistEvent event) {
		}

		@Override
		public void assistSessionRestarted(ContentAssistEvent event) {
			// needs to be reversed because it gets cycled after computing the proposals...
			proposalCycle = proposalCycle.reverseCycle();
		}

	}

	private enum ProposalCycle {
		ALL,
		LOCAL,
		OBJECT;

		public String description() {
			switch (this) {
			case ALL:
				return Messages.C4ScriptCompletionProcessor_AllCompletions;
			case LOCAL:
				return Messages.C4ScriptCompletionProcessor_LocalCompletions;
			case OBJECT:
				return Messages.C4ScriptCompletionProcessor_ObjectCompletions;
			default:
				return null;
			}
		}

		public ProposalCycle reverseCycle() {
			return values()[ordinal() == 0 ? values().length-1 : ordinal() - 1];
		}

		public ProposalCycle cycle() {
			return values()[(this.ordinal()+1)%values().length];
		}
	}

	private final ContentAssistant assistant;
	private ExprElm contextExpression;
	private List<IStoredTypeInformation> contextTypeInformation;
	private ProposalCycle proposalCycle = ProposalCycle.ALL;
	private Function _activeFunc;
	private String untamperedPrefix;

	public C4ScriptCompletionProcessor(C4ScriptEditor editor, ContentAssistant assistant) {
		super(editor);
		this.assistant = assistant;

		if (assistant != null) {
			assistant.setRepeatedInvocationTrigger(getIterationBinding());
			assistant.addCompletionListener(new ClonkCompletionListener());
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
		if (!index.isEmpty()) {
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
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		int wordOffset = offset - 1;
		IDocument doc = viewer.getDocument();
		String prefix = null;
		try {
			while (BufferedScanner.isWordPart(doc.getChar(wordOffset)) || BufferedScanner.isUmlaut(doc.getChar(wordOffset))) {
				wordOffset--;
			}
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

		ClonkProjectNature nature = ClonkProjectNature.get(editor);
		List<String> statusMessages = new ArrayList<String>(4);
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		Index index = nature.index();

		final Function activeFunc = getActiveFunc(doc, offset);
		this._activeFunc = activeFunc;

		statusMessages.add(Messages.C4ScriptCompletionProcessor_ProjectFiles);

		if (proposalCycle == ProposalCycle.ALL || activeFunc == null)
			if (editor().scriptBeingEdited().index().engine() != null)
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
		assistant.setStatusMessage(getProposalCycleMessage());

		doCycle();

		if (proposals.size() == 0) {
			return new ICompletionProposal[] {
					new CompletionProposal("",offset,0,0,null,Messages.C4ScriptCompletionProcessor_NoProposalsAvailable,null,null) //$NON-NLS-1$ 
			};
		}

		return sortProposals(proposals);
	}

	private void proposalsInsideOfFunction(int offset, int wordOffset,
			IDocument doc, String prefix,
			List<ICompletionProposal> proposals, Index index,
			final Function activeFunc) {

		Script editorScript = Utilities.scriptForEditor(editor);
		contextExpression = null;
		internalProposalsInsideOfFunction(offset, wordOffset, doc, prefix, proposals,
				index, activeFunc, editorScript, null);
	}

	@Override
	protected IFile pivotFile() {
		if (editor != null) {
			return super.pivotFile();
		} else if (_currentEditorScript != null) {
			return (IFile) _currentEditorScript.scriptStorage();
		} else {
			return null;
		}
	}

	// this is all messed up and hacky
	private Script _currentEditorScript;

	private void internalProposalsInsideOfFunction(int offset, int wordOffset,
			IDocument doc, String prefix, List<ICompletionProposal> proposals,
			Index index, final Function activeFunc,
			Script editorScript,
			C4ScriptParser parser) {

		List<IHasSubDeclarations> contextStructures = new LinkedList<IHasSubDeclarations>();
		contextStructures.add(editorScript);
		boolean contextStructuresChanged = false;
		_currentEditorScript = editorScript;
		boolean specifiedParser = parser != null;
		Sequence contextSequence = null;
		CallFunc innermostCallFunc = null;

		if (editorScript != null) {
			final int preservedOffset = offset - (activeFunc != null?activeFunc.body().start():0);
			if (contextExpression == null && !specifiedParser) {
				ExpressionLocator locator = new ExpressionLocator(preservedOffset);
				parser = C4ScriptParser.reportExpressionsAndStatements(doc, editorScript, activeFunc, locator,
						null, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
				contextExpression = locator.getExprAtRegion();
				if (contextTypeInformation != null) {
					parser.pushTypeInformationList(contextTypeInformation);
					parser.applyStoredTypeInformationList(true);
				}
			}
			// only present completion proposals specific to the <expr>->... thingie if cursor inside identifier region of declaration access expression.
			if (contextExpression != null) {
				innermostCallFunc = contextExpression.parentOfType(CallFunc.class);
				if (
						contextExpression instanceof MemberOperator ||
						(contextExpression instanceof AccessDeclaration && Utilities.regionContainsOffset(contextExpression.identifierRegion(), preservedOffset))
				) {
					// we only care about sequences
					contextSequence = Utilities.as(contextExpression.parent(), Sequence.class);
				}
			}
			if (contextSequence != null) {
				// cut off stuff after ->
				for (int i = contextSequence.subElements().length-1; i >= 0; i--) {
					if (contextSequence.subElements()[i] instanceof MemberOperator) {
						if (i < contextSequence.subElements().length-1)
							contextSequence = contextSequence.sequenceWithElementsRemovedFrom(contextSequence.subElements()[i+1]);
						break;
					}
				}
				for (IType t : contextSequence.typeInContext(parser)) {
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
			parser.endTypeInferenceBlock();
		}

		if (proposalCycle == ProposalCycle.ALL) {
			if (editorScript.index().engine() != null && (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))) {
				for (Function func : editorScript.index().engine().functions()) {
					proposalForFunc(func, prefix, offset, proposals, editorScript.index().engine().name(), true);
				}
				if (contextSequence == null) {
					for (Variable var : editorScript.index().engine().variables()) {
						proposalForVar(var,prefix,offset,proposals);
					}
				}
			}
		}

		if (contextSequence == null && (proposalCycle == ProposalCycle.ALL || proposalCycle == ProposalCycle.LOCAL) && activeFunc != null) {
			for (Variable v : activeFunc.parameters()) {
				proposalForVar(v, prefix, wordOffset, proposals);
			}
			for (Variable v : activeFunc.localVars()) {
				proposalForVar(v, prefix, wordOffset, proposals);
			}
		}

		int whatToDisplayFromScripts = IHasSubDeclarations.INCLUDES;
		if (contextSequence == null || MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= IHasSubDeclarations.VARIABLES;
		if (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))
			whatToDisplayFromScripts |= IHasSubDeclarations.FUNCTIONS;
		if (contextSequence == null)
			whatToDisplayFromScripts |= IHasSubDeclarations.STATIC_VARIABLES;
		
		if (proposalCycle != ProposalCycle.OBJECT)
			for (Index i : index.relevantIndexes())
				proposalsForIndex(i, offset, wordOffset, prefix, proposals, whatToDisplayFromScripts);
		
		for (IHasSubDeclarations s : contextStructures)
			proposalsForStructure(s, new HashSet<IHasSubDeclarations>(), prefix, offset, wordOffset, proposals, contextStructuresChanged, index, whatToDisplayFromScripts);
		
		
		if (innermostCallFunc != null) {
			SpecialScriptRules rules = parser.getSpecialScriptRules();
			if (rules != null) {
				SpecialFuncRule funcRule = rules.funcRuleFor(innermostCallFunc.declarationName(), SpecialScriptRules.FUNCTION_PARM_PROPOSALS_CONTRIBUTOR);
				if (funcRule != null) {
					ExprElm parmExpr = innermostCallFunc.findSubElementContaining(contextExpression);
					funcRule.contributeAdditionalProposals(innermostCallFunc, parser, innermostCallFunc.indexOfParm(parmExpr), parmExpr, this, prefix, offset, proposals);
				}
			}
		}
		if (contextSequence == null && proposalCycle == ProposalCycle.ALL) {
			ImageRegistry reg = ClonkCore.instance().getImageRegistry();
			if (reg.get("keyword") == null) { //$NON-NLS-1$
				reg.put("keyword", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.instance().getBundle(), new Path("icons/keyword.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for(String keyword : BuiltInDefinitions.KEYWORDS) {
				if (prefix != null) {
					if (!keyword.toLowerCase().startsWith(prefix)) continue;
				}
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

	public static class ParmInfo {public String name; public IType type;}

	private static final IConverter<ParmInfo, String> PARM_PRINTER = new IConverter<ParmInfo, String>() {
		@Override
		public String convert(ParmInfo parmInfo) {
			return String.format("%s %s", parmInfo.type.typeName(false), parmInfo.name);
		}
	};

	private String getFunctionScaffold(String functionName, ParmInfo... parmTypes) {
		StringBuilder builder = new StringBuilder();
		builder.append(Keywords.Func);
		builder.append(" "); //$NON-NLS-1$
		builder.append(functionName);
		StringUtil.writeBlock(builder, "(", ")", ", ", ArrayUtil.arrayIterable(ArrayUtil.map(parmTypes, String.class, PARM_PRINTER))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		switch (Conf.braceStyle) {
		case NewLine:
			builder.append("\n"); //$NON-NLS-1$
			break;
		case SameLine:
			builder.append(" "); //$NON-NLS-1$
			break;
		}
		builder.append("{\n\n}"); //$NON-NLS-1$
		return builder.toString();
	}

	private ClonkCompletionProposal callbackProposal(String prefix, String callback, boolean funcSupplied, List<ICompletionProposal> proposals, int offset, ParmInfo... parmTypes) {
		ImageRegistry reg = ClonkCore.instance().getImageRegistry();
		if (reg.get("callback") == null) { //$NON-NLS-1$
			reg.put("callback", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.instance().getBundle(), new Path("icons/callback.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		String repString = funcSupplied ? (callback!=null?callback:"") : getFunctionScaffold(callback, parmTypes); //$NON-NLS-1$
		ClonkCompletionProposal prop = new ClonkCompletionProposal(
				null,
				repString, offset, replacementLength, 
				repString.length(), reg.get("callback") , callback, null,null,Messages.C4ScriptCompletionProcessor_Callback, editor()); //$NON-NLS-1$
		proposals.add(prop);
		return prop;
	}

	private static final ParmInfo[] EFFECT_FUNCTION_PARM_BOILERPLATE = new ParmInfo[] {
		Gen.object(ParmInfo.class, "obj", PrimitiveType.OBJECT),
		Gen.object(ParmInfo.class, "effect", PrimitiveType.PROPLIST)
	};

	private void proposalsOutsideOfFunction(ITextViewer viewer, int offset,
			int wordOffset, String prefix,
			List<ICompletionProposal> proposals, Index index) {

		// check whether func keyword precedes location (whole function blocks won't be created then)
		boolean funcSupplied = precededBy(viewer, offset, Keywords.Func);
		boolean directiveExpectingObject =
			precededBy(viewer, offset, "#" + Directive.DirectiveType.INCLUDE.toString()) ||
			precededBy(viewer, offset, "#" + Directive.DirectiveType.APPENDTO.toString());

		if (!directiveExpectingObject) {
			// propose creating functions for standard callbacks
			for(String callback : BuiltInDefinitions.OBJECT_CALLBACKS) {
				if (prefix != null) {
					if (!callback.toLowerCase().startsWith(prefix))
						continue;
				}
				callbackProposal(prefix, callback, funcSupplied, proposals, offset).setCategory(Category.Callbacks);
			}

			// propose to just create function with the name already typed
			if (untamperedPrefix != null && untamperedPrefix.length() > 0) {
				callbackProposal(prefix, untamperedPrefix, funcSupplied, proposals, offset).setCategory(Category.NewFunction);
			}

			// propose creating effect functions
			String capitalizedPrefix = StringUtil.capitalize(untamperedPrefix); 
			for (EffectFunction.HardcodedCallbackType t : EffectFunction.HardcodedCallbackType.values()) {
				callbackProposal(prefix, t.nameForEffect(capitalizedPrefix), funcSupplied, proposals, wordOffset, EFFECT_FUNCTION_PARM_BOILERPLATE).setCategory(Category.EffectCallbacks);
			}

			if (!funcSupplied) {

				// propose declaration keywords (var, static, ...)
				for(String declarator : BuiltInDefinitions.DECLARATORS) {
					if (prefix != null) {
						if (!declarator.toLowerCase().startsWith(prefix)) continue;
					}
					ImageRegistry reg = ClonkCore.instance().getImageRegistry();
					if (reg.get("declarator") == null) { //$NON-NLS-1$
						reg.put("declarator", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.instance().getBundle(), new Path("icons/declarator.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
					}
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					ClonkCompletionProposal prop = new ClonkCompletionProposal(null, declarator,offset,replacementLength,declarator.length(), reg.get("declarator") , declarator.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, editor()); //$NON-NLS-1$
					prop.setCategory(Category.Keywords);
					proposals.add(prop);
				}

				// propose directives (#include, ...)
				for(String directive : BuiltInDefinitions.DIRECTIVES) {
					if (prefix != null) {
						if (!directive.toLowerCase().contains(prefix)) continue;
					}
					ImageRegistry reg = ClonkCore.instance().getImageRegistry();
					if (reg.get("directive") == null) { //$NON-NLS-1$
						reg.put("directive", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.instance().getBundle(), new Path("icons/directive.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
					}
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					ClonkCompletionProposal prop = new ClonkCompletionProposal(null, directive,offset,replacementLength,directive.length(), reg.get("directive") , directive.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, editor()); //$NON-NLS-1$
					prop.setCategory(Category.Directives);
					proposals.add(prop);
				}
			}
		}

		// propose objects for #include or something
		if (directiveExpectingObject) {
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

	private String getProposalCycleMessage() {
		TriggerSequence sequence = getIterationBinding();
		return String.format(Messages.C4ScriptCompletionProcessor_PressToShowCycle, sequence.format(), proposalCycle.cycle().description());
	}

	private void proposalsForStructure(IHasSubDeclarations structure, Set<IHasSubDeclarations> loopCatcher, String prefix, int offset, int wordOffset, List<ICompletionProposal> proposals, boolean noPrivateFuncs, Index index, int mask) {
		if (loopCatcher.contains(structure))
			return;
		else
			loopCatcher.add(structure);
		for (Declaration dec : structure.allSubDeclarations(mask)) {
			Function func;
			Variable var;
			Script include;
			if ((func = Utilities.as(dec, Function.class)) != null) {
				if (func.visibility() != FunctionScope.GLOBAL)
					if (!noPrivateFuncs  || func.visibility() == FunctionScope.PUBLIC)
						proposalForFunc(func, prefix, offset, proposals, structure.name(), true);
			}
			else if ((var = Utilities.as(dec, Variable.class)) != null) {
				if (var.scope() != Scope.STATIC && var.scope() != Scope.CONST)
					proposalForVar(var, prefix, wordOffset, proposals);
			}
			else if ((include = Utilities.as(dec, Script.class)) != null) {
				proposalsForStructure(include, loopCatcher, prefix, offset, wordOffset, proposals, noPrivateFuncs, index, mask);
			}
		}
	}

	protected Function getActiveFunc(IDocument document, int offset) {
		Script thisScript = Utilities.scriptForEditor(editor);
		return thisScript != null ? thisScript.funcAt(new Region(offset,1)) : null;
	}

	private IContextInformation prevInformation;

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		IContextInformation info = null;
		try {
			FuncCallInfo funcCallInfo = editor.getInnermostCallFuncExprParm(offset);
			if (funcCallInfo != null) {
				IIndexEntity entity = funcCallInfo.callFunc.declaration();
				if (entity == null) {
					RegionDescription d = new RegionDescription();
					if (funcCallInfo.locator.initializeRegionDescription(d, editor().scriptBeingEdited(), new Region(offset, 1))) {
						funcCallInfo.locator.initializeProposedDeclarations(editor().scriptBeingEdited(), d, null, funcCallInfo.callFunc);
						if (funcCallInfo.locator.potentialEntities() != null)
							for (IIndexEntity e : funcCallInfo.locator.potentialEntities()) {
								if (entity == null)
									entity = e;
								else {
									entity = null;
									break;
								}
							}
					}
				}
//				if (dec == null && funcCallInfo.locator != null)
//					dec = funcCallInfo.locator.getDeclaration();
				if (entity instanceof Function) {
					String parmString = ((Function)entity).longParameterString(false, false).trim();
					if (parmString.length() == 0)
						parmString = Messages.C4ScriptCompletionProcessor_NoParameters;
					info = new ClonkContextInformation(
							entity.name() + "()", null, //$NON-NLS-1$
							parmString,
							funcCallInfo.parmIndex, funcCallInfo.parmsStart, funcCallInfo.parmsEnd, ((Function)entity).numParameters()
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
		return COMPLETION_INFORMATION_AUTO_ACTIVATION_CHARS;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return CONTEXT_INFORMATION_AUTO_ACTIVATION_CHARS;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return new ClonkContextInformationValidator();
	}

	private KeySequence getIterationBinding() {
		final IBindingService bindingSvc= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		TriggerSequence binding= bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		if (binding instanceof KeySequence)
			return (KeySequence) binding;
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

}
