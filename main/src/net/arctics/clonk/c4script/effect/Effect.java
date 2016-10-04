package net.arctics.clonk.c4script.effect;

import static net.arctics.clonk.util.Utilities.isAnyOf;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.util.ArrayUtil;

public class Effect extends ProplistDeclaration {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Map<String, EffectFunction> functions = new HashMap<String, EffectFunction>();

	private static final Map<String, IType> engineDefinedProperties = ArrayUtil.map(false,
		"CommandTarget", PrimitiveType.OBJECT,
		"Interval", PrimitiveType.INT,
		"Priority", PrimitiveType.INT,
		"Name", PrimitiveType.STRING,
		"Time", PrimitiveType.INT
	);

	public Effect(final String name, final Iterable<EffectFunction> functions) {
		setName(name);
		functions.forEach(this::addFunction);
		for (final Map.Entry<String, IType> engineDefinedProperty : engineDefinedProperties.entrySet()) {
			final Variable v = new Variable(engineDefinedProperty.getKey(), engineDefinedProperty.getValue());
			v.setScope(Scope.VAR);
			addComponent(v, false);
		}
	}

	public Map<String, EffectFunction> functions() { return functions; }

	@Override
	public Declaration findLocalDeclaration(final String declarationName, final Class<? extends Declaration> declarationClass) {
		if (Function.class.isAssignableFrom(declarationClass)) {
			return functions.get(declarationName);
		} else {
			return null;
		}
	}

	public void addFunction(final EffectFunction function) {
		function.setEffect(this);
		functions.put(function.callbackName(), function);
	}

	public static IType[] parameterTypesForCallback(final String callbackName, final Script script, final IType proplistType) {
		final IType effectParameter = script.engine().settings().supportsProplists ? proplistType : PrimitiveType.INT;
		if (isAnyOf(callbackName, "Start", "Timer", "Stop")) {
			return new IType[] {PrimitiveType.OBJECT, effectParameter};
		}
		if (callbackName.equals("Effect")) {
			return new IType[] {PrimitiveType.STRING, PrimitiveType.OBJECT, effectParameter};
		}
		if (callbackName.equals("Damage")) {
			return new IType[] {PrimitiveType.OBJECT, effectParameter, PrimitiveType.INT, PrimitiveType.INT};
		}
		return new IType[] {PrimitiveType.OBJECT, effectParameter};
	}

	@Override
	public String typeName(final boolean special) {
		return special ? name() : PrimitiveType.PROPLIST.typeName(false);
	}

}
