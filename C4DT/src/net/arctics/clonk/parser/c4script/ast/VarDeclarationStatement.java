package net.arctics.clonk.parser.c4script.ast;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.Region;

public class VarDeclarationStatement extends KeywordStatement {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private List<Pair<String, ExprElm>> varInitializations;
	private C4VariableScope scope;

	@SuppressWarnings("unchecked")
	public VarDeclarationStatement(String varName, ExprElm initialization, C4VariableScope scope) {
		this(Utilities.list(new Pair<String, ExprElm>(varName, initialization)), scope);
	}
	public VarDeclarationStatement(List<Pair<String, ExprElm>> varInitializations, C4VariableScope scope) {
		super();
		this.varInitializations = varInitializations;
		this.scope = scope;
		assignParentToSubElements();
	}
	@Override
	public String getKeyword() {
		return scope.toKeyword();
	}
	@Override
	public ExprElm[] getSubElements() {
		List<ExprElm> result = new LinkedList<ExprElm>();
		for (Pair<String, ExprElm> initialization : varInitializations) {
			if (initialization.getSecond() != null)
				result.add(initialization.getSecond());
		}
		return result.toArray(new ExprElm[0]);
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		int j = 0;
		for (Pair<String, ExprElm> pair : varInitializations) {
			if (pair.getSecond() != null)
				pair.setSecond(elms[j++]);
		}
	}
	public List<Pair<String, ExprElm>> getVarInitializations() {
		return varInitializations;
	}
	public void setVarInitializations(List<Pair<String, ExprElm>> varInitializations) {
		this.varInitializations = varInitializations;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(getKeyword());
		builder.append(" "); //$NON-NLS-1$
		int counter = 0;
		for (Pair<String, ExprElm> var : varInitializations) {
			builder.append(var.getFirst());
			if (var.getSecond() != null) {
				builder.append(" = "); //$NON-NLS-1$
				var.getSecond().print(builder, depth+1);
			}
			if (++counter < varInitializations.size())
				builder.append(", "); //$NON-NLS-1$
			else
				builder.append(";"); //$NON-NLS-1$
		}
	}
	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		C4Function activeFunc = parser.getActiveFunc();
		if (activeFunc != null) {				
			int addToMakeAbsolute = activeFunc.getBody().getStart() + this.getExprStart();
			offset += addToMakeAbsolute;
			for (Pair<String, ExprElm> pair : varInitializations) {
				String varName = pair.getFirst();
				C4Variable var = activeFunc.findVariable(varName);
				if (var != null && var.isAt(offset))
					return new DeclarationRegion(var, new Region(var.getLocation().getStart()-activeFunc.getBody().getStart(), var.getLocation().getLength()));
			}
		}
		return super.declarationAt(offset, parser);
	}
}