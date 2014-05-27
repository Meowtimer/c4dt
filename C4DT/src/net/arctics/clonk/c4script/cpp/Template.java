package net.arctics.clonk.c4script.cpp;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.DispatchCase.caze;
import static net.arctics.clonk.util.DispatchCase.dispatch;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.AppendableBackedExprWriter;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SynthesizedFunction;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Ellipsis;
import net.arctics.clonk.c4script.ast.ForStatement;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.Literal;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.Nil;
import net.arctics.clonk.c4script.ast.ReturnStatement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.Typing;
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

	static String unclashKeyword(String name) {
		switch (name) {
		case "alignas":
		case "alignof":
		case "and":
		case "and_eq":
		case "asm":
		case "auto":
		case "bitand":
		case "bitor":
		case "bool":
		case "break":
		case "case":
		case "catch":
		case "char":
		case "char16_t":
		case "char32_t":
		case "class":
		case "compl":
		case "const":
		case "constexpr":
		case "const_cast":
		case "continue":
		case "decltype":
		case "default":
		case "delete":
		case "do":
		case "double":
		case "dynamic_cast":
		case "else":
		case "enum":
		case "explicit":
		case "export":
		case "extern":
		case "false":
		case "float":
		case "for":
		case "friend":
		case "goto":
		case "if":
		case "inline":
		case "int":
		case "long":
		case "mutable":
		case "namespace":
		case "new":
		case "noexcept":
		case "not":
		case "not_eq":
		case "nullptr":
		case "operator":
		case "or":
		case "or_eq":
		case "private":
		case "protected":
		case "public":
		case "register":
		case "reinterpret_cast":
		case "return":
		case "short":
		case "signed":
		case "sizeof":
		case "static":
		case "static_assert":
		case "static_cast":
		case "struct":
		case "switch":
		case "template":
		case "this":
		case "thread_local":
		case "throw":
		case "true":
		case "try":
		case "typedef":
		case "typeid":
		case "typename":
		case "union":
		case "unsigned":
		case "using":
		case "virtual":
		case "void":
		case "volatile":
		case "wchar_t":
		case "while":
		case "xor":
		case "xor_eq":
			return name + "_";
		default:
			return name;
		}
	}

	static class LeftRightType {
		public final IType left, right;
		public LeftRightType(IType left, IType right) {
			super();
			this.left = left;
			this.right = right;
		}
	}

	static class NumberedFunction {
		public final Function function;
		public final int number;
		public NumberedFunction(Function function, int number) {
			super();
			this.function = function;
			this.number = number;
		}
		public String name() {
			return number > 0 ? format("%s%d", function.name(), number) : function.name();
		}
	}

	static final class TypedIdentifier {
		public final String name;
		public final IType type;
		public String name() { return name; }
		public String unclashedName() { return unclashKeyword(name); }
		public IType type() { return type; }
		public TypedIdentifier(String name, IType type) {
			super();
			this.name = name;
			this.type = type;
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
				p -> format("%s %s", cppTypeString(p.type()), unclashKeyword(p.name()))
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
							append(";\n");
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
					}),
					caze(ReturnStatement.class, ret -> {
						if (ret.returnExpr() == null) {
							output.append("return {};");
							return true;
						} else
							return false;
					}),
					caze(ArrayElementExpression.class, ee -> {
						if (ee.predecessor() != null && ee.argument() != null) {
							if (ee.argument().ty().simpleType() == PrimitiveType.STRING) {
								printConversionSuffix(PrimitiveType.PROPLIST, ee.predecessor());
								output.append("->GetPropertyByS(");
								ee.argument().print(this, depth);
								printConversionSuffix(PrimitiveType.STRING, ee.argument());
								output.append(")");
							} else {
								printConversionSuffix(PrimitiveType.ARRAY, ee.predecessor());
								output.append("->GetItem(");
								ee.argument().print(this, depth);
								printConversionSuffix(PrimitiveType.INT, ee.argument());
								output.append(")");
							}
							return true;
						} else
							return false;
					}),
					caze(Ellipsis.class, ell -> {
						final Function f = ell.parent(Function.class);
						output.append(f.parameters().stream().map(Variable::name).map(Template::unclashKeyword).collect(Collectors.joining(", ")));
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
		final List<String> uniqueFuncNames = functions.stream().map(f -> f.name()).distinct().collect(Collectors.toList());
		final Map<String, List<Function>> functionsByName = functions.stream().collect(Collectors.groupingBy(f -> f.name()));
		final List<NumberedFunction> numberedFunctions = functionsByName.entrySet().stream()
			.flatMap(e -> IntStream.range(0, e.getValue().size())
				.<NumberedFunction>mapToObj(x -> new NumberedFunction(e.getValue().get(x), x)))
			.collect(Collectors.toList());

		final List<TypedIdentifier> fields = new DeclarationsStreamer(index, script)
			.declarations(script, s -> s.variables().stream())
			.collect(Collectors.groupingBy(f -> f.name()))
			.entrySet().stream().map(e -> new TypedIdentifier(
				e.getKey(),
				e.getValue().stream().map(Variable::type).reduce(Typing.INFERRED::unify).orElse(PrimitiveType.ANY)
			))
			.collect(Collectors.toList());

		final Set<String> referencedGlobalFunctions =
			ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), CallDeclaration.class)
				.filter(call -> call.predecessor() == null)
				.map(call -> call.function())
				.filter(f -> f != null)
				.map(f -> f.name())
				.distinct()
				.collect(Collectors.toSet());

		final AtomicInteger counter = new AtomicInteger();
		final Map<String, Integer> strTable =
			concatMultiple(
				// direct string literals
				ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), StringLiteral.class).map(l -> l.stringValue()),
				// names of functions from target calls or not resolved
				ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), CallDeclaration.class)
					.filter(c -> c.predecessor() != null || c.function() == null)
					.map(c -> c.name()),
				// target variables accesses
				ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), AccessVar.class)
					.filter(av -> av.proxiedDefinition() == null && av.predecessor() != null)
					.map(av -> av.name())
			)
				.distinct()
				.collect(Collectors.toMap(s -> s, s -> counter.incrementAndGet()));

		final Set<Definition> idRefs =
			ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), AccessVar.class)
				.map(av -> av.proxiedDefinition())
				.filter(def -> def != null)
				.collect(Collectors.toSet());

		this.globals = referencedGlobalFunctions.stream().map(n -> format("C4AulFunc* %s;", n));
		this.assignGlobals = referencedGlobalFunctions.stream().map(n -> format("%1$s = def.GetFunc(\"%1$s\");", n));
		this.locals = fields.stream().map(l -> format("C4String* %s;", l.unclashedName()));
		this.assignLocals = fields.stream().map(l -> format("%s = Strings.RegString(\"%s\");", l.unclashedName(), l.name()));
		this.funcs = uniqueFuncNames.stream().map(fn -> format("C4String* %s;", fn));
		this.assignFuncs = uniqueFuncNames.stream().map(fn -> format("%1$s = Strings.RegString(\"%1$s\");", fn));
		this.natives = uniqueFuncNames.stream().map(fn -> format("C4AulFunc* %s;", fn));
		this.nativeWrappers = numberedFunctions.stream()
			.map(nf -> {
				final Function f = nf.function;
				return format(
					"static %s %s(%s* __this__%s) { return __this__->s_%2$s(%s); }",
					cppTypeString(f.returnType()),
					nf.name(),
					script.name(),
					parmsString(f, true),
					f.parameters().stream().map(Variable::name).map(Template::unclashKeyword).collect(Collectors.joining(", "))
				);
			});
		this.assignNatives = uniqueFuncNames.stream().map(n -> format("%s = AddFunc(&def.Script, \"%1$s\", W::%1$s);", n));
		this.stringTable = strTable.entrySet().stream().map(e -> format("C4String* _%d;", e.getValue()));
		this.assignStringTable = strTable.entrySet().stream()
			.sorted((a, b) -> a.getValue().compareTo(b.getValue()))
			.map(e -> format("_%d = Strings.RegString(\"%s\");", e.getValue(), e.getKey()));
		this.instanceFields = fields.stream().map(f -> format("%s %s;", cppTypeString(f.type()), unclashKeyword(f.name())));
		this.instanceFuncs = numberedFunctions.stream().map(f -> format(
			"%s s_%s(%s);",
			cppTypeString(f.function.returnType()),
			f.name(),
			parmsString(f.function, false)
		));
		this.getPropertyByS = fields.stream().map(f -> format("if (k == D.L.%s) { *pResult = C4Value(%1$s); return true; }", f.unclashedName()));
		this.addNativeFields = fields.stream().map(f -> format("(*properties)[i++] = C4Value(%s);", f.unclashedName()));
		this.setPropertyByS = fields.stream().map(f -> format("if (k == D.L.%s) { %1$s = to%s; return; }", f.unclashedName(), valueConversionSuffix(f.type())));
		this.resetProperty = fields.stream().map(f -> format("if (k == D.L.%s) { %1$s = {}; return; }", f.unclashedName()));
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
			"			L.Assign();",
			"			F.Assign();",
			"			N.Assign(*this);",
			"			S.Assign();",
			"			G.Assign(*this);",
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