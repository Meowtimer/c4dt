package net.arctics.clonk.c4group;

import static net.arctics.clonk.util.DispatchCase.caze;
import static net.arctics.clonk.util.Utilities.block;
import static net.arctics.clonk.util.Utilities.tri;
import static net.arctics.clonk.util.Utilities.walk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.DispatchCase;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.filesystem.EFS;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Represents a C4Group in memory.
 * @author ZokRadonh
 *
 */
public class C4Group extends C4GroupItem implements Serializable, ITreeNode {

	@FunctionalInterface
	public interface StreamReadCallback {
		public void readStream(InputStream stream);
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public void readFromStream(final C4GroupItem whoWantsThat, final long pos, final StreamReadCallback callback) throws IOException {
		parentGroup().readFromStream(whoWantsThat, pos, callback);
	}

	/**
	 * Causes the group to create its stream. If it already has been created it gets recreated
	 * @return the stream
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public InputStream requireStream() throws FileNotFoundException, IOException {
		return parentGroup().requireStream();
	}

	/**
	 * Causes the group to drop its stream.
	 * @throws IOException
	 */
	public void releaseStream() throws IOException {
		parentGroup().releaseStream();
	}

	private final String entryName;
	private List<C4GroupItem> childEntries;
	private boolean loaded;
	private boolean hasChildren;
	private C4GroupHeader header;
	private C4GroupEntryHeader entryHeader;
	private final C4Group parentGroup;
	private int offset;

	private long originModifiedAtLoadTime;
	private File origin;

	public int baseOffset() {
		final C4Group pg = parentGroup();
		return
			(pg != null ? pg.baseOffset() : 0) +
			offset + C4GroupHeader.STORED_SIZE + C4GroupEntryHeader.STORED_SIZE * header.getEntries();
	}

	public File origin() {
		return origin;
	}

	protected C4Group(final C4Group parent, final String name, final File file) {
		this.parentGroup = parent;
		this.entryName = name;
		this.origin = file;
		this.originModifiedAtLoadTime = file != null ? file.lastModified() : 0;
	}

	protected C4Group(final String name, final C4GroupHeader header) {
		this.entryName = name;
		this.childEntries = new LinkedList<C4GroupItem>();
		this.parentGroup = null;
		this.header = header;
	}

	public static C4Group openDirectory(final File file) {
		return new C4GroupTopLevelCompressed(file);
	}

	/**
	 * Open a C4Group file but do not parse it yet
	 * @param file the file to open
	 * @return the group created from the file
	 * @throws C4GroupInvalidDataException
	 * @throws IOException
	 */
	public static C4Group openFile(final File file) throws C4GroupInvalidDataException, IOException {
		final C4Group result = new C4GroupTopLevelCompressed(file.getName(), file);
		result.requireStream();
		return result;
	}

	public static C4Group openFile(final IFile file) throws IOException, CoreException {
		final C4Group result = new C4GroupTopLevelCompressed(file.getName(), new File(file.getFullPath().toOSString()));
		result.requireStream();
		return result;
	}

    public static void MemScramble(final byte[] buffer, final int size) {
        int cnt; byte temp;
        // XOR deface
        for (cnt = 0; cnt < size; cnt++)
            buffer[cnt] ^= 237;
        // BYTE swap
        for (cnt = 0; cnt + 2 < size; cnt += 3) {
            temp = buffer[cnt];
            buffer[cnt] = buffer[cnt + 2];
            buffer[cnt + 2] = temp;
        }
    }

	@Override
	public boolean hasChildren() {
		return hasChildren;
	}

