package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.ISerializationResolvable;
import net.arctics.clonk.index.Index;

/**
 * Represents a C4ID. This class manages a global pool of unique {@link ID} objects and restricts construction of new instances to calling {@link #get(String)}..
 * @author madeen
 *
 */
public final class ID implements Serializable, ISerializationResolvable {
	private static final Map<String, ID> idPool = new HashMap<String, ID>();
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final ID NULL = get("NULL"); //$NON-NLS-1$

	private final String name;

	private ID(String id) {
		name = id;
		idPool.put(id, this);
	}

	/**
	 * Resolve serialized {@link ID} by returning an interned version of it.
	 */
	@Override
	public ID resolve(Index index) {
		synchronized (idPool) {
			ID special = idPool.get(name);
			if (special == null) {
				idPool.put(name, this);
				return this;
			}
			return special;
		}
	}

	/**
	 * Return an {@link ID} instance with the specified string value.
	 * @param stringValue The string value
	 * @return A newly created {@link ID} added to the global pool or an already existing one.
	 */
	public static ID get(String stringValue) {
		synchronized (idPool) {
			ID existing = idPool.get(stringValue);
			return existing != null ? existing : new ID(stringValue);
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

	/**
	 * Return the length of this ID's {@link #stringValue()}
	 * @return The length
	 */
	public int length() {
		return name.length();
	}
}
