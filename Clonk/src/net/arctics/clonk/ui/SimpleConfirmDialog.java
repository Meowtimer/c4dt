package net.arctics.clonk.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class SimpleConfirmDialog extends Dialog {

	public SimpleConfirmDialog(Shell parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButton(org.eclipse.swt.widgets.Composite, int, java.lang.String, boolean)
	 */
	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		// TODO Auto-generated method stub
		Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) button.setText(Messages.SimpleConfirmDialog_0);
		return button;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		new Label(comp,SWT.NONE).setText(Messages.SimpleConfirmDialog_1);
		
		return comp;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		// TODO Auto-generated method stub
		super.okPressed();
	}
	
	

}
