package net.arctics.clonk.cli;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.printingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.AppendableBackedExprWriter;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.c4script.Conf;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.NumberLiteral;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.command.SelfContainedScript;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ui.editors.c4script.ReplacementStatement;
import net.arctics.clonk.util.StreamUtil;

public class C4ScriptToCPPConverter {

	private final Map<String, String> stringConstants = new HashMap<String, String>();
	private final Set<Function> globalFunctionsUsed = new HashSet<Function>();
	private final Set<Variable> globalConstantsUsed = new HashSet<Variable>();

	private String regString(final String text) {
		String ident = stringConstants.get(text);
		if (ident == null) {
			ident = "__string__" + stringConstants.size(); //$NON-NLS-1$
			stringConstants.put(text, ident);
		}
		return ident;
	}

	public void printExprElement(final Function function, final ASTNode element, final Writer output, final int depth) {
		element.print(new AppendableBackedExprWriter(output) {

			boolean prelude = true;

			@Override
			public boolean doCustomPrinting(final ASTNode elm, final int depth) {
				if (prelude && elm instanceof Block) {
					prelude = false;
					return writePrelude(elm, depth);
				}
				else if (elm instanceof StringLiteral) {
					append(String.format("C4Value(%s)", regString(((StringLiteral) elm).literal())));
					return true;
				}
				else if (elm instanceof NumberLiteral) {
					append(String.format("C4Value(%d)", ((NumberLiteral)elm).literal()));
					return true;
				}
				else if (elm instanceof VarDeclarationStatement) {
					final VarDeclarationStatement statement = (VarDeclarationStatement) elm;
					append("C4Value"); //$NON-NLS-1$
					append(" "); //$NON-NLS-1$
					int counter = 0;
					for (final VarInitialization var : statement.variableInitializations()) {
						append(var.name);
						if (var.expression != null) {
							append(" = "); //$NON-NLS-1$
							var.expression.print(this, depth+1);
						}
						if (++counter < statement.variableInitializations().length)
							append(", "); //$NON-NLS-1$
						else
							append(";"); //$NON-NLS-1$
					}
					return true;
				}
				else if (elm instanceof Sequence) {
					final Sequence sequence = (Sequence)elm;
					final ASTNode[] elements = sequence.subElements();
					for (int i = elements.length-1; i >= 0; i--) {
						final ASTNode e = elements[i];
						final CallDeclaration callFunc = as(e, CallDeclaration.class);
						final MemberOperator op = i-1 >= 0 ? as(elements[i-1], MemberOperator.class) : null;
						if (callFunc != null && op != null)
							if (callFunc.declaration() instanceof Function) {
								final Function f = (Function) callFunc.declaration();
								if (f.parentDeclaration() instanceof Engine) {
									globalFunctionsUsed.add(f);
									final Sequence sequenceStart = sequence.subSequenceUpTo(op);
									append(String.format("engine%s->Exec(C4Value(", f.name())); sequenceStart.print(this, depth); append(").getPropList(), &C4AulParSet"); Conf.printNodeList(this, callFunc.params(), 0, "(", ")"); append(", true)");
									return true;
								}
							}
					}
				}
				else if (elm instanceof CallDeclaration) {
					final CallDeclaration callFunc = (CallDeclaration) elm;
					if (callFunc.declaration() instanceof Function) {
						final Function f = (Function) callFunc.declaration();
						if (f.parentDeclaration() instanceof Engine) {
							globalFunctionsUsed.add(f);
							append(String.format("engine%s->Exec(ctx->Obj, &C4AulParSet", f.name()));
							Conf.printNodeList(this, callFunc.params(), 0, "(", ")");
							append(", true)");
							return true;
						}
					}
				}
				else if (elm instanceof AccessVar) {
					final Variable var = as(((AccessVar)elm).declaration(), Variable.class);
					if (var != null && var.parentDeclaration() instanceof Engine) {
						globalConstantsUsed.add(var);
						append(String.format("const%s", var.name()));
						return true;
					}
				}
				return false;
			}

			private boolean writePrelude(final ASTNode elm, final int depth) {
				final Block block = (Block) elm;
				final Statement[] statements = new Statement[1+block.statements().length];
				statements[0] = new ReplacementStatement("FindEngineFunctions();");
				System.arraycopy(block.statements(), 0, statements, 1, block.statements().length);
				Block.printBlock(statements, this, depth);
				return true;
			}
		}, depth);
	}

