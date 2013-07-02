package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.Flags.OUTPUTDABBLEGRAPH;
import static net.arctics.clonk.util.StringUtil.blockString;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.multiply;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input.Visit;
import net.arctics.clonk.util.TaskExecution;

@SuppressWarnings("serial")
class Plan extends LinkedList<Runnable> {

	class VisitsByName extends LinkedList<Visit> implements Runnable {
		@Override
		public void run() {
			for (final Visit v : this)
				v.prepare();
		}
	}

	private final DabbleInference inference;
	private final Map<String, VisitsByName> visits = new HashMap<>();
	private final Set<Visit> doubleTakes = new HashSet<>();

	void populateVisitsMap() {
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values()) {
				VisitsByName list = visits.get(v.function.name());
				if (list == null) {
					list = new VisitsByName();
					visits.put(v.function.name(), list);
				}
				list.add(v);
			}
	}

	static boolean requires(Visit testDependent, Visit testRequirement, Set<Visit> catcher) {
		if (!catcher.add(testDependent))
			return false;
		if (testDependent.requirements.contains(testRequirement))
			return true;
		else for (final Visit td_ : testDependent.requirements)
			if (requires(td_, testRequirement, catcher))
				return true;
		return false;
	}

	void addRequirement(Visit dependent, Visit requirement) {
		if (requirement == dependent)
			return;
		if (!requires(requirement, dependent, new HashSet<Visit>())) {
			dependent.requirements.add(requirement);
			requirement.dependents.add(dependent);
		} else {
			if (DEBUG)
				System.out.println(String.format("Not adding requirement %s to %s", requirement.toString(), dependent.toString()));
			dependent.doubleTake = true;
			doubleTakes.add(dependent);
		}
	}

	IASTVisitor<Visit> resultUsedRequirementsDetector = new ResultUsedRequirementsDetector();

	final IASTVisitor<Visit> variableInitializationsRequirementsVisitor = new IASTVisitor<Visit>() {
		@Override
		public TraversalContinuation visitNode(ASTNode node, Visit v) {
			if (
				node instanceof AccessVar && node.predecessorInSequence() == null &&
				!(isLocal((AccessVar) node, v) || containedInAssignment(node))
			) {
				final AccessVar av = (AccessVar) node;
				for (final Script s : v.input().conglomerate) {
					final List<AccessVar> references = s.varReferences().get(av.name());
					if (references != null)
						for (final AccessVar ref : references)
							for (ASTNode p = ref.parent(); p != null; p = p.parent())
								if (p instanceof BinaryOp && ((BinaryOp)p).operator().isAssignment() && ref.containedIn(((BinaryOp)p).leftSide())) {
									final Function fun = ref.parent(Function.class);
									final Visit other = v.input().visits.get(fun);
									if (other != null)
										addRequirement(v, other);
									/*	final List<CallDeclaration> calls = s.callMap().get(fun.name());
										if (calls != null)
											for (final CallDeclaration cd : calls) {
												final Visit assignerCaller = inference.visitFor(cd.parentOfType(Function.class), s);
												if (assignerCaller != null)
													addRequirement(v, assignerCaller);
											} */
									break;
								}
				}
			}
			return TraversalContinuation.Continue;
		}
		private boolean containedInAssignment(ASTNode node) {
			for (ASTNode p = node.parent(); p != null; p = p.parent())
				if (p instanceof BinaryOp && ((BinaryOp)p).operator().isAssignment())
					return true;
			return false;
		}
		private boolean isLocal(AccessVar node, Visit v) {
			return v.function.findLocalDeclaration(node.name(), Variable.class) != null;
		}
	};

	void determineRequirements() {

		// callee -> caller if callee's result used
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values())
				v.function.traverse(resultUsedRequirementsDetector, v);

		// caller -> callee
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values()) {
				final int numParameters = v.function().numParameters();
				if (i.shouldTypeFromCalls(v.function)) {
					final List<CallDeclaration> calls = inference.index().callsTo(v.function.name());
					if (calls != null)
						for (final CallDeclaration call : calls) {
							if (call.params().length != numParameters)
								continue;
							final Function caller = call.parent(Function.class);
							final List<Visit> callerVisits = visits.get(caller.name());
							if (callerVisits != null)
								for (final Visit callerVisit : callerVisits)
									if (callerVisit.function == caller)
										addRequirement(v, callerVisit);
						}
				}
			}

		// functions containing initialization of used variable -> variable user
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values())
				v.function.traverse(variableInitializationsRequirementsVisitor, v);
	}

	class ResultUsedRequirementsDetector implements IASTVisitor<Visit> {
		@Override
		public TraversalContinuation visitNode(ASTNode node, Visit v) {
			if (node instanceof CallDeclaration && resultNeeded(node)) {
				final CallDeclaration cd = (CallDeclaration) node;
				final List<Visit> calledVisits = visits.get(cd.name());
				if (calledVisits != null)
					for (final Visit cv : calledVisits)
						addRequirement(v, cv);
			}
			return TraversalContinuation.Continue;
		}

		public boolean resultNeeded(final ASTNode node) {
			boolean resultUsed = false;
			for (ASTNode p = node.parent(), c = node; p != null; c = p, p = p.parent()) {
				if (p instanceof SimpleStatement)
					break;
				if (p instanceof Sequence && ((Sequence)p).lastElement() == c)
					continue;
				resultUsed = true;
				break;
			}
			return resultUsed;
		}
	}

	class Cluster extends HashSet<Visit> implements Runnable {

		StringBuilder log = DEBUG ? new StringBuilder() : null;

		void print(int depth, Collection<Visit> layer, Set<Visit> catcher) {
			for (final Visit v : layer) {
				System.out.println(String.format("%s-> %s", multiply("\t", depth), v.toString()));
				if (catcher.add(v))
					print(depth+1, v.dependents, catcher);
			}
		}
		void print() { print(0, this, new HashSet<Visit>()); }
		void run(Visit v, int depth) {
			if (log != null)
				log.append(String.format("%sVisiting %s\n", multiply("\t", depth), v.toString()));
			v.run();
			for (final Visit d : v.dependents)
				if (d.requirements.remove(v) && d.requirements.size() == 0)
					run(d, depth+1);
		}
		@Override
		public void run() {
			for (final Visit v : this)
				run(v, 0);
		}
		void find(Set<Visit> catcher, Visit start) {
			if (catcher.add(start)) {
				if (start.requirements.size() == 0)
					this.add(start);
				for (final Visit v : start.requirements)
					find(catcher, v);
				for (final Visit v : start.dependents)
					find(catcher, v);
			}
		}
		@Override
		public String toString() {
			return blockString("", "", ", ", this);
		}
		private void collect(Collection<Visit> col, Set<Visit> set) {
			for (final Visit v : col)
				if (set.add(v))
					collect(v.dependents, set);
		}
		public Set<Visit> flatten() {
			final Set<Visit> set = new HashSet<Visit>();
			collect(this, set);
			return set;
		}
	}

	void populate() {
		final List<Visit> roots = new LinkedList<>();
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values())
				if (v.requirements.size() == 0)
					if (v.dependents.size() == 0)
						this.add(v);
					else
						roots.add(v);
		while (roots.size() > 0) {
			final Visit root = roots.get(0);
			final Cluster c = new Cluster();
			c.find(new HashSet<Visit>(), root);
			roots.removeAll(c);
			this.add(c);
		}
	}

	void output() {
		class Output {
			{ new File("/Users/madeen/Desktop/DabbleInference").mkdirs(); }
			String randomColor() {
				final Random random = new Random();
				final char[] hex = "0123456789ABCDEF".toCharArray();
				final char[] chars = new char[6];
				for (int i = 0; i < 6; i++)
					chars[i] = hex[random.nextInt(hex.length)];
				return new String(chars);
			};
			void run() {
				int x = 0;
				for (final Runnable r : Plan.this) {
					final Cluster c = as(r, Cluster.class);
					if (c == null)
						continue;
					try (FileWriter writer = new FileWriter(new File(String.format("/Users/madeen/Desktop/DabbleInference/output%d.dot", ++x)))) {
						writer.append("digraph G {bgcolor=white\n");
						final Set<Visit> f = c.flatten();
						for (final Visit v : f)
							if (!v.requirements.isEmpty() || !v.dependents.isEmpty())
								writer.append(String.format("\tnode [ style=filled,shape=\"box\",fillcolor=\"antiquewhite:aquamarine\" ] \"%s\";\n", v.toString()));

						for (final Visit v : f)
							for (final Visit d : v.dependents) {
								final String col = randomColor();
								writer.append(String.format("\t\"%s\" -> \"%s\" [color=\"#%s\"];\n", v, d, col));
							}

						writer.append("}");
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		new Output().run();
	}

	Plan(DabbleInference inference) {
		this.inference = inference;
		populateVisitsMap();
		determineRequirements();
		populate();
		if (OUTPUTDABBLEGRAPH)
			output();
	}

	void run() {
		// prepare
		TaskExecution.threadPool(visits.values(), 3);
		// actual inference
		TaskExecution.threadPool(this, 3);
		if (DEBUG)
			for (final Runnable r : this)
				if (r instanceof Cluster) {
					final Cluster c = (Cluster)r;
					System.out.println(c.log.toString());
				}
		// double takes
		for (final Visit v : doubleTakes)
			v.doubleTake = false;
		TaskExecution.threadPool(doubleTakes, 3);
	}
}