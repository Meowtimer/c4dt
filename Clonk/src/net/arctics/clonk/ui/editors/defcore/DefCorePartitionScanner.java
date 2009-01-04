package net.arctics.clonk.ui.editors.defcore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class DefCorePartitionScanner extends RuleBasedPartitionScanner {

	public final static String C4INI_COMMENT = "__c4ini_comment";
	
	public final static String[] C4INI_PARTITIONS = {C4INI_COMMENT, IDocument.DEFAULT_CONTENT_TYPE};
	
	public DefCorePartitionScanner() {
		IToken singleLineComment = new Token(C4INI_COMMENT);
		
		IPredicateRule[] rules = new IPredicateRule[1];
		rules[0] = new EndOfLineRule(";", singleLineComment);
		
		setPredicateRules(rules);
	}
	
}
