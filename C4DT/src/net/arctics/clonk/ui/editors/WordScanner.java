/**
 * 
 */
package net.arctics.clonk.ui.editors;

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
	@Override
	public boolean isWordPart(char c) {
		return BufferedScanner.isWordPart(c);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	@Override
	public boolean isWordStart(char c) {
		return BufferedScanner.isWordStart(c);
	}

}
