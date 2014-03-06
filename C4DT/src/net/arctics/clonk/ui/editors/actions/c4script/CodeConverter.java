package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.AppendableBackedExprWriter;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Conf;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.IHasCode;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SynthesizedFunction;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.c4script.typing.PrimitiveType;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public abstract class CodeConverter {
	public interface ICodeConverterContext {
		String defineFunctionLocalVariable(String name);
	}
	private ASTNode codeFor(final Declaration declaration) {
		if (declaration instanceof IHasCode)
			return ((IHasCode)declaration).code();
		else
			return declaration;
	}
	public void runOnDocument(
		final Script script,
		final IDocument document
	) {
		synchronized (document) {
			final TextChange textChange = new DocumentChange(Messages.TidyUpCodeAction_TidyUpCode, document);
			textChange.setEdit(new MultiTextEdit());
			final List<Declaration> decs = new ArrayList<Declaration>();
			for (final Declaration d : script.subDeclarations(script.index(), DeclMask.FUNCTIONS|DeclMask.VARIABLES|DeclMask.STATIC_VARIABLES))
				if (!(d instanceof Variable && d.parentDeclaration() instanceof Function) && codeFor(d) != null)
					decs.add(d);
			Collections.sort(decs, new Comparator<Declaration>() {
				@Override
				public int compare(final Declaration a, final Declaration b) {
					final ASTNode codeA = codeFor(a);
					final ASTNode codeB = codeFor(b);
					return codeB.absolute().getOffset()-codeA.absolute().getOffset();
				}
			});
			for (final Declaration d : decs)
				try {
					if (d instanceof SynthesizedFunction)
						continue;
					final ASTNode elms = codeFor(d);
					if (d instanceof Function) {
						final Function f = (Function)d;
						final StringBuilder header = new StringBuilder(f.header().getLength()+10);
						f.printHeader(new AppendableBackedExprWriter(header), false);
						textChange.addEdit(new ReplaceEdit(f.header().start(), f.header().getLength(), header.toString()));
						f.setOldStyle(false);
					}
					convertCode(document, textChange, d, elms);
				} catch (final CloneNotSupportedException e1) {
					e1.printStackTrace();
				} catch (final BadLocationException e) {
					e.printStackTrace();
				}
			try {
				textChange.perform(new NullProgressMonitor());
			} catch (final CoreException e) {
				e.printStackTrace();
			} catch (final Exception x) {
				x.printStackTrace();
			}
		}
	}
	protected abstract ASTNode performConversion(ASTNode expression, Declaration declaration, CodeConverter.ICodeConverterContext cookie);
	private static boolean superflousBetweenFuncHeaderAndBody(final char c) {
		return c == '\t' || c == ' ' || c == '\n' || c == '\r';
	}
	private void convertCode(final IDocument document, final TextChange textChange, final Declaration codeOwner, final ASTNode code) throws BadLocationException, CloneNotSupportedException {
		final IRegion region = code.absolute();
		int oldStart = region.getOffset();
		int oldLength = region.getLength();
		while (oldStart - 1 >= 0 && superflousBetweenFuncHeaderAndBody(document.getChar(oldStart-1))) {
			oldStart--;
			oldLength++;
		}
		oldLength = Math.min(oldLength, document.getLength()-oldStart);
		final String oldString = document.get(oldStart, oldLength);
		final StringBuilder builder = new StringBuilder();
		if (!(codeOwner instanceof Function))
			builder.append(' ');
		final ASTNodePrinter newStringWriter = new AppendableBackedExprWriter(builder);
		final Function function = as(codeOwner, Function.class);
		if (function != null)
			Conf.blockPrelude(newStringWriter, 0);
		final class CodeConverterContext implements ICodeConverterContext {
			private final Map<String, VarInitialization> addedVars = new HashMap<>(3);
			@Override
			public String defineFunctionLocalVariable(final String name) {
				if (function != null && function.findVariable(name) == null && addedVars.get(name) == null) {
					final Variable var = new Variable(name, PrimitiveType.ANY);
					addedVars.put(name, new VarInitialization(name, null, 0, 0, var, null));
				}
				return name;
			}
			public ASTNode postProcess(ASTNode converted) {
				if (converted instanceof FunctionBody && addedVars.size() > 0)
					converted = new FunctionBody(function, concat(
						new VarDeclarationStatement(
							Arrays.asList(addedVars.values().toArray(new VarInitialization[addedVars.size()])), Scope.VAR),
						converted.subElements()));
				return converted;
			}
		}
		final CodeConverterContext ctx = new CodeConverterContext();
		ASTNode conv = performConversion(code, codeOwner, ctx);
		conv = ctx.postProcess(conv);
		conv.print(newStringWriter, 0);
		final String newString = newStringWriter.toString();
		if (!oldString.equals(newString)) try {
			textChange.addEdit(new ReplaceEdit(oldStart, oldLength, newString));
		} catch (final MalformedTreeException malformed) {
			System.out.println(codeOwner.name());
			malformed.printStackTrace();
			throw malformed;
		}
	}
}