package net.arctics.clonk.ast;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.typing.IRefinedPrimitiveType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.stringtbl.StringTbl;

public class Placeholder extends ASTNode implements IRefinedPrimitiveType {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	protected String entryName;
	
	public Placeholder(final String entryName) { this.entryName = entryName; }
	
	@Override
	public boolean isValidAtEndOfSequence() { return true; }
	@Override
	public boolean isValidInSequence(final ASTNode predecessor) { return true; }
	public String entryName() { return entryName; }
	
	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append('$');
		builder.append(entryName);
		builder.append('$');
	}
	
	@Override
	public EntityRegion entityAt(final int offset, final ExpressionLocator<?> locator) {
		final StringTbl stringTbl = parent(Script.class).localStringTblMatchingLanguagePref();
		if (stringTbl != null) {
			final NameValueAssignment entry = stringTbl.map().get(entryName);
			if (entry != null)
				return new EntityRegion(entry, this);
		}
		return super.entityAt(offset, locator);
	}
	
	@Override
	public boolean hasSideEffects() { return true; /* let's just assume that */ }
	@Override
	public Iterator<IType> iterator() { return iterable((IType)this).iterator(); }
	@Override
	public String typeName(final boolean special) { return special ? toString() : PrimitiveType.ANY.typeName(false); }
	@Override
	public IType simpleType() { return PrimitiveType.ANY; }
	@Override
	public PrimitiveType primitiveType() { return PrimitiveType.ANY; }
	
	@Override
	protected boolean equalAttributes(ASTNode other) {
		final Placeholder o = as(other, Placeholder.class);
		return o != null && eq(entryName, o.entryName) && super.equalAttributes(other);
	}

}