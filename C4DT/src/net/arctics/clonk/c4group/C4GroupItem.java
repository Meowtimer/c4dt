package net.arctics.clonk.c4group;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import net.arctics.clonk.util.INodeWithPath;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

public abstract class C4GroupItem extends FileStore implements INodeWithPath {
	public static final C4GroupHeaderFilterBase ACCEPT_EVERYTHING = (header, context) -> true;
	public static final C4GroupHeaderFilterBase ACCEPT_EVERYTHING_DONTSTORECONTENTS = new C4GroupHeaderFilterBase() {
		@Override
		public boolean accepts(final C4GroupEntryHeader header, final C4Group context) { return true; };
		@Override
		public int flagsForEntry(final C4GroupFile entry) { return 0; };
	};

	/**
	 * Does this item have children?
	 * @return
	 */
	public abstract boolean hasChildren();

	/**
	 * Read this item
	 * @throws C4GroupInvalidDataException
	 * @throws IOException
	 * @throws CoreException
	 */
	public abstract void readIntoMemory(boolean recursively, C4GroupHeaderFilterBase filter, InputStream stream) throws C4GroupInvalidDataException, IOException, CoreException;

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
	@Override
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
	public abstract C4Group parentGroup();

	/**
	 * Returns the entry header
	 * @return
	 */
	public abstract C4GroupEntryHeader entryHeader();

	/**
	 * Extracts this file to disk
	 */
	public abstract void extractToFileSystem(IContainer internPath) throws CoreException;

	/**
	 * Extracts this file to disk with the given progress monitor
	 */
	public abstract void extractToFileSystem(IContainer internPath, IProgressMonitor monitor) throws CoreException;

	/**
	 * release data stored in memory to preserve space
	 */
	public abstract void releaseData();

	@Override
	public URI toURI() {
		final C4Group masterGroup = (this instanceof C4Group ? (C4Group)this : parentGroup()).masterGroup();
		final File origin = masterGroup.origin();
		if (origin != null)
			try {
				String path = new Path(origin.getParent()).append(path().toString()).toPortableString();
				if (!path.startsWith("/")) //$NON-NLS-1$
					path = "/"+path; //$NON-NLS-1$
				path = C4GroupFileSystem.replaceSpecialChars(path);
				final URI uri = new URI("c4group", path, null); //$NON-NLS-1$
				return uri;
			} catch (final URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		return null;
	}

	@Override
	public boolean equals(final Object obj) {
		// remove sophisticatedness
		return obj == this;
	}

	@Override
	public INodeWithPath parentNode() {
		return parentGroup();
	}

	public static C4GroupItem groupItemBackingResource(final IResource resource) {
		final URI uri = resource.getLocationURI();
		try {
			final IFileStore fileStore = EFS.getStore(uri);
			return Utilities.as(fileStore, C4GroupItem.class);
		} catch (final CoreException e) {
			return null;
		}
	}

}
