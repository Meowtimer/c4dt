package net.arctics.clonk.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.BuiltInDefinitions;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClonkCompletionProcessor implements IContentAssistProcessor {

	private final int SHOW_ALL = 0;
	private final int SHOW_LOCAL = 1;
	
	private ITextEditor editor;
	private ContentAssistant assistant;
	private ExprElm contextExpression;
	private int proposalCycle = SHOW_ALL;
	
	public ClonkCompletionProcessor(ITextEditor editor,
			ContentAssistant assistant) {
		this.editor = editor;
		this.assistant = assistant;

		assistant.addCompletionListener(new ICompletionListener() {
			
			public void selectionChanged(ICompletionProposal proposal,
					boolean smartToggle) {
			}
		
			public void assistSessionStarted(ContentAssistEvent event) {
				proposalCycle = SHOW_ALL;
			}
		
			public void assistSessionEnded(ContentAssistEvent event) {
			}
		});
		
	}
	
	protected void doCycle() {
		if (proposalCycle == SHOW_ALL) proposalCycle = SHOW_LOCAL;
		else proposalCycle = SHOW_ALL;
	}
	
	public void proposalForFunc(C4Function func,String prefix,int offset,List<ClonkCompletionProposal> proposals,String parentName) {
		if (prefix != null) {
			if (!func.getName().toLowerCase().startsWith(prefix))
				return;
		}
		String displayString = func.getLongParameterString(true);
		int replacementLength = 0;
		if (prefix != null) replacementLength = prefix.length();
		
		String contextInfoString = func.getLongParameterString(false);
		IContextInformation contextInformation = new ContextInformation(func.getName() + "()",contextInfoString); 
		
		ClonkCompletionProposal prop = new ClonkCompletionProposal(func.getName() + "()",offset,replacementLength,func.getName().length()+1,
				Utilities.getIconForFunction(func), displayString.trim(),contextInformation,null," - " + parentName);
		proposals.add(prop);
	}

	public void proposalForObject(C4Object obj,String prefix,int offset,List<ClonkCompletionProposal> proposals) {
		try {
			if (obj == null || obj.getId() == null)
				return;

			if (prefix != null) {
				if (!(
						obj.getName().toLowerCase().startsWith(prefix) ||
						obj.getId().getName().toLowerCase().startsWith(prefix) ||
						(obj instanceof C4ObjectIntern && ((C4ObjectIntern)obj).getObjectFolder() != null && ((C4ObjectIntern)obj).getObjectFolder().getName().startsWith(prefix))
				))
					return;
			}
			String displayString = obj.getName();
			int replacementLength = 0;
			if (prefix != null) replacementLength = prefix.length();

			String contextInfoString = obj.getName();
			IContextInformation contextInformation = new ContextInformation(obj.getId().getName(),contextInfoString); 

			ClonkCompletionProposal prop = new ClonkCompletionProposal(obj.getId().getName(),offset,replacementLength,obj.getId().getName().length(),
					Utilities.getIconForObject(obj), displayString.trim(),contextInformation,null," - " + obj.getId().getName());
			proposals.add(prop);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(obj.toString());
		}
	}

	public ClonkCompletionProposal proposalForVar(C4Variable var, String prefix, int offset, List<ClonkCompletionProposal> proposals) {
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
	
	private void proposalForIndex(ClonkIndex index, int offset, int wordOffset, String prefix,
			List<ClonkCompletionProposal> proposals) {
		if (!index.isEmpty()) {
			for (C4Function func : index.getGlobalFunctions()) {
				if (func.getScript() == null)
					System.out.println(func.getName());
				else
					proposalForFunc(func, prefix, offset, proposals, func.getScript().getName());
			}
			for (C4Variable var : index.getStaticVariables()) {
				proposalForVar(var,prefix,offset,proposals);
			}
			for (List<C4Object> objs : index.getIndexedObjects().values()) {
				proposalForObject(objs.get(objs.size()-1), prefix, wordOffset, proposals);
			}
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
		
		ClonkProjectNature nature = net.arctics.clonk.Utilities.getProject(editor);
		List<String> statusMessages = new ArrayList<String>(4);
		List<ClonkCompletionProposal> proposals = new ArrayList<ClonkCompletionProposal>();
		ClonkIndex index = nature.getIndexedData();

		// refresh to find about whether caret is inside a function and to get all the declarations
		try {
			((C4ScriptEditor)editor).reparseWithDocumentContents(null,true);
		} catch (CompilerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final C4Function activeFunc = getActiveFunc(doc, offset);
		
		statusMessages.add("Project files");
		
		if (proposalCycle == SHOW_ALL || activeFunc == null) {
			if (!ClonkCore.EXTERN_INDEX.isEmpty()) {
				statusMessages.add("Extern libs");
			}
			if (ClonkCore.ENGINE_OBJECT != null) {
				statusMessages.add("Engine functions");
			}
		}
		
		if (activeFunc == null) {
		
			try {
				if (viewer.getDocument().get(offset - 5, 5).equalsIgnoreCase("func ")) {
					for(String callback : BuiltInDefinitions.OBJECT_CALLBACKS) {
						if (prefix != null) {
							if (!callback.toLowerCase().startsWith(prefix)) continue;
						}
						ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
						if (reg.get("callback") == null) {
							reg.put("callback", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/callback.png"), null)));
						}
						int replacementLength = 0;
						if (prefix != null) replacementLength = prefix.length();
						ClonkCompletionProposal prop = new ClonkCompletionProposal(callback,offset,replacementLength,callback.length(), reg.get("callback") , callback.trim(),null,null," - Callback");
						proposals.add(prop);
					}
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
					if (!directive.toLowerCase().startsWith(prefix)) continue;
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
			proposalForIndex(index, offset, wordOffset, prefix, proposals);
			proposalForIndex(ClonkCore.EXTERN_INDEX, offset, wordOffset, prefix, proposals);
		}
		else {
			proposalForIndex(index, offset, wordOffset, prefix, proposals);
			
			if (proposalCycle == SHOW_ALL) {
				proposalForIndex(ClonkCore.EXTERN_INDEX, offset, wordOffset, prefix, proposals);
				
				if (ClonkCore.ENGINE_OBJECT != null) {
					for (C4Function func : ClonkCore.ENGINE_OBJECT.getDefinedFunctions()) {
						proposalForFunc(func, prefix, offset, proposals, ClonkCore.ENGINE_OBJECT.getName());
					}
					for (C4Variable var : ClonkCore.ENGINE_OBJECT.getDefinedVariables()) {
						proposalForVar(var,prefix,offset,proposals);
					}
				}
				
				if (activeFunc != null) {
					for (C4Variable v : activeFunc.getParameter()) {
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
					C4ScriptParser parser = C4ScriptParser.reportExpressionsInStatements(doc, activeFunc.getBody().getOffset(), offset, contextScript, activeFunc, new IExpressionListener() {
						public TraversalContinuation expressionDetected(ExprElm expression) {
							if (activeFunc.getBody().getOffset() + expression.getExprStart() <= preservedOffset) {
								contextExpression = expression;
								return TraversalContinuation.Continue;
							}
							return TraversalContinuation.Cancel;
						}
					});
					if (contextExpression != null) {
						C4Object guessed = contextExpression.guessObjectType(parser);
						if (guessed != null) {
							contextScript = guessed;
							contextObjChanged = true;
						}
					}
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CompilerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParsingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (contextScript != null) {
				proposalsFromScript(contextScript, new HashSet<C4ScriptBase>(), prefix, offset, wordOffset, proposals, contextObjChanged);
			}
			if (proposalCycle == SHOW_ALL) {
				for(String keyword : BuiltInDefinitions.KEYWORDS) {
					if (prefix != null) {
						if (!keyword.toLowerCase().startsWith(prefix)) continue;
					}
					ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
					if (reg.get("keyword") == null) {
						reg.put("keyword", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/keyword.png"), null)));
					}
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					ClonkCompletionProposal prop = new ClonkCompletionProposal(keyword,offset,replacementLength,keyword.length(), reg.get("keyword") , keyword.trim(),null,null," - Engine");
					proposals.add(prop);
				}
			}
		}
		
		StringBuilder statusMessage = new StringBuilder("Shown data: ");
		for(String message : statusMessages) {
			statusMessage.append(message);
			if (statusMessages.get(statusMessages.size() - 1) != message) statusMessage.append(", ");
		}
		
		assistant.setStatusMessage(statusMessage.toString());
		if (proposalCycle == SHOW_LOCAL)
			assistant.setStatusMessage("Press 'Ctrl+Space' to show all completions");
		else if (proposalCycle == SHOW_ALL)
			assistant.setStatusMessage("Press 'Ctrl+Space' to show project completions");
		
		doCycle();
		
		if (proposals.size() == 0) {
			return new ICompletionProposal[] { new CompletionProposal("",offset,0,0,null,"No proposals available",null,null) };
		}
		
		ClonkCompletionProposal[] result = proposals.toArray(new ClonkCompletionProposal[] {});
		
		Arrays.sort(result, new Comparator<ClonkCompletionProposal>() {
			public int compare(ClonkCompletionProposal arg0,
					ClonkCompletionProposal arg1) {
				return (arg0.getDisplayString().compareToIgnoreCase(arg1.getDisplayString()));
			}
			
		});
		
		return result;
	}

	private void proposalsFromScript(C4ScriptBase script, HashSet<C4ScriptBase> loopCatcher, String prefix, int offset, int wordOffset, List<ClonkCompletionProposal> proposals, boolean noPrivateFuncs) {
		if (loopCatcher.contains(script)) {
			return;
		}
		loopCatcher.add(script);
		for (C4Function func : script.getDefinedFunctions()) {
			if (func.getVisibility() != C4FunctionScope.FUNC_GLOBAL)
				if (!noPrivateFuncs  || func.getVisibility() == C4FunctionScope.FUNC_PUBLIC)
					proposalForFunc(func, prefix, offset, proposals, script.getName());
		}
		for (C4Variable var : script.getDefinedVariables()) {
			if (var.getScope() != C4VariableScope.VAR_STATIC && var.getScope() != C4VariableScope.VAR_CONST)
			proposalForVar(var, prefix, wordOffset, proposals);
		}
		for (C4Object o : script.getIncludes())
			proposalsFromScript(o, loopCatcher, prefix, offset, wordOffset, proposals, noPrivateFuncs);
	}

	protected C4Function getActiveFunc(IDocument document, int offset) {
		C4ScriptBase thisScript = Utilities.getScriptForEditor(editor);
		return thisScript != null ? thisScript.funcAt(new Region(offset,1)) : null;
		// restored
//		try {
//			int openBrackets = 0;
//			int closeBrackets = 0;
//			String content = document.get(0, offset);
//			for(int i = 0; i < content.length();i++) {
//				char c = content.charAt(i);
//				if (c == '{') openBrackets++;
//				else if (c == '}') closeBrackets++;
//			}
//			if (openBrackets > closeBrackets) return true;
//			else return false;
//		} catch (BadLocationException e) {
//			return false;
//		}
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
		for(C4Function func : ClonkCore.ENGINE_OBJECT.getDefinedFunctions()) {
			if (func.getName().equalsIgnoreCase(prefix)) {
				String displayString = func.getLongParameterString(false).trim();
				info = new ContextInformation(func.getName() + "()",displayString); 
			}
		}
		
//		IContextInformation info = new ContextInformation("contextdisplay String", "information String");
		
		return new IContextInformation[] { info };
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
//		return new char[] { '(' };
	}

	public IContextInformationValidator getContextInformationValidator() {
		return new ClonkContextInformationValidator();
	}

	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
