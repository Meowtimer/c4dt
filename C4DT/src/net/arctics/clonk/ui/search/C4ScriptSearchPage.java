package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.Utilities.fileEditedBy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Sink;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.search.ui.IReplacePage;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class C4ScriptSearchPage extends DialogPage implements ISearchPage, IReplacePage {
	private Text templateText;
	private Text replacementText;
	private ISearchPageContainer container;
	
	public C4ScriptSearchPage() {}

	public C4ScriptSearchPage(String title) {
		super(title);
	}

	public C4ScriptSearchPage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void createControl(Composite parent) {
		Composite ctrl = new Composite(parent, SWT.NONE);
		setControl(ctrl);
		GridLayout gl_ctrl = new GridLayout(2, false);
		ctrl.setLayout(gl_ctrl);
		
		Label presetLabel = new Label(ctrl, SWT.NONE);
		presetLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		presetLabel.setText("Template Preset");
		
		Combo presetCombo = new Combo(ctrl, SWT.READ_ONLY);
		GridData gd_presetCombo = new GridData(GridData.FILL_HORIZONTAL);
		gd_presetCombo.widthHint = 568;
		presetCombo.setLayoutData(gd_presetCombo);
		
		Label customTemplateLabel = new Label(ctrl, SWT.NONE);
		customTemplateLabel.setText("Custom Template");
		customTemplateLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		templateText = new Text(ctrl, SWT.BORDER);
		GridData gd_templateText = new GridData(GridData.FILL_BOTH);
		gd_templateText.widthHint = 527;
		gd_templateText.heightHint = 150;
		templateText.setLayoutData(gd_templateText);
		
		Label replacementLabel = new Label(ctrl, SWT.NONE);
		replacementLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		replacementLabel.setText("Replacement");
		
		replacementText = new Text(ctrl, SWT.BORDER);
		GridData gd_replacementText = new GridData(GridData.FILL_BOTH);
		gd_replacementText.heightHint = 168;
		replacementText.setLayoutData(gd_replacementText);
	}

	@Override
	public boolean performAction() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IStructuredSelection sel = new StructuredSelection(fileEditedBy(editor));
		if (sel == null)
			return false;
		final List<Script> scope = new LinkedList<Script>();
		Set<IProject> projects = new HashSet<IProject>();
		switch (container.getSelectedScope()) {
		case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
			for (Object s : sel.toArray())
				if (s instanceof IResource) {
					IProject proj = ((IResource)s).getProject();
					if (projects.contains(proj))
						continue;
					projects.add(proj);
					ClonkProjectNature nature = ClonkProjectNature.get(proj);
					if (nature != null)
						nature.index().allScripts(new Sink<Script>() {
							@Override
							public void receivedObject(Script item) {
								scope.add(item);
							}
						});
				}
			break;
		case ISearchPageContainer.SELECTION_SCOPE:
			for (Object s : sel.toArray())
				if (s instanceof IResource) {
					Script script = Script.get((IResource)s, true);
					if (script != null)
						scope.add(script);
				}
			break;
		case ISearchPageContainer.WORKSPACE_SCOPE:
			for (IProject proj : ClonkProjectNature.clonkProjectsInWorkspace()) {
				ClonkProjectNature nature = ClonkProjectNature.get(proj);
				if (nature != null)
					nature.index().allScripts(new Sink<Script>() {
						@Override
						public void receivedObject(Script item) {
							scope.add(item);
						}
					});
			}
			break;
		default:
			return false;
		}
		NewSearchUI.runQueryInBackground(new C4ScriptSearchQuery(templateText.getText(), null, scope));
		return true;
	}
	
	@Override
	public boolean performReplace() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void setContainer(ISearchPageContainer container) {
		this.container = container;
	}
}
