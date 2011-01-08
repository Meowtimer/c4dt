package net.arctics.clonk.parser.c4script;

public interface IHasConstraint {
	public enum ConstraintKind {
		Exact,
		Includes,
		CallerType
	}
	C4ScriptBase constraintScript();
	ConstraintKind constraintKind();
}
