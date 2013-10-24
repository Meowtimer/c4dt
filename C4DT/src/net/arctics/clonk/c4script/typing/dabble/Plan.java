package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.Flags.DEBUG;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

@SuppressWarnings("serial")
class Plan {

	private final class VariableInitializationDependenciesVisitor implements IASTVisitor<Visit> {
		@Override
		public TraversalContinuation visitNode(ASTNode node, Visit v) {
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
										addDependency(v, other);
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
		public TraversalContinuation visitNode(ASTNode node, Visit v) {
			if (node instanceof CallDeclaration && resultNeeded(node)) {
				final CallDeclaration cd = (CallDeclaration) node;
				final List<Visit> calledVisits = visits.get(cd.name());
				if (calledVisits != null)
					for (final Visit cv : calledVisits)
						addDependency(v, cv);
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

	final DabbleInference inference;
	final Map<String, VisitsByName> visits = new HashMap<>();
	int total;
	final Set<Visit> doubleTakes = new HashSet<>();
	final List<Visit> roots = new LinkedList<>();
	final IASTVisitor<Visit> resultUsedRequirementsDetector = new ResultUsedDependencyDetector();
	final IASTVisitor<Visit> variableInitializationsRequirementsVisitor = new VariableInitializationDependenciesVisitor();

	Plan(DabbleInference inference) {
		this.inference = inference;
		populateVisitsMap();
		determineDependencies();
		populate();
	}

	void populateVisitsMap() {
		total = 0;
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values()) {
				total++;
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
		if (testDependent.dependencies.contains(testRequirement))
			return true;
		else for (final Visit td_ : testDependent.dependencies)
			if (requires(td_, testRequirement, catcher))
				return true;
		return false;
	}

	void addDependency(Visit dependent, Visit requirement) {
		if (requirement == dependent)
			return;
		if (!requires(requirement, dependent, new HashSet<Visit>())) {
			dependent.dependencies.add(requirement);
			requirement.dependents.add(dependent);
		} else {
			if (DEBUG)
				System.out.println(String.format("Not adding requirement %s to %s", requirement.toString(), dependent.toString()));
			dependent.doubleTake = true;
			doubleTakes.add(dependent);
		}
	}

	void determineDependencies() {

		switch (inference.typing) {
		case STATIC: case DYNAMIC:
			return;
		default:
			break;
		}

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
										addDependency(v, callerVisit);
						}
				}
			}

		// functions containing initialization of used variable -> variable user
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values())
				v.function.traverse(variableInitializationsRequirementsVisitor, v);
	}

	void populate() {
		for (final Input i : inference.input.values())
			for (final Visit v : i.visits.values())
				if (v.dependencies.size() == 0)
					roots.add(v);
	}
}