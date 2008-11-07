package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

public class ClonkPairMatcher extends DefaultCharacterPairMatcher implements ICharacterPairMatcher {

	public ClonkPairMatcher(char[] chars) {
		super(chars);
	}

}
