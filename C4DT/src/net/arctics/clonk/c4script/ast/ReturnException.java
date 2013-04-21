package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;

public class ReturnException extends ControlFlowException {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private Object result;

	public Object result() {
		return result;
	}
	
	public ReturnException(Object result) {
		super(ControlFlow.Return);
		this.result = result;
	}
	
}