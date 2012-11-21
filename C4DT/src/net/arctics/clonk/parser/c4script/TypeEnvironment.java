package net.arctics.clonk.parser.c4script;

import java.util.ArrayList;
import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.ast.ITypeInfo;

public class TypeEnvironment extends ArrayList<ITypeInfo> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public TypeEnvironment up;
	public TypeEnvironment() {
		super();
	}
	public TypeEnvironment(int capacity) {
		super(capacity);
	}
	public TypeEnvironment inject(TypeEnvironment other) {
		for (ITypeInfo info : this)
			for (Iterator<ITypeInfo> it = other.iterator(); it.hasNext();) {
				ITypeInfo info2 = it.next();
				if (info2.local()) {
					it.remove();
					continue;
				}
				if (info2.refersToSameExpression(info)) {
					info.merge(info2);
					it.remove();
				}
			}
		this.addAll(other);
		return this;
	}
	public void apply(C4ScriptParser parser, boolean soft) {
		for (ITypeInfo info : this)
			info.apply(soft, parser);
	}
}