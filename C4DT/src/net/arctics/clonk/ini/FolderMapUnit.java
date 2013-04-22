package net.arctics.clonk.ini;

import net.arctics.clonk.Core;

public class FolderMapUnit extends IniUnit {

	@Override
	protected String configurationName() {
		return "FolderMap.txt";
	}
	
	public FolderMapUnit(Object input) {
		super(input);
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

}
