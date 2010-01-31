package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.io.FilenameFilter;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class ClonkFolderView extends ViewPart implements ISelectionListener, IPropertyChangeListener, IDoubleClickListener, SelectionListener {
	
	public static final String PREF_CREATE_LINK_IN_CURRENT_PROJECT = ClonkFolderView.class.getSimpleName() + "_CreateLinkInCurrentProj"; //$NON-NLS-1$
	public static final String PREF_DELETE_LINKS_ON_SHUTDOWN = ClonkFolderView.class.getSimpleName() + "_DeleteLinksOnShutdown"; //$NON-NLS-1$
	public static final String PREF_PROJECT_TO_CREATE_LINK_IN = ClonkFolderView.class.getSimpleName() + "_ProjectToCreateLinkIn"; //$NON-NLS-1$
	
	private static class ClonkFolderContentProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			try {
				File folder = (File) parentElement;
				return folder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return !name.startsWith(".") && C4Group.getGroupType(name) != C4GroupType.OtherGroup || new File(dir, name).isDirectory(); //$NON-NLS-1$
					}
				});
			} catch (Exception e) {
				return new Object[0];
			}
		}

		@Override
		public Object getParent(Object element) {
			return ((File)element).getParentFile();
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof File && ((File)element).isDirectory();
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
			switch (C4Group.getGroupType(((File)element).toString())) {
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
			if (((File)element).isFile())
				return new StyledString(((File)element).getName(), StyledString.QUALIFIER_STYLER);
			else
				return new StyledString(((File)element).getName());
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
	private Text projText;
	private Button projButton;
	private Menu treeMenu;
	private MenuItem importMenuItem;
	private MenuItem linkMenuItem;
	
	private static FormData createFormData(FormAttachment left, FormAttachment right, FormAttachment top, FormAttachment bottom) {
		FormData result = new FormData();
		result.left   = left;
		result.top    = top;
		result.right  = right;
		result.bottom = bottom;
		return result;
	}
	
	private void createProjectEditor(Composite parent, Object groupLayoutData)
	{
		
		// Create widget group
		Composite grp = new Composite(parent, SWT.NONE);
		grp.setLayout(new GridLayout(3, false));
		grp.setLayoutData(groupLayoutData);
		
		// Text plus button
		Label label = new Label(grp, SWT.HORIZONTAL | SWT.LEFT);
		label.setText(Messages.ClonkFolderView_Project);
		projText = new Text(grp, SWT.SINGLE | SWT.BORDER);
		projText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projText.addSelectionListener(this);
		projButton = new Button(grp, SWT.PUSH);
		projButton.setText(Messages.ClonkFolderView_Browse);
		
		// Install listener
		projButton.addSelectionListener(this);
		
	}
	
	private IProject selectedProject() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(projText.getText());
		} catch (Exception e) {
			return null;
		}
	}
	
	private void updateProjectChooserEnablization() {
		projButton.setEnabled(!openInCurrentProject.getSelection());
		projText.setEnabled(!openInCurrentProject.getSelection());
		ClonkCore.getDefault().getPreferenceStore().setValue(PREF_CREATE_LINK_IN_CURRENT_PROJECT, openInCurrentProject.getSelection());
	}
	
	@Override
	public void createPartControl(final Composite parent) {
		
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);
		ClonkCore.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		
		parent.setLayout(new FormLayout());

		optionsComposite = new Composite(parent, SWT.NO_SCROLL);
		optionsComposite.setLayout(new FormLayout());

		openInCurrentProject = new Button(optionsComposite, SWT.CHECK);
		openInCurrentProject.setText(Messages.ClonkFolderView_OpenInCurrentProject);
		openInCurrentProject.setLayoutData(createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(0, 5),
			new FormAttachment(0, 5+openInCurrentProject.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)
		));
		openInCurrentProject.addSelectionListener(this);

		removeLinkedFilesOnShutdown = new Button(optionsComposite, SWT.CHECK);
		removeLinkedFilesOnShutdown.setText(Messages.ClonkFolderView_RemoveLinkedFilesOnShutdown);
		removeLinkedFilesOnShutdown.setLayoutData(createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(openInCurrentProject, 5),
			new FormAttachment(openInCurrentProject, 40)
		));
		removeLinkedFilesOnShutdown.addSelectionListener(this);
		
		createProjectEditor(optionsComposite, createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(removeLinkedFilesOnShutdown, 5),
			new FormAttachment(100, 5)
		));
		
		optionsComposite.setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(100, -optionsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y),
			new FormAttachment(100, 0)
		));
		
		openInCurrentProject.setSelection(ClonkCore.getDefault().getPreferenceStore().getBoolean(PREF_CREATE_LINK_IN_CURRENT_PROJECT));
		removeLinkedFilesOnShutdown.setSelection(ClonkCore.getDefault().getPreferenceStore().getBoolean(PREF_DELETE_LINKS_ON_SHUTDOWN));
		updateProjectChooserEnablization();
		
		folderTree = new TreeViewer(parent, SWT.NONE);
		folderTree.getTree().setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(0, 0),
			new FormAttachment(optionsComposite, 0)
		));
		ClonkFolderContentProvider prov = new ClonkFolderContentProvider();
		folderTree.setContentProvider(prov);
		folderTree.setInput(new File(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.GAME_PATH)));
		folderTree.setLabelProvider(prov);
		folderTree.addDoubleClickListener(this);
		
		treeMenu = new Menu(getSite().getShell(), SWT.POP_UP);
		linkMenuItem = new MenuItem(treeMenu, SWT.PUSH);
		linkMenuItem.setText(Messages.ClonkFolderView_Link);
		importMenuItem = new MenuItem(treeMenu, SWT.PUSH);
		importMenuItem.setText(Messages.ClonkFolderView_Import);
		linkMenuItem.addSelectionListener(this);
		importMenuItem.addSelectionListener(this);
		folderTree.getTree().setMenu(treeMenu);
		
		parent.layout();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!openInCurrentProject.getSelection())
			return;
		IProject proj = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.getFirstElement() instanceof IResource) {
				proj = ((IResource)ssel.getFirstElement()).getProject();
			}
		}
		if (proj != null)
			projText.setText(proj.getName());
	}
	
	@Override
	public void dispose() {
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);
		ClonkCore.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ClonkPreferences.GAME_PATH)) {
			folderTree.setInput(new File(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.GAME_PATH)));
		}
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		if (event.getSource() == folderTree) {
			linkSelection();
		}
	}

	private void linkSelection() {
		File sel = (File) ((IStructuredSelection)folderTree.getSelection()).getFirstElement();
		IProject proj = selectedProject();
		if (proj != null)
			LinkC4GroupFileHandler.linkC4GroupFile(proj, sel);
	}
	
	private void importSelection() {
		File sel = (File) ((IStructuredSelection)folderTree.getSelection()).getFirstElement();
		IProject proj = selectedProject();
		if (proj != null) {
			QuickImportHandler.importFiles(getSite().getShell(), proj, sel);
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == openInCurrentProject) {
			updateProjectChooserEnablization();
		}
		else if (e.getSource() == removeLinkedFilesOnShutdown) {
			ClonkCore.getDefault().getPreferenceStore().setValue(PREF_DELETE_LINKS_ON_SHUTDOWN, removeLinkedFilesOnShutdown.getSelection());
		}
		else if (e.getSource() == projButton) {
			IProject project = Utilities.clonkProjectSelectionDialog(selectedProject());
			if (project != null)
				projText.setText(project.getName());
		}
		else if (e.getSource() == importMenuItem) {
			importSelection();
		}
		else if (e.getSource() == linkMenuItem) {
			linkSelection();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		if (e.getSource() == projText) {
			ClonkCore.getDefault().getPreferenceStore().setValue(PREF_PROJECT_TO_CREATE_LINK_IN, projText.getText());
		}
	}

}
