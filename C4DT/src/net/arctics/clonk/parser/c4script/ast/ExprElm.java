package net.arctics.clonk.parser.c4script.ast;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.CachedEngineFuncs;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IHasConstraint;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.ui.editors.c4script.IPostSerializable;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPrintable;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * base class for making expression trees
 */
public class ExprElm implements IRegion, Cloneable, IPrintable, Serializable, IPostSerializable<ExprElm, DeclarationObtainmentContext> {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static final ExprElm NULL_EXPR = new ExprElm();
	public static final ExprElm[] EMPTY_EXPR_ARRAY = new ExprElm[0];
	public static final Object EVALUATION_COMPLEX = new Object();
	
	public static final int PROPERLY_FINISHED = 1;
	public static final int STATEMENT_REACHED = 2;
	public static final int MISPLACED = 4;

	public static final ExprElm nullExpr(int start, int length, C4ScriptParser parser) {
		ExprElm result = new ExprElm();
		parser.setExprRegionRelativeToFuncBody(result, start, start+length);
		return result;
	}
	
	private int exprStart, exprEnd;
	private ExprElm parent, predecessorInSequence;
	private int flags = PROPERLY_FINISHED;

	/**
	 * Recursion level of the method that parsed this expression/statement which
	 * could have been parseStatement or parseExpression
	 */
	private int parsingRecursion;
	
	/**
	 * 
	 * @return
	 */
	public int getParsingRecursion() {
		return parsingRecursion;
	}
	
