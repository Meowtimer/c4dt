package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.util.ArrayUtil;

public class CallReturnType implements IType, IResolvableType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final CallDeclaration call;
	private final SpecialFuncRule rule;
	
	public CallReturnType(CallDeclaration call, SpecialFuncRule rule, Script originatingScript) {
		super();
		this.call = call;
		this.rule = rule;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return ArrayUtil.iterable(this, call.function().returnType()).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return true;
	}

	@Override
	public String typeName(boolean special) {
		if (special)
			return String.format("Result of %s", call != null ? call.toString() : "<Unknown>");
		else
			return call != null && call.function() != null ? call.function().returnType().typeName(false) : PrimitiveType.ANY.typeName(false);
	}
	
	@Override
	public String toString() {
		return typeName(true);
	}

	@Override
	public int precision() {
		return PrimitiveType.FUNCTION.precision();
	}

	@Override
	public IType simpleType() {
		return PrimitiveType.ANY;
	}

	@Override
	public void setTypeDescription(String description) {
		// ...
	}

	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		try {
			if (rule != null) {
				IType ruleSays = rule.returnType(context, call);
				if (ruleSays != null)
					return ruleSays;
			}
			Function originalFunc = call.function(context);
			Script contextScript = context.script();
			if (contextScript == originalFunc.script() && !(originalFunc.returnType() instanceof IResolvableType))
				return originalFunc.returnType();
			IType predType = call.unresolvedPredecessorType();
			context.pushCurrentFunctionCall(call);
			try {
				IType ct = predType != null ? TypeUtil.resolve(predType, context, callerType) : callerType;
				Function func = contextScript == originalFunc.script() ? originalFunc : (ct instanceof Script ? ((Script)ct).findFunction(originalFunc.name()) : null);
				return func != null ? TypeUtil.resolve(func.returnType(), context, ct) : PrimitiveType.UNKNOWN;
			} finally {
				context.popCurrentFunctionCall();
			}
		} catch (Exception e) {
			//e.printStackTrace();
			return PrimitiveType.UNKNOWN;
		}
	}

}
