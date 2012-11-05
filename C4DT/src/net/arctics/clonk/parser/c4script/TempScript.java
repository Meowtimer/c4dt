package net.arctics.clonk.parser.c4script;

import java.io.UnsupportedEncodingException;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.EngineSettings;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.SimpleScriptStorage;

import org.eclipse.core.resources.IStorage;

public final class TempScript extends Script {
	private final String expression;
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public TempScript(String expression) {
		super(new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			private final Engine tempEngine = new Engine("Temp Engine") { //$NON-NLS-1$
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
				private final EngineSettings tempSettings = new EngineSettings();
				{
					tempSettings.strictDefaultLevel = 2;
				}
				@Override
				public EngineSettings settings() {
					return tempSettings;
				}
			};
			@Override
			public Engine engine() {
				return tempEngine;
			};
		});
		this.expression = expression;
	}

	@Override
	public IStorage scriptStorage() {
		try {
			return new SimpleScriptStorage(expression, expression);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
}