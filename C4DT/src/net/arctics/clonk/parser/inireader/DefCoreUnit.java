package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.c4script.ScriptBase;

public class DefCoreUnit extends IniUnit {
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	@Override
	protected String getConfigurationName() {
		return "DefCore.txt"; //$NON-NLS-1$
	}

	public DefCoreUnit(Object input) {
		super(input);
	}

	public C4ID getObjectID() {
		IniEntry entry = entryInSection("DefCore", "id"); //$NON-NLS-1$ //$NON-NLS-2$
		if (entry instanceof ComplexIniEntry)
			return (C4ID)((ComplexIniEntry)entry).getExtendedValue();
		return C4ID.NULL;
	}
	
	public String getName() {
		IniEntry entry = entryInSection("DefCore", "Name"); //$NON-NLS-1$ //$NON-NLS-2$
		return entry instanceof ComplexIniEntry ? (String)((ComplexIniEntry)entry).getExtendedValue() : defaultName;
	}
	
	@Override
	public void commitTo(ScriptBase script) {
		if (script instanceof Definition) {
			((Definition)script).setId(this.getObjectID());
		}
	}
	
}
