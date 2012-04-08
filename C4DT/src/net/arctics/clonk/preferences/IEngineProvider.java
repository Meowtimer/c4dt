package net.arctics.clonk.preferences;

import net.arctics.clonk.index.Engine;

/**
 * Provides an engine. D'oh.
 * @author madeen
 *
 */
public interface IEngineProvider {
	/**
	 * Provide the engine.
	 * @return The provided engine
	 */
	Engine getEngine(boolean fallbackToDefault);
}
