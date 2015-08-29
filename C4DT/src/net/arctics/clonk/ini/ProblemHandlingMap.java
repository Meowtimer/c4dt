package net.arctics.clonk.ini;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.StringUtil.blockString;
import static net.arctics.clonk.util.Utilities.block;
import static net.arctics.clonk.util.Utilities.walk;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.arctics.clonk.Problem;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodeMatcher;
import net.arctics.clonk.c4script.Standalone;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.ini.IniData.IniConfiguration;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.ini.IniData.IniSectionDefinition;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Tagged;

public class ProblemHandlingMap implements IniUnit.INode<ProblemHandlingMap> {
	public enum Handling {
		Hide,
		TurnIntoWarning,
		Show,
		TurnIntoError
	}
	public final ASTNode[] patterns;
	public final Map<Problem, Handling> map;
	public final ProblemHandlingMap[] nested;
	public ProblemHandlingMap(ASTNode[] patterns, Map<Problem, Handling> map, ProblemHandlingMap... nested) {
		super();
		this.patterns = patterns;
		this.map = map;
		this.nested = nested;
	}
	@Override
	public Stream<ProblemHandlingMap> elements() {
		return stream(nested);
	}
	@Override
	public Stream<Pair<String, Object>> attributes() {
		return map.entrySet().stream().map(e -> Pair.pair(e.getKey().name(), e.getValue().name()));
	}
	@Override
	public String name() {
		return blockString("", "", ", ", stream(patterns));
	}
	public IniUnit toIniUnit() {
		return IniUnit.from(this, m -> m);
	}
	static class WithParent extends Tagged<ProblemHandlingMap, WithParent> {
		public WithParent(ProblemHandlingMap item, WithParent tag) {
			super(item, tag);
		}
	}
	public Stream<WithParent> recursiveNested(WithParent parent) {
		final WithParent me = new WithParent(this, parent);
		return Stream.concat(
			Stream.of(me),
			stream(nested).flatMap(n -> n.recursiveNested(me))
		);
	}
	public Stream<ProblemHandlingMap> find(ASTNode node) {
		class Racer {
			final WithParent map;
			WithParent current;
			int patternIndex;
			Racer(WithParent map) {
				super();
				this.map = map;
				this.current = map;
				this.patternIndex = map.item.patterns.length-1;
			}
			ProblemHandlingMap map() { return map.item; }
			void test(ASTNode node) {
				if (current == null) {
					return;
				}
				while (patternIndex > -1 && current.item.patterns[patternIndex].match(node) != null) {
					patternIndex--;
				}
				if (patternIndex == -1) {
					current = current.tag;
					if (current != null) {
						patternIndex = current.item.patterns.length-1;
					}
				}
			}
			boolean finished() { return current == null; }
		}
		final List<Racer> racers = stream(nested).flatMap(n -> n.recursiveNested(null))
			.map(Racer::new).collect(Collectors.toList());
		walk(node, ASTNode::parent).forEach(n -> racers.forEach(r -> r.test(n)));
		return racers.stream().filter(Racer::finished).map(Racer::map);
	}

	public static ProblemHandlingMap from(Engine engine, IniUnit.INode<ProblemHandlingMap> n) {
		final List<ASTNode> patternNodes = parseNodesList(new Standalone(engine).parser(n.name()));
		final Map<Problem, ProblemHandlingMap.Handling> map = n.attributes().collect(Collectors.toMap(
			p -> Problem.valueOf(p.first()),
			p -> ProblemHandlingMap.Handling.valueOf(p.second().toString())
		));
		return new ProblemHandlingMap(
			patternNodes.toArray(new ASTNode[patternNodes.size()]),
			map,
			n.elements().map(e -> from(engine, e)).collect(Collectors.toList()).toArray(new ProblemHandlingMap[0])
		);
	}

	private static List<ASTNode> parseNodesList(final Standalone.Parser p) {
		final List<ASTNode> patternNodes = new LinkedList<>();
		while (!p.reachedEOF()) {
			final ASTNode pat = p.parseNode();
			if (pat == null) {
				break;
			}
			patternNodes.add(ASTNodeMatcher.prepareForMatching(pat));
			p.eatWhitespace();
			if (p.peek() == ',') {
				p.read();
			}
		}
		return patternNodes;
	}

	public static class Unit extends IniUnit {
		private static final long serialVersionUID = 1L;
		public Unit(Object input) { super(input); }
		static final IniConfiguration CONFIGURATION;
		static final IniSectionDefinition SECTION;
		static {
			CONFIGURATION = new IniConfiguration();
			SECTION = new IniSectionDefinition();
			final Map<String, Integer> enumValues = stream(Handling.values()).collect(Collectors.toMap(h -> h.name(), h -> h.ordinal()));
			stream(Problem.values()).forEach(p -> SECTION.entries().put(p.name(), block(() -> {
				final IniEntryDefinition def = new IniEntryDefinition(p.name(), Enum.class);
				def.enumValues = enumValues;
				return def;
			})));
			//CONFIGURATION.sections.put("", SECTION);
		}
		@Override
		public IniConfiguration configuration() {
			return CONFIGURATION;
		}
		@Override
		protected IniSectionDefinition sectionDataFor(IniSection s, IniSection p) {
			return SECTION;
		}
	}
}
