package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IFile;

public class MaterialUnit extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	@Override
	public String getConfigurationName() {
		return "Material.txt"; //$NON-NLS-1$
	}
	
	public MaterialUnit(IFile file) {
		super(file);
	}
	
	public MaterialUnit(InputStream stream) {
		super(stream);
	}
	
	public MaterialUnit(String text) {
		super(text);
	}
}
