package net.arctics.clonk.command;

import java.util.Arrays;
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
	public Collection<Script> includes(Index index, Object origin, int options) {
		return Arrays.asList(Command.COMMAND_BASESCRIPT);
	}

	public Function main() {
		return main;
	}

	public Object invoke(Object... args) {
		return main.invoke(null);
	}

}