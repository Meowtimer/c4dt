package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

public class ClonkCommentScanner extends RuleBasedScanner {

	private IToken defaultToken;
	
	public ClonkCommentScanner(ColorManager manager) {
		defaultToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.COMMENT)));
		setDefaultReturnToken(getDefaultToken());
		
	}
	
	public IToken getDefaultToken() {
		return defaultToken;
	}
	
}
