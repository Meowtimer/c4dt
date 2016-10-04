package net.arctics.clonk.c4script;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4script.typing.IRefinedPrimitiveType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.util.StringUtil;

/**
 * A proplist declaration parsed from an {key:value, ...} expression.
 * @author madeen
 *
 */
public class ProplistDeclaration extends Structure implements IRefinedPrimitiveType, IHasIncludes<ProplistDeclaration>, Cloneable, IProplistDeclaration {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	protected Variable[] components = NO_VARIABLES;
	protected Variable[] added = NO_VARIABLES;

	protected ProplistDeclaration prototype;

	static final Variable[] NO_VARIABLES = new Variable[0];

	/**
	 * Create a new ProplistDeclaration, passing it component variables it takes over directly. The list won't be copied.
	 * @param components The component variables
	 */
	public ProplistDeclaration(Variable[] components) { this.components = components; }

	/** Create initially empty proplist */
	public ProplistDeclaration() { }

	public ProplistDeclaration(final String name) { this(NO_VARIABLES); setName(name); }

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#addComponent(net.arctics.clonk.parser.c4script.Variable)
	 */
	@Override
	public synchronized Variable addComponent(final Variable variable, final boolean adhoc) {
		final Variable found = findComponent(variable.name());
		if (found != null) {
			return found;
		} else {
			variable.setParent(this);
			if (adhoc) {
				added = concat(added, variable);
			} else {
				components = concat(components, variable);
			}
			return variable;
		}
	}

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#findComponent(java.lang.String, java.util.Set)
	 */
	public Variable findComponent(final String declarationName, final Set<ProplistDeclaration> recursionPrevention) {
		if (!recursionPrevention.add(this)) {
			return null;
		}
		for (final Variable v : components) {
			if (v.name().equals(declarationName)) {
				return v;
			}
		}
		for (final Variable v : added) {
			if (v.name().equals(declarationName)) {
				return v;
			}
		}
		final IProplistDeclaration proto = prototype();
		return (
			proto instanceof ProplistDeclaration ? ((ProplistDeclaration)proto).findComponent(declarationName, recursionPrevention) :
			proto != null ? proto.findComponent(declarationName) :
			null
		);
	}

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#findComponent(java.lang.String)
	 */
	@Override
	public Variable findComponent(final String declarationName) {
		return findComponent(declarationName, new HashSet<ProplistDeclaration>());
	}

	@Override
	public Declaration findLocalDeclaration(final String declarationName, final Class<? extends Declaration> declarationClass) {
		for (final Variable v : components) {
			if (v.name().equals(declarationName)) {
				return v;
			}
		}
		for (final Variable v : added) {
			if (v.name().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}

	@Override
	public List<? extends Declaration> subDeclarations(final Index contextIndex, final int mask) {
		if ((mask & DeclMask.VARIABLES) != 0) {
			final ArrayList<Variable> items = new ArrayList<>(components.length + added.length);
			Collections.addAll(items, components);
			Collections.addAll(items,  added);
			return items;
		} else {
			return Collections.emptyList();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(final T from) {
		if (Variable.class.isAssignableFrom(from.getClass())) {
			return (T) findComponent(from.name());
		} else {
			return super.latestVersionOf(from);
		}
	};

	@Override
	public Iterator<IType> iterator() {
		return iterable(PrimitiveType.PROPLIST, (IType)this).iterator();
	}

	@Override
	public String typeName(final boolean special) {
		if (special) {
			final String s = StringUtil.blockString("{", "}", ", ", components(true));
			if (s.length() < 50) {
				return s;
			}
		}
		return PrimitiveType.PROPLIST.typeName(special);
	}

	@Override
	public IType simpleType() {
		return PrimitiveType.PROPLIST;
	}

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#prototype()
	 */
	@Override
	public ProplistDeclaration prototype() {
		IType prototypeType = null;
		for (final Variable v : components) {
			if (v.name().equals(PROTOTYPE_KEY)) {
				prototypeType = v.type();
				break;
			}
		}
		if (prototypeType == null) {
			prototypeType = prototype;
		}
		if (prototypeType != null) {
			for (final IType ty : prototypeType) {
				if (ty instanceof ProplistDeclaration) {
					return (ProplistDeclaration)ty;
				}
			}
		}
		return null;
	}

	@Override
	public Collection<ProplistDeclaration> includes(final Index contextIndex, final ProplistDeclaration origin, final int options) {
		return IHasIncludes.Default.includes(contextIndex, this, origin, options);
	}

	@Override
	public boolean doesInclude(final Index contextIndex, final ProplistDeclaration other) {
		final Set<ProplistDeclaration> includes = new HashSet<ProplistDeclaration>(10);
		gatherIncludes(contextIndex, this, includes, GatherIncludesOptions.Recursive);
		return includes.contains(other);
	}

	@Override
	public boolean gatherIncludes(final Index contextIndex, final ProplistDeclaration origin, final Collection<ProplistDeclaration> set, final int options) {
		if (!set.add(this)) {
			return false;
		}
		final ProplistDeclaration proto = prototype();
		if (proto != null) {
			if ((options & GatherIncludesOptions.Recursive) == 0) {
				set.add(proto);
			} else {
				proto.gatherIncludes(contextIndex, origin, set, options);
			}
		}
		return true;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		final ProplistDeclaration o = as(other, ProplistDeclaration.class);
		return o != null && o.sameLocation(this);
	}

	@Override
	public ProplistDeclaration clone() {
		final ProplistDeclaration clone = new ProplistDeclaration(
			stream(components).map(Variable::clone).toArray(length -> new Variable[length])
		);
		clone.setLocation(this);
		return clone;
	}

	@Override
	public String toString() {
		return StringUtil.writeBlock(null, "{", "}", ", ", stream(components)).toString();
	}

	/**
	 * Set {@link #implicitPrototype()} and return this declaration.
	 * @param prototypeExpression The implicit prototype to set
	 * @return
	 * @return This one.
	 */
	public ProplistDeclaration setPrototype(final ProplistDeclaration prototype) {
		this.prototype = prototype;
		return this;
	}

	@Override
	public Collection<Variable> components(final boolean includeAdhocComponents) {
		final ArrayList<Variable> list = new ArrayList<Variable>(components.length + (includeAdhocComponents ? added.length : 0));
		Collections.addAll(list, components);
		if (includeAdhocComponents) {
			Collections.addAll(list, added);
		}
		return list;
	}

	public int numComponents(final boolean includeAdhocComponents) {
		return components.length + (includeAdhocComponents ? added.length : 0);
	}

	@Override
	public PrimitiveType primitiveType() { return PrimitiveType.PROPLIST; }

}
