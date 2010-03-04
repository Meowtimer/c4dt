package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.eclipse.swt.dnd.FileTransfer;

public class DragExternalDefsAssistant extends CommonDragAdapterAssistant {

	public DragExternalDefsAssistant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Transfer[] getSupportedTransferTypes() {
		return new Transfer[] {
			FileTransfer.getInstance()
		};
	}

	@Override
	public boolean setDragData(DragSourceEvent event, IStructuredSelection selection) {
		if (selection != null) {
			if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
				List<String> files = new ArrayList<String>();
				for (Iterator<?> it = selection.iterator(); it.hasNext();) {
					Object elm = it.next();
					/*if (elm instanceof C4ObjectExtern) {
						files.add(((C4ObjectExtern)elm).getFilePath());
					}*/
				}
				if (!files.isEmpty()) {
					event.data = files.toArray(new String[files.size()]);
					return true;
				}
			}
		}
		return false;
	}

}