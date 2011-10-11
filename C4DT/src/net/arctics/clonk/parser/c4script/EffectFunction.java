package net.arctics.clonk.parser.c4script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;

/**
 * One of the Fx* functions
 * @author madeen
 *
 */
public class EffectFunction extends Function {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	public static final String FUNCTION_NAME_PREFIX = "Fx"; //$NON-NLS-1$
	public static final String FUNCTION_NAME_FORMAT = FUNCTION_NAME_PREFIX + "%s%s"; //$NON-NLS-0$
	
	public enum HardcodedCallbackType {
		Start,
		Timer,
		Stop;
		
		Pattern pattern;
		
		private HardcodedCallbackType() {
			pattern = Pattern.compile(String.format(FUNCTION_NAME_FORMAT, "(.*?)", name()+"$"));
		}
		
		public Pattern getPattern() {
			return pattern;
		}
		
		public String nameForEffect(String effect) {
			return String.format(FUNCTION_NAME_FORMAT, effect, name());
		}
	}

	private EffectFunction startFunction;
	private final HardcodedCallbackType hardcodedCallbackType;
	private Pattern additionalCallbacksPattern;
	private String effectName;
	
	public EffectFunction(String effectName, HardcodedCallbackType type) {
		super();
		this.hardcodedCallbackType = type;
		this.effectName = effectName;
		if (type == HardcodedCallbackType.Start) {
			additionalCallbacksPattern = Pattern.compile(String.format(FUNCTION_NAME_FORMAT, effectName, "(.*?)"));
			startFunction = this;
		}
	}
	
	public Pattern getAdditionalCallbacksPattern() {
		return additionalCallbacksPattern;
	}

	public HardcodedCallbackType getHardcodedCallbackType() {
		return hardcodedCallbackType;
	}
	
	public String getEffectName() {
		return effectName;
	}
	
	private void setStartFunction(EffectFunction startFunction) {
		this.startFunction = startFunction;
		if (startFunction != null) {
			this.additionalCallbacksPattern = startFunction.additionalCallbacksPattern;
			this.effectName = startFunction.effectName;
		} else {
			this.additionalCallbacksPattern = null;
			this.effectName = null;
		}
	}
	
	public EffectFunction getStartFunction() {
		return startFunction;
	}
	
	public void findStartCallback()	{
		Script script = getScript();
		for (EffectFunction f : script.functions(EffectFunction.class)) {
			if (f.getHardcodedCallbackType() != HardcodedCallbackType.Start)
				continue;
			if (this.hardcodedCallbackType != null) {
				// compare by known effect name
				if (this.effectName.equals(f.effectName)) {
					this.setStartFunction(f);
					break;
				}
			}
			else {
				if (f.getHardcodedCallbackType() == HardcodedCallbackType.Start) {
					// look if this name matches Fx<EffectName>(.*)
					Matcher m = f.getAdditionalCallbacksPattern().matcher(name);
					if (m.matches()) {
						this.setStartFunction(f);
						break;
					}
				}
			}
		}
	}
	
	public IType getEffectType() {
		synchronized (parameters) {
			return parameters.size() >= 2 ? parameters.get(1).getType() : PrimitiveType.PROPLIST;
		}
	}

}
