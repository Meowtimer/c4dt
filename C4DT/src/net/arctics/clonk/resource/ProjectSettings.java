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

	/** Static Typing mode */
	public enum StaticTyping {
		/** No Static Typing */
		Off,
		/** No Static Typing yet, but migrating */
		Migrating,
		/** Statically typed */
		On
	}
	
	/** Name of engine to use for this project */
	@IniField
	public String engineName;
	/** String containing list of {@link ParserErrorCode}s that will be disabled when building the project */
	@IniField
	public String disabledErrors;
	/** Static typing mode for this project. */
	@IniField
	public StaticTyping staticTyping = StaticTyping.Off;
	
	private Engine cachedEngine;
	private HashSet<ParserErrorCode> disabledErrorsSet;
	
	public ProjectSettings() {
	}
	
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