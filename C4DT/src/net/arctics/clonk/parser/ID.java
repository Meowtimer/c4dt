package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class ID implements Serializable {
	private static final Map<String, ID> idPool = new HashMap<String, ID>();
	private static final long serialVersionUID = 833007356188766488L;
	public static final ID NULL = getID("NULL"); //$NON-NLS-1$
	
	private String name;
	
	protected ID(String id) {
		name = id;
		idPool.put(id, this);
	}
	
	public ID internalize() {
		ID special = idPool.get(name);
		if (special == null) {
			idPool.put(name, this);
			return this;
		}
		return special;
	}
	
	public static ID getSpecialID(String infoText) {
		if (idPool.containsKey(infoText)) {
			return idPool.get(infoText);
		}
		else {
			return new ID(infoText);
		}
	}
	
	public static ID getID(String id) {
		if (idPool.containsKey(id)) {
			return idPool.get(id);
		}
		else {
			return new ID(id);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ID && ((ID)obj).getName().equals(getName());
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
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	public final int length() {
		return name.length();
	}
}
