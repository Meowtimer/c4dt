package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Arrays;

public final class IncludesParameters {

	public final Script script, origin;

	public final int options;

	@Override
	public boolean equals(final Object o) {
		final IncludesParameters other = as(o, IncludesParameters.class);
		return (
			other != null &&
			script == other.script &&
			origin == other.origin &&
			options == other.options
		);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new int[] {script.hashCode(), origin != null ? origin.hashCode() : 0, options});
	}

	public IncludesParameters(final Script script, final Script origin, final int options) {
		this.script = script;
		this.origin = origin;
		this.options = options;
	}
}