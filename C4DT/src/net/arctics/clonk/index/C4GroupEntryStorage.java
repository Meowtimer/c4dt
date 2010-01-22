package net.arctics.clonk.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import net.arctics.clonk.resource.c4group.C4EntryHeader;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.HeaderFilterBase;
import net.arctics.clonk.resource.c4group.InvalidDataException;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class C4GroupEntryStorage implements IStorage, Serializable {

	private static final long serialVersionUID = 1L;

	private final static class HeaderFilter extends HeaderFilterBase {
		
		public IPath itemPath;
		public int segmentIndex;
		public C4GroupItem item;
		public String[] acceptedEntries;
		
		private C4EntryHeader goldenHeader;

		@Override
		public boolean accepts(C4EntryHeader header, C4Group context) {
			if (item == context) {
				if (segmentIndex < itemPath.segmentCount() && header.getEntryName().equals(itemPath.segment(segmentIndex))) {
					segmentIndex++;
					goldenHeader = header;
					return true;
				}
				else if (acceptedEntries != null) {
					for (String e : acceptedEntries)
						if (header.getEntryName().equals(e))
							return true;
				}
			}
			return false;
		}

		@Override
		public void processData(C4GroupItem item) throws CoreException {
			/*if (item == this.item && segmentIndex == itemPath.segmentCount() && item instanceof C4GroupEntry) {
				C4GroupEntry entry = (C4GroupEntry) item;
				resultantInputStream = entry.getContents();
			}*/
		}
		
		public C4Group goldenGroup() {
			return item instanceof C4Group ? (C4Group)goldenItem() : null;
		}
		
		public C4GroupItem goldenItem() {
			return segmentIndex == itemPath.segmentCount() ? item : null;
		}

		@Override
		public void created(C4EntryHeader header, C4GroupItem item) {
			if (header == goldenHeader) {
				this.item = item;
			}
		}

	}

	private Object container;
	private String itemName;
	private transient ByteArrayInputStream cachedContents;
	
	public void setSelectedGroup(C4Group selectedGroup) {
		this.container= selectedGroup;
	}
	
	public static C4Group selectGroup(IContainedInExternalLib container, String... acceptedEntries) throws InvalidDataException, IOException, CoreException {
		C4Group group = C4Group.openFile(container.getExternalLib().getFile());
		HeaderFilter headerFilter = new HeaderFilter();
		headerFilter.itemPath = pathWithoutProjectSegment(container);
		headerFilter.item = group;
		headerFilter.acceptedEntries = acceptedEntries;
		group.readIntoMemory(true, headerFilter);
		return headerFilter.goldenGroup();
	}
	
	private static IPath pathWithoutProjectSegment(IContainedInExternalLib container) {
		IPath containerPath = ((ITreeNode)container).getPath();
		int i;
		for (i = 0; i < containerPath.segmentCount(); i++) {
			if (containerPath.segment(i).equals(container.getExternalLib().getNodeName()))
				break;
		}
		return containerPath.removeFirstSegments(i+1);
	}

	public C4GroupEntryStorage(Object container, String itemName) {
		this.container = container;
		this.itemName = itemName;
	}
	
	public C4GroupEntryStorage(Object container, C4GroupEntry entry) {
		this (container, entry.getName());
		cachedContents = new ByteArrayInputStream(entry.getContentsAsArray());
	}
	
	@Override
	public InputStream getContents() throws CoreException {
		if (cachedContents != null) {
			cachedContents.reset();
			return cachedContents;
		} else try {
			C4Group group;
			if (container instanceof IContainedInExternalLib) {
				group = selectGroup((IContainedInExternalLib) container, itemName);
			} else if (container instanceof C4Group) {
				group = (C4Group) container;
			} else
				return null;
			C4GroupItem entry = group.findChild(itemName);
			if (entry instanceof C4GroupEntry) {
				cachedContents = new ByteArrayInputStream(((C4GroupEntry) entry).getContentsAsArray());
				return cachedContents;
			}
			else
				return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public IPath getFullPath() {
		return pathWithoutProjectSegment((IContainedInExternalLib) container).append(itemName);
	}

	@Override
	public String getName() {
		return itemName;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

}
