package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.eq;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Problem;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.Utilities;

public class Directive extends Declaration implements Serializable, IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final Directive[] CANONICALS = map
		(DirectiveType.values(), Directive.class, new IConverter<DirectiveType, Directive>() {
		@Override
		public Directive convert(DirectiveType from) {
			return new Directive(from, "");
		};
	});

	public enum DirectiveType {
		STRICT,
		INCLUDE,
		APPENDTO;

		private final String lowerCase = name().toLowerCase();

		public static DirectiveType makeType(String arg) {
			for (final DirectiveType d : values())
				if (d.toString().equals(arg))
					return d;
			return null;
		}

		/**
		 * Return the name of the enum constant as lower-case string.
		 */
		@Override
		public String toString() {
			return lowerCase;
		}
	}

	private final DirectiveType type;
	private String content;
	private transient ID cachedID;

	public Directive(DirectiveType type, String content) {
		this.content = content;
		this.type = type;
	}

	public Directive(String type, String content) {
		this(DirectiveType.makeType(type),content);
	}

	/**
	 * @return the type
	 */
	public DirectiveType type() {
		return type;
	}

	/**
	 * @return the content
	 */
	public String contents() { return content; }

	@Override
	public String toString() {
		if (content != "" && content != null) { //$NON-NLS-1$
			if (type == DirectiveType.APPENDTO || type == DirectiveType.INCLUDE) {
				final Index index = this.index();
				final Definition d = index != null ? index.anyDefinitionWithID(this.contentAsID()) : null;
				if (d != null)
					return String.format("#%s %s (%s)", type.toString(), content, d.name());
			}
			return String.format("#%s %s", type.toString(), content); //$NON-NLS-1$
		}
		return "#" + type.toString(); //$NON-NLS-1$
	}

	@Override
	public String name() {
		return type.toString();
	}

	public ID contentAsID() {
		if (cachedID == null)
			cachedID = ID.get(this.contents());
		return cachedID;
	}

	public static String[] arrayOfDirectiveStrings() {
		final String[] result = new String[DirectiveType.values().length];
		for (final DirectiveType d : DirectiveType.values())
			result[d.ordinal()] = d.toString();
		return result;
	}

	public void validate(C4ScriptParser parser) throws ParsingException {
		switch (type()) {
		case APPENDTO:
			break; // don't create error marker when appending to unknown object
		case INCLUDE:
			if (contents() == null)
				parser.markers().error(parser, Problem.MissingDirectiveArgs, null, this, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, this.toString());
			else {
				final ID id = contentAsID();
				final Definition obj = parser.script().index().definitionNearestTo(parser.script().resource(), id);
				if (obj == null)
					parser.markers().error(parser, Problem.UndeclaredIdentifier, null, this, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, contents());
			}
			break;
		default:
			break;
		}
	}

	@Override
	public boolean matchedBy(Matcher matcher) {
		if (matcher.reset(type().name()).lookingAt() || matcher.reset("#"+type().name()).lookingAt())
			return true;
		return contents() != null && matcher.reset(contents()).lookingAt();
	}

	public boolean refersTo(Definition definition) {
		switch (type) {
		case APPENDTO: case INCLUDE:
			final ID id = contentAsID();
			return Utilities.eq(id, definition.id());
		default:
			return false;
		}
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("#"+type().toString());
		if (contents() != null) {
			output.append(" ");
			output.append(contents());
		}
	}

	@Override
	public boolean equalAttributes(ASTNode other) {
		final Directive d = (Directive) other;
		if (eq(d.content, this.content))
			return true;
		return false;
	}

	@Override
	public String patternMatchingText() { return type().toString(); }

	@Override
	public ASTNode[] subElements() {
		try {
			switch (type()) {
			case APPENDTO: case INCLUDE:
				return new ASTNode[] { new IDLiteral(contentAsID()) };
			case STRICT:
				return new ASTNode[] { new IntegerLiteral(contents() != null ? Long.parseLong(contents()) : engine().settings().strictDefaultLevel) };
			default:
				return null;
			}
		} catch (final Exception e) {
			// ignore those feeble attempts
			return super.subElements();
		}
	}

	@Override
	public void setSubElements(ASTNode[] elms) {
		content = elms[0].printed();
		cachedID = null;
	}
}
