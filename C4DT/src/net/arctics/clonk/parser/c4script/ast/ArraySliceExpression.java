package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public class ArraySliceExpression extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode lo, hi;
	
	public ArraySliceExpression(ASTNode lo, ASTNode hi) {
		this.lo = lo;
		this.hi = hi;
		assignParentToSubElements();
	}
	
	public ASTNode lo() {
		return lo;
	}
	
	public ASTNode hi() {
		return hi;
	}
	
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("["); //$NON-NLS-1$
		if (lo != null)
			lo.print(output, depth+1);
		output.append(":"); //$NON-NLS-1$
		if (hi != null)
			hi.print(output, depth+1);
		output.append("]"); //$NON-NLS-1$
	}
	
	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {lo, hi};
	}
	@Override
	public void setSubElements(ASTNode[] subElements) {
		lo  = subElements[0];
		hi = subElements[1];
	}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		super.reportProblems(parser);
		IType type = predecessorType(parser);
		ArrayElementExpression.warnIfNotArray(predecessorInSequence(), parser, type);
	}
	
	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		ArrayType arrayType = predecessorTypeAs(ArrayType.class, context);
		if (arrayType != null)
			return lo == null && hi == null ? arrayType : arrayType.typeForSlice(
				evaluateAtParseTime(lo, context),
				evaluateAtParseTime(hi, context)
			);
		else
			return PrimitiveType.ARRAY;
	}

	@Override
	public void assignment(ASTNode rightSide, C4ScriptParser context) {
		ArrayType arrayType = predecessorTypeAs(ArrayType.class, context);
		IType sliceType = rightSide.unresolvedType(context);
		if (arrayType != null)
			context.storeType(predecessorInSequence(), arrayType.modifiedBySliceAssignment(
				evaluateAtParseTime(lo, context),
				evaluateAtParseTime(hi, context),
				sliceType
			));
	}
	
	@Override
	public boolean isModifiable(C4ScriptParser context) { return true; }
	@Override
	public boolean isValidInSequence(ASTNode predecessor, C4ScriptParser context) { return predecessor != null; }
	
}