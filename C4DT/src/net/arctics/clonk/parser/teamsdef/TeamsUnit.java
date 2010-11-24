package net.arctics.clonk.parser.teamsdef;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniUnitWithNamedSections;

public class TeamsUnit extends IniUnitWithNamedSections {

	public TeamsUnit(IFile file) {
		super(file);
	}
	public TeamsUnit(String text) {
		super(text);
	}
	public TeamsUnit(InputStream stream) {
		super(stream);
	}
	
	@Override
	protected String getConfigurationName() {
		return "Teams.txt";
	}

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

}
