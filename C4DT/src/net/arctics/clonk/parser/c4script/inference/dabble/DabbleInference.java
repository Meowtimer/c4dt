package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.isAnyOf;
import static net.arctics.clonk.util.Utilities.threadPool;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.EngineSettings;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.TraversalContinuation;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IHasConstraint;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.IProplistDeclaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.parser.c4script.ast.ArrayExpression;
import net.arctics.clonk.parser.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.BoolLiteral;
import net.arctics.clonk.parser.c4script.ast.BreakStatement;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallExpr;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ConditionalStatement;
import net.arctics.clonk.parser.c4script.ast.ContinueStatement;
import net.arctics.clonk.parser.c4script.ast.ControlFlow;
import net.arctics.clonk.parser.c4script.ast.Ellipsis;
import net.arctics.clonk.parser.c4script.ast.FloatLiteral;
import net.arctics.clonk.parser.c4script.ast.ForStatement;
import net.arctics.clonk.parser.c4script.ast.FunctionDescription;
import net.arctics.clonk.parser.c4script.ast.GarbageStatement;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.ILoop;
import net.arctics.clonk.parser.c4script.ast.IfStatement;
import net.arctics.clonk.parser.c4script.ast.IntegerLiteral;
import net.arctics.clonk.parser.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.parser.c4script.ast.Literal;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.MissingStatement;
import net.arctics.clonk.parser.c4script.ast.NewProplist;
import net.arctics.clonk.parser.c4script.ast.Nil;
import net.arctics.clonk.parser.c4script.ast.OperatorExpression;
import net.arctics.clonk.parser.c4script.ast.Parenthesized;
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.StructuralType;
import net.arctics.clonk.parser.c4script.ast.Tuple;
import net.arctics.clonk.parser.c4script.ast.TypeChoice;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.ast.UnaryOp;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarInitialization;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.resource.ClonkBuilder;
import net.arctics.clonk.resource.ProjectSettings.Typing;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.PerClass;
import net.arctics.clonk.util.Sink;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

@Capabilities(capabilities=Capabilities.ISSUES|Capabilities.TYPING)
public class DabbleInference extends ProblemReportingStrategy {
	private static final boolean UNUSEDPARMWARNING = false;
	private static final boolean DEBUG = false;

	private static class Shared {
		C4ScriptParser[] parsers;
		IProgressMonitor monitor;
		final Map<Script, ScriptProcessor> processors = new HashMap<>();
		final Set<Function> finishedFunctions = new HashSet<>();
	}

	private Shared shared;

	@Override
	public void initialize(Markers markers, ClonkBuilder builder) {
		super.initialize(markers, builder);
		shared = new Shared();
		shared.parsers = builder.parsers().toArray(new C4ScriptParser[builder.parsers().size()]);
		shared.monitor = builder.monitor();
	}

