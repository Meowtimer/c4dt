package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.objectsEqual;
import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.IType;

import org.eclipse.jface.text.Region;

/**
 * Either '->' or '.' operator. As a middleman, this operator delegates some of its operations to its predecessor, like
 * type expectations ({@link #typeJudgement(IType, C4ScriptParser, TypingJudgementMode, ParserErrorCode)}) or obtainment of its own type ({@link #unresolvedType(ProblemReportingContext)}).<br/>
 * Different typing assumptions are made based on the notation.
 * @author madeen
 *
 */
public class MemberOperator extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	boolean dotNotation;
	private final boolean hasTilde;
	private ID id;
	private int idOffset;

	public ID id() { return id; }
	public boolean dotNotation() { return dotNotation; }

	@Override
	protected void offsetExprRegion(int amount, boolean start, boolean end) {
		super.offsetExprRegion(amount, start, end);
		if (start)
			idOffset += amount;
	}

	/**
	 * Helper method to test whether some arbitrary expression ends with a MemberOperator in dot notation.
	 * @param expression The {@link ASTNode} to test
	 * @return True if the expression represents something like (CreateObject(Clonk)->GetContainer().), false if not.
	 */
	public static boolean endsWithDot(ASTNode expression) {
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
	 * @param idOffset Relative offset of id, used for correct hyperlink detection (see {@link #entityAt(int, C4ScriptParser)})
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
	public void doPrint(ASTNodePrinter output, int depth) {
		if (dotNotation)
			// so simple
			output.append('.');
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
	 * MemberOperators are never valid when not preceded by another {@link ASTNode}
	 */
	@Override
	public boolean isValidInSequence(ASTNode predecessor, C4ScriptParser context) {
		if (predecessor != null)
			/*IType t = predecessor.getType(context);
			if (t == null || t.subsetOfType(C4TypeSet.ARRAY_OR_STRING))
				return false;*/
			return true;
		return false;
	}

	@Override
	public boolean isValidAtEndOfSequence(C4ScriptParser context) {
		return false;
	}

	@Override
	public EntityRegion entityAt(int offset, ProblemReportingContext context) {
		if (id != null && offset >= idOffset && offset < idOffset+4)
			return new EntityRegion(context.script().nearestDefinitionWithId(id), new Region(start()+idOffset, 4));
		return null;
	}

	@Override
	public boolean equalAttributes(ASTNode other) {
		if (!super.equalAttributes(other))
			return false;
		MemberOperator otherOp = (MemberOperator) other;
		if (dotNotation != otherOp.dotNotation || hasTilde != otherOp.hasTilde || objectsEqual(id, otherOp.id))
			return false;
		return true;
	}

	public boolean hasTilde() {
		return hasTilde;
	}

	@Override
	public ASTNode optimize(final ProblemReportingContext context) throws CloneNotSupportedException {
		if (context.script().engine().settings().supportsProplists) {
			ASTNode succ = successorInSequence();
			if (succ instanceof AccessVar)
				return dotOperator();
		}
		return super.optimize(context);
	}

	public static boolean unforgiving(ASTNode p) {
		return p instanceof MemberOperator && !((MemberOperator)p).hasTilde();
	}

}