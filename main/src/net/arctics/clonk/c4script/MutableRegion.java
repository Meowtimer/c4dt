package net.arctics.clonk.c4script;

import org.eclipse.jface.text.IRegion;

public class MutableRegion implements IRegion {

	private int offset;
	private int length;
	
	public MutableRegion(final int offset, final int length) {
		super();
		this.offset = offset;
		this.length = length;
	}
	
	public MutableRegion(final IRegion other) {
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

	public void setLength(final int length) {
		this.length = length;
	}

	public void setOffset(final int offset) {
		this.offset = offset;
	}
	
	public void incOffset(final int amount) {
		offset += amount;
	}
	
	public void incLength(final int amount) {
		length += amount;
	}
	
	public void setStartAndEnd(final int start, final int end) {
		offset = start;
		length = end-start;
	}

	public int getEnd() {
		return offset+length;
	}
	
	public boolean maybeIncOffset(final int minOffsetAffected, final int amount) {
		if (offset >= minOffsetAffected) {
			offset += amount;
			return true;
		} else {
			return false;
		}
	}
	
}