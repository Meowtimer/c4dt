package net.arctics.clonk.ui.editors.c4script;

import java.util.stream.Stream;

import net.arctics.clonk.Problem;
import net.arctics.clonk.ui.editors.c4script.ProblemFixer;
import net.arctics.clonk.util.Pair;

import static java.util.Arrays.stream;

class Problems {
	final Problem[] problems;
	Problems(Problem[] problems) {
		super();
		this.problems = problems;
	}
	public Stream<Pair<Problem, ProblemFixer>> fixedBy(ProblemFixer fixer) {
		return stream(problems).map(p -> Pair.pair(p, fixer));
	}
}
