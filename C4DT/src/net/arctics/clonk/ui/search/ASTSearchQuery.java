package net.arctics.clonk.ui.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodeMatcher;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Standalone;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.Statement.Attachment;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.TaskExecution;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;

public class ASTSearchQuery extends SearchQuery {

	public static class Match extends SearchMatch {
		private final Map<String, Object> subst;
		public Map<String, Object> subst() { return subst; }
		public Match(String line, int lineOffset, Object element, ASTNode matched, Map<String, Object> subst) {
			super(line, lineOffset, element, matched, false, false);
			this.subst = subst;
		}
	}

	private void addMatch(ASTNode match, Script script, int s, int l, Map<String, Object> subst) {
		final Match m = match(match, script, subst);
		result.addMatch(m);
	}

	protected Match match(ASTNode match, Script script, Map<String, Object> subst) {
		final BufferedScanner scanner = scanner(script);
		final IRegion lineRegion = scanner.regionOfLineContainingRegion(match.absolute());
		final String line = scanner.bufferSubstringAtRegion(lineRegion);
		final Match m = new Match(line, lineRegion.getOffset(), script, match, subst);
		return m;
	}

	protected BufferedScanner scanner(Script script) {
		synchronized (scanners) {
			BufferedScanner scanner = scanners.get(script);
			if (scanner == null)
				scanner = new BufferedScanner(script.file());
			scanners.put(script, scanner);
			return scanner;
		}
	}

	private final String templateText;
	private final ASTNode template;
	private final ASTNode replacement;
	private final Collection<Script> scope;
	private final Map<Script, BufferedScanner> scanners = new HashMap<>();

	public ASTNode replacement() { return replacement; }
	public ASTNode template() { return template; }

	public ASTSearchQuery(String templateExpressionText, String replacementExpressionText, Collection<Script> scope) throws ProblemException {
		final Standalone stal = new Standalone(scope);
		this.templateText = templateExpressionText;
		this.template = ASTNodeMatcher.prepareForMatching(stal.parse(templateExpressionText));
		this.replacement = replacementExpressionText != null ? ASTNodeMatcher.prepareForMatching(stal.parse(replacementExpressionText)) : null;
		this.scope = scope;
	}

	@Override
	protected IStatus doRun(final IProgressMonitor monitor) throws OperationCanceledException {
		TaskExecution.threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				class ScriptSearcher implements Runnable, IASTVisitor<Script> {
					private final Script script;
					private final Map<String, Match> matches = new HashMap<String, Match>();
					public ScriptSearcher(Script script) {
						script.requireLoaded();
						this.script = script;
					}
					@Override
					public void run() {
						if (monitor.isCanceled())
							return;
						try {
							script.traverse(this, script);
							commitMatches();
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
					private void commitMatches() {
						for (final Match m : matches.values())
							result.addMatch(m);
						matches.clear();
					}
					@Override
					public TraversalContinuation visitNode(ASTNode expression, Script script) {
						if (monitor.isCanceled())
							return TraversalContinuation.Cancel;
						final Map<String, Object> subst = template.match(expression);
						if (subst != null) {
							final IRegion r = expression.absolute();
							addMatch(expression, script, r.getOffset(), r.getLength(), subst);
							return TraversalContinuation.SkipSubElements;
						} else {
							if (expression instanceof Statement) {
								final Statement stmt = (Statement) expression;
								if (stmt.attachments() != null)
									for (final Attachment a : stmt.attachments())
										if (a instanceof ASTNode)
											visitNode((ASTNode)a, script);
							}
							return TraversalContinuation.Continue;
						}
					}
				}
				for (final Script s : scope)
					if (s.file() != null)
						pool.execute(new ScriptSearcher(s));
			}
		}, 20, scope.size());
		return new Status(IStatus.OK, Core.PLUGIN_ID, 0, "C4Script Search Success", null);
	}

	@Override
	public String getLabel() { return String.format("Search for '%s'", templateText); }

}
