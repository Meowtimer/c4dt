package net.arctics.clonk.parser.inireader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniDataBase;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.util.Utilities;

public class CustomIniUnit extends IniUnit {

	private IniConfiguration configuration;
	
	public IniConfiguration getConfiguration() {
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
	
	public CustomIniUnit(IniConfiguration configuration, Object object, Object defaults) throws IllegalArgumentException, IllegalAccessException {
		this("", configuration); //$NON-NLS-1$
		assert(defaults == null || object.getClass() == defaults.getClass());
		for (Field f : object.getClass().getFields()) {
			IniField annot;
			if ((annot = f.getAnnotation(IniField.class)) != null) {
				if (Utilities.objectsEqual(f.get(object), f.get(defaults)))
					continue;
				IniDataSection dataSection = getConfiguration().getSections().get(annot.category());
				if (dataSection != null) {
					IniDataBase dataItem = dataSection.getEntry(f.getName());
					if (dataItem instanceof IniDataEntry) {
						IniDataEntry entry = (IniDataEntry) dataItem;
						Constructor<?> ctor;
						Object value = f.getType() == entry.getEntryClass() ? f.get(object) : null;
						if (value == null) {
							try {
								ctor = entry.getEntryClass().getConstructor(f.getType());
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
		for (IniSection section : this.getSections()) {
			commitSection(object, section, true);
		}
	}

	public void commitSection(Object object, IniSection section, boolean takeIntoAccountCategory) throws NoSuchFieldException, IllegalAccessException {
		for (IniItem item : section.getSubItemMap().values()) {
			if (!(item instanceof IniEntry))
				continue;
			IniEntry entry = (IniEntry) item;
			Field f = object.getClass().getField(entry.getName());
			IniField annot;
			if (f != null && (annot = f.getAnnotation(IniField.class)) != null && (!takeIntoAccountCategory || annot.category().equals(section.getName()))) {
				Object val = entry.getValueObject();
				if (val instanceof IConvertibleToPrimitive)
					val = ((IConvertibleToPrimitive)val).convertToPrimitive();
				if (f.getType() != String.class && val instanceof String)
					setFromString(f, object, (String)val);
				else
					f.set(object, val);
			}
		}
	}
	
	private void setFromString(Field f, Object object, String val) throws NumberFormatException, IllegalArgumentException, IllegalAccessException {	
		if (f.getType() == Integer.TYPE)
			f.set(object, Integer.valueOf(val));
		else if (f.getType() == java.lang.Boolean.TYPE)
			f.set(object, java.lang.Boolean.valueOf(val));
	}

	public void parseAndCommitTo(Object obj) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		parse(false);
		commitTo(obj);
	}

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
}
