package net.arctics.clonk.parser.c4script;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.command.BodyPreservingFunction;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ExprWriter;
import net.arctics.clonk.parser.c4script.ast.NumberLiteral;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement.VarInitialization;
import net.arctics.clonk.util.Utilities;

public class C4ScriptToCPPConverter {
	
	private Map<String, String> stringConstants = new HashMap<String, String>();
	
	public String regString(String text) {
		String ident = stringConstants.get(text);
		if (ident == null) {
			ident = "__string__" + stringConstants.size(); //$NON-NLS-1$
			stringConstants.put(text, ident);
		}
		return ident;
	}
	
	public void printStringTable(Writer writer, int depth) throws IOException {
		/*for (Entry<String, String> entry : stringConstants.entrySet()) {
			writer.append("void stringTable() {"); //$NON-NLS-1$
			//writer.append("\t)
		}*/
	}
	
	public void print(ExprElm element, final Writer output, int depth) {
		element.print(new ExprWriter() {
			
			@Override
			public boolean doCustomPrinting(ExprElm elm, int depth) {
				if (elm instanceof StringLiteral) {
					append(regString(((StringLiteral) elm).getLiteral()));
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
					for (VarInitialization var : statement.getVarInitializations()) {
						append(var.name);
						if (var.expression != null) {
							append(" = "); //$NON-NLS-1$
							var.expression.print(this, depth+1);
						}
						if (++counter < statement.getVarInitializations().size())
							append(", "); //$NON-NLS-1$
						else
							append(";"); //$NON-NLS-1$
					}
					return true;
				}
				else
					return false;
			}
			
			@Override
			public void append(char c) {
				try {
					output.append(c);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void append(String text) {
				try {
					output.append(text);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, depth);
	}

	public void print(C4Function function, Block body, Writer output) throws IOException {
		output.append("static ");
		output.append(C4Type.cppTypeFromType(function.getReturnType()));
		output.append(" ");
		output.append(function.getName());
		output.append("(C4AulContext *cthr");
		for (C4Variable parm : function.getParameters()) {
			output.append(", ");
			output.append(C4Type.cppTypeFromType(parm.getType()));
			output.append(" ");
			output.append(parm.getName());
		}
		output.append(")\n{\n");
		this.print(body, output, 0); //$NON-NLS-1$
		output.append("}\n");
	}
	
	public void print(C4ScriptBase script, Writer output) throws IOException {
		for (C4Function f : script.functions()) {
			if (f instanceof BodyPreservingFunction) {
				BodyPreservingFunction bf = (BodyPreservingFunction) f;
				print(bf, bf.getBodyBlock(), output);
			}
		}
	}
	
	public static void main(String[] args) throws IOException, ParsingException {
		if (args.length == 0) {
			System.out.println("Provide script file");
			return;
		}
		File scriptToConvert = new File(args[0]);
		if (!scriptToConvert.exists()) {
			System.out.println(String.format("File %s does not exist", args[0]));
			return;
		}
		ClonkCore.headlessInitialize("OpenClonk");
		InputStreamReader reader = new InputStreamReader(new FileInputStream(scriptToConvert));
		String script = Utilities.stringFromReader(reader);
		ExecutableScript scriptObj = new ExecutableScript(scriptToConvert.getName(), script);
		PrintWriter printWriter = new PrintWriter(System.out);
		new C4ScriptToCPPConverter().print(scriptObj, printWriter);
	}
}
