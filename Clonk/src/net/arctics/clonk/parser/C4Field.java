package net.arctics.clonk.parser;

import java.io.Serializable;

public abstract class C4Field implements Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String name;
	protected SourceLocation location;
	protected C4Field parentField;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param location the location to set
	 */
	public void setLocation(SourceLocation location) {
		this.location = location;
	}
	/**
	 * @return the location
	 */
	public SourceLocation getLocation() {
		return location;
	}
	public int sortCategory() {
		// TODO Auto-generated method stub
		return 0;
	}
	/**
	 * @param object the object to set
	 */
	public void setObject(C4Object object) {
		setParentField(object);
	}
	/**
	 * @return the object
	 */
	public C4Object getObject() {
		for (C4Field f = this; f != null; f = f.parentField)
			if (f instanceof C4Object)
				return (C4Object)f;
		return null;
	}
	public void setParentField(C4Field field) {
		this.parentField = field;
	}
	public String getShortInfo() {
		return getName();
	}
	public Object[] getChildFields() {
		return null;
	}
	public boolean hasChildFields() {
		return false;
	}
	public C4Field latestVersion() {
		if (parentField instanceof C4Structure)
			return ((C4Structure)parentField).findField(getName());
		return this;
	}
}
