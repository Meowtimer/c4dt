package net.arctics.clonk.ui.editors.defcore;

import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.IClonkColorConstants;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;

public class DefCoreSourceViewerConfiguration extends
		TextSourceViewerConfiguration {
	private ColorManager colorManager;
	private ITextEditor textEditor;
	private DefCoreScanner scanner;
	
	public DefCoreSourceViewerConfiguration(ColorManager colorManager, ITextEditor textEditor) {
		this.colorManager = colorManager;
		this.textEditor = textEditor;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(
			ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getDefCoreScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		return reconciler;
	}
	
	protected DefCoreScanner getDefCoreScanner() {
		if (scanner == null) {
			scanner = new DefCoreScanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IClonkColorConstants.DEFAULT))));
		}
		return scanner;
	}
	
	
}
