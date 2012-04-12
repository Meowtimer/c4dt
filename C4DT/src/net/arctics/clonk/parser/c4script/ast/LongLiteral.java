package net.arctics.clonk.parser.c4script.ast;


import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public class LongLiteral extends NumberLiteral {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * A NumberLiteral representing '0'
	 */
	public static final LongLiteral ZERO = new LongLiteral(0);
	
	private final boolean hex;
	private long literal;

	public LongLiteral(long value, boolean hex) {
		this.literal = value;
		this.hex = hex;
	}
	
	public LongLiteral(long value) {
		this.literal = value;
		this.hex = false;
	}

	public long longValue() {
		return literal;
	}
	
	public Long literal() {
		return literal;
	}
	
	@Override
	public boolean literalsEqual(Literal<?> other) {
		if (other instanceof LongLiteral)
			return ((LongLiteral)other).literal == this.literal;
		else
			return super.literalsEqual(other);
	}
	
	public int intValue() {
		return (int)literal;
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		if (hex) {
			output.append("0x"); //$NON-NLS-1$
			output.append(Long.toHexString(longValue()).toUpperCase());
		}
		else
			super.doPrint(output, depth);
	}

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		/*if (literal == 0)
			return PrimitiveType.ANY;
		else*/
			return PrimitiveType.INT;
	}

	/**
	 * Literal is expressed as hexadecimal value
	 * @return That.
	 */
	public boolean isHex() {
		return hex;
	}

}
