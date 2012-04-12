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

public class LongLiteral extends NumberLiteral {
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
		public boolean containsType(IType type) {
			return false; // my number of possible values is really small
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
	public static final LongLiteral ZERO = new LongLiteral(0);
	
	/**
	 * Special type for '0' values. Used to make object and 0 compatible while avoiding general compatibility between numbers and object values.
	 */
	public static final IType ZERO_TYPE = new ZeroType();
	
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
		if (literal == 0 && context.containingScript().engine().settings().treatZeroAsAny)
			return ZERO_TYPE;
		else
			return PrimitiveType.INT;
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
