package net.arctics.clonk.cli;

import static net.arctics.clonk.util.Utilities.as;

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
import java.util.Map.Entry;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.NumberLiteral;
import net.arctics.clonk.c4script.ast.Sequence;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.command.SelfContainedScript;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.ui.editors.c4script.ReplacementStatement;
import net.arctics.clonk.util.StreamUtil;

public class C4ScriptToCPPConverter {
	
	private final Map<String, String> stringConstants = new HashMap<String, String>();
	private final Set<Function> globalFunctionsUsed = new HashSet<Function>();
	private final Set<Variable> globalConstantsUsed = new HashSet<Variable>();
	
	private String regString(String text) {
		String ident = stringConstants.get(text);
		if (ident == null) {
			ident = "__string__" + stringConstants.size(); //$NON-NLS-1$
			stringConstants.put(text, ident);
		}
		return ident;
	}
	
	public void printExprElement(final Function function, ASTNode element, final Writer output, int depth) {
		element.print(new AppendableBackedExprWriter(output) {
			
			boolean prelude = true;
			
			@Override
			public boolean doCustomPrinting(ASTNode elm, int depth) {
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
					VarDeclarationStatement statement = (VarDeclarationStatement) elm;
					append("C4Value"); //$NON-NLS-1$
					append(" "); //$NON-NLS-1$
					int counter = 0;
					for (VarInitialization var : statement.variableInitializations()) {
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
					Sequence sequence = (Sequence)elm;
					ASTNode[] elements = sequence.subElements();
					for (int i = elements.length-1; i >= 0; i--) {
						ASTNode e = elements[i];
						CallDeclaration callFunc = as(e, CallDeclaration.class);
						MemberOperator op = i-1 >= 0 ? as(elements[i-1], MemberOperator.class) : null;
						if (callFunc != null && op != null)
							if (callFunc.declaration() instanceof Function) {
								Function f = (Function) callFunc.declaration();
								if (f.parentDeclaration() instanceof Engine) {
									globalFunctionsUsed.add(f);
									Sequence sequenceStart = sequence.subSequenceUpTo(op);
									append(String.format("engine%s->Exec(C4Value(", f.name())); sequenceStart.print(this, depth); append(").getPropList(), &C4AulParSet"); CallDeclaration.printParmString(this, callFunc.params(), 0); append(", true)");
									return true;
								}
							}
					}
				}
				else if (elm instanceof CallDeclaration) {
					CallDeclaration callFunc = (CallDeclaration) elm;
					if (callFunc.declaration() instanceof Function) {
						Function f = (Function) callFunc.declaration();
						if (f.parentDeclaration() instanceof Engine) {
							globalFunctionsUsed.add(f);
							append(String.format("engine%s->Exec(ctx->Obj, &C4AulParSet", f.name()));
							CallDeclaration.printParmString(this, callFunc.params(), 0);
							append(", true)");
							return true;
						}
					}
				}
				else if (elm instanceof AccessVar) {
					Variable var = as(((AccessVar)elm).declaration(), Variable.class);
					if (var != null && var.parentDeclaration() instanceof Engine) {
						globalConstantsUsed.add(var);
						append(String.format("const%s", var.name()));
						return true;
					}
				}
				return false;
			}

			private boolean writePrelude(ASTNode elm, int depth) {
				Block block = (Block) elm;
				Statement[] statements = new Statement[1+block.statements().length];
				statements[0] = new ReplacementStatement("FindEngineFunctions();");
				System.arraycopy(block.statements(), 0, statements, 1, block.statements().length);
				Block.printBlock(statements, this, depth);
				return true;
			}
		}, depth);
	}

	public void printFunction(Function function, Block body, Writer output) throws IOException {
		output.append("static C4Value ");
		output.append(function.name());
		output.append("(C4AulContext *ctx");
		for (Variable parm : function.parameters()) {
			output.append(", C4Value ");
			output.append(parm.name());
		}
		output.append(")");
		output.append("\n");
		this.printExprElement(function, body, output, 0);
	}
	
	public void printScript(Script script, Writer output) throws IOException {
		
		printHeader(output);
		
		output.append("namespace\n{\n");
		
		StringWriter scriptWriter = new StringWriter();
		for (Function f : script.functions()) {			
			printFunction(f, f.body(), scriptWriter);
			scriptWriter.append('\n');
			scriptWriter.append('\n');
		}
		
		printStringTable(output);
		printFunctionTable(output);
		
		output.append(scriptWriter.getBuffer());
	}
	
	private void printHeader(Writer output) throws IOException {
		String[] includes = new String[] {
			"C4Include.h",
			"C4StringTable.h",
			"C4Aul.h",
			"C4Object.h",
			"C4Def.h",
			"C4AulDefFunc.h"
		};
		for (String include : includes)
			output.append(String.format("#include \"%s\"\n", include));
		output.append("\n");
	}

	private void printStringTable(Writer output) throws IOException {
		for (Entry<String, String> entry : stringConstants.entrySet())
			output.append(String.format("C4String* %s = ::Strings.RegString(\"%s\");\n", entry.getValue(), entry.getKey()));
	}
	
	private void printFunctionTable(Writer output) throws IOException {
		for (Function f : globalFunctionsUsed)
			output.append(String.format("C4AulFunc *engine%s;\n", f.name()));
		for (Variable v : globalConstantsUsed)
			output.append(String.format("C4Value const%s;\n", v.name()));
		output.append("bool engineFunctionsFound = false;\n");
		output.append("void FindEngineFunctions()\n{\n");
		output.append("\tif(engineFunctionsFound)\n\t\t\treturn;\n");
		output.append("\tengineFunctionsFound = true;\n");
		for (Function f : globalFunctionsUsed)
			output.append(String.format("\tengine%1$s = ::ScriptEngine.GetFirstFunc(\"%1$s\");\n", f.name()));
		for (Variable v : globalConstantsUsed)
			output.append(String.format("\t::ScriptEngine.GetGlobalConstant(\"%1$s\", &const%1$s);\n", v.name()));
		output.append("}\n");
		output.append("}\n\n");
	}
	
	public static void main(String[] args) throws IOException, ParsingException {
		if (args.length < 3) {
			help();
			return;
		}
		String engineConfigurationFolder = args[0];
		String engine = args[1];
		File scriptToConvert = new File(args[2]);
		if (!scriptToConvert.exists()) {
			System.out.println(String.format("File %s does not exist", scriptToConvert.getPath()));
			return;
		}
		Core.headlessInitialize(engineConfigurationFolder, engine);
		InputStreamReader reader = new InputStreamReader(new FileInputStream(scriptToConvert));
		String script = StreamUtil.stringFromReader(reader);
		Index dummyIndex = new Index();
		SelfContainedScript scriptObj = new SelfContainedScript(scriptToConvert.getName(), script, dummyIndex);
		PrintWriter printWriter = new PrintWriter(System.out);
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
