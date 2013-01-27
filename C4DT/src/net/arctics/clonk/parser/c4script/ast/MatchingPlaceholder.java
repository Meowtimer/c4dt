package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;

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
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
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
		while (!scanner.reachedEOF())
			switch (scanner.read()) {
			case '/': {
				int start = scanner.tell();
				int end = start;
				while (!scanner.reachedEOF() && scanner.read() != '/')
					end++;
				stringRepresentationPattern = Pattern.compile(scanner.readStringAt(start, end));
				break;
			}
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
				code = new SelfContainedScript(entry, String.format("func Transform(value) { return %s; }",
					scanner.readString(scanner.bufferSize()-scanner.tell())), new Index())
					{
						private static final long serialVersionUID = 1L;
						@Override
						public Collection<? extends IHasIncludes> includes(Index index, IHasIncludes origin, int options) {
							return Arrays.asList(TRANSFORMATIONS);
						};
					}.findFunction("Transform");
				break;
			case '^': {
				int start = scanner.tell();
				int end = start;
				while (!scanner.reachedEOF() && scanner.read() != '^')
					end++;
				associatedDeclarationNamePattern = Pattern.compile(scanner.readStringAt(start, end));
				break;
			}
			case '>': {
				int start = scanner.tell();
				int end = start;
				while (!scanner.reachedEOF() && scanner.peek() != ',') {
					scanner.read();
					end++;
				}
				property = scanner.readStringAt(start, end);
				break;
			}
			case '[':
				int end = start;
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
				String className = scanner.readIdent();
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
						break;
					} catch (ClassNotFoundException e) {
						continue;
					}
				if (requiredClass == null)
					throw new ParsingException(String.format("AST class not found: %s", className));
			}
		this.entryName = entry;
	}

	@SuppressWarnings("unchecked")
	public Object transformSubstitution(Object substitution) {
		if (property() != null)
			try {
				substitution = substitution.getClass().getMethod(property()).invoke(substitution);
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (code != null)
			try {
				if (substitution instanceof ASTNode[])
					substitution = Arrays.asList((ASTNode[])substitution);
				substitution = code.invoke(substitution);
				if (substitution instanceof List)
					return ((List<ASTNode>)substitution).toArray(new ASTNode[((List<ASTNode>) substitution).size()]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (substitution instanceof String)
			substitution = new AccessVar((String)substitution);
		return substitution;
	}

	public boolean satisfiedBy(ASTNode element) {
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
}