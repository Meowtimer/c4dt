package net.arctics.clonk.index;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.as;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.IRefinedPrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.util.Utilities;

public class MetaDefinition implements IRefinedPrimitiveType {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Definition definition;
	public Definition definition() { return definition; }
	public MetaDefinition(Definition definition) { super(); this.definition = definition; }
	@Override
	public Iterator<IType> iterator() { return iterable(PrimitiveType.ID, this).iterator(); }
	@Override
	public String typeName(boolean special) { return String.format("id[%s]", definition.typeName(special)); }
	@Override
	public IType simpleType() { return PrimitiveType.ID; }
	@Override
	public String toString() { return typeName(true); }
	@Override
	public PrimitiveType primitiveType() { return PrimitiveType.ID; }
	@Override
	public boolean equals(Object obj) {
		final MetaDefinition other = as(obj, MetaDefinition.class);
		return other != null && Utilities.objectsEqual(other.definition(), definition());
	}
}