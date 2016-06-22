package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Function;

/** A function which is anonymous */
public class AnonymousFunction extends Function {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public AnonymousFunction(String name) {
		this.setName(ANONYMOUS_NAME);
	}
}
