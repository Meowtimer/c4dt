package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.util.StringUtil.blockString;
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

	IASTVisitor<Visit> requirementsVisitor = new IASTVisitor<Visit>() {
		@Override
		public TraversalContinuation visitNode(ASTNode node, Visit v) {
			if (node instanceof CallDeclaration && resultNeeded(node)) {
				final CallDeclaration cd = (CallDeclaration) node;
				final List<Visit> calledVisits = visits.get(cd.name());
				if (calledVisits != null)
					for (final Visit cv : calledVisits)
						addRequirement(v, cv);
			}
			else if (
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

	void determineRequirements() {
		for (final Input i : inference.input.values())
			for (final Visit v : i.plan.values()) {

				if (i.shouldTypeFromCalls(v.function)) {
					final List<CallDeclaration> calls = i.index.callsTo(v.function.name());
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

				v.function.traverse(requirementsVisitor, v);

				v.requirements.remove(v);

			}
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
		void bottom(Set<Visit> catcher, Visit start) {
			if (start.dependents.size() == 0)
				catcher.add(start);
			else
				for (final Visit d : start.dependents)
					bottom(catcher, d);
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
			bottom(bottom, start);
			top(bottom, new HashSet<Visit>());
		}
		@Override
		public String toString() {
			return blockString("", "", ", ", this);
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

	void output() {
		try (FileWriter writer = new FileWriter(new File("/Users/madeen/Desktop/output.dot"))) {
			writer.append("digraph G {bgcolor=white\n");
			for (final Input i : inference.input.values())
				for (final Visit v : i.plan.values())
					if (!v.requirements.isEmpty() || !v.dependents.isEmpty())
						writer.append(String.format("\tnode [ style=filled,shape=\"box\",fillcolor=\"antiquewhite:aquamarine\" ] \"%s\";\n", v.toString()));

			for (final Input i : inference.input.values())
				for (final Visit v : i.plan.values())
					for (final Visit d : v.dependents)
						writer.append(String.format("\t\"%s\" -> \"%s\";\n", v, d));
//					for (final Visit r : v.requirements)
//						writer.append(String.format("\t\"%s\" -> \"%s\";", r, v));

			writer.append("}");
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	Graph(DabbleInference inference) {
		this.inference = inference;
		populateVisitsMap();
		prepareVisits();
		determineRequirements();
		//output();
		//verify();
		populate();
	}

	void prepareVisits() {
		for (final List<Visit> l : visits.values())
			for (final Visit v : l)
				v.prepare();
	}

	void run() {
//		for (final Runnable r : this)
//			if (r instanceof Cluster) {
//				System.out.println("-----------");
//				((Cluster)r).print();
//				System.out.println("-----------");
//			}
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