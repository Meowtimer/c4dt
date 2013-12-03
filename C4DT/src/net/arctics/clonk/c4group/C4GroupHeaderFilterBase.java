package net.arctics.clonk.c4group;

import org.eclipse.core.runtime.CoreException;

/**
 * Interface used to filter files in {@link C4Group}s so they won't be loaded into memory
 */
public abstract class C4GroupHeaderFilterBase {
	
	public static final int DONTREADINTOMEMORY = 1;
	
	public abstract boolean accepts(C4GroupEntryHeader header, C4Group context);
	
	public void processGroupItem(final C4GroupItem item) throws CoreException {
		
	}
	
	public void notifyAboutCreatedItem(final C4GroupEntryHeader header, final C4GroupItem item) {
		
	}
	
	public int flagsForEntry(final C4GroupFile entry) {
		return 0;
	}
}