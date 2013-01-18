package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.IFunctionCall;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public interface DeclarationObtainmentContext extends IEvaluationContext {
	@Override
	Script script();
	Function currentFunction();
	void setCurrentFunction(Function function);
	IType queryTypeOfExpression(ExprElm exprElm, IType defaultType);
	Definition definition();
	void reportProblems(Function function);
	void storeType(ExprElm exprElm, IType type);
	Declaration currentDeclaration();
	SourceLocation absoluteSourceLocationFromExpr(ExprElm expression);
	CachedEngineDeclarations cachedEngineDeclarations();
	void pushCurrentFunctionCall(IFunctionCall call);
	void popCurrentFunctionCall();
	IFunctionCall currentFunctionCall();
}