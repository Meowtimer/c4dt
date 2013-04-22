package net.arctics.clonk.ui.search;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodeMatcher;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.C4ScriptParser;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.Statement.Attachment;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.TaskExecution;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class ASTSearchQuery extends SearchQuery {

	public static class Match extends SearchMatch {
		private final ASTNode matched;
		private final Map<String, Object> subst;
		public ASTNode matched() { return matched; }
		public Map<String, Object> subst() { return subst; }
		public Match(String line, int lineOffset, Object element, int offset, int length, ASTNode matched, Map<String, Object> subst) {
			super(line, lineOffset, element, offset, length, false, false);
			this.matched = matched;
			this.subst = subst;
		}
	}

	private void addMatch(ASTNode match, C4ScriptParser parser, int s, int l, Map<String, Object> subst) {
		final Match m = match(match, parser, s, l, subst);
		result.addMatch(m);
	}

	protected static Match match(ASTNode match, C4ScriptParser parser, int s, int l, Map<String, Object> subst) {
		final IRegion lineRegion = parser.regionOfLineContainingRegion(new Region(s, l));
		final String line = parser.bufferSubstringAtRegion(lineRegion);
		final Match m = new Match(line, lineRegion.getOffset(), parser.script(), s, l, match, subst);
		return m;
	}

	private final String templateText;
	private final ASTNode template;
	private final ASTNode replacement;
	private final Iterable<Script> scope;

	public ASTNode replacement() { return replacement; }
	public ASTNode template() { return template; }

	private Engine commonEngine(Iterable<Script> scripts) {
		Engine e = null;
		for (final Script s : scripts)
			if (e == null)
				e = s.engine();
			else if (e != s.engine())
				throw new IllegalArgumentException("Scripts from different engines");
		return e;
	}

	public ASTSearchQuery(String templateExpressionText, String replacementExpressionText, Iterable<Script> scope) throws ParsingException {
		this.templateText = templateExpressionText;
		final Engine engine = commonEngine(scope);
		this.template = ASTNodeMatcher.matchingExpr(templateExpressionText, engine);
		this.replacement = replacementExpressionText != null ? ASTNodeMatcher.matchingExpr(replacementExpressionText, engine) : null;
		this.scope = scope;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) throws OperationCanceledException {
		TaskExecution.threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService item) {
				class ScriptSearcher implements Runnable, IASTVisitor<C4ScriptParser> {
					private final C4ScriptParser parser;
					private final Map<String, Match> matches = new HashMap<String, Match>();
					public ScriptSearcher(Script script) {
						C4ScriptParser p = null;
						try {
							p = new C4ScriptParser(script);
						} catch (final Exception e) {
							System.out.println(String.format("Creating parser failed for '%s'", script));
						}
						parser = p;
					}
					@Override
					public void run() {
						if (parser == null)
							return;
						parser.script().traverse(this, parser);
						commitMatches();
					}
					private void commitMatches() {
						for (final Match m : matches.values())
							result.addMatch(m);
						matches.clear();
					}
					@Override
					public TraversalContinuation visitNode(ASTNode expression, C4ScriptParser parser) {
						final Map<String, Object> subst = template.match(expression);
						if (subst != null) {
							final IRegion r = expression.absolute();
							addMatch(expression, parser, r.getOffset(), r.getLength(), subst);
							return TraversalContinuation.SkipSubElements;
						} else {
							if (expression instanceof Statement) {
								final Statement stmt = (Statement) expression;
								if (stmt.attachments() != null)
									for (final Attachment a : stmt.attachments())
										if (a instanceof ASTNode)
											visitNode((ASTNode)a, parser);	
							}
							return TraversalContinuation.Continue;
						}
					}
				}
				for (final Script s : scope)
					if (s.scriptFile() != null)
						item.execute(new ScriptSearcher(s));
			}
		}, 20);
		return new Status(IStatus.OK, Core.PLUGIN_ID, 0, "C4Script Search Success", null);
	}

	@Override
	public String getLabel() { return String.format("Search for '%s'", templateText); }

}
