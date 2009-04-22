/**
 * 
 */
package net.arctics.clonk.ui.editors.ini;

import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.c4script.ColorManager;

import org.eclipse.core.runtime.IProgressMonitor;

public class IniTextEditor extends ClonkTextEditor {
	
	public static final String PAGE_ID = "rawIniEditor";
	
	private String title;
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
	}
	
	public IniTextEditor() {
		super();
		setSourceViewerConfiguration(new IniSourceViewerConfiguration(new ColorManager(), this));
	}

	public void resetPartName() {
		setPartName(title);
	}
}