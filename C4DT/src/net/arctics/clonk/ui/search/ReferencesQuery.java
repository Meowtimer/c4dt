package net.arctics.clonk.ui.search;

import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.TraversalContinuation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.FunctionEntry;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.KeyValuePair;
import net.arctics.clonk.util.Sink;
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

public class ReferencesQuery extends SearchQueryBase {

	protected Declaration declaration;
	private final Object[] scope;
	protected ProblemReportingStrategy strategy;

	public ReferencesQuery(Declaration declaration, ClonkProjectNature project) {
		super();
		this.declaration = declaration.latestVersion();
		this.scope = declaration.occurenceScope(project);
	}

	@Override
	public String getLabel() {
		return String.format(Messages.ClonkSearchQuery_SearchFor, declaration.toString()); 
	}
	
	private class Visitor implements IResourceVisitor, IASTVisitor<ProblemReportingContext> {
		private boolean potentiallyReferencedByObjectCall(ASTNode expression) {
			if (expression instanceof CallDeclaration && expression.predecessorInSequence() instanceof MemberOperator) {
				CallDeclaration callFunc = (CallDeclaration) expression;
				return callFunc.declarationName().equals(declaration.name());
			}
			return false;
		}
		@Override
		public TraversalContinuation visitNode(ASTNode node, ProblemReportingContext context) {
			if (node instanceof AccessDeclaration) {
				AccessDeclaration accessDeclExpr = (AccessDeclaration) node;
				Declaration dec = accessDeclExpr.declaration();
				if (dec != null && dec.latestVersion() == declaration)
					result.addMatch(node, context, false, accessDeclExpr.indirectAccess());
				else if (potentiallyReferencedByObjectCall(node)) {
					Function otherFunc = (Function) accessDeclExpr.declaration();
					boolean potential = (otherFunc == null || !((Function)declaration).isRelatedFunction(otherFunc));
					result.addMatch(node, context, potential, accessDeclExpr.indirectAccess());
				}
			}
			else if (node instanceof IDLiteral && declaration instanceof Script) {
				if (((IDLiteral)node).definition(context) == declaration)
					result.addMatch(node, context, false, false);
			}
			else if (node instanceof StringLiteral) {
				EntityRegion decRegion = node.entityAt(0, context);
				if (decRegion != null && decRegion.entityAs(Declaration.class) == declaration)
					result.addMatch(node, context, true, true);
			}
			return TraversalContinuation.Continue;
		}
		@Override
		public boolean visit(IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				Script script = Script.get(resource, true);
				if (script != null)
					searchScript(resource, script);
			}
			return true;
		}
		
		public void searchScript(IResource resource, Script script) {
			C4ScriptParser parser = new C4ScriptParser(script);
			ProblemReportingContext ctx = strategy.localTypingContext(parser);
			searchScript(resource, ctx);
		}
		
		public void searchScript(IResource resource, ProblemReportingContext context) {
			Script script = context.script();
			if (script.scriptFile() != null) {
				if (declaration instanceof Definition) {
					Directive include = script.directiveIncludingDefinition((Definition) declaration);
					if (include != null)
						result.addMatch(include, context, false, false);
				}
				for (Function f : script.functions())
					f.traverse(this, context);
			}

			// also search related files (actmap, defcore etc)
			try {
				searchScriptRelatedFiles(script);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		getSearchResult(); // make sure we have one
		final Visitor visitor = new Visitor();
		this.strategy = ((ProjectIndex)this.declaration.index()).nature().settings()
			.instantiateProblemReportingStrategies(Capabilities.TYPING).get(0);
		Utilities.threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				for (Object scope : ReferencesQuery.this.scope)
					if (scope instanceof IContainer) try {
						((IContainer)scope).accept(visitor);
					} catch (Exception e) {
						e.printStackTrace();
					}
					else if (scope instanceof Script) {
						final Script script = (Script) scope;
						pool.execute(new Runnable() {
							@Override
							public void run() {
								try {
									C4ScriptParser parser = new C4ScriptParser(script);
									ProblemReportingContext ctx = strategy.localTypingContext(parser);
									visitor.searchScript((IResource) script.scriptStorage(), ctx);
								} catch (Exception e) {}
							}
						});
					}
					else if (scope instanceof Function) {
						Function func = (Function)scope;
						C4ScriptParser parser = new C4ScriptParser(func.script());
						parser.setCurrentFunction(func);
						func.traverse(visitor, strategy.localTypingContext(parser));
					}
			}
		}, 20);
		return new Status(IStatus.OK, Core.PLUGIN_ID, 0, Messages.ClonkSearchQuery_Success, null);
	}

	private void searchScriptRelatedFiles(Script script) throws CoreException {
		if (script instanceof Definition) {
			IContainer objectFolder = ((Definition)script).definitionFolder();
			for (IResource res : objectFolder.members())
				if (res instanceof IFile) {
					IFile file = (IFile)res;
					Structure pinned = Structure.pinned(file, true, false);
					if (pinned instanceof IniUnit) {
						IniUnit iniUnit = (IniUnit) pinned;
						for (IniSection sec : iniUnit)
							for (IniItem entry : sec)
								if (entry instanceof ComplexIniEntry) {
									ComplexIniEntry complex = (ComplexIniEntry) entry;
									if (complex.definition() != null) {
										Class<?> entryClass = complex.definition().entryClass();
										if (entryClass == FunctionEntry.class) {
											Definition obj = Definition.definitionCorrespondingToFolder(objectFolder);
											if (obj != null) {
												Declaration declaration = obj.findFunction(complex.stringValue());
												if (declaration == this.declaration)
													result.addMatch(new ClonkSearchMatch(complex.toString(), 0, iniUnit, complex.end()-complex.stringValue().length(), complex.stringValue().length(), false, false));
											}
										}
										else if (declaration instanceof Definition)
											if (entryClass == ID.class) {
												if (script.index().anyDefinitionWithID((ID) complex.extendedValue()) == declaration)
													result.addMatch(new ClonkSearchMatch(complex.toString(), 0, iniUnit, complex.end()-complex.stringValue().length(), complex.stringValue().length(), false, false));
											}
											else if (entryClass == IDArray.class)
												for (KeyValuePair<ID, Integer> pair : ((IDArray)complex.extendedValue()).components()) {
													Definition obj = script.index().anyDefinitionWithID(pair.key());
													if (obj == declaration)
														result.addMatch(new ClonkSearchMatch(pair.toString(), 0, iniUnit, complex.end()-complex.stringValue().length(), complex.stringValue().length(), false, false));
												}
									}
								}
					}
				}
		}
	}
	
	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			Script script = Utilities.scriptForEditor(editor);
			if (script != null)
				return result.getMatches(script);
		}
		return NO_MATCHES;
	}

	@Override
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			Script script = Utilities.scriptForEditor(editor);
			if (script != null && match.getElement().equals(script.scriptStorage()))
				return true;
		}
		return false;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		Script script = Script.get(file, true);
		if (script != null)
			return result.getMatches(script);
		return NO_MATCHES;
	}

}
