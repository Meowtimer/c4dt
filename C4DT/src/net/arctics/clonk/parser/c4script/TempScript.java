package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.SimpleScriptStorage;

import org.eclipse.core.resources.IStorage;

public final class TempScript extends Script {
	private final String expression;
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public TempScript(String expression, final Engine engine) {
		super(new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() { return engine; };
		});
		this.expression = expression;
	}

	@Override
	public IStorage source() { return new SimpleScriptStorage(expression, expression); }
}