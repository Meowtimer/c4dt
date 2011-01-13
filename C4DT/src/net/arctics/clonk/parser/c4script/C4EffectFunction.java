package net.arctics.clonk.parser.c4script;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;

/**
 * One of the Fx* functions
 * @author madeen
 *
 */
public class C4EffectFunction extends C4Function {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	public static final String FUNCTION_NAME_FORMAT = "Fx%s%s"; //$NON-NLS-0$
	
	public enum Type {
		Start,
		Timer,
		Stop;
		
		Pattern pattern;
		
		private Type() {
			pattern = Pattern.compile(String.format(FUNCTION_NAME_FORMAT, "(.*?)", name()+"$"));
		}
		
		public Pattern getPattern() {
			return pattern;
		}
	}
	
	private Map<Type, C4EffectFunction> relatedFunctions = new HashMap<Type, C4EffectFunction>();
	private Type type;
	private String effectName;
	
	public C4EffectFunction getRelatedEffectFunction(Type type) {
		return relatedFunctions.get(type);
	}

	public Type getEffectFunctionType() {
		return type;
	}
	
	public String getEffectName() {
		return effectName;
	}
	
	public void findRelatedEffectFunctions()	{
		relatedFunctions.clear();
		
		// get own type
		type = null;
		for (Type t : Type.values()) {
			Matcher matcher = t.getPattern().matcher(getName());
			if (matcher.matches()) {
				type = t;
				relatedFunctions.put(t, this);
				effectName = matcher.group(1);
				break;
			}
		}
		assert(type != null);
		
		// get related functions
		C4ScriptBase script = getScript();
		for (Type t : Type.values()) {
			if (t != type) {
				C4Function relatedFunc = script.findFunction(String.format(FUNCTION_NAME_FORMAT, effectName, t.name()));
				if (relatedFunc instanceof C4EffectFunction) {
					relatedFunctions.put(t, (C4EffectFunction) relatedFunc);
				}
			}
		}
	}

}
