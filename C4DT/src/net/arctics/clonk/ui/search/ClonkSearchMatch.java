package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.parser.Structure;

import org.eclipse.search.ui.text.Match;

public class ClonkSearchMatch extends Match {

	private final String line;
	private final int lineOffset;
	private final boolean potential;
	private final boolean indirect;
	
	@Override
	public String toString() {
		return line;
	}
	
	public ClonkSearchMatch(String line, int lineOffset, Object element, int offset, int length, boolean potential, boolean indirect) {
		super(element, offset, length);
		this.line = defaulting(line, "...");
		this.lineOffset = lineOffset;
		this.potential = potential;
		this.indirect = indirect;
	}
	
	public String line() {
		return line;
	}

	public Structure structure() {
		Structure s = (Structure) getElement();
		if (s != null)
			return (Structure) s.latestVersion();
		else
			return null;
	}

	public int lineOffset() {
		return lineOffset;
	}

	public boolean isPotential() {
		return potential;
	}

	public boolean isIndirect() {
		return indirect;
	}

}