	public void printFunction(final Function function, final Block body, final Writer output) throws IOException {
		output.append("static C4Value ");
		output.append(function.name());
		output.append("(C4AulContext *ctx");
		for (final Variable parm : function.parameters()) {
			output.append(", C4Value ");
			output.append(parm.name());
		}
		output.append(")");
		output.append("\n");
		this.printExprElement(function, body, output, 0);
	}

	public void printScript(final Script script, final Writer output) throws IOException {

		printHeader(output);

		output.append("namespace\n{\n");

		final StringWriter scriptWriter = new StringWriter();
		for (final Function f : script.functions()) {
			printFunction(f, f.body(), scriptWriter);
			scriptWriter.append('\n');
			scriptWriter.append('\n');
		}

		printStringTable(output);
		printFunctionTable(output);

		output.append(scriptWriter.getBuffer());
	}

	private void printHeader(final Writer output) throws IOException {
		final String[] includes = new String[] {
			"C4Include.h",
			"C4StringTable.h",
			"C4Aul.h",
			"C4Object.h",
			"C4Def.h",
			"C4AulDefFunc.h",
			"C4NativeDef.h"
		};
		stream(includes)
			.map(include -> String.format("#include \"%s\"\n", include))
			.forEach(printingException(output::append, IOException.class));
		output.append("\n");
	}

	private void printStringTable(final Writer output) throws IOException {
		stringConstants.entrySet().stream().map(
			entry -> String.format("C4String* %s = ::Strings.RegString(\"%s\");\n", entry.getValue(), entry.getKey())
		).forEach(printingException(output::append, IOException.class));
	}

	private void printFunctionTable(final Writer output) throws IOException {
		for (final Function f : globalFunctionsUsed)
			output.append(String.format("C4AulFunc *engine%s;\n", f.name()));
		for (final Variable v : globalConstantsUsed)
			output.append(String.format("C4Value const%s;\n", v.name()));
		output.append("bool engineFunctionsFound = false;\n");
		output.append("void FindEngineFunctions()\n{\n");
		output.append("\tif(engineFunctionsFound)\n\t\t\treturn;\n");
		output.append("\tengineFunctionsFound = true;\n");
		for (final Function f : globalFunctionsUsed)
			output.append(String.format("\tengine%1$s = ::ScriptEngine.GetFirstFunc(\"%1$s\");\n", f.name()));
		for (final Variable v : globalConstantsUsed)
			output.append(String.format("\t::ScriptEngine.GetGlobalConstant(\"%1$s\", &const%1$s);\n", v.name()));
		output.append("}\n");
		output.append("}\n\n");
	}

	public static void main(final String[] args) throws IOException, ProblemException {
		if (args.length < 3) {
			help();
			return;
		}
		final String engineConfigurationFolder = args[0];
		final String engine = args[1];
		final File scriptToConvert = new File(args[2]);
		if (!scriptToConvert.exists()) {
			System.out.println(String.format("File %s does not exist", scriptToConvert.getPath()));
			return;
		}
		Core.headlessInitialize(engineConfigurationFolder, engine);
		final InputStreamReader reader = new InputStreamReader(new FileInputStream(scriptToConvert));
		final String script = StreamUtil.stringFromReader(reader);
		final Index dummyIndex = new Index();
		final SelfContainedScript scriptObj = new SelfContainedScript(scriptToConvert.getName(), script, dummyIndex);
		final PrintWriter printWriter = new PrintWriter(System.out);
		new C4ScriptToCPPConverter().printScript(scriptObj, printWriter);
		printWriter.flush();
	}

	private static void help() {
		System.out.println("Parameters");
		System.out.println("1: Engine configuration folder");
		System.out.println("2: Name of engine to use");
		System.out.println("3: Script file to be converted");
	}
}
