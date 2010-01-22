package net.arctics.clonk.resource.c4group;

import org.eclipse.core.runtime.CoreException;

/**
 * Interface used to filter files in c4groups so they won't be loaded into memory
 */
public interface IHeaderFilter {
	public boolean accepts(C4EntryHeader header, C4Group context);
	public abstract void processData(C4GroupItem item) throws CoreException;
}