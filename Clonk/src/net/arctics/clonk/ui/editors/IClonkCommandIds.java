package net.arctics.clonk.ui.editors;

import net.arctics.clonk.ClonkCore;

public abstract interface IClonkCommandIds {
	
	// script editor
	public static final String RENAME_DECLARATION = ClonkCore.id("ui.editors.actions.RenameDeclaration");
	public static final String CONVERT_OLD_CODE_TO_NEW_CODE = ClonkCore.id("ui.editors.actions.ConvertOldCodeToNewCode");
	public static final String OPEN_DECLARATION = ClonkCore.id("ui.editors.actions.OpenDeclaration");
	public static final String FIND_REFERENCES = ClonkCore.id("ui.editors.actions.FindReferences");
}
