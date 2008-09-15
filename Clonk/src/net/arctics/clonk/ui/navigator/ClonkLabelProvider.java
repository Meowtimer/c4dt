package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4DefCoreParser;
import net.arctics.clonk.resource.c4group.C4Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;

public class ClonkLabelProvider extends LabelProvider implements IStyledLabelProvider {
	
	public ClonkLabelProvider() {
		instance = this;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();

		if (element instanceof IProject) {
			return super.getImage(element);
		}
		else if (element instanceof IFile) {
			if (element.toString().endsWith(".c")) {
				if (reg.get("c4script") == null) {
					reg.put("c4script", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/c4scriptIcon.png"), null)));
				}
				return reg.get("c4script");
			}
			if (element.toString().endsWith(".txt")) {
				if (reg.get("c4txt") == null) {
					reg.put("c4txt", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/text.png"), null)));
				}
				return reg.get("c4txt");
			}
			if (element.toString().endsWith(".c4m")) {
				if (reg.get("c4material") == null) {
					reg.put("c4material", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/Clonk_C4.png"), null)));
				}
				return reg.get("c4material");
			}
		}
		else if (element instanceof IFolder) {
			IFolder folder = (IFolder)element;
			if (folder.getName().startsWith("c4f")) {
				if (reg.get("c4folder") == null) {
					reg.put("c4folder", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/Clonk_folder.png"), null)));
				}
				return reg.get("c4folder");
			}
			else if (folder.getName().startsWith("c4d")) {
				if (reg.get("c4object") == null) {
					reg.put("c4object", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/C4Object.png"), null)));
				}
				return reg.get("c4object");
			}
			else if (folder.getName().startsWith("c4s")) {
				if (reg.get("c4scenario") == null) {
					reg.put("c4scenario", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/Clonk_scenario.png"), null)));
				}
				return reg.get("c4scenario");
			}
			else if (folder.getName().startsWith("c4g")) {
				if (reg.get("c4datafolder") == null) {
					reg.put("c4datafolder", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/Clonk_datafolder.png"), null)));
				}
				return reg.get("c4datafolder");
			}
		}
		if (element instanceof C4Entry) {
			C4Entry entry = (C4Entry)element;
			if (entry.getName().endsWith(".txt")) {
				if (reg.get("c4txt") == null) {
					reg.put("c4txt", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/text.png"), null)));
				}
				return reg.get("c4txt");
			}
			else if (entry.getName().endsWith(".png")) {
				
				return super.getImage(element.toString());
			}
			else if (entry.getName().endsWith(".c")) {
				if (reg.get("c4script") == null) {
					reg.put("c4script", ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/c4scriptIcon.png"), null)));
				}
				return reg.get("c4script");
			}
		}
		
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

	public StyledString getStyledText(Object element) {
		
		if (element instanceof IFolder) {
			StyledString buf = new StyledString();
			IFolder folder = (IFolder)element;
			if (folder.getName().startsWith("c4d.")) {
				IResource res = folder.findMember("DefCore.txt");
				if (res != null && res instanceof IFile) {
					try {
						((IFile)res).accept(C4DefCoreParser.getInstance());
						buf.append(folder.getName().substring(4));
						buf.append(" ["+ C4DefCoreParser.getInstance().getDefFor((IFile)res).getId().getName() + "]",StyledString.DECORATIONS_STYLER);
						return buf;
					} catch (CoreException e) {
						e.printStackTrace();
						return new StyledString(folder.getName().substring(4) + "|error");
					}
				}
				else {
					return new StyledString(folder.getName().substring(4));
				}
			}
			if (folder.getName().startsWith("c4f.") || folder.getName().startsWith("c4s.") || folder.getName().startsWith("c4g."))
				return new StyledString(folder.getName().substring(4));
		}
		return new StyledString(getText(element));
	}

	
	
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
	
	public static ClonkLabelProvider instance;
	
}
