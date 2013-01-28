package net.arctics.clonk.command;

import java.io.UnsupportedEncodingException;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference;

import org.eclipse.core.resources.IStorage;

public class SelfContainedScript extends Script {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected String script;
	
	public SelfContainedScript(String name, String script, Index index) {
		super(index);
		setName(name);
		this.script = script;
		C4ScriptParser parser = new C4ScriptParser(script, this, null);
		try {
			parser.parse();
			new DabbleInference().localTypingContext(parser).reportProblems();
		} catch (ParsingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String scriptText() {
		return script;
	}

	@Override
	public IStorage scriptStorage() {
		try {
			return new SimpleScriptStorage(name(), script);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

}