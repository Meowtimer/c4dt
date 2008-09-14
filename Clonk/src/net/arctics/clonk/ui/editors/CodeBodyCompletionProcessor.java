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
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;


public class CodeBodyCompletionProcessor implements IContentAssistProcessor {
	private ITextEditor editor;
	private ContentAssistant assistant;
	
	public CodeBodyCompletionProcessor(ITextEditor editor,
			ContentAssistant assistant) {
		this.editor = editor;
		this.assistant = assistant;

	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,	int offset) {
//		viewer.addTextInputListener(compiler);
		
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
		} catch (BadLocationException e) {
			prefix = null;
		}
		
		ClonkProjectNature nature = getProject(editor);
		List<String> statusMessages = new ArrayList<String>(4);
		List<ClonkCompletionProposal> proposals = new ArrayList<ClonkCompletionProposal>();
		ClonkIndexer indexer = nature.getIndexer();
		if (indexer.isClonkDirIndexed()) {
			statusMessages.add("Clonk directory");
		}
		if (nature.isIndexed()) {
			statusMessages.add("Project files");
		}
		if (indexer.getObjects().size() > 0) {
			for (C4Object obj : indexer.getObjects().values()) {
				for (C4Function func : obj.getDefinedFunctions()) {
					if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
						if (prefix != null) {
							if (!func.getName().toLowerCase().startsWith(prefix.toLowerCase())) continue;
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
						ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
						if (reg.get("func_global") == null) {
							reg.put("func_global", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/global.png"), null)));
						}
						int replacementLength = 0;
						if (prefix != null) replacementLength = prefix.length();
						ClonkCompletionProposal prop = new ClonkCompletionProposal(func.getName(),offset,replacementLength,func.getName().length(), reg.get("func_global") , displayString.toString(),null,null," - " + obj.getName());
						proposals.add(prop);
					}
				}
			}
		}
		
		if (ClonkCore.ENGINE_FUNCTIONS.size() > 0) {
			for(C4Function func : ClonkCore.ENGINE_FUNCTIONS) {
				if (prefix != null) {
					if (!func.getName().toLowerCase().startsWith(prefix.toLowerCase())) continue;
				}
				ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
				if (reg.get("func_global") == null) {
					reg.put("func_global", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/global.png"), null)));
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
				ClonkCompletionProposal prop = new ClonkCompletionProposal(func.getName(),offset,replacementLength,func.getName().length(), reg.get("func_global") , displayString.toString(),null,func.getDescription()," - Engine");
				proposals.add(prop);
			}
			statusMessages.add("Engine functions");
		}
		
		for(String keyword : BuiltInDefinitions.KEYWORDS) {
			if (prefix != null) {
				if (!keyword.toLowerCase().startsWith(prefix.toLowerCase())) continue;
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
				return (arg0.getDisplayString().compareTo(arg1.getDisplayString()));
			}
			
		});
		
		return result;
	}
	
	protected ClonkProjectNature getProject(ITextEditor editor) {
		try {
			if (editor.getEditorInput() instanceof FileEditorInput) {
				IProjectNature clonkProj = ((FileEditorInput)editor.getEditorInput()).getFile().getProject().getNature("net.arctics.clonk.clonknature");
				if (clonkProj instanceof ClonkProjectNature) {
					return (ClonkProjectNature)clonkProj;
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected IFile getEditingFile(ITextEditor editor) {
		if (editor.getEditorInput() instanceof FileEditorInput) {
			return ((FileEditorInput)editor.getEditorInput()).getFile();
		}
		else return null;
	}
	
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
//		String context = viewer.getDocument().get();
		IContextInformation[] result = new IContextInformation[2];
		result[0] = new ContextInformation("das ist der hallo","das ist die information");
		result[1] = new ContextInformation("das ist der fritz","das ist die fritz-information");
		return result;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return new char[] {'('};
	}

	public IContextInformationValidator getContextInformationValidator() { // called when proposal is selected and about to be inserted
		// TODO Auto-generated method stub
		return null; // null means, that processor is unable to compute context information
	}

	public String getErrorMessage() {
		return null;
//		return "No proposals available";
	}

}
