package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.BuiltInDefinitions;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IStoredTypeInformation;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor.FuncCallInfo;
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

public class C4ScriptCompletionProcessor extends ClonkCompletionProcessor<C4ScriptEditor> {

	private static final char[] CONTEXT_INFORMATION_AUTO_ACTIVATION_CHARS = new char[] {'('};
	private static final char[] COMPLETION_INFORMATION_AUTO_ACTIVATION_CHARS = new char[] {
		'.'
	};
	
	private final class ClonkCompletionListener implements ICompletionListener, ICompletionListenerExtension {

		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		}
	
		public void assistSessionStarted(ContentAssistEvent event) {
			proposalCycle = ProposalCycle.ALL;
			
			// refresh to find out whether caret is inside a function and to get all the declarations
			/*try {
				try {
					((C4ScriptEditor)editor).reparseWithDocumentContents(null, true);
				} catch (ParsingException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}*/
		}
	
		public void assistSessionEnded(ContentAssistEvent event) {
		}

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
	
	private ContentAssistant assistant;
	@SuppressWarnings("unused")
	private ExprElm contextExpression, contextExpression2;
	private List<IStoredTypeInformation> contextTypeInformation;
	private ProposalCycle proposalCycle = ProposalCycle.ALL;
	private C4Function _activeFunc;
	private String _prefix;
	
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
	
	private void proposalsForIndex(ClonkIndex index, int offset, int wordOffset, String prefix, List<ICompletionProposal> proposals) {
		if (!index.isEmpty()) {
			if (_activeFunc != null) {
				C4Scenario s2 = _activeFunc.getScenario();
				for (C4Function func : index.getGlobalFunctions()) {
					C4Scenario s1 = func.getScenario();
					if (s1 != null && s2 != null && s1 != s2)
						continue;
					proposalForFunc(func, prefix, offset, proposals, func.getScript().getName(), true);
				}
				for (C4Variable var : index.getStaticVariables()) {
					C4Scenario s1 = var.getScenario();
					if (s1 != null && s1 != s2)
						continue;
					proposalForVar(var,prefix,offset,proposals);
				}
			}
			proposalsForIndexedObjects(index, offset, wordOffset, prefix, proposals);
		}
	}
	
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
		
		this._prefix = prefix;
		if (prefix != null)
			prefix = prefix.toLowerCase();
		
		ClonkProjectNature nature = ClonkProjectNature.get(editor);
		List<String> statusMessages = new ArrayList<String>(4);
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		ClonkIndex index = nature.getIndex();

		final C4Function activeFunc = getActiveFunc(doc, offset);
		this._activeFunc = activeFunc;
		
		statusMessages.add(Messages.C4ScriptCompletionProcessor_ProjectFiles);
		
		if (proposalCycle == ProposalCycle.ALL || activeFunc == null) {
			if (getEditor().scriptBeingEdited().getIndex().getEngine() != null) {
				statusMessages.add(Messages.C4ScriptCompletionProcessor_EngineFunctions);
			}
		}
		
