package net.arctics.clonk.c4script.cpp;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static net.arctics.clonk.util.DispatchCase.caze;
import static net.arctics.clonk.util.DispatchCase.dispatch;
import static net.arctics.clonk.util.StreamUtil.concatStreams;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.StringUtil.blockString;
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
import net.arctics.clonk.ast.AppendableBackedNodePrinter;
import net.arctics.clonk.ast.Raw;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SynthesizedFunction;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.ArrayExpression;
import net.arctics.clonk.c4script.ast.BinaryOp;
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

public class CPPTemplate {

	private static final String NIL = "C4VNull";

	static String cppTypeString(PrimitiveType type) {
		switch ((type)) {
		case ARRAY:
			return "C4ValueArray";
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

	static PrimitiveType primitiveTypeFromType(IType type) {
		return
			type instanceof PrimitiveType ? (PrimitiveType)type :
			type.simpleType() instanceof PrimitiveType ? (PrimitiveType)type.simpleType() :
			null;
	}

	static String primitiveDispatch(IType type, java.util.function.Function<PrimitiveType, String> pfn) {
		final PrimitiveType primitiveType = primitiveTypeFromType(type);
		return primitiveType != null ? pfn.apply(primitiveType) :
			format("any /* %s: %s */", type.typeName(true), type.getClass().getSimpleName());
	}

	static String cppTypeString(IType type) {
		return primitiveDispatch(type, CPPTemplate::cppTypeString);
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

	static String valueConversionSuffix(IType sourceType, IType targetType) {
		final PrimitiveType primitiveSourceType = primitiveTypeFromType(sourceType);
		final PrimitiveType primitiveTargetType = primitiveTypeFromType(targetType);
		if (primitiveSourceType == primitiveTargetType) {
			return null;
		}
		if (primitiveTargetType != null) {
			return valueConversionSuffix(primitiveTargetType);
		}
		return null;
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

	String parmsString(Function f, boolean comma) {
		return f.parameters().isEmpty() ? "" : (
			(comma ? ", " : "") + f.parameters().stream().map(
				p -> format("%s %s", cppTypeString(p.type()), unclashKeyword(p.name()))
			).collect(Collectors.joining(", "))
		);
	}
	public String printNode(Map<String, Integer> strTable, final Function function, final ASTNode node) {
		final StringWriter output = new StringWriter();
		final java.util.function.Function<String, Integer> stringNumber =
			string -> defaulting(strTable.get(string), -1);
		node.print(new AppendableBackedNodePrinter(output) {
			IType effectiveType(ASTNode node) {
				System.out.println(node.getClass().getSimpleName());
				if (node instanceof AccessVar || node instanceof Literal || node instanceof ArrayExpression) {
					return node.ty();
				}
				final BinaryOp bop = as(node, BinaryOp.class);
				if (bop != null) {
					return bop.operator().returnType();
				}
				return PrimitiveType.ANY;
			}
			private void printNodeWithConversion(PrimitiveType targetType, ASTNode node, int depth) {
				final PrimitiveType sourceType = as(effectiveType(node).simpleType(), PrimitiveType.class);

				if (targetType == null || targetType == sourceType) {
					node.print(this, depth);
					return;
				}

				switch (targetType) {
				case ANY: case UNKNOWN: case VOID:
					switch (sourceType) {
					case ARRAY: case BOOL: case OBJECT: case PROPLIST: case NUM: case STRING:
						append("C4Value(");
						node.print(this, depth);
						append(")");
						break;
					default:
						node.print(this, depth);
						break;
					}
					break;
				default:
					final String suffix = valueConversionSuffix(sourceType, targetType);
					node.print(this, depth);
					if (suffix != null) {
						output.append(suffix);
					}
					break;
				}
			}
			private LeftRightType leftRightTypes(BinaryOp bop) {
				return
					bop.operator() == Operator.Assign ? new LeftRightType(bop.leftSide().ty(), bop.rightSide().ty()) :
					new LeftRightType(bop.operator().firstArgType(), bop.operator().secondArgType());
			}
			private void printString(String str) {
				append(String.format("D.S._%d", stringNumber.apply(str)));
			}
			private void printPiece(IType targetType, Sequence.Piece piece, int depth) {
				switch (piece.kind) {
				case ArrayElement:
					output.append("arrayElement(");
					printPiece(PrimitiveType.ARRAY, piece.previous, depth);
					output.append(", ");
					printNodeWithConversion(PrimitiveType.INT, piece.node, depth);
					output.append(")");
					break;
				case Property:
					output.append("property(");
					printPiece(PrimitiveType.PROPLIST, piece.previous, depth);
					output.append(", ");
					printString(((StringLiteral)piece.node).literal());
					output.append(")");
					break;
				default:
					break;
				}
			}
			PrimitiveType primitiveType(IType type) {
				return (PrimitiveType) type.simpleType();
			}
			@Override
			public boolean doCustomPrinting(final ASTNode node, final int depth) {
				return defaulting(dispatch(node,
					caze(StringLiteral.class, lit -> {
						printString(lit.stringValue());
						return true;
					}),
					caze(VarDeclarationStatement.class, statement -> {
						if (statement.parent() instanceof ForStatement) {
							return false;
						}
						stream(statement.variableInitializations()).map(vi -> vi.variable).forEach(var -> {
							append(cppTypeString(var.type()));
							append(" ");
							append(var.name());
							final ASTNode init = var.initializationExpression();
							if (init != null) {
								append(" = "); //$NON-NLS-1$
								printNodeWithConversion(primitiveType(var.type()), init, depth + 1);
							}
							append(";\n");
						});
						return true;
					}),
					caze(CallDeclaration.class, call -> {
						if (call.function() != null && call.predecessor() == null) {
							append(format("Exec(D.G.%s", call.name()));
						} else {
							append("Exec(");
							printString(call.name());
						}
						if (call.params().length > 0) {
							stream(call.params()).forEach(parameter -> {
								if (parameter instanceof Ellipsis) {
									final Function f = call.parent(Function.class);
									final String ellipsisExpansion = f.parameters().stream()
										.map(Variable::name)
										.map(CPPTemplate::unclashKeyword)
										.collect(Collectors.joining(", "));
									if (ellipsisExpansion.length() > 0) {
										append(", ");
										append(ellipsisExpansion);
									}
								} else {
									append(", ");
									if (parameter != null) {
										parameter.print(this, depth);
									} else {
										append(NIL);
									}
								}
							});
						}
						append(")");
						return true;
					}),
					caze(Nil.class, nil -> {
						append(NIL);
						return true;
					}),
					caze(AccessVar.class, av -> {
						if (av.proxiedDefinition() != null) {
							append(format("D.ID.%s", av.name()));
							return true;
						} else if (av.predecessor() != null) {
							append("GetPropertyByS(");
							printString(av.name());
							return true;
						} else {
							return false;
						}
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
						 {
							output.append("("); //$NON-NLS-1$
						}

						final LeftRightType lr = leftRightTypes(bop);
						printNodeWithConversion(primitiveType(lr.left), leftSide, depth);

						if (needsBrackets)
						 {
							output.append(")"); //$NON-NLS-1$
						}

						output.append(" "); //$NON-NLS-1$
						output.append(operator.operatorName());

						needsBrackets = rightSide instanceof BinaryOp && operator.priority() > ((BinaryOp)rightSide).operator().priority();
						if (needsBrackets)
						 {
							output.append(" ("); //$NON-NLS-1$
						}

						final String printed = rightSide.printed(depth);
						if (!printed.startsWith("\n")) {
							output.append(" ");
						}
						printNodeWithConversion(primitiveType(lr.right), rightSide, depth);

						if (needsBrackets)
						 {
							output.append(")"); //$NON-NLS-1$
						}
						return true;
					}),
					caze(FunctionBody.class, body -> {
						output.append("{\n");
 						stream(body.statements()).forEach(statement -> {
 							statement.print(this, depth + 1);
 							output.append("\n");
 						});
 						output.append("\treturn ");
 						printNodeWithConversion(primitiveType(body.owner().returnType()), new Raw("C4Value()", PrimitiveType.ANY), depth);
 						output.append(";\n");
 						output.append("}");
						return true;
					}),
					caze(ReturnStatement.class, ret -> {
						if (ret.returnExpr() == null) {
							output.append("return {};");
							return true;
						} else {
							return false;
						}
					}),
					caze(Sequence.class, sequence -> {
						printPiece(null, sequence.toPieces(), depth);
						return true;
					}),
					caze(ArrayExpression.class, arrayExpression -> {
						output.append("array(");
						boolean first = true;
						for (final ASTNode parameter : arrayExpression.subElements()) {
							if (first) {
								first = false;
								output.append(", ");
							}
							printNodeWithConversion(PrimitiveType.ANY, parameter, depth + 1);
						}
						output.append(")");
						return true;
					})
				), Boolean.FALSE);
			}
		}, 0);
		return output.toString();
	}
	public CPPTemplate(Index index, Script script) {

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

		final Set<String> referencedGlobalFunctionNames =
			ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), CallDeclaration.class)
				.filter(call -> call.predecessor() == null)
				.map(call -> call.function())
				.filter(f -> f != null)
				.map(f -> f.name())
				.distinct()
				.collect(Collectors.toSet());

		final AtomicInteger counter = new AtomicInteger();
		final Map<String, Integer> strTable =
			concatStreams(
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
				.collect(Collectors.toMap(identity(), s -> counter.incrementAndGet()));

		final Set<Definition> idRefs =
			ofType(functions.stream().flatMap(f -> f.recursiveNodesStream()), AccessVar.class)
				.map(av -> av.proxiedDefinition())
				.filter(def -> def != null)
				.collect(Collectors.toSet());

		final Stream<String> globals = referencedGlobalFunctionNames.stream().map(n -> format("C4AulFunc* %s;", n));
		final Stream<String> assignGlobals = referencedGlobalFunctionNames.stream().map(n -> format("%1$s = def.GetFunc(\"%1$s\");", n));
		final Stream<String> locals = fields.stream().map(l -> format("C4String* %s;", l.unclashedName()));
		final Stream<String> assignLocals = fields.stream().map(l -> format("%s = Strings.RegString(\"%s\");", l.unclashedName(), l.name()));
		final Stream<String> funcs = uniqueFuncNames.stream().map(fn -> format("C4String* %s;", fn));
		final Stream<String> assignFuncs = uniqueFuncNames.stream().map(fn -> format("%1$s = Strings.RegString(\"%1$s\");", fn));
		final Stream<String> natives = uniqueFuncNames.stream().map(fn -> format("C4AulFunc* %s;", fn));
		final Stream<String> nativeWrappers = numberedFunctions.stream()
			.map(numberedFunction -> (
				"static " +
				cppTypeString(numberedFunction.function.returnType()) + " " + numberedFunction.name() +
				"(C4PropList* self"+parmsString(numberedFunction.function, true)+")" +
				" { return static_cast<" + script.name() + "*>(self)->s_" + numberedFunction.name() + "(" +
					numberedFunction.function.parameters().stream()
						.map(Variable::name)
						.map(CPPTemplate::unclashKeyword)
						.collect(Collectors.joining(", ")) + "); }"
			));
		final Stream<String> assignNatives = uniqueFuncNames.stream().map(n -> format("%s = AddFunc(&def.Script, \"%1$s\", W::%1$s);", n));
		final Stream<String> stringTable = strTable.entrySet().stream().map(e -> format("C4String* _%d;", e.getValue()));
		final Stream<String> assignStringTable = strTable.entrySet().stream()
			.sorted((a, b) -> a.getValue().compareTo(b.getValue()))
			.map(e -> format("_%d = Strings.RegString(\"%s\");", e.getValue(), e.getKey()));
		final Stream<String> instanceFields = fields.stream().map(f -> format("%s %s;", cppTypeString(f.type()), unclashKeyword(f.name())));
		final Stream<String> instanceFuncs = numberedFunctions.stream().map(f -> format(
			"%s s_%s(%s);",
			cppTypeString(f.function.returnType()),
			f.name(),
			parmsString(f.function, false)
		));
		final Stream<String> getPropertyByS = fields.stream().map(f -> format("if (k == D.L.%s) { *pResult = C4Value(%1$s); return true; }", f.unclashedName()));
		final Stream<String> addNativeFields = fields.stream().map(f -> format("(*properties)[i++] = C4Value(%s);", f.unclashedName()));
		final Stream<String> setPropertyByS = fields.stream().map(f -> format("if (k == D.L.%s) { %1$s = to%s; return; }", f.unclashedName(), valueConversionSuffix(PrimitiveType.ANY, f.type())));
		final Stream<String> resetProperty = fields.stream().map(f -> format("if (k == D.L.%s) { %1$s = {}; return; }", f.unclashedName()));
		final Stream<String> idTable = idRefs.stream().map(id -> format("C4Def* %s;", id.name()));
		final Stream<String> assignIDTable = idRefs.stream().map(id -> format("%s = Definitions.GetByName(StdStrBuf(\"%1$s\"));", id.name()));

		final Stream<String> convertedFunctions = functions.stream().map(f ->
			format("%s %s::s_%s(%s)\n%s\n",
				cppTypeString(f.returnType()),
				script.name(),
				f.name(),
				parmsString(f, false),
				printNode(strTable, f, f.code())
			)
		);

		this.skeleton = Arrays.<Object>asList(
			"#include <C4Include.h>",
			"#include <C4NativeDef.h>",
			"#include <C4ValueHelpers.h>",
			"",
			"class "+script.name()+" : public C4Object",
			"{",
			"public:",
			"	// meta",
			"	class Def : public C4NativeDef",
			"	{",
			"	private:",
			"	public:",
			"		virtual C4Object* NewInstance();",
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
			"	// Exec overloads",
			"	C4Value Exec(C4AulFunc* func);",
			getExecOverloads(script, true),
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
			"C4Value " + script.name() + "::Exec(C4AulFunc* func) { if (!func) { return {}; } return func->Exec(this); }",
			getExecOverloads(script, false),
			"",
			format("C4Object* %s::Def::NewInstance() { return new %s(); }", script.name(), script.name()),
			"",
			convertedFunctions,
			format("decltype(%s::D) %1$s::D(\"%s\");", script.name(), script.resource() != null ? script.resource().getProjectRelativePath().toPortableString() : "")
		);
	}

	private Stream<String> getExecOverloads(Script script, boolean declaration) {
		return IntStream.range(1, 11)
			.mapToObj(num -> {
				final List<String> parameterNames = IntStream
					.range(0, num).mapToObj(parameterIndex -> "parameter" + (parameterIndex+1))
					.collect(Collectors.toList());
				return
					"C4Value "+(declaration?"":(script.name()+"::"))+"Exec(C4AulFunc* func, " +
					blockString("", "", ", ", parameterNames.stream().map(parameterName -> "const C4Value& " + parameterName)) + ")" +
					(declaration ? ";" : (" { if (!func) { return {}; } return func->Exec(this, &C4AulParSet("+blockString("", "", ", ", parameterNames)+")); }"));
			});
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

	private static int numberOfOccurencesOfCharacterInString(String string, char character) {
		int result = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == character) {
				result++;
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public Stream<String> flatten() {

		class Context {

			int indent = 0;

			private Indented indentify(Object item) {
				final String str = as(item, String.class);
				final int currentIndentation = indent;
				if (str != null) {
					indent = Math.max(0, indent + numberOfOccurencesOfCharacterInString(str, '{') - numberOfOccurencesOfCharacterInString(str, '}'));
				}
				return new Indented(currentIndentation, item);
			}

			Stream<String> flatten(Indented indented) {
				return
					indented.item instanceof String ? stream(new String[] {(String)indented.item}) :
					indented.item instanceof Stream ? ((Stream<String>)indented.item).map(str -> StringUtil.multiply("\t", indented.indentation) + str) :
					Stream.empty();
			}
		}

		final Context indent = new Context();
		return skeleton.stream()
			.map(indent::indentify)
			.flatMap(indent::flatten);
	}

	public static void render(Index index, Script script, PrintWriter output) {
		new CPPTemplate(index, script).flatten().forEach(s -> {
			output.append(s);
			output.append("\n");
		});
	}
}