    /**
     * Reads the C4Group (or parts of it based on the supplied filter) into memory
     * @param recursively whether to load sub groups recursively or only this group
     * @param filter the filter used for deciding which files are to be dropped
     * @param stream The stream to read the compressed data from
     * @throws C4GroupInvalidDataException
     * @throws IOException
     * @throws CoreException
     */
	@Override
	public void readIntoMemory(final boolean recursively, final C4GroupHeaderFilterBase filter, InputStream stream0) {

		final InputStream stream = stream0 != null ? stream0 : stream();
		final String[] files;

		if (!loaded) {
			loaded = true;
			// compressed
			childEntries =
				stream != null ? block(() -> {
					header = tri(() -> C4GroupHeader.createFromStream(stream), Exception.class, Exception::printStackTrace);
					if (header == null)
						return null;
					final List<Object> readObjects = IntStream.range(0,  header.getEntries())
						.mapToObj(i -> {
							final C4GroupEntryHeader entryHeader = tri(() -> C4GroupEntryHeader.createFromStream(stream), C4GroupInvalidDataException.class, Exception::printStackTrace);
							return !(filter.accepts(entryHeader, this) || entryHeader.isGroup())
								? entryHeader
								: block(() -> {
									final C4GroupItem entry = entryHeader.isGroup()
										? block(() -> {
											final C4Group group = new C4Group(this, entryHeader.entryName(), null); // a group does not need all header information
											group.offset = entryHeader.offset();
											return group;
										}) : new C4GroupFile(this, entryHeader);
									filter.notifyAboutCreatedItem(entryHeader, entry);
									return entry;
								});
						})
						.collect(Collectors.toList());
					// process group before processing child items
					filter.processGroupItem(this);
					// open (read into memory or process in a way defined by the filter) or skip
					return readObjects.stream().map(x -> DispatchCase.<Object, C4GroupItem>dispatch(
						x,
						caze(C4GroupEntryHeader.class, header -> {
							try {
								header.skipData(stream);
							} catch (final Exception e) {
								e.printStackTrace();
							}
							return null;
						}),
						caze(C4GroupItem.class, item -> {
							try {
								if (recursively || item instanceof C4GroupFile)
									item.readIntoMemory(true, filter, stream);
							} catch (final Exception e) {
								e.printStackTrace();
							}
							return item;
						})
					)).collect(Collectors.toList());
				}) :
				// uncompressed
				origin != null && (files = origin.list()) != null ? Arrays.stream(files).map(childFileName -> {
					final File child = new File(origin, childFileName);
					final C4GroupEntryHeader header = new C4GroupEntryHeader(child);
					return filter.accepts(header, this) ? block(() -> {
						final boolean isFile = !header.isGroup();
						final C4GroupItem childItem =
							isFile
							? new C4GroupFile(this, header)
						: child.isDirectory()
						? new C4GroupUncompressed(this, child.getName(), child)
							: new C4GroupTopLevelCompressed(this, child.getName(), child);
						final InputStream fstream = isFile ? tri(() -> new FileInputStream(child), IOException.class, Exception::printStackTrace) : stream;
						try {
							if (fstream != null && recursively || !(childItem instanceof C4Group))
								childItem.readIntoMemory(true, filter, fstream);
						} catch (final Exception e) {
							e.printStackTrace();
						}
						finally {
							if (isFile && fstream != null)
								try {
									fstream.close();
								} catch (final Exception e) {
									e.printStackTrace();
								}
						}
						return childItem;
					}) : null;
				}).filter(x -> x != null).collect(Collectors.toList())
				// wa?
				: null;
		}

	}

	/**
	 * Like {@link #readIntoMemory(boolean, C4GroupHeaderFilterBase, InputStream)}, but with the stream parameter set to {@link #stream()}.
	 * @throws C4GroupInvalidDataException
	 * @throws IOException
	 * @throws CoreException
	 */
	public final void readIntoMemory(final boolean recursively, final C4GroupHeaderFilterBase filter) throws C4GroupInvalidDataException, IOException, CoreException {
		readIntoMemory(recursively, filter, stream());
	}

	/**
	 * Extracts the group to the specified location in the project
	 */
	@Override
	public void extractToFileSystem(final IContainer parent) throws CoreException {
		extractToFileSystem(parent, null);
	}

