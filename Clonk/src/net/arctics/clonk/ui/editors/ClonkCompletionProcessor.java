package net.arctics.clonk.ui.editors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BuiltInDefinitions;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4ScriptParser.ExprElm;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.Utilities;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClonkCompletionProcessor implements IContentAssistProcessor {

	private ITextEditor editor;
	private ContentAssistant assistant;
	
	public ClonkCompletionProcessor(ITextEditor editor,
			ContentAssistant assistant) {
		this.editor = editor;
		this.assistant = assistant;

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
	
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		
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
//		if (indexer.) {
//			statusMessages.add("Clonk directory");
//		}
		// TODO implement Clonk directory indexing status indicator
		if (nature.isIndexed()) {
			statusMessages.add("Project files");
		}
		if (ClonkCore.ENGINE_OBJECT != null) {
			statusMessages.add("Engine functions");
		}
		
		if (!isInCodeBody(doc, offset)) {
		
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
		}
		else {
			if (!index.isEmpty()) {
				for (C4Function func : index.getGlobalFunctions()) {
					proposalForFunc(func, prefix, offset, proposals, func.getObject().getName());
				}
				for (C4Variable var : index.getStaticVariables()) {
					if (prefix != null && !var.getName().toLowerCase().startsWith(prefix))
						continue;
					String displayString = var.getName();
					int replacementLength = 0;
					if (prefix != null)
						replacementLength = prefix.length();
					ClonkCompletionProposal prop = new ClonkCompletionProposal(
						var.getName(), offset, replacementLength, var.getName().length(), Utilities.getIconForVariable(var), displayString, 
						null, var.getAdditionalProposalInfo(), " - " + var.getObject().getName()
					);
					proposals.add(prop);
				}
			}
			
			if (ClonkCore.ENGINE_OBJECT != null) {
				for (C4Function func : ClonkCore.ENGINE_OBJECT.getDefinedFunctions()) {
					proposalForFunc(func, prefix, offset, proposals, ClonkCore.ENGINE_OBJECT.getName());
				}
			}
			
			C4Object contextObj = Utilities.getObjectForEditor(editor);
			if (contextObj != null) {
				try {
					int off = Utilities.getStartOfExpression(doc, offset);
					String expr = doc.get(off,offset-off+1);
					InputStream stream = new ByteArrayInputStream(expr.getBytes());
					C4ScriptParser parser = new C4ScriptParser(stream,  expr.length(), contextObj);
					ExprElm e = parser.parseExpressionWithoutOperators(0);
					C4Object guessedType = (e == null) ? null : e.guessObjectType(parser);
					if (guessedType != null)
						contextObj = guessedType;
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
			
			if (contextObj != null) {
				for (C4Function func : contextObj.getDefinedFunctions()) {
					proposalForFunc(func, prefix, offset, proposals, contextObj.getName());
				}
			}
			
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
		
		StringBuilder statusMessage = new StringBuilder("Indexed data: ");
		for(String message : statusMessages) {
			statusMessage.append(message);
			if (statusMessages.get(statusMessages.size() - 1) != message) statusMessage.append(", ");
		}
		
		assistant.setStatusMessage(statusMessage.toString());
		
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

	protected boolean isInCodeBody(IDocument document, int offset) {
		// needs refresh
//		C4Object thisObj = Utilities.getObjectForEditor(editor);
//		return thisObj != null && thisObj.funcAt(new Region(offset,1)) != null;
		// restored
		try {
			int openBrackets = 0;
			int closeBrackets = 0;
			String content = document.get(0, offset);
			for(int i = 0; i < content.length();i++) {
				char c = content.charAt(i);
				if (c == '{') openBrackets++;
				else if (c == '}') closeBrackets++;
			}
			if (openBrackets > closeBrackets) return true;
			else return false;
		} catch (BadLocationException e) {
			return false;
		}
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
