package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.Flags.DEBUG;
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
import java.util.concurrent.ExecutorService;

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
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.TaskExecution;

@SuppressWarnings("serial")
class Graph extends LinkedList<Runnable> {
	private final DabbleInference inference;
	private final Map<String, List<Visit>> visits = new HashMap<>();

	void populateVisitsMap() {
		for (final Input i : inference.input.values())
			for (final Visit v : i.plan.values()) {
				List<Visit> list = visits.get(v.function.name());
				if (list == null) {
					list = new LinkedList<>();
					visits.put(v.function.name(), list);
				}
				list.add(v);
			}
	}

	boolean requires(Visit testDependent, Visit testRequirement, Set<Visit> catcher) {
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
		} else if (DEBUG)
			System.out.println(String.format("Not adding requirement %s to %s", requirement.toString(), dependent.toString()));
	}

	IASTVisitor<Visit> resultUsedRequirementsDetector = new IASTVisitor<Visit>() {
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
		boolean resultNeeded(final ASTNode node) {
			boolean resultNeeded = false;
			for (ASTNode p = node.parent(), c = node; p != null; c = p, p = p.parent()) {
				if (p instanceof SimpleStatement)
					break;
				if (p instanceof Sequence && ((Sequence)p).lastElement() == c)
					continue;
				resultNeeded = true;
				break;
			}
			return resultNeeded;
		}
	};

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
									final Function fun = ref.parentOfType(Function.class);
									final Visit other = v.input().plan.get(fun);
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
		for (final Input i : inference.input.values())
			for (final Visit v : i.plan.values())
				v.function.traverse(resultUsedRequirementsDetector, v);

		for (final Input i : inference.input.values())
			for (final Visit v : i.plan.values())
				if (i.shouldTypeFromCalls(v.function)) {
					final List<CallDeclaration> calls = inference.index().callsTo(v.function.name());
					if (calls != null)
						for (final CallDeclaration call : calls) {
							final Function caller = call.parentOfType(Function.class);
							final List<Visit> callerVisits = visits.get(caller.name());
							if (callerVisits != null)
								for (final Visit callerVisit : callerVisits)
									if (callerVisit.function == caller)
										addRequirement(v, callerVisit);
						}
				}

		for (final Input i : inference.input.values())
			for (final Visit v : i.plan.values())
				v.function.traverse(variableInitializationsRequirementsVisitor, v);
	}

	class Cluster extends HashSet<Visit> implements Runnable {
		void print(int depth, Collection<Visit> layer, Set<Visit> catcher) {
			for (final Visit v : layer) {
				System.out.println(String.format("%s-> %s", multiply("\t", depth), v.toString()));
				if (catcher.add(v))
					print(depth+1, v.dependents, catcher);
			}
		}
		void print() { print(0, this, new HashSet<Visit>()); }
		void run(Visit v, int depth) {
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
		void bottom(Set<Visit> into, Visit start, Set<Visit> catcher) {
			if (!catcher.add(start))
				return;
			if (start.dependents.size() == 0)
				into.add(start);
			else
				for (final Visit d : start.dependents)
					bottom(into, d, catcher);
		}
		void top(Collection<Visit> layer, Set<Visit> catcher) {
			for (final Visit v : layer)
				if (v.requirements.size() == 0)
					this.add(v);
				else if (catcher.add(v))
					top(v.requirements, catcher);
		}
		void find(Set<Visit> catcher, Visit start) {
			final Set<Visit> bottom = new HashSet<>();
			bottom(bottom, start, new HashSet<Visit>());
			top(bottom, new HashSet<Visit>());
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

	Cluster findCluster(Visit root) {
		final Cluster c = new Cluster();
		c.find(new HashSet<Visit>(), root);
		return c;
	}

	void populate() {
		final List<Visit> roots = new LinkedList<>();
		for (final Input i : inference.input.values())
			for (final Visit v : i.plan.values())
				if (v.requirements.size() == 0)
					if (v.dependents.size() == 0)
						this.add(v);
					else
						roots.add(v);
		while (roots.size() > 0) {
			final Visit root = roots.get(0);
			final Cluster c = findCluster(root);
			roots.removeAll(c);
			this.add(c);
		}
	}

	static String randomColor() {
		final Random random = new Random();
		final char[] hex = "0123456789ABCDEF".toCharArray();
		final char[] chars = new char[6];
		for (int i = 0; i < 6; i++)
			chars[i] = hex[random.nextInt(hex.length)];
		return new String(chars);
	};

	void output() {
		int x = 0;
		for (final Runnable r : this) {
			final Cluster c = as(r, Cluster.class);
			if (c == null)
				continue;
			try (FileWriter writer = new FileWriter(new File(String.format("/Users/madeen/Desktop/output%d.dot", ++x)))) {
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

	Graph(DabbleInference inference) {
		this.inference = inference;
		populateVisitsMap();
		determineRequirements();
		//verify();
		populate();
		//output();
	}

	void run() {
		if (DEBUG)
			for (final Runnable r : this)
				if (r instanceof Cluster) {
					System.out.println("-----------");
					((Cluster)r).print();
					System.out.println("-----------");
				}
		if (this.size() < 20)
			for (final Runnable r : this)
				r.run();
		else
			TaskExecution.threadPool(new Sink<ExecutorService>() {
				@Override
				public void receivedObject(ExecutorService pool) {
					for (final Runnable runnable : Graph.this)
						pool.execute(runnable);
				}
			}, 20);
	}
}