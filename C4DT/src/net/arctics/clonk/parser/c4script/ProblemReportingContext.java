package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.IASTPositionProvider;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public interface ProblemReportingContext extends IEvaluationContext, IASTPositionProvider, ITypingContext {
	Definition definition();
	SourceLocation absoluteSourceLocationFromExpr(ASTNode expression);
	CachedEngineDeclarations cachedEngineDeclarations();
	BufferedScanner scanner();
	Markers markers();
	void reportProblems();
	void reportProblemsOfFunction(Function function);
}