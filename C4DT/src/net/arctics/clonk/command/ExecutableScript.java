package net.arctics.clonk.command;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import org.eclipse.core.resources.IStorage;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class ExecutableScript extends ScriptBase {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private String script;
	private InvokableFunction main;
	private ClonkIndex index;

	@Override
	public ClonkIndex getIndex() {
		return index;
	}

	@Override
	public String getScriptText() {
		return script;
	}

	@Override
	public IStorage getScriptStorage() {
		try {
			return new SimpleScriptStorage(getName(), script);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public ExecutableScript(String name, String script, ClonkIndex index) {
		super();
		setName(name);
		this.script = script;
		this.index = index;
		C4ScriptParser parser = new C4ScriptParser(script, this, null) {
			@Override
			protected Function newFunction(String nameWillBe) {
				return new InvokableFunction();
			}
			@Override
			public void parseCodeOfFunction(Function function, boolean withNewContext) throws ParsingException {
				if (!(function instanceof InvokableFunction))
					return;
				if (function.getName().equals("Main")) { //$NON-NLS-1$
					main = (InvokableFunction)function;
				}
				super.parseCodeOfFunction(function, withNewContext);
			}
		};
		try {
			parser.parse();
		} catch (ParsingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Collection<IHasIncludes> getIncludes(ClonkIndex index, boolean recursive) {
		return Arrays.asList((IHasIncludes)Command.COMMAND_BASESCRIPT);
	}

	public InvokableFunction getMain() {
		return main;
	}

	public Object invoke(Object... args) {
		return main.invoke(args);
	}

}