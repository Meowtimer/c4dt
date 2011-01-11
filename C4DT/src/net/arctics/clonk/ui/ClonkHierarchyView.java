package net.arctics.clonk.ui;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.UI;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

public class ClonkHierarchyView extends ViewPart {

	private TreeViewer hierarchyTree;
	
	private static class HierarchyTreeContentProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {
		
		public interface IFilter {
			public boolean test(C4Object parent, C4ScriptBase script);
			public boolean isRootScript(C4ScriptBase script);
		}
		
		private static final IFilter INCLUDES_FILTER = new IFilter() {
			@Override
			public boolean test(C4Object parent, C4ScriptBase script) {
				return script.includes(parent);
			}
			@Override
			public boolean isRootScript(C4ScriptBase script) {
				return script instanceof C4Object && !(script instanceof C4Scenario) && !script.getIncludes().iterator().hasNext();
			}
		};
		
		private IFilter filter = INCLUDES_FILTER;
		
		public C4ScriptBase[] getScriptsDerivedFrom(Object parentElement, IFilter filter) {
			if (parentElement instanceof C4Object) {
				C4Object parent = (C4Object) parentElement;
				List<C4ScriptBase> result = new LinkedList<C4ScriptBase>();
				for (IProject p : ClonkProjectNature.getClonkProjects()) {
					ClonkIndex index = ClonkProjectNature.get(p).getIndex();
					for (C4ScriptBase script : index.allScripts()) {
						if (filter.test(parent, script))
							result.add(script);
					}
				}
				return result.toArray(new C4ScriptBase[result.size()]);
			}
			else
				return null;
		}
		
		@Override
		public Object[] getChildren(Object parentElement) {
			return getScriptsDerivedFrom(parentElement, filter);
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

		private C4ScriptBase[] getRootScripts(Object input, IFilter filter) {
			List<C4ScriptBase> result = new LinkedList<C4ScriptBase>();
			IProject[] clonkProjects = ClonkProjectNature.getClonkProjects(); 
			for (IProject p : clonkProjects) {
				ClonkIndex index = ClonkProjectNature.get(p).getIndex();
				for (C4ScriptBase script : index.allScripts()) {
					if (filter.isRootScript(script))
						result.add(script);
				}
			}
			return result.toArray(new C4ScriptBase[result.size()]);
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			return getRootScripts(inputElement, filter);
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
		createTreeViewer(parent);
		makeActions();
		contributeToActionBars();
	}
	
	private Action setHierarchyModeAction;
	
	private void makeActions() {
		setHierarchyModeAction = new Action() {
			@Override
			public void run() {
				
			};
		};
		setHierarchyModeAction.setToolTipText("Select the mode of hierarchical display");
		setHierarchyModeAction.setText("Mode");
	}

	private void createTreeViewer(Composite parent) {
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
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	private void fillLocalPullDown(IMenuManager manager) {
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(setHierarchyModeAction);
	}

	@Override
	public void setFocus() {
	}

}
