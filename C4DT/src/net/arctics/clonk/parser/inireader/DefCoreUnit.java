package net.arctics.clonk.parser.inireader;

import java.io.InputStream;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

import org.eclipse.core.resources.IFile;

public class DefCoreUnit extends IniUnit {
	
//	private final static String[] DEFCORE_SECTIONS = new String[] { "DefCore" , "Physical" };
//	
//	private final List<DefCoreOption> defCoreOptions = DefCoreOption.createNewDefCoreList();
//	private final List<DefCoreOption> physicalOptions = DefCoreOption.createNewPhysicalList();
	
	private static final long serialVersionUID = 1L;
	
	private final IniConfiguration configuration = ClonkCore.getDefault().iniConfigurations.getConfigurationFor("DefCore.txt"); //$NON-NLS-1$
	
	public DefCoreUnit(InputStream stream) {
		super(stream);
	}
	
	public DefCoreUnit(IFile file) {
		super(file);
	}
	
	public DefCoreUnit(String text) {
		super(text);
	}
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
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
	public void commitTo(C4ScriptBase script) {
		if (script instanceof C4Object)
			((C4Object)script).setId(this.getObjectID());
	}
	
}
