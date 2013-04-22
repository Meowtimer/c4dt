package net.arctics.clonk.ini;

import net.arctics.clonk.Core;

public class MaterialUnit extends IniUnit {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	@Override
	public String configurationName() {
		return "Material.txt"; //$NON-NLS-1$
	}
	
	public MaterialUnit(Object input) {
		super(input);
	}
}
