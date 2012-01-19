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
	
	public String nameOfEntryToTakeSectionNameFrom(IniSection section) {
		return "Name"; //$NON-NLS-1$
	}

	@Override
	public String sectionToString(IniSection section) {
		IniItem nameEntry = section.subItemByKey(nameOfEntryToTakeSectionNameFrom(section));
		if (nameEntry instanceof IniEntry) {
			String val = ((IniEntry) nameEntry).stringValue();
			val = StringTbl.evaluateEntries(this, val, true).evaluated;
			return "["+val+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.sectionToString(section);
	}
	
	public IPredicate<IniSection> nameMatcherPredicate(final String value) {
		return new IPredicate<IniSection>() {
			@Override
			public boolean test(IniSection section) {
				IniItem entry = section.subItemByKey(nameOfEntryToTakeSectionNameFrom(section)); 
				return (entry instanceof IniEntry && ((IniEntry)entry).stringValue().equals(value));
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		if (from instanceof IniSection) {
			IniSection section = (IniSection) from;
			IniEntry entry = (IniEntry) section.subItemByKey(nameOfEntryToTakeSectionNameFrom(section.parentSection()));
			if (entry != null)
				return (T) sectionMatching(nameMatcherPredicate(entry.stringValue()));
			else
				return null;
		} else
			return null;
	};
	
}
