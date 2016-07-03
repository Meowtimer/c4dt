package net.arctics.clonk.ini;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ini.IniData.IniConfiguration;

public class CustomIniUnit extends IniUnit {

	private static final ConcurrentHashMap<Class<?>, IniConfiguration> GENERATED_CONFIGURATIONS = new ConcurrentHashMap<Class<?>, IniConfiguration>();

	private IniConfiguration configuration;

	@Override
	public IniConfiguration configuration() {
		return configuration;
	}

	public void setConfiguration(final IniConfiguration configuration) {
		this.configuration = configuration;
	}

	public CustomIniUnit(final Object input) {super(input);}

	public CustomIniUnit(final Object input, final IniConfiguration configuration) {
		super(input);
		this.setConfiguration(configuration);
	}

	public static IniConfiguration configurationForClass(final Class<?> cls) {
		return GENERATED_CONFIGURATIONS.computeIfAbsent(cls, IniConfiguration::createFromClass);
	}

	public static CustomIniUnit load(final Object input, final Object into) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		final CustomIniUnit result = new CustomIniUnit(input);
		result.setConfiguration(configurationForClass(into.getClass()));
		result.parseAndCommitTo(into);
		return result;
	}

	public static void save(final ASTNodePrinter writer, final Object obj, final Object defaults) throws IOException, IllegalArgumentException, IllegalAccessException {
		final CustomIniUnit unit = new CustomIniUnit(configurationForClass(obj.getClass()), obj, defaults);
		unit.save(writer, false);
	}

	public CustomIniUnit(final IniConfiguration configuration, final Object object, final Object defaults) throws IllegalArgumentException, IllegalAccessException {
		this("", configuration); //$NON-NLS-1$
		assert(defaults == null || object.getClass() == defaults.getClass());
		readObjectFields(object, defaults);
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
}
