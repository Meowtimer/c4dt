package net.arctics.clonk.ui.editors;

import net.arctics.clonk.ClonkCore;

public interface ClonkCommandIds {
	
	// script editor
	public static final String RENAME_DECLARATION = ClonkCore.id("ui.editors.actions.RenameDeclaration"); //$NON-NLS-1$
	public static final String CONVERT_OLD_CODE_TO_NEW_CODE = ClonkCore.id("ui.editors.actions.TidyUpCode"); //$NON-NLS-1$
	public static final String OPEN_DECLARATION = ClonkCore.id("ui.editors.actions.OpenDeclaration"); //$NON-NLS-1$
	public static final String FIND_REFERENCES = ClonkCore.id("ui.editors.actions.FindReferences"); //$NON-NLS-1$
	public static final String GROUP_CLONK = ClonkCore.id("ui.editors.actions.clonkGroup"); //$NON-NLS-1$
	public static final String FIND_DUPLICATES = ClonkCore.id("ui.editors.actions.FindDuplicates"); //$NON-NLS-1$
	public static final String TOGGLE_COMMENT = ClonkCore.id("ui.editors.actions.ToggleComment"); //$NON-NLS-1$
}
