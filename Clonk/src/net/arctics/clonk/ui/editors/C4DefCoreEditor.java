package net.arctics.clonk.ui.editors;

import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
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
			form.setText("hallo");
			form.getBody().setLayout(new GridLayout(2,false));
			
//			SectionPart part = new SectionPart(form.getBody(),toolkit,Section.EXPANDED);
//			managedForm.addPart(part);
			
			Label lab = toolkit.createLabel(form.getBody(), "blub",SWT.LEFT);
			
			toolkit.createButton(form.getBody(), "test", SWT.PUSH);
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
	protected void addPages() {
		try {
			addPage(new DefCoreSectionPage(this, "DefCore", "[DefCore]"));
			int index = addPage(new RawSourcePage(this, RawSourcePage.PAGE_ID,"DefCore.txt"),this.getEditorInput());
			// editors as pages are not able to handle tab title strings
			// so here is a dirty trick:
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
