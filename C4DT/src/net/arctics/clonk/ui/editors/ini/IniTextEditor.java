package net.arctics.clonk.ui.editors.ini;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.TextChangeListenerBase;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Composite;

public class IniTextEditor extends ClonkTextEditor {
	
	public static final class TextChangeListener extends TextChangeListenerBase<IniTextEditor, IniUnit> {
		
		private static final Map<IDocument, TextChangeListenerBase<IniTextEditor, IniUnit>> listeners = new HashMap<IDocument, TextChangeListenerBase<IniTextEditor, IniUnit>>();

		private boolean unitParsed;
		public int unitLocked;
		
		public TextChangeListener() {
			super();
		}
		
		@Override
		public void documentChanged(DocumentEvent event) {
			super.documentChanged(event);
			forgetUnitParsed();
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
		public boolean ensureIniUnitUpToDate() {
			if (!unitParsed) {
				unitParsed = true;
				structure.parse(false);
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
			textChangeListener.ensureIniUnitUpToDate();
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
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		getIniUnit();
	}

	public boolean ensureIniUnitUpToDate() {
		return textChangeListener.ensureIniUnitUpToDate();
	}

	public void forgetUnitParsed() {
		textChangeListener.forgetUnitParsed();
	}
	
}