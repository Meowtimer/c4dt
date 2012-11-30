package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public class Nil extends Literal<Object> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		return PrimitiveType.UNKNOWN;
	};
	@Override
	public Object literal() {
		return null;
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(Keywords.Nil);
	}
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		if (!parser.engine().settings().supportsNil)
			parser.error(ParserErrorCode.NotSupported, this, C4ScriptParser.NO_THROW, Keywords.Nil, parser.engine().name());
	}
}
