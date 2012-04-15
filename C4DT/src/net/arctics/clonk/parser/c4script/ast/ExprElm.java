package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IPostLoadable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IResolvableType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPrintable;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Base class for making expression trees
 */
public class ExprElm implements IRegion, Cloneable, IPrintable, Serializable, IPostLoadable<ExprElm, DeclarationObtainmentContext> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final ExprElm NULL_EXPR = new ExprElm();
	public static final ExprElm[] EMPTY_EXPR_ARRAY = new ExprElm[0];
	public static final Object EVALUATION_COMPLEX = new Object();
	
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
	public static final ExprElm nullExpr(int start, int length, C4ScriptParser parser) {
		ExprElm result = new ExprElm();
		parser.setExprRegionRelativeToFuncBody(result, start, start+length);
		return result;
	}
	
	private int exprStart, exprEnd;
	private ExprElm parent, predecessorInSequence;
	private int flags = PROPERLY_FINISHED;

	private int nestingDepth;

	/**
	 * Set how deeply nested the expression is in the AST.
	 * @param nestingDepth The depth
	 */
	public void setNestingDepth(int nestingDepth) {
		this.nestingDepth = nestingDepth;
	}
	
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
		// cheap man's solution to the mutability-of-exprelms problem:
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
		if (modified) {
			setSubElements(subElms);
		}
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
				if (from == null) {
					return null;
				}
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
	public <T extends ExprElm> T parentOfType(Class<T> cls) {
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
			parser.warningWithCode(ParserErrorCode.NoSideEffects, this);
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

	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor == null;
	}
	
	public boolean isValidAtEndOfSequence(C4ScriptParser context) {
		return false;
	}
	
	/**
	 * Return type of the expression, adjusting the result of obtainType under some circumstances
	 * @param context Parser acting as the context (supplying current function, script begin parsed etc.)
	 * @return The type of the expression
	 */
	public final IType type(DeclarationObtainmentContext context) {
		return IResolvableType._.resolve(obtainType(context), context, callerType(context));
	}

	/**
	 * Return type of object the expression is executed in
	 * @param context Context information
	 * @return The type
	 */
	protected IType callerType(DeclarationObtainmentContext context) {
		return context.containingScript();
	}
	
	/**
	 * Overridable method to obtain the type of the declaration.
	 * @param context Parser acting as the context (supplying current function, script begin parsed etc.)
	 * @return The type of the expression
	 */
	protected IType obtainType(DeclarationObtainmentContext context) {
		IType t = context.queryTypeOfExpression(this, PrimitiveType.UNKNOWN);
		if (t == null)
			t = PrimitiveType.UNKNOWN;
		return t;
	}
	
	public final Definition guessObjectType(DeclarationObtainmentContext context) {
		return Utilities.as(Definition.scriptFrom(type(context)), Definition.class);
	}

	public boolean isModifiable(C4ScriptParser context) {
		return true;
	}

	public boolean hasSideEffects() {
		ExprElm[] subElms = subElements();
		if (subElms != null) {
			for (ExprElm e : subElms) {
				if (e != null && e.hasSideEffects())
					return true;
			}
		}
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

	public int end() {
		return exprEnd;
	}

	public int start() {
		return exprStart;
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
		this.exprStart = start;
		this.exprEnd   = end;
	}
	
	public void setExprRegion(IRegion r) {
		this.setExprRegion(r.getOffset(), r.getOffset()+r.getLength());
	}
	
	/**
	 * Give ExprElm a chance to complain about things.
	 * @param parser The parser to report errors to, preferably via some variant of {@link C4ScriptParser#markerWithCode(ParserErrorCode, int, int, int, int, Object...)}
	 * @throws ParsingException
	 */
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		// i'm totally error-free
	}
	
	/**
	 * Returning true tells the {@link C4ScriptParser} to not recursively call {@link #reportErrors(C4ScriptParser)} on {@link #subElements()} 
	 * @return Do you just show up, play the music,
	 */
	public boolean skipReportingErrorsForSubElements() {return false;}

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
	public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
		ExprElm[] subElms = subElements();
		ExprElm[] newSubElms = new ExprElm[subElms.length];
		boolean differentSubElms = false;
		for (int i = 0; i < subElms.length; i++) {
			newSubElms[i] = subElms[i] != null ? subElms[i].optimize(context) : null;
			if (newSubElms[i] != subElms[i])
				differentSubElms = true;
		}
		if (differentSubElms) {
			ExprElm replacement = this.clone();
			replacement.setSubElements(newSubElms);
			replacement.assignParentToSubElements();
			return replacement;
		}
		return this; // nothing to be changed
	}

	/**
	 * Returns whether the expression can be converted to the given type
	 * @param otherType the type to test convertibility to
	 * @return true if conversion is possible or false if not
	 */
	public boolean canBeConvertedTo(IType otherType, C4ScriptParser context) {
		// 5555 is ID
		return type(context) == PrimitiveType.INT && otherType == PrimitiveType.ID;
	}

	public boolean validForType(IType t, C4ScriptParser context) {
		if (t == null)
			return true;
		IType myType = type(context);
		return t.canBeAssignedFrom(myType) || myType.subsetOf(t) || canBeConvertedTo(t, context);
	}

	public TraversalContinuation traverse(IScriptParserListener listener, int minimumParseRecursion) {
		return traverse(listener, null, minimumParseRecursion);
	}
	
	public final TraversalContinuation traverse(IScriptParserListener listener) {
		return traverse(listener, null, 0);
	}

	/**
	 * Traverses this expression by calling expressionDetected on the supplied IExpressionListener for the root expression and its sub elements.
	 * @param listener the expression listener
	 * @param parser the parser as context
	 * @param minimumNesting Minimum AST nesting expressions being reported need to have 
	 * @return flow control for the calling function
	 */
	public TraversalContinuation traverse(IScriptParserListener listener, C4ScriptParser parser, int minimumNesting) {
		TraversalContinuation result;
		if (nestingDepth >= minimumNesting) {
			result = listener.expressionDetected(this, parser);
			switch (result) {
			case Cancel:
				return TraversalContinuation.Cancel;
			case Continue: case TraverseSubElements:
				break;
			case SkipSubElements:
				return TraversalContinuation.Continue;
			}
		} else
			result = TraversalContinuation.Continue;
		for (ExprElm sub : subElements()) {
			if (sub == null)
				continue;
			switch (sub.traverse(listener, parser, minimumNesting)) {
			case Continue:
				break;
			case TraverseSubElements: case Cancel:
				result = TraversalContinuation.Cancel;
			}
		}
		return result;
	}
	
	public final TraversalContinuation traverse(IScriptParserListener listener, C4ScriptParser parser) {
		return traverse(listener, parser, 0);
	}

	public IRegion region(int offset) {
		return new Region(offset+start(), end()-start());
	}

	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
		return null;
	}

	public static IType combineTypes(IType first, IType second) {
		return TypeSet.create(first, second);
	}

	private static final ExprElm[] exprElmsForTypes = new ExprElm[PrimitiveType.values().length];

	/**
	 * Returns a canonical {@link ExprElm} object for the given type such that its {@link #type(DeclarationObtainmentContext)} returns the given type
	 * @param type the type to return a canonical ExprElm of
	 * @return the canonical {@link ExprElm} object
	 */
	public static ExprElm exprElmForPrimitiveType(final PrimitiveType type) {
		if (exprElmsForTypes[type.ordinal()] == null) {
			exprElmsForTypes[type.ordinal()] = new ExprElm() {
				/**
				 * 
				 */
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

				@Override
				protected IType obtainType(DeclarationObtainmentContext context) {
					return type;
				}
			};
		}
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
		return toString(1);
	}
	
	public Comment commentedOut() {
		String str = this.toString();
		return new Comment(str, str.contains("\n"), false); //$NON-NLS-1$
	}
	
	public void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		/*if (type == PrimitiveType.UNKNOWN || type == PrimitiveType.ANY)
			return; // expecting it to be of any or unknown type? come back when you can be more specific please
		 */
		ITypeInfo info;
		switch (mode) {
		case Expect: case Force:
			info = context.requestStoredTypeInformation(this);
			if (info != null)
				if (mode == TypeExpectancyMode.Force || info.type() == PrimitiveType.UNKNOWN || info.type() == PrimitiveType.ANY)
					info.storeType(type);
			break;
		case Hint:
			info = context.queryStoredTypeInformation(this);
			if (info != null && !info.generalTypeHint(type) && errorWhenFailed != null)
				context.warningWithCode(errorWhenFailed, this, info.type().typeName(false));
			break;
		}
	}
	
	public final void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode) {
		expectedToBeOfType(type, context, mode, null);
	}
	
	public final void expectedToBeOfType(IType type, C4ScriptParser context) {
		expectedToBeOfType(type, context, TypeExpectancyMode.Expect, null);
	}

	public void assignment(ExprElm rightSide, C4ScriptParser context) {
		context.storeTypeInformation(this, rightSide.type(context));
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

	public boolean containedIn(ExprElm expression) {
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
		for (ExprElm subElm : subElements()) {
			if (subElm != null) {
				if (elm.containedIn(subElm))
					return subElm;
			}
		}
		return null;
	}

	/**
	 * Returns whether this ExprElm represents a constant value.
	 * @return Whether constant or not.
	 */
	public boolean isConstant() {
		return false;
	}

	public boolean containsOffset(int offset) {
		return offset >= start() && offset <= end();
	}

	public ITypeInfo createStoredTypeInformation(C4ScriptParser parser) {
		ITypeable d = GenericStoredTypeInfo.typeableFromExpression(this, parser);
		if (d != null && !d.typeIsInvariant())
			return new GenericStoredTypeInfo(this, parser);
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
	 * Shortcut for obtaining {@link Engine#cachedFuncs()}
	 * @param context Context the {@link Engine} is lifted from
	 * @return The {@link CachedEngineDeclarations}
	 */
	public final CachedEngineDeclarations cachedFuncs(DeclarationObtainmentContext context) {
		return context.containingScript().index().engine().cachedFuncs();
	}
	
	/**
	 * Increment the offset of this expression by some amount.
	 * @param amount The amount
	 * @param start Whether the start offset is to be incremented
	 * @param end Whether the end offset is to be incremented
	 */
	protected void offsetExprRegion(int amount, boolean start, boolean end) {
		if (start)
			exprStart += amount;
		if (end)
			exprEnd += amount;
	}
	
	private static void offsetExprRegionRecursively(ExprElm elm, int diff) {
		if (elm == null)
			return;
		elm.offsetExprRegion(diff, true, true);
		for (ExprElm e : elm.subElements()) {
			offsetExprRegionRecursively(e, diff);
		}
	}
	
	private void offsetExprRegionRecursivelyStartingAt(ExprElm elm, int diff) {
		boolean started = false;
		ExprElm[] elms = subElements();
		for (ExprElm e : elms) {
			if (e == elm) {
				started = true;
			} else if (started) {
				offsetExprRegionRecursively(e, diff);
			}
		}
		offsetExprRegion(diff, false, true);
		if (parent() != null) {
			parent().offsetExprRegionRecursivelyStartingAt(this, diff);
		}
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
		
		if (diff == 0) {
			diff = with.getLength();
		}

		ExprElm[] subElms = subElements();
		ExprElm[] newSubElms = new ExprElm[subElms.length];
		boolean differentSubElms = false;
		for (int i = 0; i < subElms.length; i++) {
			if (subElms[i] == element) {
				newSubElms[i] = with;
				differentSubElms = true;
			} else {
				newSubElms[i] = subElms[i];
				if (differentSubElms) {
					offsetExprRegionRecursively(subElms[i], diff);
				}
			}
		}
		if (differentSubElms) {
			setSubElements(newSubElms);
			with.setParent(this);
			offsetExprRegion(diff, false, true);
			if (parent() != null) {
				parent().offsetExprRegionRecursivelyStartingAt(this, diff);
			}
		} else {
			throw new InvalidParameterException("element must actually be a subelement of this");
		}
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
	 * @param listener Listener that gets notified when changes are found.
	 * @return Whether elements are equal or not.
	 */
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		if (other.getClass() == this.getClass()) {
			ExprElm[] mySubElements = this.subElements();
			ExprElm[] otherSubElements = other.subElements();
			if (mySubElements.length != otherSubElements.length) {
				switch (listener.differs(this, other, IASTComparisonDelegate.SUBELEMENTS_LENGTH)) {
				case IgnoreLeftSide: case IgnoreRightSide:
					break;
				default:
					return DifferenceHandling.Differs;
				}
			}

			int myIndex, otherIndex;
			for (myIndex = 0, otherIndex = 0; myIndex < mySubElements.length && otherIndex < otherSubElements.length; myIndex++, otherIndex++) {
				ExprElm a = mySubElements[myIndex];
				ExprElm b = otherSubElements[otherIndex];
				
				// compare elements, taking the possibility into account that one or both of the elements might be null
				// if only one of both is null, the listener gets to decide what to do now based on a differs call with
				// the what parameter being set to the index of the null element in the respective array
				DifferenceHandling handling;
				if (a != null && b != null)
					handling = a.compare(b, listener);
				else if (a == null && b != null)
					handling = listener.differs(a, b, myIndex);
				else if (a != null && b == null)
					handling = listener.differs(a, b, otherIndex);
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
				}
			}
			if (myIndex == mySubElements.length && otherIndex == otherSubElements.length)
				return DifferenceHandling.Equal;
			else
				return DifferenceHandling.Differs;
		}
		else {
			return listener.differs(this, other, other);
		}
	}
	
	/**
	 * Stored Type Information that applies the stored type information by determining the {@link ITypeable} being referenced by some arbitrary {@link ExprElm} and setting its type.
	 * @author madeen
	 *
	 */
	protected static final class GenericStoredTypeInfo extends TypeInfo {
		private final ExprElm referenceElm;
		
		public GenericStoredTypeInfo(ExprElm referenceElm, C4ScriptParser parser) {
			super();
			this.referenceElm = referenceElm;
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
			if (expr instanceof AccessDeclaration && referenceElm instanceof AccessDeclaration && ((AccessDeclaration)expr).declaration() == ((AccessDeclaration)referenceElm).declaration())
				return true;
			ExprElm chainA, chainB;
			for (chainA = expr, chainB = referenceElm; chainA != null && chainB != null; chainA = chainA.predecessorInSequence(), chainB = chainB.predecessorInSequence()) {
				if (!chainA.compare(chainB, IDENTITY_DIFFERENCE_LISTENER).isEqual())
					return false;
			}
			return chainA == null || chainB == null;
		}

		@Override
		public boolean refersToSameExpression(ITypeInfo other) {
			if (other instanceof GenericStoredTypeInfo)
				return ((GenericStoredTypeInfo)other).referenceElm.equals(referenceElm);
			else
				return false;
		}
		
		public static ITypeable typeableFromExpression(ExprElm referenceElm, C4ScriptParser parser) {
			EntityRegion decRegion = referenceElm.declarationAt(referenceElm.getLength()-1, parser);
			if (decRegion != null && decRegion.entityAs(ITypeable.class) != null)
				return decRegion.entityAs(ITypeable.class);
			else
				return null;
		}
		
		@Override
		public void apply(boolean soft, C4ScriptParser parser) {
			ITypeable typeable = typeableFromExpression(referenceElm, parser);
			if (typeable != null) {
				// only set types of declarations inside the current index so definition references of one project
				// don't leak into a referenced base project (ClonkMars def referenced in ClonkRage or something)
				Index index = typeable.index();
				if (index == null)
					return;
				if (index == parser.containingScript().index())
					typeable.expectedToBeOfType(type, TypeExpectancyMode.Expect);
			}
		}
		
		@Override
		public String toString() {
			return referenceElm.toString() + ": " + super.toString();
		}
		
	}
	
	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass())
			return compare((ExprElm) other, NULL_DIFFERENCE_LISTENER).isEqual();
		else
			return false;
	}
	
	public Statement containingStatementOrThis() {
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
		for (ExprElm s : subElements()) {
			if (s != null) {
				s.setAssociatedDeclaration(declaration);
			}
		}
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
		for (ExprElm e : subElements()) {
			if (e != null) {
				e.postLoad(this, root);
			}
		}
	}
	
	protected final void missing(C4ScriptParser parser) throws ParsingException {
		ParserErrorCode code = this instanceof Statement ? ParserErrorCode.MissingStatement : ParserErrorCode.MissingExpression;
		parser.errorWithCode(code, this, this);
	}

	/**
	 * Increment {@link #start()} and {@link #end()} by the specified amount. Also call {@link #incrementLocation(int)} recursively for {@link #subElements()}
	 * @param amount Amount to increment the location by
	 */
	public void incrementLocation(int amount) {
		setExprRegion(exprStart+amount, exprStart+amount);
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

}