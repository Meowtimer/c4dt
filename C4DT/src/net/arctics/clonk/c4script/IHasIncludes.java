package net.arctics.clonk.c4script;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.index.Index;

/**
 * Some type that has includes. Most likely some kind of {@link Script}.
 * @author madeen
 */
public interface IHasIncludes<T extends IHasIncludes<?>> extends IType {

	public static class GatherIncludesOptions {
		public static final int Recursive = 1;
		public static final int NoAppendages = 2;
	}

	Collection<T> includes(Index contextIndex, Object origin, int options);
	/**
	 * Return whether this type includes another one.
	 * @param other The other type
	 * @return True or false.
	 */
	boolean doesInclude(Index contextIndex, T other);
	/**
	 * Gather includes from some {@link Index} into a set. This operation can be performed recursively or not.
	 * @param set The set which will contain the includes. For recursion catching, the type itself is also added to this set.
	 * @param data.index The index to look for includes in
	 * @param options A bitmask Whether includes of includes will also be added and so on.
	 * @return False if this type is already contained in the set, true if this type wasn't contained.
	 * gatherIncludes implementations are responsible for adding this type to the set in this case, or else infinite recursion wreaks its ugly head.
	 */
	boolean gatherIncludes(Index contextIndex, Object origin, Collection<T> set, int options);

	/**
	 * Default implementation for some interface methods.
	 * @author madeen
	 *
	 */
	public static class Default {
		/**
		 * Get includes by gathering them using {@link IHasIncludes<T>#gatherIncludes(Set, Index, boolean)}.
		 * @param instance The instance to call {@link IHasIncludes<T>#gatherIncludes(Set, Index, boolean)} on
		 * @param data.index The index to pass to {@link IHasIncludes<T>#gatherIncludes(Set, Index, boolean)}
		 * @param options Options mask. See {@link GatherIncludesOptions}
		 * @return Requested includes.
		 */
		public static <T extends IHasIncludes<?>> Collection<T> includes(Index contextIndex, IHasIncludes<T> instance, Object origin, int options) {
			final Set<T> result = new HashSet<T>(10);
			instance.gatherIncludes(contextIndex, origin, result, options);
			result.remove(instance);
			return result;
		}
	}
}
