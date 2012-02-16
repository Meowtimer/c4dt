package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniDataBase;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.util.Utilities;

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
	
	public static void save(Writer writer , Object obj, Object defaults) throws IOException, IllegalArgumentException, IllegalAccessException {
		CustomIniUnit unit = new CustomIniUnit(configurationForClass(obj.getClass()), obj, defaults);
		unit.save(writer);
	}
	
	public CustomIniUnit(IniConfiguration configuration, Object object, Object defaults) throws IllegalArgumentException, IllegalAccessException {
		this("", configuration); //$NON-NLS-1$
		assert(defaults == null || object.getClass() == defaults.getClass());
		for (Field f : object.getClass().getFields()) {
			IniField annot;
			if ((annot = f.getAnnotation(IniField.class)) != null) {
				if (defaults != null && Utilities.objectsEqual(f.get(object), f.get(defaults)))
					continue;
				IniDataSection dataSection = configuration().getSections().get(annot.category());
				if (dataSection != null) {
					IniDataBase dataItem = dataSection.getEntry(f.getName());
					if (dataItem instanceof IniDataEntry) {
						IniDataEntry entry = (IniDataEntry) dataItem;
						Constructor<?> ctor;
						Object value = f.getType() == entry.entryClass() ? f.get(object) : null;
						if (value == null) {
							try {
								ctor = entry.entryClass().getConstructor(f.getType());
							} catch (SecurityException e) {
								ctor = null;
							} catch (NoSuchMethodException e) {
								ctor = null;
							}
							if (ctor != null) {
								try {
									value = ctor.newInstance(f.get(object));
								} catch (Exception e) {
									value = null;
								}
							}
						}
						if (value != null) {
							IniSection section = this.requestSection(annot.category(), dataSection);
							ComplexIniEntry complEntry = new ComplexIniEntry(0, 0, f.getName(), value);
							complEntry.setEntryConfig(entry);
							section.putEntry(complEntry);
						}
					}
				}
			}
		}
	}

	public void commitTo(Object object) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		for (IniSection section : this.sections()) {
			commitSection(object, section, true);
		}
	}

	public void commitSection(Object object, IniSection section, boolean takeIntoAccountCategory) {
		for (IniItem item : section.subItemMap().values()) {
			if (item instanceof IniSection) {
				commitSection(object, (IniSection)item, takeIntoAccountCategory);
			} else if (item instanceof IniEntry) {
				IniEntry entry = (IniEntry) item;
				Field f;
				try {
					f = object.getClass().getField(entry.name());
				} catch (Exception e) {
					// don't panic - probably unknown field
					//e.printStackTrace();
					continue;
				}
				IniField annot;
				if (f != null && (annot = f.getAnnotation(IniField.class)) != null && (!takeIntoAccountCategory || annot.category().equals(section.name()))) {
					Object val = entry.value();
					if (val instanceof IConvertibleToPrimitive)
						val = ((IConvertibleToPrimitive)val).convertToPrimitive();
					try {
						if (f.getType() != String.class && val instanceof String)
							setFromString(f, object, (String)val);
						else
							f.set(object, val);
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		}
	}
	
	private void setFromString(Field f, Object object, String val) throws NumberFormatException, IllegalArgumentException, IllegalAccessException {	
		if (f.getType() == Integer.TYPE)
			f.set(object, Integer.valueOf(val));
		else if (f.getType() == Long.TYPE)
			f.set(object, Long.valueOf(val));
		else if (f.getType() == java.lang.Boolean.TYPE)
			f.set(object, java.lang.Boolean.valueOf(val));
	}

	public void parseAndCommitTo(Object obj) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		parser().parse(false);
		commitTo(obj);
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
}
