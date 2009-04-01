package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ScriptParser.Keywords;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.util.Pair;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

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
	
	public final static class FieldRegion {
		private C4Field field;
		private IRegion region;
		public C4Field getField() {
			return field;
		}
		public FieldRegion(C4Field field, IRegion region) {
			super();
			this.field = field;
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
		
		private int exprStart, exprEnd;
		private transient ExprElm parent, predecessorInSequence;
		
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
				parser.warningWithCode(C4ScriptParserErrorCode.NoSideEffects, this);
		}

		public void setParent(ExprElm parent) {
			this.parent = parent;
		}

		public void print(StringBuilder output, int depth) {
			//output.append("Implement me");
		}

		public boolean isValidInSequence(ExprElm predecessor) {
			return predecessor == null;
		}
		public C4Type getType() {
			return C4Type.UNKNOWN;
		}

		public C4Object guessObjectType(C4ScriptParser context) {
			return context.queryObjectTypeOfExpression(this);
		}
		
		public ExprElm getExemplaryArrayElement(C4ScriptParser context) {
			return NULL_EXPR;
		}

		public boolean modifiable() {
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
			return new ExprElm[0];
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
		public boolean canBeConvertedTo(C4Type otherType) {
			// 5555 is ID
			return getType() == C4Type.INT && otherType == C4Type.ID;
		}
		
		public boolean validForType(C4Type t) {
			return t.canBeAssignedFrom(getType()) || canBeConvertedTo(t);
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
		
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
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
					public C4Type getType() {
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

		public void expectedToBeOfType(C4Type type) {
			// so what
		}
		
		public void inferTypeFromAssignment(ExprElm rightSide,
				C4ScriptParser parser) {
			parser.storeTypeInformation(this, rightSide.getType(), rightSide.guessObjectType(parser));
		}
		
		public boolean isReturn() {
			return false;
		}

		public boolean containedIn(ExprElm expression) {
			if (expression == this)
				return true;
			for (ExprElm e : expression.getSubElements())
				if (this.containedIn(e))
					return true;
			return false;
		}
		
		public boolean isConstant() {
			return false;
		}

	}

	public static class ExprObjectCall extends ExprElm {
		private boolean hasTilde;
		private C4ID id;
		private int idOffset;

		public ExprObjectCall(boolean hasTilde, C4ID id, int idOffset) {
			super();
			this.hasTilde = hasTilde;
			this.id = id;
			this.idOffset = idOffset;
		}

		@Override
		public void print(StringBuilder output, int depth) {
			if (hasTilde)
				output.append("->~");
			else
				output.append("->");
			if (id != null) {
				output.append(id.getName());
				output.append("::");
			}
		}

		public C4ID getId() {
			return id;
		}

		public void setId(C4ID id) {
			this.id = id;
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor) {
			if (predecessor != null) {
				C4Type t = predecessor.getType();
				if (t == null || t == C4Type.ARRAY || t == C4Type.STRING)
					return false;
				return true;
			}
			return false;
		}

		@Override
		public C4Type getType() {
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
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			if (id != null && offset >= idOffset && offset < idOffset+4)
				return new FieldRegion(parser.getContainer().getNearestObjectWithId(id), new Region(getExprStart()+idOffset, 4));
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

	public abstract static class ExprValue extends ExprElm {

		@Override
		public C4Type getType() {
			return C4Type.ANY;
		}

	}

	public static class ExprSequence extends ExprValue {
		protected ExprElm[] elements;
		public ExprSequence(ExprElm[] elms) {
			elements = elms;
			ExprElm prev = null;
			for (ExprElm e : elements) {
				e.setPredecessorInSequence(prev);
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
		public C4Type getType() {
			return (elements == null || elements.length == 0) ? C4Type.UNKNOWN : elements[elements.length-1].getType();
		}
		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			return (elements == null || elements.length == 0) ? super.guessObjectType(context) : elements[elements.length-1].guessObjectType(context);
		}
		@Override
		public boolean modifiable() {
			return elements != null && elements.length > 0 && elements[elements.length-1].modifiable();
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

	}

	public static abstract class ExprAccessField extends ExprValue {
		protected C4Field field;
		private boolean fieldNotFound = false;
		protected final String fieldName;
		
		public C4Field getField(C4ScriptParser parser) {
			if (field == null && !fieldNotFound) {
				field = getFieldImpl(parser);
				fieldNotFound = field == null;
			}
			return field;
		}
		
		public C4Field getField() {
			return field; // return without trying to obtain it (no parser context)
		}
		
		protected abstract C4Field getFieldImpl(C4ScriptParser parser);

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			getField(parser); // find the field so subclasses can complain about missing variables/functions
		}

		public ExprAccessField(String fieldName) {
			this.fieldName = fieldName;
		}
		@Override
		public void print(StringBuilder output, int depth) {
			output.append(fieldName);
		}

		public IRegion fieldRegion(int offset) {
			return new Region(offset+getExprStart(), fieldName.length());
		}

		@Override
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			return new FieldRegion(getField(parser), region(0));
		}

		public String getFieldName() {
			return fieldName;
		}
		
		@Override
		public int getIdentifierLength() {
			return fieldName.length();
		}

		public boolean indirectAccess() {
			return field == null || !field.getName().equals(fieldName);
		}
	}

	public static class ExprAccessVar extends ExprAccessField {
		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			if (field != null) {
				if (field == C4Variable.THIS && context.getContainer() instanceof C4Object)
					return (C4Object) context.getContainer();
				return ((C4Variable)field).getExpectedContent();
			}
			return super.guessObjectType(context);
		}

		@Override
		public boolean modifiable() {
			return field == null || ((C4Variable)field).getScope() != C4VariableScope.VAR_CONST;
		}

		public ExprAccessVar(String varName) {
			super(varName);
		}
		
		@Override
		public void expectedToBeOfType(C4Type type) {
			if (field != null)
				((C4Variable)field).expectedToBeOfType(type);
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor) {
			return predecessor == null;
		}

		@Override
		protected C4Field getFieldImpl(C4ScriptParser parser) {
			FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
			info.setContextFunction(parser.getActiveFunc());
			info.setSearchOrigin(parser.getContainer());
			return parser.getContainer().findVariable(fieldName, info);
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (field == null)
				parser.errorWithCode(C4ScriptParserErrorCode.UndeclaredIdentifier, this, true, fieldName);
		}

		@Override
		public C4Type getType() {
			return field != null ? ((C4Variable)field).getType() : super.getType();
		}
		
		public void inferTypeFromAssignment(ExprElm rightSide, C4ScriptParser parser) {
			if (field instanceof C4Variable)
				((C4Variable)field).inferTypeFromAssignment(rightSide, parser);
		}

	}

	public static class ExprCallFunc extends ExprAccessField {
		private ExprElm[] params;
		public ExprCallFunc(String funcName, ExprElm... parms) {
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
		public boolean modifiable() {
			C4Type t = getType();
			return t == C4Type.REFERENCE || t == C4Type.ANY || t == C4Type.UNKNOWN;
		}
		@Override
		public boolean hasSideEffects() {
			return true;
		}
		@Override
		public C4Type getType() {
			if (field instanceof C4Function)
				return ((C4Function)field).getReturnType();
			return super.getType();
		}
		@Override
		public boolean isValidInSequence(ExprElm elm) {
			return super.isValidInSequence(elm) || elm instanceof ExprObjectCall;	
		}
		@Override
		protected C4Field getFieldImpl(C4ScriptParser parser) {
			if (fieldName.equals(Keywords.Return))
				return null;
			if (fieldName.equals(Keywords.Inherited) || fieldName.equals(Keywords.SafeInherited)) {
				return parser.getActiveFunc().getInherited();
			}
			ExprElm p = getPredecessorInSequence();
			C4ScriptBase lookIn = p == null ? parser.getContainer() : p.guessObjectType(parser);
			if (lookIn != null) {
				FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
				info.setSearchOrigin(parser.getContainer());
				C4Field field = lookIn.findFunction(fieldName, info);
				// might be a variable called as a function (not after '->')
				if (field == null && p == null)
					field = lookIn.findVariable(fieldName, info);
				return field;
			} else if (p != null) {
				// find global function
				C4Field field = parser.getContainer().getIndex().findGlobalFunction(fieldName);
				if (field == null)
					field = ClonkCore.getDefault().EXTERN_INDEX.findGlobalField(fieldName);
				if (field == null)
					field = ClonkCore.getDefault().ENGINE_OBJECT.findFunction(fieldName);
				return field;
			}
			return null;
		}
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (field == C4ScriptParser.CachedEngineFuncs.Par) {
				if (params.length > 0) {
					parser.unnamedParamaterUsed(params[0]);
				}
				else
					parser.unnamedParamaterUsed(ExprNumber.ZERO);
			}
			else if (fieldName.equals(Keywords.Return))
				parser.warningWithCode(C4ScriptParserErrorCode.ReturnAsFunction, this);
			else {
				if (field instanceof C4Variable) {
					if (params.length == 0) {
						// no warning when in #strict mode
						if (parser.getStrictLevel() >= 2)
							parser.warningWithCode(C4ScriptParserErrorCode.VariableCalled, this, field.getName());
					} else {
						parser.errorWithCode(C4ScriptParserErrorCode.VariableCalled, this, field.getName(), true);
					}
				}
				else if (field instanceof C4Function) {
					C4Function f = (C4Function)field;
					int givenParam = 0;
					for (C4Variable parm : f.getParameters()) {
						if (givenParam >= params.length)
							break;
						ExprElm given = params[givenParam++];
						if (given == null)
							continue;
						//given.reportErrors(parser); no duplicate errors
						if (!given.validForType(parm.getType()))
							parser.warningWithCode(C4ScriptParserErrorCode.IncompatibleTypes, given, parm.getType(), given.getType());
						given.expectedToBeOfType(parm.getType());
//						if (parm.getScript() != ClonkCore.getDefault().ENGINE_OBJECT)
//							parm.inferTypeFromAssignment(given, parser);
					}
				}
				else if (field == null && getPredecessorInSequence() == null) {
					if (fieldName.equals(Keywords.Inherited)) {
						parser.errorWithCode(C4ScriptParserErrorCode.NoInheritedFunction, getExprStart(), getExprStart()+fieldName.length(), true, parser.getActiveFunc().getName(), true);
					}
					// _inherited yields no warning or error
					else if (!fieldName.equals(Keywords.SafeInherited)) {
						parser.errorWithCode(C4ScriptParserErrorCode.UndeclaredIdentifier, getExprStart(), getExprStart()+fieldName.length(), true, fieldName, true);
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
			return fieldName.equals("FindObjects") || fieldName.equals("FindObject2");
		}
		@Override
		public C4Object guessObjectType(C4ScriptParser parser) {
			// FIXME: could lead to problems when one of those functions does not take an id as first parameter
			if (params != null && params.length >= 1 && getType() == C4Type.OBJECT && (fieldName.startsWith("Create") || fieldName.startsWith("Find"))) {
				return params[0].guessObjectType(parser);
			}
			else if (params.length == 0 && fieldName.equals(C4Variable.THIS.getName())) {
				return parser.getContainerObject();
			}
			else if (isCriteriaSearch()) {
				return searchCriteriaAssumedResult(parser);
			}
			else if (fieldName.equals("GetID") && params.length == 0) {
				return getPredecessorInSequence() == null ? parser.getContainerObject() : getPredecessorInSequence().guessObjectType(parser);
			}
			if (field instanceof ITypedField) {
				C4Object obj = ((ITypedField)field).getExpectedContent();
				if (obj != null)
					return obj;
			}
			return super.guessObjectType(parser);
		}
		protected ExprBinaryOp applyOperatorTo(C4ScriptParser parser, ExprElm[] parms, C4ScriptOperator operator) throws CloneNotSupportedException {
			ExprBinaryOp op = new ExprBinaryOp(operator);
			ExprBinaryOp result = op;
			for (int i = 0; i < parms.length; i++) {
				ExprElm one = parms[i].newStyleReplacement(parser);
				ExprElm two = i+1 < parms.length ? parms[i+1] : null;
				if (op.getLeftSide() == null)
					op.setLeftSide(one);
				else if (two == null) {
					op.setRightSide(one);
				}
				else {
					ExprBinaryOp nu = new ExprBinaryOp(operator);
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
			C4ScriptOperator replOperator = C4ScriptOperator.oldStyleFunctionReplacement(fieldName);
			if (replOperator != null && params.length == 1) {
				// LessThan(x) -> x < 0
				if (replOperator.getNumArgs() == 2)
					return new ExprBinaryOp(replOperator, params[0].newStyleReplacement(parser), new ExprNumber(0));
				ExprElm n = params[0].newStyleReplacement(parser);
				if (n instanceof ExprBinaryOp)
					n = new ExprParenthesized(n);
				return new ExprUnaryOp(replOperator, replOperator.isPostfix() ? ExprUnaryOp.Placement.Postfix : ExprUnaryOp.Placement.Prefix, n);
			}
			if (replOperator != null && params.length >= 2) {
				return applyOperatorTo(parser, params, replOperator);
			}
			
			// ObjectCall(ugh, "UghUgh", 5) -> ugh->UghUgh(5)
			if (params.length >= 2 && fieldName.equals("ObjectCall") && params[1] instanceof ExprString) {
				ExprElm[] parmsWithoutObject = new ExprElm[params.length-2];
				for (int i = 0; i < parmsWithoutObject.length; i++)
					parmsWithoutObject[i] = params[i+2].newStyleReplacement(parser);
				return new ExprSequence(new ExprElm[] {
						params[0].newStyleReplacement(parser),
						new ExprObjectCall(true, null, 0),
						new ExprCallFunc(((ExprString)params[1]).stringValue(), parmsWithoutObject)});
			}
			
			// OCF_Awesome() -> OCF_Awesome
			if (params.length == 0 && field instanceof C4Variable) {
				return new ExprAccessVar(fieldName);
			}
			
			// Par(5) -> nameOfParm6
			if (params.length <= 1 && field == C4ScriptParser.CachedEngineFuncs.Par && (params.length == 0 || params[0] instanceof ExprNumber)) {
				ExprNumber number = params.length > 0 ? (ExprNumber) params[0] : ExprNumber.ZERO;
				if (number.intValue() >= 0 && number.intValue() < parser.getActiveFunc().getParameters().size())
					return new ExprAccessVar(parser.getActiveFunc().getParameters().get(number.intValue()).getName());
			}
			
			// SetVar(5, "ugh") -> Var(5) = "ugh"
			if (params.length == 2 && (fieldName.equals("SetVar") || fieldName.equals("SetLocal")) || fieldName.equals("AssignVar")) {
				return new ExprBinaryOp(C4ScriptOperator.Assign, new ExprCallFunc(fieldName.substring(fieldName.equals("AssignVar") ? "Assign".length() : "Set".length()), params[0].newStyleReplacement(parser)), params[1].newStyleReplacement(parser));
			}
			
			// Call("Func", 5, 5) -> Func(5, 5)
			if (params.length >= 1 && fieldName.equals("Call") && params[0] instanceof ExprString) {
				ExprElm[] parmsWithoutName = new ExprElm[params.length-1];
				for (int i = 0; i < parmsWithoutName.length; i++)
					parmsWithoutName[i] = params[i+1].newStyleReplacement(parser);
				return new ExprCallFunc(((ExprString)params[0]).stringValue(), parmsWithoutName);
			}
			
			return super.newStyleReplacement(parser);
		}
		@Override
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			return new FieldRegion(getField(parser), new Region(getExprStart(), fieldName.length()));
		}
		public C4Object searchCriteriaAssumedResult(C4ScriptParser context) {
			C4Object result = null;
			// parameters to FindObjects itself are also &&-ed together
			if (fieldName.equals("Find_And") || isCriteriaSearch()) {
				for (ExprElm parm : params) {
					if (parm instanceof ExprCallFunc) {
						ExprCallFunc call = (ExprCallFunc)parm;
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
			else if (fieldName.equals("Find_ID")) {
				if (params.length > 0 && params[0] instanceof ExprID) {
					result = ((ExprID)params[0]).guessObjectType(context);
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
					public C4Type getType() {
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
			return new ExprTuple(params);
		}
		@Override
		public boolean isReturn() {
			return fieldName.equals(Keywords.Return);
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
	}

	public static class ExprOperator extends ExprValue {
		private final C4ScriptOperator operator;

		@Override
		public C4Type getType() {
			return operator.getResultType();
		}

		public ExprOperator(C4ScriptOperator operator) {
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
		public boolean modifiable() {
			return getOperator().returnsRef();
		}

	}

	public static class ExprBinaryOp extends ExprOperator {
		
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
					return new ExprBinaryOp(op, getLeftSide().newStyleReplacement(context), getRightSide().newStyleReplacement(context));
				}
			}
			
			// blub() && blab() && return(1); -> {blub(); blab(); return(1);}
			if (getOperator() == C4ScriptOperator.And && (getParent() instanceof SimpleStatement)) {// && getRightSide().isReturn()) {
				LinkedList<ExprElm> leftSideArguments = new LinkedList<ExprElm>();
				ExprElm r;
				boolean works = true;
				// gather left sides (must not be operators)
				for (r = getLeftSide(); r instanceof ExprBinaryOp; r = ((ExprBinaryOp)r).getLeftSide()) {
					ExprBinaryOp op = (ExprBinaryOp)r;
					if (op.getOperator() != C4ScriptOperator.And) {
						works = false;
						break;
					}
					if (op.getRightSide() instanceof ExprBinaryOp) {
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
						statements.add(new ReturnStatement(((ExprCallFunc)getRightSide()).getReturnArg().newStyleReplacement(context)));
					else
						statements.add(new SimpleStatement(getRightSide().newStyleReplacement(context)));
					return new Block(statements);
				}
			}
			
			return super.newStyleReplacement(context);
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

		public ExprBinaryOp(C4ScriptOperator operator, ExprElm leftSide, ExprElm rightSide) {
			super(operator);
			setLeftSide(leftSide);
			setRightSide(rightSide);
		}

		public void checkTopLevelAssignment(C4ScriptParser parser) throws ParsingException {
			if (!getOperator().modifiesArgument())
				parser.warningWithCode(C4ScriptParserErrorCode.NoAssignment, this);
		}

		public ExprBinaryOp(C4ScriptOperator op) {
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
			boolean needsBrackets = leftSide instanceof ExprBinaryOp && getOperator().getPriority() > ((ExprBinaryOp)leftSide).getOperator().getPriority();
			if (needsBrackets)
				output.append("(");
			leftSide.print(output, depth+1);
			if (needsBrackets)
				output.append(")");
			
			output.append(" ");
			output.append(getOperator().getOperatorName());
			output.append(" ");
			
			needsBrackets = rightSide instanceof ExprBinaryOp && getOperator().getPriority() > ((ExprBinaryOp)rightSide).getOperator().getPriority();
			if (needsBrackets)
				output.append("(");
			rightSide.print(output, depth+1);
			if (needsBrackets)
				output.append(")");
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			getLeftSide().reportErrors(parser);
			getRightSide().reportErrors(parser);
			// sanity
			setExprRegion(getLeftSide().getExprStart(), getRightSide().getExprEnd());
			// i'm an assignment operator and i can't modify my left side :C
			if (getOperator().modifiesArgument() && !getLeftSide().modifiable()) {
				parser.errorWithCode(C4ScriptParserErrorCode.ExpressionNotModifiable, getLeftSide(), true);
			}
			if ((getOperator() == C4ScriptOperator.StringEqual || getOperator() == C4ScriptOperator.ne) && (parser.getStrictLevel() >= 2)) {
				parser.warningWithCode(C4ScriptParserErrorCode.ObsoleteOperator, this, getOperator().getOperatorName());
			}
			if (!getLeftSide().validForType(getOperator().getFirstArgType()))
				parser.warningWithCode(C4ScriptParserErrorCode.IncompatibleTypes, getLeftSide(), getOperator().getFirstArgType(), getLeftSide().getType());
			if (!getRightSide().validForType(getOperator().getSecondArgType()))
				parser.warningWithCode(C4ScriptParserErrorCode.IncompatibleTypes, getRightSide(), getOperator().getSecondArgType(), getRightSide().getType());
			
			getLeftSide().expectedToBeOfType(getOperator().getFirstArgType());
			getRightSide().expectedToBeOfType(getOperator().getSecondArgType());
			
			if (getOperator() == C4ScriptOperator.Assign) {
				getLeftSide().inferTypeFromAssignment(getRightSide(), parser);
			}
		}

	}

	public static class ExprParenthesized extends ExprValue {
		private ExprElm innerExpr;

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {innerExpr};
		}
		@Override
		public void setSubElements(ExprElm[] elements) {
			innerExpr = elements[0];
		}
		public ExprParenthesized(ExprElm innerExpr) {
			super();
			this.innerExpr = innerExpr;
		}
		public void print(StringBuilder output, int depth) {
			output.append("(");
			innerExpr.print(output, depth+1);
			output.append(")");
		}
		public C4Type getType() {
			return innerExpr.getType();
		}
		@Override
		public boolean modifiable() {
			return innerExpr.modifiable();
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
			if (!(getParent() instanceof ExprOperator))
				return innerExpr.newStyleReplacement(parser);
			return super.newStyleReplacement(parser);
		}

	}

	public static class ExprUnaryOp extends ExprOperator {

		public enum Placement {
			Prefix,
			Postfix
		}

		private final Placement placement;
		private ExprElm argument;

		public ExprUnaryOp(C4ScriptOperator operator, Placement placement, ExprElm argument) {
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
		
		private boolean needsSpace(ExprUnaryOp other) {
			C4ScriptOperator a = this.getOperator();
			C4ScriptOperator b = this.getOperator();
			return a.spaceNeededBetweenMeAnd(b);
		}

		public void print(StringBuilder output, int depth) {
			ExprUnaryOp unop = (argument instanceof ExprUnaryOp) ? (ExprUnaryOp)argument : null;
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
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			getArgument().reportErrors(parser);
			if (getOperator().modifiesArgument() && !getArgument().modifiable()) {
				//				System.out.println(getArgument().toString() + " does not behave");
				parser.errorWithCode(C4ScriptParserErrorCode.ExpressionNotModifiable, getArgument(), true);
			}
			if (!getArgument().validForType(getOperator().getFirstArgType())) {
				parser.warningWithCode(C4ScriptParserErrorCode.IncompatibleTypes, getArgument(), getOperator().getFirstArgType().toString(), getArgument().getType().toString());
			}
			getArgument().expectedToBeOfType(getOperator().getFirstArgType());
		}

		@Override
		public ExprElm newStyleReplacement(C4ScriptParser context) throws CloneNotSupportedException {
			// could happen when argument is transformed to binary operator
			ExprElm arg = getArgument().newStyleReplacement(context);
			if (arg instanceof ExprBinaryOp)
				return new ExprUnaryOp(getOperator(), placement, new ExprParenthesized(arg));
			if (getOperator() == C4ScriptOperator.Not && arg instanceof ExprParenthesized) {
				 ExprParenthesized brackets = (ExprParenthesized)arg;
				 if (brackets.getInnerExpr() instanceof ExprBinaryOp) {
					 ExprBinaryOp op = (ExprBinaryOp) brackets.getInnerExpr();
					 if (op.getOperator() == C4ScriptOperator.Equal) {
						 return new ExprBinaryOp(C4ScriptOperator.NotEqual, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
					 }
					 else if (op.getOperator() == C4ScriptOperator.NotEqual) {
						 return new ExprBinaryOp(C4ScriptOperator.Equal, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
					 }
					 else if (op.getOperator() == C4ScriptOperator.StringEqual) {
						 return new ExprBinaryOp(C4ScriptOperator.ne, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
					 }
					 else if (op.getOperator() == C4ScriptOperator.ne) {
						 return new ExprBinaryOp(C4ScriptOperator.StringEqual, op.getLeftSide().newStyleReplacement(context), op.getRightSide().newStyleReplacement(context));
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
		public boolean modifiable() {
			return placement == Placement.Prefix && getOperator().returnsRef();
		}

	}

	public static class ExprLiteral<T> extends ExprValue {
		private final T literal;

		public ExprLiteral(T literal) {
			super();
			this.literal = literal;
		}

		public T getLiteral() {
			return literal;
		}

		@Override
		public boolean modifiable() {
			return false;
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor) {
			return predecessor == null;
		}
		
		@Override
		public boolean isConstant() {
			return true;
		}

	}

	public static final class ExprNumber extends ExprLiteral<Long> {

		public static final ExprNumber ZERO = new ExprNumber(0);
		
		private boolean hex;

		public ExprNumber(long value, boolean hex) {
			super(new Long(value));
			this.hex = hex;
		}
		
		public ExprNumber(long value) {
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

		public C4Type getType() {
			if (longValue() == 0)
				return C4Type.UNKNOWN; // FIXME: to prevent warnings when assigning 0 to object-variables
			return C4Type.INT;
		}

		@Override
		public boolean canBeConvertedTo(C4Type otherType) {
			// 0 is the NULL object or NULL string
			return (longValue() == 0 && (otherType == C4Type.OBJECT || otherType == C4Type.STRING)) || super.canBeConvertedTo(otherType);
		}

		public boolean isHex() {
			return hex;
		}

		public void setHex(boolean hex) {
			this.hex = hex;
		}

	}

	public static final class ExprString extends ExprLiteral<String> {
		public ExprString(String literal) {
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
		public C4Type getType() {
			return C4Type.STRING;
		}
		
		@Override
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			if (getParent() instanceof ExprCallFunc) {
				ExprCallFunc parentFunc = (ExprCallFunc) getParent();
				int myIndex = parentFunc.indexOfParm(this);
				
				//  link to functions that are called indirectly
				
				// GameCall: look for nearest scenario and find function in its script
				if (myIndex == 0 && parentFunc.getFieldName().equals("GameCall")) {
					ClonkIndex index = parser.getContainer().getIndex();
					C4Scenario scenario = ClonkIndex.pickNearest(parser.getContainer().getResource(), index.getIndexedScenarios());
					if (scenario != null) {
						C4Function scenFunc = scenario.findFunction(stringValue());
						if (scenFunc != null)
							return new FieldRegion(scenFunc, this);
					}
				}
				
				// ScheduleCall: second parameter is function name; first is object to call the function in
				else if (myIndex == 1 && parentFunc.getFieldName().equals("ScheduleCall")) {
					C4Object typeToLookIn = parentFunc.getParams()[0].guessObjectType(parser);
					if (typeToLookIn != null) {
						C4Function func = typeToLookIn.findFunction(stringValue());
						if (func != null)
							return new FieldRegion(func, this);
					}
				}
				
				// LocalN: look for local var in object
				else if (myIndex == 0 && parentFunc.getFieldName().equals("LocalN")) {
					C4Object typeToLookIn = parentFunc.getParams().length > 1 ? parentFunc.getParams()[1].guessObjectType(parser) : null;
					if (typeToLookIn == null && parentFunc.getPredecessorInSequence() != null)
						typeToLookIn = parentFunc.getPredecessorInSequence().guessObjectType(parser);
					if (typeToLookIn == null)
						typeToLookIn = parser.getContainerObject();
					if (typeToLookIn != null) {
						C4Variable var = typeToLookIn.findLocalVariable(stringValue(), false);
						if (var != null)
							return new FieldRegion(var, this);
					}
				}
				
			}
			return null;
		}

	}

	public static final class ExprID extends ExprLiteral<C4ID> {
		public ExprID(C4ID literal) {
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
		public C4Type getType() {
			return C4Type.ID;
		}

		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			// FIXME: does not actually return an object of type idValue but the id itself :/
			return context.getContainer().getNearestObjectWithId(idValue());
		}

		@Override
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			return new FieldRegion(parser.getContainer().getNearestObjectWithId(idValue()), region(0));
		}

	}
	
	public static final class ExprBool extends ExprLiteral<Boolean> {
		public boolean booleanValue() {
			return getLiteral().booleanValue();
		}
		public ExprBool(boolean value) {
			super(new Boolean(value));
		}
		public C4Type getType() {
			return C4Type.BOOL;
		}
		@Override
		public void print(StringBuilder output, int depth) {
			output.append(booleanValue() ? Keywords.True : Keywords.False);
		}
	}

	public static final class ExprAccessArray extends ExprValue {

		private ExprElm argument;
		
		@Override
		public C4Type getType() {
			return C4Type.ANY; // FIXME: guess type of elements
		}

		public ExprAccessArray(ExprElm argument) {
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
		public boolean isValidInSequence(ExprElm predecessor) {
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
		public boolean modifiable() {
			return true;
		}

		public ExprElm getArgument() {
			return argument;
		}

	}

	public static class ExprArray extends ExprSequence {
		public ExprArray(ExprElm[] elms) {
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
		public C4Type getType() {
			return C4Type.ARRAY;
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor) {
			return predecessor == null;
		}

		@Override
		public boolean modifiable() {
			return false;
		}
		
		@Override
		public ExprElm getExemplaryArrayElement(C4ScriptParser context) {
			C4Type type = C4Type.UNKNOWN;
			for (ExprElm e : elements) {
				type = ExprElm.combineTypes(type, e.getType());
			}
			return type == C4Type.UNKNOWN ? super.getExemplaryArrayElement(context) : ExprElm.getExprElmForType(type);
		}

	}

	public static class ExprTuple extends ExprSequence {

		public ExprTuple(ExprElm[] elms) {
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

	public static class ExprEllipsis extends ExprElm {

		public ExprEllipsis() {
			super();
		}

		@Override
		public void print(StringBuilder output, int depth) {
			output.append("...");
		}

	}
	
	public static class ExprPlaceholder extends ExprElm {
		private String entryName;
		
		public ExprPlaceholder(String entryName) {
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
		public C4Type getType() {
			return null;
		}
		protected void printIndent(StringBuilder builder, int indentDepth) {
			for (int i = 0; i < indentDepth; i++)
				builder.append("\t"); // FIXME: should be done according to user's preferences
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
			if (!(body instanceof Block)) {
				builder.append("\n");
				printIndent(builder, depth);
			} else
				builder.append(" ");
			body.print(builder, depth + ((body instanceof Block) ? 0 : 1));
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
			if (returnExpr instanceof ExprParenthesized)
				return new ReturnStatement(((ExprParenthesized)returnExpr).getInnerExpr().newStyleReplacement(parser));
			// return (0, Sound("Ugh")); -> { Sound("Ugh"); return 0; }
			if (returnExpr instanceof ExprTuple) {
				ExprTuple tuple = (ExprTuple) returnExpr;
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
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			if (contents == null)
				return null;
			String[] assignments = contents.split("\\|");
			int off = 1;
			for (String assignment : assignments) {
				if (offset >= off && offset < off+assignment.length()) {
					String[] nameValue = assignment.split("=");
					if (nameValue.length == 2) {
						String name = nameValue[0].trim();
						String value = nameValue[1].trim();
						if (name.equals("Condition") || name.equals("Image"))
							return new FieldRegion(parser.getContainer().findDeclaration(value), new Region(off+nameValue[0].length()+1, value.length()));
					}
					break;
				}
				off += assignment.length()+1;
			}
			return null;
		}
	}
	
}
