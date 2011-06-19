package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.IType;

/**
 * Some type that has includes.
 * @author madeen
 *
 */
public interface IHasIncludes extends IHasSubDeclarations, IType {
	Collection<? extends IHasIncludes> getIncludes(ClonkIndex index, boolean recursive);
	/**
	 * Return whether this type includes another one.
	 * @param other The other type
	 * @return True or false.
	 */
	boolean includes(IHasIncludes other);
	/**
	 * Gather includes from some {@link ClonkIndex} into a set. This operation can be performed recursively or not.
	 * @param set The set which will contain the includes. For recursion catching, the type itself is also added to this set. 
	 * @param index The index to look for includes in
	 * @param recursive Whether includes of includes will also be added and so on.
	 * @return False if this type is already contained in the set, true if this type wasn't contained.
	 * gatherIncludes implementations are responsible for adding this type to the set in this case, or else infinite recursion wreaks its ugly head. 
	 */
	boolean gatherIncludes(Set<IHasIncludes> set, ClonkIndex index, boolean recursive);
	
	/**
	 * Empty list representing "no includes".
	 */
	static final Collection<IHasIncludes> NO_INCLUDES = new ArrayList<IHasIncludes>(0);
	
	/**
	 * Default implementation for some interface methods.
	 * @author madeen
	 *
	 */
	public static class Default {
		/**
		 * Get includes by gathering them using {@link IHasIncludes#gatherIncludes(Set, ClonkIndex, boolean)}.
		 * @param instance The instance to call {@link IHasIncludes#gatherIncludes(Set, ClonkIndex, boolean)} on
		 * @param index The index to pass to {@link IHasIncludes#gatherIncludes(Set, ClonkIndex, boolean)}
		 * @param recursive The recursive parameter to pass to {@link IHasIncludes#gatherIncludes(Set, ClonkIndex, boolean)}
		 * @return Direct or recursive includes, depending on the recursive parameter. This collection does not include the instance itself.
		 */
		public static Collection<? extends IHasIncludes> getIncludes(IHasIncludes instance, ClonkIndex index, boolean recursive) {
			Set<IHasIncludes> result = new HashSet<IHasIncludes>();
			instance.gatherIncludes(result, index, recursive);
			result.remove(instance);
			return result;
		}
	}
}
