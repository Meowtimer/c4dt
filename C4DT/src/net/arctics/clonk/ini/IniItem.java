package net.arctics.clonk.ini;

import java.io.Writer;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.ITreeNode;

public interface IniItem extends ITreeNode {
	void print(ASTNodePrinter writer, int indentation);
	void validate(Markers markers) throws ProblemException;
	String key();
	/**
	 * Return whether this item won't be saved when {@link IniUnit#save(Writer, boolean)} is called.
	 * @return True if transient and thus not saved, or false if persistent.
	 */
	boolean isTransient();
}
