package net.arctics.clonk.resource.c4group;

public abstract interface IHeaderFilterCreationListener extends IHeaderFilter {
	void created(C4EntryHeader header, C4GroupItem item);
}