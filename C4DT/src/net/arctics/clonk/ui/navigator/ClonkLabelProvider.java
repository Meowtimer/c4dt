package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ClonkLabelProvider extends LabelProvider implements IStyledLabelProvider, IColorProvider {
	public ClonkLabelProvider() {}
	@Override
	public Image getImage(final Object element) {
		if (element instanceof IProject)
			return super.getImage(element);
		else if (element instanceof IFile) {
			if (Script.looksLikeScriptFile(element.toString()))
				return UI.SCRIPT_ICON;
			if (element.toString().endsWith(".txt"))
				return UI.TEXT_ICON;
			final Engine engine = ClonkProjectNature.engineFromResource((IFile)element);
			if (engine != null)
				if (element.toString().endsWith(engine.settings().materialExtension))
					return engine.image("material");
		}
		else if (element instanceof IFolder) {
			final IFolder folder = (IFolder)element;
			final Engine engine = ClonkProjectNature.engineFromResource(folder);
			if (engine != null)
				return engine.image(engine.extensionForFileName(folder.getName()));
		}
		return UI.iconFor(element);
	}
	@Override
	public String getText(final Object element) {
		if (element instanceof IProject)
			return ((IProject)element).getName();
		else if (element instanceof IFile)
			return ((IFile)element).getName();
		return super.getText(element);
	}
	public static String stringWithoutExtension(final String s) { return s.substring(0,s.lastIndexOf(".")); } //$NON-NLS-1$
	@Override
	public StyledString getStyledText(final Object element) {
		if (element instanceof IFolder) {
			final IFolder folder = (IFolder)element;
			final Engine engine = ClonkProjectNature.engineFromResource(folder);
			if (engine != null) {
				final FileExtension groupType = engine.extensionForFileName(folder.getName());
				if (groupType == FileExtension.DefinitionGroup)
					// add [C4ID] to .c4d folders
					try {
						final String c4id = folder.getPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID);
						return getIDText(folder.getName(), c4id, false);
					} catch (final CoreException e) {
						e.printStackTrace();
					}
				if (groupType == FileExtension.FolderGroup || groupType == FileExtension.ScenarioGroup || groupType == FileExtension.ResourceGroup)
					return new StyledString(folder.getName().substring(0,folder.getName().lastIndexOf("."))); //$NON-NLS-1$
			}
			return new StyledString(((IFolder)element).getName());
		}
		else if (element instanceof IResource)
			return new StyledString(((IResource)element).getName());
		else if (element instanceof Definition) {
			final Definition obj = (Definition) element;
			final String c4id = obj.id().toString();
			return getIDText(obj.nodeName(), c4id, true);
		}
		else if (element instanceof INode)
			return new StyledString(element.toString(), StyledString.COUNTER_STYLER);
		return new StyledString(element.toString());
	}
	private StyledString getIDText(final String baseName, final String id, final boolean virtual) {
		final StyledString buf = new StyledString();
		if (virtual)
			buf.append(stringWithoutExtension(baseName), StyledString.COUNTER_STYLER);
		else
			buf.append(stringWithoutExtension(baseName));
		if (id != null) {
			buf.append(" [",StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
			buf.append(id,StyledString.DECORATIONS_STYLER);
			buf.append("]",StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
		}
		return buf;
	}
	protected static ImageDescriptor[][] computeOverlays(final Object element) {
		final ImageDescriptor[][] result = new ImageDescriptor[4][1];
		if (element instanceof IResource) {
			final IResource res = (IResource)element;
			try {
				final IMarker[] markers = res.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (markers.length > 0)
					for(final IMarker marker : markers) {
						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
							result[2][0] = UI.imageDescriptorForPath("icons/error_co.gif"); //$NON-NLS-1$
							break;
						}
						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_WARNING)
							result[2][0] = UI.imageDescriptorForPath("icons/warning_co.gif"); //$NON-NLS-1$
					}
			} catch (final CoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		return result;
	}
	@Override
	public Color getForeground(final Object element) { return null; }
	@Override
	public Color getBackground(final Object element) {
		try {
			if (element instanceof IResource)
				for (IResource resource = (IResource)element; resource != null; resource = resource.getParent()) {
					final RGB rgb = ColorTagging.rgbForResource(resource);
					if (rgb != null)
						return new Color(Display.getCurrent(), rgb);
				}
		} catch (final Exception e) {}
		return null;
	}
}