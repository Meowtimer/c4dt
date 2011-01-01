package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;

public class ActMapUnit extends IniUnitWithNamedSections {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static final String ACTION_SECTION = "Action"; //$NON-NLS-1$
	public static final String FILE_NAME = "ActMap.txt"; //$NON-NLS-1$
	
	@Override
	protected String getConfigurationName() {
		return FILE_NAME;
	}
	
	public ActMapUnit(Object input) {
		super(input);
	}

}
