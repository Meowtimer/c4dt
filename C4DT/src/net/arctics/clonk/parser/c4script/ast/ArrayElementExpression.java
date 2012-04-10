package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;

public class ArrayElementExpression extends Value {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected ExprElm argument;

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		IType t = super.obtainType(context);
		if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
			return t;
		if (predecessorInSequence() != null) {
			ArrayType at = predecessorTypeAs(ArrayType.class, context);
			if (at != null)
				return at.typeForElementWithIndex(evaluateAtParseTime(argument, context));
		}
		return PrimitiveType.ANY;
	}

	public ArrayElementExpression(ExprElm argument) {
		super();
		this.argument = argument;
		assignParentToSubElements();
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append("["); //$NON-NLS-1$
		argument().print(output, depth+1);
		output.append("]"); //$NON-NLS-1$
	}

	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor != null;
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		IType type = predecessorType(parser);
		if (type != null && type != PrimitiveType.UNKNOWN && !type.containsAnyTypeOf(PrimitiveType.ARRAY, PrimitiveType.PROPLIST))
			parser.warningWithCode(ParserErrorCode.NotAnArrayOrProplist, predecessorInSequence());
		ExprElm arg = argument();
		if (arg != null)
			arg.reportErrors(parser);
		else
			parser.warningWithCode(ParserErrorCode.MissingExpression, this);
	}

	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {argument};
	}

	@Override
	public void setSubElements(ExprElm[] subElements) {
		argument = subElements[0];
	}

	@Override
	public boolean isModifiable(C4ScriptParser context) {
		return true;
	}

	public ExprElm argument() {
		return argument;
	}
	
	@Override
	public void assignment(ExprElm rightSide, DeclarationObtainmentContext context) {
		ArrayType arrayType = predecessorTypeAs(ArrayType.class, context);
		IType rightSideType = rightSide.obtainType(context);
		if (arrayType != null) {
			Object argEv = evaluateAtParseTime(argument, context);
			IType mutation;
			if (argEv instanceof Number) {
				mutation = arrayType.modifiedBySliceAssignment(
					argEv,
					((Number)argEv).intValue()+1,
					new ArrayType(rightSideType, rightSideType)
				);
			} else
				mutation = arrayType.unknownLength();
			context.storeTypeInformation(predecessorInSequence(), mutation); 
		}
	}

}