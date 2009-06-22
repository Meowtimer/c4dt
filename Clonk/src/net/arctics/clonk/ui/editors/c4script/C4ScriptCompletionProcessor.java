package net.arctics.clonk.ui.editors.c4script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BuiltInDefinitions;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.IStoredTypeInformation;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.*;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.WordScanner;
import net.arctics.clonk.util.Utilities;

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
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class C4ScriptCompletionProcessor extends ClonkCompletionProcessor<C4ScriptEditor> {
	
	private final class ClonkCompletionListener implements ICompletionListener, ICompletionListenerExtension {

		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		}
	
		public void assistSessionStarted(ContentAssistEvent event) {
			proposalCycle = ProposalCycle.SHOW_ALL;
			
			// refresh to find out whether caret is inside a function and to get all the declarations
			try {
				try {
					((C4ScriptEditor)editor).reparseWithDocumentContents(null,true);
				} catch (ParsingException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		public void assistSessionEnded(ContentAssistEvent event) {
		}

		public void assistSessionRestarted(ContentAssistEvent event) {
			// needs to be reversed because it gets cycled after computing the proposals...
			proposalCycle = proposalCycle.reverseCycle();
		}
		
	}
	
	private enum ProposalCycle {
		SHOW_ALL,
		SHOW_LOCAL,
		SHOW_OBJECT;
		
		public String description() {
			switch (this) {
			case SHOW_ALL:
				return "all completions";
			case SHOW_LOCAL:
				return "local completions";
			case SHOW_OBJECT:
				return "object completions";
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
	private ExprElm contextExpression;
	private List<IStoredTypeInformation> contextTypeInformation;
	private ProposalCycle proposalCycle = ProposalCycle.SHOW_ALL;
	private C4Function _activeFunc;
	
	public C4ScriptCompletionProcessor(C4ScriptEditor editor, ContentAssistant assistant) {
		super(editor);
		this.assistant = assistant;
		
		assistant.setRepeatedInvocationTrigger(getIterationBinding());
		assistant.addCompletionListener(new ClonkCompletionListener());
		
	}
	
	protected void doCycle() {
		proposalCycle = proposalCycle.cycle();
	}

	public ClonkCompletionProposal proposalForVar(C4Variable var, String prefix, int offset, List<ICompletionProposal> proposals) {
		if (prefix != null && !var.getName().toLowerCase().startsWith(prefix))
			return null;
		if (var.getScript() == null)
			return null;
		String displayString = var.getName();
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		ClonkCompletionProposal prop = new ClonkCompletionProposal(
			var.getName(), offset, replacementLength, var.getName().length(), Utilities.getIconForVariable(var), displayString, 
			null, var.getAdditionalProposalInfo(), " - " + var.getScript().getName()
		);
		proposals.add(prop);
		return prop;
	}
	
	private void proposalsForIndex(ClonkIndex index, int offset, int wordOffset, String prefix, List<ICompletionProposal> proposals) {
		if (!index.isEmpty()) {
			if (_activeFunc != null) {
				for (C4Function func : index.getGlobalFunctions()) {
					if (func.getScript() == null)
						System.out.println(func.getName());
					else
						proposalForFunc(func, prefix, offset, proposals, func.getScript().getName(), true);
				}
				for (C4Variable var : index.getStaticVariables()) {
					proposalForVar(var,prefix,offset,proposals);
				}
			}
			proposalsForIndexedObjects(index, offset, wordOffset, prefix, proposals);
		}
	}
	
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		int wordOffset = offset - 1;
		WordScanner scanner = new WordScanner();
		IDocument doc = viewer.getDocument();
		String prefix = null;
		try {
			while (scanner.isWordPart(doc.getChar(wordOffset))) {
				wordOffset--;
			}
			wordOffset++;
			if (wordOffset < offset) {
				prefix = doc.get(wordOffset, offset - wordOffset);
				
				offset = wordOffset;
			}
			if (prefix != null)
				prefix = prefix.toLowerCase();
		} catch (BadLocationException e) {
			prefix = null;
		}
		
		ClonkProjectNature nature = Utilities.getClonkNature(editor);
		List<String> statusMessages = new ArrayList<String>(4);
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		ClonkIndex index = nature.getIndex();

		final C4Function activeFunc = getActiveFunc(doc, offset);
		this._activeFunc = activeFunc;
		
		statusMessages.add("Project files");
		
		if (proposalCycle == ProposalCycle.SHOW_ALL || activeFunc == null) {
			if (!ClonkCore.getDefault().EXTERN_INDEX.isEmpty()) {
				statusMessages.add("Extern libs");
			}
			if (ClonkCore.getDefault().ENGINE_OBJECT != null) {
				statusMessages.add("Engine functions");
			}
		}
		
		if (activeFunc == null) {
			proposalsOutsideOfFunction(viewer, offset, wordOffset, prefix,
					proposals, index);
		}
		else {
			proposalsInsideOfFunction(offset, wordOffset, doc, prefix,
					proposals, index, activeFunc);
		}
		
		StringBuilder statusMessage = new StringBuilder("Shown data: ");
		for(String message : statusMessages) {
			statusMessage.append(message);
			if (statusMessages.get(statusMessages.size() - 1) != message) statusMessage.append(", ");
		}
		
		//assistant.setStatusMessage(statusMessage.toString());
		assistant.setStatusMessage(getProposalCycleMessage());
		
		doCycle();
		
		if (proposals.size() == 0) {
			return new ICompletionProposal[] { new CompletionProposal("",offset,0,0,null,"No proposals available",null,null) };
		}
		
		return sortProposals(proposals);
	}

	private void proposalsInsideOfFunction(int offset, int wordOffset,
			IDocument doc, String prefix,
			List<ICompletionProposal> proposals, ClonkIndex index,
			final C4Function activeFunc) {
		
		if (proposalCycle == ProposalCycle.SHOW_ALL) {
			
			if (ClonkCore.getDefault().ENGINE_OBJECT != null) {
				for (C4Function func : ClonkCore.getDefault().ENGINE_OBJECT.functions()) {
					proposalForFunc(func, prefix, offset, proposals, ClonkCore.getDefault().ENGINE_OBJECT.getName(), true);
				}
				for (C4Variable var : ClonkCore.getDefault().ENGINE_OBJECT.variables()) {
					proposalForVar(var,prefix,offset,proposals);
				}
			}
			
			if (activeFunc != null) {
				for (C4Variable v : activeFunc.getParameters()) {
					proposalForVar(v, prefix, wordOffset, proposals);
				}
				for (C4Variable v : activeFunc.getLocalVars()) {
					proposalForVar(v, prefix, wordOffset, proposals);
				}
			}
		}
		C4ScriptBase contextScript = Utilities.getScriptForEditor(editor);
		boolean contextObjChanged = false;
		if (contextScript != null) {
			try {
				contextExpression = null;
				final int preservedOffset = offset;
				C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(doc, activeFunc.getBody().getOffset(), offset, contextScript, activeFunc, new IExpressionListener() {
					public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
						if (expression instanceof Statement) {
							if (contextExpression != null && !contextExpression.containedIn(expression))
								contextExpression = null;
							return TraversalContinuation.Continue;
						}
						if (activeFunc.getBody().getOffset() + expression.getExprStart() <= preservedOffset) {
							contextExpression = expression;
							try {
	                            contextTypeInformation = parser.copyCurrentTypeInformationList();
                            } catch (CloneNotSupportedException e) {
	                            e.printStackTrace();
                            }
							return TraversalContinuation.Continue;
						}
						return TraversalContinuation.Cancel;
					}
				});
				if (contextExpression != null) {
					parser.pushTypeInformationList(contextTypeInformation);
					parser.applyStoredTypeInformationList(true);
					if (contextExpression.containsOffset(preservedOffset-activeFunc.getBody().getOffset())) {
						C4Object guessedType = parser.queryObjectTypeOfExpression(contextExpression);
						if (guessedType != null) {
							contextScript = guessedType;
							contextObjChanged = true;
						}
					}
				}
				parser.endTypeInferenceBlock();
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
		
		if (!contextObjChanged) {
			if (proposalCycle != ProposalCycle.SHOW_OBJECT)
				proposalsForIndex(index, offset, wordOffset, prefix, proposals);;
			if (proposalCycle == ProposalCycle.SHOW_ALL)
				proposalsForIndex(ClonkCore.getDefault().EXTERN_INDEX, offset, wordOffset, prefix, proposals);
		}
		
		if (contextScript != null) {
			proposalsFromScript(contextScript, new HashSet<C4ScriptBase>(), prefix, offset, wordOffset, proposals, contextObjChanged, index);
		}
		if (proposalCycle == ProposalCycle.SHOW_ALL) {
			ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
			if (reg.get("keyword") == null) {
				reg.put("keyword", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/keyword.png"), null)));
			}
			for(String keyword : BuiltInDefinitions.KEYWORDS) {
				if (prefix != null) {
					if (!keyword.toLowerCase().startsWith(prefix)) continue;
				}
				int replacementLength = 0;
				if (prefix != null) replacementLength = prefix.length();
				ClonkCompletionProposal prop = new ClonkCompletionProposal(keyword,offset,replacementLength,keyword.length(), reg.get("keyword") , keyword.trim(),null,null," - Engine");
				proposals.add(prop);
			}
		}
	}

	private void proposalsOutsideOfFunction(ITextViewer viewer, int offset,
			int wordOffset, String prefix,
			List<ICompletionProposal> proposals, ClonkIndex index) {
		try {
			boolean funcSupplied = offset >= 5 && viewer.getDocument().get(offset - 5, 5).equalsIgnoreCase("func "); 

			for(String callback : BuiltInDefinitions.OBJECT_CALLBACKS) {
				if (prefix != null) {
					if (!callback.toLowerCase().startsWith(prefix))
						continue;
				}
				ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
				if (reg.get("callback") == null) {
					reg.put("callback", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/callback.png"), null)));
				}
				int replacementLength = 0;
				if (prefix != null) replacementLength = prefix.length();
				String repString = funcSupplied ? callback : ("protected func " + callback + "() {\n}"); 
				ClonkCompletionProposal prop = new ClonkCompletionProposal(
						repString, offset, replacementLength, 
						repString.length(), reg.get("callback") , callback, null,null," - Callback");
				proposals.add(prop);
			}

		} catch (BadLocationException e) {
			// ignore
		}
		
		
		for(String declarator : BuiltInDefinitions.DECLARATORS) {
			if (prefix != null) {
				if (!declarator.toLowerCase().startsWith(prefix)) continue;
			}
			ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
			if (reg.get("declarator") == null) {
				reg.put("declarator", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/declarator.png"), null)));
			}
			int replacementLength = 0;
			if (prefix != null) replacementLength = prefix.length();
			ClonkCompletionProposal prop = new ClonkCompletionProposal(declarator,offset,replacementLength,declarator.length(), reg.get("declarator") , declarator.trim(),null,null," - Engine");
			proposals.add(prop);
		}
		
		for(String directive : BuiltInDefinitions.DIRECTIVES) {
			if (prefix != null) {
				if (!directive.toLowerCase().contains(prefix)) continue;
			}
			ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
			if (reg.get("directive") == null) {
				reg.put("directive", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/directive.png"), null)));
			}
			int replacementLength = 0;
			if (prefix != null) replacementLength = prefix.length();
			ClonkCompletionProposal prop = new ClonkCompletionProposal(directive,offset,replacementLength,directive.length(), reg.get("directive") , directive.trim(),null,null," - Engine");
			proposals.add(prop);
		}
		
		// propose objects for #include or something
		proposalsForIndex(index, offset, wordOffset, prefix, proposals);
		if (proposalCycle == ProposalCycle.SHOW_ALL)
			proposalsForIndex(ClonkCore.getDefault().EXTERN_INDEX, offset, wordOffset, prefix, proposals);
	}

	private String getProposalCycleMessage() {
		TriggerSequence sequence = getIterationBinding();
		return "Press '" + sequence.format() + "' to show " + proposalCycle.cycle().description();
	}

	private void proposalsFromScript(C4ScriptBase script, HashSet<C4ScriptBase> loopCatcher, String prefix, int offset, int wordOffset, List<ICompletionProposal> proposals, boolean noPrivateFuncs, ClonkIndex index) {
		if (loopCatcher.contains(script)) {
			return;
		}
		loopCatcher.add(script);
		for (C4Function func : script.functions()) {
			if (func.getVisibility() != C4FunctionScope.FUNC_GLOBAL)
				if (!noPrivateFuncs  || func.getVisibility() == C4FunctionScope.FUNC_PUBLIC)
					proposalForFunc(func, prefix, offset, proposals, script.getName(), true);
		}
		for (C4Variable var : script.variables()) {
			if (var.getScope() != C4VariableScope.VAR_STATIC && var.getScope() != C4VariableScope.VAR_CONST)
				proposalForVar(var, prefix, wordOffset, proposals);
		}
		for (C4ScriptBase o : script.getIncludes(index))
			proposalsFromScript(o, loopCatcher, prefix, offset, wordOffset, proposals, noPrivateFuncs, index);
	}

	protected C4Function getActiveFunc(IDocument document, int offset) {
		C4ScriptBase thisScript = Utilities.getScriptForEditor(editor);
		return thisScript != null ? thisScript.funcAt(new Region(offset,1)) : null;
	}
	
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		
		int wordOffset = offset - 2;
		WordScanner scanner = new WordScanner();
		IDocument doc = viewer.getDocument();
		String prefix = null;
		try {
			while (scanner.isWordPart(doc.getChar(wordOffset))) {
				wordOffset--;
			}
			wordOffset++;
			if (wordOffset < offset-1) {
				prefix = doc.get(wordOffset, offset-1 - wordOffset);
				
				offset = wordOffset;
			}
		} catch (BadLocationException e) {
			prefix = null;
		}
		IContextInformation info = null;
		for(C4Function func : ClonkCore.getDefault().ENGINE_OBJECT.functions()) {
			if (func.getName().equalsIgnoreCase(prefix)) {
				String displayString = func.getLongParameterString(false).trim();
				info = new ContextInformation(func.getName() + "()",displayString);
				break;
			}
		}
		
		return new IContextInformation[] { info };
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
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
