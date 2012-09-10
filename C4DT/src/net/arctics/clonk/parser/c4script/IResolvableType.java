package net.arctics.clonk.parser.c4script;

import java.util.HashSet;
import java.util.Set;

public interface IResolvableType {

	public abstract IType resolve(DeclarationObtainmentContext context, IType callerType);
	
	public static class _ {
		private static ThreadLocal<Set<IResolvableType>> recursion = new ThreadLocal<Set<IResolvableType>>();
		public static IType resolve(IType t, DeclarationObtainmentContext context, IType callerType) {
			if (t instanceof IResolvableType) {
				IResolvableType rt = (IResolvableType)t;
				Set<IResolvableType> r = recursion.get();
				if (r == null)
					recursion.set(r = new HashSet<IResolvableType>());
				else if (r.contains(rt))
					return t;
				r.add(rt);
				try {
					return rt.resolve(context, callerType);
				} finally {
					r.remove(rt);
				}
			} else
				return t;
		}
	}

}