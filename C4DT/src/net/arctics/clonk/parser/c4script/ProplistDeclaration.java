package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.arrayIterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.util.CompoundIterable;
import net.arctics.clonk.util.StringUtil;

/**
 * A proplist declaration parsed from an {key:value, ...} expression.
 * @author madeen
 *
 */
public class ProplistDeclaration extends Structure implements IType, IHasIncludes, Cloneable {

	private static final long serialVersionUID = 1L;
	public static final String PROTOTYPE_KEY = "Prototype";
	
	protected List<Variable> components;
	protected boolean adHoc;
	protected ExprElm implicitPrototype;
	
	/**
	 * Return the implicitly set prototype expression for this declaration. Acts as fallback if no explicit 'Prototype' field is found.
	 * @return The implicit prototype
	 */
	public ExprElm implicitPrototype() {
		return implicitPrototype;
	}

	/**
	 * Whether the declaration was "explicit" {blub=<blub>...} or
	 * by assigning values separately (effect.var1 = ...; ...)
	 * @return adhoc-ness
	 */
	public boolean isAdHoc() {
		return adHoc;
	}
	
	/**
	 * Each assignment in a proplist declaration is represented by a {@link Variable} object.
	 * @return Return the list of component variables this proplist declaration is made up of.
	 */
	public List<Variable> components() {
		return components;
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
	
	/**
	 * Add a new component variable to this declaration.
	 * @param variable The variable to add
	 * @return Return either the passed variable or an already existing one with that name
	 */
	public Variable addComponent(Variable variable) {
		Variable found = findComponent(variable.name());
		if (found != null) {
			return found;
		} else {
			components.add(variable);
			variable.setParentDeclaration(this);
			return variable;
		}
	}
	
	/**
	 * Find a component variable by name.
	 * @param declarationName The name of the variable
	 * @return The found variable or null.
	 */
	public Variable findComponent(String declarationName, Set<ProplistDeclaration> recursionPrevention) {
		if (recursionPrevention.contains(this))
			return null;
		else
			recursionPrevention.add(this);
		for (Variable v : components)
			if (v.name().equals(declarationName))
				return v;
		ProplistDeclaration proto = prototype();
		if (proto != null)
			return proto.findComponent(declarationName, recursionPrevention);
		else
			return null;
	}
	
	public Variable findComponent(String declarationName) {
		return findComponent(declarationName, new HashSet<ProplistDeclaration>());
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		for (Variable v : components()) {
			if (v.name().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}
	
	@Override
	public Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		if ((mask & VARIABLES) != 0) {
			List<Iterable<? extends Declaration>> its = new LinkedList<Iterable<? extends Declaration>>();
			its.add(components());
			return new CompoundIterable<Declaration>(its);
		}
		else
			return NO_SUB_DECLARATIONS;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		if (Variable.class.isAssignableFrom(from.getClass())) {
			return (T) findComponent(from.name());
		} else {
			return super.latestVersionOf(from);
		}
	};
	
	@Override
	public Iterator<IType> iterator() {
		return arrayIterable(PrimitiveType.PROPLIST, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return PrimitiveType.PROPLIST.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return PrimitiveType.PROPLIST.typeName(special);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return PrimitiveType.PROPLIST.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return PrimitiveType.PROPLIST.containsType(type);
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return PrimitiveType.PROPLIST.containsAnyTypeOf(types);
	}

	@Override
	public int specificness() {
		return PrimitiveType.PROPLIST.specificness()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.PROPLIST;
	}
	
	@Override
	public void setTypeDescription(String description) {}
	
	/**
	 * Return the prototype of this proplist declaration. Obtained from the special 'Prototype' entry.
	 * @return The Prototype {@link ProplistDeclaration} or null, if either the 'Prototype' entry does not exist or the type of the Prototype expression does not denote a proplist declaration.
	 */
	public ProplistDeclaration prototype() {
		ExprElm prototypeExpr = null;
		for (Variable v : components) {
			if (v.name().equals(PROTOTYPE_KEY)) {
				prototypeExpr = v.initializationExpression();
				break;
			}
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
	public Collection<? extends IHasIncludes> includes(Index contextIndex, int options) {
		return IHasIncludes.Default.includes(contextIndex, this, options);
	}

	@Override
	public boolean doesInclude(Index contextIndex, IHasIncludes other) {
		List<IHasIncludes> includes = new ArrayList<IHasIncludes>(10);
		gatherIncludes(contextIndex, includes, GatherIncludesOptions.Recursive);
		return includes.contains(other);
	}
	
	@Override
	public boolean gatherIncludes(Index contextIndex, List<IHasIncludes> set, int options) {
		if (set.contains(this))
			return false;
		else
			set.add(this);
		IHasIncludes proto = prototype();
		if (proto != null)
			if ((options & GatherIncludesOptions.Recursive) == 0)
				set.add(proto);
			else
				proto.gatherIncludes(contextIndex, set, options);
		return true;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ProplistDeclaration && ((ProplistDeclaration)other).location().equals(this.location()))
			return true;
		return false;
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

}
