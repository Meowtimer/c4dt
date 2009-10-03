package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.*;

public class ClonkPartitionScanner extends RuleBasedPartitionScanner {
	public final static String C4S_COMMENT = "__c4s_comment";
	public final static String C4S_MULTI_LINE_COMMENT = "__c4s_multi_comment";
	public final static String C4S_CODEBODY = "__c4s_codebody";
	public final static String C4S_STRING = "__c4s_string";
	
	public final static String[] C4S_PARTITIONS = {C4S_COMMENT, C4S_MULTI_LINE_COMMENT, C4S_STRING, C4S_CODEBODY, IDocument.DEFAULT_CONTENT_TYPE};

	public ClonkPartitionScanner() {

		IToken singleLineComment = new Token(C4S_COMMENT);
		IToken multiLineComment = new Token(C4S_MULTI_LINE_COMMENT);
		IToken string = new Token(C4S_STRING);
		IPredicateRule[] rules = new IPredicateRule[] {
			new EndOfLineRule("//", singleLineComment),
			new MultiLineRule("/*", "*/", multiLineComment,(char)0,true),
			new SingleLineRule("\"","\"",string, '\\')
		};
		setPredicateRules(rules);
	}
	
	public String getContentType() {
		return super.fContentType;
	}
	
	public int getPartitionOffset() {
		return super.fPartitionOffset;
	}
}
