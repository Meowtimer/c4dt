package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IResolvableType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;

public class IterateArrayStatement extends KeywordStatement implements ILoop {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ExprElm elementExpr, arrayExpr, body;

	public IterateArrayStatement(ExprElm elementExpr, ExprElm arrayExpr, ExprElm body) {
		super();
		this.elementExpr = elementExpr;
		this.arrayExpr   = arrayExpr;
		this.body        = body;
		assignParentToSubElements();
	}

	public ExprElm arrayExpr() {
		return arrayExpr;
	}

	public void setArrayExpr(ExprElm arrayExpr) {
		this.arrayExpr = arrayExpr;
	}

	public ExprElm elementExpr() {
		return elementExpr;
	}

	public void setElementExpr(ExprElm elementExpr) {
		this.elementExpr = elementExpr;
	}

	@Override
	public String keyword() {
		return Keywords.For;
	}

	@Override
	public void doPrint(ExprWriter writer, int depth) {
		StringBuilder builder = new StringBuilder(keyword().length()+2+1+1+Keywords.In.length()+1+2);
		builder.append(keyword() + " ("); //$NON-NLS-1$
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

	@Override
	public ExprElm body() {
		return body;
	}

	public void setBody(ExprElm body) {
		this.body = body;
	}

	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {elementExpr, arrayExpr, body};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		elementExpr = elms[0];
		arrayExpr   = elms[1];
		body        = elms[2];
	}
	
	@Override
	public boolean skipReportingProblemsForSubElements() {return true;}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		Variable loopVariable;
		AccessVar accessVar;
		if (elementExpr instanceof VarDeclarationStatement)
			loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
		else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
			if (accessVar.declarationFromContext(parser) == null) {
				// implicitly create loop variable declaration if not found
				SourceLocation varPos = parser.absoluteSourceLocationFromExpr(accessVar);
				loopVariable = parser.createVarInScope(accessVar.declarationName(), Scope.VAR, varPos.start(), varPos.end(), null);
			} else
				loopVariable = as(accessVar.declaration(), Variable.class);
		} else
			loopVariable = null;
		
		parser.reportProblemsOf(elementExpr, true);
		parser.reportProblemsOf(arrayExpr, true);

		IType type = arrayExpr.type(parser);
		if (!type.canBeAssignedFrom(PrimitiveType.ARRAY))
			parser.warning(ParserErrorCode.IncompatibleTypes, arrayExpr, 0, type, PrimitiveType.ARRAY);
		IType elmType = IResolvableType._.resolve(ArrayType.elementTypeSet(type), parser, arrayExpr.callerType(parser));
		parser.pushTypeInfos();
		if (loopVariable != null) {
			if (elmType != null)
				new AccessVar(loopVariable).expectedToBeOfType(elmType, parser);
			loopVariable.setUsed(true);
		}
		parser.reportProblemsOf(body, true);
		parser.popTypeInfos(true);
	}

}