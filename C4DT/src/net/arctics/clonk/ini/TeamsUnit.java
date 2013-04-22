package net.arctics.clonk.ini;

import net.arctics.clonk.Core;

public class TeamsUnit extends IniUnitWithNamedSections {

	public TeamsUnit(Object input) {
		super(input);
	}
	
	@Override
	protected String configurationName() {
		return "Teams.txt";
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

}
