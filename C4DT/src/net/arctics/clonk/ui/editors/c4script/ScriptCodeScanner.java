package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.c4script.BuiltInDefinitions;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.ui.editors.ClonkRuleBasedScanner;
import net.arctics.clonk.ui.editors.ClonkWhitespaceDetector;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.CombinedWordRule;
import net.arctics.clonk.ui.editors.PragmaRule;
import net.arctics.clonk.ui.editors.WordScanner;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

public class ScriptCodeScanner extends ClonkRuleBasedScanner {

	private static final class OperatorRule implements IRule {

		/** Clonk operators */
		private final char[] CLONK_OPERATORS= { ':', ';', '.', '=', '/', '\\', '+', '-', '*', '<', '>', '?', '!', ',', '|', '&', '^', '%', '~'};
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
			for (int index= 0; index < CLONK_OPERATORS.length; index++)
				if (CLONK_OPERATORS[index] == character)
					return true;
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		@Override
		public IToken evaluate(ICharacterScanner scanner) {

			char character = (char) scanner.read();
			if (character == '/') {
				char peek = (char) scanner.read();
				if (peek == '*' || peek == '/') {
					scanner.unread();
					scanner.unread();
					return Token.UNDEFINED;
				}
				else
					scanner.unread();
			}
			if (isOperator(character)) {
				do
					character = (char) scanner.read();
				while (isOperator(character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}
	
	public static Map<String,IToken> fTokenMap= new HashMap<String, IToken>();
	
	public ScriptCodeScanner(ColorManager manager, Engine engine) {
		super(manager, engine, "DEFAULT");
	}
	
	@Override
	protected void commitRules(ColorManager manager, Engine engine) {
		
		IToken defaultToken = createToken(manager, "DEFAULT"); //$NON-NLS-1$
		
		IToken operator = createToken(manager, "OPERATOR"); //$NON-NLS-1$
		IToken keyword = createToken(manager, "KEYWORD"); //$NON-NLS-1$
		IToken type = createToken(manager, "TYPE"); //$NON-NLS-1$
		IToken engineFunction = createToken(manager, "ENGINE_FUNCTION"); //$NON-NLS-1$
		IToken objCallbackFunction = createToken(manager, "OBJ_CALLBACK"); //$NON-NLS-1$
		IToken string = createToken(manager, "STRING"); //$NON-NLS-1$
		IToken number = createToken(manager, "NUMBER"); //$NON-NLS-1$
		IToken bracket = createToken(manager, "BRACKET"); //$NON-NLS-1$
		IToken returnToken = createToken(manager, "RETURN"); //$NON-NLS-1$
		IToken directive = createToken(manager, "DIRECTIVE"); //$NON-NLS-1$
		
		List<IRule> rules = new ArrayList<IRule>();
		
		// string
		rules.add(new SingleLineRule("\"", "\"", string, '\\')); //$NON-NLS-1$ //$NON-NLS-2$
		
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new ClonkWhitespaceDetector()));

		// Add rule for operators
		rules.add(new OperatorRule(operator));

		// Add rule for brackets
		rules.add(new BracketRule(bracket));

		WordScanner wordDetector = new WordScanner();
		CombinedWordRule combinedWordRule = new CombinedWordRule(wordDetector, defaultToken);
		
		// Add word rule for keyword 'return'.
		CombinedWordRule.WordMatcher returnWordRule = new CombinedWordRule.WordMatcher();
		returnWordRule.addWord(Keywords.Return, returnToken);  
		combinedWordRule.addWordMatcher(returnWordRule);

		// Add word rule for keywords, types, and constants.
		CombinedWordRule.WordMatcher wordRule= new CombinedWordRule.WordMatcher();
		for (String k : BuiltInDefinitions.KEYWORDS)
			wordRule.addWord(k.trim(), keyword);
		for (String k : BuiltInDefinitions.DECLARATORS)
			wordRule.addWord(k.trim(), keyword);
		for (PrimitiveType t : PrimitiveType.values()) 
			if (t != PrimitiveType.UNKNOWN && engine.supportsPrimitiveType(t))
				wordRule.addWord(t.typeName(false).trim().toLowerCase(), type);
		for (Function func : engine.functions())
			wordRule.addWord(func.name(), engineFunction);
		for (String c : engine.settings().callbackFunctions())
			wordRule.addWord(c, objCallbackFunction);
		
		
		combinedWordRule.addWordMatcher(wordRule);
		rules.add(combinedWordRule);
		rules.add(new PragmaRule(Directive.arrayOfDirectiveStrings(), directive));
		rules.add(new NumberRule(number));
		
		setRules(rules.toArray(new IRule[rules.size()]));
	}

}
