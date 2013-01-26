package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;

public class FloatLiteral extends NumberLiteral {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final double literal;
	
	public FloatLiteral(double literal) {
		this.literal = literal;
	}
	
	@Override
	public Number literal() {
		return literal;
	}
	
	@Override
	public boolean literalsEqual(Literal<?> other) {
		if (other instanceof FloatLiteral)
			return ((FloatLiteral)other).literal == this.literal;
		else
			return super.literalsEqual(other);
	}

}
