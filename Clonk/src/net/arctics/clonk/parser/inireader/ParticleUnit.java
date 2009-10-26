package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class ParticleUnit extends IniUnit {

	private static final long serialVersionUID = 1L;

	public ParticleUnit(IFile file) {
		super(file);
	}
	
	public ParticleUnit(InputStream stream) {
		super(stream);
	}
	
	public ParticleUnit(String text) {
		super(text);
	}
	
	private final IniConfiguration configuration = ClonkCore.getDefault().iniConfigurations.getConfigurationFor("Particle.txt"); //$NON-NLS-1$
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}

}
