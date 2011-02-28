package net.arctics.clonk.parser.c4script.ast;

import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.util.ArrayUtil;
import org.eclipse.jface.text.Region;

public class VarDeclarationStatement extends KeywordStatement {
	
	public static final class VarInitialization extends ExprElm {

		private static final long serialVersionUID = 1L;

		public String name;
		public ExprElm expression;
		public Variable variableBeingInitialized;
		public VarInitialization(String name, ExprElm expression, int namePos) {
			super();
			this.name = name;
			this.expression = expression;
			setExprRegion(namePos, expression != null ? expression.getExprEnd() : namePos + name.length());
		}
		@Override
		public ExprElm[] getSubElements() {
			return new ExprElm[] {expression};
		}
		@Override
		public void setSubElements(ExprElm[] elms) {
			expression = elms[0];
		}
		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append(name);
			if (expression != null) {
				output.append(" = ");
				expression.print(output, depth+1);
			}
		}
		public VarInitialization getPreviousInitialization() {
			VarInitialization[] brothers = getParent(VarDeclarationStatement.class).varInitializations;
			return ArrayUtil.boundChecked(brothers, ArrayUtil.indexOf(this, brothers)-1);
		}
		public VarInitialization getNextInitialization() {
			VarInitialization[] brothers = getParent(VarDeclarationStatement.class).varInitializations;
			return ArrayUtil.boundChecked(brothers, ArrayUtil.indexOf(this, brothers)+1);
		}
	}
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private VarInitialization[] varInitializations;
	private Scope scope;

	public VarDeclarationStatement(List<VarInitialization> varInitializations, Scope scope) {
		super();
		this.varInitializations = varInitializations.toArray(new VarInitialization[varInitializations.size()]);
		this.scope = scope;
		assignParentToSubElements();
	}
	public VarDeclarationStatement(String varName, ExprElm initialization, int namePos, Scope scope) {
		this(ArrayUtil.list(new VarInitialization(varName, initialization, namePos)), scope);
	}
	@Override
	public String getKeyword() {
		return scope.toKeyword();
	}
	@Override
	public VarInitialization[] getSubElements() {
		return varInitializations;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		VarInitialization[] newElms = new VarInitialization[elms.length];
		System.arraycopy(elms, 0, newElms, 0, elms.length);
		varInitializations = newElms;
	}
	public final VarInitialization[] getVarInitializations() {
		return varInitializations;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(getKeyword());
		builder.append(" "); //$NON-NLS-1$
		int counter = 0;
		for (VarInitialization var : varInitializations) {
			var.print(builder, depth);
			if (++counter < varInitializations.length)
				builder.append(", "); //$NON-NLS-1$
			else
				builder.append(";"); //$NON-NLS-1$
		}
	}
	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		Function activeFunc = parser.getCurrentFunc();
		if (activeFunc != null) {				
			int addToMakeAbsolute = activeFunc.getBody().getStart() + this.getExprStart();
			offset += addToMakeAbsolute;
			for (VarInitialization pair : varInitializations) {
				String varName = pair.name;
				Variable var = activeFunc.findVariable(varName);
				if (var != null && var.isAt(offset))
					return new DeclarationRegion(var, new Region(var.getLocation().getStart()-activeFunc.getBody().getStart(), var.getLocation().getLength()));
			}
		}
		return super.declarationAt(offset, parser);
	}
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		for (VarInitialization initialization : varInitializations) {
			if (initialization.variableBeingInitialized != null && initialization.expression != null) {
				new AccessVar(initialization.variableBeingInitialized).expectedToBeOfType(initialization.expression.getType(parser), parser, TypeExpectancyMode.Force);
			}
		}
	}
}