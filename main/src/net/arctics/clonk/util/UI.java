package net.arctics.clonk.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.IProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.ini.DefCoreUnit;
import net.arctics.clonk.ini.IniEntry;
import net.arctics.clonk.ini.IntegerArray;
import net.arctics.clonk.landscapescript.Overlay;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
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
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * Stores references to some objects needed for various components of the user interface
 */
public enum UI {
	;

	private final static Map<String, Image> images = new HashMap<>();
	public final static Image SCRIPT_ICON = imageForPath("icons/c4scriptIcon.png"); //$NON-NLS-1$
	public final static Image MAP_ICON = imageForPath("icons/map.png");
	public final static Image MAPOVERLAY_ICON = imageForPath("icons/mapoverlay.png");
	public final static Image TEXT_ICON = imageForPath("icons/text.png"); //$NON-NLS-1$
	public final static Image MATERIAL_ICON = imageForPath("icons/Clonk_C4.png"); //$NON-NLS-1$
	public final static Image CLONK_ENGINE_ICON = imageForPath("icons/Clonk_engine.png"); //$NON-NLS-1$
	public final static Image DUPE_ICON = imageForPath("icons/dupe.png");
	public final static Image PROPLIST_ICON = imageForPath("icons/proplist.png");
	public final static Image DIRECTIVE_ICON = imageForPath("icons/directive.png");
	public final static Image WARNING_ICON = imageForPath("icons/warning.png");

	public static Image halfTransparent(final Image image) {
		String name;
		Searching: {
			for (final Map.Entry<String, Image> img : images.entrySet())
				if (img.getValue() == image) {
					name = img.getKey();
					break Searching;
				}
			return null;
		}
		final String htName = name+"(halfTransparent)";
		final Image existing = Core.instance().getImageRegistry().get(htName);
		if (existing != null)
			return existing;
		final ImageData data = image.getImageData();
		for (int x = 0; x < data.width; x++)
			for (int y = 0; y < data.height; y++)
				data.setAlpha(x, y, data.getAlpha(x, y)/2);
		final Image ht = new Image(image.getDevice(), data);
		Core.instance().getImageRegistry().put(htName, ht);
		return ht;
	}

	/**
	 * Return a function icon signifying the function's protection level.
	 * @param function The function to return an icon for
	 * @return The function icon.
	 */
	public static Image functionIcon(final Function function) {
		final String iconName = function.visibility().name().toLowerCase();
		return imageForPath(String.format("icons/%s.png", iconName));
	}

	/**
	 * Return a variable icon. The icon reflects whether the variable is static or local.
	 * @param variable The variable to return an icon for
	 * @return The variable icon.
	 */
	public static Image variableIcon(final Variable variable) {
		final String iconName = variable.scope().toString().toLowerCase();
		return imageForPath(String.format("icons/%s.png", iconName));
	}

	/**
	 * Return an icon for some object. Null will be returned if the object isn't of some class one of the
	 * icon-returning functions ({@link #functionIcon(Function)}, {@link #variableIcon(Variable)} etc) could be called with.
	 * @param element The element to return an icon for
	 * @return The icon or null if the object is of some exotic type.
	 */
	public static Image iconFor(final Object element) {
		if (element instanceof Function)
			return functionIcon((Function)element);
		if (element instanceof Variable)
			return variableIcon((Variable)element);
		if (element instanceof Definition)
			return definitionIcon((Definition)element);
		else if (element instanceof Overlay)
			return ((Overlay)element).isMap() ? MAP_ICON : MAPOVERLAY_ICON;
		else if (element instanceof Script)
			return SCRIPT_ICON;
		else if (element instanceof IProplistDeclaration)
			return PROPLIST_ICON;
		else if (element instanceof Directive)
			return DIRECTIVE_ICON;
		return null;
	}

	/**
	 * Return a {@link Definition} icon.
	 * @param def The definition to return an icon for.
	 * @return The icon.
	 */
	public static Image definitionIcon(final Definition def) {
		return def.engine().image(FileExtension.DefinitionGroup);
	}

	private static Object imageThingieForURL(final URL url, final boolean returnDescriptor) {
		if (url == null)
			return null;
		final String path = url.toExternalForm();
		final ImageRegistry reg = Core.instance().getImageRegistry();
		Object result;
		while ((result = returnDescriptor ? reg.getDescriptor(path) : reg.get(path)) == null)
			reg.put(path, ImageDescriptor.createFromURL(url));
		return result;
	}

