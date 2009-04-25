/**
 * 
 */
package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.BufferedScanner;

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
		return BufferedScanner.isWordPart(c);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	public boolean isWordStart(char c) {
		return BufferedScanner.isWordStart(c);
	}

}
