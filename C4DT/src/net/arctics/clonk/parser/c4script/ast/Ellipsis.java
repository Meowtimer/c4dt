package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class Ellipsis extends ExprElm {


	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public Ellipsis() {
		super();
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append("..."); //$NON-NLS-1$
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		parser.unnamedParamaterUsed(this); // it's kinda sound...
	}
	
	@Override
	public boolean isValidAtEndOfSequence(C4ScriptParser context) {
		return false;
	}

}