package net.arctics.clonk.ui.navigator;

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
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings("rawtypes")
public class WeakReferencingContentProvider<T extends ILabelProvider & ITreeContentProvider & IStyledLabelProvider> implements ILabelProvider, ITreeContentProvider, IStyledLabelProvider {

	private final T wrapped;

	private static class WeakItem extends WeakReference<Object> implements IAdaptable {
		public WeakItem(Object referent) {
			super(referent);
		}
		@Override
		public String toString() {
			final Object o = get();
			return o != null ? o.toString() : "<Lost>";
		}
		@Override
		public Object getAdapter(Class adapter) {
			final Object obj = get();
			if (adapter.isInstance(obj))
				return obj;
			else
				return null;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof WeakItem)
				obj = ((WeakItem)obj).get();
			return eq(obj, this.get());
		}
	}

	private static class LabelProviderListenerWrapper implements ILabelProviderListener {
		private final ILabelProviderListener wrapped;
		public LabelProviderListenerWrapper(ILabelProviderListener wrapped) {
			super();
			this.wrapped = wrapped;
		}
		@Override
		public void labelProviderChanged(LabelProviderChangedEvent event) {
			wrapped.labelProviderChanged(new LabelProviderChangedEvent((IBaseLabelProvider)event.getSource(), wrap(event.getElements())));
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LabelProviderListenerWrapper)
				return ((LabelProviderListenerWrapper)obj).wrapped == this.wrapped;
			else
				return false;
		}
	}

	public WeakReferencingContentProvider(T wrapped) {
		this.wrapped = wrapped;
	}

	private static Object[] wrap(Object[] raw) {
		if (raw == null)
			return null;
		final Object[] r = new WeakItem[raw.length];
		for (int i = 0; i < raw.length; i++)
			r[i] = new WeakItem(raw[i]);
		return r;
	}

	private static Object unwrap(Object item) {
		if (item instanceof WeakItem)
			return ((WeakItem)item).get();
		else
			return item;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		wrapped.addListener(new LabelProviderListenerWrapper(listener));
	}

	@Override
	public void dispose() {
		wrapped.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return wrapped.isLabelProperty(unwrap(element), property);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		wrapped.removeListener(new LabelProviderListenerWrapper(listener));
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		wrapped.inputChanged(viewer, unwrap(oldInput), unwrap(newInput));
	}

	@Override
	public StyledString getStyledText(Object element) {
		return wrapped.getStyledText(unwrap(element));
	}

	@Override
	public Object[] getElements(Object inputElement) {
		final Object[] raw = wrapped.getElements(unwrap(inputElement));
		return wrap(raw);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		final Object[] raw = wrapped.getChildren(unwrap(parentElement));
		return wrap(raw);
	}

	@Override
	public Object getParent(Object element) {
		return wrapped.getParent(unwrap(element));
	}

	@Override
	public boolean hasChildren(Object element) {
		return wrapped.hasChildren(unwrap(element));
	}

	@Override
	public Image getImage(Object element) {
		return wrapped.getImage(unwrap(element));
	}

	@Override
	public String getText(Object element) {
		return wrapped.getText(unwrap(element));
	}

	public ViewerSorter sorter(final ViewerSorter wrappedSorter) {
		return new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return wrappedSorter.compare(viewer, unwrap(e1), unwrap(e2));
			}
		};
	}

}
