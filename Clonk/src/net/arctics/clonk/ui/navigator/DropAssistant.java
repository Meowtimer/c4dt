package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

public class DropAssistant extends CommonDropAdapterAssistant {

	public DropAssistant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter dropAdapter, DropTargetEvent dropTargetEvent, Object target) {
		if (target instanceof IContainer && Utilities.getClonkNature(((IContainer)target).getProject()) != null &&
			dropTargetEvent.dataTypes.length == 1 &&
			FileTransfer.getInstance().isSupportedType(dropTargetEvent.dataTypes[0]))
		{
			return new Status(IStatus.OK, ClonkCore.PLUGIN_ID, "Drag");
		}
		return new Status(IStatus.CANCEL, ClonkCore.PLUGIN_ID, "Nope");
	}

	@Override
	public IStatus validateDrop(Object target, int operation,
	        TransferData transferType) {
		// TODO Auto-generated method stub
		return null;
	}

}
