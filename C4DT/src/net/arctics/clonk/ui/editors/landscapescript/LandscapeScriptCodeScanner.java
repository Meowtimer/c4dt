package net.arctics.clonk.ui.editors.landscapescript;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.landscapescript.OverlayBase;
import net.arctics.clonk.ui.editors.StructureTextScanner;
import net.arctics.clonk.ui.editors.WhitespaceDetector;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.CombinedWordRule;
import net.arctics.clonk.ui.editors.WordScanner;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

public class LandscapeScriptCodeScanner extends StructureTextScanner {


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
		@Override
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

	public final static String KEYWORDS = "__keywords"; //$NON-NLS-1$

	private IRule[] currentRules;

	public LandscapeScriptCodeScanner(ColorManager manager) {
		super(manager, null);
	}

	@Override
	protected void commitRules(ColorManager manager, Engine engine) {
		IToken defaultToken = createToken(manager, "DEFAULT"); //$NON-NLS-1$

		IToken operator = createToken(manager, "OPERATOR"); //$NON-NLS-1$
		IToken keyword = createToken(manager, "KEYWORD"); //$NON-NLS-1$
		IToken number = createToken(manager, "NUMBER"); //$NON-NLS-1$
		IToken bracket = createToken(manager, "BRACKET"); //$NON-NLS-1$
		IToken comment = createToken(manager, "COMMENT"); //$NON-NLS-1$

		List<IRule> rules = new ArrayList<IRule>();
		
		rules.add(new EndOfLineRule("//", comment)); //$NON-NLS-1$
		rules.add(new MultiLineRule("/*", "*/", comment)); //$NON-NLS-1$ //$NON-NLS-2$

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new WhitespaceDetector()));

		rules.add(new NumberRule(number));

		// Add rule for operators
		rules.add(new OperatorRule(operator));

		// Add rule for brackets
		rules.add(new BracketRule(bracket));

		WordScanner wordDetector= new WordScanner();
		CombinedWordRule combinedWordRule= new CombinedWordRule(wordDetector, defaultToken);

		// Add word rule for keywords, types, and constants.
		CombinedWordRule.WordMatcher wordRule= new CombinedWordRule.WordMatcher();
		for (String mapGenKeyword : OverlayBase.DEFAULT_CLASS.keySet())
			wordRule.addWord(mapGenKeyword, keyword);

		combinedWordRule.addWordMatcher(wordRule);
		rules.add(combinedWordRule);

		currentRules = rules.toArray(new IRule[0]);
		setRules(currentRules);
	}


}
