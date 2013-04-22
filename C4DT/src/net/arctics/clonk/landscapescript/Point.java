package net.arctics.clonk.landscapescript;

import net.arctics.clonk.Core;

public class Point extends OverlayBase {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public Range x, y;

	@Override
	public String toString() {
		return printed(0).replaceAll("\n", " ");
	}

}
