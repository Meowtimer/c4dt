package net.arctics.clonk.ast;

import net.arctics.clonk.Core;

public class ControlFlowException extends Exception {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final ControlFlow controlFlow;

	public ControlFlow controlFlow() {
		return controlFlow;
	}

	public ControlFlowException(final ControlFlow controlFlow) {
		super();
		this.controlFlow = controlFlow;
	}
}