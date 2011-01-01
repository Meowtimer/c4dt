package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class Ellipsis extends ExprElm {


	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

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

}