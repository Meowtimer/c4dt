package net.arctics.clonk.ui.editors.ini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ini.IniItem;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class IniTextEditor extends StructureTextEditor {

	private IniUnitEditingState state;

	static final FileDocumentProvider DOCUMENT_PROVIDER = new FileDocumentProvider() {
		final String C4INI_COMMENT = "__c4ini_comment"; //$NON-NLS-1$
		final String[] C4INI_PARTITIONS = {C4INI_COMMENT, IDocument.DEFAULT_CONTENT_TYPE};
		class IniPartitionScanner extends RuleBasedPartitionScanner {
			public IniPartitionScanner() {
				final IToken singleLineComment = new Token(C4INI_COMMENT);
				setPredicateRules(new IPredicateRule[] {
					new EndOfLineRule(";", singleLineComment), //$NON-NLS-1$
					new EndOfLineRule("#", singleLineComment) //$NON-NLS-1$
				});
			}
		}
		@Override
		protected IDocument createDocument(Object element) throws CoreException {
			final IDocument document = super.createDocument(element);
			if (document != null) {
				final IDocumentPartitioner partitioner = new FastPartitioner(new IniPartitionScanner(), C4INI_PARTITIONS);
				partitioner.connect(document);
				document.setDocumentPartitioner(partitioner);
			}
			return document;
		}
	};

	public IniTextEditor() { setDocumentProvider(DOCUMENT_PROVIDER); }

	@Override
	public void refreshOutline() {
		state.forgetUnitParsed();
		if (outlinePage != null)
			outlinePage.setInput(unit());
	}

	@Override
	public Declaration structure() {
		return unit();
	}

	public IniUnit unit() {
		final IniUnitEditingState state = state();
		if (state != null) {
			state.ensureIniUnitUpToDate();
			return state.structure();
		}
		return null;
	}

	private void collectAnnotationPositions(IniItem item, List<Position> positions) {
		if (item.childCollection() != null)
			for (final INode i : item.childCollection())
				if (i instanceof IniItem) {
					if (i instanceof IniSection) {
						final IniSection sec = (IniSection) i;
						positions.add(new Position(sec.absolute().getOffset(), sec.sectionEnd()-sec.start()));
					}
					collectAnnotationPositions((IniItem) i, positions);
				}
	}

	public void updateFoldingStructure() {
		final List<Position> positions = new ArrayList<Position>(20);
		collectAnnotationPositions(unit(), positions);
		final Annotation[] annotations = new Annotation[positions.size()];
		final HashMap<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
		for (int i = 0; i < positions.size(); i++) {
			final ProjectionAnnotation annotation = new ProjectionAnnotation();
			newAnnotations.put(annotation, positions.get(i));
			annotations[i] = annotation;
		}
		projectionAnnotationModel.modifyAnnotations(oldAnnotations, newAnnotations, null);
		oldAnnotations = annotations;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		unit();
		updateFoldingStructure();
	}

	public boolean ensureIniUnitUpToDate() {
		return state.ensureIniUnitUpToDate();
	}

	public void forgetUnitParsed() {
		state.forgetUnitParsed();
	}

	@Override
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		final ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	@Override
	protected IniUnitEditingState state() {
		final IniUnit unit = (IniUnit) Structure.pinned(Utilities.fileEditedBy(this), true, false);
		if (state == null && unit != null && unit.isEditable()) {
			state = StructureEditingState.request(IniUnitEditingState.class, getDocumentProvider().getDocument(getEditorInput()), unit, this);
			setSourceViewerConfiguration(state);
		}
		return state;
	}

	@Override
	protected void editorSaved() {
		ensureIniUnitUpToDate();
		super.editorSaved();
	}

}