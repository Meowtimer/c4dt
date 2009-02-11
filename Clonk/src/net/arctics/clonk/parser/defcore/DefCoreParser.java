package net.arctics.clonk.parser.defcore;

import java.io.InputStream;
import java.io.InvalidClassException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniParserException;
import net.arctics.clonk.parser.inireader.IniReader;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

public class DefCoreParser extends IniReader {
	
//	private final static String[] DEFCORE_SECTIONS = new String[] { "DefCore" , "Physical" };
//	
//	private final List<DefCoreOption> defCoreOptions = DefCoreOption.createNewDefCoreList();
//	private final List<DefCoreOption> physicalOptions = DefCoreOption.createNewPhysicalList();
	
	private final IniConfiguration configuration = ClonkCore.getDefault().INI_CONFIGURATIONS.getConfigurationFor("DefCore.txt");
	
	public DefCoreParser(InputStream stream) {
		super(stream);
	}
	
	public DefCoreParser(IFile file) {
		super(file);
	}

	@Override
	protected boolean isSectionNameValid(String name) {
		return configuration.hasSection(name);
//		for(String section : DEFCORE_SECTIONS) {
//			if (name.equalsIgnoreCase(section)) return true;
//		}
//		return false;
	}
	
	@Override
	protected IniEntry validateEntry(IniEntry entry, IniSection section)
			throws IniParserException {
		IniDataSection sectionConfig = configuration.getSections().get(section.getName());
		if (!sectionConfig.hasEntry(entry.getKey())) {
			throw new IniParserException(IMarker.SEVERITY_WARNING, "Unknown option '" + entry.getKey() + "'", entry.getStartPos(), entry.getKey().length() + entry.getStartPos());
		}
		IniDataEntry entryConfig = sectionConfig.getEntries().get(entry.getKey());
		try {
			try {
				Object value = configuration.getFactory().create(entryConfig.getEntryClass(), entry.getValue());
				return ComplexIniEntry.adaptFrom(entry, value, entryConfig);
			}
			catch(IniParserException e) { // add offsets and throw through
				// FIXME: whitespace before and after '=' is not taken into account
				if (e.getOffset() == 0 || e.getEndOffset() == 0) {
					e.setOffset(entry.getStartPos() + entry.getKey().length() + 1);
					e.setEndOffset(entry.getStartPos() + entry.getKey().length() + 1 + entry.getValue().length());
				}
				throw e;
			}
		} catch (InvalidClassException e) {
			throw new IniParserException(IMarker.SEVERITY_WARNING, "There is a bug in the ini scheme. Report the following data to a C4DT developer: " + e.getMessage(),entry.getStartPos(),entry.getStartPos() + entry.getKey().length());
		}
	}

//	/**
//	 * Searches the option for given name
//	 * @param name the name of the option (e.g. "BurnTo")
//	 * @return The found option or <tt>null</tt> if not found
//	 */
//	public DefCoreOption getDefCoreOption(String name) {
//		ListIterator<DefCoreOption> it = defCoreOptions.listIterator();
//		while(it.hasNext()) {
//			if (it.next().getName().equalsIgnoreCase(name)) return it.previous();
//		}
//		return null;
//	}
//	
//	/**
//	 * Searches the option for given name
//	 * @param name the name of the option (e.g. "Throw")
//	 * @return The found option or <tt>null</tt> if not found
//	 */
//	public DefCoreOption getPhysicalOption(String name) {
//		ListIterator<DefCoreOption> it = physicalOptions.listIterator();
//		while(it.hasNext()) {
//			if (it.next().getName().equalsIgnoreCase(name)) return it.previous();
//		}
//		return null;
//	}
//	

	public C4ID getObjectID() {
		ComplexIniEntry entry = (ComplexIniEntry) entries.get("id");
		return (C4ID) entry.getExtendedValue();
	}
	
	public String getName() {
		ComplexIniEntry entry = (ComplexIniEntry) entries.get("name");
		return entry != null ? (String) entry.getExtendedValue() : defaultName;
	}
	
}
