package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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
	private static final String[] EXTENSIONS_TO_ALWAYS_LOAD = new String[] {
		".c", //$NON-NLS-1$
		".c4m" //$NON-NLS-1$
	};

	/**
	 * Names of files to always load into memory so that subsequent seeking in the group file is not necessary.
	 */
	private static final String[] FILES_TO_ALWAYS_LOAD = new String[] {
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
	};

	// there should be some function to do that somewhere -.-
	public static String replaceSpecialChars(String path) {
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
	public void removeGroupFromRegistry(C4Group group) {
		rootGroups.remove(group.getOrigin());
	}

	private void purgeDeadEntries() {
		List<File> markedForDeletion = null;
		for (Map.Entry<File, WeakReference<C4Group>> entry : rootGroups.entrySet())
			if (entry.getValue().get() == null) {
				if (markedForDeletion == null)
					markedForDeletion = new LinkedList<File>();
				markedForDeletion.add(entry.getKey());
			}
		if (markedForDeletion != null)
			for (File f : markedForDeletion)
				rootGroups.remove(f);
	}

	@Override
	public IFileStore getStore(URI uri) {
		purgeDeadEntries();
		String groupFilePath = uri.getSchemeSpecificPart();
		File file = new File(groupFilePath);
		File groupFile = file;

		C4Group group = null;
		for (groupFile = file; groupFile != null; groupFile = groupFile.getParentFile()) {
			WeakReference<C4Group> ref;
			if ((ref = rootGroups.get(groupFile)) != null)
				if ((group = ref.get()) != null)
					break;
		}
		if (group == null) {
			for (groupFile = file; groupFile != null && !groupFile.exists(); groupFile = groupFile.getParentFile());
			if (groupFile != null && !groupFile.isDirectory()) {
				WeakReference<C4Group> ref = rootGroups.get(groupFile);
				group = ref != null ? ref.get() : null;
				if (group == null) {
					try {
						group = C4Group.openFile(groupFile);
						try {
							group.readIntoMemory(true, new C4GroupHeaderFilterBase() {

								@Override
								public boolean accepts(C4GroupEntryHeader header, C4Group context) {
									return true;
								}

								@Override
								public int flagsForEntry(C4GroupFile entry) {
									for (String s : EXTENSIONS_TO_ALWAYS_LOAD)
										if (entry.getName().endsWith(s))
											return 0;
									for (String s : FILES_TO_ALWAYS_LOAD)
										if (entry.getName().equalsIgnoreCase(s))
											return 0;
									return C4GroupHeaderFilterBase.DONTREADINTOMEMORY;
								}
							});
						} finally {
							group.releaseStream();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						return invalidGroupFileStoreForFile(file);
					}
					rootGroups.put(groupFile, new WeakReference<C4Group>(group));
				}
			}
			else {
				if (file.isDirectory()) {
					group = new C4GroupUncompressed(null, file.getName(), file);
					rootGroups.put(file, new WeakReference<C4Group>(group));
				}
			}
		}

		if (file == groupFile) {
			return group;
		}
		else if (group != null) {
			return group.findChild(new Path(file.getAbsolutePath().substring(groupFile.getAbsolutePath().length())));
		}
		else {
			return invalidGroupFileStoreForFile(file);
		}
	}

	private IFileStore invalidGroupFileStoreForFile(File file) {
		C4Group group = new C4GroupTopLevelCompressed(file.getName(), file);
		rootGroups.put(file, new WeakReference<C4Group>(group));
		return group;
	}

}
