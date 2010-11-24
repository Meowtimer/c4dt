package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.stringtbl.StringTbl;

import org.eclipse.core.resources.IFile;

public abstract class IniUnitWithNamedSections extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	public IniUnitWithNamedSections(IFile file) {
		super(file);
	}
	
	public IniUnitWithNamedSections(InputStream stream) {
		super(stream);
	}
	
	public IniUnitWithNamedSections(String text) {
		super(text);
	}

	@Override
	public String sectionToString(IniSection section) {
		IniItem nameEntry = section.getSubItem("Name"); //$NON-NLS-1$
		if (nameEntry instanceof IniEntry) {
			String val = ((IniEntry) nameEntry).getValue();
			val = StringTbl.evaluateEntries(this, val, 0);
			return "["+val+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.sectionToString(section);
	}
	
}
