package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;

public class MissingStatement extends Statement {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public MissingStatement(int start) {
		super();
		setLocation(start, start+1);
	}
}
