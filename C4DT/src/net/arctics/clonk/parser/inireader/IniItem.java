package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.Writer;
import net.arctics.clonk.util.ITreeNode;

public interface IniItem extends ITreeNode {
	void writeTextRepresentation(Writer writer, int indentation) throws IOException;
	void validate();
	String getKey();
}
