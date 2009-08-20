package net.arctics.clonk.ui.editors.mapcreator;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.parser.BuiltInDefinitions;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.IClonkColorConstants;
import net.arctics.clonk.ui.editors.WordScanner;
import net.arctics.clonk.ui.editors.c4script.ClonkWhitespaceDetector;
import net.arctics.clonk.ui.editors.c4script.CombinedWordRule;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

public class MapCreatorCodeScanner extends RuleBasedScanner {


	private static final class OperatorRule implements IRule {

		/** Clonk operators */
		private final char[] CLONK_OPERATORS= { '^', '&', '|' };
		/** Token to return for this rule */
		private final IToken fToken;

		/**
		 * Creates a new operator rule.
		 *
		 * @param token Token to use for this rule
		 */
		public OperatorRule(IToken token) {
			fToken= token;
		}

		/**
		 * Is this character an operator character?
		 *
		 * @param character Character to determine whether it is an operator character
		 * @return <code>true</code> if the character is an operator, <code>false</code> otherwise.
		 */
		public boolean isOperator(char character) {
			for (int index= 0; index < CLONK_OPERATORS.length; index++) {
				if (CLONK_OPERATORS[index] == character)
					return true;
			}
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {

			int character= scanner.read();
			if (isOperator((char) character)) {
				do {
					character= scanner.read();
				} while (isOperator((char) character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}

	/**
	 * Rule to detect java brackets.
	 *
	 * @since 3.3
	 */
	private static final class BracketRule implements IRule {

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

	public final static String KEYWORDS = "__keywords";

	private IRule[] currentRules;

	public MapCreatorCodeScanner(ColorManager manager) {

		IToken defaultToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.DEFAULT)));

		IToken operator = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.OPERATOR)));
		IToken keyword = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.KEYWORD)));
		//			IToken number = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.NUMBER)));
		IToken bracket = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.BRACKET)));

		//			fTokenMap.put(ClonkScriptPartitionScanner.C4S_STRING, string);

		List<IRule> rules = new ArrayList<IRule>();

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new ClonkWhitespaceDetector()));


		// Add rule for operators
		rules.add(new OperatorRule(operator));

		// Add rule for brackets
		rules.add(new BracketRule(bracket));

		WordScanner wordDetector= new WordScanner();
		CombinedWordRule combinedWordRule= new CombinedWordRule(wordDetector, defaultToken);

		// Add word rule for keywords, types, and constants.
		CombinedWordRule.WordMatcher wordRule= new CombinedWordRule.WordMatcher();
		for (String c4keyword : BuiltInDefinitions.MAPGENKEYWORDS)
			wordRule.addWord(c4keyword, keyword);

		combinedWordRule.addWordMatcher(wordRule);
		rules.add(combinedWordRule);

		currentRules = (IRule[])rules.toArray(new IRule[0]);
		setRules(currentRules);
	}


}
