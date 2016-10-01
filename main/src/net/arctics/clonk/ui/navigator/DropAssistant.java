package net.arctics.clonk.ui.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;

public class DropAssistant extends CommonDropAdapterAssistant {

	@Override
	public IStatus handleDrop(final CommonDropAdapter dropAdapter, final DropTargetEvent dropTargetEvent, final Object target) {
		final boolean canDrop = (
			target instanceof IContainer && ClonkProjectNature.get(((IContainer)target).getProject()) != null &&
			dropTargetEvent.dataTypes.length == 1 &&
			FileTransfer.getInstance().isSupportedType(dropTargetEvent.dataTypes[0])
		);
		return canDrop
			? new Status(IStatus.OK, Core.PLUGIN_ID, "Drag") //$NON-NLS-1$
			: new Status(IStatus.CANCEL, Core.PLUGIN_ID, "Nope"); //$NON-NLS-1$
	}

	@Override
	public IStatus validateDrop(final Object target, final int operation, final TransferData transferType) {
		return null;
	}

}
