package net.arctics.clonk.ast;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPrintable;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Base class for making expression trees
 */
public class ASTNode extends SourceLocation implements Cloneable, IPrintable, Serializable {

	public ASTNode() {}
	protected ASTNode(int start, int end) { super(start, end); }

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final ASTNode NULL_EXPR = new ASTNode();
	public static final ASTNode[] EMPTY_EXPR_ARRAY = new ASTNode[0];
	public static final Object EVALUATION_COMPLEX = new Object() {
		@Override
		public boolean equals(Object obj) {
			return false; // never!
		};
	};

	protected transient ASTNode parent, predecessor;

	/**
	 * Assign 'this' as the parent element of all elements returned by {@link #subElements()}.
	 * One should not forget calling this when creating sub elements.
	 */
	protected void assignParentToSubElements() {
		// Clone sub elements if they look like they might belong to some other parent
		final ASTNode[] subElms = traversalSubElements();
		boolean modified = false;
		for (int i = 0; i < subElms.length; i++) {
			ASTNode e = subElms[i];
			if (e != null) {
				if (e.parent() != null && e.parent() != this) {
					modified = true;
					subElms[i] = e = e.clone();
				}
				e.setParent(this);
			}
		}
		if (modified)
			setSubElements(subElms);
	}

	@Override
	public ASTNode clone() {
		ASTNode clone;
		clone = (ASTNode) super.clone();
		final ASTNode[] clonedElms = ArrayUtil.map(subElements(), ASTNode.class, new IConverter<ASTNode, ASTNode>() {
			@Override
			public ASTNode convert(ASTNode from) {
				if (from == null)
					return null;
				return from.clone();
			}
		});
		clone.setSubElements(clonedElms);
		return clone;
	}

	/**
	 * Return the parent of this expression.
	 * @return
	 */
	public ASTNode parent() { return parent; }

	/**
	 * Return the first parent in the parent chain of this expression that is of the given class
	 * @param cls The class to test for
	 * @return The first parent of the given class or null if such parent does not exist
	 */
	@SuppressWarnings("unchecked")
	public <T> T parent(Class<T> cls) {
		ASTNode e;
		for (e = parent(); e != null && !cls.isAssignableFrom(e.getClass()); e = e.parent());
		return (T) e;
	}

	public final boolean is(Class<? extends ASTNode> cls) {
		return cls.isInstance(this);
	}

	/**
	 * Same as {@link #parent(Class)}, but will return the last parent declaration matching the type instead of the first one.
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T> T topLevelParent(Class<T> type) {
		T result = null;
		for (ASTNode f = this; f != null; f = f.parent())
			if (type.isAssignableFrom(f.getClass()))
				result = (T) f;
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T thisOrParent(Class<T> cls) {
		ASTNode e;
		for (e = this; e != null && !cls.isAssignableFrom(e.getClass()); e = e.parent());
		return (T) e;
	}

	/**
	 * Set the parent of this expression.
	 * @param parent
	 */
	public void setParent(ASTNode parent) { this.parent = parent; }

	/**
	 * Print additional text prepended to the actual expression text ({@link #doPrint(ASTNodePrinter, int)})
	 * @param output The output writer to print the prependix to
	 * @param depth Print depth inherited from the underlying {@link #print(ASTNodePrinter, int)} call
	 */
	public void printPrefix(ASTNodePrinter output, int depth) {}

	/**
	 * Perform the actual intrinsic printing for this kind of expression
	 * @param output Output writer
	 * @param depth Depth inherited from {@link #print(ASTNodePrinter, int)}
	 */
	public void doPrint(ASTNodePrinter output, int depth) {}

	/**
	 * Return the printed string for this node. Calls {@link #print(ASTNodePrinter, int)}.
	 * @return The printed string.
	 */
	public String printed() {
		final StringBuilder builder = new StringBuilder();
		print(builder, 0);
		return builder.toString();
	}

	/**
	 * Print additional text appended to the actual expression text ({@link #doPrint(ASTNodePrinter, int)})
	 * @param output Output writer
	 * @param depth Depth inherited from {@link #print(ASTNodePrinter, int)}
	 */
	public void printSuffix(ASTNodePrinter output, int depth) {}

