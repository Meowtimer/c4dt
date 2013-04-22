package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.ini.IniData.IniSectionDefinition;

public class ScenarioUnit extends IniUnit {

	public static final String FILENAME = "Scenario.txt"; //$NON-NLS-1$
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	@Override
	public Scenario scenario() {
		return iniFile != null ? Scenario.get(iniFile.getParent()) : null;
	}
	
	@Override
	protected String configurationName() {
		return FILENAME;
	}
	
	public ScenarioUnit(Object input) {
		super(input);
	}
	
	@Override
	protected IniSectionDefinition sectionDataFor(IniSection section, IniSection parentSection) {
		if (section.name().startsWith("Player")) //$NON-NLS-1$
			return configuration().sections().get("Player"); //$NON-NLS-1$
		return super.sectionDataFor(section, parentSection);
	}
	
	@Override
	protected boolean isSectionNameValid(String name, IniSection parentSection) {
		return (parentSection == null && name.matches("Player[1234]")) || super.isSectionNameValid(name, parentSection); //$NON-NLS-1$
	}

}
