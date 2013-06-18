package net.arctics.clonk.c4script.typing.dabble;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.typing.TypeVariable;
import net.arctics.clonk.c4script.typing.Typing;

@SuppressWarnings("serial")
public class TypeEnvironment extends HashMap<Declaration, TypeVariable> {
	final Typing typing;
	public final TypeEnvironment up;
	public TypeEnvironment(Typing typing, TypeEnvironment up) {
		super(5);
		this.typing = typing;
		this.up = up;
	}
	public TypeEnvironment(Typing typing) {
		this(typing, null);
	}
	public TypeEnvironment inject(TypeEnvironment other) {
		for (final Map.Entry<Declaration, TypeVariable> otherInfo : other.entrySet()) {
			final TypeVariable myVar = this.get(otherInfo.getKey());
			if (myVar != null)
				myVar.set(typing.unify(myVar.get(), otherInfo.getValue().get()));
			else
				this.put(otherInfo.getKey(), otherInfo.getValue());
		}
		return this;
	}
	public void apply(boolean soft) {
		for (final TypeVariable info : this.values())
			info.apply(soft);
	}
	public void add(TypeVariable var) { this.put(var.key(), var); }
	public static TypeEnvironment newSynchronized(Typing typing) {
		return new TypeEnvironment(typing) {
			@Override
			public synchronized TypeEnvironment inject(TypeEnvironment other) {
				return super.inject(other);
			}
			@Override
			public synchronized TypeVariable get(Object key) { return super.get(key); }
		};
	}
}