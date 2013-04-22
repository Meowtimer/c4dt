package net.arctics.clonk.ini;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.ini.IniData.IniConfiguration;
import net.arctics.clonk.parser.ASTNodePrinter;

public class CustomIniUnit extends IniUnit {
	
	private static final Map<Class<?>, IniConfiguration> GENERATED_CONFIGURATIONS = new HashMap<Class<?>, IniConfiguration>();

	private IniConfiguration configuration;
	
	@Override
	public IniConfiguration configuration() {
		return configuration;
	}

	public void setConfiguration(IniConfiguration configuration) {
		this.configuration = configuration;
	}

	public CustomIniUnit(Object input) {super(input);}
	
	public CustomIniUnit(Object input, IniConfiguration configuration) {
		super(input);
		this.setConfiguration(configuration);
	}
	
	public static IniConfiguration configurationForClass(Class<?> cls) {
		IniConfiguration result = GENERATED_CONFIGURATIONS.get(cls);
		if (result == null) {
			result = IniConfiguration.createFromClass(cls);
			GENERATED_CONFIGURATIONS.put(cls, result);
		}
		return result;
	}
	
	public static CustomIniUnit load(Object input, Object into) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		CustomIniUnit result = new CustomIniUnit(input);
		result.setConfiguration(configurationForClass(into.getClass()));
		result.parseAndCommitTo(into);
		return result;
	}
	
	public static void save(ASTNodePrinter writer, Object obj, Object defaults) throws IOException, IllegalArgumentException, IllegalAccessException {
		CustomIniUnit unit = new CustomIniUnit(configurationForClass(obj.getClass()), obj, defaults);
		unit.save(writer, false);
	}
	
	public CustomIniUnit(IniConfiguration configuration, Object object, Object defaults) throws IllegalArgumentException, IllegalAccessException {
		this("", configuration); //$NON-NLS-1$
		assert(defaults == null || object.getClass() == defaults.getClass());
		readObjectFields(object, defaults);
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
}
