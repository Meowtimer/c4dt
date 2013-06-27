package net.arctics.clonk.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.ui.editors.ColorManager.SyntaxElementStyle;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

public abstract class StructureTextScanner extends RuleBasedScanner {

	public static class ScannerPerEngine<T extends StructureTextScanner> {
		private static final Class<?>[] CTOR_SIGNATURE = new Class<?>[] {ColorManager.class, Engine.class};
		private static final List<ScannerPerEngine<?>> INSTANCES = new ArrayList<ScannerPerEngine<?>>();
		private final Map<String, T> scanners = new HashMap<String, T>();
		private final Class<T> scannerClass;
		public static Iterable<ScannerPerEngine<?>> instances() {
			return Collections.unmodifiableList(INSTANCES);
		}
		public ScannerPerEngine(Class<T> cls) {
			scannerClass = cls;
			INSTANCES.add(this);
		}
		public T get(Engine engine) {
			T scanner = scanners.get(engine.name());
			if (scanner == null)
				try {
					scanner = scannerClass.getConstructor(CTOR_SIGNATURE).newInstance(ColorManager.INSTANCE, engine);
					scanners.put(engine.name(), scanner);
				} catch (final Exception e) {
					e.printStackTrace();
					return null;
				}
			return scanner;
		}
		public static void refreshScanners() {
			for (final ScannerPerEngine<?> i : INSTANCES)
				for (final StructureTextScanner s : i.scanners.values())
					s.recommitRules();
		}
	}

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
				if (character == '0')
					if (scanner.read() == 'x') {
						do
							character = scanner.read();
						while ((character >= '0' && character <= '9') || (character >= 'A' && character <= 'F') || (character >= 'a' && character <= 'f'));
						scanner.unread();
						return token;
					} else
						scanner.unread();
				do
					character = scanner.read();
				while (character >= 0x30 && character <= 0x39);
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
			for (int index= 0; index < JAVA_BRACKETS.length; index++)
				if (JAVA_BRACKETS[index] == character)
					return true;
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		@Override
		public IToken evaluate(ICharacterScanner scanner) {

			int character= scanner.read();
			if (isBracket((char) character)) {
				do
					character= scanner.read();
				while (isBracket((char) character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}

	protected StructureTextScanner(ColorManager manager, Engine engine) {
		this(manager, engine, "DEFAULT");
	}

	private final ColorManager manager;
	private final Engine engine;

	protected StructureTextScanner(ColorManager manager, Engine engine, String returnTokenTag) {
		this.manager = manager;
		this.engine = engine;
		commitRules(manager, engine);
		setDefaultReturnToken(createToken(manager, returnTokenTag));
	}

	public void recommitRules() {
		this.commitRules(manager, engine);
	}

	protected void commitRules(ColorManager manager, Engine engine) {}

	protected Token createToken(ColorManager manager, String colorPrefName) {
		final SyntaxElementStyle style = manager.syntaxElementStyles.get(colorPrefName);
		return new Token(new TextAttribute(manager.getColor(style.rgb()), null, style.style()));
	}
}
