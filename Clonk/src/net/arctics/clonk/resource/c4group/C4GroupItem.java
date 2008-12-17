package net.arctics.clonk.resource.c4group;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface C4GroupItem {
	
	public interface IHeaderFilter {
		public boolean accepts(C4EntryHeader header, C4Group context);
		public static final IHeaderFilter ACCEPT_EVERYTHING = new IHeaderFilter() {
			public boolean accepts(C4EntryHeader header, C4Group context) {
				return true;
			}
		};
	}
	
	/**
	 * Has this item children?
	 * @return
	 */
	public boolean hasChildren();
	
	/**
	 * Is this item completely read from hard disk?
	 * @return
	 */
	public boolean isCompleted();
	
	/**
	 * Read this item
	 * @throws InvalidDataException
	 * @throws IOException 
	 */
	public void open(boolean recursively, IHeaderFilter filter) throws InvalidDataException, IOException;
	
	/**
	 * Writes this entry and all sub items to stream
	 * @throws FileNotFoundException 
	 */
	public void writeTo(OutputStream stream) throws FileNotFoundException;
	
	/**
	 * The entry name
	 * @return
	 */
	public String getName();
	
	/**
	 * Returns the size of the object and all subitems
	 * @return
	 */
	public int computeSize();
	
	/**
	 * The parent group this item is a child from
	 * @return
	 */
	public C4Group getParentGroup();
	
	/**
	 * Returns the entry header
	 * @return
	 */
	public C4EntryHeader getEntryHeader();
	
	/**
	 * Extracts this file to hard disk
	 */
	public void extractToFilesystem(IContainer internPath) throws CoreException;
	
	/**
	 * Extracts this file to hard disk with the given progress monitor
	 */
	public void extractToFilesystem(IContainer internPath, IProgressMonitor monitor) throws CoreException;
}
