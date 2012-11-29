package net.arctics.clonk.parser.c4script;


public interface IResolvableType {
	public IType resolve(DeclarationObtainmentContext context, IType callerType);
}