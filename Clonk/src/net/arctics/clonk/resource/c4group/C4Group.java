package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
		return result;
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
	private static final Pattern fileNameSwapPattern = Pattern.compile("\\Ac4\\w\\..+");
	
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
	
	protected static String swapExtension(String projectString) {
		return projectString.substring(projectString.indexOf('.') + 1) + "." + projectString.substring(0,projectString.indexOf('.'));
	}
	
	public static boolean createFile(IFolder folder, File targetDir) throws InvalidDataException, FileNotFoundException {
		if (!targetDir.isDirectory()) {
			throw new InvalidDataException("targetDir ist not a directory! (" + targetDir.getAbsolutePath() + ")");
		}
		try {
			File target = new File(targetDir,swapExtension(folder.getName()));
			if (target.exists()) target.delete();
			FileOutputStream stream = new FileOutputStream(target); // TODO recognize not swapped folder name
			C4Group root = makeGroup(folder);
			GZIPOutputStream zipper = new GZIPOutputStream(stream);
			root.writeTo(zipper);
			zipper.close();
//			root.writeTo(stream);
			stream.close();
			RandomAccessFile clonker = new RandomAccessFile(target,"rw");
			clonker.write(new byte[] { 0x1E, (byte) 0x8C },0,2);
			clonker.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private static boolean hasToBeSwapped(String file) {
		return fileNameSwapPattern.matcher(file).matches();
	}
	
	protected static C4Group makeGroup(IFolder folder) {
		try {
			String groupName = folder.getName();
			if (hasToBeSwapped(groupName)) groupName = swapExtension(groupName);
			IResource[] children = folder.members();
			int entryCount = children.length;
			C4Group group = new C4Group(groupName, C4GroupHeader.createHeader(entryCount, "Eclipse C4Scripter"));// TODO implement new project option for "maker" string
			int groupSize = 0;
//			C4EntryHeader[] headers = new C4EntryHeader[entryCount];
			for (int i = 0; i < entryCount;i++) {
				String entryName = children[i].getName();
				if (hasToBeSwapped(entryName)) entryName = swapExtension(entryName);
				if (children[i] instanceof IFolder) {
					C4Group subGroup = makeGroup((IFolder)children[i]);
					subGroup.entryHeader.setOffset(groupSize);
					group.getChildEntries().add(subGroup);
					
//					makeGroup(children[i], stream);
//					headers[i] = C4EntryHeader.createHeader(entryName, false, true, children[i]., entrySize, offset, time)
				}
				else if (children[i] instanceof IFile) {
					File file = new File(((IFile)children[i]).getRawLocationURI());
					int size = (int)file.length();
					C4Entry entry = C4Entry.makeEntry(group, C4EntryHeader.createHeader(entryName, false, false, size, 0, 0, GregorianCalendar.getInstance()),file);
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
			group.entryHeader = C4EntryHeader.createHeader(groupName, false, true, groupSize, 0, 0, GregorianCalendar.getInstance());
			return group;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Open a C4Group file but not parse it yet
	 * @param file
	 * @return
	 * @throws InvalidDataException
	 * @throws FileNotFoundException
	 */
	public static C4Group OpenFile(File file) throws InvalidDataException, FileNotFoundException {
		try {
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
					}
					return super.read();
				}
			}),file.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
	
	public static C4GroupType getGroupType(String groupName) {
		String ext = groupName.substring(groupName.lastIndexOf(".") + 1);
		C4GroupType result = extensionToGroupTypeMap.get(ext);
		if (result != null)
			return result;
		return C4GroupType.OtherGroup;
	}
	
	public C4GroupType getGroupType() {
		return getGroupType(getName());
	}
	
    /**
     * Fetches and parses the index of a container file
     * @param stream
     * @throws InvalidDataException
     */
	public void open(boolean recursively) throws InvalidDataException {
		
		if (parentGroup != null && parentGroup.getChildEntries().get(0) != this) {
			C4GroupItem predecessor = parentGroup.getChildEntries().get(parentGroup.getChildEntries().indexOf(this) - 1);
			predecessor.open(true);
		}
		
		if (!completed) {
			header = C4GroupHeader.createFromStream(stream);
			
			childEntries = new ArrayList<C4GroupItem>(header.getEntries());
			C4GroupItem entry;
			C4EntryHeader entryHeader;
			for(int i = 0; i < header.getEntries(); i++) {
				hasChildren = true;
				entryHeader = C4EntryHeader.createFromStream(stream);
				if (entryHeader.isGroup()) {
					entry = new C4Group(this, entryHeader.getEntryName()); // a group does not need all header information
				}
				else {
					entry = new C4Entry(this, entryHeader);
				}
				childEntries.add(entry);
			}
		}
		if (recursively) {
			for(C4GroupItem item : childEntries) {
				item.open(true);
			}
		}
		completed = true;
	}

	/**
	 * 
	 */	
	public void extractToFilesystem(IContainer parent) throws CoreException {
		extractToFilesystem(parent, null);
	}
	
	/**
	 * 
	 */	
	public void extractToFilesystem(IContainer parent, IProgressMonitor monitor) throws CoreException {
		IFolder me = null;
		if (parent instanceof IFolder) {
			me = ((IFolder)parent).getFolder(getExtractedName());
		}
		else if (parent instanceof IProject) {
			me = ((IProject)parent).getFolder(getExtractedName());
		}
		else {
			throw new InvalidParameterException("You can not import files to workspace root!");
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
	 * Returns Object.c4d
	 */
	public String getName() {
		return entryName;
	}
	
	/**
	 * Returns c4d.Object
	 * @return
	 */
	public String getExtractedName() {
		return entryName; // C:
	}

	public boolean isCompleted() {
		return completed;
	}
	
	/**
	 * @return the stream
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
		for(C4GroupItem item : childEntries) {
			if (visitor.visit(item, type)) {
				if (item instanceof C4Group) {
					if (monitor != null) monitor.worked(1);
					((C4Group)item).accept(visitor, type);
				}
			}
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
}
