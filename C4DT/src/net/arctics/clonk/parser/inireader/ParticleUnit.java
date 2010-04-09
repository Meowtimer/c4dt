package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

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
	
	@Override
	protected String getConfigurationName() {
		return "Particle.txt"; //$NON-NLS-1$
	}

}
