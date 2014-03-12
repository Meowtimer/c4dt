package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.filter;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeUtil;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.ProjectResource;
import java.util.function.Predicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Locates {@link IIndexEntity}s referenced at some location in a script. Usually {@link Declaration}, but might also be {@link ProjectResource} or some such.
 *
 */
public class EntityLocator extends ExpressionLocator<Void> {
	private IIndexEntity entity;
	private Set<IIndexEntity> potentialEntities;

	/**
	 * Set of entities the location potentially refers to. Filled in the case of a function call for which the object type is not exactly known and similar situations.
	 * @return Set of potential entities
	 */
	public Set<? extends IIndexEntity> potentialEntities() { return potentialEntities; }

	private static Predicate<IIndexEntity> IS_GLOBAL = item -> item instanceof Declaration && ((Declaration)item).isGlobal();

	/**
	 * Initialize {@link EntityLocator} with an editor, a document and a region. After invoking the constructor, {@link #expressionRegion()}, {@link #entity()} etc will be if locating succeeded.
	 * @param doc The script document
	 * @param region Region in the script
	 * @throws BadLocationException
	 * @throws ProblemException
	 */
	public EntityLocator(final Script script, final IDocument doc, final IRegion region) throws BadLocationException, ProblemException {
		if (script == null)
			return;
		exprRegion = region;
		script.traverse(this, null);
		if (exprAtRegion != null) {
			final EntityRegion declRegion = exprAtRegion.entityAt(exprRegion.getOffset()-exprAtRegion.absolute().getOffset(), this);
			initializeProposedDeclarations(script, declRegion, exprAtRegion);
		}
	}

	public void initializeProposedDeclarations(final Script script, final EntityRegion declRegion, final ASTNode exprAtRegion) {
		boolean setRegion;
		if (declRegion != null && declRegion.potentialEntities() != null && declRegion.potentialEntities().size() > 0) {
			// region denotes multiple declarations - set proposed declarations to those
			this.potentialEntities = new HashSet<IIndexEntity>();
			this.potentialEntities.addAll(declRegion.potentialEntities());
			setRegion = true;
		}
		else if (declRegion != null && declRegion.entity() instanceof Directive) {
			final Directive dir = (Directive) declRegion.entity();
			switch (dir.type()) {
			case INCLUDE: case APPENDTO:
				this.entity = script.index().definitionNearestTo(script.resource(), dir.contentAsID());
				setRegion = true;
				break;
			default:
				setRegion = false;
				break;
			}
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
			//final IType ty = defaulting(access.predecessorInSequence() != null ? access.predecessorInSequence().typingSnapshot() : null, PrimitiveType.UNKNOWN);
			//for (final IType t : ty)
				//if (t == PrimitiveType.OBJECT || t == PrimitiveType.ANY || t == PrimitiveType.UNKNOWN || t == PrimitiveType.ID) {
					for (final Index i : script.index().relevantIndexes())
						i.loadScriptsContainingDeclarationsNamed(declarationName);
					for (final Index i : script.index().relevantIndexes()) {
						final List<Declaration> decs = i.declarationMap().get(declarationName);
						if (decs != null)
							projectDeclarations.addAll(decs);
					}
				//	break;
			//	}

			if (projectDeclarations != null)
				projectDeclarations = filter(projectDeclarations, item -> access.declarationClass().isInstance(item));

			final Function engineFunc = exprAtRegion.parent(Script.class).engine().findFunction(declarationName);
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
			this.exprRegion = new Region(exprAtRegion.sectionOffset()+declRegion.region().getOffset(),
				declRegion.region().getLength());
	}

	/**
	 * @return Region of the expression detected at the location the {@link EntityLocator} was initialized at.
	 */
	public IRegion expressionRegion() { return exprRegion; }
	/**
	 * @return The entity the expression region refers to with sufficient certainty.
	 */
	public IIndexEntity entity() { return entity; }

	@Override
	public TraversalContinuation visitNode(final ASTNode expression, final Void v) {
		if (expression instanceof ProplistDeclaration)
			return TraversalContinuation.SkipSubElements;
		expression.traverse(new IASTVisitor<Void>() {
			@Override
			public TraversalContinuation visitNode(final ASTNode expression, final Void v) {
				if (expression instanceof ProplistDeclaration)
					return TraversalContinuation.SkipSubElements;
				final IRegion a = expression.absolute();
				if (exprRegion.getOffset() >= a.getOffset() && exprRegion.getOffset() < a.getOffset()+a.getLength()) {
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, null);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}

	public String infoText() {
		final IIndexEntity entity = entity();
		final ASTNode expr = expressionAtRegion();
		if (entity == null || expr == null)
			return null;
		if (entity instanceof Variable && expr instanceof AccessDeclaration) {
			final Function f = expr.parent(Function.class);
			final Script s = f.script();
			final Function.Typing typing = s.typings().get(f);
			return ((Variable)entity).infoText(defaulting(typing.nodeTypes[expr.localIdentifier()], PrimitiveType.UNKNOWN));
		} else if (entity instanceof Function && expr instanceof AccessDeclaration) {
			final ASTNode pred = expr.predecessor();
			final IType contextTy = pred == null ? expr.parent(Script.class) : TypeUtil.inferredType(pred);
			final Script context = contextTy instanceof MetaDefinition ? ((MetaDefinition)contextTy).definition() : as(contextTy, Script.class);
			final Function.Typing typing = context != null ? context.typings().get((Function)entity) : null;
			return ((Function)entity).infoText(typing);
		} else {
			final ASTNode pred = expr.predecessor();
			final Script context = pred == null ? expr.parent(Script.class) : as(TypeUtil.inferredType(pred), Script.class);
			return entity.infoText(context);
		}
	}
}