package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class ClonkWhitespaceDetector implements IWhitespaceDetector {
	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
