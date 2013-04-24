package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.c4script.typing.TypeUnification.unify;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.builder.ProjectSettings.Typing;
import net.arctics.clonk.c4script.ArrayType;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.FindDeclarationInfo;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.IProplistDeclaration;
import net.arctics.clonk.c4script.IType;
import net.arctics.clonk.c4script.ITypeable;
import net.arctics.clonk.c4script.InitializationFunction;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.c4script.ProblemReportingContext;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SpecialEngineRules;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Directive.DirectiveType;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.InitializationFunction.VarInitializationAccess;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.c4script.ast.ArrayExpression;
import net.arctics.clonk.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.BoolLiteral;
import net.arctics.clonk.c4script.ast.BreakStatement;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.CallExpr;
import net.arctics.clonk.c4script.ast.CallInherited;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.ConditionalStatement;
import net.arctics.clonk.c4script.ast.ContinueStatement;
import net.arctics.clonk.c4script.ast.ControlFlow;
import net.arctics.clonk.c4script.ast.FloatLiteral;
import net.arctics.clonk.c4script.ast.ForStatement;
import net.arctics.clonk.c4script.ast.FunctionDescription;
import net.arctics.clonk.c4script.ast.GarbageStatement;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.ILoop;
import net.arctics.clonk.c4script.ast.IfStatement;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.c4script.ast.Literal;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.MissingStatement;
import net.arctics.clonk.c4script.ast.NewProplist;
import net.arctics.clonk.c4script.ast.Nil;
import net.arctics.clonk.c4script.ast.OperatorExpression;
import net.arctics.clonk.c4script.ast.Parenthesized;
import net.arctics.clonk.c4script.ast.Placeholder;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.ReturnStatement;
import net.arctics.clonk.c4script.ast.Sequence;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.ThisType;
import net.arctics.clonk.c4script.ast.Tuple;
import net.arctics.clonk.c4script.ast.TypeChoice;
import net.arctics.clonk.c4script.ast.UnaryOp;
import net.arctics.clonk.c4script.ast.Unfinished;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.c4script.ast.WhileStatement;
import net.arctics.clonk.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.c4script.typing.CallTargetType;
import net.arctics.clonk.c4script.typing.FunctionType;
import net.arctics.clonk.c4script.typing.TypeUnification;
import net.arctics.clonk.c4script.typing.TypingJudgementMode;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input.Visit;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input.Visitor;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Definition.ProxyVar;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.EngineSettings;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.util.PerClass;
import net.arctics.clonk.util.Profiled;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.TaskExecution;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

@Capabilities(capabilities=Capabilities.ISSUES|Capabilities.TYPING)
public class DabbleInference extends ProblemReportingStrategy {

	static final boolean UNUSEDPARMWARNING = false;

	private static class Shared {
		final Map<Script, Input> input = new HashMap<>();
		boolean local = false;
		Script[] scripts;
		public synchronized Input getInput(Script script) { return input.get(script); }
	}
	private Shared shared;
	
	// flags
	private boolean typeThisAsObject;

	@Override
	public void setArgs(String args) {
		typeThisAsObject = false;
		for (final String a : args.split(",")) //$NON-NLS-1$
			if (a.equals("typeThisAsObject")) //$NON-NLS-1$
				typeThisAsObject = true;
	}

	@Override
	public void initialize(Markers markers, IProgressMonitor progressMonitor, Script[] scripts) {
		super.initialize(markers, progressMonitor, scripts);
		shared = new Shared();
		shared.scripts = scripts;
		assembleCommittee();
	}

	@Override
	public void run() { work(); }

