package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;

public class ControlFlowException extends Exception {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private ControlFlow controlFlow;

	public ControlFlow getControlFlow() {
		return controlFlow;
	}

	public ControlFlowException(ControlFlow controlFlow) {
		super();
		this.controlFlow = controlFlow;
	}
}