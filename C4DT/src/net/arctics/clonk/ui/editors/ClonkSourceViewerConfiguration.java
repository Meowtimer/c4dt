package net.arctics.clonk.ui.editors;


import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class ClonkSourceViewerConfiguration<EditorType extends ClonkTextEditor> extends TextSourceViewerConfiguration {
	private EditorType textEditor;
	private ColorManager colorManager;
	protected ITextHover hover;

	public ClonkSourceViewerConfiguration(IPreferenceStore store, ColorManager colorManager, EditorType textEditor) {
		super(store);
		this.textEditor = textEditor;
		this.colorManager = colorManager;
	}
	
	public ColorManager getColorManager() {
		return colorManager;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return ClonkPartitionScanner.C4S_PARTITIONS;
	}

	public EditorType editor() {
		return textEditor;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) { 
		return new IHyperlinkDetector[] {
			new URLHyperlinkDetector()
		};
	}
	
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if (hover == null) {
			hover = new ClonkTextHover<EditorType>(this); 
		}
		return hover;
	}
	
	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		// pff, spell checking comments, that's ridiculous
		return null;
		/*ClonkReconcilerStrategy strategy = new ClonkReconcilerStrategy(sourceViewer, EditorsUI.getSpellingService());
		Reconciler reconciler = new Reconciler();
		reconciler.setReconcilingStrategy(strategy, ClonkPartitionScanner.C4S_COMMENT);
		reconciler.setDocumentPartitioning(ClonkPartitionScanner.C4S_COMMENT);
		return reconciler;*/
	}
	
	public void refreshSyntaxColoring() {
		
	}

}
