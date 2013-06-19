package net.arctics.clonk.builder;

import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;

@Capabilities(capabilities=Capabilities.ISSUES|Capabilities.TYPING)
final class NullProblemReportingStrategy extends ProblemReportingStrategy {
	@Override
	public void run() {}
}