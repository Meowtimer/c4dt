package net.arctics.clonk.ui.debug;

import java.util.Collection;
import java.util.LinkedList;

import net.arctics.clonk.debug.ClonkLaunchConfigurationDelegate;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class LaunchMainTab extends AbstractLaunchConfigurationTab {
	
	/** Project selection widgets */
	private UI.ProjectEditorBlock projectEditor;
	
	/** Scenario selection widgets */
	private Text fScenText;
	private Button fScenButton;
	
	/** Launch options */
	private Button fFullscreenButton;
	private Button fConsoleButton;
	private Button fRecordButton;
	private Text fCustomOptions;
	
	/** Listener used to track changes in widgets */
	private class WidgetListener implements ModifyListener, SelectionListener {

		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}

		public void widgetDefaultSelected(SelectionEvent e) {
		}

		public void widgetSelected(SelectionEvent e) {
			if(e.getSource() == projectEditor.AddButton)
				chooseClonkProject();
			else if(e.getSource() == fScenButton)
				chooseScenario();
			else
				updateLaunchConfigurationDialog();
		}
		
	}
	
	WidgetListener fListener = new WidgetListener();
	
	/** Places all needed widgets into the tab */
	public void createControl(Composite parent) {

		// Create top-level composite
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		setControl(comp);
		
		// Put editors
		createProjectEditor(comp);
		createScenarioEditor(comp);
		createLaunchOptionsEditor(comp);
		createCustomOptionsEditor(comp);

	}
	
	/** 
	 * Create widgets for project selection
	 * @see org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab#createProjectEditor
	 */
	private void createProjectEditor(Composite parent)
	{
		projectEditor = new UI.ProjectEditorBlock(parent, fListener, fListener, null, Messages.LaunchMainTab_ProjectTitle);
	}
	
	
	/** 
	 * Create widgets for scenario selection
	 */
	private void createScenarioEditor(Composite parent)
	{
		
		// Create widget group
		Group grp = new Group(parent, SWT.NONE);
		grp.setText(Messages.LaunchMainTab_ScenarioTitle);
		grp.setLayout(new GridLayout(2, false));
		grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grp.setFont(parent.getFont());
		
		// Text plus button
		fScenText = new Text(grp, SWT.SINGLE | SWT.BORDER);
		fScenText.setFont(parent.getFont());
		fScenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fScenButton = createPushButton(grp, Messages.LaunchMainTab_Browse, null);
		
		// Install listener
		fScenText.addModifyListener(fListener);
		fScenButton.addSelectionListener(fListener);
		
	}

	/** 
	 * Create widgets for launch options
	 */
	private void createLaunchOptionsEditor(Composite parent) {
		
		// Create widget group
		Group grp = new Group(parent, SWT.NONE);
		grp.setText(Messages.LaunchMainTab_LaunchMode);
		grp.setLayout(new GridLayout(2, false));
		grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grp.setFont(parent.getFont());
		
		// Full screen, console and record switches
		fConsoleButton = createRadioButton(grp, Messages.LaunchMainTab_Console);
		fFullscreenButton = createRadioButton(grp, Messages.LaunchMainTab_Fullscreen);
		fRecordButton = createCheckButton(grp, Messages.LaunchMainTab_CreateRecord);
		
		// Install listener
		fFullscreenButton.addSelectionListener(fListener);
		fConsoleButton.addSelectionListener(fListener);
		fRecordButton.addSelectionListener(fListener);
		
	}
	
	private void createCustomOptionsEditor(Composite parent) {
		// Create widget group
		Group grp = new Group(parent, SWT.NONE);
		grp.setText(Messages.LaunchMainTab_CustomOptions);
		grp.setLayout(new GridLayout(1, false));
		grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grp.setFont(parent.getFont());
		
		// Custom options
		fCustomOptions = new Text(grp, SWT.SINGLE | SWT.BORDER);
		fCustomOptions.setFont(parent.getFont());
		fCustomOptions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fCustomOptions.addModifyListener(fListener);
	}
	
	/** The name of the tab */
	public String getName() {
		return Messages.LaunchMainTab_Main;
	}
	
	public void initializeFrom(ILaunchConfiguration conf) {
		
		try {

			// Read attributes
			projectEditor.Text.setText(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, "")); //$NON-NLS-1$
			fScenText.setText(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, "")); //$NON-NLS-1$
			fFullscreenButton.setSelection(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, false));
			fConsoleButton.setSelection(!fFullscreenButton.getSelection());
			fRecordButton.setSelection(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, false));
			fCustomOptions.setText(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_CUSTOMARGS, "")); //$NON-NLS-1$
			
		} catch (CoreException e) {
			setErrorMessage(e.getStatus().getMessage());
			
			// Set defaults
			fScenText.setText("");	 //$NON-NLS-1$
			projectEditor.Text.setText(""); //$NON-NLS-1$
			fFullscreenButton.setSelection(false);
			fConsoleButton.setSelection(true);
			fRecordButton.setSelection(false);
			fCustomOptions.setText(""); //$NON-NLS-1$
			
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy wc) {
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, projectEditor.Text.getText());
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, fScenText.getText());
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, fFullscreenButton.getSelection());
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, fRecordButton.getSelection());
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_CUSTOMARGS, fCustomOptions.getText());
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy wc) {
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, ""); //$NON-NLS-1$
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, Util.isMac()); // FIXME: Mac Clonk crashes when run with /console but has windowed mode
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, false);
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_CUSTOMARGS, ""); //$NON-NLS-1$
	}

	public IProject validateProject() {
		
		// Must be a valid path segment
		String projectName = projectEditor.Text.getText();
		if(!new Path("").isValidSegment(projectName)) { //$NON-NLS-1$
			setErrorMessage(Messages.LaunchMainTab_InvalidProjectName);
			return null;
		}
		
		// Search project in workspace
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if(project == null) {
			setErrorMessage(String.format(Messages.LaunchMainTab_ProjectDoesNotExist, projectName));
			return null;
		}
		
		// Project must be open
		if(!project.isOpen()) {
			setErrorMessage(String.format(Messages.LaunchMainTab_ProjectNotOpen, projectName));
			return null;
		}
	
		// Everything okay
		return project;
	}

	public IResource validateScenario(IProject project) {
		
		// Must be a valid scenario file name
		String scenName = fScenText.getText();
		if(C4Group.getGroupType(scenName) != C4Group.C4GroupType.ScenarioGroup) {
			setErrorMessage(Messages.LaunchMainTab_ScenarioNameInvalid);
			return null;
		}
		
		// Must exist in project
		// TODO: Allow plain files, too? Launch should not be a problem, at least
		//       until we do more magic with the scenario's innards.
		IResource scenFile = project.getFolder(scenName);
		if(!scenFile.exists()) {
			setErrorMessage(Messages.LaunchMainTab_ScenarioDoesNotExist);
			return null;
		}
		
		// Done
		return scenFile;
	}
	
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		
		// Reset existing messages
		setErrorMessage(null); setMessage(null);
		
		// Validate scenario
		IProject project = validateProject();
		if(project == null)
			return false;
		if(validateScenario(project) == null)
			return false;
		
		// Done
		return super.isValid(launchConfig);
	}

	public void chooseClonkProject() {
		
		IProject project = Utilities.selectClonkProject(validateProject());
		if (project != null) {
			projectEditor.Text.setText(project.getName());
		}

	}
	
	public void chooseScenario() {

		// Find all available scenarios
		// Note: I really have no idea why we even bother to build a project selection
		//       dialog when the scenario selection doesn't use the available information.
		//       But it's the way Eclipse's built-in tabs work, so whatever...
		final Collection<IResource> scenarios = new LinkedList<IResource>();
		IResourceVisitor scenCollector = new IResourceVisitor() {
			public boolean visit(IResource res) throws CoreException {
				// Top-level
				if(res instanceof IProject)
					return true;
				// Type lookup
				C4Group.C4GroupType type = 
					C4Group.EXTENSION_TO_GROUP_TYPE_MAP.get(res.getFileExtension());
				if(type == C4Group.C4GroupType.ScenarioGroup)
					scenarios.add(res);
				// Only recurse into scenario folders
				return type == C4Group.C4GroupType.FolderGroup;
			}
		};
		for(IProject proj : Utilities.getClonkProjects())
			try {
				proj.accept(scenCollector);
			} catch (CoreException e) {}
		
		// Create dialog with all available scenarios
		ElementListSelectionDialog dialog
			= new ElementListSelectionDialog(getShell(), new ClonkLabelProvider());
		dialog.setTitle(Messages.LaunchMainTab_ChooseClonkScenario);
		dialog.setMessage(Messages.LaunchMainTab_ChooseClonkScenarioPretty);
		dialog.setElements(scenarios.toArray());
		
		// Show
		if(dialog.open() == Window.OK) {
			IResource scen = (IResource) dialog.getFirstResult();
			projectEditor.Text.setText(scen.getProject().getName());
			fScenText.setText(scen.getProjectRelativePath().toString());
		}
		
	}

	@Override
	public Image getImage() {
		return UI.CLONK_ENGINE_ICON; 
	}
	
}
