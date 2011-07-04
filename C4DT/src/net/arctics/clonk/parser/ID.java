package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.index.IResolvable;
import net.arctics.clonk.index.Index;

/**
 * Represents a C4ID.
 * @author madeen
 *
 */
public final class ID implements Serializable, IResolvable {
	private static final Map<String, ID> idPool = new HashMap<String, ID>();
	private static final long serialVersionUID = 833007356188766488L;
	public static final ID NULL = get("NULL"); //$NON-NLS-1$
	
	private String name;
	
	private ID(String id) {
		name = id;
		idPool.put(id, this);
	}
	
	@Override
	public ID resolve(Index index) {
		ID special = idPool.get(name);
		if (special == null) {
			idPool.put(name, this);
			return this;
		}
		return special;
	}
	
	/**
	 * Return an {@link ID} instance with the specified string value.
	 * @param stringValue The string value
	 * @return A newly created {@link ID} added to the global pool or an already existing one.
	 */
	public static ID get(String stringValue) {
		if (idPool.containsKey(stringValue)) {
			return idPool.get(stringValue);
		}
		else {
			return new ID(stringValue);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ID && ((ID)obj).stringValue().equals(stringValue());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	/**
	 * The string representation of the id. (e.g. CLNK)
	 * @return the name
	 */
	public String stringValue() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return stringValue();
	}

	public final int length() {
		return name.length();
	}
}
