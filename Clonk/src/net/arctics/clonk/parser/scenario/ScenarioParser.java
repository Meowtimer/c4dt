package net.arctics.clonk.parser.scenario;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniReader;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class ScenarioParser extends IniReader {

	private final static IniConfiguration configuration = ClonkCore.getDefault().INI_CONFIGURATIONS.getConfigurationFor("Scenario.txt");
	
	public ScenarioParser(IFile file) {
		super(file);
	}
	
	public ScenarioParser(InputStream stream) {
		super(stream);
	}
	
	public ScenarioParser(String text) {
		super(text);
	}
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}

}
