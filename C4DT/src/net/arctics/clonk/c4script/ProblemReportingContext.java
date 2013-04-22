package net.arctics.clonk.c4script;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Markers;

public interface ProblemReportingContext extends IASTPositionProvider, ITypingContext {
	Definition definition();
	SourceLocation absoluteSourceLocationFromExpr(ASTNode expression);
	CachedEngineDeclarations cachedEngineDeclarations();
	Markers markers();
	void setMarkers(Markers markers);
	Script script();
	void reportProblems();
	Object visitFunction(Function function);
	boolean triggersRevisit(Function function, Function called);
	public void setObserver(IASTVisitor<ProblemReportingContext> observer);
}