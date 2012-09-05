package net.arctics.clonk.ui.editors.c4script;

enum ProposalCycle {
	ALL,
	LOCAL,
	OBJECT;

	public String description() {
		switch (this) {
		case ALL:
			return Messages.C4ScriptCompletionProcessor_AllCompletions;
		case LOCAL:
			return Messages.C4ScriptCompletionProcessor_LocalCompletions;
		case OBJECT:
			return Messages.C4ScriptCompletionProcessor_ObjectCompletions;
		default:
			return null;
		}
	}

	public ProposalCycle reverseCycle() {
		return values()[ordinal() == 0 ? values().length-1 : ordinal() - 1];
	}

	public ProposalCycle cycle() {
		return values()[(this.ordinal()+1)%values().length];
	}
}