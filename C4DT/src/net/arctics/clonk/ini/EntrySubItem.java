package net.arctics.clonk.ini;

import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.IHasKeyAndValue;

/**
 * Sub item of an ini entry.
 *
 * @param <EntryType> Entry class
 */
public class EntrySubItem<EntryType extends IHasChildrenWithContext> implements IHasContext,
		IHasKeyAndValue<String, String>, IHasChildrenWithContext {
	
	public EntrySubItem(EntryType entry, Object context, int index) {
		super();
		this.entry = entry;
		this.context = context;
		this.index = index;
	}

	private final EntryType entry;
	private final Object context;
	private final int index;

	@Override
	public Object context() {
		return context;
	}

	@Override
	public String key() {
		return "["+String.valueOf(index)+"]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String stringValue() {
		return entry.valueOfChildAt(index).toString();
	}

	@Override
	public Object valueOfChildAt(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IHasContext[] children(Object context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setValueOfChildAt(int index, Object value) {
		// TODO Auto-generated method stub
		
	}

}
