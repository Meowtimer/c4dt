package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

/**
 * A proplist declaration parsed from an {key:value, ...} expression.
 * @author madeen
 *
 */
public class ProplistDeclaration extends Structure implements IRefinedPrimitiveType, IHasIncludes, Cloneable, IProplistDeclaration {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected List<Variable> components;
	protected List<Variable> adhocComponents;
	protected boolean adHoc;
	protected ExprElm implicitPrototype;
	
	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#implicitPrototype()
	 */
	@Override
	public ExprElm implicitPrototype() {
		return implicitPrototype;
	}

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
		ProplistDeclaration result = new ProplistDeclaration();
		result.adHoc = true;
		result.components = new LinkedList<Variable>();
		return result;
	}
	
	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#addComponent(net.arctics.clonk.parser.c4script.Variable)
	 */
	@Override
	public Variable addComponent(Variable variable, boolean adhoc) {
		Variable found = findComponent(variable.name());
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
			variable.setParentDeclaration(this);
			return variable;
		}
	}
	
	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#findComponent(java.lang.String, java.util.Set)
	 */
	public Variable findComponent(String declarationName, Set<ProplistDeclaration> recursionPrevention) {
		if (recursionPrevention.contains(this))
			return null;
		else
			recursionPrevention.add(this);
		synchronized (components) {
			for (Variable v : components)
				if (v.name().equals(declarationName))
					return v;
			if (adhocComponents != null)
				for (Variable v : adhocComponents)
					if (v.name().equals(declarationName))
						return v;
		}
		IProplistDeclaration proto = prototype();
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
			for (Variable v : components)
				if (v.name().equals(declarationName))
					return v;
			if (adhocComponents != null)
				for (Variable v : adhocComponents)
					if (v.name().equals(declarationName))
						return v;
		}
		return null;
	}
	
	@Override
	public Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		if ((mask & VARIABLES) != 0)
			return Collections.unmodifiableCollection(components);
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
		return iterable(PrimitiveType.PROPLIST, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return PrimitiveType.PROPLIST.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		if (special) {
			String s = StringUtil.blockString("{", "}", ", ", components(true));
			if (s.length() < 50)
				return s;
		}
		return PrimitiveType.PROPLIST.typeName(special);
	}

	@Override
	public int precision() {
		return PrimitiveType.PROPLIST.precision()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.PROPLIST;
	}
	
	@Override
	public void setTypeDescription(String description) {}
	
	/* (non-Javadoc)
	 * @see net.arctics.clonk.parser.c4script.IProplistDeclaration#prototype()
	 */
	@Override
	public ProplistDeclaration prototype() {
		ExprElm prototypeExpr = null;
		for (Variable v : components)
			if (v.name().equals(PROTOTYPE_KEY)) {
				prototypeExpr = v.initializationExpression();
				break;
			}
		if (prototypeExpr == null)	
			prototypeExpr = implicitPrototype;
		if (prototypeExpr != null) {
			IType t = prototypeExpr.type(declarationObtainmentContext());
			if (t != null)
				for (IType ty : t)
					if (ty instanceof ProplistDeclaration)
						return (ProplistDeclaration)ty;
		}
		return null;
	}

	@Override
	public Collection<? extends IHasIncludes> includes(Index contextIndex, IHasIncludes origin, int options) {
		return IHasIncludes.Default.includes(contextIndex, this, origin, options);
	}

	@Override
	public boolean doesInclude(Index contextIndex, IHasIncludes other) {
		List<IHasIncludes> includes = new ArrayList<IHasIncludes>(10);
		gatherIncludes(contextIndex, this, includes, GatherIncludesOptions.Recursive);
		return includes.contains(other);
	}
	
	@Override
	public boolean gatherIncludes(Index contextIndex, IHasIncludes origin, List<IHasIncludes> set, int options) {
		if (set.contains(this))
			return false;
		else
			set.add(this);
		IHasIncludes proto = prototype();
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
		ProplistDeclaration o = as(other, ProplistDeclaration.class);
		return o != null && Utilities.objectsEqual(o.location(), this.location());
	}
	
	@Override
	public ProplistDeclaration clone() throws CloneNotSupportedException {
		List<Variable> clonedComponents = new ArrayList<Variable>(this.components.size());
		for (Variable v : components)
			clonedComponents.add(v.clone());
		ProplistDeclaration clone = new ProplistDeclaration(clonedComponents);
		clone.location = this.location.clone();
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
	public ProplistDeclaration withImplicitProtoype(ExprElm prototypeExpression) {
		this.implicitPrototype = prototypeExpression;
		return this;
	}
	
	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
	}

	@Override
	public Collection<Variable> components(boolean includeAdhocComponents) {
		synchronized(components) {
			ArrayList<Variable> list = new ArrayList<Variable>(components.size()+(includeAdhocComponents&&adhocComponents!=null?adhocComponents.size():0));
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
