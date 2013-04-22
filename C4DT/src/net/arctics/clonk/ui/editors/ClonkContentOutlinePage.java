package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.ArrayUtil.map;

import java.util.List;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.ui.navigator.WeakReferencingContentProvider;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StringUtil;

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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class ClonkContentOutlinePage extends ContentOutlinePage {

	private Composite composite;
	private ClonkTextEditor editor;
	private Text filterBox;
	
	@Override
	public Control getControl() { return composite; }
	public ClonkTextEditor editor() { return editor; }

	private void openForeignDeclarations() {
		final IStructuredSelection sel = (IStructuredSelection)getTreeViewer().getSelection();
		for (final IIndexEntity entity : map(sel.toArray(), IIndexEntity.class, new IConverter<Object, IIndexEntity>() {
			@Override
			public IIndexEntity convert(Object from) {
				if (from instanceof IAdaptable)
					from = ((IAdaptable)from).getAdapter(Declaration.class);
				if (from instanceof IIndexEntity)
					return (IIndexEntity)from;
				else if (from instanceof Declaration)
					return ((Declaration)from).parentOfType(IIndexEntity.class);
				else
					return null;
			}
		}))
			if (entity != null) {
				List<? extends IIndexEntity> entities;
				if (entity instanceof Directive) {
					final Directive d = (Directive)entity;
					final Iterable<? extends Definition> defs = editor.structure().index().definitionsWithID(d.contentAsID());
					if (defs != null)
						entities = ArrayUtil.list(defs);
					else
						entities = ArrayUtil.list(entity); 
				} else
					entities = ArrayUtil.list(entity);
				new ClonkHyperlink(null, entities).open();
			}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NO_SCROLL);
		final GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);
		filterBox = new Text(composite, SWT.SEARCH | SWT.CANCEL);
		filterBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		filterBox.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTreeViewer().refresh();
			}
		});
		super.createControl(composite);
		getTreeViewer().getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		getTreeViewer().setFilters(new ViewerFilter[] {
			new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (StringUtil.patternFromRegExOrWildcard(filterBox.getText()).matcher(((ILabelProvider)getTreeViewer().getLabelProvider()).getText(element)).find())
						return true;
					if (element instanceof Declaration) {
						final Object[] subDecs = ((Declaration)element).subDeclarationsForOutline();
						if (subDecs != null)
							for (final Object sd : subDecs)
								if (select(viewer, element, sd))
									return true;
					}
					return false;
				}
			}
		});
		getTreeViewer().getTree().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.CR)
					openForeignDeclarations();
			}
			@Override
			public void keyPressed(KeyEvent e) {}
		});
		getTreeViewer().getTree().addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {}
			@Override
			public void mouseDown(MouseEvent e) {}
			@Override
			public void mouseDoubleClick(MouseEvent e) {openForeignDeclarations();}
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
		public int category(Object element) {
			int multiplier = 1;
			if (element instanceof Declaration) {
				final Declaration d = (Declaration)element;
				if (!d.containedIn(editor.structure()))
					multiplier = 1000;
				return d.sortCategory() * multiplier;
			}
			return 10000;
		}
	};
	
	private void setTreeViewerInput(Declaration obj) {
		final TreeViewer treeViewer = this.getTreeViewer();
		if (treeViewer == null)
			return;
		final WeakReferencingContentProvider<ClonkOutlineProvider> provider = new WeakReferencingContentProvider<ClonkOutlineProvider>(new ClonkOutlineProvider(this));
		treeViewer.setLabelProvider(provider);
		treeViewer.setContentProvider(provider);
		treeViewer.setSorter(provider.sorter(DECLARATION_SORTER));
		treeViewer.setInput(obj);
		treeViewer.refresh();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSelection().isEmpty())
			return;
		if (event.getSelection() instanceof IStructuredSelection) {
			Declaration dec = (Declaration) ((IAdaptable)((IStructuredSelection)event.getSelection()).getFirstElement()).getAdapter(Declaration.class);
			if (dec != null)
				dec = dec.latestVersion();
			if (dec != null)
				if (dec.containedIn(editor.structure()))
					editor.selectAndReveal(dec);
		}
	}

	/**
	 * @param clonkTextEditor the editor to set
	 */
	public void setEditor(ClonkTextEditor clonkTextEditor) {
		this.editor = clonkTextEditor;
	}

	public void refresh() {
		final Declaration newInput = editor().structure();
		if (getTreeViewer().getInput() != newInput)
			setTreeViewerInput(newInput);
		else
			getTreeViewer().refresh();
	}
	
	public void select(Declaration field) {
		final TreeViewer viewer = getTreeViewer();
		viewer.removeSelectionChangedListener(this);
		try {
			this.setSelection(new StructuredSelection(field));
		} finally {
			viewer.addSelectionChangedListener(this);
		}
	}
	
	public void setInput(Object input) {
		getTreeViewer().setInput(input);
	}

	public void clear() {
		getTreeViewer().setInput(null);
	}
	
}
