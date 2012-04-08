package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.IHasIncludes;

public interface IHasConstraint extends IResolvableType {
	public enum ConstraintKind {
		Exact,
		Includes,
		CallerType
	}
	IHasIncludes constraint();
	ConstraintKind constraintKind();
}
