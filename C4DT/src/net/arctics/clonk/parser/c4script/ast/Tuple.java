package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;

public class Tuple extends Sequence {

	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public Tuple(ExprElm[] elms) {
		super(elms);		
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append('(');
		if (elements != null) {
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] != null)
					elements[i].print(output, depth+1);
				if (i < elements.length-1)
					output.append(", "); //$NON-NLS-1$
			}
		}
		output.append(')');
	}

}