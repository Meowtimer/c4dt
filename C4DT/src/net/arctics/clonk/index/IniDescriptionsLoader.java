package net.arctics.clonk.index;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.parser.c4script.IHasName;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
import net.arctics.clonk.parser.inireader.IEntryFactory;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;
import net.arctics.clonk.parser.inireader.IniData.IniSectionDefinition;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniParserException;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.IHasUserDescription;
import net.arctics.clonk.util.IStorageLocation;

public class IniDescriptionsLoader {
	private class DescriptionsIniConfiguration extends IniConfiguration {
		public DescriptionsIniConfiguration() {
			super();
			sections.put("Descriptions", new IniSectionDefinition() { //$NON-NLS-1$
				private final IniEntryDefinition entry = new IniEntryDefinition("", String.class); //$NON-NLS-1$

				@Override
				public boolean hasEntry(String entryName) {
					return engine.findDeclaration(entryName) != null;
				}

				@Override
				public IniEntryDefinition entryForKey(String key) {
					return entry;
				}
			});
			factory = new IEntryFactory() {
				@Override
				public Object create(Class<?> type, String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
					return value;
				}
			};
		}
	}

	private final Engine engine;
	private transient Map<String, Map<String, String>> descriptions = new HashMap<String, Map<String, String>>();

	public IniDescriptionsLoader(Engine engine) {
		this.engine = engine;
	}

	public <T extends IHasUserDescription & IHasName> String descriptionFor(T declaration) {
		Map<String, String> descs;
		try {
			descs = loadDescriptions(ClonkPreferences.languagePref());
			return descs != null ? descs.get(declaration.name()) : null;
		} catch (IOException e) {
			return null;
		}
	}

	public Map<String, String> loadDescriptions(String language) throws IOException {
		Map<String, String> result = descriptions.get(language);
		if (result != null)
			return result;
		else {
			String fileName = String.format("descriptions%s.ini", language); //$NON-NLS-1$
			IStorageLocation[] storageLocations = engine.storageLocations();
			for (int i = storageLocations.length - 1; i >= 0; i--) {
				IStorageLocation loc = storageLocations[i];
				URL descs = loc.locatorForEntry(fileName, false);
				if (descs != null) {
					InputStream input = descs.openStream();
					try {
						IniUnit unit = new CustomIniUnit(input, new DescriptionsIniConfiguration());
						unit.parser().parse(false);
						IniSection section = unit.sectionWithName("Descriptions", false); //$NON-NLS-1$
						if (section != null) {
							result = new HashMap<String, String>();
							for (Entry<String, IniItem> item : section.subItemMap().entrySet())
								if (item.getValue() instanceof IniEntry) {
									IniEntry entry = (IniEntry) item.getValue();
									result.put(entry.key(), entry.stringValue().replace("|||", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
								}
							descriptions.put(language, result);
							return result;
						}
					} finally {
						input.close();
					}
				}
			}
		}
		return null;
	}
}
