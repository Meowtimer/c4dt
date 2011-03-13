package net.arctics.clonk.ui;

import net.arctics.clonk.ClonkCore;
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
import org.eclipse.ui.PlatformUI;

public class LightweightLabelDecorator implements ILightweightLabelDecorator {

	public void decorate(Object element, IDecoration decoration) {
		DecorationContext context = null;
		if (decoration.getDecorationContext() instanceof DecorationContext) {
			context = ((DecorationContext)decoration.getDecorationContext());
			context.putProperty(IDecoration.ENABLE_REPLACE, Boolean.TRUE);
		}
		if (PlatformUI.getWorkbench() == null) {
			System.out.println("no workbench"); //$NON-NLS-1$
		}
		else if (PlatformUI.getWorkbench().getDecoratorManager() == null) {
			System.out.println("no decmgr"); //$NON-NLS-1$
		}
		else if (PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator() == null) {
			System.out.println("no labeldec"); //$NON-NLS-1$
		}
		if (element instanceof IResource) {
			IResource res = (IResource) element;
			if (!res.getProject().isOpen()) return;
			if (res instanceof IFolder) {
				GroupType groupType = ClonkProjectNature.getEngine(res).getGroupTypeForFileName(res.getName());
				
				if (groupType == GroupType.FolderGroup) {
					decoration.addOverlay(UI.getIconDescriptor("icons/Clonk_folder.png"),IDecoration.REPLACE); //$NON-NLS-1$
				}
				else if (groupType == GroupType.DefinitionGroup) {
					decoration.addOverlay(UI.getIconDescriptor("icons/C4Object.png"),IDecoration.REPLACE); //$NON-NLS-1$
				}
				else if (groupType == GroupType.ScenarioGroup) {
					decoration.addOverlay(UI.getIconDescriptor("icons/Clonk_scenario.png"),IDecoration.REPLACE); //$NON-NLS-1$
				}
				else if (groupType == GroupType.ResourceGroup) {
					decoration.addOverlay(UI.getIconDescriptor("icons/Clonk_datafolder.png"),IDecoration.REPLACE); //$NON-NLS-1$
				}
			}
			try {
				int severity = res.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (severity == IMarker.SEVERITY_WARNING) decoration.addOverlay(getIcon("warning_co.gif"),IDecoration.BOTTOM_LEFT); //$NON-NLS-1$
				else if (severity == IMarker.SEVERITY_ERROR) decoration.addOverlay(getIcon("error_co.gif"),IDecoration.BOTTOM_LEFT); //$NON-NLS-1$
//				IMarker[] markers = res.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
//				if (markers.length > 0) {
//					int severity = 0;
//					for(IMarker marker : markers) {
//						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
//							severity = 2;
////							decoration.addOverlay(getIcon("error_co.gif"),IDecoration.BOTTOM_LEFT);
////							decoration.addOverlay(UI.getIconDescriptor("icons/error_co.gif"),IDecoration.BOTTOM_LEFT);
//							break;
//						}
//						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_WARNING) {
//							if (severity < 2) severity = 1;
////							decoration.addOverlay(getIcon("warning_co.gif"),IDecoration.BOTTOM_LEFT);
//						}
//					}
//					
//				}
			} catch (CoreException e) {
				e.printStackTrace();
			}

		} else {
			System.out.println("other"); //$NON-NLS-1$
		}
	}

	private ImageDescriptor getIcon(String name) {
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		if (reg.get(name) == null) {
			reg.put(name, UI.getIconDescriptor("icons/" + name)); //$NON-NLS-1$
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
