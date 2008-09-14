package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents one entry in a C4Group file.
 * @author ZokRadonh
 *
 */
public class C4Entry implements C4GroupItem {
	
	private C4EntryHeader header;
    private C4Group parentGroup;
    private boolean completed;
	private byte[] contents;
	
	private File exportFromFile;
	
	private InputStream stream;
    
    public C4Entry(C4Group parentGroup, C4EntryHeader header)
    {
        this.parentGroup = parentGroup;
        stream = parentGroup.getStream();
        this.header = header;
    }
    
    protected C4Entry() {
    }
    
    public static C4Entry makeEntry(C4Group parent, C4EntryHeader header, File exportFromFile) {
    	C4Entry entry = new C4Entry();
    	entry.parentGroup = parent;
    	entry.header = header;
    	entry.contents = null;
    	entry.exportFromFile = exportFromFile;
    	return entry;
    }
    
	@Override
	public void open(boolean recursively) throws InvalidDataException {
		if (completed) return;
		
		if (parentGroup.getChildEntries().get(0) != this) {
			C4GroupItem predecessor = parentGroup.getChildEntries().get(parentGroup.getChildEntries().indexOf(this) - 1);
			predecessor.open(true);
		}
    	
    	completed = true;
    	
    	// fetch contents
    	contents = new byte[getSize()];
    	try {
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
	public Calendar getTime() {
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
	 * @return the stream
	 */
	public InputStream getStream() {
		return stream;
	}

	/**
	 * @return the contents
	 */
	public byte[] getContents() {
		return contents;
	}

	@Override
	public String getName() {
		return header.getEntryName();
	}

	@Override
	public C4Group getParentGroup() {
		return parentGroup;
	}

	/**
	 * @param contents the contents to set
	 */
	public void setContents(byte[] contents) {
		this.contents = contents;
	}

	@Override
	public void extractToFilesystem(IContainer parent) throws CoreException {
		extractToFilesystem(parent, null);
	}
	
	public void extractToFilesystem(IContainer parent, IProgressMonitor monitor) throws CoreException {
		if (parent instanceof IFolder) {
			IFile me = ((IFolder)parent).getFile(getName());
			me.create(new java.io.ByteArrayInputStream(contents), IResource.NONE, monitor);
		}
		else {
			IFile me = ((IProject)parent).getFile(getName());
			me.create(new java.io.ByteArrayInputStream(contents), IResource.NONE, monitor);
		}
	}

	@Override
	public int computeSize() {
		if (completed) return contents.length;
		else return header.getSize();
	}

	@Override
	public C4EntryHeader getEntryHeader() {
		return header;
	}

	@Override
	public void writeTo(OutputStream stream) throws FileNotFoundException {
		InputStream inStream = new java.io.FileInputStream(exportFromFile);
		byte[] buffer = new byte[1024];
		int read = 0;
		try {
			while((read = inStream.read(buffer)) > 0) {
				stream.write(buffer, 0, read);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
