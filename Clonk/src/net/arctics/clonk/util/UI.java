package net.arctics.clonk.util;

import org.eclipse.swt.graphics.Image;

/**
 * Stores references to some objects needed for various components of the user interface
 */
public abstract class UI {
	public final static Image GENERAL_OBJECT_ICON = Utilities.getIconImage("c4object","icons/C4Object.png");
	public final static Image SCRIPT_ICON = Utilities.getIconImage("c4script","icons/c4scriptIcon.png");
	public final static Image GROUP_ICON = Utilities.getIconImage("c4datafolder","icons/Clonk_datafolder.png");
	public final static Image FOLDER_ICON = Utilities.getIconImage("c4folder","icons/Clonk_folder.png");
	public static final Image SCENARIO_ICON = Utilities.getIconImage("c4scenario","icons/Clonk_scenario.png");
	public static final Image TEXT_ICON = Utilities.getIconImage("c4txt","icons/text.png");
	public static final Image MATERIAL_ICON = Utilities.getIconImage("c4material","icons/Clonk_C4.png");
	public static final Image DEPENDENCIES_ICON = Utilities.getIconImage("c4dependencies", "icons/Dependencies.png");
	
	public static final String FILEDIALOG_CLONK_FILTER = "*.c4g;*.c4d;*.c4f;*.c4s";
}
