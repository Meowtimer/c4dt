package net.arctics.clonk.ui.editors;

public class ScriptCommentScanner extends ClonkRuleBasedScanner {
	
	public ScriptCommentScanner(ColorManager manager) {
		setDefaultReturnToken(createToken(manager, "COMMENT"));
	}
	
}
