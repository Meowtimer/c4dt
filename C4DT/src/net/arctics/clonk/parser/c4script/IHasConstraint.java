package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.IHasIncludes;

public interface IHasConstraint {
	public enum ConstraintKind {
		Exact,
		Includes,
		CallerType
	}
	IHasIncludes constraint();
	ConstraintKind constraintKind();
	IType resolve(DeclarationObtainmentContext context, IType callerType);
}
