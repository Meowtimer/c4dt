package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class PragmaRule implements IPredicateRule {

	private final IToken successToken;
	private final char[][] directives;
	
	public PragmaRule(final String[] directives, final IToken token) {
		successToken = token;
		this.directives = new char[directives.length][];
		for(int i = 0;i < directives.length;i++)
			this.directives[i] = directives[i].toCharArray();
	}

	@Override
	public IToken evaluate(final ICharacterScanner scanner, final boolean resume) {
		
		int c = scanner.read();
		if (c == '#')
			for(int i = 0; i < directives.length;i++) {
				int x;
				for(x = 0; x < directives[i].length;x++) {
					c = scanner.read();
					
					if (c == ICharacterScanner.EOF || c != directives[i][x]) {
						for(;x >= 0;x--) scanner.unread();
						break;
					}
				}
				if (x >= 0) {
					c = scanner.read();
					if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == ICharacterScanner.EOF)
						return getSuccessToken();
					else 
						for(;x >= 0;x--) scanner.unread();
				}
			}
		scanner.unread(); // #-unread
		return Token.UNDEFINED;
	}

	@Override
	public IToken getSuccessToken() {
		return successToken;
	}

	@Override
	public IToken evaluate(final ICharacterScanner scanner) {
		return evaluate(scanner, false);
	}

}
