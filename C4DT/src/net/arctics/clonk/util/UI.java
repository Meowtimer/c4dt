package net.arctics.clonk.util;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4Variable;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * Stores references to some objects needed for various components of the user interface
 */
public abstract class UI {
	public final static Image GENERAL_OBJECT_ICON = getIconImage("c4object","icons/C4Object.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static Image SCRIPT_ICON = getIconImage("c4script","icons/c4scriptIcon.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static Image GROUP_ICON = getIconImage("c4datafolder","icons/Clonk_datafolder.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static Image FOLDER_ICON = getIconImage("c4folder","icons/Clonk_folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image SCENARIO_ICON = getIconImage("c4scenario","icons/Clonk_scenario.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image TEXT_ICON = getIconImage("c4txt","icons/text.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image MATERIAL_ICON = getIconImage("c4material","icons/Clonk_C4.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image DEPENDENCIES_ICON = getIconImage("c4dependencies", "icons/Dependencies.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image CLONK_ENGINE_ICON = getIconImage("c4engine", "icons/Clonk_engine"); //$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String FILEDIALOG_CLONK_FILTER = "*.c4g;*.c4d;*.c4f;*.c4s"; //$NON-NLS-1$
	
	public static Image getIconForFunction(C4Function function) {
		String iconName = function.getVisibility().name().toLowerCase();
		return ClonkCore.getDefault().getIconImage(iconName);
	}
	
	public static Image getIconForVariable(C4Variable variable) {
		String iconName = variable.getScope().toString().toLowerCase();
		return ClonkCore.getDefault().getIconImage(iconName);
	}

	public static Image getIconForObject(Object element) {
		if (element instanceof C4Function)
			return getIconForFunction((C4Function)element);
		if (element instanceof C4Variable)
			return getIconForVariable((C4Variable)element);
		if (element instanceof C4Object)
			return UI.GENERAL_OBJECT_ICON;
		if (element instanceof C4ScriptBase)
			return UI.SCRIPT_ICON;
		return null;
	}
	
	public static Image getIconForC4Object(C4Object element) {
		return UI.GENERAL_OBJECT_ICON;
	}

	public static ImageDescriptor getIconDescriptor(String path) {
		return ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path(path), null));
	}
	
	public static Image getIconImage(String registryKey, String iconPath) {
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		Image img = reg.get(registryKey);
		if (img == null) {
			reg.put(registryKey, getIconDescriptor(iconPath));
			img = reg.get(registryKey);
		}
		return img;
	}
	
	public static class ProjectEditorBlock {
		public Text Text;
		public Button AddButton;
		public ProjectEditorBlock(Composite parent, ModifyListener textModifyListener, SelectionListener addListener, Object groupLayoutData, String groupText) {
			// Create widget group
			Composite container;
			if (groupText != null) {
				Group g = new Group(parent, SWT.NONE);
				g.setText(groupText);
				container = g;
			} else {
				container = new Composite(parent, SWT.NONE);
			}
			container.setLayout(new GridLayout(2, false));
			container.setLayoutData(groupLayoutData != null ? groupLayoutData : new GridData(GridData.FILL_HORIZONTAL));
			
			// Text plus button
			Text = new Text(container, SWT.SINGLE | SWT.BORDER);
			Text.setText(net.arctics.clonk.ui.navigator.Messages.ClonkFolderView_Project);
			Text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			AddButton = new Button(container, SWT.PUSH);
			AddButton.setText("Browse");
			
			// Install listener
			if (textModifyListener != null)
				Text.addModifyListener(textModifyListener);
			if (addListener != null)
				AddButton.addSelectionListener(addListener);
		}
	}
}
