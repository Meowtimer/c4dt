package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ScriptBase.FindFieldInfo;
import net.arctics.clonk.parser.C4ScriptParser.ErrorCode;
import net.arctics.clonk.parser.C4ScriptParser.Keywords;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
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
		private int exprStart, exprEnd;
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		private transient ExprElm parent;
		private transient ExprElm predecessorInSequence;

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
			return null; // no idea, dude
		}
		
		public ExprElm getExemplaryArrayElement(C4ScriptParser context) {
			return NULL_EXPR;
		}

		public boolean modifiable() {
			return true;
		}

		public boolean hasSideEffects() {
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

		public ExprElm exhaustiveNewStyleReplacement(C4ScriptParser context) throws CloneNotSupportedException {
			ExprElm repl;
			for (ExprElm original = this; (repl = original.newStyleReplacement(context)) != original; original = repl);
			return repl;
		}
		
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
			return traverse(listener, null);
		}
		
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

		public static final ExprElm NULL_EXPR = new ExprElm() {};

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
		
		public boolean isReturn() {
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
			// explicit id
			if (id != null) {
				return context.getContainer().getIndex().getObjectFromEverywhere(id);
			}
			// stuff before -> decides
			return getPredecessorInSequence() != null ? getPredecessorInSequence().guessObjectType(context) : super.guessObjectType(context);
		}

		@Override
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			if (id != null && offset >= idOffset && offset < idOffset+4)
				return new FieldRegion(parser.getContainer().getIndex().getObjectFromEverywhere(id), new Region(getExprStart()+idOffset, 4));
			return null;
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
		public ExprCallFunc(String funcName, ExprElm... parms) {
			super(funcName);
			params = parms;
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
				FindFieldInfo info = new FindFieldInfo(lookIn.getIndex());
				C4Field field = lookIn.findFunction(fieldName, info);
				// eventually it's a variable called as a function (not after '->')
				if (field == null && p == null)
					field = lookIn.findVariable(fieldName, info);
				return field;
			} else if (p != null) {
				// find global function
				C4Field field = parser.getContainer().getIndex().findGlobalFunction(fieldName);
				if (field == null)
					field = ClonkCore.EXTERN_INDEX.findGlobalField(fieldName);
				if (field == null)
					field = ClonkCore.ENGINE_OBJECT.findFunction(fieldName);
				return field;
			}
			return null;
		}
		@Override
		public void reportErrors(C4ScriptParser parser) throws ParsingException {
			super.reportErrors(parser);
			if (fieldName.equals("Par")) {
				if (params.length > 0) {
					parser.unnamedParamaterUsed(params[0]);
				}
				else
					parser.unnamedParamaterUsed(ExprNumber.ZERO);
			}
			else if (fieldName.equals(Keywords.Return))
				parser.warningWithCode(ErrorCode.ReturnAsFunction, this);
			else {
				if (field instanceof C4Variable) {
					if (params.length == 0) {
						parser.warningWithCode(ErrorCode.VariableCalled, this, field.getName());
					} else {
						parser.errorWithCode(ErrorCode.VariableCalled, this, field.getName(), true);
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
						if (!given.validForType(parm.getType()))
							parser.warningWithCode(ErrorCode.IncompatibleTypes, given, parm.getType(), given.getType());
						given.expectedToBeOfType(parm.getType());
						//parm.inferTypeFromAssignment(given, parser);
					}
				}
				else if (field == null && getPredecessorInSequence() == null) {
					if (fieldName.equals(Keywords.Inherited)) {
						parser.errorWithCode(ErrorCode.NoInheritedFunction, getExprStart(), getExprStart()+fieldName.length(), true, parser.getActiveFunc().getName(), true);
					}
					// _inherited yields no warning or error
					else if (!fieldName.equals(Keywords.SafeInherited)) {
						parser.errorWithCode(ErrorCode.UndeclaredIdentifier, getExprStart(), getExprStart()+fieldName.length(), true, fieldName, true);
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
			if (params != null && getType() == C4Type.OBJECT && (fieldName.startsWith("Create") || fieldName.startsWith("Find"))) {
				if (params.length >= 1) {
					return params[0].guessObjectType(parser);
				}
			}
			else if (params.length == 0 && fieldName.equals(C4Variable.THIS.getName())) {
				return parser.getContainerObject();
			}
			else if (isCriteriaSearch()) {
				return searchCriteriaAssumedResult(parser);
			}
			else if (fieldName.equals("GetID") && params.length == 0) {
				return parser.getContainerObject();
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
			// TODO: for more than two arguments
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
			else if (params.length >= 2 && fieldName.equals("ObjectCall") && params[1] instanceof ExprString) {
				ExprElm[] parmsWithoutObject = new ExprElm[params.length-2];
				for (int i = 0; i < parmsWithoutObject.length; i++)
					parmsWithoutObject[i] = params[i+2].newStyleReplacement(parser);
				return new ExprSequence(new ExprElm[] {
						params[0].newStyleReplacement(parser),
						new ExprObjectCall(false, null, 0),
						new ExprCallFunc(((ExprString)params[1]).stringValue(), parmsWithoutObject)});
			}
			
			// OCF_Awesome() -> OCF_Awesome
			else if (params.length == 0 && field instanceof C4Variable) {
				return new ExprAccessVar(fieldName);
			}
			
			// Par(5) -> nameOfParm6
			else if (params.length <= 1 && fieldName.equals("Par") && (params.length == 0 || params[0] instanceof ExprNumber)) {
				ExprNumber number = params.length > 0 ? (ExprNumber) params[0] : ExprNumber.ZERO;
				if (number.intValue() >= 0 && number.intValue() < parser.getActiveFunc().getParameters().size())
					return new ExprAccessVar(parser.getActiveFunc().getParameters().get(number.intValue()).getName());
			}
			
			// SetVar(5, "ugh") -> Var(5) = "ugh"
			else if (params.length == 2 && fieldName.equals("SetVar")) {
				return new ExprBinaryOp(C4ScriptOperator.Assign, new ExprCallFunc("Var", params[0].newStyleReplacement(parser)), params[1].newStyleReplacement(parser));
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
		
		@Override
		public boolean modifiable() {
			return false;
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
			if (getOperator() == C4ScriptOperator.And) {
				List<ExprElm> leftSideArguments = new LinkedList<ExprElm>();
				ExprBinaryOp op;
				ExprElm r;
				// gather left sides (must not be operators)
				for (r = this; (op = r instanceof ExprBinaryOp ? (ExprBinaryOp)r : null) != null && op.getOperator() == C4ScriptOperator.And && !(op.getLeftSide() instanceof ExprBinaryOp); r = ((ExprBinaryOp)r).getRightSide()) {
					leftSideArguments.add(op.getLeftSide());
				}
				// return at the right end signals this should rather be a block
				if (r instanceof ExprCallFunc && ((ExprCallFunc)r).getFieldName().equals(Keywords.Return)) {
					List<Statement> statements = new LinkedList<Statement>();
					// wrap expressions in statements
					for (ExprElm ex : leftSideArguments) {
						statements.add(new SimpleStatement(ex.newStyleReplacement(context)));
					}
					// convert func call to proper return statement
					statements.add(new ReturnStatement(((ExprCallFunc)r).getReturnArg().newStyleReplacement(context)));
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

		public void print(StringBuilder output, int depth) {
			
			// put brackets around operands in case some transformation messed up prioritization
			boolean needsBrackets = leftSide instanceof ExprBinaryOp && getOperator().priority() > ((ExprBinaryOp)leftSide).getOperator().priority();
			if (needsBrackets)
				output.append("(");
			leftSide.print(output, depth+1);
			if (needsBrackets)
				output.append(")");
			
			output.append(" ");
			output.append(getOperator().getOperatorName());
			output.append(" ");
			
			needsBrackets = rightSide instanceof ExprBinaryOp && getOperator().priority() > ((ExprBinaryOp)rightSide).getOperator().priority();
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
				parser.errorWithCode(ErrorCode.ExpressionNotModifiable, getLeftSide(), true);
			}
			if (!getLeftSide().validForType(getOperator().getFirstArgType()))
				parser.warningWithCode(ErrorCode.IncompatibleTypes, getLeftSide(), getOperator().getFirstArgType(), getLeftSide().getType());
			if (!getRightSide().validForType(getOperator().getSecondArgType()))
				parser.warningWithCode(ErrorCode.IncompatibleTypes, getRightSide(), getOperator().getSecondArgType(), getRightSide().getType());
			
			getLeftSide().expectedToBeOfType(getOperator().getFirstArgType());
			getRightSide().expectedToBeOfType(getOperator().getSecondArgType());
			
			if (getOperator() == C4ScriptOperator.Assign) {
				if (getLeftSide() instanceof ExprAccessVar) {
					C4Variable v = (C4Variable) ((ExprAccessVar)getLeftSide()).getField(parser);
					if (v != null)
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

		public void print(StringBuilder output, int depth) {
			if (placement == Placement.Postfix) {
				argument.print(output, depth+1);
				if (argument instanceof ExprUnaryOp && ((ExprUnaryOp)argument).placement == Placement.Postfix)
					output.append(" "); // - -5 -.-
				output.append(getOperator().getOperatorName());
			} else {
				output.append(getOperator().getOperatorName());
				if (argument instanceof ExprUnaryOp && ((ExprUnaryOp)argument).placement == Placement.Prefix)
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
				parser.errorWithCode(ErrorCode.ExpressionNotModifiable, getArgument(), true);
			}
			if (!getArgument().validForType(getOperator().getFirstArgType())) {
				parser.warningWithCode(ErrorCode.IncompatibleTypes, getArgument(), getOperator().getFirstArgType().toString(), getArgument().getType().toString());
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
				return C4Type.ANY; // FIXME: to prevent warnings when assigning 0 to object-variables
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
			return context.getContainer().getIndex().getObjectFromEverywhere(idValue());
		}

		@Override
		public FieldRegion fieldAt(int offset, C4ScriptParser parser) {
			return new FieldRegion(parser.getContainer().getIndex().getObjectFromEverywhere(idValue()), region(0));
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

	public static final class ExprAccessArray extends ExprUnaryOp {

		@Override
		public C4Type getType() {
			return C4Type.ANY; // not type of operator. the field is dead in this class :/
		}

		public ExprAccessArray(ExprElm argument) {
			super(null, Placement.Postfix, argument);
			// TODO Auto-generated constructor stub
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
		public boolean modifiable() {
			return true;
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
	}
	
	/**
	 * A {} block
	 * @author madeen
	 *
	 */
	public static class Block extends Statement {
		
		private Statement[] statements;

		public Block(List<Statement> statements) {
			this(statements.toArray(new Statement[0]));
		}
		
		public Block(Statement[] statements) {
			super();
			this.statements = statements;
		}

		public ExprElm[] getStatements() {
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
		public void print(StringBuilder builder, int depth) {
			builder.append("{\n");
			for (Statement statement : statements) {
				printIndent(builder, depth); statement.print(builder, depth+1); builder.append("\n");
			}
			printIndent(builder, depth-1); builder.append("}");
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
		}

		protected void printBody(StringBuilder builder, int depth) {
			if (!(body instanceof Block)) {
				builder.append("\n");
				printIndent(builder, depth);
			} else
				builder.append(" ");
			body.print(builder, depth + ((body instanceof Block) ? 0 : 1));
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
				if (!(elseExpr instanceof Block)) {
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
			// TODO Auto-generated constructor stub
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
			// remove ';' that elementExpr prints
			if (builder.charAt(builder.length()-1) == ';')
				builder.deleteCharAt(builder.length()-1);
			builder.append(" " + Keywords.In + " ");
			arrayExpr.print(builder, depth+1);
			builder.append(") ");
			body.print(builder, depth);
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
		public VarDeclarationStatement(List<Pair<String, ExprElm>> varInitializations) {
			super();
			this.varInitializations = varInitializations;
		}
		@Override
		public String getKeyword() {
			return Keywords.VarNamed;
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
							return new FieldRegion(parser.getContainer().findField(value), new Region(off+nameValue[0].length()+1, value.length()));
					}
					break;
				}
				off += assignment.length()+1;
			}
			return null;
		}
	}
	
}
