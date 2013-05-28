package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.util.IPredicate;

public abstract class IniUnitWithNamedSections extends IniUnit {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public IniUnitWithNamedSections(Object input) {
		super(input);
	}
	
	public String nameOfEntryToTakeSectionNameFrom(IniSection section) {
		return "Name"; //$NON-NLS-1$
	}

	@Override
	public String sectionToString(IniSection section) {
		IniItem nameEntry = section.itemByKey(nameOfEntryToTakeSectionNameFrom(section));
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
				IniItem entry = section.itemByKey(nameOfEntryToTakeSectionNameFrom(section)); 
				return (entry instanceof IniEntry && ((IniEntry)entry).stringValue().equals(value));
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		if (from instanceof IniSection) {
			IniSection section = (IniSection) from;
			IniEntry entry = (IniEntry) section.itemByKey(nameOfEntryToTakeSectionNameFrom(section.parentSection()));
			if (entry != null)
				return (T) sectionMatching(nameMatcherPredicate(entry.stringValue()));
			else
				return null;
		} else
			return null;
	};
	
}
