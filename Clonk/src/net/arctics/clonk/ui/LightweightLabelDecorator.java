package net.arctics.clonk.ui;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class LightweightLabelDecorator implements ILightweightLabelDecorator {

	public void decorate(Object element, IDecoration decoration) {
		DecorationContext context = null;
		if (decoration.getDecorationContext() instanceof DecorationContext) {
			context = ((DecorationContext)decoration.getDecorationContext());
			context.putProperty(IDecoration.ENABLE_REPLACE, Boolean.TRUE);
		}
		if (PlatformUI.getWorkbench() == null) {
			System.out.println("no workbench");
		}
		else if (PlatformUI.getWorkbench().getDecoratorManager() == null) {
			System.out.println("no decmgr");
		}
		else if (PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator() == null) {
			System.out.println("no labeldec");
		}
		if (element instanceof IResource) {
			IResource res = (IResource) element;
			if (res instanceof IFolder) {
				if (res.getName().endsWith(".c4d")) {
					decoration.addOverlay(Utilities.getIconDescriptor("icons/C4Object.png"),IDecoration.REPLACE);
				}
			}
			try {
				IMarker[] markers = res.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (markers.length > 0) {
					for(IMarker marker : markers) {
						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
							decoration.addOverlay(getIcon("error_co.gif"),IDecoration.BOTTOM_LEFT);
//							decoration.addOverlay(Utilities.getIconDescriptor("icons/error_co.gif"),IDecoration.BOTTOM_LEFT);
							break;
						}
						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_WARNING) {
							decoration.addOverlay(getIcon("warning_co.gif"),IDecoration.BOTTOM_LEFT);
//							decoration.addOverlay(Utilities.getIconDescriptor("icons/warning_co.gif"),IDecoration.BOTTOM_LEFT);
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}

		} else {
			System.out.println("other");
		}
	}

	private ImageDescriptor getIcon(String name) {
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		if (reg.get(name) == null) {
			reg.put(name, Utilities.getIconDescriptor("icons/" + name));
		}
		return reg.getDescriptor(name);
	}
	
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

}
