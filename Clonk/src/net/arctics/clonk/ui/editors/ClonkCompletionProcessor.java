package net.arctics.clonk.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BuiltInDefinitions;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4Type;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.ClonkIndexer;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
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
		ClonkIndexer indexer = nature.getIndexer();
		if (indexer.isClonkDirIndexed()) {
			statusMessages.add("Clonk directory");
		}
		if (nature.isIndexed()) {
			statusMessages.add("Project files");
		}
		if (ClonkCore.ENGINE_FUNCTIONS.size() > 0) {
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
			IFile scriptFile = Utilities.getEditingFile(editor);
			if (indexer.getObjects().size() > 0) {
				for (C4Object obj : indexer.getObjects().values()) {
					boolean currentObj = scriptFile.equals(obj.getScript());
					for (C4Function func : obj.getDefinedFunctions()) {
						if (currentObj || func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
							if (prefix != null) {
								if (!func.getName().toLowerCase().startsWith(prefix)) continue;
							}
							
							StringBuilder displayString = new StringBuilder(func.getName());
							displayString.append("(");
							if (func.getParameter().size() > 0) {
								for(C4Variable par : func.getParameter()) {
									if (par.getType() != C4Type.UNKNOWN && par.getType() != null) {
										displayString.append(par.getType().toString());
										displayString.append(' ');
										displayString.append(par.getName());
										displayString.append(',');
										displayString.append(' ');
									}
								}
							}
							if (displayString.charAt(displayString.length() - 1) == ' ') {
								displayString.delete(displayString.length() - 2,displayString.length());
							}
							displayString.append(")");
							int replacementLength = 0;
							if (prefix != null) replacementLength = prefix.length();
							
							String contextInfoString = func.getLongParameterString(false);
							IContextInformation contextInformation = new ContextInformation(func.getName() + "()",contextInfoString); 
							
							ClonkCompletionProposal prop = new ClonkCompletionProposal(func.getName(),offset,replacementLength,func.getName().length(), Utilities.getIconForFunction(func), displayString.toString().trim(),contextInformation,null," - " + obj.getName());
							proposals.add(prop);
						}
					}
					if (currentObj) {
						ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
						for (C4Variable var : obj.getDefinedVariables()) {
							if (prefix != null && !var.getName().toLowerCase().startsWith(prefix))
								continue;
							String displayString = var.getName();
							int replacementLength = 0;
							if (prefix != null)
								replacementLength = prefix.length();
							ClonkCompletionProposal prop = new ClonkCompletionProposal(
								var.getName(), offset, replacementLength, var.getName().length(), Utilities.getIconForVariable(var), displayString, 
								null, var.getAdditionalProposalInfo(), " - " + obj.getName()
							);
							proposals.add(prop);
						}
					}
				}
			}
			
			if (ClonkCore.ENGINE_FUNCTIONS.size() > 0) {
				for(C4Function func : ClonkCore.ENGINE_FUNCTIONS) {
					if (prefix != null) {
						if (!func.getName().toLowerCase().startsWith(prefix)) continue;
					}
					ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
					if (reg.get("func_global") == null) {
						reg.put("func_global", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/global.png"), null)));
					}
					String displayString = func.getLongParameterString(true);
					int replacementLength = 0;
					if (prefix != null) replacementLength = prefix.length();
					
					String contextInfoString = func.getLongParameterString(false);
					IContextInformation contextInformation = new ContextInformation(func.getName() + "()",contextInfoString); 
					
					ClonkCompletionProposal prop = new ClonkCompletionProposal(func.getName() + "()",offset,replacementLength,func.getName().length() + 1, reg.get("func_global") , displayString.trim(),contextInformation,func.getDescription()," - Engine");
					proposals.add(prop);
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
		for(C4Function func : ClonkCore.ENGINE_FUNCTIONS) {
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
		return new char[] { '(' };
	}

	public IContextInformationValidator getContextInformationValidator() {
		return new ClonkContextInformationValidator();
	}

	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
