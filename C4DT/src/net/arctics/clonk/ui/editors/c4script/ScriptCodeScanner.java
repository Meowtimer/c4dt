package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

import net.arctics.clonk.c4script.BuiltInDefinitions;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.CombinedWordRule;
import net.arctics.clonk.ui.editors.PragmaRule;
import net.arctics.clonk.ui.editors.StructureTextScanner;
import net.arctics.clonk.ui.editors.WhitespaceDetector;
import net.arctics.clonk.ui.editors.WordScanner;

public class ScriptCodeScanner extends StructureTextScanner {

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
		public OperatorRule(final IToken token) {
			fToken= token;
		}

		/**
		 * Is this character an operator character?
		 *
		 * @param character Character to determine whether it is an operator character
		 * @return <code>true</code> if the character is an operator, <code>false</code> otherwise.
		 */
		public boolean isOperator(final char character) {
			for (final char element : CLONK_OPERATORS) {
				if (element == character) {
					return true;
				}
			}
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		@Override
		public IToken evaluate(final ICharacterScanner scanner) {

			char character = (char) scanner.read();
			if (character == '/') {
				final char peek = (char) scanner.read();
				if (peek == '*' || peek == '/') {
					scanner.unread();
					scanner.unread();
					return Token.UNDEFINED;
				} else {
					scanner.unread();
				}
			}
			if (isOperator(character)) {
				do {
					character = (char) scanner.read();
				} while (isOperator(character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}

	public static Map<String,IToken> fTokenMap= new HashMap<String, IToken>();

	public ScriptCodeScanner(final ColorManager manager, final Engine engine) {
		super(manager, engine, "DEFAULT");
	}

	@Override
	protected void commitRules(final ColorManager manager, final Engine engine) {

		final IToken defaultToken = createToken(manager, "DEFAULT"); //$NON-NLS-1$

		final IToken operator = createToken(manager, "OPERATOR"); //$NON-NLS-1$
		final IToken keyword = createToken(manager, "KEYWORD"); //$NON-NLS-1$
		final IToken type = createToken(manager, "TYPE"); //$NON-NLS-1$
		final IToken engineFunction = createToken(manager, "ENGINE_FUNCTION"); //$NON-NLS-1$
		final IToken objCallbackFunction = createToken(manager, "OBJ_CALLBACK"); //$NON-NLS-1$
		final IToken string = createToken(manager, "STRING"); //$NON-NLS-1$
		final IToken number = createToken(manager, "NUMBER"); //$NON-NLS-1$
		final IToken bracket = createToken(manager, "BRACKET"); //$NON-NLS-1$
		final IToken returnToken = createToken(manager, "RETURN"); //$NON-NLS-1$
		final IToken directive = createToken(manager, "DIRECTIVE"); //$NON-NLS-1$

		final List<IRule> rules = new ArrayList<IRule>();

		// string
		rules.add(new SingleLineRule("\"", "\"", string, '\\')); //$NON-NLS-1$ //$NON-NLS-2$

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new WhitespaceDetector()));

		// Add rule for operators
		rules.add(new OperatorRule(operator));

		// Add rule for brackets
		rules.add(new BracketRule(bracket));

		final WordScanner wordDetector = new WordScanner();
		final CombinedWordRule combinedWordRule = new CombinedWordRule(wordDetector, defaultToken);

		// Add word rule for keyword 'return'.
		final CombinedWordRule.WordMatcher returnWordRule = new CombinedWordRule.WordMatcher();
		returnWordRule.addWord(Keywords.Return, returnToken);
		combinedWordRule.addWordMatcher(returnWordRule);

		// Add word rule for keywords, types, and constants.
		final CombinedWordRule.WordMatcher wordRule= new CombinedWordRule.WordMatcher();
		for (final String k : BuiltInDefinitions.KEYWORDS) {
			wordRule.addWord(k.trim(), keyword);
		}
		for (final String k : BuiltInDefinitions.DECLARATORS) {
			wordRule.addWord(k.trim(), keyword);
		}
		for (final Entry<String, PrimitiveType> entry : PrimitiveType.REGULAR_MAP.entrySet()) {
			if (entry.getValue() != PrimitiveType.UNKNOWN && engine.supportsPrimitiveType(entry.getValue())) {
				wordRule.addWord(entry.getKey(), type);
			}
		}
		for (final Function func : engine.functions()) {
			wordRule.addWord(func.name(), engineFunction);
		}
		for (final String c : engine.settings().callbackFunctions()) {
			wordRule.addWord(c, objCallbackFunction);
		}


		combinedWordRule.addWordMatcher(wordRule);
		rules.add(combinedWordRule);
		rules.add(new PragmaRule(Directive.arrayOfDirectiveStrings(), directive));
		rules.add(new NumberRule(number));

		setRules(rules.toArray(new IRule[rules.size()]));
	}

}
