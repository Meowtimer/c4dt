package net.arctics.clonk.parser.inireader;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;

public class ScenarioUnit extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	@Override
	protected String getConfigurationName() {
		return "Scenario.txt"; //$NON-NLS-1$
	}
	
	public ScenarioUnit(IFile file) {
		super(file);
	}
	
	public ScenarioUnit(InputStream stream) {
		super(stream);
	}
	
	public ScenarioUnit(String text) {
		super(text);
	}
	
	@Override
	protected IniDataSection getSectionDataFor(IniSection section) {
		if (section.getName().startsWith("Player")) //$NON-NLS-1$
			return getConfiguration().getSections().get("Player"); //$NON-NLS-1$
		return super.getSectionDataFor(section);
	}
	
	@Override
	protected boolean isSectionNameValid(String name) {
		return name.matches("Player[1234]") || super.isSectionNameValid(name); //$NON-NLS-1$
	}

}
