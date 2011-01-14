package net.arctics.clonk.resource.c4group;

import org.eclipse.core.runtime.CoreException;

/**
 * Interface used to filter files in c4groups so they won't be loaded into memory
 */
public abstract class C4GroupHeaderFilterBase {
	
	public static final int DONTREADINTOMEMORY = 1;
	
	public abstract boolean accepts(C4GroupEntryHeader header, C4Group context);
	
	public void processData(C4GroupItem item) throws CoreException {
		
	}
	
	public void created(C4GroupEntryHeader header, C4GroupItem item) {
		
	}
	
	public int getFlags(C4GroupEntry entry) {
		return 0;
	}
}