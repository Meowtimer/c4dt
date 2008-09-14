package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class CodeBodyRule implements IPredicateRule {

	private IToken successToken;
	private int openBrackets = 0;
	
	public CodeBodyRule(IToken token) {
		successToken = token;
	}
	
	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		if (resume) {
			if (scanner instanceof ClonkPartitionScanner) {
				ClonkPartitionScanner clonkScanner = (ClonkPartitionScanner)scanner;
				if (clonkScanner.getContentType() == ClonkPartitionScanner.C4S_COMMENT) {
					return successToken;
				}
				else if (clonkScanner.getContentType() == ClonkPartitionScanner.C4S_MULTI_LINE_COMMENT) {
					return successToken;
				}
				else if (clonkScanner.getContentType() == ClonkPartitionScanner.C4S_CODEBODY) {
					int offset = clonkScanner.getTokenOffset();
				}
			}
		}
		if (!resume && openBrackets == 0) {
			if (scanner.read() != '{') {
				scanner.unread();
				return Token.UNDEFINED;
			}
			openBrackets = 1;
		}
		while(openBrackets > 0) {
			int c = scanner.read();
			if (c == '{') openBrackets++;
			else if (c == '}') openBrackets--;
			else if (c == ICharacterScanner.EOF) {
				return Token.UNDEFINED;
			}
			else if (c == '/') {
				int next = scanner.read();
				if (next == '/' || next == '*') {
					scanner.unread();
					scanner.unread();
					return getSuccessToken(); //paused
				}
			}
		}
		return getSuccessToken();
	}

	public IToken getSuccessToken() {
		return successToken;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		return evaluate(scanner, false);
	}

}
