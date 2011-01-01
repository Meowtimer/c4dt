package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;

public class ScenarioUnit extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	@Override
	protected String getConfigurationName() {
		return "Scenario.txt"; //$NON-NLS-1$
	}
	
	public ScenarioUnit(Object input) {
		super(input);
	}
	
	public ScenarioUnit(InputStream stream) {
		super(stream);
	}
	
	public ScenarioUnit(String text) {
		super(text);
	}
	
	@Override
	protected IniDataSection getSectionDataFor(IniSection section, IniSection parentSection) {
		if (section.getName().startsWith("Player")) //$NON-NLS-1$
			return getConfiguration().getSections().get("Player"); //$NON-NLS-1$
		return super.getSectionDataFor(section, parentSection);
	}
	
	@Override
	protected boolean isSectionNameValid(String name, IniSection parentSection) {
		return (parentSection == null && name.matches("Player[1234]")) || super.isSectionNameValid(name, parentSection); //$NON-NLS-1$
	}

}
