package net.arctics.clonk.resource.c4group;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.resource.c4group.C4Group.StreamReadCallback;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents one entry in a C4Group file.
 * @author ZokRadonh
 *
 */
public class C4GroupEntry extends C4GroupItem implements IStorage, Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String[] NO_CHILDNAMES = new String[0];
	
	public static final int STORED_SIZE = 316;
	
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
    
	public void readIntoMemory(boolean recursively, HeaderFilterBase filter, InputStream stream) throws InvalidDataException, IOException, CoreException {
		if (completed) return;
		completed = true;
		
		/*if (parentGroup.getChildren().get(0) != this) {
			C4GroupItem predecessor = parentGroup.getChildren().get(parentGroup.getChildren().indexOf(this) - 1);
			predecessor.readIntoMemory(true, filter);
		}*/

		if ((filter.getFlags(this) & HeaderFilterBase.DONTREADINTOMEMORY) == 0) {
			fetchContents(stream);
		}
		else {
			stream.skip(getSize());
		}
		
		// process contents (contents could be null after this call)
		filter.processData(this);
    	
	}

	private void fetchContents(InputStream stream) {
		//System.out.println("Fetching contents of " + this);
		contents = new byte[getSize()];
		try {
			for (
				int readCount = 0;
				readCount != contents.length;
				readCount += stream.read(contents, readCount, contents.length - readCount)
			);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public InputStream getContents() throws CoreException {
		if (contents == null) {
			try {
				getParentGroup().readFromStream(this, getParentGroup().baseOffset() + header.getOffset(), new StreamReadCallback() {
					@Override
					public void readStream(InputStream stream) {
						fetchContents(stream);
					}
				});
				try {
					return new ByteArrayInputStream(getContentsAsArray());
				} finally {
					contents = null; // don't store
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return new ByteArrayInputStream(contents);
	}

    public String toString() {
    	StringBuilder builder = new StringBuilder();
    	for (C4GroupItem item = this; item != null; item = item.getParentGroup()) {
    		builder.insert(0, item.getName());
    		builder.insert(0, '/');
    	}
        return builder.toString();
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
	
	/**
	 * Return the contents of this entry as a string
	 * @return the contents of this entry as string
	 */
	public String getContentsAsString() {
		return new String(getContentsAsArray());
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
	
	/**
	 * Release the contents of this item to preserve memory
	 */
	public void releaseData() {
		setContents(null);
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

	public IPath getFullPath() {
		return ITreeNode.Default.getPath(this);
	}
	
	@Override
	public IPath getPath() {
		return getFullPath();
	}

	public boolean isReadOnly() {
		return true;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class cls) {
		if (cls == C4GroupEntry.class)
			return this;
		return null;
	}

	public void accept(IC4GroupVisitor visitor, C4GroupType type, IProgressMonitor monitor) {
		visitor.visit(this, type);
	}

	@Override
	public String getNodeName() {
		return getName();
	}
	
	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		return NO_CHILDNAMES;
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		FileInfo fileInfo = new FileInfo(getName());
		fileInfo.setExists(true);
		fileInfo.setLastModified(getParentGroup().lastModified());
		return fileInfo;
	}

	@Override
	public IFileStore getChild(String name) {
		return null; // go away :c
	}

	@Override
	public IFileStore getParent() {
		return getParentGroup();
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		return getContents();
	}
	
}
