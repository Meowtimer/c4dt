package net.arctics.clonk.ini;

import net.arctics.clonk.Core;

public class ActMapUnit extends IniUnitWithNamedSections {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final String ACTION_SECTION = "Action"; //$NON-NLS-1$
	public static final String FILE_NAME = "ActMap.txt"; //$NON-NLS-1$
	
	@Override
	protected String configurationName() {
		return FILE_NAME;
	}
	
	public ActMapUnit(final Object input) {
		super(input);
	}

}
