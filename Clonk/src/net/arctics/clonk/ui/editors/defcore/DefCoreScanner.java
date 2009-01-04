package net.arctics.clonk.ui.editors.defcore;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.ui.editors.ClonkWhitespaceDetector;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.CombinedWordRule;
import net.arctics.clonk.ui.editors.IClonkColorConstants;
import net.arctics.clonk.ui.editors.WordScanner;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

public class DefCoreScanner extends RuleBasedScanner {
	
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
	
	private static final class NumberRule implements IRule {

		private IToken token;
		
		public NumberRule(IToken token) {
			this.token = token;
		}
		
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
	
	public DefCoreScanner(ColorManager manager) {
		
		IToken defaultToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.DEFAULT)));
		
		IToken operator = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.OPERATOR)));
		IToken section = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.KEYWORD)));
		IToken number = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.NUMBER)));
		IToken constant = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.ENGINE_FUNCTION)));
		
		List<IRule> rules = new ArrayList<IRule>();
		
		rules.add(new SingleLineRule("[", "]", section, '\\'));
		
		rules.add(new OperatorRule(operator));
		
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new ClonkWhitespaceDetector()));
		
		WordScanner wordDetector = new WordScanner();
		CombinedWordRule combinedWordRule = new CombinedWordRule(wordDetector, defaultToken);
		
		CombinedWordRule.WordMatcher wordRule = new CombinedWordRule.WordMatcher();
		
		for(C4Variable var : ClonkCore.ENGINE_OBJECT.getDefinedVariables()) {
			if (var.getScope() == C4VariableScope.VAR_CONST)
				wordRule.addWord(var.getName(), constant);
		}
		
		combinedWordRule.addWordMatcher(wordRule);
		
		rules.add(combinedWordRule);
		
		rules.add(new NumberRule(number));
		
		setRules(rules.toArray(new IRule[rules.size()]));
	}
}
