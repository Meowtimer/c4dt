package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialFuncRule;
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
		return String.format("Result of %s", call != null ? call.toString() : "<Unknown>");
	}
	
	@Override
	public String toString() {
		return typeName(true);
	}

	@Override
	public boolean intersects(IType type) {
		for (IType t : type)
			if (subsetOf(t))
				return true;
		return false;
	}

	@Override
	public boolean subsetOf(IType type) {
		return true;
	}

	@Override
	public boolean subsetOfAny(IType... types) {
		return IType.Default.subsetOfAny(this, types);
	}

	@Override
	public int precision() {
		return PrimitiveType.FUNCTION.precision();
	}

	@Override
	public IType staticType() {
		return PrimitiveType.ANY;
	}

	@Override
	public void setTypeDescription(String description) {
		// ...
	}

	@Override
	public IType eat(IType other) {
		return this;
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
			IType ct = TypeUtil.resolve(predType != null ? predType : callerType, context, callerType);
			Function func = contextScript == originalFunc.script() ? originalFunc : (ct instanceof Script ? ((Script)ct).findFunction(originalFunc.name()) : null);
			return func != null ? TypeUtil.resolve(func.returnType(), context, ct) : PrimitiveType.UNKNOWN;
		} catch (Exception e) {
			e.printStackTrace();
			return PrimitiveType.UNKNOWN;
		}
	}

}
