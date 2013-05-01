package net.arctics.clonk.ast;

import net.arctics.clonk.util.IPredicate;

/**
 * Interface for transforming an element in an expression.
 * @author madeen
 *
 */
public interface ITransformer {
	/**
	 * Canonical object to be returned by {@link #transform(ASTNode, ASTNode, ASTNode)} if the passed element is to be removed
	 * instead of being replaced.
	 */
	static final ASTNode REMOVE = new ASTNode();
	/**
	 * {@link IPredicate} filtering out references to {@link #REMOVE}.
	 */
	static final IPredicate<ASTNode> FILTER_REMOVE = new IPredicate<ASTNode>() {
		@Override
		public boolean test(ASTNode item) {
			return item != ITransformer.REMOVE;
		}
	};
	/**
	 * Transform the passed expression. For various purposes some context is supplied as well so the transformer can
	 * see the last expression it was passed and what it transformed it to.
	 * @param previousExpression The previous expression passed to the transformer
	 * @param previousTransformationResult The previous transformation result
	 * @param expression The expression to be transformed now.
	 * @return The transformed expression, the expression unmodified or some canonical object like {@link #REMOVE}
	 */
	Object transform(ASTNode previousExpression, Object previousTransformationResult, ASTNode expression);
}