	/**
	 * Call all the printing methods in one bundle ({@link #printPrefix(ASTNodePrinter, int)}, {@link #doPrint(ASTNodePrinter, int)}, {@link #printSuffix(ASTNodePrinter, int)})
	 * The {@link ASTNodePrinter} is also given a chance to do its own custom printing using {@link ASTNodePrinter#doCustomPrinting(ASTNode, int)}
	 * @param output Output writer
	 * @param depth Depth determining the indentation level of the output
	 */
	public final void print(ASTNodePrinter output, int depth) {
		if (!output.doCustomPrinting(this, depth)) {
			this.printPrefix(output, depth);
			this.doPrint(output, depth);
			this.printSuffix(output, depth);
		}
	}

	@Override
	public final void print(final StringBuilder builder, int depth) {
		print(new AppendableBackedExprWriter(builder), depth);
	}

	public boolean isValidInSequence(ASTNode predecessor) { return predecessor == null; }
	public boolean isValidAtEndOfSequence() { return true; }
	public boolean allowsSequenceSuccessor(ASTNode successor) { return true; }

	public boolean hasSideEffects() {
		for (final ASTNode e : subElements())
			if (e != null && e.hasSideEffects())
				return true;
		return false;
	}

	@Override
	public int getLength() { return end()-start(); }
	@Override
	public int getOffset() { return start(); }
	/**
	 * If this node has an identifier return its offset relative to the {@link #sectionOffset()} (same as {@link #start()}).
	 * The default limplementation just returns {@link #start()}.
	 * @return Identifier offset
	 */
	public int identifierStart() { return start(); }
	/**
	 * If this node has an identifier return its length.
	 * The default limplementation just returns {@link #getLength()}.
	 * @return The identifier length
	 */
	public int identifierLength() { return getLength(); }
	/**
	 * Return {@link #identifierStart()} and {@link #identifierLength()} as a {@link IRegion}.
	 * @return The identifier region
	 */
	public final IRegion identifierRegion() { return new Region(identifierStart(), identifierLength()); }
	public void setLocation(int start, int end) { this.start = start; this.end = end; }
	public void setLocation(IRegion r) { this.setLocation(r.getOffset(), r.getOffset()+r.getLength()); }
	public void setPredecessor(ASTNode p) { predecessor = p; }
	public ASTNode predecessor() { return predecessor; }

	/**
	 * Return the sub elements of this {@link ASTNode}.
	 * The resulting array may contain nulls since the order of elements in the array is always the same but some expressions may allow leaving out some elements.
	 * @return The array of sub elements
	 */
	public ASTNode[] subElements() { return EMPTY_EXPR_ARRAY; }

