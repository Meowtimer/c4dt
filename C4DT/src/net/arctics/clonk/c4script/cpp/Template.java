package net.arctics.clonk.c4script.cpp;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static net.arctics.clonk.c4script.cpp.DispatchCase.caze;
import static net.arctics.clonk.c4script.cpp.DispatchCase.dispatch;
import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.AppendableBackedExprWriter;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SynthesizedFunction;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.ForStatement;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.Literal;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.Nil;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.util.StringUtil;

public class Template {

	static String cppTypeString(PrimitiveType type) {
		switch ((type)) {
		case ARRAY:
			return "C4ValueArray*";
		case BOOL:
			return "bool";
		case FUNCTION:
			return "C4AulFunc*";
		case ID:
			return "C4ID";
		case INT:
			return "int";
		case OBJECT:
			return "C4Object*";
		case PROPLIST:
			return "C4PropList*";
		case STRING:
			return "C4String*";
		default:
			return "C4Value";
		}
	}

	static String primitiveDispatch(IType type, java.util.function.Function<PrimitiveType, String> pfn) {
		return
			type instanceof PrimitiveType ? pfn.apply((PrimitiveType)type) :
			type.simpleType() instanceof PrimitiveType ? pfn.apply((PrimitiveType)type.simpleType()) :
			format("any /* %s: %s */", type.typeName(true), type.getClass().getSimpleName());
	}

	static String cppTypeString(IType type) {
		return primitiveDispatch(type, Template::cppTypeString);
	}

	static String valueConversionSuffix(PrimitiveType targetType) {
		switch (targetType) {
		case ARRAY:
			return ".getArray()";
		case BOOL:
			return ".getBool()";
		case FUNCTION:
			return ".getFunction()";
		case ID:
			return ".getDef()";
		case INT:
			return ".getInt()";
		case OBJECT:
			return ".getObj()";
		case PROPLIST:
			return ".getPropList()";
		case STRING:
			return ".getStr()";
		default:
			return "";
		}
	}

	static String valueConversionSuffix(IType targetType) {
		return primitiveDispatch(targetType, Template::valueConversionSuffix);
	}

	@SafeVarargs
	static <T> Stream<T> concatMultiple(Stream<T>... streams) {
		return stream(streams).reduce(Stream::concat).orElseGet(Stream::empty);
	}

	static class LeftRightType {
		public final IType left, right;
		public LeftRightType(IType left, IType right) {
			super();
			this.left = left;
			this.right = right;
		}
	}

	private final List<Object> skeleton;
	private final Stream<String>
		globals, assignGlobals, locals, assignLocals, funcs,
		assignFuncs, natives, nativeWrappers, assignNatives,
		stringTable, assignStringTable, instanceFields, instanceFuncs,
		getPropertyByS, addNativeFields, setPropertyByS, resetProperty,
		convertedFunctions, idTable, assignIDTable;

