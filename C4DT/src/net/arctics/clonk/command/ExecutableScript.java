package net.arctics.clonk.command;

import java.util.Collection;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Index;


public class ExecutableScript extends SelfContainedScript {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final Function main;

	public ExecutableScript(String name, String script, Index index) {
		super(name, script, index);
		main = this.findFunction("Main");
	}


	@Override
	public boolean gatherIncludes(Index contextIndex, Script origin, Collection<Script> set, int options) {
		set.add(Command.BASE);
		return super.gatherIncludes(contextIndex, origin, set, options);
	}

	public Function main() { return main; }

	public Object invoke(Object... args) {
		return main.invoke(null);
	}

}