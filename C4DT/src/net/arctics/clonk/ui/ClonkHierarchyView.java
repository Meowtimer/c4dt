package net.arctics.clonk.ui;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class ClonkHierarchyView extends ViewPart {

	private TreeViewer hierarchyTree;
	
	private static class HierarchyTreeContentProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {
		
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof C4Object) {
				C4Object parent = (C4Object) parentElement;
				List<C4ScriptBase> result = new LinkedList<C4ScriptBase>();
				for (IProject p : Utilities.getClonkProjects()) {
					ClonkIndex index = ClonkProjectNature.get(p).getIndex();
					for (C4ScriptBase script : index.allScripts()) {
						if (script.includes(parent))
							result.add(script);
					}
				}
				return result.toArray(new C4ScriptBase[result.size()]);
			}
			else
				return null;
		}

		@Override
		public Object getParent(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return true;
		}

		private C4ScriptBase[] getRootScripts(Object input) {
			List<C4ScriptBase> result = new LinkedList<C4ScriptBase>();
			IProject[] clonkProjects = Utilities.getClonkProjects(); 
			for (IProject p : clonkProjects) {
				ClonkIndex index = ClonkProjectNature.get(p).getIndex();
				for (C4ScriptBase script : index.allScripts()) {
					if (script.getIncludes().length == 0)
						result.add(script);
				}
			}
			return result.toArray(new C4ScriptBase[result.size()]);
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			return getRootScripts(inputElement);
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public StyledString getStyledText(Object element) {
			return new StyledString(getText(element));
		}
		
		@Override
		public String getText(Object element) {
			return ((C4ScriptBase)element).getName();
		}
		
		@Override
		public Image getImage(Object element) {
			if (element instanceof C4Object) {
				return UI.GENERAL_OBJECT_ICON;
			}
			else
				return null;
		}
		
	}
	
	@Override
	public void createPartControl(Composite parent) {
		hierarchyTree = new TreeViewer(parent, SWT.NONE);
		hierarchyTree.getTree().setLayoutData(UI.createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(0, 0),
			new FormAttachment(0, 0))
		);
		HierarchyTreeContentProvider provider = new HierarchyTreeContentProvider();
		hierarchyTree.setContentProvider(provider);
		hierarchyTree.setLabelProvider(provider);
		hierarchyTree.setComparator(new ViewerComparator());
		hierarchyTree.setInput(ResourcesPlugin.getWorkspace());
	}

	@Override
	public void setFocus() {
	}

}
