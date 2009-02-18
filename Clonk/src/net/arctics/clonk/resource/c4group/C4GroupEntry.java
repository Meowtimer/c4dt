package net.arctics.clonk.resource.c4group;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;

import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * Represents one entry in a C4Group file.
 * @author ZokRadonh
 *
 */
public class C4GroupEntry implements C4GroupItem, IStorage, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private transient C4EntryHeader header;
    private transient C4Group parentGroup;
    private transient boolean completed;
	private byte[] contents;
	
	private transient File exportFromFile;
    
    public C4GroupEntry(C4Group parentGroup, C4EntryHeader header)
    {
        this.parentGroup = parentGroup;
        this.header = header;
    }
    
    protected C4GroupEntry() {
    	completed = true;
    }
    
    public static C4GroupEntry makeEntry(C4Group parent, C4EntryHeader header, File exportFromFile) {
    	C4GroupEntry entry = new C4GroupEntry();
    	entry.parentGroup = parent;
    	entry.header = header;
    	entry.contents = null;
    	entry.exportFromFile = exportFromFile;
    	return entry;
    }
    
	public void open(boolean recursively, IHeaderFilter filter) throws InvalidDataException, IOException {
		if (completed) return;
		
		if (parentGroup.getChildEntries().get(0) != this) {
			C4GroupItem predecessor = parentGroup.getChildEntries().get(parentGroup.getChildEntries().indexOf(this) - 1);
			predecessor.open(true, filter);
		}
    	
    	completed = true;
    	
    	// fetch contents
    	contents = new byte[getSize()];
    	try {
    		InputStream stream = parentGroup.getStream();
			int readCount = stream.read(contents);
			while (readCount != contents.length) {
				readCount += stream.read(contents,readCount,contents.length - readCount);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
	}

    public String toString()
    {
        return getEntryName();
    }
    
    /**
	 * @return the entryName
	 */
	public String getEntryName() {
		return header.getEntryName();
	}

	/**
	 * @return the packed
	 */
	public boolean isPacked() {
		return header.isPacked();
	}

	/**
	 * @return the hasChildren
	 */
	public boolean hasChildren() {
		return false;
	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return header.getSize();
	}

	/**
	 * @return the entrySize
	 */
	public int getEntrySize() {
		return header.getEntrySize();
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return header.getOffset();
	}

	/**
	 * @return the time
	 */
	public int getTime() {
		return header.getTime();
	}

	/**
	 * @return the hasCRC
	 */
	public boolean hasCRC() {
		return header.hasCRC();
	}

	/**
	 * @return the completed
	 */
	public boolean isCompleted() {
		return completed;
	}

	/**
	 * @return the crc
	 */
	public int getCrc() {
		return header.getCrc();
	}

	/**
	 * @return the contents
	 */
	public byte[] getContentsAsArray() {
		return contents;
	}

	public String getName() {
		return header.getEntryName();
	}

	public C4Group getParentGroup() {
		return parentGroup;
	}

	/**
	 * @param contents the contents to set
	 */
	public void setContents(byte[] contents) {
		this.contents = contents;
	}

	public void extractToFilesystem(IContainer parent) throws CoreException {
		extractToFilesystem(parent, null);
	}
	
	public void extractToFilesystem(IContainer parent, IProgressMonitor monitor) throws CoreException {
		IFile me = null;
		if (parent instanceof IFolder) {
			me = ((IFolder)parent).getFile(getName());
		}
		else if (parent instanceof IProject) {
			me = ((IProject)parent).getFile(getName());
		}
		if (me != null) try {
			me.create(new java.io.ByteArrayInputStream(contents), IResource.NONE, monitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public int computeSize() {
		if (completed) return contents.length;
		else return header.getSize();
	}

	public C4EntryHeader getEntryHeader() {
		return header;
	}

	public void writeTo(OutputStream stream) throws IOException {
		InputStream inStream = new java.io.FileInputStream(exportFromFile);
		try {
			byte[] buffer = new byte[1024];
			int read = 0;
			try {
				while((read = inStream.read(buffer)) > 0) {
					stream.write(buffer, 0, read);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} finally {
			inStream.close();
		}
	}

	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(getContentsAsArray());
	}

	public IPath getFullPath() {
		LinkedList<String> pathSegments = new LinkedList<String>();
		for (C4GroupItem item = this; item != null; item = item.getParentGroup()) {
			pathSegments.addFirst(item.getName());
		}
		StringBuilder pathBuilder = new StringBuilder();
		for (String pathSegment : pathSegments) {
			if (pathBuilder.length() > 0)
				pathBuilder.append('/');
			pathBuilder.append(pathSegment);
		}
		return new Path(pathBuilder.toString());
	}

	public boolean isReadOnly() {
		return true;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class cls) {
		if (cls == C4GroupEntry.class)
			return this;
		return null;
	}

	public void accept(IC4GroupVisitor visitor, C4GroupType type,
			IProgressMonitor monitor) {
		visitor.visit(this, type);
	}
}
