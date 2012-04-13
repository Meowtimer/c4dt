package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.TypeInfoList;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;

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
	public boolean skipReportingErrorsForSubElements() {return true;}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		parser.reportErrorsOf(elementExpr, true, null);
		parser.reportErrorsOf(arrayExpr, true, null);
		
		Variable loopVariable = elementExpr instanceof VarDeclarationStatement
			? ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable
			: null;
		IType type = arrayExpr.type(parser);
		if (!type.canBeAssignedFrom(PrimitiveType.ARRAY))
			parser.warningWithCode(ParserErrorCode.IncompatibleTypes, arrayExpr, type.toString(), PrimitiveType.ARRAY.toString());
		ArrayType at = as(type, ArrayType.class);
		if (loopVariable != null) {
			if (at != null)
				parser.storeTypeInformation(new AccessVar(loopVariable), at.generalElementType());
			loopVariable.setUsed(true);
		}
		
		TypeInfoList bodyTyping = parser.typeInfoList();
		parser.reportErrorsOf(body, true, bodyTyping);
		parser.injectTypeInfos(bodyTyping);
	}

}