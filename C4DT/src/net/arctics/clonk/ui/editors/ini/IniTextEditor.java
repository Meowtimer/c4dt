package net.arctics.clonk.ui.editors.ini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.arctics.clonk.ini.IniItem;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;

public class IniTextEditor extends ClonkTextEditor {

	public IniTextEditor() {
		super();
		setSourceViewerConfiguration(new IniSourceViewerConfiguration(getPreferenceStore(), new ColorManager(), this));
	}

	@Override
	public void refreshOutline() {
		textChangeListener.forgetUnitParsed();
		if (outlinePage != null)
			outlinePage.setInput(unit());
	}

	@Override
	public Declaration structure() {
		return unit();
	}

	public IniUnit unit() {
		IniUnit unit = null;
		unit = (IniUnit) Structure.pinned(Utilities.fileEditedBy(this), true, false);
		if (textChangeListener == null && unit != null && unit.isEditable())
			textChangeListener = IniUnitEditingState.addTo(getDocumentProvider().getDocument(getEditorInput()), unit, this);
		else if (textChangeListener != null)
			textChangeListener.ensureIniUnitUpToDate(this);
		return unit;
	}

	public void lockUnit() {
		textChangeListener.unitLocked++;
	}

	public void unlockUnit() {
		textChangeListener.unitLocked--;
	}

	private IniUnitEditingState textChangeListener;

	private void collectAnnotationPositions(IniItem item, List<Position> positions) {
		if (item.childCollection() != null)
			for (final INode i : item.childCollection())
				if (i instanceof IniItem) {
					if (i instanceof IniSection) {
						final IniSection sec = (IniSection) i;
						positions.add(new Position(sec.start(), sec.sectionEnd()-sec.start()));
					}
					collectAnnotationPositions((IniItem) i, positions);
				}
	}

	public void updateFoldingStructure() {
		final List<Position> positions = new ArrayList<Position>(20);
		collectAnnotationPositions(unit(), positions);
		final Annotation[] annotations = new Annotation[positions.size()];

		// this will hold the new annotations along with their corresponding positions
		final HashMap<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();

		for(int i =0;i<positions.size();i++) {
			final ProjectionAnnotation annotation = new ProjectionAnnotation();
			newAnnotations.put(annotation,positions.get(i));
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
		return textChangeListener.ensureIniUnitUpToDate(this);
	}

	public void forgetUnitParsed() {
		textChangeListener.forgetUnitParsed();
	}

	@Override
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		final ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	@Override
	protected StructureEditingState<?, ?> editingState() {
		return textChangeListener;
	}

	@Override
	protected void editorSaved() {
		super.editorSaved();
	}

}