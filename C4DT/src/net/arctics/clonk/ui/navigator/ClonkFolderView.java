package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.io.FilenameFilter;

import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class ClonkFolderView extends ViewPart {

	private static class ClonkFolderContentProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			try {
				File folder = (File) parentElement;
				return folder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return !name.startsWith(".") && C4Group.getGroupType(name) != C4GroupType.OtherGroup || new File(dir, name).isDirectory();
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
	
	private class TreeListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (e.getSource() == folderTree.getTree()) {
				File sel = (File) ((IStructuredSelection)folderTree.getSelection()).getFirstElement();
				IProject proj = selectedProject();
				LinkC4GroupFileHandler.linkC4GroupFile(proj, sel);
			}
		}

		@Override
		public void mouseDown(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseUp(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private Composite checkBoxes;
	private TreeViewer folderTree;
	private Sash sash;
	
	private Button openInCurrentProject;
	private Button removeLinkedFilesOnShutdown;
	private Text projText;
	private Button projButton;
	
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
		grp.setLayout(new GridLayout(2, false));
		grp.setLayoutData(groupLayoutData);
		
		// Text plus button
		projText = new Text(grp, SWT.SINGLE | SWT.BORDER);
		projText.setFont(parent.getFont());
		projText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projButton = new Button(grp, SWT.PUSH);
		projButton.setText("Browse");
		
		// Install listener
		SelectionListener listener = new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == projButton) {
					IProject project = Utilities.clonkProjectSelectionDialog(selectedProject());
					if (project != null)
						projText.setText(project.getName());
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		};
		projButton.addSelectionListener(listener);
		
	}
	
	private IProject selectedProject() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(projText.getText());
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new FormLayout());
		
		checkBoxes = new Composite(parent, SWT.NO_SCROLL);
		
		checkBoxes.setLayout(new FormLayout());
		
		openInCurrentProject = new Button(checkBoxes, SWT.CHECK);
		openInCurrentProject.setText("Open In Current Project");
		openInCurrentProject.setLayoutData(createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(0, 5),
			new FormAttachment(0, 5+openInCurrentProject.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)
		));

		removeLinkedFilesOnShutdown = new Button(checkBoxes, SWT.CHECK);
		removeLinkedFilesOnShutdown.setText("Remove Linked Files On Shutdown");
		removeLinkedFilesOnShutdown.setLayoutData(createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(openInCurrentProject, 5),
			new FormAttachment(openInCurrentProject, 40)
		));
		
		createProjectEditor(checkBoxes, createFormData(
			new FormAttachment(0, 5),
			new FormAttachment(100, 5),
			new FormAttachment(removeLinkedFilesOnShutdown, 5),
			new FormAttachment(100, 5)
		));
		
		sash = new Sash(parent, SWT.HORIZONTAL);
		folderTree = new TreeViewer(parent, SWT.NONE);
		
		checkBoxes.setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(0, 0),
			new FormAttachment(sash, 0)
		));
		
		final FormData sashData;
		sash.setLayoutData(sashData = createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(40, 0),
			null
		));
		sash.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				sashData.top = new FormAttachment(0, event.y);
				parent.layout();
			}
		});
		
		folderTree.getTree().setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(sash, 0),
			new FormAttachment(100, 0)
		));
		ClonkFolderContentProvider prov = new ClonkFolderContentProvider();
		folderTree.setContentProvider(prov);
		folderTree.setInput(new File(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.GAME_PATH)));
		folderTree.setLabelProvider(prov);
		folderTree.getTree().addMouseListener(new TreeListener());
		
		parent.layout();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
