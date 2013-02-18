package net.arctics.clonk.parser;

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
import net.arctics.clonk.index.ISerializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IHasCode;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ast.ASTComparisonDelegate;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ControlFlow;
import net.arctics.clonk.parser.c4script.ast.ControlFlowException;
import net.arctics.clonk.parser.c4script.ast.ForStatement;
import net.arctics.clonk.parser.c4script.ast.MatchingPlaceholder;
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.IPrintable;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Base class for making expression trees
 */
public class ASTNode extends SourceLocation implements Cloneable, IPrintable, Serializable {

	public ASTNode() {}
	protected ASTNode(int start, int end) { super(start, end); }

	public static class Ticket implements ISerializationResolvable, Serializable, IASTVisitor<Object> {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final Declaration owner;
		private final String textRepresentation;
		private final int depth;
		private transient ASTNode found;
		private static int depth(ASTNode elm) {
			int depth;
			for (depth = 1, elm = elm.parent(); elm != null; elm = elm.parent(), depth++);
			return depth;
		}
		public Ticket(Declaration owner, ASTNode elm) {
			this.owner = owner;
			this.textRepresentation = elm.toString();
			this.depth = depth(elm);
		}
		@Override
		public Object resolve(Index index) {
			if (owner instanceof IHasCode) {
				if (owner instanceof IndexEntity)
					((IndexEntity) owner).requireLoaded();
				ASTNode code = ((IHasCode) owner).code();
				if (code != null)
					code.traverse(this, null);
				return found;
			}
			return null;
		}
		@Override
		public TraversalContinuation visitNode(ASTNode expression, Object context) {
			int ed = depth(expression);
			if (ed == depth && textRepresentation.equals(expression.toString())) {
				found = expression;
				return TraversalContinuation.Cancel;
			}
			else if (ed > depth)
				return TraversalContinuation.SkipSubElements;
			else
				return TraversalContinuation.Continue;
		}
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final ASTNode NULL_EXPR = new ASTNode();
	public static final ASTNode[] EMPTY_EXPR_ARRAY = new ASTNode[0];
	public static final Object EVALUATION_COMPLEX = new Object() {
		@Override
		public boolean equals(Object obj) {
			return false; // never!
		};
	};

	protected transient ASTNode parent, predecessorInSequence;

