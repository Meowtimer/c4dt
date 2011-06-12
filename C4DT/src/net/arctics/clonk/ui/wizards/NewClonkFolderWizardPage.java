package net.arctics.clonk.ui.wizards;

import java.lang.reflect.Field;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public class NewClonkFolderWizardPage extends WizardPage {

	public interface IAdditionToTextField {
		public void fill(Composite container, Text textField);
	}
	
	protected Text containerText;
	protected Text fileText;
	private String folderExtension;
	protected ISelection selection;
	protected IProject project;
	
	/**
	 * Return group type. Used for setting a fitting icon for the page.
	 * @return
	 */
	protected GroupType groupType() {
		return null;
	}
	
	public NewClonkFolderWizardPage(ISelection selection) {
		super("wizardPage"); //$NON-NLS-1$
		setTitle(Messages.NewClonkFolderWizardPage_Title);
		setDescription(Messages.NewClonkFolderWizardPage_Description);
		this.selection = selection;
		GroupType groupType = this.groupType();
		if (groupType != null) {
			Engine engine = ClonkProjectNature.getEngine(selection);
			if (engine != null)
				setImageDescriptor(engine.imageDescriptor(groupType.name()+"Big"));
		}
	}
	
	public Text addTextField(String label) {
		return addTextField(label, null, null, null);
	}
	
	public Text addTextField(String label, IAdditionToTextField addition) {
		return addTextField(label, null, null, addition);
	}
	
	public Text addTextField(String label, final Object context, final String property, IAdditionToTextField addition) {
		Composite container = (Composite) getControl();
		Label labelObj = new Label(container, SWT.NULL);
		labelObj.setText(label);
		Text result = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		result.setLayoutData(gd);
		result.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				if (context != null && property != null) {
					try {
						Field field = context.getClass().getField(property);
						field.set(context, ((Text)event.widget).getText());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				dialogChanged();
			}
		});
		if (addition != null)
			addition.fill(container, result);
		else {
			// fill last cell with dummy
			new Label(container, SWT.NULL);
		}
		return result;
	}
	
	@Override
	public void createControl(Composite parent) {
		actuallyCreateControl(parent);
		initialize();
		dialogChanged();
	}

	protected void actuallyCreateControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		
		containerText = addTextField(Messages.NewClonkFolderWizardPage_ContainerText, new IAdditionToTextField() {
			public void fill(Composite container, Text textField) {
				Button button = new Button(container, SWT.PUSH);
				button.setText(Messages.NewClonkFolderWizardPage_BrowseContainer);
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						handleBrowse();
					}
				});
			}
		});
		fileText = addTextField(Messages.NewClonkFolderWizardPage_FolderText);
	}
	
	/**
	 * Ensures that both text fields are set.
	 */
	protected void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));
		String fileName = getFileName();

		if (getContainerName().length() == 0) {
			updateStatus(Messages.NewClonkFolderWizardPage_NoContainer);
			return;
		}
		if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus(Messages.NewClonkFolderWizardPage_ContainerDoesNotExist);
			return;
		}
		if (!container.isAccessible()) {
			updateStatus(Messages.NewClonkFolderWizardPage_ContainerMustBeWritable);
			return;
		}
		if (fileName.length() == 0) {
			updateStatus(Messages.NewClonkFolderWizardPage_NoFolderName);
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus(Messages.NewClonkFolderWizardPage_FolderNameInvalid);
			return;
		}
		updateStatus(null);
	}
	
	protected void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	protected void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				Messages.NewClonkFolderWizardPage_SelectContainerTitle);
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}
	
	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	protected void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
				project = ((IResource)obj).getProject();
			}
		}
		fileText.setText(Messages.NewClonkFolderWizard_FolderFileName);
	}
	
	public String getContainerName() {
		return containerText.getText();
	}
	
	public String getFileName() {
		if (fileText.getText().equals("") || folderExtension == null)
			return "";
		StringBuilder builder = new StringBuilder(fileText.getText().length()+1+folderExtension.length());
		builder.append(fileText.getText());
		if (!folderExtension.startsWith("."))
			builder.append(".");
		builder.append(folderExtension);
		return builder.toString();
	}
	
	public String getFolderExtension() {
		return folderExtension;
	}
	
	public void setFolderExtension(String value) {
		folderExtension = value;
	}

	public Text getContainerText() {
		return containerText;
	}

	public Text getFileText() {
		return fileText;
	}

}
