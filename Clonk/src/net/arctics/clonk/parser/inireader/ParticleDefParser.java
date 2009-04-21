package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class ParticleDefParser extends IniReader {

	public ParticleDefParser(IFile file) {
		super(file);
	}
	
	public ParticleDefParser(InputStream stream) {
		super(stream);
	}
	
	public ParticleDefParser(String text) {
		super(text);
	}
	
	private final IniConfiguration configuration = ClonkCore.getDefault().INI_CONFIGURATIONS.getConfigurationFor("Particle.txt");
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}

}
