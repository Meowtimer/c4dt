/**
 * 
 */
package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * @author ZokRadonh
 *
 */
public class WordScanner implements IWordDetector {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	 */
	public boolean isWordPart(char c) {
		if ((c >= 0x41 && c <= 0x5a) || (c >= 0x61 && c <= 0x7a) || c == '_' || (c >= 0x30 && c <= 0x39)) return true;
		else return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	public boolean isWordStart(char c) {
		if ((c >= 0x41 && c <= 0x5a) || (c >= 0x61 && c <= 0x7a) || c == '_') return true;
		else return false;
	}

}
