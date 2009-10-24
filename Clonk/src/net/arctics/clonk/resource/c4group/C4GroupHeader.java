package net.arctics.clonk.resource.c4group;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * The header of a C4Group group
 * @author ZokRadonh
 *
 */
public class C4GroupHeader implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final int STORED_SIZE = 204;
	
    private String id;
    private int ver1, ver2;
    private int entries;
    private String maker;
    private String password;
    private int creation;
    private int original;
    
    protected C4GroupHeader() {
    }
    
    public static C4GroupHeader createHeader(int entryCount, String maker) {
    	C4GroupHeader header = new C4GroupHeader();
    	header.id = "RedWolf Design GrpFolder"; //$NON-NLS-1$
    	header.entries = entryCount;
    	header.ver1 = 1;
    	header.ver2 = 2;
    	header.maker = maker;
    	header.password = ""; //$NON-NLS-1$
    	header.creation = 1221126239; // TODO implement creation time
    	header.original = 0;
    	
    	return header;
    }
    
    public void writeTo(OutputStream stream) throws IOException {
		byte[] completeHeader = new byte[STORED_SIZE];
		arrayCopyTo(stringToByte(id), completeHeader, 0, 24);
		arrayCopyTo(new byte[] { 0x1, 0x0, 0x0, 0x0 }, completeHeader, 28);
		arrayCopyTo(new byte[] { 0x2, 0x0, 0x0, 0x0 }, completeHeader, 32);
		arrayCopyTo(int32ToByte(entries),completeHeader,36);
		arrayCopyTo(stringToByte(maker), completeHeader, 40, 30);
		arrayCopyTo(stringToByte(password), completeHeader, 72, 30);
		arrayCopyTo(int32ToByte(creation),completeHeader,104);
		arrayCopyTo(int32ToByte(original),completeHeader,108);
		C4Group.MemScramble(completeHeader, STORED_SIZE);
		stream.write(completeHeader,0, STORED_SIZE);
	}
    
    private void arrayCopyTo(byte[] source, byte[] target, int dstOffset) {
    	arrayCopyTo(source, target, dstOffset, source.length);
    }
    
    private void arrayCopyTo(byte[] source, byte[] target, int dstOffset, int length) {
    	for(int i = 0;i < length;i++) {
    		if (i >= source.length) target[dstOffset + i] = 0x0; // fill with zeros
    		else target[dstOffset + i] = source[i];
    	}
    }
    
	public static C4GroupHeader createFromStream(InputStream stream) throws InvalidDataException, IOException {
    	C4GroupHeader result = new C4GroupHeader();
    	
    	// read header
    	byte[] buffer = new byte[STORED_SIZE];
    	
    	int readCount = stream.read(buffer,0, STORED_SIZE);
    	while (readCount != STORED_SIZE) {
    		readCount += stream.read(buffer,readCount,STORED_SIZE - readCount);
    	}
		
		
		// unscramble header
    	C4Group.MemScramble(buffer, STORED_SIZE);
    	
    	// parse header
    	result.id = byteToString(buffer,0,24).trim();
    	int compare = result.id.compareTo("RedWolf Design GrpFolder"); //$NON-NLS-1$
    	if (compare > 0) {
    		C4Group.MemScramble(buffer, STORED_SIZE);
    		throw new InvalidDataException("Header id is invalid ('" + result.id + "')"); //$NON-NLS-1$ //$NON-NLS-2$
    		
    	}
    	result.ver1 = byteToInt32(buffer,28);
    	if (result.ver1 != 1) {
    		throw new InvalidDataException(Messages.C4GroupHeader_5);
    	}
    	result.ver2 = byteToInt32(buffer,32);
    	if (result.ver2 != 2) {
    		throw new InvalidDataException(Messages.C4GroupHeader_6);
    	}
    	result.entries = byteToInt32(buffer, 36);
    	if (result.entries > 1000) {
    		throw new InvalidDataException(Messages.C4GroupHeader_7);
    	}
    	result.maker = byteToString(buffer, 40, 30).trim();
    	result.password = byteToString(buffer, 72, 30).trim();
    	result.creation = byteToInt32(buffer, 104);
    	result.original = byteToInt32(buffer, 108);
    	
    	return result;
    }

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the ver1
	 */
	public int getVer1() {
		return ver1;
	}

	/**
	 * @return the ver2
	 */
	public int getVer2() {
		return ver2;
	}

	/**
	 * @return the entries
	 */
	public int getEntries() {
		return entries;
	}

	/**
	 * @return the maker
	 */
	public String getMaker() {
		return maker;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the creation
	 */
	public int getCreation() {
		return creation;
	}

	/**
	 * @return the original
	 */
	public int getOriginal() {
		return original;
	}
	
    public static int byteToInt32(byte[] buffer, int offset) {
    	int result = 0;
    	for(int i = 0;i < 4;i++) {
    		result += (buffer[i + offset] & 0xFF) << (i*8);
    		
    	}
    	return result;
    }
    
    public static byte[] int32ToByte(int number) {
    	byte[] buffer = new byte[4];
    	for(int i = 0;i < 4;i++) {
    		int mix = (0xFF << (i*8));
    		int flush = number & mix;
    		buffer[i] = (byte)((flush >> (i*8) & 0xFF));
    	}
    	return buffer;
    }
    
    public static String byteToString(byte[] buffer, int offset, int length) {
    	try {
			return new String(buffer,offset,length,"ISO-8859-1"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "unkwnown encoding"; //$NON-NLS-1$
		}
    }
    
    public static byte[] stringToByte(String str) {
    	try {
			return str.getBytes("ISO-8859-1"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    public static boolean byteToBoolean(byte[] buffer, int offset) {
    	return buffer[offset] == 1;
    }
    
    public static byte[] booleanToByte(boolean bool) {
    	return new byte[] { bool ? (byte)1 : (byte)0 };
    }
    
}
