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
							group.readIntoMemory(false, new HeaderFilterBase() {

								@Override
								public boolean accepts(C4EntryHeader header, C4Group context) {
									return true;
								}

								@Override
								public int getFlags(C4GroupEntry entry) {
									if (!entry.getName().equals("Script.c"))
										return HeaderFilterBase.DONTREADINTOMEMORY;
									else
										return 0;
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
