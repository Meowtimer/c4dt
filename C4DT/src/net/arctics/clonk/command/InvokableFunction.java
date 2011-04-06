package net.arctics.clonk.command;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.ast.ControlFlowException;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ReturnException;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.c4script.ast.evaluate.IVariableValueProvider;

public class InvokableFunction extends Function {
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	public Object invoke(final Object... args) {
		final IVariableValueProvider variableProvider = args != null && args.length > 0 && args[0] instanceof IVariableValueProvider ? (IVariableValueProvider)args[0] : null;
		IEvaluationContext context = new IEvaluationContext() {

			@Override
			public Object[] getArguments() {
				return args;
			}

			@Override
			public Function getFunction() {
				return InvokableFunction.this;
			}

			@Override
			public Object getValueForVariable(String varName) {
				return variableProvider != null ? variableProvider.getValueForVariable(varName) : null;
			}

			@Override
			public void reportOriginForExpression(ExprElm expression, SourceLocation location, IFile file) {
				
			}

			@Override
			public ScriptBase getScript() {
				return InvokableFunction.this.getScript();
			}

			@Override
			public int getCodeFragmentOffset() {
				return InvokableFunction.this.getCodeFragmentOffset();
			}

		};
		for (Statement s : getCodeBlock().getStatements()) {
			try {
				s.evaluate(context);
			} catch (ReturnException e) {
				return e.getResult();
			} catch (ControlFlowException e) {
				switch (e.getControlFlow()) {
				case BreakLoop:
					return null;
				case Continue:
					break;
				default:
					return null;
				}
			}
		}
		return null;
	}
}