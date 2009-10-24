package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BuiltInDefinitions;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.IClonkColorConstants;
import net.arctics.clonk.ui.editors.WordScanner;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class C4ScriptCodeScanner extends RuleBasedScanner {

	/**
	 * Rule to detect clonk operators.
	 *
	 */
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
	
	public static Map<String,IToken> fTokenMap= new HashMap<String, IToken>();
//	static String[] fgKeywords= {
//		"break", 
//		"continue",
//		"const",
//		"do", 
//		"else",
//		"for", "func", 
//		"global",
//		"if",
//		"local",
//		"private", "protected", "public", 
//		"static",
//		"this",
//		"var",
//		"while"
//	};

	private static final String RETURN= "return"; //$NON-NLS-1$

//	private static String[] fgTypes= { "any", "array", "bool", "dword", "id", "int", "object", "string" }; //$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ //$NON-NLS-9$  //$NON-NLS-10$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$

	private static String[] fgConstants= { "false", "null", "true" }; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

//	private static String[] fgBuiltInFunctions = {"CreateObject","FindObjects"};
	
	private static String[] fgDirectives = {"include", "strict", "appendto"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	public final static String KEYWORDS = "__keywords"; //$NON-NLS-1$
	
	private IRule[] currentRules;
	
	public C4ScriptCodeScanner(ColorManager manager) {

		IToken defaultToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.DEFAULT)));
		
		IToken operator = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.OPERATOR)));
		IToken keyword = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.KEYWORD)));
		IToken type = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.TYPE)));
		IToken engineFunction = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.ENGINE_FUNCTION)));
		IToken objCallbackFunction = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.OBJ_CALLBACK)));
		IToken string = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.STRING)));
//		IToken number = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.NUMBER)));
		IToken bracket = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.BRACKET)));
		IToken returnToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.RETURN)));
		IToken pragma = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.PRAGMA)));
		
//		fTokenMap.put(ClonkScriptPartitionScanner.C4S_STRING, string);
		
		List<IRule> rules = new ArrayList<IRule>();
		
		rules.add(new SingleLineRule("\"", "\"", string, '\\')); //$NON-NLS-1$ //$NON-NLS-2$
		
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new ClonkWhitespaceDetector()));
		

		// Add rule for operators
		rules.add(new OperatorRule(operator));

		// Add rule for brackets
		rules.add(new BracketRule(bracket));

		WordScanner wordDetector= new WordScanner();
		CombinedWordRule combinedWordRule= new CombinedWordRule(wordDetector, defaultToken);
		
		// Add word rule for keyword 'return'.
		CombinedWordRule.WordMatcher returnWordRule= new CombinedWordRule.WordMatcher();
		returnWordRule.addWord(RETURN, returnToken);  
		combinedWordRule.addWordMatcher(returnWordRule);

		// Add word rule for keywords, types, and constants.
		CombinedWordRule.WordMatcher wordRule= new CombinedWordRule.WordMatcher();
//		for (int i=0; i<fgKeywords.length; i++)
//			wordRule.addWord(fgKeywords[i], keyword);
		for (String c4keyword : BuiltInDefinitions.KEYWORDS)
			wordRule.addWord(c4keyword.trim(), keyword);
		for (String c4keyword : BuiltInDefinitions.DECLARATORS)
			wordRule.addWord(c4keyword.trim(), keyword);
//		for (int i=0; i<fgTypes.length; i++)
//			wordRule.addWord(fgTypes[i], type);
		for (C4Type c4type : C4Type.values()) 
			if (c4type != C4Type.UNKNOWN)
				wordRule.addWord(c4type.name().trim().toLowerCase(), type);
		for (int i=0; i<fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], type);
		for (C4Function func : ClonkCore.getDefault().getEngineObject().functions())
			wordRule.addWord(func.getName(), engineFunction);
		for (int i=0; i<BuiltInDefinitions.OBJECT_CALLBACKS.length; i++)
			wordRule.addWord(BuiltInDefinitions.OBJECT_CALLBACKS[i], objCallbackFunction);
		
		
		combinedWordRule.addWordMatcher(wordRule);
		
		rules.add(combinedWordRule);
		
		rules.add(new PragmaRule(fgDirectives,pragma));
		
//		for (int i=0; i<fgDirectives.length; i++)
//			rules.add(new PatternRule("#" + fgDirectives[i]," ",pragma,(char)0,true));
		
//		WordRule engineFunctionRule = new WordRule(new WordScanner());
		
		//rules.add(new NumberRule(number));
		
//		rules.add(new OperatorScanner());
////		rules.add(operatorRule);
//		rules.add(keywordRule);
//		rules.add(typeRule);
//		rules.add(engineFunctionRule);
		currentRules = (IRule[])rules.toArray(new IRule[0]);
		setRules(currentRules);
	}
}
