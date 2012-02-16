package net.arctics.clonk.parser.teamsdef;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.inireader.IniUnitWithNamedSections;

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
