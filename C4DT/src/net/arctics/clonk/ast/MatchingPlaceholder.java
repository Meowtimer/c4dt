package net.arctics.clonk.ast;

import static java.lang.String.format;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.Conf;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.GarbageStatement;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.evaluate.Constant;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.CommandFunction;
import net.arctics.clonk.command.SelfContainedScript;
import net.arctics.clonk.index.Definition.ProxyVar;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.ui.editors.actions.c4script.CodeConverter;
import net.arctics.clonk.util.SelfcontainedStorage;
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
		public IStorage source() { return new SelfcontainedStorage("CommandBase", ""); } //$NON-NLS-1$ //$NON-NLS-2$
		@Override
		public String name() { return "CommandBaseScript"; } //$NON-NLS-1$
		@Override
		public String nodeName() { return name(); }
		@CommandFunction
		public static List<ASTNode> reverse(Object context, List<ASTNode> input) {
			final ArrayList<ASTNode> list = new ArrayList<ASTNode>(input);
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
			} catch (final ControlFlowException e) {
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
		@CommandFunction
		public static String var(IEvaluationContext context, String name) {
			if (context.self() instanceof CodeConverter.ICodeConverterContext)
				return ((CodeConverter.ICodeConverterContext)context.self()).var(name);
			else
				return null;
		}
		@CommandFunction
		public static CallDeclaration Call(IEvaluationContext context, String name, ASTNode[] params) {
			return new CallDeclaration(name, params);
		}
		@Override
		public IVariable variable(AccessVar access, Object obj) {
			final Class<? extends ASTNode> cls = findClass(access.name());
			return new Constant(cls);
		}
		private static Map<String, Class<?extends ASTNode>> classCache = new HashMap<>();
		@SuppressWarnings("unchecked")
		public static Class<? extends ASTNode> findClass(String className) {
			final String[] packageFormats = new String[] {
				"%s.ast.%s",
				"%s.c4script.ast.%s",
				"%s.parser.%s",
				"%s.c4script.%s",
				"%s.c4script.ast.%sLiteral",
				"%s.c4script.ast.%sStatement",
				"%s.c4script.ast.%sDeclaration",
				"%s.c4script.ast.Access%s",
				"%s.index.%s"
			};
			synchronized (classCache) {
				final Class<? extends ASTNode> existing = classCache.get(className);
				if (existing != null)
					return existing;
			}
			Class<?extends ASTNode> result = null;
			for (final String pkgFormat : packageFormats)
				try {
					result = (Class<? extends ASTNode>) ASTNode.class.getClassLoader().loadClass(String.format(pkgFormat, Core.PLUGIN_ID, className));
					if (ASTNode.class.isAssignableFrom(result))
						break;
				} catch (final ClassNotFoundException e) {
					continue;
				}
			synchronized (classCache) {
				classCache.put(className, result);
				return result;
			}
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

	private Class<? extends ASTNode> requiredClass;
	private Pattern stringRepresentationPattern;
	private Multiplicity multiplicity = Multiplicity.One;
	private ASTNode[] subElements;
	private Function code;
	private Pattern associatedDeclarationNamePattern;
	private String property;
	private EnumSet<Flag> flags;
	private boolean negated;

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

	private void parse(Placeholder original) throws ProblemException {
		final String matchText = original.entryName();
		final BufferedScanner scanner = new BufferedScanner(matchText);
		final String entry = scanner.readIdent();
		if (scanner.peek() == ':')
			scanner.read();
		while (!scanner.reachedEOF()) {
			int start, end;
			switch (scanner.read()) {
			case '~':
				negated = true;
				break;
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
				String codeString = scanner.readString(scanner.bufferSize()-scanner.tell());
				if (!codeString.startsWith("{"))
					codeString = String.format("return %s;", codeString);
				setCode(original, entry, codeString);
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
					final String[] attribs = scanner.readStringAt(start, end).split(",");
					this.flags = EnumSet.noneOf(Flag.class);
					for (int i = 0; i < attribs.length; i++) {
						final String a = attribs[i].trim();
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
				final String className = scanner.readStringAt(start, end);
				if (className.length() == 0) {
					scanner.read();
					continue;
				}
				setRequiredClass(className);
			}
		}
		this.entryName = entry;
	}
	private void setRequiredClass(final String className) throws ProblemException {
		requiredClass = Transformations.findClass(className);
		if (requiredClass == null)
			throw new ProblemException(String.format("AST class not found: %s", className));
	}
	private void setCode(Placeholder original, final String entry, String codeString) {
		final Index index = original.parent(Declaration.class).index();
		final Script transformations = new Transformations(index);
		code = new SelfContainedScript(
			entry, String.format("func Transform(value, placeholder) { %s }", codeString),
			index
		) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean gatherIncludes(Index contextIndex, Script origin, Collection<Script> set, int options) {
				if (!super.gatherIncludes(contextIndex, origin, set, options))
					return false;
				set.add(transformations);
				return true;
			};
		}.findFunction("Transform");
	}

	public Object transformSubstitution(Object substitution, Object context) {
		if (!(substitution instanceof Object[]))
			return null;
		final Object[] s = (Object[]) substitution;
		final Object[] n = new Object[s.length];
		System.arraycopy(s, 0, n, 0, s.length);

		if (property() != null)
			for (int i = 0; i < s.length; i++)
				try {
					n[i] = n[i] == null ? null : n[i].getClass().getMethod(property()).invoke(s[i]);
				} catch (final Exception e) {
					System.out.println(format("Failed to get %s on %s of type %s",
						property(), n[i], n[i].getClass()));
				}

		if (code != null)
			for (int i = 0; i < n.length; i++) try {
				n[i] = code.invoke(code.new FunctionInvocation(new Object[] {n[i], this}, code.script(), context));
			} catch (final Exception e) {
				e.printStackTrace();
			}

		final ASTNode[] r = new ASTNode[n.length];
		for (int i = 0; i < s.length; i++) {
			final Object item = n[i];
			ASTNode node;
			if (item instanceof ASTNode)
				node = (ASTNode)item;
			else if (item instanceof String) {
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
		return r;
	}

	public boolean satisfiedBy(ASTNode element) {
		boolean r = internalSatisfied(element);
		if (negated)
			r = !r;
		return r;
	}
	private boolean internalSatisfied(ASTNode element) {
		if (element == null)
			System.out.println("here");
		RequiredClass: if (requiredClass != null) {
			// OC: references to definitions are not IDLiterals but AccessVars referring to proxy variables
			final AccessVar av = as(element, AccessVar.class);
			if (requiredClass == IDLiteral.class) {
				if (av != null && av.declaration() instanceof ProxyVar)
					break RequiredClass;
			}
			else if (requiredClass == AccessVar.class && av != null && av.declaration() instanceof ProxyVar)
				return false;
			if (!requiredClass.isInstance(element))
				return false;
		}
		if (stringRepresentationPattern != null) {
			final IPlaceholderPatternMatchTarget target = as(element, IPlaceholderPatternMatchTarget.class);
			final String patternMatchingText = target != null ? target.patternMatchingText() : element != null ? element.toString() : null;
			if (patternMatchingText == null || !stringRepresentationPattern.matcher(patternMatchingText).matches())
				return false;
		}
		if (associatedDeclarationNamePattern != null) {
			final Declaration decl = associatedDeclaration(element);
			if (decl != null && associatedDeclarationNamePattern.matcher(decl.name()).matches())
				return true;
			return false;
		}
		if (code != null)
			if (!eq(Boolean.TRUE, code.invoke(code.new FunctionInvocation(new Object[] {element}, code.script(), this))))
				return false;
		return true;
	}

	protected Declaration associatedDeclaration(ASTNode element) {
		final CallDeclaration call = as(element.parent(), CallDeclaration.class);
		if (call != null)
			return call.parmDefinitionForParmExpression(element);

		final FunctionBody body = as(element, FunctionBody.class);
		if (body != null)
			return body.owner();

		return null;
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("$");
		output.append(entryName);
		final List<String> attribs = new ArrayList<String>(4);
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
			Conf.printNodeList(output, subElements, depth, "(", ")");
	}

	protected MatchingPlaceholder() { super(""); }

	public MatchingPlaceholder(Placeholder original) throws ProblemException {
		super(original.entryName());
		parse(original);
	}

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		return this; // so meta
	}

}