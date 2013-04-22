package net.arctics.clonk.ini;

import net.arctics.clonk.Core;

public class ParticleUnit extends IniUnit {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public ParticleUnit(Object input) {
		super(input);
	}
	
	@Override
	protected String configurationName() {
		return "Particle.txt"; //$NON-NLS-1$
	}
	
	@Override
	protected IniEntry validateEntry(IniEntry entry, IniSection section, boolean modifyMarkers) throws IniParserException {
		if (section.name().equals("Particle") && entry.name().equals("Name")) {
			setName(entry.stringValue());
		}
		return super.validateEntry(entry, section, modifyMarkers);
	}

}
