package net.arctics.clonk.c4group;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.WeakHashMap;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents one file in a {@link C4Group} file.
 * @author ZokRadonh
 *
 */
public class C4GroupFile extends C4GroupItem implements Serializable {

	private static class EntryCache {
		private static class CachedEntry {
			public File file;
			public long creationTime;
			public CachedEntry(final File file, final long creationTime) {
				super();
				this.file = file;
				this.creationTime = creationTime;
			}
			public boolean modified() {
				return file.lastModified() != creationTime;
			}
		}
		private final Map<C4GroupFile, CachedEntry> files = new WeakHashMap<C4GroupFile, CachedEntry>();
		public File getCachedFile(final C4GroupFile groupEntry) throws IOException, CoreException {
			synchronized (files) {
				CachedEntry e = files.get(groupEntry);
				if (e == null || e.modified()) {
					final File f = File.createTempFile("c4dt", "c4groupcache"); //$NON-NLS-1$ //$NON-NLS-2$
					try (final FileOutputStream fileStream = new FileOutputStream(f)) {
						final byte[] contents = groupEntry.getContents();
						fileStream.write(contents);
					}
					f.deleteOnExit();
					e = new CachedEntry(f, f.lastModified());
					files.put(groupEntry, e);
				}
				return e.file;
			}
		}
	}
	private static final EntryCache CACHE = new EntryCache();
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private static final String[] NO_CHILDNAMES = new String[0];
	public static final int STORED_SIZE = 316;

	private final transient C4GroupEntryHeader header;
	private final transient C4Group parentGroup;
	private byte[] contents;

	public C4GroupFile(final C4Group parentGroup, final C4GroupEntryHeader header) {
		this.parentGroup = parentGroup;
		this.header = header;
	}

	@Override
	public void readIntoMemory(final boolean recursively, final C4GroupHeaderFilterBase filter, final InputStream stream) throws C4GroupInvalidDataException, IOException, CoreException {

		if ((filter.flagsForEntry(this) & C4GroupHeaderFilterBase.READINTOMEMORY) != 0)
			fetchContents(stream);
		else
			stream.skip(getSize());

		// process contents (contents could be null after this call)
		filter.processGroupItem(this);

	}

	private void fetchContents(final InputStream stream) {
		contents = new byte[getSize()];
		try {
			for (
				int readCount = 0;
				readCount != contents.length;
				readCount += stream.read(contents, readCount, contents.length - readCount)
			);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] getContents() {
		if (contents == null)
			try {
				parentGroup().readFromStream(this, parentGroup().baseOffset() + header.offset(), this::fetchContents);
				try {
					return contents;
				} finally {
					contents = null; // don't store
				}
			} catch (final IOException e) {
				e.printStackTrace();
				return null;
			}
		return contents;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		for (C4GroupItem item = this; item != null; item = item.parentGroup()) {
			builder.insert(0, item.getName());
			builder.insert(0, '/');
		}
		return builder.toString();
	}

	public String getEntryName() { return header.entryName(); }
	public boolean isPacked() { return header.isPacked(); }
	@Override
	public boolean hasChildren() { return false; }
	public int getSize() { return header.size(); }
	public int getEntrySize() { return header.entrySize(); }
	public int getOffset() { return header.offset(); }
	public int getTime() { return header.time(); }
	public boolean hasCRC() { return header.hasCRC(); }
	@Override
	public String getName() { return header.entryName(); }
	@Override
	public C4Group parentGroup() { return parentGroup; }
	public void setContents(final byte[] contents) { this.contents = contents; }
	/**
	 * Release the contents of this item to preserve memory
	 */
	@Override
	public void releaseData() { contents = null; }
	@Override
	public void extractToFileSystem(final IContainer parent) throws CoreException {
		extractToFileSystem(parent, null);
	}
	@Override
	public int computeSize() { return contents != null ? contents.length : header != null ? header.size() : 0; }
	@Override
	public C4GroupEntryHeader entryHeader() { return header; }
	@Override
	public IFileStore getChild(final String name) { return null; }
	@Override
	public IFileStore getParent() { return parentGroup(); }
	@Override
	public IPath path() { return ITreeNode.Default.path(this); }
	@Override
	public String nodeName() { return getName(); }
	@Override
	public String[] childNames(final int options, final IProgressMonitor monitor) throws CoreException { return NO_CHILDNAMES; }

	@Override
	public void extractToFileSystem(final IContainer parent, final IProgressMonitor monitor) throws CoreException {
		IFile me = null;
		if (parent instanceof IFolder)
			me = ((IFolder)parent).getFile(getName());
		else if (parent instanceof IProject)
			me = ((IProject)parent).getFile(getName());
		if (me != null) try {
			me.create(new ByteArrayInputStream(getContents()), IResource.NONE, monitor);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeTo(final OutputStream stream) throws IOException {
		try (final InputStream inStream = new ByteArrayInputStream(contents)) {
			final byte[] buffer = new byte[1024];
			int read = 0;
			try {
				while((read = inStream.read(buffer)) > 0)
					stream.write(buffer, 0, read);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class cls) {
		if (cls == C4GroupFile.class)
			return this;
		return null;
	}

	@Override
	public IFileInfo fetchInfo(final int options, final IProgressMonitor monitor) throws CoreException {
		final FileInfo fileInfo = new FileInfo(getName());
		fileInfo.setExists(true);
		fileInfo.setLastModified(parentGroup().lastModified());
		return fileInfo;
	}

	@Override
	public InputStream openInputStream(final int options, final IProgressMonitor monitor) throws CoreException {
		return new ByteArrayInputStream(getContents());
	}

	@Override
	public File toLocalFile(final int options, final IProgressMonitor monitor) throws CoreException {
		try {
			return CACHE.getCachedFile(this);
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
