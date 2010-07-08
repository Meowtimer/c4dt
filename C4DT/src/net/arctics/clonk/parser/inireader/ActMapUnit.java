package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IFile;

public class ActMapUnit extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static final String ACTION_SECTION = "Action"; //$NON-NLS-1$
	public static final String FILE_NAME = "ActMap.txt"; //$NON-NLS-1$
	
	@Override
	protected String getConfigurationName() {
		return FILE_NAME;
	}
	
	public ActMapUnit(IFile file) {
		super(file);
	}
	
	public ActMapUnit(InputStream stream) {
		super(stream);
	}
	
	public ActMapUnit(String text) {
		super(text);
	}
	
	@Override
	public String sectionToString(IniSection section) {
		IniEntry nameEntry = section.getEntry("Name"); //$NON-NLS-1$
		if (nameEntry != null)
			return "["+nameEntry.getValue()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		return super.sectionToString(section);
	}

}
