package net.arctics.clonk.c4script.effect;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;

/**
 * One of the Fx* functions
 * @author madeen
 *
 */
public class EffectFunction extends Function {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final String FUNCTION_NAME_PREFIX = "Fx"; //$NON-NLS-1$
	public static final String FUNCTION_NAME_FORMAT = FUNCTION_NAME_PREFIX + "%s%s";
	public static final String[] DEFAULT_CALLBACKS = {"Start", "Timer", "Stop", "Effect", "Damage"};

	public static String functionName(final String effectName, final String callbackName) {
		return String.format(FUNCTION_NAME_FORMAT, effectName, callbackName);
	}

	private Effect effect;
	private String callbackName;

	public Effect effect() {
		return effect;
	}

	public void setEffect(final Effect effect) {
		this.effect = effect;
		this.callbackName = this.name().substring(FUNCTION_NAME_PREFIX.length()+effect.name().length());
	}

	public String callbackName() {
		return callbackName;
	}

	public IType effectType() {
		final Variable[] parameters = this.parameters;
		return parameters.length >= 2 ? parameters[1].type() : PrimitiveType.PROPLIST;
	}

}
