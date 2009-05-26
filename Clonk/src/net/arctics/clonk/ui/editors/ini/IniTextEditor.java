/**
 * 
 */
package net.arctics.clonk.ui.editors.ini;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.IProgressMonitor;

public class IniTextEditor extends ClonkTextEditor {
	
	private IniUnit unit;
	private boolean unitParsed;

	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
	}
	
	public IniTextEditor() {
		super();
		setSourceViewerConfiguration(new IniSourceViewerConfiguration(new ColorManager(), this));
	}
	
	@Override
	public void refreshOutline() {
		forgetUnitParsed();
		outlinePage.setInput(getIniUnit());
	}
	
	public boolean ensureIniUnitUpToDate() {
		if (!unitParsed) {
			unitParsed = true;
			try {
				unit = Utilities.createAdequateIniUnit(Utilities.getEditingFile(this), getDocumentProvider().getDocument(getEditorInput()).get());
				unit.parse();
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
		unitParsed = false;
	}

	public IniUnit getIniUnit() {
		ensureIniUnitUpToDate();
		return unit;
	}
	
}