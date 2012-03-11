package net.arctics.clonk.ui.search;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.ScriptParserListener;
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
	
	private class UltimateListener extends ScriptParserListener implements IResourceVisitor {

		private StringLiteral functionNameExpr;

		private boolean potentiallyReferencedByObjectCall(ExprElm expression) {
			if (expression instanceof CallFunc && expression.predecessorInSequence() instanceof MemberOperator) {
				CallFunc callFunc = (CallFunc) expression;
				return callFunc.declarationName().equals(declaration.name());
			}
			return false;
		}

		private boolean potentiallyReferencedByCallFunction(AccessDeclaration expression, C4ScriptParser parser) {
			functionNameExpr = null;
			if (expression instanceof CallFunc) {
				CallFunc callFunc = (CallFunc) expression;
				for (ExprElm e : callFunc.params()) {
					// ask the string literals whether they might refer to a function
					if (e instanceof StringLiteral) {
						functionNameExpr = (StringLiteral) e;
						EntityRegion decRegion = e.declarationAt(0, parser);
						if (decRegion != null)
							return decRegion.entityAs(Declaration.class) == declaration;
						break;
					}
				}
			}
			return false;
		}
		
		@Override
		public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
			if (expression instanceof AccessDeclaration) {
				AccessDeclaration accessDeclExpr = (AccessDeclaration) expression;
				Declaration dec = accessDeclExpr.declarationFromContext(parser);
				if (dec != null && dec.latestVersion() == declaration)
					result.addMatch(expression, parser, false, accessDeclExpr.indirectAccess());
				else if (Utilities.isAnyOf(accessDeclExpr.declaration(), expression.cachedFuncs(parser).CallFunctions) && potentiallyReferencedByCallFunction(accessDeclExpr, parser)) {
					result.addMatch(functionNameExpr, parser, true, true);
				}
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
			return TraversalContinuation.Continue;
		}
		
		@Override
		public int minimumParsingRecursion() {
			return 0;
		}
		
		@Override
		public boolean visit(IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				Script script = Script.get(resource, true);
				if (script != null) {
					searchScript(resource, script);
				}
			}
			return true;
		}
		
		public void searchScript(IResource resource, Script script) {
			C4ScriptParser parser = new C4ScriptParser(script);
			if (declaration instanceof Definition) {
				Directive include = script.directiveIncludingDefinition((Definition) declaration);
				if (include != null)
					result.addMatch(include.asExpression(), parser, false, false);
			}
			for (Function f : script.functions()) {
				parser.setCurrentFunc(f);
				parser.reportExpressionsAndStatements(f, this, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
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
		UltimateListener listener = new UltimateListener();
		try {
			for (Object scope : this.scope) {
				if (scope instanceof IContainer) {
					((IContainer)scope).accept(listener);
				}
				else if (scope instanceof Script) {
					Script script = (Script) scope;
					listener.searchScript((IResource) script.scriptStorage(), script);
				}
				else if (scope instanceof Function) {
					Function func = (Function)scope;
					C4ScriptParser parser = new C4ScriptParser(func.script());
					parser.setCurrentFunc(func);
					parser.reportExpressionsAndStatements(func, listener, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return new Status(IStatus.OK, Core.PLUGIN_ID, 0, Messages.ClonkSearchQuery_Success, null);
	}

	private void searchScriptRelatedFiles(Script script) throws CoreException {
		if (script instanceof Definition) {
			IContainer objectFolder = ((Definition)script).scriptStorage().getParent();
			for (IResource res : objectFolder.members()) {
				if (res instanceof IFile) {
					IFile file = (IFile)res;
					Structure pinned = Structure.pinned(file, true, false);
					if (pinned instanceof IniUnit) {
						IniUnit iniUnit = (IniUnit) pinned;
						for (IniSection sec : iniUnit) {
							for (IniItem entry : sec) {
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
										else if (declaration instanceof Definition) {
											if (entryClass == ID.class) {
												if (script.index().anyDefinitionWithID((ID) complex.extendedValue()) == declaration) {
													result.addMatch(new ClonkSearchMatch(complex.toString(), 0, iniUnit, complex.end()-complex.stringValue().length(), complex.stringValue().length(), false, false));
												}
											}
											else if (entryClass == IDArray.class) {
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
