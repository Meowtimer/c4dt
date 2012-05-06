package net.arctics.clonk.ui.editors.c4script;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectResource;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Little helper thingie to find {@link IIndexEntity}s referenced at some location in a script. Usually {@link Declaration}, but might also be {@link ProjectResource} or some such. 
 *
 */
public class EntityLocator extends ExpressionLocator {
	private IIndexEntity entity;
	private Set<IIndexEntity> potentialEntities;
	private C4ScriptParser parser;
	
	/**
	 * Set of entities the location potentially refers to. Filled in the case of a function call for which the object type is not exactly known and similar situations.
	 * @return Set of potential entities
	 */
	public Set<? extends IIndexEntity> potentialEntities() {
		return potentialEntities;
	}

	private static IPredicate<IIndexEntity> IS_GLOBAL = new IPredicate<IIndexEntity>() {
		@Override
		public boolean test(IIndexEntity item) {
			return item instanceof Declaration && ((Declaration)item).isGlobal();
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
	
	public boolean initializeRegionDescription(RegionDescription d, Script script, IRegion region) {
		d.func = script.funcAt(region);
		if (d.func == null) {
			Variable var = script.variableWithInitializationAt(region);
			if (var == null)
				return false;
			else
				d.initialize(var.initializationExpressionLocation(), var.engine(), ExpressionsAndStatementsReportingFlavour.OnlyExpressions);
		} else
			d.initialize(d.func.body(), d.func.engine(), ExpressionsAndStatementsReportingFlavour.AlsoStatements);
		return true;
	}

	/**
	 * Initialize {@link EntityLocator} with an editor, a document and a region. After invoking the constructor, {@link #expressionRegion()}, {@link #entity()} etc will be if locating succeeded. 
	 * @param editor The editor
	 * @param doc The script document
	 * @param region Region in the script
	 * @throws BadLocationException
	 * @throws ParsingException
	 */
	public EntityLocator(ITextEditor editor, IDocument doc, IRegion region) throws BadLocationException, ParsingException {
		final Script script = Utilities.scriptForEditor(editor);
		if (script == null)
			return;
		RegionDescription d = new RegionDescription();
		if (!initializeRegionDescription(d, script, region)) {
			simpleFindDeclaration(doc, region, script, null);
			return;
		}
		if (region.getOffset() >= d.bodyStart) {
			exprRegion = new Region(region.getOffset()-d.bodyStart,0);
			parser = C4ScriptParser.reportExpressionsAndStatements(doc, script, d.func != null ? d.func : d.body, this, null, d.flavour, false);
			if (exprAtRegion != null) {
				EntityRegion declRegion = exprAtRegion.declarationAt(exprRegion.getOffset()-exprAtRegion.start(), parser);
				initializeProposedDeclarations(script, d, declRegion, exprAtRegion);
			}
		}
		else
			simpleFindDeclaration(doc, region, script, d.func);
	}

	public void initializeProposedDeclarations(final Script script, RegionDescription regionDescription, EntityRegion declRegion, ExprElm exprAtRegion) {
		boolean setRegion;
		if (declRegion != null && declRegion.potentialEntities() != null && declRegion.potentialEntities().size() > 0) {
			// region denotes multiple declarations - set proposed declarations to those
			this.potentialEntities = declRegion.potentialEntities();
			setRegion = true;
		}
		else if (declRegion != null && declRegion.entity() != null) {
			// declaration was found; return it if this is not an object call ('->') or if the found declaration is non-global
			// in which case the type of the calling object is probably known
			this.entity = declRegion.entity();
			setRegion = true;
		}
		else if (exprAtRegion instanceof AccessDeclaration) {
			final AccessDeclaration access = (AccessDeclaration) exprAtRegion;
			
			// gather declarations with that name from involved project indexes
			List<Declaration> projectDeclarations = new LinkedList<Declaration>();
			String declarationName = access.declarationName();
			// load scripts that contain the declaration name in their dictionary which is available regardless of loaded state
			IType t = access.predecessorInSequence() != null ? access.predecessorInSequence().type(parser) : null;
			if (t == null || t.specificness() <= PrimitiveType.OBJECT.specificness()) {
				for (Index i : script.index().relevantIndexes())
					i.loadScriptsContainingDeclarationsNamed(declarationName);
				for (Index i : script.index().relevantIndexes()) {
					List<Declaration> decs = i.declarationMap().get(declarationName);
					if (decs != null)
						projectDeclarations.addAll(decs);
				}
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
				potentialEntities = new HashSet<IIndexEntity>();
				if (projectDeclarations != null)
					potentialEntities.addAll(projectDeclarations);
				// only add engine func if not overloaded by any global function
				if (engineFunc != null && !Utilities.any(potentialEntities, IS_GLOBAL))
					potentialEntities.add(engineFunc);
				if (potentialEntities.size() == 0)
					potentialEntities = null;
				else if (potentialEntities.size() == 1) {
					for (IIndexEntity e : potentialEntities) {
						this.entity = e;
						break;
					}
				}
				setRegion = potentialEntities != null;
			}
			else
				setRegion = false;
		}
		else
			setRegion = false;
		if (setRegion && declRegion != null)
			this.exprRegion = new Region(regionDescription.bodyStart+declRegion.region().getOffset(), declRegion.region().getLength());
	}

	private void simpleFindDeclaration(IDocument doc, IRegion region, Script script, Function func) throws BadLocationException {
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
		entity = script.findDeclaration(doc.get(exprRegion.getOffset(), exprRegion.getLength()), new FindDeclarationInfo(script.index(), func));
	}

	/**
	 * @return Region of the expression detected at the location the {@link EntityLocator} was initialized at.
	 */
	public IRegion expressionRegion() {
		return exprRegion;
	}

	/**
	 * @return The entity the expression region refers to with sufficient certainty.
	 */
	public IIndexEntity entity() {
		return entity;
	}

	@Override
	public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
		expression.traverse(new ScriptParserListener() {
			@Override
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (exprRegion.getOffset() >= expression.start() && exprRegion.getOffset() < expression.end()) {
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, parser);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}
}