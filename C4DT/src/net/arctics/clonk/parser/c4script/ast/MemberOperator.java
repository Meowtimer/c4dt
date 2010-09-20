package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4TypeSet;
import net.arctics.clonk.parser.c4script.IType;

import org.eclipse.jface.text.Region;

public class MemberOperator extends ExprElm {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	boolean dotNotation;
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
			pred.expectedToBeOfType(
				dotNotation ? C4Type.PROPLIST : C4TypeSet.OBJECT_OR_ID, parser, TypeExpectancyMode.Hint,
				dotNotation ? ParserErrorCode.NotAProplist : ParserErrorCode.CallingMethodOnNonObject
			);
		}
	}

}