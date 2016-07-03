package net.arctics.clonk.ast;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;

import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.Core;

public class SourceLocation implements IRegion, Serializable, Cloneable, Comparable<SourceLocation> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final SourceLocation ZERO = new SourceLocation(0, 0);

	private int start, end;

	public SourceLocation() {}

	public SourceLocation(final int start,final int end) {
		this.start = start;
		this.end = end;
	}

	public static SourceLocation offsetRegion(final IRegion region, final int offset) {
		return new SourceLocation(region.getOffset() + offset, region.getOffset() + region.getLength() + offset);
	}

	public int start() { return start; }
	public int end() { return end; }

	public void setStart(final int start) {
		this.start = start;
	}

	public void setEnd(final int end) {
		this.end = end;
	}

	@Override
	public int getLength() { return end-start; }
	@Override
	public int getOffset() { return start; }

	@Override
	public boolean equals(final Object obj) {
		final SourceLocation cmp = as(obj, SourceLocation.class);
		return cmp != null && cmp.start() == start && cmp.end() == end;
	}

	@Override
	public String toString() {
		return "("+start+", "+end+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	// http://stackoverflow.com/questions/113511/hash-code-implementation -.-
	@Override
	public int hashCode() {
		return start * 37 + end * 37;
	}

	public SourceLocation offset(final int o) {
		return new SourceLocation(o+start, o+end);
	}

	@Override
	public SourceLocation clone() {
		try {
			return (SourceLocation)super.clone();
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public SourceLocation relativeTo(final IRegion other) {
		return new SourceLocation(this.start-other.getOffset(), this.end-other.getOffset());
	}

	@Override
	public int compareTo(final SourceLocation o) {
		return start - o.start;
	}

	public SourceLocation add(final IRegion other) {
		return new SourceLocation(start+other.getOffset(), start+other.getOffset()+other.getLength());
	}

	public boolean containsOffset(final int offset) {
		return offset >= start && offset <= end;
	}

	public boolean sameLocation(final SourceLocation other) {
		return other != null && this.start == other.start && this.end == other.end;
	}

	public boolean isAt(final int offset) {
		return offset >= start && offset <= end;
	}

}
