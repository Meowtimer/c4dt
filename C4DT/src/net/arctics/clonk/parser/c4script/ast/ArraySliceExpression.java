package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public class ArraySliceExpression extends ExprElm {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ExprElm lo, hi;
	
	public ArraySliceExpression(ExprElm lo, ExprElm hi) {
		this.lo = lo;
		this.hi = hi;
		assignParentToSubElements();
	}
	
	public ExprElm lo() {
		return lo;
	}
	
	public ExprElm hi() {
		return hi;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append("["); //$NON-NLS-1$
		if (lo != null)
			lo.print(output, depth+1);
		output.append(":"); //$NON-NLS-1$
		if (hi != null)
			hi.print(output, depth+1);
		output.append("]"); //$NON-NLS-1$
	}
	
	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {lo, hi};
	}
	@Override
	public void setSubElements(ExprElm[] subElements) {
		lo  = subElements[0];
		hi = subElements[1];
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		IType type = predecessorType(parser);
		if (type != null && type != PrimitiveType.UNKNOWN && !(type.intersects(PrimitiveType.ARRAY) || type.intersects(PrimitiveType.PROPLIST)))
			parser.warning(ParserErrorCode.NotAnArrayOrProplist, predecessorInSequence(), 0);
	}
	
	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		ArrayType arrayType = predecessorTypeAs(ArrayType.class, context);
		if (arrayType != null)
			return lo == null && hi == null ? arrayType : arrayType.typeForSlice(
				evaluateAtParseTime(lo, context),
				evaluateAtParseTime(hi, context)
			);
		else
			return PrimitiveType.ARRAY;
	}

	@Override
	public void assignment(ExprElm rightSide, C4ScriptParser context) {
		ArrayType arrayType = predecessorTypeAs(ArrayType.class, context);
		IType sliceType = rightSide.obtainType(context);
		if (arrayType != null)
			context.storeType(predecessorInSequence(), arrayType.modifiedBySliceAssignment(
				evaluateAtParseTime(lo, context),
				evaluateAtParseTime(hi, context),
				sliceType
			));
	}
	
	@Override
	public boolean isModifiable(C4ScriptParser context) {
		return true;
	}
	
	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor != null;
	}
	
}