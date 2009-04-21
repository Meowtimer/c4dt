package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class ActMapParser extends IniReader {

	public static final String ACTION_SECTION = "Action";
	
	private final IniConfiguration configuration = ClonkCore.getDefault().INI_CONFIGURATIONS.getConfigurationFor("ActMap.txt");
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}
	
	public ActMapParser(IFile file) {
		super(file);
	}
	
	public ActMapParser(InputStream stream) {
		super(stream);
	}
	
	public ActMapParser(String text) {
		super(text);
	}

}
