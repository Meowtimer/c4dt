package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.ArrayUtil.convertArray;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ui.editors.EntityHyperlink;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IResource;
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
		private final Pattern[] patterns;
		public Filter() {
			super();
			patterns = ArrayUtil.map(((Text)getPatternControl()).getText().split(" "), Pattern.class, CASEINSENSITIVE_PATTERNS_FROM_STRINGS);
		}
		@Override
		public boolean equalsFilter(final ItemsFilter filter) {
			if (filter instanceof Filter) {
				final Filter f = (Filter)filter;
				if (f.patterns.length != this.patterns.length)
					return false;
				for (int i = 0; i < patterns.length; i++)
					if (!patterns[i].equals(f.patterns[i]))
						return false;
				return true;
			}
			return false;
		}
		public Pattern[] getPatterns() { return patterns; }
		@Override
		public boolean isConsistentItem(final Object item) { return false; }
		@Override
		public boolean matchItem(final Object item) {
			final IIndexEntity entity = (IIndexEntity)item;
			for (final Pattern p : getPatterns()) {
				final Matcher matcher = p.matcher("");
				if (!entity.matchedBy(matcher))
					return false;
			}
			for (final Pattern p : getPatterns())
				entity.matchedBy(p.matcher(""));
			return true;
		}
	}

	private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements IStyledLabelProvider {
		@Override
		public StyledString getStyledText(final Object element) {
			if (element != null) {
				final StyledString result = ClonkOutlineProvider.styledTextFor(element, false, null);
				if (element instanceof Declaration && ((Declaration)element).parentDeclaration() instanceof Engine) {
					result.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					result.append(((Declaration)element).parentDeclaration().name());
				}
				if (element instanceof IHasRelatedResource) {
					final IResource resource = ((IHasRelatedResource)element).resource();
					if (resource != null) {
						result.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
						result.append(resource.getProjectRelativePath().toOSString(), StyledString.QUALIFIER_STYLER);
					}
				}
				return result;
			} else
				return new StyledString("");
		}
	}

	private static final String DIALOG_SETTINGS = "DeclarationChooserDialogSettings"; //$NON-NLS-1$

	private final Set<? extends IIndexEntity> entities;

	public EntityChooser(final String title, final Shell shell, final Collection<? extends IIndexEntity> entities) {
		super(shell, true);
		this.entities = entities != null ? new HashSet<IIndexEntity>(entities) : null;
		setTitle(title);
		setListLabelProvider(new LabelProvider());
	}

	public EntityChooser(final String title, final Shell shell) { this(title, shell, null); }

	@Override
	public void create() {
		super.create();
		if (entities != null && getInitialPattern() == null)
			((Text)this.getPatternControl()).setText(".*");
	}

	@Override
	protected Control createExtendedContentArea(final Composite parent) {
		return null;
	}

	private static IConverter<String, Pattern> CASEINSENSITIVE_PATTERNS_FROM_STRINGS = from -> StringUtil.patternFromRegExOrWildcard(from);

	@Override
	protected ItemsFilter createFilter() { return new Filter(); }

	@Override
	protected void fillContentProvider(final AbstractContentProvider contentProvider, final ItemsFilter itemsFilter, final IProgressMonitor progressMonitor) throws CoreException {
		// load scripts that have matching declaration names in their dictionaries
		final Pattern[] patternStrings = ((Filter)itemsFilter).getPatterns();
		final Runnable refreshListRunnable = new Runnable() {
			@Override
			public void run() {
				refresh();
			}
		};
		if (entities != null)
			for (final IIndexEntity d : entities)
				if (d instanceof Index) {
					final Index index = (Index)d;
					index.forAllRelevantIndexes(new Sink<Index>() {
						@Override
						public void receive(final Index index) {
							index.allScripts(new Sink<Script>() {
								int declarationsBatchSize = 0;
								@Override
								public void receive(final Script s) {
									if (progressMonitor.isCanceled())
										return;
									if (s.dictionary() != null)
										for (final String str : s.dictionary())
											for (final Pattern ps : patternStrings) {
												final Matcher matcher = ps.matcher(str);
												if (matcher.lookingAt()) {
													s.requireLoaded();
													for (final Declaration d : s.subDeclarations(s.index(), DeclMask.ALL))
														if (!(d instanceof Directive) && d.matchedBy(matcher)) {
															contentProvider.add(d, itemsFilter);
															if (++declarationsBatchSize == 5) {
																Display.getDefault().asyncExec(refreshListRunnable);
																declarationsBatchSize = 0;
															}
														}
													return;
												}
											}
								}
							});
						}
					});
				}
				else if (d instanceof Engine)
					for (final Declaration engineDeclaration : ((Engine)d).subDeclarations(null, DeclMask.ALL))
						contentProvider.add(engineDeclaration, itemsFilter);
				else
					contentProvider.add(d, itemsFilter);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = Core.instance().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null)
			settings = Core.instance().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		return settings;
	}

	@Override
	public String getElementName(final Object item) {
		return item.toString();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getItemsComparator() {
		return new Comparator() {
			@Override
			public int compare(final Object a, final Object b) {
				return a.toString().compareTo(b.toString());
			}
		};
	}

	@Override
	protected IStatus validateItem(final Object item) {
		return Status.OK_STATUS;
	}

	public IIndexEntity[] selectedEntities() {
		return convertArray(this.getResult(), IIndexEntity.class);
	}

	public boolean openSelection() {
		boolean b = true;
		for (final IIndexEntity e : selectedEntities()) {
			EntityHyperlink.openTarget(e, b);
			b = false;
		}
		return !b;
	}

	public void run() {
		if (open() == Window.OK)
			openSelection();
	}

}
