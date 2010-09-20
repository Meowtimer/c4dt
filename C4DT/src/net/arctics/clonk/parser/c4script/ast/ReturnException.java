package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;

public class ReturnException extends ControlFlowException {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private Object result;

	public Object getResult() {
		return result;
	}
	
	public ReturnException(Object result) {
		super(ControlFlow.Return);
		this.result = result;
	}
	
}