package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.*;

public class CStylePartitionScanner extends RuleBasedPartitionScanner {
	public final static String COMMENT = "__c4s_comment"; //$NON-NLS-1$
	public final static String MULTI_LINE_COMMENT = "__c4s_multi_comment"; //$NON-NLS-1$
	public final static String CODEBODY = "__c4s_codebody"; //$NON-NLS-1$
	public final static String STRING = "__c4s_string"; //$NON-NLS-1$
	public final static String JAVADOC_COMMENT = "__c4s_javadoccomment";
	
	public final static String[] PARTITIONS = {
		COMMENT, MULTI_LINE_COMMENT, JAVADOC_COMMENT, STRING, CODEBODY,
		IDocument.DEFAULT_CONTENT_TYPE
	};

	public CStylePartitionScanner() {

		final IToken singleLineComment = new Token(COMMENT);
		final IToken multiLineComment = new Token(MULTI_LINE_COMMENT);
		final IToken javaDocComment = new Token(JAVADOC_COMMENT);
		final IToken string = new Token(STRING);
		final IPredicateRule[] rules = new IPredicateRule[] {
			new EndOfLineRule("//", singleLineComment), //$NON-NLS-1$
			new MultiLineRule("/**", "*/", javaDocComment, (char)0, true),
			new MultiLineRule("/*", "*/", multiLineComment, (char)0, true), //$NON-NLS-1$ //$NON-NLS-2$
			new SingleLineRule("\"","\"",string, '\\') //$NON-NLS-1$ //$NON-NLS-2$
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