		if (activeFunc == null) {
			proposalsOutsideOfFunction(viewer, offset, wordOffset, prefix, proposals, index);
		} else {
			proposalsInsideOfFunction(offset, wordOffset, doc, prefix, proposals, index, activeFunc);
		}
		
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
			List<ICompletionProposal> proposals, ClonkIndex index,
			final C4Function activeFunc) {
		
		C4ScriptBase editorScript = Utilities.getScriptForEditor(editor);
		contextExpression = null;
		internalProposalsInsideOfFunction(offset, wordOffset, doc, prefix, proposals,
				index, activeFunc, editorScript, null);
	}
	
	@Override
	protected IFile pivotFile() {
		if (editor != null) {
			return super.pivotFile();
		} else if (_currentEditorScript != null) {
			return (IFile) _currentEditorScript.getScriptStorage();
		} else {
			return null;
		}
	}
	
	// this is all messed up and hacky
	private C4ScriptBase _currentEditorScript;

	private void internalProposalsInsideOfFunction(int offset, int wordOffset,
			IDocument doc, String prefix, List<ICompletionProposal> proposals,
			ClonkIndex index, final C4Function activeFunc,
			C4ScriptBase editorScript,
			C4ScriptParser parser) {
		List<C4ScriptBase> contextScripts = new LinkedList<C4ScriptBase>();
		contextScripts.add(editorScript);
		boolean contextScriptsChanged = false;
		_currentEditorScript = editorScript;
		boolean specifiedParser = parser != null;
		if (editorScript != null) {
			final int preservedOffset = offset - (activeFunc != null?activeFunc.getBody().getStart():0);
			if (contextExpression == null && !specifiedParser) {
				ExpressionLocator locator = new ExpressionLocator(preservedOffset);
				parser = C4ScriptParser.reportExpressionsAndStatements(doc, editorScript, activeFunc, locator,
					null, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
				contextExpression = locator.getExprAtRegion();
				Sequence seq = contextExpression.getParent(Sequence.class);
				if (seq != null) {
					contextExpression = seq;
				}
				if (contextTypeInformation != null) {
					parser.pushTypeInformationList(contextTypeInformation);
					parser.applyStoredTypeInformationList(true);
				}
			}
			if (contextExpression != null) {
				// cut off stuff after ->
				if (contextExpression instanceof Sequence) {
					Sequence seq = (Sequence) contextExpression;
					for (int i = seq.getElements().length-1; i >= 0; i--) {
						if (seq.getElements()[i] instanceof MemberOperator) {
							if (i < seq.getElements().length-1)
								contextExpression = seq.sequenceWithElementsRemovedFrom(seq.getElements()[i+1]);
							break;
						}
					}
				}
				for (IType t : contextExpression.getType(parser)) {
					C4ScriptBase script = C4ScriptBase.scriptFrom(t);
					if (script != null) {
						if (!contextScriptsChanged) {
							contextScripts.clear();
							contextScriptsChanged = true;
						}
						contextScripts.add(script);
					}
				}
				// expression was of no use - discard and show global proposals
				if (!contextScriptsChanged)
					contextExpression = null;
			}
			parser.endTypeInferenceBlock();
		}
		
		if (contextExpression == null && proposalCycle == ProposalCycle.ALL) {
			if (editorScript.getIndex().getEngine() != null) {
				for (C4Function func : editorScript.getIndex().getEngine().functions()) {
					proposalForFunc(func, prefix, offset, proposals, editorScript.getIndex().getEngine().getName(), true);
				}
				for (C4Variable var : editorScript.getIndex().getEngine().variables()) {
					proposalForVar(var,prefix,offset,proposals);
				}
			}
		}
		
		if (contextExpression == null && (proposalCycle == ProposalCycle.ALL || proposalCycle == ProposalCycle.LOCAL) && activeFunc != null /*&& contextExpression == null* what was the reason for that again?*/) {
			for (C4Variable v : activeFunc.getParameters()) {
				proposalForVar(v, prefix, wordOffset, proposals);
			}
			for (C4Variable v : activeFunc.getLocalVars()) {
				proposalForVar(v, prefix, wordOffset, proposals);
			}
		}

		if (contextExpression == null && proposalCycle != ProposalCycle.OBJECT)
			for (ClonkIndex i : index.relevantIndexes())
				proposalsForIndex(i, offset, wordOffset, prefix, proposals);

		int whatToDisplayFromScripts = INCLUDES;
		if (contextExpression == null || MemberOperator.endsWithDot(contextExpression))
			whatToDisplayFromScripts |= VARIABLES;
		if (contextExpression == null || !MemberOperator.endsWithDot(contextExpression))
			whatToDisplayFromScripts |= FUNCTIONS;
		for (C4ScriptBase s : contextScripts) {
			proposalsFromScript(s, new HashSet<C4ScriptBase>(), prefix, offset, wordOffset, proposals, contextScriptsChanged, index, whatToDisplayFromScripts);
		}
		if (contextExpression == null && proposalCycle == ProposalCycle.ALL) {
			ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
			if (reg.get("keyword") == null) { //$NON-NLS-1$
				reg.put("keyword", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/keyword.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for(String keyword : BuiltInDefinitions.KEYWORDS) {
				if (prefix != null) {
					if (!keyword.toLowerCase().startsWith(prefix)) continue;
				}
				int replacementLength = 0;
				if (prefix != null) replacementLength = prefix.length();
				ClonkCompletionProposal prop = new ClonkCompletionProposal(null, keyword,offset,replacementLength,keyword.length(), reg.get("keyword") , keyword.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, getEditor()); //$NON-NLS-1$
				proposals.add(prop);
			}
		}
	}
	
	public static List<ICompletionProposal> computeProposalsForExpression(ExprElm expression, C4Function function, C4ScriptParser parser, IDocument document) {
		List<ICompletionProposal> result = new LinkedList<ICompletionProposal>();
		C4ScriptCompletionProcessor processor = new C4ScriptCompletionProcessor(null, null);
		ClonkIndex index = function.getScript().getIndex();
		processor.contextExpression = expression;
		processor.internalProposalsInsideOfFunction(expression != null ? expression.getExprEnd() : 0, 0, document, "", result, index, function, function.getScript(), parser);
		return result;
	}
	
	private String getFunctionScaffold(String functionName) {
		StringBuilder builder = new StringBuilder();
		builder.append(Keywords.Func);
		builder.append(" "); //$NON-NLS-1$
		builder.append(functionName);
		builder.append("()"); //$NON-NLS-1$
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
	
	private void callbackProposal(String prefix, String callback, boolean funcSupplied, List<ICompletionProposal> proposals, int offset) {
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		if (reg.get("callback") == null) { //$NON-NLS-1$
			reg.put("callback", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/callback.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		// FIXME: copy signature of overloaded func and respect brace style
		String repString = funcSupplied ? (callback!=null?callback:"") : getFunctionScaffold(callback); //$NON-NLS-1$ //$NON-NLS-2$
		ClonkCompletionProposal prop = new ClonkCompletionProposal(
				null,
				repString, offset, replacementLength, 
				repString.length(), reg.get("callback") , callback, null,null,Messages.C4ScriptCompletionProcessor_Callback, getEditor()); //$NON-NLS-1$
		proposals.add(prop);
	}

	private void proposalsOutsideOfFunction(ITextViewer viewer, int offset,
			int wordOffset, String prefix,
			List<ICompletionProposal> proposals, ClonkIndex index) {
		try {
			boolean funcSupplied = offset >= 5 && viewer.getDocument().get(offset - 5, 5).equalsIgnoreCase("func ");  //$NON-NLS-1$

			for(String callback : BuiltInDefinitions.OBJECT_CALLBACKS) {
				if (prefix != null) {
					if (!callback.toLowerCase().startsWith(prefix))
						continue;
				}
				callbackProposal(prefix, callback, funcSupplied, proposals, offset);
			}
			callbackProposal(prefix, _prefix, funcSupplied, proposals, offset);

		} catch (BadLocationException e) {
			// ignore
		}
		
		
		for(String declarator : BuiltInDefinitions.DECLARATORS) {
			if (prefix != null) {
				if (!declarator.toLowerCase().startsWith(prefix)) continue;
			}
			ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
			if (reg.get("declarator") == null) { //$NON-NLS-1$
				reg.put("declarator", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/declarator.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			int replacementLength = 0;
			if (prefix != null) replacementLength = prefix.length();
			ClonkCompletionProposal prop = new ClonkCompletionProposal(null, declarator,offset,replacementLength,declarator.length(), reg.get("declarator") , declarator.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, getEditor()); //$NON-NLS-1$
			proposals.add(prop);
		}
		
		for(String directive : BuiltInDefinitions.DIRECTIVES) {
			if (prefix != null) {
				if (!directive.toLowerCase().contains(prefix)) continue;
			}
			ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
			if (reg.get("directive") == null) { //$NON-NLS-1$
				reg.put("directive", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/directive.png"), null))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			int replacementLength = 0;
			if (prefix != null) replacementLength = prefix.length();
			ClonkCompletionProposal prop = new ClonkCompletionProposal(null, directive,offset,replacementLength,directive.length(), reg.get("directive") , directive.trim(),null,null,Messages.C4ScriptCompletionProcessor_Engine, getEditor()); //$NON-NLS-1$
			proposals.add(prop);
		}
		
		// propose objects for #include or something
		for (ClonkIndex i : index.relevantIndexes())
			proposalsForIndex(i, offset, wordOffset, prefix, proposals);
	}

	private String getProposalCycleMessage() {
		TriggerSequence sequence = getIterationBinding();
		return String.format(Messages.C4ScriptCompletionProcessor_PressToShowCycle, sequence.format(), proposalCycle.cycle().description());
	}

	private static final int VARIABLES = 1;
	private static final int FUNCTIONS = 2;
	private static final int INCLUDES  = 4;
	
	private void proposalsFromScript(C4ScriptBase script, HashSet<C4ScriptBase> loopCatcher, String prefix, int offset, int wordOffset, List<ICompletionProposal> proposals, boolean noPrivateFuncs, ClonkIndex index, int mask) {
		if (loopCatcher.contains(script)) {
			return;
		}
		loopCatcher.add(script);
		if ((mask & FUNCTIONS) != 0) {
			for (C4Function func : script.functions()) {
				if (func.getVisibility() != C4FunctionScope.GLOBAL)
					if (!noPrivateFuncs  || func.getVisibility() == C4FunctionScope.PUBLIC)
						proposalForFunc(func, prefix, offset, proposals, script.getName(), true);
			}
		}
		if ((mask & VARIABLES) != 0) {
			for (C4Variable var : script.variables()) {
				if (var.getScope() != C4VariableScope.STATIC && var.getScope() != C4VariableScope.CONST)
					proposalForVar(var, prefix, wordOffset, proposals);
			}
		}
		if ((mask & INCLUDES) != 0) {
			for (C4ScriptBase o : script.getIncludes(index))
				proposalsFromScript(o, loopCatcher, prefix, offset, wordOffset, proposals, noPrivateFuncs, index, mask);
		}
	}

	protected C4Function getActiveFunc(IDocument document, int offset) {
		C4ScriptBase thisScript = Utilities.getScriptForEditor(editor);
		return thisScript != null ? thisScript.funcAt(new Region(offset,1)) : null;
	}
	
	private IContextInformation prevInformation;

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		IContextInformation info = null;
		try {
	        FuncCallInfo funcCallInfo = editor.getInnermostCallFuncExprParm(offset);
	        if (funcCallInfo != null) {
	        	C4Declaration dec = funcCallInfo.callFunc.getDeclaration();
	        	if (dec instanceof C4Function) {
	        		String parmString = ((C4Function)dec).getLongParameterString(false, false).trim();
	        		if (parmString.length() == 0)
	        			parmString = Messages.C4ScriptCompletionProcessor_NoParameters;
	        		info = new ClonkContextInformation(
	        			dec.getName() + "()", null, //$NON-NLS-1$
	        			parmString,
	        			funcCallInfo.parmIndex, funcCallInfo.parmsStart, funcCallInfo.parmsEnd, ((C4Function)dec).getParameters().size()
	        		);
	        	}
	        }
		} catch (Exception e) { 	    
			e.printStackTrace();
		}
		try {
			// HACK: if changed, hide the old one -.-
			if (!Utilities.objectsEqual(prevInformation, info)) {
				ClonkContentAssistant assistant = this.getEditor().getContentAssistant();
				//if (!assistant.isProposalPopupActive())
					assistant.hide();
			}
			return info != null ? new IContextInformation[] {info} : null;
		} finally {
			prevInformation = info;
		}
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return COMPLETION_INFORMATION_AUTO_ACTIVATION_CHARS;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return CONTEXT_INFORMATION_AUTO_ACTIVATION_CHARS;
	}

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
	
	public String getErrorMessage() {
		return null;
	}
	
}
