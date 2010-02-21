package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.filesystem.C4GroupFileSystem;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class ClonkFolderView extends ViewPart implements ISelectionListener, IDoubleClickListener, SelectionListener {

	public static final String PREF_CREATE_LINK_IN_CURRENT_PROJECT = ClonkFolderView.class.getSimpleName()
		+ "_CreateLinkInCurrentProj"; //$NON-NLS-1$
	public static final String PREF_DELETE_LINKS_ON_SHUTDOWN = ClonkFolderView.class.getSimpleName()
		+ "_DeleteLinksOnShutdown"; //$NON-NLS-1$
	public static final String PREF_PROJECT_TO_CREATE_LINK_IN = ClonkFolderView.class.getSimpleName()
		+ "_ProjectToCreateLinkIn"; //$NON-NLS-1$
	
	private static ClonkFolderView instance;
	
	public static ClonkFolderView instance() {
		return instance;
	}
	
	public ClonkFolderView() {
		super();
		instance = this;
	}

	private static class ClonkFolderContentProvider extends LabelProvider
			implements ITreeContentProvider, IStyledLabelProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			try {
				File folder = (File) parentElement;
				return folder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						if (name.startsWith("."))
							return false;
						if (Util.isMac() && name.endsWith(".app"))
							return false;
						return (C4Group.getGroupType(name) != C4GroupType.OtherGroup || new File(dir, name).isDirectory()); //$NON-NLS-1$
					}
				});
			} catch (Exception e) {
				return new Object[0];
			}
		}

		@Override
		public Object getParent(Object element) {
			return ((File) element).getParentFile();
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof File && ((File) element).isDirectory();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub

		}

		@Override
		public Image getImage(Object element) {
			switch (C4Group.getGroupType(((File) element).toString())) {
			case DefinitionGroup:
				return UI.GENERAL_OBJECT_ICON;
			case FolderGroup:
				return UI.FOLDER_ICON;
			case ResourceGroup:
				return UI.GROUP_ICON;
			case ScenarioGroup:
				return UI.SCENARIO_ICON;
			default:
				return null;
			}
		}

		@Override
		public StyledString getStyledText(Object element) {
			if (((File) element).isFile())
				return new StyledString(((File) element).getName(), StyledString.QUALIFIER_STYLER);
			else
				return new StyledString(((File) element).getName());
		}

		@Override
		public String getText(Object element) {
			return getStyledText(element).getString();
		}

	}

	private Composite optionsComposite;
	private TreeViewer folderTree;

	private Button openInCurrentProject;
	private Button removeLinkedFilesOnShutdown;
	private UI.ProjectEditorBlock projectEditor;
	private Menu treeMenu;
	private MenuItem importMenuItem;
	private MenuItem linkMenuItem;
	private MenuItem refreshMenuItem;

	private static FormData createFormData(FormAttachment left, FormAttachment right, FormAttachment top, FormAttachment bottom) {
		FormData result = new FormData();
		result.left = left;
		result.top = top;
		result.right = right;
		result.bottom = bottom;
		return result;
	}

	private void createProjectEditor(Composite parent, Object groupLayoutData) {
		projectEditor = new UI.ProjectEditorBlock(parent, null, this, groupLayoutData, null);
	}

	private IProject selectedProject() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(projectEditor.Text.getText());
		} catch (Exception e) {
			return null;
		}
	}

	private void updateProjectChooserEnablization() {
		projectEditor.AddButton.setEnabled(!openInCurrentProject.getSelection());
		projectEditor.Text.setEnabled(!openInCurrentProject.getSelection());
		ClonkCore.getDefault().getPreferenceStore().setValue(PREF_CREATE_LINK_IN_CURRENT_PROJECT, openInCurrentProject.getSelection());
		// trigger setting of project
		selectionChanged(Utilities.getProjectExplorer(getSite().getWorkbenchWindow()), getSite().getWorkbenchWindow().getSelectionService().getSelection(IPageLayout.ID_PROJECT_EXPLORER));
	}

	@Override
	public void createPartControl(final Composite parent) {

		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);

		parent.setLayout(new FormLayout());

		optionsComposite = new Composite(parent, SWT.NO_SCROLL);
		optionsComposite.setLayout(new FormLayout());

		openInCurrentProject = new Button(optionsComposite, SWT.CHECK);
		openInCurrentProject.setText(Messages.ClonkFolderView_OpenInCurrentProject);
		openInCurrentProject.setLayoutData(createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(0, 5),
			new FormAttachment(0, 5 + openInCurrentProject.computeSize(SWT.DEFAULT, SWT.DEFAULT).y))
		);
		openInCurrentProject.addSelectionListener(this);

		removeLinkedFilesOnShutdown = new Button(optionsComposite, SWT.CHECK);
		removeLinkedFilesOnShutdown.setText(Messages.ClonkFolderView_RemoveLinkedFilesOnShutdown);
		removeLinkedFilesOnShutdown.setLayoutData(createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(openInCurrentProject, 5),
			new FormAttachment(openInCurrentProject, 40))
		);
		removeLinkedFilesOnShutdown.addSelectionListener(this);
		
		PreferenceStore dummyPrefStore = new PreferenceStore();
		dummyPrefStore.setValue(ClonkPreferences.ACTIVE_ENGINE, ClonkCore.getDefault().getPreferenceStore().getString(ClonkPreferences.ACTIVE_ENGINE));

		createProjectEditor(optionsComposite, createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(removeLinkedFilesOnShutdown, 0),
			new FormAttachment(100, 0))
		);

		optionsComposite.setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(100, -optionsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y),
			new FormAttachment(100, 0))
		);

		openInCurrentProject.setSelection(ClonkCore.getDefault().getPreferenceStore().getBoolean(PREF_CREATE_LINK_IN_CURRENT_PROJECT));
		removeLinkedFilesOnShutdown.setSelection(ClonkCore.getDefault().getPreferenceStore().getBoolean(PREF_DELETE_LINKS_ON_SHUTDOWN));
		updateProjectChooserEnablization();

		folderTree = new TreeViewer(parent, SWT.NONE);
		folderTree.getTree().setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(0, 0),
			new FormAttachment(optionsComposite, 0))
		);
		ClonkFolderContentProvider prov = new ClonkFolderContentProvider();
		folderTree.setContentProvider(prov);
		refreshTree(false);
		folderTree.setLabelProvider(prov);
		folderTree.addDoubleClickListener(this);

		treeMenu = new Menu(getSite().getShell(), SWT.POP_UP);
		linkMenuItem = new MenuItem(treeMenu, SWT.PUSH);
		linkMenuItem.setText(Messages.ClonkFolderView_Link);
		importMenuItem = new MenuItem(treeMenu, SWT.PUSH);
		importMenuItem.setText(Messages.ClonkFolderView_Import);
		refreshMenuItem = new MenuItem(treeMenu, SWT.PUSH);
		refreshMenuItem.setText(Messages.ClonkFolderView_Refresh0);
		
		for (MenuItem item : new MenuItem[] {linkMenuItem, importMenuItem, refreshMenuItem}) {
			item.addSelectionListener(this);
		}
		folderTree.getTree().setMenu(treeMenu);

		parent.layout();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!openInCurrentProject.getEnabled())
			return;
		IProject proj = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.getFirstElement() instanceof IResource) {
				proj = ((IResource) ssel.getFirstElement()).getProject();
			}
		}
		if (proj != null)
			projectEditor.Text.setText(proj.getName());
		if (folderTree != null)
			refreshTree(true);
	}

	@Override
	public void dispose() {
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);
		instance = null;
		super.dispose();
	}

	private void refreshTree(boolean onlyIfInputChanged) {
		IProject p = selectedProject();
		C4Engine engine = null;
		if (p != null && p.isOpen()) {
			ClonkProjectNature nature = ClonkProjectNature.get(p);
			if (nature != null)
				engine = nature.getIndex().getEngine();
		}
		if (engine == null)
			engine = ClonkCore.getDefault().getActiveEngine();
		String clonkPath = engine != null ? engine.getCurrentSettings().gamePath : null;
		File clonkFolder = (clonkPath != null && !clonkPath.equals("")) //$NON-NLS-1$
			? new File( clonkPath)
			: new File("/"); //$NON-NLS-1$
		if (!onlyIfInputChanged || !Utilities.objectsEqual(folderTree.getInput(), clonkFolder)) {
			folderTree.setInput(clonkFolder);
		}
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		if (event.getSource() == folderTree) {
			linkSelection();
		}
	}

	private void linkSelection() {
		File sel = (File) ((IStructuredSelection) folderTree.getSelection()).getFirstElement();
		IProject proj = selectedProject();
		if (proj != null)
			LinkC4GroupFileHandler.linkC4GroupFile(proj, sel);
	}

	private void importSelection() {
		File sel = (File) ((IStructuredSelection) folderTree.getSelection()).getFirstElement();
		IProject proj = selectedProject();
		if (proj != null) {
			QuickImportHandler.importFiles(getSite().getShell(), proj, sel);
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == openInCurrentProject) {
			updateProjectChooserEnablization();
		} else if (e.getSource() == removeLinkedFilesOnShutdown) {
			ClonkCore.getDefault().getPreferenceStore().setValue(
				PREF_DELETE_LINKS_ON_SHUTDOWN,
				removeLinkedFilesOnShutdown.getSelection()
			);
		} else if (e.getSource() == projectEditor.AddButton) {
			IProject project = Utilities.selectClonkProject(selectedProject());
			if (project != null)
				projectEditor.Text.setText(project.getName());
		} else if (e.getSource() == importMenuItem) {
			importSelection();
		} else if (e.getSource() == linkMenuItem) {
			linkSelection();
		} else if (e.getSource() == refreshMenuItem) {
			refreshTree(false);
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		if (e.getSource() == projectEditor.Text) {
			ClonkCore.getDefault().getPreferenceStore().setValue(PREF_PROJECT_TO_CREATE_LINK_IN, projectEditor.Text.getText());
			refreshTree(true);
		}
	}

	static {
		PlatformUI.getWorkbench().addWorkbenchListener(
			new IWorkbenchListener() {
				@Override
				public boolean preShutdown(IWorkbench workbench, boolean forced) {
					// delete linked c4group files
					try {
						if (ClonkCore.getDefault().getPreferenceStore().getBoolean(PREF_DELETE_LINKS_ON_SHUTDOWN)) {
							for (IProject proj : Utilities.getClonkProjects()) {
								for (IResource res : proj.members()) {
									URI uri = res.getLocationURI();
									if (uri.getScheme().equals(C4GroupFileSystem.getInstance().getScheme())) {
										res.delete(true, new NullProgressMonitor());
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return true;
				}

				@Override
				public void postShutdown(IWorkbench workbench) {
					// TODO Auto-generated method stub
				}
			}
		);
	}

	public void update() {
		refreshTree(false);
	}

}
