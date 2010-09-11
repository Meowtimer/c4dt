package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.index.CachedEngineFuncs;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.Statement.Attachment.Position;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.UnaryOp.Placement;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPrintable;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Utilities;
import net.arctics.clonk.parser.ParsingException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Contains classes that form a c4script syntax tree.
 */
public abstract class C4ScriptExprTree {
	
	public enum BraceStyleType {
		NewLine,
		SameLine,
	}
	
	// options
	public static boolean alwaysConvertObjectCalls = false;
	public static BraceStyleType braceStyle = BraceStyleType.NewLine;
	public static String indentString = "\t"; //$NON-NLS-1$

	public enum TraversalContinuation {
		Continue,
		TraverseSubElements,
		SkipSubElements,
		Cancel
	}
	
	public enum ExpressionListenerEvent {
		ExpressionDetected,
		TypeInformationListStackAboutToChange
	}

	public interface IExpressionListener {
		public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser);
		public void endTypeInferenceBlock(List<IStoredTypeInformation> typeInfos);
	}
	
	public static abstract class ExpressionListener implements IExpressionListener {
		@Override
		public void endTypeInferenceBlock(List<IStoredTypeInformation> typeInfos) {
			// don't care
		}
	}

	public interface ILoop {
		ExprElm getBody();
	}

	public enum ControlFlow {
		Continue,
		NextIteration,
		BreakLoop,
		Return,
	}
	
	public interface ExprWriter {
		boolean doCustomPrinting(ExprElm elm, int depth);
		void append(String text);
		void append(char c);
	}
	
	public static void printIndent(ExprWriter builder, int indentDepth) {
		for (int i = 0; i < indentDepth; i++)
			builder.append(indentString); // FIXME: should be done according to user's preferences
	}

	public final static class DeclarationRegion {
		private C4Declaration declaration;
		private IRegion region;
		private String text;
		public C4Declaration getDeclaration() {
			return declaration;
		}
		public DeclarationRegion(C4Declaration declaration, IRegion region, String text) {
			super();
			this.declaration = declaration;
			this.region = region;
			this.text = text;
		}
		public DeclarationRegion(C4Declaration declaration, IRegion region) {
			this(declaration, region, null);
		}
		public IRegion getRegion() {
			return region;
		}
		public String getText() {
			return text;
		}
		public void setDeclaration(C4Declaration declaration) {
			this.declaration = declaration;
		}
		public DeclarationRegion addOffsetInplace(int offset) {
			region = new Region(region.getOffset()+offset, region.getLength());
			return this;
		}
		@Override
		public String toString() {
			if (declaration != null && region != null)
				return declaration.toString() + "@(" + region.toString() + ")" + (text != null ? "("+text+")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			else
				return "Empty DeclarationRegion"; //$NON-NLS-1$
		}
	}
	
	private static final IConverter<ExprElm, Object> EVALUATE_EXPR = new IConverter<ExprElm, Object>() {
		@Override
        public Object convert(ExprElm from) {
            try {
				return from != null ? from.evaluate() : null;
			} catch (ControlFlowException e) {
				return null;
			}
        }
	};
	
	public static class ControlFlowException extends Exception {

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		private ControlFlow controlFlow;

		public ControlFlow getControlFlow() {
			return controlFlow;
		}

		public ControlFlowException(ControlFlow controlFlow) {
			super();
			this.controlFlow = controlFlow;
		}
	}
	
	public static class ReturnException extends ControlFlowException {

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		private Object result;

		public Object getResult() {
			return result;
		}
		
		public ReturnException(Object result) {
			super(ControlFlow.Return);
			this.result = result;
		}
		
	}

	public interface IVariableValueProvider {
		Object getValueForVariable(String varName);
	}
	
	public interface IEvaluationContext extends IVariableValueProvider {
		Object[] getArguments();
		C4Function getFunction();
	}
	
	/**
	 * base class for making expression trees
	 */
	public static class ExprElm implements IRegion, Cloneable, IPrintable {
		
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

		protected void assignParentToSubElements() {
			for (ExprElm e : getSubElements())
				if (e != null)
					e.setParent(this);
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
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
			return t.canBeAssignedFrom(getType(context)) || canBeConvertedTo(t, context);
		}

		public TraversalContinuation traverse(IExpressionListener listener) {
			return traverse(listener, null);
		}

		/**
		 * Traverses this expression by calling expressionDetected on the supplied IExpressionListener for the root expression and its sub elements.
		 * @param listener the expression listener
		 * @param parser the parser as contet
		 * @return flow control for the calling function
		 */
		public TraversalContinuation traverse(IExpressionListener listener, C4ScriptParser parser) {
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
		
		public enum TypeExpectancyMode {
			Expect,
			Hint,
			Force
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

		public boolean containsOffset(int preservedOffset) {
			return preservedOffset >= getExprStart() && preservedOffset <= getExprEnd();
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
		
		public void replaceSubElement(ExprElm element, ExprElm with) {
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
		}

	}

	public static class MemberOperator extends ExprElm {
		private boolean dotNotation;
		private boolean hasTilde;
		private C4ID id;
		private int idOffset;
		
		public static boolean endsWithDot(ExprElm expression) {
			return
				expression instanceof Sequence &&
				((Sequence)expression).getLastElement() instanceof MemberOperator &&
				((MemberOperator)((Sequence)expression).getLastElement()).dotNotation;
		}

		public MemberOperator(boolean dotNotation, boolean hasTilde, C4ID id, int idOffset) {
			super();
			this.dotNotation = dotNotation;
			this.hasTilde = hasTilde;
			this.id = id;
			this.idOffset = idOffset;
		}

		@Override
		public void doPrint(ExprWriter output, int depth) {
			if (dotNotation) {
				// so simple
				output.append('.');
			}
			else {
				if (hasTilde)
					output.append("->~"); //$NON-NLS-1$
				else
					output.append("->"); //$NON-NLS-1$
				if (id != null) {
					output.append(id.getName());
					output.append("::"); //$NON-NLS-1$
				}
			}
		}

		public C4ID getId() {
			return id;
		}

		public void setId(C4ID id) {
			this.id = id;
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
			if (predecessor != null) {
				/*IType t = predecessor.getType(context);
				if (t == null || t.subsetOfType(C4TypeSet.ARRAY_OR_STRING))
					return false;*/
				return true;
			}
			return false;
		}

		@Override
		public IType getType(C4ScriptParser context) {
			// explicit id
			if (id != null) {
				return context.getContainer().getNearestObjectWithId(id);
			}
			// stuff before -> decides
			return getPredecessorInSequence() != null ? getPredecessorInSequence().getType(context) : super.getType(context);
		}

		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			if (id != null && offset >= idOffset && offset < idOffset+4)
				return new DeclarationRegion(parser.getContainer().getNearestObjectWithId(id), new Region(getExprStart()+idOffset, 4));
			return null;
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			ExprElm pred = getPredecessorInSequence();
			if (pred != null) {
				pred.expectedToBeOfType(dotNotation ? C4Type.OBJECT : C4TypeSet.OBJECT_OR_ID, parser, TypeExpectancyMode.Hint, ParserErrorCode.CallingMethodOnNonObject);
			}
		}

	}

	public abstract static class Value extends ExprElm {

		@Override
		public IType getType(C4ScriptParser context) {
			return context.queryTypeOfExpression(this, C4Type.ANY);
		}

	}

	public static class Sequence extends Value {
		protected ExprElm[] elements;
		public Sequence(ExprElm... elms) {
			elements = elms;
			ExprElm prev = null;
			for (ExprElm e : elements) {
				e.setPredecessorInSequence(prev);
				if (prev != null)
					prev.setSuccessorInSequence(e);
				e.setParent(this);
				prev = e;
			}
		}
		@Override
		public ExprElm[] getSubElements() {
			return elements;
		}
		@Override
		public void setSubElements(ExprElm[] elms) {
			elements = elms;
		}
		@Override
		public void doPrint(ExprWriter output, int depth) {
			for (ExprElm e : elements) {
				e.print(output, depth+1);
			}
		}
		@Override
		public IType getType(C4ScriptParser context) {
			return (elements == null || elements.length == 0) ? C4Type.UNKNOWN : elements[elements.length-1].getType(context);
		}
		@Override
		public boolean modifiable(C4ScriptParser context) {
			return elements != null && elements.length > 0 && elements[elements.length-1].modifiable(context);
		}
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			for (ExprElm e : elements) {
				e.reportErrors(parser);
			}
		}
		public ExprElm[] getElements() {
			return elements;
		}
		public ExprElm getLastElement() {
			return elements != null && elements.length > 1 ? elements[elements.length-1] : null;
		}
		@Override
		public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
			ExprElm last = getLastElement();
			if (last != null)
				// things in sequences should take into account their predecessors
				return last.createStoredTypeInformation(parser);
			return super.createStoredTypeInformation(parser);
		}

	}

	public static abstract class AccessDeclaration extends Value {
		protected C4Declaration declaration;
		private boolean declNotFound = false;
		protected final String declarationName;

		public C4Declaration getDeclaration(C4ScriptParser parser) {
			if (declaration == null && !declNotFound) {
				declaration = getDeclImpl(parser);
				declNotFound = declaration == null;
			}
			return declaration;
		}

		public C4Declaration getDeclaration() {
			return declaration; // return without trying to obtain it (no parser context)
		}

		protected abstract C4Declaration getDeclImpl(C4ScriptParser parser);

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			getDeclaration(parser); // find the field so subclasses can complain about missing variables/functions
		}

		public AccessDeclaration(String fieldName) {
			this.declarationName = fieldName;
		}
		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append(declarationName);
		}

		public IRegion declarationRegion(int offset) {
			return new Region(offset+getExprStart(), declarationName.length());
		}

		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			return new DeclarationRegion(getDeclaration(parser), region(0));
		}

		public String getDeclarationName() {
			return declarationName;
		}

		@Override
		public int getIdentifierLength() {
			return declarationName.length();
		}

		public boolean indirectAccess() {
			return declaration == null || !declaration.getName().equals(declarationName);
		}
	}

	public static class AccessVar extends AccessDeclaration {

		private static final class VariableTypeInformation extends StoredTypeInformation {
			private C4Declaration decl;

			public VariableTypeInformation(C4Declaration varDeclaration) {
				this.decl = varDeclaration;
				if (varDeclaration instanceof C4Variable) {
					C4Variable var = (C4Variable) varDeclaration;
					storeType(C4TypeSet.create(var.getType(), var.getObjectType()));
				}
			}

			public boolean expressionRelevant(ExprElm expr, C4ScriptParser parser) {
				// obj-> asks for the relevance of obj
				if (expr instanceof Sequence) {
					Sequence seq = (Sequence) expr;
					if (seq.getLastElement() instanceof MemberOperator && seq.getLastElement().getPredecessorInSequence() instanceof AccessVar)
						expr = seq.getLastElement().getPredecessorInSequence();
					else
						return false;
				}
				return expr instanceof AccessVar && ((AccessVar)expr).getDeclaration() == decl;
			}

			@Override
			public void apply(boolean soft) {
				if (decl == null)
					return;
				decl = decl.latestVersion(); 
				if (decl instanceof C4Variable) {
					C4Variable var = (C4Variable) decl;
					if (!soft || var.getScope() == C4VariableScope.VAR) {
						// for serialization, split static and non-static types again
						var.setType(C4TypeSet.staticIngredients(getType()));
						var.setObjectType(C4TypeSet.objectIngredient(getType()));
					}
				}
			}

			public boolean sameExpression(IStoredTypeInformation other) {
				return other.getClass() == VariableTypeInformation.class && ((VariableTypeInformation)other).decl == decl;
			}

			@Override
			public String toString() {
				return "variable " + decl.getName() + " " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$
			}

		}

		@Override
		public boolean modifiable(C4ScriptParser context) {
			return declaration == null || ((C4Variable)declaration).getScope() != C4VariableScope.CONST;
		}

		public AccessVar(String varName) {
			super(varName);
		}

		public AccessVar(C4Variable v) {
			this(v.getName());
			this.declaration = v;
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
			return
				// either null or
				predecessor == null ||
				// following a dot
				(predecessor instanceof MemberOperator && ((MemberOperator)predecessor).dotNotation);
		}

		@Override
		protected C4Declaration getDeclImpl(C4ScriptParser parser) {
			FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
			info.setContextFunction(parser.getActiveFunc());
			ExprElm p = getPredecessorInSequence();
			C4ScriptBase lookIn = p == null ? parser.getContainer() : p.guessObjectType(parser);
			info.setSearchOrigin(lookIn);
			return lookIn.findVariable(declarationName, info);
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (declaration == null && getPredecessorInSequence() == null)
				parser.errorWithCode(ParserErrorCode.UndeclaredIdentifier, this, true, declarationName);
			// local variable used in global function
			else if (declaration instanceof C4Variable) {
				C4Variable var = (C4Variable) declaration;
				switch (var.getScope()) {
					case LOCAL:
						if (parser.getActiveFunc().getVisibility() == C4FunctionScope.GLOBAL) {
							parser.errorWithCode(ParserErrorCode.LocalUsedInGlobal, this, true);
						}
						break;
					case STATIC:
						parser.getContainer().addUsedProjectScript(var.getScript());
						break;
					case VAR:
						if (var.getLocation() != null) {
							int locationUsed = parser.getActiveFunc().getBody().getOffset()+this.getExprStart();
							if (locationUsed < var.getLocation().getOffset())
								parser.warningWithCode(ParserErrorCode.VarUsedBeforeItsDeclaration, this, var.getName());
						}
						break;
				}
			}
		}

		public static IStoredTypeInformation createStoredTypeInformation(C4Declaration declaration) {
			return new VariableTypeInformation(declaration);
		}

		@Override
		public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
			return createStoredTypeInformation(getDeclaration());
		}
		
		@Override
		public IType getType(C4ScriptParser context) {
			C4Declaration d = getDeclaration(context);
			// getDeclaration(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
			if (d == C4Variable.THIS)
				return context.getContainerObject() != null ? context.getContainerObject() : C4Type.OBJECT;
			IType stored = context.queryTypeOfExpression(this, null);
			if (stored != null)
				return stored;
			if (d instanceof C4Variable) {
				C4Variable v = (C4Variable) d;
				if (v.getObjectType() != null)
					return v.getObjectType();
				else
					return v.getType();
			}
			return C4Type.UNKNOWN;
		}

		@Override
		public void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
			if (getDeclaration() == C4Variable.THIS)
				return;
			super.expectedToBeOfType(type, context, mode, errorWhenFailed);
		}

		@Override
		public void inferTypeFromAssignment(ExprElm expression, C4ScriptParser context) {
			if (getDeclaration() == C4Variable.THIS)
				return;
			super.inferTypeFromAssignment(expression, context);
		}
		
		private static C4Object getObjectBelongingToStaticVar(C4Variable var) {
			C4Declaration parent = var.getParentDeclaration();
			if (parent instanceof C4Object && ((C4Object)parent).getStaticVariable() == var)
				return (C4Object) parent;
			else
				return null;
		}

		@Override
		public Object evaluateAtParseTime(C4ScriptBase context) {
			C4Object obj;
			if (declaration instanceof C4Variable) {
				C4Variable var = (C4Variable) declaration;
				if (var.getScope() == C4VariableScope.CONST)
					return var.getConstValue();
				else if ((obj = getObjectBelongingToStaticVar(var)) != null)
					return obj.getId(); // just return the id
			}
			return super.evaluateAtParseTime(context);
		}

		public boolean constCondition() {
			return declaration instanceof C4Variable && ((C4Variable)declaration).getScope() == C4VariableScope.CONST;
		}
		
		@Override
		public Object evaluate(IEvaluationContext context) throws ControlFlowException {
			if (context != null) {
				return context.getValueForVariable(getDeclarationName());
			}
			else {
				return super.evaluate(context);
			}
		}
		
		@Override
		public boolean isConstant() {
			if (getDeclaration() instanceof C4Variable) {
				C4Variable var = (C4Variable) getDeclaration();
				return getObjectBelongingToStaticVar(var) != null;
			}
			else
				return false;
		}

	}

	public static class CallFunc extends AccessDeclaration {
		
		private final static class FunctionReturnTypeInformation extends StoredTypeInformation {
			private C4Function function;

			public FunctionReturnTypeInformation(C4Function function) {
				super();
				this.function = function;
				if (function != null)
					type = function.getReturnType();
			}
			
			@Override
			public void storeType(IType type) {
				// don't store if function.getReturnType() already specifies a type (including any)
				// this is to prevent cases where for example the result of EffectVar in one instance is
				// used as int and then as something else which leads to an erroneous type incompatibility warning
				if (type == C4Type.UNKNOWN)
					super.storeType(type);
			}
			
			@Override
			public boolean expressionRelevant(ExprElm expr, C4ScriptParser parser) {
				if (expr instanceof CallFunc) {
					CallFunc callFunc = (CallFunc) expr;
					if (callFunc.getDeclaration() == this.function)
						return true;
				}
				return false;
			}
			
			@Override
			public boolean sameExpression(IStoredTypeInformation other) {
				return other instanceof FunctionReturnTypeInformation && ((FunctionReturnTypeInformation)other).function == this.function;
			}
			
			@Override
			public String toString() {
				return "function " + function + " " + super.toString();
			}
			
			@Override
			public void apply(boolean soft) {
				if (function == null)
					return;
				function = (C4Function) function.latestVersion();
				if (!soft && !function.isEngineDeclaration()) {
					function.forceType(getType());
				}
			}
			
		}
		
		private final static class VarFunctionsTypeInformation extends StoredTypeInformation {
			private C4Function varFunction;
			private long varIndex;

			private VarFunctionsTypeInformation(C4Function function, long val) {
				varFunction = function;
				varIndex = val;
			}

			public boolean expressionRelevant(ExprElm expr, C4ScriptParser parser) {
				if (expr instanceof CallFunc) {
					CallFunc callFunc = (CallFunc) expr;
					Object ev;
					return
						callFunc.getDeclaration() == varFunction &&
						callFunc.getParams().length == 1 && // don't bother with more complex cases
						callFunc.getParams()[0].getType(parser) == C4Type.INT &&
						((ev = callFunc.getParams()[0].evaluateAtParseTime(parser.getContainer())) != null) &&
						ev.equals(varIndex);
				}
				return false;
			}

			public boolean sameExpression(IStoredTypeInformation other) {
				if (other.getClass() == VarFunctionsTypeInformation.class) {
					VarFunctionsTypeInformation otherInfo = (VarFunctionsTypeInformation) other;
					return otherInfo.varFunction == this.varFunction && otherInfo.varIndex == this.varIndex; 
				}
				else
					return false;
			}
			
			@Override
			public String toString() {
				return String.format("%s(%d)", varFunction.getName(), varIndex); //$NON-NLS-1$
			}
			
		}

		private ExprElm[] params;
		private int parmsStart, parmsEnd;

		public void setParmsRegion(int start, int end) {
			parmsStart = start;
			parmsEnd   = end;
		}

		public int getParmsStart() {
			return parmsStart;
		}

		public int getParmsEnd() {
			return parmsEnd;
		}

		public CallFunc(String funcName, ExprElm... parms) {
			super(funcName);
			params = parms;
			assignParentToSubElements();
		}
		
		public CallFunc(C4Function function, ExprElm... parms) {
			this(function.getName());
			this.declaration = function;
		}
		
		@Override
		public void doPrint(ExprWriter output, int depth) {
			super.doPrint(output, depth);
			output.append("("); //$NON-NLS-1$
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					if (params[i] != null)
						params[i].print(output, depth+1);
					if (i < params.length-1)
						output.append(", "); //$NON-NLS-1$
				}
			}
			output.append(")"); //$NON-NLS-1$
		}
		@Override
		public boolean modifiable(C4ScriptParser context) {
			IType t = getType(context);
			return t.canBeAssignedFrom(C4TypeSet.REFERENCE_OR_ANY_OR_UNKNOWN);
		}
		@Override
		public boolean hasSideEffects() {
			return true;
		}
		@Override
		public IType getType(C4ScriptParser context) {
			C4Declaration d = getDeclaration(context);
			
			// look for gathered type information
			IType stored = context.queryTypeOfExpression(this, null);
			if (stored != null)
				return stored;
			
			// calling this() as function -> return object type belonging to script
			if (params.length == 0 && (d == getCachedFuncs(context).This || d == C4Variable.THIS)) {
				C4Object obj = context.getContainerObject();
				if (obj != null)
					return obj;
			}
			
			// it's a criteria search (FindObjects etc) so guess return type from arguments passed to the criteria search function
			if (isCriteriaSearch()) {
				IType t = searchCriteriaAssumedResult(context);
				if (t != null) {
					C4Function f = (C4Function) d;
					if (f.getReturnType() == C4Type.ARRAY)
						return new C4ArrayType(t);
					else
						return t;
				}
			}
			
			// it's either a Find* or Create* function that takes some object type specifier as first argument - return that type
			// (FIXME: could be triggered for functions which don't actually return objects matching the type passed as first argument)
			if (params != null && params.length >= 1 && d instanceof C4Function && ((C4Function)d).getReturnType() == C4Type.OBJECT && (declarationName.startsWith("Create") || declarationName.startsWith("Find"))) { //$NON-NLS-1$ //$NON-NLS-2$
				IType t = params[0].getType(context);
				if (t instanceof C4ObjectType)
					return ((C4ObjectType)t).getType();
			}
			
			// GetID() for this
			if (params.length == 0 && d == getCachedFuncs(context).GetID) { //$NON-NLS-1$
				IType t = getPredecessorInSequence() == null ? context.getContainerObject() : getPredecessorInSequence().getType(context);
				if (t instanceof C4Object)
					return ((C4Object)t).getObjectType();
			}
			
			// generic typed d (variable or function)
			if (d instanceof ITypedDeclaration) {
				C4Object obj = ((ITypedDeclaration)d).getObjectType();
				if (obj != null)
					return obj;
			}

			// function that does not return a reference: return typeset out of object type and generic type
			if (d instanceof C4Function && ((C4Function)d).getReturnType() != C4Type.REFERENCE) {
				C4Function function = (C4Function) d;
				return function.getCombinedType();
			}
			else
				return super.getType(context);
		}
		@Override
		public boolean isValidInSequence(ExprElm elm, C4ScriptParser context) {
			return super.isValidInSequence(elm, context) || elm instanceof MemberOperator;	
		}
		@Override
		protected C4Declaration getDeclImpl(C4ScriptParser parser) {
			if (declarationName.equals(Keywords.Return))
				return null;
			if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited)) {
				return parser.getActiveFunc().getInherited();
			}
			ExprElm p = getPredecessorInSequence();
			C4ScriptBase lookIn = p == null ? parser.getContainer() : p.guessObjectType(parser);
			if (lookIn != null) {
				FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
				info.setSearchOrigin(parser.getContainer());
				C4Declaration field = lookIn.findFunction(declarationName, info);
				// might be a variable called as a function (not after '->')
				if (field == null && p == null)
					field = lookIn.findVariable(declarationName, info);
				return field;
			} else if (p != null) {
				// find global function
				C4Declaration declaration = parser.getContainer().getIndex().findGlobalFunction(declarationName);
				if (declaration == null)
					declaration = parser.getContainer().getIndex().getEngine().findFunction(declarationName);

				// only return found declaration if it's the only choice 
				if (declaration != null) {
					List<C4Declaration> allFromLocalIndex = parser.getContainer().getIndex().getDeclarationMap().get(declarationName);
					C4Declaration decl = parser.getContainer().getEngine().findLocalFunction(declarationName, false);
					if (
							(allFromLocalIndex != null ? allFromLocalIndex.size() : 0) +
							(decl != null ? 1 : 0) == 1
					)
						return declaration;
				}
			}
			return null;
		}
		@Override
		public void reportErrors(final C4ScriptParser context) throws ParsingException {
			super.reportErrors(context);
			
			// notify parser about unnamed parameter usage
			if (declaration == getCachedFuncs(context).Par) {
				if (params.length > 0) {
					context.unnamedParamaterUsed(params[0]);
				}
				else
					context.unnamedParamaterUsed(NumberLiteral.ZERO);
			}
			// return as function
			else if (declarationName.equals(Keywords.Return)) {
				if (context.getStrictLevel() >= 2)
					context.errorWithCode(ParserErrorCode.ReturnAsFunction, this, true);
				else
					context.warningWithCode(ParserErrorCode.ReturnAsFunction, this);
			}
			else {
				
				// inherited/_inherited not allowed in non-strict mode
				if (context.getStrictLevel() <= 0) {
					if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited)) {
						context.errorWithCode(ParserErrorCode.InheritedDisabledInStrict0, this);
					}
				}
				
				// variable as function
				if (declaration instanceof C4Variable) {
					if (params.length == 0) {
						// no warning when in #strict mode
						if (context.getStrictLevel() >= 2)
							context.warningWithCode(ParserErrorCode.VariableCalled, this, declaration.getName());
					} else {
						context.errorWithCode(ParserErrorCode.VariableCalled, this, true, declaration.getName());
					}
				}
				else if (declaration instanceof C4Function) {
					C4Function f = (C4Function)declaration;
					if (f.getVisibility() == C4FunctionScope.GLOBAL) {
						context.getContainer().addUsedProjectScript(f.getScript());
					}
					int givenParam = 0;
					boolean specialCaseHandled = false;
					// yay for special cases ~ blörgs, don't bother
					/*CachedEngineFuncs cachedFuncs = getCachedFuncs(context);
					if (params.length >= 3 &&  (f == cachedFuncs.AddCommand || f == cachedFuncs.AppendCommand || f == cachedFuncs.SetCommand)) {
						// look if command is "Call"; if so treat parms 2, 3, 4 as any
						Object command = params[1].evaluateAtParseTime(context.getContainer());
						if (command instanceof String && command.equals("Call")) { //$NON-NLS-1$
							for (C4Variable parm : f.getParameters()) {
								if (givenParam >= params.length)
									break;
								ExprElm given = params[givenParam++];
								if (given == null)
									continue;
								IType parmType = givenParam >= 2 && givenParam <= 4 ? C4Type.ANY : parm.getType();
								if (!given.validForType(parmType, context))
									context.warningWithCode(ParserErrorCode.IncompatibleTypes, given, parmType, given.getType(context));
								else
									given.expectedToBeOfType(parmType, context);
							}
							specialCaseHandled = true;
						}
					}*/
					
					// another one: Schedule ... parse passed expression and check it's correctness
					if (!specialCaseHandled && params.length >= 1 && f.getName().equals("Schedule")) {
						IType objType = params.length >= 4 ? params[3].getType(context) : context.getContainerObject();
						C4Object obj = objType != null ? C4TypeSet.objectIngredient(objType) : null;
						Object scriptExpr = params[0].evaluateAtParseTime(obj);
						if (scriptExpr instanceof String) {
							C4ScriptParser.parseStandaloneStatement((String)scriptExpr, context.getActiveFunc(), null, new IMarkerListener() {
								@Override
								public void markerEncountered(ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) {
									// ignore complaining about missing ';'
									if (code == ParserErrorCode.TokenExpected && args[0].equals(";"))
										return;
									try {
										context.markerWithCode(code, params[0].getExprStart()+1+markerStart, params[0].getExprStart()+1+markerEnd, true, severity, args);
									} catch (ParsingException e) {
										// shouldn't happen
										e.printStackTrace();
									}
								}
							});
						}
					}
					
					// not a special case... check regular parameter types
					if (!specialCaseHandled) {
						for (C4Variable parm : f.getParameters()) {
							if (givenParam >= params.length)
								break;
							ExprElm given = params[givenParam++];
							if (given == null)
								continue;
							if (!given.validForType(parm.getType(), context))
								context.warningWithCode(ParserErrorCode.IncompatibleTypes, given, parm.getType(), given.getType(context));
							else
								given.expectedToBeOfType(parm.getType(), context);
						}
					}
					
					// warn about too many parameters
					// try again, but only for engine functions
					if (f.isEngineDeclaration() && !declarationName.equals(Keywords.SafeInherited) && f.tooManyParameters(actualParmsNum())) {
						context.addLatentMarker(ParserErrorCode.TooManyParameters, this, IMarker.SEVERITY_WARNING, f, f.getParameters().size(), actualParmsNum(), f.getName());
					}
					
				}
				else if (declaration == null && getPredecessorInSequence() == null) {
					if (declarationName.equals(Keywords.Inherited)) {
						context.errorWithCode(ParserErrorCode.NoInheritedFunction, getExprStart(), getExprStart()+declarationName.length(), true, context.getActiveFunc().getName(), true);
					}
					// _inherited yields no warning or error
					else if (!declarationName.equals(Keywords.SafeInherited)) {
						context.errorWithCode(ParserErrorCode.UndeclaredIdentifier, getExprStart(), getExprStart()+declarationName.length(), true, declarationName, true);
					}
				}
			}
		}
		public int actualParmsNum() {
			int result = params.length;
			while (result > 0 && params[result-1] instanceof Ellipsis)
				result--;
			return result;
		}
		@Override
		public ExprElm[] getSubElements() {
			return params;
		}
		@Override
		public void setSubElements(ExprElm[] elms) {
			params = elms;
		}
		private boolean isCriteriaSearch() {
			return declaration instanceof C4Function && ((C4Function)declaration).isCriteriaSearch;
		}
		protected BinaryOp applyOperatorTo(C4ScriptParser parser, ExprElm[] parms, C4ScriptOperator operator) throws CloneNotSupportedException {
			BinaryOp op = new BinaryOp(operator);
			BinaryOp result = op;
			for (int i = 0; i < parms.length; i++) {
				ExprElm one = parms[i].optimize(parser);
				ExprElm two = i+1 < parms.length ? parms[i+1] : null;
				if (op.getLeftSide() == null)
					op.setLeftSide(one);
				else if (two == null) {
					op.setRightSide(one);
				}
				else {
					BinaryOp nu = new BinaryOp(operator);
					op.setRightSide(nu);
					nu.setLeftSide(one);
					op = nu;
				}
			}
			return result;
		}
		@Override
		public ExprElm optimize(C4ScriptParser parser) throws CloneNotSupportedException {

			// And(ugh, blugh) -> ugh && blugh
			C4ScriptOperator replOperator = C4ScriptOperator.oldStyleFunctionReplacement(declarationName);
			if (replOperator != null && params.length == 1) {
				// LessThan(x) -> x < 0
				if (replOperator.getNumArgs() == 2)
					return new BinaryOp(replOperator, params[0].optimize(parser), NumberLiteral.ZERO);
				ExprElm n = params[0].optimize(parser);
				if (n instanceof BinaryOp)
					n = new Parenthesized(n);
				return new UnaryOp(replOperator, replOperator.isPostfix() ? UnaryOp.Placement.Postfix : UnaryOp.Placement.Prefix, n);
			}
			if (replOperator != null && params.length >= 2) {
				return applyOperatorTo(parser, params, replOperator);
			}

			// ObjectCall(ugh, "UghUgh", 5) -> ugh->UghUgh(5)
			if (params.length >= 2 && declaration == getCachedFuncs(parser).ObjectCall && params[1] instanceof StringLiteral && (alwaysConvertObjectCalls || !this.containedInLoopHeaderOrNotStandaloneExpression()) && !params[0].hasSideEffects()) {
				ExprElm[] parmsWithoutObject = new ExprElm[params.length-2];
				for (int i = 0; i < parmsWithoutObject.length; i++)
					parmsWithoutObject[i] = params[i+2].optimize(parser);
				String lit = ((StringLiteral)params[1]).stringValue();
				if (lit.length() > 0 && lit.charAt(0) != '~') {
					return alwaysConvertObjectCalls && this.containedInLoopHeaderOrNotStandaloneExpression()
					? new Sequence(new ExprElm[] {
							params[0].optimize(parser),
							new MemberOperator(false, true, null, 0),
							new CallFunc(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
					)
					: new IfStatement(params[0].optimize(parser),
							new SimpleStatement(new Sequence(new ExprElm[] {
									params[0].optimize(parser),
									new MemberOperator(false, true, null, 0),
									new CallFunc(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
							)),
							null
					);
				}
			}

			// OCF_Awesome() -> OCF_Awesome
			if (params.length == 0 && declaration instanceof C4Variable) {
				return new AccessVar(declarationName);
			}

			// also check for not-nullness since in OC Var/Par are gone and declaration == ...Par returns true -.-
			
			// Par(5) -> nameOfParm6
			if (params.length <= 1 && declaration != null && declaration == getCachedFuncs(parser).Par && (params.length == 0 || params[0] instanceof NumberLiteral)) {
				NumberLiteral number = params.length > 0 ? (NumberLiteral) params[0] : NumberLiteral.ZERO;
				if (number.intValue() >= 0 && number.intValue() < parser.getActiveFunc().getParameters().size() && parser.getActiveFunc().getParameters().get(number.intValue()).isActualParm())
					return new AccessVar(parser.getActiveFunc().getParameters().get(number.intValue()).getName());
			}
			
			// SetVar(5, "ugh") -> Var(5) = "ugh"
			if (params.length == 2 && declaration != null && (declaration == getCachedFuncs(parser).SetVar || declaration == getCachedFuncs(parser).SetLocal || declaration == getCachedFuncs(parser).AssignVar)) {
				return new BinaryOp(C4ScriptOperator.Assign, new CallFunc(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), params[0].optimize(parser)), params[1].optimize(parser)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			// DecVar(0) -> Var(0)--
			if (params.length <= 1 && declaration != null && (declaration == getCachedFuncs(parser).DecVar || declaration == getCachedFuncs(parser).IncVar)) {
				return new UnaryOp(declaration == getCachedFuncs(parser).DecVar ? C4ScriptOperator.Decrement : C4ScriptOperator.Increment, Placement.Prefix,
						new CallFunc(getCachedFuncs(parser).Var.getName(), new ExprElm[] {
							params.length == 1 ? params[0].optimize(parser) : NumberLiteral.ZERO
						})
				);
			}

			// Call("Func", 5, 5) -> Func(5, 5)
			if (params.length >= 1 && declaration != null && declaration == getCachedFuncs(parser).Call && params[0] instanceof StringLiteral) {
				String lit = ((StringLiteral)params[0]).stringValue();
				if (lit.length() > 0 && lit.charAt(0) != '~') {
					ExprElm[] parmsWithoutName = new ExprElm[params.length-1];
					for (int i = 0; i < parmsWithoutName.length; i++)
						parmsWithoutName[i] = params[i+1].optimize(parser);
					return new CallFunc(((StringLiteral)params[0]).stringValue(), parmsWithoutName);
				}
			}

			return super.optimize(parser);
		}

		private boolean containedInLoopHeaderOrNotStandaloneExpression() {
			SimpleStatement simpleStatement = null;
			for (ExprElm p = getParent(); p != null; p = p.getParent()) {
				if (p instanceof Block)
					break;
				if (p instanceof ILoop) {
					if (simpleStatement != null && simpleStatement == ((ILoop)p).getBody())
						return false;
					return true;
				}
				if (!(p instanceof SimpleStatement))
					return true;
				else
					simpleStatement = (SimpleStatement) p;
			} 
			return false;
		}

		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			return new DeclarationRegion(getDeclaration(parser), new Region(getExprStart(), declarationName.length()));
		}

		public C4Object searchCriteriaAssumedResult(C4ScriptParser context) {
			C4Object result = null;
			// parameters to FindObjects itself are also &&-ed together
			if (declarationName.equals("Find_And") || isCriteriaSearch()) { //$NON-NLS-1$
				for (ExprElm parm : params) {
					if (parm instanceof CallFunc) {
						CallFunc call = (CallFunc)parm;
						C4Object t = call.searchCriteriaAssumedResult(context);
						if (t != null) {
							if (result == null)
								result = t;
							else {
								if (t.includes(result))
									result = t;
							}
						}
					}
				}
			}
			else if (declarationName.equals("Find_ID")) { //$NON-NLS-1$
				if (params.length > 0) {
					result = params[0].guessObjectType(context);
				}
			}
			return result;
		}
		public ExprElm getReturnArg() {
			if (params.length == 1)
				return params[0];
			return new Tuple(params);
		}
		@Override
		public ControlFlow getControlFlow() {
			return declarationName.equals(Keywords.Return) ? ControlFlow.Return : super.getControlFlow();
		}
		public ExprElm[] getParams() {
			return params;
		}
		public int indexOfParm(ExprElm parm) {
			for (int i = 0; i < params.length; i++)
				if (params[i] == parm)
					return i;
			return -1;
		}
		@Override
		public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
			C4Declaration d = getDeclaration();
			CachedEngineFuncs cache = getCachedFuncs(parser);
			if (Utilities.isAnyOf(d, cache.Var, cache.Local, cache.Par)) {
				Object ev;
				if (getParams().length == 1 && (ev = getParams()[0].evaluateAtParseTime(parser.getContainer())) != null) {
					if (ev instanceof Number) {
						// Var() with a sane constant number
						return new VarFunctionsTypeInformation((C4Function) d, ((Number)ev).intValue());
					}
				}
			}
			else if (d instanceof C4Function) {
				C4Function f = (C4Function) d;
				IType retType = f.getReturnType();
				if (retType == null || !(retType.containsAnyTypeOf(C4Type.ANY, C4Type.REFERENCE)))
					return new FunctionReturnTypeInformation((C4Function)d);
			}
			return super.createStoredTypeInformation(parser);
		}
		
		@Override
		public Object evaluate(IEvaluationContext context) {
		    if (declaration instanceof C4Function) {
		    	Object[] args = Utilities.map(getParams(), Object.class, EVALUATE_EXPR);
		    	return ((C4Function)declaration).invoke(args);
		    }
		    else
		    	return null;
		}
	}

	public static class Operator extends Value {
		private final C4ScriptOperator operator;

		@Override
		public IType getType(C4ScriptParser context) {
			return operator.getResultType();
		}

		public Operator(C4ScriptOperator operator) {
			super();
			this.operator = operator;
		}

		public C4ScriptOperator getOperator() {
			return operator;
		}

		@Override
		public boolean hasSideEffects() {
			return getOperator().modifiesArgument() || super.hasSideEffects();
		}

		@Override
		public boolean modifiable(C4ScriptParser context) {
			return getOperator().returnsRef();
		}

	}

	public static class BinaryOp extends Operator {
		
		@Override
		public IType getType(C4ScriptParser context) {
			switch (getOperator()) {
			// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
			case And: case Or:
				IType leftSideType = getLeftSide().getType(context);
				IType rightSideType = getRightSide().getType(context);
				if (leftSideType == rightSideType)
					return leftSideType;
				else
					return C4Type.ANY;
			default:
				return super.getType(context);
			}
		}

		@Override
		public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
			// #strict 2: ne -> !=, S= -> ==
			if (context.getStrictLevel() >= 2) {
				C4ScriptOperator op = getOperator();
				if (op == C4ScriptOperator.StringEqual || op == C4ScriptOperator.eq)
					op = C4ScriptOperator.Equal;
				else if (op == C4ScriptOperator.ne)
					op = C4ScriptOperator.NotEqual;
				if (op != getOperator()) {
					return new BinaryOp(op, getLeftSide().optimize(context), getRightSide().optimize(context));
				}
			}

			// blub() && blab() && return(1); -> {blub(); blab(); return(1);}
			if ((getOperator() == C4ScriptOperator.And || getOperator() == C4ScriptOperator.Or) && (getParent() instanceof SimpleStatement)) {// && getRightSide().isReturn()) {
				ExprElm block = convertOperatorHackToBlock(context);
				if (block != null)
					return block;
			}

			return super.optimize(context);
		}

		private ExprElm convertOperatorHackToBlock(C4ScriptParser context) throws CloneNotSupportedException {
			LinkedList<ExprElm> leftSideArguments = new LinkedList<ExprElm>();
			ExprElm r;
			boolean works = true;
			C4ScriptOperator hackOp = this.getOperator();
			// gather left sides (must not be operators)
			for (r = getLeftSide(); r instanceof BinaryOp; r = ((BinaryOp)r).getLeftSide()) {
				BinaryOp op = (BinaryOp)r;
				if (op.getOperator() != hackOp) {
					works = false;
					break;
				}
				if (op.getRightSide() instanceof BinaryOp) {
					works = false;
					break;
				}
				leftSideArguments.addLast(op.getRightSide());
			}
			// return at the right end signals this should rather be a block
			if (works) {
				leftSideArguments.addFirst(r);
				List<Statement> statements = new LinkedList<Statement>();
				// wrap expressions in statements
				for (ExprElm ex : leftSideArguments) {
					statements.add(new SimpleStatement(ex.optimize(context)));
				}
				// convert func call to proper return statement
				if (getRightSide().getControlFlow() == ControlFlow.Return)
					statements.add(new ReturnStatement(((CallFunc)getRightSide()).getReturnArg().optimize(context)));
				else
					statements.add(new SimpleStatement(getRightSide().optimize(context)));
				return new Block(statements);
			}
			return null;
		}

		private ExprElm leftSide, rightSide;

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {leftSide, rightSide};
		}

		@Override
		public void setSubElements(ExprElm[] elements) {
			leftSide  = elements[0];
			rightSide = elements[1];
		}

		public BinaryOp(C4ScriptOperator operator, ExprElm leftSide, ExprElm rightSide) {
			super(operator);
			setLeftSide(leftSide);
			setRightSide(rightSide);
		}

		public void checkTopLevelAssignment(C4ScriptParser parser) throws ParsingException {
			if (!getOperator().modifiesArgument())
				parser.warningWithCode(ParserErrorCode.NoAssignment, this);
		}

		public BinaryOp(C4ScriptOperator op) {
			super(op);
		}

		public ExprElm getLeftSide() {
			return leftSide;
		}

		public ExprElm getRightSide() {
			return rightSide;
		}

		public void setLeftSide(ExprElm leftSide) {
			this.leftSide = leftSide;
			leftSide.setParent(this);
		}

		public void setRightSide(ExprElm rightSide) {
			this.rightSide = rightSide;
			rightSide.setParent(this);
		}

		public void doPrint(ExprWriter output, int depth) {

			// put brackets around operands in case some transformation messed up prioritization
			boolean needsBrackets = leftSide instanceof BinaryOp && getOperator().getPriority() > ((BinaryOp)leftSide).getOperator().getPriority();
			if (needsBrackets)
				output.append("("); //$NON-NLS-1$
			leftSide.print(output, depth+1);
			if (needsBrackets)
				output.append(")"); //$NON-NLS-1$

			output.append(" "); //$NON-NLS-1$
			output.append(getOperator().getOperatorName());
			output.append(" "); //$NON-NLS-1$

			needsBrackets = rightSide instanceof BinaryOp && getOperator().getPriority() > ((BinaryOp)rightSide).getOperator().getPriority();
			if (needsBrackets)
				output.append("("); //$NON-NLS-1$
			rightSide.print(output, depth+1);
			if (needsBrackets)
				output.append(")"); //$NON-NLS-1$
		}

		@Override
		public void reportErrors(C4ScriptParser context) throws ParsingException {
			getLeftSide().reportErrors(context);
			getRightSide().reportErrors(context);
			// sanity
			setExprRegion(getLeftSide().getExprStart(), getRightSide().getExprEnd());
			// i'm an assignment operator and i can't modify my left side :C
			if (getOperator().modifiesArgument() && !getLeftSide().modifiable(context)) {
				context.errorWithCode(ParserErrorCode.ExpressionNotModifiable, getLeftSide(), true);
			}
			// obsolete operators in #strict 2
			if ((getOperator() == C4ScriptOperator.StringEqual || getOperator() == C4ScriptOperator.ne) && (context.getStrictLevel() >= 2)) {
				context.warningWithCode(ParserErrorCode.ObsoleteOperator, this, getOperator().getOperatorName());
			}
			// wrong parameter types
			if (!getLeftSide().validForType(getOperator().getFirstArgType(), context))
				context.warningWithCode(ParserErrorCode.IncompatibleTypes, getLeftSide(), getOperator().getFirstArgType(), getLeftSide().getType(context));
			if (!getRightSide().validForType(getOperator().getSecondArgType(), context))
				context.warningWithCode(ParserErrorCode.IncompatibleTypes, getRightSide(), getOperator().getSecondArgType(), getRightSide().getType(context));

			IType expectedLeft, expectedRight;
			switch (getOperator()) {
			case Assign: case Equal:
				expectedLeft = expectedRight = null;
				break;
			default:
				expectedLeft = getOperator().getFirstArgType();
				expectedRight = getOperator().getSecondArgType();
			}
			
			if (expectedLeft != null)
				getLeftSide().expectedToBeOfType(expectedLeft, context);
			if (expectedRight != null)
				getRightSide().expectedToBeOfType(expectedRight, context);

			if (getOperator() == C4ScriptOperator.Assign) {
				getLeftSide().inferTypeFromAssignment(getRightSide(), context);
			}
		}

		@Override
		public Object evaluateAtParseTime(C4ScriptBase context) {
			try {
				Object leftSide  = getOperator().getFirstArgType().convert(this.getLeftSide().evaluateAtParseTime(context));
				Object rightSide = getOperator().getSecondArgType().convert(this.getRightSide().evaluateAtParseTime(context));
				if (leftSide != null && leftSide != ExprElm.EVALUATION_COMPLEX) {
					switch (getOperator()) {
					case And:
						// false && <anything> => false
						if (leftSide.equals(false))
							return false;
					case Or:
						// true || <anything> => true 
						if (leftSide.equals(true))
							return true;
					}
					if (rightSide != null && rightSide != ExprElm.EVALUATION_COMPLEX) {
						return evaluateOn(leftSide, rightSide);
					}
				}
			}
			catch (ClassCastException e) {}
			catch (NullPointerException e) {}
			return super.evaluateAtParseTime(context);
		}

		private Object evaluateOn(Object leftSide, Object rightSide) {
	        switch (getOperator()) {
	        case Add:
	        	return ((Number)leftSide).longValue() + ((Number)rightSide).longValue();
	        case Subtract:
	        	return ((Number)leftSide).longValue() - ((Number)rightSide).longValue();
	        case Multiply:
	        	return ((Number)leftSide).longValue() * ((Number)rightSide).longValue();
	        case Divide:
	        	return ((Number)leftSide).longValue() / ((Number)rightSide).longValue();
	        case Modulo:
	        	return ((Number)leftSide).longValue() % ((Number)rightSide).longValue();
	        case Larger:
	        	return ((Number)leftSide).longValue() > ((Number)rightSide).longValue();
	        case Smaller:
	        	return ((Number)leftSide).longValue() < ((Number)rightSide).longValue();
	        case LargerEqual:
	        	return ((Number)leftSide).longValue() >= ((Number)rightSide).longValue();
	        case SmallerEqual:
	        	return ((Number)leftSide).longValue() <= ((Number)rightSide).longValue();
	        case Equal:
	        	return leftSide.equals(rightSide);
	        default:
	        	return null;
	        }
        }
		
		@Override
		public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		    Object left = getLeftSide().evaluate(context);
		    Object right = getRightSide().evaluate(context);
		    if (left != null && right != null)
		    	return evaluateOn(left, right);
		    else
		    	return null;
		}

	}

	public static class Parenthesized extends Value {
		private ExprElm innerExpr;

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {innerExpr};
		}
		@Override
		public void setSubElements(ExprElm[] elements) {
			innerExpr = elements[0];
		}
		public Parenthesized(ExprElm innerExpr) {
			super();
			this.innerExpr = innerExpr;
			assignParentToSubElements();
		}
		public void doPrint(ExprWriter output, int depth) {
			output.append("("); //$NON-NLS-1$
			innerExpr.print(output, depth+1);
			output.append(")"); //$NON-NLS-1$
		}
		public IType getType(C4ScriptParser context) {
			return innerExpr.getType(context);
		}
		@Override
		public boolean modifiable(C4ScriptParser context) {
			return innerExpr.modifiable(context);
		}
		@Override
		public boolean hasSideEffects() {
			return innerExpr.hasSideEffects();
		}

		public ExprElm getInnerExpr() {
			return innerExpr;
		}

		@Override
		public ExprElm optimize(C4ScriptParser parser)
		throws CloneNotSupportedException {
			if (!(getParent() instanceof Operator) && !(getParent() instanceof Sequence))
				return innerExpr.optimize(parser);
			return super.optimize(parser);
		}

		@Override
		public Object evaluateAtParseTime(C4ScriptBase context) {
			return innerExpr.evaluateAtParseTime(context);
		}

	}

	public static class UnaryOp extends Operator {

		public enum Placement {
			Prefix,
			Postfix
		}

		private final Placement placement;
		private ExprElm argument;

		public UnaryOp(C4ScriptOperator operator, Placement placement, ExprElm argument) {
			super(operator);
			this.placement = placement;
			this.argument = argument;
			this.argument.setParent(this);
		}

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {argument};
		}

		@Override
		public void setSubElements(ExprElm[] elements) {
			argument = elements[0];
		}

		private boolean needsSpace(UnaryOp other) {
			return this.getOperator().spaceNeededBetweenMeAnd(other.getOperator());
		}

		public void doPrint(ExprWriter output, int depth) {
			UnaryOp unop = (argument instanceof UnaryOp) ? (UnaryOp)argument : null;
			if (unop != null && unop.placement != this.placement)
				unop = null;
			if (placement == Placement.Postfix) {
				argument.print(output, depth+1);
				if (unop != null && needsSpace(unop))
					output.append(" "); // - -5 -.- //$NON-NLS-1$
				output.append(getOperator().getOperatorName());
			} else {
				output.append(getOperator().getOperatorName());
				if (unop != null && needsSpace(unop))
					output.append(" "); // - -5 -.- //$NON-NLS-1$
				argument.print(output, depth+1);
			}
		}

		public ExprElm getArgument() {
			return argument;
		}

		@Override
		public void reportErrors(C4ScriptParser context) throws ParsingException {
			getArgument().reportErrors(context);
			if (getOperator().modifiesArgument() && !getArgument().modifiable(context)) {
				//				System.out.println(getArgument().toString() + " does not behave");
				context.errorWithCode(ParserErrorCode.ExpressionNotModifiable, getArgument(), true);
			}
			if (!getArgument().validForType(getOperator().getFirstArgType(), context)) {
				context.warningWithCode(ParserErrorCode.IncompatibleTypes, getArgument(), getOperator().getFirstArgType().toString(), getArgument().getType(context).toString());
			}
			getArgument().expectedToBeOfType(getOperator().getFirstArgType(), context);
		}

		@Override
		public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
			// could happen when argument is transformed to binary operator
			ExprElm arg = getArgument().optimize(context);
			if (arg instanceof BinaryOp)
				return new UnaryOp(getOperator(), placement, new Parenthesized(arg));
			if (getOperator() == C4ScriptOperator.Not && arg instanceof Parenthesized) {
				Parenthesized brackets = (Parenthesized)arg;
				if (brackets.getInnerExpr() instanceof BinaryOp) {
					BinaryOp op = (BinaryOp) brackets.getInnerExpr();
					if (op.getOperator() == C4ScriptOperator.Equal) {
						return new BinaryOp(C4ScriptOperator.NotEqual, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
					}
					else if (op.getOperator() == C4ScriptOperator.NotEqual) {
						return new BinaryOp(C4ScriptOperator.Equal, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
					}
					else if (op.getOperator() == C4ScriptOperator.StringEqual) {
						return new BinaryOp(C4ScriptOperator.ne, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
					}
					else if (op.getOperator() == C4ScriptOperator.ne) {
						return new BinaryOp(C4ScriptOperator.StringEqual, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
					}
				}
			}
			return super.optimize(context);
		}

		@Override
		public boolean isConstant() {
			return argument.isConstant();
		}

		@Override
		public boolean modifiable(C4ScriptParser context) {
			return placement == Placement.Prefix && getOperator().returnsRef();
		}

		@Override
		public Object evaluateAtParseTime(C4ScriptBase context) {
			try {
				Object ev = argument.evaluateAtParseTime(context);
				Object conv = getOperator().getFirstArgType().convert(ev);
				switch (getOperator()) {
				case Not:
					return !(Boolean)conv;
				case Subtract:
					return -((Number)conv).longValue();
				case Add:
					return conv;
				}
			}
			catch (ClassCastException e) {}
			catch (NullPointerException e) {}
			return super.evaluateAtParseTime(context);
		}

	}

	public static class Literal<T> extends Value {
		private final T literal;

		@Override
		public void expectedToBeOfType(IType type, C4ScriptParser parser, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
			// constantly steadfast do i resist the pressure of expectancy lied upon me
		}

		@Override
		public void inferTypeFromAssignment(ExprElm arg0, C4ScriptParser arg1) {
			// don't care
		}

		public Literal(T literal) {
			super();
			this.literal = literal;
		}

		public T getLiteral() {
			return literal;
		}

		@Override
		public boolean modifiable(C4ScriptParser context) {
			return false;
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
			return predecessor == null;
		}

		@Override
		public boolean isConstant() {
			return true;
		}

		@Override
		public T evaluateAtParseTime(C4ScriptBase context) {
			return literal;
		}
		
		@Override
		public Object evaluate(IEvaluationContext context) {
		    return literal;
		}
		
		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append(getLiteral().toString());
		}

	}

	public static final class NumberLiteral extends Literal<Long> {

		public static final NumberLiteral ZERO = new NumberLiteral(0);

		private boolean hex;

		public NumberLiteral(long value, boolean hex) {
			super(Long.valueOf(value));
			this.hex = hex;
		}

		public NumberLiteral(long value) {
			this(value, false);
		}

		public long longValue() {
			return getLiteral().longValue();
		}

		public int intValue() {
			return (int)longValue();
		}

		@Override
		public void doPrint(ExprWriter output, int depth) {
			if (hex) {
				output.append("0x"); //$NON-NLS-1$
				output.append(Long.toHexString(longValue()).toUpperCase());
			}
			else
				super.doPrint(output, depth);
		}

		public IType getType(C4ScriptParser context) {
			if (longValue() == 0)
				return C4Type.ANY; // FIXME: to prevent warnings when assigning 0 to object-variables
			return C4Type.INT;
		}

		@Override
		public boolean canBeConvertedTo(IType otherType, C4ScriptParser context) {
			// 0 is the NULL object or NULL string
			return (longValue() == 0 && (otherType.canBeAssignedFrom(C4TypeSet.STRING_OR_OBJECT))) || super.canBeConvertedTo(otherType, context);
		}

		public boolean isHex() {
			return hex;
		}

		public void setHex(boolean hex) {
			this.hex = hex;
		}
		
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			long val = longValue();
			//ExprElm region;
			if (getParent() instanceof UnaryOp && ((UnaryOp)getParent()).getOperator() == C4ScriptOperator.Subtract) {
				val = -val;
				//region = getParent();
			}
			/*else
				region = this;
			/* who needs it -.-
			if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE)
				parser.warningWithCode(ParserErrorCode.OutOfIntRange, region, String.valueOf(val));
			*/
		}

	}

	public static final class StringLiteral extends Literal<String> {
		public StringLiteral(String literal) {
			super(literal != null ? literal : ""); //$NON-NLS-1$
		}

		public String stringValue() {
			return getLiteral();
		}
		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append("\""); //$NON-NLS-1$
			output.append(stringValue());
			output.append("\""); //$NON-NLS-1$
		}

		@Override
		public IType getType(C4ScriptParser context) {
			return C4Type.STRING;
		}

		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {

			// first check if a string tbl entry is referenced
			DeclarationRegion result = getStringTblEntryForLanguagePref(offset-1, parser.getContainer(), true);
			if (result != null)
				return result;

			// look whether this string can be considered a function name
			if (getParent() instanceof CallFunc) {
				CallFunc parentFunc = (CallFunc) getParent();
				int myIndex = parentFunc.indexOfParm(this);

				//  link to functions that are called indirectly

				// GameCall: look for nearest scenario and find function in its script
				if (myIndex == 0 && parentFunc.getDeclaration() == getCachedFuncs(parser).GameCall) {
					ClonkIndex index = parser.getContainer().getIndex();
					C4Scenario scenario = ClonkIndex.pickNearest(parser.getContainer().getResource(), index.getIndexedScenarios());
					if (scenario != null) {
						C4Function scenFunc = scenario.findFunction(stringValue());
						if (scenFunc != null)
							return new DeclarationRegion(scenFunc, identifierRegion());
					}
				}
				
				else if (myIndex == 0 && parentFunc.getDeclarationName().equals("Schedule")) { //$NON-NLS-1$
					// parse first parm of Schedule as expression and see what goes
					ExpressionLocator locator = new ExpressionLocator(offset-1); // make up for '"'
					try {
						C4ScriptParser.parseStandaloneStatement(getLiteral(), parser.getActiveFunc(), locator);
					} catch (ParsingException e) {}
					if (locator.getExprAtRegion() != null) {
						DeclarationRegion reg = locator.getExprAtRegion().declarationAt(offset, parser);
						if (reg != null)
							return reg.addOffsetInplace(getExprStart()+1);
						else
							return null;
					}
					else
						return super.declarationAt(offset, parser);	
				}

				// LocalN: look for local var in object
				else if (myIndex == 0 && parentFunc.getDeclaration() == getCachedFuncs(parser).LocalN) {
					C4Object typeToLookIn = parentFunc.getParams().length > 1 ? parentFunc.getParams()[1].guessObjectType(parser) : null;
					if (typeToLookIn == null && parentFunc.getPredecessorInSequence() != null)
						typeToLookIn = parentFunc.getPredecessorInSequence().guessObjectType(parser);
					if (typeToLookIn == null)
						typeToLookIn = parser.getContainerObject();
					if (typeToLookIn != null) {
						C4Variable var = typeToLookIn.findVariable(stringValue());
						if (var != null)
							return new DeclarationRegion(var, identifierRegion());
					}
				}

				// look for function called by Call("...")
				else if (myIndex == 0 && parentFunc.getDeclaration() == getCachedFuncs(parser).Call) {
					C4Function f = parser.getContainer().findFunction(stringValue());
					if (f != null)
						return new DeclarationRegion(f, identifierRegion());
				}

				// ProtectedCall/PrivateCall/ObjectCall, a bit more complicated than Call
				else if (myIndex == 1 && (Utilities.isAnyOf(parentFunc.getDeclaration(), getCachedFuncs(parser).ObjectCallFunctions) || parentFunc.getDeclarationName().equals("ScheduleCall"))) { //$NON-NLS-1$
					C4Object typeToLookIn = parentFunc.getParams()[0].guessObjectType(parser);
					if (typeToLookIn == null && parentFunc.getPredecessorInSequence() != null)
						typeToLookIn = parentFunc.getPredecessorInSequence().guessObjectType(parser);
					if (typeToLookIn == null)
						typeToLookIn = parser.getContainerObject();
					if (typeToLookIn != null) {
						C4Function f = typeToLookIn.findFunction(stringValue());
						if (f != null)
							return new DeclarationRegion(f, identifierRegion());
					} 
				}

			}
			return null;
		}

		private DeclarationRegion getStringTblEntryRegion(int offset) {
			int firstDollar = stringValue().lastIndexOf('$', offset-1);
			int secondDollar = stringValue().indexOf('$', offset);
			if (firstDollar != -1 && secondDollar != -1) {
				String entry = stringValue().substring(firstDollar+1, secondDollar);
				return new DeclarationRegion(null, new Region(getExprStart()+1+firstDollar, secondDollar-firstDollar+1), entry);
			}
			return null;
		}
		
		private DeclarationRegion getStringTblEntryForLanguagePref(int offset, C4ScriptBase container, boolean returnNullIfNotFound) {
			DeclarationRegion result = getStringTblEntryRegion(offset);
			if (result != null) {
				StringTbl stringTbl = container.getStringTblForLanguagePref();
				C4Declaration e = stringTbl != null ? stringTbl.getMap().get(result.getText()) : null;
				if (e == null && returnNullIfNotFound) {
					result = null;
				} else {
					result.setDeclaration(e);
				}
			}
			return result;
		}

		@Override
		public String evaluateAtParseTime(C4ScriptBase context) {
			String value = getLiteral().replaceAll("\\\"", "\"");
			int valueLen = value.length();
			StringBuilder builder = new StringBuilder(valueLen*2);
			// insert stringtbl entries
			Outer: for (int i = 0; i < valueLen;) {
				if (i+1 < valueLen) {
					switch (value.charAt(i)) {
					case '$':
						DeclarationRegion region = getStringTblEntryForLanguagePref(i+1, context, true);
						if (region != null) {
							builder.append(((NameValueAssignment)region.getDeclaration()).getValue());
							i += region.getRegion().getLength();
							continue Outer;
						}
						break;
					case '\\':
						switch (value.charAt(++i)) {
						case '"': case '\\':
							builder.append(value.charAt(i++));
							continue Outer;
						}
						break;
					}
				}
				builder.append(value.charAt(i++));
			}
			return builder.toString();
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			
			// warn about overly long strings
			int max = parser.getContainer().getIndex().getEngine().getCurrentSettings().maxStringLen;
			if (max != 0 && getLiteral().length() > max) {
				parser.warningWithCode(ParserErrorCode.StringTooLong, this, getLiteral().length(), max);
			}
			
			// stringtbl entries
			// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
			// and checking for the existence of the table entries there is overkill
			if (parser.hasAppendTo() || parser.getContainer().getResource() == null)
				return;
			String value = getLiteral();
			int valueLen = value.length();
			// warn when using non-declared string tbl entries
			for (int i = 0; i < valueLen;) {
				if (i+1 < valueLen && value.charAt(i) == '$') {
					DeclarationRegion region = getStringTblEntryRegion(i+1);
					if (region != null) {
						StringBuilder listOfLangFilesItsMissingIn = null;
						try {
							for (IResource r : (parser.getContainer().getResource() instanceof IContainer ? (IContainer)parser.getContainer().getResource() : parser.getContainer().getResource().getParent()).members()) {
								if (!(r instanceof IFile))
									continue;
								IFile f = (IFile) r;
								Matcher m = StringTbl.PATTERN.matcher(r.getName());
								if (m.matches()) {
									String lang = m.group(1);
									StringTbl tbl = (StringTbl)StringTbl.pinned(f, true, false);
									if (tbl != null) {
										if (tbl.getMap().get(region.getText()) == null) {
											if (listOfLangFilesItsMissingIn == null)
												listOfLangFilesItsMissingIn = new StringBuilder(10);
											if (listOfLangFilesItsMissingIn.length() > 0)
												listOfLangFilesItsMissingIn.append(", "); //$NON-NLS-1$
											listOfLangFilesItsMissingIn.append(lang);
										}
									}
								}
							}
						} catch (CoreException e) {}
						if (listOfLangFilesItsMissingIn != null) {
							parser.warningWithCode(ParserErrorCode.MissingLocalizations, region.getRegion(), listOfLangFilesItsMissingIn.toString());
						}
						i += region.getRegion().getLength();
						continue;
					}
				}
				++i;
			}
		}

		@Override
		public int getIdentifierStart() {
			return getExprStart()+1;
		}

		@Override
		public int getIdentifierLength() {
			return stringValue().length();
		}

	}

	public static final class IDLiteral extends Literal<C4ID> {
		public IDLiteral(C4ID literal) {
			super(literal);
		}

		public C4ID idValue() {
			return getLiteral();
		}

		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append(idValue().getName());
		}

		@Override
		public IType getType(C4ScriptParser context) {
			C4Object obj = context.getContainer().getNearestObjectWithId(idValue());
			return obj != null ? obj.getObjectType() : C4Type.ID;
		}

		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			return new DeclarationRegion(parser.getContainer().getNearestObjectWithId(idValue()), region(0));
		}

	}

	public static final class BoolLiteral extends Literal<Boolean> {
		public boolean booleanValue() {
			return getLiteral().booleanValue();
		}
		public BoolLiteral(boolean value) {
			super(Boolean.valueOf(value));
		}
		public IType getType(C4ScriptParser context) {
			return C4Type.BOOL;
		}
		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append(booleanValue() ? Keywords.True : Keywords.False);
		}
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			if (getParent() instanceof BinaryOp) {
				C4ScriptOperator op = ((BinaryOp) getParent()).getOperator();
				if (op == C4ScriptOperator.And || op == C4ScriptOperator.Or)
					parser.warningWithCode(ParserErrorCode.BoolLiteralAsOpArg, this, this.toString());
			}
		}
	}

	public static class ArrayElementExpression extends Value {

		protected ExprElm argument;

		@Override
		public IType getType(C4ScriptParser context) {
			return C4Type.UNKNOWN; // FIXME: guess type of elements
		}

		public ArrayElementExpression(ExprElm argument) {
			super();
			this.argument = argument;
			assignParentToSubElements();
		}

		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append("["); //$NON-NLS-1$
			getArgument().print(output, depth+1);
			output.append("]"); //$NON-NLS-1$
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
			return predecessor != null;
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			ExprElm arg = getArgument();
			if (arg != null)
				arg.reportErrors(parser);
		}

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {argument};
		}

		@Override
		public void setSubElements(ExprElm[] subElements) {
			argument = subElements[0];
		}

		@Override
		public boolean modifiable(C4ScriptParser context) {
			return true;
		}

		public ExprElm getArgument() {
			return argument;
		}

	}
	
	public static class ArraySliceExpression extends ArrayElementExpression {
		
		private ExprElm argument2;
		
		public ArraySliceExpression(ExprElm lo, ExprElm hi) {
			super(lo);
			this.argument2 = hi;
		}
		
		public ExprElm lo() {
			return argument;
		}
		
		public ExprElm hi() {
			return argument2;
		}
		
		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append("["); //$NON-NLS-1$
			argument.print(output, depth+1);
			output.append(":"); //$NON-NLS-1$
			argument2.print(output, depth+1);
			output.append("]"); //$NON-NLS-1$
		}
		
		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {argument, argument2};
		}
		@Override
		public void setSubElements(ExprElm[] subElements) {
			argument  = subElements[0];
			argument2 = subElements[1];
		}
		
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (argument2 != null)
				argument2.reportErrors(parser);
		}
		
		@Override
		public boolean modifiable(C4ScriptParser context) {
			return false;
		}
		
		@Override
		public IType getType(C4ScriptParser context) {
			return C4Type.ARRAY;
		}
		
	}

	public static class ArrayExpression extends Sequence {
		public ArrayExpression(ExprElm... elms) {
			super(elms);
		}

		public void doPrint(ExprWriter output, int depth) {
			output.append("["); //$NON-NLS-1$
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] != null)
					elements[i].print(output, depth+1);
				if (i < elements.length-1)
					output.append(", "); //$NON-NLS-1$
			}
			output.append("]"); //$NON-NLS-1$
		}

		@Override
		public IType getType(C4ScriptParser context) {
			return C4Type.ARRAY;
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
			return predecessor == null;
		}

		@Override
		public boolean modifiable(C4ScriptParser context) {
			return false;
		}
		
		@Override
		public Object evaluate(IEvaluationContext context) {
			return Utilities.map(getElements(), Object.class, EVALUATE_EXPR);
		}

	}

	public static class PropListExpression extends Value {
		private Pair<String, ExprElm>[] components;
		public PropListExpression(Pair<String, ExprElm>[] components) {
			this.components = components;
		}
		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append('{');
			for (int i = 0; i < components.length; i++) {
				Pair<String, ExprElm> component = components[i];
				output.append('\n');
				printIndent(output, depth-2);
				output.append(component.getFirst());
				output.append(": "); //$NON-NLS-1$
				component.getSecond().print(output, depth+1);
				if (i < components.length-1) {
					output.append(',');
				} else {
					output.append('\n'); printIndent(output, depth-3); output.append('}');
				}
			}
		}
		@Override
		public IType getType(C4ScriptParser parser) {
			return C4Type.PROPLIST;
		}
		@Override
		public boolean modifiable(C4ScriptParser parser) {
			return false;
		}
		@Override
		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser parser) {
			return predecessor == null;
		}
		@Override
		public ExprElm[] getSubElements() {
			ExprElm[] result = new ExprElm[components.length];
			for (int i = 0; i < result.length; i++)
				result[i] = components[i].getSecond();
			return result;
		}
		@Override
		public void setSubElements(ExprElm[] elms) {
			for (int i = 0; i < Math.min(elms.length, components.length); i++) {
				components[i].setSecond(elms[i]);
			}
		}
	}

	public static class Tuple extends Sequence {

		public Tuple(ExprElm[] elms) {
			super(elms);		
		}

		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append('(');
			if (elements != null) {
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] != null)
						elements[i].print(output, depth+1);
					if (i < elements.length-1)
						output.append(", "); //$NON-NLS-1$
				}
			}
			output.append(')');
		}

	}

	public static class Ellipsis extends ExprElm {

		public Ellipsis() {
			super();
		}

		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append("..."); //$NON-NLS-1$
		}
		
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			parser.unnamedParamaterUsed(this); // it's kinda sound...
		}

	}

	public static class Placeholder extends ExprElm {
		private String entryName;

		public Placeholder(String entryName) {
			this.entryName = entryName;
		}

		public String getEntryName() {
			return entryName;
		}
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append('$');
			builder.append(entryName);
			builder.append('$');
		}
		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			StringTbl stringTbl = parser.getContainer().getStringTblForLanguagePref();
			if (stringTbl != null) {
				NameValueAssignment entry = stringTbl.getMap().get(entryName);
				if (entry != null)
					return new DeclarationRegion(entry, this);
			}
			return super.declarationAt(offset, parser);
		}
	}
	
	/**
	 * Baseclass for statements.
	 *
	 */
	public static class Statement extends ExprElm {
		
		public interface Attachment {
			public enum Position {
				Pre,
				Post
			}
			void applyAttachment(Position position, ExprWriter builder, int depth);
		}
		
		public static class EmptyLinesAttachment implements Attachment {
			private int num;
			public int getNum() {
				return num;
			}
			public EmptyLinesAttachment(int num) {
				super();
				this.num = num;
			}
			@Override
			public void applyAttachment(Position position, ExprWriter builder, int depth) {
				switch (position) {
				case Pre:
					for (int i = 0; i < num; i++) {
						//printIndent(builder, depth);
						builder.append("\n");
					}
					break;
				}
			}
		}

		private List<Attachment> attachments;
		
		public void addAttachment(Attachment attachment) {
			if (attachments == null)
				attachments = new LinkedList<Attachment>();
			attachments.add(attachment);
		}
		
		@SuppressWarnings("unchecked")
		public <T extends Attachment> T getAttachment(Class<T> cls) {
			if (attachments != null) {
				for (Attachment a : attachments) {
					if (cls.isAssignableFrom(a.getClass())) {
						return (T) a;
					}
				}
			}
			return null;
		}

		public Comment getInlineComment() {
			return getAttachment(Comment.class);
		}

		public void setInlineComment(Comment inlineComment) {
			Comment old = getInlineComment();
			if (old != null) {
				attachments.remove(old);
			}
			addAttachment(inlineComment);
		}

		@Override
		public IType getType(C4ScriptParser context) {
			return null;
		}

		@Override
		public boolean hasSideEffects() {
			return true;
		}
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			//			for (ExprElm elm : getSubElements())
			//				if (elm != null)
			//					elm.reportErrors(parser);
		}

		public void printPrependix(ExprWriter builder, int depth) {
			if (attachments != null) {
				for (Attachment a : attachments) {
					a.applyAttachment(Position.Pre, builder, depth);
				}
			}	
		}
		
		public void printAppendix(ExprWriter builder, int depth) {
			if (attachments != null) {
				for (Attachment a : attachments) {
					a.applyAttachment(Position.Post, builder, depth);
				}
			}
		}

	}

	/**
	 * A {} block
	 *
	 */
	public static class Block extends Statement {

		private Statement[] statements;

		public Block(List<Statement> statements) {
			this(statements.toArray(new Statement[statements.size()]));
		}

		public Block(Statement[] statements) {
			super();
			this.statements = statements;
			assignParentToSubElements();
		}

		public Statement[] getStatements() {
			return statements;
		}

		public void setStatements(Statement[] statements) {
			this.statements = statements;
		}

		@Override
		public void setSubElements(ExprElm[] elms) {
			Statement[] typeAdjustedCopy = new Statement[elms.length];
			System.arraycopy(elms, 0, typeAdjustedCopy, 0, elms.length);
			setStatements(typeAdjustedCopy);
		}

		@Override
		public ExprElm[] getSubElements() {
			return getStatements();
		}

		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append("{\n"); //$NON-NLS-1$
			for (Statement statement : statements) {
				statement.printPrependix(builder, depth);
				printIndent(builder, depth);
				statement.print(builder, depth+1);
				statement.printAppendix(builder, depth);
				builder.append("\n"); //$NON-NLS-1$
			}
			printIndent(builder, depth-1); builder.append("}"); //$NON-NLS-1$
		}
		
		@Override
		public ExprElm optimize(C4ScriptParser parser)
		throws CloneNotSupportedException {
			if (getParent() != null && !(getParent() instanceof KeywordStatement) && !(this instanceof BunchOfStatements)) {
				return new BunchOfStatements(statements);
			}
			// uncomment never-reached statements
			boolean notReached = false;
			Statement[] commentedOutList = null;
			for (int i = 0; i < statements.length; i++) {
				Statement s = statements[i];
				if (notReached) {
					if (commentedOutList != null) {
						commentedOutList[i] = s instanceof Comment ? s : s.commentedOut();
					}
					else if (!(s instanceof Comment)) {
						commentedOutList = new Statement[statements.length];
						System.arraycopy(statements, 0, commentedOutList, 0, i);
						commentedOutList[i] = s.commentedOut();
					}
				}
				else
					notReached = s != null && s.getControlFlow() != ControlFlow.Continue;
			}
			if (commentedOutList != null)
				return new Block(commentedOutList);
			else
				return super.optimize(parser);
		}
		
		@Override
		public ControlFlow getControlFlow() {
			for (Statement s : statements) {
				// look for first statement that breaks execution
				ControlFlow cf = s.getControlFlow();
				if (cf != ControlFlow.Continue)
					return cf;
			}
			return ControlFlow.Continue;
		}
		
		@Override
		public EnumSet<ControlFlow> getPossibleControlFlows() {
			EnumSet<ControlFlow> result = EnumSet.noneOf(ControlFlow.class);
			for (Statement s : statements) {
				ControlFlow cf = s.getControlFlow();
				if (cf != ControlFlow.Continue)
					return EnumSet.of(cf);
				EnumSet<ControlFlow> cfs = s.getPossibleControlFlows();
				result.addAll(cfs);
			}
			return result;
		}

	}

	static class BunchOfStatements extends Block {
		public BunchOfStatements(List<Statement> statements) {
			super(statements);
		}

		public BunchOfStatements(Statement... statements) {
			super(statements);
		}

		@Override
		public void doPrint(ExprWriter builder, int depth) {
			boolean first = true;
			for (Statement statement : getStatements()) {
				statement.printPrependix(builder, depth);
				if (first)
					first = false;
				else {
					builder.append("\n"); //$NON-NLS-1$
					printIndent(builder, depth-1);
				}
				statement.print(builder, depth);
				statement.printAppendix(builder, depth);
			}
		}
	}

	/**
	 * Simple statement wrapper for an expression.
	 * 
	 */
	public static class SimpleStatement extends Statement {
		private ExprElm expression;

		public SimpleStatement(ExprElm expression) {
			super();
			this.expression = expression;
			assignParentToSubElements();
		}

		public ExprElm getExpression() {
			return expression;
		}

		public void setExpression(ExprElm expression) {
			this.expression = expression;
		}

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {expression};
		}

		@Override
		public void setSubElements(ExprElm[] elms) {
			expression = elms[0];
		}

		@Override
		public void doPrint(ExprWriter builder, int depth) {
			expression.print(builder, depth+1);
			builder.append(";"); //$NON-NLS-1$
		}

		@Override
		public ExprElm optimize(C4ScriptParser parser) throws CloneNotSupportedException {
			ExprElm exprReplacement = expression.optimize(parser);
			if (exprReplacement instanceof Statement)
				return exprReplacement;
			if (exprReplacement == expression)
				return this;
			return new SimpleStatement(exprReplacement);
		}

		@Override
		public ControlFlow getControlFlow() {
			return expression.getControlFlow();
		}

		@Override
		public boolean hasSideEffects() {
			return expression.hasSideEffects();
		}
		
		@Override
		public Object evaluate(IEvaluationContext context) throws ControlFlowException {
			return expression.evaluate(context);
		}

	}

	/**
	 * Baseclass for statements which begin with a keyword
	 *
	 */
	public static abstract class KeywordStatement extends Statement {
		public abstract String getKeyword();
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(getKeyword());
			builder.append(";"); //$NON-NLS-1$
		}

		protected void printBody(ExprElm body, ExprWriter builder, int depth) {
			int depthAdd = 0;
			if (!(body instanceof EmptyStatement)) {
				if (braceStyle == BraceStyleType.NewLine)
					builder.append("\n"); //$NON-NLS-1$
				boolean isBlock = body instanceof Block;
				switch (braceStyle) {
				case NewLine:
					printIndent(builder, depth - (isBlock ? 1 : 0));
					break;
				case SameLine:
					builder.append(' ');
					break;
				}
				depthAdd = isBlock ? 0 : 1;
			}
			body.print(builder, depth + depthAdd);
		}
	}

	public static class ContinueStatement extends KeywordStatement {
		@Override
		public String getKeyword() {
			return Keywords.Continue;
		}
		@Override
		public ControlFlow getControlFlow() {
			return ControlFlow.NextIteration;
		}
	}

	public static class BreakStatement extends KeywordStatement {
		@Override
		public String getKeyword() {
			return Keywords.Break;
		}
		@Override
		public ControlFlow getControlFlow() {
			return ControlFlow.BreakLoop;
		}
	}

	public static class ReturnStatement extends KeywordStatement {

		private ExprElm returnExpr;

		@Override
		public Object evaluate(IEvaluationContext context) throws ControlFlowException {
			throw new ReturnException(returnExpr.evaluate(context));
		}
		
		public ReturnStatement(ExprElm returnExpr) {
			super();
			this.returnExpr = returnExpr;
		}

		@Override
		public String getKeyword() {
			return Keywords.Return;
		}

		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(getKeyword());
			if (returnExpr != null) {
				builder.append(" "); //$NON-NLS-1$
				// return(); -> return 0;
				if (returnExpr == ExprElm.NULL_EXPR)
					builder.append("0"); //$NON-NLS-1$
				else
					returnExpr.print(builder, depth+1);
			}
			builder.append(";"); //$NON-NLS-1$
		}

		public ExprElm getReturnExpr() {
			return returnExpr;
		}

		public void setReturnExpr(ExprElm returnExpr) {
			this.returnExpr = returnExpr;
		}

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {returnExpr};
		}

		@Override
		public void setSubElements(ExprElm[] elms) {
			returnExpr = elms[0];
		}

		@Override
		public ControlFlow getControlFlow() {
			return ControlFlow.Return;
		}

		@Override
		public ExprElm optimize(C4ScriptParser parser) throws CloneNotSupportedException {
			// return (0); -> return 0;
			if (returnExpr instanceof Parenthesized)
				return new ReturnStatement(((Parenthesized)returnExpr).getInnerExpr().optimize(parser));
			// return (0, Sound("Ugh")); -> { Sound("Ugh"); return 0; }
			// FIXME: should declare temporary variable so that order of expression execution isn't changed
			/*
			if (returnExpr instanceof Tuple) {
				Tuple tuple = (Tuple) returnExpr;
				ExprElm[] tupleElements = tuple.getElements();
				List<Statement> statements = new LinkedList<Statement>();
				for (int i = 1; i < tupleElements.length; i++) {
					statements.add(new SimpleStatement(tupleElements[i].newStyleReplacement(parser)));
				}
				statements.add(new ReturnStatement(tupleElements[0].newStyleReplacement(parser)));
				return getParent() instanceof ConditionalStatement ? new Block(statements) : new BunchOfStatements(statements);
			}
			 */
			return super.optimize(parser);
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			if (returnExpr != null)
				new CallFunc(parser.getActiveFunc()).expectedToBeOfType(returnExpr.getType(parser), parser, TypeExpectancyMode.Expect);
		}
	}

	public static abstract class ConditionalStatement extends KeywordStatement {
		protected ExprElm condition;
		protected ExprElm body;

		public ExprElm getCondition() {
			return condition;
		}

		public void setCondition(ExprElm condition) {
			this.condition = condition;
		}

		public ConditionalStatement(ExprElm condition, ExprElm body) {
			super();
			this.condition = condition;
			this.body = body;
			assignParentToSubElements();
		}

		protected void printBody(ExprWriter builder, int depth) {
			printBody(body, builder, depth);
		}

		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(getKeyword());
			builder.append(" ("); //$NON-NLS-1$
			condition.print(builder, depth+1);
			builder.append(")"); //$NON-NLS-1$
			printBody(builder, depth);
		}

		public ExprElm getBody() {
			return body;
		}

		public void setBody(ExprElm body) {
			this.body = body;
		}

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {condition, body};
		}

		@Override
		public void setSubElements(ExprElm[] elms) {
			condition = elms[0];
			body      = elms[1];
		}

	}

	public static class IfStatement extends ConditionalStatement {

		private ExprElm elseExpr;

		public IfStatement(ExprElm condition, ExprElm body, ExprElm elseExpr) {
			super(condition, body);
			this.elseExpr = elseExpr;
			assignParentToSubElements();
		}

		@Override
		public String getKeyword() {
			return Keywords.If;
		}
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(getKeyword());
			builder.append(" ("); //$NON-NLS-1$
			condition.print(builder, depth);
			builder.append(")"); //$NON-NLS-1$
			printBody(builder, depth);
			if (elseExpr != null) {
				builder.append("\n"); //$NON-NLS-1$
				printIndent(builder, depth-1);
				builder.append(Keywords.Else);
				builder.append(" "); //$NON-NLS-1$
				boolean isBlock = elseExpr instanceof Block;
				if (!(elseExpr instanceof IfStatement)) {
					switch (braceStyle) {
					case NewLine:
						builder.append("\n"); //$NON-NLS-1$
						printIndent(builder, depth - (isBlock?1:0));
						break;
					case SameLine:
						break;
					}
				}
				elseExpr.print(builder, depth);
			}
		}

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {condition, body, elseExpr};
		}

		@Override
		public void setSubElements(ExprElm[] elms) {
			condition = elms[0];
			body      = elms[1];
			elseExpr  = elms[2];
		}
		
		@Override
		public EnumSet<ControlFlow> getPossibleControlFlows() {
			EnumSet<ControlFlow> result = EnumSet.of(ControlFlow.Continue);
			result.addAll(body.getPossibleControlFlows());
			if (elseExpr != null)
				result.addAll(elseExpr.getPossibleControlFlows());
			return result;
		}
		
		public ExprElm getElse() {
			return elseExpr;
		}
		
		@Override
		public ControlFlow getControlFlow() {
			// return most optimistic flow (the smaller ordinal() the more "continuy" the flow is)
			ControlFlow ifCase = body.getControlFlow();
			ControlFlow elseCase = elseExpr != null ? elseExpr.getControlFlow() : ControlFlow.Continue;
			return ifCase.ordinal() < elseCase.ordinal() ? ifCase : elseCase;
		}

	}

	public static class WhileStatement extends ConditionalStatement implements ILoop {
		public WhileStatement(ExprElm condition, ExprElm body) {
			super(condition, body);
		}

		@Override
		public String getKeyword() {
			return Keywords.While;
		}
		
		@Override
		public EnumSet<ControlFlow> getPossibleControlFlows() {
			EnumSet<ControlFlow> result = body.getPossibleControlFlows();
			result.removeAll(EnumSet.of(ControlFlow.BreakLoop, ControlFlow.NextIteration));
			return result;
		}
	}

	public static class DoWhileStatement extends WhileStatement {
		public DoWhileStatement(ExprElm condition, ExprElm body) {
			super(condition, body);
		}

		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(Keywords.Do);
			printBody(builder, depth);
			builder.append(" "); //$NON-NLS-1$
			builder.append(Keywords.While);
			builder.append(" ("); //$NON-NLS-1$
			condition.print(builder, depth);
			builder.append(");"); //$NON-NLS-1$
		}
	}

	public static class ForStatement extends ConditionalStatement implements ILoop {
		private ExprElm initializer, increment;
		public ForStatement(ExprElm initializer, ExprElm condition, ExprElm increment, ExprElm body) {
			super(condition, body);
			this.initializer = initializer;
			this.increment = increment;
			assignParentToSubElements();
		}
		@Override
		public String getKeyword() {
			return Keywords.For;
		}
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(getKeyword() + " ("); //$NON-NLS-1$
			if (initializer != null)
				initializer.print(builder, depth+1);
			else
				builder.append(";"); //$NON-NLS-1$
			builder.append(" "); // no ';' since initializer is already a statement //$NON-NLS-1$
			if (condition != null)
				condition.print(builder, depth+1);
			builder.append("; "); //$NON-NLS-1$
			if (increment != null)
				increment.print(builder, depth);
			builder.append(")"); //$NON-NLS-1$
			printBody(builder, depth);
		}
		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {initializer, condition, increment, body};
		}

		@Override
		public void setSubElements(ExprElm[] elms) {
			initializer = elms[0];
			condition   = elms[1];
			increment   = elms[2];
			body        = elms[3];
		}
	}

	public static class IterateArrayStatement extends KeywordStatement implements ILoop {
		private ExprElm elementExpr, arrayExpr, body;

		public IterateArrayStatement(ExprElm elementExpr, ExprElm arrayExpr, ExprElm body) {
			super();
			this.elementExpr = elementExpr;
			this.arrayExpr   = arrayExpr;
			this.body        = body;
			assignParentToSubElements();
		}

		public ExprElm getArrayExpr() {
			return arrayExpr;
		}

		public void setArrayExpr(ExprElm arrayExpr) {
			this.arrayExpr = arrayExpr;
		}

		public ExprElm getElementExpr() {
			return elementExpr;
		}

		public void setElementExpr(ExprElm elementExpr) {
			this.elementExpr = elementExpr;
		}

		@Override
		public String getKeyword() {
			return Keywords.For;
		}

		@Override
		public void doPrint(ExprWriter writer, int depth) {
			StringBuilder builder = new StringBuilder(getKeyword().length()+2+1+1+Keywords.In.length()+1+2);
			builder.append(getKeyword() + " ("); //$NON-NLS-1$
			elementExpr.print(builder, depth+1);
			// remove ';' that elementExpr (a statement) prints
			if (builder.charAt(builder.length()-1) == ';')
				builder.deleteCharAt(builder.length()-1);
			builder.append(" " + Keywords.In + " "); //$NON-NLS-1$ //$NON-NLS-2$
			arrayExpr.print(builder, depth+1);
			builder.append(") "); //$NON-NLS-1$
			writer.append(builder.toString());
			printBody(body, writer, depth);
		}

		public ExprElm getBody() {
			return body;
		}

		public void setBody(ExprElm body) {
			this.body = body;
		}

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {elementExpr, arrayExpr, body};
		}

		@Override
		public void setSubElements(ExprElm[] elms) {
			elementExpr = elms[0];
			arrayExpr   = elms[1];
			body        = elms[2];
		}

	}

	public static class VarDeclarationStatement extends KeywordStatement {
		private List<Pair<String, ExprElm>> varInitializations;
		private C4VariableScope scope;
		public VarDeclarationStatement(List<Pair<String, ExprElm>> varInitializations, C4VariableScope scope) {
			super();
			this.varInitializations = varInitializations;
			this.scope = scope;
			assignParentToSubElements();
		}
		@Override
		public String getKeyword() {
			return scope.toKeyword();
		}
		@Override
		public ExprElm[] getSubElements() {
			List<ExprElm> result = new LinkedList<ExprElm>();
			for (Pair<String, ExprElm> initialization : varInitializations) {
				if (initialization.getSecond() != null)
					result.add(initialization.getSecond());
			}
			return result.toArray(new ExprElm[0]);
		}
		@Override
		public void setSubElements(ExprElm[] elms) {
			int j = 0;
			for (Pair<String, ExprElm> pair : varInitializations) {
				if (pair.getSecond() != null)
					pair.setSecond(elms[j++]);
			}
		}
		public List<Pair<String, ExprElm>> getVarInitializations() {
			return varInitializations;
		}
		public void setVarInitializations(List<Pair<String, ExprElm>> varInitializations) {
			this.varInitializations = varInitializations;
		}
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(getKeyword());
			builder.append(" "); //$NON-NLS-1$
			int counter = 0;
			for (Pair<String, ExprElm> var : varInitializations) {
				builder.append(var.getFirst());
				if (var.getSecond() != null) {
					builder.append(" = "); //$NON-NLS-1$
					var.getSecond().print(builder, depth+1);
				}
				if (++counter < varInitializations.size())
					builder.append(", "); //$NON-NLS-1$
				else
					builder.append(";"); //$NON-NLS-1$
			}
		}
		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			int addToMakeAbsolute = parser.getActiveFunc().getBody().getStart() + this.getExprStart();
			offset += addToMakeAbsolute;
			for (Pair<String, ExprElm> pair : varInitializations) {
				String varName = pair.getFirst();
				C4Variable var = parser.getActiveFunc().findVariable(varName);
				if (var != null && var.isAt(offset))
					return new DeclarationRegion(var, new Region(var.getLocation().getStart()-parser.getActiveFunc().getBody().getStart(), var.getLocation().getLength()));
			}
			return super.declarationAt(offset, parser);
		}
	}

	public static class EmptyStatement extends Statement {
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append(";"); //$NON-NLS-1$
		}
	}

	public static class Comment extends Statement implements Statement.Attachment {
		private String comment;
		private boolean multiLine;

		public Comment(String comment, boolean multiLine) {
			super();
			this.comment = comment;
			this.multiLine = multiLine;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
		
		private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("(\\s*)");
		private static final Pattern WHITE_SPACE_AT_END_PATTERN = Pattern.compile("(\\s*)$");

		private String commentAsPrintedStatement(C4Function function, int depth) {
			try {
				Statement s = C4ScriptParser.parseStandaloneStatement(comment, function, null);
				if (s != null) {
					String str = s.toString(depth);
					Matcher matcher = WHITE_SPACE_PATTERN.matcher(comment);
					if (matcher.find())
						str = matcher.group(1) + str;
					matcher = WHITE_SPACE_AT_END_PATTERN.matcher(comment);
					if (matcher.find())
						str = str + matcher.group(1);
					return str;
				} else {
					return comment;
				}
			} catch (ParsingException e) {
				return comment;
			}
		}
		
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			String c = commentAsPrintedStatement(null, depth);
			if (multiLine = multiLine || c.contains("\n")) {
				builder.append("/*"); //$NON-NLS-1$
				builder.append(c);
				builder.append("*/"); //$NON-NLS-1$
			}
			else {
				builder.append("//"); //$NON-NLS-1$
				builder.append(c);
			}
		}

		public boolean isMultiLine() {
			return multiLine;
		}

		public void setMultiLine(boolean multiLine) {
			this.multiLine = multiLine;
		}

		public boolean precedesOffset(int offset, CharSequence script) {
			int count = 0;
			if (offset > getExprEnd()) {
				for (int i = getExprEnd()+1; i < offset; i++) {
					if (!BufferedScanner.isLineDelimiterChar(script.charAt(i)))
						return false;
					if (script.charAt(i) == '\n' && ++count > 1)
						return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			// parse comment as expression and see what goes
			ExpressionLocator locator = new ExpressionLocator(offset-2); // make up for '//' or /*'
			try {
				C4ScriptParser.parseStandaloneStatement(comment, parser.getActiveFunc(), locator);
			} catch (ParsingException e) {}
			if (locator.getExprAtRegion() != null) {
				DeclarationRegion reg = locator.getExprAtRegion().declarationAt(offset, parser);
				if (reg != null)
					return reg.addOffsetInplace(getExprStart()+2);
				else
					return null;
			}
			else
				return super.declarationAt(offset, parser);
		}

		@Override
		public void applyAttachment(Position position, ExprWriter builder, int depth) {
			switch (position) {
			case Post:
				builder.append(" ");
				this.print(builder, depth);
				break;
			}
		}

	}

	public static class FunctionDescription extends Statement implements Serializable {
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		private String contents;
		public FunctionDescription(String contents) {
			super();
			this.contents = contents;
		}
		@Override
		public void doPrint(ExprWriter builder, int depth) {
			builder.append('[');
			builder.append(contents);
			builder.append(']');
		}
		public String getContents() {
			return contents;
		}
		public void setContents(String contents) {
			this.contents = contents;
		}
		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			if (contents == null)
				return null;
			String[] parts = contents.split("\\|"); //$NON-NLS-1$
			int off = 1;
			for (String part : parts) {
				if (offset >= off && offset < off+part.length()) {
					if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
						StringTbl stringTbl = parser.getContainer().getStringTblForLanguagePref();
						if (stringTbl != null) {
							NameValueAssignment entry = stringTbl.getMap().get(part.substring(1, part.length()-1));
							if (entry != null)
								return new DeclarationRegion(entry, new Region(getExprStart()+off, part.length()));
						}
					}
					else {
						String[] nameValue = part.split("="); //$NON-NLS-1$
						if (nameValue.length == 2) {
							String name = nameValue[0].trim();
							String value = nameValue[1].trim();
							int sep = value.indexOf(':');
							if (sep != -1)
								value = value.substring(0, sep);
							if (name.equals("Condition") || name.equals("Image")) //$NON-NLS-1$ //$NON-NLS-2$
								return new DeclarationRegion(parser.getContainer().findDeclaration(value), new Region(getExprStart()+off+nameValue[0].length()+1, value.length()));
						}
					}
					break;
				}
				off += part.length()+1;
			}
			return null;
		}
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			// see StringLiteral.reportErrors
			if (parser.hasAppendTo())
				return;
			int off = 1;
			for (String part : contents.split("\\|")) { //$NON-NLS-1$
				if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
					StringTbl stringTbl = parser.getContainer().getStringTblForLanguagePref();
					String entryName = part.substring(1, part.length()-1);
					if (stringTbl == null || stringTbl.getMap().get(entryName) == null) {
						parser.warningWithCode(ParserErrorCode.UndeclaredIdentifier, new Region(getExprStart()+off, part.length()), entryName);
					}
				}
				off += part.length()+1;
			}
		}
	}

}
