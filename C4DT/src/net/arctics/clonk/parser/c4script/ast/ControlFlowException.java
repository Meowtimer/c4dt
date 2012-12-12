package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;

public class ControlFlowException extends Exception {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private ControlFlow controlFlow;

	public ControlFlow controlFlow() {
		return controlFlow;
	}

	public ControlFlowException(ControlFlow controlFlow) {
		super();
		this.controlFlow = controlFlow;
	}
}