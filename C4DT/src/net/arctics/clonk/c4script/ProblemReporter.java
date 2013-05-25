package net.arctics.clonk.c4script;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.c4script.typing.ITypingContext;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Markers;

/**
 * A thing reporting problems it finds. It is responsible for a single script.
 * @author madeen
 */
public interface ProblemReporter extends IASTPositionProvider, ITypingContext {
	/**
	 * Return the {@link #script()} cast to {@link Definition} or null.
	 * @return Script as definition or null.
	 */
	Definition definition();
	/**
	 * Return an absolute source location for some expression found in the script the context is responsible for.
	 * @param expression The expression to return the absolute location for
	 * @return The absolute location.
	 */
	SourceLocation absoluteSourceLocationFromExpr(ASTNode expression);
	/**
	 * Return some object holding cached references to engine declarations, namely a {@link CachedEngineDeclarations} object.
	 * @return The cached engine declarations.
	 */
	CachedEngineDeclarations cachedEngineDeclarations();
	/**
	 * Return {@link Markers} this reporter adds markers to when it finds problems to report. 
	 * @return The {@link Markers} object
	 */
	Markers markers();
	/**
	 * Set {@link #markers()}.
	 * @param markers New value
	 */
	void setGlobalMarkers(Markers markers);
	/**
	 * Return the {@link Script} the reporter is responsible for.
	 * @return
	 */
	Script script();
	/**
	 * Run on the whole {@link #script()}.
	 */
	void run();
	/**
	 * Visit a {@link Function} reporting problems found in its code. Other functions might be visited as well, depending on implementation logic. 
	 * @param function The function which marks the starting point of the visitation journey
	 * @return Whatever result the implementation likes to return as a result of a visit.
	 */
	Object visit(Function function);
	/**
	 * Set an {@link IASTVisitor} as the observer of this reporter. It gets notified whenever the context visits a single syntax element.
	 * @param observer The observer to set
	 */
	public void setObserver(IASTVisitor<ProblemReporter> observer);
}