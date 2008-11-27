package net.arctics.clonk.parser;

import org.eclipse.core.resources.IContainer;

import net.arctics.clonk.Utilities;

public abstract class C4Object extends C4ScriptBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected C4ID id;
	
//	private List<IC4ObjectListener> changeListeners = new LinkedList<IC4ObjectListener>();
	
	/**
	 * Creates a new C4Object and assigns it to <code>container</code>
	 * @param id C4ID (e.g. CLNK)
	 * @param name intern name
	 * @param container ObjectName.c4d resource
	 */
	protected C4Object(C4ID id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return getName() + " (" + id.toString() + ")";
	}
	
	/**
	 * The id of this object. (e.g. CLNK)
	 * @return the id
	 */
	public C4ID getId() {
		return id;
	}
	
	/**
	 * Sets the id property of this object.
	 * This method does not change resources.
	 * @param newId
	 */
	public void setId(C4ID newId) {
		if (id.equals(newId))
			return;
		ClonkIndex index = this.getIndex();
		index.removeObject(this);
		id = newId;
		index.addObject(this);
	}
	

	@Override
	protected boolean refersToThis(String name, FindFieldInfo info) {
		if (info.getFieldClass() == null || info.getFieldClass() == C4Object.class) {
			if (id != null && id.getName().equals(name))
				return true;
		}
		return false;
	}
	
}
