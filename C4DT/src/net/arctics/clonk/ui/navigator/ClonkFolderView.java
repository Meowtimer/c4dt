package net.arctics.clonk.ui.navigator;

import static net.arctics.clonk.util.Utilities.as;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.resource.c4group.C4Group.StreamReadCallback;
import net.arctics.clonk.resource.c4group.C4GroupEntryHeader;
import net.arctics.clonk.resource.c4group.C4GroupFile;
import net.arctics.clonk.resource.c4group.C4GroupHeaderFilterBase;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.C4GroupTopLevelCompressed;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class ClonkFolderView extends ViewPart implements ISelectionListener, IDoubleClickListener, SelectionListener {

	public static final String PREF_CREATE_LINK_IN_CURRENT_PROJECT = ClonkFolderView.class.getSimpleName()
		+ "_CreateLinkInCurrentProj"; //$NON-NLS-1$
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

	private class ClonkFolderContentProvider extends LabelProvider
			implements ITreeContentProvider, IStyledLabelProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			try {
				File folder = (File) parentElement;
				return folder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						if (name.startsWith(".")) //$NON-NLS-1$
							return false;
						if (Util.isMac() && name.endsWith(".app")) //$NON-NLS-1$
							return false;
						return (currentEngine().groupTypeForFileName(name) != GroupType.OtherGroup || new File(dir, name).isDirectory()); 
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
			Engine engine = currentEngine();
			GroupType gt = engine.groupTypeForFileName((((File) element).toString()));
			return engine.image(gt);
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
	private UI.ProjectSelectionBlock projectEditor;
	private Menu treeMenu;
	private MenuItem importMenuItem;
	private MenuItem linkMenuItem;
	private MenuItem refreshMenuItem;
	private MenuItem lookForID;

	private void createProjectEditor(Composite parent, Object groupLayoutData) {
		projectEditor = new UI.ProjectSelectionBlock(parent, null, this, groupLayoutData, null);
	}

	private IProject selectedProject() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(projectEditor.text.getText());
		} catch (Exception e) {
			return null;
		}
	}

	private void updateProjectChooserEnablization() {
		projectEditor.addButton.setEnabled(!openInCurrentProject.getSelection());
		projectEditor.text.setEnabled(!openInCurrentProject.getSelection());
		Core.instance().getPreferenceStore().setValue(PREF_CREATE_LINK_IN_CURRENT_PROJECT, openInCurrentProject.getSelection());
		// trigger setting of project
		selectionChanged(UI.projectExplorer(getSite().getWorkbenchWindow()), getSite().getWorkbenchWindow().getSelectionService().getSelection(IPageLayout.ID_PROJECT_EXPLORER));
	}

	@Override
	public void createPartControl(final Composite parent) {

		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);

		parent.setLayout(new FormLayout());

		optionsComposite = new Composite(parent, SWT.NO_SCROLL);
		optionsComposite.setLayout(new FormLayout());

		openInCurrentProject = new Button(optionsComposite, SWT.CHECK);
		openInCurrentProject.setText(Messages.ClonkFolderView_OpenInCurrentProject);
		openInCurrentProject.setLayoutData(UI.createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(0, 5),
			new FormAttachment(0, 5 + openInCurrentProject.computeSize(SWT.DEFAULT, SWT.DEFAULT).y))
		);
		openInCurrentProject.addSelectionListener(this);

		PreferenceStore dummyPrefStore = new PreferenceStore();
		dummyPrefStore.setValue(ClonkPreferences.ACTIVE_ENGINE, Core.instance().getPreferenceStore().getString(ClonkPreferences.ACTIVE_ENGINE));

		createProjectEditor(optionsComposite, UI.createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(openInCurrentProject, 0),
			new FormAttachment(100, 0))
		);

		optionsComposite.setLayoutData(UI.createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(100, -optionsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y),
			new FormAttachment(100, 0))
		);

		openInCurrentProject.setSelection(Core.instance().getPreferenceStore().getBoolean(PREF_CREATE_LINK_IN_CURRENT_PROJECT));
		updateProjectChooserEnablization();

		folderTree = new TreeViewer(parent, SWT.MULTI);
		folderTree.getTree().setLayoutData(UI.createFormData(
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
		new MenuItem(treeMenu, SWT.SEPARATOR);
		refreshMenuItem = new MenuItem(treeMenu, SWT.PUSH);
		refreshMenuItem.setText(Messages.ClonkFolderView_Refresh0);
		lookForID = new MenuItem(treeMenu, SWT.PUSH);
		lookForID.setText(Messages.ClonkFolderView_LookForIDMenuText);
		
		for (MenuItem item : new MenuItem[] {linkMenuItem, importMenuItem, refreshMenuItem, lookForID})
			item.addSelectionListener(this);
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
			if (ssel.getFirstElement() instanceof IResource)
				proj = ((IResource) ssel.getFirstElement()).getProject();
		}
		if (proj != null)
			projectEditor.text.setText(proj.getName());
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
		Engine engine = currentEngine();
		String clonkPath = engine != null ? engine.settings().gamePath : null;
		File clonkFolder = (clonkPath != null && !clonkPath.equals("")) //$NON-NLS-1$
			? new File( clonkPath)
			: new File("/"); //$NON-NLS-1$
		if (!onlyIfInputChanged || !Utilities.objectsEqual(folderTree.getInput(), clonkFolder))
			folderTree.setInput(clonkFolder);
	}

	protected Engine currentEngine() {
		IProject p = selectedProject();
		Engine engine = null;
		if (p != null && p.isOpen()) {
			ClonkProjectNature nature = ClonkProjectNature.get(p);
			if (nature != null)
				engine = nature.index().engine();
		}
		if (engine == null)
			engine = Core.instance().activeEngine();
		return engine;
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		if (event.getSource() == folderTree)
			linkSelection();
	}

	private void linkSelection() {
		for (Object f : ((IStructuredSelection)folderTree.getSelection()).toArray()) {
			File sel = (File) f;
			if (sel.isDirectory()) {
				UI.message(String.format(Messages.ClonkFolderView_JustAFolder, sel.getName()));
				return;
			}
			IProject proj = selectedProject();
			if (proj != null)
				LinkC4GroupFileHandler.linkC4GroupFile(proj, sel);
		}
	}

	private void importSelection() {
		for (Object f : ((IStructuredSelection)folderTree.getSelection()).toArray()) {
			File sel = (File)f;
			IProject proj = selectedProject();
			if (proj != null)
				QuickImportHandler.importFiles(getSite().getShell(), proj, sel);
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == openInCurrentProject)
			updateProjectChooserEnablization();
		else if (e.getSource() == projectEditor.addButton) {
			IProject project = UI.selectClonkProject(selectedProject());
			if (project != null)
				projectEditor.text.setText(project.getName());
		} else if (e.getSource() == importMenuItem)
			importSelection();
		else if (e.getSource() == linkMenuItem)
			linkSelection();
		else if (e.getSource() == refreshMenuItem)
			refreshTree(false);
		else if (e.getSource() == lookForID)
			lookForID();
	}

	private void lookForID() {
		InputDialog inputDialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
    		"Look for ID", Messages.ClonkFolderView_LookForIDPromptCaption, "CLNK", null); //$NON-NLS-1$ //$NON-NLS-3$
		if (inputDialog.open() != Window.OK)
			return;

		final Pattern idPattern = Pattern.compile(String.format("^id\\=%s$", inputDialog.getValue()), Pattern.CASE_INSENSITIVE|Pattern.MULTILINE); //$NON-NLS-1$
		final Object[] selection = ((IStructuredSelection)folderTree.getSelection()).toArray();
		new Job(Messages.ClonkFolderView_LookForIDJobDescription) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<File> containing = new ArrayList<File>();
				
				for (Object f : selection) {
					final File sel = (File)f;
					if (sel.isDirectory())
						continue;
					try {
						final C4GroupTopLevelCompressed group = new C4GroupTopLevelCompressed(sel.getName(), sel);
						final C4GroupHeaderFilterBase headerFilter = new C4GroupHeaderFilterBase() {
							@Override
							public boolean accepts(C4GroupEntryHeader header, C4Group context) {
								return header.entryName().equals("DefCore.txt"); //$NON-NLS-1$
							}
							@Override
							public void processGroupItem(C4GroupItem item) throws CoreException {
								C4GroupFile file = as(item, C4GroupFile.class);
								if (file != null) {
									String s = file.getContentsAsString();
									if (idPattern.matcher(s).find())
										containing.add(sel);
								}
							}
						};
						group.readFromStream(group, 0, new StreamReadCallback() {
							@Override
							public void readStream(InputStream stream) {
								try {
									group.readIntoMemory(true, headerFilter, stream);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						folderTree.setSelection(new StructuredSelection(containing));
					}
					
				});
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		if (e.getSource() == projectEditor.text) {
			Core.instance().getPreferenceStore().setValue(PREF_PROJECT_TO_CREATE_LINK_IN, projectEditor.text.getText());
			refreshTree(true);
		}
	}

	public void update() {
		refreshTree(false);
	}

}
