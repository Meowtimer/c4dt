package net.arctics.clonk.resource.c4group;

import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;

public interface IC4GroupVisitor {
	/**
	 * Visits the <tt>item</tt> recursively.<br>
	 * <tt>packageType</tt> is the type of the group file.
	 * @param item
	 * @param packageType type of the group file
	 * @return <code>true</code>, if children should be visited too
	 */
	public boolean visit(C4GroupItem item, C4GroupType packageType);
	/**
	 * Called to inform the visitor that all the children of group have been visited
	 * @param group the group
	 */
	public void groupFinished(C4Group group);
}
