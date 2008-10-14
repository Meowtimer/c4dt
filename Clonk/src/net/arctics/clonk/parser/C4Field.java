package net.arctics.clonk.parser;

public abstract class C4Field  {
	protected String name;
	protected SourceLocation location;
	private C4Object object;

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
		this.object = object;
	}
	/**
	 * @return the object
	 */
	public C4Object getObject() {
		return object;
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
}
