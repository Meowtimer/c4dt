package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class ActMapUnit extends IniUnit {

	private static final long serialVersionUID = 1L;

	public static final String ACTION_SECTION = "Action";
	
	private final IniConfiguration configuration = ClonkCore.getDefault().INI_CONFIGURATIONS.getConfigurationFor("ActMap.txt");
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}
	
	public ActMapUnit(IFile file) {
		super(file);
	}
	
	public ActMapUnit(InputStream stream) {
		super(stream);
	}
	
	public ActMapUnit(String text) {
		super(text);
	}

}