	/**
	 * Extracts the group to the specified location in the project using a progress monitor
	 */
	@Override
	public void extractToFileSystem(final IContainer parent, final IProgressMonitor monitor) throws CoreException {
		IFolder me = null;
		if (parent instanceof IFolder)
			me = ((IFolder)parent).getFolder(getName());
		else if (parent instanceof IProject)
			me = ((IProject)parent).getFolder(getName());
		else
			throw new InvalidParameterException(Messages.NoImportingToWorkspaceRoot);
		me.create(IResource.NONE, true, monitor);
		for(final C4GroupItem item : childEntries) {
			if (monitor.isCanceled()) break;
			item.extractToFileSystem(me, monitor);
			monitor.worked(item.computeSize());
		}
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

	/**
	 * Returns the file name of the item
	 */
	@Override
	public String getName() {
		return entryName;
	}

	/**
	 * @return the stream the group is read from
	 */
	public InputStream stream() {
		return parentGroup().stream();
	}

	/**
	 * @return the childEntries
	 */
	public List<C4GroupItem> children() {
		if (childEntries == null)
			try {
				if (origin != null)
					originModifiedAtLoadTime = origin.lastModified();
				if (origin != null && origin.isDirectory())
					readIntoMemory(false, ACCEPT_EVERYTHING_DONTSTORECONTENTS, null);
				else
					readFromStream(this, (parentGroup() != null ? parentGroup().baseOffset() : 0) + offset, stream -> {
						try {
							readIntoMemory(false, ACCEPT_EVERYTHING_DONTSTORECONTENTS, stream);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					});
			} catch (final IOException e) {
				e.printStackTrace();
			}
		return childEntries;
	}

	/**
	 * @return the header
	 */
	public C4GroupHeader header() {
		return header;
	}

	@Override
	public C4Group parentGroup() {
		return parentGroup;
	}

	public C4GroupItem findChild(final String itemName) {
		return children().stream()
			.filter(item -> item.getName().equalsIgnoreCase(itemName))
			.findFirst().orElse(null);
	}

	public C4GroupItem findChild(final IPath path) {
		final C4GroupItem item = findChild(path.segment(0));
		return
			path.segmentCount() == 1 ? item :
			item instanceof C4Group ? ((C4Group)item).findChild(path.removeFirstSegments(1)) :
			null;
	}

	/**
	 * Close the stream the group is being read from
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (childEntries != null) {
			childEntries.clear();
			childEntries = null;
		}
		releaseStream();
	}

	@Override
	public int computeSize() {
		if (entryHeader == null) {
			int size = 0;
			for(final C4GroupItem item : childEntries)
				size += item.computeSize();
			return size;
		}
		else return entryHeader.size();
	}

	@Override
	public C4GroupEntryHeader entryHeader() {
		return entryHeader;
	}

	@Override
	public void writeTo(final OutputStream stream) throws FileNotFoundException {
		try {
			header.writeTo(stream); // group header
			for(final C4GroupItem item : childEntries)
				item.entryHeader().writeTo(stream);
			for(final C4GroupItem item : childEntries)
				item.writeTo(stream);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void releaseData() {
	}

	@Override
	public void addChild(final ITreeNode node) {
		// nope
	}

	@Override
	public Collection<? extends INode> childCollection() {
		return this.children();
	}

	@Override
	public ITreeNode parentNode() {
		return parentGroup();
	}

	@Override
	public IPath path() {
		return ITreeNode.Default.path(this);
	}

	@Override
	public boolean subNodeOf(final ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	@Override
	public String nodeName() {
		return getName();
	}

	public C4Group masterGroup() {
		return walk(this, C4Group::parentGroup).reduce((a, b) -> b).orElse(null);
	}

	@Override
	public IFileStore[] childStores(final int options, final IProgressMonitor monitor) throws CoreException {
		return childEntries == null ? new IFileStore[0] : childEntries.toArray(new IFileStore[childEntries.size()]);
	}

	@Override
	public String[] childNames(final int options, final IProgressMonitor monitor) throws CoreException {
		if (outdated()) {
			childEntries = null; // force refresh
			loaded = false;
		}
		final List<C4GroupItem> childEntries = this.children();
		final String[] result = new String[childEntries.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = childEntries.get(i).getName();
		return result;
	}

	@Override
	public IFileInfo fetchInfo(final int options, final IProgressMonitor monitor) throws CoreException {
		final FileInfo fileInfo = new FileInfo(getName());
		fileInfo.setExists(true);
		fileInfo.setAttribute(EFS.ATTRIBUTE_HIDDEN, false);
		fileInfo.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		fileInfo.setAttribute(EFS.ATTRIBUTE_ARCHIVE, false);
		fileInfo.setDirectory(true);
		return fileInfo;
	}

	@Override
	public IFileStore getChild(final String name) {
		return findChild(name);
	}

	@Override
	public IFileStore getParent() {
		return parentGroup();
	}

	@Override
	public InputStream openInputStream(final int options, final IProgressMonitor monitor) throws CoreException {
		throw new CoreException(new Status(1, Core.PLUGIN_ID, "FileStore stream cannot be opened on C4Group")); //$NON-NLS-1$
	}

	public long lastModified() {
		return origin != null ? origin.lastModified() : parentGroup() != null ? parentGroup().lastModified() : EFS.NONE;
	}

	@Override
	protected void copyDirectory(final IFileInfo sourceInfo, final IFileStore destination, final int options, final IProgressMonitor monitor) throws CoreException {

		final boolean needsToReleaseStream = stream() == null;
		try {
			if (needsToReleaseStream)
				requireStream();
		} catch (final IOException e) {
			e.printStackTrace();
			return;
		}

		try {

			try {
				IFileStore[] children = null;
				int opWork = 1;
				if ((options & EFS.SHALLOW) == 0) {
					children = childStores(EFS.NONE, null);
					opWork += children.length;
				}
				monitor.beginTask("", opWork); //$NON-NLS-1$
				monitor.subTask(Messages.C4Group_Copying);
				// create directory
				destination.mkdir(EFS.NONE, new SubProgressMonitor(monitor, 1));
				// don't copy attributes since that is stupid
				//transferAttributes(sourceInfo, destination);

				if (children == null)
					return;
				// copy children
				for (int i = 0; i < children.length; i++)
					children[i].copy(destination.getChild(children[i].getName()), options, new SubProgressMonitor(monitor, 1));
			} finally {
				monitor.done();
			}

		} finally {
			try {
				if (needsToReleaseStream)
					releaseStream();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Return whether the file this group was loaded from has been changed since reading its structure
	 * @return what he says
	 */
	public boolean outdated() {
		return origin != null ? origin.lastModified() != originModifiedAtLoadTime : parentGroup != null ? parentGroup.outdated() : false;
	}

	@Override
	public File toLocalFile(final int options, final IProgressMonitor monitor) throws CoreException {
		return origin;
	}

	/**
	 * Return whether the file this group originates from still exists in the filesystem
	 * @return true if it exists, otherwise false
	 */
	public boolean existsOnDisk() {
		// delegate to parent
		return parentGroup == null || parentGroup.existsOnDisk();
	}

}
