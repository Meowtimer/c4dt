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

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;

public class IniTextEditor extends StructureTextEditor {

	private IniUnitEditingState state;

	public IniTextEditor() { }

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

	private void collectAnnotationPositions(final IniItem item, final List<Position> positions) {
		if (item.childCollection() != null)
			for (final INode i : item.childCollection())
				if (i instanceof IniItem) {
					if (i instanceof IniSection) {
						final IniSection sec = (IniSection) i;
						positions.add(new Position(sec.absolute().getOffset(), sec.end()-sec.start()));
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
	public void createPartControl(final Composite parent) {
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
	protected ISourceViewer createSourceViewer(final Composite parent, final IVerticalRuler ruler, final int styles) {
		final ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	@Override
	public IniUnitEditingState state() {
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