	String parmsString(Function f, boolean comma) {
		return f.parameters().isEmpty() ? "" : (
			(comma ? ", " : "") + f.parameters().stream().map(
				p -> format("%s %s", cppTypeString(p.type()), p.name())
			).collect(Collectors.joining(", "))
		);
	}
	public String printNode(Map<String, Integer> strTable, final Function function, final ASTNode node) {
		final StringWriter output = new StringWriter();
		final java.util.function.Function<String, Integer> strNum = s -> defaulting(strTable.get(s), -1);
		node.print(new AppendableBackedExprWriter(output) {
			private void printConversionSuffix(IType targetType, ASTNode node) {
				final IType ty = node.ty();
				final String suffix = !(node instanceof Literal || node instanceof AccessVar)
					? valueConversionSuffix(ty) : null;
				if (suffix != null)
					output.append(suffix);
			}
			private LeftRightType leftRightTypes(BinaryOp bop) {
				return
					bop.operator() == Operator.Assign ? new LeftRightType(bop.leftSide().ty(), bop.leftSide().ty()) :
					new LeftRightType(bop.operator().firstArgType(), bop.operator().secondArgType());
			}
			@Override
			public boolean doCustomPrinting(final ASTNode node, final int depth) {
				return defaulting(dispatch(node,
					caze(StringLiteral.class, lit -> {
						append(String.format("D.S._%d", strNum.apply(lit.stringValue())));
						return true;
					}),
					caze(VarDeclarationStatement.class, statement -> {
						if (statement.parent() instanceof ForStatement)
							return false;
						stream(statement.variableInitializations()).map(vi -> vi.variable).forEach(var -> {
							append(cppTypeString(var.type()));
							append(" ");
							append(var.name());
							final ASTNode init = var.initializationExpression();
							if (init != null) {
								append(" = "); //$NON-NLS-1$
								init.print(this, depth+1);
								printConversionSuffix(var.type(), init);
							}
							append(";");
						});
						return true;
					}),
					caze(CallDeclaration.class, call -> {
						if (call.function() != null && call.predecessor() == null)
							append(format("Exec(D.G.%s", call.name()));
						else
							append(format("Exec(D.S._%d", strNum.apply(call.name())));
						if (call.params().length > 0)
							stream(call.params()).forEach(par -> {
								append(", ");
								if (par != null)
									par.print(this, depth);
							});
						append(")");
						return true;
					}),
					caze(Nil.class, nil -> {
						append("nullptr");
						return true;
					}),
					caze(AccessVar.class, av -> {
						if (av.proxiedDefinition() != null) {
							append(format("D.ID.%s", av.name()));
							return true;
						} else if (av.predecessor() != null) {
							append(format("GetPropertyByS(D.S._%d)", strNum.apply(av.name())));
							return true;
						}
						else
							return false;
					}),
					caze(MemberOperator.class, mo -> {
						append("->");
						return true;
					}),
					caze(BinaryOp.class, bop -> {
						final ASTNode leftSide = bop.leftSide();
						final ASTNode rightSide = bop.rightSide();
						final Operator operator = bop.operator();
						boolean needsBrackets = leftSide instanceof BinaryOp && operator.priority() > ((BinaryOp)leftSide).operator().priority();
						if (needsBrackets)
							output.append("("); //$NON-NLS-1$
						leftSide.print(this, depth);
						final LeftRightType lr = leftRightTypes(bop);
						printConversionSuffix(lr.left, leftSide);

						if (needsBrackets)
							output.append(")"); //$NON-NLS-1$

						output.append(" "); //$NON-NLS-1$
						output.append(operator.operatorName());

						needsBrackets = rightSide instanceof BinaryOp && operator.priority() > ((BinaryOp)rightSide).operator().priority();
						if (needsBrackets)
							output.append(" ("); //$NON-NLS-1$
						else {
							final String printed = rightSide.printed(depth);
							if (!printed.startsWith("\n"))
								output.append(" ");
							rightSide.print(this, depth);
							printConversionSuffix(lr.right, rightSide);
						}
						if (needsBrackets)
							output.append(")"); //$NON-NLS-1$
						return true;
					}),
					caze(FunctionBody.class, body -> {
						Block.printBlock(concat(body.statements(),
							new AccessVar(format("return C4Value()%s;", valueConversionSuffix(body.owner().returnType())))),
							this, depth);
						return true;
					})
				), Boolean.FALSE);
			}
		}, 0);
		return output.toString();
	}
	public Template(Index index, Script script) {

		final List<Function> functions = new DeclarationsStreamer(index, script)
			.declarations(script, s -> s.functions().stream())
			.filter(f -> f.code() != null && !(f instanceof SynthesizedFunction))
			.collect(Collectors.toList());
		final List<Variable> fields = new DeclarationsStreamer(index, script)
			.declarations(script, s -> s.variables().stream())
			.collect(Collectors.toList());

		final Set<Function> referencedGlobalFunctions =
			ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), CallDeclaration.class)
				.filter(call -> call.predecessor() == null)
				.map(call -> call.function())
				.filter(f -> f != null)
				.collect(Collectors.toSet());

		final AtomicInteger counter = new AtomicInteger();
		final Map<String, Integer> strTable =
			concatMultiple(
				ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), StringLiteral.class).map(l -> l.stringValue()),
				ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), CallDeclaration.class)
					.filter(c -> c.predecessor() != null)
					.map(c -> c.name()),
				ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), AccessVar.class)
					.filter(av -> av.proxiedDefinition() == null)
					.map(av -> av.name())
			)
				.distinct()
				.collect(Collectors.toMap(s -> s, s -> counter.incrementAndGet()));
		final Set<Definition> idRefs =
			ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), AccessVar.class)
				.map(av -> av.proxiedDefinition())
				.filter(def -> def != null)
				.collect(Collectors.toSet());

		this.globals = referencedGlobalFunctions.stream().map(f -> format("C4AulFunc* %s;", f.name()));
		this.assignGlobals = referencedGlobalFunctions.stream().map(f -> format("%1$s = def.GetFunc(\"%1$s\");", f.name()));
		this.locals = fields.stream().map(l -> format("C4String* %s;", l.name()));
		this.assignLocals = fields.stream().map(l -> format("%1$s = Strings.RegString(\"%1$s\");", l.name()));
		this.funcs = functions.stream().map(f -> format("C4String* %s;", f.name()));
		this.assignFuncs = functions.stream().map(f -> format("%1$s = Strings.RegString(\"%1$s\");", f.name()));
		this.natives = functions.stream().map(f -> format("C4AulFunc* %s;", f.name()));
		this.nativeWrappers = functions.stream().map(f -> format(
			"static %s %s(%s* target%s) { return target->s_%2$s(%s); }",
			cppTypeString(f.returnType()),
			f.name(),
			script.name(),
			parmsString(f, true),
			f.parameters().stream().map(p -> p.name()).collect(Collectors.joining(", "))
		));
		this.assignNatives = functions.stream().map(f -> format("%s = AddFunc(&def.Script, \"%1$s\", W::%1$s);", f.name()));
		this.stringTable = strTable.entrySet().stream().map(e -> format("C4String* _%d;", e.getValue()));
		this.assignStringTable = strTable.entrySet().stream()
			.sorted((a, b) -> a.getValue().compareTo(b.getValue()))
			.map(e -> format("_%d = Strings.RegString(\"%s\");", e.getValue(), e.getKey()));
		this.instanceFields = fields.stream().map(f -> format("%s %s;", cppTypeString(f.type()), f.name()));
		this.instanceFuncs = functions.stream().map(f -> format(
			"%s s_%s(%s);",
			cppTypeString(f.returnType()),
			f.name(),
			parmsString(f, false)
		));
		this.getPropertyByS = fields.stream().map(f -> format("if (k == D.L.%s) { *pResult = C4Value(%1$s); return true; }", f.name()));
		this.addNativeFields = fields.stream().map(f -> format("(*properties)[i++] = C4Value(%s);", f.name()));
		this.setPropertyByS = fields.stream().map(f -> format("if (k == D.L.%s) { %1$s = to%s; return; }", f.name(), valueConversionSuffix(f.type())));
		this.resetProperty = fields.stream().map(f -> format("if (k == D.L.%s) { %1$s = {}; return; }", f.name()));
		this.idTable = idRefs.stream().map(id -> format("C4Def* %s;", id.name()));
		this.assignIDTable = idRefs.stream().map(id -> format("%s = Definitions.GetByName(StdStrBuf(\"%1$s\"));", id.name()));

		this.convertedFunctions = functions.stream().map(f ->
			format("%s %s::s_%s(%s)\n%s\n",
				cppTypeString(f.returnType()),
				script.name(),
				f.name(),
				parmsString(f, false),
				printNode(strTable, f, f.code())
			)
		);

		this.skeleton = Arrays.<Object>asList(
			"class "+script.name()+" : public C4Object",
			"{",
			"public:",
			"	// meta",
			"	class Def : public C4NativeDef",
			"	{",
			"	public:",
			"		using C4NativeDef::C4NativeDef;",
			"		struct",
			"		{",
			globals,
			"			void Assign(Def& def)",
			"			{",
			assignGlobals,
			"			}",
			"		} G;",
			"		struct",
			"		{",
			locals,
			"			void Assign()",
			"			{",
			assignLocals,
			"			}",
			"		} L;",
			"		struct",
			"		{",
			funcs,
			"			void Assign()",
			"			{",
			assignFuncs,
			"			}",
			"		} F;",
			"		struct",
			"		{",
			natives,
			"			struct W",
			"			{",
			nativeWrappers,
			"			};",
			"			void Assign(Def& def)",
			"			{",
			assignNatives,
			"			}",
			"		} N;",
			"		struct",
			"		{",
			stringTable,
			"			void Assign()",
			"			{",
			assignStringTable,
			"			}",
			"		} S;",
			"		struct",
			"		{",
			idTable,
			"			void Assign()",
			"			{",
			assignIDTable,
			"			}",
			"		} ID;",
			"		virtual void AddNatives()",
			"		{",
			"			C4NativeDef::AddNatives();",
			"			G.Assign(*this);",
			"			L.Assign();",
			"			F.Assign();",
			"			N.Assign(*this);",
			"			S.Assign();",
			"		}",
			"	};",
			"	static Def D;",
			"	// fields",
			instanceFields,
			"	// functions",
			instanceFuncs,
			"",
			"	// property management",
			"	virtual bool GetPropertyByS(C4String *k, C4Value *pResult) const",
			"	{",
			getPropertyByS,
			"		return C4Object::GetPropertyByS(k, pResult);",
			"	}",
			"",
			"	virtual C4ValueArray * GetProperties() const",
			"	{",
			"		C4ValueArray* properties = C4Object::GetProperties();",
			"		int i = properties->GetSize();",
			"		properties->SetSize(i+"+fields.size()+");",
			addNativeFields,
			"		return properties;",
			"	}",
			"",
			"	virtual void SetPropertyByS(C4String * k, const C4Value & to)",
			"	{",
			setPropertyByS,
			"		C4Object::SetPropertyByS(k, to);",
			"	}",
			"",
			"	virtual void ResetProperty(C4String * k)",
			"	{",
			resetProperty,
			"		C4Object::ResetProperty(k);",
			"	}",
			"};",
			"",
			convertedFunctions,
			format("decltype(%s::D) %1$s::D(\"%s\");", script.name(), script.resource().getProjectRelativePath().toPortableString())
		);
	}
	class Indented {
		public int indentation;
		public Object item;
		public Indented(int indentation, Object item) {
			super();
			this.indentation = indentation;
			this.item = item;
		}
	}
	@SuppressWarnings("unchecked")
	public Stream<String> flatten() {
		final AtomicInteger indent = new AtomicInteger();
		final List<Indented> indented = skeleton.stream().map(item -> {
			final String str = as(item, String.class);
			final int currentIndentation = indent.get();
			if (str != null)
				if (str.contains("}"))
					indent.decrementAndGet();
				else if (str.contains("{"))
					indent.incrementAndGet();
			return new Indented(currentIndentation, item);
		}).collect(Collectors.toList());
		return indented.stream().flatMap(s ->
			s.item instanceof String ? stream(new String[] {(String)s.item}) :
			s.item instanceof Stream ? ((Stream<String>)s.item).map(str -> StringUtil.multiply("\t", s.indentation) + str) :
			Stream.empty()
		);
	}

	public static void printScript(Index index, Script script, PrintWriter output) {
		new Template(index, script).flatten().forEach(s -> {
			output.append(s);
			output.append("\n");
		});
	}
}