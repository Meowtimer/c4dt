package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.util.KeyValuePair;

public class IDArray extends ArrayValue<IDLiteral, Integer> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public IDArray() { super(); }
	@SafeVarargs
	public IDArray(final KeyValuePair<IDLiteral, Integer>... values) {
		for (final KeyValuePair<IDLiteral, Integer> v : values)
			add(v);
	}
	@Override
	public KeyValuePair<IDLiteral, Integer> singleComponentFromString(final int offset, final String s) {
		final String[] idAndCount = s.split("="); //$NON-NLS-1$
		if (idAndCount.length < 2)
			return null;
		final IDLiteral lit = new IDLiteral(ID.get(idAndCount[0].trim()));
		lit.setLocation(offset, offset+lit.idValue().stringValue().length());
		return new KeyValuePair<IDLiteral, Integer>(
			lit,
			Integer.parseInt(idAndCount[1].trim())
		);
	}
}