package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IResolvableType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;
import net.arctics.clonk.parser.c4script.inference.dabble.SpecialEngineRules.SpecialFuncRule;
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
	public IType simpleType() {
		return PrimitiveType.ANY;
	}

	@Override
	public void setTypeDescription(String description) {
		// ...
	}

	@Override
	public IType resolve(ProblemReportingContext context, IType callerType) {
		try {
			ScriptProcessor processor = as(context, ScriptProcessor.class);
			if (processor == null)
				return PrimitiveType.UNKNOWN;
			if (rule != null) {
				IType ruleSays = rule.returnType(processor, call);
				if (ruleSays != null)
					return ruleSays;
			}
			Function originalFunc = call.function(context);
			Script contextScript = context.script();
			if (contextScript == originalFunc.script() && !(originalFunc.returnType() instanceof IResolvableType))
				return originalFunc.returnType();
			IType predType = processor.reporter(call).unresolvedPredecessorType(call, processor);
			IType ct = predType != null ? TypeUtil.resolve(predType, context, callerType) : callerType;
			Function func = contextScript == originalFunc.script() ? originalFunc : (ct instanceof Script ? ((Script)ct).findFunction(originalFunc.name()) : null);
			return func != null ? TypeUtil.resolve(func.returnType(), context, ct) : PrimitiveType.UNKNOWN;
		} catch (Exception e) {
			return PrimitiveType.UNKNOWN;
		}
	}

}
