package net.arctics.clonk.index;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import net.arctics.clonk.resource.c4group.C4EntryHeader;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.C4GroupItem.IHeaderFilterCreationListener;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class C4GroupEntryStorage implements IStorage, Serializable {

	private static final long serialVersionUID = 1L;

	private final static class HeaderFilter implements IHeaderFilterCreationListener {
		
		public IPath itemPath;
		public int segmentIndex;
		public C4GroupItem item;
		private C4EntryHeader goldenHeader;

		@Override
		public boolean accepts(C4EntryHeader header, C4Group context) {
			if (item == context) {
				if (segmentIndex < itemPath.segmentCount() && header.getEntryName().equals(itemPath.segment(segmentIndex))) {
					segmentIndex++;
					goldenHeader = header;
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
		
		public C4GroupEntry goldenEntry() {
			return item instanceof C4GroupEntry && segmentIndex == itemPath.segmentCount() ? (C4GroupEntry)item : null;
		}

		@Override
		public void created(C4EntryHeader header, C4GroupItem item) {
			if (header == goldenHeader) {
				this.item = item;
			}
		}

	}

	private IContainedInExternalLib container;
	private String itemName;
	private transient ByteArrayInputStream cachedContents;
	
	public C4GroupEntryStorage(IContainedInExternalLib container, String itemName) {
		this.container = container;
		this.itemName = itemName;
	}
	
	public C4GroupEntryStorage(IContainedInExternalLib container, C4GroupEntry entry) {
		this (container, entry.getName());
		cachedContents = new ByteArrayInputStream(entry.getContentsAsArray());
	}
	
	@Override
	public InputStream getContents() throws CoreException {
		if (cachedContents != null) {
			cachedContents.reset();
			return cachedContents;
		} else try {
			C4Group group = C4Group.openFile(container.getExternalLib().getFile());
			HeaderFilter headerFilter = new HeaderFilter();
			headerFilter.itemPath = getFullPath();
			headerFilter.item = group;
			group.readIntoMemory(true, headerFilter);
			C4GroupEntry entry = headerFilter.goldenEntry();
			if (entry != null) {
				cachedContents = new ByteArrayInputStream(entry.getContentsAsArray());
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
		IPath containerPath = ((ITreeNode)container).getPath();
		int i;
		for (i = 0; i < containerPath.segmentCount(); i++) {
			if (containerPath.segment(i).equals(container.getExternalLib().getNodeName()))
				break;
		}
		return containerPath.removeFirstSegments(i+1).append(itemName);
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
