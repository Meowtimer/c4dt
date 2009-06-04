package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.Collection;
import java.util.Comparator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class DeclarationChooser extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "DeclarationChooserDialogSettings";
	
	private Collection<C4Declaration> declarations;

	public DeclarationChooser(Shell shell, Collection<C4Declaration> declarations) {
		super(shell);
		this.declarations = declarations;
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
				if (!(item instanceof C4Declaration)) return false;
				final C4Declaration decl = (C4Declaration) item;
				final String search = this.getPattern().toUpperCase();
				final C4Structure structure = decl.getTopLevelStructure();
				return decl.nameContains(search) || (structure != null && structure.nameContains(search));
			}
			
		};
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		for (C4Declaration d : declarations)
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
		return ClonkOutlineProvider.getStyledTextForEveryone(item).toString();
	}

	@Override
	protected Comparator<?> getItemsComparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	public C4Declaration[] getSelectedDeclarations() {
		return Utilities.convertArray(this.getResult(), C4Declaration.class);
	}

}
