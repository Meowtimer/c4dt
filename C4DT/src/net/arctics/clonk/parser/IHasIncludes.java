package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Script;

/**
 * Some type that has includes. Most likely some kind of {@link Script}.
 * @author madeen
 */
public interface IHasIncludes extends IHasSubDeclarations, IType {
	
	public static class GatherIncludesOptions {
		public static final int Recursive = 1;
		public static final int NoAppendages = 2;
	}
	
	Collection<? extends IHasIncludes> includes(Index contextIndex, int options);
	/**
	 * Return whether this type includes another one.
	 * @param other The other type
	 * @return True or false.
	 */
	boolean doesInclude(Index contextIndex, IHasIncludes other);
	/**
	 * Gather includes from some {@link Index} into a set. This operation can be performed recursively or not.
	 * @param set The set which will contain the includes. For recursion catching, the type itself is also added to this set. 
	 * @param index The index to look for includes in
	 * @param options A bitmask Whether includes of includes will also be added and so on.
	 * @return False if this type is already contained in the set, true if this type wasn't contained.
	 * gatherIncludes implementations are responsible for adding this type to the set in this case, or else infinite recursion wreaks its ugly head. 
	 */
	boolean gatherIncludes(Index contextIndex, List<IHasIncludes> set, int options);
	
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
		 * Get includes by gathering them using {@link IHasIncludes#gatherIncludes(Set, Index, boolean)}.
		 * @param instance The instance to call {@link IHasIncludes#gatherIncludes(Set, Index, boolean)} on
		 * @param index The index to pass to {@link IHasIncludes#gatherIncludes(Set, Index, boolean)}
		 * @param recursive The recursive parameter to pass to {@link IHasIncludes#gatherIncludes(Set, Index, boolean)}
		 * @return Direct or recursive includes, depending on the recursive parameter. This collection does not include the instance itself.
		 */
		public static Collection<? extends IHasIncludes> includes(Index contextIndex, IHasIncludes instance, int options) {
			List<IHasIncludes> result = new ArrayList<IHasIncludes>(10);
			instance.gatherIncludes(contextIndex, result, options);
			result.remove(instance);
			return result;
		}
	}
}
