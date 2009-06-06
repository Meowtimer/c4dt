package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents 
 * @author ZokRadonh
 *
 */
public class C4Group implements C4GroupItem, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum C4GroupType {
		OtherGroup,
		DefinitionGroup,
		ResourceGroup,
		ScenarioGroup,
		FolderGroup
	}
	
	private static Map<String, C4GroupType> getExtensionToGroupTypeMap() {
		Map<String, C4GroupType> result = new HashMap<String, C4GroupType>(C4GroupType.values().length);
		result.put("c4d", C4GroupType.DefinitionGroup);
		result.put("c4g", C4GroupType.ResourceGroup);
		result.put("c4s", C4GroupType.ScenarioGroup);
		result.put("c4f", C4GroupType.FolderGroup);
		return Collections.unmodifiableMap(result);
	}
	
	public static final Map<String, C4GroupType> extensionToGroupTypeMap = getExtensionToGroupTypeMap();

	private String entryName;
	private List<C4GroupItem> childEntries;
	private transient InputStream stream;
	private boolean completed;
	private boolean hasChildren;
	private C4GroupHeader header;
	private C4EntryHeader entryHeader;
	private C4Group parentGroup;
	private int sizeOfChildren;
	
	/**
	 * Constructor for root group
	 * @param stream
	 */
	protected C4Group(InputStream stream, String name) {
		this.stream = stream;
		parentGroup = null;
		entryName = name;
	}
	
	/**
	 * Constructor for nested groups
	 * @param parent
	 */
	protected C4Group(C4Group parent, String name) {
		parentGroup = parent;
		stream = parent.getStream();
		entryName = name;
	}
	
	protected C4Group(String name, C4GroupHeader header) {
		entryName = name;
		childEntries = new LinkedList<C4GroupItem>();
		this.header = header;
	}
	
	protected static C4Group makeGroup(IFolder folder) {
		try {
			String groupName = folder.getName();
			IResource[] children = folder.members();
			int entryCount = children.length;
			C4Group group = new C4Group(groupName, C4GroupHeader.createHeader(entryCount, "Eclipse C4Scripter"));// TODO implement new project option for "maker" string
			int groupSize = 0;
			for (int i = 0; i < entryCount;i++) {
				String entryName = children[i].getName();
				if (children[i] instanceof IFolder) {
					C4Group subGroup = makeGroup((IFolder)children[i]);
					subGroup.entryHeader.setOffset(groupSize);
					group.getChildEntries().add(subGroup);
				}
				else if (children[i] instanceof IFile) {
					File file = new File(((IFile)children[i]).getRawLocationURI());
					int size = (int)file.length();
					C4GroupEntry entry = C4GroupEntry.makeEntry(group, C4EntryHeader.createHeader(entryName, false, false, size, 0, 0, (int) GregorianCalendar.getInstance().getTimeInMillis()),file);
					entry.getEntryHeader().setOffset(groupSize);
					group.getChildEntries().add(entry);
				}
				else {
					System.out.println("Don't know how to treat resource " + children[i].getName() + " of class " + children[i].getClass().getName());
					continue;
				}
				groupSize += ((java.util.LinkedList<C4GroupItem>)group.getChildEntries()).getLast().computeSize();
			}
			groupSize += 204; // group header
			groupSize += 316 * entryCount; // entry headers
			group.entryHeader = C4EntryHeader.createHeader(groupName, false, true, groupSize, 0, 0, (int) GregorianCalendar.getInstance().getTimeInMillis());
			return group;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Open a C4Group file but do not parse it yet
	 * @param file
	 * @return
	 * @throws InvalidDataException
	 * @throws IOException 
	 */
	public static C4Group openFile(File file) throws InvalidDataException, IOException {
		return new C4Group(new GZIPInputStream(new FileInputStream(file) {
			private int timesRead = 0;

			/* (non-Javadoc)
			 * @see java.io.FileInputStream#read()
			 */
			@Override
			public int read() throws IOException {
				if (timesRead < 2) { // deface magic header
					timesRead++;
					int readByte = super.read();
					if (readByte == 0x1E) return 0x1F;
					if (readByte == 0x8C) return 0x8B;
					return readByte;
				}
				return super.read();
			}
		}),file.getName());

	}
	
    public static void MemScramble(byte[] buffer, int size)
    {
        int cnt; byte temp;
        // XOR deface
        for (cnt = 0; cnt < size; cnt++)
            buffer[cnt] ^= 237;
        // BYTE swap
        for (cnt = 0; cnt + 2 < size; cnt += 3)
        {
            temp = buffer[cnt];
            buffer[cnt] = buffer[cnt + 2];
            buffer[cnt + 2] = temp;
        }
    }

	public boolean hasChildren() {
		return hasChildren;
	}
	
	public static C4GroupType getGroupTypeExt(String ext) {
		C4GroupType result = extensionToGroupTypeMap.get(ext);
		if (result != null)
			return result;
		return C4GroupType.OtherGroup;		
	}
	
	public static C4GroupType getGroupType(String groupName) {
		return getGroupTypeExt(groupName.substring(groupName.lastIndexOf(".") + 1));
	}
	
	public C4GroupType getGroupType() {
		return getGroupType(getName());
	}
	
    /**
     * Fetches and parses the index of a container file
     * @param stream
     * @throws InvalidDataException
     * @throws IOException 
     * @throws CoreException
     */
	public void open(boolean recursively, IHeaderFilter filter) throws InvalidDataException, IOException, CoreException {
		
		if (parentGroup != null && parentGroup.getChildEntries().get(0) != this) {
			C4GroupItem predecessor = parentGroup.getChildEntries().get(parentGroup.getChildEntries().indexOf(this) - 1);
			predecessor.open(true, filter);
		}
		
		if (!completed) {
			
			completed = true;
			header = C4GroupHeader.createFromStream(stream);
			
			childEntries = new ArrayList<C4GroupItem>(header.getEntries());
			List<Object> readObjects = new LinkedList<Object>();
			
			// populate readObjects with either C4GroupHeader (meaning the file this header describes is to be skipped) or C4GroupItem (meaning this item is to be added to child list of the calling group)
			sizeOfChildren = 0;
			for(int i = 0; i < header.getEntries(); i++) {
				hasChildren = true;
				C4EntryHeader entryHeader = C4EntryHeader.createFromStream(stream);
				sizeOfChildren += entryHeader.getSize();
				if (!filter.accepts(entryHeader, this)) {
					// FIXME: skipping groups will not work at this time
					readObjects.add(entryHeader);
				}
				else {
					C4GroupItem entry;
					if (entryHeader.isGroup()) {
						entry = new C4Group(this, entryHeader.getEntryName()); // a group does not need all header information
					}
					else {
						entry = new C4GroupEntry(this, entryHeader);
					}
					readObjects.add(entry);
				}
			}
			
			// process group before processing child items
			filter.processData(this);
			
			if (recursively) {
				// open (read into memory or process in a way defined by the filter) or skip 
				for (Object o : readObjects) {
					if (o instanceof C4EntryHeader) {
						((C4EntryHeader)o).skipData(stream);
					}
					else if (o instanceof C4GroupItem) {
						C4GroupItem item = (C4GroupItem) o;
						childEntries.add(item);
						item.open(true, filter);
					}
				}
			}
			else {
				for (Object o : readObjects)
					if (o instanceof C4GroupItem)
						childEntries.add((C4GroupItem) o);
			}
			
		}
	}
	
	public void open(boolean recursively) throws InvalidDataException, IOException, CoreException {
		open(recursively, IHeaderFilter.ACCEPT_EVERYTHING);
	}

	/**
	 * Extracts the group to the specified location in the project 
	 */	
	public void extractToFilesystem(IContainer parent) throws CoreException {
		extractToFilesystem(parent, null);
	}
	
	/**
	 * Extracts the group to the specified location in the project using a progress monitor
	 */	
	public void extractToFilesystem(IContainer parent, IProgressMonitor monitor) throws CoreException {
		IFolder me = null;
		if (parent instanceof IFolder) {
			me = ((IFolder)parent).getFolder(getName());
		}
		else if (parent instanceof IProject) {
			me = ((IProject)parent).getFolder(getName());
		}
		else {
			throw new InvalidParameterException("You cannot import files to workspace root!");
		}
		me.create(IResource.NONE, true, monitor);
		for(C4GroupItem item : childEntries) {
			if (monitor.isCanceled()) break;
			item.extractToFilesystem(me, monitor);
			monitor.worked(item.computeSize());
		}
	}
	
	public String toString() {
		return entryName;
	}
	
	/**
	 * Returns the file name of the item
	 */
	public String getName() {
		return entryName;
	}

	public boolean isCompleted() {
		return completed;
	}
	
	/**
	 * @return the stream the group is read from
	 */
	public InputStream getStream() {
		return stream;
	}
	
	/**
	 * @return the childEntries
	 */
	public List<C4GroupItem> getChildEntries() {
		return childEntries;
	}

	/**
	 * @return the header
	 */
	public C4GroupHeader getHeader() {
		return header;
	}

	public C4Group getParentGroup() {
		return parentGroup;
	}
	
	/**
	 * Close the stream the group is being read from
	 */
	public void close() {
		if (childEntries != null) {
			childEntries.clear();
			childEntries = null;
		}
		try {
			// only close stream if this group owns it
			if (parentGroup == null && stream != null)
				stream.close();
			stream = null; // discard stream
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int computeSize() {
		if (entryHeader == null) {
			int size = 0;
			for(C4GroupItem item : childEntries) {
				size += item.computeSize();
			}
			return size;
		}
		else return entryHeader.getSize();
	}

	public C4EntryHeader getEntryHeader() {
		return entryHeader;
	}

	public void writeTo(OutputStream stream) throws FileNotFoundException {
		try {
			header.writeTo(stream); // group header
			for(C4GroupItem item : childEntries) { // write index
				item.getEntryHeader().writeTo(stream); 
			}
			for(C4GroupItem item : childEntries) { // write data
				item.writeTo(stream);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void accept(IC4GroupVisitor visitor) {
		accept(visitor,getGroupType(), null);
	}
	
	public void accept(IC4GroupVisitor visitor, C4GroupType type) {
		accept(visitor,type, null);
	}
	
	public void accept(IC4GroupVisitor visitor, C4GroupType type, IProgressMonitor monitor) {
		if (visitor.visit(this, type)) {
			for(C4GroupItem item : childEntries) {
				if (monitor != null)
					monitor.worked(1);
				item.accept(visitor, type, monitor);
			}
			visitor.groupFinished(this);
		}
	}

	public C4GroupItem findEntry(String entryName) {
		for (C4GroupItem entry : getChildEntries()) {
			if (entry.getName().equalsIgnoreCase(entryName)) {
				return entry;
			}
		}
		return null;
	}
	
	public void releaseData() {
	}

	public int getSizeOfChildren() {
		return sizeOfChildren;
	}
	
}
