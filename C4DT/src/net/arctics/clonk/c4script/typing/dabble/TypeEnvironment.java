package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.c4script.typing.TypeUnification.unify;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.typing.TypeVariable;

@SuppressWarnings("serial")
public class TypeEnvironment extends HashMap<Declaration, TypeVariable> {
	public TypeEnvironment up;
	public TypeEnvironment() { super(5); }
	public TypeEnvironment(TypeEnvironment up) { this(); this.up = up; }
	public TypeEnvironment inject(TypeEnvironment other) {
		for (final Map.Entry<Declaration, TypeVariable> otherInfo : other.entrySet()) {
			final TypeVariable myVar = this.get(otherInfo.getKey());
			if (myVar != null)
				myVar.set(unify(myVar.get(), otherInfo.getValue().get()));
			else
				this.put(otherInfo.getKey(), otherInfo.getValue());
		}
		return this;
	}
	public void apply(boolean soft) {
		for (final TypeVariable info : this.values())
			info.apply(soft);
	}
	public TypeVariable find(Declaration declaration) {
		return this.get(declaration);
	}
	public void add(TypeVariable var) { this.put(var.key(), var); }
	public static TypeEnvironment newSynchronized() {
		return new TypeEnvironment() {
			@Override
			public synchronized TypeEnvironment inject(TypeEnvironment other) {
				return super.inject(other);
			}
			@Override
			public synchronized TypeVariable find(Declaration declaration) {
				return super.find(declaration);
			}
			@Override
			public synchronized TypeVariable get(Object key) {
				return super.get(key);
			}
		};
	}
}