package net.arctics.clonk.parser.c4script;

import org.eclipse.jface.text.IRegion;

public class MutableRegion implements IRegion {

	private int offset;
	private int length;
	
	public MutableRegion(int offset, int length) {
		super();
		this.offset = offset;
		this.length = length;
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
	
}