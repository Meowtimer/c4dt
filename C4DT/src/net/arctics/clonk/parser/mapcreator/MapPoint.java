package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.Core;

public class MapPoint extends MapOverlayBase {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public Range x, y;

	@Override
	public String toString() {
		return toString(0).replaceAll("\n", " ");
	}

}
