package net.arctics.clonk.c4script.ast;


import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;

public class IntegerLiteral extends NumberLiteral {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * A NumberLiteral representing '0'
	 */
	public static final IntegerLiteral ZERO = new IntegerLiteral(0);
	
	private final boolean hex;
	private final long literal;

	public IntegerLiteral(long value, boolean hex) {
		this.literal = value;
		this.hex = hex;
	}
	
	public IntegerLiteral(long value) {
		this.literal = value;
		this.hex = false;
	}

	public long longValue() {
		return literal;
	}
	
	@Override
	public Long literal() {
		return literal;
	}
	
	@Override
	public boolean literalsEqual(Literal<?> other) {
		if (other instanceof IntegerLiteral)
			return ((IntegerLiteral)other).literal == this.literal;
		else
			return super.literalsEqual(other);
	}
	
	public int intValue() {
		return (int)literal;
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		if (hex) {
			output.append("0x"); //$NON-NLS-1$
			output.append(Long.toHexString(longValue()).toUpperCase());
		}
		else
			super.doPrint(output, depth);
	}

	/**
	 * Literal is expressed as hexadecimal value
	 * @return That.
	 */
	public boolean isHex() {
		return hex;
	}

}
