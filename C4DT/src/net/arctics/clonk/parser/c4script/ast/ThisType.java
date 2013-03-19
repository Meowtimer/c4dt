package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;

public class ThisType extends TypeChoice {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public ThisType(Script script) {
		super(
			script,
			script instanceof Definition ? ((Definition)script).metaDefinition() : PrimitiveType.ID
		);
	}
	@Override
	public String typeName(boolean special) { return left().typeName(special); }
	public Script script() { return (Script)left(); }
}
