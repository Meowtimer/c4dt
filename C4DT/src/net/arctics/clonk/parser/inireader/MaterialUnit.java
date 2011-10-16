package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;

public class MaterialUnit extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	@Override
	public String configurationName() {
		return "Material.txt"; //$NON-NLS-1$
	}
	
	public MaterialUnit(Object input) {
		super(input);
	}
}
