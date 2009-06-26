package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class MaterialUnit extends IniUnit {

	private static final long serialVersionUID = 1L;
	
	private final IniConfiguration configuration = ClonkCore.getDefault().iniConfigurations.getConfigurationFor("Material.txt");
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}
	
	public MaterialUnit(IFile file) {
		super(file);
	}
	
	public MaterialUnit(InputStream stream) {
		super(stream);
	}
	
	public MaterialUnit(String text) {
		super(text);
	}
}
