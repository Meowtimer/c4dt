package net.arctics.clonk.c4script.typing;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.ast.AccessDeclaration;

import org.eclipse.jface.text.IRegion;

/**
 * Interface providing some facilities to type {@link ASTNode}s.
 * @author madeen
 *
 */
public interface ITypingContext {
	/**
	 * Return the type of a node.
	 * @param node The node to return the type of
	 * @return Whatever this context thinks is the type of the node.
	 */
	IType typeOf(ASTNode node);
	/**
	 * Return the type of a node if it is an instance of some class.
	 * @param node The node to return the type of
	 * @param cls Class of the type
	 * @return Whatever this context thinks is the type of the node, an instance of cls,
	 *  null if the type of the node is not an instance of cls.
	 */
	<T extends IType> T typeOf(ASTNode node, Class<T> cls);
	/**
	 * Obtain a {@link Declaration} this typing context thinks the provided {@link AccessDeclaration} refers to.
	 * @param access The access node
	 * @return The declaration or null if no declaration could be obtained.
	 */
	<T extends AccessDeclaration> Declaration obtainDeclaration(T access);
	/**
	 * Perform a typing judgement on a node.
	 * @param node The node
	 * @param type The type
	 * @param mode Judgement mode.
	 * @return Whether judgement was in some way some successful.
	 */
	boolean judgement(ASTNode node, IType type, TypingJudgementMode mode);
	/**
	 * Create a marker pointing out some type incompatibility at a node.
	 * @param node The node at which the incompatibility is to be marked
	 * @param region Text region
	 * @param left First type.
	 * @param right Second type incompatible with the first one
	 */
	void incompatibleTypesMarker(ASTNode node, IRegion region, IType left, IType right);
	/**
	 * Returns whether some node is deemed to refer to a modifiable value.
	 * @param node The node to test
	 * @return True if modifiable, false if not.
	 */
	boolean isModifiable(ASTNode node);
	/**
	 * Return declaration a node refers to.
	 * @param node The node
	 * @return The referred-to declaration
	 */
	Declaration declarationOf(ASTNode node);
}