	private static Object imageThingieForPath(final String path, final boolean returnDescriptor) {
		return imageThingieForURL(FileLocator.find(Core.instance().getBundle(), new Path(path), null), returnDescriptor);
	}

	public static ImageDescriptor imageDescriptorForPath(final String path) {
		return Core.instance().runsHeadless() ? null : (ImageDescriptor) imageThingieForPath(path, true);
	}

	public static Image imageForPath(final String iconPath) {
		final Image img = Core.instance().runsHeadless() ? null : (Image) imageThingieForPath(iconPath, false);
		images.put(iconPath, img);
		return img;
	}

	public static ImageDescriptor imageDescriptorForURL(final URL url) {
		return Core.instance().runsHeadless() ? null : (ImageDescriptor) imageThingieForURL(url, true);
	}

	public static Image imageForURL(final URL url) {
		return Core.instance().runsHeadless() ? null : (Image) imageThingieForURL(url, false);
	}

	public static class ProjectSelectionBlock {
		public Text text;
		public Button addButton;
		public ProjectSelectionBlock(final Composite parent, final ModifyListener textModifyListener, final SelectionListener addListener, final Object groupLayoutData, final String groupText) {
			// Create widget group
			Composite container;
			if (groupText != null) {
				final Group g = new Group(parent, SWT.NONE);
				g.setText(groupText);
				container = g;
			} else
				container = new Composite(parent, SWT.NONE);
			container.setLayout(new GridLayout(2, false));
			container.setLayoutData(groupLayoutData != null ? groupLayoutData : new GridData(GridData.FILL_HORIZONTAL));

			// Text plus button
			text = new Text(container, SWT.SINGLE | SWT.BORDER);
			text.setText(net.arctics.clonk.ui.navigator.Messages.ClonkFolderView_Project);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			addButton = new Button(container, SWT.PUSH);
			addButton.setText(net.arctics.clonk.ui.navigator.Messages.Browse);

			// Install listener
			if (textModifyListener != null)
				text.addModifyListener(textModifyListener);
			if (addListener != null)
				addButton.addSelectionListener(addListener);
		}
	}

	public static FormData createFormData(final FormAttachment left, final FormAttachment right, final FormAttachment top, final FormAttachment bottom) {
		final FormData result = new FormData();
		result.left = left;
		result.top = top;
		result.right = right;
		result.bottom = bottom;
		return result;
	}

	public static CheckboxTableViewer createProjectReferencesViewer(final Composite parent) {
		final CheckboxTableViewer result = CheckboxTableViewer.newCheckList(parent, SWT.TOP | SWT.BORDER);
		result.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		result.setContentProvider(new WorkbenchContentProvider() {
			@Override
			public Object[] getChildren(final Object element) {
				return ClonkProjectNature.clonkProjectsInWorkspace();
			}
		});
		result.setComparator(new ViewerComparator());
		result.setInput(ResourcesPlugin.getWorkspace());
		return result;
	}

	public static boolean confirm(final Shell shell, final String text, final String confirmTitle) {
		return MessageDialog.openQuestion(
			shell,
			confirmTitle != null ? confirmTitle : Messages.UI_Confirm,
			text
		);
	}

	public static String input(final Shell shell, final String title, final String prompt, final String defaultValue, final IInputValidator validator) {
		final InputDialog newNameDialog = new InputDialog(shell, title, prompt, defaultValue, validator);
		switch (newNameDialog.open()) {
		case Window.CANCEL:
			return null;
		}
		return newNameDialog.getValue();
	}

	public static String input(final Shell shell, final String title, final String prompt, final String defaultValue) {
		return input(shell, title, prompt, defaultValue, null);
	}

	public static void message(final String message, final int kind) {
		MessageDialog.open(kind, null, Core.HUMAN_READABLE_NAME, message, SWT.NONE);
	}

	public static void message(final String message) {
		message(message, MessageDialog.INFORMATION);
	}

	public static void informAboutException(final String message, final Object... additionalFormatArguments) {
		final Exception exception = additionalFormatArguments.length > 0 && additionalFormatArguments[0] instanceof Exception ? (Exception)additionalFormatArguments[0] : null;
		if (exception != null) {
			exception.printStackTrace();
			additionalFormatArguments[0] = exception.getMessage();
		}
		final String msg = String.format(message, additionalFormatArguments);
		Display.getDefault().asyncExec(() -> message(msg, MessageDialog.ERROR));
	}

