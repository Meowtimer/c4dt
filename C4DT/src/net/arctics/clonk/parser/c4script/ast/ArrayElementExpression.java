package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public class ArrayElementExpression extends ExprElm {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected ExprElm argument;

	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		IType t = super.unresolvedType(context);
		if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
			return t;
		if (predecessorInSequence() != null)
			for (IType ty : predecessorInSequence().type(context)) {
				ArrayType at = as(ty, ArrayType.class);
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
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		super.reportProblems(parser);
		IType type = predecessorType(parser);
		if (type == null)
			type = PrimitiveType.UNKNOWN;
		ExprElm arg = argument();
		if (arg == null)
			parser.warning(ParserErrorCode.MissingExpression, this, 0);
		else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
			IType argType = arg.type(parser);
			if (argType == PrimitiveType.STRING) {
				if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
					parser.warning(ParserErrorCode.NotAProplist, predecessorInSequence(), 0);
				else
					predecessorInSequence().typingJudgement(PrimitiveType.PROPLIST, parser);
			}
			else if (argType == PrimitiveType.INT)
				if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
					parser.warning(ParserErrorCode.NotAnArrayOrProplist, predecessorInSequence(), 0);
				else
					predecessorInSequence().typingJudgement(PrimitiveType.ARRAY, parser);
		}
	}

	public static void warnIfNotArray(ExprElm elm, C4ScriptParser parser, IType type) {
		if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
			TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
			TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
		{
			TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type);
			TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type);
			parser.warning(ParserErrorCode.NotAnArrayOrProplist, elm, 0);
		}
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
	public void assignment(ExprElm rightSide, C4ScriptParser context) {
		IType predType_ = predecessorType(context);
		for (IType predType : predType_) {
			ArrayType arrayType = as(predType, ArrayType.class);
			IType rightSideType = rightSide.type(context);
			if (arrayType != null) {
				Object argEv = evaluateAtParseTime(argument, context);
				IType mutation;
				if (argEv instanceof Number)
					mutation = arrayType.modifiedBySliceAssignment(
						argEv,
						((Number)argEv).intValue()+1,
						new ArrayType(rightSideType, rightSideType)
					);
				else
					mutation = new ArrayType(
						TypeUnification.unify(rightSideType, arrayType.generalElementType()),
						ArrayType.NO_PRESUMED_LENGTH
					);
				context.storeType(predecessorInSequence(), mutation);
				break;
			} else if (predType == PrimitiveType.UNKNOWN || predType == PrimitiveType.ARRAY)
				predecessorInSequence().typingJudgement(
					new ArrayType(rightSideType, ArrayType.NO_PRESUMED_LENGTH),
					context,
					TypingJudgementMode.Force
				);
		}
	}

}