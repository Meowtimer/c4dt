package net.arctics.clonk.parser;

import org.eclipse.core.resources.IFile;

public interface IASTPositionProvider {
	ASTNode node();
	IFile file();
	Declaration container();
	int sectionOffset();
	int fragmentOFfset();
}
