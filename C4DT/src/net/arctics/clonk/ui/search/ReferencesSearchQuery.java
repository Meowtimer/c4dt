package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.as;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Definition.ProxyVar;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.TaskExecution;
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
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class ReferencesSearchQuery extends SearchQuery {

	protected Declaration declaration;
	private final Object[] scope;

	public ReferencesSearchQuery(final ClonkProjectNature start, final Declaration declaration) {
		super();
		this.declaration = declaration.latestVersion();
		this.scope = declaration.occurenceScope(map(start.projectSet(), ClonkProjectNature.SELECT_INDEX));
	}

	@Override
	public String getLabel() {
		return String.format(Messages.ClonkSearchQuery_SearchFor, declaration.toString());
	}

	private class Visitor extends ExpressionLocator<Structure> implements IResourceVisitor {
		private boolean potentiallyReferencedByObjectCall(final ASTNode expression) {
			if (expression instanceof CallDeclaration && expression.predecessor() instanceof MemberOperator) {
				final CallDeclaration callFunc = (CallDeclaration) expression;
				return callFunc.declaration() == null && callFunc.name().equals(declaration.name());
			}
			return false;
		}
		private boolean related(final Function fn) {
			final Set<Function> catcher = new HashSet<>();
			for (Function inh = fn; inh != null && catcher.add(inh); inh = inh.inheritedFunction())
				if (inh == declaration)
					return true;
			return false;
		}
		@Override
		public TraversalContinuation visitNode(final ASTNode node, final Structure context) {
			if (node instanceof Function) {
				final Function fn = (Function) node;
				if (related(fn))
					result.addIdentifierMatch(context, fn, false, true);
			}
			if (node instanceof AccessDeclaration) {
				final AccessDeclaration accessDeclExpr = (AccessDeclaration) node;
				Declaration dec = accessDeclExpr.declaration();
				if (dec != null)
					dec = dec.latestVersion();
				if (dec == declaration || (dec instanceof ProxyVar && ((ProxyVar)dec).definition() == declaration))
					result.addIdentifierMatch(context, node, false, accessDeclExpr.indirectAccess());
				else if (
					dec instanceof Function && declaration instanceof Function &&
					related((Function)dec)
				)
					result.addIdentifierMatch(context, node, false, true);
				else if (potentiallyReferencedByObjectCall(node)) {
					final Function otherFunc = (Function) accessDeclExpr.declaration();
					final boolean potential = (otherFunc == null || !((Function)declaration).isRelatedFunction(otherFunc));
					result.addIdentifierMatch(context, node, potential, accessDeclExpr.indirectAccess());
				}
			}
			else if (node instanceof IDLiteral && declaration instanceof Definition) {
				if (context.index().definitionNearestTo(context.file(), ((IDLiteral)node).idValue()) == declaration)
					result.addIdentifierMatch(context, node, false, false);
			}
			else if (node instanceof StringLiteral) {
				final EntityRegion decRegion = node.entityAt(0, this);
				if (decRegion != null && decRegion.entityAs(Declaration.class) == declaration)
					result.addIdentifierMatch(context, node, true, true);
			}
			return TraversalContinuation.Continue;
		}
		@Override
		public boolean visit(final IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				final Script script = Script.get(resource, true);
				if (script != null)
					searchScript(resource, script);
			}
			return true;
		}
		public void searchScript(final IResource resource, final Script script) {
			script.requireLoaded();
			if (script.file() != null) {
				if (declaration instanceof Definition) {
					final Directive include = script.directiveIncludingDefinition((Definition) declaration);
					if (include != null)
						result.addIdentifierMatch(script, include, false, false);
				}
				for (final Function f : script.functions())
					f.traverse(this, script);
			}

			// also search related files (actmap, defcore etc)
			try {
				searchScriptRelatedFiles(script);
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}
		private void searchScriptRelatedFiles(final Script script) throws CoreException {
			if (script instanceof Definition) {
				final IContainer objectFolder = ((Definition)script).definitionFolder();
				for (final IResource res : objectFolder.members())
					if (res instanceof IFile) {
						final IFile file = (IFile)res;
						final IniUnit unit = as(Structure.pinned(file, true, false), IniUnit.class);
						if (unit != null)
							unit.traverse(this, unit);
					}
			}
		}
	}

	@Override
	protected IStatus doRun(final IProgressMonitor monitor) throws OperationCanceledException {
		getSearchResult(); // make sure we have one
		final Visitor visitor = new Visitor();
		TaskExecution.threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(final ExecutorService pool) {
				for (final Object scope : ReferencesSearchQuery.this.scope)
					if (scope instanceof IContainer) try {
						((IContainer)scope).accept(visitor);
					} catch (final Exception e) {
						e.printStackTrace();
					}
					else if (scope instanceof Script) {
						final Script script = (Script) scope;
						pool.execute(new Runnable() {
							@Override
							public void run() {
								try {
									visitor.searchScript((IResource) script.source(), script);
								} catch (final Exception e) {}
							}
						});
					}
					else if (scope instanceof Function) {
						final Function func = (Function)scope;
						func.traverse(visitor, func.script());
					}
			}
		}, 20, scope.length);
		return new Status(IStatus.OK, Core.PLUGIN_ID, 0, Messages.ClonkSearchQuery_Success, null);
	}

	@Override
	public Match[] computeContainedMatches(final AbstractTextSearchResult result, final IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			final Script script = Utilities.scriptForEditor(editor);
			if (script != null)
				return result.getMatches(script);
		}
		return NO_MATCHES;
	}

	@Override
	public boolean isShownInEditor(final Match match, final IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			final Script script = Utilities.scriptForEditor(editor);
			if (script != null && match.getElement().equals(script.source()))
				return true;
		}
		return false;
	}

	@Override
	public Match[] computeContainedMatches(final AbstractTextSearchResult result, final IFile file) {
		final Script script = Script.get(file, true);
		if (script != null)
			return result.getMatches(script);
		return NO_MATCHES;
	}

}
