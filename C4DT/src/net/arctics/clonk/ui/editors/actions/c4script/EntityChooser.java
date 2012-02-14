package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.ArrayUtil.convertArray;
import static net.arctics.clonk.util.ArrayUtil.setFromIterable;

import java.util.Comparator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class EntityChooser extends FilteredItemsSelectionDialog {

	protected final class Filter extends ItemsFilter {
		private Pattern[] patterns;

		public Pattern[] getPatterns() {
			if (patterns == null)
				patterns = ArrayUtil.map(this.getPattern().split(" "), Pattern.class, CASEINSENSITIVE_PATTERNS_FROM_STRINGS);
			return patterns;
		}
		
		@Override
		public boolean isConsistentItem(Object item) {
			return false;
		}

		@Override
		public boolean matchItem(Object item) {
			IIndexEntity entity = (IIndexEntity)item;
			for (Pattern p : getPatterns()) {
				Matcher matcher = p.matcher("");
				if (entity.matchedBy(matcher))
					return true;
			}
			return false;
		}
	}

	private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements IStyledLabelProvider {
		@Override
		public StyledString getStyledText(Object element) {
			if (element != null) {
				StyledString result = ClonkOutlineProvider.getStyledTextForEveryone(element);
				result.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				if (element instanceof IHasRelatedResource)
					result.append(((IHasRelatedResource)element).resource().getProjectRelativePath().toOSString(), StyledString.QUALIFIER_STYLER);
				return result;
			} else
				return new StyledString("");
		}
	}
	
	private static final String DIALOG_SETTINGS = "DeclarationChooserDialogSettings"; //$NON-NLS-1$
	
	private final Set<? extends IIndexEntity> entities;
	private Index index;

	public EntityChooser(Shell shell, Set<? extends IIndexEntity> entities) {
		super(shell);
		this.entities = entities;
		setListLabelProvider(new LabelProvider());
		setTitle(Messages.DeclarationChooser_Label);
	}
	
	public EntityChooser(Shell shell, Iterable<? extends IIndexEntity> declarations) {
		this(shell, setFromIterable(declarations));
	}
	
	public EntityChooser(Shell shell, Index index) {
		this(shell, (Set<? extends IIndexEntity>)null);
		this.index = index;
	}

	@Override
	public void create() { 
		super.create();
		if (entities != null && getInitialPattern() == null)
			((Text)this.getPatternControl()).setText(entities.iterator().next().name());
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	private static IConverter<String, Pattern> CASEINSENSITIVE_PATTERNS_FROM_STRINGS = new IConverter<String, Pattern>() {
		@Override
		public Pattern convert(String from) {
			return StringUtil.patternFromRegExOrWildcard(from);
		}
	};
	
	@Override
	protected ItemsFilter createFilter() {
		return new Filter();
	}
	
	@Override
	protected void fillContentProvider(final AbstractContentProvider contentProvider, final ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		// load scripts that have matching declaration names in their dictionaries
		final Pattern[] patternStrings = ((Filter)itemsFilter).getPatterns();
		final Runnable refreshListRunnable = new Runnable() {
			@Override
			public void run() {
				refresh();
			}
		};
		if (index != null)
			index.forAllRelevantIndexes(new Index.r() {
				@Override
				public void run(Index index) {
					int declarationsBatchSize = 0;
					MainLoop: for (Script s : index.allScripts()) {
						if (s.dictionary() != null) {
							for (String str : s.dictionary()) {
								for (Pattern ps : patternStrings) {
									Matcher matcher = ps.matcher(str);
									if (matcher.lookingAt()) {
										s.requireLoaded();
										for (Declaration d : s.allSubDeclarations(IHasSubDeclarations.DIRECT_SUBDECLARATIONS))
											if (d.matchedBy(matcher)) {
												contentProvider.add(d, itemsFilter);
												if (++declarationsBatchSize == 5) {
													Display.getDefault().asyncExec(refreshListRunnable);
													declarationsBatchSize = 0;
												}
											}
										continue MainLoop;
									}
								}
							}
						}
					}
				}
			});
		if (entities != null)
			for (IIndexEntity d : entities)
				contentProvider.add(d, itemsFilter);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = ClonkCore.instance().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = ClonkCore.instance().getDialogSettings().addNewSection(DIALOG_SETTINGS);
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

	public IIndexEntity[] selectedEntities() {
		return convertArray(this.getResult(), IIndexEntity.class);
	}
	
	public boolean openSelection() {
		boolean b = true;
		for (IIndexEntity e : selectedEntities()) {
			ClonkHyperlink.openTarget(e, b);
			b = false;
		}
		return !b;
	}
	
	public void run() {
		if (open() == Window.OK)
			openSelection();
	}

}
