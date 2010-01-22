package net.arctics.clonk.filesystem;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.resource.c4group.C4EntryHeader;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.HeaderFilterBase;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.Path;

public class C4GroupFileSystem extends FileSystem {

	private Map<File, C4Group> rootGroups = new HashMap<File, C4Group>();

	@Override
	public IFileStore getStore(URI uri) {
		String groupFilePath = uri.getPath();
		File file = new File(groupFilePath);
		File groupFile = file;
		while (groupFile != null && !groupFile.exists())
			groupFile = groupFile.getParentFile();
		if (groupFile != null && !groupFile.isDirectory()) {
			C4Group group = rootGroups.get(groupFile);
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
							public int getFlags() {
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
				rootGroups.put(groupFile, group);
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
		else
			return null;
	}

}
