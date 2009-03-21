package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.ui.OverlayIcon;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class ClonkLabelProvider extends LabelProvider implements IStyledLabelProvider {
	
	public ClonkLabelProvider() {
//		instance = this;
	}
	
	public Image getImage(Object element) {
//		if (true) return null;
		if (element instanceof IProject) {
			return super.getImage(element);
		}
		else if (element instanceof IFile) {
			if (element.toString().endsWith(".c")) {
				return computeImage("c4script","icons/c4scriptIcon.png",(IResource)element);
			}
			if (element.toString().endsWith(".txt")) {
				return computeImage("c4txt","icons/text.png",(IResource)element);
			}
			if (element.toString().endsWith(".c4m")) {
				return computeImage("c4material","icons/Clonk_C4.png",(IResource)element);
			}
		}
		else if (element instanceof IFolder) {
			IFolder folder = (IFolder)element;
			C4GroupType groupType = Utilities.groupTypeFromFolderName(folder.getName());
			
			if (groupType == C4GroupType.FolderGroup) {
				return computeImage("c4folder","icons/Clonk_folder.png",(IResource)element);
			}
			else if (groupType == C4GroupType.DefinitionGroup) {
				return computeImage("c4object","icons/C4Object.png",(IResource)element);
			}
			else if (groupType == C4GroupType.ScenarioGroup) {
				return computeImage("c4scenario","icons/Clonk_scenario.png",(IResource)element);
			}
			else if (groupType == C4GroupType.ResourceGroup) {
				return computeImage("c4datafolder","icons/Clonk_datafolder.png",(IResource)element);
			}
		}
//		if (element instanceof C4Entry) {
//			
//			C4Entry entry = (C4Entry)element;
//			if (entry.getName().endsWith(".txt")) {
//				return computeImage("c4txt","icons/text.png",(IResource)element);
//			}
//			else if (entry.getName().endsWith(".png")) {
//				return super.getImage(element.toString());
//			}
//			else if (entry.getName().endsWith(".c")) {
//				return computeImage("c4script","icons/c4scriptIcon.png",(IResource)element);
//			}
//		}
		
		return null;
	}

	public String getText(Object element) {
		if (element instanceof IProject) {
			return ((IProject)element).getName();
		}
		else if (element instanceof IFile) {
			return ((IFile)element).getName();
		}
		return super.getText(element);
	}

	public static String stringWithoutExtension(String s) {
		return s.substring(0,s.lastIndexOf("."));
	}
	
	public StyledString getStyledText(Object element) {

		if (element instanceof IFolder) {
			IFolder folder = (IFolder)element;
			C4GroupType groupType = Utilities.groupTypeFromFolderName(folder.getName());
			if (groupType == C4GroupType.DefinitionGroup) {
				// add [C4ID] to .c4d folders
				StyledString buf = new StyledString();
				buf.append(stringWithoutExtension(folder.getName()));
				try {
					String c4id = folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID);
					if (c4id != null) {
						buf.append(" [",StyledString.DECORATIONS_STYLER);
						buf.append(c4id,StyledString.DECORATIONS_STYLER);
						buf.append("]",StyledString.DECORATIONS_STYLER);
					}
					// FIXME stop activation of lazy loading:
					if (folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) == null) {
						C4ObjectIntern.objectCorrespondingTo(folder);
					}

				} catch (CoreException e) {
					e.printStackTrace();
				}

				return buf;
			}
			if (groupType == C4GroupType.FolderGroup || groupType == C4GroupType.ScenarioGroup || groupType == C4GroupType.ResourceGroup)
				return new StyledString(folder.getName().substring(0,folder.getName().lastIndexOf(".")));
		}
		return new StyledString(((IResource)element).getName());
	}

	public static Image computeImage(String registryKey, String iconPath, IResource element) {
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		Image img = reg.get(registryKey);
		if (img == null) {
			reg.put(registryKey, Utilities.getIconDescriptor(iconPath));
			img = reg.get(registryKey);
		}
//			if (element.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE).length > 0) {
//				return decorateImage(reg.getDescriptor(registryKey), element).createImage();
//			}
		return img;
	}
	
	protected static ImageDescriptor decorateImage(ImageDescriptor input,
			Object element) {
		return new OverlayIcon(input,computeOverlays(element),new Point(22,16));
	}
	
	protected static ImageDescriptor[][] computeOverlays(Object element) {
		ImageDescriptor[][] result = new ImageDescriptor[4][1];
		if (element instanceof IResource) {
			IResource res = (IResource)element;
			try {
				IMarker[] markers = res.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (markers.length > 0) {
					for(IMarker marker : markers) {
						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
							result[2][0] = Utilities.getIconDescriptor("icons/error_co.gif");
							break;
						}
						if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_WARNING) {
							result[2][0] = Utilities.getIconDescriptor("icons/warning_co.gif");
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		return result;
	}
//
//
//	protected String decorateText(String input, Object element) {
//		if (element instanceof IProject) {
//			return ((IProject)element).getName();
//		}
//		else if (element instanceof IFile) {
//			return ((IFile)element).getName();
//		}
//		return super.decorateText(input, element);
//	}

	public void dispose() {
		super.dispose();
	}

	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return true;
	}
	
	public void testRefresh() {
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}
	
//	public static ClonkLabelProvider instance;
//
//	public static ClonkLabelProvider getInstance() {
//		return instance;
//	}
	
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}
	
}
