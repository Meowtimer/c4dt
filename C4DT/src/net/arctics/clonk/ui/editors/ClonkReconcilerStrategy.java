package net.arctics.clonk.ui.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;

// FIXME totally unneeded and does not work and is copypasta'd from http://blog.ankursharma.org/2009/08/adding-spellchecking-to-custom-editors_19.html
public class ClonkReconcilerStrategy extends SpellingReconcileStrategy {

	public ClonkReconcilerStrategy(ISourceViewer sourceViewer, SpellingService spellingService) {
		super(sourceViewer, spellingService);
	}
	
	@SuppressWarnings("unchecked")
	public void reconcile(IRegion region) {

		AbstractDocument document = (AbstractDocument) getDocument();
		IDocumentPartitioner docPartitioner = document.getDocumentPartitioner(ClonkPartitionScanner.C4S_COMMENT);

		IAnnotationModel model = getAnnotationModel();
		if (region.getOffset() == 0 && region.getLength() == document.getLength()) {
			//reconciling whole document
			super.reconcile(region);
			deleteUnwantedAnnotations();
		} else {
			//partial reconciliation
			//preserve spelling annotations first
			Iterator<Annotation> iter = model.getAnnotationIterator();
			Map<Annotation, Position> spellingErrors = new HashMap<Annotation, Position>(1);
			while (iter.hasNext()) {
				Annotation annotation = iter.next();
				if (annotation instanceof SpellingAnnotation) {
					SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
					Position position = model.getPosition(spellingAnnotation);
					String contentType = docPartitioner.getContentType(position.getOffset());

					if (ClonkPartitionScanner.C4S_COMMENT.equalsIgnoreCase(contentType) ||ClonkPartitionScanner.C4S_MULTI_LINE_COMMENT.equalsIgnoreCase(contentType)) {
						spellingErrors.put(spellingAnnotation, model.getPosition(annotation));
					}
				}
			}

			//reconcile
			super.reconcile(region);

			//restore annotations
			model = getAnnotationModel();
			iter = spellingErrors.keySet().iterator();
			while (iter.hasNext()) {
				Annotation annotation = iter.next();
				model.addAnnotation(annotation, spellingErrors.get(annotation));
			}
			deleteUnwantedAnnotations();
		}

	}

	/**
	 * Deletes the spelling annotations marked for XML Tags
	 */
	@SuppressWarnings("unchecked")
	private void deleteUnwantedAnnotations() {
		AbstractDocument document = (AbstractDocument) getDocument();
		IDocumentPartitioner docPartitioner = document.getDocumentPartitioner(ClonkPartitionScanner.C4S_COMMENT);
		IAnnotationModel model = getAnnotationModel();
		Iterator<Annotation> iter = model.getAnnotationIterator();

		while (iter.hasNext()) {
			Annotation annotation = iter.next();
			if (annotation instanceof SpellingAnnotation) {
				SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
				Position position = model.getPosition(spellingAnnotation);
				String contentType = docPartitioner.getContentType(position.getOffset());
				if (!(ClonkPartitionScanner.C4S_COMMENT.equalsIgnoreCase(contentType) || ClonkPartitionScanner.C4S_MULTI_LINE_COMMENT.equalsIgnoreCase(contentType))) {
					model.removeAnnotation(spellingAnnotation);
				}
			}
		}
	}
}

