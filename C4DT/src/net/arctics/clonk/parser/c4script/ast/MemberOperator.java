package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine.EngineSettings;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.Region;

/**
 * Either '->' or '.' operator. As a middleman, this operator delegates some of its operations to its predecessor, like
 * type expectations ({@link #expectedToBeOfType(IType, C4ScriptParser, TypeExpectancyMode, ParserErrorCode)}) or obtainment of its own type ({@link #obtainType(DeclarationObtainmentContext)}).<br/>
 * Different typing assumptions are made based on the notation.
 * @author madeen
 *
 */
public class MemberOperator extends ExprElm {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	boolean dotNotation;
	private boolean hasTilde;
	private ID id;
	private int idOffset;
	
	@Override
	protected void offsetExprRegion(int amount, boolean start, boolean end) {
		super.offsetExprRegion(amount, start, end);
		if (start)
			idOffset += amount;
	}
	
	/**
	 * Helper method to test whether some arbitrary expression ends with a MemberOperator in dot notation.
	 * @param expression The {@link ExprElm} to test
	 * @return True if the expression represents something like (CreateObject(Clonk)->GetContainer().), false if not.
	 */
	public static boolean endsWithDot(ExprElm expression) {
		return
			expression instanceof Sequence &&
			((Sequence)expression).lastElement() instanceof MemberOperator &&
			((MemberOperator)((Sequence)expression).lastElement()).dotNotation;
	}

	/**
	 * Construct a new MemberOperator.
	 * @param dotNotation If true, the operator represents a '.', otherwise '->'
	 * @param hasTilde Whether '->' is followed by '~'
	 * @param id {@link ID} specified after '->'. Can be null.
	 * @param idOffset Relative offset of id, used for correct hyperlink detection (see {@link #declarationAt(int, C4ScriptParser)})
	 */
	public MemberOperator(boolean dotNotation, boolean hasTilde, ID id, int idOffset) {
		super();
		this.dotNotation = dotNotation;
		this.hasTilde = hasTilde;
		this.id = id;
		this.idOffset = idOffset;
	}
	
	public static MemberOperator dotOperator() {
		return new MemberOperator(true, false, null, 0);
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
				output.append(id.stringValue());
				output.append("::"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Get the optionally specified id (->CLNK::) 
	 * @return The {@link ID} or null
	 */
	public ID getId() {
		return id;
	}

	/**
	 * Set the optionally specified id.
	 * @param id The {@link ID} to set
	 */
	public void setId(ID id) {
		this.id = id;
	}

	/**
	 * MemberOperators are never valid when not preceded by another {@link ExprElm}
	 */
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

	/**
	 * MemberOperator delegates this call to {@link #predecessorInSequence()}, if there is one.
	 * @see net.arctics.clonk.parser.c4script.ast.ExprElm#obtainType(DeclarationObtainmentContext)
	 */
	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		// explicit id
		if (id != null) {
			return context.containingScript().nearestDefinitionWithId(id);
		}
		// stuff before -> decides
		return predecessorInSequence() != null ? predecessorInSequence().type(context) : super.obtainType(context);
	}
	
	/**
	 * MemberOperator delegates this call to {@link #predecessorInSequence()}, if there is one.
	 * @see net.arctics.clonk.parser.c4script.ast.ExprElm#expectedToBeOfType(net.arctics.clonk.parser.c4script.IType, net.arctics.clonk.parser.c4script.C4ScriptParser, net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode, net.arctics.clonk.parser.ParserErrorCode)
	 */
	@Override
	public void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		// delegate to predecessor
		if (predecessorInSequence() != null)
			predecessorInSequence().expectedToBeOfType(type, context, mode, errorWhenFailed);
	}

	@Override
	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
		if (id != null && offset >= idOffset && offset < idOffset+4)
			return new EntityRegion(parser.containingScript().nearestDefinitionWithId(id), new Region(start()+idOffset, 4));
		return null;
	}

	/**
	 * Based on whether this MemberOperator uses dot notation or not, the preceding {@link ExprElm} will either be expected to be of type {@link PrimitiveType#PROPLIST} or
	 * {@link TypeSet#OBJECT_OR_ID}.<br/>
	 * Additionally, a warning is emitted if space between the actual operator and ~ is left and this is not allowed ({@link EngineSettings#spaceAllowedBetweenArrowAndTilde})
	 */
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		ExprElm pred = predecessorInSequence();
		if (pred != null)
			pred.sequenceTilMe().expectedToBeOfType(
				dotNotation ? PrimitiveType.PROPLIST : TypeSet.OBJECT_OR_ID, parser, TypeExpectancyMode.Hint,
				dotNotation ? ParserErrorCode.NotAProplist : ParserErrorCode.CallingMethodOnNonObject
			);
		if (getLength() > 3 && !parser.containingScript().engine().settings().spaceAllowedBetweenArrowAndTilde)
			parser.errorWithCode(ParserErrorCode.MemberOperatorWithTildeNoSpace, this);
	}
	
	@Override
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		DifferenceHandling handling = super.compare(other, listener);
		if (handling != DifferenceHandling.Equal)
			return handling;
		MemberOperator otherOp = (MemberOperator) other;
		if (dotNotation != otherOp.dotNotation)
			return listener.differs(this, other, "dotNotation");	
		if (hasTilde != otherOp.hasTilde)
			return listener.differs(this, other, "hasTilde");
		if (!Utilities.objectsEqual(id, otherOp.id))
			return listener.differs(this, other, "id");
		return DifferenceHandling.Equal;
	}
	
	public boolean hasTilde() {
		return hasTilde;
	}
	
	@Override
	public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
		if (context.containingScript().engine().settings().proplistsSupported) {
			ExprElm succ = successorInSequence();
			if (succ instanceof AccessDeclaration && ((AccessDeclaration)succ).declarationFromContext(context) instanceof Variable)
				return dotOperator();
		}
		return super.optimize(context);
	}

}