package net.arctics.clonk.ui;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.index.Engine;
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
	public void decorate(final Object element, final IDecoration decoration) {
		DecorationContext context = null;
		if (decoration.getDecorationContext() instanceof DecorationContext) {
			context = ((DecorationContext)decoration.getDecorationContext());
			context.putProperty(IDecoration.ENABLE_REPLACE, Boolean.TRUE);
		}
		if (element instanceof IResource) {
			final IResource res = (IResource) element;
			if (!res.getProject().isOpen()) return;
			if (res instanceof IFolder) {
				final Engine engine = ClonkProjectNature.engineFromResource(res);
				if (engine != null) {
					final FileExtension groupType = engine.extensionForFileName(res.getName());
					final ImageDescriptor imgDesc = engine.imageDescriptor(groupType);
					if (imgDesc != null)
						decoration.addOverlay(imgDesc, IDecoration.REPLACE);
				}
			}
			try {
				final int severity = res.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (severity == IMarker.SEVERITY_WARNING)
					decoration.addOverlay(getIcon("warning_co.gif"),IDecoration.BOTTOM_LEFT); //$NON-NLS-1$
				else if (severity == IMarker.SEVERITY_ERROR)
					decoration.addOverlay(getIcon("error_co.gif"),IDecoration.BOTTOM_LEFT); //$NON-NLS-1$
			} catch (final CoreException e) {
				e.printStackTrace();
			}

		} else
			System.out.println("other"); //$NON-NLS-1$
	}

	private ImageDescriptor getIcon(final String name) {
		final ImageRegistry reg = Core.instance().getImageRegistry();
		if (reg.get(name) == null)
			reg.put(name, UI.imageDescriptorForPath("icons/" + name)); //$NON-NLS-1$
		return reg.getDescriptor(name);
	}
	
	@Override
	public void addListener(final ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(final Object element, final String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(final ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

}
