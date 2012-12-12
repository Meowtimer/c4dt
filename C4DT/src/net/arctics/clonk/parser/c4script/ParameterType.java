package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.ast.IFunctionCall;

public class ParameterType implements IResolvableType {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Variable parameter;
	public ParameterType(Variable variable) {
		parameter = variable;
	}

	public Variable parameter() { return parameter; }
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ParameterType && ((ParameterType)obj).parameter() == this.parameter();
	}
	
	@Override
	public Iterator<IType> iterator() {
		return iterable(PrimitiveType.UNKNOWN, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return true;
	}

	@Override
	public String typeName(boolean special) {
		if (special && parameter() != null)
			return String.format("Type of parameter '%s'", parameter.name());
		else
			return PrimitiveType.UNKNOWN.typeName(false);
	}
	
	@Override
	public String toString() { return typeName(true); }

	@Override
	public int precision() { return 1; }
	@Override
	public IType simpleType() { return PrimitiveType.UNKNOWN; }
	@Override
	public void setTypeDescription(String description) {}

	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		Variable p = parameter();
		if (p == null)
			return PrimitiveType.UNKNOWN;
		IFunctionCall call = context.currentFunctionCall();
		if (call != null && call.quasiCalledFunction(context) == p.parentDeclaration())
			return call.concreteParameterType(parameter, context);
		else if (context.currentFunction() != p.parentDeclaration())
			return PrimitiveType.UNKNOWN;
		else
			return this;
	}
}