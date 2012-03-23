package net.arctics.clonk.parser.c4script.ast;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.SameTypeAsSomeTypeable;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.util.ArrayUtil;

/**
 * A number literal representing either a float literal expressed as {@link Double} or an int literal expressed as {@link Long}.
 * @author madeen
 *
 */
public final class NumberLiteral extends Literal<Number> {

	private static class ZeroType extends SameTypeAsSomeTypeable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		
		public ZeroType() {
			super(null);
		}

		@Override
		protected IType actualType() {
			return PrimitiveType.INT;
		}
		
		@Override
		public String typeName(boolean special) {
			if (special)
				return "zero";
			else
				return PrimitiveType.INT.typeName(false);
		}
		
		@Override
		public Iterator<IType> iterator() {
			return ArrayUtil.arrayIterable(this, PrimitiveType.INT, PrimitiveType.OBJECT, PrimitiveType.ID, PrimitiveType.ARRAY).iterator();
		}
	}
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * A NumberLiteral representing '0'
	 */
	public static final NumberLiteral ZERO = new NumberLiteral(0);
	
	/**
	 * Special type for '0' values. Used to make object and 0 compatible while avoiding general compatibility between numbers and object values.
	 */
	public static final IType ZERO_TYPE = new ZeroType();
	
	private final boolean hex;

	public NumberLiteral(Number value, boolean hex) {
		super(value);
		this.hex = hex;
	}
	
	public NumberLiteral(Number value) {
		super(value);
		this.hex = false;
	}

	public NumberLiteral(long value) {
		this(value, false);
	}

	public long longValue() {
		return literal.longValue();
	}
	
	public double doubleValue() {
		return literal.doubleValue();
	}

	public int intValue() {
		return literal.intValue();
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
		if (literal instanceof Long && longValue() == 0 && context.containingScript().engine().settings().treatZeroAsAny)
			return ZERO_TYPE;
		else {
			if (literal instanceof Long)
				return PrimitiveType.INT;
			else if (literal instanceof Double)
				return PrimitiveType.FLOAT;
			else
				return PrimitiveType.ANY;
		}
	}

	@Override
	public boolean canBeConvertedTo(IType otherType, C4ScriptParser context) {
		// 0 is the NULL object or NULL string
		return (obtainType(context) == ZERO_TYPE && (otherType.canBeAssignedFrom(TypeSet.STRING_OR_OBJECT))) || super.canBeConvertedTo(otherType, context);
	}

	/**
	 * Literal is expressed as hexadecimal value
	 * @return That.
	 */
	public boolean isHex() {
		return hex;
	}

}