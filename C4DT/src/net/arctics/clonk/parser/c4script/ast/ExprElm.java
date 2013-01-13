package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IPostLoadable;
import net.arctics.clonk.index.ISerializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IHasCode;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.resource.ProjectSettings.Typing;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.IPrintable;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Base class for making expression trees
 */
public class ExprElm extends SourceLocation implements Cloneable, IPrintable, Serializable, IPostLoadable<ExprElm, DeclarationObtainmentContext> {

	public static class Ticket implements ISerializationResolvable, Serializable, IASTVisitor {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final Declaration owner;
		private final String textRepresentation;
		private final int depth;
		private transient ExprElm found;
		private static int depth(ExprElm elm) {
			int depth;
			for (depth = 1, elm = elm.parent(); elm != null; elm = elm.parent(), depth++);
			return depth;
		}
		public Ticket(Declaration owner, ExprElm elm) {
			this.owner = owner;
			this.textRepresentation = elm.toString();
			this.depth = depth(elm);
		}
		@Override
		public Object resolve(Index index) {
			if (owner instanceof IHasCode) {
				if (owner instanceof IndexEntity)
					((IndexEntity) owner).requireLoaded();
				ExprElm code = ((IHasCode) owner).code();
				if (code != null)
					code.traverse(this, null);
				return found;
			}
			return null;
		}
		@Override
		public TraversalContinuation visitExpression(ExprElm expression, C4ScriptParser parser) {
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

	public static final ExprElm NULL_EXPR = new ExprElm();
	public static final ExprElm[] EMPTY_EXPR_ARRAY = new ExprElm[0];
	public static final Object EVALUATION_COMPLEX = new Object() {
		@Override
		public boolean equals(Object obj) {
			return false; // never!
		};
	};
	
	public static final int PROPERLY_FINISHED = 1;
	public static final int STATEMENT_REACHED = 2;
	public static final int MISPLACED = 4;

	/**
	 * Create a new expression to signify some non-expression at a given location.
	 * @param start The start of the location to mark as 'missing an expression'
	 * @param length The length of the null-expression
	 * @param parser Parser used to adjust the expression location to be relative to the function body
	 * @return The constructed null expression
	 */
	public static final ExprElm whitespace(int start, int length, C4ScriptParser parser) {
		ExprElm result = new Whitespace();
		parser.setExprRegionRelativeToFuncBody(result, start, start+length);
		return result;
	}
	
	private ExprElm parent, predecessorInSequence;
	private int flags = PROPERLY_FINISHED;
	
	public final boolean flagsEnabled(int flags) {
		return (this.flags & flags) != 0;
	}
	
	public final void setFlagsEnabled(int flags, boolean enabled) {
		if (enabled)
			this.flags |= flags;
		else
			this.flags &= ~flags;
	}
	
	public boolean isFinishedProperly() {return flagsEnabled(PROPERLY_FINISHED);}
	public void setFinishedProperly(boolean finished) {setFlagsEnabled(PROPERLY_FINISHED, finished);}

	/**
	 * Assign 'this' as the parent element of all elements returned by {@link #subElements()}.
	 * One should not forget calling this when creating sub elements.
	 */
	protected void assignParentToSubElements() {
		// Clone sub elements if they look like they might belong to some other parent
		ExprElm[] subElms = subElements();
		boolean modified = false;
		for (int i = 0; i < subElms.length; i++) {
			ExprElm e = subElms[i];
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
	public ExprElm clone() {
		ExprElm clone;
		try {
			clone = (ExprElm) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Clone not supported which really shouldn't happen");
		}
		ExprElm[] clonedElms = ArrayUtil.map(subElements(), ExprElm.class, new IConverter<ExprElm, ExprElm>() {
			@Override
			public ExprElm convert(ExprElm from) {
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
	public ExprElm parent() {
		return parent;
	}
	
	/**
	 * Return the first parent in the parent chain of this expression that is of the given class
	 * @param cls The class to test for
	 * @return The first parent of the given class or null if such parent does not exist
	 */
	@SuppressWarnings("unchecked")
	public <T> T parentOfType(Class<T> cls) {
		ExprElm e;
		for (e = this; e != null && !cls.isAssignableFrom(e.getClass()); e = e.parent());
		return (T) e;
	}

	/**
	 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed. 
	 * @param parser The parser used to create the warning marker if conditions are met (!{@link #hasSideEffects()})
	 */
	public void warnIfNoSideEffects(C4ScriptParser parser) {
		if (parent() instanceof IterateArrayStatement && ((IterateArrayStatement)parent()).elementExpr() == this)
			return;
		if (!hasSideEffects())
			parser.warning(ParserErrorCode.NoSideEffects, this, 0);
	}

	/**
	 * Set the parent of this expression.
	 * @param parent
	 */
	public void setParent(ExprElm parent) {
		this.parent = parent;
	}
	
	/**
	 * Print additional text prepended to the actual expression text ({@link #doPrint(ExprWriter, int)})
	 * @param output The output writer to print the prependix to
	 * @param depth Print depth inherited from the underlying {@link #print(ExprWriter, int)} call
	 */
	public void printPrependix(ExprWriter output, int depth) {}
	
	/**
	 * Perform the actual intrinsic C4Script-printing for this kind of expression
	 * @param output Output writer
	 * @param depth Depth inherited from {@link #print(ExprWriter, int)}
	 */
	public void doPrint(ExprWriter output, int depth) {}
	
	/**
	 * Print additional text appended to the actual expression text ({@link #doPrint(ExprWriter, int)})
	 * @param output Output writer
	 * @param depth Depth inherited from {@link #print(ExprWriter, int)}
	 */
	public void printAppendix(ExprWriter output, int depth) {}
	
	/**
	 * Call all the printing methods in one bundle ({@link #printPrependix(ExprWriter, int)}, {@link #doPrint(ExprWriter, int)}, {@link #printAppendix(ExprWriter, int)})
	 * The {@link ExprWriter} is also given a chance to do its own custom printing using {@link ExprWriter#doCustomPrinting(ExprElm, int)}
	 * @param output Output writer
	 * @param depth Depth determining the indentation level of the output
	 */
	public final void print(ExprWriter output, int depth) {
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

	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) { return predecessor == null; }
	public boolean isValidAtEndOfSequence(C4ScriptParser context) { return true; }
	public boolean allowsSequenceSuccessor(C4ScriptParser context, ExprElm successor) { return true; }
	
	/**
	 * Return type of the expression, adjusting the result of obtainType under some circumstances
	 * @param context Parser acting as the context (supplying current function, script begin parsed etc.)
	 * @return The type of the expression
	 */
	public IType type(DeclarationObtainmentContext context) {
		IType urt = unresolvedType(context);
		return TypeUtil.resolve(urt, context, callerType(context));
	}

	/**
	 * Return type of object the expression is executed in
	 * @param context Context information
	 * @return The type
	 */
	protected IType callerType(DeclarationObtainmentContext context) {
		IType predType = unresolvedPredecessorType(context);
		return defaulting(predType, context.script());
	}
	
	/**
	 * Overridable method to obtain the type of the declaration.
	 * @param context Parser acting as the context (supplying current function, script begin parsed etc.)
	 * @return The type of the expression
	 */
	public IType unresolvedType(DeclarationObtainmentContext context) {
		return context.queryTypeOfExpression(this, PrimitiveType.UNKNOWN);
	}
	
	public final Definition guessObjectType(DeclarationObtainmentContext context) {
		return Utilities.as(Definition.scriptFrom(type(context)), Definition.class);
	}

	public boolean isModifiable(C4ScriptParser context) {
		return true;
	}

	public boolean hasSideEffects() {
		ExprElm[] subElms = subElements();
		if (subElms != null)
			for (ExprElm e : subElms)
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

	public void setExprRegion(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public void setExprRegion(IRegion r) {
		this.setExprRegion(r.getOffset(), r.getOffset()+r.getLength());
	}
	
	/**
	 * Reset some cached state so {@link #reportProblems(C4ScriptParser)} is more likely to not report on errors that have since been fixed.
	 * Also called recursively on {@link #subElements()}.
	 * @param parser context
	 */
	public void reconsider(C4ScriptParser parser) {
		for (ExprElm e : this.subElements())
			if (e != null)
				e.reconsider(parser);
	}
	
	/**
	 * Give ExprElm a chance to complain about things.
	 * @param parser The parser to report errors to, preferably via some variant of {@link C4ScriptParser#marker(ParserErrorCode, int, int, int, int, Object...)}
	 * @throws ParsingException
	 */
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		// i'm totally error-free
	}
	
	/**
	 * Returning true tells the {@link C4ScriptParser} to not recursively call {@link #reportProblems(C4ScriptParser)} on {@link #subElements()} 
	 * @return Do you just show up, play the music,
	 */
	public boolean skipReportingProblemsForSubElements() {return false;}

	public void setPredecessorInSequence(ExprElm p) {
		predecessorInSequence = p;
	}

	public ExprElm predecessorInSequence() {
		return predecessorInSequence;
	}
	
	public ExprElm successorInSequence() {
		if (parent() instanceof Sequence)
			return ((Sequence)parent()).successorOfSubElement(this);
		else
			return null;
	}

	/**
	 * Return the sub elements of this {@link ExprElm}.
	 * The resulting array may contain nulls since the order of elements in the array is always the same but some expressions may allow leaving out some elements.
	 * A {@link ForStatement} does not require a condition, for example. 
	 * @return The array of sub elements
	 */
	public ExprElm[] subElements() {
		return EMPTY_EXPR_ARRAY;
	}

	/**
	 * Set the sub elements. The passed arrays must contain elements in the same order as returned by {@link #subElements()}.
	 * @param elms The array of elements to assign to this element as sub elements
	 */
	public void setSubElements(ExprElm[] elms) {
		if (subElements().length > 0)
			System.out.println("setSubElements should be implemented when subElements() is implemented ("+getClass().getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Keeps applying optimize to the expression and its modified versions until an expression and its replacement are identical e.g. there is nothing to be modified anymore
	 * @param context
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public ExprElm exhaustiveOptimize(C4ScriptParser context) throws CloneNotSupportedException {
		ExprElm repl;
		for (ExprElm original = this; (repl = original.optimize(context)) != original; original = repl);
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
	public ExprElm optimize(final C4ScriptParser context) throws CloneNotSupportedException {
		return transformSubElements(new ITransformer() {
			@Override
			public Object transform(ExprElm prev, Object prevT, ExprElm expression) {
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
		 * Canonical object to be returned by {@link #transform(ExprElm, ExprElm, ExprElm)} if the passed element is to be removed
		 * instead of being replaced.
		 */
		static final ExprElm REMOVE = new ExprElm();
		/**
		 * Transform the passed expression. For various purposes some context is supplied as well so the transformer can
		 * see the last expression it was passed and what it transformed it to.
		 * @param previousExpression The previous expression passed to the transformer
		 * @param previousTransformationResult The previous transformation result
		 * @param expression The expression to be transformed now.
		 * @return The transformed expression, the expression unmodified or some canonical object like {@link #REMOVE}
		 */
		Object transform(ExprElm previousExpression, Object previousTransformationResult, ExprElm expression);
	}
	
	/**
	 * Transform the expression using the supplied transformer.
	 * @param transformer The transformer whose {@link ITransformer#transform(ExprElm)} will be called on each sub element
	 * @return Either this element if no transformation took place or a clone with select transformations applied
	 * @throws CloneNotSupportedException
	 */
	public ExprElm transformSubElements(ITransformer transformer) {
		ExprElm[] subElms = subElements();
		ExprElm[] newSubElms = new ExprElm[subElms.length];
		boolean differentSubElms = false, removal = false;
		ExprElm prev = null;
		Object prevT = null;
		for (int i = 0, j = 0; i < subElms.length; i++, j++) {
			ExprElm s = subElms[i];
			Object t = transformer.transform(prev, prevT, s);
			if (t instanceof ExprElm[] && ((ExprElm[])t).length == 1)
				t = ((ExprElm[])t)[0];
			if (t instanceof ExprElm) {
				newSubElms[j] = (ExprElm)t;
				if (t != s) {
					differentSubElms = true;
					if (t == ITransformer.REMOVE)
						removal = true;
				}
			}
			else if (t instanceof ExprElm[]) {
				differentSubElms = true;
				ExprElm[] multi = (ExprElm[])t;
				ExprElm[] newNew = new ExprElm[j+multi.length+newSubElms.length-j-1];
				System.arraycopy(subElms, 0, newNew, 0, j);
				System.arraycopy(multi, 0, newNew, j, multi.length);
				newSubElms = newNew;
				j += multi.length-1;
			}
			prev = s;
			prevT = t;
		}
		if (differentSubElms) {
			ExprElm replacement = this.clone();
			if (removal)
				newSubElms = ArrayUtil.filter(newSubElms, new IPredicate<ExprElm>() {
					@Override
					public boolean test(ExprElm item) {
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
	public ExprElm transformRecursively(final ITransformer transformer) {
		return (ExprElm)new ITransformer() {
			@Override
			public Object transform(ExprElm prev, Object prevT, ExprElm expression) {
				expression = expression != null ? expression.transformSubElements(this) : null;
				return transformer.transform(prev, prevT, expression);
			}
		}.transform(null, null, this);
	}

	/**
	 * Returns whether the expression can be converted to the given type
	 * @param otherType the type to test convertibility to
	 * @return true if conversion is possible or false if not
	 */
	public static boolean canBeConvertedTo(IType type, IType otherType, C4ScriptParser context) {
		// 5555 is ID
		return type == PrimitiveType.INT && otherType == PrimitiveType.ID && context.engine().settings().integersConvertibleToIDs;
	}

	/**
	 * Return whether this expression is valid as a value of the specified type.
	 * @param type The type to test against
	 * @param context Script parser context
	 * @return True if valid, false if not.
	 */
	public boolean validForType(IType type, C4ScriptParser context) {
		if (type == null)
			return true;
		IType myType = type(context);
		return type.canBeAssignedFrom(myType);
	}

	/**
	 * Traverses this expression by calling expressionDetected on the supplied IExpressionListener for the root expression and its sub elements.
	 * @param listener the expression listener
	 * @param parser the parser as context
	 * @return flow control for the calling function
	 */
	public TraversalContinuation traverse(IASTVisitor listener, C4ScriptParser parser) {
		TraversalContinuation result = listener.visitExpression(this, parser);
		switch (result) {
		case Cancel:
			return TraversalContinuation.Cancel;
		case Continue: case TraverseSubElements:
			break;
		case SkipSubElements:
			return TraversalContinuation.Continue;
		}
		for (ExprElm sub : subElements()) {
			if (sub == null)
				continue;
			switch (sub.traverse(listener, parser)) {
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
	 * @param parser Script parser context
	 * @return An object describing the referenced entity or null if no entity is referenced.
	 */
	public EntityRegion entityAt(int offset, C4ScriptParser parser) {
		return null;
	}

	private static final ExprElm[] exprElmsForTypes = new ExprElm[PrimitiveType.values().length];

	/**
	 * Returns a canonical {@link ExprElm} object for the given type such that its {@link #type(DeclarationObtainmentContext)} returns the given type
	 * @param type the type to return a canonical ExprElm of
	 * @return the canonical {@link ExprElm} object
	 */
	public static ExprElm exprElmForPrimitiveType(final PrimitiveType type) {
		if (exprElmsForTypes[type.ordinal()] == null)
			exprElmsForTypes[type.ordinal()] = new ExprElm() {
				/**
				 * 
				 */
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

				@Override
				public IType unresolvedType(DeclarationObtainmentContext context) {
					return type;
				}
			};
		return exprElmsForTypes[type.ordinal()];
	}

	/**
	 * Returns the expression tree as a C4Script expression string
	 * @param depth hint for indentation (only needed for statements)
	 * @return the C4Script expression string
	 */
	public String toString(int depth) {
		StringBuilder builder = new StringBuilder();
		print(builder, depth);
		return builder.toString();
	}

	@Override
	public String toString() {
		return toString(0);
	}
	
	public Comment commentedOut() {
		String str = this.toString();
		return new Comment(str, str.contains("\n"), false); //$NON-NLS-1$
	}
	
	public boolean typingJudgement(IType type, C4ScriptParser context, TypingJudgementMode mode) {
		ITypeInfo info;
		switch (mode) {
		case Expect:
			info = context.requestTypeInfo(this);
			if (info != null)
				if (info.type() == PrimitiveType.UNKNOWN || info.type() == PrimitiveType.ANY) {
					info.storeType(type);
					return true;
				} else
					return false;
			return true;
		case Force:
			info = context.requestTypeInfo(this);
			if (info != null) {
				info.storeType(type);
				return true;
			} else
				return false;
		case Hint:
			info = context.queryTypeInfo(this);
			return info == null || info.hint(type);
		case Unify:
			info = context.requestTypeInfo(this);
			if (info != null) {
				info.storeType(TypeUnification.unify(info.type(), type));
				return true;
			} else
				return false;
		default:
			return false;
		}
	}
	
	public void assignment(ExprElm rightSide, C4ScriptParser context) {
		if (context.staticTyping() == Typing.Static) {
			IType left = this.type(context);
			IType right = rightSide.type(context); 
			if (!left.canBeAssignedFrom(right))
				context.incompatibleTypes(rightSide, left, right);
		} else {
			this.typingJudgement(rightSide.type(context), context, TypingJudgementMode.Force);
			context.linkTypesOf(this, rightSide);
		}
	}

	public ControlFlow controlFlow() {
		return ControlFlow.Continue;
	}
	
	public EnumSet<ControlFlow> possibleControlFlows() {
		return EnumSet.of(controlFlow()); 
	}

	public final boolean isAlways(boolean what, IEvaluationContext context) {
		Object ev = this.evaluateAtParseTime(context);
		return ev != null && Boolean.valueOf(what).equals(PrimitiveType.BOOL.convert(ev));
	}
	
	public static boolean convertToBool(Object value) {
		return !Boolean.FALSE.equals(PrimitiveType.BOOL.convert(value));
	}

	public final boolean containedIn(ExprElm expression) {
		if (expression == this)
			return true;
		try {
			for (ExprElm e : expression.subElements())
				if (this.containedIn(e))
					return true;
		} catch (NullPointerException e) {
			System.out.println(expression);
		}
		return false;
	}
	
	/**
	 * Return direct sub element of this ExprElm that contains elm.
	 * @param elm The expression that has one of the sub elements in its parent chain.
	 * @return Sub element containing elm or null.
	 */
	public ExprElm findSubElementContaining(ExprElm elm) {
		for (ExprElm subElm : subElements())
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

	public ITypeInfo createTypeInfo(C4ScriptParser parser) {
		ITypeable d = GenericTypeInfo.typeableFromExpression(this, parser);
		if (d != null && !d.staticallyTyped())
			return new GenericTypeInfo(this, parser);
		return null;
	}
	
	/**
	 * Rudimentary possibility for evaluating the expression. Only used for evaluating the value of the SetProperty("Name", ...) call in a Definition function (OpenClonk) right now
	 * @param context the context to evaluate in
	 * @return the result
	 */
	public Object evaluateAtParseTime(IEvaluationContext context) {
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
	
	private static void offsetExprRegionRecursively(ExprElm elm, int diff) {
		if (elm == null)
			return;
		elm.offsetExprRegion(diff, true, true);
		for (ExprElm e : elm.subElements())
			offsetExprRegionRecursively(e, diff);
	}
	
	private void offsetExprRegionRecursivelyStartingAt(ExprElm elm, int diff) {
		boolean started = false;
		ExprElm[] elms = subElements();
		for (ExprElm e : elms)
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
	public ExprElm replaceSubElement(ExprElm element, ExprElm with, int diff) throws InvalidParameterException {
		assert(element != with);
		assert(element != null);
		assert(with != null);
		
		ExprElm[] subElms = subElements();
		ExprElm[] newSubElms = new ExprElm[subElms.length];
		boolean differentSubElms = false;
		for (int i = 0; i < subElms.length; i++)
			if (subElms[i] == element) {
				newSubElms[i] = with;
				with.setExprRegion(element);
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
	
	private static final IASTComparisonDelegate NULL_DIFFERENCE_LISTENER = new IASTComparisonDelegate() {
		@Override
		public DifferenceHandling differs(ExprElm a, ExprElm b, Object what) {
			// the humanity
			return DifferenceHandling.Differs;
		}

		@Override
		public boolean optionEnabled(Option option) {
			switch (option) {
			case CheckForIdentity:
				return true;
			default:
				return false;
			}
		}

		@Override
		public void wildcardMatched(Wildcard wildcard, ExprElm expression) {
		}
	};
	
	/**
	 * Compare element with other element. Return true if there are no differences, false otherwise.
	 * @param other The other expression
	 * @param delegate Listener that gets notified when changes are found.
	 * @return Whether elements are equal or not.
	 */
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate delegate) {
		if (other != null && (other.getClass() == this.getClass() || delegate.differs(this, other, IASTComparisonDelegate.CLASS) == DifferenceHandling.Equal)) {
			ExprElm[] mySubElements = this.subElements();
			ExprElm[] otherSubElements = other.subElements();
			int myIndex, otherIndex;
			for (myIndex = 0, otherIndex = 0; myIndex < mySubElements.length && otherIndex < otherSubElements.length; myIndex++, otherIndex++) {
				ExprElm mine = mySubElements[myIndex];
				ExprElm others = otherSubElements[otherIndex];
				
				// compare elements, taking the possibility into account that one or both of the elements might be null
				// if only one of both is null, the listener gets to decide what to do now based on a differs call with
				// the what parameter being set to the index of the null element in the respective array
				DifferenceHandling handling;
				if (mine != null && others != null)
					handling = mine.compare(others, delegate);
				else if (mine == null && others != null)
					handling = delegate.differs(mine, others, myIndex);
				else if (mine != null && others == null)
					handling = delegate.differs(mine, others, otherIndex);
				else // a != null && b != null
					handling = DifferenceHandling.Equal;
				
				switch (handling) {
				case Differs:
					return DifferenceHandling.Differs;
				case IgnoreLeftSide:
					// decrement index for other so the current other element will be compared to the next my element 
					otherIndex--;
					break;
				case IgnoreRightSide:
					// vice versa
					myIndex--;
					break;
				default:
					break;
				}
			}
			if (myIndex == mySubElements.length && otherIndex == otherSubElements.length)
				return DifferenceHandling.Equal;
			else switch (delegate.differs(this, other, IASTComparisonDelegate.SUBELEMENTS_LENGTH)) {
			case IgnoreLeftSide: case IgnoreRightSide:
				return DifferenceHandling.Equal;
			default:
				return DifferenceHandling.Differs;
			}
		} else
			return DifferenceHandling.Differs;
	}
	
	/**
	 * Stored Type Information that applies the stored type information by determining the {@link ITypeable} being referenced by some arbitrary {@link ExprElm} and setting its type.
	 * @author madeen
	 *
	 */
	protected static final class GenericTypeInfo extends TypeInfo {
		private final ExprElm expression;
		
		public GenericTypeInfo(ExprElm referenceElm, C4ScriptParser parser) {
			super();
			this.expression = referenceElm;
			ITypeable typeable = typeableFromExpression(referenceElm, parser);
			if (typeable != null)
				this.type = typeable.type();
		}
		
		private static final IASTComparisonDelegate IDENTITY_DIFFERENCE_LISTENER = new IASTComparisonDelegate() {
			@Override
			public DifferenceHandling differs(ExprElm a, ExprElm b, Object what) {
				// ok
				return DifferenceHandling.Differs;
			}

			@Override
			public boolean optionEnabled(Option option) {
				switch (option) {
				case CheckForIdentity:
					return true;
				default:
					return false;
				}
			}

			@Override
			public void wildcardMatched(Wildcard wildcard, ExprElm expression) {
			}
			
		};

		@Override
		public boolean storesTypeInformationFor(ExprElm expr, C4ScriptParser parser) {
			if (expr instanceof AccessDeclaration && expression instanceof AccessDeclaration && ((AccessDeclaration)expr).declaration() == ((AccessDeclaration)expression).declaration())
				return !Utilities.isAnyOf(((AccessDeclaration)expr).declaration(), parser.cachedEngineDeclarations().VarAccessFunctions);
			ExprElm chainA, chainB;
			for (chainA = expr, chainB = expression; chainA != null && chainB != null; chainA = chainA.predecessorInSequence(), chainB = chainB.predecessorInSequence())
				if (!chainA.compare(chainB, IDENTITY_DIFFERENCE_LISTENER).isEqual())
					return false;
			return chainA == null || chainB == null;
		}

		@Override
		public boolean refersToSameExpression(ITypeInfo other) {
			if (other instanceof GenericTypeInfo)
				return ((GenericTypeInfo)other).expression.equals(expression);
			else
				return false;
		}
		
		public static ITypeable typeableFromExpression(ExprElm referenceElm, C4ScriptParser parser) {
			EntityRegion decRegion = referenceElm.entityAt(referenceElm.getLength()-1, parser);
			if (decRegion != null && decRegion.entityAs(ITypeable.class) != null)
				return decRegion.entityAs(ITypeable.class);
			else
				return null;
		}
		
		@Override
		public void apply(boolean soft, C4ScriptParser parser) {
			ITypeable typeable = typeableFromExpression(expression, parser);
			if (typeable != null) {
				// don't apply typing to non-local things if only applying type information softly
				// this prevents assigning types to instance variables when only hovering over some function or something like that
				if (soft && !typeable.isLocal())
					return;
				// only set types of declarations inside the current index so definition references of one project
				// don't leak into a referenced base project (ClonkMars def referenced in ClonkRage or something)
				Index index = typeable.index();
				if (index == null || index != parser.script().index())
					return;

				typeable.expectedToBeOfType(type, TypingJudgementMode.Expect);
			}
		}
		
		@Override
		public String toString() {
			return String.format("[%s: %s]", expression.toString(), type.typeName(true));
		}
		
	}
	
	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass())
			return compare((ExprElm) other, NULL_DIFFERENCE_LISTENER).isEqual();
		else
			return false;
	}
	
	public Statement statement() {
		ExprElm p;
		for (p = this; p != null && !(p instanceof Statement); p = p.parent());
		return (Statement)p;
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends ExprElm> void collectExpressionsOfType(List<T> list, Class<T> type) {
		if (type.isInstance(this))
			list.add((T) this);
		for (ExprElm e : subElements()) {
			if (e == null)
				continue;
			e.collectExpressionsOfType(list, type);
		}
	}
	
	public <T extends ExprElm> Iterable<T> collectionExpressionsOfType(Class<T> cls) {
		List<T> l = new LinkedList<T>();
		collectExpressionsOfType(l, cls);
		return l;
	}
	
	public void setAssociatedDeclaration(Declaration declaration) {
		for (ExprElm s : subElements())
			if (s != null)
				s.setAssociatedDeclaration(declaration);
	}

	public ExprElm sequenceTilMe() {
		Sequence fullSequence = sequence();
		if (fullSequence != null) {
			List<ExprElm> elms = new LinkedList<ExprElm>();
			for (ExprElm e : fullSequence.subElements()) {
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

	@Override
	public void postLoad(ExprElm parent, DeclarationObtainmentContext root) {
		for (ExprElm e : subElements())
			if (e != null)
				e.postLoad(this, root);
	}
	
	protected final void missing(C4ScriptParser parser) throws ParsingException {
		ParserErrorCode code = this instanceof Statement ? ParserErrorCode.MissingStatement : ParserErrorCode.MissingExpression;
		parser.error(code, this, C4ScriptParser.NO_THROW, this);
	}

	/**
	 * Increment {@link #start()} and {@link #end()} by the specified amount. Also call {@link #incrementLocation(int)} recursively for {@link #subElements()}
	 * @param amount Amount to increment the location by
	 */
	public void incrementLocation(int amount) {
		setExprRegion(start+amount, start+amount);
		for (ExprElm e : subElements())
			if (e != null)
				e.incrementLocation(amount);
	}
	
	public static Object evaluateAtParseTime(ExprElm element, IEvaluationContext context) {
		return element != null ? element.evaluateAtParseTime(context) : null;
	}
	
	public IType predecessorType(DeclarationObtainmentContext context) {
		return predecessorInSequence != null ? predecessorInSequence.type(context) : null;
	}
	
	public <T extends IType> T predecessorTypeAs(Class<T> cls, DeclarationObtainmentContext context) {
		return predecessorInSequence != null ? as(predecessorInSequence.type(context), cls) : null;
	}
	
	/**
	 * Check whether the given expression contains a reference to a constant.
	 * @param condition The expression to check
	 * @return Whether the expression contains a constant.
	 */
	public boolean containsConst() {
		if (this instanceof AccessVar && ((AccessVar)this).constCondition())
			return true;
		for (ExprElm expression : this.subElements())
			if(expression != null && expression.containsConst())
				return true;
		return false;
	}
	
	protected final IType unresolvedPredecessorType(DeclarationObtainmentContext context) {
		ExprElm e = this;
		//for (e = predecessorInSequence; e != null && e instanceof MemberOperator; e = e.predecessorInSequence);
		return e != null && e.predecessorInSequence != null ? e.predecessorInSequence.unresolvedType(context) : null;
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
	public Map<String, Object> match(ExprElm other) {
		class ComparisonDelegate implements IASTComparisonDelegate {
			public Map<String, Object> result;
			@Override
			public void wildcardMatched(Wildcard wildcard, ExprElm expression) {}
			@Override
			public boolean optionEnabled(Option option) { return false; }
			@Override
			public DifferenceHandling differs(ExprElm a, ExprElm b, Object what) {
				if (what == CLASS && a instanceof MatchingPlaceholder) {
					MatchingPlaceholder mp = (MatchingPlaceholder)a;
					if (result == null)
						result = new HashMap<String, Object>();
					if (mp.satisfiedBy(b)) {
						result.put(mp.entryName(), mp.remainder() ? new ExprElm[] {b} : b);
						return DifferenceHandling.Equal;
					} else
						return DifferenceHandling.Differs;
				}
				else if (what == SUBELEMENTS_LENGTH && a instanceof MatchingPlaceholder && a.subElements().length == 0)
					return DifferenceHandling.IgnoreRightSide;
				else if (what == SUBELEMENTS_LENGTH && a.subElements().length > b.subElements().length) {
					ExprElm[] mine = a.subElements();
					ExprElm[] others = b.subElements();
					for (int i = others.length; i < mine.length; i++)
						if (!(mine[i] instanceof MatchingPlaceholder && ((MatchingPlaceholder)mine[i]).remainder()))
							return DifferenceHandling.Differs;
					return DifferenceHandling.IgnoreLeftSide;
				}
				else if (what == SUBELEMENTS_LENGTH && a.subElements().length < b.subElements().length) {
					ExprElm[] mine = a.subElements();
					ExprElm[] others = b.subElements();
					MatchingPlaceholder remainder = mine.length > 0 ? as(mine[mine.length-1], MatchingPlaceholder.class) : null;
					if (remainder != null && remainder.remainder()) {
						for (int i = mine.length; i < others.length; i++)
							if (!consume(remainder, others[i]))
								return DifferenceHandling.Differs;
						return DifferenceHandling.IgnoreRightSide;
					} else
						return DifferenceHandling.Differs; 
				}
				else
					return DifferenceHandling.Differs;
			}
			private boolean consume(ExprElm consumer, ExprElm extra) {
				if (consumer instanceof MatchingPlaceholder && consumer instanceof MatchingPlaceholder) {
					MatchingPlaceholder mp = (MatchingPlaceholder)consumer;
					if (mp.remainder() && mp.satisfiedBy(extra) && mp.compare(extra, this).isEqual()) {
						Object existing = result.get(mp.entryName());
						if (existing instanceof ExprElm)
							existing = new ExprElm[] {(ExprElm)existing, extra};
						else if (existing instanceof ExprElm[])
							existing = ArrayUtil.concat((ExprElm[])existing, extra);
						result.put(mp.entryName(), existing);
						return true;
					}
				}
				return false;
			}
		}
		ComparisonDelegate delegate = new ComparisonDelegate();
		if (this.compare(other, delegate).isEqual())
			return delegate.result != null ? delegate.result : Collections.<String, Object>emptyMap();
		else
			return null;
	}
	
	/**
	 * Return an instance of a specified class with its public fields set to results from {@link #match(ExprElm)}
	 * @param other The other expression to match against
	 * @param resultType Type of the resulting object
	 */
	public <T> boolean match(ExprElm other, T match) {
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
	public ExprElm transform(final Map<String, Object> substitutions) {
		return transformRecursively(new ITransformer() {
			@Override
			public Object transform(ExprElm prev, Object prevT, ExprElm expression) {
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
	
	/**
	 * Replace {@link Placeholder} objects with {@link MatchingPlaceholder} objects that bring
	 * improved matching capabilities with them.
	 * @return A version of this expression with {@link MatchingPlaceholder} inserted for {@link Placeholder}
	 */
	public ExprElm matchingExpr() {
		ExprElm result = this.transformRecursively(new ITransformer() {
			@Override
			public Object transform(ExprElm prev, Object prevT, ExprElm expression) {
				if (expression != null && expression.getClass() == Placeholder.class)
					try {
						return new MatchingPlaceholder(((Placeholder)expression).entryName());
					} catch (ParsingException e) {
						e.printStackTrace();
						return expression;
					}
				else if (expression instanceof CallExpr && prevT instanceof MatchingPlaceholder) {
					((MatchingPlaceholder)prevT).setSubElements(expression.transformRecursively(this).subElements());
					return REMOVE;
				} else if (
					expression instanceof Sequence &&
					expression.subElements().length == 1 && expression.subElements()[0] instanceof MatchingPlaceholder
				)
					return expression.subElements()[0];
				else
					return expression;
			}
		});
		if (result instanceof SimpleStatement)
			return ((SimpleStatement)result).expression();
		else
			return result;
	}
	
	public IRegion absolute() {
		ExprElm p;
		for (p = this; p != null && p.parent != null; p = p.parent);
		Declaration d = p.owningDeclaration();
		return this.region(d instanceof Function ? ((Function)d).bodyLocation().start() : 0);
	}
	
}