package net.arctics.clonk.parser.c4script;


public interface IResolvableType extends IType {
	public IType resolve(ProblemReportingContext context, IType callerType);
}