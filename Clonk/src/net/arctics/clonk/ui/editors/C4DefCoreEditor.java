package net.arctics.clonk.ui.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class C4DefCoreEditor extends FormEditor {

	public C4DefCoreEditor() {
	}

	public static class DefCoreSectionPage extends FormPage {

		public DefCoreSectionPage(FormEditor editor, String id, String title) {
			super(editor, id, title);
		}

		protected void createFormContent(IManagedForm managedForm) {
			super.createFormContent(managedForm);

			FormToolkit toolkit = managedForm.getToolkit();
			ScrolledForm form = managedForm.getForm();
			toolkit.decorateFormHeading(form.getForm());
			form.setText("DefCore main options");
			
			GridLayout layout = new GridLayout(1,false);
			form.getBody().setLayout(layout);
			form.getBody().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			
			SectionPart part = new SectionPart(form.getBody(),toolkit,Section.CLIENT_INDENT | Section.TITLE_BAR | Section.EXPANDED);
			
			part.getSection().setText("section text");
			part.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			Composite sectionComp = toolkit.createComposite(part.getSection());
			sectionComp.setLayout(new GridLayout());
			sectionComp.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			part.getSection().setClient(sectionComp);
			
			toolkit.createLabel(sectionComp, "Title bar dingens halt");
			
			toolkit.createLabel(sectionComp, "blub",SWT.LEFT);
//			lab.setText("flub");
//			lab.setVisible(true);
		}
	}

	public static class RawSourcePage extends TextEditor {
		
		public static final String PAGE_ID = "rawDefCore";
		
		private FormEditor fEditor;
		private String id;
		private String title;
		
		public RawSourcePage(FormEditor editor, String id, String title) {
			fEditor = editor;
			this.id = id;
			setPartName(title);
			setContentDescription(title);
			this.title = title;
		}

		public void resetPartName() {
			setPartName(title);
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		IResource res = (IResource) input.getAdapter(IResource.class);
		if (res != null) {
			setPartName(res.getParent().getName() + "/" + res.getName());
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new DefCoreSectionPage(this, "DefCore", "[DefCore]"));
			int index = addPage(new RawSourcePage(this, RawSourcePage.PAGE_ID,"DefCore.txt"),this.getEditorInput());
			// editors as pages are not able to handle tab title strings
			// so here is a dirty trick:
			if (getContainer() instanceof CTabFolder)
				((CTabFolder)getContainer()).getItem(index).setText("DefCore.txt");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

}
