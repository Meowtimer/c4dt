package net.arctics.clonk.ui.search;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Directive;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4Scenario;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprAccessField;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprCallFunc;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprID;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprString;
import net.arctics.clonk.parser.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.C4ScriptExprTree.Statement;
import net.arctics.clonk.parser.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.resource.ClonkProjectNature;

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

	private C4Field field;
	private Object[] scope;
	private C4ScriptBase declaringScript;
	private boolean declaringScriptIsScenario;
	
	private ClonkSearchResult result;
	
	public ClonkSearchQuery(C4Field field, ClonkProjectNature project) {
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
					if (callFunc.getFieldName().equals("GameCall")) {
						if (callFunc.getParams().length > 0 && callFunc.getParams()[0] instanceof ExprString) {
							if (((ExprString)callFunc.getParams()[0]).stringValue().equals(field.getName()))
								return true;
						}
					}
				}
				return false;
			}
			public TraversalContinuation expressionDetected(ExprElm expression,
					C4ScriptParser parser) {
				if (expression instanceof ExprAccessField) {
					ExprAccessField accessField = (ExprAccessField) expression;
					if (accessField.getField(parser) == field)
						result.addMatch(expression, parser);
					else if (declaringScriptIsScenario && calledThroughGameCall(expression))
						result.addMatch(expression, parser);
				}
				else if (expression instanceof ExprID && field instanceof C4ScriptBase) {
					if (expression.guessObjectType(parser) == field)
						result.addMatch(expression, parser);
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
						try {
							searchObject(searchExpressions, resource, script);
						} catch (CompilerException e) {
							e.printStackTrace();
						}
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
					try {
						searchObject(searchExpressions, (IResource) script.getScriptFile(), script);
					} catch (CompilerException e) {
						e.printStackTrace();
					}
				}
				else if (scope instanceof C4Function) {
					C4Function func = (C4Function) scope;
					C4ScriptBase script = func.getScript();
					try {
						C4ScriptParser parser = new C4ScriptParser((IFile) script.getScriptFile(), script);
						parser.setExpressionListener(searchExpressions);
						parser.parseCodeOfFunction(func);
					} catch (CompilerException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return new Status(IStatus.OK, ClonkCore.PLUGIN_ID, 0, "Okeydokey", null);
	}
	
	private void searchObject(
			final IExpressionListener searchExpressions,
			IResource resource, C4ScriptBase script)
			throws CompilerException {
		C4ScriptParser parser = new C4ScriptParser((IFile) resource, script);
		if (field instanceof C4Object) {
			C4Directive include = script.getIncludeDirectiveFor((C4Object) field);
			if (include != null)
				result.addMatch(include.getExprElm(), parser);
		}
		parser.setExpressionListener(searchExpressions);
		parser.parseCodeOfFunctions();
	}

}
