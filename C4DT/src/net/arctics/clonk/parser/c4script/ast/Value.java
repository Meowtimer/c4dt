package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;

public abstract class Value extends ExprElm {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	protected IType obtainType(C4ScriptParser context) {
		return context.queryTypeOfExpression(this, C4Type.ANY);
	}
	
	@Override
	public boolean isValidAtEndOfSequence(C4ScriptParser context) {
		return true;
	}

}