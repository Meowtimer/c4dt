package net.arctics.clonk.ini;

import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.IHasKeyAndValue;

/**
 * Sub item of an ini entry.
 *
 * @param <EntryType> Entry class
 */
public class EntrySubItem<EntryType extends IHasChildrenWithContext>
	implements IHasContext, IHasKeyAndValue<String, String>, IHasChildrenWithContext {
	public EntrySubItem(final EntryType entry, final Object context, final int index) {
		super();
		this.entry = entry;
		this.context = context;
		this.index = index;
	}
	private final EntryType entry;
	private final Object context;
	private final int index;
	@Override
	public Object context() { return context; }
	@Override
	public String key() { return String.format("[%d]", index); } //$NON-NLS-1$
	@Override
	public String stringValue() { return entry.valueOfChildAt(index).toString(); }
	@Override
	public Object valueOfChildAt(final int index) { return null; }
	@Override
	public IHasContext[] children(final Object context) { return null; }
	@Override
	public boolean hasChildren() { return false; }
}
