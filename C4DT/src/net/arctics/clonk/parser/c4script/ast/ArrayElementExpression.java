package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
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
			t = predecessorInSequence().typeInContext(context);
			if (t instanceof ArrayType)
				return ((ArrayType)t).typeForElementWithIndex(argument.evaluateAtParseTime(context));
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
		getArgument().print(output, depth+1);
		output.append("]"); //$NON-NLS-1$
	}

	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor != null;
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		ExprElm predecessor = predecessorInSequence();
		if (predecessor != null) {
			IType type = predecessor.typeInContext(parser);
			if (type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY && !type.containsAnyTypeOf(PrimitiveType.ARRAY, PrimitiveType.PROPLIST)) {
				parser.warningWithCode(ParserErrorCode.NotAnArrayOrProplist, predecessor);
			}
		}
		ExprElm arg = getArgument();
		if (arg != null)
			arg.reportErrors(parser);
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

	public ExprElm getArgument() {
		return argument;
	}
	
	protected ArrayType arrayType(DeclarationObtainmentContext context) {
		return predecessorInSequence() != null ? as(predecessorInSequence().obtainType(context), ArrayType.class) : null;
	}
	
	@Override
	public void assignment(ExprElm rightSide, DeclarationObtainmentContext context) {
		ArrayType arrayType = arrayType(context);
		IType rightSideType = rightSide.obtainType(context);
		if (arrayType != null) {
			Object argEv = evaluateAtParseTime(argument, context);
			if (argEv instanceof Number)
				context.storeTypeInformation(predecessorInSequence(), arrayType.modifiedBySliceAssignment(
					argEv,
					((Number)argEv).intValue()+1,
					new ArrayType(rightSideType, rightSideType)
				));
		}
	}

}