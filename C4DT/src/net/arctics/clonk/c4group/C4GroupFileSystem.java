package net.arctics.clonk.c4group;

import static net.arctics.clonk.util.Utilities.walk;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.Path;

/**
 * File system for read-only access to c4groups.
 * @author madeen
 *
 */
public class C4GroupFileSystem extends FileSystem {

	/**
	 * The scheme identifier of this file system.
	 */
	public static final String SCHEME = "c4group"; //$NON-NLS-1$

	/**
	 * Extensions for files to always load into memory so that subsequent seeking in the group file is not necessary.
	 */
	private static final List<String> EXTENSIONS_TO_ALWAYS_LOAD = Arrays.asList(
		".c", //$NON-NLS-1$
		".c4m" //$NON-NLS-1$
	);

	/**
	 * Names of files to always load into memory so that subsequent seeking in the group file is not necessary.
	 */
	private static final List<String> FILES_TO_ALWAYS_LOAD = Arrays.asList(
		"DefCore.txt", //$NON-NLS-1$
		"ActMap.txt", //$NON-NLS-1$
		"DescDE.txt", //$NON-NLS-1$
		"DescUS.txt", //$NON-NLS-1$
		"Names.txt", //$NON-NLS-1$
		"Particle.txt", //$NON-NLS-1$
		"StringTblDE.txt", //$NON-NLS-1$
		"StringTblUS.txt", //$NON-NLS-1$
		"Landscape.txt", //$NON-NLS-1$
		"Teams.txt" //$NON-NLS-1$
	);

	// there should be some function to do that somewhere -.-
	public static String replaceSpecialChars(final String path) {
		return path.replace("[", "%5B").replace("]", "%5D"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * Map to keep track of created C4Groups. {@link WeakReference}s are used as values so that the {@link C4Group} object can be purged if nothing apart
	 * from the file system is holding on to it.
	 */
	private final Map<File, WeakReference<C4Group>> rootGroups = new HashMap<File, WeakReference<C4Group>>();

	/**
	 * Global instance of the file system
	 */
	private static C4GroupFileSystem sharedInstance;

	/**
	 * Create a new C4GroupFileSystem. This will only work once, setting the variable backing {@link #instance()}.
	 * Subsequent calls will result in failed assertions.
	 * This constructor is declared as to allow the plugin.xml parsing machinery to call it.
	 */
	public C4GroupFileSystem() {
		super();
		synchronized (C4GroupFileSystem.class) {
			assert (sharedInstance == null);
			sharedInstance = this;
		}
	}

	/**
	 * Return the singleton instance of this file system. There still needs to be a corresponding configuration entry for the file system in the plugin.xml file
	 * or else this method will return null since the singleton was never created.
	 * @return The singleton
	 */
	public static C4GroupFileSystem instance() {
		return sharedInstance;
	}

	/**
	 * Delete the passed group from the internal state of the file system instance.
	 * Called when a linked resource in the workspace is about to be deleted.
	 * @param group The group to delete
	 */
	public void removeGroupFromRegistry(final C4Group group) {
		synchronized (rootGroups) {
			rootGroups.remove(group.origin());
		}
	}

	private void purgeDeadEntries() {
		synchronized (rootGroups) {
			List<File> markedForDeletion = null;
			for (final Map.Entry<File, WeakReference<C4Group>> entry : rootGroups.entrySet())
				if (entry.getValue().get() == null) {
					if (markedForDeletion == null)
						markedForDeletion = new LinkedList<File>();
					markedForDeletion.add(entry.getKey());
				}
			if (markedForDeletion != null)
				for (final File f : markedForDeletion)
					rootGroups.remove(f);
		}
	}

	private C4Group readRootGroup(File physical) {
		try {
			final C4Group group = C4Group.openFile(physical);
			try {
				group.readIntoMemory(true, new C4GroupHeaderFilterBase() {
					@Override
					public boolean accepts(final C4GroupEntryHeader header, final C4Group context) {
						return true;
					}
					@Override
					public int flagsForEntry(final C4GroupFile entry) {
						return
							EXTENSIONS_TO_ALWAYS_LOAD.stream().anyMatch(s -> entry.getName().endsWith(s)) ||
							FILES_TO_ALWAYS_LOAD.stream().anyMatch(s -> entry.getName().equalsIgnoreCase(s))
							? C4GroupHeaderFilterBase.READINTOMEMORY : 0;
					}
				});
				rootGroups.put(physical, new WeakReference<C4Group>(group));
				return group;
			} finally {
				group.releaseStream();
			}
		}
		catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public IFileStore getStore(final URI uri) {
		purgeDeadEntries();
		final String groupFilePath = uri.getSchemeSpecificPart();
		final File file = new File(groupFilePath);

		class GroupAndFile {
			public C4Group group;
			public File file;
			public GroupAndFile(C4Group group, File file) {
				super();
				this.group = group;
				this.file = file;
			}
		}
		final GroupAndFile group =
			walk(file, f -> f.getParentFile()).map(f -> {
				final WeakReference<C4Group> ref = rootGroups.get(f);
				final C4Group g = ref != null ? ref.get() : null;
				return g != null ? new GroupAndFile(g, f) : null;
			})
			.filter(g -> g != null).findFirst()
			.orElseGet(() -> {
				final File physical = walk(file, f -> f != null && !f.exists(), f -> f.getParentFile())
					.reduce((p, n) -> n)
					.orElse(file);
				if (physical != null && !physical.isDirectory())
					synchronized (rootGroups) {
						final WeakReference<C4Group> ref = rootGroups.get(physical);
						final C4Group existingRootGroup = ref != null ? ref.get() : null;
						final C4Group g = existingRootGroup != null ? existingRootGroup : readRootGroup(physical);
						return new GroupAndFile(g, physical);
					}
				else if (file.isDirectory())
					synchronized (rootGroups) {
						final C4Group g = new C4GroupUncompressed(null, file.getName(), file);
						rootGroups.put(file, new WeakReference<C4Group>(g));
						return new GroupAndFile(g, file);
					}
				else
					return null;
			});

		return
			group != null && group.file == file ? group.group :
			group != null ? group.group.findChild(new Path(file.getAbsolutePath().substring(group.file.getAbsolutePath().length()))) :
			invalidGroupFileStoreForFile(file);
	}

	private IFileStore invalidGroupFileStoreForFile(final File file) {
		return EFS.getNullFileSystem().getStore(new Path(file.getAbsolutePath()));
	}

}
