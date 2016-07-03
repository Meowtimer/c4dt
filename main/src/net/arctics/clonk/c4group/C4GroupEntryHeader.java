package net.arctics.clonk.c4group;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import net.arctics.clonk.Core;

public class C4GroupEntryHeader implements Serializable {

	public static final int STORED_SIZE = 316;

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final String entryName; //260
    private final boolean packed; // 4
    private final boolean group;
    private final int size;
    private final int entrySize;
	private final int offset;
	private final int time;
    private final boolean hasCRC;
    private final int crc; // unsigned?

    public C4GroupEntryHeader(final File file) {
    	this.entryName = file.getName();
    	this.packed = false;
    	this.group = file.isDirectory();
    	// GROUPFIXME
    	//group = C4Group.getGroupType(file.getName()) != GroupType.OtherGroup || file.isDirectory();
    	this.size = (int) file.length();
    	this.entrySize = 0;
    	this.offset = 0;
    	this.time = (int) file.lastModified();
    	this.hasCRC = false;
    	this.crc = 0;
    }

    public C4GroupEntryHeader(
    	final String name,
    	final boolean packed,
    	final boolean group,
    	final int size,
    	final int entrySize,
    	final int offset,
    	final int time,
    	final boolean hasCRC,
    	final int crc
    ) {
    	entryName = name;
    	this.packed = packed;
    	this.group = group;
    	this.size = size;
    	this.entrySize = entrySize;
    	this.offset = offset;
    	this.time = time;
    	// TODO implement CRC
    	this.hasCRC = hasCRC;
    	this.crc = crc;
    }

    public void writeTo(final OutputStream stream) throws IOException {
    	final byte[] buffer = new byte[STORED_SIZE];
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

    public static C4GroupEntryHeader createFromStream(final InputStream stream) throws C4GroupInvalidDataException {
		try {
			final byte[] buffer = readToBuffer(stream, STORED_SIZE);
			return new C4GroupEntryHeader(
				C4GroupHeader.byteToString(buffer, 0, 260).trim(),
				C4GroupHeader.byteToInt32(buffer, 260) > 0,
				C4GroupHeader.byteToInt32(buffer, 264) > 0,
				C4GroupHeader.byteToInt32(buffer, 268), // size that is used
				C4GroupHeader.byteToInt32(buffer, 272), // size that is always 0
				C4GroupHeader.byteToInt32(buffer, 276),
				C4GroupHeader.byteToInt32(buffer, 280),
				C4GroupHeader.byteToBoolean(buffer, 284),
				C4GroupHeader.byteToInt32(buffer, 285)
			);
		} catch (final IOException e) {
			e.printStackTrace();
			throw new C4GroupInvalidDataException(e.getMessage()); //$NON-NLS-1$
		}
    }

	private static byte[] readToBuffer(final InputStream stream, int bufferSize) throws IOException {
		final byte[] buffer = new byte[bufferSize];
		int readCount = stream.read(buffer,0,bufferSize);
		while (readCount != bufferSize) {
			readCount += stream.read(buffer,readCount,bufferSize - readCount);
		}
		return buffer;
	}

	public void skipData(final InputStream stream) throws IOException {
		if (this.isGroup()) {
			throw new IOException("skipData for groups not implemented"); //$NON-NLS-1$
		} else {
			stream.skip(size());
		}
	}

	public String entryName() { return entryName; }
	public boolean isPacked() { return packed; }
	public int size() { return size; }
	public int entrySize() { return entrySize; }
	public int offset() { return offset; }
	public int time() { return time; }
	public boolean hasCRC() { return hasCRC; }
	public boolean isGroup() { return group; }

	private static void arrayCopyTo(final byte[] source, final byte[] target, final int dstOffset) {
    	arrayCopyTo(source, target, dstOffset, source.length);
    }

    private static void arrayCopyTo(final byte[] source, final byte[] target, final int dstOffset, final int length) {
    	for(int i = 0;i < length;i++) {
			if (i >= source.length) {
				target[dstOffset + i] = 0x0; // fill with zeros
			} else {
				target[dstOffset + i] = source[i];
			}
		}
    }

}
