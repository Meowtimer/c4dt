package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.DeclMask;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.util.StringUtil;

/**
 * A proplist declaration parsed from an {key:value, ...} expression.
 * @author madeen
 *
 */
public class ProplistDeclaration extends Structure implements IRefinedPrimitiveType, IHasIncludes<ProplistDeclaration>, Cloneable, IProplistDeclaration {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected List<Variable> components;
	protected List<Variable> adhocComponents;
	protected boolean adHoc;
	protected ProplistDeclaration prototype;

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#isAdHoc()
	 */
	@Override
	public boolean isAdHoc() {
		return adHoc;
	}

	private ProplistDeclaration() {
		setName(String.format("%s {...}", PrimitiveType.PROPLIST.toString()));
	}

	/**
	 * Create a new ProplistDeclaration, passing it component variables it takes over directly. The list won't be copied.
	 * @param components The component variables
	 */
	public ProplistDeclaration(List<Variable> components) {
		this.components = components;
	}

	/**
	 * Create an adhoc proplist declaration.
	 * @return The newly created adhoc proplist declaration.
	 */
	public static ProplistDeclaration newAdHocDeclaration() {
		final ProplistDeclaration result = new ProplistDeclaration();
		result.adHoc = true;
		result.components = new LinkedList<Variable>();
		return result;
	}

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#addComponent(net.arctics.clonk.parser.c4script.Variable)
	 */
	@Override
	public Variable addComponent(Variable variable, boolean adhoc) {
		final Variable found = findComponent(variable.name());
		if (found != null)
			return found;
		else {
			List<Variable> list;
			if (adhoc)
				synchronized (components) {
					if (adhocComponents == null)
						adhocComponents = new ArrayList<Variable>(5);
					list = adhocComponents;
				}
			else
				list = components;
			list.add(variable);
			variable.setParent(this);
			return variable;
		}
	}

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#findComponent(java.lang.String, java.util.Set)
	 */
	public Variable findComponent(String declarationName, Set<ProplistDeclaration> recursionPrevention) {
		if (!recursionPrevention.add(this))
			return null;
		synchronized (components) {
			for (final Variable v : components)
				if (v.name().equals(declarationName))
					return v;
			if (adhocComponents != null)
				for (final Variable v : adhocComponents)
					if (v.name().equals(declarationName))
						return v;
		}
		final IProplistDeclaration proto = prototype();
		if (proto instanceof ProplistDeclaration)
			return ((ProplistDeclaration)proto).findComponent(declarationName, recursionPrevention);
		else if (proto != null)
			return proto.findComponent(declarationName);
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#findComponent(java.lang.String)
	 */
	@Override
	public Variable findComponent(String declarationName) {
		return findComponent(declarationName, new HashSet<ProplistDeclaration>());
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		synchronized (components) {
			for (final Variable v : components)
				if (v.name().equals(declarationName))
					return v;
			if (adhocComponents != null)
				for (final Variable v : adhocComponents)
					if (v.name().equals(declarationName))
						return v;
		}
		return null;
	}

	@Override
	public Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		if ((mask & DeclMask.VARIABLES) != 0) {
			final ArrayList<Variable> items = new ArrayList<>(components.size()+(adhocComponents != null ? adhocComponents.size() : 0));
			items.addAll(components);
			if (adhocComponents != null)
				items.addAll(adhocComponents);
			return items;
		}
		else
			return NO_SUB_DECLARATIONS;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		if (Variable.class.isAssignableFrom(from.getClass()))
			return (T) findComponent(from.name());
		else
			return super.latestVersionOf(from);
	};

	@Override
	public Iterator<IType> iterator() {
		return iterable(PrimitiveType.PROPLIST, (IType)this).iterator();
	}

	@Override
	public String typeName(boolean special) {
		if (special) {
			final String s = StringUtil.blockString("{", "}", ", ", components(true));
			if (s.length() < 50)
				return s;
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
		for (final Variable v : components)
			if (v.name().equals(PROTOTYPE_KEY)) {
				prototypeType = v.type();
				break;
			}
		if (prototypeType == null)
			prototypeType = prototype;
		if (prototypeType != null)
			for (final IType ty : prototypeType)
				if (ty instanceof ProplistDeclaration)
					return (ProplistDeclaration)ty;
		return null;
	}

	@Override
	public Collection<ProplistDeclaration> includes(Index contextIndex, Object origin, int options) {
		return IHasIncludes.Default.includes(contextIndex, this, origin, options);
	}

	@Override
	public boolean doesInclude(Index contextIndex, ProplistDeclaration other) {
		final Set<ProplistDeclaration> includes = new HashSet<ProplistDeclaration>(10);
		gatherIncludes(contextIndex, this, includes, GatherIncludesOptions.Recursive);
		return includes.contains(other);
	}

	@Override
	public boolean gatherIncludes(Index contextIndex, Object origin, Collection<ProplistDeclaration> set, int options) {
		if (!set.add(this))
			return false;
		final ProplistDeclaration proto = prototype();
		if (proto != null)
			if ((options & GatherIncludesOptions.Recursive) == 0)
				set.add(proto);
			else
				proto.gatherIncludes(contextIndex, origin, set, options);
		return true;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		final ProplistDeclaration o = as(other, ProplistDeclaration.class);
		return o != null && o.sameLocation(this);
	}

	@Override
	public ProplistDeclaration clone() {
		final List<Variable> clonedComponents = new ArrayList<Variable>(this.components.size());
		for (final Variable v : components)
			clonedComponents.add(v.clone());
		final ProplistDeclaration clone = new ProplistDeclaration(clonedComponents);
		clone.setLocation(this);
		clone.adHoc = this.adHoc;
		return clone;
	}

	@Override
	public String toString() {
		return StringUtil.writeBlock(null, "{", "}", ", ", components);
	}

	/**
	 * Set {@link #implicitPrototype()} and return this declaration.
	 * @param prototypeExpression The implicit prototype to set
	 * @return This one.
	 */
	public void setPrototype(ProplistDeclaration prototype) {
		this.prototype = prototype;
	}

	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
	}

	@Override
	public Collection<Variable> components(boolean includeAdhocComponents) {
		synchronized(components) {
			final ArrayList<Variable> list = new ArrayList<Variable>(components.size()+(includeAdhocComponents&&adhocComponents!=null?adhocComponents.size():0));
			list.addAll(components);
			if (includeAdhocComponents && adhocComponents != null)
				list.addAll(adhocComponents);
			return list;
		}
	}

	public int numComponents(boolean includeAdhocComponents) {
		synchronized(components) {
			int result = components.size();
			if (includeAdhocComponents && adhocComponents != null)
				result += adhocComponents.size();
			return result;
		}
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.PROPLIST;
	}

}
