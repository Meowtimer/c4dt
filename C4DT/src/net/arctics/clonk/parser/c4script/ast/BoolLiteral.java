package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public abstract class BoolLiteral extends Literal<Boolean> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public abstract boolean booleanValue();
	
	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		return PrimitiveType.BOOL;
	}
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		if (parent() instanceof BinaryOp) {
			Operator op = ((BinaryOp) parent()).operator();
			if (op == Operator.And || op == Operator.Or)
				parser.warning(ParserErrorCode.BoolLiteralAsOpArg, this, 0, this.toString());
		}
	}
}