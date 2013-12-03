package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.eq;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.Utilities;

public class Directive extends Declaration implements Serializable, IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final Directive[] CANONICALS = map
		(DirectiveType.values(), Directive.class, new IConverter<DirectiveType, Directive>() {
		@Override
		public Directive convert(final DirectiveType from) {
			return new Directive(from, "", 0);
		};
	});

	public enum DirectiveType {
		STRICT,
		INCLUDE,
		APPENDTO;

		private final String lowerCase = name().toLowerCase();

		public static DirectiveType makeType(final String arg) {
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
	private final int identifierStart;
	private transient ID cachedID;

	public Directive(final DirectiveType type, final String content, final int identifierStart) {
		this.content = content;
		this.type = type;
		this.identifierStart = identifierStart;
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

	public void validate(final ScriptParser parser) throws ProblemException {
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
	public boolean matchedBy(final Matcher matcher) {
		if (matcher.reset(type().name()).lookingAt() || matcher.reset("#"+type().name()).lookingAt())
			return true;
		return contents() != null && matcher.reset(contents()).lookingAt();
	}

	public boolean refersTo(final Definition definition) {
		switch (type) {
		case APPENDTO: case INCLUDE:
			final ID id = contentAsID();
			return Utilities.eq(id, definition.id());
		default:
			return false;
		}
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append("#"+type().toString());
		if (contents() != null) {
			output.append(" ");
			output.append(contents());
		}
	}

	@Override
	public boolean equalAttributes(final ASTNode other) {
		final Directive d = (Directive) other;
		return super.equalAttributes(other) && eq(d.content, this.content) && eq(d.type, this.type);
	}

	@Override
	public String patternMatchingText() { return type().toString(); }

	@Override
	public ASTNode[] subElements() {
		try {
			switch (type()) {
			case APPENDTO: case INCLUDE:
				final ID id = contentAsID();
				final IDLiteral lit = new IDLiteral(id);
				final int idPos = this.absolute().getOffset()+1+type().name().length()+1;
				lit.setLocation(idPos, idPos+id.length());
				return new ASTNode[] { tempSubElement(lit) };
			case STRICT:
				return new ASTNode[] { tempSubElement(new IntegerLiteral(contents() != null ? Long.parseLong(contents()) : engine().settings().strictDefaultLevel)) };
			default:
				return null;
			}
		} catch (final Exception e) {
			// ignore those feeble attempts
			return super.subElements();
		}
	}

	@Override
	public void setSubElements(final ASTNode[] elms) {
		content = elms[0].printed();
		cachedID = null;
	}

	@Override
	public int identifierStart() { return start()+identifierStart; }
	@Override
	public int identifierLength() { return content != null ? content.length() : null; }
}
