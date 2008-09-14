package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.io.FilenameFilter;

import net.arctics.clonk.resource.c4group.C4Entry;
import net.arctics.clonk.resource.c4group.C4Group;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ClonkNavigator implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object parentElement) {
		return null;
//		if (parentElement instanceof IProject) {
//			try {
//				IProjectNature clonkProject = ((IProject) parentElement).getNature("net.arctics.clonk.clonknature");
//				if (clonkProject == null) return null;
//				File file = new File(((IResource)parentElement).getLocationURI());
//				FilenameFilter filter = new FilenameFilter() {
//					@Override
//					public boolean accept(File dir, String name) {
//						if (name.endsWith(".c4d")) return true;
//						else return false;
//					}
//				};
//				String[] files = file.list(filter);
//				Object[] result = new Object[files.length];
//				try {
//					for (int i = 0;i < files.length;i++) {
//						result[i] = C4Group.OpenFile(new File(((IResource)parentElement).getLocationURI().getPath(),files[i]));
//					}
//					return result;
//				} catch (InvalidDataException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//			} catch (CoreException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		else if (parentElement instanceof C4Group) {
//			C4Group group = (C4Group)parentElement;
//			if (!group.isCompleted())
//				try {
//					group.open(false);
//				} catch (InvalidDataException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//					return null;
//				}
//			return group.getChildEntries().toArray();
//		}
//		return null;
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			if (((IProject)element).isOpen()) return true;
		}
		if (element instanceof C4Entry) {
			return false;
//			C4Entry entry = (C4Entry)element;
//			if (!entry.isCompleted()) {
//				try {
//					entry.open(false);
//				} catch (InvalidDataException e) {
//					e.printStackTrace();
//					return false;
//				}
//			}
		}
		if (element instanceof C4Group) {
			return true;
//			C4Group group = (C4Group)element;
//			if (!group.isCompleted())
//				try {
//					group.open(false);
//				} catch (InvalidDataException e) {
//					e.printStackTrace();
//					return false;
//				}
//			return ((C4Group)element).hasChildren();
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			File file = new File(((IWorkspaceRoot)inputElement).getLocationURI());
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (name.endsWith(".c4d")) return true;
					else return false;
				}
			};
			String[] files = file.list(filter);
			Object[] projects = new Object[files.length];
			
			for(int i = 0;i < files.length;i++) {
				projects[i] = ResourcesPlugin.getWorkspace().getRoot().getProject(files[i]);
//				((IProject)projects[i]).o
			}
			return projects;
		}
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public Object[] getElements(Saveable saveable) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Saveable getSaveable(Object element) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Saveable[] getSaveables() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