	/**
	 * Assign 'this' as the parent element of all elements returned by {@link #subElements()}.
	 * One should not forget calling this when creating sub elements.
	 */
	protected void assignParentToSubElements() {
		// Clone sub elements if they look like they might belong to some other parent
		ASTNode[] subElms = subElements();
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
		ASTNode[] clonedElms = ArrayUtil.map(subElements(), ASTNode.class, new IConverter<ASTNode, ASTNode>() {
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
	public <T> T parentOfType(Class<T> cls) {
		ASTNode e;
		for (e = parent(); e != null && !cls.isAssignableFrom(e.getClass()); e = e.parent());
		return (T) e;
	}

	@SuppressWarnings("unchecked")
	public <T> T thisOrParentOfType(Class<T> cls) {
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
	public void printPrependix(ASTNodePrinter output, int depth) {}

	/**
	 * Perform the actual intrinsic C4Script-printing for this kind of expression
	 * @param output Output writer
	 * @param depth Depth inherited from {@link #print(ASTNodePrinter, int)}
	 */
	public void doPrint(ASTNodePrinter output, int depth) {}

	/**
	 * Return the printed string for this node. Calls {@link #print(ASTNodePrinter, int)}.
	 * @return The printed string.
	 */
	public String printed() {
		StringBuilder builder = new StringBuilder();
		print(builder, 0);
		return builder.toString();
	}

	/**
	 * Print additional text appended to the actual expression text ({@link #doPrint(ASTNodePrinter, int)})
	 * @param output Output writer
	 * @param depth Depth inherited from {@link #print(ASTNodePrinter, int)}
	 */
	public void printAppendix(ASTNodePrinter output, int depth) {}

	/**
	 * Call all the printing methods in one bundle ({@link #printPrependix(ASTNodePrinter, int)}, {@link #doPrint(ASTNodePrinter, int)}, {@link #printAppendix(ASTNodePrinter, int)})
	 * The {@link ASTNodePrinter} is also given a chance to do its own custom printing using {@link ASTNodePrinter#doCustomPrinting(ASTNode, int)}
	 * @param output Output writer
	 * @param depth Depth determining the indentation level of the output
	 */
	public final void print(ASTNodePrinter output, int depth) {
		if (!output.doCustomPrinting(this, depth)) {
			this.printPrependix(output, depth);
			this.doPrint(output, depth);
			this.printAppendix(output, depth);
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
		ASTNode[] subElms = subElements();
		if (subElms != null)
			for (ASTNode e : subElms)
				if (e != null && e.hasSideEffects())
					return true;
		return false;
	}

	@Override
	public int getLength() {
		return end()-start();
	}

	@Override
	public int getOffset() {
		return start();
	}

	public int identifierStart() {
		return start();
	}

	public int identifierLength() {
		return getLength();
	}

	public final IRegion identifierRegion() {
		return new Region(identifierStart(), identifierLength());
	}

	public void setLocation(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void setLocation(IRegion r) {
		this.setLocation(r.getOffset(), r.getOffset()+r.getLength());
	}

	public void setPredecessorInSequence(ASTNode p) {
		predecessorInSequence = p;
	}

	public ASTNode predecessorInSequence() {
		return predecessorInSequence;
	}

	public ASTNode successorInSequence() {
		if (parent() instanceof Sequence)
			return ((Sequence)parent()).successorOfSubElement(this);
		else
			return null;
	}

	/**
	 * Return the sub elements of this {@link ASTNode}.
	 * The resulting array may contain nulls since the order of elements in the array is always the same but some expressions may allow leaving out some elements.
	 * A {@link ForStatement} does not require a condition, for example.
	 * @return The array of sub elements
	 */
	public ASTNode[] subElements() {
		return EMPTY_EXPR_ARRAY;
	}

	/**
	 * Set the sub elements. The passed arrays must contain elements in the same order as returned by {@link #subElements()}.
	 * @param elms The array of elements to assign to this element as sub elements
	 */
	public void setSubElements(ASTNode[] elms) {
		if (subElements().length > 0)
			System.out.println("setSubElements should be implemented when subElements() is implemented ("+getClass().getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Keeps applying optimize to the expression and its modified versions until an expression and its replacement are identical e.g. there is nothing to be modified anymore
	 * @param context
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public ASTNode exhaustiveOptimize(final ProblemReportingContext context) throws CloneNotSupportedException {
		ASTNode repl;
		for (ASTNode original = this; (repl = original.optimize(context)) != original; original = repl);
		return repl;
	}

	/**
	 * Returns an expression that is functionally equivalent to the original expression but modified to adhere to #strict/#strict 2 rules and be more readable.
	 * For example, And/Or function calls get replaced by operators, uses of the Call function get converted to direct function calls.
	 * This method tries to reuse existing objects and reassigns the parents of those objects so the original ExprElm tree might be invalid in subtle ways.
	 * @param context the script parser as a context for accessing the script the expression has been parsed in
	 * @return a #strict/#strict 2/readability enhanced version of the original expression
	 * @throws CloneNotSupportedException
	 */
	public ASTNode optimize(final ProblemReportingContext context) throws CloneNotSupportedException {
		return transformSubElements(new ITransformer() {
			@Override
			public Object transform(ASTNode prev, Object prevT, ASTNode expression) {
				if (expression == null)
					return expression;
				try {
					return expression.optimize(context);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					return expression;
				}
			}
		});
	}

	/**
	 * Interface for transforming an element in an expression.
	 * @author madeen
	 *
	 */
	public interface ITransformer {
		/**
		 * Canonical object to be returned by {@link #transform(ASTNode, ASTNode, ASTNode)} if the passed element is to be removed
		 * instead of being replaced.
		 */
		static final ASTNode REMOVE = new ASTNode();
		/**
		 * Transform the passed expression. For various purposes some context is supplied as well so the transformer can
		 * see the last expression it was passed and what it transformed it to.
		 * @param previousExpression The previous expression passed to the transformer
		 * @param previousTransformationResult The previous transformation result
		 * @param expression The expression to be transformed now.
		 * @return The transformed expression, the expression unmodified or some canonical object like {@link #REMOVE}
		 */
		Object transform(ASTNode previousExpression, Object previousTransformationResult, ASTNode expression);
	}

	/**
	 * Transform the expression using the supplied transformer.
	 * @param transformer The transformer whose {@link ITransformer#transform(ASTNode)} will be called on each sub element
	 * @return Either this element if no transformation took place or a clone with select transformations applied
	 * @throws CloneNotSupportedException
	 */
	public ASTNode transformSubElements(ITransformer transformer) {
		ASTNode[] subElms = subElements();
		ASTNode[] newSubElms = new ASTNode[subElms.length];
		boolean differentSubElms = false, removal = false;
		ASTNode prev = null;
		Object prevT = null;
		for (int i = 0, j = 0; i < subElms.length; i++, j++) {
			ASTNode s = subElms[i];
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
				ASTNode[] multi = (ASTNode[])t;
				ASTNode[] newNew = new ASTNode[j+multi.length+newSubElms.length-j-1];
				System.arraycopy(newSubElms, 0, newNew, 0, j);
				System.arraycopy(multi, 0, newNew, j, multi.length);
				newSubElms = newNew;
				j += multi.length-1;
			}
			prev = s;
			prevT = t;
		}
		if (differentSubElms) {
			ASTNode replacement = this.clone();
			if (removal)
				newSubElms = ArrayUtil.filter(newSubElms, new IPredicate<ASTNode>() {
					@Override
					public boolean test(ASTNode item) {
						return item != ITransformer.REMOVE;
					}
				});
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
			result = new BunchOfStatements((ASTNode[])result);
		if (!(result instanceof ASTNode))
			System.out.println("nope");
		return (ASTNode)result;
	}

	/**
	 * Traverses this expression by calling expressionDetected on the supplied {@link IASTVisitor} for the root expression and its sub elements.
	 * @param listener the expression listener
	 * @param context Context object
	 * @return flow control for the calling function
	 */
	public <T> TraversalContinuation traverse(IASTVisitor<T> listener, T context) {
		TraversalContinuation result = listener.visitNode(this, context);
		switch (result) {
		case Cancel:
			return TraversalContinuation.Cancel;
		case Continue: case TraverseSubElements:
			break;
		case SkipSubElements:
			return TraversalContinuation.Continue;
		}
		for (ASTNode sub : subElements()) {
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
	 * @param context {@link ProblemReportingContext} being contexty
	 * @return An object describing the referenced entity or null if no entity is referenced.
	 */
	public EntityRegion entityAt(int offset, ProblemReportingContext context) {
		return null;
	}

	/**
	 * Returns the expression tree as a C4Script expression string
	 * @param depth hint for indentation (only needed for statements)
	 * @return the C4Script expression string
	 */
	public final String printed(int depth) {
		StringBuilder builder = new StringBuilder();
		print(builder, depth);
		return builder.toString();
	}

	@Override
	public String toString() {
		return printed(0);
	}

	public Comment commentedOut() {
		String str = this.toString();
		return new Comment(str, str.contains("\n"), false); //$NON-NLS-1$
	}

	public ControlFlow controlFlow() {
		return ControlFlow.Continue;
	}

	public EnumSet<ControlFlow> possibleControlFlows() {
		return EnumSet.of(controlFlow());
	}

	public final boolean isAlways(boolean what, IEvaluationContext context) {
		Object ev = this.evaluateStatic(context);
		return ev != null && Boolean.valueOf(what).equals(PrimitiveType.BOOL.convert(ev));
	}

	public static boolean convertToBool(Object value) {
		return !Boolean.FALSE.equals(PrimitiveType.BOOL.convert(value));
	}

	public final boolean containedIn(ASTNode expression) {
		if (expression == null)
			return false;
		if (expression == this)
			return true;
			for (ASTNode e : expression.subElements())
				if (this.containedIn(e))
					return true;
		return false;
	}

	/**
	 * Return direct sub element of this ExprElm that contains elm.
	 * @param elm The expression that has one of the sub elements in its parent chain.
	 * @return Sub element containing elm or null.
	 */
	public ASTNode findSubElementContaining(ASTNode elm) {
		for (ASTNode subElm : subElements())
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
		for (ASTNode e : elm.subElements())
			offsetExprRegionRecursively(e, diff);
	}

	private void offsetExprRegionRecursivelyStartingAt(ASTNode elm, int diff) {
		boolean started = false;
		ASTNode[] elms = subElements();
		for (ASTNode e : elms)
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

		ASTNode[] subElms = subElements();
		ASTNode[] newSubElms = new ASTNode[subElms.length];
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

	private static final ASTComparisonDelegate NULL_DIFFERENCE_LISTENER = new ASTComparisonDelegate();

	/**
	 * Compare element with other element. Return true if there are no differences, false otherwise.
	 * @param other The other expression
	 * @param delegate Listener that gets notified when changes are found.
	 * @return Whether elements are equal or not.
	 */
	public final boolean compare(ASTNode other, ASTComparisonDelegate delegate) {
		if (other == null)
			return false;
		ASTNode oldLeft = delegate.left;
		ASTNode oldRight = delegate.right;
		delegate.left = this;
		delegate.right = other;
		try {

			if ((other == null || other.getClass() != this.getClass()) && !delegate.ignoreClassDifference())
				return false;
			if (delegate.considerDifferent() || !(equalAttributes(other) || delegate.ignoreAttributeDifference()))
				return false;

			ASTNode[] mine = this.subElements();
			ASTNode[] others = other.subElements();
			ASTNode[][] leftToRightMapping = delegate.compareSubElements(mine, others);

			if (leftToRightMapping != null) {
				delegate.applyLeftToRightMapping(mine, leftToRightMapping);
				return true;
			} else
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
			return compare((ASTNode) other, NULL_DIFFERENCE_LISTENER);
		else
			return false;
	}

	public Statement statement() {
		ASTNode p;
		for (p = this; p != null && !(p instanceof Statement); p = p.parent());
		return (Statement)p;
	}

	@SuppressWarnings("unchecked")
	protected <T extends ASTNode> void collectExpressionsOfType(List<T> list, Class<T> type) {
		if (type.isInstance(this))
			list.add((T) this);
		for (ASTNode e : subElements()) {
			if (e == null)
				continue;
			e.collectExpressionsOfType(list, type);
		}
	}

	public <T extends ASTNode> Iterable<T> collectionExpressionsOfType(Class<T> cls) {
		List<T> l = new LinkedList<T>();
		collectExpressionsOfType(l, cls);
		return l;
	}

	public ASTNode sequenceTilMe() {
		Sequence fullSequence = sequence();
		if (fullSequence != null) {
			List<ASTNode> elms = new LinkedList<ASTNode>();
			for (ASTNode e : fullSequence.subElements()) {
				elms.add(e);
				if (e == this)
					break;
			}
			return elms.size() == 1 ? this : new Sequence(elms);
		} else
			return this;
	}

	private Sequence sequence() {
		return as(parent(), Sequence.class);
	}

	public void postLoad(ASTNode parent, ProblemReportingContext context) {
		this.parent = parent;
		ASTNode prev = null;
		for (ASTNode e : subElements()) {
			if (e != null) {
				e.predecessorInSequence = prev;
				e.postLoad(this, context);
			}
			prev = e;
		}
	}

	/**
	 * Increment {@link #start()} and {@link #end()} by the specified amount. Also call {@link #incrementLocation(int)} recursively for {@link #subElements()}
	 * @param amount Amount to increment the location by
	 */
	public void incrementLocation(int amount) {
		setLocation(start+amount, start+amount);
		for (ASTNode e : subElements())
			if (e != null)
				e.incrementLocation(amount);
	}

	public static Object evaluateStatic(ASTNode element, IEvaluationContext context) {
		return element != null ? element.evaluateStatic(context) : null;
	}

	/**
	 * Check whether the given expression contains a reference to a constant.
	 * @param condition The expression to check
	 * @return Whether the expression contains a constant.
	 */
	public boolean containsConst() {
		if (this instanceof AccessVar && ((AccessVar)this).constCondition())
			return true;
		for (ASTNode expression : this.subElements())
			if(expression != null && expression.containsConst())
				return true;
		return false;
	}

	/**
	 * Return the {@link Declaration} this expression element is owned by. This may be the function whose body this element is contained in.
	 * @return The owning {@link Declaration}
	 */
	public Declaration owningDeclaration() {
		return parent != null ? parent.owningDeclaration() : null;
	}

	/**
	 * Compare this expression with another one, returning sub elements of the other expression that correspond to {@link Placeholder}s in this one.
	 * @param other Expression to match against
	 * @return Map mapping placeholder name to specific sub expressions in the passed expression
	 */
	public Map<String, Object> match(ASTNode other) {
		ASTNodeMatcher delegate = new ASTNodeMatcher();
		if (delegate.equal(this, other))
			return delegate.result != null ? delegate.result : Collections.<String, Object>emptyMap();
		else
			return null;
	}

	/**
	 * Return an instance of a specified class with its public fields set to results from {@link #match(ASTNode)}
	 * @param other The other expression to match against
	 * @param resultType Type of the resulting object
	 */
	public <T> boolean match(ASTNode other, T match) {
		Map<String, Object> matches = match(other);
		if (matches != null) try {
			for (Map.Entry<String, Object> kv : matches.entrySet())
				try {
					Field f = match.getClass().getField(kv.getKey());
					f.setAccessible(true); // my eyes
					f.set(match, as(kv.getValue(), f.getType()));
				} catch (NoSuchFieldException e) {
					continue; // ignore non-existing fields
				}
			return true;
		} catch (Exception e) {
			return false;
		} else
			return false;
	}

	/**
	 * Transform expression by replacing {@link Placeholder} nodes with corresponding values from the passed map.
	 * @param substitutions The map to use as source for {@link Placeholder} substitutions
	 * @return The transformed expression
	 */
	public ASTNode transform(final Map<String, Object> substitutions) {
		return transformRecursively(new ITransformer() {
			@Override
			public Object transform(ASTNode prev, Object prevT, ASTNode expression) {
				if (expression instanceof Placeholder) {
					MatchingPlaceholder mp = as(expression, MatchingPlaceholder.class);
					Object substitution = substitutions.get(((Placeholder)expression).entryName());
					if (substitution != null)
						return mp != null ? mp.transformSubstitution(substitution) : substitution;
					else
						return REMOVE;
				}
				return expression;
			}
		});
	}

	private transient IType inferredType;
	public IType inferredType() { return inferredType; }
	public void inferredType(IType type) { inferredType = type; }

	public transient Object temporaryProblemReportingObject;

	public final int sectionOffset() {
		Function f = parentOfType(Function.class);
		return f != null ? f.bodyLocation().start() : 0;
	}
	public IRegion absolute() { return this.region(sectionOffset()); }

}