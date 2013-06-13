package net.arctics.clonk.index;

import static net.arctics.clonk.util.Utilities.as;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.ini.IniEntry;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.ini.IniUnitParser;
import net.arctics.clonk.util.IStorageLocation;

public class IniDescriptionsLoader {

	private final Engine engine;

	public IniDescriptionsLoader(Engine engine) { this.engine = engine; }

	public void loadDescriptions(String language) {
		final String fileName = String.format("Descriptions%s.ini", language); //$NON-NLS-1$
		final IStorageLocation[] storageLocations = engine.storageLocations();
		for (int i = storageLocations.length - 1; i >= 0; i--) {
			final IStorageLocation loc = storageLocations[i];
			final URL descs = loc.locatorForEntry(fileName, false);
			if (descs != null)
				try (InputStream input = descs.openStream()) {
					final IniUnit unit = new IniUnit(input);
					try {
						final IniUnitParser parser = new IniUnitParser(unit);
						parser.parse(false);
					} catch (final ProblemException e) {
						e.printStackTrace();
					}
					final IniSection functions = unit.sectionWithName("Functions", false); //$NON-NLS-1$
					if (functions != null)
						for (final Function f : engine.functions()) {
							final IniSection sec = as(functions.itemByKey(f.name()), IniSection.class);
							if (sec != null) {
								final IniEntry de = as(sec.itemByKey("Description"), IniEntry.class);
								if (de != null)
									f.setUserDescription(de.value().toString());
								for (final Variable p : f.parameters()) {
									final IniSection ps = as(sec.itemByKey(p.name()), IniSection.class);
									if (ps != null) {
										final IniEntry dp = as(ps.itemByKey("Description"), IniEntry.class);
										if (dp != null)
											if (dp.value() != null)
												p.setUserDescription(dp.value().toString());
									}
								}
							}
						}
					final IniSection variables = unit.sectionWithName("Variables", false); //$NON-NLS-1$
					if (variables != null)
						for (final Variable v : engine.variables()) {
							final IniSection sec = as(variables.itemByKey(v.name()), IniSection.class);
							if (sec != null) {
								final IniEntry de = as(sec.itemByKey("Description"), IniEntry.class);
								if (de != null)
									v.setUserDescription(de.value().toString());
							}
						}
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
		}
	}
}
