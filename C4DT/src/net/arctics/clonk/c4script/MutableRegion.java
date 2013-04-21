package net.arctics.clonk.c4script;

import org.eclipse.jface.text.IRegion;

public class MutableRegion implements IRegion {

	private int offset;
	private int length;
	
	public MutableRegion(int offset, int length) {
		super();
		this.offset = offset;
		this.length = length;
	}
	
	public MutableRegion(IRegion other) {
		this(other.getOffset(), other.getLength());
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public void incOffset(int amount) {
		offset += amount;
	}
	
	public void incLength(int amount) {
		length += amount;
	}
	
	public void setStartAndEnd(int start, int end) {
		offset = start;
		length = end-start;
	}

	public int getEnd() {
		return offset+length;
	}
	
	public boolean maybeIncOffset(int minOffsetAffected, int amount) {
		if (offset >= minOffsetAffected) {
			offset += amount;
			return true;
		} else
			return false;
	}
	
}