	public static IProject[] selectClonkProjects(final boolean multiSelect, final IProject... initialSelection) {
		// Create dialog listing all Clonk projects
		final ElementListSelectionDialog dialog
			= new ElementListSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new ClonkLabelProvider());
		dialog.setTitle(Messages.Utilities_ChooseClonkProject);
		dialog.setMessage(Messages.Utilities_ChooseClonkProjectPretty);
		dialog.setElements(ClonkProjectNature.clonkProjectsInWorkspace());
		dialog.setMultipleSelection(multiSelect);

		// Set selection
		dialog.setInitialSelections(new Object [] { initialSelection });

		// Show
		if(dialog.open() == Window.OK)
			return ArrayUtil.convertArray(dialog.getResult(), IProject.class);
		else
			return null;
	}

	public static IProject selectClonkProject(final IProject initialSelection) {
		final IProject[] projects = selectClonkProjects(false, initialSelection);
		return projects != null ? projects[0] : null;
	}

	public static CommonNavigator projectExplorer() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null)
			return projectExplorer(workbench.getActiveWorkbenchWindow());
		return null;
	}

	public static ISelection projectExplorerSelection(final IWorkbenchSite site) {
		return site.getWorkbenchWindow().getSelectionService().getSelection(IPageLayout.ID_PROJECT_EXPLORER);
	}

	public static ISelection projectExplorerSelection() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection(IPageLayout.ID_PROJECT_EXPLORER);
	}

	public static CommonNavigator projectExplorer(final IWorkbenchWindow window) {
		if (window != null) {
			final IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				final IViewPart viewPart = page.findView(IPageLayout.ID_PROJECT_EXPLORER);
				if (viewPart instanceof CommonNavigator)
					return (CommonNavigator) viewPart;
			}
		}
		return null;
	}

	public static CommonNavigator[] projectExplorers() {
		final List<CommonNavigator> navs = new ArrayList<CommonNavigator>(PlatformUI.getWorkbench().getWorkbenchWindowCount());
		for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			final CommonNavigator nav = projectExplorer(window);
			if (nav != null)
				navs.add(nav);
		}
		return navs.toArray(new CommonNavigator[navs.size()]);
	}

	public static void refreshAllProjectExplorers(final Object at) {
		for (final CommonNavigator nav : projectExplorers())
			nav.getCommonViewer().refresh(at);
	}

	public static Image getPicture(final DefCoreUnit defCore, final Image graphics) {
		Image result = null;
		final IniEntry pictureEntry = defCore.entryInSection("DefCore", "Picture"); //$NON-NLS-1$ //$NON-NLS-2$
		if (pictureEntry != null && pictureEntry.value() instanceof IntegerArray) {
			final IntegerArray values = (IntegerArray) pictureEntry.value();
			result = new Image(Display.getCurrent(), values.get(2), values.get(3));
			final GC gc = new GC(result);
			try {
				gc.drawImage(graphics, values.get(0), values.get(1), values.get(2), values.get(3), 0, 0, result.getBounds().width, result.getBounds().height);
			} finally {
				gc.dispose();
			}
		}
		return result;
	}

	public static Image imageForContainer(final IContainer container) throws CoreException, IOException {
		Image image = null;
		IResource graphicsFile = container.findMember("Graphics.png"); //$NON-NLS-1$
		if (graphicsFile == null)
			graphicsFile = container.findMember("Graphics.bmp"); //$NON-NLS-1$
		if (graphicsFile instanceof IFile) {
			final InputStream fileContents = ((IFile)graphicsFile).getContents();
			try {
				final Image fullGraphics = new Image(Display.getCurrent(), fileContents);
				try {
					final IResource defCoreFile = container.findMember("DefCore.txt"); //$NON-NLS-1$
					if (defCoreFile instanceof IFile) {
						final DefCoreUnit defCore = (DefCoreUnit) Structure.pinned(defCoreFile, true, false);
						image = getPicture(defCore, fullGraphics);
					}
				} finally {
					if (image == null)
						image = fullGraphics;
					else
						fullGraphics.dispose();
				}
			} finally {
				fileContents.close();
			}
		}
		return image;
	}

}
