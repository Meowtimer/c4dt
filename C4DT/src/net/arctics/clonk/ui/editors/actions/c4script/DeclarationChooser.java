package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Declaration.DeclarationLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class DeclarationChooser extends FilteredItemsSelectionDialog {

	private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements IStyledLabelProvider {
		@Override
		public StyledString getStyledText(Object element) {
			if (element != null) {
				DeclarationLocation decLocation = (DeclarationLocation) element;
				StyledString result = ClonkOutlineProvider.getStyledTextForEveryone(decLocation.getDeclaration());
				result.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				if (decLocation.getResource() != null)
					result.append(decLocation.getResource().getProjectRelativePath().toOSString(), StyledString.QUALIFIER_STYLER);
				return result;
			} else
				return new StyledString("");
		}
	}
	
	private static final String DIALOG_SETTINGS = "DeclarationChooserDialogSettings"; //$NON-NLS-1$
	
	private Set<DeclarationLocation> declarations;
	private Index index;

	public DeclarationChooser(Shell shell, Set<DeclarationLocation> proposedDeclarations) {
		super(shell);
		this.declarations = proposedDeclarations;
		setListLabelProvider(new LabelProvider());
		setTitle(Messages.DeclarationChooser_Label);
	}
	
	public DeclarationChooser(Shell shell, Index index) {
		this(shell, (Set<DeclarationLocation>)null);
		this.index = index;
	}
	
	public DeclarationChooser(Shell shell, Set<Declaration> proposedDeclarations, boolean doYouHateIt) {
		this(shell, getFirstDeclarationsFromDeclarationLocationsOf(proposedDeclarations));
	}

	private static Set<DeclarationLocation> getFirstDeclarationsFromDeclarationLocationsOf(Collection<Declaration> proposedDeclarations) {
		Set<DeclarationLocation> l = new HashSet<DeclarationLocation>();
		for (Declaration d : proposedDeclarations) {
			DeclarationLocation[] locations = d.getDeclarationLocations();
			if (locations != null) {
				for (DeclarationLocation dl : locations) {
					l.add(dl);
					break;
				}
			}
		}
		return l;
	}

	@Override
	public void create() { 
		super.create();
		if (declarations != null)
			((Text)this.getPatternControl()).setText(declarations.iterator().next().getDeclaration().getName());
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	private IConverter<String, String> STRING_UPPERCASER = new IConverter<String, String>() {
		@Override
		public String convert(String from) {
			return from.toUpperCase();
		}
	};
	
	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {
			
			@Override
			public boolean isConsistentItem(Object item) {
				return false;
			}

			@Override
			public boolean matchItem(Object item) {
				String[] patternStrings = ArrayUtil.map(this.getPattern().split(" "), String.class, STRING_UPPERCASER);
				if (item instanceof DeclarationLocation)
					item = ((DeclarationLocation)item).getDeclaration();
				if (!(item instanceof Declaration))
					return false;
				final Declaration decl = (Declaration) item;
				for (String p : patternStrings) {
					final Structure structure = decl.getTopLevelStructure();
					if (!(decl.nameContains(p) || (structure != null && structure.nameContains(p))))
						return false;
				}
				return true;
			}
			
		};
	}
	
	@Override
	protected void fillContentProvider(final AbstractContentProvider contentProvider, final ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		if (declarations != null)
			for (DeclarationLocation d : declarations)
				contentProvider.add(d, itemsFilter);
		else if (index != null)
			index.forAllRelevantIndexes(new Index.r() {
				@Override
				public void run(Index index) {
					for (List<Declaration> decs : index.declarationMap().values())
						for (Declaration d : decs)
							if (d.getScript() != null && d.getScript().getScriptFile() != null)
								contentProvider.add(new DeclarationLocation(d, d.getLocation(), d.getScript().getScriptFile()), itemsFilter);
				}
			});
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = ClonkCore.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = ClonkCore.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
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
			@Override
			public int compare(Object a, Object b) {
				return a.toString().compareTo(b.toString());
			}
		};
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	public DeclarationLocation[] getSelectedDeclarationLocations() {
		return ArrayUtil.convertArray(this.getResult(), DeclarationLocation.class);
	}
	
	public boolean openSelection() {
		boolean b = true;
		for (DeclarationLocation loc : getSelectedDeclarationLocations()) {
			ClonkTextEditor.openDeclarationLocation(loc, b);
			b = false;
		}
		return b;
	}
	
	public void run() {
		if (open() == Window.OK)
			openSelection();
	}

}
