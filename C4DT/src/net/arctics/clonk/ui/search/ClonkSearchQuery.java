package net.arctics.clonk.ui.search;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.c4script.C4Directive;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.Function;
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
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

public class ClonkSearchQuery implements ISearchQuery {

	private C4Declaration declaration;
	private Object[] scope;
	private C4ScriptBase declaringScript;
	private ClonkSearchResult result;
	
	public ClonkSearchQuery(C4Declaration declaration, ClonkProjectNature project) {
		super();
		this.declaration = declaration;
		this.declaringScript = declaration.getScript();
		this.scope = declaration.occurenceScope(project);
	}

	public boolean canRerun() {
		return true;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public String getLabel() {
		return String.format(Messages.ClonkSearchQuery_SearchFor, declaration.toString()); 
	}

	public ISearchResult getSearchResult() {
		if (result == null) {
			result = new ClonkSearchResult(this);
		}
		return result;
	}
	
	private class UltimateListener extends ScriptParserListener implements IResourceVisitor {

		private StringLiteral functionNameExpr;

		private boolean potentiallyReferencedByObjectCall(ExprElm expression) {
			if (expression instanceof CallFunc && expression.getPredecessorInSequence() instanceof MemberOperator) {
				CallFunc callFunc = (CallFunc) expression;
				return callFunc.getDeclarationName().equals(declaration.getName());
			}
			return false;
		}

		private boolean potentiallyReferencedByCallFunction(AccessDeclaration expression, C4ScriptParser parser) {
			functionNameExpr = null;
			if (expression instanceof CallFunc) {
				CallFunc callFunc = (CallFunc) expression;
				for (ExprElm e : callFunc.getParams()) {
					// ask the string literals whether they might refer to a function
					if (e instanceof StringLiteral) {
						functionNameExpr = (StringLiteral) e;
						DeclarationRegion decRegion = e.declarationAt(0, parser);
						if (decRegion != null)
							return decRegion.getDeclaration() == declaration;
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
				if (accessDeclExpr.getDeclaration(parser) == declaration)
					result.addMatch(expression, parser, false, accessDeclExpr.indirectAccess());
				else if (Utilities.isAnyOf(accessDeclExpr.getDeclaration(), expression.getCachedFuncs(parser).CallFunctions) && potentiallyReferencedByCallFunction(accessDeclExpr, parser)) {
					result.addMatch(functionNameExpr, parser, true, true);
				}
				else if (potentiallyReferencedByObjectCall(expression)) {
					C4Function otherFunc = (C4Function) accessDeclExpr.getDeclaration();
					boolean potential = (otherFunc == null || !((C4Function)declaration).isRelatedFunction(otherFunc));
					result.addMatch(expression, parser, potential, accessDeclExpr.indirectAccess());
				}
			}
			else if (expression instanceof IDLiteral && declaration instanceof C4ScriptBase) {
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
				C4ScriptBase script = C4ScriptBase.get((IFile) resource, true);
				if (script != null) {
					searchScript(resource, script);
				}
			}
			return true;
		}
		
		public void searchScript(IResource resource, C4ScriptBase script) {
			C4ScriptParser parser = new C4ScriptParser(script);
			if (declaration instanceof C4Object) {
				C4Directive include = script.getIncludeDirectiveFor((C4Object) declaration);
				if (include != null)
					result.addMatch(include.getExprElm(), parser, false, false);
			}
			for (C4Function f : script.functions()) {
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

	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		getSearchResult(); // make sure we have one
		UltimateListener listener = new UltimateListener();
		try {
			for (Object scope : this.scope) {
				if (scope instanceof IContainer) {
					((IContainer)scope).accept(listener);
				}
				else if (scope instanceof C4ScriptBase) {
					C4ScriptBase script = (C4ScriptBase) scope;
					listener.searchScript((IResource) script.getScriptStorage(), script);
				}
				else if (scope instanceof C4Function) {
					C4Function func = (C4Function)scope;
					C4ScriptParser parser = new C4ScriptParser(func.getScript());
					parser.setCurrentFunc(func);
					parser.reportExpressionsAndStatements(func, listener, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return new Status(IStatus.OK, ClonkCore.PLUGIN_ID, 0, Messages.ClonkSearchQuery_Success, null);
	}

	private void searchScriptRelatedFiles(C4ScriptBase script) throws CoreException {
		if (script instanceof C4ObjectIntern) {
			IContainer objectFolder = ((C4ObjectIntern)script).getScriptStorage().getParent();
			for (IResource res : objectFolder.members()) {
				if (res instanceof IFile) {
					IFile file = (IFile)res;
					C4Structure pinned = C4Structure.pinned(file, true, false);
					if (pinned instanceof IniUnit) {
						IniUnit iniUnit = (IniUnit) pinned;
						for (IniSection sec : iniUnit) {
							for (IniItem entry : sec) {
								if (entry instanceof ComplexIniEntry) {
									ComplexIniEntry complex = (ComplexIniEntry) entry;
									if (complex.getEntryConfig() != null) {
										Class<?> entryClass = complex.getEntryConfig().getEntryClass();
										if (entryClass == Function.class) {
											C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(objectFolder);
											if (obj != null) {
												C4Declaration declaration = obj.findFunction(complex.getValue());
												if (declaration == this.declaration)
													result.addMatch(new ClonkSearchMatch(complex.toString(), 0, iniUnit, complex.getEndPos()-complex.getValue().length(), complex.getValue().length(), false, false));
											}
										}
										else if (declaration instanceof C4Object) {
											if (entryClass == C4ID.class) {
												if (script.getIndex().getObjectFromEverywhere((C4ID) complex.getExtendedValue()) == declaration) {
													result.addMatch(new ClonkSearchMatch(complex.toString(), 0, iniUnit, complex.getEndPos()-complex.getValue().length(), complex.getValue().length(), false, false));
												}
											}
											else if (entryClass == IDArray.class) {
												for (KeyValuePair<C4ID, Integer> pair : ((IDArray)complex.getExtendedValue()).getComponents()) {
													C4Object obj = script.getIndex().getObjectFromEverywhere(pair.getKey());
													if (obj == declaration)
														result.addMatch(new ClonkSearchMatch(pair.toString(), 0, iniUnit, complex.getEndPos()-complex.getValue().length(), complex.getValue().length(), false, false));
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

	public C4ScriptBase getDeclaringScript() {
		return declaringScript;
	}

}
