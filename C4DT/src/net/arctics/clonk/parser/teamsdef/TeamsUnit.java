package net.arctics.clonk.parser.teamsdef;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniUnitWithNamedSections;

public class TeamsUnit extends IniUnitWithNamedSections {

	public TeamsUnit(Object input) {
		super(input);
	}
	
	@Override
	protected String getConfigurationName() {
		return "Teams.txt";
	}

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

}
