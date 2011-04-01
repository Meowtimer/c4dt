package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.Structure;
import org.eclipse.search.ui.text.Match;

public class ClonkSearchMatch extends Match {

	private String line;
	private int lineOffset;
	private boolean potential;
	private boolean indirect;
	
	@Override
	public String toString() {
		return line;
	}
	
	public ClonkSearchMatch(String line, int lineOffset, Object element, int offset, int length, boolean potential, boolean indirect) {
		super(element, offset, length);
		this.line = line;
		this.lineOffset = lineOffset;
		this.potential = potential;
		this.indirect = indirect;
	}
	
	public String getLine() {
		return line;
	}

	public Structure getStructure() {
		return (Structure) getElement();
	}

	public int getLineOffset() {
		return lineOffset;
	}

	public boolean isPotential() {
		return potential;
	}

	public boolean isIndirect() {
		return indirect;
	}

}
