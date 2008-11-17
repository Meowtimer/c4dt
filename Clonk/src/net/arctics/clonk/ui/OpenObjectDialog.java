package net.arctics.clonk.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class OpenObjectDialog extends FilteredItemsSelectionDialog {
	
	public static final String DIALOG_SETTINGS = "OpenObjectDialogSettings";
	
	private class OpenObjectLabelProvider extends LabelProvider implements IStyledLabelProvider {

		public StyledString getStyledText(Object element) {
			if (!(element instanceof C4Object)) return new StyledString(element.toString());
			C4Object obj = (C4Object) element;
			StyledString buf = new StyledString(obj.getName());
			buf.append(" - ", StyledString.QUALIFIER_STYLER);
			buf.append(obj.getId().getName(), StyledString.QUALIFIER_STYLER);
			return buf;
		}
		
	}
	
	/*protected class OpenObjectContentProvider extends AbstractContentProvider {
		
		@Override
		public void add(Object item, ItemsFilter itemsFilter) {
			
		}
		
	}*/
	
	public OpenObjectDialog(Shell shell) {
		super(shell);
		setListLabelProvider(new OpenObjectLabelProvider());
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		// theres nothing special here
		return null;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {
		
			@Override
			public boolean matchItem(Object item) {
				if (!(item instanceof C4Object)) return false;
				final C4Object obj = (C4Object) item;
				if (obj == null || obj.getId() == null || obj.getName() == null) {
					return false;
				}
				final String search = this.getPattern();
				final String objId = obj.getId().getName();
				if (objId.contains(search) || obj.getName().contains(search))
					return true;
				return false;
			}
		
			@Override
			public boolean isConsistentItem(Object item) {
				// TODO Auto-generated method stub ??
				return false;
			}
		}; 
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject project : projects) {
			if (project.isOpen()) {
				if (project.isNatureEnabled(ClonkCore.CLONK_NATURE_ID)) {
					ClonkProjectNature nature = Utilities.getProject(project);
					Map<C4ID,List<C4Object>> objects = nature.getIndexedData().getIndexedObjects();
					progressMonitor.beginTask("Searching", objects.size());
					for(List<C4Object> idObjects : objects.values()) {
						contentProvider.add(idObjects.get(idObjects.size() - 1), itemsFilter);
						progressMonitor.worked(1);
					}
					progressMonitor.done();
				}
			}
		}
		
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = ClonkCore.getDefault().getDialogSettings()
				.getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = ClonkCore.getDefault().getDialogSettings()
					.addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	@Override
	public String getElementName(Object item) {
		if (!(item instanceof C4Object))
			return item.toString();
		return ((C4Object)item).getId().getName();
	}

	@Override
	protected Comparator getItemsComparator() {
	      return new Comparator() {
	          public int compare(Object arg0, Object arg1) {
	             return arg0.toString().compareTo(arg1.toString());
	          }
	       };
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

}
