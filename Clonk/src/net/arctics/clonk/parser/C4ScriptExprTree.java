package net.arctics.clonk.parser;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Object.FindFieldInfo;
import net.arctics.clonk.parser.C4ScriptParser.ErrorCode;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;

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
		public TraversalContinuation expressionDetected(ExprElm expression);
	}
	
	/**
	 * @author madeen
	 * base class for making expression trees
	 */
	public abstract static class ExprElm implements IRegion, Cloneable {
		private int exprStart, exprEnd;
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		private ExprElm parent;
		private ExprElm predecessorInSequence;

		public ExprElm getParent() {
			return parent;
		}

		public void warnIfNoSideEffects(C4ScriptParser parser) {
			if (!hasSideEffects())
				parser.warningWithCode(ErrorCode.NoSideEffects, this);
		}

		public void setParent(ExprElm parent) {
			this.parent = parent;
		}

		public void print(StringBuilder output) {
			output.append("Implement me");
		}

		public boolean isValidInSequence(ExprElm predecessor) {
			return true;
		}
		public C4Type getType() {
			return C4Type.UNKNOWN;
		}

		public C4Object guessObjectType(C4ScriptParser context) {
			return null; // no idea, dude
		}

		public boolean modifiable() {
			return true;
		}

		public boolean hasSideEffects() {
			return false;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			print(b);
			return b.toString();
		}

		public int getLength() {
			return exprEnd-exprStart;
		}

		public int getOffset() {
			return exprStart;
		}

		public int getExprEnd() {
			return exprEnd;
		}

		public int getExprStart() {
			return exprStart;
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
			// ...
		}

		public ExprElm newStyleReplacement() throws CloneNotSupportedException {
			ExprElm[] subElms = getSubElements();
			ExprElm[] newSubElms = new ExprElm[subElms.length];
			boolean differentSubElms = false;
			for (int i = 0; i < subElms.length; i++) {
				newSubElms[i] = subElms[i].newStyleReplacement();
				if (newSubElms[i] != subElms[i])
					differentSubElms = true;
			}
			if (differentSubElms) {
				ExprElm replacement = (ExprElm)this.clone();
				replacement.setSubElements(newSubElms);
				return replacement;
			}
			return this; // nothing to be changed
		}
		
		public boolean canBeConvertedTo(C4Type otherType) {
			// 5555 is ID
			return getType() == C4Type.INT && otherType == C4Type.ID;
		}
		
		public boolean validForType(C4Type t) {
			return t.canBeAssignedFrom(getType()) || canBeConvertedTo(t);
		}
		
		public TraversalContinuation traverse(IExpressionListener listener) {
			TraversalContinuation c = listener.expressionDetected(this);
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
				switch (sub.traverse(listener)) {
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

		public static final ExprElm NULL_EXPR = new ExprElm() {};

		public IRegion region(int offset) {
			return new Region(offset+getExprStart(), getExprEnd()-getExprStart());
		}

	}

	public static class ExprObjectCall extends ExprElm {
		private boolean hasTilde;
		private C4ID id;

		public ExprObjectCall(boolean hasTilde, C4ID id) {
			super();
			this.hasTilde = hasTilde;
			this.id = id;
		}

		public void print(StringBuilder output) {
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
				if (t == null || t == C4Type.ARRAY || t == C4Type.STRING || t == C4Type.UNKNOWN)
					return false;
			}
			return true;
		}

		@Override
		public C4Type getType() {
			return null; // invalid as an expression
		}

		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			return getPredecessorInSequence() != null ? getPredecessorInSequence().guessObjectType(context) : super.guessObjectType(context);
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
		public void print(StringBuilder output) {
			for (ExprElm e : elements) {
				e.print(output);
			}
		}
		public C4Type getType() {
			return (elements == null || elements.length == 0) ? C4Type.UNKNOWN : elements[elements.length-1].getType();
		}
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

	}

	public static abstract class ExprAccessField extends ExprValue {
		protected C4Field field;
		private boolean fieldNotFound = false;
		protected final String fieldName;
		
		public final C4Field getField(C4ScriptParser parser) {
			if (field == null && !fieldNotFound) {
				field = getFieldImpl(parser);
				fieldNotFound = field == null;
			}
			return field;
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
		public void print(StringBuilder output) {
			output.append(fieldName);
		}

		public IRegion fieldRegion(int offset) {
			return new Region(offset+getExprStart(), fieldName.length());
		}
	}

	public static class ExprAccessVar extends ExprAccessField {
		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			return (field != null) ? ((C4Variable)field).getExpectedContent() : super.guessObjectType(context);
		}

		public ExprAccessVar(String varName) {
			super(varName);
		}

		@Override
		public boolean isValidInSequence(ExprElm predecessor) {
			if (predecessor == null)
				return true;
			return false;
		}

		@Override
		protected C4Field getFieldImpl(C4ScriptParser parser) {
			FindFieldInfo info = new FindFieldInfo(parser.getContainer().getIndex());
			info.setContext(parser.getActiveFunc());
			return parser.getContainer().findVariable(fieldName, info);
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (field == null)
				parser.warningWithCode(ErrorCode.UndeclaredIdentifier, this, fieldName);
		}

		@Override
		public C4Type getType() {
			return field != null ? ((C4Variable)field).getType() : super.getType();
		}

	}

	public static class ExprCallFunc extends ExprAccessField {
		private ExprElm[] params;
		public ExprCallFunc(String funcName, ExprElm[] parms) {
			super(funcName);
			params = parms;
		}
		@Override
		public void print(StringBuilder output) {
			super.print(output);
			output.append("(");
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					if (params[i] != null)
						params[i].print(output);
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
		protected C4Field getFieldImpl(C4ScriptParser parser) {
			if (fieldName.equals("return"))
				return null;
			ExprElm p = getPredecessorInSequence();
			C4Object lookIn = p == null ? parser.getContainer() : p.guessObjectType(parser);
			if (lookIn != null) {
				FindFieldInfo info = new FindFieldInfo(lookIn.getIndex());
				C4Field field = lookIn.findFunction(fieldName, info);
				// eventually it's a variable called as a function (not after '->')
				if (field == null && p == null)
					field = lookIn.findVariable(fieldName, info);
				return field;
			}
			return null;
		}
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (fieldName.equals("return"))
				parser.warningWithCode(ErrorCode.ReturnAsFunction, this);
			else {
				if (field instanceof C4Variable) {
					if (params.length == 0) {
						parser.warningWithCode(ErrorCode.VariableCalled, this, field.getName());
					} else {
						parser.errorWithCode(ErrorCode.VariableCalled, this, field.getName());
					}
				}
				else if (field instanceof C4Function) {
					C4Function f = (C4Function)field;
					int givenParam = 0;
					for (C4Variable parm : f.getParameter()) {
						if (givenParam >= params.length)
							break;
						ExprElm given = params[givenParam++];
						if (given == null)
							continue;
						if (!given.validForType(parm.getType()))
							parser.warningWithCode(ErrorCode.IncompatibleTypes, given, parm.getType(), given.getType());
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
		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			if (params != null && getType() == C4Type.OBJECT && (fieldName.startsWith("Create") || fieldName.startsWith("Find"))) {
				if (params.length >= 1 && params[0] instanceof ExprID) {
					ExprID id = (ExprID)params[0];
					ClonkIndex index = context.getContainer().getIndex();
					C4Object obj = index.getLastObjectWithId(id.idValue());
					if (obj == null && ClonkCore.EXTERN_INDEX != index)
						obj = ClonkCore.EXTERN_INDEX.getLastObjectWithId(id.idValue());
					return obj;
				}
			}
			return super.guessObjectType(context);
		}
		@Override
		public ExprElm newStyleReplacement() throws CloneNotSupportedException {
			C4ScriptOperator replOperator = C4ScriptOperator.oldStyleFunctionReplacement(fieldName);
			// TODO: for more than two arguments
			if (replOperator != null && params.length == 2) {
				return new ExprBinaryOp(replOperator, params[0].newStyleReplacement(), params[1].newStyleReplacement());
			}
			return super.newStyleReplacement();
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
			return getOperator().modifiesArgument();
		}

	}

	public static class ExprBinaryOp extends ExprOperator {
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
				parser.warningWithCode(ErrorCode.NoAssignment, this);
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

		public void print(StringBuilder output) {
			leftSide.print(output);
			output.append(" ");
			output.append(getOperator().getOperatorName());
			output.append(" ");
			rightSide.print(output);
		}

		@Override
		public boolean modifiable() {
			return false;
		}

		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			getLeftSide().reportErrors(parser);
			getRightSide().reportErrors(parser);
			// sanity
			setExprRegion(getLeftSide().getExprStart(), getRightSide().getExprEnd());
			// i'm an assignment operator and i can't modify my left side :C
			if (getOperator().modifiesArgument() && !getLeftSide().modifiable()) {
				parser.errorWithCode(ErrorCode.ExpressionNotModifiable, getLeftSide());
			}
			if (!getLeftSide().validForType(getOperator().getFirstArgType()))
				parser.warningWithCode(ErrorCode.IncompatibleTypes, getLeftSide(), getOperator().getFirstArgType(), getLeftSide().getType());
			if (!getRightSide().validForType(getOperator().getSecondArgType()))
				parser.warningWithCode(ErrorCode.IncompatibleTypes, getRightSide(), getOperator().getSecondArgType(), getRightSide().getType());
			
			if (getOperator() == C4ScriptOperator.Assign) {
				if (getLeftSide() instanceof ExprAccessVar) {
					C4Variable v = (C4Variable) ((ExprAccessVar)getLeftSide()).getField(parser);
					v.inferTypeFromAssignment(getRightSide(), parser);
				}
			}
		}

	}

	public static class ExprParenthesized extends ExprValue {
		private ExprElm innerExpr;

		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {innerExpr};
		}
		public ExprParenthesized(ExprElm innerExpr) {
			super();
			this.innerExpr = innerExpr;
		}
		public void print(StringBuilder output) {
			output.append("(");
			innerExpr.print(output);
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

		public void print(StringBuilder output) {
			if (placement == Placement.Postfix) {
				argument.print(output);
				if (argument instanceof ExprUnaryOp)
					output.append(" "); // - -5 -.-
				output.append(getOperator().getOperatorName());
			} else {
				output.append(getOperator().getOperatorName());
				if (argument instanceof ExprUnaryOp)
					output.append(" "); // - -5 -.-
				argument.print(output);
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
				parser.errorWithCode(ErrorCode.ExpressionNotModifiable, getArgument());
			}
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

	}

	public static final class ExprNumber extends ExprLiteral<Integer> {

		public ExprNumber(int value) {
			super(new Integer(value));
		}

		public int intValue() {
			return getLiteral().intValue();
		}

		public void print(StringBuilder output) {
			output.append(intValue());
		}

		public C4Type getType() {
			return C4Type.INT;
		}

		@Override
		public boolean canBeConvertedTo(C4Type otherType) {
			// 0 is the NULL object or NULL string
			return (intValue() == 0 && (otherType == C4Type.OBJECT || otherType == C4Type.STRING)) || super.canBeConvertedTo(otherType);
		}

	}

	public static final class ExprString extends ExprLiteral<String> {
		public ExprString(String literal) {
			super(literal);
		}

		public String stringValue() {
			return getLiteral();
		}
		public void print(StringBuilder output) {
			output.append("\"");
			output.append(stringValue());
			output.append("\"");
		}

		@Override
		public C4Type getType() {
			return C4Type.STRING;
		}

	}

	public static final class ExprID extends ExprLiteral<C4ID> {
		public ExprID(C4ID literal) {
			super(literal);
		}

		public C4ID idValue() {
			return getLiteral();
		}

		public void print(StringBuilder output) {
			output.append(idValue().getName());
		}

		@Override
		public C4Type getType() {
			return C4Type.ID;
		}

		@Override
		public C4Object guessObjectType(C4ScriptParser context) {
			// FIXME: does not actually return an object of type idValue but the id itself :/
			C4Object result = context.getContainer().getProject().getIndexedData().getLastObjectWithId(idValue());
			if (result == null)
				result = ClonkCore.EXTERN_INDEX.getLastObjectWithId(idValue());
			return result;
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
		public void print(StringBuilder output) {
			output.append(booleanValue() ? "true" : "false");
		}
	}

	public static final class ExprAccessArray extends ExprUnaryOp {

		@Override
		public C4Type getType() {
			return C4Type.ANY; // not type of operator. the field is dead in this class :/
		}

		public ExprAccessArray(ExprElm argument) {
			super(null, Placement.Postfix, argument);
			// TODO Auto-generated constructor stub
		}

		public void print(StringBuilder output) {
			output.append("[");
			getArgument().print(output);
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

	}

	public static class ExprArray extends ExprSequence {
		public ExprArray(ExprElm[] elms) {
			super(elms);
		}

		public void print(StringBuilder output) {
			output.append("[");
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] != null)
					elements[i].print(output);
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

	}

	public static class ExprTuple extends ExprSequence {

		public ExprTuple(ExprElm[] elms) {
			super(elms);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void print(StringBuilder output) {
			output.append('(');
			if (elements != null) {
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] != null)
						elements[i].print(output);
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

		public void print(StringBuilder output) {
			output.append("...");
		}

	}
}
