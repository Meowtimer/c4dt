package net.arctics.clonk.util;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.mapcreator.MapCreatorMap;
import net.arctics.clonk.parser.mapcreator.MapOverlay;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Stores references to some objects needed for various components of the user interface
 */
public abstract class UI {
	public final static Image SCRIPT_ICON = getIconImage("c4script","icons/c4scriptIcon.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static Image MAP_ICON = getIconImage("mapCreatorMap", "icons/map.png");
	public final static Image MAPOVERLAY_ICON = getIconImage("mapCreatorMapOverlay", "icons/mapoverlay.png");
	public static final Image TEXT_ICON = getIconImage("c4txt","icons/text.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image MATERIAL_ICON = getIconImage("c4material","icons/Clonk_C4.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image DEPENDENCIES_ICON = getIconImage("c4dependencies", "icons/Dependencies.png"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image CLONK_ENGINE_ICON = getIconImage("c4engine", "icons/Clonk_engine"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final Image DUPE_ICON = getIconImage("dupe", "icons/dupe.png");
	
	public static Image getIconForFunction(Function function) {
		String iconName = function.getVisibility().name().toLowerCase();
		return ClonkCore.getDefault().getIconImage(iconName);
	}
	
	public static Image getIconForVariable(Variable variable) {
		String iconName = variable.getScope().toString().toLowerCase();
		return ClonkCore.getDefault().getIconImage(iconName);
	}

	public static Image getIconForObject(Object element) {
		if (element instanceof Function)
			return getIconForFunction((Function)element);
		if (element instanceof Variable)
			return getIconForVariable((Variable)element);
		if (element instanceof Definition) {
			Definition def = (Definition)element;
			return def.getEngine().getGroupTypeToIconMap().get(GroupType.DefinitionGroup);
		}
		else if (element instanceof MapCreatorMap)
			return MAP_ICON;
		else if (element instanceof MapOverlay)
			return MAPOVERLAY_ICON;
		else if (element instanceof ScriptBase)
			return SCRIPT_ICON;
		return null;
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
			AddButton.setText(net.arctics.clonk.ui.navigator.Messages.Browse);
			
			// Install listener
			if (textModifyListener != null)
				Text.addModifyListener(textModifyListener);
			if (addListener != null)
				AddButton.addSelectionListener(addListener);
		}
	}
	
	public static FormData createFormData(FormAttachment left, FormAttachment right, FormAttachment top, FormAttachment bottom) {
		FormData result = new FormData();
		result.left = left;
		result.top = top;
		result.right = right;
		result.bottom = bottom;
		return result;
	}

	public static CheckboxTableViewer createProjectReferencesViewer(Composite parent) {
		CheckboxTableViewer result = CheckboxTableViewer.newCheckList(parent, SWT.TOP | SWT.BORDER);
		result.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		result.setContentProvider(new WorkbenchContentProvider() {
			@Override
			public Object[] getChildren(Object element) {
				return ClonkProjectNature.getClonkProjects();
			}
		});
		result.setComparator(new ViewerComparator());
		result.setInput(ResourcesPlugin.getWorkspace());
		return result;
	}
	
	public static boolean confirm(Shell shell, String text, String confirmTitle) {
		return MessageDialog.openQuestion(
			shell,
			confirmTitle != null ? confirmTitle : Messages.UI_Confirm,
			text
		);
	}
	
	public static String input(Shell shell, String title, String prompt, String defaultValue, IInputValidator validator) {
		InputDialog newNameDialog = new InputDialog(shell, title, prompt, defaultValue, validator);
		switch (newNameDialog.open()) {
		case InputDialog.CANCEL:
			return null;
		}
		return newNameDialog.getValue();
	}
	
	public static String input(Shell shell, String title, String prompt, String defaultValue) {
		return input(shell, title, prompt, defaultValue, null);
	}
	
	public static void message(String message, int kind) {
		MessageDialog.open(kind, null, ClonkCore.HUMAN_READABLE_NAME, message, SWT.NONE);
	}
	
	public static void message(String message) {
		message(message, MessageDialog.INFORMATION);
	}
	
	public static void informAboutException(String message, Object... additionalFormatArguments) {
		Exception exception = additionalFormatArguments.length > 0 && additionalFormatArguments[0] instanceof Exception ? (Exception)additionalFormatArguments[0] : null;
		if (exception != null) {
			exception.printStackTrace();
			additionalFormatArguments[0] = exception.getMessage();
		}
		final String msg = String.format(message, additionalFormatArguments);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				message(msg, MessageDialog.ERROR);
			}
		});
	}
	
	public static IProject[] selectClonkProjects(boolean multiSelect, IProject... initialSelection) {
		// Create dialog listing all Clonk projects
		ElementListSelectionDialog dialog
			= new ElementListSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new ClonkLabelProvider());
		dialog.setTitle(Messages.Utilities_ChooseClonkProject);
		dialog.setMessage(Messages.Utilities_ChooseClonkProjectPretty);
		dialog.setElements(ClonkProjectNature.getClonkProjects());
		dialog.setMultipleSelection(multiSelect);

		// Set selection
		dialog.setInitialSelections(new Object [] { initialSelection });

		// Show
		if(dialog.open() == Window.OK) {
			return ArrayUtil.convertArray(dialog.getResult(), IProject.class);
		}
		else
			return null;
	}
	
	public static IProject selectClonkProject(IProject initialSelection) {
		IProject[] projects = selectClonkProjects(false, initialSelection);
		return projects != null ? projects[0] : null;
	}
	
	public static IViewPart findViewInActivePage(IWorkbenchSite site, String id) {
		if (site != null && site.getWorkbenchWindow() != null && site.getWorkbenchWindow().getActivePage() != null)
			return site.getWorkbenchWindow().getActivePage().findView(id);
		else
			return null;
	}

}
