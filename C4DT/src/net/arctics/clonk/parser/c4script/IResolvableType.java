package net.arctics.clonk.parser.c4script;

public interface IResolvableType {

	public abstract IType resolve(DeclarationObtainmentContext context, IType callerType);
	
	public static class _ {
		public static IType resolve(IType t, DeclarationObtainmentContext context, IType callerType) {
			return t instanceof IResolvableType ? ((IResolvableType)t).resolve(context, callerType) : t;
		}
	}

}