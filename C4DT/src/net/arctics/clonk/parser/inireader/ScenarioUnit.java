package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniSectionData;

public class ScenarioUnit extends IniUnit {

	private static final long serialVersionUID = 1L;
	
	private final static IniConfiguration configuration = ClonkCore.getDefault().iniConfigurations.getConfigurationFor("Scenario.txt"); //$NON-NLS-1$
	
	public ScenarioUnit(IFile file) {
		super(file);
	}
	
	public ScenarioUnit(InputStream stream) {
		super(stream);
	}
	
	public ScenarioUnit(String text) {
		super(text);
	}
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}
	
	@Override
	protected IniSectionData getSectionDataFor(IniSection section) {
		if (section.getName().startsWith("Player")) //$NON-NLS-1$
			return configuration.getSections().get("Player"); //$NON-NLS-1$
		return super.getSectionDataFor(section);
	}
	
	@Override
	protected boolean isSectionNameValid(String name) {
		return name.matches("Player[1234]") || super.isSectionNameValid(name); //$NON-NLS-1$
	}

}