	@Override
	public void run() {
		threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				for (C4ScriptParser p : shared.parsers)
					if (p != null)
						shared.processors.put(p.script(), new ScriptProcessor(p, shared));
				for (final ScriptProcessor processor : shared.processors.values())
					pool.execute(processor);
			}
		}, 20);
	}

	@Override
	public ProblemReportingContext localTypingContext(Script script) {
		return localTypingContext(new C4ScriptParser(script));
	}

	@Override
	public ProblemReportingContext localTypingContext(C4ScriptParser parser) {
		markers = parser.markers();
		return new ScriptProcessor(parser, new Shared());
	}

	public class ScriptProcessor implements Runnable, ProblemReportingContext {

		final C4ScriptParser parser;
		ASTNode reportingNode;
		TypeEnvironment typeEnvironment;
		final Typing typing;
		CachedEngineDeclarations cachedEngineDeclarations;
		final Shared shared;
		private final Object working = new Object();
		final int strictLevel;

		@Override
		public Typing typing() { return typing; }
		public C4ScriptParser parser() { return parser; }

		public ScriptProcessor(C4ScriptParser parser, Shared shared) {
			this.shared = shared;
			this.parser = parser;
			this.typing = parser.typing();
			this.cachedEngineDeclarations = this.parser.script().engine().cachedDeclarations();
			this.strictLevel = parser.script().strictLevel();
		}

		public final SpecialFuncRule specialRuleFor(CallDeclaration node, int role) {
			Engine engine = script().engine();
			if (engine != null && engine.specialRules() != null)
				return engine.specialRules().funcRuleFor(node.declarationName(), role);
			else
				return null;
		}

		private void assignDefaultParmTypesToFunction(Function function) {
			if (parser.specialEngineRules() != null)
				for (SpecialFuncRule funcRule : parser.specialEngineRules().defaultParmTypeAssignerRules())
					if (funcRule.assignDefaultParmTypes(this, function))
						return;
		}

		@Override
		public void reportProblemsOfFunction(Function function) {
			if (function == null || function.body() == null)
				return;
			synchronized (shared.finishedFunctions) {
				if (shared.finishedFunctions.contains(function))
					return;
				else
					shared.finishedFunctions.add(function);
			}
			if (function.script() == parser.script())
				synchronized (working) {
					assignDefaultParmTypesToFunction(function);
					try {
						reportProblemsOf(function, function.body().statements(), false);
					} catch (ParsingException e) {
						System.out.println(e.getMessage());
					}
				}
			else {
				ScriptProcessor other = shared.processors.get(function.script());
				if (other != null)
					//	System.out.println(String.format("%s calls out to %s::%s", parser.script().name(), other.script(), function.name()));
					other.reportProblemsOfFunction(function);
			}
		}

		@Override
		public void incompatibleTypes(ASTNode node, IRegion region, IType left, IType right) {
			try {
				if (left == null)
					left = PrimitiveType.ANY;
				if (right == null)
					right = PrimitiveType.ANY;
				markers().marker(parser, ParserErrorCode.IncompatibleTypes, node, region.getOffset(), region.getOffset()+region.getLength(), Markers.NO_THROW,
					typing == Typing.Static ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
					left.typeName(true), right.typeName(true)
				);
			} catch (ParsingException e) {}
		}

		/**
		 * Let an expression report errors. Calling {@link ASTNode#reportProblems(C4ScriptParser)} indirectly like that ensures
		 * that error markers created will be decorated with information about the expression reporting the error.
		 * @param expression The expression to report errors.
		 * @throws ParsingException
		 * @return The expression parameter is returned to allow for expression chaining.
		 */
		public <T extends ASTNode> T reportProblemsOf(T expression, boolean recursive) throws ParsingException {
			if (expression == null)
				return null;
			ASTNode saved = reportingNode;
			reportingNode = expression;
			try {
				ProblemReporter<? super T> reporter = reporter(expression);
				if (recursive && !reporter.skipReportingProblemsForSubElements())
					for (ASTNode e : expression.subElements())
						if (e != null)
							reportProblemsOf(e, true);
				reporter.reportProblems(expression, this);
			} finally {
				reportingNode = saved;
			}
			return expression;
		}

		public TypeEnvironment newTypeEnvironment() {
			TypeEnvironment l = new TypeEnvironment();
			l.up = this.typeEnvironment;
			this.typeEnvironment = l;
			return l;
		}

		public void endTypeEnvironment(boolean inject, boolean ignoreLocals) {
			if (inject && typeEnvironment.up != null)
				typeEnvironment.up.inject(typeEnvironment, ignoreLocals);
			typeEnvironment = typeEnvironment.up;
		}

		/**
		 * Requests type information for an expression
		 * @param expression the expression
		 * @return the type information or null if none has been stored
		 */
		public ITypeInfo requestTypeInfo(ASTNode expression) {
			if (typeEnvironment == null || typing == Typing.Static || typing == Typing.Dynamic)
				return null;
			boolean topMostLayer = true;
			ITypeInfo base = null;
			for (TypeEnvironment list = typeEnvironment; list != null; list = list.up) {
				for (ITypeInfo info : list)
					if (info.storesTypeInformationFor(expression, this))
						if (!topMostLayer) {
							base = info;
							break;
						}
						else
							return info;
				topMostLayer = false;
			}
			ITypeInfo newlyCreated = reporter(expression).createTypeInfo(expression, this);
			if (newlyCreated != null) {
				if (base != null)
					newlyCreated.merge(base);
				typeEnvironment.add(newlyCreated);
			}
			return newlyCreated;
		}

		/**
		 * Query the type of an arbitrary expression. With some luck the parser will be able to give an answer.
		 * @param expression the expression to query the type of
		 * @return The typeinfo or null if nothing was found
		 */
		public ITypeInfo queryTypeInfo(ASTNode expression) {
			if (typeEnvironment == null)
				return null;
			for (TypeEnvironment list = typeEnvironment; list != null; list = list.up)
				for (ITypeInfo info : list)
					if (info.storesTypeInformationFor(expression, this))
						return info;
			return null;
		}

		/**
		 * Look up stored type information for the passed expression, defaulting to the specified type if no
		 * information could be found.
		 * @param expression The expression to query the type for
		 * @param defaultType Default type to return if no type was found.
		 * @return Expression type as deduced by usage of the expression or the default type.
		 */
		@Override
		public IType queryTypeOfExpression(ASTNode expression, IType defaultType) {
			ITypeInfo info = queryTypeInfo(expression);
			return info != null ? info.type() : defaultType;
		}

		private boolean createWarningAtDeclarationOfVariable(
			ASTNode[] statements,
			Variable variable,
			ParserErrorCode code,
			Object... format
		) {
			for (ASTNode s : statements)
				for (VarDeclarationStatement decl : s.collectionExpressionsOfType(VarDeclarationStatement.class))
					for (VarInitialization initialization : decl.variableInitializations())
						if (initialization.variable == variable) {
							ASTNode old = reportingNode;
							reportingNode = decl;
							markers().warning(parser, code, initialization, initialization, 0, format);
							reportingNode = old;
							return true;
						}
			return false;
		}
		/**
		 * Warn about variables declared inside the given block that have not been referenced elsewhere ({@link Variable#isUsed() == false})
		 * @param func The function the block belongs to.
		 * @param block The {@link Block}
		 */
		public void warnAboutPossibleProblemsWithFunctionLocalVariables(Function func, ASTNode[] statements) {
			if (func == null)
				return;
			if (UNUSEDPARMWARNING)
				for (Variable p : func.parameters())
					if (!p.isUsed())
						markers().warning(parser, ParserErrorCode.UnusedParameter, null, p, Markers.ABSOLUTE_MARKER_LOCATION, p.name());
			if (func.localVars() != null)
				for (Variable v : func.localVars()) {
					if (!v.isUsed())
						createWarningAtDeclarationOfVariable(statements, v, ParserErrorCode.Unused, v.name());
					Variable shadowed = parser.script().findVariable(v.name());
					// ignore those pesky static variables from scenario scripts
					if (shadowed != null && !(shadowed.parentDeclaration() instanceof Scenario))
						createWarningAtDeclarationOfVariable(statements, v, ParserErrorCode.IdentShadowed, v.qualifiedName(), shadowed.qualifiedName());
				}
		}
		public void reportProblemsOf(Function f, ASTNode[] statements, boolean onlyTypeLocals) throws ParsingException {
			assignReporters(f);
			newTypeEnvironment();
			{
				if (f != null)
					for (Variable p : f.parameters())
						if (p.type() == PrimitiveType.UNKNOWN) {
							ITypeInfo varTypeInfo = requestTypeInfo(new AccessVar(p));
							if (varTypeInfo != null)
								varTypeInfo.storeType(p.parameterType());
						}
				for (ASTNode s : statements)
					reportProblemsOf(s, true);
			}
			typeEnvironment.apply(this, onlyTypeLocals);
			endTypeEnvironment(true, true);
			warnAboutPossibleProblemsWithFunctionLocalVariables(f, statements);
			clearReporters(f);
		}

		@Override
		public void reportProblems() {
			newTypeEnvironment();
			for (Variable v : script().variables()) {
				ASTNode init = v.initializationExpression();
				if (init != null) {
					Function owningFunc = as(init.owningDeclaration(), Function.class);
					if (owningFunc == null) {
						newTypeEnvironment();
						{
							assignReporters(init);
							try { reportProblemsOf(init, true); }
							catch (ParsingException e) {}
							judgement(new AccessVar(v), ty(init, this), TypingJudgementMode.Force, this);
							clearReporters(init);
						}
						endTypeEnvironment(true, true);
					}
					if (v.scope() == Scope.CONST && !init.isConstant())
						try {
							markers().error(parser, ParserErrorCode.ConstantValueExpected, init,
								owningFunc == null ? init : owningFunc.bodyLocation().add(init),
									Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, v.name());
						} catch (ParsingException e) {}
				}
			}
			for (Function f : parser.script().functions())
				reportProblemsOfFunction(f);
			typeEnvironment.apply(this, false);
			for (Variable v : parser.script().variables())
				if (v.scope() == Scope.CONST)
					v.lockType();
			endTypeEnvironment(false, false);
		}
		private void clearReporters(ASTNode node) {
			node.traverse(new IASTVisitor<Void>() {
				@Override
				public TraversalContinuation visitNode(ASTNode node, Void parser) {
					node.temporaryProblemReportingObject = null;
					return TraversalContinuation.Continue;
				}
			}, null);
		}
		private void assignReporters(ASTNode node) {
			node.traverse(new IASTVisitor<Void>() {
				@Override
				public TraversalContinuation visitNode(ASTNode node, Void parser) {
					node.temporaryProblemReportingObject = findReporter(node);
					return TraversalContinuation.Continue;
				}
			}, null);
		}

		@Override
		public void run() {
			if (shared.monitor.isCanceled())
				return;
			shared.monitor.subTask(String.format("Reporting problems for '%s'", script().name()));
			synchronized (working) {
				reportProblems();
			}
			shared.monitor.worked(1);
		}

		@Override
		public Object[] arguments() {
			return parser.arguments();
		}

		@Override
		public Function function() {
			return parser.function();
		}

		@Override
		public int codeFragmentOffset() {
			return parser.codeFragmentOffset();
		}

		@Override
		public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) {
			parser.reportOriginForExpression(expression, location, file);
		}

		@Override
		public Object valueForVariable(String varName) {
			return ASTNode.EVALUATION_COMPLEX;
		}

		@Override
		public void storeType(ASTNode node, IType type) {
			ITypeInfo requested = requestTypeInfo(node);
			if (requested != null)
				requested.storeType(type);
		}

		@Override
		public Definition definition() { return parser.definition(); }
		@Override
		public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) { return parser.absoluteSourceLocationFromExpr(expression); }
		@Override
		public CachedEngineDeclarations cachedEngineDeclarations() { return cachedEngineDeclarations; }
		@Override
		public Script script() { return parser.script(); }
		@Override
		public IFile file() { return script().scriptFile(); }
		@Override
		public Declaration container() { return script(); }
		@Override
		public int fragmentOffset() { return parser.fragmentOffset(); }
		@Override
		public IType typeOf(ASTNode node) { return ty(node, this); }
		@Override
		public BufferedScanner scanner() { return parser; }

		@Override
		public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) {
			@SuppressWarnings("unchecked")
			AccessDeclarationProblemReporter<T> reporter = (AccessDeclarationProblemReporter<T>)reporter(access);
			return reporter.obtainDeclaration(access, this);
		}

		@Override
		public boolean validForType(ASTNode node, IType type) {
			return reporter(node).validForType(node, type, this);
		}

		@Override
		public void assignment(ASTNode leftSide, ASTNode rightSide) {
			reporter(leftSide).assignment(leftSide, rightSide, this);
		}

		@Override
		public void typingJudgement(ASTNode node, IType type, TypingJudgementMode mode) {
			reporter(node).typingJudgement(node, type, this, mode);
		}

		@Override
		@SuppressWarnings("unchecked")
		public final <T extends IType> T typeOf(ASTNode node, Class<T> cls) {
			for (IType t : typeOf(node))
				if (cls.isInstance(t))
					return (T)t;
			return null;
		}

		public final <T extends ASTNode> ProblemReporter<? super T> reporter(T node) {
			return DabbleInference.this.reporter(node);
		}

		@Override
		public Markers markers() {
			return DabbleInference.this.markers();
		}
	}

	class ProblemReporter<T extends ASTNode> extends PerClass<ASTNode, T, ProblemReporter<? super T>> {
		public ProblemReporter(Class<T> cls) { super(cls); }
		public void findSuper() {
			for (Class<? super T> s = cls.getSuperclass(); s != null; s = s.getSuperclass()) {
				@SuppressWarnings("unchecked")
				ProblemReporter<? super T> sup = (ProblemReporter<? super T>)problemReporters.get(s);
				if (sup != null) {
					supr = sup;
					return;
				}
			}
			supr = NULL_REPORTER;
		}

		/**
		 * Returning true tells the {@link ScriptProcessor} to not recursively call {@link #reportProblems(ASTNode, ScriptProcessor)} on {@link ASTNode#subElements()}
		 * @return Do you just show up, play the music,
		 */
		public boolean skipReportingProblemsForSubElements() {return false;}
		public void reportProblems(T node, ScriptProcessor processor) throws ParsingException {}

		public IType unresolvedType(T node, ScriptProcessor processor) { return processor.queryTypeOfExpression(node, PrimitiveType.UNKNOWN); }

		/**
		 * Return type of the expression, adjusting the result of obtainType under some circumstances
		 * @param context Parser acting as the context (supplying current function, script begin parsed etc.)
		 * @return The type of the expression
		 */
		public IType type(T node, ScriptProcessor processor) {
			IType urt = unresolvedType(node, processor);
			return TypeUtil.resolve(urt, processor, reporter(node).callerType(node, processor));
		}

		public IType callerType(T node, ScriptProcessor processor) {
			ASTNode pred = node.predecessorInSequence();
			if (pred != null)
				return ty(pred, processor);
			else
				return supr.callerType(node, processor);
		}

		public final IType predecessorType(ASTNode node, ScriptProcessor processor) {
			ASTNode p = node.predecessorInSequence();
			return p != null ? ty(p, processor) : null;
		}

		public final <X extends IType> X predecessorTypeAs(ASTNode node, Class<X> cls, ScriptProcessor processor) {
			return as(predecessorType(node, processor), cls);
		}

		public final IType unresolvedPredecessorType(ASTNode node, ScriptProcessor processor) {
			ASTNode e = node;
			//for (e = predecessorInSequence; e != null && e instanceof MemberOperator; e = e.predecessorInSequence);
			return e != null && e.predecessorInSequence() != null ? reporter(e.predecessorInSequence()).unresolvedType(e.predecessorInSequence(), processor) : null;
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final boolean validForType(ASTNode node, IType type, ScriptProcessor processor) {
			if (type == null)
				return true;
			IType myType = ty(node, processor);
			return type.canBeAssignedFrom(myType);
		}

		public boolean typingJudgement(T node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
//			if (DEBUG)
//				markers().warning(processor, ParserErrorCode.TypingJudgment, node, node, Markers.NO_THROW, node.printed(), type.typeName(true));
			ITypeInfo info;
			switch (mode) {
			case Expect:
				info = processor.requestTypeInfo(node);
				if (info != null)
					if (info.type() == PrimitiveType.UNKNOWN || info.type() == PrimitiveType.ANY) {
						info.storeType(type);
						return true;
					} else
						return false;
				return true;
			case Force:
				info = processor.requestTypeInfo(node);
				if (info != null) {
					info.storeType(type);
					return true;
				} else
					return false;
			case Hint:
				info = processor.queryTypeInfo(node);
				return info == null || info.hint(type);
			case Unify:
				info = processor.requestTypeInfo(node);
				if (info != null) {
					info.storeType(TypeUnification.unify(info.type(), type));
					return true;
				} else
					return false;
			default:
				return false;
			}
		}

		public void assignment(T leftSide, ASTNode rightSide, ScriptProcessor processor) {
			if (processor.typing == Typing.Static) {
				IType leftTy = ty(leftSide, this, processor);
				IType rightTy = ty(rightSide, processor);
				if (!leftTy.canBeAssignedFrom(rightTy))
					processor.incompatibleTypes(rightSide, rightSide, leftTy, rightTy);
			} else
				judgement(leftSide, ty(rightSide, processor), TypingJudgementMode.Force, processor);
			//processor.linkTypesOf(this, rightSide);
		}

		public ITypeInfo createTypeInfo(T node, ScriptProcessor processor) {
			ITypeable d = GenericTypeInfo.typeableFromExpression(node, processor);
			if (d != null && !d.staticallyTyped())
				return new GenericTypeInfo(node, processor);
			return null;
		}

		@Override
		public String toString() { return String.format("ProblemReporter<%s>", cls.getSimpleName()); }
	}

	class AccessDeclarationProblemReporter<T extends AccessDeclaration> extends ProblemReporter<T> {
		public AccessDeclarationProblemReporter(Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(T node, ScriptProcessor processor) {
			return null;
		}
		@Override
		public void reportProblems(T node, ScriptProcessor processor) throws ParsingException {
			supr.reportProblems(node, processor);
			internalObtainDeclaration(node, processor);
		}
		protected final Declaration internalObtainDeclaration(T node, ScriptProcessor processor) {
			if (node.declaration() == null)
				node.setDeclaration(obtainDeclaration(node, processor));
			if (node.declaration() == null) {
				processor.script().index().loadScriptsContainingDeclarationsNamed(node.declarationName());
				node.setDeclaration(obtainDeclaration(node, processor));
			}
			return node.declaration();
		}
		@Override
		public ITypeInfo createTypeInfo(T node, ScriptProcessor processor) {
			if (node.declaration() instanceof ITypeable && ((ITypeable)node.declaration()).staticallyTyped())
				return null;
			else
				return super.createTypeInfo(node, processor);
		}
	}

	private final ProblemReporter<ASTNode> NULL_REPORTER = new ProblemReporter<ASTNode>(ASTNode.class) {
		@Override
		public IType unresolvedType(ASTNode node, ScriptProcessor processor) {
			return PrimitiveType.UNKNOWN;
		}
		@Override
		public IType callerType(ASTNode node, ScriptProcessor processor) {
			return PrimitiveType.UNKNOWN;
		}
	};

	private final <T extends ASTNode> ProblemReporter<? super T> findReporter(T node) {
		for (Class<?> cls = node.getClass(); cls != null; cls = cls.getSuperclass()) {
			@SuppressWarnings("unchecked")
			ProblemReporter<? super T> reporter = (ProblemReporter<? super T>)problemReporters.get(cls);
			if (reporter != null)
				return reporter;
		}
		return NULL_REPORTER;
	}

	@SuppressWarnings("unchecked")
	private final <T extends ASTNode> ProblemReporter<? super T> reporter(T node) {
		if (node.temporaryProblemReportingObject != null)
			return (ProblemReporter<? super T>)node.temporaryProblemReportingObject;
		else
			return findReporter(node);
	}

	public final IType ty(ASTNode node, ScriptProcessor processor) {
		return ty(node, reporter(node), processor);
	}

	public final <T extends ASTNode> IType ty(T node, ProblemReporter<T> reporter, ScriptProcessor processor) {
		IType type = reporter.type(node, processor);
		node.inferredType(type);
		return type;
	}

	public final void judgement(ASTNode node, IType type, TypingJudgementMode mode, ScriptProcessor processor) {
		reporter(node).typingJudgement(node, type, processor, mode);
	}

	private final Map<Class<? extends ASTNode>, ProblemReporter<? extends ASTNode>> problemReporters = new HashMap<Class<? extends ASTNode>, ProblemReporter<?>>();
	{
		@SuppressWarnings("rawtypes")
		ProblemReporter<?>[] reporters = new ProblemReporter[] {

			new AccessDeclarationProblemReporter<AccessDeclaration>(AccessDeclaration.class),

			new AccessDeclarationProblemReporter<AccessVar>(AccessVar.class) {
				@Override
				public Declaration obtainDeclaration(AccessVar node, ScriptProcessor processor) {
					((AccessDeclarationProblemReporter<? super AccessVar>)supr).obtainDeclaration(node, processor);
					ASTNode sequencePredecessor = node.predecessorInSequence();
					IType type = processor.script();
					if (sequencePredecessor != null)
						type = processor.queryTypeOfExpression(sequencePredecessor, null);
					if (type != null) for (IType t : type) {
						Script scriptToLookIn;
						if ((scriptToLookIn = Definition.scriptFrom(t)) == null) {
							// find pseudo-variable from proplist expression
							if (t instanceof IProplistDeclaration) {
								Variable proplistComponent = ((IProplistDeclaration)t).findComponent(node.declarationName());
								if (proplistComponent != null)
									return proplistComponent;
							}
						} else {
							FindDeclarationInfo info = new FindDeclarationInfo(processor.script().index());
							info.contextFunction = sequencePredecessor == null ? node.parentOfType(Function.class) : null;
							info.searchOrigin = scriptToLookIn;
							info.findGlobalVariables = sequencePredecessor == null;
							Declaration v = scriptToLookIn.findDeclaration(node.declarationName(), info);
							if (v instanceof Definition)
								//	context.performParsingPhaseTwo((Definition)v);
								v = ((Definition)v).proxyVar();
							if (v != null)
								return v;
						}
					}
					return null;
				}
				@Override
				public IType unresolvedType(AccessVar node, ScriptProcessor processor) {
					Declaration d = internalObtainDeclaration(node, processor);
					// declarationFromContext(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
					if (d == Variable.THIS)
						if (processor.script() instanceof Definition)
							return ((Definition)processor.script()).thisType();
						else
							return new ConstrainedProplist(processor.script(), ConstraintKind.CallerType, true, true);
					IType stored = processor.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;
					if (d instanceof Function)
						return new FunctionType((Function)d);
					else if (d instanceof ITypeable)
						return ((ITypeable) d).type();
					//return new SameTypeAsSomeTypeable((ITypeable)d);
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public IType callerType(AccessVar node, ScriptProcessor processor) {
					Variable v = as(node.declaration(), Variable.class);
					if (v != null) switch (v.scope()) {
					case CONST: case STATIC:
						return null;
					default:
						break;
					}
					return super.callerType(node, processor);
				}
				@Override
				public boolean typingJudgement(AccessVar node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
					if (node.declaration() == Variable.THIS)
						return true;
					return super.typingJudgement(node, type, processor, mode);
				}
				@Override
				public void reportProblems(AccessVar node, ScriptProcessor processor) throws ParsingException {
					super.reportProblems(node, processor);
					ASTNode pred = node.predecessorInSequence();
					Declaration declaration = node.declaration();
					if (declaration == null && pred == null)
						markers().error(processor.parser, ParserErrorCode.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.declarationName());
					// local variable used in global function
					else if (declaration instanceof Variable) {
						Variable var = (Variable) declaration;
						var.setUsed(true);
						switch (var.scope()) {
						case LOCAL:
							Declaration d = node.parentOfType(Declaration.class);
							if (d != null && pred == null) {
								Function f = d.topLevelParentDeclarationOfType(Function.class);
								Variable v = d.topLevelParentDeclarationOfType(Variable.class);
								if (
									(f != null && f.visibility() == FunctionScope.GLOBAL) ||
									(f == null && v != null && v.scope() != Scope.LOCAL)
								)
									markers().error(processor.parser, ParserErrorCode.LocalUsedInGlobal, node, node, Markers.NO_THROW);
							}
							break;
						case STATIC: case CONST:
							processor.parser.script().addUsedScript(var.script());
							break;
						case VAR:
							if (processor.parser.currentFunction() != null && var.parentDeclaration() == processor.parser.currentFunction()) {
								int locationUsed = processor.parser.currentFunction().bodyLocation().getOffset()+node.start();
								if (locationUsed < var.start())
									markers().warning(processor.parser, ParserErrorCode.VarUsedBeforeItsDeclaration, node, node, 0, var.name());
							}
							break;
						case PARAMETER:
							break;
						}
					} else if (declaration instanceof Function)
						if (!processor.parser.script().engine().settings().supportsFunctionRefs)
							markers().error(processor.parser, ParserErrorCode.FunctionRefNotAllowed, node, node, Markers.NO_THROW, processor.parser.script().engine().name());
				}
				public void initializeFromAssignment(Variable var, ASTNode referee, ASTNode expression, ScriptProcessor processor) {
					IType type = ty(expression, processor);
					var.expectedToBeOfType(type, TypingJudgementMode.Expect);
					var.setLocation(processor.absoluteSourceLocationFromExpr(referee));
					var.forceType(type);
					var.setInitializationExpression(expression);
				}
				@Override
				public void assignment(AccessVar leftSide, ASTNode rightSide, ScriptProcessor processor) {
					Declaration declaration = leftSide.declaration();
					if (declaration == Variable.THIS)
						return;
					if (declaration == null) {
						IType predType = predecessorType(leftSide, processor);
						if (predType != null && predType.canBeAssignedFrom(PrimitiveType.PROPLIST))
							if (predType instanceof IProplistDeclaration) {
								IProplistDeclaration proplDecl = (IProplistDeclaration) predType;
								if (proplDecl.isAdHoc()) {
									Variable var = new Variable(leftSide.declarationName(), Variable.Scope.VAR);
									initializeFromAssignment(var, leftSide, rightSide, processor);
									proplDecl.addComponent(var, true);
									declaration = var;
								}
							} else for (IType t : predType)
								if (t == processor.script()) {
									Variable var = new Variable(leftSide.declarationName(), Variable.Scope.LOCAL);
									initializeFromAssignment(var, leftSide, rightSide, processor);
									processor.script().addDeclaration(var);
									declaration = var;
									break;
								}
					}
					super.assignment(leftSide, rightSide, processor);
				}
				@Override
				public ITypeInfo createTypeInfo(AccessVar node, ScriptProcessor processor) {
					if (node.declaration() instanceof Variable && node.declaration().parentDeclaration() instanceof Function)
						return new LocalVariableInfo(node);
					else
						return super.createTypeInfo(node, processor);
				}
			},

			new ProblemReporter<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType unresolvedType(ArrayExpression node, final ScriptProcessor processor) {
					return new ArrayType(
						null,
						ArrayUtil.map(node.subElements(), IType.class, new IConverter<ASTNode, IType>() {
							@Override
							public IType convert(ASTNode from) {
								return from != null ? ty(from, processor) : PrimitiveType.UNKNOWN;
							}
						})
						);
				}
			},

			new ProblemReporter<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType unresolvedType(ArrayElementExpression node, ScriptProcessor processor) {
					IType t = supr.unresolvedType(node, processor);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					ASTNode pred = node.predecessorInSequence();
					if (pred != null)
						for (IType ty : ty(pred, processor)) {
							ArrayType at = as(ty, ArrayType.class);
							if (at != null)
								return at.typeForElementWithIndex(ASTNode.evaluateAtParseTime(node.argument(), processor));
						}
					return PrimitiveType.ANY;
				}
				@Override
				public void assignment(ArrayElementExpression leftSide, ASTNode rightSide, ScriptProcessor processor) {
					IType predType_ = predecessorType(leftSide, processor);
					for (IType predType : predType_) {
						ArrayType arrayType = as(predType, ArrayType.class);
						IType rightSideType = ty(rightSide, processor);
						ASTNode pred = leftSide.predecessorInSequence();
						if (arrayType != null) {
							Object argEv = ASTNode.evaluateAtParseTime(leftSide.argument(), processor);
							IType mutation;
							if (argEv instanceof Number)
								mutation = arrayType.modifiedBySliceAssignment(
									argEv,
									((Number)argEv).intValue()+1,
									new ArrayType(rightSideType, rightSideType)
								);
							else
								mutation = new ArrayType(
									TypeUnification.unify(rightSideType, arrayType.generalElementType()),
									ArrayType.NO_PRESUMED_LENGTH
								);
							processor.storeType(pred, mutation);
							break;
						} else if (predType == PrimitiveType.UNKNOWN || predType == PrimitiveType.ARRAY)
							judgement(
								pred,
								new ArrayType(rightSideType, ArrayType.NO_PRESUMED_LENGTH),
								TypingJudgementMode.Force,
								processor
							);
					}
				}
				@Override
				public void reportProblems(ArrayElementExpression node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					IType type = predecessorType(node, processor);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					ASTNode arg = node.argument();
					if (arg == null)
						markers().warning(processor.parser, ParserErrorCode.MissingExpression, node, node, 0);
					else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
						IType argType = ty(arg, processor);
						ASTNode pred = node.predecessorInSequence();
						if (argType == PrimitiveType.STRING) {
							if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
								markers().warning(processor.parser, ParserErrorCode.NotAProplist, node, pred, 0);
							else
								judgement(pred, PrimitiveType.PROPLIST, TypingJudgementMode.Unify, processor);
						}
						else if (argType == PrimitiveType.INT)
							if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
								markers().warning(processor.parser, ParserErrorCode.NotAnArrayOrProplist, node, pred, 0);
							else
								reporter(pred).typingJudgement(pred, PrimitiveType.ARRAY, processor, TypingJudgementMode.Unify);
					}
				}
			},

			new ProblemReporter<ArraySliceExpression>(ArraySliceExpression.class) {
				private void warnIfNotArray(ASTNode node, ScriptProcessor processor, IType type) {
					if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
						TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
						TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
					{
						TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type);
						TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type);
						markers().warning(processor.parser, ParserErrorCode.NotAnArrayOrProplist, node, node, 0);
					}
				}
				@Override
				public void reportProblems(ArraySliceExpression node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					IType type = predecessorType(node, processor);
					warnIfNotArray(node.predecessorInSequence(), processor, type);
				}
			},

			new ProblemReporter<OperatorExpression>(OperatorExpression.class) {
				@Override
				public IType unresolvedType(OperatorExpression node, ScriptProcessor processor) {
					return node.operator().resultType();
				}
			},

			new ProblemReporter<BinaryOp>(BinaryOp.class) {
				@Override
				public IType unresolvedType(BinaryOp node, ScriptProcessor processor) {
					switch (node.operator()) {
					// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
					case And: case Or: case JumpNotNil:
						IType leftSideType = ty(node.leftSide(), processor);
						IType rightSideType = ty(node.rightSide(), processor);
						if (leftSideType == rightSideType)
							return leftSideType;
						else
							return TypeUnification.unify(leftSideType, rightSideType);
					case Assign:
						return ty(node.rightSide(), processor);
					default:
						return supr.unresolvedType(node, processor);
					}
				}
				@Override
				public void reportProblems(BinaryOp node, ScriptProcessor processor) throws ParsingException {
					final Operator op = node.operator();
					// sanity
					ASTNode left = node.leftSide();
					ASTNode right = node.rightSide();
					node.setLocation(left.start(), right.end());
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !left.isModifiable(processor.parser))
						markers().error(processor.parser, ParserErrorCode.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2impor
					if ((op == Operator.StringEqual || op == Operator.ne) && (processor.strictLevel >= 2))
						markers().warning(processor.parser, ParserErrorCode.ObsoleteOperator, node, node, 0, op.operatorName());
					// wrong parameter types
					if (!validForType(left, op.firstArgType(), processor))
						processor.incompatibleTypes(node, left, op.firstArgType(), ty(left, processor));
					if (!validForType(right, op.secondArgType(), processor))
						processor.incompatibleTypes(node, right, op.secondArgType(), ty(right, processor));

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
						reporter(left).assignment(left, right, processor);
						break;
					default:
						break;
					}

					if (expectedLeft != null)
						judgement(left, expectedLeft, TypingJudgementMode.Unify, processor);
					if (expectedRight != null)
						judgement(right, expectedRight, TypingJudgementMode.Unify, processor);
				}
				@Override
				public ITypeInfo createTypeInfo(BinaryOp node, ScriptProcessor processor) {
					ASTNode leftSide = node.leftSide();
					if (node.operator() == Operator.Assign && leftSide != null)
						return reporter(leftSide).createTypeInfo(leftSide, processor);
					return super.createTypeInfo(node, processor);
				}
			},

			new ProblemReporter<UnaryOp>(UnaryOp.class) {
				@Override
				public void reportProblems(UnaryOp node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !arg.isModifiable(processor.parser))
						markers().error(processor.parser, ParserErrorCode.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					ProblemReporter<? super ASTNode> rarg = reporter(arg);
					if (!rarg.validForType(arg, node.operator().firstArgType(), processor))
						processor.incompatibleTypes(node, arg, node.operator().firstArgType(),
							ty(arg, rarg, processor));
					rarg.typingJudgement(arg, node.operator().firstArgType(), processor, TypingJudgementMode.Expect);
				}
			},

			new ProblemReporter<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void reportProblems(BoolLiteral node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					if (node.parent() instanceof BinaryOp) {
						Operator op = ((BinaryOp) node.parent()).operator();
						if (op == Operator.And || op == Operator.Or)
							markers().warning(processor.parser, ParserErrorCode.BoolLiteralAsOpArg, node, node, 0, this.toString());
					}
				}
			},

			new ProblemReporter<ContinueStatement>(ContinueStatement.class) {
				@Override
				public void reportProblems(ContinueStatement node, ScriptProcessor processor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						markers().error(processor.parser, ParserErrorCode.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.reportProblems(node, processor);
				}
			},

			new ProblemReporter<BreakStatement>(BreakStatement.class) {
				@Override
				public void reportProblems(BreakStatement node, ScriptProcessor processor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						markers().error(processor.parser, ParserErrorCode.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.reportProblems(node, processor);
				}
			},

			new ProblemReporter<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(ScriptProcessor processor, ASTNode node, boolean tupleIsError) throws ParsingException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							markers().error(processor.parser, ParserErrorCode.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (processor.strictLevel >= 2)
							markers().error(processor.parser, ParserErrorCode.ReturnAsFunction, node, node, Markers.NO_THROW);
					ASTNode[] subElms = node.subElements();
					for (ASTNode e : subElms)
						warnAboutTupleInReturnExpr(processor, e, true);
				}
				@Override
				public void reportProblems(ReturnStatement node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(processor, returnExpr, false);
					Function currentFunction = node.parentOfType(Function.class);
					if (currentFunction == null)
						markers().error(processor.parser, ParserErrorCode.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (processor.typing == Typing.Static && currentFunction.staticallyTyped()) {
							if (!reporter(returnExpr).validForType(returnExpr, currentFunction.returnType(), processor))
								processor.incompatibleTypes(node,
									returnExpr, currentFunction.returnType(), ty(returnExpr, processor));
						}
						else {
							IType type = reporter(returnExpr).unresolvedType(returnExpr, processor);
							CallDeclaration dummy = new CallDeclaration(currentFunction);
							judgement(dummy, type, TypingJudgementMode.Unify, processor);
							//parser.linkTypesOf(dummy, returnExpr);
						}
				}
			},

			new AccessDeclarationProblemReporter<CallDeclaration>(CallDeclaration.class) {
				/**
				 * Find a {@link Function} for some hypothetical {@link CallDeclaration}, using contextual information such as the {@link ASTNode#type(ProblemReportingContext)} of the {@link ASTNode} preceding this {@link CallDeclaration} in the {@link Sequence}.
				 * @param pred The predecessor of the hypothetical {@link CallDeclaration} ({@link ASTNode#predecessorInSequence()})
				 * @param functionName Name of the function to look for. Would correspond to the hypothetical {@link CallDeclaration}'s {@link #declarationName()}
				 * @param processor Context to use for searching
				 * @param listToAddPotentialDeclarationsTo When supplying a non-null value to this parameter, potential declarations will be added to the collection. Such potential declarations would be obtained by querying the {@link Index}'s {@link Index#declarationMap()}.
				 * @return The {@link Function} that is very likely to be the one actually intended to be referenced by the hypothetical {@link CallDeclaration}.
				 */
				private Declaration findFunction(
					CallDeclaration node,
					String functionName, IType callerType,
					ScriptProcessor processor,
					Set<IIndexEntity> listToAddPotentialDeclarationsTo
				) {
					IType lookIn = callerType != null ? callerType : processor.script();
					if (lookIn != null) for (IType ty : lookIn) {
						if (!(ty instanceof IHasConstraint))
							continue;
						Script script = as(((IHasConstraint)ty).constraint(), Script.class);
						if (script == null)
							continue;
						FindDeclarationInfo info = new FindDeclarationInfo(processor.script().index());
						info.searchOrigin = processor.script();
						info.contextFunction = node.parentOfType(Function.class);
						info.findGlobalVariables = callerType == null;
						Declaration dec = script.findDeclaration(functionName, info);
						// parse function before this one
						if (dec instanceof Function && node.parentOfType(Function.class) != null)
							processor.reportProblemsOfFunction((Function)dec);
						if (dec != null)
							if (listToAddPotentialDeclarationsTo == null)
								return dec;
							else
								listToAddPotentialDeclarationsTo.add(dec);
					}
					if (callerType != null) {
						// find global function
						Declaration declaration;
						try {
							declaration = processor.script().index().findGlobal(Declaration.class, functionName);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
						// find engine function
						if (declaration == null)
							declaration = processor.script().index().engine().findFunction(functionName);

						List<Declaration> allFromLocalIndex = processor.script().index().declarationMap().get(functionName);
						Declaration decl = processor.script().engine().findLocalFunction(functionName, false);
						int numCandidates = 0;
						if (allFromLocalIndex != null)
							numCandidates += allFromLocalIndex.size();
						if (decl != null)
							numCandidates++;

						// only return found global function if it's the only choice
						if (declaration != null && numCandidates == 1)
							if (listToAddPotentialDeclarationsTo == null)
								return declaration;
							else
								listToAddPotentialDeclarationsTo.add(declaration);
					}
					if (listToAddPotentialDeclarationsTo != null && listToAddPotentialDeclarationsTo.size() > 0)
						return ArrayUtil.filteredIterable(listToAddPotentialDeclarationsTo, Declaration.class).iterator().next();
					else
						return null;
				}
				protected Declaration _obtainDeclaration(CallDeclaration node, Set<IIndexEntity> potentialDeclarationsOutput, ScriptProcessor processor) {
					String declarationName = node.declarationName();
					if (declarationName.equals(Keywords.Return))
						return null;
					if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited)) {
						Function activeFunc = node.parentOfType(Function.class);
						if (activeFunc != null) {
							Function inher = activeFunc.inheritedFunction();
							if (inher != null) {
								if (potentialDeclarationsOutput != null)
									potentialDeclarationsOutput.add(inher);
								return inher;
							}
						}
					}
					IType unresolvedPredecessorType = unresolvedPredecessorType(node, processor);
					ASTNode p = node.predecessorInSequence();
					if (p instanceof MemberOperator)
						p = p.predecessorInSequence();
					if (potentialDeclarationsOutput != null)
						node.setPotentialDeclarations(potentialDeclarationsOutput);
					return findFunction(node, declarationName,
						unresolvedPredecessorType != null
						? TypeUtil.resolve(unresolvedPredecessorType, processor, reporter(p).unresolvedPredecessorType(p, processor))
							: null, processor, potentialDeclarationsOutput);
				}
				@Override
				protected Declaration obtainDeclaration(CallDeclaration node, ScriptProcessor processor) {
					((AccessDeclarationProblemReporter<? super CallDeclaration>)supr).obtainDeclaration(node, processor);
					return _obtainDeclaration(node, null, processor);
				}
				private IType declarationType(CallDeclaration node, ScriptProcessor processor) {
					Declaration d = internalObtainDeclaration(node, processor);

					// look for gathered type information
					IType stored = processor.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && (d == processor.cachedEngineDeclarations().This || d == Variable.THIS)) {
						Definition obj = processor.definition();
						if (obj != null)
							return obj;
					}

					if (d instanceof Function) {
						// Some special rule applies and the return type is set accordingly
						SpecialFuncRule rule = node.specialRuleFromContext(processor, SpecialEngineRules.RETURNTYPE_MODIFIER);
						Function f = (Function)d;
						return f.returnType();
					}
					if (d instanceof Variable)
						return ((Variable)d).type();

					return supr != null ? supr.unresolvedType(node, processor) : PrimitiveType.UNKNOWN;
				}
				private boolean unknownFunctionShouldBeError(CallDeclaration node, ScriptProcessor processor) {
					ASTNode pred = node.predecessorInSequence();
					// stand-alone function? always bark!
					if (pred == null)
						return true;
					// not typed? weird
					IType predType = ty(pred, processor);
					if (predType == null)
						return false;
					// called via ~? ok
					if (pred instanceof MemberOperator)
						if (((MemberOperator)pred).hasTilde())
							return false;
					// wat
					boolean anythingNonPrimitive = false;
					// allow this->Unknown()
					if (predType instanceof IHasConstraint && ((IHasConstraint)predType).constraintKind() == ConstraintKind.CallerType)
						return false;
					for (IType t : predType) {
						if (t instanceof PrimitiveType)
							continue;
						if (!(t instanceof IHasConstraint))
							return false;
						else {
							IHasConstraint hasConstraint = (IHasConstraint) t;
							anythingNonPrimitive = true;
							// something resolved to something less specific than a ScriptBase? drop
							if (!(hasConstraint.resolve(processor, reporter(node).callerType(node, processor)) instanceof Script))
								return false;
						}
					}
					return anythingNonPrimitive;
				}
				@Override
				public IType unresolvedType(CallDeclaration node, ScriptProcessor processor) {
					IType type = declarationType(node, processor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public void reportProblems(CallDeclaration node, ScriptProcessor processor) throws ParsingException {
					super.reportProblems(node, processor);

					CachedEngineDeclarations cachedEngineDeclarations = processor.cachedEngineDeclarations();
					String declarationName = node.declarationName();
					Declaration declaration = node.declaration();
					ASTNode[] params = node.params();
					ASTNode predecessor = node.predecessorInSequence();

					// return as function
					if (declarationName.equals(Keywords.Return)) {
						if (processor.strictLevel >= 2)
							markers().error(processor.parser, ParserErrorCode.ReturnAsFunction, node, node, Markers.NO_THROW);
						else
							markers().warning(processor.parser, ParserErrorCode.ReturnAsFunction, node, node, 0);
					}
					else {
						// inherited/_inherited not allowed in non-strict mode
						if (processor.strictLevel <= 0)
							if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited))
								markers().error(processor.parser, ParserErrorCode.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						// variable as function
						if (declaration instanceof Variable) {
							((Variable)declaration).setUsed(true);
							IType type = declarationType(node, processor);
							// no warning when in #strict mode
							if (processor.strictLevel >= 2)
								if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !PrimitiveType.FUNCTION.canBeAssignedFrom(type))
									markers().warning(processor.parser, ParserErrorCode.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
						} else if (declaration instanceof Function) {
							Function f = (Function)declaration;
							if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
								processor.parser.script().addUsedScript(f.script());

							SpecialFuncRule rule = processor.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
							boolean specialCaseHandled =
								rule != null &&
								rule.validateArguments(node, params, processor);

							// not a special case... check regular parameter types
							if (!specialCaseHandled) {
								int givenParam = 0;
								for (Variable parm : f.parameters()) {
									if (givenParam >= params.length)
										break;
									ASTNode given = params[givenParam++];
									if (given == null)
										continue;
									if (!validForType(given, parm.type(), processor))
										processor.incompatibleTypes(node, given, parm.type(), ty(given, processor));
									//else
										//judgement(given, parm.type(), TypingJudgementMode.Unify, processor);
								}
							}
						}
						else if (declaration == null)
							if (unknownFunctionShouldBeError(node, processor)) {
								int start = node.start();
								if (declarationName.equals(Keywords.Inherited)) {
									Function activeFunc = processor.parser.currentFunction();
									if (activeFunc != null)
										markers().error(processor.parser, ParserErrorCode.NoInheritedFunction, node, start, start+declarationName.length(), Markers.NO_THROW, processor.parser.currentFunction().name(), true);
									else
										markers().error(processor.parser, ParserErrorCode.NotAllowedHere, node, start, start+declarationName.length(), Markers.NO_THROW, declarationName);
								}
								// _inherited yields no warning or error
								else if (!declarationName.equals(Keywords.SafeInherited))
									markers().error(processor.parser, ParserErrorCode.UndeclaredIdentifier, node, start, start+declarationName.length(), Markers.NO_THROW, declarationName);
							} else if (predecessor != null && MemberOperator.unforgiving(predecessor))
								judgement(predecessor, new StructuralType(declarationName), TypingJudgementMode.Unify, processor);
					}
				}
				@Override
				public ITypeInfo createTypeInfo(CallDeclaration node, ScriptProcessor processor) {
					Declaration d = node.declaration();
					CachedEngineDeclarations cache = processor.cachedEngineDeclarations();
					if (isAnyOf(d, cache.VarAccessFunctions)) {
						Object ev;
						if (node.params().length == 1 && (ev = node.params()[0].evaluateAtParseTime(node.parentOfType(Function.class))) != null)
							if (ev instanceof Number)
								// Var() with a sane constant number
								return new VarFunctionsTypeInfo(cache.Local == d ? null : node.parentOfType(Function.class), (Function) d, ((Number)ev).intValue());
					}
					else if (d instanceof Function) {
						Function f = (Function) d;
						if (f.staticallyTyped() || f.isEngineDeclaration())
							return null;
						return new FunctionReturnTypeInfo((Function)d);
					}
					else if (d != null)
						return new GenericTypeInfo(node, processor);
					return super.createTypeInfo(node, processor);
				}
			},

			new ProblemReporter<Sequence>(Sequence.class) {
				@Override
				public IType unresolvedType(Sequence node, ScriptProcessor processor) {
					ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
							: reporter(elements[elements.length-1]).unresolvedType(elements[elements.length-1], processor);
				}
				@Override
				public void assignment(Sequence leftSide, ASTNode rightSide, ScriptProcessor processor) {
					ASTNode lastElement = leftSide.lastElement();
					reporter(lastElement).assignment(lastElement, rightSide, processor);
				}
				@Override
				public void reportProblems(Sequence node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					ASTNode p = null;
					for (ASTNode e : node.subElements()) {
						if (
							(e != null && !e.isValidInSequence(p, processor.parser)) ||
							(p != null && !p.allowsSequenceSuccessor(processor.parser, e))
							)
							markers().error(processor.parser, ParserErrorCode.NotAllowedHere, node, e, Markers.NO_THROW, e);
						p = e;
					}
				}
			},

			new ProblemReporter<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType unresolvedType(ArraySliceExpression node, ScriptProcessor processor) {
					ArrayType arrayType = predecessorTypeAs(node, ArrayType.class, processor);
					if (arrayType != null)
						return node.lo() == null && node.hi() == null ? arrayType : arrayType.typeForSlice(
							ASTNode.evaluateAtParseTime(node.lo(), processor),
							ASTNode.evaluateAtParseTime(node.hi(), processor)
							);
					else
						return PrimitiveType.ARRAY;
				}
				@Override
				public void assignment(ArraySliceExpression leftSide, ASTNode rightSide, ScriptProcessor processor) {
					ArrayType arrayType = predecessorTypeAs(leftSide, ArrayType.class, processor);
					IType sliceType = reporter(rightSide).unresolvedType(rightSide, processor);
					if (arrayType != null)
						processor.storeType(leftSide.predecessorInSequence(), arrayType.modifiedBySliceAssignment(
							ASTNode.evaluateAtParseTime(leftSide.lo(), processor),
							ASTNode.evaluateAtParseTime(leftSide.hi(), processor),
							sliceType
							));
				}
			},

			new ProblemReporter<Literal>(Literal.class) {
				@Override
				public boolean typingJudgement(Literal node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
					// constantly steadfast do i resist the pressure of expectancy lied upon me
					return true;
				}
				@Override
				public void assignment(Literal leftSide, ASTNode rightSide, ScriptProcessor processor) {
					// don't care
				}
				@Override
				public ITypeInfo createTypeInfo(Literal node, ScriptProcessor processor) {
					return null; // nope
				}
			},

			new ProblemReporter<Nil>(Nil.class) {
				@Override
				public IType unresolvedType(Nil node, ScriptProcessor processor) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void reportProblems(Nil node, ScriptProcessor processor) throws ParsingException {
					if (!processor.script().engine().settings().supportsNil)
						markers().error(processor.parser, ParserErrorCode.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, processor.script().engine().name());
				}
			},

			new ProblemReporter<StringLiteral>(StringLiteral.class) {
				@Override
				public IType unresolvedType(StringLiteral node, ScriptProcessor processor) { return PrimitiveType.STRING; }
				@Override
				public void reportProblems(StringLiteral node, ScriptProcessor processor) throws ParsingException {
					// warn about overly long strings
					long max = processor.script().index().engine().settings().maxStringLen;
					String lit = node.literal();
					if (max != 0 && lit.length() > max)
						markers().warning(processor.parser, ParserErrorCode.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (processor.parser.hasAppendTo() || processor.script().resource() == null)
						return;
					String value = lit;
					int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
							if (region != null) {
								StringTbl.reportMissingStringTblEntries(processor, region);
								i += region.region().getLength();
								continue;
							}
						}
						++i;
					}
				}
			},

			new ProblemReporter<IntegerLiteral>(IntegerLiteral.class) {
				@Override
				public IType unresolvedType(IntegerLiteral node, ScriptProcessor processor) {
					if (node.longValue() == 0 && processor.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new ProblemReporter<FloatLiteral>(FloatLiteral.class) {
				@Override
				public void reportProblems(FloatLiteral node, ScriptProcessor processor) throws ParsingException {
					if (!processor.script().engine().settings().supportsFloats)
						markers().error(processor.parser, ParserErrorCode.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.reportProblems(node, processor);
				}
			},

			new ProblemReporter<IDLiteral>(IDLiteral.class) {
				@Override
				public IType unresolvedType(IDLiteral node, ScriptProcessor processor) {
					Definition obj = processor.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.objectType() : PrimitiveType.ID;
				}
			},

			new ProblemReporter<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType unresolvedType(BoolLiteral node, ScriptProcessor processor) {
					return PrimitiveType.BOOL;
				}
			},

			new ProblemReporter<CallExpr>(CallExpr.class) {
				@Override
				public IType unresolvedType(CallExpr node, ScriptProcessor processor) {
					ASTNode pred = node.predecessorInSequence();
					IType type = reporter(pred).unresolvedType(pred, processor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void reportProblems(CallExpr node, ScriptProcessor processor) throws ParsingException {
					if (!processor.script().engine().settings().supportsFunctionRefs)
						markers().error(processor.parser, ParserErrorCode.FunctionRefNotAllowed, node, node, Markers.NO_THROW, processor.script().engine().name());
					else {
						IType type = reporter(node.predecessorInSequence()).unresolvedType(node.predecessorInSequence(), processor);
						if (!PrimitiveType.FUNCTION.canBeAssignedFrom(type))
							markers().error(processor.parser, ParserErrorCode.CallingExpression, node, node, Markers.NO_THROW);
					}
				}
			},

			new ProblemReporter<Statement>(Statement.class) {
				@Override
				public IType unresolvedType(Statement node, ScriptProcessor processor) {
					return PrimitiveType.UNKNOWN;
				}
				protected void notFinishedError(ASTNode node, ScriptProcessor processor) throws ParsingException {
					if (!node.isFinishedProperly())
						// don't traverse children - one not-finished error is enough
						markers().error(processor.parser, ParserErrorCode.NotFinished, node, node, Markers.NO_THROW, node);
				}
				/**
				 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed.
				 * @param parser The parser used to create the warning marker if conditions are met (!{@link #hasSideEffects()})
				 */
				public void warnIfNoSideEffects(Statement node, ScriptProcessor processor) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						markers().warning(processor.parser, ParserErrorCode.NoSideEffects, node, node, 0);
				}
				@Override
				public void reportProblems(Statement node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					warnIfNoSideEffects(node, processor);
					if (!node.flagsEnabled(ASTNode.STATEMENT_REACHED))
						markers().warning(processor.parser, ParserErrorCode.NeverReached, node, node, 0);
					notFinishedError(node, processor);
				}
			},

			new ProblemReporter<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void reportProblems(VarDeclarationStatement node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					for (VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								IType initializationType = reporter(initialization.expression).unresolvedType(initialization.expression, processor);
								if (
									initialization.variable.staticallyTyped() &&
									!initialization.variable.type().canBeAssignedFrom(TypeUtil.resolve(initializationType, processor, processor.script()))
									)
									processor.incompatibleTypes(
										node,
										initialization.expression,
										initialization.variable.type(), initializationType
									);
								else {
									switch (node.scope()) {
									case VAR: case PARAMETER:
										initializationType = TypeUtil.resolve(initializationType, processor, processor.script());
										break;
									default:
										break;
									}
									AccessVar av = new AccessVar(initialization.variable);
									judgement(av, initializationType, TypingJudgementMode.Unify, processor);
								}
							}
				}
			},

			new ProblemReporter<PropListExpression>(PropListExpression.class) {
				@Override
				public IType unresolvedType(PropListExpression node, ScriptProcessor processor) {
					return node.definedDeclaration();
				}
				@Override
				public void reportProblems(PropListExpression node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					if (!processor.script().engine().settings().supportsProplists)
						markers().error(processor.parser, ParserErrorCode.NotSupported, node, node, Markers.NO_THROW,
							net.arctics.clonk.parser.c4script.ast.Messages.PropListExpression_ProplistsFeature,
							processor.script().engine().name());
				}
			},

			new ProblemReporter<Parenthesized>(Parenthesized.class) {
				@Override
				public IType unresolvedType(Parenthesized node, ScriptProcessor processor) {
					return ty(node.innerExpression(), processor);
				}
			},

			new ProblemReporter<MemberOperator>(MemberOperator.class) {
				@Override
				public IType unresolvedType(MemberOperator node, ScriptProcessor processor) {
					if (node.id() != null)
						return processor.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					ASTNode pred = node.predecessorInSequence();
					return pred != null ? reporter(pred).unresolvedType(pred, processor) : supr.unresolvedType(node, processor);
				}
				@Override
				public boolean typingJudgement(MemberOperator node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
					ASTNode p = node.predecessorInSequence();
					return p != null ? reporter(p).typingJudgement(p, type, processor, mode) : false;
				}
				@Override
				public void reportProblems(MemberOperator node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					ASTNode pred = node.predecessorInSequence();
					EngineSettings settings = processor.script().engine().settings();
					if (pred != null) {
						IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : TypeChoice.make(PrimitiveType.OBJECT, PrimitiveType.ID);
						ASTNode sequenceTilMe = pred.sequenceTilMe();
						ProblemReporter<? super ASTNode> stmReporter = reporter(sequenceTilMe);
						if (!stmReporter.typingJudgement(sequenceTilMe, requiredType, processor, TypingJudgementMode.Hint))
							markers().warning(processor.parser, node.dotNotation() ? ParserErrorCode.NotAProplist : ParserErrorCode.CallingMethodOnNonObject, node, node, 0,
								ty(sequenceTilMe, stmReporter, processor).typeName(false));
					}
					if (node.getLength() > 3 && !settings.spaceAllowedBetweenArrowAndTilde)
						markers().error(processor.parser, ParserErrorCode.MemberOperatorWithTildeNoSpace, node, node, Markers.NO_THROW);
					if (node.dotNotation() && !settings.supportsProplists)
						markers().error(processor.parser, ParserErrorCode.DotNotationNotSupported, node, node, Markers.NO_THROW, node);
				}
			},

			new ProblemReporter<IterateArrayStatement>(IterateArrayStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void reportProblems(IterateArrayStatement node, ScriptProcessor processor) throws ParsingException {
					Variable loopVariable;
					AccessVar accessVar;
					ASTNode elementExpr = node.elementExpr();
					ASTNode arrayExpr = node.arrayExpr();
					if (elementExpr instanceof VarDeclarationStatement)
						loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
					else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
						if (processor.obtainDeclaration(accessVar) == null) {
							// implicitly create loop variable declaration if not found
							SourceLocation varPos = processor.absoluteSourceLocationFromExpr(accessVar);
							loopVariable = processor.parser.createVarInScope(node.parentOfType(Function.class), accessVar.declarationName(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(accessVar.declaration(), Variable.class);
					} else
						loopVariable = null;

					processor.reportProblemsOf(elementExpr, true);
					processor.reportProblemsOf(arrayExpr, true);

					IType type = ty(arrayExpr, processor);
					if (!type.canBeAssignedFrom(PrimitiveType.ARRAY))
						processor.incompatibleTypes(node, arrayExpr, type, PrimitiveType.ARRAY);
					IType elmType = TypeUtil.resolve(ArrayType.elementTypeSet(type), processor, reporter(arrayExpr).callerType(arrayExpr, processor));
					processor.newTypeEnvironment();
					{
						if (loopVariable != null) {
							if (elmType != null)
								judgement(new AccessVar(loopVariable), elmType, TypingJudgementMode.Unify, processor);
							loopVariable.setUsed(true);
						}
						processor.reportProblemsOf(node.body(), true);
					}
					processor.endTypeEnvironment(true, false);
				}
			},

			new ProblemReporter<Ellipsis>(Ellipsis.class) {
				@Override
				public void reportProblems(Ellipsis node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					processor.parser.unnamedParamaterUsed(node);
				};
			},

			new ProblemReporter<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void reportProblems(SimpleStatement node, ScriptProcessor processor) throws ParsingException {
					BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						markers().warning(processor.parser, ParserErrorCode.NoAssignment, node, op, 0);
					supr.reportProblems(node, processor);
				}
			},

			new ProblemReporter<ConditionalStatement>(ConditionalStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() {return true;}
				@Override
				public void reportProblems(ConditionalStatement node, ScriptProcessor processor) throws ParsingException {
					processor.reportProblemsOf(node.condition(), true);
					processor.newTypeEnvironment();
					processor.reportProblemsOf(node.body(), true);
					processor.endTypeEnvironment(true, false);
					loopConditionWarnings(node, processor);
				}
				/**
				 * Emit warnings about loop conditions that could result in loops never executing or never ending.
				 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
				 * @param condition The loop condition to check
				 */
				protected void loopConditionWarnings(ConditionalStatement node, ScriptProcessor processor) {
					ASTNode condition = node.condition();
					if (node.body() == null || condition == null || !(node instanceof ILoop))
						return;
					Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateAtParseTime(node.parentOfType(Function.class)));
					if (Boolean.FALSE.equals(condEv))
						markers().warning(processor, ParserErrorCode.ConditionAlwaysFalse, condition, condition, Markers.NO_THROW, condition);
					else if (Boolean.TRUE.equals(condEv)) {
						EnumSet<ControlFlow> flows = node.body().possibleControlFlows();
						if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
							markers().warning(processor, ParserErrorCode.InfiniteLoop, node, node, Markers.NO_THROW);
					}
				}
			},

			new ProblemReporter<IfStatement>(IfStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void reportProblems(IfStatement node, ScriptProcessor processor) throws ParsingException {
					ASTNode condition = node.condition();
					processor.reportProblemsOf(condition, true);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					TypeEnvironment ifEnvironment = processor.newTypeEnvironment();
					processor.reportProblemsOf(node.body(), true);
					processor.endTypeEnvironment(false, false);
					if (node.elseExpression() != null) {
						TypeEnvironment elseEnvironment = processor.newTypeEnvironment();
						processor.reportProblemsOf(node.elseExpression(), true);
						processor.endTypeEnvironment(false, false);
						ifEnvironment.inject(elseEnvironment, false);
					}
					if (ifEnvironment.up != null)
						ifEnvironment.up.inject(ifEnvironment, false);

					if (!condition.containsConst()) {
						Object condEv = PrimitiveType.BOOL.convert(condition.evaluateAtParseTime(node.parentOfType(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							markers().warning(processor.parser,
								condEv.equals(true) ? ParserErrorCode.ConditionAlwaysTrue : ParserErrorCode.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ProblemReporter<ForStatement>(ForStatement.class) {
				@Override
				public void reportProblems(ForStatement node, ScriptProcessor processor) throws ParsingException {
					supr.reportProblems(node, processor);
					if (node.initializer() != null)
						processor.reportProblemsOf(node.initializer(), true);
					if (node.increment() != null)
						processor.reportProblemsOf(node.increment(), true);
				}
			},

			new ProblemReporter<NewProplist>(NewProplist.class) {
				@Override
				public void reportProblems(NewProplist node, ScriptProcessor processor) throws ParsingException {
					node.definedDeclaration().setPrototype(as(ty(node.prototype(), processor), ProplistDeclaration.class));
				}
			},

			new ProblemReporter<Placeholder>(Placeholder.class) {
				@Override
				public void reportProblems(Placeholder node, ScriptProcessor processor) throws ParsingException {
					StringTbl.reportMissingStringTblEntries(processor, new EntityRegion(null, node, node.entryName()));
				}
			},

			new ProblemReporter<MissingStatement>(MissingStatement.class) {
				@Override
				public void reportProblems(MissingStatement node, ScriptProcessor processor) throws ParsingException {
					markers().error(processor.parser, ParserErrorCode.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new ProblemReporter<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void reportProblems(GarbageStatement node, ScriptProcessor processor) throws ParsingException {
					markers().error(processor.parser, ParserErrorCode.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new ProblemReporter<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void reportProblems(FunctionDescription node, ScriptProcessor processor) throws ParsingException {
					if (processor.parser.hasAppendTo())
						return;
					int off = 1;
					for (String part : node.contents().split("\\|")) { //$NON-NLS-1$
						if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
							StringTbl stringTbl = processor.script().localStringTblMatchingLanguagePref();
							String entryName = part.substring(1, part.length()-1);
							if (stringTbl == null || stringTbl.map().get(entryName) == null)
								markers().warning(processor.parser, ParserErrorCode.UndeclaredIdentifier, node,
									new Region(node.start()+off, part.length()), 0, entryName);
						}
						off += part.length()+1;
					}
				}
			},

			new ProblemReporter<Comment>(Comment.class) {
				@Override
				public void reportProblems(Comment node, ScriptProcessor processor) throws ParsingException {
					String s = node.text();
					int markerPriority;
					int searchStart = 0;
					do {
						markerPriority = IMarker.PRIORITY_LOW;
						int todoIndex = s.indexOf("TODO", searchStart);
						if (todoIndex != -1)
							markerPriority = IMarker.PRIORITY_NORMAL;
						else {
							todoIndex = s.indexOf("FIXME", searchStart);
							if (todoIndex != -1)
								markerPriority = IMarker.PRIORITY_HIGH;
						}
						if (todoIndex != -1) {
							int lineEnd = s.indexOf('\n', todoIndex);
							if (lineEnd == -1)
								lineEnd = s.length();
							searchStart = lineEnd;
							markers().todo(processor.parser.file(), node, s.substring(todoIndex, lineEnd), node.start()+2+todoIndex, node.start()+2+lineEnd, markerPriority);
						}
					} while (markerPriority > IMarker.PRIORITY_LOW);
				}
			}

		};
		for (ProblemReporter<?> reporter : reporters)
			problemReporters.put(reporter.cls(), reporter);
		for (ProblemReporter<?> reporter : reporters)
			reporter.findSuper();
	}

}
