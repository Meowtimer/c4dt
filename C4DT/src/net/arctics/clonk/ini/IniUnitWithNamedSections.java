package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.stringtbl.StringTbl;
import java.util.function.Predicate;

public abstract class IniUnitWithNamedSections extends IniUnit {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public IniUnitWithNamedSections(final Object input) {
		super(input);
	}

	public String nameOfEntryToTakeSectionNameFrom(final IniSection section) {
		return "Name"; //$NON-NLS-1$
	}

	@Override
	public String sectionToString(final IniSection section) {
		final IniItem nameEntry = section.itemByKey(nameOfEntryToTakeSectionNameFrom(section));
		if (nameEntry instanceof IniEntry) {
			String val = ((IniEntry) nameEntry).stringValue();
			val = StringTbl.evaluateEntries(this, val, true).evaluated;
			return "["+val+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.sectionToString(section);
	}

	public Predicate<IniSection> nameMatcherPredicate(final String value) {
		return section -> {
			final IniItem entry = section.itemByKey(nameOfEntryToTakeSectionNameFrom(section));
			return (entry instanceof IniEntry && ((IniEntry)entry).stringValue().equals(value));
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(final T from) {
		if (from instanceof IniSection) {
			final IniSection section = (IniSection) from;
			final IniEntry entry = (IniEntry) section.itemByKey(nameOfEntryToTakeSectionNameFrom(section.parentSection()));
			if (entry != null)
				return (T) sectionMatching(nameMatcherPredicate(entry.stringValue()));
			else
				return null;
		} else
			return null;
	};

}
