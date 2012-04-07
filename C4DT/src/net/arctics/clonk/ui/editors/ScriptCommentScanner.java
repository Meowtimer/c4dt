package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.Token;

public class ScriptCommentScanner extends ClonkRuleBasedScanner {
	
	public ScriptCommentScanner(ColorManager manager, String tag) {
		setDefaultReturnToken(createToken(manager, tag));
		setDefaultReturnToken(new Token(new TextAttribute(manager.getColor(ColorManager.colorForSyntaxElement(tag)))));
	}
	
}
