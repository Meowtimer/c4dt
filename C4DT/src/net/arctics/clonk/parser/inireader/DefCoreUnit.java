package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ID;

public class DefCoreUnit extends IniUnit {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	@Override
	protected String configurationName() {
		return "DefCore.txt"; //$NON-NLS-1$
	}

	public DefCoreUnit(Object input) {
		super(input);
	}

	public ID definitionID() {
		IniEntry entry = entryInSection("DefCore", "id"); //$NON-NLS-1$ //$NON-NLS-2$
		if (entry instanceof ComplexIniEntry)
			return (ID)((ComplexIniEntry)entry).extendedValue();
		else
			return ID.NULL;
	}
	
	@Override
	public String name() {
		IniEntry entry = entryInSection("DefCore", "Name"); //$NON-NLS-1$ //$NON-NLS-2$
		return entry instanceof ComplexIniEntry ? (String)((ComplexIniEntry)entry).extendedValue() : defaultName;
	}
	
	@Override
	public boolean requiresScriptReparse() {
		return true; // i guess
	}
	
}
