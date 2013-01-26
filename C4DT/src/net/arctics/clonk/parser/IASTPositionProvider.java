package net.arctics.clonk.parser;

import org.eclipse.core.resources.IFile;

public interface IASTPositionProvider {
	IFile file();
	Declaration container();
	int fragmentOffset();
}
