package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.map;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ast.ASTNodePrinter;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.Utilities;

public class Directive extends Declaration implements Serializable {

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
			for (DirectiveType d : values())
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
	private final String content;
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
	public String contents() {
		return content;
	}

	@Override
	public String toString() {
		if (content != "" && content != null) { //$NON-NLS-1$
			if (type == DirectiveType.APPENDTO || type == DirectiveType.INCLUDE) {
				Definition d = this.index().anyDefinitionWithID(this.contentAsID());
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
		String[] result = new String[DirectiveType.values().length];
		for (DirectiveType d : DirectiveType.values())
			result[d.ordinal()] = d.toString();
		return result;
	}

	public void validate(C4ScriptParser parser) throws ParsingException {
		switch (type()) {
		case APPENDTO:
			break; // don't create error marker when appending to unknown object
		case INCLUDE:
			if (contents() == null)
				parser.error(ParserErrorCode.MissingDirectiveArgs, this, C4ScriptParser.NO_THROW, this.toString());
			else {
				ID id = contentAsID();
				Definition obj = parser.script().index().definitionNearestTo(parser.script().resource(), id);
				if (obj == null)
					parser.error(ParserErrorCode.UndeclaredIdentifier, this, C4ScriptParser.NO_THROW, contents());
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
			ID id = contentAsID();
			return Utilities.objectsEqual(id, definition.id());
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

}
