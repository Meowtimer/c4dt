package net.arctics.clonk.parser.c4script.ast;

import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.SameTypeAsSomeTypeable;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.util.ArrayUtil;

public final class NumberLiteral extends Literal<Long> {

	public static class ZeroType extends SameTypeAsSomeTypeable {
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		
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
			return ArrayUtil.arrayIterable(this, PrimitiveType.INT, PrimitiveType.OBJECT, PrimitiveType.ID).iterator();
		}
	}
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static final NumberLiteral ZERO = new NumberLiteral(0);
	public static final ZeroType ZERO_TYPE = new ZeroType();
	
	private boolean hex;

	public NumberLiteral(long value, boolean hex) {
		super(Long.valueOf(value));
		this.hex = hex;
	}

	public NumberLiteral(long value) {
		this(value, false);
	}

	public long longValue() {
		return getLiteral().longValue();
	}

	public int intValue() {
		return (int)longValue();
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
		if (longValue() == 0 && context.getContainer().getEngine().getCurrentSettings().treatZeroAsAny)
			return ZERO_TYPE;
		else
			return PrimitiveType.INT;
	}

	@Override
	public boolean canBeConvertedTo(IType otherType, C4ScriptParser context) {
		// 0 is the NULL object or NULL string
		return (longValue() == 0 && (otherType.canBeAssignedFrom(TypeSet.STRING_OR_OBJECT))) || super.canBeConvertedTo(otherType, context);
	}

	public boolean isHex() {
		return hex;
	}

	public void setHex(boolean hex) {
		this.hex = hex;
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		long val = longValue();
		//ExprElm region;
		if (getParent() instanceof UnaryOp && ((UnaryOp)getParent()).getOperator() == Operator.Subtract) {
			val = -val;
			//region = getParent();
		}
		/*else
			region = this;
		/* who needs it -.-
		if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE)
			parser.warningWithCode(ParserErrorCode.OutOfIntRange, region, String.valueOf(val));
		*/
	}

}