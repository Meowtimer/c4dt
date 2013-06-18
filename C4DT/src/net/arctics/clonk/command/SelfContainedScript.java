package net.arctics.clonk.command;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.util.SelfcontainedStorage;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.NullProgressMonitor;

public class SelfContainedScript extends Script {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected String script;

	public SelfContainedScript(String name, String script, Index index) {
		super(index);
		setName(name);
		this.script = script;
		final ScriptParser parser = new ScriptParser(script, this, null);
		try {
			parser.parse();
			deriveInformation();
			new DabbleInference()
				.configure(index, "")
				.initialize(null, new NullProgressMonitor(), new Script[] {this})
				.run();
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
	}

	@Override
	public IStorage source() {
		return new SelfcontainedStorage(name(), script);
	}

}