package net.arctics.clonk.resource.c4group;

import org.eclipse.core.runtime.CoreException;

/**
 * Interface used to filter files in {@link C4Group}s so they won't be loaded into memory
 */
public abstract class C4GroupHeaderFilterBase {
	
	public static final int DONTREADINTOMEMORY = 1;
	
	public abstract boolean accepts(C4GroupEntryHeader header, C4Group context);
	
	public void processGroupItem(C4GroupItem item) throws CoreException {
		
	}
	
	public void notifyAboutCreatedItem(C4GroupEntryHeader header, C4GroupItem item) {
		
	}
	
	public int flagsForEntry(C4GroupFile entry) {
		return 0;
	}
}