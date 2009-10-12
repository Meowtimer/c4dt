package net.arctics.clonk.index;

import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.resource.ExternalLib;

/**
 * Interface to be implemented by scripts that are not inside the workspace.
 */
public interface IExternalScript {
	ExternalLib getExternalLib();
	SimpleScriptStorage getSimpleStorage();
}
