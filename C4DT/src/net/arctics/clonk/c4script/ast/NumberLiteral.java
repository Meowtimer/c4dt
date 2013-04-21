package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;

/**
 * A number literal representing either a float literal expressed as {@link Double} or an int literal expressed as {@link Long}.
 * @author madeen
 *
 */
public abstract class NumberLiteral extends Literal<Number> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static ASTNode from(Number parsedNumber) {
		if (parsedNumber instanceof Double)
			return new FloatLiteral(parsedNumber.doubleValue());
		else
			return new IntegerLiteral(parsedNumber.longValue());
	}

}