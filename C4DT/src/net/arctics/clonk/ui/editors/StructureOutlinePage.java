package net.arctics.clonk.ui.editors;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.ArrayUtil.list;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.block;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.ui.navigator.WeakReferencingContentProvider;
import net.arctics.clonk.util.StringUtil;

public class StructureOutlinePage extends ContentOutlinePage {

	private Composite composite;
	private StructureTextEditor editor;
	private Text filterBox;

	@Override
	public Control getControl() { return composite; }
	public StructureTextEditor editor() { return editor; }

	private IIndexEntity pampelmuse(Object from) {
		return
			from instanceof IAdaptable ? (IIndexEntity)((IAdaptable)from).getAdapter(Declaration.class) :
			from instanceof IIndexEntity ? (IIndexEntity)from :
			from instanceof Declaration ? ((Declaration)from).parent(IIndexEntity.class) : null;
	}

	private void openForeignDeclarations() {
		stream(((IStructuredSelection)getTreeViewer().getSelection()).toArray())
			.map(this::pampelmuse)
			.filter(e -> e != null)
			.forEach(entity -> {
				final List<? extends IIndexEntity> entities =
					entity instanceof Directive ? block(() -> {
						final Directive d = (Directive)entity;
						final Iterable<? extends Definition> defs =
							editor.structure().index().definitionsWithID(d.contentAsID());
						return defs != null ? list(defs) : list(entity);
					}) : list(entity);
				new EntityHyperlink(null, entities).open();
			});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(final Composite parent) {
		composite = new Composite(parent, SWT.NO_SCROLL);
		final GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);
		filterBox = new Text(composite, SWT.SEARCH | SWT.CANCEL);
		filterBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		filterBox.addModifyListener(e -> getTreeViewer().refresh());
		super.createControl(composite);
		getTreeViewer().getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		getTreeViewer().setFilters(new ViewerFilter[] {
			new ViewerFilter() {
				@SuppressWarnings({ })
				@Override
				public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
					if (StringUtil.patternFromRegExOrWildcard(filterBox.getText()).matcher(((ILabelProvider)getTreeViewer().getLabelProvider()).getText(element)).find())
						return true;
					if (element instanceof Declaration) {
						final Object[] subDecs = ((Declaration)element).subDeclarationsForOutline();
						return subDecs != null && stream(subDecs).anyMatch(sd -> select(viewer, element, sd));
					}
					return false;
				}
			}
		});
		getTreeViewer().getTree().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.keyCode == SWT.CR)
					openForeignDeclarations();
			}
			@Override
			public void keyPressed(final KeyEvent e) {}
		});
		getTreeViewer().getTree().addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(final MouseEvent e) {}
			@Override
			public void mouseDown(final MouseEvent e) {}
			@Override
			public void mouseDoubleClick(final MouseEvent e) {openForeignDeclarations();}
		});
		if (editor != null) {
			final Declaration topLevelDeclaration = editor().structure();
			if (topLevelDeclaration != null)
				setTreeViewerInput(topLevelDeclaration);
		}
		parent.layout();
	}

	private final ViewerSorter DECLARATION_SORTER = new ViewerSorter() {
		@Override
		public int category(final Object element) {
			if (element instanceof Declaration) {
				final Declaration d = (Declaration)element;
				return d.sortCategory() * (d.containedIn(editor.structure()) ? 1 : 1000);
			} else
				return 10000;
		}
	};

	private void setTreeViewerInput(final Declaration obj) {
		final TreeViewer treeViewer = this.getTreeViewer();
		if (treeViewer == null)
			return;
		final WeakReferencingContentProvider<ClonkOutlineProvider> provider = new WeakReferencingContentProvider<ClonkOutlineProvider>(new ClonkOutlineProvider());
		treeViewer.setLabelProvider(provider);
		treeViewer.setContentProvider(provider);
		treeViewer.setSorter(provider.sorter(DECLARATION_SORTER));
		treeViewer.setInput(obj);
		treeViewer.refresh();
	}

	@Override
	public void selectionChanged(final SelectionChangedEvent event) {
		if (event.getSelection().isEmpty())
			return;
		if (event.getSelection() instanceof IStructuredSelection) {
			Declaration dec = ((IAdaptable)((IStructuredSelection)event.getSelection()).getFirstElement()).getAdapter(Declaration.class);
			if (dec != null)
				dec = dec.latestVersion();
			if (dec != null)
				if (dec.containedIn(editor.structure()))
					editor.selectAndReveal(dec.regionToSelect());
		}
	}

	/**
	 * @param editor the editor to set
	 */
	public void setEditor(final StructureTextEditor editor) {
		this.editor = editor;
	}

	public void refresh() {
		final Declaration newInput = editor().structure();
		final Declaration section = as(editor().section(), Declaration.class);
		if (getTreeViewer().getInput() != newInput)
			setTreeViewerInput(newInput);
		else
			getTreeViewer().refresh();
		if (section != null) {
			final Object[] subs = section.subDeclarationsForOutline();
			if (subs.length > 0)
				getTreeViewer().reveal(subs[0]);
		}
	}

	public void select(final Declaration field) {
		final TreeViewer viewer = getTreeViewer();
		viewer.removeSelectionChangedListener(this);
		try {
			this.setSelection(new StructuredSelection(field));
		} finally {
			viewer.addSelectionChangedListener(this);
		}
	}

	public void setInput(final Object input) {
		getTreeViewer().setInput(input);
	}

	public void clear() {
		getTreeViewer().setInput(null);
	}

}
