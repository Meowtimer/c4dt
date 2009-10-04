package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.UnaryOp.Placement;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Utilities;
import net.arctics.clonk.parser.ParsingException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Contains classes that form a c4script syntax tree.
 */
public abstract class C4ScriptExprTree {
	
	public enum TraversalContinuation {
		Continue,
		TraverseSubElements,
		SkipSubElements,
		Cancel
	}
	
	public interface IExpressionListener {
		public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser);
	}
	
	public final static class DeclarationRegion {
		private C4Declaration declaration;
		private IRegion region;
		public C4Declaration getDeclaration() {
			return declaration;
		}
		public DeclarationRegion(C4Declaration declaration, IRegion region) {
			super();
			this.declaration = declaration;
			this.region = region;
		}
		public IRegion getRegion() {
			return region;
		}
	}
	
	/**
	 * @author madeen
	 * base class for making expression trees
	 */
	public abstract static class ExprElm implements IRegion, Cloneable {
		
		public static final ExprElm NULL_EXPR = new ExprElm() {};
		public static final ExprElm[] EMPTY_EXPR_ARRAY = new ExprElm[0];
		
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

		public void print(StringBuilder output, int depth) {
			//output.append("Implement me");
		}

		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
			return predecessor == null;
		}
		public C4Type getType(C4ScriptParser context) {
			return context.queryTypeOfExpression(this, C4Type.UNKNOWN);
		}

		public C4Object guessObjectType(C4ScriptParser context) {
			return context.queryObjectTypeOfExpression(this);
		}
		
		public ExprElm getExemplaryArrayElement(C4ScriptParser context) {
			return NULL_EXPR;
		}

		public boolean modifiable(C4ScriptParser context) {
			return true;
		}

		public boolean hasSideEffects() {
			for (ExprElm e : getSubElements()) {
				if (e.hasSideEffects())
					return true;
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
				System.out.println("setSubElements should be implemented when getSubElements() is implemented ("+getClass().getName()+")");
		}

		/**
		 * Keeps applying newStyleReplacement to the expression and its modified versions until an expression and its replacement are identical e.g. there is nothing to be modified anymore
		 * @param context
		 * @return
		 * @throws CloneNotSupportedException
		 */
		public ExprElm exhaustiveNewStyleReplacement(C4ScriptParser context) throws CloneNotSupportedException {
			ExprElm repl;
			for (ExprElm original = this; (repl = original.newStyleReplacement(context)) != original; original = repl);
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
		public ExprElm newStyleReplacement(C4ScriptParser context) throws CloneNotSupportedException {
			ExprElm[] subElms = getSubElements();
			ExprElm[] newSubElms = new ExprElm[subElms.length];
			boolean differentSubElms = false;
			for (int i = 0; i < subElms.length; i++) {
				newSubElms[i] = subElms[i] != null ? subElms[i].newStyleReplacement(context) : null;
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
		public boolean canBeConvertedTo(C4Type otherType, C4ScriptParser context) {
			// 5555 is ID
			return getType(context) == C4Type.INT && otherType == C4Type.ID;
		}
		
		public boolean validForType(C4Type t, C4ScriptParser context) {
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
		
		public static C4Type combineTypes(C4Type first, C4Type second) {
			if (first == C4Type.UNKNOWN)
				return second == C4Type.ANY ? C4Type.UNKNOWN : second;
			if (first != second)
				return C4Type.ANY;
			return first;
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
					public C4Type getType(C4ScriptParser context) {
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

		public void expectedToBeOfType(C4Type type, C4ScriptParser context) {
			IStoredTypeInformation info = context.requestStoredTypeInformation(this);
			if (info != null && type != C4Type.UNKNOWN)
				info.storeType(type);
		}
		
		public void inferTypeFromAssignment(ExprElm rightSide, C4ScriptParser parser) {
			parser.storeTypeInformation(this, rightSide.getType(parser), rightSide.guessObjectType(parser));
		}
		
		public boolean isReturn() {
			return false;
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
		
		public IStoredTypeInformation createStoredTypeInformation() {
			return null;
		}
		
		protected void printIndent(StringBuilder builder, int indentDepth) {
			for (int i = 0; i < indentDepth; i++)
				builder.append("\t"); // FIXME: should be done according to user's preferences
		}
		
		/**
		 * Rudimentary possibility for evaluating the expression. Only used for evaluating the value of the SetProperty("Name", ...) call in a Definition function (OpenClonk) right now
		 * @param context the context to evaluate in
		 * @return the result
		 */
		public Object evaluate(C4ScriptBase context) {
			return null;
		}

	}

	public static class MemberOperator extends ExprElm {
		private boolean dotNotation;
		private boolean hasTilde;
		private C4ID id;
		private int idOffset;

		public MemberOperator(boolean dotNotation, boolean hasTilde, C4ID id, int idOffset) {
			super();
			this.dotNotation = dotNotation;
			this.hasTilde = hasTilde;
			this.id = id;
			this.idOffset = idOffset;
		}

		@Override
		public void print(StringBuilder output, int depth) {
			if (dotNotation) {
				// so simple
				output.append('.');
			}
			else {
				if (hasTilde)
					output.append("->~");
				else
					output.append("->");
				if (id != null) {
					output.append(id.getName());
					output.append("::");
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
				C4Type t = predecessor.getType(context);
				if (t == null || t == C4Type.ARRAY || t == C4Type.STRING)
					return false;
				return true;
			}
			return false;
		}

		@Override
		public C4Type getType(C4ScriptParser context) {
			return null; // invalid as an expression
		}

		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			// explicit id
			if (id != null) {
				return context.getContainer().getNearestObjectWithId(id);
			}
			// stuff before -> decides
			return getPredecessorInSequence() != null ? getPredecessorInSequence().guessObjectType(context) : super.guessObjectType(context);
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
			// not really since -> can also be used with ids
//			ExprElm pred = getPredecessorInSequence();			
//			if (pred != null)
//				pred.expectedToBeOfType(C4Type.OBJECT);
		}

	}

	public abstract static class Value extends ExprElm {

		@Override
		public C4Type getType(C4ScriptParser context) {
			return context.queryTypeOfExpression(this, C4Type.ANY);
		}

	}

	public static class Sequence extends Value {
		protected ExprElm[] elements;
		public Sequence(ExprElm[] elms) {
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
		public void print(StringBuilder output, int depth) {
			for (ExprElm e : elements) {
				e.print(output, depth+1);
			}
		}
		@Override
		public C4Type getType(C4ScriptParser context) {
			return (elements == null || elements.length == 0) ? C4Type.UNKNOWN : elements[elements.length-1].getType(context);
		}
		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			return (elements == null || elements.length == 0) ? super.guessObjectType(context) : elements[elements.length-1].guessObjectType(context);
		}
		@Override
		public boolean modifiable(C4ScriptParser context) {
			return elements != null && elements.length > 0 && elements[elements.length-1].modifiable(context);
		}
		@Override
		public boolean hasSideEffects() {
			return true; // FIXME: check elements?
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
		public void print(StringBuilder output, int depth) {
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
			}

			public boolean expressionRelevant(ExprElm expr) {
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
				if (decl instanceof C4Variable) {
					C4Variable var = (C4Variable) decl;
					if (!soft || var.getScope() == C4VariableScope.VAR_VAR) {
						var.setType(getType());					
						var.setExpectedContent(getObjectType());
					}
				}
			}

			public boolean sameExpression(IStoredTypeInformation other) {
				return other.getClass() == VariableTypeInformation.class && ((VariableTypeInformation)other).decl == decl;
			}
			
			@Override
			public String toString() {
				return "variable " + decl.getName() + " " +  super.toString();
			}
			
		}

		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			if (declaration != null) {
				if (declaration == C4Variable.THIS && context.getContainer() instanceof C4Object)
					return (C4Object) context.getContainer();
			}
			C4Object o = super.guessObjectType(context);
			if (o == null && declaration != null)
				o = ((C4Variable)declaration).getExpectedContent();
			return o;
		}

		@Override
		public boolean modifiable(C4ScriptParser context) {
			return declaration == null || ((C4Variable)declaration).getScope() != C4VariableScope.VAR_CONST;
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
			return predecessor == null;
		}

		@Override
		protected C4Declaration getDeclImpl(C4ScriptParser parser) {
			FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
			info.setContextFunction(parser.getActiveFunc());
			info.setSearchOrigin(parser.getContainer());
			return parser.getContainer().findVariable(declarationName, info);
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (declaration == null)
				parser.errorWithCode(ParserErrorCode.UndeclaredIdentifier, this, true, declarationName);
		}
		
		@Override
		public IStoredTypeInformation createStoredTypeInformation() {
			if (getDeclaration() instanceof C4Variable && ((C4Variable)getDeclaration()).isTypeLocked())
				return null;
			return new VariableTypeInformation(getDeclaration());
		}
		
		@Override
		public C4Type getType(C4ScriptParser context) {
			// getDeclaration(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
			if (getDeclaration(context) == C4Variable.THIS)
				return C4Type.OBJECT;
			C4Type stored = context.queryTypeOfExpression(this, null);
			if (stored != null)
				return stored;
			if (getDeclaration() instanceof C4Variable)
				return ((C4Variable)getDeclaration()).getType();
			return C4Type.ANY;
		}
		
		@Override
		public void expectedToBeOfType(C4Type type, C4ScriptParser context) {
			if (getDeclaration() == C4Variable.THIS)
				return;
			super.expectedToBeOfType(type, context);
		}
		
		@Override
		public void inferTypeFromAssignment(ExprElm expression, C4ScriptParser context) {
			if (getDeclaration() == C4Variable.THIS)
				return;
			super.inferTypeFromAssignment(expression, context);
		}
		
	}

	public static class CallFunc extends AccessDeclaration {
		private final static class VarFunctionsTypeInformation extends StoredTypeInformation {
			private int varIndex;

			private VarFunctionsTypeInformation(int val) {
				varIndex = val;
			}

			public boolean expressionRelevant(ExprElm expr) {
				if (expr instanceof CallFunc) {
					CallFunc callFunc = (CallFunc) expr;
					return
						callFunc.getDeclaration() == CachedEngineFuncs.Var &&
						callFunc.getParams().length > 0 &&
						callFunc.getParams()[0] instanceof NumberLiteral &&
						((NumberLiteral)callFunc.getParams()[0]).intValue() == varIndex;
				}
				return false;
			}

			public boolean sameExpression(IStoredTypeInformation other) {
				return other.getClass() == VarFunctionsTypeInformation.class && ((VarFunctionsTypeInformation)other).varIndex == varIndex;
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
		@Override
		public void print(StringBuilder output, int depth) {
			super.print(output, depth);
			output.append("(");
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					if (params[i] != null)
						params[i].print(output, depth+1);
					if (i < params.length-1)
						output.append(", ");
				}
			}
			output.append(")");
		}
		@Override
		public boolean modifiable(C4ScriptParser context) {
			C4Type t = getType(context);
			return t == C4Type.REFERENCE || t == C4Type.ANY || t == C4Type.UNKNOWN;
		}
		@Override
		public boolean hasSideEffects() {
			return true;
		}
		@Override
		public C4Type getType(C4ScriptParser context) {
			if (declaration instanceof C4Function)
				return ((C4Function)declaration).getReturnType();
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
				C4Declaration field = parser.getContainer().getIndex().findGlobalFunction(declarationName);
				if (field == null)
					field = ClonkCore.getDefault().getExternIndex().findGlobalDeclaration(declarationName);
				if (field == null)
					field = ClonkCore.getDefault().getEngineObject().findFunction(declarationName);
				return field;
			}
			return null;
		}
		@Override
		public void reportErrors(C4ScriptParser context) throws ParsingException {
			super.reportErrors(context);
			if (declaration == CachedEngineFuncs.Par) {
				if (params.length > 0) {
					context.unnamedParamaterUsed(params[0]);
				}
				else
					context.unnamedParamaterUsed(NumberLiteral.ZERO);
			}
			else if (declarationName.equals(Keywords.Return))
				context.warningWithCode(ParserErrorCode.ReturnAsFunction, this);
			else {
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
					int givenParam = 0;
					for (C4Variable parm : f.getParameters()) {
						if (givenParam >= params.length)
							break;
						ExprElm given = params[givenParam++];
						if (given == null)
							continue;
						//given.reportErrors(parser); no duplicate errors
						if (!given.validForType(parm.getType(), context))
							context.warningWithCode(ParserErrorCode.IncompatibleTypes, given, parm.getType(), given.getType(context));
						given.expectedToBeOfType(parm.getType(), context);
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
		@Override
		public ExprElm[] getSubElements() {
			return params;
		}
		@Override
		public void setSubElements(ExprElm[] elms) {
			params = elms;
		}
		private boolean isCriteriaSearch() {
			return declarationName.equals("FindObjects") || declarationName.equals("FindObject2");
		}
		@Override
		public C4Object guessObjectType(C4ScriptParser parser) {
			// FIXME: could lead to problems when one of those functions does not take an id as first parameter
			if (params.length == 0 && getDeclaration() == C4Variable.THIS) {
				return parser.getContainerObject();
			}
			else if (isCriteriaSearch()) {
				return searchCriteriaAssumedResult(parser);
			}
			else if (params != null && params.length >= 1 && getType(parser) == C4Type.OBJECT && (declarationName.startsWith("Create") || declarationName.startsWith("Find"))) {
				return params[0].guessObjectType(parser);
			}
			else if (declarationName.equals("GetID") && params.length == 0) {
				return getPredecessorInSequence() == null ? parser.getContainerObject() : getPredecessorInSequence().guessObjectType(parser);
			}
			if (declaration instanceof ITypedDeclaration) {
				C4Object obj = ((ITypedDeclaration)declaration).getExpectedContent();
				if (obj != null)
					return obj;
			}
			return super.guessObjectType(parser);
		}
		protected BinaryOp applyOperatorTo(C4ScriptParser parser, ExprElm[] parms, C4ScriptOperator operator) throws CloneNotSupportedException {
			BinaryOp op = new BinaryOp(operator);
			BinaryOp result = op;
			for (int i = 0; i < parms.length; i++) {
				ExprElm one = parms[i].newStyleReplacement(parser);
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
		public ExprElm newStyleReplacement(C4ScriptParser parser) throws CloneNotSupportedException {
			
			// And(ugh, blugh) -> ugh && blugh
			C4ScriptOperator replOperator = C4ScriptOperator.oldStyleFunctionReplacement(declarationName);
			if (replOperator != null && params.length == 1) {
				// LessThan(x) -> x < 0
				if (replOperator.getNumArgs() == 2)
					return new BinaryOp(replOperator, params[0].newStyleReplacement(parser), new NumberLiteral(0));
				ExprElm n = params[0].newStyleReplacement(parser);
				if (n instanceof BinaryOp)
					n = new Parenthesized(n);
				return new UnaryOp(replOperator, replOperator.isPostfix() ? UnaryOp.Placement.Postfix : UnaryOp.Placement.Prefix, n);
			}
			if (replOperator != null && params.length >= 2) {
				return applyOperatorTo(parser, params, replOperator);
			}
			
			// ObjectCall(ugh, "UghUgh", 5) -> ugh->UghUgh(5)
			if (params.length >= 2 && declarationName.equals("ObjectCall") && params[1] instanceof StringLiteral) {
				ExprElm[] parmsWithoutObject = new ExprElm[params.length-2];
				for (int i = 0; i < parmsWithoutObject.length; i++)
					parmsWithoutObject[i] = params[i+2].newStyleReplacement(parser);
				return new Sequence(new ExprElm[] {
						params[0].newStyleReplacement(parser),
						new MemberOperator(false, true, null, 0),
						new CallFunc(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)});
			}
			
			// OCF_Awesome() -> OCF_Awesome
			if (params.length == 0 && declaration instanceof C4Variable) {
				return new AccessVar(declarationName);
			}
			
			// Par(5) -> nameOfParm6
			if (params.length <= 1 && declaration == CachedEngineFuncs.Par && (params.length == 0 || params[0] instanceof NumberLiteral)) {
				NumberLiteral number = params.length > 0 ? (NumberLiteral) params[0] : NumberLiteral.ZERO;
				if (number.intValue() >= 0 && number.intValue() < parser.getActiveFunc().getParameters().size())
					return new AccessVar(parser.getActiveFunc().getParameters().get(number.intValue()).getName());
			}
			
			// SetVar(5, "ugh") -> Var(5) = "ugh"
			if (params.length == 2 && (declaration == CachedEngineFuncs.SetVar || declaration == CachedEngineFuncs.SetLocal || declaration == CachedEngineFuncs.AssignVar)) {
				return new BinaryOp(C4ScriptOperator.Assign, new CallFunc(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), params[0].newStyleReplacement(parser)), params[1].newStyleReplacement(parser));
			}
			
			// DecVar(0) -> Var(0)--
			if (params.length <= 1 && (declaration == CachedEngineFuncs.DecVar || declaration == CachedEngineFuncs.IncVar)) {
				return new UnaryOp(declaration == CachedEngineFuncs.DecVar ? C4ScriptOperator.Decrement : C4ScriptOperator.Increment, Placement.Prefix,
					new CallFunc(CachedEngineFuncs.Var.getName(), new ExprElm[] {
						params.length == 1 ? params[0].newStyleReplacement(parser) : new NumberLiteral(0)
					})
				);
			}
			
			// Call("Func", 5, 5) -> Func(5, 5)
			if (params.length >= 1 && declarationName.equals("Call") && params[0] instanceof StringLiteral) {
				ExprElm[] parmsWithoutName = new ExprElm[params.length-1];
				for (int i = 0; i < parmsWithoutName.length; i++)
					parmsWithoutName[i] = params[i+1].newStyleReplacement(parser);
				return new CallFunc(((StringLiteral)params[0]).stringValue(), parmsWithoutName);
			}
			
			return super.newStyleReplacement(parser);
		}
		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			return new DeclarationRegion(getDeclaration(parser), new Region(getExprStart(), declarationName.length()));
		}
		public C4Object searchCriteriaAssumedResult(C4ScriptParser context) {
			C4Object result = null;
			// parameters to FindObjects itself are also &&-ed together
			if (declarationName.equals("Find_And") || isCriteriaSearch()) {
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
			else if (declarationName.equals("Find_ID")) {
				if (params.length > 0 && params[0] instanceof IDLiteral) {
					result = ((IDLiteral)params[0]).guessObjectType(context);
				}
			}
			return result;
		}
		@Override
		public ExprElm getExemplaryArrayElement(C4ScriptParser context) {
			final C4Object obj = searchCriteriaAssumedResult(context);
			if (obj != null) {
				return new ExprElm() {
					@Override
					public C4Type getType(C4ScriptParser context) {
						return C4Type.OBJECT;
					}
					@Override
					public C4Object guessObjectType(C4ScriptParser arg0) {
						return obj;
					}
				};
			}
			return super.getExemplaryArrayElement(context);
		}
		public ExprElm getReturnArg() {
			if (params.length == 1)
				return params[0];
			return new Tuple(params);
		}
		@Override
		public boolean isReturn() {
			return declarationName.equals(Keywords.Return);
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
		public IStoredTypeInformation createStoredTypeInformation() {
			if (getDeclaration() == CachedEngineFuncs.Var) {
				if (getParams().length > 0 && getParams()[0] instanceof NumberLiteral) {
					NumberLiteral number = (NumberLiteral)getParams()[0];
					if (number.intValue() >= 0) {
						// Var() with a sane constant number
						return new VarFunctionsTypeInformation(number.intValue());
					}
				}
			}
			return super.createStoredTypeInformation();
		}
	}

	public static class Operator extends Value {
		private final C4ScriptOperator operator;

		@Override
		public C4Type getType(C4ScriptParser context) {
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
		public ExprElm newStyleReplacement(C4ScriptParser context) throws CloneNotSupportedException {
			// #strict 2: ne -> !=, S= -> ==
			if (context.getStrictLevel() >= 2) {
				C4ScriptOperator op = getOperator();
				if (op == C4ScriptOperator.StringEqual || op == C4ScriptOperator.eq)
					op = C4ScriptOperator.Equal;
				else if (op == C4ScriptOperator.ne)
					op = C4ScriptOperator.NotEqual;
				if (op != getOperator()) {
					return new BinaryOp(op, getLeftSide().newStyleReplacement(context), getRightSide().newStyleReplacement(context));
				}
			}
			
			// blub() && blab() && return(1); -> {blub(); blab(); return(1);}
			if ((getOperator() == C4ScriptOperator.And || getOperator() == C4ScriptOperator.Or) && (getParent() instanceof SimpleStatement)) {// && getRightSide().isReturn()) {
				ExprElm block = convertOperatorHackToBlock(context);
				if (block != null)
					return block;
			}
			
			return super.newStyleReplacement(context);
		}

		private ExprElm convertOperatorHackToBlock(C4ScriptParser context)
				throws CloneNotSupportedException {
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
					statements.add(new SimpleStatement(ex.newStyleReplacement(context)));
				}
				// convert func call to proper return statement
				if (getRightSide().isReturn())
					statements.add(new ReturnStatement(((CallFunc)getRightSide()).getReturnArg().newStyleReplacement(context)));
				else
					statements.add(new SimpleStatement(getRightSide().newStyleReplacement(context)));
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

		public void print(StringBuilder output, int depth) {
			
			// put brackets around operands in case some transformation messed up prioritization
			boolean needsBrackets = leftSide instanceof BinaryOp && getOperator().getPriority() > ((BinaryOp)leftSide).getOperator().getPriority();
			if (needsBrackets)
				output.append("(");
			leftSide.print(output, depth+1);
			if (needsBrackets)
				output.append(")");
			
			output.append(" ");
			output.append(getOperator().getOperatorName());
			output.append(" ");
			
			needsBrackets = rightSide instanceof BinaryOp && getOperator().getPriority() > ((BinaryOp)rightSide).getOperator().getPriority();
			if (needsBrackets)
				output.append("(");
			rightSide.print(output, depth+1);
			if (needsBrackets)
				output.append(")");
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
			if ((getOperator() == C4ScriptOperator.StringEqual || getOperator() == C4ScriptOperator.ne) && (context.getStrictLevel() >= 2)) {
				context.warningWithCode(ParserErrorCode.ObsoleteOperator, this, getOperator().getOperatorName());
			}
			if (!getLeftSide().validForType(getOperator().getFirstArgType(), context))
				context.warningWithCode(ParserErrorCode.IncompatibleTypes, getLeftSide(), getOperator().getFirstArgType(), getLeftSide().getType(context));
			if (!getRightSide().validForType(getOperator().getSecondArgType(), context))
				context.warningWithCode(ParserErrorCode.IncompatibleTypes, getRightSide(), getOperator().getSecondArgType(), getRightSide().getType(context));
			
			getLeftSide().expectedToBeOfType(getOperator().getFirstArgType(), context);
			getRightSide().expectedToBeOfType(getOperator().getSecondArgType(), context);
			
			if (getOperator() == C4ScriptOperator.Assign) {
				getLeftSide().inferTypeFromAssignment(getRightSide(), context);
			}
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
		}
		public void print(StringBuilder output, int depth) {
			output.append("(");
			innerExpr.print(output, depth+1);
			output.append(")");
		}
		public C4Type getType(C4ScriptParser context) {
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
		public ExprElm newStyleReplacement(C4ScriptParser parser)
				throws CloneNotSupportedException {
			if (!(getParent() instanceof Operator) && !(getParent() instanceof Sequence))
				return innerExpr.newStyleReplacement(parser);
			return super.newStyleReplacement(parser);
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
			C4ScriptOperator a = this.getOperator();
			C4ScriptOperator b = this.getOperator();
			return a.spaceNeededBetweenMeAnd(b);
		}

		public void print(StringBuilder output, int depth) {
			UnaryOp unop = (argument instanceof UnaryOp) ? (UnaryOp)argument : null;
			if (unop != null && unop.placement != this.placement)
				unop = null;
			if (placement == Placement.Postfix) {
				argument.print(output, depth+1);
				if (unop != null && needsSpace(unop))
					output.append(" "); // - -5 -.-
				output.append(getOperator().getOperatorName());
			} else {
				output.append(getOperator().getOperatorName());
				if (unop != null && needsSpace(unop))
					output.append(" "); // - -5 -.-
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
		public ExprElm newStyleReplacement(C4ScriptParser context) throws CloneNotSupportedException {
			// could happen when argument is transformed to binary operator
			ExprElm arg = getArgument().newStyleReplacement(context);
			if (arg instanceof BinaryOp)
				return new UnaryOp(getOperator(), placement, new Parenthesized(arg));
			if (getOperator() == C4ScriptOperator.Not && arg instanceof Parenthesized) {
				 Parenthesized brackets = (Parenthesized)arg;
				 if (brackets.getInnerExpr() instanceof BinaryOp) {
					 BinaryOp op = (BinaryOp) brackets.getInnerExpr();
					 if (op.getOperator() == C4ScriptOperator.Equal) {
						 return new BinaryOp(C4ScriptOperator.NotEqual, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
					 }
					 else if (op.getOperator() == C4ScriptOperator.NotEqual) {
						 return new BinaryOp(C4ScriptOperator.Equal, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
					 }
					 else if (op.getOperator() == C4ScriptOperator.StringEqual) {
						 return new BinaryOp(C4ScriptOperator.ne, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
					 }
					 else if (op.getOperator() == C4ScriptOperator.ne) {
						 return new BinaryOp(C4ScriptOperator.StringEqual, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
					 }
				 }
			}
			return super.newStyleReplacement(context);
		}
		
		@Override
		public boolean isConstant() {
			return argument.isConstant();
		}
		
		@Override
		public boolean modifiable(C4ScriptParser context) {
			return placement == Placement.Prefix && getOperator().returnsRef();
		}

	}

	public static class Literal<T> extends Value {
		private final T literal;
		
		@Override
		public void expectedToBeOfType(C4Type arg0, C4ScriptParser arg1) {
			// don't care
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
		public T evaluate(C4ScriptBase context) {
			return literal;
		}

	}

	public static final class NumberLiteral extends Literal<Long> {

		public static final NumberLiteral ZERO = new NumberLiteral(0);
		
		private boolean hex;

		public NumberLiteral(long value, boolean hex) {
			super(new Long(value));
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
		public void print(StringBuilder output, int depth) {
			if (hex) {
				output.append("0x");
				output.append(Long.toHexString(longValue()).toUpperCase());
			}
			else
				output.append(longValue());
		}

		public C4Type getType(C4ScriptParser context) {
			if (longValue() == 0)
				return C4Type.ANY; // FIXME: to prevent warnings when assigning 0 to object-variables
			return C4Type.INT;
		}

		@Override
		public boolean canBeConvertedTo(C4Type otherType, C4ScriptParser context) {
			// 0 is the NULL object or NULL string
			return (longValue() == 0 && (otherType == C4Type.OBJECT || otherType == C4Type.STRING)) || super.canBeConvertedTo(otherType, context);
		}

		public boolean isHex() {
			return hex;
		}

		public void setHex(boolean hex) {
			this.hex = hex;
		}

	}

	public static final class StringLiteral extends Literal<String> {
		public StringLiteral(String literal) {
			super(literal);
		}

		public String stringValue() {
			return getLiteral();
		}
		@Override
		public void print(StringBuilder output, int depth) {
			output.append("\"");
			output.append(stringValue());
			output.append("\"");
		}

		@Override
		public C4Type getType(C4ScriptParser context) {
			return C4Type.STRING;
		}
		
		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			DeclarationRegion result = getStringTblEntryAt(offset-1, parser.getContainer());
			if (result != null)
				return result;
			
			if (getParent() instanceof CallFunc) {
				CallFunc parentFunc = (CallFunc) getParent();
				int myIndex = parentFunc.indexOfParm(this);
				
				//  link to functions that are called indirectly
				
				// GameCall: look for nearest scenario and find function in its script
				if (myIndex == 0 && parentFunc.getDeclaration() == CachedEngineFuncs.GameCall) {
					ClonkIndex index = parser.getContainer().getIndex();
					C4Scenario scenario = ClonkIndex.pickNearest(parser.getContainer().getResource(), index.getIndexedScenarios());
					if (scenario != null) {
						C4Function scenFunc = scenario.findFunction(stringValue());
						if (scenFunc != null)
							return new DeclarationRegion(scenFunc, identifierRegion());
					}
				}
				
				// ScheduleCall: second parameter is function name; first is object to call the function in
				else if (myIndex == 1 && parentFunc.getDeclaration() == CachedEngineFuncs.ScheduleCall) {
					C4Object typeToLookIn = parentFunc.getParams()[0].guessObjectType(parser);
					if (typeToLookIn != null) {
						C4Function func = typeToLookIn.findFunction(stringValue());
						if (func != null)
							return new DeclarationRegion(func, identifierRegion());
					}
				}
				
				// LocalN: look for local var in object
				else if (myIndex == 0 && parentFunc.getDeclaration() == CachedEngineFuncs.LocalN) {
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
				
				else if (myIndex == 0 && parentFunc.getDeclaration() == CachedEngineFuncs.Call) {
					C4Function f = parser.getContainer().findFunction(stringValue());
					if (f != null)
						return new DeclarationRegion(f, identifierRegion());
				}
				
				else if (myIndex == 1 && Utilities.isAnyOf(parentFunc.getDeclaration(), CachedEngineFuncs.ProtectedCall, CachedEngineFuncs.PrivateCall)) {
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
		
		private DeclarationRegion getStringTblEntryAt(int offset, C4ScriptBase container) {
			try {
				StringTbl stringTbl = container.getStringTblForLanguagePref();
				if (stringTbl == null)
					return null;
				int firstDollar = stringValue().lastIndexOf('$', offset-1);
				int secondDollar = stringValue().indexOf('$', offset);
				if (firstDollar != -1 && secondDollar != -1) {
					String entry = stringValue().substring(firstDollar+1, secondDollar);
					if (entry != null) {
						NameValueAssignment e = stringTbl.getMap().get(entry);
						if (e != null)
							return new DeclarationRegion(e, new Region(getExprStart()+1+firstDollar, secondDollar-firstDollar+1));
					}
				}
			} catch (CoreException e) {}
			return null;
		}
		
		@Override
		public String evaluate(C4ScriptBase context) {
			String value = getLiteral();
			int valueLen = value.length();
			StringBuilder builder = new StringBuilder(valueLen*2);
			// insert stringtbl entries
			for (int i = 0; i < valueLen;) {
				if (i+1 < valueLen && value.charAt(i) == '$') {
					DeclarationRegion region = getStringTblEntryAt(i+1, context);
					if (region != null) {
						builder.append(((NameValueAssignment)region.getDeclaration()).getValue());
						i += region.getRegion().getLength();
						continue;
					}
				}
				builder.append(value.charAt(i++));
			}
			return builder.toString();
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
		public void print(StringBuilder output, int depth) {
			output.append(idValue().getName());
		}

		@Override
		public C4Type getType(C4ScriptParser context) {
			return C4Type.ID;
		}

		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			// FIXME: does not actually return an object of type idValue but the id itself :/
			return context.getContainer().getNearestObjectWithId(idValue());
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
			super(new Boolean(value));
		}
		public C4Type getType(C4ScriptParser context) {
			return C4Type.BOOL;
		}
		@Override
		public void print(StringBuilder output, int depth) {
			output.append(booleanValue() ? Keywords.True : Keywords.False);
		}
	}

	public static final class ArrayElementAccess extends Value {

		private ExprElm argument;
		
		@Override
		public C4Type getType(C4ScriptParser context) {
			return C4Type.ANY; // FIXME: guess type of elements
		}

		public ArrayElementAccess(ExprElm argument) {
			super();
			this.argument = argument;
		}

		@Override
		public void print(StringBuilder output, int depth) {
			output.append("[");
			getArgument().print(output, depth+1);
			output.append("]");
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
			return predecessor != null;
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			getArgument().reportErrors(parser);
			// bleh, getOperator() returns null here FIXME: not deriving this class from ExprUnaryOp?
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

	public static class ArrayExpression extends Sequence {
		public ArrayExpression(ExprElm[] elms) {
			super(elms);
		}

		public void print(StringBuilder output, int depth) {
			output.append("[");
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] != null)
					elements[i].print(output, depth+1);
				if (i < elements.length-1)
					output.append(", ");
			}
			output.append("]");
		}

		@Override
		public C4Type getType(C4ScriptParser context) {
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
		public ExprElm getExemplaryArrayElement(C4ScriptParser context) {
			C4Type type = C4Type.UNKNOWN;
			for (ExprElm e : elements) {
				type = ExprElm.combineTypes(type, e.getType(context));
			}
			return type == C4Type.UNKNOWN ? super.getExemplaryArrayElement(context) : ExprElm.getExprElmForType(type);
		}

	}
	
	public static class PropListExpression extends Value {
		private Pair<String, ExprElm>[] components;
		public PropListExpression(Pair<String, ExprElm>[] components) {
			this.components = components;
		}
		@Override
		public void print(StringBuilder output, int depth) {
			output.append('{');
			for (int i = 0; i < components.length; i++) {
				Pair<String, ExprElm> component = components[i];
				output.append('\n');
				printIndent(output, depth-2);
				output.append(component.getFirst());
				output.append(": ");
				component.getSecond().print(output, depth+1);
				if (i < components.length-1) {
					output.append(',');
				} else {
					output.append('\n'); printIndent(output, depth-3); output.append('}');
				}
			}
		}
		@Override
		public C4Type getType(C4ScriptParser parser) {
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
		public void print(StringBuilder output, int depth) {
			output.append('(');
			if (elements != null) {
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] != null)
						elements[i].print(output, depth+1);
					if (i < elements.length-1)
						output.append(", ");
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
		public void print(StringBuilder output, int depth) {
			output.append("...");
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
		public void print(StringBuilder builder, int depth) {
			builder.append('$');
			builder.append(entryName);
			builder.append('$');
		}
		@Override
		public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
			try {
				StringTbl stringTbl = parser.getContainer().getStringTblForLanguagePref();
				if (stringTbl != null) {
					NameValueAssignment entry = stringTbl.getMap().get(entryName);
					if (entry != null)
						return new DeclarationRegion(entry, this);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return super.declarationAt(offset, parser);
		}
	}
	
	/**
	 * Baseclass for statements.
	 * @author madeen
	 *
	 */
	public static class Statement extends ExprElm {
		
		private Comment inlineComment;
		
		public Comment getInlineComment() {
			return inlineComment;
		}
		
		public void setInlineComment(Comment inlineComment) {
			this.inlineComment = inlineComment;
		}
		
		@Override
		public C4Type getType(C4ScriptParser context) {
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
		
		public void printAppendix(StringBuilder builder, int depth) {
			if (inlineComment != null) {
				builder.append(" ");
				inlineComment.print(builder, depth);
			}
		}
	}
	
	/**
	 * A {} block
	 * @author madeen
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
		
		protected void printStatement(StringBuilder builder, Statement statement, int depth) {
			statement.print(builder, depth);
			statement.printAppendix(builder, depth);
		}
		
		@Override
		public void print(StringBuilder builder, int depth) {
			builder.append("{\n");
			for (Statement statement : statements) {
				printIndent(builder, depth); printStatement(builder, statement, depth+1); builder.append("\n");
			}
			printIndent(builder, depth-1); builder.append("}");
		}
		
		@Override
		public ExprElm newStyleReplacement(C4ScriptParser parser)
				throws CloneNotSupportedException {
			if (getParent() != null && !(getParent() instanceof KeywordStatement) && !(this instanceof BunchOfStatements)) {
				return new BunchOfStatements(statements);
			}
			return super.newStyleReplacement(parser);
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
		public void print(StringBuilder builder, int depth) {
			boolean first = true;
			for (Statement statement : getStatements()) {
				if (first)
					first = false;
				else {
					builder.append("\n");
					printIndent(builder, depth-1);
				}
				printStatement(builder, statement, depth+1);
			}
		}
	}
	
	/**
	 * Simple statement wrapper for an expression.
	 * @author madeen
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
		public void print(StringBuilder builder, int depth) {
			expression.print(builder, depth+1);
			builder.append(";");
		}

		@Override
		public ExprElm newStyleReplacement(C4ScriptParser parser) throws CloneNotSupportedException {
			ExprElm exprReplacement = expression.newStyleReplacement(parser);
			if (exprReplacement instanceof Statement)
				return exprReplacement;
			if (exprReplacement == expression)
				return this;
			return new SimpleStatement(exprReplacement);
		}
		
		@Override
		public boolean isReturn() {
			return expression.isReturn();
		}
		
		@Override
		public boolean hasSideEffects() {
			return expression.hasSideEffects();
		}
	}
	
	/**
	 * Baseclass for statements which begin with a keyword
	 * @author madeen
	 *
	 */
	public static abstract class KeywordStatement extends Statement {
		public abstract String getKeyword();
		@Override
		public void print(StringBuilder builder, int depth) {
			builder.append(getKeyword());
			builder.append(";");
		}

		protected void printBody(ExprElm body, StringBuilder builder, int depth) {
			int depthAdd = 0;
			if (!(body instanceof EmptyStatement)) {
				if (!(body instanceof Block)) {
					builder.append("\n");
					printIndent(builder, depth);
					depthAdd = 1;
				} else
					builder.append(" ");
			}
			body.print(builder, depth + depthAdd);
		}
	}
	
	public static class ContinueStatement extends KeywordStatement {
		@Override
		public String getKeyword() {
			return Keywords.Continue;
		}
	}
	
	public static class BreakStatement extends KeywordStatement {
		@Override
		public String getKeyword() {
			return Keywords.Break;
		}
	}
	
	public static class ReturnStatement extends KeywordStatement {
		
		private ExprElm returnExpr;
		
		public ReturnStatement(ExprElm returnExpr) {
			super();
			this.returnExpr = returnExpr;
		}

		@Override
		public String getKeyword() {
			return Keywords.Return;
		}
		
		@Override
		public void print(StringBuilder builder, int depth) {
			builder.append(getKeyword());
			if (returnExpr != null) {
				builder.append(" ");
				// return(); -> return 0;
				if (returnExpr == ExprElm.NULL_EXPR)
					builder.append("0");
				else
					returnExpr.print(builder, depth+1);
			}
			builder.append(";");
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
		public boolean isReturn() {
			return true;
		}
		
		@Override
		public ExprElm newStyleReplacement(C4ScriptParser parser)
				throws CloneNotSupportedException {
			// return (0); -> return 0;
			if (returnExpr instanceof Parenthesized)
				return new ReturnStatement(((Parenthesized)returnExpr).getInnerExpr().newStyleReplacement(parser));
			// return (0, Sound("Ugh")); -> { Sound("Ugh"); return 0; }
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
			return super.newStyleReplacement(parser);
		}
		
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			if (returnExpr != null)
				parser.getActiveFunc().inferTypeFromAssignment(returnExpr, parser);
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

		protected void printBody(StringBuilder builder, int depth) {
			printBody(body, builder, depth);
		}
		
		@Override
		public void print(StringBuilder builder, int depth) {
			builder.append(getKeyword());
			builder.append(" (");
			condition.print(builder, depth+1);
			builder.append(")");
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
		public void print(StringBuilder builder, int depth) {
			builder.append(getKeyword());
			builder.append(" (");
			condition.print(builder, depth);
			builder.append(")");
			printBody(builder, depth);
			if (elseExpr != null) {
				builder.append("\n");
				printIndent(builder, depth-1);
				builder.append(Keywords.Else);
				builder.append(" ");
				if (!(elseExpr instanceof Block || elseExpr instanceof IfStatement)) {
					builder.append("\n");
					printIndent(builder, depth);
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
		
	}
	
	public static class WhileStatement extends ConditionalStatement {
		public WhileStatement(ExprElm condition, ExprElm body) {
			super(condition, body);
		}

		@Override
		public String getKeyword() {
			return Keywords.While;
		}
	}
	
	public static class DoWhileStatement extends WhileStatement {
		public DoWhileStatement(ExprElm condition, ExprElm body) {
			super(condition, body);
		}

		@Override
		public void print(StringBuilder builder, int depth) {
			builder.append(Keywords.Do);
			printBody(builder, depth);
			builder.append(" ");
			builder.append(Keywords.While);
			builder.append(" (");
			condition.print(builder, depth);
			builder.append(");");
		}
	}
	
	public static class ForStatement extends ConditionalStatement {
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
		public void print(StringBuilder builder, int depth) {
			builder.append(getKeyword() + " (");
			if (initializer != null)
				initializer.print(builder, depth+1);
			else
				builder.append(";");
			builder.append(" "); // no ';' since initializer is already a statement
			if (condition != null)
				condition.print(builder, depth+1);
			builder.append("; ");
			if (increment != null)
				increment.print(builder, depth);
			builder.append(")");
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
	
	public static class IterateArrayStatement extends KeywordStatement {
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
		public void print(StringBuilder builder, int depth) {
			builder.append(getKeyword() + " (");
			elementExpr.print(builder, depth+1);
			// remove ';' that elementExpr (a statement) prints
			if (builder.charAt(builder.length()-1) == ';')
				builder.deleteCharAt(builder.length()-1);
			builder.append(" " + Keywords.In + " ");
			arrayExpr.print(builder, depth+1);
			builder.append(") ");
			printBody(body, builder, depth);
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
		public void print(StringBuilder builder, int depth) {
			builder.append(getKeyword());
			builder.append(" ");
			int counter = 0;
			for (Pair<String, ExprElm> var : varInitializations) {
				builder.append(var.getFirst());
				if (var.getSecond() != null) {
					builder.append(" = ");
					var.getSecond().print(builder, depth+1);
				}
				if (++counter < varInitializations.size())
					builder.append(", ");
				else
					builder.append(";");
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
		public void print(StringBuilder builder, int depth) {
			builder.append(";");
		}
	}
	
	public static class Comment extends Statement {
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
		
		@Override
		public void print(StringBuilder builder, int depth) {
			if (isMultiLine()) {
				builder.append("/*");
				builder.append(comment);
				builder.append("*/");
			}
			else {
				builder.append("//");
				builder.append(comment);
			}
		}

		public boolean isMultiLine() {
			return multiLine;
		}

		public void setMultiLine(boolean multiLine) {
			this.multiLine = multiLine;
		}

		public boolean precedesOffset(int offset, CharSequence script) {
			if (offset > getExprEnd()) {
				for (int i = getExprEnd()+1; i < offset; i++)
					if (!BufferedScanner.isLineDelimiterChar(script.charAt(i)))
						return false;
				return true;
			}
			return false;
		}
		
	}
	
	// just some empty lines that should be preserved when converting code
	public static class EmptyLines extends Statement {
		private int numLines;

		public int getNumLines() {
			return numLines;
		}

		public void setNumLines(int numLines) {
			this.numLines = numLines;
		}

		public EmptyLines(int numLines) {
			super();
			this.numLines = numLines;
		}
		
		@Override
		public void print(StringBuilder builder, int depth) {
			for (int i = 0; i < numLines; i++)
				builder.append("\n");
		}
	}
	
	public static class FunctionDescription extends Statement implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private String contents;
		public FunctionDescription(String contents) {
			super();
			this.contents = contents;
		}
		@Override
		public void print(StringBuilder builder, int depth) {
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
			String[] parts = contents.split("\\|");
			int off = 1;
			for (String part : parts) {
				if (offset >= off && offset < off+part.length()) {
					if (part.startsWith("$") && part.endsWith("$")) {
						try {
							StringTbl stringTbl = parser.getContainer().getStringTblForLanguagePref();
							if (stringTbl != null) {
								NameValueAssignment entry = stringTbl.getMap().get(part.substring(1, part.length()-1));
								if (entry != null)
									return new DeclarationRegion(entry, new Region(getExprStart()+off, part.length()));
							}
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
					else {
						String[] nameValue = part.split("=");
						if (nameValue.length == 2) {
							String name = nameValue[0].trim();
							String value = nameValue[1].trim();
							int sep = value.indexOf(':');
							if (sep != -1)
								value = value.substring(0, sep);
							if (name.equals("Condition") || name.equals("Image"))
								return new DeclarationRegion(parser.getContainer().findDeclaration(value), new Region(getExprStart()+off+nameValue[0].length()+1, value.length()));
						}
					}
					break;
				}
				off += part.length()+1;
			}
			return null;
		}
	}
	
}
