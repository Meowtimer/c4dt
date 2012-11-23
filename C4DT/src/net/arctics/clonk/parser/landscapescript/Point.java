package net.arctics.clonk.parser.landscapescript;

import net.arctics.clonk.Core;

public class Point extends OverlayBase {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public Range x, y;

	@Override
	public String toString() {
		return toString(0).replaceAll("\n", " ");
	}

}
