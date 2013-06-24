package net.arctics.clonk.ini;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.parser.Markers;

public interface ISelfValidatingIniEntryValue {
	public void validate(Markers markers, IniEntry context) throws ProblemException;
}
