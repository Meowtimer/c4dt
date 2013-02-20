package net.arctics.clonk.parser.inireader;

import java.io.Writer;

import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.util.ITreeNode;

public interface IniItem extends ITreeNode {
	void print(ASTNodePrinter writer, int indentation);
	void validate(Markers markers) throws ParsingException;
	String key();
	/**
	 * Return whether this item won't be saved when {@link IniUnit#save(Writer, boolean)} is called.
	 * @return True if transient and thus not saved, or false if persistent.
	 */
	boolean isTransient();
}
