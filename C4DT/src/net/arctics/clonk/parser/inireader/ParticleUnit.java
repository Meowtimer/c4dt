package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;

public class ParticleUnit extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public ParticleUnit(Object input) {
		super(input);
	}
	
	@Override
	protected String getConfigurationName() {
		return "Particle.txt"; //$NON-NLS-1$
	}
	
	@Override
	protected IniEntry validateEntry(IniEntry entry, IniSection section, boolean modifyMarkers) throws IniParserException {
		if (section.getName().equals("Particle") && entry.getName().equals("Name")) {
			setName(entry.getValue());
		}
		return super.validateEntry(entry, section, modifyMarkers);
	}

}
