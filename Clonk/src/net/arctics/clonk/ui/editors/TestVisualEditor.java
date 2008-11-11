package net.arctics.clonk.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * This is a FormEditor howto
 * @author ZokRadonh
 *
 */
public class TestVisualEditor extends FormEditor {

	private String identifier;
	
	public TestVisualEditor(String identifierName) {
		identifier = identifierName;
	}
	
	public static class EditFunctionPage extends FormPage {

		public EditFunctionPage(FormEditor editor, String id, String title) {
			super(editor, id, title);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
		 */
		@Override
		protected void createFormContent(IManagedForm managedForm) {
			super.createFormContent(managedForm);
			
			FormToolkit toolkit = managedForm.getToolkit();
			
			toolkit.createLabel(managedForm.getForm(), "blub");
			// TODO Auto-generated method stub
		}
	}
	
	@Override
	protected void addPages() {
		try {
			addPage(new EditFunctionPage(this, "edit" + identifier, identifier + "()"));
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
