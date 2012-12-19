package net.arctics.clonk.resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.inireader.IniField;
import net.arctics.clonk.util.SettingsBase;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IProject;

public class ProjectSettings extends SettingsBase {

	/** Typing mode */
	public enum Typing {
		/** Static typing completely disabled. No parameter annotations allowed. */
		Dynamic,
		/** Migrating to dynamic typing where no type annotations exist at all. **/
		MigratingToDynamic,
		/** Allow type annotations for parameters, as the engine does. */
		ParametersOptionallyTyped,
		/** No Static Typing yet, but migrating. Allow type annotations but do not require them. */
		MigratingToStatic,
		/** Statically typed */
		Static;

		public boolean allowsNonParameterAnnotations() {
			switch (this) {
			case MigratingToStatic: case Static:
				return true;
			default:
				return false;
			}
		}
	}
	
	/** Name of engine to use for this project */
	@IniField
	public String engineName;
	/** String containing list of {@link ParserErrorCode}s that will be disabled when building the project */
	@IniField
	public String disabledErrors;
	/** Typing mode for this project. */
	@IniField
	public Typing typing = Typing.ParametersOptionallyTyped;
	
	private Engine cachedEngine;
	private HashSet<ParserErrorCode> disabledErrorsSet;
	
	public ProjectSettings() {}
	
	public Engine engine() {
		if (cachedEngine == null) {
			// engineName can be "" or null since that is handled by loadEngine
			cachedEngine = Core.instance().loadEngine(engineName);
			if (cachedEngine == null)
				cachedEngine = Core.instance().activeEngine();
		}
		return cachedEngine;
	}
	
	public HashSet<ParserErrorCode> getDisabledErrorsSet() {
		if (disabledErrorsSet == null) {
			disabledErrorsSet = new HashSet<ParserErrorCode>();
			if (!disabledErrors.equals("")) {
				String ds[] = disabledErrors.split(",");
				for (String d : ds)
					try {
						disabledErrorsSet.add(ParserErrorCode.valueOf(d));
					} catch (IllegalArgumentException e) {
						System.out.println("Unknown parser error: " + d);
					}
			}
		}
		return disabledErrorsSet;
	}
	
	public void setDisabledErrors(String disabledErrors) {
		this.disabledErrors = disabledErrors;
		disabledErrorsSet = null;
	}
	
	public void setDisabledErrorsSet(HashSet<ParserErrorCode> errorCodes) {
		this.disabledErrorsSet = errorCodes;
		if (errorCodes != null)
			this.disabledErrors = StringUtil.writeBlock(null, "", "", ",", errorCodes);
	}

	public String getDisabledErrors() {
		return disabledErrors;
	}

	public String getEngineName() {
		return engineName;
	}
	
	public void setEngineName(String engineName) {
		this.engineName = engineName;
		cachedEngine = null;
	}

	public void guessValues(ClonkProjectNature nature) {
		guessEngine(nature);
	}

	private void guessEngine(ClonkProjectNature nature) {
		List<IProject> referencingProjects = nature.referencingClonkProjects();
		Map<String, Integer> score = new HashMap<String, Integer>();
		for (String engine : Core.instance().namesOfAvailableEngines())
			score.put(engine, 0);
		for (IProject proj : referencingProjects) {
			String projName = proj.getName();
			String engine;
			if (projName.equalsIgnoreCase("OPENCLONK") || projName.equalsIgnoreCase("OC"))
				engine = "OpenClonk";
			else if (projName.equalsIgnoreCase("ClonkRage") || projName.equalsIgnoreCase("CR"))
				engine = "ClonkRage";
			else
				continue;
			score.put(engine, score.get(engine)+1);
		}
		Entry<String, Integer> best = null;
		for (Entry<String, Integer> entry : score.entrySet())
			if (best == null || entry.getValue() > best.getValue())
				best = entry;
		setEngineName(best.getKey());
	}
}