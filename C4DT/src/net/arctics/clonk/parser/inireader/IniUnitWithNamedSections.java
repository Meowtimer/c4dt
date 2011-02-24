package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.stringtbl.StringTbl;

public abstract class IniUnitWithNamedSections extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	public IniUnitWithNamedSections(Object input) {
		super(input);
	}
	
	public String sectionNameEntryName(IniSection section) {
		return "Name"; //$NON-NLS-1$
	}

	@Override
	public String sectionToString(IniSection section) {
		IniItem nameEntry = section.getSubItem(sectionNameEntryName(section));
		if (nameEntry instanceof IniEntry) {
			String val = ((IniEntry) nameEntry).getValue();
			val = StringTbl.evaluateEntries(this, val, 0);
			return "["+val+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.sectionToString(section);
	}
	
}
