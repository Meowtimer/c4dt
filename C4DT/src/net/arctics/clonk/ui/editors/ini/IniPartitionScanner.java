package net.arctics.clonk.ui.editors.ini;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class IniPartitionScanner extends RuleBasedPartitionScanner {

	public final static String C4INI_COMMENT = "__c4ini_comment"; //$NON-NLS-1$
	
	public final static String[] C4INI_PARTITIONS = {C4INI_COMMENT, IDocument.DEFAULT_CONTENT_TYPE};
	
	public IniPartitionScanner() {
		IToken singleLineComment = new Token(C4INI_COMMENT);
		
		IPredicateRule[] rules = new IPredicateRule[2];
		rules[0] = new EndOfLineRule(";", singleLineComment); //$NON-NLS-1$
		rules[1] = new EndOfLineRule("#", singleLineComment); //$NON-NLS-1$
		
		setPredicateRules(rules);
	}
	
}
