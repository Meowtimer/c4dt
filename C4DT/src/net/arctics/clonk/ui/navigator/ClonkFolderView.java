package net.arctics.clonk.ui.navigator;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group;
import net.arctics.clonk.c4group.C4GroupEntryHeader;
import net.arctics.clonk.c4group.C4GroupFile;
import net.arctics.clonk.c4group.C4GroupHeaderFilterBase;
import net.arctics.clonk.c4group.C4GroupItem;
import net.arctics.clonk.c4group.C4GroupTopLevelCompressed;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.TaskExecution;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
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

	private final class DefCoreTextSearch extends Job {
		private final Object[] selection;
		private final Pattern idPattern;
		private final List<File> containing = Collections.synchronizedList(new ArrayList<File>());
		private DefCoreTextSearch(String name, Object[] selection, Pattern idPattern) {
			super(name);
			this.selection = selection;
			this.idPattern = idPattern;
		}
		class GroupScanner implements Runnable {
			private final File groupFile;
			public GroupScanner(File f) { this.groupFile = f; }
			@Override
			public void run() {
				try {
					final C4GroupTopLevelCompressed group = new C4GroupTopLevelCompressed(groupFile.getName(), groupFile);
					final C4GroupHeaderFilterBase headerFilter = new C4GroupHeaderFilterBase() {
						@Override
						public int flagsForEntry(C4GroupFile entry) {
							return READINTOMEMORY;
						}
						@Override
						public boolean accepts(final C4GroupEntryHeader header, final C4Group context) {
							return header.entryName().equals("DefCore.txt"); //$NON-NLS-1$
						}
						@Override
						public void processGroupItem(final C4GroupItem item) {
							final C4GroupFile file = as(item, C4GroupFile.class);
							if (file != null) {
								final String s = new String(file.getContents());
								if (idPattern.matcher(s).find())
									containing.add(groupFile);
							}
						}
					};
					group.readFromStream(group, 0, stream -> {
						try {
							group.readIntoMemory(true, headerFilter, stream);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					});
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			TaskExecution.threadPool(
				ofType(stream(selection), File.class)
					.filter(f -> !f.isDirectory())
					.map(f -> new GroupScanner(f))
					.collect(Collectors.toList()),
					20
				);
			Display.getDefault().asyncExec(() -> folderTree.setSelection(new StructuredSelection(containing)));
			return Status.OK_STATUS;
		}
	}

	private class ClonkFolderContentProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {

		@Override
		public Object[] getChildren(final Object parentElement) {
			try {
				final File folder = (File) parentElement;
				return folder.listFiles((dir, name) -> {
					if (name.startsWith(".")) //$NON-NLS-1$
						return false;
					if (Util.isMac() && name.endsWith(".app")) //$NON-NLS-1$
						return false;
					return (currentEngine().extensionForFileName(name) != FileExtension.Other || new File(dir, name).isDirectory());
				});
			} catch (final Exception e) {
				return new Object[0];
			}
		}

		@Override
		public Object getParent(final Object element) {
			return ((File) element).getParentFile();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return element instanceof File && ((File) element).isDirectory();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			// TODO Auto-generated method stub

		}

		@Override
		public Image getImage(final Object element) {
			final Engine engine = currentEngine();
			final FileExtension gt = engine.extensionForFileName((((File) element).toString()));
			return engine.image(gt);
		}

		@Override
		public StyledString getStyledText(final Object element) {
			if (((File) element).isFile())
				return new StyledString(((File) element).getName(), StyledString.QUALIFIER_STYLER);
			else
				return new StyledString(((File) element).getName());
		}

		@Override
		public String getText(final Object element) {
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

	private void createProjectEditor(final Composite parent, final Object groupLayoutData) {
		projectEditor = new UI.ProjectSelectionBlock(parent, null, this, groupLayoutData, null);
	}

	private IProject selectedProject() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(projectEditor.text.getText());
		} catch (final Exception e) {
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

		final PreferenceStore dummyPrefStore = new PreferenceStore();
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
		final ClonkFolderContentProvider prov = new ClonkFolderContentProvider();
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

		for (final MenuItem item : new MenuItem[] {linkMenuItem, importMenuItem, refreshMenuItem, lookForID})
			item.addSelectionListener(this);
		folderTree.getTree().setMenu(treeMenu);

		parent.layout();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
		if (!openInCurrentProject.getEnabled())
			return;
		IProject proj = null;
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) selection;
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

	private void refreshTree(final boolean onlyIfInputChanged) {
		final Engine engine = currentEngine();
		final String clonkPath = engine != null ? engine.settings().gamePath : null;
		final File clonkFolder = (clonkPath != null && !clonkPath.equals("")) //$NON-NLS-1$
			? new File( clonkPath)
			: new File("/"); //$NON-NLS-1$
		if (!onlyIfInputChanged || !Utilities.eq(folderTree.getInput(), clonkFolder))
			folderTree.setInput(clonkFolder);
	}

	protected Engine currentEngine() {
		final IProject p = selectedProject();
		Engine engine = null;
		if (p != null && p.isOpen()) {
			final ClonkProjectNature nature = ClonkProjectNature.get(p);
			if (nature != null)
				engine = nature.index().engine();
		}
		if (engine == null)
			engine = Core.instance().activeEngine();
		return engine;
	}

	@Override
	public void doubleClick(final DoubleClickEvent event) {
		if (event.getSource() == folderTree)
			linkSelection();
	}

	private void linkSelection() {
		for (final Object f : ((IStructuredSelection)folderTree.getSelection()).toArray()) {
			final File sel = (File) f;
			if (sel.isDirectory()) {
				UI.message(String.format(Messages.ClonkFolderView_JustAFolder, sel.getName()));
				return;
			}
			final IProject proj = selectedProject();
			if (proj != null)
				LinkC4GroupFileHandler.linkC4GroupFile(proj, sel);
		}
	}

	private void importSelection() {
		for (final Object f : ((IStructuredSelection)folderTree.getSelection()).toArray()) {
			final File sel = (File)f;
			final IProject proj = selectedProject();
			if (proj != null)
				QuickImportHandler.importFiles(getSite().getShell(), proj, sel);
		}
	}

	@Override
	public void widgetSelected(final SelectionEvent e) {
		if (e.getSource() == openInCurrentProject)
			updateProjectChooserEnablization();
		else if (e.getSource() == projectEditor.addButton) {
			final IProject project = UI.selectClonkProject(selectedProject());
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
		final InputDialog inputDialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
    		"Look for ID", Messages.ClonkFolderView_LookForIDPromptCaption, "CLNK", null); //$NON-NLS-1$
		if (inputDialog.open() != Window.OK)
			return;

		final Pattern idPattern = Pattern.compile(String.format("^id\\=%s$", inputDialog.getValue()), Pattern.CASE_INSENSITIVE|Pattern.MULTILINE); //$NON-NLS-1$
		final Object[] selection = ((IStructuredSelection)folderTree.getSelection()).toArray();
		new DefCoreTextSearch(Messages.ClonkFolderView_LookForIDJobDescription, selection, idPattern).schedule();
	}

	@Override
	public void widgetDefaultSelected(final SelectionEvent e) {
		if (e.getSource() == projectEditor.text) {
			Core.instance().getPreferenceStore().setValue(PREF_PROJECT_TO_CREATE_LINK_IN, projectEditor.text.getText());
			refreshTree(true);
		}
	}

	public void update() {
		refreshTree(false);
	}

}
