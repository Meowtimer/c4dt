package net.arctics.clonk.parser.foldermap;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.parser.inireader.IniUnit;

public class FolderMapUnit extends IniUnit {

	private static final String FILE_NAME = "FolderMap.txt";
	
	@Override
	protected String getConfigurationName() {
		return FILE_NAME;
	}
	
	public FolderMapUnit(IFile file) {
		super(file);
	}
	
	public FolderMapUnit(String text) {
		super(text);
	}

	private static final long serialVersionUID = 1L;

}
