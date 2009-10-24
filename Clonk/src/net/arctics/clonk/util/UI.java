package net.arctics.clonk.util;

import org.eclipse.swt.graphics.Image;

/**
 * Stores references to some objects needed for various components of the user interface
 */
public abstract class UI {
	public final static Image GENERAL_OBJECT_ICON = Utilities.getIconImage("c4object","icons/C4Object.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static Image SCRIPT_ICON = Utilities.getIconImage("c4script","icons/c4scriptIcon.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static Image GROUP_ICON = Utilities.getIconImage("c4datafolder","icons/Clonk_datafolder.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static Image FOLDER_ICON = Utilities.getIconImage("c4folder","icons/Clonk_folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image SCENARIO_ICON = Utilities.getIconImage("c4scenario","icons/Clonk_scenario.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image TEXT_ICON = Utilities.getIconImage("c4txt","icons/text.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image MATERIAL_ICON = Utilities.getIconImage("c4material","icons/Clonk_C4.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image DEPENDENCIES_ICON = Utilities.getIconImage("c4dependencies", "icons/Dependencies.png"); //$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String FILEDIALOG_CLONK_FILTER = "*.c4g;*.c4d;*.c4f;*.c4s"; //$NON-NLS-1$
}
