package net.arctics.clonk.builder;


import static java.util.Arrays.stream;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.EngineSettings;
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
	
	private EngineSettings customEngineSettings;
	
	/**
	 * Custom settings for this project
	 */
	public EngineSettings customEngineSettings() {
		return customEngineSettings;
	}
	
	public void customEngineSettings(EngineSettings engineSettings) {
		this.customEngineSettings = engineSettings;
	}

	public ProjectSettings() {}

	public Engine engine() {
		return defaulting(
			cachedEngine,
			() ->
				// engineName can be "" or null since that is handled by loadEngine
				cachedEngine = defaulting(
					Core.instance().loadEngine(engineName),
					Core.instance().activeEngine()
				)
		);
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

	@SuppressWarnings("unchecked")
	public Collection<ProblemReportingStrategyInfo> problemReportingStrategies() {
		return defaulting(
			_problemReportingStrategies,
			() -> {
				final Pattern strategyRefPattern = Pattern.compile("(.*?)(\\[(.*?)\\])?");
				final String[] classNames = problemReportingStrategies != null ? problemReportingStrategies.split(",") : new String[0];
				final List<ProblemReportingStrategyInfo> strategies = stream(classNames)
					.map(strategyRefPattern::matcher)
					.filter(Matcher::matches)
					.map(matcher -> {
						final String className = matcher.group(1);
						final String args = matcher.group(3);
						try {
							final Class<? extends ProblemReportingStrategy> cls = (Class<? extends ProblemReportingStrategy>) ProblemReportingStrategy.class.getClassLoader().loadClass(Core.id(className));
							final ProblemReportingStrategyInfo result = ProblemReportingStrategy.class.isAssignableFrom(cls)
								? new ProblemReportingStrategyInfo(cls, args)
								: null;
							return result;
						} catch (final ClassNotFoundException e) {
							System.out.println(e.getMessage());
							return null;
						}
					})
					.filter(x -> x != null)
					.collect(Collectors.toList());
				if (strategies.isEmpty()) {
					strategies.add(new ProblemReportingStrategyInfo(DabbleInference.class, ""));
				}
				return _problemReportingStrategies = strategies;
			}
		);
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