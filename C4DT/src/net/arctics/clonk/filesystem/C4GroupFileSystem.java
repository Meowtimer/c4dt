package net.arctics.clonk.filesystem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.resource.c4group.C4EntryHeader;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4UncompressedGroup;
import net.arctics.clonk.resource.c4group.HeaderFilterBase;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.Path;

public class C4GroupFileSystem extends FileSystem {

	private static final String[] EXTENSIONS_TO_ALWAYS_LOAD = new String[] {
		".c", //$NON-NLS-1$
		".c4m" //$NON-NLS-1$
	};

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

	private Map<File, WeakReference<C4Group>> rootGroups = new HashMap<File, WeakReference<C4Group>>();

	private static C4GroupFileSystem sharedInstance;

	public C4GroupFileSystem() {
		super();
		synchronized (C4GroupFileSystem.class) {
			assert (sharedInstance == null);
			sharedInstance = this;
		}
	}

	public static C4GroupFileSystem getInstance() {
		return sharedInstance;
	}

	public void delete(C4Group group) {
		rootGroups.remove(group.getOrigin());
	}

	public void purgeDeadEntries() {
		List<File> markedForDeletion = null;
		for (Map.Entry<File, WeakReference<C4Group>> entry : rootGroups.entrySet()) {
			if (entry.getValue().get() == null) {
				if (markedForDeletion == null)
					markedForDeletion = new LinkedList<File>();
				markedForDeletion.add(entry.getKey());
			}
		}
		if (markedForDeletion != null) {
			for (File f : markedForDeletion)
				rootGroups.remove(f);
		}
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
							group.readIntoMemory(true, new HeaderFilterBase() {

								@Override
								public boolean accepts(C4EntryHeader header, C4Group context) {
									return true;
								}

								@Override
								public int getFlags(C4GroupEntry entry) {
									for (String s : EXTENSIONS_TO_ALWAYS_LOAD)
										if (entry.getName().endsWith(s))
											return 0;
									for (String s : FILES_TO_ALWAYS_LOAD)
										if (entry.getName().equalsIgnoreCase(s))
											return 0;
									return HeaderFilterBase.DONTREADINTOMEMORY;
								}
							});
						} finally {
							group.releaseStream();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						return null;
					}
					rootGroups.put(groupFile, new WeakReference<C4Group>(group));
				}
			}
			else {
				if (file.isDirectory()) {
					group = new C4UncompressedGroup(null, file.getName(), file);
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
			return null;
		}
	}

}
