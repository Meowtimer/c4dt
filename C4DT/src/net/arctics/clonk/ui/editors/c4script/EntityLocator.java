package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.FindDeclarationInfo;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.IType;
import net.arctics.clonk.c4script.InitializationFunction;
import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.typing.TypeUtil;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectResource;
import net.arctics.clonk.parser.ParsingException;
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
public class EntityLocator extends ExpressionLocator<Object> {
	private IIndexEntity entity;
	private Set<IIndexEntity> potentialEntities;

	/**
	 * Set of entities the location potentially refers to. Filled in the case of a function call for which the object type is not exactly known and similar situations.
	 * @return Set of potential entities
	 */
	public Set<? extends IIndexEntity> potentialEntities() { return potentialEntities; }

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
		public Function func;
		public void initialize(IRegion body, Engine engine) {
			this.body = body;
			this.bodyStart = body.getOffset();
			this.engine = engine;
		}
	}

	public boolean initializeRegionDescription(RegionDescription d, Script script, IRegion region) {
		d.func = script.funcAt(region);
		if (d.func == null) {
			final Variable var = script.variableWithInitializationAt(region);
			if (var == null)
				return false;
			else
				d.initialize(var.initializationExpressionLocation(), var.engine());
		} else
			d.initialize(d.func.bodyLocation(), d.func.engine());
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
		final RegionDescription d = new RegionDescription();
		if (!initializeRegionDescription(d, script, region)) {
			simpleFindDeclaration(doc, region, script, null);
			return;
		}
		if (region.getOffset() >= d.bodyStart) {
			exprRegion = new Region(region.getOffset()-d.bodyStart, 0);
			if (d.func != null)
				d.func.traverse(this, null);
			if (exprAtRegion != null) {
				final EntityRegion declRegion = exprAtRegion.entityAt(exprRegion.getOffset()-exprAtRegion.start(), TypeUtil.problemReportingContext(script));
				initializeProposedDeclarations(script, d, declRegion, exprAtRegion);
			}
		}
		else
			simpleFindDeclaration(doc, region, script, d.func);
	}

	public void initializeProposedDeclarations(final Script script, RegionDescription regionDescription, EntityRegion declRegion, ASTNode exprAtRegion) {
		boolean setRegion;
		if (declRegion != null && declRegion.potentialEntities() != null && declRegion.potentialEntities().size() > 0) {
			// region denotes multiple declarations - set proposed declarations to those
			this.potentialEntities = new HashSet<IIndexEntity>();
			this.potentialEntities.addAll(declRegion.potentialEntities());
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
			List<IIndexEntity> projectDeclarations = new LinkedList<IIndexEntity>();
			final String declarationName = access.name();
			// load scripts that contain the declaration name in their dictionary which is available regardless of loaded state
			final IType ty = defaulting(access.predecessorInSequence() != null ? access.predecessorInSequence().inferredType() : null, PrimitiveType.UNKNOWN);
			for (final IType t : ty)
				if (t == PrimitiveType.OBJECT || t == PrimitiveType.ANY || t == PrimitiveType.UNKNOWN || t == PrimitiveType.ID) {
					for (final Index i : script.index().relevantIndexes())
						i.loadScriptsContainingDeclarationsNamed(declarationName);
					for (final Index i : script.index().relevantIndexes()) {
						final List<Declaration> decs = i.declarationMap().get(declarationName);
						if (decs != null)
							projectDeclarations.addAll(decs);
					}
					break;
				}

			if (projectDeclarations != null)
				projectDeclarations = Utilities.filter(projectDeclarations, new IPredicate<IIndexEntity>() {
					@Override
					public boolean test(IIndexEntity item) {
						return access.declarationClass().isInstance(item);
					}
				});

			final Function engineFunc = regionDescription.engine.findFunction(declarationName);
			if (projectDeclarations != null || engineFunc != null) {
				potentialEntities = new HashSet<IIndexEntity>();
				if (projectDeclarations != null)
					potentialEntities.addAll(projectDeclarations);
				// only add engine func if not overloaded by any global function
				if (engineFunc != null && !Utilities.any(potentialEntities, IS_GLOBAL))
					potentialEntities.add(engineFunc);
				if (potentialEntities.size() == 0)
					potentialEntities = null;
				else if (potentialEntities.size() == 1)
					for (final IIndexEntity e : potentialEntities) {
						this.entity = e;
						break;
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
		} catch (final BadLocationException e) {
			return;
		}
		final int localOffset = region.getOffset() - lineInfo.getOffset();
		int start,end;
		for (start = localOffset; start > 0 && Character.isJavaIdentifierPart(line.charAt(start-1)); start--);
		for (end = localOffset; end < line.length() && Character.isJavaIdentifierPart(line.charAt(end)); end++);
		exprRegion = new Region(lineInfo.getOffset()+start,end-start);
		for (final Declaration d : script.subDeclarations(script.index(), DeclMask.FUNCTIONS|DeclMask.VARIABLES)) {
			if (d instanceof InitializationFunction)
				continue;
			if (d.isAt(region.getOffset())) {
				entity = d;
				return;
			}
		}
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
	public TraversalContinuation visitNode(ASTNode expression, Object context) {
		expression.traverse(new IASTVisitor<Void>() {
			@Override
			public TraversalContinuation visitNode(ASTNode expression, Void _) {
				if (exprRegion.getOffset() >= expression.start() && exprRegion.getOffset() < expression.end()) {
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, null);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}
}