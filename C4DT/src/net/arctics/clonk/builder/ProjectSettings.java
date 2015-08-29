package net.arctics.clonk.builder;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.ini.IniField;
import net.arctics.clonk.util.SettingsBase;

public class ProjectSettings extends SettingsBase {

	/** Name of engine to use for this project */
	@IniField
	public String engineName;
	/** String containing list of {@link Problem}s that will be disabled when building the project */
	@IniField
	public String disabledErrors;
	/** Typing mode for this project. */
	@IniField
	public Typing typing = Typing.INFERRED;
	/** Typing this project is in the process of being migrated to */
	@IniField(category="Migration")
	public Typing migrationTyping = null;
	@IniField
	public String problemReportingStrategies;

	private Engine cachedEngine;

	public ProjectSettings() {}

	public synchronized Engine engine() {
		if (cachedEngine == null) {
			// engineName can be "" or null since that is handled by loadEngine
			cachedEngine = Core.instance().loadEngine(engineName);
			if (cachedEngine == null) {
				cachedEngine = Core.instance().activeEngine();
			}
		}
		return cachedEngine;
	}

	public static final class ProblemReportingStrategyInfo {
		public ProblemReportingStrategyInfo(final Class<? extends ProblemReportingStrategy> cls, final String args) {
			super();
			this.cls = cls;
			this.args = args;
		}
		public final Class<? extends ProblemReportingStrategy> cls;
		public final String args;
	}

	private Collection<ProblemReportingStrategyInfo> _problemReportingStrategies;

	public Collection<ProblemReportingStrategyInfo> problemReportingStrategies() {
		if (_problemReportingStrategies == null) {
			final Collection<ProblemReportingStrategyInfo> strats = new ArrayList<ProblemReportingStrategyInfo>();
			if (problemReportingStrategies != null && problemReportingStrategies.length() > 0) {
				final Matcher strategyRefMatcher = Pattern.compile("(.*?)(\\[(.*?)\\])?").matcher("");
				final String[] classNames = problemReportingStrategies.split(",");
				for (final String strategyRef : classNames) {
					if (strategyRefMatcher.reset(strategyRef).matches()) {
						final String className = strategyRefMatcher.group(1);
						final String args = strategyRefMatcher.group(3);
						try {
							@SuppressWarnings("unchecked")
							final Class<? extends ProblemReportingStrategy> cls = (Class<? extends ProblemReportingStrategy>) ProblemReportingStrategy.class.getClassLoader().loadClass(Core.id(className));
							if (ProblemReportingStrategy.class.isAssignableFrom(cls)) {
								strats.add(new ProblemReportingStrategyInfo(cls, args));
							}
						} catch (final ClassNotFoundException e) {
							e.printStackTrace();
							continue;
						}
					}
				}
			}
			if (strats.isEmpty()) {
				strats.add(new ProblemReportingStrategyInfo(DabbleInference.class, ""));
			}
			return _problemReportingStrategies = strats;
		}
		return _problemReportingStrategies;
	}

	public String disabledErrorsString() { return disabledErrors; }
	public String engineName() { return engineName; }

	public void setEngineName(final String engineName) {
		this.engineName = engineName;
		cachedEngine = null;
	}

	public void guessValues(final ClonkProjectNature nature) {
		guessEngine(nature);
	}

	private void guessEngine(final ClonkProjectNature nature) {
		final List<IProject> referencingProjects = nature.referencingClonkProjects();
		final Map<String, Integer> score = new HashMap<String, Integer>();
		for (final String engine : Core.instance().namesOfAvailableEngines()) {
			score.put(engine, 0);
		}
		for (final IProject proj : referencingProjects) {
			final String projName = proj.getName();
			final String engine =
				projName.equalsIgnoreCase("OPENCLONK") || projName.equalsIgnoreCase("OC") ? "OpenClonk" :
				projName.equalsIgnoreCase("ClonkRage") || projName.equalsIgnoreCase("CR") ? "ClonkRage" :
				null;
			if (engine != null) {
				score.put(engine, score.get(engine)+1);
			}
		}
		Entry<String, Integer> best = null;
		for (final Entry<String, Integer> entry : score.entrySet()) {
			if (best == null || entry.getValue() > best.getValue()) {
				best = entry;
			}
		}
		setEngineName(best.getKey());
	}

	public void concludeTypingMigration() {
		typing = migrationTyping;
		migrationTyping = null;
	}
}