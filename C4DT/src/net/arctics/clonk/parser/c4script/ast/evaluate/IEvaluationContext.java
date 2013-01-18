package net.arctics.clonk.parser.c4script.ast.evaluate;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;

/**
 * Context for evaluating expressions (at parsetime/some other time)
 * @author madeen
 *
 */
public interface IEvaluationContext extends IVariableValueProvider {
	/**
	 * Get arguments supplied to the call of the containing function
	 * @return The arguments
	 */
	Object[] arguments();
	/**
	 * Get the function the expression to be evaluated resides in. <i>May be null</i>.
	 * @return The function
	 */
	Function function();
	/**
	 * The script that acts as the general context for this evaluation. <i>May not be null</i>.
	 * @return
	 */
	Script script();
	/**
	 * Offset in characters of the code fragment that is being evaluated.
	 * @return The offset
	 */
	int codeFragmentOffset();
	/**
	 * Report back the origin of some expression to the context. Useful to trace back where some string was actually declared.
	 * @param expression The expression that evaluated to the value to be found at the specified location
	 * @param location The location in the resource
	 * @param file The file
	 */
	void reportOriginForExpression(ExprElm expression, IRegion location, IFile file);
}