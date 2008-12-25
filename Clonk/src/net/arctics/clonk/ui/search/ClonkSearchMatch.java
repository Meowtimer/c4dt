package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.C4ScriptBase;

import org.eclipse.search.ui.text.Match;

public class ClonkSearchMatch extends Match {

	private String line;
	private int lineOffset;
	
	@Override
	public String toString() {
		return line;
	}
	
	public ClonkSearchMatch(String line, int lineOffset, Object element, int offset, int length) {
		super(element, offset, length);
		this.line = line;
		this.lineOffset = lineOffset;
	}

	public String getLine() {
		return line;
	}

	public C4ScriptBase getScript() {
		return (C4ScriptBase) getElement();
	}

	public int getLineOffset() {
		return lineOffset;
	}

}
