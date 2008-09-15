package net.arctics.clonk.ui.editors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.model.WorkbenchContentProvider;

public class C4DefCoreEditor extends TextEditor {

	public C4DefCoreEditor() {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#editorSaved()
	 */
	@Override
	protected void editorSaved() {
		super.editorSaved();
		PlatformUI.getWorkbench().getViewRegistry().find("net.arctics.clonk.navigator.resourceContent");
	}

}
