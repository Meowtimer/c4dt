package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.Writer;

import net.arctics.clonk.util.ITreeNode;

public interface IniItem extends ITreeNode {
	void writeTextRepresentation(Writer writer, int indentation) throws IOException;
	void validate();
	String key();
	/**
	 * Return whether this item won't be saved when {@link IniUnit#save(Writer, boolean)} is called.
	 * @return True if transient and thus not saved, or false if persistent.
	 */
	boolean isTransient();
}
