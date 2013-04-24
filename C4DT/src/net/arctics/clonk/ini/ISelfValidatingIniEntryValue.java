package net.arctics.clonk.ini;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.parser.Markers;

public interface ISelfValidatingIniEntryValue {
	public void validate(Markers markers, ComplexIniEntry context) throws ProblemException;
}
