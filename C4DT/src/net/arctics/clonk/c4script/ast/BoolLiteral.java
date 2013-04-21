package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;

public abstract class BoolLiteral extends Literal<Boolean> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public abstract boolean booleanValue();
}