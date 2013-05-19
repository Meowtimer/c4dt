package net.arctics.clonk.ui.editors.c4script;


public enum ProposalCycle {
	ALL,
	OBJECT;

	public String description() {
		switch (this) {
		case ALL:
			return Messages.C4ScriptCompletionProcessor_AllCompletions;
		case OBJECT:
			return Messages.C4ScriptCompletionProcessor_ObjectCompletions;
		default:
			return null;
		}
	}
	public ProposalCycle cycle() { return values()[(this.ordinal()+1)%values().length]; }
}