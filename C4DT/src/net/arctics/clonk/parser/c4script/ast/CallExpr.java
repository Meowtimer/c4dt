package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;

/**
 * Call the sequence
 * @author madeen
 *
 */
public class CallExpr extends Tuple implements IFunctionCall {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public CallExpr(ASTNode[] params) {
		super(params);
	}
	
	@Override
	public ASTNode[] params() {
		return subElements();
	}
	
	@Override
	public boolean isValidInSequence(ASTNode predecessor, C4ScriptParser context) {
		return predecessor != null;
	}
	
	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		IType type = predecessorInSequence().unresolvedType(context);
		if (type instanceof FunctionType)
			return ((FunctionType)type).prototype().returnType();
		else
			return PrimitiveType.ANY;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		CallDeclaration.printParmString(output, subElements(), depth);
	}
	
	@Override
	public boolean hasSideEffects() {
		return true;
	}

	@Override
	public Function quasiCalledFunction(DeclarationObtainmentContext context) {
		for (IType type : predecessorInSequence().type(context))
			if (type instanceof FunctionType)
				return ((FunctionType)type).prototype();
		return null;
	}

	@Override
	public int parmsStart() {
		return this.start()+1;
	}
	
	@Override
	public int parmsEnd() {
		return this.end()-1;
	}
	
	@Override
	public int indexOfParm(ASTNode parm) {
		for (int i = 0; i < elements.length; i++)
			if (elements[i] == parm)
				return i;
		return -1;
	}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		if (!parser.script().engine().settings().supportsFunctionRefs)
			parser.error(ParserErrorCode.FunctionRefNotAllowed, this, C4ScriptParser.NO_THROW, parser.script().engine().name());
		else {
			IType type = predecessorInSequence().unresolvedType(parser);
			if (!PrimitiveType.FUNCTION.canBeAssignedFrom(type))
				parser.error(ParserErrorCode.CallingExpression, this, C4ScriptParser.NO_THROW);
		}
	}

	@Override
	public IType concreteParameterType(Variable parameter, DeclarationObtainmentContext context) {
		Function f = quasiCalledFunction(context);
		if (f != null) {
			int ndx = f.parameters().indexOf(parameter);
			if (ndx != -1 && ndx < elements.length)
				return elements[ndx].type(context);
		}
		return PrimitiveType.UNKNOWN;
	}
	
}
