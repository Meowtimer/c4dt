package net.arctics.clonk.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.SimpleScriptStorage;

import org.eclipse.core.resources.IStorage;

public final class TempScript extends Script {
	private final String text;
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public TempScript(String text, final Engine engine) {
		super(new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() { return engine; };
		});
		this.text = text;
	}

	@Override
	public IStorage source() { return new SimpleScriptStorage(text, text); }
}