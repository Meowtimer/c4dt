package net.arctics.clonk.ui.editors.ini;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.ui.editors.ClonkRuleBasedScanner;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.WordScanner;
import net.arctics.clonk.ui.editors.c4script.ClonkWhitespaceDetector;
import net.arctics.clonk.ui.editors.c4script.CombinedWordRule;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

public class IniScanner extends ClonkRuleBasedScanner {
	
	private static final class OperatorRule implements IRule {

		/** Clonk operators */
		private final char[] DEFCORE_OPERATORS= { '=', '[', ']', ',', '|', ';' };
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
			for (int index= 0; index < DEFCORE_OPERATORS.length; index++) {
				if (DEFCORE_OPERATORS[index] == character)
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
	
	public IniScanner(ColorManager manager, Engine engine) {
		
		IToken defaultToken = createToken(manager, "DEFAULT"); //$NON-NLS-1$
		
		IToken operator = createToken(manager, "OPERATOR"); //$NON-NLS-1$
		IToken section = createToken(manager, "KEYWORD"); //$NON-NLS-1$
		IToken number = createToken(manager, "NUMBER"); //$NON-NLS-1$
		IToken constant = createToken(manager, "ENGINE_FUNCTION"); //$NON-NLS-1$
		IToken comment = createToken(manager, "COMMENT"); //$NON-NLS-1$
		
		List<IRule> rules = new ArrayList<IRule>();
		
		rules.add(new SingleLineRule("[", "]", section, '\\')); //$NON-NLS-1$ //$NON-NLS-2$
		
		//rules.add(new EndOfLineRule(";", comment)); //$NON-NLS-1$
		rules.add(new EndOfLineRule("#", comment)); //$NON-NLS-1$
		rules.add(new EndOfLineRule("//", comment)); //$NON-NLS-1$
		
		rules.add(new OperatorRule(operator));
		
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new ClonkWhitespaceDetector()));
		
		WordScanner wordDetector = new WordScanner();
		CombinedWordRule combinedWordRule = new CombinedWordRule(wordDetector, defaultToken);
		
		CombinedWordRule.WordMatcher wordRule = new CombinedWordRule.WordMatcher();
		
		if (engine != null) {
			for (Variable var : engine.variables()) {
				if (var.getScope() == Scope.CONST)
					wordRule.addWord(var.name(), constant);
			}
		}
		
		combinedWordRule.addWordMatcher(wordRule);
		
		rules.add(combinedWordRule);
		
		rules.add(new NumberRule(number));
		
		setRules(rules.toArray(new IRule[rules.size()]));
	}
}
