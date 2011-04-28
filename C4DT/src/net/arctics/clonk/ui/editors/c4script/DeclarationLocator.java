package net.arctics.clonk.ui.editors.c4script;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Little helper thingie to find declarations
 *
 */
public class DeclarationLocator extends ExpressionLocator {
	private ITextEditor editor;
	private Declaration declaration;
	private List<Declaration> proposedDeclarations;
	
	public List<Declaration> getProposedDeclarations() {
		return proposedDeclarations;
	}

	public ITextEditor getEditor() {
		return editor;
	}

	private static IPredicate<Declaration> IS_FUNC = new IPredicate<Declaration>() {
		public boolean test(Declaration item) {
			return item instanceof Function;
		}
	};
	private static IPredicate<Declaration> IS_GLOBAL = new IPredicate<Declaration>() {
		public boolean test(Declaration item) {
			return item.isGlobal();
		};
	};
	
	public DeclarationLocator(ITextEditor editor, IDocument doc, IRegion region) throws BadLocationException, ParsingException {
		this.editor = editor;
		final ScriptBase script = Utilities.getScriptForEditor(getEditor());
		if (script == null)
			return;
		int bodyStart;
		IRegion body;
		Engine engine;
		Function func = script.funcAt(region);
		ExpressionsAndStatementsReportingFlavour flavour;
		if (func == null) {
			Variable var = script.variableWithInitializationAt(region);
			if (var == null) {
				// outside function and variable initialization, fallback to old technique
				simpleFindDeclaration(doc, region, script, null);
				return;
			} else {
				body = var.getInitializationExpressionLocation();
				bodyStart = body.getOffset();
				engine = var.getEngine();
				flavour = ExpressionsAndStatementsReportingFlavour.OnlyExpressions;
			}
		} else {
			body = func.getBody();
			bodyStart = body.getOffset();
			engine = func.getEngine();
			flavour = ExpressionsAndStatementsReportingFlavour.AlsoStatements;
		}
		if (region.getOffset() >= bodyStart) {
			exprRegion = new Region(region.getOffset()-bodyStart,0);
			C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(doc, script, func != null ? func : body, this, null, flavour, false);
			if (exprAtRegion != null) {
				DeclarationRegion declRegion = exprAtRegion.declarationAt(exprRegion.getOffset()-exprAtRegion.getExprStart(), parser);
				boolean setRegion;
				if (declRegion != null && declRegion.getPotentialDeclarations() != null && declRegion.getPotentialDeclarations().size() > 0) {
					// region denotes multiple declarations - set proposed declarations to those
					this.proposedDeclarations = declRegion.getPotentialDeclarations();
					setRegion = true;
				}
				else if (
					declRegion != null && declRegion.getConcreteDeclaration() != null &&
					(!(exprAtRegion.getPredecessorInSequence() instanceof MemberOperator) || !declRegion.getConcreteDeclaration().isGlobal())
				) {
					// declaration was found; return it if this is not an object call ('->') or if the found declaration is non-global
					// in which case the type of the calling object is probably known
					this.declaration = declRegion.getConcreteDeclaration();
					setRegion = true;
				}
				else if (exprAtRegion instanceof AccessDeclaration) {
					AccessDeclaration access = (AccessDeclaration) exprAtRegion;
					
					// gather declarations with that name from involved project indexes
					List<Declaration> projectDeclarations = new LinkedList<Declaration>();
					for (ClonkIndex i : script.getIndex().relevantIndexes()) {
						List<Declaration> decs = i.declarationMap().get(access.getDeclarationName());
						if (decs != null)
							projectDeclarations.addAll(decs);
					}
					
					if (projectDeclarations != null)
						projectDeclarations = Utilities.filter(projectDeclarations, IS_FUNC);
					
					Function engineFunc = engine.findFunction(access.getDeclarationName());
					if (projectDeclarations != null || engineFunc != null) {
						proposedDeclarations = new LinkedList<Declaration>();
						if (projectDeclarations != null)
							proposedDeclarations.addAll(projectDeclarations);
						// only add engine func if not overloaded by any global function
						if (engineFunc != null && !Utilities.any(proposedDeclarations, IS_GLOBAL))
							proposedDeclarations.add(engineFunc);
						if (proposedDeclarations.size() == 0)
							proposedDeclarations = null;
						else if (proposedDeclarations.size() == 1) {
							this.declaration = proposedDeclarations.get(0);
						}
						setRegion = proposedDeclarations != null;
					}
					else
						setRegion = false;
				}
				else
					setRegion = false;
				if (setRegion)
					this.exprRegion = new Region(bodyStart+declRegion.getRegion().getOffset(), declRegion.getRegion().getLength());
			}
		}
		else
			simpleFindDeclaration(doc, region, script, func);
	}

	private void simpleFindDeclaration(IDocument doc, IRegion region, ScriptBase script, Function func) throws BadLocationException {
		IRegion lineInfo;
		String line;
		try {
			lineInfo = doc.getLineInformationOfOffset(region.getOffset());
			line = doc.get(lineInfo.getOffset(),lineInfo.getLength());
		} catch (BadLocationException e) {
			return;
		}
		int localOffset = region.getOffset() - lineInfo.getOffset();
		int start,end;
		for (start = localOffset; start > 0 && Character.isJavaIdentifierPart(line.charAt(start-1)); start--);
		for (end = localOffset; end < line.length() && Character.isJavaIdentifierPart(line.charAt(end)); end++);
		exprRegion = new Region(lineInfo.getOffset()+start,end-start);
		declaration = script.findDeclaration(doc.get(exprRegion.getOffset(), exprRegion.getLength()), new FindDeclarationInfo(script.getIndex(), func));
	}

	/**
	 * @return the identRegion
	 */
	public IRegion getIdentRegion() {
		return exprRegion;
	}

	/**
	 * @return the declaration
	 */
	public Declaration getDeclaration() {
		return declaration;
	}

	public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
		expression.traverse(new ScriptParserListener() {
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (exprRegion.getOffset() >= expression.getExprStart() && exprRegion.getOffset() < expression.getExprEnd()) {
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, parser);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}
}