package net.arctics.clonk.ui.search;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class C4ScriptSearchPage extends DialogPage implements ISearchPage {

	public C4ScriptSearchPage() {}

	public C4ScriptSearchPage(String title) {
		super(title);
	}

	public C4ScriptSearchPage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void createControl(Composite parent) {
		Label l = new Label(parent, SWT.DEFAULT);
		l.setText("Yes");
		setControl(l);
	}

	@Override
	public boolean performAction() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setContainer(ISearchPageContainer container) {
		// TODO Auto-generated method stub

	}

}
