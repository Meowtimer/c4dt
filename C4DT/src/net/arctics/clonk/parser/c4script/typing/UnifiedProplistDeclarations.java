package net.arctics.clonk.parser.c4script.typing;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Variable;

public final class UnifiedProplistDeclarations extends ProplistDeclaration {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final ProplistDeclaration _a;
	private final ProplistDeclaration _b;
	
	public UnifiedProplistDeclarations(List<Variable> components, ProplistDeclaration _a, ProplistDeclaration _b) {
		super(components);
		this._a = _a;
		this._b = _b;
	}

	@Override
	public boolean gatherIncludes(Index contextIndex, Object origin, Collection<ProplistDeclaration> set, int options) {
		if (!set.add(this))
			return false;
		if ((options & GatherIncludesOptions.Recursive) != 0) {
			_a.gatherIncludes(contextIndex, origin, set, options);
			_b.gatherIncludes(contextIndex, origin, set, options);
		} else {
			set.add(_a);
			set.add(_b);
		}
		return true;
	}

	@Override
	public Collection<Variable> components(boolean includeAdhocComponents) {
		final Map<String, Variable> vars = new HashMap<String, Variable>();
		for (final Variable v : _a.components(includeAdhocComponents))
			vars.put(v.name(), v);
		for (final Variable v : _b.components(includeAdhocComponents))
			if (!vars.containsKey(v.name()))
				vars.put(v.name(), v);
		return vars.values();
	}
}