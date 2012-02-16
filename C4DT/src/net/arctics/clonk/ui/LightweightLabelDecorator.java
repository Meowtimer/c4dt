package net.arctics.clonk.ui;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

public class LightweightLabelDecorator implements ILightweightLabelDecorator {

	@Override
	public void decorate(Object element, IDecoration decoration) {
		DecorationContext context = null;
		if (decoration.getDecorationContext() instanceof DecorationContext) {
			context = ((DecorationContext)decoration.getDecorationContext());
			context.putProperty(IDecoration.ENABLE_REPLACE, Boolean.TRUE);
		}
		if (element instanceof IResource) {
			IResource res = (IResource) element;
			if (!res.getProject().isOpen()) return;
			if (res instanceof IFolder) {
				Engine engine = ClonkProjectNature.engineFromResource(res);
				if (engine != null) {
					GroupType groupType = engine.groupTypeForFileName(res.getName());
					ImageDescriptor imgDesc = engine.imageDescriptor(groupType);
					if (imgDesc != null)
						decoration.addOverlay(imgDesc, IDecoration.REPLACE);
				}
			}
			try {
				int severity = res.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (severity == IMarker.SEVERITY_WARNING)
					decoration.addOverlay(getIcon("warning_co.gif"),IDecoration.BOTTOM_LEFT); //$NON-NLS-1$
				else if (severity == IMarker.SEVERITY_ERROR)
					decoration.addOverlay(getIcon("error_co.gif"),IDecoration.BOTTOM_LEFT); //$NON-NLS-1$
			} catch (CoreException e) {
				e.printStackTrace();
			}

		} else {
			System.out.println("other"); //$NON-NLS-1$
		}
	}

	private ImageDescriptor getIcon(String name) {
		ImageRegistry reg = Core.instance().getImageRegistry();
		if (reg.get(name) == null) {
			reg.put(name, UI.imageDescriptorForPath("icons/" + name)); //$NON-NLS-1$
		}
		return reg.getDescriptor(name);
	}
	
	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

}
