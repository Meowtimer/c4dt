package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.Collection;
import java.util.Comparator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.util.ArrayUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class DeclarationChooser extends FilteredItemsSelectionDialog {

	private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements IStyledLabelProvider {

		public StyledString getStyledText(Object element) {
			StyledString result = ClonkOutlineProvider.getStyledTextForEveryone(element);
			if (element != null) {
				result.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				result.append(((Declaration)element).getTopLevelStructure().toString(), StyledString.QUALIFIER_STYLER);
			}
			return result;
		}
		
	}
	
	private static final String DIALOG_SETTINGS = "DeclarationChooserDialogSettings"; //$NON-NLS-1$
	
	private Collection<Declaration> declarations;

	public DeclarationChooser(Shell shell, Collection<Declaration> declarations) {
		super(shell);
		this.declarations = declarations;
		setListLabelProvider(new LabelProvider());
		setTitle(Messages.DeclarationChooser_Label);
	}
	
	@Override
	public void create() { 
		super.create();
		((Text)this.getPatternControl()).setText(declarations.iterator().next().getName());
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {

			@Override
			public boolean isConsistentItem(Object item) {
				return false;
			}

			@Override
			public boolean matchItem(Object item) {
				if (!(item instanceof Declaration)) return false;
				final Declaration decl = (Declaration) item;
				final String search = this.getPattern().toUpperCase();
				final Structure structure = decl.getTopLevelStructure();
				return decl.nameContains(search) || (structure != null && structure.nameContains(search));
			}
			
		};
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		for (Declaration d : declarations)
			contentProvider.add(d, itemsFilter);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = ClonkCore.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = ClonkCore.getDefault().getDialogSettings()
			.addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	@Override
	public String getElementName(Object item) {
		return item.toString();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getItemsComparator() {
		return new Comparator() {

			public int compare(Object a, Object b) {
				return 1;
			}
			
		};
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	public Declaration[] getSelectedDeclarations() {
		return ArrayUtil.convertArray(this.getResult(), Declaration.class);
	}

}
