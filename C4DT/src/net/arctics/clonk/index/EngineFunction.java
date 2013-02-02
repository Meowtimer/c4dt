package net.arctics.clonk.index;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;

public class EngineFunction extends Function {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public EngineFunction(String name, IType returnType) { super(name, returnType); }
	public EngineFunction(String name, FunctionScope scope) { super(name, scope); }
	public EngineFunction() { super(); }
	@Override
	public Function inheritedFunction() { return null; }
	@Override
	public String obtainUserDescription() { return engine().obtainDescription(this); }
	@Override
	public boolean staticallyTyped() { return true; }
}
