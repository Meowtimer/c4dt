package net.arctics.clonk.ui.search;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Directive;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprAccessDeclaration;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprCallFunc;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprID;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprObjectCall;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprString;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.Statement;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ParsingException;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

public class ClonkSearchQuery implements ISearchQuery {

	private C4Declaration field;
	private Object[] scope;
	private C4ScriptBase declaringScript;
	private boolean declaringScriptIsScenario;
	
	private ClonkSearchResult result;
	
	public ClonkSearchQuery(C4Declaration field, ClonkProjectNature project) {
		super();
		this.field = field;
		this.declaringScript = field.getScript();
		this.declaringScriptIsScenario = declaringScript instanceof C4Scenario;
		this.scope = field.occurenceScope(project);
	}

	public boolean canRerun() {
		return true;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public String getLabel() {
		return String.format("Search for '%s'", field.toString()); 
	}

	public ISearchResult getSearchResult() {
		if (result == null) {
			result = new ClonkSearchResult(this);
		}
		return result;
	}

	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		getSearchResult(); // make sure we have one
		final IExpressionListener searchExpression = new IExpressionListener() {
			private boolean calledThroughGameCall(ExprElm expression) {
				if (expression instanceof ExprCallFunc) {
					ExprCallFunc callFunc = (ExprCallFunc) expression;
					if (callFunc.getDeclarationName().equals("GameCall")) {
						if (callFunc.getParams().length > 0 && callFunc.getParams()[0] instanceof ExprString) {
							if (((ExprString)callFunc.getParams()[0]).stringValue().equals(field.getName()))
								return true;
						}
					}
				}
				return false;
			}
			private boolean potentiallyReferencedByObjectCall(ExprElm expression) {
				if (expression instanceof ExprCallFunc && expression.getPredecessorInSequence() instanceof ExprObjectCall) {
					ExprCallFunc callFunc = (ExprCallFunc) expression;
					return callFunc.getDeclarationName().equals(field.getName());
				}
				return false;
			}
			public TraversalContinuation expressionDetected(ExprElm expression,
					C4ScriptParser parser) {
				if (expression instanceof ExprAccessDeclaration) {
					ExprAccessDeclaration accessField = (ExprAccessDeclaration) expression;
					if (accessField.getDeclaration(parser) == field)
						result.addMatch(expression, parser, false, accessField.indirectAccess());
					else if (declaringScriptIsScenario && calledThroughGameCall(expression))
						result.addMatch(expression, parser, false, true);
					else if (potentiallyReferencedByObjectCall(expression)) {
						C4Function otherFunc = (C4Function) accessField.getDeclaration();
						boolean potential = (otherFunc == null || !((C4Function)field).isRelatedFunction(otherFunc));
						result.addMatch(expression, parser, potential, accessField.indirectAccess());
					}
				}
				else if (expression instanceof ExprID && field instanceof C4ScriptBase) {
					if (expression.guessObjectType(parser) == field)
						result.addMatch(expression, parser, false, false);
				}
				return TraversalContinuation.Continue;
			}
		};
		final IExpressionListener searchExpressions = new IExpressionListener() {
			public TraversalContinuation expressionDetected(ExprElm expression,
					C4ScriptParser parser) {
				if (expression instanceof Statement)
					expression.traverse(searchExpression, parser);
				return TraversalContinuation.Continue;
			}
		};
		final IResourceVisitor resourceVisitor = new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile) {
					C4ScriptBase script = Utilities.getScriptForFile((IFile) resource);
					if (script != null) {
						searchScript(searchExpressions, resource, script);
					}
				}
				return true;
			}
		}; 
		try {
			for (Object scope : this.scope) {
				if (scope instanceof IContainer) {
					((IContainer)scope).accept(resourceVisitor);
				}
				else if (scope instanceof C4ScriptBase) {
					C4ScriptBase script = (C4ScriptBase) scope;
					searchScript(searchExpressions, (IResource) script.getScriptFile(), script);
				}
				else if (scope instanceof C4Function) {
					C4Function func = (C4Function) scope;
					C4ScriptBase script = func.getScript();
					C4ScriptParser parser = new C4ScriptParser((IFile) script.getScriptFile(), script);
					parser.setExpressionListener(searchExpressions);
					try {
						parser.parseCodeOfFunction(func);
					} catch (ParsingException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return new Status(IStatus.OK, ClonkCore.PLUGIN_ID, 0, "Okeydokey", null);
	}
	
	private void searchScript(final IExpressionListener searchExpressions, IResource resource, C4ScriptBase script) {
		C4ScriptParser parser = new C4ScriptParser((IFile) resource, script);
		if (field instanceof C4Object) {
			C4Directive include = script.getIncludeDirectiveFor((C4Object) field);
			if (include != null)
				result.addMatch(include.getExprElm(), parser, false, false);
		}
		parser.setExpressionListener(searchExpressions);
		try {
			parser.parseCodeOfFunctions();
		} catch (ParsingException e) {
			e.printStackTrace();
		}
	}

	public C4ScriptBase getDeclaringScript() {
		return declaringScript;
	}

}
