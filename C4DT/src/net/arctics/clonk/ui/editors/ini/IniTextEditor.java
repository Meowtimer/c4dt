package net.arctics.clonk.ui.editors.ini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.TextChangeListenerBase;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class IniTextEditor extends ClonkTextEditor {
	
	public static final class TextChangeListener extends TextChangeListenerBase<IniTextEditor, IniUnit> {
		
		private static final Map<IDocument, TextChangeListenerBase<IniTextEditor, IniUnit>> listeners = new HashMap<IDocument, TextChangeListenerBase<IniTextEditor, IniUnit>>();

		private boolean unitParsed;
		public int unitLocked;
		
		private final Timer reparseTimer = new Timer("Reparse Timer");
		private TimerTask reparseTask;
		
		public TextChangeListener() {
			super();
		}
		
		@Override
		public void documentChanged(DocumentEvent event) {
			super.documentChanged(event);
			forgetUnitParsed();
			reparseTask = cancel(reparseTask);
			reparseTimer.schedule(reparseTask = new TimerTask() {
				@Override
				public void run() {
					boolean foundClient = false;
					for (final IniTextEditor ed : clients) {
						if (!foundClient) {
							foundClient = true;
							ensureIniUnitUpToDate(ed);
						}
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								ed.updateFoldingStructure();								
							}
						});
					}
				}
			}, 700);
		}
		public static TextChangeListener addTo(IDocument document, IniUnit unit, IniTextEditor client)  {
			try {
				return addTo(listeners, TextChangeListener.class, document, unit, client);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		public void forgetUnitParsed() {
			if (unitLocked == 0)
				unitParsed = false;
		}
		public boolean ensureIniUnitUpToDate(IniTextEditor editor) {
			if (!unitParsed) {
				unitParsed = true;
				String newDocumentString = editor != null ? editor.getSourceViewer().getDocument().get() : document.get();
				structure.getScanner().reset(newDocumentString);
				structure.parse(false, false);
			}
			return true;
		}
	}
	
	public IniTextEditor() {
		super();
		setSourceViewerConfiguration(new IniSourceViewerConfiguration(getPreferenceStore(), new ColorManager(), this));
	}
	
	@Override
	public void refreshOutline() {
		textChangeListener.forgetUnitParsed();
		if (outlinePage != null)
			outlinePage.setInput(getIniUnit());
	}
	
	@Override
	public C4Declaration getTopLevelDeclaration() {
		return getIniUnit(); 
	}

	public IniUnit getIniUnit() {
		if (textChangeListener == null) {
			IniUnit unit = null;
			try {
				unit = (IniUnit) C4Structure.pinned(Utilities.getEditingFile(this), true, false);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (unit != null && unit.isEditable()) {
				textChangeListener = TextChangeListener.addTo(getDocumentProvider().getDocument(getEditorInput()), unit, this);
			}
		}
		if (textChangeListener != null) {
			textChangeListener.ensureIniUnitUpToDate(null);
			return textChangeListener.getStructure();
		} else {
			return null;
		}
	}
	
	public void lockUnit() {
		textChangeListener.unitLocked++;
	}
	
	public void unlockUnit() {
		textChangeListener.unitLocked--;
	}
	
	private TextChangeListener textChangeListener;
	
	// projection support
	private ProjectionSupport projectionSupport;
	private ProjectionAnnotationModel projectionAnnotationModel;
	private Annotation[] oldAnnotations;
	
	private void collectAnnotationPositions(IniItem item, List<Position> positions) {
		if (item.getChildCollection() != null) {
			for (INode i : item.getChildCollection()) {
				if (i instanceof IniItem) { 
					if (i instanceof IniSection) {
						IniSection sec = (IniSection) i;
						positions.add(new Position(sec.getLocation().getOffset(), sec.getSectionEnd()-sec.getLocation().getOffset()));
					}
					collectAnnotationPositions((IniItem) i, positions);
				}
			}
		}
	}
	
	public void updateFoldingStructure() {
		List<Position> positions = new ArrayList<Position>(20);
		collectAnnotationPositions(getIniUnit(), positions);
		Annotation[] annotations = new Annotation[positions.size()];
		
		// this will hold the new annotations along with their corresponding positions
		HashMap<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
		
		for(int i =0;i<positions.size();i++) {
			ProjectionAnnotation annotation = new ProjectionAnnotation();
			newAnnotations.put(annotation,positions.get(i));
			annotations[i] = annotation;
		}
		projectionAnnotationModel.modifyAnnotations(oldAnnotations, newAnnotations, null);
		oldAnnotations = annotations;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
		projectionSupport = new ProjectionSupport(projectionViewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.install();
		projectionViewer.doOperation(ProjectionViewer.TOGGLE);
		projectionAnnotationModel = projectionViewer.getProjectionAnnotationModel();
		
		getIniUnit();
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
		ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}
	
}