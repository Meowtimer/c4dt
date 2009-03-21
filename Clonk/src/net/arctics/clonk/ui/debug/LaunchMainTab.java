package net.arctics.clonk.ui.debug;

import java.util.Collection;
import java.util.LinkedList;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.debug.ClonkLaunchConfigurationDelegate;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;
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
	private Text fProjText;
	private Button fProjButton;
	
	/** Scenario selection widgets */
	private Text fScenText;
	private Button fScenButton;
	
	/** Launch options */
	private Button fFullscreenButton;
	private Button fConsoleButton;
	private Button fRecordButton;
	
	/** Listener used to track changes in widgets */
	private class WidgetListener implements ModifyListener, SelectionListener {

		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}

		public void widgetDefaultSelected(SelectionEvent e) {
		}

		public void widgetSelected(SelectionEvent e) {
			if(e.getSource() == fProjButton)
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

	}
	
	/** 
	 * Create widgets for project selection
	 * @see org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab#createProjectEditor
	 */
	private void createProjectEditor(Composite parent)
	{
		
		// Create widget group
		Group grp = new Group(parent, SWT.NONE);
		grp.setText("Project:");
		grp.setLayout(new GridLayout(2, false));
		grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grp.setFont(parent.getFont());
		
		// Text plus button
		fProjText = new Text(grp, SWT.SINGLE | SWT.BORDER);
		fProjText.setFont(parent.getFont());
		fProjText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProjButton = createPushButton(grp, "Browse...", null);
		
		// Install listener
		fProjText.addModifyListener(fListener);
		fProjButton.addSelectionListener(fListener);
		
	}
	
	
	/** 
	 * Create widgets for scenario selection
	 */
	private void createScenarioEditor(Composite parent)
	{
		
		// Create widget group
		Group grp = new Group(parent, SWT.NONE);
		grp.setText("Scenario:");
		grp.setLayout(new GridLayout(2, false));
		grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grp.setFont(parent.getFont());
		
		// Text plus button
		fScenText = new Text(grp, SWT.SINGLE | SWT.BORDER);
		fScenText.setFont(parent.getFont());
		fScenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fScenButton = createPushButton(grp, "Browse...", null);
		
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
		grp.setText("Launch mode:");
		grp.setLayout(new GridLayout(2, false));
		grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grp.setFont(parent.getFont());
		
		// Full screen, console and record switches
		fConsoleButton = createRadioButton(grp, "Console");
		fFullscreenButton = createRadioButton(grp, "Fullscreen");
		fRecordButton = createCheckButton(grp, "Create record");
		
		// Install listener
		fFullscreenButton.addSelectionListener(fListener);
		fConsoleButton.addSelectionListener(fListener);
		fRecordButton.addSelectionListener(fListener);
		
	}
	
	/** The name of the tab */
	public String getName() {
		return "Main";
	}
	
	public void initializeFrom(ILaunchConfiguration conf) {
		
		try {

			// Read attributes
			fProjText.setText(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, ""));
			fScenText.setText(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, ""));
			fFullscreenButton.setSelection(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, false));
			fConsoleButton.setSelection(!fFullscreenButton.getSelection());
			fRecordButton.setSelection(conf.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, false));
			
		} catch (CoreException e) {
			setErrorMessage(e.getStatus().getMessage());
			
			// Set defaults
			fScenText.setText("");	
			fProjText.setText("");
			fFullscreenButton.setSelection(false);
			fConsoleButton.setSelection(true);
			fRecordButton.setSelection(false);
			
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy wc) {
		
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, fProjText.getText());
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, fScenText.getText());
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, fFullscreenButton.getSelection());
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, fRecordButton.getSelection());
			
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy wc) {
		
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, "");
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, "");
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, false);
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, false);
		
	}

	public IProject validateProject() {
		
		// Must be a valid path segment
		String projectName = fProjText.getText();
		if(!new Path("").isValidSegment(projectName)) {
			setErrorMessage("Invalid project name!");
			return null;
		}
		
		// Search project in workspace
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if(project == null) {
			setErrorMessage("Project " + projectName + " does not exist!");
			return null;
		}
		
		// Project must be open
		if(!project.isOpen()) {
			setErrorMessage("Project " + projectName + " is not open!");
			return null;
		}
	
		// Everything okay
		return project;
	}

	public IResource validateScenario(IProject project) {
		
		// Must be a valid scenario file name
		String scenName = fScenText.getText();
		if(C4Group.getGroupType(scenName) != C4Group.C4GroupType.ScenarioGroup) {
			setErrorMessage("Scenario name invalid!");
			return null;
		}
		
		// Must exist in project
		// TODO: Allow plain files, too? Launch should not be a problem, at least
		//       until we do more magic with the scenario's innards.
		IResource scenFile = project.getFolder(scenName);
		if(!scenFile.exists()) {
			setErrorMessage("Scenario doesn't exist!");
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
		
		// Create dialog listing all Clonk projects
		ElementListSelectionDialog dialog
			= new ElementListSelectionDialog(getShell(), new ClonkLabelProvider());
		dialog.setTitle("Choose Clonk project");
		dialog.setMessage("Please choose a project");
		dialog.setElements(Utilities.getClonkProjects());
		
		// Set selection
		dialog.setInitialSelections(new Object [] { validateProject() });
		
		// Show
		if(dialog.open() == Window.OK) {
			IProject project = (IProject) dialog.getFirstResult();
			fProjText.setText(project.getName());
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
					C4Group.extensionToGroupTypeMap.get(res.getFileExtension());
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
		dialog.setTitle("Choose Clonk scenario");
		dialog.setMessage("Please choose a scenario");
		dialog.setElements(scenarios.toArray());
		
		// Show
		if(dialog.open() == Window.OK) {
			IResource scen = (IResource) dialog.getFirstResult();
			fProjText.setText(scen.getProject().getName());
			fScenText.setText(scen.getProjectRelativePath().toString());
		}
		
	}

	@Override
	public Image getImage() {
		return ClonkCore.getDefault().getIconImage("Clonk_engine");
	}
	
}
