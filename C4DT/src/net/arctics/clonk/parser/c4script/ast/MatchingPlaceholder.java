package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.CommandFunction;
import net.arctics.clonk.command.SelfContainedScript;
import net.arctics.clonk.index.Definition.ProxyVar;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IEvaluationContext;
import net.arctics.clonk.parser.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.util.StringUtil;

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
		public IStorage source() {
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
		public static List<ASTNode> reverse(Object context, List<ASTNode> input) {
			ArrayList<ASTNode> list = new ArrayList<ASTNode>(input);
			Collections.reverse(list);
			return list;
		}
		@CommandFunction
		public static Object concat(Object context, Object a, Object b) {
			return a.toString()+b.toString();
		}
		@CommandFunction
		public static Object substring(Object context, String str, Long index, Long length) {
			if (length != null)
				return str.substring(index.intValue(), length.intValue());
			else
				return str.substring(index.intValue());
		}
		@CommandFunction
		public static Object eval(IEvaluationContext context, ASTNode node) {
			try {
				return node.evaluate(context);
			} catch (ControlFlowException e) {
				e.printStackTrace();
				return null;
			}
		}
		@CommandFunction
		public static String replace(IEvaluationContext context, Object text, Object what, Object replacement) {
			return text.toString().replace(what.toString(), replacement.toString());
		}
		@CommandFunction
		public static String quote(IEvaluationContext context, Object text) {
			return String.format("\"%s\"", text);
		}
	}

	/**
	 * Some extra flags specified via [...] after placeholder name
	 * @author madeen
	 *
	 */
	public enum Flag {
		/** Accumulate matches found in function block instead of listing matches on their own */
		Accumulative
	}

	public enum Multiplicity {
		One,
		AtLeastOne,
		Multiple
	}

	public static final Script TRANSFORMATIONS = new Transformations(new Index());

	private Class<? extends ASTNode> requiredClass;
	private Pattern stringRepresentationPattern;
	private Multiplicity multiplicity = Multiplicity.One;
	private ASTNode[] subElements;
	private Function code;
	private Pattern associatedDeclarationNamePattern;
	private String property;
	private EnumSet<Flag> flags;

	public boolean flagSet(Flag flag) { return flags != null && flags.contains(flag); }
	public Pattern stringRepresentationPattern() { return stringRepresentationPattern; }
	public Class<? extends ASTNode> requiredClass() { return requiredClass; }
	public Multiplicity multiplicity() { return multiplicity; }
	public String property() { return property; }

	@Override
	public ASTNode[] subElements() {
		return subElements != null ? subElements : EMPTY_EXPR_ARRAY;
	}

	@Override
	public void setSubElements(ASTNode[] elms) {
		subElements = elms;
	}

	@SuppressWarnings("unchecked")
	private void parse(String matchText) throws ParsingException {
		BufferedScanner scanner = new BufferedScanner(matchText);
		String entry = scanner.readIdent();
		if (scanner.peek() == ':')
			scanner.read();
		while (!scanner.reachedEOF()) {
			int start, end;
			switch (scanner.read()) {
			case '/':
				start = scanner.tell(); end = start;
				while (!scanner.reachedEOF() && scanner.read() != '/')
					end++;
				stringRepresentationPattern = Pattern.compile(scanner.readStringAt(start, end));
				break;
			case ',':
				break;
			case '.':
				while (scanner.peek() == '.')
					scanner.read();
				multiplicity = Multiplicity.Multiple;
				break;
			case '+':
				multiplicity = Multiplicity.AtLeastOne;
				break;
			case 8230: // ellipsis unicode, OSX likes to substitute this for three dots
				multiplicity = Multiplicity.Multiple;
				break;
			case '!':
				String tra = scanner.readString(scanner.bufferSize()-scanner.tell());
				if (!tra.startsWith("{"))
					tra = String.format("return %s;", tra);
				code = new SelfContainedScript(
					entry, String.format("func Transform(value) { %s }", tra),
					new Index()
				) {
					private static final long serialVersionUID = 1L;
					@Override
					public Collection<Script> includes(Index index, Object origin, int options) { return Arrays.asList(TRANSFORMATIONS); };
				}.findFunction("Transform");
				break;
			case '^':
				start = scanner.tell(); end = start;
				while (!scanner.reachedEOF() && scanner.read() != '^')
					end++;
				associatedDeclarationNamePattern = Pattern.compile(scanner.readStringAt(start, end));
				break;
			case '>':
				start = scanner.tell(); end = start;
				Loop: while (!scanner.reachedEOF())
					switch (scanner.peek()) {
					case ',': case '!': case '^':
						break Loop;
					default:
						scanner.read();
						end++;
					}
				property = scanner.readStringAt(start, end);
				break;
			case '[':
				start = scanner.tell(); end = start;
				while (!scanner.reachedEOF() && scanner.peek() != ']') {
					scanner.read();
					end++;
				}
				if (end > start) {
					String[] attribs = scanner.readStringAt(start, end).split(",");
					this.flags = EnumSet.noneOf(Flag.class);
					for (int i = 0; i < attribs.length; i++) {
						String a = attribs[i].trim();
						flags.add(Flag.valueOf(Character.toUpperCase(a.charAt(0))+a.substring(1)));
					}
				}
				break;
			default:
				scanner.unread();
				start = scanner.tell(); end = start;
				while (!scanner.reachedEOF() && scanner.peek() != ',') {
					scanner.read();
					end++;
				}
				String className = scanner.readStringAt(start, end);
				if (className.length() == 0) {
					scanner.read();
					continue;
				}
				String[] packageFormats = new String[] {
					"%s.parser.c4script.ast.%s",
					"%s.parser.%s",
					"%s.parser.c4script.%s",
					"%s.parser.c4script.ast.%sLiteral",
					"%s.parser.c4script.ast.%sStatement",
					"%s.parser.c4script.ast.%sDeclaration",
					"%s.parser.c4script.ast.Access%s"
				};
				for (String pkgFormat : packageFormats)
					try {
						requiredClass = (Class<? extends ASTNode>) ASTNode.class.getClassLoader().loadClass(String.format(pkgFormat, Core.PLUGIN_ID, className));
						if (ASTNode.class.isAssignableFrom(requiredClass))
							break;
					} catch (ClassNotFoundException e) {
						continue;
					}
				if (requiredClass == null)
					throw new ParsingException(String.format("AST class not found: %s", className));
			}
		}
		this.entryName = entry;
	}

	public Object transformSubstitution(Object substitution) {
		if (property() != null)
			try {
				if (substitution instanceof Object[]) {
					Object[] s = (Object[]) substitution;
					Object[] n = new Object[s.length];
					substitution = n;
					for (int i = 0; i < s.length; i++)
						n[i] = s[i].getClass().getMethod(property()).invoke(s[i]);
				} else
					substitution = substitution.getClass().getMethod(property()).invoke(substitution);
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (code != null)
			try {
				if (substitution instanceof Object[])
					substitution = Arrays.asList((Object[])substitution);
				substitution = code.invoke(code.new FunctionInvocation(new Object[] {substitution}, null));
				if (substitution instanceof List)
					substitution = ((List<?>)substitution).toArray(new Object[((List<?>) substitution).size()]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (substitution instanceof Object[]) {
			Object[] s = (Object[]) substitution;
			ASTNode[] r = new ASTNode[s.length];
			for (int i = 0; i < s.length; i++) {
				Object item = s[i];
				ASTNode node;
				if (item instanceof String) {
					if (subElements().length > 0)
						node = new CallDeclaration((String)item, subElements());
					else
						node = new AccessVar((String)item);
				} else if (item instanceof Long)
					node = new IntegerLiteral((long)item);
				else
					node = new GarbageStatement(defaulting(item, "<null>").toString(), 0);
				r[i] = node;
			}
			substitution = r;
		}
		return substitution;
	}

	public boolean satisfiedBy(ASTNode element) {
		RequiredClass: if (requiredClass != null) {
			// OC: references to definitions are not IDLiterals but AccessVars referring to proxy variables
			AccessVar av = as(element, AccessVar.class);
			if (requiredClass == IDLiteral.class) {
				if (av != null && av.declaration() instanceof ProxyVar)
					break RequiredClass;
			}
			else if (requiredClass == AccessVar.class && av != null && av.declaration instanceof ProxyVar)
				return false;
			if (!requiredClass.isInstance(element))
				return false;
		}
		if (stringRepresentationPattern != null) {
			IPlaceholderPatternMatchTarget target = as(element, IPlaceholderPatternMatchTarget.class);
			String patternMatchingText = target != null ? target.patternMatchingText() : element != null ? element.toString() : null;
			if (patternMatchingText == null || !stringRepresentationPattern.matcher(patternMatchingText).matches())
				return false;
		}
		if (associatedDeclarationNamePattern != null) {
			Declaration decl = associatedDeclaration(element);
			if (decl != null && associatedDeclarationNamePattern.matcher(decl.name()).matches())
				return true;
			return false;
		}
		return true;
	}

	protected Declaration associatedDeclaration(ASTNode element) {
		CallDeclaration call = as(element.parent(), CallDeclaration.class);
		if (call != null)
			return call.parmDefinitionForParmExpression(element);

		FunctionBody body = as(element, FunctionBody.class);
		if (body != null)
			return body.owningDeclaration();

		return null;
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("$");
		output.append(entryName);
		List<String> attribs = new ArrayList<String>(4);
		if (requiredClass != null)
			attribs.add(requiredClass.getSimpleName());
		if (stringRepresentationPattern != null)
			attribs.add("/"+stringRepresentationPattern.pattern()+"/");
		if (property != null)
			attribs.add('>'+property);
		if (code != null)
			attribs.add('!'+code.body().subElements()[0].subElements()[0].toString());
		if (multiplicity != Multiplicity.One)
			switch (multiplicity) {
			case Multiple:
				attribs.add("...");
				break;
			case AtLeastOne:
				attribs.add("+");
				break;
			default:
				break;
			}
		if (attribs.size() > 0) {
			output.append(":");
			output.append(StringUtil.blockString("", "", ",", attribs));
		}
		output.append('$');
		if (subElements != null)
			CallDeclaration.printParmString(output, subElements, depth);
	}

	public MatchingPlaceholder(String text) throws ParsingException {
		super(text);
		parse(text);
	}

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		return this; // so meta
	}

}