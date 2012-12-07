package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;

import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.parser.BufferedScanner;


public class MatchingPlaceholder extends Placeholder {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private Class<? extends ExprElm> requiredClass;
	private Pattern stringRepresentationPattern;
	private boolean remainder;
	private ExprElm[] subElements;
	private ExecutableScript transformation;

	public Pattern stringRepresentationPattern() { return stringRepresentationPattern; }
	public Class<? extends ExprElm> requiredClass() { return requiredClass; }
	public boolean remainder() { return remainder; }

	@Override
	public ExprElm[] subElements() {
		return subElements != null ? subElements : EMPTY_EXPR_ARRAY;
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		subElements = elms;
	}

	@SuppressWarnings("unchecked")
	private void parse(String matchText) {
		BufferedScanner scanner = new BufferedScanner(matchText);
		String entry = scanner.readIdent();
		if (!entry.equals("") && scanner.read() == ':') {
			while (!scanner.reachedEOF())
				switch (scanner.read()) {
				case '/':
					int start = scanner.tell();
					int end = start;
					while (scanner.read() != '/')
						end++;
					stringRepresentationPattern = Pattern.compile(scanner.readStringAt(start, end));
					break;
				case ',':
					break;
				case '.':
					while (scanner.peek() == '.')
						scanner.read();
					remainder = true;
					break;
				case '!':
					transformation = Command.executableScriptFromCommand(scanner.readString(scanner.bufferSize()-scanner.tell()));
					break;
				default:
					scanner.unread();
					String className = scanner.readIdent();
					try {
						requiredClass = (Class<? extends ExprElm>) ExprElm.class.getClassLoader().loadClass(String.format("%s.parser.c4script.ast.%s", Core.PLUGIN_ID, className));
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			this.entryName = entry;
		}
	}

	public boolean satisfiedBy(ExprElm element) {
		if (requiredClass != null && !requiredClass.isInstance(element))
			return false;
		if (stringRepresentationPattern != null) {
			IPlaceholderPatternMatchTarget target = as(element, IPlaceholderPatternMatchTarget.class);
			if (target == null)
				return false;
			String patternMatchingText = target.patternMatchingText();
			if (patternMatchingText == null || !stringRepresentationPattern.matcher(patternMatchingText).matches())
				return false;
		}
		return true;
	}

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		super.doPrint(builder, depth);
		if (subElements != null)
			CallDeclaration.printParmString(builder, subElements, depth);
	}

	public MatchingPlaceholder(String text) {
		super(text);
		parse(text);
	}
}