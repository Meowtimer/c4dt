package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.widgets.Shell;

public class ClonkOutlineInformationControl extends PopupDialog {

	public ClonkOutlineInformationControl(Shell parent, int shellStyle,
			boolean takeFocusOnOpen, boolean persistSize,
			boolean persistLocation, boolean showDialogMenu,
			boolean showPersistActions, String titleText, String infoText) {
		super(parent, shellStyle, takeFocusOnOpen, persistSize, persistLocation,
				showDialogMenu, showPersistActions, titleText, infoText);
	}

}
