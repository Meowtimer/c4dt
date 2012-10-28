package net.arctics.clonk.parser.c4script;


public interface IResolvableType {

	public abstract IType resolve(DeclarationObtainmentContext context, IType callerType);

}