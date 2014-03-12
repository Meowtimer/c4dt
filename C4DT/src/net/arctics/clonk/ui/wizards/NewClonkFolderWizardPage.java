package net.arctics.clonk.ui.wizards;

import java.lang.reflect.Field;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.index.Engine;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
	protected Text folderText;
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

	public NewClonkFolderWizardPage(final ISelection selection) {
		super("wizardPage"); //$NON-NLS-1$
		setTitle(Messages.NewClonkFolderWizardPage_Title);
		setDescription(Messages.NewClonkFolderWizardPage_Description);
		this.selection = selection;
		final GroupType groupType = this.groupType();
		if (groupType != null) {
			final Engine engine = ClonkProjectNature.engineFromSelection(selection);
			if (engine != null)
				setImageDescriptor(engine.imageDescriptor(groupType.name()+"Big"));
		}
	}

	public Text addTextField(final String label) {
		return addTextField(label, null, null, null);
	}

	public Text addTextField(final String label, final IAdditionToTextField addition) {
		return addTextField(label, null, null, addition);
	}

	public Text addTextField(final String label, final Object context, final String property, final IAdditionToTextField addition) {
		final Composite container = (Composite) getControl();
		final Label labelObj = new Label(container, SWT.NULL);
		labelObj.setText(label);
		final Text result = new Text(container, SWT.BORDER | SWT.SINGLE);
		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		result.setLayoutData(gd);
		result.addModifyListener(event -> {
			if (context != null && property != null)
				try {
					final Field field = context.getClass().getField(property);
					field.set(context, ((Text)event.widget).getText());
				} catch (final Exception e) {
					e.printStackTrace();
				}
			dialogChanged();
		});
		if (addition != null)
			addition.fill(container, result);
		else
			// fill last cell with dummy
			new Label(container, SWT.NULL);
		return result;
	}

	@Override
	public void createControl(final Composite parent) {
		layout(parent);
		fields();
		containerText = addTextField(Messages.NewClonkFolderWizardPage_ContainerText, (container, textField) -> {
			final Button button = new Button(container, SWT.PUSH);
			button.setText(Messages.NewClonkFolderWizardPage_BrowseContainer);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					handleBrowse();
				}
			});
		});
		initialize();
		dialogChanged();
	}

	protected void layout(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		final GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
	}

	protected void fields() {
		folderText = addTextField(Messages.NewClonkFolderWizardPage_FolderText);
	}

	/**
	 * Ensures that both text fields are set.
	 */
	protected void dialogChanged() {
		if (containerText == null)
			return;
		final IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));
		final String fileName = getFileName();

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

	protected void updateStatus(final String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	protected void handleBrowse() {
		final ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				Messages.NewClonkFolderWizardPage_SelectContainerTitle);
		if (dialog.open() == Window.OK) {
			final Object[] result = dialog.getResult();
			if (result.length == 1)
				containerText.setText(((Path) result[0]).toString());
		}
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	protected void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			final Object obj = ssel.getFirstElement();
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
		folderText.setText(Messages.NewClonkFolderWizard_FolderFileName);
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getFileName() {
		if (folderText.getText().equals("") || folderExtension == null)
			return "";
		final StringBuilder builder = new StringBuilder(folderText.getText().length()+1+folderExtension.length());
		builder.append(folderText.getText());
		if (!folderExtension.startsWith("."))
			builder.append(".");
		builder.append(folderExtension);
		return builder.toString();
	}

	public String getFolderExtension() {
		return folderExtension;
	}

	public void setFolderExtension(final String value) {
		folderExtension = value;
	}

	public Text getContainerText() {
		return containerText;
	}

	public Text getFileText() {
		return folderText;
	}

}
