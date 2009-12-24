package net.arctics.clonk.index;

/**
 * Interface to be implemented by scripts that are not inside the workspace.
 */
public interface IExternalScript extends IContainedInExternalLib {
	String getScriptText();
}
