package net.arctics.clonk.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

public class ClonkImportResourcePage extends WizardResourceImportPage {

	protected IStructuredSelection selection;
	
	protected ClonkImportResourcePage(String name,
			IStructuredSelection selection) {
		super(name, selection);
		this.selection = selection;
	}

	@Override
	protected void createSourceGroup(Composite parent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected ITreeContentProvider getFileProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ITreeContentProvider getFolderProvider() {
		return null;
	}

}
