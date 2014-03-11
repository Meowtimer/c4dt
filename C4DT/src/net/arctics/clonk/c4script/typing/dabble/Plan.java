package net.arctics.clonk.c4script.typing.dabble;

import static java.lang.String.format;
import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.util.StringUtil.blockString;
import static net.arctics.clonk.util.Utilities.any;

import java.util.ArrayList;
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
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.effect.EffectFunction;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input.Visit;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.TaskExecution;

@SuppressWarnings("serial")
class Plan {

	private final class CallerCalleeDependencyDetector implements Sink<ExecutorService> {
		@Override
		public void receivedObject(final ExecutorService item) {
			for (final Input i : inference.input.values())
				for (final Visit v : i.visits.values())
					item.execute(new Runnable() {
						@Override
						public void run() {
							final Function fn = v.function();
							if (fn instanceof EffectFunction) {
								final EffectFunction efn = (EffectFunction) fn;
								final List<CallDeclaration> addEffectCalls = inference.index().callsTo("AddEffect");
								if (addEffectCalls != null)
									for (final CallDeclaration call : addEffectCalls) {
										final ASTNode[] p = call.params();
										if (p.length > 0 && p[0] instanceof StringLiteral && ((StringLiteral)p[0]).literal().equals(efn.effect().name())) {
											final Function caller = call.parent(Function.class);
											final List<Visit> callerVisits = visits.get(caller.name());
											if (callerVisits != null)
												for (final Visit callerVisit : callerVisits)
													if (callerVisit.function == caller)
														addEdge(callerVisit, v);
										}
									}
							}
							final int numParameters = fn.numParameters();
							if (i.shouldTypeFromCalls(fn)) {
								final List<CallDeclaration> calls = inference.index().callsTo(fn.name());
								if (calls != null)
									for (final CallDeclaration call : calls) {
										if (call.params().length != numParameters)
											continue;
										final Function caller = call.parent(Function.class);
										final List<Visit> callerVisits = visits.get(caller.name());
										if (callerVisits != null)
											for (final Visit callerVisit : callerVisits)
												if (callerVisit.function == caller)
													addEdge(callerVisit, v);
									}
							}
						};
					});
		}
	}

	private final class VariableInitializationDependenciesVisitor implements IASTVisitor<Visit> {
		@Override
		public TraversalContinuation visitNode(final ASTNode node, final Visit v) {
			if (
				node instanceof AccessVar && node.predecessor() == null &&
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
										addEdge(other, v);
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

		private boolean containedInAssignment(final ASTNode node) {
			for (ASTNode p = node.parent(); p != null; p = p.parent())
				if (p instanceof BinaryOp && ((BinaryOp)p).operator().isAssignment())
					return true;
			return false;
		}

		private boolean isLocal(final AccessVar node, final Visit v) {
			return v.function.findLocalDeclaration(node.name(), Variable.class) != null;
		}
	}

	private final static class VisitsByName extends LinkedList<Visit> implements Runnable {
		@Override
		public void run() {
			for (final Visit v : this)
				v.prepare();
		}
	}

	private final class ResultUsedDependencyDetector implements IASTVisitor<Visit> {
		@Override
		public TraversalContinuation visitNode(final ASTNode node, final Visit v) {
			if (node instanceof CallDeclaration && resultNeeded(node)) {
				final CallDeclaration cd = (CallDeclaration) node;
				final List<Visit> calledVisits = visits.get(cd.name());
				if (calledVisits != null)
					for (final Visit cv : calledVisits)
						addEdge(cv, v);
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

	private static class DependencyTester extends HashSet<Visit> implements IPredicate<Visit> {
		final Visit dependency;
		public DependencyTester(final Visit dependency) { this.dependency = dependency; }
		@Override
		public boolean test(final Visit dependent) {
			return add(dependent) &&
				(dependent.dependencies.contains(dependency) ||
				 any(dependent.dependencies, this));
		}
	}

	private static class ASTVisitorRunnable implements Runnable {
		final Visit target;
		final IASTVisitor<Visit> visitor;
		public ASTVisitorRunnable(final Visit target, final IASTVisitor<Visit> visitor) {
			super();
			this.target = target;
			this.visitor = visitor;
		}
		@Override
		public void run() {
			target.function.traverse(visitor, target);
		}
	}

	final DabbleInference inference;
	final Map<String, VisitsByName> visits;
	final Visit[] linear;
	final int total;
	final Set<Visit> doubleTakes = new HashSet<>();
	final List<Visit> roots = new LinkedList<>();

	public Plan(final DabbleInference inference) {
		this.inference = inference;
		this.visits = visitsMap();
		this.linear = linear();
		this.total = linear.length;
		determineDependencies();
		if (DEBUG)
			for (final Visit v : linear)
				System.out.println(format("%s: %s", v, blockString("", "", ", ", v.dependencies)));
		//transferEdges();
		findRoots();
	}

	private Map<String, VisitsByName> visitsMap() {
		final Map<String, VisitsByName> visits = new HashMap<>();
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values()) {
				VisitsByName list = visits.get(v.function.name());
				if (list == null) {
					list = new VisitsByName();
					visits.put(v.function.name(), list);
				}
				list.add(v);
			}
		return visits;
	}

	private Visit[] linear() {
		int num = 0;
		for (final Input i : inference.input.values())
			num += i.visits.size();
		final Visit[] result = new Visit[num];
		num = 0;
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values())
				result[num++] = v;
		return result;
	}

	private final Object edgeLock = new Object();

	private void addEdge(final Visit dependency, final Visit dependent) {
		if (dependency == dependent)
			return;
		synchronized (edgeLock) {
			if (!new DependencyTester(dependent).test(dependency)) {
				dependent.dependencies.add(dependency);
				dependency.dependents.add(dependent);
			} else {
				if (DEBUG)
					System.out.println(format("Not adding requirement %s to %s", dependency.toString(), dependent.toString()));
				dependent.doubleTake = true;
				doubleTakes.add(dependent);
			}
		}
		//edges.add(new Edge(dependency, dependent));
	}

	private Collection<Runnable> visitorRunnables(final IASTVisitor<Visit> visitor) {
		final List<Runnable> list = new ArrayList<>(total);
		for (final Visit v : linear)
			list.add(new ASTVisitorRunnable(v, visitor));
		return list;
	}

	private void determineDependencies() {

		switch (inference.typing) {
		case STATIC: case DYNAMIC:
			return;
		default:
			break;
		}

		// callee -> caller if callee's result used
		TaskExecution.threadPool(visitorRunnables(new ResultUsedDependencyDetector()), 3);

		// caller -> callee
		TaskExecution.threadPool(new CallerCalleeDependencyDetector(), 3, total);

		// functions containing initialization of used variable -> variable user
		TaskExecution.threadPool(visitorRunnables(new VariableInitializationDependenciesVisitor()), 3);
	}

	private void findRoots() {
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values())
				if (v.dependencies.isEmpty())
					roots.add(v);
	}
}