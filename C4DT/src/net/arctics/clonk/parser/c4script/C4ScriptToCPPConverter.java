package net.arctics.clonk.parser.c4script;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.NumberLiteral;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement.VarInitialization;
import net.arctics.clonk.ui.editors.c4script.ReplacementStatement;
import net.arctics.clonk.util.StreamUtil;

public class C4ScriptToCPPConverter {
	
	private final Map<String, String> stringConstants = new HashMap<String, String>();
	private final List<Function> globalFunctionsUsed = new LinkedList<Function>();
	
	private String regString(String text) {
		String ident = stringConstants.get(text);
		if (ident == null) {
			ident = "__string__" + stringConstants.size(); //$NON-NLS-1$
			stringConstants.put(text, ident);
		}
		return ident;
	}
	
	public void printExprElement(ExprElm element, final Writer output, int depth) {
		element.print(new AppendableBackedExprWriter(output) {
			
			boolean prelude = true;
			
			@Override
			public boolean doCustomPrinting(ExprElm elm, int depth) {
				if (prelude && elm instanceof Block) {
					prelude = false;
					Block block = (Block) elm;
					Statement[] statements = new Statement[1+block.statements().length];
					statements[0] = new ReplacementStatement("FindEngineFunctions();");
					System.arraycopy(block.statements(), 0, statements, 1, block.statements().length);
					Block.printBlock(statements, this, depth);
					return true;
				}
				else if (elm instanceof StringLiteral) {
					append(String.format("C4Value(&%s)", regString(((StringLiteral) elm).getLiteral())));
					return true;
				}
				else if (elm instanceof NumberLiteral) {
					append(String.format("C4Value(%d)", ((NumberLiteral)elm).getLiteral()));
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
				else if (elm instanceof CallFunc) {
					CallFunc callFunc = (CallFunc) elm;
					if (callFunc.declaration() instanceof Function) {
						Function f = (Function) callFunc.declaration();
						if (f.parentDeclaration() instanceof Engine) {
							globalFunctionsUsed.add(f);
							append(String.format("CallEngineFunc(engine%s, C4AulParset", f.name()));
							callFunc.printParmString(this, 0);
							return true;
						}
					}
				}
				return false;
			}
		}, depth);
	}

	public void printFunction(Function function, Block body, Writer output) throws IOException {
		output.append("static ");
		output.append(PrimitiveType.cppTypeFromType(function.getReturnType()));
		output.append(" ");
		output.append(function.name());
		output.append("(C4AulContext *cthr");
		for (Variable parm : function.parameters()) {
			output.append(", ");
			output.append(PrimitiveType.cppTypeFromType(parm.getType()));
			output.append(" ");
			output.append(parm.name());
		}
		output.append(")");
		output.append("\n");
		this.printExprElement(body, output, 1);
	}
	
	public void printScript(Script script, Writer output) throws IOException {
		
		printHeader(output);
		
		output.append("namespace\n{\n");
		
		StringWriter scriptWriter = new StringWriter();
		for (Function f : script.functions()) {
			if (f instanceof Function) {
				Function invokable = f;
				printFunction(invokable, invokable.getCodeBlock(), scriptWriter);
			}
		}
		
		printStringTable(output);
		printFunctionTable(output);
		
		output.append(scriptWriter.getBuffer());
		
		output.append("\n}");
	}
	
	private void printHeader(Writer output) throws IOException {
		output.append("#include \"C4Include.h\"\n\n");
	}

	private void printStringTable(Writer output) throws IOException {
		for (Entry<String, String> entry : stringConstants.entrySet()) {
			output.append(String.format("C4String %s = C4String(\"%s\");\n", entry.getValue(), entry.getKey()));
		}
	}
	
	private void printFunctionTable(Writer output) throws IOException {
		for (Function f : globalFunctionsUsed) {
			output.append(String.format("C4AulFunc *engine%s;\n", f.name()));
		}
		output.append("bool engineFunctionsFound = false;\n");
		output.append("void FindEngineFunctions()\n{\n");
		output.append("\tif(engineFunctionsFound)\n\t\t\treturn;\n");
		output.append("\tengineFunctionsFound = true;\n");
		for (Function f : globalFunctionsUsed) {
			output.append(String.format("\tengine%1$s = ::ScriptEngine.GetFuncRecursive(\"%1$s\");\n", f.name()));
		}
		output.append("}\n");
		output.append("}\n");
	}
	
	public static void main(String[] args) throws IOException, ParsingException {
		if (args.length < 2) {
			help();
			return;
		}
		String engineConfigurationFolder = args[0];
		File scriptToConvert = new File(args[1]);
		if (!scriptToConvert.exists()) {
			System.out.println(String.format("File %s does not exist", args[0]));
			return;
		}
		ClonkCore.headlessInitialize(engineConfigurationFolder, "OpenClonk");
		InputStreamReader reader = new InputStreamReader(new FileInputStream(scriptToConvert));
		String script = StreamUtil.stringFromReader(reader);
		Index dummyIndex = new Index();
		ExecutableScript scriptObj = new ExecutableScript(scriptToConvert.getName(), script, dummyIndex);
		PrintWriter printWriter = new PrintWriter(System.out);
		new C4ScriptToCPPConverter().printScript(scriptObj, printWriter);
		printWriter.flush();
	}

	private static void help() {
		System.out.println("Parameters");
		System.out.println("1: Engine configuration folder");
		System.out.println("2: Script file to be converted");
	}
}
