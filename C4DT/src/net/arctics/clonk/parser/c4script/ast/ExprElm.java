package net.arctics.clonk.parser.c4script.ast;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.CachedEngineFuncs;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.ConstrainedObject;
import net.arctics.clonk.parser.c4script.ConstrainedType;
import net.arctics.clonk.parser.c4script.IHasConstraint;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypedDeclaration;
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
	public Object clone() throws CloneNotSupportedException {
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

	public void doPrint(ExprWriter output, int depth) {
	}
	
	public final void print(ExprWriter output, int depth) {
		if (!output.doCustomPrinting(this, depth))
			this.doPrint(output, depth);
	}
	
	public final void print(final StringBuilder builder, int depth) {
		print(new ExprWriter() {
			@Override
			public boolean doCustomPrinting(ExprElm elm, int depth) {
				return false;
			}
			
			@Override
			public void append(char c) {
				builder.append(c);
			}
			
			@Override
			public void append(String text) {
				builder.append(text);
			}
		}, depth);
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
		} else {
			return resolveConstraint(context, type);
		}
	}

	private final IType resolveConstraint(DeclarationObtainmentContext context, IType type) {
		if (type instanceof IHasConstraint) {
			IHasConstraint obl = (IHasConstraint) type;
			if (obl instanceof ConstrainedObject) {
				switch (obl.constraintKind()) {
				case CallerType:
					ExprElm pred = getPredecessorInSequence();
					if (pred != null)
						return pred.getType(context);
					else if (obl.constraintScript() != context.getContainer())
						// constraint only resolved outside of original script
						return context.getContainerAsType();
					else
						break;
				case Exact:
					return Utilities.as(obl.constraintScript(), IType.class);
				case Includes:
					break;
				}
			} else if (obl instanceof ConstrainedType) {
				Definition obj = null;
				switch (obl.constraintKind()) {
				case CallerType:
					ExprElm pred = getPredecessorInSequence();
					if (pred != null)
						obj = Utilities.as(pred.getType(context), Definition.class);
					else if (obl.constraintScript() != context.getContainer())
						obj = context.getContainerAsDefinition();
					break;
				case Exact:
					obj = Utilities.as(obl.constraintScript(), Definition.class);
					break;
				case Includes:
					break;
				}
				if (obj != null)
					return obj.getObjectType();
			}
		}
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
		if (type == PrimitiveType.UNKNOWN || type == PrimitiveType.ANY)
			return; // expecting it to be of any or unknown type? come back when you can be more specific please
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
					context.warningWithCode(errorWhenFailed, this);
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

	public void inferTypeFromAssignment(ExprElm rightSide, C4ScriptParser parser) {
		parser.storeTypeInformation(this, rightSide.getType(parser));
	}

	public ControlFlow getControlFlow() {
		return ControlFlow.Continue;
	}
	
	public EnumSet<ControlFlow> getPossibleControlFlows() {
		return EnumSet.of(getControlFlow()); 
	}

	public final boolean isAlways(boolean what, ScriptBase context) {
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

	public boolean isConstant() {
		return false;
	}

	public boolean containsOffset(int offset) {
		return offset >= getExprStart() && offset <= getExprEnd();
	}

	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		ITypedDeclaration d = GenericStoredTypeInformation.getDeclaration(this, parser);
		if (d != null && !d.typeIsInvariant()) {
			return new GenericStoredTypeInformation(this);
		}
		return null;
	}

	/**
	 * Rudimentary possibility for evaluating the expression. Only used for evaluating the value of the SetProperty("Name", ...) call in a Definition function (OpenClonk) right now
	 * @param context the context to evaluate in
	 * @return the result
	 */
	public Object evaluateAtParseTime(ScriptBase context) {
		return EVALUATION_COMPLEX;
	}
	
	/**
	 * Evaluate expression. Used for the interpreter
	 * @return the result of the evaluation
	 */
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		return null;
	}
	
	public final Object evaluate() throws ControlFlowException {
		return evaluate(null);
	}
	
	public final CachedEngineFuncs getCachedFuncs(DeclarationObtainmentContext context) {
		return context.getContainer().getIndex().getEngine().getCachedFuncs();
	}
	
	protected void offsetExprRegion(int amount, boolean start, boolean end) {
		if (start)
			exprStart += amount;
		if (end)
			exprEnd += amount;
		/*if (start || end)
			System.out.println(String.format("Adjusting %s: start=%s end=%s", this.toString(), Boolean.valueOf(start), Boolean.valueOf(end)));*/
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
	
	public ExprElm replaceSubElement(ExprElm element, ExprElm with, int diff) {
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
	
	private static final IDifferenceListener NULL_DIFFERENCE_LISTENER = new IDifferenceListener() {
		@Override
		public void differs(ExprElm a, ExprElm b, Object what) {
			// the humanity
		}

		@Override
		public boolean optionEnabled(Option option) {
			return false;
		}
	};
	
	public boolean compare(ExprElm other, IDifferenceListener listener) {
		if (other.getClass() == this.getClass()) {
			ExprElm[] mySubElements = this.getSubElements();
			ExprElm[] otherSubElements = other.getSubElements();
			if (mySubElements.length != otherSubElements.length) {
				listener.differs(this, other, IDifferenceListener.SUBELEMENTS_LENGTH);
				return false;
			} else {
				for (int i = 0; i < mySubElements.length; i++) {
					ExprElm a = mySubElements[i];
					ExprElm b = otherSubElements[i];
					if (!a.compare(b, listener)) {
						return false;
					}
				}
				return true;
			}
		}
		else {
			return false;
		}
	}
	
	protected static final class GenericStoredTypeInformation extends StoredTypeInformation {
		private ExprElm referenceElm;
		
		public GenericStoredTypeInformation(ExprElm referenceElm) {
			super();
			this.referenceElm = referenceElm;
		}
		
		private static final IDifferenceListener IDENTITY_DIFFERENCE_LISTENER = new IDifferenceListener() {
			@Override
			public void differs(ExprElm a, ExprElm b, Object what) {
				// ok
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
			
		};

		@Override
		public boolean expressionRelevant(ExprElm expr, C4ScriptParser parser) {
			return expr.compare(referenceElm, IDENTITY_DIFFERENCE_LISTENER);
		}

		@Override
		public boolean sameExpression(IStoredTypeInformation other) {
			if (other instanceof GenericStoredTypeInformation) {
				return ((GenericStoredTypeInformation)other).referenceElm.equals(referenceElm);
			} else {
				return false;
			}
		}
		
		public static ITypedDeclaration getDeclaration(ExprElm referenceElm, C4ScriptParser parser) {
			DeclarationRegion decRegion = referenceElm.declarationAt(referenceElm.getLength()-1, parser);
			if (decRegion != null && decRegion.getDeclaration() instanceof ITypedDeclaration) {
				return (ITypedDeclaration) decRegion.getDeclaration();
			} else {
				return null;
			}
		}
		
		@Override
		public void apply(boolean soft, C4ScriptParser parser) {
			ITypedDeclaration tyDec = getDeclaration(referenceElm, parser);
			if (tyDec != null) {
				// only set types of declarations inside the current index so definition references of one project
				// don't leak into a referenced base project (ClonkMars def referenced in ClonkRage or something)
				Declaration d = (Declaration)tyDec;
				if (d.getScript().getIndex() == parser.getContainer().getIndex())
					tyDec.expectedToBeOfType(type, TypeExpectancyMode.Expect);
			}
		}
		
	}
	
	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass())
			return compare((ExprElm) other, NULL_DIFFERENCE_LISTENER);
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

}