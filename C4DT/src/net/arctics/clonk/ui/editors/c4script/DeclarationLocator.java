package net.arctics.clonk.ui.editors.c4script;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
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
	private Set<Declaration> proposedDeclarations;
	
	public Set<Declaration> getProposedDeclarations() {
		return proposedDeclarations;
	}

	public ITextEditor getEditor() {
		return editor;
	}

	private static IPredicate<Declaration> IS_GLOBAL = new IPredicate<Declaration>() {
		public boolean test(Declaration item) {
			return item.isGlobal();
		};
	};
	
	public static class RegionDescription {
		public IRegion body;
		public int bodyStart;
		public Engine engine;
		public ExpressionsAndStatementsReportingFlavour flavour;
		public Function func;
		public void initialize(IRegion body, Engine engine, ExpressionsAndStatementsReportingFlavour flavour) {
			this.body = body;
			this.bodyStart = body.getOffset();
			this.engine = engine;
			this.flavour = flavour;
		}
	}
	
	public boolean initializeRegionDescription(RegionDescription d, ScriptBase script, IRegion region) {
		d.func = script.funcAt(region);
		if (d.func == null) {
			Variable var = script.variableWithInitializationAt(region);
			if (var == null)
				return false;
			else
				d.initialize(var.getInitializationExpressionLocation(), var.getEngine(), ExpressionsAndStatementsReportingFlavour.OnlyExpressions);
		} else
			d.initialize(d.func.getBody(), d.func.getEngine(), ExpressionsAndStatementsReportingFlavour.AlsoStatements);
		return true;
	}
	
	public DeclarationLocator(ITextEditor editor, IDocument doc, IRegion region) throws BadLocationException, ParsingException {
		this.editor = editor;
		final ScriptBase script = Utilities.getScriptForEditor(getEditor());
		if (script == null)
			return;
		RegionDescription d = new RegionDescription();
		if (!initializeRegionDescription(d, script, region)) {
			simpleFindDeclaration(doc, region, script, null);
			return;
		}
		if (region.getOffset() >= d.bodyStart) {
			exprRegion = new Region(region.getOffset()-d.bodyStart,0);
			C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(doc, script, d.func != null ? d.func : d.body, this, null, d.flavour, false);
			if (exprAtRegion != null) {
				DeclarationRegion declRegion = exprAtRegion.declarationAt(exprRegion.getOffset()-exprAtRegion.getExprStart(), parser);
				initializeProposedDeclarations(script, d, declRegion, exprAtRegion);
			}
		}
		else
			simpleFindDeclaration(doc, region, script, d.func);
	}

	public void initializeProposedDeclarations(final ScriptBase script, RegionDescription regionDescription, DeclarationRegion declRegion, ExprElm exprAtRegion) {
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
			final AccessDeclaration access = (AccessDeclaration) exprAtRegion;
			
			// gather declarations with that name from involved project indexes
			List<Declaration> projectDeclarations = new LinkedList<Declaration>();
			String declarationName = access.getDeclarationName();
			// load scripts that contain the declaration name in their dictionary which is available regardless of loaded state
			for (Index i : script.getIndex().relevantIndexes()) {
				i.loadScriptsContainingDeclarationsBeingNamed(declarationName);
			}
			for (Index i : script.getIndex().relevantIndexes()) {
				List<Declaration> decs = i.declarationMap().get(declarationName);
				if (decs != null)
					projectDeclarations.addAll(decs);
			}
			
			if (projectDeclarations != null)
				projectDeclarations = Utilities.filter(projectDeclarations, new IPredicate<Declaration>() {
					@Override
					public boolean test(Declaration item) {
						return access.declarationClass().isInstance(item);
					}
				});
			
			Function engineFunc = regionDescription.engine.findFunction(declarationName);
			if (projectDeclarations != null || engineFunc != null) {
				proposedDeclarations = new HashSet<Declaration>();
				if (projectDeclarations != null)
					proposedDeclarations.addAll(projectDeclarations);
				// only add engine func if not overloaded by any global function
				if (engineFunc != null && !Utilities.any(proposedDeclarations, IS_GLOBAL))
					proposedDeclarations.add(engineFunc);
				if (proposedDeclarations.size() == 0)
					proposedDeclarations = null;
				else if (proposedDeclarations.size() == 1) {
					for (Declaration d : proposedDeclarations) {
						this.declaration = d;
						break;
					}
				}
				setRegion = proposedDeclarations != null;
			}
			else
				setRegion = false;
		}
		else
			setRegion = false;
		if (setRegion && declRegion != null)
			this.exprRegion = new Region(regionDescription.bodyStart+declRegion.getRegion().getOffset(), declRegion.getRegion().getLength());
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

	@Override
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