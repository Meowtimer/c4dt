package net.arctics.clonk.util;

public class LineNumberObtainer {
	private String string;
	private int currentPos, currentLine;
	private int lastLineBreak, lastQueriedPos;
	public LineNumberObtainer(String string) {
		this.string = string;
	}
	public int obtainLineNumber(int pos) {
		assert(pos < string.length());
		lastQueriedPos = pos;
		if (pos < currentPos) {
			currentPos = 0;
			currentLine = 0;
		}
		for (; currentPos < pos; currentPos++) {
			if (string.charAt(currentPos) == '\n') {
				lastLineBreak = currentPos;
				currentLine++;
			}
		}
		return string.charAt(currentPos) == '\n' ? -1 : currentLine+1;
	}
	public int obtainCharNumberInObtainedLine() {
		return lastQueriedPos - lastLineBreak + 1;
	}
}
