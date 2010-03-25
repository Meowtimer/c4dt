package net.arctics.clonk.parser.inireader;

/**
 * Specialization to have fancy checkboxes for it in the ini editor
 */
public class Boolean extends UnsignedInteger {
	@Override
	public Object convertToPrimitive() {
		return this.getNumber() != 0;
	}
}
