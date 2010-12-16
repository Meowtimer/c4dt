package net.arctics.clonk.parser.c4script.ast;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.CachedEngineFuncs;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ObjectType;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4TypeSet;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPrintable;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * base class for making expression trees
 */
public class ExprElm implements IRegion, Cloneable, IPrintable, Serializable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static final ExprElm NULL_EXPR = new ExprElm();
	public static final ExprElm[] EMPTY_EXPR_ARRAY = new ExprElm[0];
	public static final Object EVALUATION_COMPLEX = new Object();

	public static final ExprElm nullExpr(int start, int length) {
		ExprElm result = new ExprElm();
		result.setExprRegion(start, start+length);
		return result;
	}
	
	private int exprStart, exprEnd;
	private transient ExprElm parent, predecessorInSequence, successorInSequence;
	boolean finishedProperly = true;
	
	public boolean isFinishedProperly() {
		return finishedProperly;
	}
	
	public void setFinishedProperly(boolean finishedProperly) {
		this.finishedProperly = finishedProperly;
	}

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
		ExprElm[] clonedElms = Utilities.map(getSubElements(), ExprElm.class, new IConverter<ExprElm, ExprElm>() {
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
	
	public IType getType(C4ScriptParser context) {
		return context.queryTypeOfExpression(this, C4Type.UNKNOWN);
	}
	
	public final C4Object guessObjectType(C4ScriptParser context) {
		IType t = getType(context);
		if (t instanceof C4Object)
			return (C4Object)t;
		else if (t instanceof C4ObjectType)
			return ((C4ObjectType)t).getType();
		else
			return null;
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

	public ExprElm getSuccessorInSequence() {
		return successorInSequence;
	}

	public void setSuccessorInSequence(ExprElm e) {
		successorInSequence = e;
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
		return getType(context) == C4Type.INT && otherType.canBeAssignedFrom(C4Type.ID);
	}

	public boolean validForType(IType t, C4ScriptParser context) {
		return t == null || t.canBeAssignedFrom(getType(context)) || canBeConvertedTo(t, context);
	}

	public TraversalContinuation traverse(IScriptParserListener listener) {
		return traverse(listener, null);
	}

	/**
	 * Traverses this expression by calling expressionDetected on the supplied IExpressionListener for the root expression and its sub elements.
	 * @param listener the expression listener
	 * @param parser the parser as contet
	 * @return flow control for the calling function
	 */
	public TraversalContinuation traverse(IScriptParserListener listener, C4ScriptParser parser) {
		TraversalContinuation c = listener.expressionDetected(this, parser);
		switch (c) {
		case Cancel:
			return TraversalContinuation.Cancel;
		case Continue:
			break;
		case TraverseSubElements:
			break;
		case SkipSubElements:
			return TraversalContinuation.Continue;
		}
		for (ExprElm sub : getSubElements()) {
			if (sub == null)
				continue;
			switch (sub.traverse(listener, parser)) {
			case Cancel:
				return TraversalContinuation.Cancel;
			case Continue:
				break;
			case TraverseSubElements:
				return TraversalContinuation.Cancel;
			}
		}
		return c;
	}

	public IRegion region(int offset) {
		return new Region(offset+getExprStart(), getExprEnd()-getExprStart());
	}

	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		return null;
	}

	public static IType combineTypes(IType first, IType second) {
		return C4TypeSet.create(first, second);
	}

	private static final ExprElm[] exprElmsForTypes = new ExprElm[C4Type.values().length];

	/**
	 * Returns a canonical ExprElm object for the given type such that its getType() returns the given type
	 * @param type the type to return a canonical ExprElm of
	 * @return the canonical ExprElm object
	 */
	public static ExprElm getExprElmForType(final C4Type type) {
		if (exprElmsForTypes[type.ordinal()] == null) {
			exprElmsForTypes[type.ordinal()] = new ExprElm() {
				/**
				 * 
				 */
				private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

				@Override
				public IType getType(C4ScriptParser context) {
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
		if (type == C4Type.UNKNOWN || type == C4Type.ANY)
			return; // expecting it to be of any or unknown type? come back when you can be more specific please
		IStoredTypeInformation info = context.requestStoredTypeInformation(this);
		if (info != null) {
			switch (mode) {
			case Expect:
				if (info.getType() == C4Type.UNKNOWN)
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

	public final boolean isAlways(boolean what, C4ScriptBase context) {
		Object ev = this.evaluateAtParseTime(context);
		return ev != null && Boolean.valueOf(what).equals(C4Type.BOOL.convert(ev));
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
		return null;
	}

	/**
	 * Rudimentary possibility for evaluating the expression. Only used for evaluating the value of the SetProperty("Name", ...) call in a Definition function (OpenClonk) right now
	 * @param context the context to evaluate in
	 * @return the result
	 */
	public Object evaluateAtParseTime(C4ScriptBase context) {
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
	
	public final CachedEngineFuncs getCachedFuncs(C4ScriptParser parser) {
		return parser.getContainer().getIndex().getEngine().getCachedFuncs();
	}
	
	public ExprElm replaceSubElement(ExprElm element, ExprElm with) {
		ExprElm[] subElms = getSubElements();
		ExprElm[] newSubElms = new ExprElm[subElms.length];
		boolean differentSubElms = false;
		for (int i = 0; i < subElms.length; i++) {
			newSubElms[i] = subElms[i] == element ? with : subElms[i];
			if (newSubElms[i] != subElms[i])
				differentSubElms = true;
		}
		if (differentSubElms) {
			setSubElements(newSubElms);
			assignParentToSubElements();
		}
		else {
			throw new InvalidParameterException("element must actually be a subelement of this");
		}
		return this;
	}
	
	private static final IDifferenceListener NULL_DIFFERENCE_LISTENER = new IDifferenceListener() {
		@Override
		public void differs(ExprElm a, ExprElm b, Object what) {
			// the humanity
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
	
	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass())
			return compare((ExprElm) other, NULL_DIFFERENCE_LISTENER);
		else
			return false;
	}
	
	protected Field field(String name) {
		try {
			return getClass().getField(name);
		} catch (Exception e) {
			return null;
		}
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

}