	/**
	 * Set the sub elements. The passed arrays must contain elements in the same order as returned by {@link #subElements()}.
	 * @param elms The array of elements to assign to this element as sub elements
	 */
	public void setSubElements(ASTNode[] elms) {
		if (subElements().length > 0)
			System.out.println("setSubElements should be implemented when subElements() is implemented ("+getClass().getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Transform the expression using the supplied transformer.
	 * @param transformer The transformer whose {@link ITransformer#transform(ASTNode)} will be called on each sub element
	 * @return Either this element if no transformation took place or a clone with select transformations applied
	 * @throws CloneNotSupportedException
	 */
	public ASTNode transformSubElements(ITransformer transformer) {
		final ASTNode[] subElms = traversalSubElements();
		ASTNode[] newSubElms = new ASTNode[subElms.length];
		boolean differentSubElms = false, removal = false;
		ASTNode prev = null;
		Object prevT = null;
		for (int i = 0, j = 0; i < subElms.length; i++, j++) {
			final ASTNode s = subElms[i];
			Object t = transformer.transform(prev, prevT, s);
			if (t instanceof ASTNode[] && ((ASTNode[])t).length == 1)
				t = ((ASTNode[])t)[0];
			if (t instanceof ASTNode) {
				newSubElms[j] = (ASTNode)t;
				if (t != s) {
					differentSubElms = true;
					if (t == ITransformer.REMOVE)
						removal = true;
				}
			}
			else if (t instanceof ASTNode[]) {
				differentSubElms = true;
				final ASTNode[] multi = (ASTNode[])t;
				final ASTNode[] newNew = new ASTNode[j+multi.length+newSubElms.length-j-1];
				System.arraycopy(newSubElms, 0, newNew, 0, j);
				System.arraycopy(multi, 0, newNew, j, multi.length);
				newSubElms = newNew;
				j += multi.length-1;
			}
			prev = s;
			prevT = t;
		}
		if (differentSubElms) {
			final ASTNode replacement = this.clone();
			if (removal)
				newSubElms = ArrayUtil.filter(newSubElms, ITransformer.FILTER_REMOVE);
			replacement.setSubElements(newSubElms);
			replacement.assignParentToSubElements();
			return replacement;
		}
		return this; // nothing to be changed
	}

	/**
	 * Call {@link #transformSubElements(ITransformer)} but if transforming on this layer does not work let the transformer
	 * attempt to transform on nested layers.
	 * @param transformer The transformer
	 * @return Recursively transformed expression.
	 */
	public ASTNode transformRecursively(final ITransformer transformer) {
		Object result = new ITransformer() {
			@Override
			public Object transform(ASTNode prev, Object prevT, ASTNode expression) {
				expression = expression != null ? expression.transformSubElements(this) : null;
				return transformer.transform(prev, prevT, expression);
			}
		}.transform(null, null, this);
		if (result instanceof ASTNode[])
			result = new Sequence((ASTNode[])result);
		return as(result, ASTNode.class);
	}

	/**
	 * Traverses this expression by calling expressionDetected on the supplied {@link IASTVisitor} for the root expression and its sub elements.
	 * @param listener the expression listener
	 * @param context Context object
	 * @return flow control for the calling function
	 */
	public final <T> TraversalContinuation traverse(IASTVisitor<T> listener, T context) {
		TraversalContinuation result = listener.visitNode(this, context);
		switch (result) {
		case Cancel:
			return TraversalContinuation.Cancel;
		case Continue: case TraverseSubElements:
			break;
		case SkipSubElements:
			return TraversalContinuation.Continue;
		}
		for (final ASTNode sub : traversalSubElements()) {
			if (sub == null)
				continue;
			switch (sub.traverse(listener, context)) {
			case Continue:
				break;
			case TraverseSubElements: case Cancel:
				result = TraversalContinuation.Cancel;
				break;
			default:
				break;
			}
		}
		return result;
	}

	protected ASTNode[] traversalSubElements() { return subElements(); }

	/**
	 * Return this expression's region offset by a specified amount.
	 * @param offset The offset. If 0 the expression itself will be returned since it implements {@link IRegion}
	 * @return The region offset by the specified amount.
	 */
	public IRegion region(int offset) {
		return offset == 0 ? this : new Region(offset+start(), end()-start());
	}

	/**
	 * Return an entity that this expression refers to at the specified relative offset.
	 * @param offset The offset
	 * @param locator TODO
	 * @return An object describing the referenced entity or null if no entity is referenced.
	 */
	public EntityRegion entityAt(int offset, ExpressionLocator<?> locator) {
		return null;
	}

	/**
	 * Returns a string representation of this node.
	 * @param depth hint for indentation (only needed for statements)
	 * @return the expression string
	 */
	public final String printed(int depth) {
		final StringBuilder builder = new StringBuilder();
		print(builder, depth);
		return builder.toString();
	}

	@Override
	public String toString() {
		return printed(0);
	}

	public ControlFlow controlFlow() {
		return ControlFlow.Continue;
	}

	public EnumSet<ControlFlow> possibleControlFlows() {
		return EnumSet.of(controlFlow());
	}

	public final boolean containedIn(ASTNode expression) {
		if (expression == null)
			return false;
		for (ASTNode n = this; n != null; n = n.parent)
			if (n == expression)
				return true;
		return false;
	}

	/**
	 * Return direct sub element of this ExprElm that contains elm.
	 * @param elm The expression that has one of the sub elements in its parent chain.
	 * @return Sub element containing elm or null.
	 */
	public ASTNode findSubElementContaining(ASTNode elm) {
		for (final ASTNode subElm : traversalSubElements())
			if (subElm != null)
				if (elm.containedIn(subElm))
					return subElm;
		return null;
	}

	/**
	 * Returns whether this ExprElm represents a constant value.
	 * @return Whether constant or not.
	 */
	public boolean isConstant() {
		return false;
	}

	/**
	 * Rudimentary possibility for evaluating the expression. Only used for evaluating the value of the SetProperty("Name", ...) call in a Definition function (OpenClonk) right now
	 * @param context the context to evaluate in
	 * @return the result
	 */
	public Object evaluateStatic(IEvaluationContext context) {
		return EVALUATION_COMPLEX;
	}

	/**
	 * Evaluate expression. Used for the interpreter
	 * @return the result of the evaluation
	 */
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		return null;
	}

	protected static Object value(Object obj) {
		if (obj instanceof IVariable)
			obj = ((IVariable)obj).get();
		if (obj instanceof Integer)
			obj = Long.valueOf((Integer)obj);
		return obj;
	}

	/**
	 * Evaluate the expression without a context. Same calling {@link #evaluate(IEvaluationContext)} with null.
	 * @return The result of the evaluation
	 * @throws ControlFlowException
	 */
	public final Object evaluate() throws ControlFlowException {
		return evaluate(null);
	}

	/**
	 * Increment the offset of this expression by some amount.
	 * @param amount The amount
	 * @param start Whether the start offset is to be incremented
	 * @param end Whether the end offset is to be incremented
	 */
	protected void offsetExprRegion(int amount, boolean start, boolean end) {
		if (start)
			this.start += amount;
		if (end)
			this.end += amount;
	}

	private static void offsetExprRegionRecursively(ASTNode elm, int diff) {
		if (elm == null)
			return;
		elm.offsetExprRegion(diff, true, true);
		for (final ASTNode e : elm.subElements())
			offsetExprRegionRecursively(e, diff);
	}

	private void offsetExprRegionRecursivelyStartingAt(ASTNode elm, int diff) {
		boolean started = false;
		final ASTNode[] elms = traversalSubElements();
		for (final ASTNode e : elms)
			if (e == elm)
				started = true;
			else if (started)
				offsetExprRegionRecursively(e, diff);
		offsetExprRegion(diff, false, true);
		if (parent() != null)
			parent().offsetExprRegionRecursivelyStartingAt(this, diff);
	}

	/**
	 * Replace a sub element with another one. If the replacement element's tree relationships are to be preserved, a clone should be passed instead.
	 * @param element The element to replace
	 * @param with The replacement
	 * @param diff Difference between the original element's length and the replacement's length in characters
	 * @return Return this element, modified or not.
	 * @throws InvalidParameterException Thrown if the element is not actually a sub element of this element
	 */
	public ASTNode replaceSubElement(ASTNode element, ASTNode with, int diff) throws InvalidParameterException {
		assert(element != with);
		assert(element != null);
		assert(with != null);

		final ASTNode[] subElms = subElements();
		final ASTNode[] newSubElms = new ASTNode[subElms.length];
		boolean differentSubElms = false;
		for (int i = 0; i < subElms.length; i++)
			if (subElms[i] == element) {
				newSubElms[i] = with;
				with.setLocation(element);
				differentSubElms = true;
			} else {
				newSubElms[i] = subElms[i];
				if (differentSubElms)
					offsetExprRegionRecursively(subElms[i], diff);
			}
		if (differentSubElms) {
			setSubElements(newSubElms);
			with.setParent(this);
			offsetExprRegion(diff, false, true);
			if (parent() != null)
				parent().offsetExprRegionRecursivelyStartingAt(this, diff);
		} else
			throw new InvalidParameterException("element must actually be a subelement of this");
		return this;
	}

	/**
	 * Compare element with other element. Return true if there are no differences, false otherwise.
	 * @param other The other expression
	 * @param delegate Listener that gets notified when changes are found.
	 * @return Whether elements are equal or not.
	 */
	public final boolean compare(ASTNode other, ASTComparisonDelegate delegate) {
		if (other == null)
			return false;
		final ASTNode oldLeft = delegate.left;
		final ASTNode oldRight = delegate.right;
		delegate.left = this;
		delegate.right = other;
		try {

			if ((other == null || other.getClass() != this.getClass()) && !delegate.acceptClassDifference())
				return false;
			if (delegate.considerDifferent() || !(equalAttributes(other) || delegate.acceptAttributeDifference()))
				return false;

			final ASTNode[] mine = this.subElements();
			final ASTNode[] others = other.subElements();
			final ASTNode[][] leftToRightMapping = delegate.compareSubElements(mine, others);

			if (leftToRightMapping != null)
				return delegate.applyLeftToRightMapping(mine, leftToRightMapping);
			else
				return false;
		} finally {
			delegate.left = oldLeft;
			delegate.right = oldRight;
		}
	}

	protected boolean equalAttributes(ASTNode other) { return true; }

	@Override
	public boolean equals(Object other) {
		if (other != null && other.getClass() == this.getClass())
			return compare((ASTNode) other, new ASTComparisonDelegate((ASTNode)other));
		else
			return false;
	}

	public ASTNode sequenceTilMe() {
		final Sequence fullSequence = sequence();
		if (fullSequence != null) {
			final List<ASTNode> elms = new LinkedList<ASTNode>();
			for (final ASTNode e : fullSequence.subElements()) {
				elms.add(e);
				if (e == this)
					break;
			}
			return elms.size() == 1 ? this : new Sequence(elms);
		} else
			return this;
	}

	private Sequence sequence() { return as(parent(), Sequence.class); }

	public void postLoad(ASTNode parent) {
		this.parent = parent;
		for (final ASTNode e : traversalSubElements())
			if (e != null)
				e.postLoad(this);
	}

	/**
	 * Increment {@link #start()} and {@link #end()} by the specified amount. Also call {@link #incrementLocation(int)} recursively for {@link #subElements()}
	 * @param amount Amount to increment the location by
	 */
	public void incrementLocation(int amount) {
		setLocation(start+amount, start+amount);
		for (final ASTNode e : traversalSubElements())
			if (e != null)
				e.incrementLocation(amount);
	}

	public static Object evaluateStatic(ASTNode element, IEvaluationContext context) {
		return element != null ? element.evaluateStatic(context) : null;
	}

	/**
	 * Return the {@link Declaration} this expression element is owned by. This may be the function whose body this element is contained in.
	 * @return The owning {@link Declaration}
	 */
	public Declaration owner() {
		return parent != null ? parent.owner() : null;
	}

	/**
	 * Compare this expression with another one, returning sub elements of the other expression that correspond to {@link Placeholder}s in this one.
	 * @param other Expression to match against
	 * @return Map mapping placeholder name to specific sub expressions in the passed expression
	 */
	public Map<String, Object> match(ASTNode other) {
		final ASTNodeMatcher delegate = new ASTNodeMatcher(other);
		if (delegate.equal(this, other))
			return delegate.result != null ? delegate.result : Collections.<String, Object>emptyMap();
		else
			return null;
	}

	/**
	 * Put matches of a {@link #match(ASTNode)} into an arbitrary object that provides
	 * fields with names corresponding to the matches.
	 * @param other The other expression to match against
	 * @param match The object to put matches into
	 */
	public boolean match(ASTNode other, Object match) {
		final Map<String, Object> matches = match(other);
		if (matches != null) try {
			for (final Map.Entry<String, Object> kv : matches.entrySet())
				try {
					final Field f = match.getClass().getField(kv.getKey());
					Object val = kv.getValue();
					if (!f.getType().isArray() && val instanceof Object[])
						val = ((Object[])val).length > 0 ? ((Object[])val)[0] : null;
					f.setAccessible(true); // my eyes
					f.set(match, as(val, f.getType()));
				} catch (final NoSuchFieldException e) {
					continue; // ignore non-existing fields
				}
			return true;
		} catch (final Exception e) {
			return false;
		} else
			return false;
	}

	/**
	 * Transform expression by replacing {@link Placeholder} nodes with corresponding values from the passed map.
	 * @param substitutions The map to use as source for {@link Placeholder} substitutions
	 * @return The transformed expression
	 */
	public ASTNode transform(final Map<String, Object> substitutions, final Object cookie) {
		return transformRecursively(new ITransformer() {
			@Override
			public Object transform(ASTNode prev, Object prevT, ASTNode expression) {
				if (expression instanceof Placeholder) {
					final MatchingPlaceholder mp = as(expression, MatchingPlaceholder.class);
					final Object substitution = substitutions.get(((Placeholder)expression).entryName());
					if (substitution != null)
						return mp != null ? mp.transformSubstitution(substitution, cookie) : substitution;
					else
						return REMOVE;
				}
				return expression;
			}
		});
	}

	public long globalIdentifier() {
		final IASTSection section = section();
		return section != null ? section.globalIdentifier()+localIdentifier : -1;
	}

	private transient int localIdentifier = -1;
	public final int localIdentifier() { return localIdentifier; }
	public final void localIdentifier(int v) { localIdentifier = v; }

	public final int sectionOffset() {
		final IASTSection f = section();
		return f != null ? f.absoluteOffset() : 0;
	}
	public IASTSection section() { return parent(IASTSection.class); }
	public IRegion absolute() { return this.region(sectionOffset()); }

	public void shift(int localInsertionOffset, int amount) {
		for (final ASTNode node : subElements())
			if (node != null) {
				if (node.start() >= localInsertionOffset)
					node.setStart(node.start()+amount);
				if (node.end() >= localInsertionOffset)
					node.setEnd(node.end()+amount);
			}
		final IASTSection section = section();
		if (section != null)
			((ASTNode)section).shift(localInsertionOffset, amount);
	}

}