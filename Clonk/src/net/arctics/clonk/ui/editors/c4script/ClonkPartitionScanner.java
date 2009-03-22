package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.*;

public class ClonkPartitionScanner extends RuleBasedPartitionScanner {
	public final static String C4S_COMMENT = "__c4s_comment";
	public final static String C4S_MULTI_LINE_COMMENT = "__c4s_multi_comment";
	public final static String C4S_CODEBODY = "__c4s_codebody";
	public final static String C4S_STRING = "__c4s_string";
//	public final static String C4S_
	
	public final static String[] C4S_PARTITIONS = {C4S_COMMENT, C4S_MULTI_LINE_COMMENT, C4S_STRING, C4S_CODEBODY, IDocument.DEFAULT_CONTENT_TYPE};

	public ClonkPartitionScanner() {

		IToken singleLineComment = new Token(C4S_COMMENT);
		IToken multiLineComment = new Token(C4S_MULTI_LINE_COMMENT);
		IToken string = new Token(C4S_STRING);
//		IToken codeBody = new Token(C4S_CODEBODY);
		IPredicateRule[] rules = new IPredicateRule[3];

		rules[0] = new EndOfLineRule("//", singleLineComment);
		rules[1] = new MultiLineRule("/*", "*/", multiLineComment,(char)0,true);
		rules[2] = new SingleLineRule("\"","\"",string);
//		rules[3] = new CodeBodyRule(codeBody);
//		rules[4] = new PatternRule(":","return",codeBody,(char)0,false);

		setPredicateRules(rules);
	}
	
	public String getContentType() {
		return super.fContentType;
	}
	
	public int getPartitionOffset() {
		return super.fPartitionOffset;
	}
}
