package net.arctics.clonk.ui;

import java.util.Comparator;
import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.actions.c4script.EntityChooser;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.UI;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class OpenObjectDialog extends EntityChooser {
	
	public static final String DIALOG_SETTINGS = "OpenObjectDialogSettings"; //$NON-NLS-1$
	
	private ISelection selection;
	
	private static class OpenObjectLabelProvider extends LabelProvider implements IStyledLabelProvider {
		@Override
		public StyledString getStyledText(Object element) {
			if (element == null)
				return new StyledString(Messages.OpenObjectDialog_Empty);
			if (!(element instanceof Definition)) return new StyledString(element.toString());
			Definition obj = (Definition) element;
			StyledString buf = new StyledString(obj.name());
			buf.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			buf.append(obj.id().stringValue(), StyledString.QUALIFIER_STYLER);
			return buf;
		}
	}
	
	public OpenObjectDialog(Shell shell) {
		super(shell, (Index)null);
		selection = UI.projectExplorerSelection(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite());
		setListLabelProvider(new OpenObjectLabelProvider());
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		// There's nothing special here
		return null;
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IResource) {
			IProject proj = ((IResource)((IStructuredSelection)selection).getFirstElement()).getProject();
			ClonkProjectNature nat = ClonkProjectNature.get(proj);
			if (nat != null && nat.index() != null) {
				for (Index index : nat.index().relevantIndexes()) {
					fillWithIndexContents(contentProvider, itemsFilter, progressMonitor, index);
				}
			}
		}
	}

	private void fillWithIndexContents(
		final AbstractContentProvider contentProvider,
		final ItemsFilter itemsFilter, final IProgressMonitor progressMonitor,
		Index index
	) {
		progressMonitor.beginTask(Messages.OpenObjectDialog_Searching, index.numUniqueIds());
		index.allDefinitions(new Sink<Definition>() {
			@Override
			public void receivedObject(Definition item) {
				contentProvider.add(item, itemsFilter);
				progressMonitor.worked(1);
			}
		});
		for (Scenario s : index.indexedScenarios()) {
			contentProvider.add(s, itemsFilter);
		}
		progressMonitor.done();
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = Core.instance().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null)
			settings = Core.instance().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		return settings;
	}

	@Override
	public String getElementName(Object item) {
		if (!(item instanceof Definition))
			return item.toString();
		return ((Definition)item).id().stringValue();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getItemsComparator() {
		return new Comparator() {
			@Override
			public int compare(Object arg0, Object arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		};
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}
	
	public Definition[] getSelectedObjects() {
		return ArrayUtil.convertArray(getResult(), Definition.class);
	}

}
