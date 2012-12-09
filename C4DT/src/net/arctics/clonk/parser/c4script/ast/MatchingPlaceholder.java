package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.CommandFunction;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.c4script.Script;

import org.eclipse.core.resources.IStorage;

public class MatchingPlaceholder extends Placeholder {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static class Transformations extends Script {
		protected Transformations(Index index) {
			super(index);
			Command.registerCommandsFromClass(this, this.getClass());
		}
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		@Override
		public IStorage scriptStorage() {
			try {
				return new SimpleScriptStorage("CommandBase", ""); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
		@Override
		public String name() {
			return "CommandBaseScript"; //$NON-NLS-1$
		};
		@Override
		public String nodeName() {
			return name();
		};
		@CommandFunction
		public static List<ExprElm> reverse(Object context, List<ExprElm> input) {
			ArrayList<ExprElm> list = new ArrayList<ExprElm>(input);
			Collections.reverse(list);
			return list;
		}
	}
	
	public static final Script TRANSFORMATIONS = new Transformations(new Index());

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
	private void parse(String matchText) throws ParsingException {
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
				case '…':
					remainder = true;
					break;
				case '!':
					transformation = Command.executableScriptFromCommand(scanner.readString(scanner.bufferSize()-scanner.tell()));
					break;
				default:
					scanner.unread();
					String className = scanner.readIdent();
					if (className.length() == 0) {
						scanner.read();
						continue;
					}
					String[] packageFormats = new String[] { "%s.parser.c4script.ast.%s", "%s.parser.c4script.ast.%sLiteral" };
					for (String pkgFormat : packageFormats)
						try {
							requiredClass = (Class<? extends ExprElm>) ExprElm.class.getClassLoader().loadClass(String.format(pkgFormat, Core.PLUGIN_ID, className));
							break;
						} catch (ClassNotFoundException e) {
							continue;
						}
					if (requiredClass == null)
						throw new ParsingException("AST class not found: %s", null);
				}
			this.entryName = entry;
		}
	}

	public boolean satisfiedBy(ExprElm element) {
		RequiredClass: if (requiredClass != null) {
			// OC: references to definitions are not IDLiterals but AccessVars referring to proxy variables
			if (requiredClass == IDLiteral.class) {
				AccessVar av = as(element, AccessVar.class);
				if (av != null && av.proxiedDefinition() != null)
					break RequiredClass;
			}
			if (!requiredClass.isInstance(element))
				return false;
		}
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

	public MatchingPlaceholder(String text) throws ParsingException {
		super(text);
		parse(text);
	}
}