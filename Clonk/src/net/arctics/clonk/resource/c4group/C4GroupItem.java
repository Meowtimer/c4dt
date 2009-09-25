package net.arctics.clonk.resource.c4group;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface C4GroupItem {
	
	/**
	 * Interface used to filter files in c4groups so they won't be loaded into memory
	 */
	public interface IHeaderFilter {
		public boolean accepts(C4EntryHeader header, C4Group context);
		public static final IHeaderFilter ACCEPT_EVERYTHING = new IHeaderFilter() {
			public boolean accepts(C4EntryHeader header, C4Group context) {
				return true;
			}
			public void processData(C4GroupItem item) {
				// no processing
			}
		};
		public void processData(C4GroupItem item) throws CoreException;
	}
	
	/**
	 * Does this item have children?
	 * @return
	 */
	public boolean hasChildren();
	
	/**
	 * Is this item completely read from disk?
	 * @return
	 */
	public boolean isCompleted();
	
	/**
	 * Read this item
	 * @throws InvalidDataException
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public void readIntoMemory(boolean recursively, IHeaderFilter filter) throws InvalidDataException, IOException, CoreException;
	
	/**
	 * Writes this entry and all sub items to the stream
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public void writeTo(OutputStream stream) throws FileNotFoundException, IOException;
	
	/**
	 * The entry name
	 * @return
	 */
	public String getName();
	
	/**
	 * Returns the size of the object and all sub items
	 * @return
	 */
	public int computeSize();
	
	/**
	 * The parent group this item is a child of
	 * @return
	 */
	public C4Group getParentGroup();
	
	/**
	 * Returns the entry header
	 * @return
	 */
	public C4EntryHeader getEntryHeader();
	
	/**
	 * Extracts this file to disk
	 */
	public void extractToFilesystem(IContainer internPath) throws CoreException;
	
	/**
	 * Extracts this file to disk with the given progress monitor
	 */
	public void extractToFilesystem(IContainer internPath, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * recursively call visitor.visit for all items in this group (including the group itself)
	 */
	public void accept(IC4GroupVisitor visitor, C4GroupType type, IProgressMonitor monitor);
	
	/**
	 * release data stored in memory to preserve space
	 */
	public void releaseData();
	
}
