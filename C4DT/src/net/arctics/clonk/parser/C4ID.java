package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class C4ID implements Serializable {
	private static final Map<String, C4ID> idPool = new HashMap<String, C4ID>();
	private static final long serialVersionUID = 833007356188766488L;
	public static final C4ID NULL = getID("NULL"); //$NON-NLS-1$
	
	private String name;
	
	protected C4ID(String id) {
		name = id;
		idPool.put(id, this);
	}
	
	public C4ID makeSpecial() {
		C4ID special = idPool.get(name);
		if (special == null) {
			idPool.put(name, this);
			return this;
		}
		return special;
	}
	
	public static C4ID getSpecialID(String infoText) {
		if (idPool.containsKey(infoText)) {
			return idPool.get(infoText);
		}
		else {
			return new C4ID(infoText);
		}
	}
	
	public static C4ID getID(String id) {
		if (idPool.containsKey(id)) {
			return idPool.get(id);
		}
		else {
			return new C4ID(id);
		}
	}
	
	public static boolean isValidC4ID(String c4id) {
		if (c4id == null) return false;
		if (c4id.length() != 4) return false;
		if (!c4id.matches("^[A-Z0-9_]{4}$")) return false; //$NON-NLS-1$
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof C4ID && ((C4ID)obj).getName().equals(getName());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (name == null) return 0;
		return name.hashCode();
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
}
