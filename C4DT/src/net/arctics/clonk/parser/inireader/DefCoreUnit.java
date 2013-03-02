package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.resource.ClonkBuilder;

public class DefCoreUnit extends IniUnit {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	protected String configurationName() { return "DefCore.txt"; } //$NON-NLS-1$
	public DefCoreUnit(Object input) { super(input); }
	@Override
	public boolean requiresScriptReparse() { return true; /* i guess */ }

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
	public void commitTo(Script script, ClonkBuilder builder) {
		super.commitTo(script, builder);
		if (script instanceof Definition)
			((Definition)script).setDefCoreFile(iniFile);
	}

}