	public void setParsingRecursion(int expressionRecursion) {
		this.parsingRecursion = expressionRecursion;
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

	protected void assignParentToSubElements() {
		// cheap man's solution to the mutability-of-exprelms problem:
		// Clone sub elements if they look like they might belong to some other parent
		ExprElm[] subElms = getSubElements();
		boolean modified = false;
		for (int i = 0; i < subElms.length; i++) {
			ExprElm e = subElms[i];
			if (e != null) {
				if (e.getParent() != null && e.getParent() != this) {
					modified = true;
					try {
						subElms[i] = e = (ExprElm) e.clone();
					} catch (CloneNotSupportedException cloneFail) {
						cloneFail.printStackTrace();
					}
				}
				e.setParent(this);
			}
		}
		if (modified) {
			setSubElements(subElms);
		}
	}

	@Override
	public ExprElm clone() throws CloneNotSupportedException {
		ExprElm clone = (ExprElm) super.clone();
		ExprElm[] clonedElms = ArrayUtil.map(getSubElements(), ExprElm.class, new IConverter<ExprElm, ExprElm>() {
			@Override
			public ExprElm convert(ExprElm from) {
				if (from == null) {
					return null;
				}
				try {
					return (ExprElm) from.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					return null;
				}
			}
		});
		clone.setSubElements(clonedElms);
		return clone;
	}

	public ExprElm getParent() {
		return parent;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ExprElm> T getParent(Class<T> cls) {
		ExprElm e;
		for (e = this; e != null && !cls.isAssignableFrom(e.getClass()); e = e.getParent());
		return (T) e;
	}

	public void warnIfNoSideEffects(C4ScriptParser parser) {
		if (!hasSideEffects())
			parser.warningWithCode(ParserErrorCode.NoSideEffects, this);
	}

	public void setParent(ExprElm parent) {
		this.parent = parent;
	}
	
	public void printPrependix(ExprWriter output, int depth) {}
	public void doPrint(ExprWriter output, int depth) {}
	public void printAppendix(ExprWriter output, int depth) {}
	
	public final void print(ExprWriter output, int depth) {
		if (!output.doCustomPrinting(this, depth)) {
			this.printPrependix(output, depth);
			this.doPrint(output, depth);
			this.printAppendix(output, depth);
		}
	}

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
	public final IType getType(DeclarationObtainmentContext context) {
		IType type = obtainType(context);
		if (type instanceof TypeSet) {
			TypeSet typeSet = (TypeSet) type;
			IType[] resolvedTypes = new IType[typeSet.size()];
			boolean didResolveSomething = false;
			int i = 0;
			for (IType t : typeSet) {
				IType resolved = resolveConstraint(context, t);
				if (resolved != t)
					didResolveSomething = true;
				resolvedTypes[i++] = resolved;
			}
			return didResolveSomething ? TypeSet.create(resolvedTypes) : typeSet;
		} else
			return resolveConstraint(context, type);
	}

	/**
	 * Return type of object the expression is executed in
	 * @param context Context information
	 * @return The type
	 */
	protected IType callerType(DeclarationObtainmentContext context) {
		return context.getContainer();
	}
	
	private final IType resolveConstraint(DeclarationObtainmentContext context, IType type) {
		if (type instanceof IHasConstraint)
			return ((IHasConstraint)type).resolve(context, callerType(context));
		else
			return type;
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
		return Utilities.as(Definition.scriptFrom(getType(context)), Definition.class);
	}

	public boolean modifiable(C4ScriptParser context) {
		return true;
	}

	public boolean hasSideEffects() {
		ExprElm[] subElms = getSubElements();
		if (subElms != null) {
			for (ExprElm e : subElms) {
				if (e != null && e.hasSideEffects())
					return true;
			}
		}
		return false;
	}

	public int getLength() {
		return getExprEnd()-getExprStart();
	}

	public int getOffset() {
		return getExprStart();
	}

	public int getExprEnd() {
		return exprEnd;
	}

	public int getExprStart() {
		return exprStart;
	}

	public int getIdentifierStart() {
		return getExprStart();
	}

	public int getIdentifierLength() {
		return getLength();
	}

	public final IRegion identifierRegion() {
		return new Region(getIdentifierStart(), getIdentifierLength());
	}

	public void setExprRegion(int start, int end) {
		this.exprStart = start;
		this.exprEnd   = end;
	}

	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		// i'm totally error-free
	}

	public void setPredecessorInSequence(ExprElm p) {
		predecessorInSequence = p;
	}

	public ExprElm getPredecessorInSequence() {
		return predecessorInSequence;
	}

	public ExprElm[] getSubElements() {
		return EMPTY_EXPR_ARRAY;
	}

	public void setSubElements(ExprElm[] elms) {
		if (getSubElements().length > 0)
			System.out.println("setSubElements should be implemented when getSubElements() is implemented ("+getClass().getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
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
		ExprElm[] subElms = getSubElements();
		ExprElm[] newSubElms = new ExprElm[subElms.length];
		boolean differentSubElms = false;
		for (int i = 0; i < subElms.length; i++) {
			newSubElms[i] = subElms[i] != null ? subElms[i].optimize(context) : null;
			if (newSubElms[i] != subElms[i])
				differentSubElms = true;
		}
		if (differentSubElms) {
			ExprElm replacement = (ExprElm)this.clone();
			replacement.setSubElements(newSubElms);
			replacement.assignParentToSubElements();
			return replacement;
		}
		return this; // nothing to be changed
	}

	/**
	 * Returns whether the expression can be converted to the given type
	 * @param otherType the type to test convertability to
	 * @return true if conversion is possible or false if not
	 */
	public boolean canBeConvertedTo(IType otherType, C4ScriptParser context) {
		// 5555 is ID
		return getType(context) == PrimitiveType.INT && otherType.canBeAssignedFrom(PrimitiveType.ID);
	}

	public boolean validForType(IType t, C4ScriptParser context) {
		if (t == null)
			return true;
		IType myType = getType(context);
		return t.canBeAssignedFrom(myType) || myType.containsType(t) || canBeConvertedTo(t, context);
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
	 * @param parser the parser as contet
	 * @return flow control for the calling function
	 */
	public TraversalContinuation traverse(IScriptParserListener listener, C4ScriptParser parser, int minimumParseRecursion) {
		TraversalContinuation result;
		if (parsingRecursion >= minimumParseRecursion) {
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
		for (ExprElm sub : getSubElements()) {
			if (sub == null)
				continue;
			switch (sub.traverse(listener, parser, minimumParseRecursion)) {
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
		return new Region(offset+getExprStart(), getExprEnd()-getExprStart());
	}

	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		return null;
	}

	public static IType combineTypes(IType first, IType second) {
		return TypeSet.create(first, second);
	}

	private static final ExprElm[] exprElmsForTypes = new ExprElm[PrimitiveType.values().length];

	/**
	 * Returns a canonical ExprElm object for the given type such that its getType() returns the given type
	 * @param type the type to return a canonical ExprElm of
	 * @return the canonical ExprElm object
	 */
	public static ExprElm getExprElmForType(final PrimitiveType type) {
		if (exprElmsForTypes[type.ordinal()] == null) {
			exprElmsForTypes[type.ordinal()] = new ExprElm() {
				/**
				 * 
				 */
				private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

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
		return new Comment(str, str.contains("\n")); //$NON-NLS-1$
	}
	
	public void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		/*if (type == PrimitiveType.UNKNOWN || type == PrimitiveType.ANY)
			return; // expecting it to be of any or unknown type? come back when you can be more specific please
			*/
		IStoredTypeInformation info = context.requestStoredTypeInformation(this);
		if (info != null) {
			switch (mode) {
			case Expect:
				if (info.getType() == PrimitiveType.UNKNOWN)
					info.storeType(type);
				break;
			case Force:
				info.storeType(type);
				break;
			case Hint:
				if (!info.generalTypeHint(type) && errorWhenFailed != null)
					context.warningWithCode(errorWhenFailed, this, info.getType().typeName(false));
				break;
			}
		}
	}
	
	public final void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode) {
		expectedToBeOfType(type, context, mode, null);
	}
	
	public final void expectedToBeOfType(IType type, C4ScriptParser context) {
		expectedToBeOfType(type, context, TypeExpectancyMode.Expect, null);
	}

	public void inferTypeFromAssignment(ExprElm rightSide, DeclarationObtainmentContext context) {
		context.storeTypeInformation(this, rightSide.getType(context));
	}

	public ControlFlow getControlFlow() {
		return ControlFlow.Continue;
	}
	
	public EnumSet<ControlFlow> getPossibleControlFlows() {
		return EnumSet.of(getControlFlow()); 
	}

	public final boolean isAlways(boolean what, IEvaluationContext context) {
		Object ev = this.evaluateAtParseTime(context);
		return ev != null && Boolean.valueOf(what).equals(PrimitiveType.BOOL.convert(ev));
	}

	public boolean containedIn(ExprElm expression) {
		if (expression == this)
			return true;
		try {
			for (ExprElm e : expression.getSubElements())
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
	public ExprElm getSubElementContaining(ExprElm elm) {
		for (ExprElm subElm : getSubElements()) {
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
		return offset >= getExprStart() && offset <= getExprEnd();
	}

	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		ITypeable d = GenericStoredTypeInformation.getTypeable(this, parser);
		if (d != null && !d.typeIsInvariant()) {
			return new GenericStoredTypeInformation(this, parser);
		}
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
	 * Shortcut for obtaining {@link Engine#getCachedFuncs()}
	 * @param context Context the {@link Engine} is lifted from
	 * @return The {@link CachedEngineFuncs}
	 */
	public final CachedEngineFuncs getCachedFuncs(DeclarationObtainmentContext context) {
		return context.getContainer().getIndex().getEngine().getCachedFuncs();
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
		for (ExprElm e : elm.getSubElements()) {
			offsetExprRegionRecursively(e, diff);
		}
	}
	
	private void offsetExprRegionRecursivelyStartingAt(ExprElm elm, int diff) {
		boolean started = false;
		ExprElm[] elms = getSubElements();
		for (ExprElm e : elms) {
			if (e == elm) {
				started = true;
			} else if (started) {
				offsetExprRegionRecursively(e, diff);
			}
		}
		offsetExprRegion(diff, false, true);
		if (getParent() != null) {
			getParent().offsetExprRegionRecursivelyStartingAt(this, diff);
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

		ExprElm[] subElms = getSubElements();
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
			if (getParent() != null) {
				getParent().offsetExprRegionRecursivelyStartingAt(this, diff);
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
			return false;
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
			ExprElm[] mySubElements = this.getSubElements();
			ExprElm[] otherSubElements = other.getSubElements();
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
	protected static final class GenericStoredTypeInformation extends StoredTypeInformation {
		private ExprElm referenceElm;
		
		public GenericStoredTypeInformation(ExprElm referenceElm, C4ScriptParser parser) {
			super();
			this.referenceElm = referenceElm;
			ITypeable typeable = getTypeable(referenceElm, parser);
			if (typeable != null)
				this.type = typeable.getType();
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
			ExprElm chainA, chainB;
			for (chainA = expr, chainB = referenceElm; chainA != null && chainB != null; chainA = chainA.getPredecessorInSequence(), chainB = chainB.getPredecessorInSequence()) {
				if (!chainA.compare(chainB, IDENTITY_DIFFERENCE_LISTENER).isEqual())
					return false;
			}
			return chainA == null || chainB == null;
		}

		@Override
		public boolean refersToSameExpression(IStoredTypeInformation other) {
			if (other instanceof GenericStoredTypeInformation) {
				return ((GenericStoredTypeInformation)other).referenceElm.equals(referenceElm);
			} else {
				return false;
			}
		}
		
		public static ITypeable getTypeable(ExprElm referenceElm, C4ScriptParser parser) {
			DeclarationRegion decRegion = referenceElm.declarationAt(referenceElm.getLength()-1, parser);
			if (decRegion != null && decRegion.getTypedDeclaration() != null)
				return decRegion.getTypedDeclaration();
			else
				return null;
		}
		
		@Override
		public void apply(boolean soft, C4ScriptParser parser) {
			ITypeable typeable = getTypeable(referenceElm, parser);
			if (typeable != null) {
				// only set types of declarations inside the current index so definition references of one project
				// don't leak into a referenced base project (ClonkMars def referenced in ClonkRage or something)
				Index index = typeable.getIndex();
				if (index == null)
					return;
				if (index == parser.getContainer().getIndex())
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
		for (p = this; p != null && !(p instanceof Statement); p = p.getParent());
		return (Statement)p;
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends ExprElm> void collectExpressionsOfType(List<T> list, Class<T> type) {
		for (ExprElm e : getSubElements()) {
			if (e == null)
				continue;
			if (type.isAssignableFrom(e.getClass())) {
				list.add((T) e);
			}
			e.collectExpressionsOfType(list, type);
		}
	}
	
	public <T extends ExprElm> Iterable<T> allSubExpressionsOfType(Class<T> cls) {
		List<T> l = new LinkedList<T>();
		collectExpressionsOfType(l, cls);
		return l;
	}
	
	// getting/setting some associated variable so the exprelm knows from whence it came
	public Declaration associatedDeclaration() {
		return null;
	}
	
	public void setAssociatedDeclaration(Declaration declaration) {
		for (ExprElm s : getSubElements()) {
			if (s != null) {
				s.setAssociatedDeclaration(declaration);
			}
		}
	}

	public ExprElm sequenceTilMe() {
		Sequence fullSequence = sequence();
		if (fullSequence != null) {
			List<ExprElm> elms = new LinkedList<ExprElm>();
			for (ExprElm e : fullSequence.getElements()) {
				elms.add(e);
				if (e == this)
					break;
			}
			return elms.size() == 1 ? this : new Sequence(elms);
		} else {
			return this;
		}
	}

	private Sequence sequence() {
		return getParent() instanceof Sequence ? (Sequence)getParent() : null;
	}

	@Override
	public void postSerialize(ExprElm parent, DeclarationObtainmentContext root) {
		for (ExprElm e : getSubElements()) {
			if (e != null) {
				e.postSerialize(this, root);
			}
		}
	}
	
	protected final void missing(C4ScriptParser parser) throws ParsingException {
		ParserErrorCode code = this instanceof Statement ? ParserErrorCode.MissingStatement : ParserErrorCode.MissingExpression;
		parser.errorWithCode(code, this, this);
	}

}