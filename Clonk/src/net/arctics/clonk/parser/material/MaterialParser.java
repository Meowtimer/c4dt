package net.arctics.clonk.parser.material;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniReader;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class MaterialParser extends IniReader {
	
	private final IniConfiguration configuration = ClonkCore.getDefault().INI_CONFIGURATIONS.getConfigurationFor("Material.txt");
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}
	
	public MaterialParser(IFile file) {
		super(file);
	}
	
	public MaterialParser(InputStream stream) {
		super(stream);
	}
	
	public MaterialParser(String text) {
		super(text);
	}
}
