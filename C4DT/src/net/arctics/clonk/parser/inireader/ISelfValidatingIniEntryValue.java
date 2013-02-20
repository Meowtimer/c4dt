package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;

public interface ISelfValidatingIniEntryValue {
	public void validate(Markers markers, ComplexIniEntry context) throws ParsingException;
}
