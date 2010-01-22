package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.util.INodeWithPath;

import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

public abstract class C4GroupItem extends FileStore implements INodeWithPath {
	
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
		public abstract void processData(C4GroupItem item) throws CoreException;
	}
	
	public abstract interface IHeaderFilterCreationListener extends IHeaderFilter {
		void created(C4EntryHeader header, C4GroupItem item);
	}
	
	/**
	 * Does this item have children?
	 * @return
	 */
	public abstract boolean hasChildren();
	
	/**
	 * Is this item completely read from disk?
	 * @return
	 */
	public abstract boolean isCompleted();
	
	/**
	 * Read this item
	 * @throws InvalidDataException
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public abstract void readIntoMemory(boolean recursively, IHeaderFilter filter) throws InvalidDataException, IOException, CoreException;
	
	/**
	 * Writes this entry and all sub items to the stream
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public abstract void writeTo(OutputStream stream) throws FileNotFoundException, IOException;
	
	/**
	 * The entry name
	 * @return
	 */
	public abstract String getName();
	
	/**
	 * Returns the size of the object and all sub items
	 * @return
	 */
	public abstract int computeSize();
	
	/**
	 * The parent group this item is a child of
	 * @return
	 */
	public abstract C4Group getParentGroup();
	
	/**
	 * Returns the entry header
	 * @return
	 */
	public abstract C4EntryHeader getEntryHeader();
	
	/**
	 * Extracts this file to disk
	 */
	public abstract void extractToFilesystem(IContainer internPath) throws CoreException;
	
	/**
	 * Extracts this file to disk with the given progress monitor
	 */
	public abstract void extractToFilesystem(IContainer internPath, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * recursively call visitor.visit for all items in this group (including the group itself)
	 */
	public abstract void accept(IC4GroupVisitor visitor, C4GroupType type, IProgressMonitor monitor);
	
	/**
	 * release data stored in memory to preserve space
	 */
	public abstract void releaseData();
	
	@Override
	public URI toURI() {
		C4Group masterGroup = (this instanceof C4Group ? (C4Group)this : getParentGroup()).getMasterGroup();
		File origin = masterGroup.getOrigin();
		if (origin != null) {
			try {
				URI uri = new URI("clonk", new Path(origin.getParent()).append(getPath().toString()).toString(), null);
				return uri;
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		// remove sophisticatedness
		return obj == this;
	}
	
}
