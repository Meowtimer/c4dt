package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptOperator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4TypeSet;
import net.arctics.clonk.parser.c4script.IType;

public final class NumberLiteral extends Literal<Long> {

	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static final NumberLiteral ZERO = new NumberLiteral(0);

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

	public IType getType(C4ScriptParser context) {
		if (longValue() == 0)
			return C4Type.ANY; // FIXME: to prevent warnings when assigning 0 to object-variables
		return C4Type.INT;
	}

	@Override
	public boolean canBeConvertedTo(IType otherType, C4ScriptParser context) {
		// 0 is the NULL object or NULL string
		return (longValue() == 0 && (otherType.canBeAssignedFrom(C4TypeSet.STRING_OR_OBJECT))) || super.canBeConvertedTo(otherType, context);
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
		if (getParent() instanceof UnaryOp && ((UnaryOp)getParent()).getOperator() == C4ScriptOperator.Subtract) {
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