package net.arctics.clonk.parser.c4script;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ExprWriter;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement.VarInitialization;

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
				if (elm instanceof VarDeclarationStatement) {
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
	public void print(C4Function function, Block body, Writer output) {
		
	}
	
	public static void test() {
		C4ScriptToCPPConverter converter = new C4ScriptToCPPConverter();
		try {
			StringWriter writer;
			converter.print(C4ScriptParser.parseStandaloneStatement("var ugh = 123, bla = \"ugh\";", null, null), writer = new StringWriter(), 0); //$NON-NLS-1$
			System.out.println(writer.getBuffer().toString());
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
