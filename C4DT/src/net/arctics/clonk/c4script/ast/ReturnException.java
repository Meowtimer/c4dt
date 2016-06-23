package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.ast.ControlFlowException;

public class ReturnException extends ControlFlowException {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final Object result;

	public Object result() {
		return result;
	}

	public ReturnException(final Object result) {
		super(ControlFlow.Return);
		this.result = result;
	}

}