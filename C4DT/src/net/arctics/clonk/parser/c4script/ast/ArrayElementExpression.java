package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;

public class ArrayElementExpression extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	protected ExprElm argument;

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		IType t = super.obtainType(context);
		if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
			return t;
		if (getPredecessorInSequence() != null) {
			t = getPredecessorInSequence().getType(context);
			if (t instanceof ArrayType)
				return ((ArrayType)t).getTypeForElementWithIndex(argument.evaluateAtParseTime(context));
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
		ExprElm predecessor = getPredecessorInSequence();
		if (predecessor != null) {
			IType type = predecessor.getType(parser);
			if (type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY && !type.containsAnyTypeOf(PrimitiveType.ARRAY, PrimitiveType.PROPLIST)) {
				parser.warningWithCode(ParserErrorCode.NotAnArrayOrProplist, predecessor);
			}
		}
		ExprElm arg = getArgument();
		if (arg != null)
			arg.reportErrors(parser);
	}

	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {argument};
	}

	@Override
	public void setSubElements(ExprElm[] subElements) {
		argument = subElements[0];
	}

	@Override
	public boolean modifiable(C4ScriptParser context) {
		return true;
	}

	public ExprElm getArgument() {
		return argument;
	}
	
	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		if (getPredecessorInSequence() != null) {
			IType t = getPredecessorInSequence().getType(parser);
			if (t instanceof ArrayType)
				return new DeclarationRegion(
					((ArrayType)t).getTypedDeclarationWrapperForElementWithIndex(argument.evaluateAtParseTime(parser), parser.getContainer().getIndex())
				);
		}
		return super.declarationAt(offset, parser);
	}

}