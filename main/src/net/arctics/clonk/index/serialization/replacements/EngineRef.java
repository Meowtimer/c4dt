package net.arctics.clonk.index.serialization.replacements;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;

public class EngineRef implements Serializable, IDeserializationResolvable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final String engineName;
	public EngineRef(final Engine engine) {
		this.engineName = engine.name();
	}
	@Override
	public Object resolve(final Index index, final IndexEntity deserializee) {
		return Core.instance().loadEngine(engineName);
	}
}