package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class DoubleLiteral extends NumberLiteral {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final double literal;
	
	public DoubleLiteral(double literal) {
		this.literal = literal;
	}
	
	@Override
	public Number literal() {
		return literal;
	}
	
	@Override
	public boolean literalsEqual(Literal<?> other) {
		if (other instanceof DoubleLiteral)
			return ((DoubleLiteral)other).literal == this.literal;
		else
			return super.literalsEqual(other);
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		if (!parser.script().engine().settings().supportsFloats)
			parser.error(ParserErrorCode.FloatNumbersNotSupported, this, C4ScriptParser.NO_THROW);
		super.reportErrors(parser);
	}

}
