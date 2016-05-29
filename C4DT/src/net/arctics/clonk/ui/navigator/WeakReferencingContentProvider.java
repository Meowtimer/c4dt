package net.arctics.clonk.ui.navigator;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.lang.ref.WeakReference;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;

import net.arctics.clonk.ast.Declaration;

@SuppressWarnings("rawtypes")
public class WeakReferencingContentProvider<T extends ILabelProvider & ITreeContentProvider & IStyledLabelProvider> implements ILabelProvider, ITreeContentProvider, IStyledLabelProvider {
	
	private final T wrapped;
	
	private static class WeakItem extends WeakReference<Object> implements IAdaptable {
		public WeakItem(final Object referent) {
			super(referent);
		}
		
		@Override
		public String toString() {
			final Object o = get();
			return o != null ? o.toString() : "<Lost>";
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Object getAdapter(final Class adapter) {
			final Object obj = get();
			return adapter.isInstance(obj) ? obj : null;
		}
		
		private Object getLatest() {
			final Object weakReferenced = get();
			final Declaration declaration = as(weakReferenced, Declaration.class);
			final Declaration latestDeclaration = declaration != null ? declaration.latestVersion() : null;
			return defaulting(latestDeclaration, weakReferenced);
		}
		
		@Override
		public boolean equals(Object obj) {
			final WeakItem otherWeakItem = as(obj, WeakItem.class);
			return eq(getLatest(), otherWeakItem != null ? otherWeakItem.getLatest() : obj);
		}
		
	}
	
	private static class LabelProviderListenerWrapper implements ILabelProviderListener {
		private final ILabelProviderListener wrapped;
		public LabelProviderListenerWrapper(final ILabelProviderListener wrapped) {
			super();
			this.wrapped = wrapped;
		}
		@Override
		public void labelProviderChanged(final LabelProviderChangedEvent event) {
			wrapped.labelProviderChanged(new LabelProviderChangedEvent((IBaseLabelProvider)event.getSource(), wrap(event.getElements())));
		}
		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof LabelProviderListenerWrapper) {
				return ((LabelProviderListenerWrapper)obj).wrapped == this.wrapped;
			} else {
				return false;
			}
		}
	}
	
	public WeakReferencingContentProvider(final T wrapped) { this.wrapped = wrapped; }
	
	private static Object[] wrap(final Object[] raw) {
		if (raw == null) {
			return null;
		}
		final Object[] r = new WeakItem[raw.length];
		for (int i = 0; i < raw.length; i++) {
			r[i] = new WeakItem(raw[i]);
		}
		return r;
	}
	
	private static Object unwrap(final Object item) {
		return item instanceof WeakItem ? ((WeakItem)item).get() : item;
	}
	
	@Override
	public void addListener(final ILabelProviderListener listener) {
		wrapped.addListener(new LabelProviderListenerWrapper(listener));
	}
	
	@Override
	public void dispose() { wrapped.dispose(); }
	
	@Override
	public boolean isLabelProperty(final Object element, final String property) {
		return wrapped.isLabelProperty(unwrap(element), property);
	}
	
	@Override
	public void removeListener(final ILabelProviderListener listener) {
		wrapped.removeListener(new LabelProviderListenerWrapper(listener));
	}
	
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		wrapped.inputChanged(viewer, unwrap(oldInput), unwrap(newInput));
	}
	
	@Override
	public StyledString getStyledText(final Object element) { return wrapped.getStyledText(unwrap(element)); }
	
	@Override
	public Object[] getElements(final Object inputElement) {
		final Object[] raw = wrapped.getElements(unwrap(inputElement));
		return wrap(raw);
	}
	
	@Override
	public Object[] getChildren(final Object parentElement) {
		final Object[] raw = wrapped.getChildren(unwrap(parentElement));
		return wrap(raw);
	}
	
	@Override
	public Object getParent(final Object element) { return wrapped.getParent(unwrap(element)); }
	@Override
	public boolean hasChildren(final Object element) { return wrapped.hasChildren(unwrap(element)); }
	@Override
	public Image getImage(final Object element) { return wrapped.getImage(unwrap(element)); }
	@Override
	public String getText(final Object element) { return wrapped.getText(unwrap(element)); }
	
	public ViewerComparator comparator(final ViewerComparator wrappedSorter) {
		return new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				return wrappedSorter.compare(viewer, unwrap(e1), unwrap(e2));
			}
		};
	}
}
