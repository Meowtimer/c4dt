package net.arctics.clonk.c4script.cpp;

import java.util.HashSet;
import java.util.stream.Stream;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Directive.DirectiveType;
import net.arctics.clonk.c4script.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.index.Index;

class DeclarationsStreamer {
	private final Index index;
	private final HashSet<Script> recursionCatcher;
	private final Script origin;
	public DeclarationsStreamer(Index index, Script origin) {
		this.index = index;
		this.recursionCatcher = new HashSet<>();
		this.origin = origin;
	}
	boolean enter(Script script) {
		synchronized (recursionCatcher) {
			return recursionCatcher.add(script);
		}
	}
	public <D extends Declaration> Stream<D> declarations(Script script, java.util.function.Function<Script, Stream<D>> decsSup) {
		final java.util.function.Function<Script, Stream<D>> flatten =
			s -> enter(s) ? declarations(s, decsSup) : Stream.empty();
		return Stream.concat(
			Stream.concat(
				script.includes(index, origin, GatherIncludesOptions.NoAppendages).stream()
					.flatMap(flatten),
				decsSup.apply(script)
			),
			script.directives().stream()
				.filter(d -> d.type() == DirectiveType.APPENDTO)
				.map(d -> index.definitionNearestTo(origin.resource(), d.contentAsID()))
				.filter(def -> def != null)
				.flatMap(flatten)
		);
	}
}