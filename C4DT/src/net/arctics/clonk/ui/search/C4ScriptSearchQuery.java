package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.Utilities.threadPool;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IASTVisitor;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;
import net.arctics.clonk.util.Sink;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;

public class C4ScriptSearchQuery extends SearchQueryBase {

	private final String templateText;
	private final ExprElm template;
	private final ExprElm replacement;
	private final Iterable<Script> scope;
	
	public C4ScriptSearchQuery(String templateExpressionText, String replacementExpressionText, Iterable<Script> scope) {
		this.templateText = templateExpressionText;
		this.template = C4ScriptParser.parse(templateExpressionText).matchingExpr();
		this.replacement = replacementExpressionText != null ? C4ScriptParser.parse(replacementExpressionText) : null;
		this.scope = scope;
	}
	
	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService item) {
				class ScriptSearcher implements Runnable, IASTVisitor {
					private final C4ScriptParser parser;
					public ScriptSearcher(Script script) {
						C4ScriptParser p = null;
						try {
							p = new C4ScriptParser(script);
						} catch (Exception e) {
							System.out.println(String.format("Creating parser failed for '%s'", script));
						}
						parser = p;
					}
					@Override
					public void run() {
						if (parser == null)
							return;
						for (Function f : parser.script().functions())
							f.code().traverse(this, parser);
						for (Variable v : parser.script().variables())
							if (v.initializationExpression() != null)
								v.initializationExpression().traverse(this, parser);
					}
					@Override
					public TraversalContinuation visitExpression(ExprElm expression, C4ScriptParser parser) {
						Map<String, Object> subst = template.match(expression);
						if (subst != null) {
							IRegion r = expression.absolute();
							result.addMatch(expression, parser, false, false, r.getOffset(), r.getLength());
							return TraversalContinuation.SkipSubElements;
						} else
							return TraversalContinuation.Continue;
					}
				}
				for (final Script s : scope)
					item.execute(new ScriptSearcher(s));
			}
		}, 20);
		return new Status(IStatus.OK, Core.PLUGIN_ID, 0, "C4Script Search Success", null);
	}

	@Override
	public String getLabel() {
		return String.format("Search for '%s'", templateText); 
	}

}