	@Profiled
	final void work() {
		TaskExecution.threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				final Visitor[] visitors = new Visitor[shared.scripts.length];
				int i = 0;
				for (final Script p : shared.scripts)
					if (p != null) {
						final Input info = new Input(p, 0, shared);
						shared.input.put(p.script(), info);
						final Visitor v = info.new Visitor(null);
						v.setMarkers(markers);
						visitors[i++] = v;
					}
				for (final Visitor v : visitors)
					if (v != null)
						pool.execute(v);
			}
		}, 20);
	}
	
	private Visitor requestVisitor(Script script, Function function, Visitor originator) {
		if (script == null || function.body() == null)
			return null;
		synchronized (shared) {
			final Input input = shared.input.get(script);
			if (input != null)
				return input.new Visitor(originator);
			// add new input when run locally but only if this is the first hop from an existing visitor
			// and if the function to be visited previously required parameter typing from calls
			else if (shared.local && originator != null && originator.originator == null && function.typeFromCallsHint()) {
				if (DEBUG)
					System.out.println(String.format("Make new visitor for '%s'", function.qualifiedName(script))); //$NON-NLS-1$
				return (Visitor)localTypingContext(script, 0, originator);
			}
			else {
				if (DEBUG)
					System.out.println(String.format("No visitor for '%s'", function.qualifiedName(script))); //$NON-NLS-1$
				return null;
			}
		}
	}

	@Override
	public ProblemReportingContext localTypingContext(Script script, int fragmentOffset, ProblemReportingContext chain) {
		if (chain instanceof Visitor) {
			final Input p = ((Visitor) chain).input().shared.input.get(script);
			if (p != null)
				return p.new Visitor(null);
		}
		if (!(chain instanceof Visitor)) {
			shared = new Shared();
			shared.local = true;
			assembleCommittee();
		}
		final Input info = new Input(script, fragmentOffset, shared);
		shared.input.put(script, info);
		return info.new Visitor((Visitor) chain);
	}

	public enum State {
		UNDETERMINED,
		INPROGRESS,
		FINISHED
	}

	/**
	 * One script and associated information.
	 * @author madeen
	 *
	 */
	protected final class Input {
		
		/**
		 * Type variable for the return type of a function that was visited.
		 * {@link FunctionReturnTypeVariable}s are pre-created ({@link Input#makePlan()}) and determined (set to state {@link State#FINISHED}) in a not quite deterministic order.
		 * Synchronization of multiple visitor threads will be performed by locking on objects of this type. 
		 * @author madeen
		 */
		final class Visit extends FunctionReturnTypeVariable {
			
			State state;
			Visitor visitor;
			IType[] inferredTypes;
			Expert<?>[] experts;
			
			class Delayed {
				final Function function;
				final Script script;
				public Delayed(Function function, Script script) {
					super();
					this.function = function;
					this.script = script;
				}
				@Override
				public String toString() { return function.qualifiedName(script); }
			}
			
			final LinkedList<Delayed> delayedVisits = new LinkedList<>();
			
			public void state(State state) { this.state = state; }
			
			public Visit(Function function) {
				super(function);
				state = State.UNDETERMINED;
			}
			
			void assignExperts() {
				inferredTypes = new IType[function.totalNumASTNodes()];
				experts = new Expert<?>[function.totalNumASTNodes()];
				function.body().traverse(new IASTVisitor<Void>() {
					@Override
					public TraversalContinuation visitNode(ASTNode node, Void nothing) {
						if (node instanceof AccessDeclaration && node.containedIn(script))
							((AccessDeclaration)node).setDeclaration(null);
						experts[node.localIdentifier()] = findExpert(node);
						return TraversalContinuation.Continue;
					}
				}, null);
			}
			@Override
			public void apply(boolean soft) { /* done by Dabble */ }
		}
		
		final class Visitor implements Runnable, ProblemReportingContext, IEvaluationContext {
		
			final Visitor originator;
			final Input input() { return Input.this; }
		
			ControlFlow controlFlow;
			Markers markers;
			TypeEnvironment typeEnvironment;
			boolean inPreliminaryVisit;
			IASTVisitor<ProblemReportingContext> observer;
			Thread thread;
			
			final Thread thread() { return thread; }
			
			Visit visit;
			
			public Visitor(Visitor originator, Script... scope) {
				this.originator = originator;
				this.markers = DabbleInference.this.markers();
			}
		
			public Visitor(Visitor originator) { this(originator, Input.this.script); }
			
			@SuppressWarnings("unchecked")
			private final <T extends ASTNode> Expert<? super T> expert(T node) {
				final int localID = node.localIdentifier();
				if (visit != null && localID >= 0)
					return (Expert<? super T>) visit.experts[localID];
				else
					return findExpert(node);
			}

			public final IType ty(ASTNode node) { return node != null ? ty(node, expert(node)) : null; }

			public final <T extends ASTNode> IType ty(T node, Expert<T> expert) {
				final IType type = expert.type(node, this);
				if (visit != null)
					visit.inferredTypes[node.localIdentifier()] = type;
				return type;
			}

			@Override
			public final boolean judgement(ASTNode node, IType type, TypingJudgementMode mode) {
				return expert(node).judgement(node, type, null, this, mode);
			}

			public final boolean judgement(ASTNode node, IType type, ASTNode origin, TypingJudgementMode mode) {
				return expert(node).judgement(node, type, origin, this, mode);
			}
			
			@Override
			public String toString() {
				return String.format("Visitor (%s, %s)",
					input().toString(), visit != null ? visit.function().qualifiedName(script()) : "<no function>"); //$NON-NLS-1$
			}
			@Override
			public void setObserver(IASTVisitor<ProblemReportingContext> observer) { this.observer = observer; }
			
			public final SpecialFuncRule specialRuleFor(CallDeclaration node, int role) {
				final Engine engine = script().engine();
				if (engine != null && engine.specialRules() != null)
					return engine.specialRules().funcRuleFor(node.name(), role);
				else
					return null;
			}
		
			private boolean assignDefaultParmTypesToFunction(Function function) {
				if (input().rules != null)
					for (final SpecialFuncRule funcRule : input().rules.defaultParmTypeAssignerRules())
						if (funcRule.assignDefaultParmTypes(this, function))
							return true;
				return false;
			}
		
			@Override
			public boolean triggersRevisit(Function function, Function called) { return called.typeFromCallsHint(); }
		
			private void delayVisit(Function function, Script script) {
				if (visit == null)
					return;
				for (final Visit.Delayed d : visit.delayedVisits)
					if (function == d.function && script == d.script)
						return;
				visit.delayedVisits.add(visit.new Delayed(function, script));
			}
			
			private void initialParameterTypesFromCalls(Function function, Function baseFunction, TypeVariable[] parameterTypes) {
				final List<CallDeclaration> calls = input().index.callsTo(function.name());
				if (calls != null) {
					
					final IType[][] types = new IType[calls.size()][parameterTypes.length];
					final Visitor[] visitors = new Visitor[calls.size()];
					gatherCallTypes(function, baseFunction, parameterTypes, calls, types, visitors);
		
					for (int pa = 0; pa < parameterTypes.length; pa++) {
						final Variable p = function.parameter(pa);
						if (p.staticallyTyped())
							continue;
						typeFromCalls(function, parameterTypes, pa, calls, types, visitors);
					}
				}
			}
			
			private void typeFromCalls(Function function, TypeVariable[] parTyVars, int parameterIndex, List<CallDeclaration> calls, IType[][] types, Visitor[] callVisitors) {
				IType result;
				final boolean lenient = parTyVars[parameterIndex].get() == PrimitiveType.UNKNOWN;
				final Variable p = function.parameter(parameterIndex);
				// if there are concrete parameter types not unifying seed the unification chain with some primitive type
				// and look which seed results in least disagreement. Then place warning markers at the concrete parameters
				// not unifying with that consensus.
				// Only seeds are considered which unify with parameter usage in the function body in the first place.
				Seeding: {
					int leastDiscord = Integer.MAX_VALUE;
					PrimitiveType bestSeed = null;
					for (final PrimitiveType seed : callTypingSeeds) {
						result = TypeUnification.unifyNoChoice(parTyVars[parameterIndex].get(), seed);
						if (result == null)
							continue; // disagreement with usage inside body - ignore
						int discord = 0;
						for (int ci = 0; ci < calls.size(); ci++) {
							final IType concreteTy = types[ci][parameterIndex];
							if (concreteTy == null)
								continue;
							final IType unified = TypeUnification.unifyNoChoice(result, concreteTy);
							if (unified == null)
								discord++;
							else
								result = unified;
						}
						if (discord == 0)
							// no disagreement - type away
							break Seeding;
						else if (discord < leastDiscord) {
							bestSeed = seed;
							leastDiscord = discord;
						}
					}

					if (bestSeed != null) {
						result = TypeUnification.unify(parTyVars[parameterIndex].get(), bestSeed);
						for (int ci = 0; ci < calls.size(); ci++) {
							final IType concreteTy = types[ci][parameterIndex];
							if (concreteTy == null)
								continue;
							final IType unified = TypeUnification.unifyNoChoice(result, concreteTy);
							if (unified == null) {
								final Visitor visitor = callVisitors[ci];
								final ASTNode concretePar = calls.get(ci).params()[parameterIndex];
								if (visitor != null && !lenient)
									visitor.concreteArgumentMismatch(concretePar, p, function, result, concreteTy);
							}
							else
								result = unified;
						}
					} else if (!lenient) {
						// no consensus at all - warnings at all call sides
						result = parTyVars[parameterIndex].get();
						for (int ci = 0; ci < calls.size(); ci++) {
							final IType concreteTy = types[ci][parameterIndex];
							if (concreteTy == null)
								continue;
							final Visitor visitor = callVisitors[ci];
							final ASTNode concretePar = calls.get(ci).params()[parameterIndex];
							if (visitor != null)
								visitor.concreteArgumentMismatch(concretePar, p, function, result, concreteTy);
						}
					} else
						result = PrimitiveType.ANY;

				}
				if (lenient)
					result = TypeUnification.unify(result, PrimitiveType.ANY);
				parTyVars[parameterIndex].set(result);
			}

			private void gatherCallTypes(
				Function function,
				Function baseFunction,
				TypeVariable[] parameterTypes,
				final List<CallDeclaration> calls,
				final IType[][] types,
				final Visitor[] visitors
			) {
				for (int ci = 0; ci < calls.size(); ci++) {
					final CallDeclaration call = calls.get(ci);
					Function ref = as(call.declaration(), Function.class);
					// not related - short circuit skip
					//if (ref != null && ref.baseFunction() != null && ref.baseFunction().latestVersion() != baseFunction)
					//	continue;
					final Function f = call.parentOfType(Function.class);
					final Script other = f.parentOfType(Script.class);
					final Visit fVisit = delegateFunctionVisit(f, other, false, true);
					final Visitor visitor = visitors[ci] = fVisit != null ? fVisit.visitor : null;
					ref = as(call.declaration(), Function.class);
					if (ref != null) {
						ref = ref.baseFunction();
						if (ref != null)
							ref = (Function)ref.latestVersion();
						if (ref == baseFunction) {
							final int parNum = Math.min(parameterTypes.length, call.params().length);
							for (int pa = 0; pa < parNum; pa++)
								if (!function.parameter(pa).staticallyTyped()) {
									final ASTNode concretePar = call.params()[pa];
									if (concretePar != null) {
										script().addUsedScript(other);
										types[ci][pa] = parType(f, other, fVisit, visitor, concretePar);
									}
								}
						}
					}
				}
			}

			private IType parType(
				final Function containing,
				final Script other,
				final Visit containingVisit,
				final Visitor visitor,
				final ASTNode concretePar
			) {
				IType ty = containingVisit != null ? containingVisit.inferredTypes[concretePar.localIdentifier()] : null;
				if (ty == null && visitor != null)
					ty = visitor.typeOf(concretePar);
				if (ty == null) {
					final IType[] astTypes = other.typings().functionASTTypes.get(containing.name());
					if (astTypes != null)
						ty = astTypes[concretePar.localIdentifier()];
				}
				return ty;
			}
		
			final boolean allParametersStaticallyTyped(Function function) {
				for (final Variable p : function.parameters())
					if (!p.staticallyTyped())
						return false;
				return true;
			}
		
			@Override
			public TypeVariable visit(Function function) { return visit(function, null, true); }
		
			public Visit visit(Function function, Visitor originator, boolean wait) {
				if (thread == null)
					thread = Thread.currentThread();
				if (function == null || function.body() == null)
					return null;
				
				Visit _visit;
				_visit = input().plan.get(function);
				if (_visit != null)
					synchronized (_visit) {
						switch (_visit.state) {
						case FINISHED:
							return _visit;
						case INPROGRESS:
							if (wait && _visit.visitor.thread() != Thread.currentThread())
								waitForEndOfVisit(function, originator, _visit);
							return _visit;
						case UNDETERMINED:
							_visit.state(State.INPROGRESS);
							break;
						}
					}
				else
					return null;
		
				return uncheckedVisit(_visit);
			}

			private void waitForEndOfVisit(Function function, Visitor originator, Visit _visit) {
				if (DEBUG && originator != null && originator.visit != null)
					System.out.println(String.format("'%s' waiting for '%s'", originator.visit.function().qualifiedName(originator.script()), function.qualifiedName(script()))); //$NON-NLS-1$
				int i;
				for (i = 0; i < 3 && _visit.state != State.FINISHED; i++)
					try {
						_visit.wait(40);
					} catch (final InterruptedException e) {}
				if (i == 3 && originator != null && originator.visit != null)
					System.out.println(String.format("'%s' gave up waiting for '%s'", originator.visit.function().qualifiedName(originator.script()), function.qualifiedName(script()))); //$NON-NLS-1$
			}
		
			boolean inPreliminaryVisit() {
				if (DEBUG && inPreliminaryVisit)
					System.out.println(String.format("%s: Preliminary visit - not typing from calls", this.toString()));
				return inPreliminaryVisit;
			}
			
			private void startVisit(Visit _visit) {
				visit = _visit;
				_visit.visitor = this;
				_visit.assignExperts();
				_visit.delayedVisits.clear();
			}
			
			public Visit uncheckedVisit(Visit _visit) {
				
				final Function function = _visit.function();
				final Script funScript = function.script();
				if (DEBUG)
					System.out.println(String.format("%s: Visiting %s", script().name(), function.qualifiedName(script()))); //$NON-NLS-1$
				final Visit oldVisitee = visit;
				final boolean ownedFunction = funScript == script();
				final ASTNode[] statements = function.body().statements();
				final List<Variable> parameters = function.parameters();
				final Function baseFunction = function.baseFunction();
				
				if (!ownedFunction)
					startRoaming();
				startVisit(_visit);
				try {
					final TypeEnvironment env = newTypeEnvironment();
					{
						env.add(_visit);
						createFunctionLocalsTypeVariables(function);
						final TypeVariable[] callTypes = createParameterTypeVariables(function, parameters);
						if (determineTypingFromCalls(function, ownedFunction)) {
							preliminaryVisit(function, statements);
							initialParameterTypesFromCalls(function, baseFunction, callTypes);
						}
						actualVisit(ownedFunction, statements, callTypes);
						typeUntypedCallTargets(function);
						delayedVisits();
					}
					if (ownedFunction)
						env.apply(false);
					endTypeEnvironment(env, true, true);
					unusedWarnings(function, statements);
				}
				catch (final ProblemException e) { return null; }
				finally {
					synchronized (_visit) {
						if (ownedFunction)
							function.assignType(_visit.get(), false);
						endVisit(_visit);
					}
					visit = oldVisitee;
					if (!ownedFunction)
						endRoaming();
				}
				return _visit;
			}

			private void typeUntypedCallTargets(Function function) {
				function.body().traverse(new IASTVisitor<Visitor>() {
					final IType[] types = visit.inferredTypes;
					@Override
					public TraversalContinuation visitNode(ASTNode node, Visitor context) {
						if (node instanceof CallDeclaration && node.predecessorInSequence() != null) {
							final CallDeclaration cd = (CallDeclaration) node;
							final IType predTy = types[node.predecessorInSequence().localIdentifier()];
							if (predTy instanceof CallTargetType) {
								IType unified = null;
								for (final Function f : script.index().declarationsWithName(cd.name(), Function.class))
									unified = unify(unified, f.script());
								if (unified instanceof Script && ((Script)unified).findFunction(cd.name()) != null)
									judgement(node.predecessorInSequence(), unified, TypingJudgementMode.UNIFY);
							}
						}
						return TraversalContinuation.Continue;
					}
				}, this);
			}

			private boolean determineTypingFromCalls(final Function function, final boolean ownedFunction) {
				if (!inPreliminaryVisit()) {
					final boolean typeFromCalls =
						input().typing == Typing.ParametersOptionallyTyped &&
						input().script instanceof Definition &&
						ownedFunction && function.numParameters() > 0 &&
						!assignDefaultParmTypesToFunction(function) &&
						(function.typeFromCallsHint() || !allParametersStaticallyTyped(function));
					function.setTypeFromCallsHint(typeFromCalls);
					return typeFromCalls;
				} else
					return false;
			}

			private void createFunctionLocalsTypeVariables(final Function function) {
				for (final Variable l : function.locals()) {
					final AccessVar av = AccessVar.temp(l, function.body());
					final TypeVariable ti = expert(av).requestTypeVariable(av, this);
					if (ti != null)
						ti.set(PrimitiveType.UNKNOWN);
				}
			}

			private TypeVariable[] createParameterTypeVariables(final Function function, final List<Variable> parameters) {
				// create type variables for parameters
				final TypeVariable[] callTypes = new TypeVariable[parameters.size()];
				for (int i = 0; i < callTypes.length; i++) {
					final Variable p = function.parameter(i);
					final TypeVariable tyvar = new VariableTypeVariable(p);
					tyvar.set(p.staticallyTyped() ? p.type() : PrimitiveType.UNKNOWN);
					typeEnvironment.add(tyvar);
					callTypes[i] = tyvar;
				}
				return callTypes;
			}

			private void delayedVisits() {
				for (final Visit.Delayed d : visit.delayedVisits)
					delegateFunctionVisit(d.function, d.script, false, true);
				visit.delayedVisits.clear();
			}

			private void actualVisit(final boolean ownedFunction, final ASTNode[] statements, final TypeVariable[] callTypes) throws ProblemException {
				if (ownedFunction)
					for (int i = 0; i < callTypes.length; i++)
						callTypes[i].apply(false);
				final ControlFlow old = controlFlow;
				controlFlow = ControlFlow.Continue;
				for (final ASTNode s : statements)
					visit(s, true);
				controlFlow = old;
			}

			private void preliminaryVisit(final Function function, final ASTNode[] statements) throws ProblemException {
				// when taking parameter types from calls to the function visit the function body preliminarily
				// before visiting the functions the calls are in.
				// Merge insights about types from visiting the body with the types of concrete parameters.
				// This way, the functions calling this function - which also call visit - also will have
				// some preliminary return type for this function.
				// Also, merging call types with how the parameter is actually used inside the body improves
				// the chance of correctly deciding which kind of parameters are the 'right' ones to pass to the function.
				if (DEBUG)
					System.out.println(String.format("%s: Preliminary visit", toString()));
				inPreliminaryVisit = true;
				startRoaming();
				{
					final ControlFlow old = controlFlow;
					controlFlow = ControlFlow.Continue;
					for (final ASTNode s : statements)
						visit(s, true);
					controlFlow = old;
				}
				endRoaming();
				inPreliminaryVisit = false;
				function.traverse(CLEAR_DECLARATION_REFERENCES_VISITOR, null);
			}

			private void endVisit(Visit _visit) {
				_visit.state(State.FINISHED);
				_visit.visitor = null;
				_visit.notifyAll();
			}

			public final Visit delegateFunctionVisit(Function function, Script script, boolean allowThis, boolean allowWait) {
				if (function.body() == null)
					return null;
				if (inPreliminaryVisit) {
					if (DEBUG)
						System.out.println(String.format("%s: Skip delegating visit to function '%s'", toString(), function.qualifiedName(script)));
					return null;
				}
				if (DEBUG)
					System.out.println(String.format("Delegate function visit for '%s' from '%s'", //$NON-NLS-1$
						function.qualifiedName(script),
						visit != null ? visit.function.qualifiedName(script()) : "<null>"
					));
				if (function.body() == null)
					return null;
				for (Visitor v = this; v != null; v = v.originator)
					if (v.visit != null && v.script() == script && v.visit.function() == function)
						return v.visit;
				final Visit tv = allowThis
					? new Visitor(this).visit(function, this, allowWait)
					: null;
				if (tv == null) {
					// if that does not apply use another visitor
					final Visitor other = requestVisitor(script, function, this);
					if (other != null)
						return other.visit(function, this, allowWait);
					else
						return null;
				} else
					return tv;
			}
		
			public void concreteArgumentMismatch(ASTNode argument, Variable parameter, Function callee, IType expected, IType got) {
				try {
					this.markers().marker(this,
						Problem.ConcreteArgumentMismatch,
						argument, argument.start(), argument.end(),
						Markers.NO_THROW, IMarker.SEVERITY_WARNING,
						argument, parameter.name(), callee.qualifiedName(), expected.typeName(true), got.typeName(true)
					);
				} catch (final ProblemException e) {}
			}
		
			@Override
			public void incompatibleTypesMarker(ASTNode node, IRegion region, IType left, IType right) {
				try {
					if (left == null)
						left = PrimitiveType.ANY;
					if (right == null)
						right = PrimitiveType.ANY;
					this.markers().marker(this, Problem.IncompatibleTypes, node, region.getOffset(), region.getOffset()+region.getLength(), Markers.NO_THROW,
						input().typing == Typing.Static ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
						left.typeName(true), right.typeName(true)
					);
				} catch (final ProblemException e) {}
			}
		
			public final <T extends ASTNode> T visit(T expression, boolean recursive) throws ProblemException {
				if (expression == null)
					return null;
				final Expert<? super T> expert = expert(expression);
				final ControlFlow old = controlFlow;
				if (recursive && !expert.skipReportingProblemsForSubElements())
					for (final ASTNode e : expression.subElements())
						if (e != null)
							visit(e, true);
				controlFlow = old;
				expert.visit(expression, this);
				if (observer != null && !inPreliminaryVisit())
					observer.visitNode(expression, this);
				if (controlFlow == ControlFlow.Continue)
					controlFlow = expression.controlFlow();
				return expression;
			}
		
			public TypeEnvironment newTypeEnvironment() {
				final TypeEnvironment l = new TypeEnvironment();
				l.up = typeEnvironment;
				return typeEnvironment = l;
			}
		
			public void endTypeEnvironment(TypeEnvironment env, boolean inject, boolean ignoreLocals) {
				if (inject)
					if (env.up != null)
						env.up.inject(env, ignoreLocals);
					else synchronized (input().typeEnvironment) {
						input().typeEnvironment.inject(env, ignoreLocals);
					}
				typeEnvironment = env.up;
			}
		
			private boolean createWarningAtDeclarationOfVariable(
				ASTNode[] statements,
				Variable variable,
				Problem code,
				Object... format
			) {
				for (final ASTNode s : statements)
					for (final VarDeclarationStatement decl : s.collectionExpressionsOfType(VarDeclarationStatement.class))
						for (final VarInitialization initialization : decl.variableInitializations())
							if (initialization.variable == variable) {
								this.markers().warning(this, code, initialization, initialization, 0, format);
								return true;
							}
				return false;
			}
		
			/**
			 * Warn about variables declared inside the given block that have not been referenced elsewhere ({@link Variable#isUsed() == false})
			 * @param func The function the block belongs to.
			 * @param block The {@link Block}
			 */
			public void unusedWarnings(Function func, ASTNode[] statements) {
				if (func == null)
					return;
				if (UNUSEDPARMWARNING)
					for (final Variable p : func.parameters())
						if (!p.isUsed())
							this.markers().warning(this, Problem.UnusedParameter, null, p, Markers.ABSOLUTE_MARKER_LOCATION, p.name());
				if (func.locals() != null)
					for (final Variable v : func.locals()) {
						if (!v.isUsed())
							createWarningAtDeclarationOfVariable(statements, v, Problem.Unused, v.name());
						final Variable shadowed = script().findVariable(v.name());
						// ignore those pesky static variables from scenario scripts
						if (shadowed != null && !(shadowed.parentDeclaration() instanceof Scenario))
							createWarningAtDeclarationOfVariable(statements, v, Problem.IdentShadowed, v.qualifiedName(), shadowed.qualifiedName());
					}
			}
		
			private final class RoamingMarkers extends Markers {
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
				public final Markers oldMarkers;
				public final Script origin;
				public int depth;
				private RoamingMarkers(Markers oldMarkers, Script origin) {
					this.oldMarkers = oldMarkers;
					this.origin = origin;
					this.depth = 1;
				}
				@Override
				public void marker(IASTPositionProvider positionProvider, Problem code, ASTNode node, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ProblemException {
					if (node == null || node.parentOfType(Script.class) != origin || (inPreliminaryVisit && node.containedIn(visit.function)))
						return;
					else
						oldMarkers.marker(positionProvider, code, node, markerStart, markerEnd, flags, severity, args);
				}
			}
		
			private final void startRoaming() {
				if (markers instanceof RoamingMarkers)
					((RoamingMarkers)markers).depth++;
				else
					markers = new RoamingMarkers(markers, script());
			}
		
			private final void endRoaming() {
				if (--((RoamingMarkers)markers).depth == 0)
					markers = ((RoamingMarkers)markers).oldMarkers;
			}
		
			@Override
			public void reportProblems() {
				synchronized (input()) {
					switch (input().mainVisitState) {
					case UNDETERMINED:
						input().mainVisitState = State.INPROGRESS;
						break;
					case FINISHED:
						return;
					case INPROGRESS:
						return; // meh
					}
				}
				input().mainVisit(this);
				synchronized (input()) {
					input().mainVisitState = State.FINISHED;
					input().notifyAll();
				}
			}
		
			@Override
			public void run() {
				if (progressMonitor.isCanceled())
					return;
				progressMonitor.subTask(String.format("Reporting problems for '%s'", script().name())); //$NON-NLS-1$
				reportProblems();
				progressMonitor.worked(1);
			}
		
			@Override
			public Definition definition() { return as(input().script, Definition.class); }
			@Override
			public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
				int fragmentOffset = input().fragmentOffset;
				if (fragmentOffset == 0) {
					final Function f = expression.parentOfType(Function.class);
					fragmentOffset = f != null ? f.bodyLocation().start() : 0;
				}
				return new SourceLocation(
					fragmentOffset+expression.start(),
					fragmentOffset+expression.end()
				);
			}
			@Override
			public CachedEngineDeclarations cachedEngineDeclarations() { return input().cachedEngineDeclarations; }
			@Override
			public Script script() { return input().script; }
			@Override
			public IFile file() { return script().scriptFile(); }
			@Override
			public Declaration container() { return script(); }
			@Override
			public int fragmentOffset() { return input().fragmentOffset; }
			@Override
			public IType typeOf(ASTNode node) { return ty(node); }
			@Override
			public boolean isModifiable(ASTNode node) { return expert(node).isModifiable(node, this); }
		
			@Override
			public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) {
				@SuppressWarnings("unchecked")
				final AccessDeclarationExpert<T> expert = (AccessDeclarationExpert<T>)expert(access);
				return expert.obtainDeclaration(access, this);
			}
		
			public boolean validForType(ASTNode node, IType type) {
				return expert(node).unifyDeclaredAndGiven(node, type, this) != null;
			}
		
			@Override
			@SuppressWarnings("unchecked")
			public final <T extends IType> T typeOf(ASTNode node, Class<T> cls) {
				for (final IType t : typeOf(node))
					if (cls.isInstance(t))
						return (T)t;
				return null;
			}
		
			@Override
			public Markers markers() { return markers; }
			@Override
			public void setMarkers(Markers markers) { this.markers = markers;}
			@Override
			public Object valueForVariable(AccessVar access) { return null; }
			@Override
			public Object[] arguments() { return null; }
			@Override
			public Function function() { return null; }
			@Override
			public int codeFragmentOffset() { return 0; }
			@Override
			public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) {}
			@Override
			public Object cookie() { return null; }
		}
		
		final Script script;
		final Index index;
		final Typing typing;
		final CachedEngineDeclarations cachedEngineDeclarations;
		final Shared shared;
		final int strictLevel;
		final boolean hasAppendTo;
		final Map<Function, Visit> plan;
		final IType thisType;
		final Map<String, Declaration> variableMap = new HashMap<>();
		final Map<String, Declaration> functionMap = new HashMap<>();
		final SpecialEngineRules rules;
		final int fragmentOffset;
		final TypeEnvironment typeEnvironment = new TypeEnvironment();
		
		State mainVisitState = State.UNDETERMINED;
		
		public Script script() { return script; }
		private HashMap<Function, Visit> makePlan() {
			final HashMap<Function, Visit> result = new LinkedHashMap<>();
			final List<Script> conglomerate = script.conglomerate();
			for (final Script s : conglomerate)
				if (s != script)
					for (final Function f : s.functions())
						if (script.seesFunction(f))
							result.put(f, new Visit(f));
			for (final Function f : script.functions())
				result.put(f, new Visit(f));
			return result;
		}
		public Input(Script script, int sourceFragmentOffset, Shared shared) {
			this.shared = shared;
			this.script = script;
			this.index = script.index();
			this.typing = script.index() != null && script.index().nature() != null
				? script.index().nature().settings().typing
				: Typing.ParametersOptionallyTyped;
			this.rules = script.engine().specialRules();
			this.cachedEngineDeclarations = this.script.engine().cachedDeclarations();
			this.strictLevel = script.strictLevel();
			this.thisType = typeThisAsObject ? script : new ThisType(script);
			this.fragmentOffset = sourceFragmentOffset;
			boolean hasAppendTo = false;
			for (final Directive d : script.directives())
				if (d.type() == DirectiveType.APPENDTO) {
					hasAppendTo = true;
					break;
				}
			this.hasAppendTo = hasAppendTo;
			this.plan = Collections.synchronizedMap(makePlan());
		}
		public void apply() {
			final Map<String, IType> variableTypes = new HashMap<>();
			final Map<String, IType> functionReturnTypes = new HashMap<>();
			final Map<String, IType[]> functionASTTypes = new HashMap<>();
			for (final TypeVariable tyVar : typeEnvironment.values()) {
				final Declaration d = tyVar.declaration();
				if (d.containedIn(script))
					tyVar.apply(false);
				if (d instanceof Variable)
					switch (((Variable)d).scope()) {
					case LOCAL:
						variableTypes.put(d.name(), tyVar.get());
						break;
					default:
						break;
					}
				else if (d instanceof Function)
					functionReturnTypes.put(d.name(), tyVar.get());
			}
			for (final Entry<Function, Visit> v : plan.entrySet())
				functionASTTypes.put(v.getKey().name(), v.getValue().inferredTypes);
			script.setTypings(new Script.Typings(variableTypes, functionReturnTypes, functionASTTypes));
		}
		public void mainVisit(Visitor visitor) {
			final TypeEnvironment env2 = visitor.newTypeEnvironment();
			final Function[] plan_;
			synchronized (plan) { plan_ = plan.keySet().toArray(new Function[plan.size()]); }
			for (final Function f : plan_)
				visitor.visit(f, null, false);
			visitor.endTypeEnvironment(env2, true, false);
			apply();
		}
		@Override
		public String toString() { return script.name(); }
	}

	private static final IASTVisitor<Void> CLEAR_DECLARATION_REFERENCES_VISITOR = new IASTVisitor<Void>() {
		@Override
		public TraversalContinuation visitNode(ASTNode node, Void _) {
			if (node instanceof AccessDeclaration)
				((AccessDeclaration)node).setDeclaration(null);
			return TraversalContinuation.Continue;
		}
	};

	private static final PrimitiveType[] callTypingSeeds = new PrimitiveType[] {
		PrimitiveType.UNKNOWN,
		PrimitiveType.INT,
		PrimitiveType.ID,
		PrimitiveType.OBJECT,
		PrimitiveType.STRING,
		PrimitiveType.PROPLIST
	};

	/**
	 * An object that knows how to answer various questions about nodes of type T.
	 * These questions span
	 * <ul>
	 * 	<li>typing ({@link Expert#type(ASTNode, Visitor)})</li>
	 *  <li></li>
	 * </ul>
	 * @author madeen
	 *
	 * @param <T>
	 */
	class Expert<T extends ASTNode> extends PerClass<ASTNode, T, Expert<? super T>> {
		public Expert(Class<T> cls) { super(cls); }
		public void findSuper() {
			for (Class<? super T> s = cls.getSuperclass(); s != null; s = s.getSuperclass()) {
				@SuppressWarnings("unchecked")
				final
				Expert<? super T> sup = (Expert<? super T>)committee.get(s);
				if (sup != null) {
					supr = sup;
					return;
				}
			}
			supr = MASTER_OF_NONE;
		}

		/**
		 * Returning true tells the {@link Visitor} to not recursively call {@link #visit(ASTNode, Visitor)} on {@link ASTNode#subElements()}
		 * @return Do you just show up, play the music,
		 */
		public boolean skipReportingProblemsForSubElements() {return false;}
		public void visit(T node, Visitor visitor) throws ProblemException {}
		public IType type(T node, Visitor visitor) { return PrimitiveType.UNKNOWN; }
		public Declaration typeEnvironmentKey(T node, Visitor visitor) { return null; }
		public TypeVariable createTypeVariable(T node, Visitor visitor) { return null; }
		public boolean isModifiable(T node, Visitor visitor) { return true; }

		@Override
		public String toString() { return String.format("Expert<%s>", cls.getSimpleName()); } //$NON-NLS-1$

		public final IType predecessorType(ASTNode node, Visitor visitor) {
			final ASTNode p = node.predecessorInSequence();
			return p != null ? visitor.ty(p) : null;
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final IType unifyDeclaredAndGiven(ASTNode node, IType type, Visitor visitor) {
			final IType myType = visitor.ty(node);
			if (type == null)
				return myType;
			return TypeUnification.unifyNoChoice(type, myType);
		}

		public TypeVariable findTypeVariable(T node, Visitor visitor) {
			final Declaration key = typeEnvironmentKey(node, visitor);
			return findTypeVariable(key, visitor);
		}

		/**
		 * Query the type variable of an arbitrary expression. With some luck the inference engine will be able to give an answer.
		 * @param node the expression to query the type of
		 * @return The {@link TypeVariable} or null if nothing was found
		 */
		public final TypeVariable findTypeVariable(Declaration key, Visitor visitor) {
			if (key == null)
				return null;
			for (TypeEnvironment e = visitor.typeEnvironment; e != null; e = e.up) {
				final TypeVariable tv = e.find(key);
				if (tv != null)
					return tv;
			}
			return null;
		}

		public TypeVariable findParameterTypeVariable(Variable parameter, Visitor visitor) {
			final Function function = parameter.parentOfType(Function.class);
			for (Visitor v = visitor; v != null; v = v.originator)
				if (v.visit != null && v.visit.function() == function)
					return findTypeVariable(parameter, v);
			return null;
		}

		/**
		 * Requests type information for an expression
		 * @param expression the expression
		 * @return the type information or null if none has been stored
		 */
		public final TypeVariable requestTypeVariable(T node, Visitor visitor) {
			final Declaration key = typeEnvironmentKey(node, visitor);
			if (key == null)
				return null;
			final TypeEnvironment env = visitor.typeEnvironment;
			if (env == null || visitor.input().typing == Typing.Static || visitor.input().typing == Typing.Dynamic)
				return null;
			boolean topMostLayer = true;
			TypeVariable base = null;
			for (TypeEnvironment list = env; list != null; list = list.up) {
				final TypeVariable tyvar = list.find(key);
				if (tyvar != null)
					if (!topMostLayer) {
						base = tyvar;
						break;
					}
					else
						return tyvar;
				topMostLayer = false;
			}
			final TypeVariable newtyvar = createTypeVariable(node, visitor);
			if (newtyvar != null) {
				if (base != null)
					newtyvar.set(base.get());
				env.add(newtyvar);
			}
			return newtyvar;
		}

		public boolean judgement(T node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
			final TypeVariable tyvar = requestTypeVariable(node, visitor);
			if (tyvar != null) {
				switch (mode) {
				case OVERWRITE:
					tyvar.set(type);
					break;
				case UNIFY:
					tyvar.set(TypeUnification.unify(tyvar.get(), type));
					break;
				}
				return true;
			} else
				return false;
		}

		public final void assignment(T leftSide, ASTNode rightSide, Visitor visitor) {
			switch (visitor.input().typing) {
			case Static:
				final IType leftTy = visitor.ty(leftSide, this);
				final IType rightTy = visitor.ty(rightSide);
				if (!TypeUnification.compatible(leftTy, rightTy))
					visitor.incompatibleTypesMarker(rightSide, rightSide, leftTy, rightTy);
				break;
			case ParametersOptionallyTyped:
				visitor.judgement(leftSide, visitor.ty(rightSide), rightSide, TypingJudgementMode.OVERWRITE);
				break;
			case Dynamic:
				break;
			}
		}
	}

	private final Expert<ASTNode> MASTER_OF_NONE = new Expert<ASTNode>(ASTNode.class) {
		@Override
		public IType type(ASTNode node, Visitor visitor) { return PrimitiveType.UNKNOWN; }
	};

	private final <T extends ASTNode> Expert<? super T> findExpert(T node) {
		for (Class<?> cls = node.getClass(); cls != null; cls = cls.getSuperclass()) {
			@SuppressWarnings("unchecked")
			final
			Expert<? super T> expert = (Expert<? super T>)committee.get(cls);
			if (expert != null)
				return expert;
		}
		return MASTER_OF_NONE;
	}

	private final Map<Class<? extends ASTNode>, Expert<? extends ASTNode>> committee = new HashMap<Class<? extends ASTNode>, Expert<?>>();

	class AccessDeclarationExpert<T extends AccessDeclaration> extends Expert<T> {
		public AccessDeclarationExpert(Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(T node, Visitor visitor) { return null; }
		@Override
		public void visit(T node, Visitor visitor) throws ProblemException {
			super.visit(node, visitor);
			internalObtainDeclaration(node, visitor);
		}
		protected final Declaration internalObtainDeclaration(T node, Visitor visitor) {
			if (visitor.script() == node.parentOfType(Script.class)) {
				Declaration d = node.declaration();
				if (d == null)
					d = obtainDeclaration(node, visitor);
				if (d == null) {
					visitor.script().index().loadScriptsContainingDeclarationsNamed(node.name());
					d = obtainDeclaration(node, visitor);
				}
				node.setDeclaration(d);
				return d;
			} else
				return obtainDeclaration(node, visitor);
		}
		@Override
		public TypeVariable createTypeVariable(T node, Visitor visitor) {
			if (node.declaration() instanceof ITypeable && ((ITypeable)node.declaration()).staticallyTyped())
				return null;
			else
				return super.createTypeVariable(node, visitor);
		}
		@Override
		public IType type(T node, Visitor visitor) {
			final TypeVariable tyvar = findTypeVariable(node, visitor);
			return tyvar != null ? tyvar.get() : PrimitiveType.UNKNOWN;
		}
		@Override
		public Declaration typeEnvironmentKey(T node, Visitor visitor) {
			return internalObtainDeclaration(node, visitor);
		}
	}

	private void assembleCommittee() {

		class AccessVarExpert<T extends AccessVar> extends AccessDeclarationExpert<T> {
			private AccessVarExpert(Class<T> cls) { super(cls); }
			private Declaration findUsingType(Visitor visitor, AccessVar node, ASTNode predecessor, IType type) {
				if (type == null)
					return null;
				for (final IType t : type) {
					Script scriptToLookIn;
					if ((scriptToLookIn = Definition.scriptFrom(t)) == null) {
						// find pseudo-variable from proplist expression
						if (t instanceof IProplistDeclaration) {
							final Variable proplistComponent = ((IProplistDeclaration)t).findComponent(node.name());
							if (proplistComponent != null)
								return proplistComponent;
						}
					} else {
						final FindDeclarationInfo info = new FindDeclarationInfo(visitor.script().index());
						info.searchOrigin = scriptToLookIn;
						info.findGlobalVariables = predecessor == null;
						Declaration v = scriptToLookIn.findDeclaration(node.name(), info);
						if (v instanceof Definition)
							v = ((Definition)v).proxyVar();
						return v;
					}
				}
				return null;
			}

			@Override
			protected Declaration obtainDeclaration(AccessVar node, Visitor visitor) {
				final ASTNode p = node.predecessorInSequence();
				if (p == null && node.name().equals(Variable.THIS.name()))
					return Variable.THIS;
				IType type = visitor.script();
				if (p != null)
					type = visitor.ty(p);
				if (p == null) {
					final Function f = node.parentOfType(Function.class);
					if (f != null) {
						final Variable v = f.findVariable(node.name());
						if (v != null)
							return v;
					}
					Declaration v = visitor.input().variableMap.get(node.name());
					if (v == null && !visitor.input().variableMap.containsKey(node.name())) {
						v = findUsingType(visitor, node, null, type);
						visitor.input().variableMap.put(node.name(), v);
					}
					return v;
				}
				else
					return findUsingType(visitor, node, p, type);
			}

			@Override
			public IType type(T node, Visitor visitor) {
				final Declaration d = internalObtainDeclaration(node, visitor);
				if (d == Variable.THIS) {
					final Function fn = node.parentOfType(Function.class);
					return fn != null && fn.isGlobal()
						? CallTargetType.INSTANCE // global func - don't assume this to be typed as this script
						: visitor.input().thisType;
				}
				final TypeVariable stored = findTypeVariable(node, visitor);
				if (stored != null)
					return stored.get();
				if (d instanceof Function)
					return new FunctionType((Function)d);
				else if (d instanceof Variable) {
					final Variable v = (Variable)d;
					switch (v.scope()) {
					case LOCAL:
						IType t = PrimitiveType.UNKNOWN;
						if (node.predecessorInSequence() != null) {
							final IType predTy = visitor.ty(node.predecessorInSequence());
							if (predTy != null)
								for (final IType _t : predTy)
									if (_t instanceof Script) {
										final Input nput = shared.getInput((Script) _t);
										if (nput != null)
											nput.new Visitor(visitor).reportProblems();
										final IType frt = ((Script)_t).typings().variableTypes.get(d.name());
										t = TypeUnification.unify(t, frt);
									}
						}
						return t != PrimitiveType.UNKNOWN ? t : ((Variable)d).type();
					default:
						return v.type(); 
					}
				}
				else if (d instanceof ITypeable)
					return ((ITypeable) d).type();
				return PrimitiveType.UNKNOWN;
			}

			@Override
			public void visit(T node, Visitor visitor) throws ProblemException {
				super.visit(node, visitor);
				final ASTNode pred = node.predecessorInSequence();
				final Declaration declaration = node.declaration();
				if (declaration == null && pred == null)
					visitor.markers().error(visitor, Problem.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.name());
				// local variable used in global function
				else if (declaration instanceof Variable) {
					final Variable var = (Variable) declaration;
					var.setUsed(true);
					switch (var.scope()) {
					case LOCAL:
						final Declaration d = node.parentOfType(Declaration.class);
						if (d != null && pred == null) {
							final Function f = d.topLevelParentDeclarationOfType(Function.class);
							final Variable v = d.topLevelParentDeclarationOfType(Variable.class);
							if (
								(f != null && f.visibility() == FunctionScope.GLOBAL) ||
								(f == null && v != null && v.scope() != Scope.LOCAL)
							)
								visitor.markers().error(visitor, Problem.LocalUsedInGlobal, node, node, Markers.NO_THROW);
						}
						break;
					case STATIC: case CONST:
						visitor.script().addUsedScript(var.script());
						break;
					case VAR:
						final Function currentFunction = node.parentOfType(Function.class);
						if (currentFunction != null && var.parentDeclaration() == currentFunction) {
							final int locationUsed = currentFunction.bodyLocation().getOffset()+node.start();
							if (locationUsed < var.start())
								visitor.markers().warning(visitor, Problem.VarUsedBeforeItsDeclaration, node, node, 0, String.format("%s @(%d, %d)", var.name(), var.start(), var.end())); //$NON-NLS-1$
						}
						break;
					case PARAMETER:
						break;
					}
				} else if (declaration instanceof Function)
					if (!visitor.script().engine().settings().supportsFunctionRefs)
						visitor.markers().error(visitor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitor.script().engine().name());
			}

			@Override
			public boolean judgement(T node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
				Declaration declaration = internalObtainDeclaration(node, visitor);
				if (declaration == Variable.THIS)
					return true;
				if (declaration == null && origin != null) {
					if (node.name().startsWith("Turn")) //$NON-NLS-1$
						internalObtainDeclaration(node, visitor);
					final IType predType = predecessorType(node, visitor);
					if (predType != null) {
						boolean foundSomeVar = false;
						for (final IType t : predType)
							if (t == visitor.script() && visitor.script() == node.parentOfType(Script.class)) {
								final Variable var = new Variable(node.name(), Variable.Scope.LOCAL);
								initializeFromAssignment(node, type, origin, visitor, var);
								visitor.script().addDeclaration(var);
								node.setDeclaration(declaration = var);
								foundSomeVar = true;
							}
							else if (t instanceof Declaration && t instanceof IProplistDeclaration) {
								final IProplistDeclaration proplDecl = (IProplistDeclaration) t;
								final Declaration d = (Declaration) t;
								if (d.parentOfType(IndexEntity.class) == visitor.script()) {
									final Variable var = proplDecl.addComponent(new Variable(node.name(), Variable.Scope.VAR), true);
									var.setLocation(node.absolute());
									node.setDeclaration(declaration = var);
									initializeFromAssignment(node, type, origin, visitor, var);
									foundSomeVar = true;
								} else {
									if (DEBUG)
										System.out.println(String.format("%s: Won't add '%s' to '%s'", visitor.script().name(), node.name(), d.qualifiedName())); //$NON-NLS-1$
									foundSomeVar = true;
								}
							}
						if (!foundSomeVar) {
							final ProplistDeclaration proplDecl = new ProplistDeclaration(new ArrayList<Variable>());
							final Variable var = proplDecl.addComponent(new Variable(node.name(), Variable.Scope.VAR), true);
							proplDecl.setParent(visitor.script());
							var.setLocation(node.absolute());
							node.setDeclaration(declaration = var);
							initializeFromAssignment(node, type, origin, visitor, var);
							visitor.judgement(node.predecessorInSequence(), proplDecl, TypingJudgementMode.UNIFY);
							visitor.script().addDeclaration(proplDecl);
						}
					}
				}
				return super.judgement(node, type, origin, visitor, mode);
			}

			private void initializeFromAssignment(T node, IType type, ASTNode origin, Visitor visitor, final Variable var) {
				var.setLocation(visitor.absoluteSourceLocationFromExpr(node));
				var.forceType(type);
				var.setInitializationExpression(origin);
			}

			@Override
			public TypeVariable findTypeVariable(T node, Visitor visitor) {
				final TypeVariable r = super.findTypeVariable(node, visitor);
				if (r != null)
					return r;
				final Declaration d = node.declaration();
				if (
					d != null && d.parent() == visitor.script() &&
					!(node.parent() instanceof BinaryOp && ((BinaryOp)node.parent()).operator().isAssignment())
				) {
					final List<AccessVar> references = visitor.script().varReferences().get(node.name());
					if (references != null)
						for (final AccessVar ref : references)
							for (ASTNode p = ref.parent(); p != null; p = p.parent())
								if (p instanceof BinaryOp && ((BinaryOp)p).operator().isAssignment() && ref.containedIn(((BinaryOp)p).leftSide())) {
									visitor.visit(ref.parentOfType(Function.class), visitor, false);
									break;
								}
				}
				return super.findTypeVariable(node, visitor);
			}

			@Override
			public TypeVariable createTypeVariable(T node, Visitor visitor) {
				if (node.declaration() instanceof ProxyVar)
					return null;
				else if (node.declaration() instanceof Variable)
					return new VariableTypeVariable(node);
				else
					return null;
			}

			@Override
			public boolean isModifiable(AccessVar node, Visitor visitor) {
				final Declaration declaration = node.declaration();
				final ASTNode pred = node.predecessorInSequence();
				if (pred == null)
					return declaration == null || ((Variable)declaration).scope() != Scope.CONST;
				else
					return true; // you can never be so sure
			}
		}

		class LiteralExpert<T extends Literal<?>> extends Expert<T> {
			public LiteralExpert(Class<T> cls) { super(cls); }
			@Override
			public boolean judgement(T node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
				// constantly steadfast do i resist the pressure of expectancy lied upon me
				return true;
			}
			@Override
			public boolean isModifiable(T node, Visitor visitor) { return false; }
		}

		class ConditionalStatementExpert<T extends ConditionalStatement> extends Expert<T> {
			public ConditionalStatementExpert(Class<T> cls) { super(cls); }
			@Override
			public boolean skipReportingProblemsForSubElements() {return true;}
			@Override
			public void visit(ConditionalStatement node, Visitor visitor) throws ProblemException {
				final ControlFlow t = visitor.controlFlow;
				visitor.controlFlow = ControlFlow.Continue;
				TypeEnvironment env = visitor.newTypeEnvironment();
				visitor.visit(node.condition(), true);
				visitor.endTypeEnvironment(env, true, false);
				env = visitor.newTypeEnvironment();
				visitor.visit(node.body(), true);
				visitor.endTypeEnvironment(env, true, false);
				loopConditionWarnings(node, visitor);
				visitor.controlFlow = t;
			}
			/**
			 * Emit warnings about loop conditions that could result in loops never executing or never ending.
			 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
			 * @param condition The loop condition to check
			 */
			protected void loopConditionWarnings(ConditionalStatement node, Visitor visitor) {
				final ASTNode condition = node.condition();
				if (node.body() == null || condition == null || !(node instanceof ILoop))
					return;
				final Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateStatic(node.parentOfType(Function.class)));
				if (Boolean.FALSE.equals(condEv))
					visitor.markers().warning(visitor, Problem.ConditionAlwaysFalse, condition, condition, Markers.NO_THROW, condition);
				else if (Boolean.TRUE.equals(condEv)) {
					final EnumSet<ControlFlow> flows = node.body().possibleControlFlows();
					if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
						visitor.markers().warning(visitor, Problem.InfiniteLoop, node, node, Markers.NO_THROW);
				}
			}
		}

		final Expert<?>[] classes = new Expert[] {

			new AccessDeclarationExpert<AccessDeclaration>(AccessDeclaration.class),

			new AccessVarExpert<AccessVar>(AccessVar.class),
			
			new AccessVarExpert<InitializationFunction.VarInitializationAccess>(InitializationFunction.VarInitializationAccess.class) {
				@Override
				public boolean judgement(VarInitializationAccess leftSide, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					super.judgement(leftSide, type, origin, visitor, mode);
					if (origin != null)
						if (leftSide.declaration() instanceof Variable && ((Variable)leftSide.declaration()).scope() == Scope.CONST && !origin.isConstant())
							try {
								visitor.markers().error(visitor, Problem.NonConstGlobalVarAssignment, origin, origin, Markers.NO_THROW);
							} catch (final ProblemException e) { }
					return true;
				}
				@Override
				public boolean isModifiable(VarInitializationAccess node, Visitor visitor) { return true; /* sudo */ }
			},

			new Expert<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType type(ArrayExpression node, final Visitor visitor) {
					IType elmType = PrimitiveType.UNKNOWN;
					for (final ASTNode e : node.subElements())
						if (e != null)
							elmType = TypeUnification.unify(elmType, visitor.ty(e));
					return new ArrayType(elmType);
				}
				@Override
				public boolean isModifiable(ArrayExpression node, Visitor visitor) { return false; }
			},

			new Expert<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType type(ArrayElementExpression node, Visitor visitor) {
					final IType t = supr.type(node, visitor);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					final ASTNode pred = node.predecessorInSequence();
					if (pred != null) {
						final IType predTy = visitor.ty(pred);
						if (predTy != null)
							for (final IType ty : predTy) {
								final ArrayType at = as(ty, ArrayType.class);
								if (at != null)
									return at.elementType();
							}
					}
					return PrimitiveType.ANY;
				}
				@Override
				public boolean judgement(ArrayElementExpression leftSide, IType rightSideType, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					final IType predType_ = predecessorType(leftSide, visitor);
					final ASTNode pred = leftSide.predecessorInSequence();
					if (predType_ == null)
						return false;
					if (origin != null) {
						final IType elmTy = visitor.ty(leftSide.argument());
						for (final IType e : elmTy)
							if (eq(e, PrimitiveType.STRING))
								return visitor.judgement(pred, PrimitiveType.PROPLIST, mode);
						return visitor.judgement(pred, new ArrayType(rightSideType), TypingJudgementMode.UNIFY);
					}
					return true;
				}
				@Override
				public void visit(ArrayElementExpression node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					IType type = predecessorType(node, visitor);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					final ASTNode arg = node.argument();
					if (arg == null)
						visitor.markers().warning(visitor, Problem.MissingExpression, node, node, 0);
					else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
						final IType argType = visitor.ty(arg);
						final ASTNode pred = node.predecessorInSequence();
						if (eq(argType, PrimitiveType.STRING)) {
							if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
								visitor.markers().warning(visitor, Problem.NotAProplist, node, pred, 0);
							else
								visitor.judgement(pred, PrimitiveType.PROPLIST, TypingJudgementMode.UNIFY);
						}
						else if (eq(argType, PrimitiveType.INT))
							if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
								visitor.markers().warning(visitor, Problem.NotAnArrayOrProplist, node, pred, 0);
							//else
							//	expert(pred).typingJudgement(pred, PrimitiveType.ARRAY, info, TypingJudgementMode.Unify);
					}
				}
				@Override
				public boolean isModifiable(ArrayElementExpression node, Visitor visitor) { return true; }
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				private void warnIfNotArray(ASTNode node, Visitor visitor, IType type) {
					if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
						TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
						TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
						visitor.markers().warning(visitor, Problem.NotAnArrayOrProplist, node, node, 0);
				}
				@Override
				public void visit(ArraySliceExpression node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final IType type = predecessorType(node, visitor);
					warnIfNotArray(node.predecessorInSequence(), visitor, type);
				}
				@Override
				public boolean isModifiable(ArraySliceExpression node, Visitor visitor) { return false; }
			},

			new Expert<OperatorExpression>(OperatorExpression.class) {
				@Override
				public IType type(OperatorExpression node, Visitor visitor) {
					return node.operator().returnType();
				}
				@Override
				public boolean isModifiable(OperatorExpression node, Visitor visitor) { return node.operator().returnsRef(); }
			},

			new Expert<BinaryOp>(BinaryOp.class) {
				@Override
				public IType type(BinaryOp node, Visitor visitor) {
					switch (node.operator()) {
					// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
					case And: case Or: case JumpNotNil:
						final IType leftSideType = visitor.ty(node.leftSide());
						final IType rightSideType = visitor.ty(node.rightSide());
						if (leftSideType == rightSideType)
							return leftSideType;
						else
							return TypeUnification.unify(leftSideType, rightSideType);
					case Assign:
						return visitor.ty(node.rightSide());
					default:
						return supr.type(node, visitor);
					}
				}
				@Override
				public void visit(BinaryOp node, Visitor visitor) throws ProblemException {
					final Operator op = node.operator();
					// sanity
					final ASTNode left = node.leftSide();
					final ASTNode right = node.rightSide();
					node.setLocation(left.start(), right.end());
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !visitor.expert(left).isModifiable(left, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2impor
					if ((op == Operator.StringEqual || op == Operator.ne) && (visitor.input().strictLevel >= 2))
						visitor.markers().warning(visitor, Problem.ObsoleteOperator, node, node, 0, op.operatorName());
					// wrong parameter types
					if (unifyDeclaredAndGiven(left, op.firstArgType(), visitor) == null)
						visitor.incompatibleTypesMarker(node, left, op.firstArgType(), visitor.ty(left));
					if (unifyDeclaredAndGiven(right, op.secondArgType(), visitor) == null)
						visitor.incompatibleTypesMarker(node, right, op.secondArgType(), visitor.ty(right));

					IType expectedLeft, expectedRight;
					switch (op) {
					case Assign: case Equal:
						expectedLeft = expectedRight = null;
						break;
					default:
						expectedLeft  = op.firstArgType();
						expectedRight = op.secondArgType();
					}

					switch (op) {
					case Assign: case AssignAdd: case AssignSubtract:
					case AssignMultiply: case AssignModulo: case AssignDivide:
						visitor.expert(left).assignment(left, right, visitor);
						break;
					case Equal:
						if (runtimeTypeCheck(visitor, left, right, true))
							return;
						break;
					case JumpNotNil:
						expectedLeft = visitor.ty(right);
						expectedRight = null;
						break;
					default:
						break;
					}

					if (expectedLeft != null)
						visitor.judgement(left, expectedLeft, TypingJudgementMode.UNIFY);
					if (expectedRight != null)
						visitor.judgement(right, expectedRight, TypingJudgementMode.UNIFY);
				}
				private boolean runtimeTypeCheck(Visitor visitor, ASTNode left, ASTNode right, boolean checkReverse) {
					if (
						left instanceof CallDeclaration &&
						((CallDeclaration)left).params().length >= 1 &&
						((CallDeclaration)left).name().equals("GetType") && //$NON-NLS-1$
						right instanceof AccessVar &&
						((AccessVar)right).name().startsWith("C4V_") //$NON-NLS-1$
					) {
						final IType type = PrimitiveType.fromString(((AccessVar)right).name().substring(4).toLowerCase());
						if (type != null) {
							visitor.judgement(
								((CallDeclaration)left).params()[0],
								TypeChoice.make(PrimitiveType.ANY, type),
								TypingJudgementMode.UNIFY
							);
							return true;
						}
					}
					return checkReverse && runtimeTypeCheck(visitor, right, left, false);
				}
				@Override
				public TypeVariable createTypeVariable(BinaryOp node, Visitor visitor) {
					final ASTNode leftSide = node.leftSide();
					if (node.operator().isAssignment() && leftSide != null)
						return visitor.expert(leftSide).createTypeVariable(leftSide, visitor);
					return super.createTypeVariable(node, visitor);
				}
				@Override
				public Declaration typeEnvironmentKey(BinaryOp node, Visitor visitor) {
					final ASTNode leftSide = node.leftSide();
					if (node.operator().isAssignment() && leftSide != null)
						return visitor.expert(leftSide).typeEnvironmentKey(leftSide, visitor);
					else
						return null;
				}
				@Override
				public boolean isModifiable(BinaryOp node, Visitor visitor) { return node.operator().returnsRef(); }
			},

			new Expert<UnaryOp>(UnaryOp.class) {
				@Override
				public IType type(UnaryOp node, Visitor visitor) { return node.operator().returnType(); }
				@Override
				public void visit(UnaryOp node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !visitor.expert(arg).isModifiable(arg, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					final Expert<? super ASTNode> rarg = visitor.expert(arg);
					final PrimitiveType firstArgType = node.operator().firstArgType();
					if (rarg.unifyDeclaredAndGiven(arg, firstArgType, visitor) == null)
						visitor.incompatibleTypesMarker(node, arg, firstArgType, visitor.ty(arg, rarg));
					if (firstArgType != PrimitiveType.UNKNOWN)
						rarg.judgement(arg, firstArgType, null, visitor, TypingJudgementMode.UNIFY);
				}
				@Override
				public boolean isModifiable(UnaryOp node, Visitor visitor) {
					return node.placement() == Placement.Prefix && node.operator().returnsRef();
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void visit(BoolLiteral node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					if (node.parent() instanceof BinaryOp) {
						final Operator op = ((BinaryOp) node.parent()).operator();
						if (op == Operator.And || op == Operator.Or)
							visitor.markers().warning(visitor, Problem.BoolLiteralAsOpArg, node, node, 0, this.toString());
					}
				}
			},

			new Expert<ContinueStatement>(ContinueStatement.class) {
				@Override
				public void visit(ContinueStatement node, Visitor visitor) throws ProblemException {
					if (node.parentOfType(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitor);
				}
			},

			new Expert<BreakStatement>(BreakStatement.class) {
				@Override
				public void visit(BreakStatement node, Visitor visitor) throws ProblemException {
					if (node.parentOfType(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitor);
				}
			},

			new Expert<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(Visitor visitor, ASTNode node, boolean tupleIsError) throws ProblemException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							visitor.markers().error(visitor, Problem.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (visitor.input().strictLevel >= 2)
							visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					final ASTNode[] subElms = node.subElements();
					for (final ASTNode e : subElms)
						warnAboutTupleInReturnExpr(visitor, e, true);
				}
				@Override
				public void visit(ReturnStatement node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(visitor, returnExpr, false);
					final Function currentFunction = node.parentOfType(Function.class);
					if (currentFunction == null)
						visitor.markers().error(visitor, Problem.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (visitor.input().typing == Typing.Static && currentFunction.staticallyTyped()) {
							if (visitor.expert(returnExpr).unifyDeclaredAndGiven(returnExpr, currentFunction.returnType(), visitor) == null)
								visitor.incompatibleTypesMarker(node,
									returnExpr, currentFunction.returnType(), visitor.ty(returnExpr));
						}
						else {
							final IType type = visitor.ty(returnExpr);
							visitor.judgement(node, type, TypingJudgementMode.UNIFY);
						}
				}
				@Override
				public TypeVariable createTypeVariable(ReturnStatement node, Visitor visitor) {
					return new FunctionReturnTypeVariable(node.parentOfType(Function.class));
				}
				@Override
				public Declaration typeEnvironmentKey(ReturnStatement node, Visitor visitor) {
					return node.parentOfType(Function.class);
				}
			},

			new AccessDeclarationExpert<CallDeclaration>(CallDeclaration.class) {
				/**
				 * Find a {@link Function} for some hypothetical {@link CallDeclaration}, using contextual information such as the {@link ASTNode#type(ProblemReportingContext)} of the {@link ASTNode} preceding this {@link CallDeclaration} in the {@link Sequence}.
				 * @param input Context to use for searching
				 * @param functionName Name of the function to look for. Would correspond to the hypothetical {@link CallDeclaration}'s {@link #name()}
				 * @param pred The predecessor of the hypothetical {@link CallDeclaration} ({@link ASTNode#predecessorInSequence()})
				 * @param listToAddPotentialDeclarationsTo When supplying a non-null value to this parameter, potential declarations will be added to the collection. Such potential declarations would be obtained by querying the {@link Index}'s {@link Index#declarationMap()}.
				 * @return The {@link Function} that is very likely to be the one actually intended to be referenced by the hypothetical {@link CallDeclaration}.
				 */
				private Declaration findUsingType(
					Visitor visitor,
					CallDeclaration node, String functionName,
					IType type
				) {
					final IType lookIn = type != null ? type : visitor.script();
					if (lookIn != null) for (final IType ty : lookIn) {
						Script script = as(ty, Script.class);
						if (script == null && ty instanceof MetaDefinition)
							script = ((MetaDefinition)ty).definition();
						if (script == null)
							continue;
						script.requireLoaded();
						final FindDeclarationInfo info = new FindDeclarationInfo(visitor.script().index());
						info.searchOrigin = visitor.script();
						info.contextFunction = node.parentOfType(Function.class);
						info.findGlobalVariables = type == null;
						final Declaration dec = script.findDeclaration(functionName, info);
						if (dec != null)
							return dec;
					}
					if (type != null) {
						// find global function
						Declaration declaration;
						try {
							declaration = visitor.script().index().findGlobal(Declaration.class, functionName);
						} catch (final Exception e) {
							e.printStackTrace();
							return null;
						}
						// find engine function
						if (declaration == null)
							declaration = visitor.script().index().engine().findFunction(functionName);

						final List<Declaration> allFromLocalIndex = visitor.script().index().declarationMap().get(functionName);
						final Declaration decl = visitor.script().engine().findLocalFunction(functionName, false);
						int numCandidates = 0;
						if (allFromLocalIndex != null)
							numCandidates += allFromLocalIndex.size();
						if (decl != null)
							numCandidates++;

						// only return found global function if it's the only choice
						if (declaration != null && numCandidates == 1)
							return declaration;
					}
					return null;
				}
				@Override
				protected Declaration obtainDeclaration(CallDeclaration node, Visitor visitor) {
					final String declarationName = node.name();
					if (declarationName.equals(Keywords.Return))
						return null;
					final ASTNode p = node.predecessorInSequence();
					Declaration f;
					if (p == null) {
						f = visitor.input().functionMap.get(node.name());
						if (f == null && !visitor.input().functionMap.containsKey(node.name())) {
							f = findUsingType(visitor, node, declarationName, visitor.script());
							visitor.input().functionMap.put(declarationName, f);
						}
					} else
						f = findUsingType(visitor, node, declarationName, visitor.ty(p));
					return f;
				}
				private IType declarationType(CallDeclaration node, Visitor visitor) {
					final Declaration d = internalObtainDeclaration(node, visitor);

					// look for gathered type information
					final TypeVariable tyvar = findTypeVariable(node, visitor);
					if (tyvar != null)
						return tyvar.get();

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && d != null && (d == visitor.cachedEngineDeclarations().This || d == Variable.THIS))
						return node.parentOfType(Function.class).isGlobal() ? CallTargetType.INSTANCE : visitor.input().thisType;

					if (d instanceof Function) {
						final Function fn = (Function) d;
						// Some special rule applies and the return type is set accordingly
						final SpecialFuncRule rule = node.specialRuleFromContext(visitor, SpecialEngineRules.RETURNTYPE_MODIFIER);
						if (rule != null) {
							final IType type = rule.returnType(visitor, node);
							if (type != null)
								return type;
						}
						IType t = PrimitiveType.UNKNOWN;
						if (node.predecessorInSequence() != null) {
							final IType predTy = visitor.ty(node.predecessorInSequence());
							if (predTy != null)
								for (final IType _t : predTy)
									if (_t instanceof Script) {
										final Script _s = (Script)_t;
										final Visitor other = requestVisitor(_s, fn, visitor);
										final TypeVariable rtv = other != null ? other.visit(_s.override(fn)) : null;
										t = TypeUnification.unify(t, rtv != null ? rtv.get() : fn.returnType(_s));
									}
						} else {
							final Visit v = visitor.delegateFunctionVisit(fn, visitor.script(), true, false);
							if (v != null)
								t = v.get();
						}
						return t != PrimitiveType.UNKNOWN ? t : fn.returnType();
					}
					if (d instanceof Variable) {
						final Variable v = (Variable)d;
						switch (v.scope()) {
						case LOCAL:
							final IType t = shared.local && node.predecessorInSequence() == null
							? visitor.script().typings().variableTypes.get(v.name()) : null;
							return t != null ? t : v.type();
						default:
							return v.type();
						}
					}
					return supr != null ? supr.type(node, visitor) : PrimitiveType.UNKNOWN;
				}
				private IType unknownFunctionShouldBeError(CallDeclaration node, Visitor visitor) {
					ASTNode pred = node.predecessorInSequence();
					// stand-alone function? always bark!
					if (pred == null)
						return visitor.script();
					// not typed? weird
					final IType predType = visitor.ty(pred);
					if (predType == null)
						return null;
					// called via ~? ok
					if (pred instanceof MemberOperator)
						if (((MemberOperator)pred).hasTilde())
							return null;
						else
							pred = pred.predecessorInSequence();
					// allow this->Unknown()
					final AccessDeclaration ad = as(pred, AccessDeclaration.class);
					if (ad != null && (ad.declaration() == Variable.THIS || ad.declaration() == visitor.cachedEngineDeclarations().This))
						return null;
					boolean anyScripts = false;
					for (final IType t : predType)
						if (t instanceof Definition)
							anyScripts = true;
					return anyScripts ? predType : null;
				}
				@Override
				public IType type(CallDeclaration node, Visitor visitor) {
					final IType type = declarationType(node, visitor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(CallDeclaration node, Visitor visitor) throws ProblemException {
					
					// call ty() for parameter nodes to store inferredType which is queried by initialParameterTypesFromCalls
					for (final ASTNode e : node.params())
						if (e != null) {
							visitor.visit(e, true);
							visitor.ty(e);
						}
					
					super.visit(node, visitor);
					
					final CachedEngineDeclarations cachedEngineDeclarations = visitor.cachedEngineDeclarations();
					final String declarationName = node.name();
					final Declaration declaration = internalObtainDeclaration(node, visitor);
					final ASTNode[] params = node.params();
					final ASTNode predecessor = node.predecessorInSequence();

					if (declarationName.equals(Keywords.Return)) {
						// return as function
						if (visitor.input().strictLevel >= 2)
							visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
						else
							visitor.markers().warning(visitor, Problem.ReturnAsFunction, node, node, 0);
					} else if (declaration instanceof Variable) {
						// variable as function
						((Variable)declaration).setUsed(true);
						final IType type = declarationType(node, visitor);
						// no warning when in #strict mode
						if (visitor.input().strictLevel >= 2)
							if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !TypeUnification.compatible(PrimitiveType.FUNCTION, type))
								visitor.markers().warning(visitor, Problem.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
					} else if (declaration instanceof Function) {
						final Function f = (Function)declaration;
						
						if (shared.local && !visitor.inPreliminaryVisit()) {
							final IType predTy = predecessor != null ? visitor.ty(predecessor) : visitor.script();
							if (predTy instanceof Script)
								visitor.delayVisit(f, (Script)predTy);
						}
						
						if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
							visitor.script().addUsedScript(f.script());

						final SpecialFuncRule rule = visitor.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
						final boolean specialCaseHandled =
							rule != null &&
							rule.validateArguments(node, params, visitor);

						// not a special case... check regular parameter types
						if (!specialCaseHandled) {
							int givenParam = 0;
							for (final Variable parm : f.parameters()) {
								if (givenParam >= params.length)
									break;
								final ASTNode given = params[givenParam++];
								if (given == null)
									continue;
								final TypeVariable parmTyVar = findParameterTypeVariable(parm, visitor);
								final IType parmTy = parmTyVar != null ? parmTyVar.get() : parm.type();
								final IType unified = unifyDeclaredAndGiven(given, parmTy, visitor);
								if (unified == null)
									visitor.incompatibleTypesMarker(node, given, parmTy, visitor.ty(given));
								else if (eq(parmTy, PrimitiveType.UNKNOWN))
									visitor.judgement(given, unified, TypingJudgementMode.UNIFY);
							}
						}
					} else if (declaration == null) {
						final IType container = unknownFunctionShouldBeError(node, visitor);
						if (container != null) {
							final int start = node.start();
							visitor.markers().error(visitor, Problem.DeclarationNotFound,
								node, start, start+declarationName.length(), Markers.NO_THROW, declarationName, container.typeName(true));
						}
					}
				}
				@Override
				public TypeVariable createTypeVariable(CallDeclaration node, Visitor visitor) {
					final Declaration d = node.declaration();
					if (d instanceof Function) {
						final Function f = (Function) d;
						if (f.staticallyTyped() || f.isEngineDeclaration() || f != node.parentOfType(Function.class))
							return null;
						return new FunctionReturnTypeVariable((Function)d);
					} else
						return null;
				}
				@Override
				public Declaration typeEnvironmentKey(CallDeclaration node, Visitor visitor) {
					return internalObtainDeclaration(node, visitor);
				}
				@Override
				public boolean isModifiable(CallDeclaration node, Visitor visitor) {
					final Declaration declaration = node.declaration();
					final IType t = declaration instanceof Function ? ((Function)declaration).returnType() : PrimitiveType.UNKNOWN;
					return
						TypeUnification.compatible(t, PrimitiveType.REFERENCE) ||
						TypeUnification.compatible(t, PrimitiveType.UNKNOWN);
				}
			},

			new AccessDeclarationExpert<CallInherited>(CallInherited.class) {
				@Override
				public IType type(CallInherited node, Visitor visitor) {
					final TypeVariable tyVar = findTypeVariable(node, visitor);
					if (tyVar != null)
						return tyVar.get();
					final Function fn = node.parentOfType(Function.class);
					final Function inherited = fn.inheritedFunction();
					if (inherited != null)
						if (inherited.body() != null) {
							final Visit inhv = visitor.input().new Visit(inherited);
							visitor.input().new Visitor(visitor, inherited.script()).uncheckedVisit(inhv);
							visitor.judgement(node, inhv.get(), TypingJudgementMode.OVERWRITE);
							return inhv.get();
						} else
							return inherited.returnType();
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(CallInherited node, Visitor visitor) throws ProblemException {
					if (node.parentOfType(Script.class) == visitor.script()) {
						// inherited/_inherited not allowed in non-strict mode
						if (visitor.input().strictLevel <= 0)
							visitor.markers().error(visitor, Problem.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						node.setDeclaration(node.parentOfType(Function.class).inheritedFunction());
						if (node.declaration() == null && !node.failsafe())
							visitor.markers().error(visitor, Problem.NoInheritedFunction, node, node, Markers.NO_THROW, node.parentOfType(Function.class).name());
					}
				}
				@Override
				public Declaration typeEnvironmentKey(CallInherited node, Visitor visitor) {
					final Function f = node.parentOfType(Function.class);
					return f != null ? f.inheritedFunction() : null;
				}
			},

			new Expert<Sequence>(Sequence.class) {
				@Override
				public IType type(Sequence node, Visitor visitor) {
					final ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
						: visitor.ty(elements[elements.length-1]);
				}
				@Override
				public boolean judgement(Sequence node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					return visitor.judgement(node.lastElement(), type, origin, mode);
				}
				@Override
				public void visit(Sequence node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					ASTNode p = null;
					for (final ASTNode e : node.subElements()) {
						if (
							(e != null && !e.isValidInSequence(p)) ||
							(p != null && !p.allowsSequenceSuccessor(e))
						)
							visitor.markers().error(visitor, Problem.NotAllowedHere, node, e, Markers.NO_THROW, e);
						p = e;
					}
					if (p != null && !p.isValidAtEndOfSequence())
						visitor.markers().error(visitor, Problem.NotFinished, node, node, Markers.NO_THROW, node.printed());
				}
				@Override
				public boolean isModifiable(Sequence node, Visitor visitor) {
					final ASTNode[] elements = node.subElements();
					if (elements != null && elements.length > 0) {
						final ASTNode last = elements[elements.length-1];
						return visitor.expert(last).isModifiable(last, visitor);
					} else
						return false;
				}
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType type(ArraySliceExpression node, Visitor visitor) {
					final ArrayType arrayType = as(predecessorType(node, visitor), ArrayType.class);
					return defaulting(arrayType, PrimitiveType.ARRAY);
				}
			},

			new Expert<Nil>(Nil.class) {
				@Override
				public IType type(Nil node, Visitor visitor) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(Nil node, Visitor visitor) throws ProblemException {
					if (!visitor.script().engine().settings().supportsNil)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, visitor.script().engine().name());
				}
			},

			new LiteralExpert<StringLiteral>(StringLiteral.class) {
				@Override
				public IType type(StringLiteral node, Visitor visitor) { return PrimitiveType.STRING; }
				@Override
				public void visit(StringLiteral node, Visitor visitor) throws ProblemException {
					// warn about overly long strings
					final long max = visitor.script().index().engine().settings().maxStringLen;
					final String lit = node.literal();
					if (max != 0 && lit.length() > max)
						visitor.markers().warning(visitor, Problem.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (visitor.input().hasAppendTo || visitor.script().resource() == null)
						return;
					final String value = lit;
					final int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							final EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
							if (region != null) {
								StringTbl.reportMissingStringTblEntries(visitor, region, node);
								i += region.region().getLength();
								continue;
							}
						}
						++i;
					}
				}
			},

			new LiteralExpert<IntegerLiteral>(IntegerLiteral.class) {
				@Override
				public IType type(IntegerLiteral node, Visitor visitor) {
					if (node.longValue() == 0 && visitor.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new LiteralExpert<FloatLiteral>(FloatLiteral.class) {
				@Override
				public void visit(FloatLiteral node, Visitor visitor) throws ProblemException {
					if (!visitor.script().engine().settings().supportsFloats)
						visitor.markers().error(visitor, Problem.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.visit(node, visitor);
				}
			},

			new LiteralExpert<IDLiteral>(IDLiteral.class) {
				@Override
				public IType type(IDLiteral node, Visitor visitor) {
					final Definition obj = visitor.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.metaDefinition() : PrimitiveType.ID;
				}
			},

			new LiteralExpert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType type(BoolLiteral node, Visitor visitor) {
					return PrimitiveType.BOOL;
				}
			},

			new Expert<CallExpr>(CallExpr.class) {
				@Override
				public IType type(CallExpr node, Visitor visitor) {
					final ASTNode pred = node.predecessorInSequence();
					final IType type = visitor.ty(pred);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void visit(CallExpr node, Visitor visitor) throws ProblemException {
					if (!visitor.script().engine().settings().supportsFunctionRefs)
						visitor.markers().error(visitor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitor.script().engine().name());
					else {
						final IType type = visitor.expert(node.predecessorInSequence()).type(node.predecessorInSequence(), visitor);
						if (!TypeUnification.compatible(PrimitiveType.FUNCTION, type))
							visitor.markers().error(visitor, Problem.CallingExpression, node, node, Markers.NO_THROW);
					}
				}
			},

			new Expert<Statement>(Statement.class) {
				@Override
				public IType type(Statement node, Visitor visitor) {
					return PrimitiveType.UNKNOWN;
				}
				/**
				 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed.
				 * @param input The info
				 */
				public void warnIfNoSideEffects(Statement node, Visitor visitor) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						visitor.markers().warning(visitor, Problem.NoSideEffects, node, node, 0);
				}
				@Override
				public void visit(Statement node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					warnIfNoSideEffects(node, visitor);
					if (visitor.controlFlow != ControlFlow.Continue)
						visitor.markers().warning(visitor, Problem.NeverReached, node, node, 0);
				}
			},

			new Expert<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void visit(VarDeclarationStatement node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					for (final VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								final IType initializationType = visitor.ty(initialization.expression);
								if (
									initialization.variable.staticallyTyped() &&
									!TypeUnification.compatible(
										initialization.variable.type(),
										initializationType
									)
								)
									visitor.incompatibleTypesMarker(
										node,
										initialization.expression,
										initialization.variable.type(), initializationType
									);
								else {
									final AccessVar av = AccessVar.temp(initialization.variable, initialization);
									visitor.judgement(av, initializationType, TypingJudgementMode.OVERWRITE);
								}
							}
				}
			},

			new Expert<PropListExpression>(PropListExpression.class) {
				@Override
				public IType type(PropListExpression node, Visitor visitor) {
					return node.definedDeclaration();
				}
				@Override
				public void visit(PropListExpression node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					if (!visitor.script().engine().settings().supportsProplists)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW,
							net.arctics.clonk.c4script.ast.Messages.PropListExpression_ProplistsFeature,
							visitor.script().engine().name());
					for (final Variable v : node.components())
						if (v.initializationExpression() != null)
							visitor.judgement(AccessVar.temp(v, node), visitor.ty(v.initializationExpression()), TypingJudgementMode.UNIFY);
				}
				@Override
				public boolean isModifiable(PropListExpression node, Visitor visitor) { return false; }
			},

			new Expert<Parenthesized>(Parenthesized.class) {
				@Override
				public IType type(Parenthesized node, Visitor visitor) {
					return visitor.ty(node.innerExpression());
				}
				@Override
				public boolean isModifiable(Parenthesized node, Visitor visitor) {
					return visitor.expert(node.innerExpression()).isModifiable(node.innerExpression(), visitor);
				}
			},

			new Expert<MemberOperator>(MemberOperator.class) {
				final IType OBJECTISH = CallTargetType.INSTANCE;
				@Override
				public IType type(MemberOperator node, Visitor visitor) {
					if (node.id() != null)
						return visitor.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					final ASTNode pred = node.predecessorInSequence();
					return pred != null ? visitor.ty(pred) : supr.type(node, visitor);
				}
				@Override
				public boolean judgement(MemberOperator node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					final ASTNode p = node.predecessorInSequence();
					return p != null ? visitor.expert(p).judgement(p, type, origin, visitor, mode) : false;
				}
				@Override
				public void visit(MemberOperator node, Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final ASTNode pred = node.predecessorInSequence();
					final EngineSettings settings = visitor.script().engine().settings();
					if (pred != null) {
						final IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : typeThisAsObject ? PrimitiveType.OBJECT : OBJECTISH;
						final Expert<? super ASTNode> stmReporter = visitor.expert(pred);
						if (!TypeUnification.compatible(requiredType, visitor.ty(pred)))
							visitor.markers().warning(visitor, node.dotNotation() ? Problem.NotAProplist : Problem.CallingMethodOnNonObject, node, node, 0,
								visitor.ty(pred, stmReporter).typeName(false));
						else {
							final IType predTy = visitor.ty(pred);
							if (eq(predTy, PrimitiveType.UNKNOWN) || eq(predTy, PrimitiveType.ANY))
								visitor.judgement(pred, requiredType, TypingJudgementMode.UNIFY);
						}
					}
					if (node.getLength() > 3 && !settings.spaceAllowedBetweenArrowAndTilde)
						visitor.markers().error(visitor, Problem.MemberOperatorWithTildeNoSpace, node, node, Markers.NO_THROW);
					if (node.dotNotation() && !settings.supportsProplists)
						visitor.markers().error(visitor, Problem.DotNotationNotSupported, node, node, Markers.NO_THROW, node);
				}
			},

			new Expert<IterateArrayStatement>(IterateArrayStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(IterateArrayStatement node, Visitor visitor) throws ProblemException {
					final ControlFlow t = visitor.controlFlow;
					visitor.controlFlow = ControlFlow.Continue;
					Variable loopVariable;
					AccessVar accessVar;
					final ASTNode elementExpr = node.elementExpr();
					final ASTNode arrayExpr = node.arrayExpr();
					if (elementExpr instanceof VarDeclarationStatement)
						loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
					else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
						final Declaration d = visitor.obtainDeclaration(accessVar);
						if (d == null) {
							// implicitly create loop variable declaration if not found
							final SourceLocation varPos = visitor.absoluteSourceLocationFromExpr(accessVar);
							loopVariable = visitor.input().script.createVarInScope(Variable.DEFAULT_VARIABLE_FACTORY, node.parentOfType(Function.class), accessVar.name(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(d, Variable.class);
					} else
						loopVariable = null;

					visitor.visit(elementExpr, true);
					visitor.visit(arrayExpr, true);

					final IType type = visitor.ty(arrayExpr);
					if (!TypeUnification.compatible(type, PrimitiveType.ARRAY))
						visitor.incompatibleTypesMarker(node, arrayExpr, type, PrimitiveType.ARRAY);
					final IType elmType = ArrayType.elementTypeSet(type);
					final TypeEnvironment env = visitor.newTypeEnvironment();
					{
						if (loopVariable != null) {
							loopVariable.setUsed(true);
							visitor.judgement(AccessVar.temp(loopVariable, node), elmType, TypingJudgementMode.UNIFY);
						}
						visitor.visit(node.body(), true);
					}
					visitor.endTypeEnvironment(env, true, false);
					visitor.controlFlow = t;
				}
			},

			new Expert<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void visit(SimpleStatement node, Visitor visitor) throws ProblemException {
					final BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						visitor.markers().warning(visitor, Problem.NoAssignment, node, op, 0);
					supr.visit(node, visitor);
				}
			},

			new ConditionalStatementExpert<IfStatement>(IfStatement.class) {
				@Override
				public void visit(IfStatement node, Visitor visitor) throws ProblemException {
					final ControlFlow old = visitor.controlFlow;
					final ASTNode condition = node.condition();
					visitor.visit(condition, true);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					final TypeEnvironment ifEnvironment = visitor.newTypeEnvironment();
					visitor.visit(node.body(), true);
					visitor.endTypeEnvironment(ifEnvironment, false, false);
					visitor.controlFlow = old;
					if (node.elseExpression() != null) {
						final TypeEnvironment elseEnvironment = visitor.newTypeEnvironment();
						visitor.visit(node.elseExpression(), true);
						visitor.endTypeEnvironment(elseEnvironment, false, false);
						ifEnvironment.inject(elseEnvironment, false);
					}
					if (ifEnvironment.up != null)
						ifEnvironment.up.inject(ifEnvironment, false);
					visitor.controlFlow = old;

					if (!condition.containsConst()) {
						final Object condEv = PrimitiveType.BOOL.convert(condition.evaluateStatic(node.parentOfType(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							visitor.markers().warning(visitor,
								condEv.equals(true) ? Problem.ConditionAlwaysTrue : Problem.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ConditionalStatementExpert<ForStatement>(ForStatement.class) {
				@Override
				public void visit(ForStatement node, Visitor visitor) throws ProblemException {
					if (node.initializer() != null)
						visitor.visit(node.initializer(), true);
					super.visit(node, visitor);
					if (node.increment() != null)
						visitor.visit(node.increment(), true);
				}
			},

			new ConditionalStatementExpert<WhileStatement>(WhileStatement.class),

			new Expert<NewProplist>(NewProplist.class) {
				@Override
				public void visit(NewProplist node, Visitor visitor) throws ProblemException {
					node.definedDeclaration().setPrototype(as(visitor.ty(node.prototype()), ProplistDeclaration.class));
				}
			},

			new Expert<Placeholder>(Placeholder.class) {
				@Override
				public void visit(Placeholder node, Visitor visitor) throws ProblemException {
					StringTbl.reportMissingStringTblEntries(visitor, new EntityRegion(null, node, node.entryName()), node);
				}
			},

			new Expert<MissingStatement>(MissingStatement.class) {
				@Override
				public void visit(MissingStatement node, Visitor visitor) throws ProblemException {
					visitor.markers().error(visitor, Problem.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new Expert<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void visit(GarbageStatement node, Visitor visitor) throws ProblemException {
					visitor.markers().error(visitor, Problem.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new Expert<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void visit(FunctionDescription node, Visitor visitor) throws ProblemException {
					if (visitor.input().hasAppendTo)
						return;
					int off = 1;
					for (final String part : node.contents().split("\\|")) { //$NON-NLS-1$
						if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
							final StringTbl stringTbl = visitor.script().localStringTblMatchingLanguagePref();
							final String entryName = part.substring(1, part.length()-1);
							if (stringTbl == null || stringTbl.map().get(entryName) == null)
								visitor.markers().warning(visitor, Problem.UndeclaredIdentifier, node,
									new Region(node.start()+off, part.length()), 0, entryName);
						}
						off += part.length()+1;
					}
				}
			},

			new Expert<Comment>(Comment.class) {
				@Override
				public void visit(Comment node, Visitor visitor) throws ProblemException {
					if (node.parentOfType(Script.class) == visitor.script()) {
						final String s = node.text();
						int markerPriority;
						int searchStart = 0;
						do {
							markerPriority = IMarker.PRIORITY_LOW;
							int todoIndex = s.indexOf("TODO", searchStart); //$NON-NLS-1$
							if (todoIndex != -1)
								markerPriority = IMarker.PRIORITY_NORMAL;
							else {
								todoIndex = s.indexOf("FIXME", searchStart); //$NON-NLS-1$
								if (todoIndex != -1)
									markerPriority = IMarker.PRIORITY_HIGH;
							}
							if (todoIndex != -1) {
								int lineEnd = s.indexOf('\n', todoIndex);
								if (lineEnd == -1)
									lineEnd = s.length();
								searchStart = lineEnd;
								visitor.markers().todo(visitor.file(), node, s.substring(todoIndex, lineEnd), node.start()+2+todoIndex, node.start()+2+lineEnd, markerPriority);
							}
						} while (markerPriority > IMarker.PRIORITY_LOW);
					}
				}
			},

			new Expert<Unfinished>(Unfinished.class) {
				@Override
				public IType type(Unfinished node, Visitor visitor) {
					return visitor.ty(node.expression());
				}
				@Override
				public void visit(Unfinished node, Visitor visitor) throws ProblemException {
					visitor.markers().error(visitor, Problem.NotFinished, node, node, Markers.NO_THROW, node);
				}
			}

		};
		for (final Expert<?> expert : classes)
			committee.put(expert.cls(), expert);
		for (final Expert<?> expert : classes)
			expert.findSuper();
	}

}
