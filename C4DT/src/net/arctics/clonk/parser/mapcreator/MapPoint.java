package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.ClonkCore;

public class MapPoint extends MapOverlayBase {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	public Range x, y;

	@Override
	public String toString() {
		return toString(0).replaceAll("\n", " ");
	}

}
