package net.arctics.clonk.ui.editors;

import java.awt.font.TextHitInfo;

import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Object;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.Region;

public class ClonkSourceViewerConfiguration extends SourceViewerConfiguration {
	
	private class HyperlinkDetector implements IHyperlinkDetector {
		
		private ClonkSourceViewerConfiguration configuration;

		/**
		 * @param configuration
		 */
		public HyperlinkDetector(ClonkSourceViewerConfiguration configuration) {
			super();
			this.configuration = configuration;
		}

		public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region, boolean canShowMultipleHyperlinks) {
			IDocument doc = viewer.getDocument();
			IRegion lineInfo;
			String line;
			try {
				lineInfo = doc.getLineInformationOfOffset(region.getOffset());
				line = doc.get(lineInfo.getOffset(),lineInfo.getLength());
			} catch (BadLocationException e) {
				return null;
			}
			int localOffset = region.getOffset() - lineInfo.getOffset();
			int start,end;
			for (start = localOffset; start > 0 && Character.isJavaIdentifierPart(line.charAt(start-1)); start--);
			for (end = localOffset; end < line.length() && Character.isJavaIdentifierPart(line.charAt(end)); end++);
			String ident = line.substring(start, end);
			ITextEditor editor = this.configuration.getEditor();
			C4Object obj = Utilities.getObjectForEditor(editor);
			C4Field field = obj.findField(ident, new C4Object.FindFieldInfo(Utilities.getProject(editor).getIndexer()));
			if (field != null) {
				return new IHyperlink[] {
					new C4ScriptHyperlink(new Region(lineInfo.getOffset()+start,ident.length()),field)
				};
			} else {
				return null;
			}
		}
		
	}
	
	private class C4ScriptHyperlink implements IHyperlink {

		private IRegion region;
		private C4Field target;
		
		/**
		 * @param region
		 * @param target
		 */
		public C4ScriptHyperlink(IRegion region, C4Field target) {
			super();
			this.region = region;
			this.target = target;
		}

		public IRegion getHyperlinkRegion() {
			return region;
		}

		public String getHyperlinkText() {
			return target.getName();
		}

		public String getTypeLabel() {
			return "C4Script Hyperlink";
		}

		public void open() {
			// ???
		}
		
	}
	
	private ClonkDoubleClickStrategy doubleClickStrategy;
	private ClonkCodeScanner scanner;
	private ClonkCommentScanner commentScanner;
	private ColorManager colorManager;
	private ITextEditor textEditor;

	public ClonkSourceViewerConfiguration(ColorManager colorManager, ITextEditor textEditor) {
		this.colorManager = colorManager;
		this.textEditor = textEditor;
	}
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return ClonkPartitionScanner.C4S_PARTITIONS;
	}
	
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new ClonkDoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected ITextEditor getEditor() {
		return textEditor;
	}
	
	protected ClonkCodeScanner getClonkScanner() {
		if (scanner == null) {
			scanner = new ClonkCodeScanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IClonkColorConstants.DEFAULT))));
		}
		return scanner;
	}
	
	protected ClonkCommentScanner getClonkCommentScanner() {
		if (commentScanner == null) {
			commentScanner = new ClonkCommentScanner(colorManager);
			commentScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IClonkColorConstants.COMMENT))));
		}
		return commentScanner;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTabWidth(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {		
		return 2;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
//		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
//		assistant.setContentAssistProcessor(new CodeBodyCompletionProcessor(getEditor(),assistant), ClonkPartitionScanner.C4S_CODEBODY);
		assistant.setContentAssistProcessor(new ClonkCompletionProcessor(getEditor(),assistant), IDocument.DEFAULT_CONTENT_TYPE);
		assistant.install(sourceViewer);
		
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		
		assistant.setRepeatedInvocationMode(true);
		
		assistant.setStatusLineVisible(true);
		assistant.setStatusMessage("Standard proposals");
		
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		
		assistant.enableColoredLabels(true);
		
		assistant.setInformationControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
//				BrowserInformationControl control = new BrowserInformationControl(parent, "Arial", "Press 'Tab' from proposal table or click for focus");
				DefaultInformationControl def = new DefaultInformationControl(parent,"Press 'Tab' from proposal table or click for focus");
				return def;
			}
		});
		
		
		return assistant;
	}
	
	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		
		IQuickAssistAssistant assistant = new QuickAssistAssistant();
		assistant.setQuickAssistProcessor(new ClonkQuickAssistProcessor());
		return assistant;
	}
	
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(getClonkScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_CODEBODY);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_CODEBODY);
		
		dr = new DefaultDamagerRepairer(getClonkScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_STRING);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_STRING);
		
		dr = new DefaultDamagerRepairer(getClonkScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		dr = new DefaultDamagerRepairer(getClonkCommentScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_COMMENT);
		
		dr = new DefaultDamagerRepairer(getClonkCommentScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_MULTI_LINE_COMMENT);
		
//		NonRuleBasedDamagerRepairer ndr =
//			new NonRuleBasedDamagerRepairer(
//				new TextAttribute(
//					colorManager.getColor(IClonkColorConstants.COMMENT)));
//		
//		reconciler.setDamager(ndr, ClonkPartitionScanner.C4S_COMMENT);
//		reconciler.setRepairer(ndr, ClonkPartitionScanner.C4S_COMMENT);
		
		return reconciler;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectors(org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		// TODO Auto-generated method stub
		return new IHyperlinkDetector[] {
				new HyperlinkDetector(this)
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTextHover(org.eclipse.jface.text.source.ISourceViewer, java.lang.String, int)
	 */
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer,
			String contentType, int stateMask) {
		// TODO Auto-generated method stub
		return super.getTextHover(sourceViewer, contentType, stateMask);
	}

}