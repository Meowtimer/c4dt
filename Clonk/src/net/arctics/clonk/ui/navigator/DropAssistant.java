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
			return new Status(IStatus.OK, ClonkCore.PLUGIN_ID, "Drag"); //$NON-NLS-1$
		}
		return new Status(IStatus.CANCEL, ClonkCore.PLUGIN_ID, "Nope"); //$NON-NLS-1$
	}

	@Override
	public IStatus validateDrop(Object target, int operation, TransferData transferType) {
//		Class<?>[] classes = new Class[] {
//			EditorInputTransfer.class,
//			FileTransfer.class,
//			HTMLTransfer.class,
//			ImageTransfer.class,
//			//LocalSelectionTransfer.class,
//			MarkerTransfer.class,
//			PluginTransfer.class,
//			ResourceTransfer.class,
//			RTFTransfer.class,
//			TextTransfer.class,
//			URLTransfer.class
//		};
//		for (Class<?> c : classes) {
//			try {
//				if (((Transfer)c.getMethod("getInstance", new Class<?>[0]).invoke(null, new Object[0])).isSupportedType(transferType)) {
//					System.out.println(c.toString());
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		if (target instanceof IContainer && ResourceTransfer.getInstance().isSupportedType(transferType)) {
//			ClonkProjectNature nature = Utilities.getClonkNature((ITextEditor) target);
//			if (nature != null) {
//				for (ExternalLib lib : nature.getDependencies()) {
//					//if (lib.getPath())
//				}
//			}
//		}
		return null;
	}

}
