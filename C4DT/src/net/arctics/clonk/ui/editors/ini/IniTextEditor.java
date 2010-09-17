package net.arctics.clonk.ui.editors.ini;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Composite;

public class IniTextEditor extends ClonkTextEditor {
	
	private IniUnit unit;
	private boolean unitParsed;
	private int unitLocked;
	
	public IniTextEditor() {
		super();
		setSourceViewerConfiguration(new IniSourceViewerConfiguration(getPreferenceStore(), new ColorManager(), this));
	}
	
	@Override
	public void refreshOutline() {
		forgetUnitParsed();
		if (outlinePage != null)
			outlinePage.setInput(getIniUnit());
	}
	
	public boolean ensureIniUnitUpToDate() {
		if (!unitParsed) {
			unitParsed = true;
			try {
				unit = IniUnit.createAdequateIniUnit(Utilities.getEditingFile(this), true, getDocumentProvider().getDocument(getEditorInput()).get());
				unit.parse(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return unit != null;
	}
	
	@Override
	public C4Declaration getTopLevelDeclaration() {
		return getIniUnit(); 
	}

	public void forgetUnitParsed() {
		if (unitLocked == 0)
			unitParsed = false;
	}

	public IniUnit getIniUnit() {
		ensureIniUnitUpToDate();
		return unit;
	}
	
	public void lockUnit() {
		unitLocked++;
	}
	
	public void unlockUnit() {
		unitLocked--;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		getDocumentProvider().getDocument(getEditorInput()).addDocumentListener(new IDocumentListener() {

			public void documentAboutToBeChanged(DocumentEvent event) {
			}

			public void documentChanged(DocumentEvent event) {
				forgetUnitParsed();
			}
			
		});
	}
	
}