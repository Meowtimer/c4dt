package net.arctics.clonk.parser.c4script;

public interface IHasConstraint {
	public enum ConstraintKind {
		Exact,
		Includes,
		CallerType
	}
	ScriptBase constraintScript();
	ConstraintKind constraintKind();
	IType resolve(DeclarationObtainmentContext context, IType callerType);
}
