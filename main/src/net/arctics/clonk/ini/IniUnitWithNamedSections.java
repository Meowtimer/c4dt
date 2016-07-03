package net.arctics.clonk.ini;

import net.arctics.clonk.Core;

public abstract class IniUnitWithNamedSections extends IniUnit {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	public String nameEntryName(IniSection section) {
		return "Name"; //$NON-NLS-1$
	}
	public IniUnitWithNamedSections(final Object input) { super(input); }
}
