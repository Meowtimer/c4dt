package net.arctics.clonk.ui.editors;


import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

public class ScriptCommentScanner extends RuleBasedScanner {

	private IToken defaultToken;
	
	public ScriptCommentScanner(ColorManager manager) {
		defaultToken = new Token(new TextAttribute(manager.getColor(ClonkColorConstants.getColor("COMMENT"))));
		setDefaultReturnToken(getDefaultToken());
		
	}
	
	public IToken getDefaultToken() {
		return defaultToken;
	}
	
}
