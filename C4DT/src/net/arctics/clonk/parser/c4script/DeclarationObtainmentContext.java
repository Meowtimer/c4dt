package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public interface DeclarationObtainmentContext extends IEvaluationContext {
	Script container();
	Function getCurrentFunc();
	IType queryTypeOfExpression(ExprElm exprElm, IType defaultType);
	Definition getContainerAsDefinition();
	void parseCodeOfFunction(Function function, boolean withNewContext) throws ParsingException;
	void storeTypeInformation(ExprElm exprElm, IType type);
	Declaration getCurrentDeclaration();
	SourceLocation absoluteSourceLocationFromExpr(ExprElm expression);
}