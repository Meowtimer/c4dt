package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ID;
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

	public ID getObjectID() {
		IniEntry entry = entryInSection("DefCore", "id"); //$NON-NLS-1$ //$NON-NLS-2$
		if (entry instanceof ComplexIniEntry)
			return (ID)((ComplexIniEntry)entry).getExtendedValue();
		return ID.NULL;
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
	
	@Override
	public boolean requiresScriptReparse() {
		return true; // i guess
	}
	
}
