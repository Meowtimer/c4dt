package net.arctics.clonk.ui.editors.c4script;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
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
	private C4Declaration declaration;
	private List<C4Declaration> proposedDeclarations;
	
	public List<C4Declaration> getProposedDeclarations() {
		return proposedDeclarations;
	}

	public ITextEditor getEditor() {
		return editor;
	}

	private static IPredicate<C4Declaration> IS_FUNC = new IPredicate<C4Declaration>() {
		public boolean test(C4Declaration item) {
			return item instanceof C4Function;
		}
	};
	private static IPredicate<C4Declaration> IS_GLOBAL = new IPredicate<C4Declaration>() {
		public boolean test(C4Declaration item) {
			return item.isGlobal();
		};
	};
	
	public DeclarationLocator(ITextEditor editor, IDocument doc, IRegion region) throws BadLocationException, ParsingException {
		this.editor = editor;
		final C4ScriptBase script = Utilities.getScriptForEditor(getEditor());
		if (script == null)
			return;
		int bodyStart;
		IRegion body;
		C4Engine engine;
		C4Function func = script.funcAt(region);
		ExpressionsAndStatementsReportingFlavour flavour;
		if (func == null) {
			C4Variable var = script.variableWithInitializationAt(region);
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
			C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatementsWithSpecificFlavour(doc, body, script, func, this, null, flavour);
			if (exprAtRegion != null) {
				DeclarationRegion declRegion = exprAtRegion.declarationAt(exprRegion.getOffset()-exprAtRegion.getExprStart(), parser);
				boolean setRegion;
				if (
					declRegion != null && declRegion.getDeclaration() != null &&
					(!(exprAtRegion.getPredecessorInSequence() instanceof MemberOperator) || !declRegion.getDeclaration().isGlobal())
				) {
					// declaration was found; return it if this is not an object call ('->') or if the found declaration is non-global
					// in which case the type of the calling object is probably known 
					this.declaration = declRegion.getDeclaration();
					setRegion = true;
				}
				else if (exprAtRegion instanceof AccessDeclaration) {
					AccessDeclaration access = (AccessDeclaration) exprAtRegion;
					
					// gather declarations with that name from involved project indexes
					List<C4Declaration> projectDeclarations = new LinkedList<C4Declaration>();
					for (ClonkIndex i : script.getIndex().relevantIndexes()) {
						List<C4Declaration> decs = i.getDeclarationMap().get(access.getDeclarationName());
						if (decs != null)
							projectDeclarations.addAll(decs);
					}
					
					if (projectDeclarations != null)
						projectDeclarations = Utilities.filter(projectDeclarations, IS_FUNC);
					
					C4Function engineFunc = engine.findFunction(access.getDeclarationName());
					if (projectDeclarations != null || engineFunc != null) {
						proposedDeclarations = new LinkedList<C4Declaration>();
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

	private void simpleFindDeclaration(IDocument doc, IRegion region, C4ScriptBase script, C4Function func) throws BadLocationException {
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
	public C4Declaration getDeclaration() {
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