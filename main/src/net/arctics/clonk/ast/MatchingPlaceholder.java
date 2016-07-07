package net.arctics.clonk.ast;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static net.arctics.clonk.util.ArrayUtil.map;
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
import java.util.stream.Stream;

import org.eclipse.core.resources.IStorage;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.builder.CodeConverter.ICodeConverterContext;
import net.arctics.clonk.c4script.Conf;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.GarbageStatement;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.evaluate.Constant;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.CommandFunction;
import net.arctics.clonk.command.SelfContainedScript;
import net.arctics.clonk.index.Definition.ProxyVar;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.util.ScriptAccessibles;
import net.arctics.clonk.util.ScriptAccessibles.Callable;
import net.arctics.clonk.util.SelfcontainedStorage;
import net.arctics.clonk.util.StringUtil;

public class MatchingPlaceholder extends Placeholder {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static class Transformations extends Script {

		protected Transformations(final Index index) {
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
		public static List<ASTNode> reverse(final Object context, final List<ASTNode> input) {
			final ArrayList<ASTNode> list = new ArrayList<ASTNode>(input);
			Collections.reverse(list);
			return list;
		}

		@CommandFunction
		public static Object concat(final Object context, final Object a, final Object b) {
			return a.toString()+b.toString();
		}

		@CommandFunction
		public static Object substring(final Object context, final String str, final Long index, final Long length) {
			if (length != null) {
				return str.substring(index.intValue(), length.intValue());
			} else {
				return str.substring(index.intValue());
			}
		}

		@CommandFunction
		public static Object eval(final IEvaluationContext context, final ASTNode node) {
			try {
				return node.evaluate(context);
			} catch (final ControlFlowException e) {
				e.printStackTrace();
				return null;
			}
		}

		@CommandFunction
		public static String replace(final IEvaluationContext context, final Object text, final Object what, final Object replacement) {
			return text.toString().replace(what.toString(), replacement.toString());
		}

		@CommandFunction
		public static String quote(final IEvaluationContext context, final Object text) {
			return String.format("\"%s\"", text);
		}

		@CommandFunction
		public static void FunctionUsesVarArray(final IEvaluationContext context, ASTNode node) {
			final Function fn = node.parent(Function.class);
			final ICodeConverterContext ctx = as(context.self(), ICodeConverterContext.class);
			if (fn != null && ctx != null) {
				ctx.functionUsesVarArray(fn);
			}
		}

		@CommandFunction
		public static ASTNode VarArrayVar(final IEvaluationContext context, ASTNode node, ASTNode index) {
			final Function fn = node.parent(Function.class);
			final ICodeConverterContext ctx = as(context.self(), ICodeConverterContext.class);
			if (fn != null && ctx != null) {
				ctx.functionUsesVarArray(fn);
				return new Sequence(
					new AccessVar(ICodeConverterContext.VAR_ARRAY_NAME),
					new ArrayElementExpression(index)
				);
			} else {
				return null;
			}
		}

		@CommandFunction
		public static String EnforceLocal(final IEvaluationContext context, final String name, ASTNode node) {
			final Function fn = node.parent(Function.class);
			final ICodeConverterContext ctx = as(context.self(), ICodeConverterContext.class);
			return fn != null && ctx != null ? ctx.defineFunctionVariable(fn.name(), name) : null;
		}

		@CommandFunction
		public static IType Type(final IEvaluationContext context, final ASTNode node) {
			final Function fn = node.parent(Function.class);
			if (fn == null) {
				return null;
			}
			final Script script = fn.parent(Script.class);
			if (script == null) {
				return null;
			}
			final IType ty = script.typings().get(node);
			return ty;
		}

		@CommandFunction
		public static CallDeclaration Call(final IEvaluationContext context, final String name, final ASTNode[] params) {
			return new CallDeclaration(name, params);
		}

		@CommandFunction
		public static StringLiteral String(final IEvaluationContext context, final String value) {
			return new StringLiteral(value);
		}

		@CommandFunction
		public static AccessVar Var(final IEvaluationContext context, final String name) {
			return new AccessVar(name);
		}

		@CommandFunction
		public static MemberOperator MemberOperator(final IEvaluationContext context, final boolean dot, final boolean tilde, final ID id) {
			return new MemberOperator(dot, tilde, id, 0);
		}

		@Override
		public IVariable variable(final AccessDeclaration access, final Object obj) {
			if (obj instanceof MatchingPlaceholder || obj instanceof ICodeConverterContext) {
				final Class<? extends ASTNode> cls = findClass(access.name());
				return new Constant(cls);
			} else {
				return null;
			}
		}

		private static Map<String, Class<?extends ASTNode>> classCache = new HashMap<>();

		@SuppressWarnings("unchecked")
		public static Class<? extends ASTNode> findClass(final String className) {
			synchronized (classCache) {
				final Class<? extends ASTNode> existing = classCache.get(className);
				if (existing != null) {
					return existing;
				}
			}
			final Map<String, Class<? extends ASTNode>> shortcuts = map(false,
				"func", Function.class,
				"var", Variable.class,
				"proplist", PropListExpression.class
			);
			final Class<? extends ASTNode> scls = shortcuts.get(className);
			if (scls != null) {
				return scls;
			}
			final String[] packageFormats = new String[] {
				"%s.c4script.ast.%s",
				"%s.parser.%s",
				"%s.c4script.%s",
				"%s.c4script.ast.%sLiteral",
				"%s.c4script.ast.%sStatement",
				"%s.c4script.ast.%sDeclaration",
				"%s.c4script.ast.Access%s",
				"%s.c4script.typing.%s",
				"%s.index.%s",
				"%s.ast.%s",
				"%s.%s"
			};
			final Class<?extends ASTNode> result = stream(packageFormats)
				.map(pkgFormat -> {
					try {
						return (Class<? extends ASTNode>) ASTNode.class.getClassLoader().loadClass(String.format(pkgFormat, Core.PLUGIN_ID, className));
					} catch (final ClassNotFoundException e) {
						return null;
					}
				})
				.filter(cls -> cls != null && ASTNode.class.isAssignableFrom(cls))
				.findFirst()
				.orElse(null);
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

	public interface Multiplicity {
		boolean acceptable(int multiplicity);
		Multiplicity One = m -> m == 1;
		Multiplicity AtLeastOne = m -> m >= 1;
		Multiplicity Multiple = m -> true;
		default Integer absolute() { return this == One ? 1 : null; }
		default String printed() {
			if (this == One) {
				return "#1";
			} else if (this == AtLeastOne) {
				return "+";
			} else if (this == Multiple) {
				return "...";
			} else {
				return "???";
			}
		}
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

	private MatchingPlaceholder proto, sub;

	public boolean flagSet(final Flag flag) { return flags != null && flags.contains(flag); }

	public Pattern stringRepresentationPattern() { return stringRepresentationPattern; }

	public Class<? extends ASTNode> requiredClass() { return requiredClass; }

	public Multiplicity multiplicity() { return multiplicity; }

	public String property() { return property; }

	public MatchingPlaceholder proto() { return proto; }

	public void proto(MatchingPlaceholder value) {
		proto = value;
		if (value != null) {
			this.requiredClass = value.requiredClass;
			this.stringRepresentationPattern = value.stringRepresentationPattern;
			this.associatedDeclarationNamePattern = value.associatedDeclarationNamePattern;
			this.code = value.code;
			this.negated = value.negated;
			this.subElements = value.subElements;
		}
	}

	public MatchingPlaceholder sub() { return sub; }

	public void sub(MatchingPlaceholder sub) { this.sub = sub; }

	@Override
	public ASTNode[] subElements() {
		return subElements != null ? subElements : EMPTY_EXPR_ARRAY;
	}

	@Override
	public void setSubElements(final ASTNode[] elms) {
		subElements = elms;
	}

	private void parse(final Placeholder original) throws ProblemException {
		final String matchText = original.entryName();
		final BufferedScanner scanner = new BufferedScanner(matchText);
		final String entry = scanner.readIdent();
		if (scanner.peek() == ':') {
			scanner.read();
		}
		while (!scanner.reachedEOF()) {
			int start, end;
			switch (scanner.read()) {
			case '~':
				negated = true;
				break;
			case '/':
				start = scanner.tell(); end = start;
				while (!scanner.reachedEOF() && scanner.read() != '/') {
					end++;
				}
				stringRepresentationPattern = Pattern.compile(scanner.readStringAt(start, end));
				break;
			case ',':
				break;
			case '.':
				while (scanner.peek() == '.') {
					scanner.read();
				}
				multiplicity = Multiplicity.Multiple;
				break;
			case '+':
				multiplicity = Multiplicity.AtLeastOne;
				break;
			case 8230: // ellipsis unicode, OSX likes to substitute this for three dots
				multiplicity = Multiplicity.Multiple;
				break;
			case '#':
			{
				start = scanner.tell(); end = start;
				while (!scanner.reachedEOF() && Character.isDigit(scanner.peek())) {
					scanner.read();
					end++;
				}
				final int req_num = Integer.parseInt(scanner.readStringAt(start, end));
				multiplicity = makeMultiplicity(req_num);
				break;
			}
			case '!': case '?':
				String codeString = scanner.readString(scanner.bufferSize()-scanner.tell());
				if (!codeString.startsWith("{")) {
					codeString = String.format("return %s;", codeString);
				}
				setCode(original, entry, codeString);
				break;
			case '^':
				start = scanner.tell(); end = start;
				while (!scanner.reachedEOF() && scanner.read() != '^') {
					end++;
				}
				associatedDeclarationNamePattern = Pattern.compile(scanner.readStringAt(start, end));
				break;
			case '>':
				start = scanner.tell(); end = start;
				Loop: while (!scanner.reachedEOF()) {
					switch (scanner.peek()) {
					case ',': case '!': case '^':
						break Loop;
					default:
						scanner.read();
						end++;
					}
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
				Loop: while (!scanner.reachedEOF()) {
					switch (scanner.peek()) {
					case ',': case '!':
						break Loop;
					default:
						scanner.read();
						end++;
						break;
					}
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

	protected static Multiplicity makeMultiplicity(final int req_num) {
		return req_num == 1 ? Multiplicity.One : new Multiplicity() {
			@Override
			public Integer absolute() { return req_num; }
			@Override
			public boolean acceptable(int num) {
				return num <= req_num;
			}
			@Override
			public String printed() { return "#"+req_num; }
		};
	}

	private boolean setRequiredClass(final String className) throws ProblemException {
		final Class<? extends ASTNode> cls = Transformations.findClass(className);
		if (cls != null) {
			requiredClass = cls;
			return true;
		} else {
			System.out.println(String.format("AST class not found: %s", className));
			return false;
		}
	}

	private void setCode(final Placeholder original, final String entry, final String codeString) {
		final Index index = original.parent(Declaration.class).index();
		final Script transformations = new Transformations(index);
		code = new SelfContainedScript(
			entry, String.format("func Transform(value, placeholder) { %s }", codeString),
			index
		) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean gatherIncludes(final Index contextIndex, final Script origin, final Collection<Script> set, final int options) {
				if (!super.gatherIncludes(contextIndex, origin, set, options)) {
					return false;
				}
				set.add(transformations);
				return true;
			};
		}.findFunction("Transform");
	}

	public Object transformSubstitution(final Object substitution, final Object context) {
		final Object[] n = as(substitution, Object[].class);
		if (n == null) {
			return null;
		}

		Stream<Object> stream = stream(n);

		if (property != null) {
			stream = stream.map(v -> {
				try {
					final Callable getter = v == null ? null : ScriptAccessibles.getter(v.getClass(), property);
					return getter != null ? getter.invoke(v) : null;
				} catch (final Exception e) {
					System.out.println(format("Failed to get %s on %s of type %s", property, v, v.getClass()));
					return v;
				}
			});
		}

		if (code != null) {
			stream = stream.map(v -> {
				try {
					return code.invoke(code.new Invocation(new Object[] {v, this}, code.script(), context));
				} catch (final Exception e) {
					e.printStackTrace();
					return v;
				}
			});
		}

		return stream.map(this::pfirsichZitrone).toArray(l -> new ASTNode[l]);
	}

	private ASTNode pfirsichZitrone(Object item) {
		return
			item instanceof ASTNode ? (ASTNode)item :
			item instanceof String ?
				subElements().length > 0 ? new CallDeclaration((String)item, subElements()) :
				new AccessVar((String)item) :
			item instanceof Long ? new IntegerLiteral((long)item) :
			new GarbageStatement(defaulting(item, "<null>").toString(), 0);
	}


	public boolean satisfiedBy(final ASTNode element) {
		boolean r = internalSatisfied(element);
		if (negated) {
			r = !r;
		}
		return r;
	}

	private boolean internalSatisfied(final ASTNode element) {
		RequiredClass: if (requiredClass != null) {
			// OC: references to definitions are not IDLiterals but AccessVars referring to proxy variables
			final AccessVar av = as(element, AccessVar.class);
			if (requiredClass == IDLiteral.class) {
				if (av != null && av.declaration() instanceof ProxyVar) {
					break RequiredClass;
				}
			}
			else if (requiredClass == AccessVar.class && av != null && av.declaration() instanceof ProxyVar) {
				return false;
			}
			if (!requiredClass.isInstance(element)) {
				return false;
			}
		}
		if (stringRepresentationPattern != null) {
			final IPlaceholderPatternMatchTarget target = as(element, IPlaceholderPatternMatchTarget.class);
			final String patternMatchingText = target != null ? target.patternMatchingText() : element != null ? element.toString() : null;
			if (patternMatchingText == null || !stringRepresentationPattern.matcher(patternMatchingText).matches()) {
				return false;
			}
		}
		if (associatedDeclarationNamePattern != null) {
			final Declaration decl = associatedDeclaration(element);
			if (decl == null || !associatedDeclarationNamePattern.matcher(decl.name()).matches()) {
				return false;
			}
		}
		if (code != null) {
			try {
				final Object codeResult = code.invoke(code.new Invocation(new Object[] {element}, code.script(), this));
				if (!eq(Boolean.TRUE, codeResult)) {
					return false;
				}
			} catch (final Exception e) {
				//System.out.println(e.getMessage());
				code.invoke(code.new Invocation(new Object[] {element}, code.script(), this));
				return false;
			}
		}
		if (sub != null && !sub.satisfiedBy(element)) {
			return false;
		}
		return true;
	}

	protected Declaration associatedDeclaration(final ASTNode element) {
		final CallDeclaration call = as(element.parent(), CallDeclaration.class);
		if (call != null) {
			return call.parmDefinitionForParmExpression(element);
		}

		final FunctionBody body = as(element, FunctionBody.class);
		if (body != null) {
			return body.owner();
		}

		return null;
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append("$");
		output.append(entryName);
		final List<String> attribs = new ArrayList<String>(4);
		if (requiredClass != null) {
			attribs.add(requiredClass.getSimpleName());
		}
		if (stringRepresentationPattern != null) {
			attribs.add("/"+stringRepresentationPattern.pattern()+"/");
		}
		if (property != null) {
			attribs.add('>'+property);
		}
		if (code != null) {
			attribs.add('!'+code.body().subElements()[0].subElements()[0].toString());
		}
		if (multiplicity != Multiplicity.One) {
			attribs.add(multiplicity.printed());
		}
		if (attribs.size() > 0) {
			output.append(":");
			if (negated) {
				output.append('~');
			}
			output.append(StringUtil.blockString("", "", ",", attribs));
		}
		output.append('$');
		if (sub != null) {
			output.append('[');
			sub.print(output, depth);
			output.append(']');
		}
		if (subElements != null) {
			Conf.printNodeList(output, subElements, depth, "(", ")");
		}
	}

	protected MatchingPlaceholder() { super(""); }

	public MatchingPlaceholder(final Placeholder original) throws ProblemException {
		super(original.entryName());
		parse(original);
	}

	@Override
	public Object evaluate(final IEvaluationContext context) throws ControlFlowException {
		return this; // so meta
	}

	public boolean simple() {
		return
			requiredClass == null &&
			stringRepresentationPattern == null &&
			associatedDeclarationNamePattern == null &&
			code == null &&
			!negated &&
			(subElements == null || subElements.length == 0) &&
			proto == null;
	}

	public boolean simpleAndMultiplicityOne() {
		return multiplicity == Multiplicity.One && simple();
	}

	public boolean consistent(Object value) {
		final int mult = value instanceof Object[] ? ((Object[])value).length : 1;
		return multiplicity().absolute() == null || multiplicity().absolute() == mult;
	}

	@Override
	protected boolean equalAttributes(ASTNode other_) {
		final MatchingPlaceholder other = as(other_, MatchingPlaceholder.class);
		return (
			other != null &&
			super.equalAttributes(other) &&
			eq(this.associatedDeclarationNamePattern, other.associatedDeclarationNamePattern) &&
			eq(this.code, other.code) &&
			eq(this.negated, other.negated) &&
			eq(this.proto, other.proto) &&
			eq(this.requiredClass, other.requiredClass) &&
			eq(this.stringRepresentationPattern, other.stringRepresentationPattern) &&
			eq(this.subElements, other.subElements)
		);
	}

}