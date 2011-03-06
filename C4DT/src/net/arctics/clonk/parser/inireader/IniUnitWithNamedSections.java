package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.util.IPredicate;

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
	
	public IPredicate<IniSection> nameMatcherPredicate(final String value) {
		return new IPredicate<IniSection>() {
			@Override
			public boolean test(IniSection section) {
				IniItem entry = section.getSubItem(sectionNameEntryName(section)); //$NON-NLS-1$
				System.out.println(entry);
				return (entry instanceof IniEntry && ((IniEntry)entry).getValue().equals(value));
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T getLatestVersion(T from) {
		if (from instanceof IniSection) {
			IniSection section = (IniSection) from;
			IniEntry entry = (IniEntry) section.getSubItem(sectionNameEntryName(section.getParentSection()));
			if (entry != null)
				return (T) sectionMatching(nameMatcherPredicate(entry.getValue()));
			else
				return null;
		} else
			return null;
	};
	
}
