package net.arctics.clonk.ui.search;

import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.VisitCodeFlavour;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IASTVisitor;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;
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

	public ReferencesQuery(Declaration declaration, ClonkProjectNature project) {
		super();
		this.declaration = declaration.latestVersion();
		this.scope = declaration.occurenceScope(project);
	}

	@Override
	public String getLabel() {
		return String.format(Messages.ClonkSearchQuery_SearchFor, declaration.toString()); 
	}
	
	private class Visitor implements IResourceVisitor, IASTVisitor {

		private boolean potentiallyReferencedByObjectCall(ExprElm expression) {
			if (expression instanceof CallDeclaration && expression.predecessorInSequence() instanceof MemberOperator) {
				CallDeclaration callFunc = (CallDeclaration) expression;
				return callFunc.declarationName().equals(declaration.name());
			}
			return false;
		}
		
		@Override
		public TraversalContinuation visitExpression(ExprElm expression, C4ScriptParser parser) {
			if (expression instanceof AccessDeclaration) {
				AccessDeclaration accessDeclExpr = (AccessDeclaration) expression;
				Declaration dec = accessDeclExpr.declarationFromContext(parser);
				if (dec != null && dec.latestVersion() == declaration)
					result.addMatch(expression, parser, false, accessDeclExpr.indirectAccess());
				else if (potentiallyReferencedByObjectCall(expression)) {
					Function otherFunc = (Function) accessDeclExpr.declaration();
					boolean potential = (otherFunc == null || !((Function)declaration).isRelatedFunction(otherFunc));
					result.addMatch(expression, parser, potential, accessDeclExpr.indirectAccess());
				}
			}
			else if (expression instanceof IDLiteral && declaration instanceof Script) {
				if (expression.guessObjectType(parser) == declaration)
					result.addMatch(expression, parser, false, false);
			}
			else if (expression instanceof StringLiteral) {
				EntityRegion decRegion = expression.declarationAt(0, parser);
				if (decRegion != null && decRegion.entityAs(Declaration.class) == declaration)
					result.addMatch(expression, parser, true, true);
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
			if (script.scriptFile() != null) {
				C4ScriptParser parser = new C4ScriptParser(script);
				if (declaration instanceof Definition) {
					Directive include = script.directiveIncludingDefinition((Definition) declaration);
					if (include != null)
						result.addMatch(include.asExpression(), parser, false, false);
				}
				for (Function f : script.functions()) {
					parser.setCurrentFunction(f);
					parser.visitCode(f, this, VisitCodeFlavour.AlsoStatements, false);
				}
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
								visitor.searchScript((IResource) script.scriptStorage(), script);
							}
						});
					}
					else if (scope instanceof Function) {
						Function func = (Function)scope;
						C4ScriptParser parser = new C4ScriptParser(func.script());
						parser.setCurrentFunction(func);
						parser.visitCode(func, visitor, VisitCodeFlavour.AlsoStatements, false);
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
									if (complex.entryConfig() != null) {
										Class<?> entryClass = complex.entryConfig().entryClass();
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
