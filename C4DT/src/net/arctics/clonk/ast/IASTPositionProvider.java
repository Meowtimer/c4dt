package net.arctics.clonk.ast;


import org.eclipse.core.resources.IFile;

public interface IASTPositionProvider {
	IFile file();
	Declaration container();
	int fragmentOffset();
}
