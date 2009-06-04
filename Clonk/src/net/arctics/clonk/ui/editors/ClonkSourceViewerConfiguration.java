package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class ClonkSourceViewerConfiguration<EditorType extends ClonkTextEditor> extends TextSourceViewerConfiguration {
	private EditorType textEditor;
	private ColorManager colorManager;

	public ClonkSourceViewerConfiguration(ColorManager colorManager, EditorType textEditor) {
		this.textEditor = textEditor;
		this.colorManager = colorManager;
	}
	
	public ColorManager getColorManager() {
		return colorManager;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return ClonkPartitionScanner.C4S_PARTITIONS;
	}

	public EditorType getEditor() {
		return textEditor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectors(org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) { 
		return new IHyperlinkDetector[] {
				new URLHyperlinkDetector()
		};
	}

}
