package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public interface DeclarationObtainmentContext extends IEvaluationContext {
	Script containingScript();
	Function currentFunction();
	IType queryTypeOfExpression(ExprElm exprElm, IType defaultType);
	Definition containerAsDefinition();
	void reportProblems(Function function);
	void storeType(ExprElm exprElm, IType type);
	Declaration currentDeclaration();
	SourceLocation absoluteSourceLocationFromExpr(ExprElm expression);
}