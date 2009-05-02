package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

public class C4ScriptCommentScanner extends RuleBasedScanner {

	private IToken defaultToken;
	
	public C4ScriptCommentScanner(ColorManager manager) {
		defaultToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.COMMENT)));
		setDefaultReturnToken(getDefaultToken());
		
	}
	
	public IToken getDefaultToken() {
		return defaultToken;
	}
	
}
