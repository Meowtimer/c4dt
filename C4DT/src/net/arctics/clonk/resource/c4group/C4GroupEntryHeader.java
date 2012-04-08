package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import net.arctics.clonk.Core;

public class C4GroupEntryHeader implements Serializable {

	public static final int STORED_SIZE = 316;
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private String entryName; //260
    private boolean packed; // 4
    private boolean group;
    private int size;
    private int entrySize;
	private int offset;
	private int time;
    private boolean hasCRC;
    private int crc; // unsigned?
    
    protected C4GroupEntryHeader() {}
    
    public C4GroupEntryHeader(File file) {
    	entryName = file.getName();
    	packed = false;
    	group = file.isDirectory();
    	// GROUPFIXME
    	//group = C4Group.getGroupType(file.getName()) != GroupType.OtherGroup || file.isDirectory();
    	size = (int) file.length();
    	offset = 0;
    	time = (int) file.lastModified();
    }
    
    public static C4GroupEntryHeader createHeader(String name, boolean packed, boolean group, int size, int entrySize, int offset, int time) {
    	C4GroupEntryHeader header = new C4GroupEntryHeader();
    	header.entryName = name;
    	header.packed = packed;
    	header.group = group;
    	header.size = size;
    	header.entrySize = entrySize;
    	header.offset = offset;
    	header.time = time; 
    	header.hasCRC = false;
    	header.crc = 0;
    	
    	
    	// TODO implement CRC
    	return header;
    }
    
    public void writeTo(OutputStream stream) throws IOException {
    	byte[] buffer = new byte[STORED_SIZE];
    	arrayCopyTo(C4GroupHeader.stringToByte(entryName),buffer,0,260);
    	arrayCopyTo(C4GroupHeader.booleanToByte(packed),buffer,260,4);
    	arrayCopyTo(C4GroupHeader.booleanToByte(group),buffer,264,4);
    	arrayCopyTo(C4GroupHeader.int32ToByte(size),buffer,268,4);
    	arrayCopyTo(C4GroupHeader.int32ToByte(entrySize),buffer,272,4);
    	arrayCopyTo(C4GroupHeader.int32ToByte(offset),buffer,276,4);
    	arrayCopyTo(C4GroupHeader.int32ToByte(time),buffer,280,4);
    	arrayCopyTo(new byte[] {(byte) (hasCRC ? 0x2 : 0x0)},buffer,284);
    	arrayCopyTo(C4GroupHeader.int32ToByte(crc),buffer,285);
    	stream.write(buffer, 0, STORED_SIZE);
    }
    
    public static C4GroupEntryHeader createFromStream(InputStream stream) throws C4GroupInvalidDataException {
    	C4GroupEntryHeader header = new C4GroupEntryHeader();
		byte[] buffer = new byte[STORED_SIZE];
		try {
			int readCount = stream.read(buffer,0,STORED_SIZE);
			while (readCount != STORED_SIZE) {
				readCount += stream.read(buffer,readCount,STORED_SIZE - readCount);
			}
			header.entryName = C4GroupHeader.byteToString(buffer, 0, 260).trim();
			header.packed = C4GroupHeader.byteToInt32(buffer, 260) > 0;
			header.group = C4GroupHeader.byteToInt32(buffer, 264) > 0;
			header.size = C4GroupHeader.byteToInt32(buffer, 268); // size that is used
			header.entrySize = C4GroupHeader.byteToInt32(buffer, 272); // size that is always 0
			header.offset = C4GroupHeader.byteToInt32(buffer, 276);
			header.time = C4GroupHeader.byteToInt32(buffer, 280);
			header.hasCRC = C4GroupHeader.byteToBoolean(buffer, 284);
//			if (header.hasCRC == false) {
//				throw new InvalidDataException("Suspicious entry header: every entry has a CRC except this one.");
//			}
			header.crc = C4GroupHeader.byteToInt32(buffer, 285);

		} catch (IOException e) {
			e.printStackTrace();
			throw new C4GroupInvalidDataException("There was an IOException."); //$NON-NLS-1$
		}
		return header;
    }
    
	
	public void skipData(InputStream stream) throws IOException {
		if (this.isGroup()) {
			throw new IOException("skipData for groups not implemented"); //$NON-NLS-1$
		}
		else {
			stream.skip(getSize());
		}
	}
    
    /**
	 * @return the entryName
	 */
	public String getEntryName() {
		return entryName;
	}
	/**
	 * @return the packed
	 */
	public boolean isPacked() {
		return packed;
	}
	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}
	/**
	 * @return the entrySize
	 */
	public int getEntrySize() {
		return entrySize;
	}
	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}
	/**
	 * @return the time
	 */
	public int getTime() {
		return time;
	}
	/**
	 * @return the hasCRC
	 */
	public boolean hasCRC() {
		return hasCRC;
	}
	/**
	 * @return the crc
	 */
	public int getCrc() {
		return crc;
	}

	/**
	 * @return the group
	 */
	public boolean isGroup() {
		return group;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	private static void arrayCopyTo(byte[] source, byte[] target, int dstOffset) {
    	arrayCopyTo(source, target, dstOffset, source.length);
    }
    
    private static void arrayCopyTo(byte[] source, byte[] target, int dstOffset, int length) {
    	for(int i = 0;i < length;i++) {
    		if (i >= source.length) target[dstOffset + i] = 0x0; // fill with zeros
    		else target[dstOffset + i] = source[i];
    	}
    }
    
}
