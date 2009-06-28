package net.arctics.clonk.ui.search;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4Directive;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.CachedEngineFuncs;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.DeclarationRegion;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprAccessDeclaration;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprCallFunc;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprID;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprObjectCall;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprString;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.Statement;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.Function;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniEntry;
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
		return String.format("Search for '%s'", declaration.toString()); 
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
			
			private ExprString functionNameExpr;
			
			private boolean potentiallyReferencedByObjectCall(ExprElm expression) {
				if (expression instanceof ExprCallFunc && expression.getPredecessorInSequence() instanceof ExprObjectCall) {
					ExprCallFunc callFunc = (ExprCallFunc) expression;
					return callFunc.getDeclarationName().equals(declaration.getName());
				}
				return false;
			}
			private boolean potentiallyReferencedByCallFunction(ExprAccessDeclaration expression, C4ScriptParser parser) {
				functionNameExpr = null;
				if (expression instanceof ExprCallFunc) {
					ExprCallFunc callFunc = (ExprCallFunc) expression;
					for (ExprElm e : callFunc.getParams()) {
						if (e instanceof ExprString) {
							functionNameExpr = (ExprString) e;
							DeclarationRegion decRegion = e.declarationAt(0, parser);
							if (decRegion != null)
								return decRegion.getDeclaration() == declaration;
							break;
						}
					}
				}
				return false;
			}
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (expression instanceof ExprAccessDeclaration) {
					ExprAccessDeclaration accessDeclExpr = (ExprAccessDeclaration) expression;
					if (accessDeclExpr.getDeclaration(parser) == declaration)
						result.addMatch(expression, parser, false, accessDeclExpr.indirectAccess());
					else if (Utilities.isAnyOf(accessDeclExpr.getDeclaration(), CachedEngineFuncs.CallFunctions) && potentiallyReferencedByCallFunction(accessDeclExpr, parser)) {
						result.addMatch(functionNameExpr, parser, true, true);
					}
					else if (potentiallyReferencedByObjectCall(expression)) {
						C4Function otherFunc = (C4Function) accessDeclExpr.getDeclaration();
						boolean potential = (otherFunc == null || !((C4Function)declaration).isRelatedFunction(otherFunc));
						result.addMatch(expression, parser, potential, accessDeclExpr.indirectAccess());
					}
				}
				else if (expression instanceof ExprID && declaration instanceof C4ScriptBase) {
					if (expression.guessObjectType(parser) == declaration)
						result.addMatch(expression, parser, false, false);
				}
				return TraversalContinuation.Continue;
			}
		};
		final IExpressionListener searchExpressions = new IExpressionListener() {
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
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
		if (declaration instanceof C4Object) {
			C4Directive include = script.getIncludeDirectiveFor((C4Object) declaration);
			if (include != null)
				result.addMatch(include.getExprElm(), parser, false, false);
		}
		parser.setExpressionListener(searchExpressions);
		try {
			parser.parseCodeOfFunctions();
		} catch (ParsingException e) {
			e.printStackTrace();
		}
		
		// also search related files (actmap, defcore etc)
		try {
			searchScriptRelatedFiles(script);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void searchScriptRelatedFiles(C4ScriptBase script) throws CoreException {
		if (script instanceof C4ObjectIntern) {
			IContainer objectFolder = ((C4ObjectIntern)script).getScriptFile().getParent();
			for (IResource res : objectFolder.members()) {
				if (res instanceof IFile) {
					IFile file = (IFile)res;
					C4Structure pinned = C4Structure.pinned(file, true);
					if (pinned instanceof IniUnit) {
						IniUnit iniUnit = (IniUnit) pinned;
						for (IniSection sec : iniUnit) {
							for (IniEntry entry : sec) {
								if (entry instanceof ComplexIniEntry) {
									ComplexIniEntry complex = (ComplexIniEntry) entry;
									if (complex.getEntryConfig() != null) {
										Class<?> entryClass = complex.getEntryConfig().getEntryClass();
										if (entryClass == Function.class) {
											C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(objectFolder);
											if (obj != null) {
												C4Declaration declaration = obj.findFunction(complex.getValue());
												if (declaration == this.declaration)
													result.addMatch(new ClonkSearchMatch(entry.toString(), 0, iniUnit, entry.getStartPos(), entry.getEndPos()-entry.getStartPos(), false, false));
											}
										}
										else if (declaration instanceof C4Object) {
											if (entryClass == C4ID.class) {
												if (script.getIndex().getObjectFromEverywhere((C4ID) complex.getExtendedValue()) == declaration) {
													result.addMatch(new ClonkSearchMatch(entry.toString(), 0, iniUnit, entry.getStartPos(), entry.getEndPos()-entry.getStartPos(), false, false));
												}
											}
											else if (entryClass == IDArray.class) {
												for (KeyValuePair<C4ID, Integer> pair : ((IDArray)complex.getExtendedValue()).getComponents()) {
													C4Object obj = script.getIndex().getObjectFromEverywhere(pair.getKey());
													if (obj == declaration)
														result.addMatch(new ClonkSearchMatch(pair.toString(), 0, iniUnit, entry.getStartPos(), entry.getEndPos()-entry.getStartPos(), false, false));
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
