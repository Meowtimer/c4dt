package net.arctics.clonk.ui;

import java.util.Comparator;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.ui.editors.actions.c4script.EntityChooser;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

public class OpenDefinitionDialog extends EntityChooser {

	public static final String DIALOG_SETTINGS = "OpenDefinitionDialogSettings"; //$NON-NLS-1$

	private final Object selection;
	private IConverter<Definition, Image> imageStore;

	public void setImageStore(IConverter<Definition, Image> imageStore) {
		this.imageStore = imageStore;
	}

	private class OpenDefinitionLabelProvider extends LabelProvider implements IStyledLabelProvider {
		@Override
		public StyledString getStyledText(Object element) {
			if (element == null)
				return new StyledString(Messages.OpenDefinitionDialog_Empty);
			if (!(element instanceof Definition)) return new StyledString(element.toString());
			final Definition obj = (Definition) element;
			final StyledString buf = new StyledString(obj.localizedName());
			buf.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			buf.append(obj.id().stringValue(), StyledString.QUALIFIER_STYLER);
			return buf;
		}
		@Override
		public Image getImage(Object element) {
			if (imageStore != null && element instanceof Definition)
				return imageStore.convert((Definition)element);
			else
				return null;
		}
	}

	public OpenDefinitionDialog() {
		this(UI.projectExplorerSelection(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite()));

	}

	/**
	 * Create dialog with specified selection. The selection can be a {@link IStructuredSelection} with a {@link IStructuredSelection#getFirstElement()} being an {@link IResource}
	 * in which case the dialog will be populated with definitions visible from the resource's project.
	 * Another option is passing an {@link Iterable} of items as the selection which will then be displayed as-is.
	 * @param selection The selection which can be one of the things specified above
	 */
	public OpenDefinitionDialog(Object selection) {
		super(Platform.getResourceString(Core.instance().getBundle(), "%OpenDefinition_Name"), //$NON-NLS-1$
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		this.selection = selection;
		setListLabelProvider(new OpenDefinitionLabelProvider());
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		// There's nothing special here
		return null;
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IResource) {
			final IProject proj = ((IResource)((IStructuredSelection)selection).getFirstElement()).getProject();
			final ClonkProjectNature nat = ClonkProjectNature.get(proj);
			if (nat != null && nat.index() != null)
				for (final Index index : nat.index().relevantIndexes())
					fillWithIndexContents(contentProvider, itemsFilter, progressMonitor, index);
		} else if (selection instanceof Iterable)
			for (final Object o : (Iterable<?>)selection)
				contentProvider.add(o, itemsFilter);
	}

	private void fillWithIndexContents(
		final AbstractContentProvider contentProvider,
		final ItemsFilter itemsFilter, final IProgressMonitor progressMonitor,
		Index index
	) {
		progressMonitor.beginTask(Messages.OpenDefinitionDialog_Searching, index.numUniqueIds());
		index.allDefinitions(new Sink<Definition>() {
			@Override
			public void receivedObject(Definition item) {
				contentProvider.add(item, itemsFilter);
				progressMonitor.worked(1);
			}
		});
		for (final Scenario s : index.indexedScenarios())
			contentProvider.add(s, itemsFilter);
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

	public Definition[] selectedDefinitions() {
		return ArrayUtil.convertArray(getResult(), Definition.class);
	}

}
