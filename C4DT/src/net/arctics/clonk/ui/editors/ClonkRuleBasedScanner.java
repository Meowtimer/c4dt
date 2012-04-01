package net.arctics.clonk.ui.editors;

import net.arctics.clonk.ui.editors.ColorManager.SyntaxElementStyle;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

public abstract class ClonkRuleBasedScanner extends RuleBasedScanner {
	
	protected static final class NumberRule implements IRule {

		private final IToken token;
		
		public NumberRule(IToken token) {
			this.token = token;
		}
		
		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int character = scanner.read();
			boolean isNegative = false;
			if (character == '-') {
				character = scanner.read();
				isNegative = true;
			}
			if (character >= 0x30 && character <= 0x39) {
				do {
					character = scanner.read();
				} while (character >= 0x30 && character <= 0x39);
				scanner.unread();
				return token;
			}
			else {
				scanner.unread();
				if (isNegative) scanner.unread();
				return Token.UNDEFINED;
			}
		}
		
	}
	
	/**
	 * Rule to detect java brackets.
	 *
	 * @since 3.3
	 */
	protected static final class BracketRule implements IRule {

		/** Java brackets */
		private final char[] JAVA_BRACKETS= { '(', ')', '{', '}', '[', ']' };
		/** Token to return for this rule */
		private final IToken fToken;

		/**
		 * Creates a new bracket rule.
		 *
		 * @param token Token to use for this rule
		 */
		public BracketRule(IToken token) {
			fToken= token;
		}

		/**
		 * Is this character a bracket character?
		 *
		 * @param character Character to determine whether it is a bracket character
		 * @return <code>true</code> if the character is a bracket, <code>false</code> otherwise.
		 */
		public boolean isBracket(char character) {
			for (int index= 0; index < JAVA_BRACKETS.length; index++) {
				if (JAVA_BRACKETS[index] == character)
					return true;
			}
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		@Override
		public IToken evaluate(ICharacterScanner scanner) {

			int character= scanner.read();
			if (isBracket((char) character)) {
				do {
					character= scanner.read();
				} while (isBracket((char) character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}
	
	protected Token createToken(ColorManager manager, String colorPrefName) {
		SyntaxElementStyle style = ColorManager.syntaxElementStyles.get(colorPrefName);
		return new Token(new TextAttribute(manager.getColor(style.rgb()), null, style.style()));
	}
}
